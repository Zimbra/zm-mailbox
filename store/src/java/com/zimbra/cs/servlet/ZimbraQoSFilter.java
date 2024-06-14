/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.servlet.util.AuthUtil;
import com.zimbra.soap.SoapServlet;

/**
 * This Servlet {@link Filter} limits the number of concurrent HTTP requests per
 * account.
 * <p>
 * It temporarily suspends the requests if exceeding the limit. The limit is configurable
 * by {@link LC#servlet_max_concurrent_http_requests_per_account}. To disable, set
 * the LC key to 0 or remove this servlet filter from web.xml.
 *
 */


public class ZimbraQoSFilter implements Filter {

    final static int DEFAULT_WAIT_MS=50;
    final static long DEFAULT_SUSPEND_MS = 1000;
    
    final static String MAX_WAIT_INIT_PARAM="waitMs";
    final static String SUSPEND_INIT_PARAM="suspendMs";

    private long waitMs;
    private long suspendMs;

    private ConcurrentLinkedHashMap<String, Semaphore> passes = new ConcurrentLinkedHashMap.Builder<String, Semaphore>()
                                                                .maximumWeightedCapacity(2000).build();

    private Semaphore getPass(ServletRequest request) {
        String user = extractUserId(request);
        if (user == null) {
            return null;
        }
        int max = LC.servlet_max_concurrent_http_requests_per_account.intValue();
        if (max <= 0) {
            return null;
        }

        Semaphore freshPass = new Semaphore(max, true);
        Semaphore existingPass = passes.putIfAbsent(user, freshPass);
        if (existingPass == null) {
            return freshPass;
        } else {
            return existingPass;
        }
    }

    public static String extractUserId(ServletRequest request) {
        try {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest req = (HttpServletRequest) request;
                boolean isAdminRequest = AuthUtil.isAdminRequest(req);
                AuthToken at = AuthProvider.getAuthToken(req, isAdminRequest);
                if (at == null) {
                    Map <Object, Object> engineCtxt = new HashMap<Object, Object>();
                    engineCtxt.put(SoapServlet.SERVLET_REQUEST, req);
                    at = AuthProvider.getJWToken(null, engineCtxt);
                }
                if (at != null) {
                    return at.getAccountId();
                }
                // Check if this is Http Basic Authentication, if so return authorization string.
                String auth = req.getHeader("Authorization");
                if (auth != null) {
                    return auth;
                }
            } 
        } catch (Exception e) {
            // ignore
            ZimbraLog.misc.debug("error while extracting authtoken" , e);
        }
        return null;
    }
    
    public void init(FilterConfig filterConfig) {
        waitMs = DEFAULT_WAIT_MS;
        if (filterConfig.getInitParameter(MAX_WAIT_INIT_PARAM)!=null) {
            waitMs=Integer.parseInt(filterConfig.getInitParameter(MAX_WAIT_INIT_PARAM));
        }

        suspendMs = DEFAULT_SUSPEND_MS;
        if (filterConfig.getInitParameter(SUSPEND_INIT_PARAM)!=null) {
            suspendMs=Integer.parseInt(filterConfig.getInitParameter(SUSPEND_INIT_PARAM));
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
    throws IOException, ServletException {
        try {
            Semaphore pass = getPass(request);
            if (pass == null) {
                chain.doFilter(request, response);
                return;
            }
            if (pass.tryAcquire(waitMs, TimeUnit.MILLISECONDS)) {
                try {
                    chain.doFilter(request, response);
                } finally {
                    pass.release();
                }
            } else {
                Continuation continuation = ContinuationSupport.getContinuation(request);
                HttpServletRequest hreq = (HttpServletRequest) request;
                ZimbraServlet.addRemoteIpToLoggingContext(hreq);
                ZimbraServlet.addUAToLoggingContext(hreq);
                ZimbraLog.misc.warn("Exceeded the max requests limit. Suspending " + continuation);
                ZimbraLog.clearContext();
                continuation.setTimeout(suspendMs);
                continuation.suspend();
                return;
            }
        } catch(InterruptedException e) {
            ((HttpServletResponse)response).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
    }

    @Override
    public void destroy() {
    }

}
