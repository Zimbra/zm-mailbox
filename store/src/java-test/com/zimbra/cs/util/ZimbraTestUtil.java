package com.zimbra.cs.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.rules.ExternalResource;
import org.mockito.Mockito;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LocalConfig;
import com.zimbra.common.util.Log;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;

/**
 * Reusable test utilities for the three Zimbra static-dependency patterns.
 *
 * <pre>
 * ┌────────────────────────────┬────────────────────────────────────────────┬──────────────────────┐
 * │ Dependency                 │ How production code uses it                │ Strategy             │
 * ├────────────────────────────┼────────────────────────────────────────────┼──────────────────────┤
 * │ LC.some_key                │ LC.some_key.value() / .intValue()          │ Reflection on        │
 * │                            │  → KnownKey → LC.get() → LocalConfig       │ KnownKey.value field │
 * ├────────────────────────────┼────────────────────────────────────────────┼──────────────────────┤
 * │ ZimbraLog.xxx              │ ZimbraLog.mailbox.info("msg", arg)         │ log4j2 programmatic  │
 * │                            │  → public static final Log fields          │ appender – no mock   │
 * ├────────────────────────────┼────────────────────────────────────────────┼──────────────────────┤
 * │ Provisioning.getInstance() │ static singleton; setInstance() is public  │ setInstance() –      │
 * │                            │  → no PowerMock needed                     │ no PowerMock         │
 * └────────────────────────────┴────────────────────────────────────────────┴──────────────────────┘
 * </pre>
 *
 * <h3>When PowerMock IS still needed</h3>
 * <ul>
 *   <li>Mocking {@code final} classes with no test seam at all.</li>
 *   <li>Stubbing JDK statics like {@code System.currentTimeMillis()} or
 *       {@code UUID.randomUUID()} where the call site cannot be injected.</li>
 *   <li>Constructors that execute unreachable LDAP/FS calls in a static block.</li>
 * </ul>
 */
public final class ZimbraTestUtil {

    private ZimbraTestUtil() {}

    // =========================================================================
    // 1. LC — LocalConfig
    //
    // LC.get(key) → LocalConfig.getInstance().get(key)
    //   LocalConfig.get() first checks mExpanded (keys loaded from XML at boot),
    //   then delegates to KnownKey.getValue(key) which returns KnownKey.value.
    //
    // KnownKey.setValue(String) is @VisibleForTesting but package-private
    // (com.zimbra.common.localconfig). We reach it via reflection on the field.
    //
    // We also remove the key from LocalConfig.mExpanded so that
    // LocalConfig.get() falls through to KnownKey and returns our injected value.
    // (Keys present in localconfig-test.xml are in mExpanded; without removal
    // the XML value wins regardless of what setValue stores.)
    // =========================================================================

    private static final Field KNOWN_KEY_VALUE_FIELD;
    private static final Field LOCAL_CONFIG_INSTANCE_FIELD;
    private static final Field LOCAL_CONFIG_EXPANDED_FIELD;

