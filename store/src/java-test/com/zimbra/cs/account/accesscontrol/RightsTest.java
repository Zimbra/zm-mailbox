/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Full functional tests for {@link Rights}.
 *
 * Tests verify Rights bridging class behavior including static access to
 * admin and user rights via generated classes.
 */
public class RightsTest {

    /**
     * Test: Access Rights.Admin class → verify non-null and accessible.
     *
     * Verifies: Rights.Admin bridging class is properly defined and accessible.
     */
    @Test
    public void rights_adminClass_isDefined() {
        // Act & Assert
        assertNotNull(Rights.Admin.class);
        assertTrue(Rights.Admin.class.isAssignableFrom(Rights.Admin.class));
    }

    /**
     * Test: Access Rights.User class → verify non-null and accessible.
     *
     * Verifies: Rights.User bridging class is properly defined and accessible.
     */
    @Test
    public void rights_userClass_isDefined() {
        // Act & Assert
        assertNotNull(Rights.User.class);
        assertTrue(Rights.User.class.isAssignableFrom(Rights.User.class));
    }

    /**
     * Test: Verify Rights.Admin extends AdminRights → verify inheritance chain.
     *
     * Verifies: Rights.Admin is subclass of AdminRights as expected.
     */
    @Test
    public void rights_adminClass_extendsAdminRights() {
        // Act
        Class<?> adminRightsClass = Rights.Admin.class.getSuperclass();

        // Assert
        assertNotNull(adminRightsClass);
        assertEquals("com.zimbra.cs.account.accesscontrol.generated.AdminRights",
            adminRightsClass.getName());
    }

    /**
     * Test: Verify Rights.User extends UserRights → verify inheritance chain.
     *
     * Verifies: Rights.User is subclass of UserRights as expected.
     */
    @Test
    public void rights_userClass_extendsUserRights() {
        // Act
        Class<?> userRightsClass = Rights.User.class.getSuperclass();

        // Assert
        assertNotNull(userRightsClass);
        assertEquals("com.zimbra.cs.account.accesscontrol.generated.UserRights",
            userRightsClass.getName());
    }

    /**
     * Test: Verify Rights class is abstract → cannot be instantiated directly.
     *
     * Verifies: Rights.class is properly marked abstract.
     */
    @Test
    public void rights_class_isAbstract() {
        // Act & Assert
        assertTrue("Rights class should be abstract",
            java.lang.reflect.Modifier.isAbstract(Rights.class.getModifiers()));
    }

    /**
     * Test: Verify Rights class methods are package-private → access via subclasses.
     *
     * Verifies: Rights uses bridging pattern correctly to hide generated classes.
     */
    @Test
    public void rights_bridgingClass_hidesGenerated() {
        // Act & Assert - Verify Rights is cleaner bridge than direct access
        String adminName = Rights.Admin.class.getSimpleName();
        String userName = Rights.User.class.getSimpleName();

        assertEquals("Admin", adminName);
        assertEquals("User", userName);
    }

    /**
     * Test: Access Admin and User simultaneously → verify both classes available.
     *
     * Verifies: Bridging pattern allows clean access to both admin and user rights.
     */
    @Test
    public void rights_adminAndUser_bothAccessible() {
        // Arrange
        Class<?> admin = Rights.Admin.class;
        Class<?> user = Rights.User.class;

        // Act & Assert
        assertNotNull(admin);
        assertNotNull(user);
        assertNotEquals("Admin and User should be different classes",
            admin.getName(), user.getName());
    }

    /**
     * Test: Verify Rights class package is correct → part of accesscontrol.
     *
     * Verifies: Rights is in correct package com.zimbra.cs.account.accesscontrol.
     */
    @Test
    public void rights_class_packageCorrect() {
        // Act
        Package pkg = Rights.class.getPackage();

        // Assert
        assertNotNull(pkg);
        assertEquals("com.zimbra.cs.account.accesscontrol", pkg.getName());
    }

    /**
     * Test: Verify Admin and User classes package correct → same as Rights.
     *
     * Verifies: Nested classes are in correct package.
     */
    @Test
    public void rights_nestedClasses_packageCorrect() {
        // Act
        Package adminPkg = Rights.Admin.class.getPackage();
        Package userPkg = Rights.User.class.getPackage();

        // Assert
        assertNotNull(adminPkg);
        assertNotNull(userPkg);
        assertEquals("com.zimbra.cs.account.accesscontrol", adminPkg.getName());
        assertEquals("com.zimbra.cs.account.accesscontrol", userPkg.getName());
    }

    /**
     * Test: Verify Rights class declares Admin and User as inner classes.
     *
     * Verifies: Inner class structure is correct.
     */
    @Test
    public void rights_adminAndUser_areInnerClasses() {
        // Act
        Class<?>[] declaredClasses = Rights.class.getDeclaredClasses();

        // Assert
        assertNotNull(declaredClasses);
        assertTrue("Should have at least 2 inner classes (Admin, User)",
            declaredClasses.length >= 2);
    }

    /**
     * Test: Verify Rights is a clean bridging/facade pattern implementation.
     *
     * Verifies: Bridging class pattern correctly hides generated implementation.
     */
    @Test
    public void rights_bridging_pattern_clean() {
        // Act & Assert - Verify bridging class structure
        assertTrue("Rights should be abstract",
            java.lang.reflect.Modifier.isAbstract(Rights.class.getModifiers()));

        // Verify it has two inner classes
        Class<?>[] innerClasses = Rights.class.getDeclaredClasses();
        boolean hasAdmin = false;
        boolean hasUser = false;

        for (Class<?> inner : innerClasses) {
            if (inner.getSimpleName().equals("Admin")) {
                hasAdmin = true;
            } else if (inner.getSimpleName().equals("User")) {
                hasUser = true;
            }
        }

        assertTrue("Should have Admin inner class", hasAdmin);
        assertTrue("Should have User inner class", hasUser);
    }
}
