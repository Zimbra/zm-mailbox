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
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.servlet.util.CsrfUtil;
import com.zimbra.soap.RequestContext;


/**
 * @author zimbra
 *
 */
public class CsrfFilter implements Filter {

    /* (non-Javadoc)
     * @see javax.servlet.Filter#destroy()
     */
    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        ZimbraLog.clearContext();

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse)response;

        if (req.getMethod().equalsIgnoreCase("POST")) {
            if (ZimbraLog.misc.isDebugEnabled()) {
                ZimbraLog.misc.debug("CSRF: " + req.getContextPath());
            }
            try {
                if (CsrfUtil.isCsrfRequest(req, checkReqForCsrf, allowedRefHost)) {
                    resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                // We need virtual host information in DefangFilter
                // Set them in ThreadLocal here
                RequestContext reqCtxt = new RequestContext();
                String host = CsrfUtil.getRequestHost(req);
                reqCtxt.setVirtualHost(host);
                ZThreadLocal.setContext(reqCtxt);
                chain.doFilter(req, resp);

            } finally {
                // Unset the variables set in thread local
                ZThreadLocal.unset();
            }
        } else {
            chain.doFilter(req, resp);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialize the parameters related to CSRF check
        this.allowedRefHost = convertToList(filterConfig.getInitParameter(PARAM_CSRF_ALLOW_REF_HOSTS));
        this.checkReqForCsrf = Boolean.parseBoolean(filterConfig.getInitParameter(PARAM_CSRF_CHECK));
    }


    /**
     * @param initParameter
     * @return
     */
    protected static List<String> convertToList(String hostList) {
        List<String> hosts = null;
        if (!StringUtil.isNullOrEmpty(hostList)) {
            String [] temp = hostList.split(",");
            for (int i = 0; i < temp.length; ++i) {
                temp[i] = temp[i].toLowerCase();
            }
            hosts = Arrays.asList(temp);
        }
        return hosts;
    }


    protected boolean checkReqForCsrf = false;
    protected List<String> allowedRefHost = null;
    // For future use Bug 83762
    //    protected boolean allowOriginCheck  = false;


    protected static final String PARAM_CSRF_CHECK = "csrf.req.check";
    protected static final String PARAM_CSRF_ALLOW_REF_HOSTS = "allowed.referrer.host";
    // this could be an additional check, when it is better supported across browsers.
    // Bug 83762
    // protected static final String PARAM_CSRF_ALLOW_ORIGIN_CHECK = "allow.origin.check";

}
