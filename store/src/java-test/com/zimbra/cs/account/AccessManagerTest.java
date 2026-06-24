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

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.TargetType;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link AccessManager}. A concrete test subclass ({@link TestAccessManager})
 * supplies trivial implementations of the abstract right-checks so the concrete template methods
 * ({@code allowPrivateAccess}, {@code isParentOf}, {@code checkDomainStatus}, the {@code canDo}
 * ViaGrant overloads, {@code getAccount}/{@code getDomain}) can be exercised against real
 * {@link Account} and {@link Domain} objects from the in-memory harness. Also covers the default
 * "not supported" methods and the {@link ViaGrant} value object.
 */
public class AccessManagerTest {

    /** Real concrete AccessManager — canAccessAccount returns a fixed flag we control per test. */
    private static class TestAccessManager extends AccessManager {
        private boolean accessResult = false;

        @Override
        public boolean isDomainAdminOnly(AuthToken at) {
            return false;
        }

        @Override
        public boolean isAdequateAdminAccount(Account acct) {
            return false;
        }

        @Override
        public boolean canAccessAccount(AuthToken at, Account target, boolean asAdmin) {
            return accessResult;
        }

        @Override
        public boolean canAccessAccount(AuthToken at, Account target) {
            return accessResult;
        }

        @Override
        public boolean canAccessAccount(Account credentials, Account target, boolean asAdmin) {
            return accessResult;
        }

        @Override
        public boolean canAccessAccount(Account credentials, Account target) {
            return accessResult;
        }

        @Override
        public boolean canAccessDomain(AuthToken at, String domainName) {
            return accessResult;
        }

        @Override
        public boolean canAccessDomain(AuthToken at, Domain domain) {
            return accessResult;
        }

        @Override
        public boolean canAccessCos(AuthToken at, Cos cos) {
            return accessResult;
        }

        @Override
        public boolean canCreateGroup(AuthToken at, String groupEmail) {
            return accessResult;
        }

        @Override
        public boolean canCreateGroup(Account credentials, String groupEmail) {
            return accessResult;
        }

        @Override
        public boolean canAccessGroup(AuthToken at, Group group) {
            return accessResult;
        }

        @Override
        public boolean canAccessGroup(Account credentials, Group group, boolean asAdmin) {
            return accessResult;
        }

        @Override
        public boolean canAccessEmail(AuthToken at, String email) {
            return accessResult;
        }

        @Override
        public boolean canModifyMailQuota(AuthToken at, Account targetAccount, long mailQuota) {
            return accessResult;
        }

        @Override
        public boolean canDo(MailTarget grantee, Entry target, Right rightNeeded, boolean asAdmin) {
            return accessResult;
        }

        @Override
        public boolean canDo(AuthToken grantee, Entry target, Right rightNeeded, boolean asAdmin) {
            return accessResult;
        }

        @Override
        public boolean canDo(String granteeEmail, Entry target, Right rightNeeded, boolean asAdmin) {
            return accessResult;
        }

        @Override
        public boolean canGetAttrs(Account credentials, Entry target, Set<String> attrs, boolean asAdmin) {
            return accessResult;
        }

        @Override
        public boolean canGetAttrs(AuthToken credentials, Entry target, Set<String> attrs, boolean asAdmin) {
            return accessResult;
        }

        @Override
        public boolean canSetAttrs(Account credentials, Entry target, Set<String> attrs, boolean asAdmin) {
            return accessResult;
        }

        @Override
        public boolean canSetAttrs(AuthToken credentials, Entry target, Set<String> attrs, boolean asAdmin) {
            return accessResult;
        }

        @Override
        public boolean canSetAttrs(Account credentials, Entry target, Map<String, Object> attrs, boolean asAdmin) {
            return accessResult;
        }

        @Override
        public boolean canSetAttrs(AuthToken credentials, Entry target, Map<String, Object> attrs, boolean asAdmin) {
            return accessResult;
        }

        // expose protected isParentOf(Account, Account)
        boolean parentOf(Account credentials, Account target) {
            return isParentOf(credentials, target);
        }

        // expose protected checkDomainStatus(Account)
        void checkStatus(Account acct) throws ServiceException {
            checkDomainStatus(acct);
        }

        // expose protected checkDomainStatus(Group)
        void checkStatus(Group group) throws ServiceException {
            checkDomainStatus(group);
        }

