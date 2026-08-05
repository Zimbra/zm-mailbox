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
import com.zimbra.cs.account.accesscontrol.Right.RightType;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link AttrRight}. Real {@link AttrRight} instances are built via the
 * concrete subclass {@link InlineAttrRight#newInlineAttrRight(String)} (which runs the full
 * parse -&gt; setTargetType -&gt; addAttr -&gt; completeRight workflow against the live
 * {@link com.zimbra.cs.account.AttributeManager}), then exercised for classification, target
 * types, attr membership, overlap, suitability, and the SOAP target-type string. Both valid and
 * error paths are covered.
 */
public class AttrRightFunctionalTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    private AttrRight getAccountAttr(String op, String attr) throws ServiceException {
        // op is "get" or "set"; builds an inline attr right on the account target type
        return InlineAttrRight.newInlineAttrRight(op + ".account." + attr);
    }

    @Test
    public void isAttrRightInlineAttrRightReturnsTrue() throws Exception {
        // Arrange / Act
        AttrRight r = getAccountAttr("get", "displayName");

        // Assert
        assertTrue("attr right must classify as attr right", r.isAttrRight());
        assertFalse(r.isPresetRight());
        assertFalse(r.isComboRight());
    }

    @Test
    public void getRightTypeGetInlineRightIsGetAttrs() throws Exception {
        // Arrange / Act
        AttrRight r = getAccountAttr("get", "displayName");

        // Assert
        assertEquals(RightType.getAttrs, r.getRightType());
    }

    @Test
    public void getRightTypeSetInlineRightIsSetAttrs() throws Exception {
        // Arrange / Act
        AttrRight r = getAccountAttr("set", "displayName");

        // Assert
        assertEquals(RightType.setAttrs, r.getRightType());
    }

    @Test
    public void getTargetTypesAccountInlineRightContainsAccount() throws Exception {
        // Arrange / Act
        AttrRight r = getAccountAttr("get", "displayName");

        // Assert
        Set<TargetType> tts = r.getTargetTypes();
        assertEquals(1, tts.size());
        assertTrue(tts.contains(TargetType.account));
    }

    @Test
    public void executableOnTargetTypeMatchingTypeTrueOtherwiseFalse() throws Exception {
        // Arrange
        AttrRight r = getAccountAttr("get", "displayName");

        // Act / Assert
        assertTrue("executable on its own target type", r.executableOnTargetType(TargetType.account));
        assertFalse("not executable on unrelated type", r.executableOnTargetType(TargetType.domain));
    }

    @Test
    public void allAttrsAndGetAttrsSpecificAttrNotAllAndContainsAttr() throws Exception {
        // Arrange / Act
        AttrRight r = getAccountAttr("get", "displayName");

        // Assert - a specific attr was added, so it is NOT all-attrs
        assertFalse("specific-attr right must not be allAttrs", r.allAttrs());
        assertTrue("attr set must contain the added attr", r.getAttrs().contains("displayName"));
    }

    @Test
    public void getTargetTypeAttrRightThrowsInternalError() throws Exception {
        // Arrange
        AttrRight r = getAccountAttr("get", "displayName");

        // Act / Assert - AttrRight overrides getTargetType() to always fail
        try {
            r.getTargetType();
            fail("expected ServiceException from AttrRight.getTargetType");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    @Test
    public void getTargetTypeStrAccountInlineRightReturnsAccountCode() throws Exception {
        // Arrange
        AttrRight r = getAccountAttr("get", "displayName");

        // Act / Assert - single target type -> its code, no comma
        assertEquals(TargetType.account.getCode(), r.getTargetTypeStr());
    }

    @Test
    public void suitableForSetRightForGetNeedIsSuitable() throws Exception {
        // Arrange - a setAttrs right
        AttrRight setRight = getAccountAttr("set", "displayName");

        // Act / Assert - set satisfies a get need; and satisfies its own set need
        assertTrue("set right is suitable for a get need", setRight.suitableFor(RightType.getAttrs));
        assertTrue("set right is suitable for a set need", setRight.suitableFor(RightType.setAttrs));
    }

    @Test
    public void suitableForGetRightForSetNeedIsNotSuitable() throws Exception {
        // Arrange - a getAttrs right
        AttrRight getRight = getAccountAttr("get", "displayName");

        // Act / Assert - a get right cannot satisfy a set need
        assertFalse("get right must not be suitable for a set need",
                getRight.suitableFor(RightType.setAttrs));
    }

    @Test
    public void overlapsSameAttrSameTargetReturnsTrue() throws Exception {
        // Arrange - two attr rights on the same attr/target overlap
        AttrRight a = getAccountAttr("get", "displayName");
        AttrRight b = getAccountAttr("get", "displayName");

        // Act / Assert
        assertTrue("rights on the same attr+target must overlap", a.overlaps(b));
    }

    @Test
    public void overlapsDifferentAttrSameTargetReturnsFalse() throws Exception {
        // Arrange - same target type but disjoint attr sets
        AttrRight a = getAccountAttr("get", "displayName");
        AttrRight b = getAccountAttr("get", "description");

        // Act / Assert - disjoint attrs do not overlap
        assertFalse("disjoint attr rights must not overlap", a.overlaps(b));
    }

    @Test
    public void overlapsAttrRightVsPresetReturnsFalse() throws Exception {
        // Arrange - an attr right and a real preset right from the manager
        AttrRight attr = getAccountAttr("get", "displayName");
        Right preset = null;
        for (Right r : RightManager.getInstance().getAllAdminRights().values()) {
            if (r.isPresetRight()) {
                preset = r;
                break;
            }
        }
        org.junit.Assume.assumeTrue("need a preset right", preset != null);

        // Act / Assert - attr never overlaps a preset
        assertFalse("attr right must not overlap a preset right", attr.overlaps(preset));
    }

    @Test
    public void grantableOnTargetTypeDomainInheritsAccountReturnsTrue() throws Exception {
        // Arrange - an account attr right; account inherits from domain
        AttrRight r = getAccountAttr("get", "displayName");

        // Act / Assert - domain can grant an account-scoped attr right
        assertTrue("domain should be grantable for an account attr right",
                r.grantableOnTargetType(TargetType.domain));
    }

    @Test
    public void newInlineAttrRightMalformedStringThrowsParseError() {
        // Act / Assert - missing the third part
        try {
            InlineAttrRight.newInlineAttrRight("get.account");
            fail("expected ServiceException for malformed inline right");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
        }
    }

    // ---------- direct AttrRight construction (all-attrs and multi target type) ----------

    // Build a bare attr right (no specific attr -> all-attrs) on a single target type.
    private AttrRight allAttrsRight(Right.RightType type, TargetType tt) throws ServiceException {
        AttrRight r = new AttrRight("allattrs." + type + "." + tt.getCode(), type);
        r.setDesc("all-attrs right for " + tt.getCode());
        r.setTargetType(tt);
        r.completeRight();
        return r;
    }

    @Test
    public void constructorNonAttrRightTypeThrowsFailure() {
        // Act / Assert - only getAttrs/setAttrs are allowed for AttrRight
        try {
            new AttrRight("bad.combo", Right.RightType.combo);
            fail("expected ServiceException for combo right type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    @Test
    public void allAttrsNoSpecificAttrAddedReturnsTrue() throws Exception {
        // Arrange / Act - a right with no addAttr() is an all-attrs right
        AttrRight r = allAttrsRight(Right.RightType.getAttrs, TargetType.account);

        // Assert
        assertTrue("right with no specific attrs is all-attrs", r.allAttrs());
    }

    @Test
    public void getAllAttrsAllAttrsSingleTargetReturnsClassAttrs() throws Exception {
        // Arrange - all-attrs right on a single target type
        AttrRight r = allAttrsRight(Right.RightType.getAttrs, TargetType.account);

        // Act - all attrs in the account attribute class
        Set<String> attrs = r.getAllAttrs();

        // Assert - a real, non-empty set including a well-known account attr
        assertFalse("account attribute class is not empty", attrs.isEmpty());
        assertTrue("account class includes displayName", attrs.contains("displayName"));
    }

    @Test
    public void getAllAttrsNotAllAttrsThrowsFailure() throws Exception {
        // Arrange - a specific-attr right is NOT all-attrs
        AttrRight r = getAccountAttr("get", "displayName");
        assertFalse("precondition: not all-attrs", r.allAttrs());

        // Act / Assert - getAllAttrs is only valid for all-attrs rights
        try {
            r.getAllAttrs();
            fail("expected FAILURE when calling getAllAttrs on a specific-attr right");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    @Test
    public void dumpAllAttrsRightReportsAllAttrsAndTargetType() throws Exception {
        // Arrange
        AttrRight r = allAttrsRight(Right.RightType.getAttrs, TargetType.account);

        // Act
        String out = r.dump(new StringBuilder());

        // Assert - all-attrs rights print the "all attrs" marker and their target type
        assertTrue("dump should mention target type account", out.contains("account"));
        assertTrue("dump should mention all attrs", out.contains("all attrs"));
    }

    @Test
    public void dumpSpecificAttrRightListsTheAttr() throws Exception {
        // Arrange
        AttrRight r = getAccountAttr("get", "displayName");

        // Act
        String out = r.dump(new StringBuilder());

        // Assert - specific-attr rights enumerate the attrs section
        assertTrue("dump should list attrs section", out.contains("attrs:"));
        assertTrue("dump should include the specific attr", out.contains("displayName"));
    }

    @Test
    public void completeRightNoTargetTypeThrowsParseError() throws Exception {
        // Arrange - an attr right with NO target type set at all
        AttrRight r = new AttrRight("notarget.get", Right.RightType.getAttrs);

        // Act / Assert
        try {
            r.completeRight();
            fail("expected PARSE_ERROR for missing target type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
        }
    }

    @Test
    public void completeRightAllAttrsMultipleTargetTypesThrowsParseError() throws Exception {
        // Arrange - all-attrs right with two target types is illegal
        AttrRight r = new AttrRight("allattrs.multi", Right.RightType.getAttrs);
        r.setTargetType(TargetType.account);
        r.setTargetType(TargetType.domain);

        // Act / Assert
        try {
            r.completeRight();
            fail("expected PARSE_ERROR for all-attrs right with multiple target types");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
        }
    }

    @Test
    public void addAttrForbiddenAttrOnSetRightThrowsPermDenied() throws Exception {
        // Arrange - a setAttrs right; zimbraIsAdminAccount is hard-forbidden for delegated admin
        AttrRight r = new AttrRight("set.account.admin", Right.RightType.setAttrs);
        r.setTargetType(TargetType.account);

        // Act / Assert
        try {
            r.addAttr(com.zimbra.cs.account.Provisioning.A_zimbraIsAdminAccount);
            fail("expected PERM_DENIED for forbidden attr on a set right");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PERM_DENIED, e.getCode());
        }
    }

    @Test
    public void addAttrForbiddenAttrOnGetRightIsAllowed() throws Exception {
        // Arrange - a getAttrs right does NOT run the forbidden-attr check
        AttrRight r = new AttrRight("get.account.admin", Right.RightType.getAttrs);
        r.setTargetType(TargetType.account);

        // Act - forbidden check only applies to setAttrs, so this succeeds
        r.addAttr(com.zimbra.cs.account.Provisioning.A_zimbraIsAdminAccount);

        // Assert - attr was recorded and the right is no longer all-attrs
        assertFalse("specific attr added => not all-attrs", r.allAttrs());
        assertTrue("attr set contains the added attr",
                r.getAttrs().contains(com.zimbra.cs.account.Provisioning.A_zimbraIsAdminAccount));
    }

    @Test
    public void validateAttrUnknownAttrThrowsFailure() throws Exception {
        // Arrange
        AttrRight r = new AttrRight("get.account.bogus", Right.RightType.getAttrs);
        r.setTargetType(TargetType.account);

        // Act / Assert - an attr not present on the target's attribute class is rejected
        try {
            r.validateAttr("thisAttrDoesNotExistAnywhere");
            fail("expected FAILURE for an attr not on the target type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    @Test
    public void validateAttrValidAttrDoesNotThrow() throws Exception {
        // Arrange
        AttrRight r = new AttrRight("get.account.valid", Right.RightType.getAttrs);
        r.setTargetType(TargetType.account);

        // Act - displayName is on the account class, so validation passes silently
        r.validateAttr("displayName");

        // Assert - reaching here means no exception
        assertTrue("valid attr validated without exception", true);
    }

    @Test
    public void getGrantableTargetTypesAccountRightIncludesInheritingTypes() throws Exception {
        // Arrange - an account attr right
        AttrRight r = getAccountAttr("get", "displayName");

        // Act - target types from which the account target type can inherit
        Set<TargetType> grantable = r.getGrantableTargetTypes();

        // Assert - account inherits from domain and global, so they must be grantable here
        assertFalse("grantable target types must not be empty", grantable.isEmpty());
        assertTrue("account should inherit from domain", grantable.contains(TargetType.domain));
        assertTrue("account should inherit from global", grantable.contains(TargetType.global));
    }

    @Test
    public void grantableOnTargetTypeUnrelatedTypeReturnsFalse() throws Exception {
        // Arrange - account attr right; an account does not inherit from another account
        AttrRight r = getAccountAttr("get", "displayName");

        // Act / Assert - the cos target type is not an ancestor of account
        assertFalse("cos is not grantable for an account attr right",
                r.grantableOnTargetType(TargetType.cos));
    }

    @Test
    public void isValidTargetForCustomDynamicGroupAccountRightReturnsFalse() throws Exception {
        // Arrange - account attr right, not a group right
        AttrRight r = getAccountAttr("get", "displayName");

        // Act / Assert
        assertFalse("account attr right is not valid for a custom dynamic group",
                r.isValidTargetForCustomDynamicGroup());
    }

    @Test
    public void getTargetTypeStrMultipleTargetTypesIsCommaSeparated() throws Exception {
        // Arrange - directly build a right with two target types (all-attrs is fine here since
        // we never call completeRight)
        AttrRight r = new AttrRight("multi.get", Right.RightType.getAttrs);
        r.setTargetType(TargetType.account);
        r.setTargetType(TargetType.domain);

        // Act
        String str = r.getTargetTypeStr();

        // Assert - both codes present, separated by a comma
        assertTrue("must contain account code", str.contains(TargetType.account.getCode()));
        assertTrue("must contain domain code", str.contains(TargetType.domain.getCode()));
        assertTrue("multiple target types are comma-separated", str.contains(","));
    }

    @Test
    public void overlapsAttrRightVsComboContainingMatchReturnsTrue() throws Exception {
        // Arrange - an attr right, and a combo right that contains a matching attr right
        AttrRight attr = getAccountAttr("get", "displayName");
        ComboRight combo = new ComboRight("combo.with.attr");
        combo.setDesc("combo containing a matching attr right");
        combo.addRight(getAccountAttr("get", "displayName"));
        // completeRight() expands the contained rights into the combo's attr-right set,
        // which overlaps() inspects via getAttrRights()
        combo.completeRight();

        // Act / Assert - the combo contains an overlapping attr right
        assertTrue("attr right overlaps combo that contains a matching attr right",
                attr.overlaps(combo));
    }

    @Test
    public void overlapsAttrRightVsComboNoMatchReturnsFalse() throws Exception {
        // Arrange - an attr right and a combo whose attr rights are disjoint
        AttrRight attr = getAccountAttr("get", "displayName");
        ComboRight combo = new ComboRight("combo.no.match");
        combo.setDesc("combo with disjoint attr rights");
        combo.addRight(getAccountAttr("get", "description"));
        combo.completeRight();

        // Act / Assert
        assertFalse("attr right must not overlap a combo with disjoint attr rights",
                attr.overlaps(combo));
    }

    @Test
    public void overlapsDisjointTargetTypesReturnsFalse() throws Exception {
        // Arrange - two all-attrs rights on different target types do not overlap
        AttrRight a = allAttrsRight(Right.RightType.getAttrs, TargetType.account);
        AttrRight b = allAttrsRight(Right.RightType.getAttrs, TargetType.cos);

        // Act / Assert - disjoint target types short-circuit overlap to false
        assertFalse("rights on disjoint target types must not overlap", a.overlaps(b));
    }

    @Test
    public void overlapsAllAttrsRightSameTargetReturnsTrue() throws Exception {
        // Arrange - an all-attrs right overlaps any same-target attr right
        AttrRight allAttrs = allAttrsRight(Right.RightType.getAttrs, TargetType.account);
        AttrRight specific = getAccountAttr("get", "displayName");

        // Act / Assert - if either side is all-attrs (and targets intersect), they overlap
        assertTrue("all-attrs right overlaps a same-target specific right",
                allAttrs.overlaps(specific));
    }
}
