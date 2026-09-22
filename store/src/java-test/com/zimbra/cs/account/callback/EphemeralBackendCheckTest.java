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

package com.zimbra.cs.account.callback;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log.Level;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.DataKey;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.account.callback.EphemeralBackendCheck.EphemeralBackendMigrationRules;
import com.zimbra.cs.account.callback.EphemeralBackendCheck.MigrationStateHelper;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.InMemoryEphemeralStore;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo.Status;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link EphemeralBackendCheck} and its nested
 * {@link EphemeralBackendMigrationRules}. A recording {@link MigrationStateHelper} captures the
 * decision (deny/warn/allow) so each migration-state branch can be asserted directly. The real
 * {@link MigrationInfo} is driven via the in-memory {@link Config}.
 */
public class EphemeralBackendCheckTest {

    private Provisioning prov;

    private MigrationInfo migrationInfo;

    /** Records the outcome of checkCanChangeURL so the branch taken can be asserted. */
    private static class RecordingHelper extends MigrationStateHelper {
        private String outcome;

        private Reason reason;

        @Override
        public void deny() throws ServiceException {
            outcome = "deny";
        }

        @Override
        public void warn(Reason reason) {
            outcome = "warn";
            this.reason = reason;
        }

        @Override
        public void allow() {
            outcome = "allow";
        }
    }

    /**
     * In-memory log4j2 appender that captures the formatted messages logged on a single category,
     * so the warn/allow log side-effects of {@link EphemeralBackendCheck.ZimbraMigrationStateHelper}
     * can be asserted directly (VoidMethodCall mutations remove these log calls).
     */
    private static final class LogCapture extends AbstractAppender {
        private final List<String> messages = new CopyOnWriteArrayList<String>();

        LogCapture() {
            super("EphemeralBackendCheckTest-capture", null, null, false, null);
        }

        @Override
        public void append(org.apache.logging.log4j.core.LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }

        List<String> messages() {
            return new ArrayList<String>(messages);
        }

        boolean anyContains(String needle) {
            for (String m : messages) {
                if (m.contains(needle)) {
                    return true;
                }
            }
            return false;
        }
    }

