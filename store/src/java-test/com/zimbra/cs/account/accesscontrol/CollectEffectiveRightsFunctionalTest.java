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

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Structural / functional tests for {@link CollectEffectiveRights}. The class is package-private
 * and its collect() path is integration-only (needs LDAP-backed grants and a fully-wired
 * RightBearer), so these tests construct a real instance via its private constructor and exercise
 * the deterministic, pure private helpers ({@code isGlobalAdmin}, {@code setToSortedList},
 * {@code collectPresetRightIfMoreRelevant}) through reflection with real {@link UserRight}
 * objects.
 */
public class CollectEffectiveRightsFunctionalTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        // The collect() admin-preset-rights path needs the admin right registry loaded.
        RightManager.getInstance().getAllAdminRights();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        if (prov.get(DomainBy.name, "example.com") == null) {
            prov.createDomain("example.com", new HashMap<String, Object>());
        }
    }

    /* Build a GlobalAdmin RightBearer over a real account via its private constructor. */
    private RightBearer newGlobalAdmin(NamedEntry entry) throws Exception {
        Constructor<RightBearer.GlobalAdmin> ctor =
                RightBearer.GlobalAdmin.class.getDeclaredConstructor(NamedEntry.class);
        ctor.setAccessible(true);
        return ctor.newInstance(entry);
    }

    private RightCommand.EffectiveRights newResult() {
        return new RightCommand.EffectiveRights("account", "tid", "tname", "gid", "gname");
    }

    /* Construct a CollectEffectiveRights with all-null collaborators via its private ctor. */
    private Object newCollector() throws Exception {
        Constructor<CollectEffectiveRights> ctor = CollectEffectiveRights.class.getDeclaredConstructor(
                RightBearer.class, com.zimbra.cs.account.Entry.class, TargetType.class,
                boolean.class, boolean.class, RightCommand.EffectiveRights.class);
        ctor.setAccessible(true);
        return ctor.newInstance(null, null, TargetType.account, false, false, null);
    }

    @Test
    public void privateConstructorBuildsInstance() throws Exception {
        // Act — the private 6-arg constructor is reachable via reflection.
        Object collector = newCollector();

        // Assert
        assertNotNull(collector);
        assertTrue(collector instanceof CollectEffectiveRights);
    }

    @Test
    public void isGlobalAdminNonGlobalAdminRightBearerFalse() throws Exception {
        // Arrange — a null RightBearer is not an instance of GlobalAdmin.
        Object collector = newCollector();
        Method m = CollectEffectiveRights.class.getDeclaredMethod("isGlobalAdmin");
        m.setAccessible(true);

        // Act
        boolean result = (Boolean) m.invoke(collector);

        // Assert — instanceof GlobalAdmin on null is false.
        assertFalse(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void setToSortedListUnsortedSetReturnsAlphabeticallySortedList() throws Exception {
        // Arrange
        Object collector = newCollector();
        Method m = CollectEffectiveRights.class.getDeclaredMethod("setToSortedList", Set.class);
        m.setAccessible(true);
        Set<String> input = new HashSet<String>();
        input.add("charlie");
        input.add("alpha");
        input.add("bravo");

        // Act
        List<String> sorted = (List<String>) m.invoke(collector, input);

        // Assert — entries are sorted ascending.
        assertEquals(3, sorted.size());
        assertEquals("alpha", sorted.get(0));
        assertEquals("bravo", sorted.get(1));
        assertEquals("charlie", sorted.get(2));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectPresetRightIfMoreRelevantNewRightAddedToAllowed() throws Exception {
        // Arrange — a positive (non-negative) grant lands in the allowed map.
        Object collector = newCollector();
        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectPresetRightIfMoreRelevant",
                Right.class, boolean.class, Integer.class, Map.class, Map.class);
        m.setAccessible(true);

        Right right = new UserRight("viewFreeBusy");
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();

        // Act — negative=false, relativity=5
        m.invoke(collector, right, Boolean.FALSE, Integer.valueOf(5), allowed, denied);

        // Assert — recorded in allowed with the given relativity; denied untouched.
        assertEquals(Integer.valueOf(5), allowed.get(right));
        assertTrue(denied.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectPresetRightIfMoreRelevantNegativeGrantAddedToDenied() throws Exception {
        // Arrange — a negative grant lands in the denied map.
        Object collector = newCollector();
        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectPresetRightIfMoreRelevant",
                Right.class, boolean.class, Integer.class, Map.class, Map.class);
        m.setAccessible(true);

        Right right = new UserRight("viewFreeBusy");
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();

        // Act — negative=true
        m.invoke(collector, right, Boolean.TRUE, Integer.valueOf(3), allowed, denied);

        // Assert
        assertEquals(Integer.valueOf(3), denied.get(right));
        assertTrue(allowed.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectPresetRightIfMoreRelevantLessRelevantGrantDoesNotOverride() throws Exception {
        // Arrange — a more-relevant (smaller) relativity already present must win.
        Object collector = newCollector();
        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectPresetRightIfMoreRelevant",
                Right.class, boolean.class, Integer.class, Map.class, Map.class);
        m.setAccessible(true);

        Right right = new UserRight("viewFreeBusy");
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();

        // Act — first a relativity of 2, then a less-relevant 9.
        m.invoke(collector, right, Boolean.FALSE, Integer.valueOf(2), allowed, denied);
        m.invoke(collector, right, Boolean.FALSE, Integer.valueOf(9), allowed, denied);

        // Assert — the smaller (more relevant) relativity is retained.
        assertEquals(Integer.valueOf(2), allowed.get(right));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectPresetRightIfMoreRelevantMoreRelevantGrantOverridesExisting() throws Exception {
        // Arrange — a subsequently smaller relativity should replace the larger one.
        Object collector = newCollector();
        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectPresetRightIfMoreRelevant",
                Right.class, boolean.class, Integer.class, Map.class, Map.class);
        m.setAccessible(true);

        Right right = new UserRight("viewFreeBusy");
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();

        // Act — first 8, then a more-relevant 1.
        m.invoke(collector, right, Boolean.FALSE, Integer.valueOf(8), allowed, denied);
        m.invoke(collector, right, Boolean.FALSE, Integer.valueOf(1), allowed, denied);

        // Assert — the more relevant relativity replaces the earlier value.
        assertEquals(Integer.valueOf(1), allowed.get(right));
    }

    @Test
    public void getEffectiveRightsGlobalAdminOnAccountSetsAllAttrsAndPresetRights() throws Exception {
        // Arrange — a global admin sees all preset rights and all attrs on the target type.
        Account target = prov.createAccount("cer-target@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer admin = newGlobalAdmin(target);
        RightCommand.EffectiveRights result = newResult();

        // Act — expandSet/Get = false, so only the all-attrs flags are set (no expansion).
        CollectEffectiveRights.getEffectiveRights(admin, target, TargetType.account,
                false, false, result);

        // Assert — global admin can set and get ALL attrs, and has a populated preset-right list.
        assertTrue("global admin can set all attrs", result.canSetAllAttrs());
        assertTrue("global admin can get all attrs", result.canGetAllAttrs());
        assertNotNull(result.presetRights());
        assertFalse("global admin should have executable preset rights",
                result.presetRights().isEmpty());
    }

    @Test
    public void getEffectiveRightsGlobalAdminExpandAttrsPopulatesExpandedAttrMaps() throws Exception {
        // Arrange
        Account target = prov.createAccount("cer-expand@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer admin = newGlobalAdmin(target);
        RightCommand.EffectiveRights result = newResult();

        // Act — expandSet/Get = true forces expandAttrs() -> fillDefaultAndConstratint().
        CollectEffectiveRights.getEffectiveRights(admin, target, TargetType.account,
                true, true, result);

        // Assert — expansion fills the per-attr maps for the account attribute class.
        assertTrue(result.canSetAllAttrs());
        assertTrue(result.canGetAllAttrs());
        assertNotNull(result.canSetAttrs());
        assertNotNull(result.canGetAttrs());
        assertFalse("expanded set-attrs map should be non-empty", result.canSetAttrs().isEmpty());
        assertFalse("expanded get-attrs map should be non-empty", result.canGetAttrs().isEmpty());
    }

    @Test
    public void getEffectiveRightsTwoArgTargetTypeDerivedGlobalAdminSetsAllAttrs() throws Exception {
        // Arrange — the 5-arg overload derives the TargetType from the target itself.
        Account target = prov.createAccount("cer-derive@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer admin = newGlobalAdmin(target);
        RightCommand.EffectiveRights result = newResult();

        // Act
        CollectEffectiveRights.getEffectiveRights(admin, target, false, false, result);

        // Assert
        assertTrue(result.canSetAllAttrs());
        assertTrue(result.canGetAllAttrs());
    }

    @Test
    public void getAllExecutableAdminPresetRightsAccountTargetTypeIncludesExecutableRights()
            throws Exception {
        // Arrange — drive the private method directly on a global-admin collector.
        Account target = prov.createAccount("cer-allexec@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer admin = newGlobalAdmin(target);
        Object collector = newCollectorWith(admin, target, TargetType.account);
        Method m = CollectEffectiveRights.class.getDeclaredMethod("getAllExecutableAdminPresetRights");
        m.setAccessible(true);

        // Act
        @SuppressWarnings("unchecked")
        Set<Right> rights = (Set<Right>) m.invoke(collector);

        // Assert — combo rights are expanded into preset rights; the set is non-empty for account.
        assertNotNull(rights);
        assertFalse(rights.isEmpty());
        for (Right r : rights) {
            assertTrue("every collected right must be executable on account",
                    r.executableOnTargetType(TargetType.account));
        }
    }

    @Test
    public void expandAttrsAccountTargetBuildsEffectiveAttrMapWithDefaults() throws Exception {
        // Arrange
        Account target = prov.createAccount("cer-expandattrs@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer admin = newGlobalAdmin(target);
        Object collector = newCollectorWith(admin, target, TargetType.account);
        Method m = CollectEffectiveRights.class.getDeclaredMethod("expandAttrs", AttrRight.class);
        m.setAccessible(true);

        // Act — PR_GET_ATTRS expands every attr in the account class (no forbidden-attr filtering).
        @SuppressWarnings("unchecked")
        SortedMap<String, RightCommand.EffectiveAttr> attrs =
                (SortedMap<String, RightCommand.EffectiveAttr>) m.invoke(collector,
                        AdminRight.PR_GET_ATTRS);

        // Assert
        assertNotNull(attrs);
        assertFalse("account class has attributes to expand", attrs.isEmpty());
    }

    @Test
    public void fillDefaultAndConstratintDomainTargetConfigConstraintsBuildsAttrMap()
            throws Exception {
        // Arrange — a Domain target resolves its constraint entry to the global config.
        Domain domain = prov.get(DomainBy.name, "example.com");
        RightBearer admin = newGlobalAdmin(domain);
        Object collector = newCollectorWith(admin, domain, TargetType.domain);
        Method m = CollectEffectiveRights.class.getDeclaredMethod("fillDefaultAndConstratint",
                Set.class, AttrRight.class);
        m.setAccessible(true);

        Set<String> attrs = new HashSet<String>();
        attrs.add(Provisioning.A_zimbraId);

        // Act
        @SuppressWarnings("unchecked")
        SortedMap<String, RightCommand.EffectiveAttr> result =
                (SortedMap<String, RightCommand.EffectiveAttr>) m.invoke(collector, attrs,
                        AdminRight.PR_GET_ATTRS);

        // Assert — the requested attr is present in the built effective-attr map.
        assertNotNull(result);
        assertTrue(result.containsKey(Provisioning.A_zimbraId));
    }

    @Test
    public void collectAdminPresetRightsUserRightAceIsSkipped() throws Exception {
        // Arrange — a user right is never an admin preset right, so it is filtered out.
        Account target = prov.createAccount("cer-userright@example.com", "pw",
                new HashMap<String, Object>());
        Account grantor = prov.createAccount("cer-grantor@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer.Grantee grantee = newGranteeWithIds(grantor, grantor.getId());
        Object collector = newCollectorWith(grantee, target, TargetType.account);

        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectAdminPresetRights",
                List.class, TargetType.class, short.class, Integer.class, boolean.class,
                Map.class, Map.class);
        m.setAccessible(true);

        List<ZimbraACE> acl = new ArrayList<ZimbraACE>();
        acl.add(new ZimbraACE(grantor.getId(), GranteeType.GT_USER,
                new UserRight("invite"), null, null));
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();
        short flags = (short) (GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_ADMIN);

        // Act
        m.invoke(collector, acl, TargetType.account, flags, Integer.valueOf(1), false,
                allowed, denied);

        // Assert — user right is skipped; neither map records anything.
        assertTrue("user rights must not be collected as admin preset rights", allowed.isEmpty());
        assertTrue(denied.isEmpty());
    }

    /* A real admin preset right that is executable on the account target type. */
    private Right presetAccountRight() throws Exception {
        return RightManager.getInstance().getRight("deleteAccount");
    }

    /* A real admin combo right (never collected directly, only expanded into presets). */
    private Right comboAccountRight() throws Exception {
        return RightManager.getInstance().getRight("adminConsoleAccountRights");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectAdminPresetRightOnTargetIndividualUserGrantCollectsPresetRight()
            throws Exception {
        // Kills L308 (the F_INDIVIDUAL collectAdminPresetRights call) and L338 (preset right must
        // NOT be treated as a user right). A GT_USER ACE only matches the individual pass.
        Account target = prov.createAccount("cer-indiv@example.com", "pw",
                new HashMap<String, Object>());
        Account grantor = prov.createAccount("cer-indiv-grantor@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer.Grantee grantee = newGranteeWithIds(grantor, grantor.getId());
        Object collector = newCollectorWith(grantee, target, TargetType.account);

        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectAdminPresetRightOnTarget",
                List.class, TargetType.class, Integer.class, boolean.class, Map.class, Map.class);
        m.setAccessible(true);

        Right right = presetAccountRight();
        List<ZimbraACE> acl = new ArrayList<ZimbraACE>();
        acl.add(new ZimbraACE(grantor.getId(), GranteeType.GT_USER, right, null, null));
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();

        // Act
        m.invoke(collector, acl, TargetType.account, Integer.valueOf(1), false, allowed, denied);

        // Assert — collected through the individual pass with the supplied relativity.
        assertEquals("individual-pass preset right must be allowed", Integer.valueOf(1),
                allowed.get(right));
        assertTrue(denied.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectAdminPresetRightOnTargetGroupGrantCollectsViaGroupPass()
            throws Exception {
        // Kills L313 (the F_GROUP collectAdminPresetRights call). A GT_GROUP ACE only matches the
        // group-member pass; if that call is removed the right is never collected.
        Account target = prov.createAccount("cer-grp@example.com", "pw",
                new HashMap<String, Object>());
        Account grantor = prov.createAccount("cer-grp-grantor@example.com", "pw",
                new HashMap<String, Object>());
        String groupId = "group-id-1234";
        RightBearer.Grantee grantee = newGranteeWithIds(grantor, groupId);
        Object collector = newCollectorWith(grantee, target, TargetType.account);

        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectAdminPresetRightOnTarget",
                List.class, TargetType.class, Integer.class, boolean.class, Map.class, Map.class);
        m.setAccessible(true);

        Right right = presetAccountRight();
        List<ZimbraACE> acl = new ArrayList<ZimbraACE>();
        acl.add(new ZimbraACE(groupId, GranteeType.GT_GROUP, right, null, null));
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();

        // Act
        m.invoke(collector, acl, TargetType.account, Integer.valueOf(1), false, allowed, denied);

        // Assert — only the group pass can collect a GT_GROUP grant.
        assertEquals("group-pass preset right must be allowed", Integer.valueOf(1),
                allowed.get(right));
        assertTrue(denied.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectAdminPresetRightsGranteeTypeFlagsMismatchIsSkipped() throws Exception {
        // Kills L324 (granteeType.hasFlags guard). A GT_GROUP ace does NOT have F_INDIVIDUAL, so
        // when the individual flags are requested it must be skipped; a negated guard would collect.
        Account target = prov.createAccount("cer-flagmiss@example.com", "pw",
                new HashMap<String, Object>());
        Account grantor = prov.createAccount("cer-flagmiss-grantor@example.com", "pw",
                new HashMap<String, Object>());
        String groupId = "group-id-flag";
        RightBearer.Grantee grantee = newGranteeWithIds(grantor, groupId);
        Object collector = newCollectorWith(grantee, target, TargetType.account);

        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectAdminPresetRights",
                List.class, TargetType.class, short.class, Integer.class, boolean.class,
                Map.class, Map.class);
        m.setAccessible(true);

        List<ZimbraACE> acl = new ArrayList<ZimbraACE>();
        acl.add(new ZimbraACE(groupId, GranteeType.GT_GROUP, presetAccountRight(), null, null));
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();
        short individualFlags = (short) (GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_ADMIN);

        // Act — request the INDIVIDUAL pass against a GROUP ace.
        m.invoke(collector, acl, TargetType.account, individualFlags, Integer.valueOf(1), false,
                allowed, denied);

        // Assert — flag mismatch => nothing collected.
        assertTrue("group ace must not match individual flags", allowed.isEmpty());
        assertTrue(denied.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectAdminPresetRightsGranteeIdNotMatchingIsSkipped() throws Exception {
        // Kills L327 (matchesGrantee guard). The grantee id set does NOT contain the ace grantee,
        // so the ace must be skipped; a negated guard would collect a non-matching grant.
        Account target = prov.createAccount("cer-nomatch@example.com", "pw",
                new HashMap<String, Object>());
        Account grantor = prov.createAccount("cer-nomatch-grantor@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer.Grantee grantee = newGranteeWithIds(grantor, "some-other-id");
        Object collector = newCollectorWith(grantee, target, TargetType.account);

        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectAdminPresetRights",
                List.class, TargetType.class, short.class, Integer.class, boolean.class,
                Map.class, Map.class);
        m.setAccessible(true);

        List<ZimbraACE> acl = new ArrayList<ZimbraACE>();
        // ace grantee id "the-real-grantee" is not in the grantee's id set.
        acl.add(new ZimbraACE("the-real-grantee", GranteeType.GT_USER, presetAccountRight(),
                null, null));
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();
        short individualFlags = (short) (GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_ADMIN);

        // Act
        m.invoke(collector, acl, TargetType.account, individualFlags, Integer.valueOf(1), false,
                allowed, denied);

        // Assert — id mismatch => skipped.
        assertTrue("non-matching grantee must not be collected", allowed.isEmpty());
        assertTrue(denied.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectAdminPresetRightsPositiveGrantSubDomainMismatchIsSkipped()
            throws Exception {
        // Kills L331 (the !ace.deny() guard) and L332 (subDomain != ace.subDomain() guard).
        // A POSITIVE grant whose subDomain flag differs from the requested subDomain must be skipped.
        Account target = prov.createAccount("cer-subdom@example.com", "pw",
                new HashMap<String, Object>());
        Account grantor = prov.createAccount("cer-subdom-grantor@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer.Grantee grantee = newGranteeWithIds(grantor, grantor.getId());
        Object collector = newCollectorWith(grantee, target, TargetType.account);

        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectAdminPresetRights",
                List.class, TargetType.class, short.class, Integer.class, boolean.class,
                Map.class, Map.class);
        m.setAccessible(true);

        List<ZimbraACE> acl = new ArrayList<ZimbraACE>();
        // positive grant (rightModifier null => subDomain()==false).
        acl.add(new ZimbraACE(grantor.getId(), GranteeType.GT_USER, presetAccountRight(),
                null, null));
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();
        short individualFlags = (short) (GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_ADMIN);

        // Act — request subDomain=true while the grant is non-subDomain.
        m.invoke(collector, acl, TargetType.account, individualFlags, Integer.valueOf(1), true,
                allowed, denied);

        // Assert — positive grant with subDomain mismatch is dropped.
        assertTrue("positive grant with subDomain mismatch must be skipped", allowed.isEmpty());
        assertTrue(denied.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectAdminPresetRightsDenyGrantSubDomainMismatchStillCollected()
            throws Exception {
        // Kills L331: a DENY grant must bypass the subDomain check entirely and land in denied
        // even when subDomain mismatches. A negated !ace.deny() guard would (wrongly) apply the
        // subDomain check to deny grants and drop this one.
        Account target = prov.createAccount("cer-denysub@example.com", "pw",
                new HashMap<String, Object>());
        Account grantor = prov.createAccount("cer-denysub-grantor@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer.Grantee grantee = newGranteeWithIds(grantor, grantor.getId());
        Object collector = newCollectorWith(grantee, target, TargetType.account);

        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectAdminPresetRights",
                List.class, TargetType.class, short.class, Integer.class, boolean.class,
                Map.class, Map.class);
        m.setAccessible(true);

        Right right = presetAccountRight();
        List<ZimbraACE> acl = new ArrayList<ZimbraACE>();
        // DENY grant; subDomain()==false because modifier is RM_DENY not RM_SUBDOMAIN.
        acl.add(new ZimbraACE(grantor.getId(), GranteeType.GT_USER, right,
                RightModifier.RM_DENY, null));
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();
        short individualFlags = (short) (GranteeFlag.F_INDIVIDUAL | GranteeFlag.F_ADMIN);

        // Act — subDomain=true, but a deny grant must NOT be subjected to the subDomain check.
        m.invoke(collector, acl, TargetType.account, individualFlags, Integer.valueOf(7), true,
                allowed, denied);

        // Assert — deny grant collected into denied regardless of subDomain mismatch.
        assertEquals("deny grant must land in denied", Integer.valueOf(7), denied.get(right));
        assertTrue(allowed.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void collectPresetRightIfMoreRelevantEqualRelevanceDoesNotReplace() throws Exception {
        // Kills L362 boundary (relativity < mostRelevant vs <=). At EQUAL relativity the original
        // must NOT re-put; a <= mutant would put again. We detect the extra put with a counting map.
        Object collector = newCollector();
        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectPresetRightIfMoreRelevant",
                Right.class, boolean.class, Integer.class, Map.class, Map.class);
        m.setAccessible(true);

        Right right = new UserRight("viewFreeBusy");
        CountingMap allowed = new CountingMap();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();

        // First call: mostRelevant is null => one put.
        m.invoke(collector, right, Boolean.FALSE, Integer.valueOf(5), allowed, denied);
        int putsAfterFirst = allowed.puts;

        // Second call with the SAME (equal) relativity.
        m.invoke(collector, right, Boolean.FALSE, Integer.valueOf(5), allowed, denied);

        // Assert — no additional put on the equal case (original: 0 extra; <= mutant: 1 extra).
        assertEquals("equal relativity must not trigger a second put", putsAfterFirst, allowed.puts);
        assertEquals(Integer.valueOf(5), allowed.get(right));
    }

    /** A HashMap that counts how many times put() is invoked. */
    private static class CountingMap extends HashMap<Right, Integer> {
        private static final long serialVersionUID = 1L;

        private int puts = 0;

        @Override
        public Integer put(Right key, Integer value) {
            puts++;
            return super.put(key, value);
        }
    }

    @Test
    public void getAllExecutableAdminPresetRightsIncludesPresetExcludesUnexpandedCombo()
            throws Exception {
        // Kills L174 (r.isPresetRight() guard). The result must contain the executable PRESET right
        // and must NOT contain the (un-expanded) COMBO right itself.
        Account target = prov.createAccount("cer-presetonly@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer admin = newGlobalAdmin(target);
        Object collector = newCollectorWith(admin, target, TargetType.account);
        Method m = CollectEffectiveRights.class.getDeclaredMethod("getAllExecutableAdminPresetRights");
        m.setAccessible(true);

        // Act
        @SuppressWarnings("unchecked")
        Set<Right> rights = (Set<Right>) m.invoke(collector);

        // Assert
        assertTrue("executable preset right must be present",
                rights.contains(presetAccountRight()));
        assertFalse("un-expanded combo right must NOT be present",
                rights.contains(comboAccountRight()));
    }

    @Test
    public void fillDefaultAndConstratintSingleAndMultiDefaultsPopulatesDefaultValues()
            throws Exception {
        // Kills L410 (defaultValue instanceof String). A single-String default must surface in
        // getDefault(); a String[] default must surface both values. A negated guard would leave
        // the single-String default map empty.
        Map<String, Object> defaults = new HashMap<String, Object>();
        defaults.put(Provisioning.A_zimbraMailQuota, "12345");          // single String default
        defaults.put(Provisioning.A_zimbraMailAlias,
                new String[] {"a@example.com", "b@example.com"});     // String[] default
        Domain domain = new Domain("def-domain", "def-domain-id",
                new HashMap<String, Object>(), defaults, prov);
        RightBearer admin = newGlobalAdmin(prov.get(DomainBy.name, "example.com"));
        Object collector = newCollectorWith(admin, domain, TargetType.domain);

        Method m = CollectEffectiveRights.class.getDeclaredMethod("fillDefaultAndConstratint",
                Set.class, AttrRight.class);
        m.setAccessible(true);

        Set<String> attrs = new HashSet<String>();
        attrs.add(Provisioning.A_zimbraMailQuota);
        attrs.add(Provisioning.A_zimbraMailAlias);

        // Act — PR_GET_ATTRS avoids the forbidden-attr filter.
        @SuppressWarnings("unchecked")
        SortedMap<String, RightCommand.EffectiveAttr> result =
                (SortedMap<String, RightCommand.EffectiveAttr>) m.invoke(collector, attrs,
                        AdminRight.PR_GET_ATTRS);

        // Assert — single String default rendered.
        RightCommand.EffectiveAttr quota = result.get(Provisioning.A_zimbraMailQuota);
        assertNotNull(quota);
        assertTrue("single-String default must be present",
                quota.getDefault().contains("12345"));
        // String[] default: both values present.
        RightCommand.EffectiveAttr alias = result.get(Provisioning.A_zimbraMailAlias);
        assertEquals(2, alias.getDefault().size());
        assertTrue(alias.getDefault().contains("a@example.com"));
        assertTrue(alias.getDefault().contains("b@example.com"));
    }

    @Test
    public void fillDefaultAndConstratintConfiguredConstraintSetsNonNullConstraint()
            throws Exception {
        // Kills L399 (hasConstraints = constraints != null && !constraints.isEmpty()). With a real
        // constraint configured, hasConstraints must be true and the EffectiveAttr carries the
        // constraint object. A negated guard would force hasConstraints false => null constraint.
        com.zimbra.cs.account.Config config = prov.getConfig();
        config.setCachedData("CONSTRAINT_CACHE", null);
        Map<String, Object> cfgAttrs = new HashMap<String, Object>();
        cfgAttrs.put(Provisioning.A_zimbraConstraint, "zimbraMailQuota:max=999");
        prov.modifyAttrs(config, cfgAttrs);
        config.setCachedData("CONSTRAINT_CACHE", null);

        Domain domain = prov.get(DomainBy.name, "example.com");
        RightBearer admin = newGlobalAdmin(domain);
        Object collector = newCollectorWith(admin, domain, TargetType.domain);

        Method m = CollectEffectiveRights.class.getDeclaredMethod("fillDefaultAndConstratint",
                Set.class, AttrRight.class);
        m.setAccessible(true);

        Set<String> attrs = new HashSet<String>();
        attrs.add(Provisioning.A_zimbraMailQuota);

        try {
            // Act
            @SuppressWarnings("unchecked")
            SortedMap<String, RightCommand.EffectiveAttr> result =
                    (SortedMap<String, RightCommand.EffectiveAttr>) m.invoke(collector, attrs,
                            AdminRight.PR_GET_ATTRS);

            // Assert — the configured constraint is attached to the effective attr.
            RightCommand.EffectiveAttr ea = result.get(Provisioning.A_zimbraMailQuota);
            assertNotNull(ea);
            assertNotNull("configured constraint must be present on the effective attr",
                    ea.getConstraint());
        } finally {
            // Cleanup so other tests see no constraint. Setting the multi-valued attr to "" would
            // leave an empty-string value that later trips AttributeConstraint.fromString; null
            // removes the attribute outright so getMultiAttrSet returns an empty set.
            Map<String, Object> clear = new HashMap<String, Object>();
            clear.put(Provisioning.A_zimbraConstraint, null);
            prov.modifyAttrs(config, clear);
            config.setCachedData("CONSTRAINT_CACHE", null);
        }
    }

    @Test
    public void fillDefaultAndConstratintSetAttrsNonGlobalAdminForbiddenAttrIsSkipped()
            throws Exception {
        // Kills L402 (rightNeeded == PR_SET_ATTRS guard). For a non-global-admin asking for
        // PR_SET_ATTRS, the forbidden attr (zimbraIsAdminAccount) must be filtered out while a
        // normal attr stays. A negated == guard would let the forbidden attr through.
        Account target = prov.createAccount("cer-forbidden@example.com", "pw",
                new HashMap<String, Object>());
        Account grantor = prov.createAccount("cer-forbidden-grantor@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer.Grantee grantee = newGranteeWithIds(grantor, grantor.getId());
        Object collector = newCollectorWith(grantee, target, TargetType.account);

        Method m = CollectEffectiveRights.class.getDeclaredMethod("fillDefaultAndConstratint",
                Set.class, AttrRight.class);
        m.setAccessible(true);

        Set<String> attrs = new HashSet<String>();
        attrs.add(Provisioning.A_zimbraIsAdminAccount);   // forbidden
        attrs.add(Provisioning.A_zimbraMailQuota);        // normal

        // Act — PR_SET_ATTRS + non-global-admin triggers the forbidden filter.
        @SuppressWarnings("unchecked")
        SortedMap<String, RightCommand.EffectiveAttr> result =
                (SortedMap<String, RightCommand.EffectiveAttr>) m.invoke(collector, attrs,
                        AdminRight.PR_SET_ATTRS);

        // Assert — forbidden attr filtered, normal attr retained.
        assertFalse("forbidden attr must be filtered out of set-attrs",
                result.containsKey(Provisioning.A_zimbraIsAdminAccount));
        assertTrue("normal attr must remain",
                result.containsKey(Provisioning.A_zimbraMailQuota));
        prov.deleteAccount(target.getId());
        prov.deleteAccount(grantor.getId());
    }

    @Test
    public void collectAdminPresetRightOnTargetEmptyAclLeavesMapsEmpty() throws Exception {
        // Arrange
        Account target = prov.createAccount("cer-emptyacl@example.com", "pw",
                new HashMap<String, Object>());
        Account grantor = prov.createAccount("cer-grantor2@example.com", "pw",
                new HashMap<String, Object>());
        RightBearer.Grantee grantee = newGranteeWithIds(grantor, grantor.getId());
        Object collector = newCollectorWith(grantee, target, TargetType.account);

        Method m = CollectEffectiveRights.class.getDeclaredMethod("collectAdminPresetRightOnTarget",
                List.class, TargetType.class, Integer.class, boolean.class, Map.class, Map.class);
        m.setAccessible(true);

        List<ZimbraACE> acl = new ArrayList<ZimbraACE>();
        Map<Right, Integer> allowed = new HashMap<Right, Integer>();
        Map<Right, Integer> denied = new HashMap<Right, Integer>();

        // Act — exercises both the individual and group-member passes over an empty ACL.
        m.invoke(collector, acl, TargetType.account, Integer.valueOf(1), false, allowed, denied);

        // Assert
        assertTrue(allowed.isEmpty());
        assertTrue(denied.isEmpty());
    }

    /* Build a collector with explicit collaborators via the private 6-arg constructor. */
    private Object newCollectorWith(RightBearer rb, Entry target, TargetType tt) throws Exception {
        Constructor<CollectEffectiveRights> ctor = CollectEffectiveRights.class.getDeclaredConstructor(
                RightBearer.class, Entry.class, TargetType.class,
                boolean.class, boolean.class, RightCommand.EffectiveRights.class);
        ctor.setAccessible(true);
        return ctor.newInstance(rb, target, tt, false, false, newResult());
    }

    /* Build a Grantee without invoking its heavy ctor; populate only the fields used here. */
    @SuppressWarnings("unchecked")
    private RightBearer.Grantee newGranteeWithIds(Account acct, String... ids) throws Exception {
        RightBearer.Grantee grantee = (RightBearer.Grantee)
                sun.reflect.ReflectionFactory.getReflectionFactory()
                        .newConstructorForSerialization(RightBearer.Grantee.class,
                                Object.class.getDeclaredConstructor())
                        .newInstance();
        setField(grantee, "mRightBearer", acct);
        setField(grantee, "mGranteeType", GranteeType.GT_USER);
        Set<String> idSet = new HashSet<String>();
        for (String id : ids) {
            idSet.add(id);
        }
        setField(grantee, "mIdAndGroupIds", idSet);
        return grantee;
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field f = findField(target.getClass(), name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private Field findField(Class<?> c, String name) throws NoSuchFieldException {
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
