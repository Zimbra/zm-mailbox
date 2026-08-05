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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.soap.admin.type.DataSourceType;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link DataSourceCallback}. Drives the real {@code preModify}
 * polling-interval validation against {@link Account} and {@link Cos} entries and verifies
 * the postModify early-return guards.
 */
public class DataSourceCallbackTest {

    private Provisioning prov;

    /** Entities created per test, deleted in {@link #tearDown()} even when a test fails. */
    private final List<Account> createdAccounts = new ArrayList<Account>();

    private final List<Cos> createdCos = new ArrayList<Cos>();

    private final List<Server> createdServers = new ArrayList<Server>();

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    @After
    public void tearDown() throws Exception {
        // Always restore the static started-flag some tests toggle, then delete tracked entities.
        setZimbraStarted(false);
        for (Account a : createdAccounts) {
            try {
                prov.deleteAccount(a.getId());
            } catch (Exception ignore) {
                // best-effort cleanup: one failure must not block the rest
            }
        }
        for (Cos c : createdCos) {
            try {
                prov.deleteCos(c.getId());
            } catch (Exception ignore) {
                // best-effort cleanup
            }
        }
        for (Server s : createdServers) {
            try {
                prov.deleteServer(s.getId());
            } catch (Exception ignore) {
                // best-effort cleanup
            }
        }
        createdAccounts.clear();
        createdCos.clear();
        createdServers.clear();
    }

