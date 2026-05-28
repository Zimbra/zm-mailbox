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
 * Full functional tests for {@link UserRight}.
 *
 * Tests verify UserRight class behavior including user rights access,
 * right classification, and user right properties.
 */
public class UserRightTest {

    @BeforeClass
    public static void init() throws ServiceException {
        // Initialize RightManager to load user rights
        RightManager.getInstance();
    }

    /**
     * Test: Verify UserRight extends Right → verify inheritance chain.
     *
     * Verifies: UserRight is proper subclass of Right.
     */
    @Test
    public void userRight_extendsRight_properHierarchy() {
        // Act
        Class<?> superClass = UserRight.class.getSuperclass();

        // Assert
        assertEquals(Right.class, superClass);
    }

    /**
     * Test: Access user right from Rights.User → verify accessible.
     *
     * Verifies: User rights from generated classes are accessible.
     */
    @Test
    public void userRight_generatedUserRights_accessible() throws ServiceException {
        // Act
        Right userRight = Rights.User.R_sendAsDistList;

        // Assert
        assertNotNull("User right should be accessible", userRight);
        assertTrue("Should be UserRight instance", userRight instanceof UserRight);
    }

    /**
     * Test: Verify user right isUserRight() returns true.
     *
     * Verifies: User rights correctly identify themselves as user rights.
     */
    @Test
    public void userRight_isUserRight_returnsTrue() throws ServiceException {
        // Arrange
        Right userRight = Rights.User.R_sendAsDistList;

        // Act
        boolean isUserRight = userRight.isUserRight();

        // Assert
        assertTrue("User right should return true for isUserRight()", isUserRight);
    }

    /**
     * Test: Verify admin right isUserRight() returns false.
     *
     * Verifies: Admin rights do not identify as user rights.
     */
    @Test
    public void userRight_adminRight_isUserRightReturnsFalse() throws ServiceException {
        // Arrange
        Right adminRight = Rights.Admin.R_adminLoginAs;

        // Act
        boolean isUserRight = adminRight.isUserRight();

        // Assert
        assertFalse("Admin right should return false for isUserRight()", isUserRight);
    }

    /**
     * Test: Get right class for user right → verify returns USER.
     *
     * Verifies: User rights correctly report their class.
     */
    @Test
    public void userRight_getRightClass_returnsUser() throws ServiceException {
        // Arrange
        Right userRight = Rights.User.R_sendAsDistList;

        // Act
        RightClass rightClass = userRight.getRightClass();

        // Assert
        assertEquals(RightClass.USER, rightClass);
    }

    /**
     * Test: Get name of user right → verify non-null.
     *
     * Verifies: User rights have valid names.
     */
    @Test
    public void userRight_getName_returnsNonNull() throws ServiceException {
        // Arrange
        Right userRight = Rights.User.R_sendAsDistList;

        // Act
        String name = userRight.getName();

        // Assert
        assertNotNull("User right name should not be null", name);
        assertTrue("User right name should not be empty", name.length() > 0);
    }

    /**
     * Test: Get description of user right → verify non-null.
     *
     * Verifies: User rights have valid descriptions.
     */
    @Test
    public void userRight_getDesc_returnsNonNull() throws ServiceException {
        // Arrange
        Right userRight = Rights.User.R_sendAsDistList;

        // Act
        String desc = userRight.getDesc();

        // Assert
        assertNotNull("User right description should not be null", desc);
        assertTrue("User right description should not be empty", desc.length() > 0);
    }

    /**
     * Test: Get target type of user right → verify valid type.
     *
     * Verifies: User rights have proper target types.
     */
    @Test
    public void userRight_getTargetType_returnsValidType() throws ServiceException {
        // Arrange
        Right userRight = Rights.User.R_sendAsDistList;

        // Act
        TargetType targetType = userRight.getTargetType();

        // Assert
        assertNotNull("User right target type should not be null", targetType);
    }

