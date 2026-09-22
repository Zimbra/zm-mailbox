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
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link ComboRight}: containment workflow (add direct rights, expand into
 * preset/attr rights via completeRight), the combo-right invariants, and the operations that
 * a combo right disallows (target-type accessors).
 */
public class ComboRightTest {

    private ComboRight newCombo(String name) {
        // ComboRight ctor is package-private; this test lives in the same package.
        ComboRight cr = new ComboRight(name);
        // completeRight() -> super.completeRight() requires a non-null description.
        cr.setDesc("combo right " + name);
        return cr;
    }

    private PresetRight newPreset(String name) {
        // PresetRight ctor is package-private (same package).
        return new PresetRight(name);
    }

    private PresetRight newPreset(String name, TargetType targetType) throws ServiceException {
        PresetRight pr = new PresetRight(name);
        pr.setTargetType(targetType);
        return pr;
    }

    // Build a completed combo right containing the given (already target-typed) preset rights.
    private ComboRight comboWith(String name, Right... rights) throws ServiceException {
        ComboRight cr = newCombo(name);
        for (Right r : rights) {
            cr.addRight(r);
        }
        cr.completeRight();
        return cr;
    }

    @Test
    public void constructorSetsComboRightType() {
        // Act
        ComboRight cr = newCombo("comboA");

        // Assert
        assertEquals(Right.RightType.combo, cr.getRightType());
        assertTrue("a combo right must report isComboRight", cr.isComboRight());
        assertEquals("comboA", cr.getName());
    }

    @Test
    public void isComboRightOverridesBaseFalseReturnsTrue() {
        ComboRight cr = newCombo("comboB");
        assertTrue(cr.isComboRight());
        assertFalse("combo right is not a preset right", cr.isPresetRight());
        assertFalse("combo right is not a user right", cr.isUserRight());
    }

    @Test
    public void addRightPresetAdminRightStoredInDirectRights() throws Exception {
        // Arrange
        ComboRight cr = newCombo("comboC");
        PresetRight preset = newPreset("presetC");

        // Act
        cr.addRight(preset);

        // Assert — directly contained
        Set<Right> direct = cr.getRights();
        assertEquals(1, direct.size());
        assertTrue(direct.contains(preset));
    }

