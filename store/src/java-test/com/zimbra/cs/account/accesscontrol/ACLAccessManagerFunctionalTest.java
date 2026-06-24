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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccessManager.AttrRightChecker;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link ACLAccessManager}. The manager's constructor initializes the real
 * {@link RightManager}; tests then drive its synchronous, non-LDAP decision methods against real
 * {@link Account} objects from the in-memory {@link com.zimbra.cs.account.MockProvisioning}
 * harness. Several override methods are intentional internal-error sentinels and are verified to
 * throw.
 */
public class ACLAccessManagerFunctionalTest {

    private static ACLAccessManager mgr;

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        // RightManager must be initialized for admin-right wiring used by the manager.
        RightManager.getInstance().getAllAdminRights();
        mgr = new ACLAccessManager();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        if (prov.get(DomainBy.name, "example.com") == null) {
            prov.createDomain("example.com", new HashMap<String, Object>());
        }
        prov.createAccount("plain@example.com", "secret", new HashMap<String, Object>());
    }

    @Test
    public void ctorInitializesRightManagerSucceeds() throws Exception {
        // Act — constructing the manager triggers RightManager.getInstance().
        ACLAccessManager local = new ACLAccessManager();

        // Assert — a usable instance is produced.
        assertNotNull(local);
    }

    @Test
    public void isAdequateAdminAccountPlainAccountFalse() throws Exception {
        // Arrange — a plain account has neither admin flag.
        Account plain = prov.get(AccountBy.name, "plain@example.com");

        // Act / Assert
        assertFalse(mgr.isAdequateAdminAccount(plain));
    }

    @Test
    public void isAdequateAdminAccountFullAdminTrue() throws Exception {
        // Arrange — set the full-admin flag.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        prov.createAccount("admin@example.com", "secret", attrs);
        Account admin = prov.get(AccountBy.name, "admin@example.com");

        // Act / Assert
        assertTrue(mgr.isAdequateAdminAccount(admin));

        // Cleanup the extra account.
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void isAdequateAdminAccountDelegatedAdminTrue() throws Exception {
        // Arrange — set only the delegated-admin flag.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE");
        prov.createAccount("deleg@example.com", "secret", attrs);
        Account deleg = prov.get(AccountBy.name, "deleg@example.com");

        // Act / Assert
        assertTrue(mgr.isAdequateAdminAccount(deleg));

        // Cleanup.
        prov.deleteAccount(deleg.getId());
    }

    @Test
    public void isDomainAdminOnlyAnyAuthTokenAlwaysFalse() throws Exception {
        // Arrange — ACL world has no concept of domain-admin-only; result is constant false.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at =
                new com.zimbra.cs.account.ZimbraAuthToken(plain);

        // Act / Assert
        assertFalse(mgr.isDomainAdminOnly(at));
    }

    @Test
    public void canAccessCosAnyCosAlwaysFalse() throws Exception {
        // Arrange
        Cos cos = prov.createCos("aclcos", new HashMap<String, Object>());
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at =
                new com.zimbra.cs.account.ZimbraAuthToken(plain);

        // Act / Assert — COS access is never granted by this manager.
        assertFalse(mgr.canAccessCos(at, cos));
    }

    @Test
    public void canAccessDomainByNameThrowsInternalError() throws Exception {
        // Arrange
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at =
                new com.zimbra.cs.account.ZimbraAuthToken(plain);

        // Act / Assert — this override is a should-never-be-called sentinel.
        try {
            mgr.canAccessDomain(at, "example.com");
            fail("expected internal-error ServiceException");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("internal error"));
        }
    }

    @Test
    public void canAccessDomainByDomainThrowsInternalError() throws Exception {
        // Arrange
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at =
                new com.zimbra.cs.account.ZimbraAuthToken(plain);
        com.zimbra.cs.account.Domain domain = prov.get(DomainBy.name, "example.com");

        // Act / Assert
        try {
            mgr.canAccessDomain(at, domain);
            fail("expected internal-error ServiceException");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("internal error"));
        }
    }

    @Test
    public void canAccessEmailAnyEmailThrowsInternalError() throws Exception {
        // Arrange
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at =
                new com.zimbra.cs.account.ZimbraAuthToken(plain);

        // Act / Assert
        try {
            mgr.canAccessEmail(at, "x@example.com");
            fail("expected internal-error ServiceException");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("internal error"));
        }
    }

    @Test
    public void targetTypesForGrantSearchReturnsAllTargetTypes() throws Exception {
        // Act — the manager searches grants across every target type.
        Set<TargetType> types = mgr.targetTypesForGrantSearch();

        // Assert — the full enum is returned.
        Set<TargetType> expected = new HashSet<TargetType>(Arrays.asList(TargetType.values()));
        assertEquals(expected, types);
        assertTrue(types.contains(TargetType.account));
        assertTrue(types.contains(TargetType.global));
    }

    // ---- helpers ----

    /**
     * Minimal concrete {@link DistributionList}. {@code DistributionList} is abstract and every
     * group-creation method on the in-memory {@code MockProvisioning} throws
     * {@code UnsupportedOperationException}, so tests build a real group object directly. The
     * "grp@example.com" name resolves to the real (active) "example.com" domain created in
     * {@code setUp}, so {@code checkDomainStatus} takes its normal pass-through branch.
     */
    private static final class TestDL extends DistributionList {
        TestDL(String name, String id, Map<String, Object> attrs, Provisioning prov) {
            super(name, id, attrs, prov);
        }
    }

    private Account globalAdmin() throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        prov.createAccount("gadmin@example.com", "secret", attrs);
        return prov.get(AccountBy.name, "gadmin@example.com");
    }

    private Account delegatedAdmin(String email) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE");
        prov.createAccount(email, "secret", attrs);
        return prov.get(AccountBy.name, email);
    }

    // Creates (idempotently) a domain whose status is suspended and an account inside it.
    private Account accountInSuspendedDomain(String domainName, String email) throws Exception {
        if (prov.get(DomainBy.name, domainName) == null) {
            Map<String, Object> dattrs = new HashMap<String, Object>();
            dattrs.put(Provisioning.A_zimbraDomainStatus, "suspended");
            prov.createDomain(domainName, dattrs);
        }
        prov.createAccount(email, "secret", new HashMap<String, Object>());
        return prov.get(AccountBy.name, email);
    }

    // ---- canDo: user-right entrances (do-not-throw wrappers) ----

    @Test
    public void canDoAuthTokenGlobalAdminAdminRightTrue() throws Exception {
        // Arrange — a global admin grantee always passes hard rules for an admin right.
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(admin);

        // Act — AuthToken entrance, asAdmin=true, an admin preset right.
        boolean allowed = mgr.canDo(at, target, Rights.Admin.R_adminLoginAs, true);

        // Assert
        assertTrue("global admin can do admin right via hard rules", allowed);

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void canDoAccountGlobalAdminAdminRightTrue() throws Exception {
        // Arrange
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");

        // Act — MailTarget (Account) entrance.
        boolean allowed = mgr.canDo((com.zimbra.cs.account.MailTarget) admin, target,
                Rights.Admin.R_adminLoginAs, true);

        // Assert
        assertTrue(allowed);

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void canDoEmailGlobalAdminAdminRightTrue() throws Exception {
        // Arrange — email-addressed grantee resolves to the admin account.
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");

        // Act — String (email) entrance.
        boolean allowed = mgr.canDo("gadmin@example.com", target, Rights.Admin.R_adminLoginAs, true);

        // Assert
        assertTrue(allowed);

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void canDoNonAdminAccountAdminRightFalse() throws Exception {
        // Arrange — a plain account fails the delegated-admin hard rule, which throws
        // PERM_DENIED internally; the user-right entrance swallows it and returns false.
        Account target = prov.get(AccountBy.name, "plain@example.com");

        // Act
        boolean allowed = mgr.canDo((com.zimbra.cs.account.MailTarget) target, target,
                Rights.Admin.R_adminLoginAs, true);

        // Assert
        assertFalse("plain account is not an eligible admin", allowed);
    }

    @Test
    public void canDoPseudoAlwaysAllowTrue() throws Exception {
        // Arrange — PR_ALWAYS_ALLOW short-circuits to true for an admin grantee.
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");

        // Act — call the 5-arg variant directly to reach the pseudo-right branch.
        boolean allowed = mgr.canDo((com.zimbra.cs.account.MailTarget) admin, target,
                AdminRight.PR_ALWAYS_ALLOW, true, null);

        // Assert
        assertTrue(allowed);

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void canDoEmailUnknownUserRightFalse() throws Exception {
        // Arrange — an unknown email with a user right becomes a guest, which is denied.
        Account target = prov.get(AccountBy.name, "plain@example.com");

        // Act — String entrance with a user right.
        boolean allowed = mgr.canDo("nobody@nowhere.invalid", target, Rights.User.R_loginAs, false);

        // Assert — guest cannot loginAs another account.
        assertFalse(allowed);
    }

    // ---- canAccessAccount variants ----

    @Test
    public void canAccessAccountGlobalAdminAsAdminTrue() throws Exception {
        // Arrange
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(admin);

        // Act — admin login-as path (asAdmin=true).
        boolean allowed = mgr.canAccessAccount(at, target, true);

        // Assert
        assertTrue("global admin can access any account as admin", allowed);

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void canAccessAccountTwoArgAuthTokenDelegatesToAsAdmin() throws Exception {
        // Arrange
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(admin);

        // Act — 2-arg overload defaults asAdmin=true.
        boolean allowed = mgr.canAccessAccount(at, target);

        // Assert
        assertTrue(allowed);

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void canAccessAccountAccountCredentialsAsAdminTrue() throws Exception {
        // Arrange
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");

        // Act — Account-credentials overload.
        boolean allowed = mgr.canAccessAccount(admin, target, true);

        // Assert
        assertTrue(allowed);

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void canAccessAccountTwoArgAccountDelegatesToAsAdmin() throws Exception {
        // Arrange
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");

        // Act — 2-arg Account overload.
        boolean allowed = mgr.canAccessAccount(admin, target);

        // Assert
        assertTrue(allowed);

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void canAccessAccountSelfUserRightTrue() throws Exception {
        // Arrange — a non-admin accessing itself for a non-loginAs user right is allowed.
        Account self = prov.get(AccountBy.name, "plain@example.com");

        // Act — asAdmin=false drives the user-right branch; self always passes.
        boolean allowed = mgr.canAccessAccount(self, self, false);

        // Assert
        assertTrue("self access for a user right is always allowed", allowed);
    }

    // ---- canGetAttrs / canSetAttrs / getGetAttrsChecker (global admin -> hard-rules TRUE) ----

    @Test
    public void canGetAttrsGlobalAdminAccountTrue() throws Exception {
        // Arrange
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");
        Set<String> attrs = new HashSet<String>(Arrays.asList(Provisioning.A_displayName));

        // Act — Account overload.
        boolean allowed = mgr.canGetAttrs(admin, target, attrs, true);

        // Assert
        assertTrue("global admin can get any attrs", allowed);

        // Also the AuthToken overload delegates to the same path.
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(admin);
        assertTrue(mgr.canGetAttrs(at, target, attrs, true));

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void canSetAttrsGlobalAdminSetBasedTrue() throws Exception {
        // Arrange
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");
        Set<String> attrs = new HashSet<String>(Arrays.asList(Provisioning.A_displayName));

        // Act — Set<String> overload + AuthToken overload.
        boolean allowedAcct = mgr.canSetAttrs(admin, target, attrs, true);
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(admin);
        boolean allowedTok = mgr.canSetAttrs(at, target, attrs, true);

        // Assert
        assertTrue(allowedAcct);
        assertTrue(allowedTok);

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void canSetAttrsGlobalAdminMapBasedTrue() throws Exception {
        // Arrange
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "New Name");

        // Act — Map overload (constraint-checking) + AuthToken Map overload.
        boolean allowedAcct = mgr.canSetAttrs(admin, target, attrs, true);
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(admin);
        boolean allowedTok = mgr.canSetAttrs(at, target, attrs, true);

        // Assert
        assertTrue(allowedAcct);
        assertTrue(allowedTok);

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void getGetAttrsCheckerGlobalAdminAllowsAll() throws Exception {
        // Arrange
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");

        // Act — Account overload returns the ALLOW_ALL checker for a global admin.
        AttrRightChecker checker = mgr.getGetAttrsChecker(admin, target, true);

        // Assert — allow-all means any attr is gettable.
        assertNotNull(checker);
        assertTrue(checker.allowAttr(Provisioning.A_displayName));

        // AuthToken overload delegates to the same path.
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(admin);
        AttrRightChecker checker2 = mgr.getGetAttrsChecker(at, target, true);
        assertTrue(checker2.allowAttr(Provisioning.A_zimbraId));

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void getGetAttrsCheckerNonAdminThrowsPermDenied() throws Exception {
        // Arrange — a plain account is neither a global nor a delegated admin. With asAdmin=true
        // and an implicit admin right (null right), HardRules.checkHardRules throws PERM_DENIED
        // ("not an eligible admin account") rather than returning a deny-all checker.
        Account plain = prov.get(AccountBy.name, "plain@example.com");

        // Act / Assert
        try {
            mgr.getGetAttrsChecker(plain, plain, true);
            fail("expected PERM_DENIED for a non-admin account");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertTrue(e.getMessage().contains("not an eligible admin account"));
        }
    }

    // ---- canPerform (global admin -> hard-rules TRUE) ----

    @Test
    public void canPerformGlobalAdminPresetRightTrue() throws Exception {
        // Arrange
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");

        // Act — MailTarget overload reaches hard-rules TRUE.
        boolean allowed = mgr.canPerform((com.zimbra.cs.account.MailTarget) admin, target,
                Rights.Admin.R_adminLoginAs, false, null, true, null);

        // Assert
        assertTrue(allowed);

        // AuthToken overload delegates to the Account path.
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(admin);
        assertTrue(mgr.canPerform(at, target, Rights.Admin.R_adminLoginAs, false, null, true, null));

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    // ---- canModifyMailQuota ----

    @Test
    public void canModifyMailQuotaGlobalAdminTrue() throws Exception {
        // Arrange — an admin token may set any quota. canModifyMailQuota delegates to
        // DomainAccessManager.canSetMailQuota, which keys off AuthToken.isAdmin() (the token's
        // admin flag set at construction), not the account's zimbraIsAdminAccount attr — so the
        // token must be built with isAdmin=true.
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at =
                new com.zimbra.cs.account.ZimbraAuthToken(admin, true, null);

        // Act / Assert
        assertTrue(at.isAdmin());
        assertTrue(mgr.canModifyMailQuota(at, target, 1000L));

        // Cleanup
        prov.deleteAccount(admin.getId());
    }

    @Test
    public void canModifyMailQuotaNonAdminNoMaxQuotaFalse() throws Exception {
        // Arrange — a plain account with no zimbraDomainAdminMaxMailQuota cannot set quotas.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(plain);

        // Act / Assert
        assertFalse(mgr.canModifyMailQuota(at, plain, 1000L));
    }

    // ---- canSetAttrsOnCreate ----

    @Test
    public void canSetAttrsOnCreateInvalidEmailThrowsInvalidRequest() throws Exception {
        // Arrange — account target type is domained, so a non-email entry name is rejected.
        Account admin = globalAdmin();
        Map<String, Object> attrs = new HashMap<String, Object>();

        // Act / Assert
        try {
            mgr.canSetAttrsOnCreate(admin, TargetType.account, "not-an-email", attrs, true);
            fail("expected INVALID_REQUEST for non-email entry name");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        } finally {
            prov.deleteAccount(admin.getId());
        }
    }

    // ---- canAccessGroup / canCreateGroup ----

    @Test
    public void canAccessGroupAuthTokenNonOwnerFalse() throws Exception {
        // Arrange — a plain account is not an owner of the group, and not an admin.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(plain);
        Group group = new TestDL("grp@example.com", "grp-id-0001",
                new HashMap<String, Object>(), prov);

        // Act — owner-right check; not an owner => denied (canDo swallows and returns false).
        boolean allowed = mgr.canAccessGroup(at, group);

        // Assert
        assertFalse(allowed);
    }

    @Test
    public void canAccessGroupAccountCredentialsNonOwnerFalse() throws Exception {
        // Arrange
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        Group group = new TestDL("grp2@example.com", "grp-id-0002",
                new HashMap<String, Object>(), prov);

        // Act — Account-credentials overload.
        boolean allowed = mgr.canAccessGroup(plain, group, true);

        // Assert
        assertFalse(allowed);
    }

    // ==================================================================
    // FALSE-path coverage. The existing tests above only assert the
    // global-admin TRUE branch; PIT's BooleanTrueReturnVals survive
    // because nothing observes a FALSE return. These pin the deny side.
    // ==================================================================

    // ---- canAccessAccount: non-admin must be DENIED (L101,110,124,127,134) ----

    @Test
    public void canAccessAccountAccountCredentialsNonAdminAsAdminFalse() throws Exception {
        // A plain account is not a delegated admin; HardRules throws PERM_DENIED which the
        // user-right entrance swallows -> the 3-arg Account overload (L124/L127) returns FALSE.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        prov.createAccount("victim@example.com", "secret", new HashMap<String, Object>());
        Account victim = prov.get(AccountBy.name, "victim@example.com");

        assertFalse("plain account cannot access another account as admin",
                mgr.canAccessAccount(plain, victim, true));

        prov.deleteAccount(victim.getId());
    }

    @Test
    public void canAccessAccountTwoArgAccountNonAdminFalse() throws Exception {
        // The 2-arg Account overload (L134) delegates with asAdmin=true; still a deny for a plain
        // grantee. BooleanTrueReturnVals on L134 would force TRUE and fail here.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        prov.createAccount("victim2@example.com", "secret", new HashMap<String, Object>());
        Account victim = prov.get(AccountBy.name, "victim2@example.com");

        assertFalse("2-arg Account overload must deny a non-admin", mgr.canAccessAccount(plain, victim));

        prov.deleteAccount(victim.getId());
    }

    @Test
    public void canAccessAccountAuthTokenNonAdminAsAdminFalse() throws Exception {
        // AuthToken 3-arg overload (L101) returns canDo's result; for a plain token that is FALSE.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        prov.createAccount("victim3@example.com", "secret", new HashMap<String, Object>());
        Account victim = prov.get(AccountBy.name, "victim3@example.com");
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(plain);

        assertFalse(mgr.canAccessAccount(at, victim, true));

        prov.deleteAccount(victim.getId());
    }

    @Test
    public void canAccessAccountTwoArgAuthTokenNonAdminFalse() throws Exception {
        // 2-arg AuthToken overload (L110) -> asAdmin=true -> deny for a plain token.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        prov.createAccount("victim4@example.com", "secret", new HashMap<String, Object>());
        Account victim = prov.get(AccountBy.name, "victim4@example.com");
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(plain);

        assertFalse(mgr.canAccessAccount(at, victim));

        prov.deleteAccount(victim.getId());
    }

    @Test
    public void canAccessAccountSelfUserRightDistinctFromOtherUserRight() throws Exception {
        // asAdmin=false drives the user-right branch (L100/L104, L127). Self is allowed (preset-right
        // self short-circuit), but accessing a DIFFERENT account for the user loginAs right is denied.
        // This pins the asAdmin=false path returning BOTH true (self) and false (other), so neither
        // NegateConditionals on L100/L119 nor BooleanTrueReturnVals can pass undetected.
        Account self = prov.get(AccountBy.name, "plain@example.com");
        prov.createAccount("other@example.com", "secret", new HashMap<String, Object>());
        Account other = prov.get(AccountBy.name, "other@example.com");

        assertTrue("self access for a user right is allowed", mgr.canAccessAccount(self, self, false));
        assertFalse("non-admin cannot loginAs a different account", mgr.canAccessAccount(self, other, false));

        prov.deleteAccount(other.getId());
    }

    @Test
    public void canAccessAccountTargetInSuspendedDifferentDomainThrowsPermDenied() throws Exception {
        // grantee is in example.com, target is in a DIFFERENT, suspended domain. The L92 guard
        // (grantee domain != target domain) is TRUE, so checkDomainStatus(target) runs and throws.
        // Negating L92 would skip the status check and not throw.
        Account grantee = prov.get(AccountBy.name, "plain@example.com");
        Account target = accountInSuspendedDomain("suspended.example", "u@suspended.example");
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(grantee);

        try {
            mgr.canAccessAccount(at, target, true);
            fail("expected PERM_DENIED because target's domain is suspended");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertTrue(e.getMessage().contains("suspended"));
        } finally {
            prov.deleteAccount(target.getId());
        }
    }

    @Test
    public void canAccessAccountAccountCredentialsSuspendedDomainThrowsPermDenied() throws Exception {
        // The Account-credentials overload (L117) ALWAYS calls checkDomainStatus(target),
        // unconditionally. VoidMethodCall removing that call would let it proceed and (for a
        // global admin) return TRUE instead of throwing.
        Account admin = globalAdmin();
        Account target = accountInSuspendedDomain("suspended2.example", "v@suspended2.example");

        try {
            mgr.canAccessAccount(admin, target, true);
            fail("expected PERM_DENIED from the unconditional domain-status check");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        } finally {
            prov.deleteAccount(target.getId());
            prov.deleteAccount(admin.getId());
        }
    }

    // ---- canGetAttrs / canSetAttrs: non-admin DENY (L302,311,345,354,365,376) ----

    @Test
    public void canGetAttrsNonAdminAccountFalse() throws Exception {
        // For a plain grantee + asAdmin=true + implicit admin right (null), HardRules throws
        // PERM_DENIED. canGetAttrs has no swallow, so it propagates. The Account overload (L302)
        // never reaches a TRUE return for a non-admin.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        Set<String> attrs = new HashSet<String>(Arrays.asList(Provisioning.A_displayName));

        try {
            mgr.canGetAttrs(plain, plain, attrs, true);
            fail("expected PERM_DENIED for a non-admin canGetAttrs");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canSetAttrsNonAdminSetBasedThrowsPermDenied() throws Exception {
        // Set<String> overload (L345/L354) for a non-admin -> PERM_DENIED, never TRUE.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        Set<String> attrs = new HashSet<String>(Arrays.asList(Provisioning.A_displayName));

        try {
            mgr.canSetAttrs(plain, plain, attrs, true);
            fail("expected PERM_DENIED for non-admin canSetAttrs(Set)");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canSetAttrsNonAdminMapBasedThrowsPermDenied() throws Exception {
        // Map overload (L365/L376) for a non-admin -> PERM_DENIED, never TRUE.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_displayName, "X");

        try {
            mgr.canSetAttrs(plain, plain, attrs, true);
            fail("expected PERM_DENIED for non-admin canSetAttrs(Map)");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void getGetAttrsCheckerGlobalAdminIsNotDenyAll() throws Exception {
        // hardRulesResult==TRUE returns ALLOW_ALL; the FALSE branch would return DENY_ALL. Pin that
        // the global-admin checker is the ALLOW_ALL one by checking a second arbitrary attr too.
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");

        AttrRightChecker checker = mgr.getGetAttrsChecker(admin, target, true);
        assertTrue(checker.allowAttr(Provisioning.A_displayName));
        assertTrue("ALLOW_ALL must allow every attr, not just one", checker.allowAttr(Provisioning.A_zimbraId));

        prov.deleteAccount(admin.getId());
    }

    // ---- canPerform: non-admin preset right DENY (L423,551) ----

    @Test
    public void canPerformNonAdminPresetRightThrowsPermDenied() throws Exception {
        // MailTarget overload (L423): a plain grantee fails HardRules for an admin preset right.
        // BooleanTrueReturnVals on L423 would force TRUE; instead the call must throw PERM_DENIED.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        Account target = prov.get(AccountBy.name, "plain@example.com");

        try {
            mgr.canPerform((com.zimbra.cs.account.MailTarget) plain, target,
                    Rights.Admin.R_adminLoginAs, false, null, true, null);
            fail("expected PERM_DENIED for a non-admin canPerform");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canPerformAuthTokenNonAdminThrowsPermDenied() throws Exception {
        // AuthToken overload (L551) delegates to the Account path; same deny for a plain token.
        Account plain = prov.get(AccountBy.name, "plain@example.com");
        Account target = prov.get(AccountBy.name, "plain@example.com");
        com.zimbra.cs.account.AuthToken at = new com.zimbra.cs.account.ZimbraAuthToken(plain);

        try {
            mgr.canPerform(at, target, Rights.Admin.R_adminLoginAs, false, null, true, null);
            fail("expected PERM_DENIED for a non-admin canPerform via AuthToken");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    // ---- canDo pseudo rights (L254,257) ----

    @Test
    public void canDoGlobalAdminSystemAdminOnlyTrueButDelegatedDenied() throws Exception {
        // PR_SYSTEM_ADMIN_ONLY (L257) returns false in the pseudo-right block, which is reached only
        // when hard rules return null (a delegated admin). A global admin short-circuits to TRUE in
        // hard rules. Contrasting the two pins both the asAdmin guard (L254) and the L257 return.
        Account gadmin = globalAdmin();
        Account dadmin = delegatedAdmin("dadmin@example.com");
        Account target = prov.get(AccountBy.name, "plain@example.com");

        // global admin: hard-rules TRUE, never reaches the pseudo block
        assertTrue("global admin allowed for system-admin-only pseudo right",
                mgr.canDo((com.zimbra.cs.account.MailTarget) gadmin, target,
                        AdminRight.PR_SYSTEM_ADMIN_ONLY, true, null));

        // delegated admin: hard-rules null -> pseudo block -> PR_SYSTEM_ADMIN_ONLY returns false
        assertFalse("delegated admin denied for system-admin-only pseudo right",
                mgr.canDo((com.zimbra.cs.account.MailTarget) dadmin, target,
                        AdminRight.PR_SYSTEM_ADMIN_ONLY, true, null));

        prov.deleteAccount(gadmin.getId());
        prov.deleteAccount(dadmin.getId());
    }

    @Test
    public void canDoDelegatedAdminAlwaysAllowPseudoTrue() throws Exception {
        // For a delegated admin (hard-rules null), PR_ALWAYS_ALLOW (L255) returns true while
        // PR_SYSTEM_ADMIN_ONLY returns false — proving the two pseudo branches are distinct and
        // the asAdmin guard (L254) is taken.
        Account dadmin = delegatedAdmin("dadmin2@example.com");
        Account target = prov.get(AccountBy.name, "plain@example.com");

        assertTrue("PR_ALWAYS_ALLOW grants a delegated admin",
                mgr.canDo((com.zimbra.cs.account.MailTarget) dadmin, target,
                        AdminRight.PR_ALWAYS_ALLOW, true, null));
        assertFalse("PR_SYSTEM_ADMIN_ONLY denies a delegated admin",
                mgr.canDo((com.zimbra.cs.account.MailTarget) dadmin, target,
                        AdminRight.PR_SYSTEM_ADMIN_ONLY, true, null));

        prov.deleteAccount(dadmin.getId());
    }

    // ---- canDo email entrance (L284,285) ----

    @Test
    public void canDoEmailResolvesToAdminTrueVsUnknownEmailGuestFalse() throws Exception {
        // L285 guards "grantee != null": a resolvable admin email proceeds to canDo (TRUE for an
        // admin right), while an unknown email for an admin right yields a null grantee -> FALSE.
        Account admin = globalAdmin();
        Account target = prov.get(AccountBy.name, "plain@example.com");

        assertTrue("known admin email is allowed an admin right",
                mgr.canDo("gadmin@example.com", target, Rights.Admin.R_adminLoginAs, true));
        assertFalse("unknown email cannot be granted an admin right",
                mgr.canDo("nobody@nowhere.invalid", target, Rights.Admin.R_adminLoginAs, true));

        prov.deleteAccount(admin.getId());
    }

    // ---- canCreateGroup deny path drives canAccessGroup-adjacent logic ----

    @Test
    public void canDoSelfUserRightTrueDistinctFromNonSelf() throws Exception {
        // checkPresetRight user-right self short-circuit (canDo through the 5-arg path). Self gets
        // TRUE; a different non-admin target for a non-loginAs user right falls through to FALSE.
        Account self = prov.get(AccountBy.name, "plain@example.com");
        prov.createAccount("peer@example.com", "secret", new HashMap<String, Object>());
        Account peer = prov.get(AccountBy.name, "peer@example.com");

        assertTrue("self is always allowed a user right",
                mgr.canDo((com.zimbra.cs.account.MailTarget) self, self, Rights.User.R_viewFreeBusy, false, null));
        // viewFreeBusy defaults to allow, so a peer is allowed by default; loginAs is not self and
        // has no allow-default -> denied. Use loginAs to get a clean FALSE for a non-self target.
        assertFalse("non-self loginAs is denied for a plain grantee",
                mgr.canDo((com.zimbra.cs.account.MailTarget) self, peer, Rights.User.R_loginAs, false, null));

        prov.deleteAccount(peer.getId());
    }
}