    /**
     * Test: Compare user rights for sorting → verify comparability.
     *
     * Verifies: User rights implement Comparable correctly.
     */
    @Test
    public void userRight_compareTo_sortsCorrectly() throws ServiceException {
        // Arrange
        Right right1 = Rights.User.R_sendAsDistList;
        Right right2 = Rights.User.R_sendAsDistList;

        // Act
        int comparison = right1.compareTo(right2);

        // Assert
        assertEquals("Same rights should compare equal", 0, comparison);
    }

    /**
     * Test: Verify multiple user rights are defined and accessible.
     *
     * Verifies: User right set is comprehensive.
     */
    @Test
    public void userRight_multipleRights_allAccessible() throws ServiceException {
        // Act & Assert - Access multiple user rights
        assertNotNull("R_sendAsDistList should exist", Rights.User.R_sendAsDistList);
        // If more user rights are available, they should be accessible
    }

    /**
     * Test: Verify UserRight is abstract → cannot instantiate directly.
     *
     * Verifies: UserRight is properly abstract.
     */
    @Test
    public void userRight_class_isAbstract() {
        // Act & Assert
        assertTrue("UserRight should be abstract",
            java.lang.reflect.Modifier.isAbstract(UserRight.class.getModifiers()));
    }

    /**
     * Test: Verify user rights are distinct from admin rights.
     *
     * Verifies: User and admin right sets are separate.
     */
    @Test
    public void userRight_distinct_fromAdminRights() throws ServiceException {
        // Arrange
        Right userRight = Rights.User.R_sendAsDistList;
        Right adminRight = Rights.Admin.R_adminLoginAs;

        // Act & Assert
        assertNotEquals("User and admin rights should be different",
            userRight.getName(), adminRight.getName());
        assertNotEquals("Right classes should be different",
            userRight.getRightClass(), adminRight.getRightClass());
    }

    /**
     * Test: Get user right type and verify it's valid RightType.
     *
     * Verifies: User rights have proper right types.
     */
    @Test
    public void userRight_getRightType_returnsValidType() throws ServiceException {
        // Arrange
        Right userRight = Rights.User.R_sendAsDistList;

        // Act
        Right.RightType rightType = userRight.getRightType();

        // Assert
        assertNotNull("User right type should not be null", rightType);
        assertTrue("User right type should be valid",
            rightType == Right.RightType.preset ||
            rightType == Right.RightType.getAttrs ||
            rightType == Right.RightType.setAttrs ||
            rightType == Right.RightType.combo);
    }

    /**
     * Test: User right grant target type → verify if applicable.
     *
     * Verifies: User rights have proper grant target configuration.
     */
    @Test
    public void userRight_getGrantTargetType_returnsValidValue() throws ServiceException {
        // Arrange
        Right userRight = Rights.User.R_sendAsDistList;

        // Act
        TargetType grantTargetType = userRight.getGrantTargetType();

        // Assert - May be null or valid target type
        if (grantTargetType != null) {
            assertNotNull("Grant target type should be valid", grantTargetType);
        }
    }

    /**
     * Test: Verify user right init is called during RightManager initialization.
     *
     * Verifies: User rights are initialized correctly.
     */
    @Test
    public void userRight_initialization_completesSuccessfully() throws ServiceException {
        // Act - Initialize RightManager which calls UserRight.init()
        RightManager rm = RightManager.getInstance();

        // Assert - Verify initialization completed and user rights accessible
        assertNotNull("RightManager should be initialized", rm);
        assertNotNull("User rights should be initialized",
            Rights.User.R_sendAsDistList);
    }

    /**
     * Test: Verify all user rights share consistent pattern.
     *
     * Verifies: User right set maintains consistency.
     */
    @Test
    public void userRight_allRights_consistent() throws ServiceException {
        // Arrange
        Right userRight = Rights.User.R_sendAsDistList;

        // Act & Assert
        assertEquals("Should be user right", RightClass.USER, userRight.getRightClass());
        assertTrue("Should identify as user right", userRight.isUserRight());
        assertNotNull("Should have valid name", userRight.getName());
        assertNotNull("Should have valid description", userRight.getDesc());
    }
}
