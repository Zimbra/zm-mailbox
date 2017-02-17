/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2015, 2016 Synacor, Inc.
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

import org.junit.Ignore;
//import org.eclipse.jetty.util.thread.QueuedThreadPool;

@Ignore("doesn't work due to Java version mismatch in build environment")
public class ContextPathBasedThreadPoolBalancerFilterTest {
    static final int WAIT_MS = 3000; // min time for slow ZimbraServlet.addRemoteIpToLoggingContext to run
/*
    @Test
    public void rulesParser_min() throws Exception {
        String source = "min=5";
        Rules rules = Rules.parse(source);
        Assert.assertEquals((Integer)5, rules.min);
        Assert.assertNull(rules.max);
        Assert.assertNull(rules.maxPercent);
    }

    @Test
    public void rulesParser_minMax() throws Exception {
        String source = "min=5;max=7";
        Rules rules = Rules.parse(source);
        Assert.assertEquals((Integer)5, rules.min);
        Assert.assertEquals((Integer)7, rules.max);
        Assert.assertNull(rules.maxPercent);
    }

    @Test
    public void rulesParser_minMaxPercent() throws Exception {
        String source = "min=5;max=40%";
        Rules rules = Rules.parse(source);
        Assert.assertEquals((Integer)5, rules.min);
        Assert.assertNull(rules.max);
        Assert.assertEquals((Integer)40, rules.maxPercent);
    }

    @Test
    public void rulesParser_singleContextPath() throws Exception {
        String source = "/soap:min=5;max=10%";
        ContextPathBasedThreadPoolBalancerFilter filter = new ContextPathBasedThreadPoolBalancerFilter();
        filter.parse(source);
        Assert.assertEquals(1, filter.rulesByContextPath.size());
        Assert.assertEquals((Integer)5, filter.rulesByContextPath.get("/soap").min);
        Assert.assertEquals((Integer)10, filter.rulesByContextPath.get("/soap").maxPercent);
    }

    @Test
    public void rulesParser_twoContextPaths() throws Exception {
        String source = "/app1:min=5;max=10%, /app2:min=2;max=5%";
        ContextPathBasedThreadPoolBalancerFilter filter = new ContextPathBasedThreadPoolBalancerFilter();
        filter.parse(source);
        Assert.assertEquals(2, filter.rulesByContextPath.size());
        Assert.assertEquals((Integer)5, filter.rulesByContextPath.get("/app1").min);
        Assert.assertEquals((Integer)10, filter.rulesByContextPath.get("/app1").maxPercent);
        Assert.assertEquals((Integer)2, filter.rulesByContextPath.get("/app2").min);
        Assert.assertEquals((Integer)5, filter.rulesByContextPath.get("/app2").maxPercent);
    }

    @Test
    public void successfulInit() throws Exception {
        final String rules = "/app1:max=10, /app2:max=10";
        JettyMonitor.setThreadPool(new QueuedThreadPool(20));

        // Mock up a FilterConfig
        FilterConfig filterConfig = EasyMock.createNiceMock(FilterConfig.class);
        EasyMock.expect(filterConfig.getInitParameter(ContextPathBasedThreadPoolBalancerFilter.RULES_INIT_PARAM)).andReturn(rules);

        // Success test
        EasyMock.replay(filterConfig);
        ContextPathBasedThreadPoolBalancerFilter filter = new ContextPathBasedThreadPoolBalancerFilter();
        filter.init(filterConfig);
        EasyMock.verify(filterConfig);
    }


    @Ignore("Does not run reliably due to non-deterministic Jetty thead pool internals")
    @Test
    public void nosuspend() throws Exception {
        final String rules = "/app1:max=1";
        final int THREAD_POOL_SIZE = 2;

        QueuedThreadPool queuedThreadPool = new QueuedThreadPool(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 100);
        queuedThreadPool.start();
        JettyMonitor.setThreadPool(queuedThreadPool);

        // Mock up a FilterConfig
        FilterConfig filterConfig = EasyMock.createNiceMock(FilterConfig.class);
        EasyMock.expect(filterConfig.getInitParameter(ContextPathBasedThreadPoolBalancerFilter.RULES_INIT_PARAM)).andReturn(rules);

        // Initialize
        EasyMock.replay(filterConfig);
        final ContextPathBasedThreadPoolBalancerFilter filter = new ContextPathBasedThreadPoolBalancerFilter();
        filter.init(filterConfig);
        EasyMock.verify(filterConfig);

        // Setup 1st request for /app1. Use a chain that hangs. Expect chain to be invoked.
        final HttpServletRequest req1 = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(req1.getRequestURI()).andReturn("/app1/whatever").times(1, Integer.MAX_VALUE);
        final HttpServletResponse res1 = EasyMock.createMock(HttpServletResponse.class);
        final FilterChain chain1 = new BlockingFilterChain();
        EasyMock.expect(res1.isCommitted()).andReturn(false).times(1);
        EasyMock.replay(req1, res1);

        // Setup 2nd request for /app2. Use a chain that hangs. Expect chain to be invoked.
        final HttpServletRequest req2 = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(req2.getRequestURI()).andReturn("/app2/whatever").times(1, Integer.MAX_VALUE);
        final HttpServletResponse res2 = EasyMock.createMock(HttpServletResponse.class);
        final FilterChain chain2 = new BlockingFilterChain();
        EasyMock.expect(res2.isCommitted()).andReturn(false).times(1);
        EasyMock.replay(req2, res2);

        // Run requests

        FutureTask<Exception> futureTask1 = new FutureTask<Exception>(new Callable<Exception>() {
           @Override
           public Exception call() throws Exception {
               try {
                   filter.doFilter(req1, res1, chain1);
                   return null;
               } catch (Exception e) {
                   return e;
               }
           }
        });
        queuedThreadPool.execute(futureTask1);

        FutureTask<Exception> futureTask2 = new FutureTask<Exception>(new Callable<Exception>() {
            @Override
            public Exception call() throws Exception {
                try {
                    filter.doFilter(req2, res2, chain2);
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }
        });
        queuedThreadPool.execute(futureTask2);

        // Wait and verify
        Thread.sleep(WAIT_MS);
        EasyMock.verify(req1, res1);
        EasyMock.verify(req2, res2);
    }

    @Ignore("Does not run reliably due to non-deterministic Jetty thead pool internals")
    @Test
    public void suspendToEnforceMax() throws Exception {
        final String rules = "/app1:max=1";
        final int THREAD_POOL_SIZE = 2;

        QueuedThreadPool queuedThreadPool = new QueuedThreadPool(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 100);
        queuedThreadPool.start();
        JettyMonitor.setThreadPool(queuedThreadPool);

        Assert.assertEquals(THREAD_POOL_SIZE, queuedThreadPool.getThreads());

        // Mock up a FilterConfig
        FilterConfig filterConfig = EasyMock.createNiceMock(FilterConfig.class);
        EasyMock.expect(filterConfig.getInitParameter(ContextPathBasedThreadPoolBalancerFilter.RULES_INIT_PARAM)).andReturn(rules);

        // Initialize
        EasyMock.replay(filterConfig);
        final ContextPathBasedThreadPoolBalancerFilter filter = new ContextPathBasedThreadPoolBalancerFilter();
        filter.init(filterConfig);
        EasyMock.verify(filterConfig);

        // Setup 1st request for /app1 and expect chain to run and invoke res1.isCommitted()
        final HttpServletRequest req1 = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(req1.getRequestURI()).andReturn("/app1/whatever").times(1, Integer.MAX_VALUE);
        final HttpServletResponse res1 = EasyMock.createMock(HttpServletResponse.class);
        EasyMock.expect(res1.isCommitted()).andReturn(false).times(1);
        final BlockingFilterChain chain1 = new BlockingFilterChain();

        // Setup 2nd request for /app1 and expect suspend, and chain to not be invoked
        final AsyncContext asyncContext2 = EasyMock.createNiceMock(AsyncContext.class);
        final HttpServletRequest req2 = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(req2.getRequestURI()).andReturn("/app1/whatever").times(1, Integer.MAX_VALUE);
        final HttpServletResponse res2 = EasyMock.createMock(HttpServletResponse.class);
        final BlockingFilterChain chain2 = new BlockingFilterChain();

        // Setup 3rd request for /app2 and expect chain to run and invoke res3.isCommitted()
        final HttpServletRequest req3 = EasyMock.createNiceMock(HttpServletRequest.class);
        EasyMock.expect(req3.getRequestURI()).andReturn("/app2/whatever").times(1, Integer.MAX_VALUE);
        final HttpServletResponse res3 = EasyMock.createMock(HttpServletResponse.class);
        EasyMock.expect(res3.isCommitted()).andReturn(false).times(1);
        final BlockingFilterChain chain3 = new BlockingFilterChain();

        // Run requests
        EasyMock.replay(req1, res1, req2, res2, req3, res3);

        FutureTask<Exception> futureTask1 = new FutureTask<Exception>(new Callable<Exception>() {
           @Override
           public Exception call() throws Exception {
               try {
                   filter.doFilter(req1, res1, chain1);
                   return null;
               } catch (Exception e) {
                   return e;
               }
           }
        });
        queuedThreadPool.execute(futureTask1);

        FutureTask<Exception> futureTask2 = new FutureTask<Exception>(new Callable<Exception>() {
            @Override
            public Exception call() throws Exception {
                try {
                    filter.doFilter(req2, res2, chain2);
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }
        });
        queuedThreadPool.execute(futureTask2);

        FutureTask<Exception> futureTask3 = new FutureTask<Exception>(new Callable<Exception>() {
            @Override
            public Exception call() throws Exception {
                try {
                    filter.doFilter(req3, res3, chain3);
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }
        });
        queuedThreadPool.execute(futureTask3);

        // Wait for threads to block on semaphore
        Thread.sleep(WAIT_MS);

        // Verify
        Assert.assertFalse(futureTask1.isDone());
        Assert.assertTrue(futureTask2.isDone());
        Assert.assertFalse(futureTask3.isDone());
        EasyMock.verify(req1, res1);
        EasyMock.verify(req2, res2);
        EasyMock.verify(req3, res3);

        // Unblock threads to cleanup
        EasyMock.resetToStrict(res1, res2, res3);
        chain1.unblock();
        chain2.unblock();
        chain3.unblock();
        Thread.sleep(WAIT_MS);
        Assert.assertTrue(futureTask1.isDone());
        Assert.assertTrue(futureTask2.isDone());
        Assert.assertTrue(futureTask3.isDone());
        if (futureTask1.get() != null) {throw futureTask1.get();}
        if (futureTask3.get() != null) {throw futureTask3.get();}
    }


    class BlockingFilterChain implements FilterChain {
        private Semaphore semaphore = new Semaphore(1);

        public BlockingFilterChain() {
            semaphore.drainPermits();
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            response.isCommitted();
            semaphore.acquireUninterruptibly();
            try {
                response.flushBuffer();
            } finally {
                semaphore.release();
            }
        }

        public void unblock() {
            semaphore.release();
        }
    }
    */
}
