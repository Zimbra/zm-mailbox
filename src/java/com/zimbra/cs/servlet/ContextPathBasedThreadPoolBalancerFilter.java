/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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

            // Ensure context path minimums don't exceed thread pool max
            int sumOfMins = 0;
            for (Rules rules: rulesByContextPath.values()) {
                if (rules.min != null) {
                    sumOfMins += rules.min;
                }
            }
            if (sumOfMins > queuedThreadPool.getMaxThreads()) {
                throw new ServletException("Sum of minimum thread pool reservations is " + sumOfMins + ", exceeding thread pool max of " + queuedThreadPool.getMaxThreads());
            }
            if (sumOfMins == queuedThreadPool.getMaxThreads()) {
                ZimbraLog.misc.warn("Sum of minimum thread pool reservations matches thread pool size of "
                    + sumOfMins + "; not reserving at least 1 thread for other requests not managed by this filter");
            }

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
            ZimbraLog.misc.warn("Suspending " + continuation);
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
            ZimbraLog.misc.debug("%s concurrency=%d", contextPath, i.get());

            // Perform default operation
            chain.doFilter(request,response);

        } finally {
            // Stop tracking request
            AtomicInteger i = activeRequestsByContextPath.get(contextPath);
            i.decrementAndGet();
            ZimbraLog.misc.debug("%s concurrency=%d", contextPath, i.get());
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

    /** Determine whether to suspend request based on state of current thread pool */
    protected boolean shouldSuspend(ServletRequest request) {

        // Disable this servlet filter if the current thread pool is not known (we're probably not running in Jetty)
        if (queuedThreadPool == null) {
            return false;
        }

        // Determine whether request is for one of the context paths this filter is managing QoS for
        String contextPath = getContextPath(request);

        // Determine the state of the thread pool
        int threads = queuedThreadPool.getThreads();
        int idleThreads = queuedThreadPool.getIdleThreads();
        int busyThreads = threads - idleThreads;
        int roomInPoolForNewThreads = queuedThreadPool.getMaxThreads() - busyThreads;

        // Enforce maximums
        Rules rules = rulesByContextPath.get(contextPath);
        if (rules != null) {
            AtomicInteger count = activeRequestsByContextPath.get(contextPath);
            if (count != null) {
                // Enforce max
                if (rules.max != null) {
                    if (count.get() > rules.max) {
                        return true;
                    }
                }
                // Enforce max %
                if (rules.maxPercent != null) {
                    if (100 * count.get() / queuedThreadPool.getMaxThreads() > rules.maxPercent) {
                        return true;
                    }
                }
            }
        }

        // Enforce minimums
        int reservationsForOtherContextPaths = 0;
        for (String str: rulesByContextPath.keySet()) {
            String contextPath_ = getContextPath(str);
            if (contextPath.equals(contextPath_)) {
                continue;
            }
            rules = rulesByContextPath.get(str);
            if (rules.min == null || rules.min.intValue() < 1) {
                continue;
            }
            AtomicInteger count = activeRequestsByContextPath.get(contextPath_);
            int activeRequestsCount = count == null ? 0 : count.intValue();
            int reservationsNeeded = Math.max(0, rules.min.intValue() - activeRequestsCount);
            reservationsForOtherContextPaths += reservationsNeeded;
        }
        if (reservationsForOtherContextPaths > roomInPoolForNewThreads) { // don't use >=, since current thread is freed by a suspend
            return true;
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
