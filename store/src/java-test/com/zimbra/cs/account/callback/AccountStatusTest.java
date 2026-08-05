/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.Op;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link AccountStatus} attribute callback. Exercises the real
 * preModify/postModify logic against a real {@link Account} created through the in-memory
 * MockProvisioning harness.
 */
public class AccountStatusTest {

    private Provisioning prov;

    private Account account;

    private AccountStatus callback;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        account = prov.createAccount("status@example.com", "test123", attrs);
        callback = new AccountStatus();
    }

    @Test
    public void preModifyStatusClosedDisablesMailStatus() throws Exception {
        // Arrange
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_CLOSED);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, Provisioning.A_zimbraAccountStatus,
                Provisioning.ACCOUNT_STATUS_CLOSED, attrsToModify, account);

        // Assert
        assertEquals(Provisioning.MAIL_STATUS_DISABLED,
                attrsToModify.get(Provisioning.A_zimbraMailStatus));
    }

    @Test
    public void preModifyStatusPendingDisablesMailStatus() throws Exception {
        // Arrange
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_PENDING);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, Provisioning.A_zimbraAccountStatus,
                Provisioning.ACCOUNT_STATUS_PENDING, attrsToModify, account);

        // Assert
        assertEquals(Provisioning.MAIL_STATUS_DISABLED,
                attrsToModify.get(Provisioning.A_zimbraMailStatus));
    }

    @Test
    public void preModifyStatusActiveEnablesMailStatusWhenNotAlsoSet() throws Exception {
        // Arrange
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, Provisioning.A_zimbraAccountStatus,
                Provisioning.ACCOUNT_STATUS_ACTIVE, attrsToModify, account);

        // Assert
        assertEquals(Provisioning.MAIL_STATUS_ENABLED,
                attrsToModify.get(Provisioning.A_zimbraMailStatus));
    }

    @Test
    public void preModifyStatusActiveWithMailStatusAlreadySetLeavesMailStatusUntouched() throws Exception {
        // Arrange — caller is simultaneously setting zimbraMailStatus to disabled
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        attrsToModify.put(Provisioning.A_zimbraMailStatus, Provisioning.MAIL_STATUS_DISABLED);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, Provisioning.A_zimbraAccountStatus,
                Provisioning.ACCOUNT_STATUS_ACTIVE, attrsToModify, account);

        // Assert — the caller-supplied value must NOT be overwritten
        assertEquals(Provisioning.MAIL_STATUS_DISABLED,
                attrsToModify.get(Provisioning.A_zimbraMailStatus));
    }

    @Test
    public void preModifyStatusActiveOnLockedOutAccountClearsLockoutAttrs() throws Exception {
        // Arrange — give the account lockout attributes, then reactivate it
        Map<String, Object> lockAttrs = new HashMap<String, Object>();
        lockAttrs.put(Provisioning.A_zimbraPasswordLockoutFailureTime, "20240101000000Z");
        lockAttrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, "20240101000000Z");
        prov.modifyAttrs(account, lockAttrs);

        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, Provisioning.A_zimbraAccountStatus,
                Provisioning.ACCOUNT_STATUS_ACTIVE, attrsToModify, account);

        // Assert — both lockout attributes are scheduled for clearing
        assertEquals("", attrsToModify.get(Provisioning.A_zimbraPasswordLockoutFailureTime));
        assertEquals("", attrsToModify.get(Provisioning.A_zimbraPasswordLockoutLockedTime));
    }

    @Test
    public void preModifyStatusActiveWithoutLockoutAttrsDoesNotAddClearEntries() throws Exception {
        // Arrange — account has no lockout attributes set
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, Provisioning.A_zimbraAccountStatus,
                Provisioning.ACCOUNT_STATUS_ACTIVE, attrsToModify, account);

        // Assert — no clear-out entries are added when there is nothing to clear
        assertFalse(attrsToModify.containsKey(Provisioning.A_zimbraPasswordLockoutFailureTime));
        assertFalse(attrsToModify.containsKey(Provisioning.A_zimbraPasswordLockoutLockedTime));
    }

    @Test
    public void preModifyStatusActiveOnNonAccountEntrySkipsLockoutHandling() throws Exception {
        // Arrange — use a Cos (non-Account Entry); status active still toggles mail status
        Entry cosEntry = prov.createCos("statuscos", new HashMap<String, Object>());
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        attrsToModify.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act
        callback.preModify(ctx, Provisioning.A_zimbraAccountStatus,
                Provisioning.ACCOUNT_STATUS_ACTIVE, attrsToModify, cosEntry);

        // Assert — mail status enabled, but no lockout-clearing for a non-Account entry
        assertEquals(Provisioning.MAIL_STATUS_ENABLED,
                attrsToModify.get(Provisioning.A_zimbraMailStatus));
        assertFalse(attrsToModify.containsKey(Provisioning.A_zimbraPasswordLockoutFailureTime));
    }

    @Test
    public void preModifyUnsettingStatusThrowsInvalidRequest() throws Exception {
        // Arrange — null value means the required attribute is being unset
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            callback.preModify(ctx, Provisioning.A_zimbraAccountStatus, null, attrsToModify, account);
            fail("expected ServiceException when unsetting required status attribute");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains(Provisioning.A_zimbraAccountStatus));
            assertTrue(e.getMessage().toLowerCase().contains("required"));
        }
    }

    @Test
    public void preModifyEmptyStringStatusThrowsInvalidRequest() throws Exception {
        // Arrange — empty string is treated as unsetting by singleValueMod
        Map<String, Object> attrsToModify = new HashMap<String, Object>();
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act + Assert
        try {
            callback.preModify(ctx, Provisioning.A_zimbraAccountStatus, "", attrsToModify, account);
            fail("expected ServiceException when status is empty");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains(Provisioning.A_zimbraAccountStatus));
        }
    }

    @Test
    public void postModifyAlreadyDoneReturnsWithoutReprocessing() throws Exception {
        // Arrange — mark the callback done first so postModify short-circuits
        CallbackContext ctx = new CallbackContext(Op.MODIFY);
        assertFalse("precondition: not yet done",
                ctx.isDoneAndSetIfNot(AccountStatus.class));

        // Act — second invocation sees done==true and returns immediately (no exception)
        callback.postModify(ctx, Provisioning.A_zimbraAccountStatus, account);

        // Assert — context remains marked done
        assertTrue(ctx.isDoneAndSetIfNot(AccountStatus.class));
    }

    @Test
    public void postModifyCreateOpSkipsClosedHandling() throws Exception {
        // Arrange — on create, the closed-account handling must be skipped
        CallbackContext ctx = new CallbackContext(Op.CREATE);
        assertTrue("precondition: this is a create op", ctx.isCreate());

        // Act — should complete without touching distribution lists
        callback.postModify(ctx, Provisioning.A_zimbraAccountStatus, account);

        // Assert — account remains active and retrievable
        Account reloaded = prov.get(com.zimbra.common.account.Key.AccountBy.name, "status@example.com");
        assertEquals(Provisioning.ACCOUNT_STATUS_ACTIVE,
                reloaded.getAttr(Provisioning.A_zimbraAccountStatus));
    }

    @Test
    public void postModifyModifyActiveAccountDoesNotRemoveFromDistributionLists() throws Exception {
        // Arrange — modify op, active (non-closed) account: closed-handling returns early
        CallbackContext ctx = new CallbackContext(Op.MODIFY);

        // Act — active status means handleAccountStatusClosed short-circuits before
        // touching getDistributionLists, so this completes cleanly
        callback.postModify(ctx, Provisioning.A_zimbraAccountStatus, account);

        // Assert — account still active; postModify does not persist a mail-status change
        Account reloaded = prov.get(com.zimbra.common.account.Key.AccountBy.name, "status@example.com");
        assertNull("postModify must not set zimbraMailStatus on an active account",
                reloaded.getAttr(Provisioning.A_zimbraMailStatus, null));
        assertEquals(Provisioning.ACCOUNT_STATUS_ACTIVE,
                reloaded.getAttr(Provisioning.A_zimbraAccountStatus));
    }
}
