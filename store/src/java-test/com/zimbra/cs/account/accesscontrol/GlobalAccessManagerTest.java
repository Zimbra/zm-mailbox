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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.accesscontrol.AllowedAttrs.Result;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link GlobalAccessManager}. Real admin and non-admin {@link Account}s are
 * created through the in-memory {@link Provisioning} harness, wrapped in real
 * {@link ZimbraAuthToken}s, and run through the manager's authorization surface: admin-account
 * adequacy, global-admin short-circuits on attr get/set and mail-quota checks, the get-attrs
 * checker result (ALLOW_ALL vs DENY_ALL), the always-false domain-admin flag, and the fixed set of
 * grant-search target types. Both the admin (allow) and non-admin (deny) paths are covered.
 */
public class GlobalAccessManagerTest {

    private static Provisioning prov;

    private GlobalAccessManager mgr;

    private Account adminAcct;

    private Account userAcct;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
    }

    @Before
    public void setUp() throws Exception {
        mgr = new GlobalAccessManager();

        // Global admin account (no domain part -> avoids domain-status lookups).
        // Assign an explicit, unique zimbraId so the by-id lookup in AuthToken.getAccount()
        // resolves back to THIS admin account. The in-memory provisioning stub otherwise
        // assigns the same well-known default id to every account, so the second account
        // created would shadow the first under that id.
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        adminAttrs.put(Provisioning.A_zimbraId, "11111111-1111-1111-1111-111111111111");
        adminAcct = prov.createAccount("gamadmin", "test123", adminAttrs);

        // Plain non-admin account, also with a distinct explicit id.
        Map<String, Object> userAttrs = new HashMap<String, Object>();
        userAttrs.put(Provisioning.A_zimbraId, "22222222-2222-2222-2222-222222222222");
        userAcct = prov.createAccount("gamuser", "test123", userAttrs);
    }

    @Test
    public void isAdequateAdminAccountAdminAccountReturnsTrue() {
        // Act / Assert
        assertTrue("admin account must be adequate", mgr.isAdequateAdminAccount(adminAcct));
    }

    @Test
    public void isAdequateAdminAccountNonAdminAccountReturnsFalse() {
        // Act / Assert
        assertFalse("non-admin account must not be adequate", mgr.isAdequateAdminAccount(userAcct));
    }

    @Test
    public void isDomainAdminOnlyAnyAuthTokenReturnsFalse() throws Exception {
        // Arrange - global access manager never reports domain-admin-only
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act / Assert
        assertFalse(mgr.isDomainAdminOnly(at));
    }

    @Test
    public void canGetAttrsGlobalAdminCredentialsReturnsTrue() throws Exception {
        // Arrange
        Set<String> attrs = new HashSet<String>();
        attrs.add("displayName");

        // Act / Assert - global admin can get any attrs
        assertTrue(mgr.canGetAttrs(adminAcct, userAcct, attrs, true));
    }

    @Test
    public void canGetAttrsNonAdminCredentialsReturnsFalse() throws Exception {
        // Arrange
        Set<String> attrs = new HashSet<String>();
        attrs.add("displayName");

        // Act / Assert - non-admin is denied at the global manager
        assertFalse(mgr.canGetAttrs(userAcct, adminAcct, attrs, true));
    }

    @Test
    public void canSetAttrsGlobalAdminWithMapReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("displayName", "X");

        // Act / Assert
        assertTrue(mgr.canSetAttrs(adminAcct, userAcct, attrs, true));
    }

    @Test
    public void canSetAttrsNonAdminWithSetReturnsFalse() throws Exception {
        // Arrange
        Set<String> attrs = new HashSet<String>();
        attrs.add("displayName");

        // Act / Assert
        assertFalse(mgr.canSetAttrs(userAcct, adminAcct, attrs, true));
    }

    @Test
    public void getGetAttrsCheckerGlobalAdminReturnsAllowAll() throws Exception {
        // Act
        AllowedAttrs checker = (AllowedAttrs) mgr.getGetAttrsChecker(adminAcct, userAcct, true);

        // Assert - admin sees ALLOW_ALL and the checker allows arbitrary attrs
        assertEquals(Result.ALLOW_ALL, checker.getResult());
        assertTrue(checker.allowAttr("displayName"));
    }

    @Test
    public void getGetAttrsCheckerNonAdminReturnsDenyAll() throws Exception {
        // Act
        AllowedAttrs checker = (AllowedAttrs) mgr.getGetAttrsChecker(userAcct, adminAcct, true);

        // Assert - non-admin sees DENY_ALL
        assertEquals(Result.DENY_ALL, checker.getResult());
        assertFalse(checker.allowAttr("displayName"));
    }

    @Test
    public void canModifyMailQuotaAdminAuthTokenReturnsTrue() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act / Assert
        assertTrue(mgr.canModifyMailQuota(at, userAcct, 1024L));
    }

    @Test
    public void canModifyMailQuotaNonAdminAuthTokenReturnsFalse() throws Exception {
        // Arrange - token not flagged admin
        AuthToken at = new ZimbraAuthToken(userAcct, false, null);

        // Act / Assert
        assertFalse(mgr.canModifyMailQuota(at, adminAcct, 1024L));
    }

    @Test
    public void canDoAdminGranteeNonUserRightReturnsTrue() {
        // Arrange - a null/non-user right falls through to the global-admin check
        // Act / Assert - admin grantee is permitted
        assertTrue(mgr.canDo(adminAcct, userAcct, null, true));
    }

    @Test
    public void canDoNonAdminGranteeNonUserRightReturnsFalse() {
        // Act / Assert - non-admin grantee on a non-user right is denied
        assertFalse(mgr.canDo(userAcct, adminAcct, null, true));
    }

    @Test
    public void canDoNullGranteeAccountReturnsFalse() {
        // Act / Assert - null credentials are always denied
        assertFalse(mgr.canDo((Account) null, userAcct, null, true));
    }

    @Test
    public void canAccessAccountGlobalAdminCredentialsReturnsTrue() throws Exception {
        // Act / Assert — global admin credentials short-circuit to allow
        assertTrue(mgr.canAccessAccount(adminAcct, userAcct, true));
    }

    @Test
    public void canAccessAccountGlobalAdminCredentialsTwoArgReturnsTrue() throws Exception {
        // Act / Assert — two-arg convenience form defaults asAdmin=true
        assertTrue(mgr.canAccessAccount(adminAcct, userAcct));
    }

    @Test
    public void canAccessAccountNullCredentialsReturnsFalse() throws Exception {
        // Act / Assert — null credentials are denied before any lookup
        assertFalse(mgr.canAccessAccount((Account) null, userAcct, true));
    }

    @Test
    public void canAccessAccountAdminAuthTokenReturnsTrue() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act / Assert — admin auth token short-circuits to allow
        assertTrue(mgr.canAccessAccount(at, userAcct, true));
    }

    @Test
    public void canAccessAccountAdminAuthTokenTwoArgReturnsTrue() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act / Assert — two-arg form defaults asAdmin=true
        assertTrue(mgr.canAccessAccount(at, userAcct));
    }

    @Test
    public void canAccessCosAdminAuthTokenReturnsTrue() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        Cos cos = prov.createCos("gamcos", cosAttrs);

        // Act / Assert — global admin can access any COS
        assertTrue(mgr.canAccessCos(at, cos));
    }

    @Test
    public void canAccessCosNonAdminAuthTokenReturnsFalse() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(userAcct, false, null);
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        Cos cos = prov.createCos("gamcos2", cosAttrs);

        // Act / Assert — non-admin denied
        assertFalse(mgr.canAccessCos(at, cos));
    }

    @Test
    public void canAccessDomainAdminAuthTokenByNameReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        prov.createDomain("gamdomain.com", domAttrs);
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act / Assert — admin can access the domain by name
        assertTrue(mgr.canAccessDomain(at, "gamdomain.com"));
    }

    @Test
    public void canAccessDomainAdminAuthTokenByDomainReturnsTrue() throws Exception {
        // Arrange
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        Domain domain = prov.createDomain("gamdomain2.com", domAttrs);
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act / Assert — admin can access the Domain object
        assertTrue(mgr.canAccessDomain(at, domain));
    }

    @Test
    public void canAccessDomainNonAdminAuthTokenByNameReturnsFalse() throws Exception {
        // Arrange
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        prov.createDomain("gamdomain3.com", domAttrs);
        AuthToken at = new ZimbraAuthToken(userAcct, false, null);

        // Act / Assert — non-admin denied
        assertFalse(mgr.canAccessDomain(at, "gamdomain3.com"));
    }

    @Test
    public void canCreateGroupGlobalAdminCredentialsReturnsTrue() throws Exception {
        // Arrange — domain must exist for the email-addr -> domain lookup
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        prov.createDomain("groupdom.com", domAttrs);

        // Act / Assert — admin credentials short-circuit to allow
        assertTrue(mgr.canCreateGroup(adminAcct, "newgroup@groupdom.com"));
    }

    @Test
    public void canCreateGroupUnknownDomainThrowsNoSuchDomain() throws Exception {
        // Act / Assert — a group email in a non-existent domain fails the lookup
        try {
            mgr.canCreateGroup(adminAcct, "x@doesnotexist.example");
            fail("expected NO_SUCH_DOMAIN");
        } catch (ServiceException e) {
            assertTrue(e.getCode().contains("NO_SUCH_DOMAIN"));
        }
    }

    @Test
    public void canAccessEmailInvalidEmailThrowsInvalidRequest() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act / Assert — a non-email string is rejected up front
        try {
            mgr.canAccessEmail(at, "notanemail");
            fail("expected INVALID_REQUEST for malformed email");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void canDoAdminAuthTokenNonUserRightReturnsTrue() {
        // Arrange
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act / Assert — auth-token form resolves the account then allows admin
        assertTrue(mgr.canDo(at, userAcct, null, true));
    }

    @Test
    public void canDoNonAdminAuthTokenNonUserRightReturnsFalse() {
        // Arrange
        AuthToken at = new ZimbraAuthToken(userAcct, false, null);

        // Act / Assert — non-admin grantee denied on a non-user right
        assertFalse(mgr.canDo(at, adminAcct, null, true));
    }

    @Test
    public void canDoGranteeEmailNonUserRightReturnsTrueForAdmin() {
        // Act / Assert — email-string grantee resolves to the admin account and is allowed
        assertTrue(mgr.canDo("gamadmin", userAcct, null, true));
    }

    @Test
    public void canDoUnknownGranteeEmailReturnsFalse() {
        // Arrange — a non-user (admin/preset) right. The email-string overload dereferences the
        // right to decide whether to synthesize a guest account, so the right must be non-null;
        // a non-user right means an unresolved email yields no grantee and is therefore denied.
        Right nonUserRight = new PresetRight("test-preset-right");

        // Act / Assert — an email that resolves to no account is denied
        assertFalse(mgr.canDo("nobody@nowhere.example", userAcct, nonUserRight, true));
    }

    @Test
    public void getGetAttrsCheckerAdminAuthTokenReturnsAllowAll() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act
        AllowedAttrs checker = (AllowedAttrs) mgr.getGetAttrsChecker(at, userAcct, true);

        // Assert — admin auth token yields ALLOW_ALL
        assertEquals(Result.ALLOW_ALL, checker.getResult());
    }

    @Test
    public void canGetAttrsAdminAuthTokenReturnsTrue() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);
        Set<String> attrs = new HashSet<String>();
        attrs.add("displayName");

        // Act / Assert
        assertTrue(mgr.canGetAttrs(at, userAcct, attrs, true));
    }

    @Test
    public void canSetAttrsAdminAuthTokenSetReturnsTrue() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);
        Set<String> attrs = new HashSet<String>();
        attrs.add("displayName");

        // Act / Assert
        assertTrue(mgr.canSetAttrs(at, userAcct, attrs, true));
    }

    @Test
    public void canSetAttrsNonAdminAuthTokenSetReturnsFalse() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(userAcct, false, null);
        Set<String> attrs = new HashSet<String>();
        attrs.add("displayName");

        // Act / Assert
        assertFalse(mgr.canSetAttrs(at, adminAcct, attrs, true));
    }

    @Test
    public void canSetAttrsGlobalAdminWithSetReturnsTrue() throws Exception {
        // Arrange
        Set<String> attrs = new HashSet<String>();
        attrs.add("displayName");

        // Act / Assert — Account-credentials Set overload
        assertTrue(mgr.canSetAttrs(adminAcct, userAcct, attrs, true));
    }

    @Test
    public void canSetAttrsNonAdminWithMapReturnsFalse() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("displayName", "X");

        // Act / Assert — Account-credentials Map overload, non-admin denied
        assertFalse(mgr.canSetAttrs(userAcct, adminAcct, attrs, true));
    }

    @Test
    public void canGetAttrsAdminAuthTokenViaTokenReturnsTrueAndNonAdminFalse() throws Exception {
        // Arrange
        AuthToken adminTok = new ZimbraAuthToken(adminAcct, true, null);
        AuthToken userTok = new ZimbraAuthToken(userAcct, false, null);
        Set<String> attrs = new HashSet<String>();
        attrs.add("zimbraId");

        // Act / Assert — both branches of the AuthToken overload
        assertTrue(mgr.canGetAttrs(adminTok, userAcct, attrs, true));
        assertFalse(mgr.canGetAttrs(userTok, adminAcct, attrs, true));
    }

    @Test
    public void canSetAttrsAdminAuthTokenMapReturnsTrue() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("displayName", "Y");

        // Act / Assert — AuthToken Map overload
        assertTrue(mgr.canSetAttrs(at, userAcct, attrs, true));
    }

    // ---------- canAccessAccount: domain-status check + admin/non-admin branches ----------

    @Test
    public void canAccessAccountAuthTokenTargetInSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange — target account lives in a SUSPENDED domain. The domain-status check (L63) runs
        // before the global-admin short-circuit, so even an admin token must be denied with PERM_DENIED.
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        domAttrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SUSPENDED);
        prov.createDomain("caa-susp.com", domAttrs);
        Map<String, Object> tAttrs = new HashMap<String, Object>();
        tAttrs.put(Provisioning.A_zimbraId, "aa111111-1111-1111-1111-111111111111");
        Account target = prov.createAccount("victim@caa-susp.com", "test123", tAttrs);
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act / Assert — kills L63 VoidMethodCall (dropping checkDomainStatus would let admin through)
        try {
            mgr.canAccessAccount(at, target, true);
            fail("expected PERM_DENIED for target in a suspended domain");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canAccessAccountAuthTokenNonAdminReturnsFalse() throws Exception {
        // Arrange — non-admin token, target in an ACTIVE domain, no login-as grant.
        prov.createDomain("caa-active.com", new HashMap<String, Object>());
        Map<String, Object> tAttrs = new HashMap<String, Object>();
        tAttrs.put(Provisioning.A_zimbraId, "ab222222-2222-2222-2222-222222222222");
        Account target = prov.createAccount("t@caa-active.com", "test123", tAttrs);
        AuthToken at = new ZimbraAuthToken(userAcct, false, null);

        // Act / Assert — kills L65 NegateConditionals (admin path) by pinning the non-admin result,
        // and L77 BooleanTrueReturnVals on the two-arg form below.
        assertFalse("non-admin token cannot access an unrelated account", mgr.canAccessAccount(at, target, false));
    }

    @Test
    public void canAccessAccountAuthTokenTwoArgNonAdminReturnsFalse() throws Exception {
        // Arrange — non-admin token via the two-arg convenience overload (defaults asAdmin=true, but the
        // token itself is not an admin token so isGlobalAdmin is false).
        prov.createDomain("caa-2arg.com", new HashMap<String, Object>());
        Map<String, Object> tAttrs = new HashMap<String, Object>();
        tAttrs.put(Provisioning.A_zimbraId, "ac333333-3333-3333-3333-333333333333");
        Account target = prov.createAccount("t2@caa-2arg.com", "test123", tAttrs);
        AuthToken at = new ZimbraAuthToken(userAcct, false, null);

        // Act / Assert — kills L77 BooleanTrueReturnVals (would force the two-arg form to always return true)
        assertFalse("two-arg canAccessAccount denies a non-admin token", mgr.canAccessAccount(at, target));
    }

    @Test
    public void canAccessAccountCredentialsTargetInSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange — Account-credentials overload; domain-status check at L86 precedes the admin check.
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        domAttrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SUSPENDED);
        prov.createDomain("caa-csusp.com", domAttrs);
        Map<String, Object> tAttrs = new HashMap<String, Object>();
        tAttrs.put(Provisioning.A_zimbraId, "ad444444-4444-4444-4444-444444444444");
        Account target = prov.createAccount("v2@caa-csusp.com", "test123", tAttrs);

        // Act / Assert — kills L86 VoidMethodCall
        try {
            mgr.canAccessAccount(adminAcct, target, true);
            fail("expected PERM_DENIED for target in a suspended domain (credentials overload)");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canAccessAccountCredentialsNonAdminReturnsFalse() throws Exception {
        // Arrange — non-admin credentials, active-domain target, no grant.
        prov.createDomain("caa-cactive.com", new HashMap<String, Object>());
        Map<String, Object> tAttrs = new HashMap<String, Object>();
        tAttrs.put(Provisioning.A_zimbraId, "ae555555-5555-5555-5555-555555555555");
        Account target = prov.createAccount("t3@caa-cactive.com", "test123", tAttrs);

        // Act / Assert — kills L89 NegateConditionals (pins the non-admin branch) and, via the two-arg
        // form below, L102 BooleanTrueReturnVals.
        assertFalse("non-admin credentials cannot access an unrelated account",
                mgr.canAccessAccount(userAcct, target, false));
    }

    @Test
    public void canAccessAccountCredentialsTwoArgNonAdminReturnsFalse() throws Exception {
        // Arrange
        prov.createDomain("caa-c2arg.com", new HashMap<String, Object>());
        Map<String, Object> tAttrs = new HashMap<String, Object>();
        tAttrs.put(Provisioning.A_zimbraId, "af666666-6666-6666-6666-666666666666");
        Account target = prov.createAccount("t4@caa-c2arg.com", "test123", tAttrs);

        // Act / Assert — kills L102 BooleanTrueReturnVals (two-arg credentials form forced to true)
        assertFalse("two-arg credentials canAccessAccount denies a non-admin", mgr.canAccessAccount(userAcct, target));
    }

    // ---------- canAccessDomain: domain-status checks + non-admin denial ----------

    @Test
    public void canAccessDomainAuthTokenByNameSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange — admin token, suspended domain by name. checkDomainStatus(domainName) at L182 must run.
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        domAttrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SUSPENDED);
        prov.createDomain("cad-suspname.com", domAttrs);
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act / Assert — kills L182 VoidMethodCall
        try {
            mgr.canAccessDomain(at, "cad-suspname.com");
            fail("expected PERM_DENIED for suspended domain by name");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canAccessDomainAuthTokenByDomainSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange — admin token, suspended Domain object. checkDomainStatus(domain) at L192 must run.
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        domAttrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SUSPENDED);
        Domain domain = prov.createDomain("cad-suspdom.com", domAttrs);
        AuthToken at = new ZimbraAuthToken(adminAcct, true, null);

        // Act / Assert — kills L192 VoidMethodCall
        try {
            mgr.canAccessDomain(at, domain);
            fail("expected PERM_DENIED for suspended Domain object");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canAccessDomainAuthTokenByDomainNonAdminReturnsFalse() throws Exception {
        // Arrange — non-admin token, active Domain object.
        Domain domain = prov.createDomain("cad-active.com", new HashMap<String, Object>());
        AuthToken at = new ZimbraAuthToken(userAcct, false, null);

        // Act / Assert — kills L194 BooleanTrueReturnVals (Domain overload forced to true)
        assertFalse("non-admin token cannot access the Domain object", mgr.canAccessDomain(at, domain));
    }

    // ---------- canCreateGroup: domain-status check + non-admin denial ----------

    @Test
    public void canCreateGroupCredentialsSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange — admin credentials, group email in a SUSPENDED domain. checkDomainStatus(domain) at
        // L134 runs before the admin short-circuit at L140.
        Map<String, Object> domAttrs = new HashMap<String, Object>();
        domAttrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SUSPENDED);
        prov.createDomain("ccg-susp.com", domAttrs);

        // Act / Assert — kills L134 VoidMethodCall
        try {
            mgr.canCreateGroup(adminAcct, "newgroup@ccg-susp.com");
            fail("expected PERM_DENIED for group in a suspended domain");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void canCreateGroupCredentialsNonAdminReturnsFalse() throws Exception {
        // Arrange — non-admin credentials, active domain, no create-distlist grant.
        prov.createDomain("ccg-active.com", new HashMap<String, Object>());

        // Act / Assert — kills L140 NegateConditionals (pins the non-admin branch to false)
        assertFalse("non-admin credentials cannot create a group without a grant",
                mgr.canCreateGroup(userAcct, "newgroup@ccg-active.com"));
    }

    // ---------- canDo(String) + canSetAttrs(AuthToken, Map) non-admin denials ----------

    @Test
    public void canDoGranteeEmailNonAdminReturnsFalse() {
        // Act / Assert — email-string grantee resolves to the NON-admin account; a non-user right
        // falls through to the global-admin check, which is false. Kills L241 BooleanTrueReturnVals
        // (which would force canDo(String,...) to always return true).
        assertFalse("non-admin email grantee on a non-user right is denied",
                mgr.canDo("gamuser", adminAcct, null, true));
    }

    @Test
    public void canSetAttrsNonAdminAuthTokenMapReturnsFalse() throws Exception {
        // Arrange
        AuthToken at = new ZimbraAuthToken(userAcct, false, null);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("displayName", "Z");

        // Act / Assert — kills L299 BooleanTrueReturnVals (AuthToken Map overload forced to true)
        assertFalse("non-admin AuthToken cannot set attrs (map overload)", mgr.canSetAttrs(at, adminAcct, attrs, true));
    }

    @Test
    public void targetTypesForGrantSearchReturnsAccountCalresourceDlGroup() {
        // Act
        Set<TargetType> tts = mgr.targetTypesForGrantSearch();

        // Assert - exactly the four user-grantable target types
        assertEquals(4, tts.size());
        assertTrue(tts.contains(TargetType.account));
        assertTrue(tts.contains(TargetType.calresource));
        assertTrue(tts.contains(TargetType.dl));
        assertTrue(tts.contains(TargetType.group));
        assertFalse(tts.contains(TargetType.domain));
    }
}