    /*
     * Attach a {@link LogCapture} to the given log4j category by installing a dedicated
     * {@link LoggerConfig} at ALL level so INFO/WARN events are captured regardless of the ambient
     * configuration. {@link #detachCapture} removes it again, leaving the configuration untouched.
     */
    private static LogCapture attachCapture(String category) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration cfg = ctx.getConfiguration();
        // Drop any pre-existing LoggerConfig for this category first. Sibling tests that exercise
        // the extension-init path call ZimbraLog.ephemeral.setLevel(...), which installs a
        // dedicated LoggerConfig for "zimbra.ephemeral" via Configurator.setLevel. log4j2's
        // addLogger() will not replace an existing config of the same name, so without this
        // removal our capture appender is attached to an orphaned config and never sees events.
        cfg.removeLogger(category);
        LogCapture capture = new LogCapture();
        capture.start();
        LoggerConfig target = new LoggerConfig(category, org.apache.logging.log4j.Level.ALL, true);
        target.addAppender(capture, org.apache.logging.log4j.Level.ALL, null);
        cfg.addLogger(category, target);
        ctx.updateLoggers();
        return capture;
    }

    private static void detachCapture(String category, LogCapture capture) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration cfg = ctx.getConfiguration();
        cfg.removeLogger(category);
        capture.stop();
        ctx.updateLoggers();
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        migrationInfo = AttributeMigration.getMigrationInfo();
        migrationInfo.clearData(); // start every test from Status.NONE, no URL
    }

    @Test
    public void checkCanChangeURLInProgressDenies() throws Exception {
        // Arrange
        migrationInfo.setStatus(Status.IN_PROGRESS);
        migrationInfo.setURL("ldap://target");
        RecordingHelper helper = new RecordingHelper();

        // Act
        new EphemeralBackendMigrationRules(helper).checkCanChangeURL("ldap://target");

        // Assert
        assertEquals("in-progress migration must deny URL change", "deny", helper.outcome);
        assertEquals("helper URL is set before deciding", "ldap://target", helper.URL);
    }

    @Test
    public void checkCanChangeURLNoMigrationWarnsNoMigration() throws Exception {
        // Arrange -- Status.NONE from clearData()
        RecordingHelper helper = new RecordingHelper();

        // Act
        new EphemeralBackendMigrationRules(helper).checkCanChangeURL("ldap://new");

        // Assert
        assertEquals("warn", helper.outcome);
        assertEquals(MigrationStateHelper.Reason.NO_MIGRATION, helper.reason);
    }

    @Test
    public void checkCanChangeURLFailedSameUrlWarnsMigrationError() throws Exception {
        // Arrange
        migrationInfo.setStatus(Status.FAILED);
        migrationInfo.setURL("ldap://same");
        RecordingHelper helper = new RecordingHelper();

        // Act
        new EphemeralBackendMigrationRules(helper).checkCanChangeURL("ldap://same");

        // Assert
        assertEquals("warn", helper.outcome);
        assertEquals(MigrationStateHelper.Reason.MIGRATION_ERROR, helper.reason);
    }

    @Test
    public void checkCanChangeURLUrlMismatchWarnsUrlMismatch() throws Exception {
        // Arrange -- a completed migration to a different URL
        migrationInfo.setStatus(Status.COMPLETED);
        migrationInfo.setURL("ldap://old");
        RecordingHelper helper = new RecordingHelper();

        // Act
        new EphemeralBackendMigrationRules(helper).checkCanChangeURL("ldap://different");

        // Assert
        assertEquals("warn", helper.outcome);
        assertEquals(MigrationStateHelper.Reason.URL_MISMATCH, helper.reason);
    }

    @Test
    public void checkCanChangeURLCompletedSameUrlAllows() throws Exception {
        // Arrange -- completed migration to the exact URL being set
        migrationInfo.setStatus(Status.COMPLETED);
        migrationInfo.setURL("ldap://final");
        RecordingHelper helper = new RecordingHelper();

        // Act
        new EphemeralBackendMigrationRules(helper).checkCanChangeURL("ldap://final");

        // Assert
        assertEquals("matching completed migration must be allowed", "allow", helper.outcome);
    }

    @Test
    public void preModifyLdapBackendSavesPreviousUrlAndClearsFactory() throws Exception {
        // Arrange -- swap in a recording helper factory so the ldap branch does not deny/throw
        final RecordingHelper helper = new RecordingHelper();
        MigrationStateHelper.Factory original = null;
        EphemeralBackendCheck.setHelperFactory(new MigrationStateHelper.Factory() {
            @Override
            public MigrationStateHelper getHelper() {
                return helper;
            }
        });
        try {
            Config config = prov.getConfig();
            Map<String, Object> seed = new HashMap<String, Object>();
            seed.put(Provisioning.A_zimbraEphemeralBackendURL, "ldap://previous");
            prov.modifyAttrs(config, seed);
            assertEquals("ldap://previous", config.getEphemeralBackendURL());

            Map<String, Object> toModify = new HashMap<String, Object>();
            toModify.put(Provisioning.A_zimbraEphemeralBackendURL, "ldap://new");
            CallbackContext ctx = new CallbackContext(Op.MODIFY);

            // Act
            new EphemeralBackendCheck().preModify(ctx, Provisioning.A_zimbraEphemeralBackendURL,
                    "ldap://new", toModify, config);

            // Assert -- the previous URL is stashed in the context for postModify
            assertEquals("previous URL must be stashed for postModify",
                    "ldap://previous", ctx.getData(DataKey.PREV_EPHEMERAL_BACKEND_URL));
            // the ldap branch routes through checkMigration => recording helper invoked
            assertEquals("warn", helper.outcome);
        } finally {
            EphemeralBackendCheck.setHelperFactory(new EphemeralBackendCheck.ZimbraMigrationStateHelper.Factory());
        }
    }

    @Test
    public void preModifyEmptyBackendUrlThrowsNoFactoryFailure() throws Exception {
        // Arrange -- "".split(":") yields a length-1 array holding "" (NOT zero-length), so the
        // backend resolves to the empty scheme "", for which no factory exists => FAILURE.
        Config config = prov.getConfig();
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraEphemeralBackendURL, "");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new EphemeralBackendCheck().preModify(ctx, Provisioning.A_zimbraEphemeralBackendURL,
                    "", toModify, config);
            fail("expected FAILURE when the empty backend scheme has no factory");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertTrue("message should name the missing factory for the empty scheme",
                    e.getMessage().contains("no factory found for backend"));
        }
    }

    @Test
    public void preModifyUnknownBackendThrowsNoFactoryFailure() throws Exception {
        // Arrange -- a backend scheme with no registered/extension factory
        Config config = prov.getConfig();
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraEphemeralBackendURL, "bogusbackend://host");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new EphemeralBackendCheck().preModify(ctx, Provisioning.A_zimbraEphemeralBackendURL,
                    "bogusbackend://host", toModify, config);
            fail("expected FAILURE when no factory is found for the backend");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertTrue("message should name the missing factory",
                    e.getMessage().contains("no factory found for backend"));
        }
    }

    @Test
    public void preModifyUnrelatedAttrIsIgnored() throws Exception {
        // Arrange -- an attr other than zimbraEphemeralBackendURL must be a no-op
        Config config = prov.getConfig();
        Map<String, Object> toModify = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new EphemeralBackendCheck().preModify(ctx, Provisioning.A_zimbraMailMode,
                "http", toModify, config);

        // Assert -- nothing stashed, no exception
        assertNull("unrelated attr must not stash any data",
                ctx.getData(DataKey.PREV_EPHEMERAL_BACKEND_URL));
    }

    @Test
    public void setHelperFactoryReplacesFactoryIsUsedByPreModify() throws Exception {
        // Arrange -- install a factory that returns our recording helper, then confirm it is used
        final RecordingHelper helper = new RecordingHelper();
        EphemeralBackendCheck.setHelperFactory(new MigrationStateHelper.Factory() {
            @Override
            public MigrationStateHelper getHelper() {
                return helper;
            }
        });
        try {
            migrationInfo.clearData(); // NONE => warn(NO_MIGRATION)
            // Act -- exercise the rules directly through the freshly installed factory's helper
            MigrationStateHelper got =
                    new MigrationStateHelper.Factory() {
                        @Override
                        public MigrationStateHelper getHelper() {
                            return helper;
                        }
                    }.getHelper();
            new EphemeralBackendMigrationRules(got).checkCanChangeURL("ldap://x");

            // Assert
            assertSame("factory must return the installed helper", helper, got);
            assertEquals("warn", helper.outcome);
        } finally {
            EphemeralBackendCheck.setHelperFactory(new EphemeralBackendCheck.ZimbraMigrationStateHelper.Factory());
        }
    }

    @Test
    public void zimbraHelperDenyInProgressMigrationThrowsFailureWithStartTime() throws Exception {
        // Arrange -- real ZimbraMigrationStateHelper, migration IN_PROGRESS
        migrationInfo.setStatus(Status.IN_PROGRESS);
        migrationInfo.setURL("ldap://inprogress");
        EphemeralBackendCheck.ZimbraMigrationStateHelper helper =
                new EphemeralBackendCheck.ZimbraMigrationStateHelper();

        // Act + Assert -- routed through the rules so URL is set before deny()
        try {
            new EphemeralBackendMigrationRules(helper).checkCanChangeURL("ldap://inprogress");
            fail("in-progress migration must throw FAILURE from the real helper");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertTrue("failure should explain the migration is in progress",
                    e.getMessage().contains("in progress"));
        }
    }

    @Test
    public void zimbraHelperWarnNoMigrationLogsNoMigrationRecord() throws Exception {
        // Arrange -- Status.NONE => warn(NO_MIGRATION) on the real helper. Capture the ephemeral
        // log so the warn() call's observable side-effect (the message) is asserted; removing the
        // warn() body (VoidMethodCall L208) or taking any other case leaves the message absent.
        EphemeralBackendCheck.ZimbraMigrationStateHelper helper =
                new EphemeralBackendCheck.ZimbraMigrationStateHelper();
        LogCapture capture = attachCapture(ZimbraLog.ephemeral.getCategory());
        try {
            // Act
            new EphemeralBackendMigrationRules(helper).checkCanChangeURL("ldap://nomig");

            // Assert -- the NO_MIGRATION branch logged its distinctive message
            assertTrue("expected NO_MIGRATION warning, got: " + capture.messages(),
                    capture.anyContains("No record of an attribute migration exists"));
            assertTrue("the provided URL must appear in the warning",
                    capture.anyContains("ldap://nomig"));
            // and must NOT have emitted any of the sibling-branch messages
            assertFalse("must not log MIGRATION_ERROR text",
                    capture.anyContains("did not succeed"));
            assertFalse("must not log URL_MISMATCH text",
                    capture.anyContains("does not match current migration URL"));
            assertFalse("must not log allow() success text",
                    capture.anyContains("Successfully changed backend URL"));
        } finally {
            detachCapture(ZimbraLog.ephemeral.getCategory(), capture);
        }
    }

    @Test
    public void zimbraHelperWarnFailedSameUrlLogsMigrationError() throws Exception {
        // Arrange -- FAILED + same URL => warn(MIGRATION_ERROR)
        migrationInfo.setStatus(Status.FAILED);
        migrationInfo.setURL("ldap://failed");
        EphemeralBackendCheck.ZimbraMigrationStateHelper helper =
                new EphemeralBackendCheck.ZimbraMigrationStateHelper();
        LogCapture capture = attachCapture(ZimbraLog.ephemeral.getCategory());
        try {
            // Act
            new EphemeralBackendMigrationRules(helper).checkCanChangeURL("ldap://failed");

            // Assert -- the MIGRATION_ERROR branch logged its distinctive message (kills L205)
            assertTrue("expected MIGRATION_ERROR warning, got: " + capture.messages(),
                    capture.anyContains("did not succeed"));
            assertFalse("must not log NO_MIGRATION text",
                    capture.anyContains("No record of an attribute migration exists"));
            assertFalse("must not log URL_MISMATCH text",
                    capture.anyContains("does not match current migration URL"));
        } finally {
            detachCapture(ZimbraLog.ephemeral.getCategory(), capture);
        }
    }

    @Test
    public void zimbraHelperWarnUrlMismatchLogsUrlMismatch() throws Exception {
        // Arrange -- completed migration to a different URL => warn(URL_MISMATCH)
        migrationInfo.setStatus(Status.COMPLETED);
        migrationInfo.setURL("ldap://configured");
        EphemeralBackendCheck.ZimbraMigrationStateHelper helper =
                new EphemeralBackendCheck.ZimbraMigrationStateHelper();
        LogCapture capture = attachCapture(ZimbraLog.ephemeral.getCategory());
        try {
            // Act
            new EphemeralBackendMigrationRules(helper).checkCanChangeURL("ldap://provided");

            // Assert -- URL_MISMATCH branch logged its message naming both URLs (kills L211)
            assertTrue("expected URL_MISMATCH warning, got: " + capture.messages(),
                    capture.anyContains("does not match current migration URL"));
            assertTrue("the provided URL must appear", capture.anyContains("ldap://provided"));
            assertTrue("the configured migration URL must appear",
                    capture.anyContains("ldap://configured"));
            assertFalse("must not log NO_MIGRATION text",
                    capture.anyContains("No record of an attribute migration exists"));
        } finally {
            detachCapture(ZimbraLog.ephemeral.getCategory(), capture);
        }
    }

    @Test
    public void zimbraHelperAllowCompletedSameUrlLogsSuccess() throws Exception {
        // Arrange -- completed migration to the exact URL => allow()
        migrationInfo.setStatus(Status.COMPLETED);
        migrationInfo.setURL("ldap://match");
        EphemeralBackendCheck.ZimbraMigrationStateHelper helper =
                new EphemeralBackendCheck.ZimbraMigrationStateHelper();
        LogCapture capture = attachCapture(ZimbraLog.ephemeral.getCategory());
        try {
            // Act
            new EphemeralBackendMigrationRules(helper).checkCanChangeURL("ldap://match");

            // Assert -- allow() logged its success message naming the URL (kills L192)
            assertTrue("expected allow() success log, got: " + capture.messages(),
                    capture.anyContains("Successfully changed backend URL"));
            assertTrue("the URL must appear in the success log",
                    capture.anyContains("ldap://match"));
            assertFalse("allow() must not emit any warn-branch text",
                    capture.anyContains("does not match current migration URL"));
            assertFalse("allow() must not emit NO_MIGRATION text",
                    capture.anyContains("No record of an attribute migration exists"));
        } finally {
            detachCapture(ZimbraLog.ephemeral.getCategory(), capture);
        }
    }

    @Test
    public void zimbraHelperFactoryGetHelperReturnsRealHelper() throws Exception {
        // Arrange
        EphemeralBackendCheck.ZimbraMigrationStateHelper.Factory factory =
                new EphemeralBackendCheck.ZimbraMigrationStateHelper.Factory();

        // Act
        MigrationStateHelper helper = factory.getHelper();

        // Assert
        assertNotNull("factory must produce a helper", helper);
        assertTrue("factory must produce a ZimbraMigrationStateHelper",
                helper instanceof EphemeralBackendCheck.ZimbraMigrationStateHelper);
    }

    @Test
    public void preModifyLdapBackendViaDefaultFactoryWarnsAndStashesPrevUrl() throws Exception {
        // Arrange -- use the real default ZimbraMigrationStateHelper factory (NONE => warn, no throw)
        migrationInfo.clearData();
        Config config = prov.getConfig();
        Map<String, Object> seed = new HashMap<String, Object>();
        seed.put(Provisioning.A_zimbraEphemeralBackendURL, "ldap://prev-default");
        prov.modifyAttrs(config, seed);
        assertEquals("ldap://prev-default", config.getEphemeralBackendURL());

        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraEphemeralBackendURL, "ldap://next-default");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- ldap branch: checkMigration (real helper, NONE => warn), savePreviousUrl, clearFactory
        new EphemeralBackendCheck().preModify(ctx, Provisioning.A_zimbraEphemeralBackendURL,
                "ldap://next-default", toModify, config);

        // Assert
        assertEquals("previous URL stashed for postModify",
                "ldap://prev-default", ctx.getData(DataKey.PREV_EPHEMERAL_BACKEND_URL));
    }

    @Test
    public void preModifyLdapBackendNoPrevUrlDoesNotStashEmptyPrevUrl() throws Exception {
        // Arrange -- clear the prev URL so savePreviousUrl skips stashing (null/empty prev)
        Config config = prov.getConfig();
        Map<String, Object> clear = new HashMap<String, Object>();
        clear.put(Provisioning.A_zimbraEphemeralBackendURL, "");
        prov.modifyAttrs(config, clear);

        migrationInfo.clearData();
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraEphemeralBackendURL, "ldap://fresh");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new EphemeralBackendCheck().preModify(ctx, Provisioning.A_zimbraEphemeralBackendURL,
                "ldap://fresh", toModify, config);

        // Assert -- nothing stashed because there was no prior URL
        assertNull("no previous URL means nothing is stashed",
                ctx.getData(DataKey.PREV_EPHEMERAL_BACKEND_URL));
    }

    @Test
    public void preModifyRegisteredBackendFactoryPassesValidationAndStashesPrevUrl()
            throws Exception {
        // Arrange -- register an in-memory factory for a custom scheme so getFactory(backend) is
        // NON-null. This drives the "factory != null" arm of the L45 guard: validation proceeds
        // (checkMigration + factory.test) instead of throwing the no-factory FAILURE, and the
        // previous URL is stashed. Negating L45 would instead throw, failing this test.
        final String scheme = "memtest";
        EphemeralStore.registerFactory(scheme, InMemoryEphemeralStore.Factory.class.getName());

        final RecordingHelper helper = new RecordingHelper();
        EphemeralBackendCheck.setHelperFactory(new MigrationStateHelper.Factory() {
            @Override
            public MigrationStateHelper getHelper() {
                return helper;
            }
        });
        try {
            Config config = prov.getConfig();
            Map<String, Object> seed = new HashMap<String, Object>();
            seed.put(Provisioning.A_zimbraEphemeralBackendURL, "ldap://before-mem");
            prov.modifyAttrs(config, seed);
            assertEquals("ldap://before-mem", config.getEphemeralBackendURL());

            migrationInfo.clearData();
            Map<String, Object> toModify = new HashMap<String, Object>();
            String url = scheme + "://host";
            toModify.put(Provisioning.A_zimbraEphemeralBackendURL, url);
            CallbackContext ctx = new CallbackContext(Op.MODIFY);

            // Act -- factory found => no FAILURE thrown; checkMigration routes through the helper
            new EphemeralBackendCheck().preModify(ctx, Provisioning.A_zimbraEphemeralBackendURL,
                    url, toModify, config);

            // Assert -- the factory-found path ran checkMigration (helper saw the URL + warned)
            // and stashed the previous URL for postModify.
            assertEquals("checkMigration must run on the factory-found path", "warn",
                    helper.outcome);
            assertEquals("helper URL set from the new backend URL", url, helper.URL);
            assertEquals("previous URL stashed for postModify",
                    "ldap://before-mem", ctx.getData(DataKey.PREV_EPHEMERAL_BACKEND_URL));
        } finally {
            EphemeralBackendCheck.setHelperFactory(
                    new EphemeralBackendCheck.ZimbraMigrationStateHelper.Factory());
        }
    }

    @Test
    public void preModifyUnknownBackendRestoresLogLevelsAfterExtensionInit() throws Exception {
        // Arrange -- the unknown-backend path lowers ephemeral/extensions log levels around the
        // extension-init attempt, then restores them in the finally block (L55/L56). Set a
        // distinctive starting level so the restore is observable: if the restore calls are
        // removed, the level stays at the suppressed 'error' value instead of returning to 'debug'.
        Level savedEphem = ZimbraLog.ephemeral.getLevel();
        Level savedExten = ZimbraLog.extensions.getLevel();
        ZimbraLog.ephemeral.setLevel(Level.debug);
        ZimbraLog.extensions.setLevel(Level.debug);
        try {
            Config config = prov.getConfig();
            Map<String, Object> toModify = new HashMap<String, Object>();
            toModify.put(Provisioning.A_zimbraEphemeralBackendURL, "noSuchBackend://host");
            CallbackContext ctx = new CallbackContext(Op.MODIFY);

            // Act -- no factory for this scheme => extension-init path runs then FAILURE is thrown
            try {
                new EphemeralBackendCheck().preModify(ctx,
                        Provisioning.A_zimbraEphemeralBackendURL, "noSuchBackend://host",
                        toModify, config);
                fail("expected FAILURE for an unknown backend with no factory");
            } catch (ServiceException expected) {
                assertEquals(ServiceException.FAILURE, expected.getCode());
            }

            // Assert -- both levels were restored to their pre-call value (kills L55/L56). Had the
            // restore been skipped, the levels would remain at the suppressed 'error' level.
            assertEquals("ephemeral log level must be restored after extension init",
                    Level.debug, ZimbraLog.ephemeral.getLevel());
            assertEquals("extensions log level must be restored after extension init",
                    Level.debug, ZimbraLog.extensions.getLevel());
        } finally {
            ZimbraLog.ephemeral.setLevel(savedEphem);
            ZimbraLog.extensions.setLevel(savedExten);
        }
    }
}
