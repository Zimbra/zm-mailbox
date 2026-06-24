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
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GlobalGrant;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link ParticallyDenied}. The public surface
 * ({@code checkPartiallyDenied}) and the grantable-target-type expansion helper are exercised
 * against real rights from {@link RightManager} and a real admin/non-admin account in the
 * in-memory MockProvisioning harness. The two static helpers are package-private, so the helper
 * is reached via reflection (same-package access still requires this for the private method).
 */
public class ParticallyDeniedTest {

    private static Right presetAdminRight;

    private static ComboRight comboAdminRight;

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        RightManager rm = RightManager.getInstance();
        for (AdminRight r : rm.getAllAdminRights().values()) {
            if (presetAdminRight == null && r.isPresetRight()) {
                presetAdminRight = r;
            }
            if (comboAdminRight == null && r.isComboRight()) {
                comboAdminRight = (ComboRight) r;
            }
        }
        assertNotNull("expected at least one preset admin right", presetAdminRight);
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account createAccount(String name, boolean admin) throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        if (admin) {
            attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        }
        return prov.createAccount(name, "pw", attrs);
    }

    @SuppressWarnings("unchecked")
    private Set<TargetType> invokeGetAllGrantableTargetTypes(Right right) throws Exception {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "getAllGrantableTargetTypes", Right.class, Set.class);
        m.setAccessible(true);
        Set<TargetType> result = new HashSet<TargetType>();
        m.invoke(null, right, result);
        return result;
    }

    private boolean invokeIsSubTarget(Entry targetSup, Entry targetSub) throws Throwable {
        Method m = ParticallyDenied.class.getDeclaredMethod(
                "isSubTarget", Provisioning.class, Entry.class, Entry.class);
        m.setAccessible(true);
        try {
            return ((Boolean) m.invoke(null, prov, targetSup, targetSub)).booleanValue();
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private Domain ensureDomain(String name) throws Exception {
        Domain d = prov.get(com.zimbra.common.account.Key.DomainBy.name, name);
        if (d == null) {
            d = prov.createDomain(name, new HashMap<String, Object>());
        }
        return d;
    }

    @Test
    public void checkPartiallyDeniedGlobalAdminGrantorReturnsWithoutThrowing() throws Exception {
        // Arrange — a global admin grantor short-circuits the whole check
        Account admin = createAccount("pd-admin@example.com", true);
        Account target = createAccount("pd-target@example.com", false);

        // Act / Assert — must return quietly (no PERM_DENIED, no SearchGrants/LDAP)
        ParticallyDenied.checkPartiallyDenied(admin, TargetType.account, target, presetAdminRight);

        // Confirm the grantor really is treated as a global admin (the branch we exercised)
        assertTrue(AccessControlUtil.isGlobalAdmin(admin, true));

        prov.deleteAccount(admin.getId());
        prov.deleteAccount(target.getId());
    }

    @Test
    public void getAllGrantableTargetTypesPresetRightCollectsGrantableTypes() throws Exception {
        // Act — a preset right contributes its own grantable target types
        Set<TargetType> result = invokeGetAllGrantableTargetTypes(presetAdminRight);

        // Assert — matches the right's own declared grantable target types
        assertNotNull(result);
        assertEquals(presetAdminRight.getGrantableTargetTypes(), result);
    }

    @Test
    public void getAllGrantableTargetTypesComboRightUnionsSubRightTypes() throws Exception {
        // A combo right unions its sub-rights' grantable target types (not the intersect that
        // ComboRight.getGrantableTargetTypes returns).
        if (comboAdminRight == null) {
            // No combo admin right registered in this build; nothing to assert.
            return;
        }

        // Arrange — compute the expected union from the combo's sub-rights
        Set<TargetType> expectedUnion = new HashSet<TargetType>();
        for (Right sub : comboAdminRight.getAllRights()) {
            expectedUnion.addAll(invokeGetAllGrantableTargetTypes(sub));
        }

        // Act
        Set<TargetType> result = invokeGetAllGrantableTargetTypes(comboAdminRight);

        // Assert — recursion gathers the union of every sub-right's grantable types
        assertEquals(expectedUnion, result);
    }

    @Test
    public void getAllGrantableTargetTypesComboContainsPresetTypesIsSuperSetOfIntersect()
            throws Exception {
        if (comboAdminRight == null) {
            return;
        }

        // Act — the union helper result
        Set<TargetType> union = invokeGetAllGrantableTargetTypes(comboAdminRight);

        // Assert — the union must contain everything in the combo's own intersect-based set
        Set<TargetType> intersect = comboAdminRight.getGrantableTargetTypes();
        if (intersect != null) {
            assertTrue("union must include the combo's intersect target types",
                    union.containsAll(intersect));
        }
    }

    @Test
    public void getAllGrantableTargetTypesUserRightUsesPresetBranch() throws Exception {
        // Arrange — a UserRight reports isPresetRight()==true, so getAllGrantableTargetTypes must
        // take the preset branch and collect the right's own grantable target types (not "nothing").
        UserRight userRight = new UserRight("pd-user-right");
        userRight.setTargetType(TargetType.account);
        assertTrue("a user right is a preset right", userRight.isPresetRight());

        // Act
        Set<TargetType> result = invokeGetAllGrantableTargetTypes(userRight);

        // Assert — the preset branch populated the set with the user right's grantable types.
        assertFalse("account-scoped user right must contribute target types", result.isEmpty());
        assertEquals(userRight.getGrantableTargetTypes(), result);
    }

    @Test
    public void isSubTargetAccountInSameDomainAsTargetDomainReturnsTrue() throws Throwable {
        // Arrange — a domain target (super) and an account that lives in that very domain (sub)
        Domain domain = ensureDomain("pd-sub.example.com");
        Account sub = createAccount("member@pd-sub.example.com", false);

        // Act
        boolean result = invokeIsSubTarget(domain, sub);

        // Assert — the account's domain id matches the super domain's id
        assertTrue("account in the same domain must be a sub-target", result);

        prov.deleteAccount(sub.getId());
    }

    @Test
    public void isSubTargetAccountInDifferentDomainNoGroupsReturnsFalse() throws Throwable {
        // Arrange — super domain and an account in a *different* domain with no group membership
        Domain superDomain = ensureDomain("pd-super.example.com");
        ensureDomain("pd-other.example.com");
        Account sub = createAccount("stranger@pd-other.example.com", false);

        // Act — getGroupMembership is empty in the harness, so the group loop finds nothing
        boolean result = invokeIsSubTarget(superDomain, sub);

        // Assert
        assertFalse("account in a different domain with no groups is not a sub-target", result);

        prov.deleteAccount(sub.getId());
    }

    @Test
    public void isSubTargetDomainSuperWithNonDomainedSubReturnsFalse() throws Throwable {
        // Arrange — a Server has no owning domain, so getTargetDomain returns null
        Domain superDomain = ensureDomain("pd-nodomain.example.com");
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Server server = prov.createServer("pd-server.example.com", attrs);

        // Act
        boolean result = invokeIsSubTarget(superDomain, server);

        // Assert — non-domained sub-entry is never a sub-target of a domain
        assertFalse("a server is not domained, so not a sub-target of a domain", result);
    }

    @Test
    public void isSubTargetGlobalGrantSuperAlwaysReturnsTrue() throws Throwable {
        // Arrange — a GlobalGrant super-target makes any entry a sub-target
        GlobalGrant globalGrant = new GlobalGrant(
                new HashMap<String, Object>(), prov);
        Account sub = createAccount("anyone@pd-sub.example.com", false);

        // Act
        boolean result = invokeIsSubTarget(globalGrant, sub);

        // Assert
        assertTrue("everything is a sub-target of the global grant", result);

        prov.deleteAccount(sub.getId());
    }

    @Test
    public void isSubTargetUnexpectedSuperTypeThrowsFailure() throws Throwable {
        // Arrange — a Cos is not a valid super target type; the method must fail loudly
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Cos cos = new Cos("pd-cos", UUID.randomUUID().toString(), attrs, prov);
        Account sub = createAccount("sub2@pd-sub.example.com", false);

        // Act / Assert
        try {
            invokeIsSubTarget(cos, sub);
            fail("expected ServiceException.FAILURE for an unexpected super target type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
            assertTrue(e.getMessage().contains("unexpected entry type"));
        } finally {
            prov.deleteAccount(sub.getId());
        }
    }

    @Test
    public void checkPartiallyDeniedNonAdminEmptyTargetTypeIntersectReturnsWithoutSearch()
            throws Exception {
        // Arrange — a non-admin grantor. We pick a target type whose sub-target types do not
        // intersect the right's grantable target types, so the method short-circuits before any
        // LDAP-backed SearchGrants (which the in-memory harness cannot run).
        Account grantor = createAccount("pd-nonadmin@example.com", false);
        assertFalse("grantor must not be a global admin",
                AccessControlUtil.isGlobalAdmin(grantor, true));

        // account target type has no sub target types, so the intersect is empty regardless of
        // the right, and checkPartiallyDenied returns before constructing SearchGrants.
        Account target = createAccount("pd-leaf-target@example.com", false);

        // Act / Assert — must complete without throwing and without needing LDAP search
        ParticallyDenied.checkPartiallyDenied(grantor, TargetType.account, target, presetAdminRight);

        // Confirm the assumption that drove the short-circuit: account has no sub target types.
        Set<TargetType> subTypes = TargetType.account.subTargetTypes();
        Set<TargetType> grantable = new HashSet<TargetType>();
        invokeGetAllGrantableTargetTypesInto(presetAdminRight, grantable);
        Set<TargetType> intersect = new HashSet<TargetType>(subTypes);
        intersect.retainAll(grantable);
        assertTrue("intersect must be empty for the short-circuit branch we exercised",
                intersect.isEmpty() || Collections.disjoint(subTypes, grantable));

        prov.deleteAccount(grantor.getId());
        prov.deleteAccount(target.getId());
    }

    private void invokeGetAllGrantableTargetTypesInto(Right right, Set<TargetType> into)
            throws Exception {
        into.addAll(invokeGetAllGrantableTargetTypes(right));
    }

    // ------------------------------------------------------------------
    // checkDenied -- the private static method that enforces (partial) denials: for every grant
    // on a sub-target of the target-to-grant, if a DENIED ACE matches the grantee (directly or via
    // one of its groups) and its right overlaps the right being granted, it throws PERM_DENIED.
    // Exercised via reflection with hand-built rights/ACLs/GrantsOnTarget (no RightManager, no LDAP).
    // ------------------------------------------------------------------

    /* Reflectively builds a {@code SearchGrants.GrantsOnTarget} (its constructor is private). */
    private static Object newGrantsOnTarget(Entry targetEntry, ZimbraACL acl) throws Exception {
        Constructor<?> c = Class.forName(
                "com.zimbra.cs.account.accesscontrol.SearchGrants$GrantsOnTarget")
                .getDeclaredConstructor(Entry.class, ZimbraACL.class);
        c.setAccessible(true);
        return c.newInstance(targetEntry, acl);
    }

    /* A ZimbraACL holding a single DENY ace for {@code granteeId} on {@code right}. */
    private static ZimbraACL deniedAcl(String granteeId, Right right) throws Exception {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(granteeId, GranteeType.GT_USER, right, RightModifier.RM_DENY, null));
        return new ZimbraACL(aces);
    }

    /* A ZimbraACL holding a single ALLOW ace for {@code granteeId} on {@code right}. */
    private static ZimbraACL allowedAcl(String granteeId, Right right) throws Exception {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(granteeId, GranteeType.GT_USER, right, null, null));
        return new ZimbraACL(aces);
    }

    private void invokeCheckDenied(Entry targetToGrant, Right rightToGrant,
            Set<Object> grantsOnTargets, String granteeId, Set<String> granteeGroups)
            throws Throwable {
        Method m = ParticallyDenied.class.getDeclaredMethod("checkDenied",
                Provisioning.class, Entry.class, Right.class, Set.class, String.class, Set.class);
        m.setAccessible(true);
        try {
            m.invoke(null, prov, targetToGrant, rightToGrant, grantsOnTargets, granteeId,
                    granteeGroups);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    public void checkDeniedDeniedOverlappingRightForGranteeThrowsPermDenied() throws Throwable {
        // Arrange — a GlobalGrant target makes every grantedOnEntry a sub-target, so the denial
        // is guaranteed to be inspected. The SAME PresetRight instance is both granted and denied,
        // so overlaps() (identity for preset-vs-preset) is true.
        GlobalGrant target = new GlobalGrant(new HashMap<String, Object>(), prov);
        Account grantedOn = createAccount("cd-grantedon@example.com", false);
        PresetRight right = new PresetRight("pd-preset-right");
        ZimbraACL acl = deniedAcl("grantee-1", right);
        Set<Object> grants = Collections.singleton(newGrantsOnTarget(grantedOn, acl));

        // Act / Assert — the grantee's denied overlapping right must block the grant.
        try {
            invokeCheckDenied(target, right, grants, "grantee-1", null);
            fail("expected PERM_DENIED when an overlapping right is denied to the grantee");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertTrue("message names the denied right", e.getMessage().contains("pd-preset-right"));
            assertTrue("message names the grantee", e.getMessage().contains("grantee-1"));
        } finally {
            prov.deleteAccount(grantedOn.getId());
        }
    }

    @Test
    public void checkDeniedDeniedRightForGranteeGroupThrowsPermDenied() throws Throwable {
        // Arrange — granteeId is null; the denied ACE's grantee is matched via the group set.
        GlobalGrant target = new GlobalGrant(new HashMap<String, Object>(), prov);
        Account grantedOn = createAccount("cd-group-grantedon@example.com", false);
        PresetRight right = new PresetRight("pd-group-right");
        ZimbraACL acl = deniedAcl("group-99", right);
        Set<Object> grants = Collections.singleton(newGrantsOnTarget(grantedOn, acl));
        Set<String> groups = new HashSet<String>();
        groups.add("group-99");

        // Act / Assert — a denial to one of the grantor's groups also blocks the grant.
        try {
            invokeCheckDenied(target, right, grants, null, groups);
            fail("expected PERM_DENIED when the right is denied to one of the grantee's groups");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
            assertTrue(e.getMessage().contains("group-99"));
        } finally {
            prov.deleteAccount(grantedOn.getId());
        }
    }

    @Test
    public void checkDeniedDeniedNonOverlappingRightDoesNotThrow() throws Throwable {
        // Arrange — the denied ACE is for a DIFFERENT preset right instance, so overlaps() is false.
        GlobalGrant target = new GlobalGrant(new HashMap<String, Object>(), prov);
        Account grantedOn = createAccount("cd-nonoverlap@example.com", false);
        PresetRight rightToGrant = new PresetRight("pd-grant-right");
        PresetRight deniedRight = new PresetRight("pd-other-right");
        ZimbraACL acl = deniedAcl("grantee-1", deniedRight);
        Set<Object> grants = Collections.singleton(newGrantsOnTarget(grantedOn, acl));

        // Act — a non-overlapping denied right must not block the grant.
        invokeCheckDenied(target, rightToGrant, grants, "grantee-1", null);

        // Assert — reaching here means no PERM_DENIED was thrown.
        assertTrue("non-overlapping denial must be ignored", true);
        prov.deleteAccount(grantedOn.getId());
    }

    @Test
    public void checkDeniedDeniedRightForDifferentGranteeDoesNotThrow() throws Throwable {
        // Arrange — the denied ACE targets some other grantee, matching neither granteeId nor groups.
        GlobalGrant target = new GlobalGrant(new HashMap<String, Object>(), prov);
        Account grantedOn = createAccount("cd-othergrantee@example.com", false);
        PresetRight right = new PresetRight("pd-right");
        ZimbraACL acl = deniedAcl("someone-else", right);
        Set<Object> grants = Collections.singleton(newGrantsOnTarget(grantedOn, acl));

        // Act — grantee id "grantee-1" does not match "someone-else".
        invokeCheckDenied(target, right, grants, "grantee-1", null);

        // Assert
        assertTrue("a denial to a different grantee must be ignored", true);
        prov.deleteAccount(grantedOn.getId());
    }

    @Test
    public void checkDeniedGrantOnNonSubTargetIsSkipped() throws Throwable {
        // Arrange — target is a Domain and the grant sits on a Server (not domained), so
        // isSubTarget is false and the denied ACE is never inspected.
        Domain target = ensureDomain("cd-nonsub.example.com");
        Map<String, Object> srvAttrs = new HashMap<String, Object>();
        srvAttrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        Server grantedOn = prov.createServer("cd-nonsub-server.example.com", srvAttrs);
        PresetRight right = new PresetRight("pd-nonsub-right");
        ZimbraACL acl = deniedAcl("grantee-1", right);
        Set<Object> grants = Collections.singleton(newGrantsOnTarget(grantedOn, acl));

        // Act — even a matching, overlapping denial is skipped because the grant is not on a sub-target.
        invokeCheckDenied(target, right, grants, "grantee-1", null);

        // Assert
        assertTrue("denials on non-sub-targets must be skipped", true);
        prov.deleteServer(grantedOn.getId());
    }

    @Test
    public void checkDeniedAllowedAceNotCheckedDoesNotThrow() throws Throwable {
        // Arrange — the ACE is an ALLOW (not a deny), so getDeniedACEs() is empty and the overlap
        // check never runs, even for the matching grantee and overlapping right.
        GlobalGrant target = new GlobalGrant(new HashMap<String, Object>(), prov);
        Account grantedOn = createAccount("cd-allow@example.com", false);
        PresetRight right = new PresetRight("pd-allow-right");
        ZimbraACL acl = allowedAcl("grantee-1", right);
        Set<Object> grants = Collections.singleton(newGrantsOnTarget(grantedOn, acl));

        // Act — only DENIED aces are consulted; an allow must never block a grant.
        invokeCheckDenied(target, right, grants, "grantee-1", null);

        // Assert
        assertTrue("allow aces are not denial checks", true);
        prov.deleteAccount(grantedOn.getId());
    }

    @Test
    public void checkDeniedEmptyGrantsDoesNotThrow() throws Throwable {
        // Arrange — no grants at all: the loop body never runs.
        GlobalGrant target = new GlobalGrant(new HashMap<String, Object>(), prov);
        PresetRight right = new PresetRight("pd-empty-right");

        // Act
        invokeCheckDenied(target, right, Collections.<Object>emptySet(), "grantee-1", null);

        // Assert
        assertTrue("no grants means nothing to deny", true);
    }
}
