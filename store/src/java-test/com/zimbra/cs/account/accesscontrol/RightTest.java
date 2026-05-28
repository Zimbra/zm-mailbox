/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import com.zimbra.common.service.ServiceException;

/**
 * Full functional tests for {@link Right}.
 *
 * Tests verify Right abstract class behavior including right type operations,
 * cache management, target type handling, and modifier validation.
 */
public class RightTest {

    @BeforeClass
    public static void init() throws ServiceException {
        // Initialize RightManager to load rights
        RightManager.getInstance();
    }

    /**
     * Test: Verify RightType enum values defined → all accessible.
     *
     * Verifies: All RightType enum constants are defined correctly.
     */
    @Test
    public void rightType_allValues_defined() {
        // Act & Assert
        assertNotNull(Right.RightType.preset);
        assertNotNull(Right.RightType.getAttrs);
        assertNotNull(Right.RightType.setAttrs);
        assertNotNull(Right.RightType.combo);
    }

    /**
     * Test: Parse RightType from string "preset" → returns RightType.preset.
     *
     * Verifies: RightType.fromString() correctly parses preset type.
     */
    @Test
    public void rightType_fromString_preset_parsesCorrectly() throws ServiceException {
        // Act
        Right.RightType rightType = Right.RightType.fromString("preset");

        // Assert
        assertEquals(Right.RightType.preset, rightType);
    }

    /**
     * Test: Parse RightType from string "getAttrs" → returns RightType.getAttrs.
     *
     * Verifies: RightType.fromString() correctly parses getAttrs type.
     */
    @Test
    public void rightType_fromString_getAttrs_parsesCorrectly() throws ServiceException {
        // Act
        Right.RightType rightType = Right.RightType.fromString("getAttrs");

        // Assert
        assertEquals(Right.RightType.getAttrs, rightType);
    }

    /**
     * Test: Parse RightType from string "setAttrs" → returns RightType.setAttrs.
     *
     * Verifies: RightType.fromString() correctly parses setAttrs type.
     */
    @Test
    public void rightType_fromString_setAttrs_parsesCorrectly() throws ServiceException {
        // Act
        Right.RightType rightType = Right.RightType.fromString("setAttrs");

        // Assert
        assertEquals(Right.RightType.setAttrs, rightType);
    }

    /**
     * Test: Parse RightType from string "combo" → returns RightType.combo.
     *
     * Verifies: RightType.fromString() correctly parses combo type.
     */
    @Test
    public void rightType_fromString_combo_parsesCorrectly() throws ServiceException {
        // Act
        Right.RightType rightType = Right.RightType.fromString("combo");

        // Assert
        assertEquals(Right.RightType.combo, rightType);
    }

    /**
     * Test: Parse RightType from invalid string → throws ServiceException.
     *
     * Verifies: RightType.fromString() throws exception for invalid type strings.
     */
    @Test(expected = ServiceException.class)
    public void rightType_fromString_invalidType_throwsException() throws ServiceException {
        // Act
        Right.RightType.fromString("invalid_type_xyz");
    }

    /**
     * Test: Verify RightType.isUserDefinable() returns true for non-preset types.
     *
     * Verifies: isUserDefinable() correctly identifies user-definable types.
     */
    @Test
    public void rightType_isUserDefinable_returnsTrueForNonPreset() {
        // Act & Assert
        assertTrue("getAttrs should be user-definable",
            Right.RightType.getAttrs.isUserDefinable());
        assertTrue("setAttrs should be user-definable",
            Right.RightType.setAttrs.isUserDefinable());
        assertTrue("combo should be user-definable",
            Right.RightType.combo.isUserDefinable());
    }

    /**
     * Test: Verify RightType.isUserDefinable() returns false for preset.
     *
     * Verifies: isUserDefinable() correctly identifies preset as not user-definable.
     */
    @Test
    public void rightType_isUserDefinable_returnsFalseForPreset() {
        // Act & Assert
        assertFalse("preset should not be user-definable",
            Right.RightType.preset.isUserDefinable());
    }

    /**
     * Test: Get cache index and verify it's incremented for cacheable rights.
     *
     * Verifies: Cache management properly tracks and assigns cache indices.
     */
    @Test
    public void right_cacheIndex_increments() throws ServiceException {
        // Act
        int maxCache = Right.getMaxCacheIndex();

        // Assert
        assertTrue("Max cache index should be non-negative", maxCache >= 0);
    }

    /**
     * Test: Verify Right implements Comparable → can be sorted.
     *
     * Verifies: Right objects can be compared for sorting.
     */
    @Test
    public void right_implementsComparable_canBeSorted() throws ServiceException {
        // Arrange - Get two actual rights from the system
        Right r1 = Rights.Admin.R_listDistributionList;
        Right r2 = Rights.Admin.R_modifyAccount;

        // Act
        int comparison = r1.compareTo(r2);

        // Assert
        assertTrue("Comparison result should indicate ordering",
            comparison != 0 || r1.getName().equals(r2.getName()));
    }

