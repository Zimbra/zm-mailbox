/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.accesscontrol;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link AccessControlUtil}. Exercises the admin-classification helpers
 * ({@code isGlobalAdmin}/{@code isDelegatedAdmin}) against real {@link Account} entries created
 * through the in-memory {@link Provisioning} harness, and the email/auth-token resolution helpers
 * ({@code emailAddrToAccount}/{@code emailAddrToMailTarget}) which look entries up via the harness
 * and fall back to {@link GuestAccount} for user rights. {@code User.R_loginAs} (a real user right)
 * is loaded by {@link RightManager} during {@link MailboxTestUtil#initServer()}.
 */
public class AccessControlUtilTest {

    private static Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        RightManager.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account createAccount(String email, Map<String, Object> attrs) throws Exception {
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        return prov.createAccount(email, "test123", attrs);
    }

    @Test
    public void isGlobalAdminAdminAccountAsAdminReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = createAccount("gadmin@example.com", attrs);

        // Act / Assert
        assertTrue("admin acting as admin is global admin",
                AccessControlUtil.isGlobalAdmin(admin));
        assertTrue(AccessControlUtil.isGlobalAdmin(admin, true));
    }

    @Test
    public void isGlobalAdminAdminAccountNotAsAdminReturnsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = createAccount("gadmin2@example.com", attrs);

        // Act / Assert — asAdmin=false short-circuits
        assertFalse(AccessControlUtil.isGlobalAdmin(admin, false));
    }

    @Test
    public void isGlobalAdminNonAdminAccountReturnsFalse() throws Exception {
        // Arrange
        Account user = createAccount("plainuser@example.com", new HashMap<String, Object>());

        // Act / Assert
        assertFalse("non-admin account is not global admin",
                AccessControlUtil.isGlobalAdmin(user));
    }

    @Test
    public void isGlobalAdminNullAccountReturnsFalse() throws Exception {
        // Act / Assert — null guard
        assertFalse(AccessControlUtil.isGlobalAdmin((Account) null, true));
    }

    @Test
    public void isGlobalAdminMailTargetOverloadWithAdminAccountReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = createAccount("mtadmin@example.com", attrs);

        // Act / Assert — MailTarget overload
        assertTrue(AccessControlUtil.isGlobalAdmin((MailTarget) admin, true));
    }

    @Test
    public void isGlobalAdminMailTargetOverloadGuestAccountReturnsFalse() throws Exception {
        // Arrange — a GuestAccount is an Account but never an admin
        GuestAccount guest = new GuestAccount("guest@external.com", null);

        // Act / Assert
        assertFalse(AccessControlUtil.isGlobalAdmin((MailTarget) guest, true));
    }

    @Test
    public void isDelegatedAdminDelegatedAdminAsAdminReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE");
        Account delegated = createAccount("deladmin@example.com", attrs);

        // Act / Assert
        assertTrue(AccessControlUtil.isDelegatedAdmin(delegated, true));
        assertFalse("asAdmin=false denies", AccessControlUtil.isDelegatedAdmin(delegated, false));
    }

    @Test
    public void isDelegatedAdminNullAccountReturnsFalse() throws Exception {
        // Act / Assert
        assertFalse(AccessControlUtil.isDelegatedAdmin(null, true));
    }

    @Test
    public void emailAddrToAccountExistingAccountResolvesRealAccount() throws Exception {
        // Arrange
        Account user = createAccount("resolve@example.com", new HashMap<String, Object>());

        // Act
        Account resolved = AccessControlUtil.emailAddrToAccount("resolve@example.com", User.R_loginAs);

        // Assert — the harness-resolved account is the same entry
        assertEquals(user.getId(), resolved.getId());
        assertEquals("resolve@example.com", resolved.getName());
    }

    @Test
    public void emailAddrToAccountUnknownEmailUserRightReturnsGuestAccount() throws Exception {
        // Act — unknown address with a user right falls back to a named GuestAccount
        Account resolved = AccessControlUtil.emailAddrToAccount("nobody@external.com", User.R_loginAs);

        // Assert
        assertTrue("user right falls back to GuestAccount", resolved instanceof GuestAccount);
        assertEquals("nobody@external.com", resolved.getName());
    }

    @Test
    public void emailAddrToAccountNullEmailUserRightReturnsAnonymousGuest() throws Exception {
        // Act — null email + user right => the well-known anonymous guest
        Account resolved = AccessControlUtil.emailAddrToAccount(null, User.R_loginAs);

        // Assert
        assertSame(GuestAccount.ANONYMOUS_ACCT, resolved);
    }

    @Test
    public void emailAddrToMailTargetExistingAccountResolvesRealAccount() throws Exception {
        // Arrange
        Account user = createAccount("mt-resolve@example.com", new HashMap<String, Object>());

        // Act
        MailTarget resolved =
                AccessControlUtil.emailAddrToMailTarget("mt-resolve@example.com", User.R_loginAs);

        // Assert
        assertTrue(resolved instanceof Account);
        assertEquals(user.getId(), ((Account) resolved).getId());
    }

    @Test
    public void emailAddrToMailTargetNullEmailUserRightReturnsAnonymousGuest() throws Exception {
        // Act
        MailTarget resolved = AccessControlUtil.emailAddrToMailTarget(null, User.R_loginAs);

        // Assert
        assertSame(GuestAccount.ANONYMOUS_ACCT, resolved);
    }

    @Test
    public void authTokenToAccountNullTokenUserRightReturnsAnonymousGuest() throws Exception {
        // Act — null auth token with a user right yields the anonymous guest
        Account resolved = AccessControlUtil.authTokenToAccount(null, User.R_loginAs);

        // Assert
        assertSame(GuestAccount.ANONYMOUS_ACCT, resolved);
    }

    @Test
    public void authTokenToAccountNullTokenAdminRightReturnsNull() throws Exception {
        // Arrange — an admin (non-user) right does not fall back to a guest
        Right adminRight = RightManager.getInstance().getAllAdminRights().values().iterator().next();

        // Act
        Account resolved = AccessControlUtil.authTokenToAccount(null, adminRight);

        // Assert — no guest fallback for admin rights
        assertNull(resolved);
    }
}
