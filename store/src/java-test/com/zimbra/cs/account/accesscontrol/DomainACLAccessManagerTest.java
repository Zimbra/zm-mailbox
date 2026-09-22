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
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link DomainACLAccessManager}, the domain-based access manager that
 * supports user ACLs only (no admin rights). The manager is constructed directly (its ctor boots
 * {@link RightManager}) and driven through the real {@code canAccessAccount}/{@code canDo} flows
 * with real {@link Account} entries created via the in-memory {@link Provisioning} harness.
 *
 * <p>Key reachable branches covered: self-access always allowed; admin-credential access via the
 * superclass {@link com.zimbra.cs.account.DomainAccessManager}; the {@code R_loginAs} user-right
 * path that skips the admin short-circuit; the {@code null} grantee → anonymous-guest defaulting;
 * and the email-string resolution overload of {@code canDo}.
 */
public class DomainACLAccessManagerTest {

    private static Provisioning prov;

    private DomainACLAccessManager mgr;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        RightManager.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        mgr = new DomainACLAccessManager();
    }

    private Account createAccount(String email, Map<String, Object> attrs) throws Exception {
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        return prov.createAccount(email, "test123", attrs);
    }

    @Test
    public void constructorInitializesRightManagerManagerUsable() throws Exception {
        // Act
        DomainACLAccessManager fresh = new DomainACLAccessManager();

        // Assert — construction wired the right manager and a usable manager instance
        assertNotNull(fresh);
        assertNotNull("RightManager available after ctor", RightManager.getInstance());
    }

    @Test
    public void canDoSelfTargetAlwaysAllowed() throws Exception {
        // Arrange — grantee is the same account as the target
        Account self = createAccount("self@example.com", new HashMap<String, Object>());

        // Act — request a non-self right; self-rule (step 1) still allows
        boolean allowed = mgr.canDo(self, self, User.R_loginAs, false, false);

        // Assert
        assertTrue("self always allowed", allowed);
    }

    @Test
    public void canDoNullGranteeNoAclReturnsDefaultGrant() throws Exception {
        // Arrange — distinct target; null grantee becomes the anonymous guest, no ACL set
        Account target = createAccount("nulltarget@example.com", new HashMap<String, Object>());

        // Act — defaultGrant=true should surface when no ACL and no configured right default
        boolean allowedDefaultTrue =
                mgr.canDo((Account) null, target, User.R_loginAs, false, true);
        boolean allowedDefaultFalse =
                mgr.canDo((Account) null, target, User.R_loginAs, false, false);

        // Assert — with no ACL, the callsite default is honored (loginAs has no configured default)
        assertTrue("default grant true honored when no ACL", allowedDefaultTrue);
        assertFalse("default grant false honored when no ACL", allowedDefaultFalse);
    }

    @Test
    public void canDoOtherUserNoAclLoginAsDeniedByDefault() throws Exception {
        // Arrange — two unrelated accounts, no ACL granting loginAs
        Account grantee = createAccount("grantee@example.com", new HashMap<String, Object>());
        Account target = createAccount("targetacct@example.com", new HashMap<String, Object>());

        // Act — loginAs with defaultGrant=false and no ACL
        boolean allowed = mgr.canDo(grantee, target, User.R_loginAs, false, false);

        // Assert
        assertFalse("no ACL, loginAs denied for unrelated user", allowed);
    }

    @Test
    public void canDoAdminCredentialsNonLoginAsRightAllowedViaAdminShortcut() throws Exception {
        // Arrange — a global-admin grantee and a separate target; use a non-loginAs admin right so
        // step 2 (admin access check) is exercised.
        Map<String, Object> adminAttrs = new HashMap<>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = createAccount("doadmin@example.com", adminAttrs);
        Account target = createAccount("doadmintarget@example.com", new HashMap<String, Object>());
        Right adminRight =
                RightManager.getInstance().getAllAdminRights().values().iterator().next();

        // Act — asAdmin=true lets the admin reach the account via the superclass check
        boolean allowed = mgr.canDo(admin, target, adminRight, true, false);

        // Assert
        assertTrue("admin credentials grant access on non-loginAs right", allowed);
    }

    @Test
    public void canAccessAccountAdminCredentialsReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> adminAttrs = new HashMap<>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = createAccount("caadmin@example.com", adminAttrs);
        Account target = createAccount("caadmintarget@example.com", new HashMap<String, Object>());

        // Act — credentials/target overload, asAdmin default true
        boolean allowed = mgr.canAccessAccount(admin, target);

        // Assert — superclass admin rule grants
        assertTrue(allowed);
    }

    @Test
    public void canAccessAccountSelfNonAdminReturnsTrueViaLoginAsSelfRule() throws Exception {
        // Arrange — non-admin account accessing itself
        Account self = createAccount("caself@example.com", new HashMap<String, Object>());

        // Act — superclass denies self for non-admin, but canDo self-rule allows loginAs
        boolean allowed = mgr.canAccessAccount(self, self, false);

        // Assert
        assertTrue("self can access self via loginAs self-rule", allowed);
    }

    @Test
    public void canAccessAccountUnrelatedNonAdminReturnsFalse() throws Exception {
        // Arrange — two unrelated non-admin accounts, no ACL
        Account creds = createAccount("cacreds@example.com", new HashMap<String, Object>());
        Account target = createAccount("catarget@example.com", new HashMap<String, Object>());

        // Act — not admin, not self, no loginAs ACL
        boolean allowed = mgr.canAccessAccount(creds, target, false);

        // Assert
        assertFalse("unrelated non-admin denied", allowed);
    }

    @Test
    public void canDoEmailGranteeUnknownNoAclReturnsDefaultGrant() throws Exception {
        // Arrange — unknown email resolves to anonymous guest; target has no ACL
        Account target = createAccount("emailtarget@example.com", new HashMap<String, Object>());

        // Act — String-email overload of canDo
        boolean allowed = mgr.canDo("unknown@external.com", target, User.R_loginAs, false, true);

        // Assert — falls through to callsite default
        assertTrue("unknown email grantee honors default grant", allowed);
    }

    @Test
    public void canDoEmailGranteeResolvesExistingSelfAllowed() throws Exception {
        // Arrange — email maps to the target itself
        Account target = createAccount("emailself@example.com", new HashMap<String, Object>());

        // Act — grantee email == target name, self-rule applies
        boolean allowed = mgr.canDo("emailself@example.com", target, User.R_loginAs, false, false);

        // Assert
        assertTrue("email grantee equal to target is self-allowed", allowed);
    }

    @Test
    public void canDoNullEmailGranteeUsesAnonymousGuest() throws Exception {
        // Arrange
        Account target = createAccount("anontarget@example.com", new HashMap<String, Object>());

        // Act — null email => GuestAccount.ANONYMOUS_ACCT grantee, no ACL, default false
        boolean allowed = mgr.canDo((String) null, target, User.R_loginAs, false, false);

        // Assert
        assertFalse("anonymous guest with no ACL denied by default", allowed);
    }

    @Test
    public void canDoGuestGranteeNoAclDeniedByDefaultFalse() throws Exception {
        // Arrange — an explicit external guest grantee
        Account target = createAccount("guesttarget@example.com", new HashMap<String, Object>());
        GuestAccount guest = new GuestAccount("ext@external.com", null);

        // Act
        boolean allowed = mgr.canDo(guest, target, User.R_loginAs, false, false);

        // Assert
        assertFalse("guest grantee with no ACL denied", allowed);
    }

    private void grantLoginAs(Account target, Account grantee) throws Exception {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(grantee.getId(), GranteeType.GT_USER, User.R_loginAs, null, null));
        ACLUtil.grantRight(prov, target, aces);
    }

    @Test
    public void canDoLoginAsGrantedOnTargetReturnsTrueViaAcl() throws Exception {
        // Arrange — grant loginAs to an unrelated grantee directly on the target, so the ACL
        // check (step 3) returns a non-null TRUE rather than falling through to the default.
        Account target = createAccount("aclgranttarget@example.com", new HashMap<String, Object>());
        Account grantee = createAccount("aclgrantgrantee@example.com", new HashMap<String, Object>());
        grantLoginAs(target, grantee);

        // Act — defaultGrant=false; only the granted ACE can produce true here
        boolean allowed = mgr.canDo(grantee, target, User.R_loginAs, false, false);

        // Assert — the explicit ACL grant is honored
        assertTrue("granted loginAs ACE returns true via ACL check", allowed);
    }

    @Test
    public void canAccessAccountAuthTokenAdminReturnsTrue() throws Exception {
        // Arrange — an admin auth token; the superclass admin rule (asAdmin && at.isAdmin) grants
        Map<String, Object> adminAttrs = new HashMap<>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = createAccount("atadmin@example.com", adminAttrs);
        Account target = createAccount("atadmintarget@example.com", new HashMap<String, Object>());
        AuthToken at = new ZimbraAuthToken(admin, true, null);

        // Act — 3-arg AuthToken overload
        boolean allowed = mgr.canAccessAccount(at, target, true);

        // Assert
        assertTrue("admin auth token grants account access", allowed);
    }

    @Test
    public void canAccessAccountAuthTokenTwoArgDelegatesToAsAdmin() throws Exception {
        // Arrange — 2-arg overload defaults asAdmin=true, so an admin token still grants
        Map<String, Object> adminAttrs = new HashMap<>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = createAccount("at2admin@example.com", adminAttrs);
        Account target = createAccount("at2target@example.com", new HashMap<String, Object>());
        AuthToken at = new ZimbraAuthToken(admin, true, null);

        // Act — 2-arg AuthToken overload
        boolean allowed = mgr.canAccessAccount(at, target);

        // Assert
        assertTrue("2-arg auth token overload delegates with asAdmin=true", allowed);
    }

    @Test
    public void canAccessAccountAuthTokenNonAdminSelfReturnsTrueViaLoginAsSelfRule() throws Exception {
        // Arrange — a non-admin auth token accessing its own account; superclass denies the
        // non-admin, but the loginAs self-rule in canDo allows it.
        Account self = createAccount("atself@example.com", new HashMap<String, Object>());
        AuthToken at = new ZimbraAuthToken(self, false, null);

        // Act
        boolean allowed = mgr.canAccessAccount(at, self, false);

        // Assert
        assertTrue("non-admin auth token can access self via loginAs", allowed);
    }

    @Test
    public void canDoAuthTokenZimbraUserGrantedLoginAsReturnsTrue() throws Exception {
        // Arrange — AuthToken overload resolves a real zimbra user via Provisioning.get(id),
        // then the granted loginAs ACE drives a true result.
        Account target = createAccount("attokentarget@example.com", new HashMap<String, Object>());
        Account grantee = createAccount("attokengrantee@example.com", new HashMap<String, Object>());
        grantLoginAs(target, grantee);
        AuthToken at = new ZimbraAuthToken(grantee, false, null);

        // Act — AuthToken canDo overload
        boolean allowed = mgr.canDo(at, target, User.R_loginAs, false, false);

        // Assert
        assertTrue("auth token zimbra-user grantee with granted ACE is allowed", allowed);
    }

    @Test
    public void canDoAuthTokenNoAclReturnsDefaultGrant() throws Exception {
        // Arrange — AuthToken grantee, no ACL; falls through to the callsite default
        Account target = createAccount("attokendef@example.com", new HashMap<String, Object>());
        Account grantee = createAccount("attokendefg@example.com", new HashMap<String, Object>());
        AuthToken at = new ZimbraAuthToken(grantee, false, null);

        // Act
        boolean allowedTrue = mgr.canDo(at, target, User.R_loginAs, false, true);
        boolean allowedFalse = mgr.canDo(at, target, User.R_loginAs, false, false);

        // Assert
        assertTrue("auth token grantee honors default grant true", allowedTrue);
        assertFalse("auth token grantee honors default grant false", allowedFalse);
    }

    @Test
    public void canDoAuthTokenNullGranteeUsesAnonymousGuest() throws Exception {
        // Arrange — null AuthToken grantee becomes the anonymous guest
        Account target = createAccount("atnulltarget@example.com", new HashMap<String, Object>());

        // Act — AuthToken canDo overload with null token
        boolean allowed = mgr.canDo((AuthToken) null, target, User.R_loginAs, false, false);

        // Assert — anonymous guest, no ACL, default false
        assertFalse("null auth token grantee defaults to anonymous and is denied", allowed);
    }

    @Test
    public void canDoAdminRightGranteeNoDomainSwallowsExceptionReturnsFalse() throws Exception {
        // Arrange — a non-admin grantee with no domain asking for an ADMIN preset right. The admin
        // short-circuit (step 2) fails (not admin), so CheckPresetRight runs; for an admin right
        // the grantee must resolve a domain, which it cannot, so a ServiceException is thrown and
        // swallowed by the catch block, yielding false.
        Account grantee = createAccount("noDomGrantee@example.com", new HashMap<String, Object>());
        Account target = createAccount("noDomTarget@example.com", new HashMap<String, Object>());
        Right adminRight = null;
        for (Right r : RightManager.getInstance().getAllAdminRights().values()) {
            if (r.isPresetRight()) {
                adminRight = r;
                break;
            }
        }
        assertNotNull("need a preset admin right for this path", adminRight);

        // Act — exception inside canDo is caught and turned into a denial
        boolean allowed = mgr.canDo(grantee, target, adminRight, false, false);

        // Assert
        assertFalse("admin-right check failure is swallowed and denied", allowed);
    }
}