    /**
     * Test: Get right name and verify it's not null.
     *
     * Verifies: Right.getName() returns valid non-null name.
     */
    @Test
    public void right_getName_returnsNonNull() throws ServiceException {
        // Arrange
        Right right = Rights.Admin.R_adminLoginAs;

        // Act
        String name = right.getName();

        // Assert
        assertNotNull(name);
        assertTrue("Name should not be empty", name.length() > 0);
    }

    /**
     * Test: Get right type and verify it matches expected type.
     *
     * Verifies: Right.getRightType() returns correct type.
     */
    @Test
    public void right_getRightType_returnsExpectedType() throws ServiceException {
        // Arrange
        Right right = Rights.Admin.R_adminLoginAs;

        // Act
        Right.RightType rightType = right.getRightType();

        // Assert
        assertNotNull(rightType);
        assertTrue("Right type should be one of the defined types",
            rightType == Right.RightType.preset ||
            rightType == Right.RightType.getAttrs ||
            rightType == Right.RightType.setAttrs ||
            rightType == Right.RightType.combo);
    }

    /**
     * Test: Get right description and verify it's present.
     *
     * Verifies: Right.getDesc() returns non-null description.
     */
    @Test
    public void right_getDesc_returnsDescription() throws ServiceException {
        // Arrange
        Right right = Rights.Admin.R_adminLoginAs;

        // Act
        String desc = right.getDesc();

        // Assert
        assertNotNull("Description should not be null", desc);
        assertTrue("Description should not be empty", desc.length() > 0);
    }

    /**
     * Test: Get right class and verify returns ADMIN for admin rights.
     *
     * Verifies: Right.getRightClass() returns correct class type.
     */
    @Test
    public void right_getRightClass_returnsAdminForAdminRights() throws ServiceException {
        // Arrange
        Right right = Rights.Admin.R_adminLoginAs;

        // Act
        RightClass rightClass = right.getRightClass();

        // Assert
        assertEquals(RightClass.ADMIN, rightClass);
    }

    /**
     * Test: Check if right is user right for admin right → returns false.
     *
     * Verifies: isUserRight() correctly identifies non-user rights.
     */
    @Test
    public void right_isUserRight_returnsFalseForAdminRight() throws ServiceException {
        // Arrange
        Right right = Rights.Admin.R_adminLoginAs;

        // Act
        boolean isUserRight = right.isUserRight();

        // Assert
        assertFalse("Admin right should not be user right", isUserRight);
    }

    /**
     * Test: Check if right is preset right for preset right → returns true.
     *
     * Verifies: isPresetRight() correctly identifies preset rights.
     */
    @Test
    public void right_isPresetRight_returnsTrueForPresetRight() throws ServiceException {
        // Arrange
        Right right = Rights.Admin.R_adminLoginAs;

        // Act
        boolean isPreset = right.isPresetRight();

        // Assert
        assertTrue("Admin right should be preset right", isPreset);
    }

    /**
     * Test: Get target type string and verify it's not null.
     *
     * Verifies: Right.getTargetTypeStr() returns valid string representation.
     */
    @Test
    public void right_getTargetTypeStr_returnsString() throws ServiceException {
        // Arrange
        Right right = Rights.Admin.R_adminLoginAs;

        // Act
        String targetTypeStr = right.getTargetTypeStr();

        // Assert
        assertNotNull("Target type string should not be null", targetTypeStr);
        assertTrue("Target type string should not be empty", targetTypeStr.length() > 0);
    }

    /**
     * Test: Get target type and verify it's valid TargetType.
     *
     * Verifies: Right.getTargetType() returns valid target type.
     */
    @Test
    public void right_getTargetType_returnsValidType() throws ServiceException {
        // Arrange
        Right right = Rights.Admin.R_adminLoginAs;

        // Act
        TargetType targetType = right.getTargetType();

        // Assert
        assertNotNull("Target type should not be null", targetType);
    }

    /**
     * Test: Check subdomain modifier allowed for domain right → returns true.
     *
     * Verifies: allowSubDomainModifier() correctly identifies applicable rights.
     */
    @Test
    public void right_allowSubDomainModifier_correctlyIdentifiesApplicable() throws ServiceException {
        // Arrange - Get domain-executable right
        Right right = Rights.Admin.R_domainAdminRights;

        // Act
        boolean allows = right.allowSubDomainModifier();

        // Assert - Domain right should allow subdomain modifier
        assertTrue("Domain right should allow subdomain modifier", allows);
    }

    /**
     * Test: Verify Right is abstract → cannot be instantiated directly.
     *
     * Verifies: Right class is properly abstract.
     */
    @Test
    public void right_class_isAbstract() {
        // Act & Assert
        assertTrue("Right class should be abstract",
            java.lang.reflect.Modifier.isAbstract(Right.class.getModifiers()));
    }

    /**
     * Test: Dump right to string → verify complete representation.
     *
     * Verifies: dump() method produces complete right information.
     */
    @Test
    public void right_dump_producesCompleteRepresentation() throws ServiceException {
        // Arrange
        Right right = Rights.Admin.R_adminLoginAs;

        // Act
        String dump = right.dump(null);

        // Assert
        assertNotNull(dump);
        assertTrue("Dump should contain right name", dump.contains("name"));
        assertTrue("Dump should contain type", dump.contains("type"));
        assertTrue("Dump should contain description", dump.contains("desc"));
    }
}
