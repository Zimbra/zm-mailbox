/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.RemoteIP;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.SoapEngine;


public class ZimbraInvalidLoginFilter extends DoSFilter {

    private static final int  DEFAULT_MAX_FAILED_LOGIN  = 5;
    private int maxFailedLogin;
    private Map<String, AtomicInteger> numberOfFailedOccurence;
    private Map<String, Long> suspiciousIpAddrLastAttempt;
    private final int DEFAULT_DELAY_IN_MIN_BETWEEN_REQ_BEFORE_REINSTATING = 60;
    private int delayInMinBetwnReqBeforeReinstating;
    private final int DEFAULT_REINSTATE_IP_TASK_INTERVAL_IN_MIN = 5;
    private int reinstateIpTaskIntervalInMin;
    private static final int MIN_TO_MS = 60 * 1000;
    public static final String AUTH_FAILED = "auth.failed";
    public int maxSizeOfFailedIpDb;
    public int DEFAULT_SIZE_OF_FAILED_IP_DB = 7000; // Considering we do not want to exceed 1MB of data

    /* (non-Javadoc)
     * @see com.zimbra.cs.servlet.DoSFilter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        Provisioning prov = Provisioning.getInstance();
        try {
            this.maxFailedLogin = prov.getConfig().getInvalidLoginFilterMaxFailedLogin();
        } catch (ServiceException e) {
            this.maxFailedLogin = DEFAULT_MAX_FAILED_LOGIN;
        }

        try {
            this.reinstateIpTaskIntervalInMin = prov.getConfig()
                .getInvalidLoginFilterReinstateIpTaskIntervalInMin();
        } catch (ServiceException e) {
            this.reinstateIpTaskIntervalInMin = DEFAULT_REINSTATE_IP_TASK_INTERVAL_IN_MIN;
        }

        try {
            this.delayInMinBetwnReqBeforeReinstating = prov.getConfig()
                .getInvalidLoginFilterDelayInMinBetwnReqBeforeReinstating();
        } catch (ServiceException e) {
            this.delayInMinBetwnReqBeforeReinstating = DEFAULT_DELAY_IN_MIN_BETWEEN_REQ_BEFORE_REINSTATING;
        }

        try {
            this.maxSizeOfFailedIpDb = prov.getConfig().getInvalidLoginFilterMaxSizeOfFailedIpDb();
        } catch (ServiceException e) {
            this.maxSizeOfFailedIpDb = this.DEFAULT_SIZE_OF_FAILED_IP_DB;
        }

        this.numberOfFailedOccurence = Collections.synchronizedMap(new LinkedHashMap<String, AtomicInteger>(
            this.maxSizeOfFailedIpDb) {
            @Override
            protected boolean removeEldestEntry(Map.Entry  eldest) {
               return size() >  maxSizeOfFailedIpDb;
            }
         });

        this.suspiciousIpAddrLastAttempt = Collections.synchronizedMap(new LinkedHashMap<String, Long>(
            this.maxSizeOfFailedIpDb) {
            @Override
            protected boolean removeEldestEntry(Map.Entry  eldest) {
               return size() >  maxSizeOfFailedIpDb;
            }
         });
        Zimbra.sTimer.schedule(new ReInStateIpTask(), 1000,
            this.reinstateIpTaskIntervalInMin * MIN_TO_MS);
        ZimbraLog.misc.info("ZimbraInvalidLoginFilter intialized");

    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.servlet.ZimbraQoSFilter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        RemoteIP remoteIp = new RemoteIP(req, ZimbraServlet.getTrustedIPs());
        String clientIp = remoteIp.getOrigIP();
        if (clientIp == null || this.getWhitelist().contains(clientIp)) {
            // Bug: 89930 Account for request coming from local server
            // or case where the proxy server has not been added to zimbraMailTrustedIP
            chain.doFilter(request, response);
            return;
        }

        if (this.maxFailedLogin <=0) {
            // InvalidLoginFilter feature is turned off
            chain.doFilter(request, response);
            return;
        }
        if (this.suspiciousIpAddrLastAttempt.containsKey(clientIp)) {
            ZimbraLog.misc.info ("Access to IP " + clientIp +  "suspended, for repeated failed login.");
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        } else {
            chain.doFilter(request, response);

            if (req.getAttribute(AUTH_FAILED) != null) {
                ZimbraLog.misc
                .info("Invalid login filter, checking if this was an auth req and authentication failed.");
                String clientIP = (String) req.getAttribute(SoapEngine.REQUEST_IP);
                boolean loginFailed = (Boolean) req.getAttribute(AUTH_FAILED);
                if (loginFailed) {

                    AtomicInteger count = null;
                    if (this.numberOfFailedOccurence.containsKey(clientIp)) {
                        count = this.numberOfFailedOccurence.get(clientIp);
                    } else{
                        this.numberOfFailedOccurence.put(clientIp, new AtomicInteger(0));
                        count = this.numberOfFailedOccurence.get(clientIp);
                    }

                    if (count.incrementAndGet() > maxFailedLogin) {
                        this.numberOfFailedOccurence.put(clientIp, count);
                        suspiciousIpAddrLastAttempt.put(clientIp,
                            System.currentTimeMillis());
                    }
                    this.numberOfFailedOccurence.put(clientIp, count);
                }
                if (ZimbraLog.misc.isDebugEnabled()) {
                    ZimbraLog.misc.debug("Login failed " + clientIP + ", "
                        + loginFailed);
                }
            }
        }
    }


    /* (non-Javadoc)
     * @see com.zimbra.cs.servlet.ZimbraQoSFilter#destroy()
     */
    @Override
    public void destroy() {
        super.destroy();
        this.numberOfFailedOccurence.clear();
        this.suspiciousIpAddrLastAttempt.clear();
        ZimbraLog.misc.info("ZimbraInvalidLoginFilter destroyed");
    }

    public  final class ReInStateIpTask extends TimerTask {

        public ReInStateIpTask() {

        }
        /* (non-Javadoc)
         * @see java.util.TimerTask#run()
         */
        @Override
        public void run() {

            Set<String> clientIps = suspiciousIpAddrLastAttempt.keySet();
            long now = System.currentTimeMillis();
            synchronized (suspiciousIpAddrLastAttempt) {
                for (String clientIp : clientIps) {
                    long lastLoginAttempt = suspiciousIpAddrLastAttempt.get(clientIp);
                    if ((now - lastLoginAttempt) > delayInMinBetwnReqBeforeReinstating * MIN_TO_MS) {
                        suspiciousIpAddrLastAttempt.remove(clientIp);
                        numberOfFailedOccurence.remove(clientIp);
                    }
                }
            }
        }

    }
}