    private Account newAccount(String name, String minInterval) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        if (minInterval != null) {
            attrs.put(Provisioning.A_zimbraDataSourceMinPollingInterval, minInterval);
        }
        Account acct = prov.createAccount(name, "test123", attrs);
        createdAccounts.add(acct);
        return acct;
    }

    @Test
    public void preModifyAccountIntervalBelowMinimumThrows() throws Exception {
        // Arrange -- minimum is 5 minutes, attempt to set 1 minute
        Account acct = newAccount("ds-low@example.com", "5m");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraDataSourcePollingInterval, "1m");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new DataSourceCallback().preModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                    "1m", toModify, acct);
            fail("expected INVALID_REQUEST when interval is below the minimum");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue("message should explain the minimum",
                    e.getMessage().contains("shorter than the allowed minimum"));
        }
    }

    @Test
    public void preModifyAccountIntervalAtOrAboveMinimumPasses() throws Exception {
        // Arrange
        Account acct = newAccount("ds-ok@example.com", "5m");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraDataSourcePollingInterval, "10m");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- 10m >= 5m minimum, no exception expected
        new DataSourceCallback().preModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                "10m", toModify, acct);

        // Assert -- reaching here without an exception means the interval was accepted; the
        // account still carries its configured minimum.
        assertEquals("5m", acct.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
    }

    @Test
    public void preModifyAccountIntervalExactlyMinimumPasses() throws Exception {
        // Arrange -- interval EQUAL to the minimum. validateInterval uses `interval < lMinInterval`
        // (L243); at equality this is false, so it must NOT throw. A ConditionalsBoundary mutation
        // to `interval <= lMinInterval` would throw here, so this test kills it.
        Account acct = newAccount("ds-eq@example.com", "5m");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraDataSourcePollingInterval, "5m");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- 5m == 5m minimum: must be accepted (no throw).
        new DataSourceCallback().preModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                "5m", toModify, acct);

        // Assert -- the equal-to-minimum interval was accepted.
        assertEquals("5m", acct.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
    }

    @Test
    public void preModifyAccountIntervalOneSecondBelowMinimumThrows() throws Exception {
        // Arrange -- just under the minimum (just below the < boundary) must throw. Together with
        // the exactly-equal test this pins both sides of the L243 boundary.
        Account acct = newAccount("ds-justbelow@example.com", "5m");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraDataSourcePollingInterval, "299s"); // 4m59s < 5m
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            new DataSourceCallback().preModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                    "299s", toModify, acct);
            fail("expected INVALID_REQUEST for an interval just below the minimum");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void preModifyZeroIntervalAlwaysPasses() throws Exception {
        // Arrange -- interval of 0 disables validation regardless of minimum
        Account acct = newAccount("ds-zero@example.com", "5m");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraDataSourcePollingInterval, "0");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new DataSourceCallback().preModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                "0", toModify, acct);

        // Assert
        assertTrue("zero interval must short-circuit validation", true);
    }

    @Test
    public void preModifyNoMinimumConfiguredPasses() throws Exception {
        // Arrange -- no min interval => lMinInterval is 0 => any positive interval passes
        Account acct = newAccount("ds-nomin@example.com", null);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraDataSourcePollingInterval, "1m");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        new DataSourceCallback().preModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                "1m", toModify, acct);

        // Assert
        assertTrue("with no configured minimum any interval is valid", true);
    }

    @Test
    public void preModifyNonIntervalAttrIsIgnored() throws Exception {
        // Arrange -- an attr not in INTERVAL_ATTRS should be skipped entirely
        Account acct = newAccount("ds-other@example.com", "5m");
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraDataSourceEnabled, "TRUE");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- attrName is not an interval attr, so no validation occurs
        new DataSourceCallback().preModify(ctx, Provisioning.A_zimbraDataSourceEnabled,
                "TRUE", toModify, acct);

        // Assert
        assertTrue("non-interval attributes are not validated", true);
    }

    @Test
    public void preModifyCosIntervalBelowMinimumIsNotValidated() throws Exception {
        // Arrange -- Cos branch of preModify. NOTE: in DataSourceCallback.validateCos the
        // arguments to validateInterval are passed in the order (newInterval, attrName,
        // minInterval) -- i.e. the newInterval and attrName positions are swapped relative
        // to validateInterval(attrName, newInterval, minInterval). As a result the method
        // attempts to parse the *attribute name* ("zimbraDataSourcePollingInterval") as a
        // time duration, which DateUtil.getTimeInterval returns as 0, short-circuiting the
        // check. So a below-minimum interval on a Cos does NOT throw -- this test asserts
        // the actual behavior of the code.
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        cosAttrs.put(Provisioning.A_zimbraDataSourceMinPollingInterval, "5m");
        Cos cos = prov.createCos("ds-cos-low", cosAttrs);
        createdCos.add(cos);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraDataSourcePollingInterval, "30s");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- below-minimum interval on a Cos is not validated; no exception is thrown
        new DataSourceCallback().preModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                "30s", toModify, cos);

        // Assert -- the min interval attr is still on the Cos and reaching here means no throw
        assertEquals("5m", cos.getAttr(Provisioning.A_zimbraDataSourceMinPollingInterval));
    }

    @Test
    public void preModifyUnrecognizedEntryTypeIsIgnored() throws Exception {
        // Arrange -- a Server entry is none of DataSource/Account/Cos, so it falls through
        Server server = prov.createServer("ds-server.example.com", new HashMap<String, Object>());
        createdServers.add(server);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraDataSourcePollingInterval, "1s");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- interval attr but entry is a Server: none of the instanceof branches match
        new DataSourceCallback().preModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                "1s", toModify, server);

        // Assert
        assertTrue("Server entries are not interval-validated", true);
    }

    @Test
    public void postModifyServerNotStartedReturnsWithoutScheduling() throws Exception {
        // Arrange -- Zimbra.started() is false in unit tests, so postModify must early-return
        Account acct = newAccount("ds-post@example.com", "5m");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- this must NOT attempt DataSourceManager scheduling (which needs a running server)
        new DataSourceCallback().postModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                acct);

        // Assert -- reached here without throwing means the started() guard worked
        assertTrue("postModify must early-return when server not started", true);
    }

    // ------------------------------------------------------------------
    // preModify -- DataSource entry branch (validateDataSource)
    // ------------------------------------------------------------------

    private DataSource newDataSource(Account acct, String dsMinIntervalIgnored) throws Exception {
        // The min interval used for DataSource validation is read from the *account*,
        // not the data source, so the data source attrs can be empty here.
        Map<String, Object> dsAttrs = new HashMap<String, Object>();
        return new DataSource(acct, DataSourceType.imap, "ds-name", "ds-id-1", dsAttrs, prov);
    }

    @Test
    public void preModifyDataSourceIntervalBelowAccountMinimumThrows() throws Exception {
        // Arrange -- account minimum is 5m; the data source's account drives the check.
        Account acct = newAccount("ds-entry-low@example.com", "5m");
        DataSource ds = newDataSource(acct, null);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraDataSourcePollingInterval, "1m");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert -- DataSource branch validates against the account minimum.
        try {
            new DataSourceCallback().preModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                    "1m", toModify, ds);
            fail("expected INVALID_REQUEST for below-minimum interval on a data source");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("shorter than the allowed minimum"));
        }
    }

    @Test
    public void preModifyDataSourceIntervalAtOrAboveMinimumPasses() throws Exception {
        // Arrange
        Account acct = newAccount("ds-entry-ok@example.com", "5m");
        DataSource ds = newDataSource(acct, null);
        Map<String, Object> toModify = new HashMap<String, Object>();
        toModify.put(Provisioning.A_zimbraDataSourcePollingInterval, "10m");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act -- 10m >= 5m minimum: no exception.
        new DataSourceCallback().preModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                "10m", toModify, ds);

        // Assert
        assertTrue("above-minimum interval on a data source must be accepted", true);
    }

    // ------------------------------------------------------------------
    // postModify -- with the server marked started (Zimbra.sInited toggled via reflection)
    // ------------------------------------------------------------------

    private void setZimbraStarted(boolean started) throws Exception {
        Field f = Zimbra.class.getDeclaredField("sInited");
        f.setAccessible(true);
        f.setBoolean(null, started);
    }

    @Test
    public void postModifyCreateCosReturnsEarlyBeforeScheduling() throws Exception {
        // Arrange -- on a COS create there are no accounts yet, so postModify must short-circuit
        // at the create+Cos guard rather than attempting to schedule data sources.
        Cos cos = prov.createCos("ds-postcos", new HashMap<String, Object>());
        createdCos.add(cos);
        CallbackContext ctx = new CallbackContext(Op.CREATE);
        setZimbraStarted(true);
        try {
            // Act -- create context + Cos entry => early return at the create guard.
            new DataSourceCallback().postModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                    cos);
            // Assert -- no throw means the guard was honored.
            assertTrue("create+Cos must short-circuit", true);
        } finally {
            setZimbraStarted(false);
        }
    }

    @Test
    public void postModifyAccountIntervalChangeSchedulesWithoutError() throws Exception {
        // Arrange -- a real account with no data sources: scheduleAccount iterates an empty list.
        Account acct = newAccount("ds-sched-acct@example.com", "5m");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        setZimbraStarted(true);
        try {
            // Act -- interval attr on an Account drives scheduleAccount (empty data source list).
            new DataSourceCallback().postModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                    acct);
            // Assert
            assertTrue("scheduleAccount over an empty data source list must not throw", true);
        } finally {
            setZimbraStarted(false);
        }
    }

    @Test
    public void postModifyDataSourceIntervalChangeSchedulesWithoutError() throws Exception {
        // Arrange -- a DataSource on an account with no mailbox: updateSchedule returns at mbox==-1.
        Account acct = newAccount("ds-sched-ds@example.com", "5m");
        DataSource ds = newDataSource(acct, null);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        setZimbraStarted(true);
        try {
            // Act -- interval attr on a DataSource drives scheduleDataSource.
            new DataSourceCallback().postModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                    ds);
            // Assert
            assertTrue("scheduleDataSource must not throw without a running mailbox", true);
        } finally {
            setZimbraStarted(false);
        }
    }

    @Test
    public void postModifyEnabledAttrOnAccountSchedulesWithoutError() throws Exception {
        // Arrange -- zimbraDataSourceEnabled is also a scheduling trigger (not an interval attr).
        Account acct = newAccount("ds-enabled@example.com", "5m");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        setZimbraStarted(true);
        try {
            // Act -- the enabled attr takes the scheduling branch for Account entries.
            new DataSourceCallback().postModify(ctx, Provisioning.A_zimbraDataSourceEnabled, acct);
            // Assert
            assertTrue("enabled-attr change must schedule without throwing", true);
        } finally {
            setZimbraStarted(false);
        }
    }

    private DataSource newDataSourceWithError(Account acct) throws Exception {
        Map<String, Object> dsAttrs = new HashMap<String, Object>();
        dsAttrs.put(Provisioning.A_zimbraDataSourceLastError, "boom");
        dsAttrs.put(Provisioning.A_zimbraDataSourceFailingSince, "20200101000000Z");
        return new DataSource(acct, DataSourceType.imap, "ds-err", "ds-err-id", dsAttrs, prov);
    }

    @Test
    public void postModifyNonSchedulingAttrOnDataSourceResetsErrorStatus() throws Exception {
        // Arrange -- a non-interval, non-enabled attr on a DataSource that DOES carry an error
        // status takes the resetErrorStatus branch (L113 -> L115). resetErrorStatus clears both the
        // last-error and failing-since attrs. This observably kills:
        //   - L115 VoidMethodCall (resetErrorStatus removed -> error attrs remain set)
        //   - L105 NegateConditionals (the `else if (entry instanceof DataSource)` branch)
        Account acct = newAccount("ds-reset@example.com", "5m");
        DataSource ds = newDataSourceWithError(acct);
        assertEquals("boom", ds.getAttr(Provisioning.A_zimbraDataSourceLastError));
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        setZimbraStarted(true);
        try {
            // Act -- displayName is neither an interval attr nor the enabled attr.
            new DataSourceCallback().postModify(ctx, Provisioning.A_zimbraDataSourceName, ds);
            // Assert -- the error status was cleared.
            assertNull("last error must be cleared by resetErrorStatus",
                    ds.getAttr(Provisioning.A_zimbraDataSourceLastError));
            assertNull("failing-since must be cleared by resetErrorStatus",
                    ds.getAttr(Provisioning.A_zimbraDataSourceFailingSince));
        } finally {
            setZimbraStarted(false);
        }
    }

    @Test
    public void postModifyServerNotStartedDoesNotResetErrorStatus() throws Exception {
        // Arrange -- same setup but the server is NOT started. The L91 guard
        //   if (!Zimbra.started() || !scheduling_enabled) return;
        // must early-return, so resetErrorStatus must NOT run and the error stays set.
        // If L91 were negated, the body would execute and clear the error.
        Account acct = newAccount("ds-notstarted@example.com", "5m");
        DataSource ds = newDataSourceWithError(acct);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        // Note: setZimbraStarted NOT called -> started() is false.

        // Act
        new DataSourceCallback().postModify(ctx, Provisioning.A_zimbraDataSourceName, ds);

        // Assert -- error status preserved because postModify short-circuited.
        assertEquals("error must remain when server not started",
                "boom", ds.getAttr(Provisioning.A_zimbraDataSourceLastError));
    }

    @Test
    public void postModifyIntervalAttrOnDataSourceDoesNotResetErrorStatus() throws Exception {
        // Arrange -- an INTERVAL attr on a DataSource takes the scheduling branch (L100 true), NOT
        // the resetErrorStatus branch. The error status must therefore be left untouched. This pins
        // the L100 NegateConditionals split: interval attr => schedule (no reset); other attr =>
        // reset. The scheduling call is a no-op without a mailbox, so the error stays set.
        Account acct = newAccount("ds-interval-noreset@example.com", "5m");
        DataSource ds = newDataSourceWithError(acct);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        setZimbraStarted(true);
        try {
            // Act -- interval attr drives scheduleDataSource (no mailbox -> no-op), not reset.
            new DataSourceCallback().postModify(ctx, Provisioning.A_zimbraDataSourcePollingInterval,
                    ds);
            // Assert -- error status untouched because the reset branch was not taken.
            assertEquals("interval attr must not clear the error status",
                    "boom", ds.getAttr(Provisioning.A_zimbraDataSourceLastError));
        } finally {
            setZimbraStarted(false);
        }
    }

    @Test
    public void postModifyNonSchedulingAttrOnAccountIsIgnored() throws Exception {
        // Arrange -- a non-scheduling attr on an Account (not a DataSource) falls through all
        // branches and simply returns.
        Account acct = newAccount("ds-noop@example.com", "5m");
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        setZimbraStarted(true);
        try {
            // Act
            new DataSourceCallback().postModify(ctx, Provisioning.A_zimbraDataSourceName, acct);
            // Assert
            assertTrue("non-scheduling attr on a non-data-source entry is ignored", true);
        } finally {
            setZimbraStarted(false);
        }
    }
}
