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
 * Full functional tests for {@link AdminRight}.
 *
 * Tests verify AdminRight class behavior including pseudo rights,
 * system admin rights, and right hierarchy.
 */
public class AdminRightTest {

    @BeforeClass
    public static void init() throws ServiceException {
        // Initialize RightManager to load admin rights
        RightManager.getInstance();
    }

    /**
     * Test: Verify AdminRight extends Right → verify inheritance chain.
     *
     * Verifies: AdminRight is proper subclass of Right.
     */
    @Test
    public void adminRight_extendsRight_properHierarchy() {
        // Act
        Class<?> superClass = AdminRight.class.getSuperclass();

        // Assert
        assertEquals(Right.class, superClass);
    }

    /**
     * Test: Access pseudo right PR_GET_ATTRS → verify non-null.
     *
     * Verifies: Pseudo right constant is initialized.
     */
    @Test
    public void adminRight_prGetAttrs_initialized() {
        // Act & Assert
        assertNotNull("PR_GET_ATTRS should be initialized", AdminRight.PR_GET_ATTRS);
    }

    /**
     * Test: Access pseudo right PR_SET_ATTRS → verify non-null.
     *
     * Verifies: Pseudo right constant is initialized.
     */
    @Test
    public void adminRight_prSetAttrs_initialized() {
        // Act & Assert
        assertNotNull("PR_SET_ATTRS should be initialized", AdminRight.PR_SET_ATTRS);
    }

    /**
     * Test: Access pseudo right PR_ALWAYS_ALLOW → verify non-null.
     *
     * Verifies: Pseudo right constant is initialized.
     */
    @Test
    public void adminRight_prAlwaysAllow_initialized() {
        // Act & Assert
        assertNotNull("PR_ALWAYS_ALLOW should be initialized", AdminRight.PR_ALWAYS_ALLOW);
    }

    /**
     * Test: Access pseudo right PR_SYSTEM_ADMIN_ONLY → verify non-null.
     *
     * Verifies: Pseudo right constant is initialized.
     */
    @Test
    public void adminRight_prSystemAdminOnly_initialized() {
        // Act & Assert
        assertNotNull("PR_SYSTEM_ADMIN_ONLY should be initialized", AdminRight.PR_SYSTEM_ADMIN_ONLY);
    }

    /**
     * Test: Access pseudo right PR_ADMIN_PRESET_RIGHT → verify non-null.
     *
     * Verifies: Pseudo right constant is initialized.
     */
    @Test
    public void adminRight_prAdminPresetRight_initialized() {
        // Act & Assert
        assertNotNull("PR_ADMIN_PRESET_RIGHT should be initialized", AdminRight.PR_ADMIN_PRESET_RIGHT);
    }

    /**
     * Test: Verify PR_GET_ATTRS is AttrRight → verify type.
     *
     * Verifies: PR_GET_ATTRS has correct type.
     */
    @Test
    public void adminRight_prGetAttrs_isAttrRight() {
        // Act & Assert
        assertTrue("PR_GET_ATTRS should be AttrRight",
            AdminRight.PR_GET_ATTRS instanceof AttrRight);
    }

    /**
     * Test: Verify PR_SET_ATTRS is AttrRight → verify type.
     *
     * Verifies: PR_SET_ATTRS has correct type.
     */
    @Test
    public void adminRight_prSetAttrs_isAttrRight() {
        // Act & Assert
        assertTrue("PR_SET_ATTRS should be AttrRight",
            AdminRight.PR_SET_ATTRS instanceof AttrRight);
    }

    /**
     * Test: Verify PR_ALWAYS_ALLOW is AdminRight → verify type.
     *
     * Verifies: PR_ALWAYS_ALLOW has correct type.
     */
    @Test
    public void adminRight_prAlwaysAllow_isAdminRight() {
        // Act & Assert
        assertTrue("PR_ALWAYS_ALLOW should be AdminRight",
            AdminRight.PR_ALWAYS_ALLOW instanceof AdminRight);
    }

    /**
     * Test: Verify PR_ALWAYS_ALLOW is preset right → verify functionality.
     *
     * Verifies: PR_ALWAYS_ALLOW has correct right type.
     */
    @Test
    public void adminRight_prAlwaysAllow_isPresetRight() {
        // Act
        boolean isPreset = AdminRight.PR_ALWAYS_ALLOW.isPresetRight();

        // Assert
        assertTrue("PR_ALWAYS_ALLOW should be preset right", isPreset);
    }

    /**
     * Test: Get PR_GET_ATTRS name → verify correct value.
     *
     * Verifies: Pseudo right has correct name.
     */
    @Test
    public void adminRight_prGetAttrs_correctName() {
        // Act
        String name = AdminRight.PR_GET_ATTRS.getName();

        // Assert
        assertNotNull(name);
        assertTrue("Name should contain 'GET_ATTRS'", name.contains("GET_ATTRS"));
    }