    static {
        try {
            KNOWN_KEY_VALUE_FIELD = KnownKey.class.getDeclaredField("value");
            KNOWN_KEY_VALUE_FIELD.setAccessible(true);

            // LocalConfig.getInstance() is package-private — access the singleton field directly.
            LOCAL_CONFIG_INSTANCE_FIELD = LocalConfig.class.getDeclaredField("mLocalConfig");
            LOCAL_CONFIG_INSTANCE_FIELD.setAccessible(true);

            LOCAL_CONFIG_EXPANDED_FIELD = LocalConfig.class.getDeclaredField("mExpanded");
            LOCAL_CONFIG_EXPANDED_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** Returns the live LocalConfig singleton via reflection. */
    private static LocalConfig localConfigInstance() {
        try {
            return (LocalConfig) LOCAL_CONFIG_INSTANCE_FIELD.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Cannot access LocalConfig singleton", e);
        }
    }

    /**
     * Overrides an LC key for the current test. Affects all reads via
     * {@code LC.some_key.value()}, {@code .intValue()}, {@code .booleanValue()}, etc.
     * Restore with {@link #resetLcKey} or use {@link LcKeyRule} for automatic cleanup.
     *
     * <pre>
     * ZimbraTestUtil.setLcKey(LC.zimbra_tmp_directory, "/tmp/test-zimbra");
     * assertEquals("/tmp/test-zimbra", LC.zimbra_tmp_directory.value());
     * </pre>
     */
    public static void setLcKey(KnownKey key, String value) {
        try {
            // 1. Write directly into KnownKey's cached 'value' field.
            KNOWN_KEY_VALUE_FIELD.set(key, value);

            // 2. Evict from LocalConfig.mExpanded so our value isn't shadowed by the XML.
            LocalConfig lc = localConfigInstance();
            if (lc != null) {
                @SuppressWarnings("unchecked")
                Map<String, String> expanded =
                        (Map<String, String>) LOCAL_CONFIG_EXPANDED_FIELD.get(lc);
                expanded.remove(key.key());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to override LC key: " + key.key(), e);
        }
    }

    /** Convenience overloads for primitive KnownKey types. */
    public static void setLcKey(KnownKey key, boolean value) { setLcKey(key, String.valueOf(value)); }
    public static void setLcKey(KnownKey key, int value)     { setLcKey(key, String.valueOf(value)); }
    public static void setLcKey(KnownKey key, long value)    { setLcKey(key, String.valueOf(value)); }

    /**
     * Resets a key back to {@code null}, forcing KnownKey to re-expand from its
     * declared default on the next read. Call in {@code @After} to prevent pollution.
     *
     * <pre>
     * {@literal @}After
     * public void tearDown() {
     *     ZimbraTestUtil.resetLcKey(LC.zimbra_tmp_directory);
     * }
     * </pre>
     */
    public static void resetLcKey(KnownKey key) {
        try {
            KNOWN_KEY_VALUE_FIELD.set(key, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset LC key: " + key.key(), e);
        }
    }

    /**
     * JUnit 4 {@code @Rule}: sets an LC key before each test, resets it after
     * (even on failure).
     *
     * <pre>
     * {@literal @}Rule
     * public final ZimbraTestUtil.LcKeyRule lcRule =
     *     ZimbraTestUtil.lcKeyRule(LC.zimbra_mailbox_lock_timeout, "5");
     * </pre>
     */
    public static LcKeyRule lcKeyRule(KnownKey key, String value) {
        return new LcKeyRule(key, value);
    }

    public static final class LcKeyRule extends ExternalResource {
        private final KnownKey key;
        private final String   override;

        LcKeyRule(KnownKey key, String value) { this.key = key; this.override = value; }

        @Override protected void before() throws Throwable { setLcKey(key, override); }
        @Override protected void after()                   { resetLcKey(key); }
    }


    // =========================================================================
    // 2. ZimbraLog — log4j2-backed public static final Log fields
    //
    // ZimbraLog.mailbox / .security / .imap etc. are concrete Log objects backed
    // by log4j2 Logger instances. They do NOT need mocking — log calls are benign
    // once log4j2 is on the classpath (MailboxTestUtil.initServer() ensures this).
    //
    // To ASSERT that a specific message was logged, install a capturing appender
    // on the underlying log4j2 logger. Use Log.getCategory() (public) to get the
    // logger name, e.g. ZimbraLog.mailbox.getCategory() == "zimbra.mailbox".
    //
    // AbstractAppender constructor used (log4j-core 2.17.1):
    //   AbstractAppender(String name, Filter filter, Layout<?> layout)
    //   — pass null for filter and layout (both optional).
    // =========================================================================

    /**
     * Installs a capturing appender on a {@link ZimbraLog} logger and returns it.
     * Remove after the test with {@link #removeLogCapture}, or use
     * {@link LogCaptureRule} for automatic cleanup.
     *
     * <pre>
     * LogCapture cap = ZimbraTestUtil.captureLog(ZimbraLog.security, Level.WARN);
     * // ... exercise code that should log a warning ...
     * assertTrue(cap.contains("authentication failed"));
     * ZimbraTestUtil.removeLogCapture(ZimbraLog.security, cap);
     * </pre>
     */
    public static LogCapture captureLog(Log zimbraLog, Level minLevel) {
        // Log.getCategory() returns the underlying log4j2 logger name, e.g. "zimbra.mailbox"
        return installCapture(zimbraLog.getCategory(), minLevel);
    }

    /** Removes a previously installed {@link LogCapture}. */
    public static void removeLogCapture(Log zimbraLog, LogCapture capture) {
        removeCapture(zimbraLog.getCategory(), capture);
    }

    private static LogCapture installCapture(String loggerName, Level minLevel) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration cfg = ctx.getConfiguration();

        LogCapture cap = new LogCapture(loggerName + "-test-capture");
        cap.start();
        cfg.addAppender(cap);

        LoggerConfig loggerCfg = cfg.getLoggerConfig(loggerName);
        if (!loggerCfg.getName().equals(loggerName)) {
            // No dedicated config entry — create one so our appender has a home.
            loggerCfg = new LoggerConfig(loggerName, minLevel, true);
            cfg.addLogger(loggerName, loggerCfg);
        }
        loggerCfg.addAppender(cap, minLevel, null);
        ctx.updateLoggers();
        return cap;
    }

    private static void removeCapture(String loggerName, LogCapture capture) {
        LoggerContext ctx      = (LoggerContext) LogManager.getContext(false);
        Configuration cfg      = ctx.getConfiguration();
        LoggerConfig  loggerCfg = cfg.getLoggerConfig(loggerName);
        loggerCfg.removeAppender(capture.getName());
        capture.stop();
        ctx.updateLoggers();
    }

    /**
     * An in-memory log4j2 appender that accumulates {@link LogEvent}s for assertion.
     */
    @SuppressWarnings("deprecation") // AbstractAppender(name, filter, layout) is stable in 2.17.x
    public static final class LogCapture extends AbstractAppender {

        private final List<LogEvent> events = Collections.synchronizedList(new ArrayList<LogEvent>());

        LogCapture(String name) {
            super(name, null, null);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }

        /** Returns a snapshot of all captured events. */
        public List<LogEvent> getEvents() {
            return new ArrayList<LogEvent>(events);
        }

        /**
         * Returns {@code true} if any captured message contains {@code fragment}
         * (case-insensitive substring match).
         */
        public boolean contains(String fragment) {
            String lower = fragment.toLowerCase();
            for (LogEvent e : events) {
                if (e.getMessage().getFormattedMessage().toLowerCase().contains(lower)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Returns {@code true} if any event at exactly {@code level} contains
         * {@code fragment}.
         */
        public boolean contains(Level level, String fragment) {
            String lower = fragment.toLowerCase();
            for (LogEvent e : events) {
                if (e.getLevel().equals(level)
                        && e.getMessage().getFormattedMessage().toLowerCase().contains(lower)) {
                    return true;
                }
            }
            return false;
        }

        public void clear() { events.clear(); }
    }

    /**
     * JUnit 4 {@code @Rule} that installs/removes a {@link LogCapture} around
     * each test automatically.
     *
     * <pre>
     * {@literal @}Rule
     * public final ZimbraTestUtil.LogCaptureRule logRule =
     *     ZimbraTestUtil.logCaptureRule(ZimbraLog.security, Level.WARN);
     *
     * {@literal @}Test
     * public void logsWarningOnBadAuth() {
     *     // exercise code...
     *     assertTrue(logRule.getCapture().contains(Level.WARN, "authentication failed"));
     * }
     * </pre>
     */
    public static LogCaptureRule logCaptureRule(Log zimbraLog, Level minLevel) {
        return new LogCaptureRule(zimbraLog, minLevel);
    }

    public static final class LogCaptureRule extends ExternalResource {
        private final Log   log;
        private final Level minLevel;
        private LogCapture  capture;

        LogCaptureRule(Log log, Level minLevel) { this.log = log; this.minLevel = minLevel; }

        @Override protected void before() throws Throwable { capture = captureLog(log, minLevel); }
        @Override protected void after() { if (capture != null) removeLogCapture(log, capture); }

        public LogCapture getCapture() { return capture; }
    }


    // =========================================================================
    // 3. Provisioning.getInstance() — public setInstance() seam
    //
    // Provisioning.setInstance(Provisioning) is public — no PowerMock needed.
    //
    // Strategy A: MockProvisioning (recommended)
    //   Full in-memory implementation: createAccount / createDomain / getAccountByName.
    //   Already installed by MailboxTestUtil.initServer() / initProvisioning().
    //
    // Strategy B: Mockito mock
    //   Stub only the calls you need; lighter than a full MockProvisioning.
    //   Always call restoreProvisioning() in @After.
    // =========================================================================

    /**
     * Installs a fresh {@link MockProvisioning} singleton and returns it.
     * Use when NOT calling {@code MailboxTestUtil.initServer()} (avoids DB/index stack).
     *
     * <pre>
     * MockProvisioning prov = ZimbraTestUtil.installMockProvisioning();
     * prov.createAccount("user@test.com", "secret", attrs);
     * // Provisioning.getInstance() now returns this in-memory implementation.
     * </pre>
     */
    public static MockProvisioning installMockProvisioning() {
        MockProvisioning mock = new MockProvisioning();
        Provisioning.setInstance(mock);
        return mock;
    }

    /**
     * Installs a Mockito mock of {@link Provisioning} and returns it.
     * Always pair with {@link #restoreProvisioning()} in {@code @After}.
     *
     * <pre>
     * Provisioning prov = ZimbraTestUtil.installMockitoProvisioning();
     * when(prov.getAccountByName("user@test.com")).thenReturn(fakeAccount);
     * // ... exercise SUT ...
     * verify(prov).getAccountByName("user@test.com");
     * ZimbraTestUtil.restoreProvisioning();
     * </pre>
     */
    public static Provisioning installMockitoProvisioning() {
        Provisioning mock = Mockito.mock(Provisioning.class);
        Provisioning.setInstance(mock);
        return mock;
    }

    /**
     * Clears the Provisioning singleton. Call in {@code @After} when using
     * {@link #installMockitoProvisioning()}.
     */
    public static void restoreProvisioning() {
        Provisioning.setInstance(null);
    }

    /**
     * JUnit 4 {@code @Rule} that wraps a Mockito Provisioning mock for the
     * lifetime of each test method.
     *
     * <pre>
     * {@literal @}Rule
     * public final ZimbraTestUtil.ProvisioningMockRule provRule =
     *     ZimbraTestUtil.provisioningMockRule();
     *
     * {@literal @}Test
     * public void lookupAccount() throws Exception {
     *     when(provRule.getProv().getAccountByName("a@b.com")).thenReturn(fakeAccount);
     * }
     * </pre>
     */
    public static ProvisioningMockRule provisioningMockRule() {
        return new ProvisioningMockRule();
    }

    public static final class ProvisioningMockRule extends ExternalResource {
        private Provisioning mock;

        @Override protected void before() throws Throwable { mock = installMockitoProvisioning(); }
        @Override protected void after()                   { restoreProvisioning(); }

        public Provisioning getProv() { return mock; }
    }
}
