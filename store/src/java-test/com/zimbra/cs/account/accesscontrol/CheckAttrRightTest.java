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
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightBearer.Grantee;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link CheckAttrRight}. Computing accessible attributes for a real
 * grantee requires LDAP-backed group membership, so these tests cover the reachable surface:
 * the public {@code accessibleAttrs()} entry point with a null grantee (the deny-all guard)
 * over a real {@link Account} target, and the package-private {@code CollectAttrsResult} enum
 * semantics that drive the collect/compute phases.
 */
public class CheckAttrRightTest {

    /** Mirrors {@code ACLUtil.ACL_CACHE_KEY} (package-private) so we can cache a live ACL. */
    private static final String ACL_CACHE_KEY = "ENTRY.ACL_CACHE";

    private Provisioning prov;

    private Account target;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        // A real domain lets the TargetIterator yield an inherited target (the domain) after the
        // account itself, so grants on the domain exercise the inherited-target loop and the
        // relativity bump applied to it.
        if (prov.get(com.zimbra.common.account.Key.DomainBy.name, "example.com") == null) {
            prov.createDomain("example.com", new HashMap<String, Object>());
        }
        target = prov.createAccount("attr-target@example.com", "test123",
                new HashMap<String, Object>());
    }

    @org.junit.After
    public void tearDown() throws Exception {
        Account t = prov.get(com.zimbra.common.account.Key.AccountBy.name, "attr-target@example.com");
        if (t != null) {
            prov.deleteAccount(t.getId());
        }
    }

    @Test
    public void accessibleAttrsNullGranteeReturnsDenyAll() throws Exception {
        // Arrange - a real get-attrs right needed on a real account target
        AttrRight rightNeeded = new AttrRight("getSomeAttrs", Right.RightType.getAttrs);

        // Act - a null grantee short-circuits to deny-all
        AllowedAttrs result = CheckAttrRight.accessibleAttrs((Grantee) null, target,
                rightNeeded, false);

        // Assert
        assertEquals("null grantee must yield DENY_ALL",
                AllowedAttrs.Result.DENY_ALL, result.getResult());
        assertFalse("deny-all must reject any attribute", result.allowAttr("displayName"));
        assertNull("deny-all has no explicit allowed set", result.getAllowed());
    }

    @Test
    public void accessibleAttrsNullGranteeSetRightReturnsDenyAll() throws Exception {
        // Arrange - a set-attrs right, same null-grantee guard
        AttrRight rightNeeded = new AttrRight("setSomeAttrs", Right.RightType.setAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs((Grantee) null, target,
                rightNeeded, true);

        // Assert
        assertEquals(AllowedAttrs.Result.DENY_ALL, result.getResult());
        assertFalse(result.allowAttr("zimbraMailStatus"));
    }

    @Test
    public void collectAttrsResultSomeIsNotAll() throws Exception {
        // Arrange / Act - reflectively read the package-private inner enum value SOME
        Object some = enumValue("SOME");
        boolean isAll = invokeIsAll(some);

        // Assert
        assertFalse("SOME must report isAll()==false", isAll);
    }

    @Test
    public void collectAttrsResultAllowAllIsAll() throws Exception {
        // Arrange / Act
        Object allowAll = enumValue("ALLOW_ALL");

        // Assert
        assertTrue("ALLOW_ALL must report isAll()==true", invokeIsAll(allowAll));
    }

    @Test
    public void collectAttrsResultDenyAllIsAll() throws Exception {
        // Arrange / Act
        Object denyAll = enumValue("DENY_ALL");

        // Assert
        assertTrue("DENY_ALL must report isAll()==true", invokeIsAll(denyAll));
    }

    // ---- helpers for the package-private inner enum CollectAttrsResult ----

    private Class<?> collectAttrsResultClass() throws Exception {
        for (Class<?> c : CheckAttrRight.class.getDeclaredClasses()) {
            if (c.getSimpleName().equals("CollectAttrsResult")) {
                return c;
            }
        }
        throw new IllegalStateException("CollectAttrsResult enum not found");
    }

    private Object enumValue(String name) throws Exception {
        Object[] constants = collectAttrsResultClass().getEnumConstants();
        for (Object o : constants) {
            if (o.toString().equals(name)) {
                return o;
            }
        }
        throw new IllegalStateException("no enum constant " + name);
    }

    private boolean invokeIsAll(Object enumConst) throws Exception {
        Method m = collectAttrsResultClass().getDeclaredMethod("isAll");
        m.setAccessible(true);
        return (Boolean) m.invoke(enumConst);
    }

    // ---- functional tests that drive computeAccessibleAttrs with a real grantee + ACEs ----

    /*
     * Build a {@link Grantee} of type GT_USER without invoking the heavy constructor (which would
     * call getGroupMembershipWithRights, unsupported on the in-memory harness). The grantee's id
     * set and domain are populated directly so computeAccessibleAttrs / matchesGrantee work.
     */
    private Grantee userGrantee(Account acct) throws Exception {
        Grantee grantee = (Grantee) sun.reflect.ReflectionFactory.getReflectionFactory()
                .newConstructorForSerialization(Grantee.class,
                        Object.class.getDeclaredConstructor())
                .newInstance();
        setField(grantee, "mRightBearer", acct);
        setField(grantee, "mGranteeType", GranteeType.GT_USER);
        java.util.Set<String> ids = new java.util.HashSet<String>();
        ids.add(acct.getId());
        setField(grantee, "mIdAndGroupIds", ids);
        setField(grantee, "mGranteeDomain", prov.getDomain(acct));
        return grantee;
    }

    private void setField(Object obj, String name, Object value) throws Exception {
        Class<?> cls = Grantee.class;
        java.lang.reflect.Field f = null;
        while (cls != null) {
            try {
                f = cls.getDeclaredField(name);
                break;
            } catch (NoSuchFieldException e) {
                cls = cls.getSuperclass();
            }
        }
        if (f == null) {
            throw new NoSuchFieldException(name);
        }
        f.setAccessible(true);
        f.set(obj, value);
    }

    /* Build an attrs right on the account target type covering the given attrs (null = all). */
    private AttrRight attrRight(String name, Right.RightType type, String... attrs) throws Exception {
        AttrRight r = new AttrRight(name, type);
        r.setTargetType(TargetType.account);
        for (String a : attrs) {
            r.addAttr(a);
        }
        return r;
    }

    /*
     * Install one ACE on the target account by building a {@link ZimbraACL} from live ACE
     * objects and caching it directly on the entry under ACLUtil's cache key.
     *
     * <p>We deliberately bypass {@link ACLUtil#grantRight} here: that path serializes the ACL to
     * LDAP attribute strings and reconstructs each {@link Right} <em>by name</em> through
     * {@link RightManager} on the next read. The custom {@link AttrRight}/{@link ComboRight}
     * instances these tests create are not registered in RightManager, so they cannot survive the
     * serialize/deserialize round-trip. Caching the live ACL keeps the real Right objects intact so
     * {@link CheckAttrRight#accessibleAttrs} reads exactly what we granted, exercising the real
     * collect/compute logic.
     */
    private void grant(com.zimbra.cs.account.Entry on, String granteeId, GranteeType gt, Right right,
            RightModifier modifier) throws Exception {
        java.util.Set<ZimbraACE> aces = new java.util.LinkedHashSet<ZimbraACE>();
        ZimbraACL existing = (ZimbraACL) on.getCachedData(ACL_CACHE_KEY);
        if (existing != null) {
            aces.addAll(existing.getAllACEs());
        }
        aces.add(new ZimbraACE(granteeId, gt, right, modifier, null));
        ZimbraACL acl = new ZimbraACL(aces);
        on.setCachedData(ACL_CACHE_KEY, acl);
    }

    /*
     * Build a GT_USER grantee whose assumable-id set also contains {@code groupId}, so a GT_GROUP
     * ACE granted to that id matches the grantee. This lets one grantee match both an individual
     * (GT_USER) ACE and a group (GT_GROUP) ACE on the same target, exercising the two grantee
     * ranks within a single target.
     */
    private Grantee userGranteeWithGroup(Account acct, String groupId) throws Exception {
        Grantee grantee = (Grantee) sun.reflect.ReflectionFactory.getReflectionFactory()
                .newConstructorForSerialization(Grantee.class,
                        Object.class.getDeclaredConstructor())
                .newInstance();
        setField(grantee, "mRightBearer", acct);
        setField(grantee, "mGranteeType", GranteeType.GT_USER);
        java.util.Set<String> ids = new java.util.HashSet<String>();
        ids.add(acct.getId());
        ids.add(groupId);
        setField(grantee, "mIdAndGroupIds", ids);
        setField(grantee, "mGranteeDomain", prov.getDomain(acct));
        return grantee;
    }

    /* Build an attrs right on an arbitrary target type covering the given attrs (none = all). */
    private AttrRight attrRightOn(String name, Right.RightType type, TargetType tt, String... attrs)
            throws Exception {
        AttrRight r = new AttrRight(name, type);
        r.setTargetType(tt);
        for (String a : attrs) {
            r.addAttr(a);
        }
        return r;
    }

    @Test
    public void accessibleAttrsGrantedSomeGetAttrsAllowsOnlyThoseAttrs() throws Exception {
        // Arrange — grant a get-attrs right covering two specific attrs to the grantee.
        Account grantee = prov.createAccount("attr-grantee@example.com", "pw",
                new HashMap<String, Object>());
        AttrRight granted = attrRight("getSome", Right.RightType.getAttrs,
                "displayName", "description");
        grant(target, grantee.getId(), GranteeType.GT_USER, granted, null);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — only the two granted attrs are allowed.
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertTrue("displayName must be allowed", result.allowAttr("displayName"));
        assertTrue("description must be allowed", result.allowAttr("description"));
        assertFalse("an ungranted attr must not be allowed", result.allowAttr("zimbraMailStatus"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsGrantedAllGetAttrsAllowsAll() throws Exception {
        // Arrange — an all-attrs get right (no specific attrs) on the account target.
        Account grantee = prov.createAccount("attr-grantee-all@example.com", "pw",
                new HashMap<String, Object>());
        AttrRight granted = attrRight("getAll", Right.RightType.getAttrs);
        grant(target, grantee.getId(), GranteeType.GT_USER, granted, null);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — allow-all yields true for any attr and a null explicit set.
        assertEquals(AllowedAttrs.Result.ALLOW_ALL, result.getResult());
        assertTrue(result.allowAttr("anyAttributeAtAll"));
        assertNull(result.getAllowed());

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsDenyAllGetAttrsDeniesAll() throws Exception {
        // Arrange — a negative all-attrs get right denies everything.
        Account grantee = prov.createAccount("attr-grantee-deny@example.com", "pw",
                new HashMap<String, Object>());
        AttrRight granted = attrRight("denyAll", Right.RightType.getAttrs);
        grant(target, grantee.getId(), GranteeType.GT_USER, granted, RightModifier.RM_DENY);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert
        assertEquals(AllowedAttrs.Result.DENY_ALL, result.getResult());
        assertFalse(result.allowAttr("displayName"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsAllowAllMinusDenySomeExcludesDeniedAttrs() throws Exception {
        // Arrange — allow all, but deny a specific attr (allow-all minus the denied set).
        Account grantee = prov.createAccount("attr-grantee-mix@example.com", "pw",
                new HashMap<String, Object>());
        AttrRight allowAll = attrRight("allowAll", Right.RightType.getAttrs);
        AttrRight denySome = attrRight("denyDisplay", Right.RightType.getAttrs, "displayName");
        grant(target, grantee.getId(), GranteeType.GT_USER, allowAll, null);
        grant(target, grantee.getId(), GranteeType.GT_USER, denySome, RightModifier.RM_DENY);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — allow-some that excludes the explicitly-denied attr.
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertFalse("explicitly denied attr must be excluded", result.allowAttr("displayName"));
        assertTrue("a non-denied attr remains allowed", result.allowAttr("zimbraMailStatus"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsDenyMoreRelevantThanAllowRemovesConflict() throws Exception {
        // Arrange — same target/relativity: grant allow+deny on the same attr. Deny wins when
        // its distance is <= the allow distance.
        Account grantee = prov.createAccount("attr-grantee-conflict@example.com", "pw",
                new HashMap<String, Object>());
        AttrRight allow = attrRight("allowOne", Right.RightType.getAttrs, "displayName", "cn");
        AttrRight deny = attrRight("denyOne", Right.RightType.getAttrs, "displayName");
        grant(target, grantee.getId(), GranteeType.GT_USER, allow, null);
        grant(target, grantee.getId(), GranteeType.GT_USER, deny, RightModifier.RM_DENY);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — conflicting attr removed from allowed, the other allowed attr remains.
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertFalse("conflicting denied attr removed", result.allowAttr("displayName"));
        assertTrue("non-conflicting allowed attr remains", result.allowAttr("cn"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsSetRightSatisfiesGetNeededAllowsAttr() throws Exception {
        // Arrange — a set-attrs grant is suitable for a get-attrs need (suitableFor()).
        Account grantee = prov.createAccount("attr-grantee-set@example.com", "pw",
                new HashMap<String, Object>());
        AttrRight setGrant = attrRight("setSome", Right.RightType.setAttrs, "givenName");
        grant(target, grantee.getId(), GranteeType.GT_USER, setGrant, null);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — positive set grant automatically gives get on that attr.
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertTrue("set grant covers the get need", result.allowAttr("givenName"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsGranteeNotMatchingAceReturnsNoAttrs() throws Exception {
        // Arrange — grant to a different grantee id; our grantee matches nothing.
        Account other = prov.createAccount("attr-other@example.com", "pw",
                new HashMap<String, Object>());
        Account grantee = prov.createAccount("attr-grantee-nomatch@example.com", "pw",
                new HashMap<String, Object>());
        AttrRight granted = attrRight("getSome", Right.RightType.getAttrs, "displayName");
        grant(target, other.getId(), GranteeType.GT_USER, granted, null);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — no ACE matched, so the allow set is empty (allow-some with nothing).
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertFalse(result.allowAttr("displayName"));

        prov.deleteAccount(other.getId());
        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsNoAcesOnTargetReturnsNoAttrs() throws Exception {
        // Arrange — a real grantee but no grants at all on the target.
        Account grantee = prov.createAccount("attr-grantee-noaces@example.com", "pw",
                new HashMap<String, Object>());
        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — collecting found nothing; allow-some with an empty allowed set.
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertFalse(result.allowAttr("displayName"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsComboRightExpandsAttrsAllowsContainedAttrs() throws Exception {
        // Arrange — a combo right that contains an attr right; its attrs expand into the allow set.
        Account grantee = prov.createAccount("attr-grantee-combo@example.com", "pw",
                new HashMap<String, Object>());
        ComboRight combo = new ComboRight("comboGet");
        combo.setDesc("combo for test");
        AttrRight inner = attrRight("innerGet", Right.RightType.getAttrs, "displayName");
        combo.addRight(inner);
        combo.completeRight(); // expands contained attr rights into getAttrRights()
        grant(target, grantee.getId(), GranteeType.GT_USER, combo, null);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — the combo's contained attr right is expanded and allowed.
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertTrue("combo-contained attr is allowed", result.allowAttr("displayName"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsNegativeSetGrantForGetNeedIgnored() throws Exception {
        // Arrange — a negative set-attrs grant must NOT deny a get-attrs need (right types differ).
        Account grantee = prov.createAccount("attr-grantee-negset@example.com", "pw",
                new HashMap<String, Object>());
        AttrRight denySet = attrRight("denySetAll", Right.RightType.setAttrs);
        grant(target, grantee.getId(), GranteeType.GT_USER, denySet, RightModifier.RM_DENY);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — the negative set grant is ignored for a get need, leaving nothing collected.
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertFalse(result.allowAttr("displayName"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsIndividualDenyBeatsGroupAllowOnSameTarget() throws Exception {
        // Arrange — on ONE target, the same attr is denied by an INDIVIDUAL (GT_USER) grant and
        // allowed by a GROUP (GT_GROUP) grant. checkTarget visits individuals at 'relativity' and
        // group members at 'relativity+1' (L239), so the deny is more relevant (smaller distance)
        // and wins: the attr ends up DENIED. This pins the per-target grantee-rank ordering AND the
        // individual/group flag routing in expandACLToAttrs.
        Account grantee = prov.createAccount("attr-grantee-rank@example.com", "pw",
                new HashMap<String, Object>());
        String groupId = "group-" + grantee.getId();

        AttrRight indivDeny = attrRight("indivDeny", Right.RightType.getAttrs, "displayName");
        AttrRight groupAllow = attrRight("groupAllow", Right.RightType.getAttrs, "displayName");
        grant(target, grantee.getId(), GranteeType.GT_USER, indivDeny, RightModifier.RM_DENY);
        grant(target, groupId, GranteeType.GT_GROUP, groupAllow, null);

        Grantee g = userGranteeWithGroup(grantee, groupId);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — individual deny (distance R) <= group allow (distance R+1) => attr removed.
        // Mutating L239 to relativity-1 would make the allow more relevant and leave it ALLOWED;
        // negating the L293 grantee-flag filter would swap which rank processes each ACE and also
        // leave the attr ALLOWED. Both must yield DENIED here.
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertFalse("individual deny must beat the less-relevant group allow",
                result.allowAttr("displayName"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsGroupOnlyAllowIsCollectedViaGroupRank() throws Exception {
        // Arrange — a lone GT_GROUP allow. It is filtered out of the individual pass (its flags lack
        // F_INDIVIDUAL/F_ADMIN) and collected in the group pass. Confirms the group rank actually
        // collects the attr (complements the conflict test above).
        Account grantee = prov.createAccount("attr-grantee-grponly@example.com", "pw",
                new HashMap<String, Object>());
        String groupId = "grp-" + grantee.getId();
        AttrRight groupAllow = attrRight("grpAllow", Right.RightType.getAttrs, "displayName");
        grant(target, groupId, GranteeType.GT_GROUP, groupAllow, null);

        Grantee g = userGranteeWithGroup(grantee, groupId);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertTrue("group grant must allow its attr", result.allowAttr("displayName"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsComboAllowAllInnerShortCircuitsToAllowAll() throws Exception {
        // Arrange — a combo right whose inner attr right covers ALL attrs (no specific attrs). When
        // expandAttrsGrantToAttrs returns ALLOW_ALL for that inner right, the combo loop must
        // short-circuit and propagate ALLOW_ALL (L337: 'if (result.isAll()) return result').
        Account grantee = prov.createAccount("attr-grantee-comboall@example.com", "pw",
                new HashMap<String, Object>());
        ComboRight combo = new ComboRight("comboGetAll");
        combo.setDesc("combo all-attrs for test");
        AttrRight innerAll = attrRight("innerGetAll", Right.RightType.getAttrs); // no attrs => allAttrs
        combo.addRight(innerAll);
        combo.completeRight();
        grant(target, grantee.getId(), GranteeType.GT_USER, combo, null);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — ALLOW_ALL, not a degraded ALLOW_SOME with an empty set. Negating L337 would skip
        // the short-circuit, fall through to CollectAttrsResult.SOME, and yield ALLOW_SOME(empty).
        assertEquals(AllowedAttrs.Result.ALLOW_ALL, result.getResult());
        assertTrue("allow-all must permit any attribute", result.allowAttr("anyAttributeAtAll"));
        assertNull("allow-all carries no explicit set", result.getAllowed());

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsGrantedTargetTypesDisjointFromNeededNotCollected() throws Exception {
        // Arrange — the granted attr right applies to the account target (passes the
        // executable-on-target check), but the NEEDED right's target types are disjoint from the
        // granted right's. L412/L414 must drop the grant because the target-type sets don't
        // intersect. Negating the L412 guard would skip that intersection check and wrongly collect
        // the attr.
        Account grantee = prov.createAccount("attr-grantee-disjoint@example.com", "pw",
                new HashMap<String, Object>());
        AttrRight granted = attrRightOn("grantedAcct", Right.RightType.getAttrs,
                TargetType.account, "displayName");
        grant(target, grantee.getId(), GranteeType.GT_USER, granted, null);

        Grantee g = userGrantee(grantee);
        // needed right's target type is cos — disjoint from the granted right's {account}
        AttrRight needed = attrRightOn("neededCos", Right.RightType.getAttrs, TargetType.cos);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — disjoint target types => the grant is ignored, attr not allowed.
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertFalse("disjoint target-type grant must not be collected",
                result.allowAttr("displayName"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsSubDomainPositiveGrantOnAccountIsSkipped() throws Exception {
        // Arrange — on an ACCOUNT target, subDomain==false. A positive grant carrying the
        // subDomain modifier (ace.subDomain()==true) must be skipped because 'subDomain !=
        // ace.subDomain()' for a non-deny grant (L313). A normal positive grant on another attr is
        // collected, proving the skip is selective.
        Account grantee = prov.createAccount("attr-grantee-subdom@example.com", "pw",
                new HashMap<String, Object>());
        AttrRight subDomainGrant = attrRight("subdom", Right.RightType.getAttrs, "displayName");
        AttrRight normalGrant = attrRight("normal", Right.RightType.getAttrs, "cn");
        grant(target, grantee.getId(), GranteeType.GT_USER, subDomainGrant, RightModifier.RM_SUBDOMAIN);
        grant(target, grantee.getId(), GranteeType.GT_USER, normalGrant, null);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        // Act
        AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

        // Assert — the subDomain grant is ignored on an account target; the normal grant survives.
        // Negating L313 would process the subDomain grant and wrongly allow displayName.
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
        assertFalse("subDomain-only positive grant must be skipped on an account target",
                result.allowAttr("displayName"));
        assertTrue("the normal positive grant must still be collected", result.allowAttr("cn"));

        prov.deleteAccount(grantee.getId());
    }

    @Test
    public void accessibleAttrsDomainAllowEntersInheritedLoopWhenTargetIsSome() throws Exception {
        // Arrange — the target account grants SOME attrs (car==SOME), and the account's DOMAIN
        // grants a DIFFERENT attr. The inherited-target loop must run (gated by L111 '!car.isAll()')
        // so the domain attr is collected. Negating L111 would skip the loop and lose the domain
        // attr.
        Account grantee = prov.createAccount("attr-grantee-domloop@example.com", "pw",
                new HashMap<String, Object>());
        com.zimbra.cs.account.Domain domain = prov.getDomain(target);
        domain.setCachedData(ACL_CACHE_KEY, null); // ensure no leaked domain ACL from prior tests

        AttrRight onAccount = attrRight("acctSome", Right.RightType.getAttrs, "cn");
        AttrRight onDomain = attrRight("domSome", Right.RightType.getAttrs, "displayName");
        grant(target, grantee.getId(), GranteeType.GT_USER, onAccount, null);
        grant(domain, grantee.getId(), GranteeType.GT_USER, onDomain, null);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        try {
            // Act
            AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

            // Assert — both the account attr and the domain attr are allowed (loop ran).
            assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
            assertTrue("account-granted attr allowed", result.allowAttr("cn"));
            assertTrue("domain-inherited attr allowed (inherited loop must run)",
                    result.allowAttr("displayName"));
        } finally {
            domain.setCachedData(ACL_CACHE_KEY, null);
            prov.deleteAccount(grantee.getId());
        }
    }

    @Test
    public void accessibleAttrsTargetDenyBeatsDomainAllowViaRelativityBump() throws Exception {
        // Arrange — the same attr is DENIED on the target account and ALLOWED on the account's
        // domain. After visiting the target, relativity is bumped by 2 (L102) so the domain grant is
        // strictly less relevant; the nearer deny wins and the attr is DENIED. Mutating the bump to
        // a subtraction would make the domain allow appear more (or equally) relevant and leave the
        // attr ALLOWED.
        Account grantee = prov.createAccount("attr-grantee-relbump@example.com", "pw",
                new HashMap<String, Object>());
        com.zimbra.cs.account.Domain domain = prov.getDomain(target);
        domain.setCachedData(ACL_CACHE_KEY, null); // ensure no leaked domain ACL from prior tests

        AttrRight targetDeny = attrRight("tgtDeny", Right.RightType.getAttrs, "displayName");
        AttrRight domainAllow = attrRight("domAllow", Right.RightType.getAttrs, "displayName");
        grant(target, grantee.getId(), GranteeType.GT_USER, targetDeny, RightModifier.RM_DENY);
        grant(domain, grantee.getId(), GranteeType.GT_USER, domainAllow, null);

        Grantee g = userGrantee(grantee);
        AttrRight needed = attrRight("getNeeded", Right.RightType.getAttrs);

        try {
            // Act
            AllowedAttrs result = CheckAttrRight.accessibleAttrs(g, target, needed, false);

            // Assert — nearer deny (target) beats farther allow (domain).
            assertEquals(AllowedAttrs.Result.ALLOW_SOME, result.getResult());
            assertFalse("nearer target deny must beat the farther domain allow",
                    result.allowAttr("displayName"));
        } finally {
            domain.setCachedData(ACL_CACHE_KEY, null);
            prov.deleteAccount(grantee.getId());
        }
    }
}