    /**
     * Test: Get PR_SET_ATTRS name → verify correct value.
     *
     * Verifies: Pseudo right has correct name.
     */
    @Test
    public void adminRight_prSetAttrs_correctName() {
        // Act
        String name = AdminRight.PR_SET_ATTRS.getName();

        // Assert
        assertNotNull(name);
        assertTrue("Name should contain 'SET_ATTRS'", name.contains("SET_ATTRS"));
    }

    /**
     * Test: Get PR_ALWAYS_ALLOW name → verify correct value.
     *
     * Verifies: Pseudo right has correct name.
     */
    @Test
    public void adminRight_prAlwaysAllow_correctName() {
        // Act
        String name = AdminRight.PR_ALWAYS_ALLOW.getName();

        // Assert
        assertNotNull(name);
        assertTrue("Name should contain 'ALWAYS_ALLOW'", name.contains("ALWAYS_ALLOW"));
    }

    /**
     * Test: Get PR_GET_ATTRS right type → verify RightType.getAttrs.
     *
     * Verifies: PR_GET_ATTRS has correct right type.
     */
    @Test
    public void adminRight_prGetAttrs_correctRightType() {
        // Act
        Right.RightType rightType = AdminRight.PR_GET_ATTRS.getRightType();

        // Assert
        assertEquals(Right.RightType.getAttrs, rightType);
    }

    /**
     * Test: Get PR_SET_ATTRS right type → verify RightType.setAttrs.
     *
     * Verifies: PR_SET_ATTRS has correct right type.
     */
    @Test
    public void adminRight_prSetAttrs_correctRightType() {
        // Act
        Right.RightType rightType = AdminRight.PR_SET_ATTRS.getRightType();

        // Assert
        assertEquals(Right.RightType.setAttrs, rightType);
    }

    /**
     * Test: Verify all admin pseudo rights are non-null → verify initialization.
     *
     * Verifies: All pseudo right constants are properly initialized.
     */
    @Test
    public void adminRight_allPseudoRights_initialized() {
        // Act & Assert
        assertNotNull("PR_GET_ATTRS should exist", AdminRight.PR_GET_ATTRS);
        assertNotNull("PR_SET_ATTRS should exist", AdminRight.PR_SET_ATTRS);
        assertNotNull("PR_ALWAYS_ALLOW should exist", AdminRight.PR_ALWAYS_ALLOW);
        assertNotNull("PR_SYSTEM_ADMIN_ONLY should exist", AdminRight.PR_SYSTEM_ADMIN_ONLY);
        assertNotNull("PR_ADMIN_PRESET_RIGHT should exist", AdminRight.PR_ADMIN_PRESET_RIGHT);
    }

    /**
     * Test: Verify AdminRight is abstract → cannot instantiate directly.
     *
     * Verifies: AdminRight is properly abstract.
     */
    @Test
    public void adminRight_class_isAbstract() {
        // Act & Assert
        assertTrue("AdminRight should be abstract",
            java.lang.reflect.Modifier.isAbstract(AdminRight.class.getModifiers()));
    }

    /**
     * Test: Access actual admin rights from Rights.Admin → verify accessible.
     *
     * Verifies: Admin rights from generated classes are accessible.
     */
    @Test
    public void adminRight_generatedAdminRights_accessible() throws ServiceException {
        // Act
        Right r1 = Rights.Admin.R_listDistributionList;
        Right r2 = Rights.Admin.R_modifyAccount;

        // Assert
        assertNotNull(r1);
        assertNotNull(r2);
        assertTrue("Generated rights should be AdminRight subclasses",
            r1 instanceof AdminRight);
    }

    /**
     * Test: Get right class for admin right → verify returns ADMIN.
     *
     * Verifies: AdminRight correctly reports its class.
     */
    @Test
    public void adminRight_getRightClass_returnsAdmin() throws ServiceException {
        // Arrange
        Right right = AdminRight.PR_ALWAYS_ALLOW;

        // Act
        RightClass rightClass = right.getRightClass();

        // Assert
        assertEquals(RightClass.ADMIN, rightClass);
    }

    /**
     * Test: Verify PR_ALWAYS_ALLOW and PR_SYSTEM_ADMIN_ONLY are different rights.
     *
     * Verifies: Distinct pseudo rights maintain distinct behavior.
     */
    @Test
    public void adminRight_pseudoRights_areDistinct() {
        // Act & Assert
        assertNotEquals("PR_ALWAYS_ALLOW and PR_SYSTEM_ADMIN_ONLY should be different",
            AdminRight.PR_ALWAYS_ALLOW.getName(),
            AdminRight.PR_SYSTEM_ADMIN_ONLY.getName());
    }

    /**
     * Test: AdminRight init method processes rights correctly → verifies flow.
     *
     * Verifies: init() method properly initializes admin rights.
     */
    @Test
    public void adminRight_initialization_completesSuccessfully() throws ServiceException {
        // Act - Initialize RightManager which calls AdminRight.init()
        RightManager rm = RightManager.getInstance();

        // Assert - Verify initialization completed
        assertNotNull("RightManager should be initialized", rm);
        assertNotNull("PR_GET_ATTRS should be initialized", AdminRight.PR_GET_ATTRS);
    }
}
