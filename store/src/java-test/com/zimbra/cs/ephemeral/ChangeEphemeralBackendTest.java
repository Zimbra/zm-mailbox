package com.zimbra.cs.ephemeral;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.callback.EphemeralBackendCheck.EphemeralBackendMigrationRules;
import com.zimbra.cs.account.callback.EphemeralBackendCheck.MigrationStateHelper;
import com.zimbra.cs.account.callback.EphemeralBackendCheck.MigrationStateHelper.Reason;
import com.zimbra.cs.ephemeral.migrate.AttributeMigration;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo;
import com.zimbra.cs.ephemeral.migrate.MigrationInfo.Status;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class ChangeEphemeralBackendTest {
    public static String MIGRATION_URL = "ldap://1";
    public static String DIFFERENT_URL = "ldap://2";
    private EphemeralBackendMigrationRules rules;
    private DummyMigrationStateHelper helper;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        AttributeMigration.getMigrationInfo().clearData();
        helper = new DummyMigrationStateHelper();
        rules = new EphemeralBackendMigrationRules(helper);
    }

    private void setMigrationInfo(Status status) throws ServiceException {
        MigrationInfo info = AttributeMigration.getMigrationInfo();
        info.setStatus(status);
        info.setURL(MIGRATION_URL);
    }

    @Test
    public void testMigrationInProgress() throws Exception {
        setMigrationInfo(Status.IN_PROGRESS);
        try {
            rules.checkCanChangeURL(MIGRATION_URL);
            fail("should throw an exception when trying to change URL during a migration");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("DummyMigrationStateHelper"));
            assertFalse(helper.allowed);
        }
    }

    @Test
    public void testNoMigration() throws Exception {
        rules.checkCanChangeURL(MIGRATION_URL);
        assertTrue(helper.allowed);
        assertEquals(Reason.NO_MIGRATION, helper.reason);
    }

    @Test
    public void testMigrationFailed() throws Exception {
        setMigrationInfo(Status.FAILED);
        rules.checkCanChangeURL(MIGRATION_URL);
        assertTrue(helper.allowed);
        assertEquals(Reason.MIGRATION_ERROR, helper.reason);
    }

    @Test
    public void testUrlMismatch() throws Exception {
        setMigrationInfo(Status.COMPLETED);
        rules.checkCanChangeURL(DIFFERENT_URL);
        assertTrue(helper.allowed);
        assertEquals(Reason.URL_MISMATCH, helper.reason);
    }

    @Test
    public void testMigrationComplete() throws Exception {
        setMigrationInfo(Status.COMPLETED);
        rules.checkCanChangeURL(MIGRATION_URL);
        assertTrue(helper.allowed);
        assertNull("reason should be null; was " + helper.reason, helper.reason);
    }

    private static class DummyMigrationStateHelper extends MigrationStateHelper {

        private Reason reason;
        private boolean allowed = false;

        @Override
        public void deny() throws ServiceException {
            throw ServiceException.FAILURE("denied by DummyMigrationStateHelper", null);
        }

        @Override
        public void warn(Reason reason) {
            this.allowed = true;
            this.reason = reason;
        }

        @Override
        public void allow() {
            allowed = true;
        }
    }
}
