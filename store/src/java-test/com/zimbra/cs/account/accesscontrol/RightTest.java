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
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link Right}. Real {@link Right} instances are obtained from the live
 * {@link RightManager} (admin + user rights) and exercised through their public surface: type
 * classification, naming, comparison/sorting, cache-index lifecycle, and the {@link RightType}
 * enum's parse/user-definable behavior. Both valid and invalid paths are covered.
 */
public class RightTest {

    private static RightManager rm;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
        rm = RightManager.getInstance();
    }

    private Right anyAdminRight() throws ServiceException {
        return rm.getAllAdminRights().values().iterator().next();
    }

    private Right anyUserRight() throws ServiceException {
        return rm.getAllUserRights().values().iterator().next();
    }

    @Test
    public void rightTypeFromStringValidNameReturnsEnum() throws Exception {
        // Arrange / Act
        RightType preset = RightType.fromString("preset");
        RightType combo = RightType.fromString("combo");

        // Assert
        assertEquals(RightType.preset, preset);
        assertEquals(RightType.combo, combo);
    }

    @Test
    public void rightTypeFromStringUnknownNameThrowsParseError() {
        // Act / Assert
        try {
            RightType.fromString("notARightType");
            fail("expected ServiceException for unknown right type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("unknown right type"));
        }
    }

    @Test
    public void rightTypeIsUserDefinablePresetVsOthersOnlyNonPresetDefinable() {
        // Assert - preset is the only non-user-definable type
        assertFalse("preset must not be user definable", RightType.preset.isUserDefinable());
        assertTrue(RightType.getAttrs.isUserDefinable());
        assertTrue(RightType.setAttrs.isUserDefinable());
        assertTrue(RightType.combo.isUserDefinable());
    }

    @Test
    public void getNameRealRightMatchesManagerKey() throws Exception {
        // Arrange
        String key = rm.getAllAdminRights().keySet().iterator().next();

        // Act
        Right r = rm.getAllAdminRights().get(key);

        // Assert - the map key is the right's name
        assertEquals(key, r.getName());
    }

    @Test
    public void getRightTypeRealRightMatchesClassification() throws Exception {
        // Arrange
        Right r = anyAdminRight();

        // Act
        RightType type = r.getRightType();

        // Assert - exactly one of the is* flags is consistent with the type
        if (type == RightType.preset) {
            assertTrue(r.isPresetRight());
        } else if (type == RightType.combo) {
            assertTrue(r.isComboRight());
        } else {
            assertTrue("getAttrs/setAttrs must report isAttrRight", r.isAttrRight());
        }
    }

    @Test
    public void getRightClassUserVsAdminRightReflectsUserFlag() throws Exception {
        // Arrange
        Right userRight = anyUserRight();
        Right adminRight = anyAdminRight();

        // Assert
        assertTrue("user right must report isUserRight", userRight.isUserRight());
        assertEquals(RightClass.USER, userRight.getRightClass());
        assertEquals(RightClass.ADMIN, adminRight.getRightClass());
    }

    @Test
    public void getDescRealRightIsPopulated() throws Exception {
        // Arrange / Act
        Right r = anyAdminRight();

        // Assert - completeRight() requires a non-null description
        assertTrue("loaded right must carry a description", r.getDesc() != null);
    }

    @Test
    public void compareToByNameSortsAlphabetically() throws Exception {
        // Arrange - collect a few real rights
        List<Right> rights = new ArrayList<Right>(rm.getAllAdminRights().values());

        // Act
        java.util.Collections.sort(rights);

        // Assert - names are non-decreasing after sort
        for (int i = 1; i < rights.size(); i++) {
            assertTrue("sorted order must be by name",
                    rights.get(i - 1).getName().compareTo(rights.get(i).getName()) <= 0);
        }
    }

    @Test
    public void compareToSameRightReturnsZero() throws Exception {
        // Arrange
        Right r = anyAdminRight();

        // Act / Assert
        assertEquals(0, r.compareTo(r));
    }

    @Test
    public void getTargetTypeStrRealPresetRightMatchesTargetTypeCode() throws Exception {
        // Arrange - find a preset right (single target type)
        Right preset = null;
        for (Right r : rm.getAllAdminRights().values()) {
            if (r.isPresetRight()) {
                preset = r;
                break;
            }
        }
        org.junit.Assume.assumeTrue("need at least one preset right", preset != null);

        // Act
        TargetType tt = preset.getTargetType();

        // Assert - the SOAP string equals the target type code
        assertEquals(tt.getCode(), preset.getTargetTypeStr());
    }

    @Test
    public void getMaxCacheIndexAfterRightsLoadedIsPositive() throws Exception {
        // Arrange - rights were loaded in @BeforeClass; cacheable ones bumped the index

        // Act
        int max = Right.getMaxCacheIndex();

        // Assert - at least some rights are cacheable
        assertTrue("max cache index should be positive after rights load, was " + max, max > 0);
    }

    @Test
    public void getCacheIndexCacheableRightWithinMaxBound() throws Exception {
        // Arrange - find a cacheable right
        Right cacheable = null;
        for (Right r : rm.getAllAdminRights().values()) {
            if (r.getCacheIndex() != -1) {
                cacheable = r;
                break;
            }
        }
        org.junit.Assume.assumeTrue("need a cacheable right", cacheable != null);

        // Assert - its index is in [0, max)
        assertTrue(cacheable.getCacheIndex() >= 0);
        assertTrue(cacheable.getCacheIndex() < Right.getMaxCacheIndex());
    }

    @Test
    public void getGrantTargetTypeStrPresetAdminRightIsNullWhenNoGrantType() throws Exception {
        // Arrange - admin preset rights have no separate grant target type
        Right preset = null;
        for (Right r : rm.getAllAdminRights().values()) {
            if (r.isPresetRight()) {
                preset = r;
                break;
            }
        }
        org.junit.Assume.assumeTrue("need a preset admin right", preset != null);

        // Act / Assert - grant target type only set on user rights, so null here
        assertEquals(null, preset.getGrantTargetTypeStr());
    }

    @Test
    public void dumpRealRightIncludesNameTypeAndTargetType() throws Exception {
        // Arrange
        Right r = anyAdminRight();

        // Act - dump into a fresh builder
        String dump = r.dump(new StringBuilder());

        // Assert - the human-readable dump carries the key identity fields
        assertTrue("dump must include the right name", dump.contains(r.getName()));
        assertTrue("dump must include the type label", dump.contains("type"));
        assertTrue("dump must include the right type value",
                dump.contains(r.getRightType().name()));
        assertTrue("dump must include the target type line", dump.contains("target Type"));
    }

    @Test
    public void dumpNullBuilderAllocatesAndReturnsContent() throws Exception {
        // Arrange
        Right r = anyAdminRight();

        // Act - passing null forces dump() to allocate its own StringBuilder
        String dump = r.dump(null);

        // Assert
        assertNotNull("dump must allocate a builder when given null", dump);
        assertTrue(dump.contains(r.getName()));
    }

    @Test
    public void executableOnTargetTypeMatchingTypeTrueOtherwiseFalse() throws Exception {
        // Arrange
        Right preset = null;
        for (Right r : rm.getAllAdminRights().values()) {
            if (r.isPresetRight()) {
                preset = r;
                break;
            }
        }
        org.junit.Assume.assumeTrue("need a preset right", preset != null);
        TargetType own = preset.getTargetType();

        // Act / Assert - executable on its own target type, not on an unrelated one
        assertTrue("right is executable on its own target type",
                preset.executableOnTargetType(own));
        TargetType other = (own == TargetType.account) ? TargetType.cos : TargetType.account;
        assertFalse("right is not executable on an unrelated target type",
                preset.executableOnTargetType(other));
    }

    @Test
    public void allowSubDomainModifierDomainTargetedRightReflectsTargetType() throws Exception {
        // Arrange - find any admin right and compare against its target type
        Right r = anyAdminRight();
        TargetType tt = r.getTargetType();

        // Act
        boolean allowed = r.allowSubDomainModifier();

        // Assert - subDomain modifier is allowed exactly when the target type is domain
        assertEquals(tt == TargetType.domain, allowed);
    }

    @Test
    public void allowDisinheritSubGroupsModifierReflectsDlAccountOrCalresource() throws Exception {
        // Arrange
        Right r = anyAdminRight();
        TargetType tt = r.getTargetType();

        // Act
        boolean allowed = r.allowDisinheritSubGroupsModifier();

        // Assert - allowed exactly for dl/account/calresource target types
        boolean expected = (tt == TargetType.dl || tt == TargetType.account
                || tt == TargetType.calresource);
        assertEquals(expected, allowed);
    }

    @Test
    public void isValidTargetForCustomDynamicGroupPresetRightTrueOnlyForGroupTarget() throws Exception {
        // Arrange - the base Right impl treats only group-targeted rights as valid
        Right preset = null;
        for (Right r : rm.getAllAdminRights().values()) {
            if (r.isPresetRight()) {
                preset = r;
                break;
            }
        }
        org.junit.Assume.assumeTrue("need a preset right", preset != null);

        // Act / Assert
        assertEquals(preset.getTargetType() == TargetType.group,
                preset.isValidTargetForCustomDynamicGroup());
    }

    @Test
    public void isTheSameRightSameInstanceVsDifferentIdentityComparison() throws Exception {
        // Arrange - two distinct real rights
        java.util.Iterator<? extends Right> it = rm.getAllAdminRights().values().iterator();
        Right a = it.next();
        Right b = it.next();

        // Act / Assert - base impl uses identity
        assertTrue("a right is the same as itself", a.isTheSameRight(a));
        assertFalse("two distinct rights are not the same", a.isTheSameRight(b));
    }

    @Test
    public void getGrantableTargetTypesUserRightIsNonEmptyAndReportedAsString() throws Exception {
        // Arrange - user rights carry grantable target types
        Right userRight = anyUserRight();

        // Act
        java.util.Set<TargetType> grantable = userRight.getGrantableTargetTypes();
        String report = userRight.reportGrantableTargetTypes();

        // Assert - the report lists each grantable target type's code
        assertFalse("user right should have grantable target types", grantable.isEmpty());
        for (TargetType tt : grantable) {
            assertTrue("report must mention " + tt.getCode(), report.contains(tt.getCode()));
        }
    }

    @Test
    public void reportGrantableTargetTypesMultipleTypesJoinedWithOr() throws Exception {
        // Arrange - find a user right with more than one grantable target type
        Right multi = null;
        for (Right r : rm.getAllUserRights().values()) {
            if (r.getGrantableTargetTypes().size() > 1) {
                multi = r;
                break;
            }
        }
        org.junit.Assume.assumeTrue("need a user right with multiple grantable types", multi != null);

        // Act
        String report = multi.reportGrantableTargetTypes();

        // Assert - multiple entries are joined with " or "
        assertTrue("multiple target types must be joined with ' or '", report.contains(" or "));
    }

    @Test
    public void getGrantTargetTypeUserRightWithGrantTypeMatchesStringForm() throws Exception {
        // Arrange - find a user right that declares a grant target type
        Right withGrant = null;
        for (Right r : rm.getAllUserRights().values()) {
            if (r.getGrantTargetType() != null) {
                withGrant = r;
                break;
            }
        }
        org.junit.Assume.assumeTrue("need a user right with a grant target type", withGrant != null);

        // Act / Assert - the string form matches the enum's code
        assertEquals(withGrant.getGrantTargetType().getCode(), withGrant.getGrantTargetTypeStr());
    }

    @Test
    public void setTargetTypeWhenAlreadySetThrowsParseError() throws Exception {
        // Arrange - a fully loaded right already has its target type
        Right r = anyAdminRight();

        // Act / Assert - re-setting must be rejected
        try {
            r.setTargetType(TargetType.account);
            fail("expected ServiceException when target type already set");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("already set"));
        }
    }

    @Test
    public void verifyTargetTypeFreshRightWithoutTargetTypeThrowsParseError() throws Exception {
        // Arrange - a brand new preset right with no target type assigned yet
        PresetRight fresh = new PresetRight("test-no-target-right");

        // Act / Assert
        try {
            fresh.verifyTargetType();
            fail("expected ServiceException for missing target type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("missing target type"));
        }
    }

    @Test
    public void completeRightFreshRightWithoutDescThrowsParseError() throws Exception {
        // Arrange - a new right with neither description nor target type
        PresetRight fresh = new PresetRight("test-incomplete-right");

        // Act / Assert - completeRight() first checks for a description
        try {
            fresh.completeRight();
            fail("expected ServiceException for missing description");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("missing description"));
        }
    }

    @Test
    public void completeRightDescSetButNoTargetTypeThrowsMissingTargetType() throws Exception {
        // Arrange - supply a description but leave target type unset
        PresetRight fresh = new PresetRight("test-desc-only-right");
        fresh.setDesc("a description");

        // Act / Assert - with a desc present, the failure is now on the target type
        try {
            fresh.completeRight();
            fail("expected ServiceException for missing target type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("missing target type"));
        }
    }

    @Test
    public void setGrantTargetTypeOnAdminPresetRightThrowsParseError() throws Exception {
        // Arrange - grant target type is only supported on user rights
        PresetRight adminPreset = new PresetRight("test-admin-preset");

        // Act / Assert
        try {
            adminPreset.setGrantTargetType(TargetType.account);
            fail("expected ServiceException - grant target type only on user rights");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("only supported on user rights"));
        }
    }

    @Test
    public void setGrantTargetTypeInvalidTypeForUserRightThrowsParseError() throws Exception {
        // Arrange - a user right whose target type does not permit the requested grant type
        UserRight ur = new UserRight("test-user-right");
        ur.setTargetType(TargetType.account);

        // Act / Assert - cos is not a valid grant target for an account-targeted right
        try {
            ur.setGrantTargetType(TargetType.cos);
            fail("expected ServiceException for invalid grant target type");
        } catch (ServiceException e) {
            assertEquals(ServiceException.PARSE_ERROR, e.getCode());
            assertTrue(e.getMessage().contains("invalid grant target type"));
        }
    }

    @Test
    public void setGrantTargetTypeValidTypeForUserRightSetsAndReadsBack() throws Exception {
        // Arrange - an account-targeted user right; account is grantable on itself
        UserRight ur = new UserRight("test-user-right-valid");
        ur.setTargetType(TargetType.account);

        // Act
        ur.setGrantTargetType(TargetType.account);

        // Assert - the grant target type is now readable
        assertEquals(TargetType.account, ur.getGrantTargetType());
        assertEquals(TargetType.account.getCode(), ur.getGrantTargetTypeStr());
    }

    // ------------------------------------------------------------------
    // Strengthened mutation-killing assertions
    // ------------------------------------------------------------------

    @Test
    public void compareToDistinctNamesReturnsSignedOrdering() throws Exception {
        // Arrange - two preset rights with names whose ordering is known.
        // Kills PrimitiveReturns on compareTo (L90): a forced 0 would lose the sign.
        PresetRight a = new PresetRight("aaa-right");
        PresetRight b = new PresetRight("zzz-right");

        // Act / Assert - exact signed semantics, not just non-zero.
        assertTrue("a < b must be strictly negative", a.compareTo(b) < 0);
        assertTrue("b > a must be strictly positive", b.compareTo(a) > 0);
        assertEquals("identical names compare equal", 0, a.compareTo(new PresetRight("aaa-right")));
    }

    @Test
    public void getHelpSetHelpReturnsExactInstance() throws Exception {
        // Arrange - the loaded rights carry no <help>, so construct one and attach it.
        // Kills NullReturns on getHelp (L145): a forced null would drop the attached Help.
        PresetRight r = new PresetRight("test-help-right");
        assertNull("a fresh right has no help", r.getHelp());
        Help help = new Help("help-1");
        r.setHelp(help);

        // Act / Assert - the exact Help object is returned.
        assertNotNull(r.getHelp());
        assertSame(help, r.getHelp());
        assertEquals("help-1", r.getHelp().getName());
    }

    @Test
    public void getUISetUIReturnsExactInstance() throws Exception {
        // Arrange. Kills NullReturns on getUI (L149).
        PresetRight r = new PresetRight("test-ui-right");
        assertNull("a fresh right has no UI", r.getUI());
        UI ui = new UI("ui-desc-1");
        r.setUI(ui);

        // Act / Assert
        assertNotNull(r.getUI());
        assertSame(ui, r.getUI());
    }

    @Test
    public void getFallbackInviteUserRightIsNonNull() throws Exception {
        // Arrange - the 'invite' user right declares fallback="InviteFallback" in the rights XML.
        // Kills NullReturns on getFallback (L169): a forced null would drop the loaded fallback.
        Right invite = rm.getUserRight("invite");
        org.junit.Assume.assumeTrue("need the invite user right", invite != null);

        // Act / Assert
        assertNotNull("invite right must carry a non-null fallback", invite.getFallback());

        // And a freshly built right without a fallback must read back null on the same path.
        assertNull(new PresetRight("test-no-fallback").getFallback());
    }

    @Test
    public void isCacheableAndGetCacheIndexDistinguishCacheableFromNot() throws Exception {
        // Arrange - the 'invite' user right declares cache="1" so it is cacheable with a valid
        // (>= 0) index; a freshly built right is not cacheable (index == -1).
        // Kills: isCacheable NegateConditionals + BooleanTrueReturns (L302),
        //        getCacheIndex PrimitiveReturns (L306).
        Right invite = rm.getUserRight("invite");
        org.junit.Assume.assumeTrue("need the invite user right", invite != null);
        PresetRight notCacheable = new PresetRight("test-not-cacheable");

        // Act / Assert
        assertTrue("invite is cacheable", invite.isCacheable());
        assertTrue("a cacheable right has a non-negative cache index", invite.getCacheIndex() >= 0);

        assertFalse("a fresh right is not cacheable", notCacheable.isCacheable());
        assertEquals("a non-cacheable right reports index -1", -1, notCacheable.getCacheIndex());
    }

    @Test
    public void setCacheableAssignsSequentialIndexAndBumpsMaxByOne() throws Exception {
        // Arrange - setCacheable() calls the private getNextCacheIndex() which does
        //   sMaxCacheIndex++; return sMaxCacheIndex - 1;
        // Kills Math on L310/L311 and PrimitiveReturns on L311: the assigned index must equal the
        // pre-call max, and the max must advance by exactly one per assignment.
        int before = Right.getMaxCacheIndex();
        PresetRight r1 = new PresetRight("test-cache-seq-1");
        PresetRight r2 = new PresetRight("test-cache-seq-2");

        // Act
        r1.setCacheable();
        int afterFirst = Right.getMaxCacheIndex();
        r2.setCacheable();
        int afterSecond = Right.getMaxCacheIndex();

        // Assert - first index == prior max; each call bumps the max by exactly 1; indices are
        // consecutive.
        assertEquals("first assigned index equals the pre-call max", before, r1.getCacheIndex());
        assertEquals("max advances by exactly one", before + 1, afterFirst);
        assertEquals("second assigned index follows the first", before + 1, r2.getCacheIndex());
        assertEquals("max advances by exactly one again", before + 2, afterSecond);
        assertTrue("now cacheable", r1.isCacheable());
    }

    @Test
    public void allowSubDomainModifierBothBranches() throws Exception {
        // Arrange - a domain-targeted right allows the sub-domain modifier; a non-domain one does
        // not. Kills BooleanFalseReturns on allowSubDomainModifier (L193) by asserting the true
        // branch, and pins the false branch too.
        PresetRight domainRight = new PresetRight("test-subdomain-domain");
        domainRight.setTargetType(TargetType.domain);
        PresetRight acctRight = new PresetRight("test-subdomain-account");
        acctRight.setTargetType(TargetType.account);

        // Act / Assert
        assertTrue("domain-targeted right allows subDomain modifier",
                domainRight.allowSubDomainModifier());
        assertFalse("account-targeted right does not allow subDomain modifier",
                acctRight.allowSubDomainModifier());
    }

    @Test
    public void allowDisinheritSubGroupsModifierEachTargetType() throws Exception {
        // Arrange - dl/account/calresource => true; everything else => false.
        // Kills NegateConditionals (L203), BooleanTrueReturns (L201) by exercising every disjunct
        // and a negative case.
        PresetRight dlRight = new PresetRight("test-dis-dl");
        dlRight.setTargetType(TargetType.dl);
        PresetRight acctRight = new PresetRight("test-dis-account");
        acctRight.setTargetType(TargetType.account);
        PresetRight calRight = new PresetRight("test-dis-calresource");
        calRight.setTargetType(TargetType.calresource);
        PresetRight domainRight = new PresetRight("test-dis-domain");
        domainRight.setTargetType(TargetType.domain);

        // Act / Assert
        assertTrue("dl allows disinheritSubGroups", dlRight.allowDisinheritSubGroupsModifier());
        assertTrue("account allows disinheritSubGroups",
                acctRight.allowDisinheritSubGroupsModifier());
        assertTrue("calresource allows disinheritSubGroups",
                calRight.allowDisinheritSubGroupsModifier());
        assertFalse("domain does not allow disinheritSubGroups",
                domainRight.allowDisinheritSubGroupsModifier());
    }

    @Test
    public void initPopulatesBothUserAndAdminGeneratedRights() throws Exception {
        // Arrange - RightManager.getInstance() (in @BeforeClass) runs Right.init(rm), which calls
        //   UserRight.init(rm);   (L61)
        //   AdminRight.init(rm);  (L62)
        // Each populates its generated static right fields. Removing either VoidMethodCall leaves
        // the corresponding generated fields null.

        // Assert - user-right init ran (L61).
        assertNotNull("UserRight.init must populate Rights.User.R_invite", Rights.User.R_invite);
        assertEquals("invite", Rights.User.R_invite.getName());
        assertTrue(Rights.User.R_invite.isUserRight());

        // Assert - admin-right init ran (L62).
        assertNotNull("AdminRight.init must populate Rights.Admin.R_accessGAL",
                Rights.Admin.R_accessGAL);
        assertEquals("accessGAL", Rights.Admin.R_accessGAL.getName());
        assertEquals(RightClass.ADMIN, Rights.Admin.R_accessGAL.getRightClass());
    }

    @Test
    public void reportGrantableTargetTypesSingleTypeHasNoOrSeparator() throws Exception {
        // Arrange - find a user right with exactly one grantable target type. The join loop
        //   if (!first) sb.append(" or ");  (L221)
        // must NOT emit " or " for a single entry; the report is exactly that one code.
        Right single = null;
        for (Right r : rm.getAllUserRights().values()) {
            if (r.getGrantableTargetTypes().size() == 1) {
                single = r;
                break;
            }
        }
        org.junit.Assume.assumeTrue("need a user right with exactly one grantable type", single != null);

        // Act
        String report = single.reportGrantableTargetTypes();
        TargetType only = single.getGrantableTargetTypes().iterator().next();

        // Assert - exactly the single code, no separator.
        assertEquals(only.getCode(), report);
        assertFalse("single grantable type must not contain ' or '", report.contains(" or "));
    }
}
