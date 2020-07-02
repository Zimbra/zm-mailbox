/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import com.zimbra.common.jetty.JettyMonitor;
import com.zimbra.common.util.ZimbraLog;

/**
 * This Servlet {@link Filter} prevents requests for any given context path like
 * /service, /soap, /zimbra, or /zimbraAdmin from monopolizing all threads in the current pool.
 */
public class ContextPathBasedThreadPoolBalancerFilter implements Filter {
    final static String RULES_INIT_PARAM = "Rules";
    final static String SUSPEND_INIT_PARAM = "suspendMs";
    final static long DEFAULT_SUSPEND_MS = 1000;
    ConcurrentHashMap<String, AtomicInteger> activeRequestsByContextPath = new ConcurrentHashMap<String, AtomicInteger>();
    ConcurrentHashMap<String, Rules> rulesByContextPath = new ConcurrentHashMap<String, Rules>();
    QueuedThreadPool queuedThreadPool = null;
    long suspendMs = 1000;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        suspendMs = DEFAULT_SUSPEND_MS;
        String str = StringUtils.trimToNull(filterConfig.getInitParameter(SUSPEND_INIT_PARAM));
        if (str!=null) {
            suspendMs=Integer.parseInt(str);
        }

        str = StringUtils.trimToNull(filterConfig.getInitParameter(RULES_INIT_PARAM));
        parse(str);
        ZimbraLog.misc.info("Initialized with %s", str);