        // expose protected checkDomainStatus(String)
        void checkStatus(String domainName) throws ServiceException {
            checkDomainStatus(domainName);
        }
    }

    /**
     * Minimal concrete {@link Group} whose only meaningful behavior is {@code getDomain()} —
     * which resolves the group's domain via the live provisioning, exactly what
     * {@code checkDomainStatus(Group)} relies on. Every other abstract is a harmless stub.
     */
    private static class TestGroup extends Group {
        TestGroup(String name, String id, Map<String, Object> attrs, Provisioning prov) {
            super(name, id, attrs, prov);
        }

        @Override
        public boolean isDynamic() {
            return false;
        }

        @Override
        public Domain getDomain() throws ServiceException {
            String dname = getDomainName();
            return dname == null ? null : getProvisioning().get(Key.DomainBy.name, dname);
        }

        @Override
        public String[] getAllMembers() {
            return new String[0];
        }

        @Override
        public Set<String> getAllMembersSet() {
            return new HashSet<String>();
        }

        @Override
        public String getDisplayName() {
            return getName();
        }

        @Override
        public String getMail() {
            return getName();
        }

        @Override
        public boolean isPrefReplyToEnabled() {
            return false;
        }

        @Override
        public String getPrefReplyToAddress() {
            return null;
        }

        @Override
        public String getPrefReplyToDisplay() {
            return null;
        }

        @Override
        public com.zimbra.common.account.ZAttrProvisioning.DistributionListSubscriptionPolicy
                getDistributionListSubscriptionPolicy() {
            return null;
        }

        @Override
        public com.zimbra.common.account.ZAttrProvisioning.DistributionListUnsubscriptionPolicy
                getDistributionListUnsubscriptionPolicy() {
            return null;
        }

        @Override
        public boolean isAddrOfEntry(String addr) {
            return false;
        }

        @Override
        public String[] getAliases() {
            return new String[0];
        }

        @Override
        public Set<String> getAllAddrsSet() {
            return new HashSet<String>();
        }
    }