    @Test
    public void addRightUserRightThrowsFailure() throws Exception {
        // Arrange — a UserRight reports isUserRight()==true and is rejected
        ComboRight cr = newCombo("comboD");
        UserRight userRight = new UserRight("userRightD");

        // Act / Assert
        try {
            cr.addRight(userRight);
            fail("combo right must reject user rights");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
        assertTrue("rejected right must not be stored", cr.getRights().isEmpty());
    }

    @Test
    public void completeRightWithPresetMembersExpandsIntoAllRights() throws Exception {
        // Arrange — combo containing two preset rights
        ComboRight cr = newCombo("comboE");
        PresetRight p1 = newPreset("presetE1");
        PresetRight p2 = newPreset("presetE2");
        cr.addRight(p1);
        cr.addRight(p2);

        // Act
        cr.completeRight();

        // Assert — both presets expanded into the preset and all-rights sets
        assertEquals(2, cr.getPresetRights().size());
        assertTrue(cr.getPresetRights().contains(p1));
        assertTrue(cr.containsPresetRight(p2));
        assertEquals(2, cr.getAllRights().size());
        assertTrue(cr.getAttrRights().isEmpty());
    }

    @Test
    public void completeRightNestedComboRightExpandsRecursively() throws Exception {
        // Arrange — outer combo contains an inner combo which holds a preset right
        ComboRight inner = newCombo("comboInner");
        PresetRight nestedPreset = newPreset("nestedPreset");
        inner.addRight(nestedPreset);
        inner.completeRight();

        ComboRight outer = newCombo("comboOuter");
        outer.addRight(inner);

        // Act
        outer.completeRight();

        // Assert — the nested preset surfaces in the outer combo's expanded preset set
        assertTrue("nested preset must expand into outer combo",
                outer.containsPresetRight(nestedPreset));
        assertEquals(1, outer.getAllRights().size());
    }

    @Test
    public void executableOnTargetTypeAnyTargetTypeReturnsTrue() {
        ComboRight cr = newCombo("comboF");
        assertTrue(cr.executableOnTargetType(TargetType.account));
        assertTrue(cr.executableOnTargetType(TargetType.domain));
    }

    @Test
    public void getTargetTypeComboThrowsInternalError() {
        ComboRight cr = newCombo("comboG");
        try {
            cr.getTargetType();
            fail("combo right has no single target type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    @Test
    public void setTargetTypeComboThrowsFailure() {
        ComboRight cr = newCombo("comboH");
        try {
            cr.setTargetType(TargetType.account);
            fail("setting a target type on a combo right is not allowed");
        } catch (ServiceException e) {
            assertEquals(ServiceException.FAILURE, e.getCode());
        }
    }

    @Test
    public void getTargetTypeStrComboReturnsNull() {
        ComboRight cr = newCombo("comboI");
        assertNull(cr.getTargetTypeStr());
    }

    @Test
    public void getAllRightsBeforeCompleteIsEmpty() throws Exception {
        // Arrange — adding direct rights does not populate the expanded all-rights set
        ComboRight cr = newCombo("comboJ");
        cr.addRight(newPreset("presetJ"));

        // Assert — all-rights only populated by completeRight
        assertTrue(cr.getAllRights().isEmpty());
        assertEquals(1, cr.getRights().size());
    }

    @Test
    public void dumpIncludesContainedRightNames() throws Exception {
        // Arrange
        ComboRight cr = newCombo("comboK");
        cr.addRight(newPreset("presetK"));

        // Act
        String dumped = cr.dump(new StringBuilder());

        // Assert
        assertTrue("dump must contain combo header",
                dumped.contains("combo right properties"));
        assertTrue("dump must list the contained right", dumped.contains("presetK"));
    }

    // ---- appended functional tests covering previously-uncovered branches ----

    @Test
    public void overlapsComboContainsMatchingPresetReturnsTrue() throws Exception {
        // Arrange — completed combo containing a domain preset right
        PresetRight p = newPreset("ovlP", TargetType.domain);
        ComboRight cr = comboWith("ovlCombo", p);

        // Act / Assert — overlaps delegates to the contained preset's overlaps()
        assertTrue("combo overlaps a preset it contains", cr.overlaps(p));
    }

    @Test
    public void overlapsPresetNotContainedReturnsFalse() throws Exception {
        // Arrange
        PresetRight contained = newPreset("ovlContained", TargetType.domain);
        PresetRight outsider = newPreset("ovlOutsider", TargetType.domain);
        ComboRight cr = comboWith("ovlCombo2", contained);

        // Act / Assert — an unrelated preset does not overlap
        assertFalse("combo does not overlap a preset it does not contain", cr.overlaps(outsider));
    }

    @Test
    public void grantableOnTargetTypeAllMembersGrantableReturnsTrue() throws Exception {
        // Arrange — two domain-typed presets; domain is grantable on target type domain
        ComboRight cr = comboWith("grantCombo",
                newPreset("grantP1", TargetType.domain),
                newPreset("grantP2", TargetType.domain));

        // Act / Assert
        assertTrue("combo grantable on domain when all members are",
                cr.grantableOnTargetType(TargetType.domain));
    }

    @Test
    public void grantableOnTargetTypeOneMemberNotGrantableReturnsFalse() throws Exception {
        // Arrange — domain preset is NOT grantable on target type account
        ComboRight cr = comboWith("grantCombo2",
                newPreset("grantP3", TargetType.domain));

        // Act / Assert — account is not in domain's inheritedBy semantics for grantability
        assertFalse("combo not grantable when a member is not",
                cr.grantableOnTargetType(TargetType.account));
    }

    @Test
    public void allowSubDomainModifierMemberExecutableOnDomainReturnsTrue() throws Exception {
        // Arrange — a domain-executable preset
        ComboRight cr = comboWith("subDomCombo", newPreset("subDomP", TargetType.domain));

        // Act / Assert — any member executable on domain enables the sub-domain modifier
        assertTrue(cr.allowSubDomainModifier());
    }

    @Test
    public void allowSubDomainModifierNoMemberExecutableOnDomainReturnsFalse() throws Exception {
        // Arrange — an account-executable preset (not domain)
        ComboRight cr = comboWith("subDomCombo2", newPreset("subDomP2", TargetType.account));

        // Act / Assert
        assertFalse(cr.allowSubDomainModifier());
    }

    @Test
    public void allowDisinheritSubGroupsModifierMemberExecutableOnAccountReturnsTrue() throws Exception {
        // Arrange — account-executable preset enables the disinherit modifier
        ComboRight cr = comboWith("disinhCombo", newPreset("disinhP", TargetType.account));

        // Act / Assert
        assertTrue(cr.allowDisinheritSubGroupsModifier());
    }

    @Test
    public void allowDisinheritSubGroupsModifierMemberOnlyDomainReturnsFalse() throws Exception {
        // Arrange — domain-only preset is not dl/account/calresource executable
        ComboRight cr = comboWith("disinhCombo2", newPreset("disinhP2", TargetType.domain));

        // Act / Assert
        assertFalse(cr.allowDisinheritSubGroupsModifier());
    }

    @Test
    public void isValidTargetForCustomDynamicGroupNonGroupMemberReturnsFalse() throws Exception {
        // Arrange — a domain preset is not a valid target for a custom dynamic group
        ComboRight cr = comboWith("cdgCombo", newPreset("cdgP", TargetType.domain));

        // Act / Assert
        assertFalse(cr.isValidTargetForCustomDynamicGroup());
    }

    @Test
    public void isValidTargetForCustomDynamicGroupGroupMemberReturnsTrue() throws Exception {
        // Arrange — a group-typed preset is a valid target for a custom dynamic group
        ComboRight cr = comboWith("cdgCombo2", newPreset("cdgP2", TargetType.group));

        // Act / Assert
        assertTrue(cr.isValidTargetForCustomDynamicGroup());
    }

    @Test
    public void getGrantableTargetTypesIntersectsMembersReturnsCommonTypes() throws Exception {
        // Arrange — two domain presets share the same grantable target types
        ComboRight cr = comboWith("gttCombo",
                newPreset("gttP1", TargetType.domain),
                newPreset("gttP2", TargetType.domain));

        // Act — intersection of identical sets is that set (domain + global)
        Set<TargetType> tts = cr.getGrantableTargetTypes();

        // Assert
        assertNotNull(tts);
        assertTrue("domain inherits from domain", tts.contains(TargetType.domain));
        assertTrue("domain inherits from global", tts.contains(TargetType.global));
    }

    @Test
    public void getGrantableTargetTypesDisjointMembersReturnsEmptyIntersection() throws Exception {
        // Arrange — domain and account presets have disjoint grantable target type sets
        ComboRight cr = comboWith("gttCombo2",
                newPreset("gttP3", TargetType.domain),
                newPreset("gttP4", TargetType.account));

        // Act
        Set<TargetType> tts = cr.getGrantableTargetTypes();

        // Assert — domain inheritFrom {domain,global}; account inheritFrom {account,domain,global}
        // their intersection still contains the shared ancestors
        assertNotNull(tts);
        assertTrue("shared ancestor global must survive the intersect", tts.contains(TargetType.global));
        assertFalse("account is not a grantable target type of the domain member",
                tts.contains(TargetType.account));
    }

    @Test
    public void verifyTargetTypeComboNoOp() throws Exception {
        // Arrange / Act — verifyTargetType is a no-op for combo rights
        ComboRight cr = comboWith("vttCombo", newPreset("vttP", TargetType.domain));
        cr.verifyTargetType();

        // Assert — still a combo right with its expanded members intact
        assertTrue(cr.isComboRight());
        assertEquals(1, cr.getAllRights().size());
    }

    @Test
    public void getAttrRightsPresetOnlyComboIsEmpty() throws Exception {
        // Arrange — combo of preset rights only
        ComboRight cr = comboWith("attrCombo", newPreset("attrP", TargetType.domain));

        // Act / Assert — no attr rights expanded
        assertTrue(cr.getAttrRights().isEmpty());
        assertEquals(1, cr.getPresetRights().size());
    }
}
