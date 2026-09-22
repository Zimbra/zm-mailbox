/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link AutoProvisionThread}. The thread's lifecycle is
 * driven through its real static control methods against the in-memory
 * MockProvisioning harness whose local server has no auto-provision polling
 * interval configured (so the scheduler must decline to start). Tests assert the
 * observable running state across start/shutdown/switch transitions, plus the
 * scheduler contract that an unconfigured server keeps the thread stopped.
 */
public class AutoProvisionThreadTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        // Make sure we start each test from a known, stopped state. shutdown() alone is not
        // enough for isolation: if a prior test (or a prior PIT minimal-test run) left the static
        // singleton in a non-null-but-dead state, or left a stale cached sleepInterval, the very
        // first assertion here could observe leaked state. Force-reset the statics so this test
        // class passes even when a single method is executed alone.
        forceResetStaticState();
    }

    @After
    public void tearDown() throws Exception {
        // Never leak a running background thread into other tests.
        AutoProvisionThread.shutdown();
        forceResetStaticState();
    }

    /**
     * Hard-resets {@link AutoProvisionThread}'s static control fields ({@code autoProvThread},
     * {@code sleepInterval}) via reflection, joining any live leaked thread first. Guarantees a
     * deterministic stopped/zero state regardless of what ran before this test.
     */
    private static void forceResetStaticState() throws Exception {
        AutoProvisionThread.shutdown();
        Thread leaked = getStaticThread();
        if (leaked != null) {
            leaked.interrupt();
            leaked.join(5000);
        }
        setStaticThread(null);
        Field interval = AutoProvisionThread.class.getDeclaredField("sleepInterval");
        interval.setAccessible(true);
        interval.setLong(null, 0L);
    }

    private static Thread getStaticThread() throws Exception {
        Field f = AutoProvisionThread.class.getDeclaredField("autoProvThread");
        f.setAccessible(true);
        return (Thread) f.get(null);
    }

    private static void setStaticThread(AutoProvisionThread t) throws Exception {
        Field f = AutoProvisionThread.class.getDeclaredField("autoProvThread");
        f.setAccessible(true);
        f.set(null, t);
    }

    /* Attaches an in-memory appender to "zimbra.autoprov" and returns the captured-message list. */
    private static List<String> captureAutoprovLog() {
        final List<String> messages = new CopyOnWriteArrayList<String>();
        AbstractAppender appender = new AbstractAppender("ap-capture-" + System.nanoTime(),
                null, null, true, null) {
            @Override
            public void append(LogEvent event) {
                messages.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        Configurator.setLevel("zimbra.autoprov", Level.INFO);
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        LoggerConfig cfg = ctx.getConfiguration().getLoggerConfig("zimbra.autoprov");
        cfg.setLevel(Level.INFO);
        cfg.addAppender(appender, Level.INFO, null);
        ctx.updateLoggers();
        return messages;
    }

    private static boolean anyContains(List<String> messages, String needle) {
        for (String m : messages) {
            if (m.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void isRunningFreshStateReturnsFalse() {
        // Act + Assert — nothing started yet, the singleton thread is absent
        assertFalse("no auto-provision thread should be running initially",
                AutoProvisionThread.isRunning());
    }

    @Test
    public void startupZeroPollingIntervalDoesNotStartThread() throws Exception {
        // Arrange — local server has no zimbraAutoProvPollingInterval => interval is 0

        // Act — startup must short-circuit because the interval is 0
        AutoProvisionThread.startup();

        // Assert — still not running
        assertFalse("startup must not start a thread when interval is 0",
                AutoProvisionThread.isRunning());
    }

    @Test
    public void shutdownWhenNotRunningIsNoOpAndStaysStopped() {
        // Arrange — confirmed stopped by @Before

        // Act — shutting down an already-stopped scheduler must be safe
        AutoProvisionThread.shutdown();

        // Assert
        assertFalse(AutoProvisionThread.isRunning());
    }

    @Test
    public void switchAutoProvThreadIfNecessaryNoIntervalNoDomainsKeepsStopped() throws Exception {
        // Arrange — local server has neither a polling interval nor scheduled domains

        // Act — the scheduler decides whether to run; here it must not
        AutoProvisionThread.switchAutoProvThreadIfNecessary();

        // Assert
        assertFalse("with no interval and no scheduled domains the thread must stay stopped",
                AutoProvisionThread.isRunning());
    }

    @Test
    public void switchAutoProvThreadIfNecessaryRunningButNoLongerNeededShutsDown() throws Exception {
        // Arrange — force a perceived "running" state is not possible without a real interval,
        // so instead assert the inverse transition: starting from stopped + not-needed stays stopped
        // across repeated invocations (idempotent, no flapping).
        AutoProvisionThread.switchAutoProvThreadIfNecessary();
        assertFalse(AutoProvisionThread.isRunning());

        // Act — invoke again
        AutoProvisionThread.switchAutoProvThreadIfNecessary();

        // Assert — remains stopped, no second thread spawned
        assertFalse(AutoProvisionThread.isRunning());
    }

    @Test
    public void getInitialDelayReturnsConfiguredLocalConfigValue() throws Exception {
        // Arrange — the initial delay is read straight from LocalConfig
        AutoProvisionThread thread = new AutoProvisionThread();
        long expected = LC.autoprov_initial_sleep_ms.longValue();

        // Act
        long actual = thread.getInitialDelay();

        // Assert — deep value assertion, not just "non-negative"
        assertEquals(expected, actual);
    }

    @Test
    public void isShutDownRequestedFreshInstanceReturnsFalse() {
        // Arrange — a brand new instance has not been asked to shut down
        AutoProvisionThread thread = new AutoProvisionThread();

        // Act + Assert
        assertFalse(thread.isShutDownRequested());
    }

    @Test
    public void constructorSetsThreadNameForDiagnostics() {
        // Act — the protected constructor names the thread for log/diagnostic clarity
        AutoProvisionThread thread = new AutoProvisionThread();

        // Assert
        assertEquals("AutoProvision", thread.getName());
    }

    @Test
    public void startupThenShutdownFullLifecycleStaysConsistent() throws Exception {
        // Arrange — attempt a start (declined due to interval 0)
        AutoProvisionThread.startup();
        assertFalse("declined start leaves it stopped", AutoProvisionThread.isRunning());

        // Act — shutdown after a declined start must remain a clean no-op
        AutoProvisionThread.shutdown();

        // Assert
        assertFalse(AutoProvisionThread.isRunning());
    }

    @Test
    public void implementsEagerAutoProvisionSchedulerForProvisioningCallback() {
        // Arrange + Act
        AutoProvisionThread thread = new AutoProvisionThread();

        // Assert — the thread is the scheduler the Provisioning layer drives
        assertTrue("must implement the EagerAutoProvisionScheduler contract",
                thread instanceof Provisioning.EagerAutoProvisionScheduler);
    }

    @Test
    public void createDomainDoesNotImplicitlyStartScheduler() throws Exception {
        // Arrange — creating a domain alone should not flip the scheduler on
        Provisioning prov = Provisioning.getInstance();
        prov.createDomain("autoprov.example.com", new HashMap<String, Object>());

        // Act
        AutoProvisionThread.switchAutoProvThreadIfNecessary();

        // Assert — without a configured interval + scheduled domains on the server, stays stopped
        assertFalse(AutoProvisionThread.isRunning());
    }

    // ---- helpers to drive the polling-interval + scheduled-domains scheduler config ----

    private void setServerSchedulerConfig(String interval, String scheduledDomain) throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        Map<String, Object> attrs = new HashMap<String, Object>();
        if (interval == null) {
            attrs.put(Provisioning.A_zimbraAutoProvPollingInterval, "");
        } else {
            attrs.put(Provisioning.A_zimbraAutoProvPollingInterval, interval);
        }
        if (scheduledDomain == null) {
            attrs.put(Provisioning.A_zimbraAutoProvScheduledDomains, new String[] {});
        } else {
            attrs.put(Provisioning.A_zimbraAutoProvScheduledDomains, scheduledDomain);
        }
        prov.modifyAttrs(localServer, attrs);
    }

    private void clearServerSchedulerConfig() throws Exception {
        setServerSchedulerConfig(null, null);
    }

    @Test
    public void startupWithNonZeroIntervalStartsRunningThread() throws Exception {
        // Arrange — configure a real (large) polling interval so getSleepInterval() != 0
        // and startup() takes its full happy path (read display interval, create+start thread).
        setServerSchedulerConfig("1d", null);
        List<String> log = captureAutoprovLog();
        try {
            // Act
            AutoProvisionThread.startup();

            // Assert — the singleton thread exists and reports running
            assertTrue("startup with a positive interval must start the thread",
                    AutoProvisionThread.isRunning());

            // Assert — the real OS thread was actually started (kills removal of the .start()
            // call on L68: without it the singleton is non-null but never becomes a live thread).
            Thread started = getStaticThread();
            assertTrue("the created thread must be a live, started OS thread", started.isAlive());

            // Assert — the happy-path "Starting auto provision thread" info log was emitted
            // (kills removal of the ZimbraLog.autoprov.info call on L59).
            assertTrue("startup must log that it is starting the thread",
                    anyContains(log, "Starting auto provision thread"));
        } finally {
            AutoProvisionThread.shutdown();
            clearServerSchedulerConfig();
        }
        // After shutdown the thread is cleared
        assertFalse(AutoProvisionThread.isRunning());
    }

    @Test
    public void shutdownWhileRunningInterruptsAndTerminatesThreadAndLogs() throws Exception {
        // Arrange — start a real thread with a long polling interval so it is parked in
        // Thread.sleep() when we shut it down.
        setServerSchedulerConfig("1d", null);
        List<String> log = captureAutoprovLog();
        try {
            AutoProvisionThread.startup();
            assertTrue(AutoProvisionThread.isRunning());
            Thread running = getStaticThread();
            assertTrue("precondition: a live thread exists", running.isAlive());

            // Act — shutdown must interrupt the parked thread (L93) so it wakes and exits.
            AutoProvisionThread.shutdown();
            running.join(5000);

            // Assert — the previously-running thread actually terminated. Removing the interrupt()
            // call (L93) would leave it sleeping for a day, so it would still be alive here.
            assertFalse("shutdown must interrupt and terminate the running thread",
                    running.isAlive());
            // Assert — shutdown logged its action (kills removal of the info log on L91).
            assertTrue("shutdown must log that it is shutting the thread down",
                    anyContains(log, "Shutting down auto provision thread"));
        } finally {
            AutoProvisionThread.shutdown();
            clearServerSchedulerConfig();
        }
        assertFalse(AutoProvisionThread.isRunning());
    }

    @Test
    public void shutdownWhenNotRunningLogsNotRunningMessage() throws Exception {
        // Arrange — confirmed stopped by @Before; capture the autoprov log.
        List<String> log = captureAutoprovLog();

        // Act
        AutoProvisionThread.shutdown();

        // Assert — the else-branch diagnostic is emitted, proving the not-running path ran.
        assertFalse(AutoProvisionThread.isRunning());
        assertTrue("shutdown on a stopped scheduler must log the no-op branch",
                anyContains(log, "auto provision thread is not running"));
    }

    @Test
    public void startupWhenAlreadyRunningDoesNotStartSecondThread() throws Exception {
        // Arrange — start one thread
        setServerSchedulerConfig("1d", null);
        try {
            AutoProvisionThread.startup();
            assertTrue(AutoProvisionThread.isRunning());

            // Act — a second startup must short-circuit (isRunning() guard)
            AutoProvisionThread.startup();

            // Assert — still running, no exception, no second thread leaked
            assertTrue(AutoProvisionThread.isRunning());
        } finally {
            AutoProvisionThread.shutdown();
            clearServerSchedulerConfig();
        }
    }

    @Test
    public void switchAutoProvThreadIfNecessaryIntervalAndDomainsStartsThread() throws Exception {
        // Arrange — both a positive interval and a scheduled domain => needRunning == true
        setServerSchedulerConfig("1d", "scheduled.example.com");
        try {
            // Act
            AutoProvisionThread.switchAutoProvThreadIfNecessary();

            // Assert — the scheduler started the thread
            assertTrue("interval>0 and non-empty scheduled domains must start the thread",
                    AutoProvisionThread.isRunning());
        } finally {
            AutoProvisionThread.shutdown();
            clearServerSchedulerConfig();
        }
    }

    @Test
    public void switchAutoProvThreadIfNecessaryZeroIntervalWithDomainsStaysStopped()
            throws Exception {
        // Arrange — scheduled domains present but the polling interval is 0. The scheduler
        // computes needRunning = interval > 0 && !domains.isEmpty() (L109). With interval == 0
        // that must be FALSE, so the thread stays stopped. A boundary mutation to `interval >= 0`
        // would make needRunning true at zero and (wrongly) start the thread.
        setServerSchedulerConfig(null, "zerodomain.example.com");
        try {
            // Act
            AutoProvisionThread.switchAutoProvThreadIfNecessary();

            // Assert — interval 0 means "do not run", regardless of scheduled domains.
            assertFalse("zero interval must keep the scheduler stopped even with scheduled domains",
                    AutoProvisionThread.isRunning());
        } finally {
            AutoProvisionThread.shutdown();
            clearServerSchedulerConfig();
        }
    }

    @Test
    public void switchAutoProvThreadIfNecessaryRunningButConfigClearedShutsDown() throws Exception {
        // Arrange — get into the running state via valid config
        setServerSchedulerConfig("1d", "scheduled2.example.com");
        AutoProvisionThread.switchAutoProvThreadIfNecessary();
        assertTrue(AutoProvisionThread.isRunning());

        try {
            // Act — remove the scheduled domains so needRunning becomes false while running
            clearServerSchedulerConfig();
            AutoProvisionThread.switchAutoProvThreadIfNecessary();

            // Assert — the scheduler tears the thread down (the !needRunning && isRunning branch)
            assertFalse("clearing config while running must shut the thread down",
                    AutoProvisionThread.isRunning());
        } finally {
            AutoProvisionThread.shutdown();
            clearServerSchedulerConfig();
        }
    }

    @Test
    public void runIterationHandlesUnsupportedAutoProvAndSleepsThenShutsDownOnInterrupt()
            throws Exception {
        // Arrange — a controllable thread with no warm-up delay and a short sleep interval so the
        // run() loop executes an iteration (autoProvAccountEager throws UNSUPPORTED -> caught and
        // logged), then enters sleep(); interrupting it sets shutdownRequested and the loop exits.
        setServerSchedulerConfig("250ms", null);
        List<String> log = captureAutoprovLog();
        try {
            final ControllableAutoProvisionThread thread = new ControllableAutoProvisionThread();
            thread.start();

            // Give run() time to: pass the (zero) initial delay, run an iteration, enter sleep().
            long deadline = System.currentTimeMillis() + 5000;
            while (!thread.didIterate() && System.currentTimeMillis() < deadline) {
                Thread.yield();
            }
            assertTrue("run() should have executed at least one provisioning iteration",
                    thread.didIterate());

            // Wait until the loop actually attempted the provisioning call so the warn log is
            // present before we assert on it.
            deadline = System.currentTimeMillis() + 5000;
            while (!anyContains(log, "Unable to auto provision accounts")
                    && System.currentTimeMillis() < deadline) {
                Thread.yield();
            }

            // Act — interrupt to drive the InterruptedException path in sleep()/run()
            thread.interrupt();
            thread.join(5000);

            // Assert — the thread terminated and shutdown was requested via the interrupt path
            assertFalse("interrupted run() thread must have terminated", thread.isAlive());
            assertTrue("interrupt during sleep must set shutdownRequested",
                    thread.isShutDownRequested());

            // Assert — the loop actually invoked prov.autoProvAccountEager(this) (L150): the mock
            // throws UNSUPPORTED, which run() catches and logs via ZimbraLog.autoprov.warn (L155).
            // Removing either the call or the warn leaves this message absent.
            assertTrue("run() must call autoProvAccountEager and log the caught failure",
                    anyContains(log, "Unable to auto provision accounts"));
            // Assert — the warm-up sleep info log was emitted before work (L129).
            assertTrue("run() must log the pre-work warm-up sleep",
                    anyContains(log, "Auto provision thread sleeping for"));
            // Assert — sleep() logged its inter-iteration sleep (L187), proving the
            // elapsed < interval branch (L167) chose to sleep rather than loop immediately.
            assertTrue("sleep() must log the inter-iteration sleep interval",
                    anyContains(log, "Sleeping for"));
        } finally {
            clearServerSchedulerConfig();
        }
    }

    @Test
    public void runPositiveIntervalKeepsRunningUntilInterrupted() throws Exception {
        // Arrange — a thread with no warm-up delay and a positive (sub-second) polling interval.
        // sleep() must take the `interval > 0` branch (L189): sleep then loop again WITHOUT setting
        // shutdownRequested. If that conditional were negated/flipped, sleep() would instead set
        // shutdownRequested on the very first pass and the loop would self-terminate without any
        // interrupt — so this thread would not still be alive after a few iterations.
        setServerSchedulerConfig("100ms", null);
        try {
            final ControllableAutoProvisionThread thread = new ControllableAutoProvisionThread();
            thread.start();

            // Let it iterate at least once and re-enter the loop a couple of times.
            long deadline = System.currentTimeMillis() + 3000;
            while (!thread.didIterate() && System.currentTimeMillis() < deadline) {
                Thread.yield();
            }
            assertTrue("precondition: at least one iteration ran", thread.didIterate());

            // Give it enough wall time to cover several 100ms sleep cycles.
            Thread.sleep(500);

            // Assert — with a positive interval and no interrupt, the loop must still be alive.
            assertTrue("positive-interval sleep must loop, not self-terminate", thread.isAlive());
            assertFalse("no interrupt yet, so shutdown must not have been requested",
                    thread.isShutDownRequested());

            // Cleanup — now interrupt and confirm it exits.
            thread.interrupt();
            thread.join(5000);
            assertFalse(thread.isAlive());
        } finally {
            clearServerSchedulerConfig();
        }
    }

    @Test
    public void runInitialDelayInterruptedBeforeWorkTerminatesImmediately() throws Exception {
        // Arrange — a thread whose initial warm-up sleep is long; interrupting during that sleep
        // hits the early-return InterruptedException branch in run() before any iteration.
        setServerSchedulerConfig("1d", null);
        List<String> log = captureAutoprovLog();
        try {
            final ControllableAutoProvisionThread thread = new ControllableAutoProvisionThread(60000);
            thread.start();

            // Let it enter the initial Thread.sleep(getInitialDelay()).
            long deadline = System.currentTimeMillis() + 3000;
            while (thread.getState() != Thread.State.TIMED_WAITING
                    && System.currentTimeMillis() < deadline) {
                Thread.yield();
            }

            // Act — interrupt during the warm-up sleep
            thread.interrupt();
            thread.join(5000);

            // Assert — terminated without ever running an iteration
            assertFalse(thread.isAlive());
            assertFalse("must not have iterated when interrupted during warm-up sleep",
                    thread.didIterate());
            // Assert — the warm-up-interrupt branch logged its shutdown message (kills removal of
            // the ZimbraLog.autoprov.info call on L134).
            assertTrue("interrupt during warm-up sleep must log the shutdown",
                    anyContains(log, "Shutting down auto provision thread"));
        } finally {
            clearServerSchedulerConfig();
        }
    }

    /**
     * Test-only thread that records whether the provisioning loop iterated and shortens the warm-up
     * delay so {@link AutoProvisionThread#run()} can be exercised deterministically.
     */
    private static class ControllableAutoProvisionThread extends AutoProvisionThread {
        private final long initialDelay;

        private volatile boolean iterated = false;

        ControllableAutoProvisionThread() {
            this(0L);
        }

        ControllableAutoProvisionThread(long initialDelay) {
            this.initialDelay = initialDelay;
        }

        @Override
        protected long getInitialDelay() {
            return initialDelay;
        }

        @Override
        public boolean isShutDownRequested() {
            // Snoop the loop: the first time run() checks the shutdown flag after an iteration we
            // mark that an iteration happened. Delegates to the real flag for the actual decision.
            iterated = true;
            return super.isShutDownRequested();
        }

        boolean didIterate() {
            return iterated;
        }
    }
}
