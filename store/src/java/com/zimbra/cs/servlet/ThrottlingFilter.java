/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.RemoteIP;
import com.zimbra.common.util.ZimbraLog;

/**
 * This Servlet {@link Filter} limits the number of concurrent HTTP requests per
 * HTTP session.
 * <p>
 * It returns 503 HTTP error if exceeding the limit. The limit is configurable
 * by {@link LC#servlet_max_concurrent_requests_per_session}. To disable, set
 * the LC key to 0 or remove this servlet filter from web.xml.
 *
 * @author ysasaki
 */
public final class ThrottlingFilter implements Filter {
    private final ConcurrentMap<String, Semaphore> sid2tracker =
        new ConcurrentHashMap<String, Semaphore>();

    @Override
    public void init(FilterConfig config) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest hreq = (HttpServletRequest) req;
        HttpServletResponse hresp = (HttpServletResponse) resp;
        HttpSession session = hreq.getSession(false);
        // always get the latest value from LC
        int max = LC.servlet_max_concurrent_requests_per_session.intValue();
        if (session == null || max <= 0) {
            // don't throttle if no session or disabled
            chain.doFilter(req, resp);
            return;
        }

        Semaphore tracker = sid2tracker.get(session.getId());
        if (tracker == null) {
            tracker = new Semaphore(max);
            Semaphore exist = sid2tracker.putIfAbsent(session.getId(), tracker);
            if (exist == null) { // absent
                session.setAttribute(getClass().getName(),
                        new SessionBindingListener());
            } else {
                tracker = exist;
            }
        }

        if (tracker.tryAcquire()) {
            try {
                chain.doFilter(req, resp);
            } finally {
                tracker.release();
            }
        } else {
            new RemoteIP(hreq, ZimbraServlet.getTrustedIPs()).addToLoggingContext();
            ZimbraLog.addToContext("jsessionid", session.getId());
            ZimbraLog.misc.warn("too many concurrent HTTP requests");
            ZimbraLog.clearContext();
            hresp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "too many concurrent HTTP requests");
        }
    }

    /**
     * Remove the entry from the tracker table upon session invalidation.
     */
    private class SessionBindingListener implements HttpSessionBindingListener {

        @Override
        public void valueBound(HttpSessionBindingEvent event) {
        }

        @Override
        public void valueUnbound(HttpSessionBindingEvent event) {
            sid2tracker.remove(event.getSession().getId());
        }

    }

}
