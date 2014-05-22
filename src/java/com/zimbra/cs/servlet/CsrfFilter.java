/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra, Inc.
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
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.net.HttpHeaders;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.CsrfTokenKey;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.servlet.util.CsrfUtil;
import com.zimbra.soap.RequestContext;

/**
 * @author zimbra
 *
 */
public class CsrfFilter implements Filter {

    protected boolean csrfCheckEnabled ;
    protected boolean csrfRefererCheckEnabled;
    protected String[] allowedRefHost = null;
    public static final String CSRF_TOKEN = "X-Zimbra-Csrf-Token";
    public static final String AUTH_TOKEN = "AuthToken";
    public static final String CSRF_TOKEN_CHECK = "CsrfTokenCheck";
    public static final String CSRF_TOKEN_VALID = "CsrfTokenValid";
    protected int maxCsrfTokenValidityInMs;
    private Random nonceGen = null;

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialize the parameters related to CSRF check
        Provisioning prov = Provisioning.getInstance();
        try {
            this.csrfCheckEnabled = prov.getConfig().isCsrfTokenCheckEnabled();
            this.csrfRefererCheckEnabled = prov.getConfig().isCsrfRefererCheckEnabled();
            this.allowedRefHost = prov.getConfig().getCsrfAllowedRefererHosts();
            nonceGen = new Random();
            CsrfTokenKey.getCurrentKey();
            ZimbraLog.misc.info("CSRF filter was initialized : "
            + "CSRFcheck enabled: " +  this.csrfCheckEnabled
            + "CSRF referer check enabled: " +  this.csrfRefererCheckEnabled
            + ", CSRFAllowedRefHost: " +  this.allowedRefHost
            + ", CSRFTokenValidity " +  this.maxCsrfTokenValidityInMs + "ms.");
        } catch (ServiceException e) {
            throw new ServletException("Error initializing CSRF filter"
                + e.getMessage());
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {

        ZimbraLog.filter.info("Destroying CSRF filter.");

    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        ZimbraLog.clearContext();

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        if (ZimbraLog.misc.isDebugEnabled()) {
             ZimbraLog.misc.debug("CSRF Request URI: " + req.getRequestURI());
        }

        if (ZimbraLog.misc.isTraceEnabled()) {
            Enumeration<String> hdrNames = req.getHeaderNames();
            ZimbraLog.misc.trace("Soap request headers.");
            while (hdrNames.hasMoreElements()) {
                String name = hdrNames.nextElement();
                // we do not want to print cookie headers for security reasons.
                if (name.contains(HttpHeaders.COOKIE))
                    continue;
                ZimbraLog.misc.trace(name + "=" + req.getHeader(name));
            }
        }

        if (this.csrfRefererCheckEnabled) {
            if (!allowReqBasedOnRefererHeaderCheck(req)) {
                ZimbraLog.misc.info("CSRF referer check failed");
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        if (!this.csrfCheckEnabled) {
            chain.doFilter(req, resp);
        } else {
            AuthToken authToken = CsrfUtil.getAuthTokenFromReq(req);
            if (CsrfUtil.doCsrfCheck(req, authToken)) {
                if (CsrfUtil.isCsrfTokenCreated(authToken)) {
                    req.setAttribute(CSRF_TOKEN_CHECK, Boolean.TRUE);
                } else {
                    ZimbraLog.misc.debug("CSRF token not created for this Auth Token.");
                }
            }
            chain.doFilter(req, resp);
            if (req.getAttribute(CSRF_TOKEN_VALID) != null) {
                boolean validToken = (Boolean) req.getAttribute(CSRF_TOKEN_VALID);
                if (!validToken) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }
        }

        try {
            // We need virtual host information in DefangFilter
            // Set them in ThreadLocal here
            RequestContext reqCtxt = new RequestContext();
            String host = CsrfUtil.getRequestHost(req);
            reqCtxt.setVirtualHost(host);
            ZThreadLocal.setContext(reqCtxt);

            // // if this is a Auth request, then the Auth token would have been
            // generated so sent CSRF token with response, we expect it in the next
            // req.
            if (req.getAttribute(AUTH_TOKEN) != null) {
                AuthToken auth = (AuthToken) req.getAttribute(AUTH_TOKEN);
                if (auth.isCsrfTokenEnabled()) {
                    generateAndSetCsrfToken(req, resp, true);
                }
            }

        } finally {
            // Unset the variables set in thread local
            ZThreadLocal.unset();
        }

    }

    /**
     * @param req
     * @param resp
     * @throws ServletException
     */
    private void generateAndSetCsrfToken(HttpServletRequest req,
        HttpServletResponse resp, boolean afterAuth) throws ServletException {
        AuthToken authToken = null;
        if (afterAuth) {
            authToken = (AuthToken) req.getAttribute(AUTH_TOKEN);
        } else {
            authToken = CsrfUtil.getAuthTokenFromReq(req);
        }

        if (authToken != null) {
            String accountId = authToken.getAccountId();
            long authTokenExpiration = authToken.getExpires();
            try {
                String token = CsrfUtil.generateCsrfToken(accountId,
                    authTokenExpiration, nonceGen.nextInt() + 1, authToken.getCrumb());
                resp.setHeader(CSRF_TOKEN, token);
            } catch (AuthTokenException e) {
                throw new ServletException("Error generating CSRF token.");
            }
        }
    }

    /**
     * @param initParameter
     * @return
     */
    protected static List<String> convertToList(String urlList) {
        List<String> urls = null;
        if (!StringUtil.isNullOrEmpty(urlList)) {
            String[] temp = urlList.split(",");
            for (int i = 0; i < temp.length; ++i) {
                temp[i] = temp[i].toLowerCase();
            }
            urls = Arrays.asList(temp);
        }
        return urls;
    }


    private boolean allowReqBasedOnRefererHeaderCheck(HttpServletRequest req) {

        try {
            if (CsrfUtil.isCsrfRequestBasedOnReferrer(req, allowedRefHost)) {
                return false;
            }
        } catch (MalformedURLException e) {
            ZimbraLog.misc.info("Error while doing referer based check." + e.getMessage());
            return false;
        }
        return true;

    }








}
