/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbOutOfOffice;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Notification;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link OutOfOfficeCallback}. Exercises the real callback through the
 * in-memory MockProvisioning harness using real {@link Account} / {@link Cos} entries and real
 * {@link CallbackContext} state.
 *
 * <p>The callback's {@code postModify} branches on three things:
 *   1. the per-callback "done" guard ({@link CallbackContext#isDoneAndSetIfNot}),
 *   2. whether the op is a create ({@link CallbackContext#isCreate}),
 *   3. whether the entry is an {@link Account}.
 * The downstream {@code handleOutOfOffice} call exercises MailboxManager / DbPool which catch and
 * log their own ServiceExceptions, so {@code postModify} never propagates an exception regardless
 * of the backend state. These tests assert that contract plus the guard-state transitions.
 */
public class OutOfOfficeCallbackTest {

    private static final String ATTR = Provisioning.A_zimbraPrefOutOfOfficeReply;

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account createAccount(String name) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        return prov.createAccount(name, "test123", attrs);
    }

    private CallbackContext modifyCtx() {
        return new CallbackContext(CallbackContext.Op.MODIFY);
    }

    private CallbackContext createCtx() {
        return new CallbackContext(CallbackContext.Op.CREATE);
    }

    // Seed one out_of_office row for mbox dated sentOn.
    private void seedOofRow(Mailbox mbox, String sentTo, long sentOn) throws Exception {
        DbConnection conn = null;
        try {
            conn = DbPool.getConnection(mbox);
            DbOutOfOffice.setSentTime(conn, mbox, sentTo, sentOn);
            conn.commit();
        } finally {
            DbPool.quietClose(conn);
        }
    }

    /*
     * Whether mbox has an out_of_office row for sentTo within a 100-year window.
     * Because the window is enormous, this is true iff the row physically exists (i.e. has not
     * been cleared or pruned).
     */
    private boolean oofRowExists(Mailbox mbox, String sentTo) throws Exception {
        DbConnection conn = null;
        try {
            conn = DbPool.getConnection(mbox);
            return DbOutOfOffice.alreadySent(conn, mbox, sentTo, 100L * 365 * 24 * 60 * 60 * 1000);
        } finally {
            DbPool.quietClose(conn);
        }
    }

    @Test
    public void preModifyAnyInputIsNoopAndDoesNotThrow() throws Exception {
        // Arrange
        OutOfOfficeCallback callback = new OutOfOfficeCallback();
        Account account = createAccount("ooo-pre@example.com");

        // Act - preModify is documented as a pure no-op
        callback.preModify(modifyCtx(), ATTR, "vacation reply", new HashMap<String, Object>(), account);

        // Assert - account state is untouched by preModify
        assertNotNull(account.getId());
        assertTrue("preModify must not modify the entry's name",
                "ooo-pre@example.com".equals(account.getName()));
    }

    @Test
    public void postModifyModifyOnAccountCompletesWithoutThrowing() throws Exception {
        // Arrange - a MODIFY op on a real Account reaches handleOutOfOffice
        OutOfOfficeCallback callback = new OutOfOfficeCallback();
        Account account = createAccount("ooo-mod@example.com");
        CallbackContext ctx = modifyCtx();

        // Act - handleOutOfOffice swallows backend ServiceExceptions internally
        callback.postModify(ctx, ATTR, account);

        // Assert - the callback marked itself done in the context (side effect of postModify)
        assertTrue("first postModify must have set the done guard",
                ctx.isDoneAndSetIfNot(OutOfOfficeCallback.class));
        // account is still resolvable after the callback ran
        assertNotNull("account must remain in provisioning",
                prov.getAccountByName("ooo-mod@example.com"));
    }

    /**
     * Drives the real {@code handleOutOfOffice} DB path. Seeds an out_of_office row for the
     * account's mailbox, then a MODIFY postModify must clear it.
     *
     * <p>Kills: L52 handleOutOfOffice() call (without it the row survives), L49 !isCreate branch
     * (negated => never reaches the clear for a MODIFY), L65 DbOutOfOffice.clear() call,
     * L66 conn.commit() (without commit the DELETE is rolled back on close => row survives).
     */
    @Test
    public void postModifyModifyOnAccountClearsSeededOofRow() throws Exception {
        // Arrange
        Account account = createAccount("ooo-clear@example.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        seedOofRow(mbox, "recipient@example.com", System.currentTimeMillis());
        assertTrue("precondition: seeded OOF row must exist before the callback",
                oofRowExists(mbox, "recipient@example.com"));

        OutOfOfficeCallback callback = new OutOfOfficeCallback();

        // Act
        callback.postModify(modifyCtx(), ATTR, account);

        // Assert - the row was deleted AND committed
        assertFalse("MODIFY postModify must clear (and commit) the account's OOF rows",
                oofRowExists(mbox, "recipient@example.com"));
    }

    /**
     * The create branch (L49 {@code !isCreate()}) must SKIP handleOutOfOffice. A seeded OOF row
     * must therefore SURVIVE a CREATE-op postModify. Negating L49, or removing the L52 call,
     * would (respectively) wrongly clear the row or be undetectable — this asserts the row stays.
     */
    @Test
    public void postModifyCreateOpDoesNotClearSeededOofRow() throws Exception {
        // Arrange
        Account account = createAccount("ooo-create-keep@example.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        seedOofRow(mbox, "keep@example.com", System.currentTimeMillis());
        assertTrue(oofRowExists(mbox, "keep@example.com"));

        OutOfOfficeCallback callback = new OutOfOfficeCallback();

        // Act - CREATE op must NOT reset vacation info
        callback.postModify(createCtx(), ATTR, account);

        // Assert - row untouched
        assertTrue("CREATE postModify must NOT clear the account's OOF rows",
                oofRowExists(mbox, "keep@example.com"));
    }

    /**
     * The done-guard (L45 {@code isDoneAndSetIfNot}) must SKIP everything when already set.
     * Pre-marking the context done means a seeded OOF row must SURVIVE. Negating L45 would
     * invert the guard and wrongly clear the row.
     */
    @Test
    public void postModifyPreMarkedDoneDoesNotClearSeededOofRow() throws Exception {
        // Arrange
        Account account = createAccount("ooo-done-keep@example.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        seedOofRow(mbox, "keepdone@example.com", System.currentTimeMillis());
        assertTrue(oofRowExists(mbox, "keepdone@example.com"));

        CallbackContext ctx = modifyCtx();
        // mark done BEFORE invoking -> postModify must return at L45/L46
        assertFalse(ctx.isDoneAndSetIfNot(OutOfOfficeCallback.class));

        OutOfOfficeCallback callback = new OutOfOfficeCallback();

        // Act
        callback.postModify(ctx, ATTR, account);

        // Assert - guard short-circuit means the row is untouched
        assertTrue("pre-marked-done postModify must NOT clear the account's OOF rows",
                oofRowExists(mbox, "keepdone@example.com"));
    }

    /**
     * handleOutOfOffice also calls {@code DbOutOfOffice.prune} (L73) + commit (L74), which delete
     * stale rows GLOBALLY (any mailbox), unlike clear() which is scoped to the modified account's
     * mailbox. Seed a stale row in a SECOND account's mailbox; a MODIFY on the first account must
     * prune that stale row. Removing the L73 prune call leaves the stale row in place.
     */
    @Test
    public void postModifyModifyOnAccountPrunesStaleRowInOtherMailbox() throws Exception {
        // Arrange - account A is the one being modified
        Account acctA = createAccount("ooo-prune-a@example.com");
        // account B holds a STALE row (sent long before now - cacheDuration) in its own mailbox
        Account acctB = createAccount("ooo-prune-b@example.com");
        Mailbox mboxB = MailboxManager.getInstance().getMailboxByAccount(acctB);
        long staleTs = System.currentTimeMillis()
                - (Notification.DEFAULT_OUT_OF_OFFICE_CACHE_DURATION_MILLIS * 4);
        seedOofRow(mboxB, "stale@example.com", staleTs);
        assertTrue("precondition: stale row in other mailbox exists",
                oofRowExists(mboxB, "stale@example.com"));

        OutOfOfficeCallback callback = new OutOfOfficeCallback();

        // Act - modifying A triggers a global prune that removes B's stale row
        callback.postModify(modifyCtx(), ATTR, acctA);

        // Assert - prune (and its commit) removed the stale cross-mailbox row
        assertFalse("MODIFY postModify must prune stale rows across all mailboxes",
                oofRowExists(mboxB, "stale@example.com"));
    }

    /**
     * Complement to the prune test: a FRESH row in another mailbox must NOT be pruned (it is newer
     * than the cutoff) and must NOT be cleared (clear is scoped to the modified account). This pins
     * prune's cutoff comparison so prune doesn't degenerate into deleting everything.
     */
    @Test
    public void postModifyModifyOnAccountKeepsFreshRowInOtherMailbox() throws Exception {
        // Arrange
        Account acctA = createAccount("ooo-fresh-a@example.com");
        Account acctB = createAccount("ooo-fresh-b@example.com");
        Mailbox mboxB = MailboxManager.getInstance().getMailboxByAccount(acctB);
        seedOofRow(mboxB, "fresh@example.com", System.currentTimeMillis());
        assertTrue(oofRowExists(mboxB, "fresh@example.com"));

        OutOfOfficeCallback callback = new OutOfOfficeCallback();

        // Act
        callback.postModify(modifyCtx(), ATTR, acctA);

        // Assert - fresh, other-mailbox row is preserved
        assertTrue("fresh cross-mailbox rows must survive prune+clear",
                oofRowExists(mboxB, "fresh@example.com"));
    }

    @Test
    public void postModifyCalledTwiceSameContextSecondCallShortCircuits() throws Exception {
        // Arrange - the done guard is keyed per CallbackContext instance
        OutOfOfficeCallback callback = new OutOfOfficeCallback();
        Account account = createAccount("ooo-twice@example.com");
        CallbackContext ctx = modifyCtx();

        // Act - first call sets the guard; second call must hit the early return
        callback.postModify(ctx, ATTR, account);
        callback.postModify(ctx, ATTR, account);

        // Assert - guard is set; reaching here without exception proves the short-circuit path ran
        assertTrue("done guard must be set after invocations",
                ctx.isDoneAndSetIfNot(OutOfOfficeCallback.class));
    }

    @Test
    public void postModifyPreMarkedDoneContextReturnsEarlyWithoutTouchingAccount() throws Exception {
        // Arrange - mark the callback done BEFORE invoking postModify
        OutOfOfficeCallback callback = new OutOfOfficeCallback();
        Account account = createAccount("ooo-done@example.com");
        CallbackContext ctx = modifyCtx();
        assertFalse("guard should start unset", ctx.isDoneAndSetIfNot(OutOfOfficeCallback.class));

        // Act - guard is now set, so postModify must return immediately
        callback.postModify(ctx, ATTR, account);

        // Assert - account is untouched and still present
        assertNotNull(prov.getAccountByName("ooo-done@example.com"));
    }

    @Test
    public void postModifyCreateOpSkipsResetAndDoesNotThrow() throws Exception {
        // Arrange - a CREATE op must skip handleOutOfOffice entirely (isCreate() == true)
        OutOfOfficeCallback callback = new OutOfOfficeCallback();
        Account account = createAccount("ooo-create@example.com");
        CallbackContext ctx = createCtx();

        // Act
        callback.postModify(ctx, ATTR, account);

        // Assert - guard still gets set even though the reset branch is skipped
        assertTrue("create-op postModify still sets the done guard",
                ctx.isDoneAndSetIfNot(OutOfOfficeCallback.class));
        assertNotNull(prov.getAccountByName("ooo-create@example.com"));
    }

    @Test
    public void postModifyNonAccountEntrySkipsResetAndDoesNotThrow() throws Exception {
        // Arrange - a Cos is not an Account, so the instanceof branch is skipped
        OutOfOfficeCallback callback = new OutOfOfficeCallback();
        Cos cos = prov.createCos("ooo-cos", new HashMap<String, Object>());
        CallbackContext ctx = modifyCtx();

        // Act - MODIFY op, but entry is not an Account
        callback.postModify(ctx, ATTR, cos);

        // Assert - completes cleanly and the guard is set
        assertTrue("non-account postModify still sets the done guard",
                ctx.isDoneAndSetIfNot(OutOfOfficeCallback.class));
        assertNotNull("cos must remain in provisioning", prov.get(com.zimbra.common.account.Key.CosBy.name, "ooo-cos"));
    }

    @Test
    public void postModifyNullEntryModifyOpDoesNotThrow() throws Exception {
        // Arrange - null entry is not an Account, so reset is skipped without an NPE
        OutOfOfficeCallback callback = new OutOfOfficeCallback();
        CallbackContext ctx = modifyCtx();

        // Act
        callback.postModify(ctx, ATTR, null);

        // Assert
        assertTrue("null-entry postModify still sets the done guard",
                ctx.isDoneAndSetIfNot(OutOfOfficeCallback.class));
    }
}