        ThreadPool threadPool = JettyMonitor.getThreadPool();
        if (threadPool instanceof QueuedThreadPool) {
            queuedThreadPool = (QueuedThreadPool)threadPool;
            ZimbraLog.misc.info("Thread pool was configured to max=" + queuedThreadPool.getMaxThreads());
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {

        // Determine whether to allow or suspend request
        boolean suspend = shouldSuspend(request);

        // Suspend request
        if (suspend) {
            Continuation continuation = ContinuationSupport.getContinuation(request);
            HttpServletRequest hreq = (HttpServletRequest) request;
            ZimbraServlet.addRemoteIpToLoggingContext(hreq);
            ZimbraServlet.addUAToLoggingContext(hreq);
            ZimbraLog.clearContext();
            continuation.setTimeout(suspendMs);
            continuation.suspend();
            return;
        }

        // Allow request
        String contextPath = getContextPath(request);
        try {
            // Start tracking request
            AtomicInteger i = activeRequestsByContextPath.get(contextPath);
            if (i == null) {
                i = new AtomicInteger(1);
                activeRequestsByContextPath.put(contextPath, i);
            } else {
                i.incrementAndGet();
            }
//            ZimbraLog.misc.debug("%s concurrency=%d", contextPath, i.get());

            // Perform default operation
            chain.doFilter(request,response);

        } finally {
            // Stop tracking request
            AtomicInteger i = activeRequestsByContextPath.get(contextPath);
            i.decrementAndGet();
//            ZimbraLog.misc.debug("%s concurrency=%d", contextPath, i.get());
        }
    }

    /** Returns the context path */
    protected String getContextPath(ServletRequest request) {
        String uri = ((HttpServletRequest)request).getRequestURI();
        return getContextPath(uri);
    }

    /** Returns the context path */
    protected String getContextPath(String uri) {
        for (String contextPath: rulesByContextPath.keySet()) {
            if (uri.startsWith(contextPath)) {
                return contextPath;
            }
        }
        return ""; // avoid null, which is unsupported as a key in the concurrent map
    }

    protected String getLoggableContextPath(String contextPath) {
        if (contextPath == null || contextPath.isEmpty()) {
            return "<blank>";
        } else {
            return contextPath;
        }
    }

    /** Determine whether to suspend request based on state of current thread pool */
    protected boolean shouldSuspend(ServletRequest request) {

        // Disable this servlet filter if the current thread pool is not known (we're probably not running in Jetty)
        if (queuedThreadPool == null) {
            return false;
        }

        // Determine whether request is for one of the context paths this filter is managing QoS for
        String contextPath = getContextPath(request);

        // Enforce maximums
        Rules rules = rulesByContextPath.get(contextPath);
        if (rules != null) {

            // Determine the state of the thread pool
            int threads = queuedThreadPool.getThreads();
            int idleThreads = queuedThreadPool.getIdleThreads();
            int busyThreads = threads - idleThreads;
            int roomInPoolForNewThreads = queuedThreadPool.getMaxThreads() - busyThreads;
            AtomicInteger count = activeRequestsByContextPath.get(contextPath);
            int activeRequestsCount = count == null ? 0 : count.intValue();
            ZimbraLog.misc.debug("Servlet (contextPath=%s active=%d), Jetty pool (threads=%d, idle=%d, busy=%d, room=%d)", getLoggableContextPath(contextPath), activeRequestsCount, threads, idleThreads, busyThreads, roomInPoolForNewThreads);

            if (activeRequestsCount > busyThreads && roomInPoolForNewThreads > 0) {
                ZimbraLog.misc.warn("ContextPathBasedThreadPoolBalancerFilter: activeRequestsCount: %d > busyThreads: %d\n" +
                                "Setting activeRequestsCount == %d \n(contextPath=%s active=%d), " +
                                "Jetty pool (threads=%d, idle=%d, busy=%d, room=%d)",
                                activeRequestsCount, busyThreads, busyThreads, getLoggableContextPath(contextPath),
                                activeRequestsCount, threads, idleThreads, busyThreads, roomInPoolForNewThreads);
                activeRequestsCount = busyThreads;
            }

            // Enforce max count
            if (rules.max != null) {
                if (activeRequestsCount > rules.max) {
                    ZimbraLog.misc.info("Suspending for %dms because context path %s is at %d configured max threads", suspendMs, getLoggableContextPath(contextPath), activeRequestsCount);
                    return true;
                }
            }
            // Enforce max %
            if (rules.maxPercent != null) {
                if (100 * activeRequestsCount / queuedThreadPool.getMaxThreads() > rules.maxPercent) {
                    ZimbraLog.misc.info("Suspending for %dms because context path %s is at %d threads (%d configured max percentage of thread pool size)", suspendMs, getLoggableContextPath(contextPath), activeRequestsCount, rules.maxPercent);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void destroy() {
    }


    protected void parse(String input) throws ServletException {
        rulesByContextPath.clear();
        for (String str: new StrTokenizer(input, ",").getTokenArray()) {
            String[] array = str.split(":");
            if (array.length != 2) {
                throw new ServletException("Malformed rules: " + input);
            }
            String key = StringUtils.trimToNull(array[0]);
            String value = StringUtils.trimToNull(array[1]);
            if (key == null || value == null) {
                throw new ServletException("Malformed rules: " + input);
            }
            Rules rules = Rules.parse(value);
            rulesByContextPath.put(key, rules);
        }
    }

    static class Rules {
        public Integer min, max, maxPercent;

        public static Rules parse(String input) throws ServletException {
            Rules rule = new Rules();
            for (String str: new StrTokenizer(input, ";").getTokenArray()) {
                String[] array = str.split("=");
                if (array.length != 2) {
                    throw new ServletException("Malformed rule: " + input);
                }
                String key = StringUtils.trimToNull(array[0]);
                String value = StringUtils.trimToNull(array[1]);
                if (key == null || value == null) {
                    throw new ServletException("Malformed rule: " + input);
                }
                switch (key) {
                case "min":
                    rule.min = new Integer(value);
                    break;
                case "max":
                    if (value.endsWith("%")) {
                        rule.maxPercent = new Integer(value.substring(0, value.length()-1));
                    } else {
                        rule.max = new Integer(value);
                    }
                    break;
                default:
                    throw new ServletException("Unknown key: " + key + " in rule: " + input);
                }
            }
            return rule;
        }
    }
}
