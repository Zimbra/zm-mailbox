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

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccessManager.ViaGrant;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link CheckPresetRight}. The full grant-resolution path requires real
 * LDAP-backed group membership, so these tests drive the reachable branches of the public
 * {@code check()} entry point against real {@link Account} domain objects from the in-memory
 * harness: the preset-right validation guard and the "no applicable grant" outcome.
 */
public class CheckPresetRightTest {

    private Provisioning prov;

    private Account target;

    private Account grantee;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        target = prov.createAccount("preset-target@example.com", "test123",
                new HashMap<String, Object>());
        grantee = prov.createAccount("preset-grantee@example.com", "test123",
                new HashMap<String, Object>());
    }

    @Test
    public void checkComboRightInsteadOfPresetThrowsInvalidRequest() throws Exception {
        // Arrange - a combo right is neither a preset nor a user right
        ComboRight combo = new ComboRight("myCombo");

        // Act / Assert
        try {
            CheckPresetRight.check(grantee, target, combo, false, null);
            fail("expected INVALID_REQUEST: CheckPresetRight can only check preset rights");
        } catch (ServiceException e) {
            assertTrue("message should explain only preset rights are checkable",
                    e.getMessage().toLowerCase().contains("preset"));
        }
    }

    @Test
    public void checkAttrRightInsteadOfPresetThrowsInvalidRequest() throws Exception {
        // Arrange - an attr right is also not a preset right
        AttrRight attrRight = new AttrRight("mySetAttrs", Right.RightType.setAttrs);

        // Act / Assert
        try {
            CheckPresetRight.check(grantee, target, attrRight, false, null);
            fail("expected INVALID_REQUEST for a non-preset (attr) right");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().toLowerCase().contains("preset"));
        }
    }

    @Test
    public void checkAdminPresetRightNonAdminGranteeReturnsNull() throws Exception {
        // Arrange - an admin preset right (isPresetRight==true, isUserRight==false). The grantee
        // is a plain account: not an admin and not a delegated admin, so it is not a valid
        // grantee for admin rights and the checker short-circuits to "no applicable grant".
        PresetRight adminRight = new PresetRight("listAccount");
        assertFalse("preset right must not be a user right", adminRight.isUserRight());
        assertTrue("preset right must report itself as preset", adminRight.isPresetRight());
        assertFalse("grantee must not be a delegated admin",
                grantee.getBooleanAttr(Provisioning.A_zimbraIsDelegatedAdminAccount, false));

        // Act
        Boolean result = CheckPresetRight.check(grantee, target, adminRight, false, null);

        // Assert - null means the non-admin grantee yields no applicable admin grant
        assertNull("non-admin grantee on an admin right -> null (grantee ignored)", result);
    }

    @Test
    public void checkAdminPresetRightDifferentTargetNonAdminGranteeStillNull() throws Exception {
        // Arrange - second target account; same non-admin grantee, different admin preset right
        Account otherTarget = prov.createAccount("preset-target2@example.com", "test123",
                new HashMap<String, Object>());
        PresetRight adminRight = new PresetRight("getAccountInfo");
        assertFalse("grantee must not be an admin account",
                grantee.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false));

        // Act
        Boolean result = CheckPresetRight.check(grantee, otherTarget, adminRight, false, null);

        // Assert
        assertNull("non-admin grantee -> null regardless of which target", result);

        // Cleanup
        prov.deleteAccount(otherTarget.getId());
    }

    @Test
    public void checkAdminPresetRightCanDelegateNeededNonAdminGranteeReturnsNull() throws Exception {
        // Arrange - exercise the canDelegateNeeded=true branch; the non-admin grantee guard fires
        // before any delegation logic, so the canDelegate flag does not change the outcome.
        PresetRight adminRight = new PresetRight("listAccount");

        // Act
        Boolean result = CheckPresetRight.check(grantee, target, adminRight, true, null);

        // Assert
        assertNull("canDelegate path with a non-admin grantee still yields null", result);
    }

    // ------------------------------------------------------------------
    // Real-grant tests that drive checkRight -> checkTarget -> checkPresetRight
    // -> matchesPresetRight -> gotResult end to end with a registered user right.
    // viewFreeBusy is a real user right (targetType=account) so the ACL survives
    // serialize/deserialize through RightManager.
    // ------------------------------------------------------------------

    /* A real, RightManager-registered user right granted directly on an account target. */
    private Right viewFreeBusy() throws Exception {
        return RightManager.getInstance().getUserRight("viewFreeBusy");
    }

    /* Grant {@code right} from {@code granteeAcct} (as a GT_USER) on {@code targetEntry}. */
    private void grantUser(Account targetEntry, Account granteeAcct, Right right,
            RightModifier modifier) throws Exception {
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(granteeAcct.getId(), GranteeType.GT_USER, right, modifier, null));
        ACLUtil.grantRight(prov, targetEntry, aces);
    }

    @Test
    public void checkAllowUserGrantDirectlyOnTargetReturnsTrueAndPopulatesVia() throws Exception {
        // Arrange - allow viewFreeBusy to the grantee directly on the target account.
        Right right = viewFreeBusy();
        grantUser(target, grantee, right, null);
        // Re-fetch so getAllACEs parses the persisted ACL.
        Account freshTarget = prov.get(Key.AccountBy.name, "preset-target@example.com");
        ViaGrant via = new ViaGrant();

        // Act - non-null via bypasses the permission cache, forcing the full resolution path.
        Boolean result = CheckPresetRight.check(grantee, freshTarget, right, false, via);

        // Assert - an allow grant whose grantee matches must resolve to TRUE (not null/false).
        // Kills: checkTarget adminFlag ternary (L270), F_INDIVIDUAL bit-or (L273), the
        // result returns (L187/L275), matchesPresetRight granteeFlag/subDomain checks
        // (L328/L341), checkPresetRight !matches continue (L387), matchesGrantee branch
        // (L395), and gotResult's allow -> TRUE return (L396).
        assertEquals(Boolean.TRUE, result);
        // gotResult populated the via record from the matching ACE (proves the allow branch ran).
        assertTrue("via must be populated for the matching grant", via.available());
        assertEquals("viewFreeBusy", via.getRight());
        assertFalse("an allow grant is not a negative grant", via.isNegativeGrant());
        assertEquals(grantee.getName(), via.getGranteeName());
    }

    @Test
    public void checkDenyUserGrantDirectlyOnTargetReturnsFalse() throws Exception {
        // Arrange - a DENY (negative) grant of viewFreeBusy to the grantee on the target.
        Right right = viewFreeBusy();
        grantUser(target, grantee, right, RightModifier.RM_DENY);
        Account freshTarget = prov.get(Key.AccountBy.name, "preset-target@example.com");
        ViaGrant via = new ViaGrant();

        // Act
        Boolean result = CheckPresetRight.check(grantee, freshTarget, right, false, via);

        // Assert - the deny grant must resolve to FALSE, distinct from the allow case's TRUE.
        // Kills the BooleanTrueReturnVals mutant at checkRight L187 (which would force TRUE) and
        // gotResult's deny branch (L457): a deny must NOT be reported as allowed.
        assertEquals(Boolean.FALSE, result);
        assertTrue("via must be populated for the matching deny grant", via.available());
        assertTrue("a deny grant must be reported as a negative grant", via.isNegativeGrant());
    }

    @Test
    public void checkRightMatchesButGranteeDoesNotSeenRightForcesFalseNotNull() throws Exception {
        // Arrange - grant viewFreeBusy to a THIRD account (not our grantee). For our grantee the
        // right + grantee-type flag match (so the right is "seen"), but the grantee identity does
        // not match, so no grant resolves. seenRight() then forces FALSE instead of null.
        Account other = prov.createAccount("preset-other@example.com", "test123",
                new HashMap<String, Object>());
        Right right = viewFreeBusy();
        grantUser(target, other, right, null);
        Account freshTarget = prov.get(Key.AccountBy.name, "preset-target@example.com");

        // Act
        Boolean result = CheckPresetRight.check(grantee, freshTarget, right, false, new ViaGrant());

        // Assert - seenRight==true (the right matched some ACE) but no grant applied to our
        // grantee -> FALSE. Kills: SeenRight.seenRight return (L57), checkPresetRight's
        // setSeenRight VoidMethodCall (L393), and checkRight's "if seenRight" negate (L257).
        // Without setSeenRight / with a flipped guard the result would be null.
        assertEquals("right seen but grantee unmatched must yield FALSE, not null",
                Boolean.FALSE, result);

        // Cleanup
        prov.deleteAccount(other.getId());
    }

    @Test
    public void checkNoGrantAtAllReturnsNull() throws Exception {
        // Arrange - no ACL on the target, a real user right whose grantee never appears.
        Right right = viewFreeBusy();
        Account freshTarget = prov.get(Key.AccountBy.name, "preset-target@example.com");

        // Act
        Boolean result = CheckPresetRight.check(grantee, freshTarget, right, false, new ViaGrant());

        // Assert - the right is never seen and no grant applies -> null. This is the counterpart
        // to the seenRight==true case above, pinning down the L257 branch on both sides.
        assertNull("no applicable grant and right never seen -> null", result);
    }

    @Test
    public void checkAllowGrantInheritedFromDomainReturnsTrueViaElseBranch() throws Exception {
        // Arrange - no grant on the account; grant viewFreeBusy on the account's DOMAIN. The
        // TargetIterator walks account -> domain, so the grant is found on the inherited (domain)
        // entry. A Domain is not a Group, so this drives checkRight's else branch: L244 (acl
        // != null so no "continue"), the subDomain computation (L248), and the inherited
        // checkTarget call (L249) that resolves the right.
        com.zimbra.cs.account.Domain domain = prov.get(
                com.zimbra.common.account.Key.DomainBy.name, "example.com");
        if (domain == null) {
            domain = prov.createDomain("example.com", new HashMap<String, Object>());
        }
        Right right = viewFreeBusy();
        Set<ZimbraACE> aces = new HashSet<ZimbraACE>();
        aces.add(new ZimbraACE(grantee.getId(), GranteeType.GT_USER, right, null, null));
        ACLUtil.grantRight(prov, domain, aces);
        Account freshTarget = prov.get(Key.AccountBy.name, "preset-target@example.com");
        ViaGrant via = new ViaGrant();

        // Act
        Boolean result = CheckPresetRight.check(grantee, freshTarget, right, false, via);

        // Assert - the domain-inherited allow grant must resolve to TRUE, and via must record
        // that the grant was found on the domain entry (proving the else/inherited path ran).
        assertEquals(Boolean.TRUE, result);
        assertTrue("via must be populated from the inherited domain grant", via.available());
        assertEquals("domain", via.getTargetType());

        // Cleanup - strip the ACL off the shared domain so other tests start clean.
        ACLUtil.revokeRight(prov, domain, aces);
    }

    @Test
    public void checkAllowGrantViaNullUsesCacheButStillReturnsTrueTwice() throws Exception {
        // Arrange - allow grant, but call with via == null so the cache path (L81/L87/L90) runs.
        Right right = viewFreeBusy();
        grantUser(target, grantee, right, null);
        Account freshTarget = prov.get(Key.AccountBy.name, "preset-target@example.com");

        // Act - first call computes + caches (cachePut, L90); second call may read the cache.
        Boolean first = CheckPresetRight.check(grantee, freshTarget, right, false, null);
        Boolean second = CheckPresetRight.check(grantee, freshTarget, right, false, null);

        // Assert - both the compute path and the (possibly) cached path must agree on TRUE.
        // Exercises the via == null branch of L81 and the cached==null/NOT_CACHED branch of L87.
        assertEquals("computed result for allow grant (via==null) must be TRUE", Boolean.TRUE, first);
        assertEquals("second lookup must agree with the first (cache consistent)", first, second);
    }

    @org.junit.After
    public void tearDown() throws Exception {
        Account t = prov.get(Key.AccountBy.name, "preset-target@example.com");
        if (t != null) {
            prov.deleteAccount(t.getId());
        }
        Account g = prov.get(Key.AccountBy.name, "preset-grantee@example.com");
        if (g != null) {
            prov.deleteAccount(g.getId());
        }
    }
}
