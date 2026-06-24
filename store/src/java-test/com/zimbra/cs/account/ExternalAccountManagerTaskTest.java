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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.AccountStatus;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link ExternalAccountManagerTask}. Drives the real {@link TimerTask#run()}
 * against the in-memory harness (it must complete without propagating any exception because the
 * body catches Throwable), and reflectively exercises the private static
 * {@code disableOrDeleteAccount} transition and the inner {@code ShareInfoVisitor} on real domain
 * objects — no mocking.
 */
public class ExternalAccountManagerTaskTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        provisioning = Provisioning.getInstance();
    }

    // ---------- run() ----------

    @Test
    public void runAnyEnvironmentIsTimerTaskAndCompletesWithoutThrowing() {
        // Arrange
        ExternalAccountManagerTask task = new ExternalAccountManagerTask();

        // Assert structural contract
        assertTrue("task is a TimerTask", task instanceof TimerTask);

        // Act — run() catches Throwable internally, so it must never propagate one
        task.run();

        // Assert — reaching here means run() returned normally
        assertTrue("run() completed without propagating an exception", true);
    }

    // ---------- disableOrDeleteAccount (private static) ----------

    @Test
    public void disableOrDeleteAccountActiveAccountDisablesAndSetsClosed() throws Exception {
        // Arrange — a real account in active status
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAccountStatus, AccountStatus.active.toString());
        Account account = provisioning.createAccount("ext-active@example.com", "secret", attrs);
        assertEquals("precondition: active", AccountStatus.active, account.getAccountStatus());

        Method m = ExternalAccountManagerTask.class.getDeclaredMethod(
                "disableOrDeleteAccount", Provisioning.class, Account.class,
                com.zimbra.cs.mailbox.Mailbox.class);
        m.setAccessible(true);

        // Act — Mailbox is unused on the active branch, so null is safe here
        m.invoke(null, provisioning, account, null);

        // Assert — transitioned active -> closed and stamped a disabled time
        assertEquals("active account is moved to closed", AccountStatus.closed,
                account.getAccountStatus());
        assertNotNull("external-account disabled time recorded",
                account.getExternalAccountDisabledTime());

        // Cleanup
        provisioning.deleteAccount(account.getId());
        assertNull("account cleaned up", provisioning.get(AccountBy.name, "ext-active@example.com"));
    }

    @Test
    public void disableOrDeleteAccountClosedWithZeroLifetimeLeavesAccountClosed() throws Exception {
        // Arrange — closed account whose disabled-lifetime is explicitly 0 (a value of "0"
        // means the account is never auto-deleted, so the delete branch is skipped)
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAccountStatus, AccountStatus.closed.toString());
        attrs.put(Provisioning.A_zimbraExternalAccountLifetimeAfterDisabled, "0");
        Account account = provisioning.createAccount("ext-closed@example.com", "secret", attrs);
        assertEquals("precondition: closed", AccountStatus.closed, account.getAccountStatus());
        assertEquals("precondition: zero lifetime", 0L,
                account.getExternalAccountLifetimeAfterDisabled());

        Method m = ExternalAccountManagerTask.class.getDeclaredMethod(
                "disableOrDeleteAccount", Provisioning.class, Account.class,
                com.zimbra.cs.mailbox.Mailbox.class);
        m.setAccessible(true);

        // Act — zero lifetime means no deletion path is taken; Mailbox unused, null is safe
        m.invoke(null, provisioning, account, null);

        // Assert — account survives and stays closed
        assertNotNull("account not deleted with zero lifetime",
                provisioning.get(AccountBy.name, "ext-closed@example.com"));
        assertEquals("status remains closed", AccountStatus.closed, account.getAccountStatus());

        // Cleanup
        provisioning.deleteAccount(account.getId());
    }

    // ---------- ShareInfoVisitor (private static inner) ----------

    @Test
    @SuppressWarnings("unchecked")
    public void shareInfoVisitorNoMatchingMountpointResultStaysFalse() throws Exception {
        // Arrange — construct the inner visitor with an empty mountpoint list
        Class<?> visitorCls = Class.forName(
                "com.zimbra.cs.account.ExternalAccountManagerTask$ShareInfoVisitor");
        Constructor<?> ctor = visitorCls.getDeclaredConstructor(List.class);
        ctor.setAccessible(true);
        Object visitor = ctor.newInstance(new ArrayList<Object>());

        // a real ShareInfoData — no mountpoints to match it against
        ShareInfoData data = new ShareInfoData();
        data.setOwnerAcctId("owner-1");
        data.setItemId(42);

        Method visit = visitorCls.getDeclaredMethod("visit", ShareInfoData.class);
        visit.setAccessible(true);
        Method getResult = visitorCls.getDeclaredMethod("getResult");
        getResult.setAccessible(true);

        // Act
        visit.invoke(visitor, data);
        boolean result = (Boolean) getResult.invoke(visitor);

        // Assert — with no mountpoints, nothing matched, so result is still false
        assertFalse("empty mountpoint list yields no valid match", result);

        // also confirm the default initial state
        Field resultField = visitorCls.getDeclaredField("result");
        resultField.setAccessible(true);
        assertFalse("initial result default is false", (Boolean) resultField.get(visitor));
    }
}