    private TestAccessManager mgr;

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        mgr = new TestAccessManager();
        prov = Provisioning.getInstance();
    }

    @Test
    public void getInstanceDefaultReturnsNonNullSingleton() throws Exception {
        // Act
        AccessManager m1 = AccessManager.getInstance();
        AccessManager m2 = AccessManager.getInstance();

        // Assert — same singleton instance returned
        assertNotNull("getInstance must never return null", m1);
        assertSameInstance(m1, m2);
    }

    /* Save / null-out / restore the AccessManager.sManager static singleton so getInstance() can
     *  be exercised from a fresh state without leaking into other tests. */
    private static AccessManager getSManager() throws Exception {
        java.lang.reflect.Field f = AccessManager.class.getDeclaredField("sManager");
        f.setAccessible(true);
        return (AccessManager) f.get(null);
    }

    private static void setSManager(AccessManager m) throws Exception {
        java.lang.reflect.Field f = AccessManager.class.getDeclaredField("sManager");
        f.setAccessible(true);
        f.set(null, m);
    }

    @Test
    public void getInstanceFreshStateUnsetConfigBuildsLcDefaultAclAccessManager() throws Exception {
        // Arrange — clear the cached singleton and ensure the global-config mech attr is unset, so the
        // init path falls through to the LC key zimbra_class_accessmanager (default ACLAccessManager).
        AccessManager saved = getSManager();
        try {
            Map<String, Object> cfg = new HashMap<String, Object>();
            cfg.put(Provisioning.A_zimbraAdminAccessControlMech, null); // null value removes the attr
            prov.modifyAttrs(prov.getConfig(), cfg);
            setSManager(null);

            // Act
            AccessManager m = AccessManager.getInstance();

            // Assert — exact concrete type is the LC default. Kills L43/L71/L74/L87 negate mutations,
            // each of which would either skip initialization (null) or substitute GlobalAccessManager.
            assertNotNull("fresh getInstance must initialize a manager", m);
            assertEquals("LC default builds an ACLAccessManager",
                    com.zimbra.cs.account.accesscontrol.ACLAccessManager.class, m.getClass());
        } finally {
            setSManager(saved);
        }
    }

    @Test
    public void getInstanceConfigMechGlobalBuildsGlobalAccessManager() throws Exception {
        // Arrange — set zimbraAdminAccessControlMech=global so the config-driven branch (L52/L56) is
        // taken and a GlobalAccessManager is built rather than the LC default.
        AccessManager saved = getSManager();
        try {
            Map<String, Object> cfg = new HashMap<String, Object>();
            cfg.put(Provisioning.A_zimbraAdminAccessControlMech, "global");
            prov.modifyAttrs(prov.getConfig(), cfg);
            setSManager(null);

            // Act
            AccessManager m = AccessManager.getInstance();

            // Assert — kills L52 negate (which would skip the config branch and fall back to the LC
            // default ACLAccessManager instead of GlobalAccessManager).
            assertEquals("config mech=global builds a GlobalAccessManager",
                    com.zimbra.cs.account.accesscontrol.GlobalAccessManager.class, m.getClass());
        } finally {
            // restore config attr and singleton
            Map<String, Object> cfg = new HashMap<String, Object>();
            cfg.put(Provisioning.A_zimbraAdminAccessControlMech, null); // remove the attr again
            prov.modifyAttrs(prov.getConfig(), cfg);
            setSManager(saved);
        }
    }

    private static void assertSameInstance(Object a, Object b) {
        assertTrue("getInstance should be a cached singleton", a == b);
    }

    @Test
    public void allowPrivateAccessSameAccountReturnsTrueWithoutDelegatingToCanAccess() throws Exception {
        // Arrange — same account id; accessResult stays false to prove the short-circuit
        Account acct = prov.createAccount("self@zimbra.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = false;

        // Act
        boolean allowed = mgr.allowPrivateAccess(acct, acct, false);

        // Assert
        assertTrue("same account always allows private access", allowed);
    }

    @Test
    public void allowPrivateAccessDifferentAccountsWithAccessReturnsTrue() throws Exception {
        // Arrange
        Account a = prov.createAccount("auth@zimbra.com", "secret", new HashMap<String, Object>());
        Account b = prov.createAccount("target@zimbra.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = true;   // canAccessAccount returns true

        // Act
        boolean allowed = mgr.allowPrivateAccess(a, b, true);

        // Assert
        assertTrue("admin access grants private access", allowed);
    }

    @Test
    public void allowPrivateAccessDifferentAccountsNoAccessReturnsFalse() throws Exception {
        // Arrange — give each account an explicit, distinct id. The in-memory MockProvisioning
        // assigns the well-known DEFAULT_ACCOUNT_ID to every account created without an explicit
        // zimbraId, so two such accounts would share an id and allowPrivateAccess would short-circuit
        // to true on the same-id check. Distinct ids ensure we exercise the canAccessAccount branch.
        Map<String, Object> attrsA = new HashMap<String, Object>();
        attrsA.put(Provisioning.A_zimbraId, "11111111-1111-1111-1111-111111111111");
        Account a = prov.createAccount("auth2@zimbra.com", "secret", attrsA);
        Map<String, Object> attrsB = new HashMap<String, Object>();
        attrsB.put(Provisioning.A_zimbraId, "22222222-2222-2222-2222-222222222222");
        Account b = prov.createAccount("target2@zimbra.com", "secret", attrsB);
        mgr.accessResult = false;

        // Assert — precondition: the two accounts really have distinct ids
        assertFalse("distinct ids => not the same account",
                a.getId().equalsIgnoreCase(b.getId()));

        // Act
        boolean allowed = mgr.allowPrivateAccess(a, b, false);

        // Assert
        assertFalse("no access => no private access", allowed);
    }

    @Test
    public void allowPrivateAccessNullAccountReturnsFalse() throws Exception {
        // Act
        boolean allowed = mgr.allowPrivateAccess(null, null, false);

        // Assert
        assertFalse("null accounts => false", allowed);
    }

    @Test
    public void isParentOfTargetInChildAccountsReturnsTrue() throws Exception {
        // Arrange — target's id is listed in parent's zimbraChildAccount
        Account child = prov.createAccount("child@zimbra.com", "secret", new HashMap<String, Object>());
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraChildAccount, child.getId());
        Account parent = prov.createAccount("parent@zimbra.com", "secret", attrs);

        // Act
        boolean isParent = mgr.parentOf(parent, child);

        // Assert
        assertTrue("child id present => parent", isParent);
    }

    @Test
    public void isParentOfTargetNotChildReturnsFalse() throws Exception {
        // Arrange — no child relationship
        Account parent = prov.createAccount("parent2@zimbra.com", "secret", new HashMap<String, Object>());
        Account other = prov.createAccount("other@zimbra.com", "secret", new HashMap<String, Object>());

        // Act
        boolean isParent = mgr.parentOf(parent, other);

        // Assert
        assertFalse("unrelated account => not parent", isParent);
    }

    @Test
    public void checkDomainStatusSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SUSPENDED);
        Domain domain = prov.createDomain("suspended.com", attrs);

        // Act / Assert
        try {
            mgr.checkDomainStatus(domain);
            fail("expected PERM_DENIED for suspended domain");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertTrue(e.getMessage().contains("domain is"));
        }
    }

    @Test
    public void checkDomainStatusActiveDomainDoesNotThrow() throws Exception {
        // Arrange — domain with no suspended/shutdown status
        Domain domain = prov.createDomain("active.com", new HashMap<String, Object>());

        // Act — should complete without exception
        mgr.checkDomainStatus(domain);

        // Assert — reaching here means no exception was thrown
        assertFalse("active domain is not suspended", domain.isSuspended());
    }

    @Test
    public void checkDomainStatusNullDomainDoesNotThrow() throws Exception {
        // Act — null domain is tolerated
        mgr.checkDomainStatus((Domain) null);

        // Assert
        assertTrue("null domain is a no-op", true);
    }

    @Test
    public void canDoWithViaGrantDelegatesToPlainCanDo() throws Exception {
        // Arrange
        Account grantee = prov.createAccount("grantee@zimbra.com", "secret", new HashMap<String, Object>());
        Account target = prov.createAccount("tgt@zimbra.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = true;
        ViaGrant via = new ViaGrant();

        // Act — the ViaGrant overload should delegate to the 4-arg canDo
        boolean allowed = mgr.canDo((MailTarget) grantee, (Entry) target, (Right) null, true, via);

        // Assert
        assertTrue("ViaGrant overload delegates to canDo result", allowed);
    }

    @Test
    public void canDoWithViaGrantMailTargetOverloadDenyReturnsFalse() throws Exception {
        // Arrange — accessResult=false makes the delegated canDo return false. Kills the L278
        // BooleanTrueReturnVals mutation, which would force the overload to always return true.
        Account grantee = prov.createAccount("cd-mt-deny-g@zimbra.com", "secret", new HashMap<String, Object>());
        Account target = prov.createAccount("cd-mt-deny-t@zimbra.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = false;

        // Act
        boolean allowed = mgr.canDo((MailTarget) grantee, (Entry) target, (Right) null, true, new ViaGrant());

        // Assert
        assertFalse("MailTarget ViaGrant overload returns the (false) delegated canDo result", allowed);
    }

    @Test
    public void canDoWithViaGrantAuthTokenOverloadDenyReturnsFalse() throws Exception {
        // Arrange — kills the L283 BooleanTrueReturnVals mutation (would always return true).
        Account target = prov.createAccount("cd-at-deny-t@zimbra.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = false;

        // Act
        boolean allowed = mgr.canDo((AuthToken) null, (Entry) target, (Right) null, true, new ViaGrant());

        // Assert
        assertFalse("AuthToken ViaGrant overload returns the (false) delegated canDo result", allowed);
    }

    @Test
    public void canDoWithViaGrantStringOverloadAllowReturnsTrue() throws Exception {
        // Arrange — accessResult=true so the delegated canDo returns true. Kills the L288
        // BooleanFalseReturnVals mutation, which would force the overload to always return false.
        Account target = prov.createAccount("cd-str-allow-t@zimbra.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = true;

        // Act
        boolean allowed = mgr.canDo("grantee-allow@zimbra.com", (Entry) target, (Right) null, true, new ViaGrant());

        // Assert
        assertTrue("String ViaGrant overload returns the (true) delegated canDo result", allowed);
    }

    @Test
    public void viaGrantUnsetReportsUnavailableAndNullFields() throws Exception {
        // Arrange / Act
        ViaGrant via = new ViaGrant();

        // Assert — no impl set => everything null/false
        assertFalse("unset ViaGrant is unavailable", via.available());
        assertNull(via.getTargetType());
        assertNull(via.getTargetName());
        assertNull(via.getGranteeType());
        assertNull(via.getGranteeName());
        assertNull(via.getRight());
        assertFalse(via.isNegativeGrant());
        // L264 EmptyObjectReturnVals: the no-impl toString would be mutated to "". Assert the real
        // MoreObjects helper output, which carries the simple class name "ViaGrant".
        String s = via.toString();
        assertTrue("unset ViaGrant toString reports its class name, got: " + s,
                s.contains("ViaGrant"));
    }

    @Test
    public void getGetAttrsCheckerDefaultThrowsNotSupported() throws Exception {
        // Arrange
        Account acct = prov.createAccount("checker@zimbra.com", "secret", new HashMap<String, Object>());

        // Act / Assert
        try {
            mgr.getGetAttrsChecker(acct, acct, false);
            fail("expected FAILURE for unsupported getGetAttrsChecker");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("not supported"));
        }
    }

    @Test
    public void canSetAttrsOnCreateDefaultThrowsNotSupported() throws Exception {
        // Arrange
        Account acct = prov.createAccount("setcreate@zimbra.com", "secret", new HashMap<String, Object>());

        // Act / Assert
        try {
            mgr.canSetAttrsOnCreate(acct, TargetType.account, "name",
                    new HashMap<String, Object>(), false);
            fail("expected FAILURE for unsupported canSetAttrsOnCreate");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("not supported"));
        }
    }

    @Test
    public void discoverUserRightsDefaultReturnsEmptyMap() throws Exception {
        // Arrange
        Account acct = prov.createAccount("disc@zimbra.com", "secret", new HashMap<String, Object>());

        // Act
        Map<Right, Set<Entry>> result = mgr.discoverUserRights(acct, new HashSet<Right>(), false);

        // Assert
        assertNotNull("default returns an empty (non-null) map", result);
        assertTrue("no rights discovered by default", result.isEmpty());
        // L382 EmptyObjectReturnVals would substitute an immutable Collections.emptyMap(); the real
        // Maps.newHashMap() is mutable. Putting must succeed and grow the map.
        result.put(null, new HashSet<Entry>());
        assertEquals("default map is a mutable HashMap, not an immutable empty map", 1, result.size());
    }

    @Test
    public void canPerformDefaultThrowsNotSupported() throws Exception {
        // Arrange
        Account acct = prov.createAccount("perform@zimbra.com", "secret", new HashMap<String, Object>());

        // Act / Assert
        try {
            mgr.canPerform((MailTarget) acct, (Entry) acct, (Right) null, false,
                    new HashMap<String, Object>(), false, new ViaGrant());
            fail("expected FAILURE for unsupported canPerform");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("not supported"));
        }
    }

    // ---------- allowPrivateAccess - canAccessAccount branch ----------

    @Test
    public void allowPrivateAccessDistinctAccountsWithAccessDelegatesToCanAccess() throws Exception {
        // Arrange — distinct ids so the same-id short-circuit is NOT taken; accessResult true
        // forces the canAccessAccount branch to return true.
        Map<String, Object> aAttrs = new HashMap<String, Object>();
        aAttrs.put(Provisioning.A_zimbraId, "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        Account a = prov.createAccount("pa-auth@zimbra.com", "secret", aAttrs);
        Map<String, Object> bAttrs = new HashMap<String, Object>();
        bAttrs.put(Provisioning.A_zimbraId, "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        Account b = prov.createAccount("pa-tgt@zimbra.com", "secret", bAttrs);
        mgr.accessResult = true;

        // Assert precondition — distinct ids
        assertFalse(a.getId().equalsIgnoreCase(b.getId()));

        // Act
        boolean allowed = mgr.allowPrivateAccess(a, b, true);

        // Assert — reached the canAccessAccount branch and it granted access
        assertTrue("distinct accounts with admin access => private access via canAccessAccount",
                allowed);
    }

    // ---------- checkDomainStatus(Account) ----------

    @Test
    public void checkDomainStatusAccountInSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange — a domain that is suspended, and an account in it
        Map<String, Object> dAttrs = new HashMap<String, Object>();
        dAttrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SUSPENDED);
        prov.createDomain("susp-acct.com", dAttrs);
        Account acct = prov.createAccount("u1@susp-acct.com", "secret", new HashMap<String, Object>());

        // Act / Assert — resolving the account's domain and finding it suspended denies access
        try {
            mgr.checkStatus(acct);
            fail("expected PERM_DENIED for account in a suspended domain");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void checkDomainStatusAccountInActiveDomainDoesNotThrow() throws Exception {
        // Arrange — active domain + account
        prov.createDomain("active-acct.com", new HashMap<String, Object>());
        Account acct = prov.createAccount("u2@active-acct.com", "secret", new HashMap<String, Object>());

        // Act — no exception expected
        mgr.checkStatus(acct);

        // Assert
        assertTrue("active domain account passes the domain status check", true);
    }

    // ---------- checkDomainStatus(String) ----------

    @Test
    public void checkDomainStatusSuspendedDomainByNameThrowsPermDenied() throws Exception {
        // Arrange
        Map<String, Object> dAttrs = new HashMap<String, Object>();
        dAttrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SUSPENDED);
        prov.createDomain("susp-byname.com", dAttrs);

        // Act / Assert
        try {
            mgr.checkStatus("susp-byname.com");
            fail("expected PERM_DENIED for suspended domain looked up by name");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void checkDomainStatusUnknownDomainNameDoesNotThrow() throws Exception {
        // Act — an unknown domain name resolves to null, which is tolerated as a no-op
        mgr.checkStatus("no-such-domain-anywhere.com");

        // Assert
        assertTrue("unknown domain name is a no-op", true);
    }

    // ---------- checkDomainStatus(Group) ----------

    @Test
    public void checkDomainStatusGroupInSuspendedDomainThrowsPermDenied() throws Exception {
        // Arrange — suspended domain and a DistributionList (a Group) in it
        Map<String, Object> dAttrs = new HashMap<String, Object>();
        dAttrs.put(Provisioning.A_zimbraDomainStatus, Provisioning.DOMAIN_STATUS_SUSPENDED);
        prov.createDomain("susp-grp.com", dAttrs);
        Group group = new TestGroup("dl@susp-grp.com",
                "dl-id-1", new HashMap<String, Object>(), prov);

        // Act / Assert
        try {
            mgr.checkStatus(group);
            fail("expected PERM_DENIED for group in a suspended domain");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void checkDomainStatusGroupInActiveDomainDoesNotThrow() throws Exception {
        // Arrange — active domain + DL group
        prov.createDomain("active-grp.com", new HashMap<String, Object>());
        Group group = new TestGroup("dl2@active-grp.com",
                "dl-id-2", new HashMap<String, Object>(), prov);

        // Act — no exception expected
        mgr.checkStatus(group);

        // Assert
        assertTrue("group in an active domain passes the status check", true);
    }

    // ---------- canDo(...) ViaGrant overloads ----------

    @Test
    public void canDoWithViaGrantAuthTokenOverloadDelegatesToCanDo() throws Exception {
        // Arrange
        Account target = prov.createAccount("cd-at-tgt@zimbra.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = true;

        // Act — the AuthToken ViaGrant overload delegates to the 4-arg canDo
        boolean allowed = mgr.canDo((AuthToken) null, (Entry) target, (Right) null, true, new ViaGrant());

        // Assert
        assertTrue("AuthToken ViaGrant overload delegates to canDo result", allowed);
    }

    @Test
    public void canDoWithViaGrantStringOverloadDelegatesToCanDo() throws Exception {
        // Arrange
        Account target = prov.createAccount("cd-str-tgt@zimbra.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = false;

        // Act — the String granteeEmail ViaGrant overload delegates to the 4-arg canDo
        boolean allowed = mgr.canDo("grantee@zimbra.com", (Entry) target, (Right) null, false, new ViaGrant());

        // Assert
        assertFalse("String ViaGrant overload returns the delegated canDo result", allowed);
    }

    // ---------- default "not supported" overloads ----------

    @Test
    public void getGetAttrsCheckerAuthTokenOverloadThrowsNotSupported() throws Exception {
        // Arrange
        Account acct = prov.createAccount("checker-at@zimbra.com", "secret", new HashMap<String, Object>());

        // Act / Assert
        try {
            mgr.getGetAttrsChecker((AuthToken) null, (Entry) acct, false);
            fail("expected FAILURE for unsupported getGetAttrsChecker(AuthToken)");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("not supported"));
        }
    }

    @Test
    public void canPerformAuthTokenOverloadThrowsNotSupported() throws Exception {
        // Arrange
        Account acct = prov.createAccount("perform-at@zimbra.com", "secret", new HashMap<String, Object>());

        // Act / Assert
        try {
            mgr.canPerform((AuthToken) null, (Entry) acct, (Right) null, false,
                    new HashMap<String, Object>(), false, new ViaGrant());
            fail("expected FAILURE for unsupported canPerform(AuthToken)");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("not supported"));
        }
    }

    // ---------- ViaGrant with a delegate impl ----------

    @Test
    public void viaGrantWithImplReportsDelegateFields() throws Exception {
        // Arrange — a concrete ViaGrant whose getters return fixed values
        ViaGrant impl = new ViaGrant() {
            @Override
            public String getTargetType() {
                return "account";
            }

            @Override
            public String getTargetName() {
                return "tgt";
            }

            @Override
            public String getGranteeType() {
                return "usr";
            }

            @Override
            public String getGranteeName() {
                return "gname";
            }

            @Override
            public String getRight() {
                return "viewMail";
            }

            @Override
            public boolean isNegativeGrant() {
                return true;
            }
        };
        ViaGrant outer = new ViaGrant();
        outer.setImpl(impl);

        // Act / Assert — the outer delegates every getter to the impl
        assertTrue("ViaGrant with an impl reports available", outer.available());
        assertEquals("account", outer.getTargetType());
        assertEquals("tgt", outer.getTargetName());
        assertEquals("usr", outer.getGranteeType());
        assertEquals("gname", outer.getGranteeName());
        assertEquals("viewMail", outer.getRight());
        assertTrue("negative grant delegated", outer.isNegativeGrant());
        assertTrue("toString includes the delegated right",
                outer.toString().contains("viewMail"));
    }

    // ---------- canSendAs / canSendOnBehalfOf (canSendInternal) ----------

    @Test
    public void canSendAsInternalTargetAddressResolvesAccountAndDelegatesToCanDo() throws Exception {
        // Arrange — RightManager must be initialized so User.R_sendAs is non-null
        com.zimbra.cs.account.accesscontrol.RightManager.getInstance().getAllAdminRights();
        prov.createDomain("send-internal.com", new HashMap<String, Object>());
        Map<String, Object> tgtAttrs = new HashMap<String, Object>();
        tgtAttrs.put(Provisioning.A_zimbraId, "cccccccc-cccc-cccc-cccc-cccccccccccc");
        Account targetAcct = prov.createAccount("owner@send-internal.com", "secret", tgtAttrs);
        Account grantee = prov.createAccount("sender@send-internal.com", "secret", new HashMap<String, Object>());
        // asAdmin=true so the result is purely canDo's value (no isAllowedSendAddress narrowing)
        mgr.accessResult = true;

        // Act — internal address resolves to the account and canDo is consulted
        boolean allowed = mgr.canSendAs(grantee, targetAcct, "owner@send-internal.com", true);

        // Assert
        assertTrue("admin canSendAs an internal account address follows canDo", allowed);
    }

    @Test
    public void canSendAsInternalTargetNoCanDoReturnsFalse() throws Exception {
        // Arrange
        com.zimbra.cs.account.accesscontrol.RightManager.getInstance().getAllAdminRights();
        prov.createDomain("send-deny.com", new HashMap<String, Object>());
        Map<String, Object> tgtAttrs = new HashMap<String, Object>();
        tgtAttrs.put(Provisioning.A_zimbraId, "dddddddd-dddd-dddd-dddd-dddddddddddd");
        Account targetAcct = prov.createAccount("owner@send-deny.com", "secret", tgtAttrs);
        Account grantee = prov.createAccount("sender@send-deny.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = false;   // canDo denies

        // Act
        boolean allowed = mgr.canSendAs(grantee, targetAcct, "owner@send-deny.com", true);

        // Assert
        assertFalse("canSendAs is false when canDo denies", allowed);
    }

    @Test
    public void canSendOnBehalfOfExternalAddressNotAllowedReturnsFalse() throws Exception {
        // Arrange — an external (non-internal-domain) target address that is NOT in the
        // target account's zimbraAllowFromAddress, so no target is resolved and result is false.
        com.zimbra.cs.account.accesscontrol.RightManager.getInstance().getAllAdminRights();
        Account targetAcct = prov.createAccount("sobo-tgt@zimbra.com", "secret", new HashMap<String, Object>());
        Account grantee = prov.createAccount("sobo-grantee@zimbra.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = true;   // even if canDo would allow, no target resolves

        // Act — external address not whitelisted => target stays null => false
        boolean allowed = mgr.canSendOnBehalfOf(grantee, targetAcct,
                "stranger@external-unknown-domain.org", false);

        // Assert
        assertFalse("un-whitelisted external address yields no send permission", allowed);
    }

    @Test
    public void canSendAsExternalAddressInAllowFromResolvesTargetAccount() throws Exception {
        // Arrange — external address that IS listed in the target account's allow-from set
        com.zimbra.cs.account.accesscontrol.RightManager.getInstance().getAllAdminRights();
        Map<String, Object> tgtAttrs = new HashMap<String, Object>();
        tgtAttrs.put(Provisioning.A_zimbraId, "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
        tgtAttrs.put(Provisioning.A_zimbraAllowFromAddress, "alias@external-allowed.org");
        Account targetAcct = prov.createAccount("af-tgt@zimbra.com", "secret", tgtAttrs);
        Account grantee = prov.createAccount("af-grantee@zimbra.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = true;

        // Act — external address in allow-from resolves the target, then canDo (admin) allows
        boolean allowed = mgr.canSendAs(grantee, targetAcct, "alias@external-allowed.org", true);

        // Assert
        assertTrue("allow-from external address with admin canDo permits sending", allowed);
    }

    @Test
    public void canSendAsNonAdminCanDoButAddressNotAllowedNarrowsToFalse() throws Exception {
        // Arrange — internal target whose allowed-send list does NOT include the requested address.
        // canDo allows (accessResult=true) but asAdmin=false forces the non-admin narrowing through
        // AccountUtil.isAllowedSendAddress, which must reject the address and flip the result to false.
        com.zimbra.cs.account.accesscontrol.RightManager.getInstance().getAllAdminRights();
        prov.createDomain("narrow-send.com", new HashMap<String, Object>());
        Map<String, Object> tgtAttrs = new HashMap<String, Object>();
        tgtAttrs.put(Provisioning.A_zimbraId, "ffffffff-ffff-ffff-ffff-ffffffffffff");
        // Restrict the delegated-sender allow list to a DIFFERENT address than the one requested,
        // so the requested "owner@narrow-send.com" is not an allowed send address.
        tgtAttrs.put(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender, "someoneelse@narrow-send.com");
        Account targetAcct = prov.createAccount("owner@narrow-send.com", "secret", tgtAttrs);
        Account grantee = prov.createAccount("sender@narrow-send.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = true;   // canDo would allow

        // Act — non-admin send: canDo allows but the address is not in the allow-send set
        boolean allowed = mgr.canSendAs(grantee, targetAcct, "owner@narrow-send.com", false);

        // Assert — L445 NegateConditionals: the original enters the narrowing branch
        // (allowed && !asAdmin) and rejects; the mutant skips it and would return true.
        assertFalse("non-admin send is narrowed to false when address is not an allowed send address",
                allowed);
    }

    @Test
    public void canSendOnBehalfOfInternalTargetWithCanDoReturnsTrue() throws Exception {
        // Arrange — internal account target, admin send so no narrowing; canDo allows.
        // Kills L408 BooleanFalseReturnVals (which would force canSendOnBehalfOf to always return false).
        com.zimbra.cs.account.accesscontrol.RightManager.getInstance().getAllAdminRights();
        prov.createDomain("sobo-allow.com", new HashMap<String, Object>());
        Map<String, Object> tgtAttrs = new HashMap<String, Object>();
        tgtAttrs.put(Provisioning.A_zimbraId, "10101010-1010-1010-1010-101010101010");
        Account targetAcct = prov.createAccount("owner@sobo-allow.com", "secret", tgtAttrs);
        Account grantee = prov.createAccount("sender@sobo-allow.com", "secret", new HashMap<String, Object>());
        mgr.accessResult = true;

        // Act
        boolean allowed = mgr.canSendOnBehalfOf(grantee, targetAcct, "owner@sobo-allow.com", true);

        // Assert
        assertTrue("admin canSendOnBehalfOf an internal address follows canDo (true)", allowed);
    }
}
