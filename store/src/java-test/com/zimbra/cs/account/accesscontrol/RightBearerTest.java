/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RightBearer}.
 *
 * {@code RightBearer} is an abstract class; its concrete subclasses
 * ({@code GlobalAdmin} and {@code Grantee}) require a live LDAP
 * {@link com.zimbra.cs.account.Provisioning} and a wired
 * {@link com.zimbra.cs.account.NamedEntry} to construct.
 *
 * This class therefore verifies:
 * <ul>
 *   <li>Class-level structure of {@code RightBearer} and its inner classes.</li>
 *   <li>The presence and signatures of key methods.</li>
 *   <li>The inheritance hierarchy between inner classes and their parent.</li>
 * </ul>
 */
public class RightBearerTest {

    // ---------------------------------------------------------------
    // Class-level structural tests
    // ---------------------------------------------------------------

    @Test
    public void testRightBearer_isAbstract() {
        assertTrue(Modifier.isAbstract(RightBearer.class.getModifiers()));
    }

    @Test
    public void testRightBearer_isPublicClass() {
        assertTrue(Modifier.isPublic(RightBearer.class.getModifiers()));
    }

    @Test
    public void testRightBearer_isNotInterface() {
        assertFalse(RightBearer.class.isInterface());
    }

    // ---------------------------------------------------------------
    // GlobalAdmin inner class
    // ---------------------------------------------------------------

    @Test
    public void testGlobalAdmin_innerClassExists() {
        Class<?>[] inners = RightBearer.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("GlobalAdmin".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("GlobalAdmin inner class must exist", found);
    }

    @Test
    public void testGlobalAdmin_extendsRightBearer() throws Exception {
        Class<?> globalAdmin = null;
        for (Class<?> c : RightBearer.class.getDeclaredClasses()) {
            if ("GlobalAdmin".equals(c.getSimpleName())) {
                globalAdmin = c;
                break;
            }
        }
        assertNotNull(globalAdmin);
        assertEquals(RightBearer.class, globalAdmin.getSuperclass());
    }

    @Test
    public void testGlobalAdmin_isNotAbstract() throws Exception {
        Class<?> globalAdmin = null;
        for (Class<?> c : RightBearer.class.getDeclaredClasses()) {
            if ("GlobalAdmin".equals(c.getSimpleName())) {
                globalAdmin = c;
                break;
            }
        }
        assertNotNull(globalAdmin);
        assertFalse(Modifier.isAbstract(globalAdmin.getModifiers()));
    }

    // ---------------------------------------------------------------
    // Grantee inner class
    // ---------------------------------------------------------------

    @Test
    public void testGrantee_innerClassExists() {
        Class<?>[] inners = RightBearer.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("Grantee".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("Grantee inner class must exist", found);
    }

    @Test
    public void testGrantee_extendsRightBearer() throws Exception {
        Class<?> grantee = null;
        for (Class<?> c : RightBearer.class.getDeclaredClasses()) {
            if ("Grantee".equals(c.getSimpleName())) {
                grantee = c;
                break;
            }
        }
        assertNotNull(grantee);
        assertEquals(RightBearer.class, grantee.getSuperclass());
    }

    @Test
    public void testGrantee_isPublic() throws Exception {
        Class<?> grantee = null;
        for (Class<?> c : RightBearer.class.getDeclaredClasses()) {
            if ("Grantee".equals(c.getSimpleName())) {
                grantee = c;
                break;
            }
        }
        assertNotNull(grantee);
        assertTrue(Modifier.isPublic(grantee.getModifiers()));
    }

    @Test
    public void testGrantee_isNotAbstract() throws Exception {
        Class<?> grantee = null;
        for (Class<?> c : RightBearer.class.getDeclaredClasses()) {
            if ("Grantee".equals(c.getSimpleName())) {
                grantee = c;
                break;
            }
        }
        assertNotNull(grantee);
        assertFalse(Modifier.isAbstract(grantee.getModifiers()));
    }

    @Test
    public void testGrantee_clearGranteeCacheMethodExists() throws Exception {
        Class<?> grantee = null;
        for (Class<?> c : RightBearer.class.getDeclaredClasses()) {
            if ("Grantee".equals(c.getSimpleName())) {
                grantee = c;
                break;
            }
        }
        assertNotNull(grantee);
        Method m = grantee.getMethod("clearGranteeCache");
        assertNotNull(m);
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals(void.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // isValidGranteeForAdminRights – static package-private method
    // ---------------------------------------------------------------

    @Test
    public void testIsValidGranteeForAdminRights_methodExists() throws Exception {
        Method m = RightBearer.class.getDeclaredMethod(
                "isValidGranteeForAdminRights",
                GranteeType.class,
                com.zimbra.cs.account.NamedEntry.class);
        assertNotNull(m);
    }

    @Test
    public void testIsValidGranteeForAdminRights_isStatic() throws Exception {
        Method m = RightBearer.class.getDeclaredMethod(
                "isValidGranteeForAdminRights",
                GranteeType.class,
                com.zimbra.cs.account.NamedEntry.class);
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void testIsValidGranteeForAdminRights_returnTypeIsBoolean() throws Exception {
        Method m = RightBearer.class.getDeclaredMethod(
                "isValidGranteeForAdminRights",
                GranteeType.class,
                com.zimbra.cs.account.NamedEntry.class);
        assertEquals(boolean.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // matchesGrantee – static package-private method
    // ---------------------------------------------------------------

    @Test
    public void testMatchesGrantee_methodExists() throws Exception {
        Class<?> granteeClass = null;
        for (Class<?> c : RightBearer.class.getDeclaredClasses()) {
            if ("Grantee".equals(c.getSimpleName())) {
                granteeClass = c;
                break;
            }
        }
        assertNotNull(granteeClass);
        Method m = RightBearer.class.getDeclaredMethod(
                "matchesGrantee",
                granteeClass,
                ZimbraACE.class);
        assertNotNull(m);
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    // ---------------------------------------------------------------
    // matchesGrantee return type
    // ---------------------------------------------------------------

    @Test
    public void testMatchesGrantee_returnTypeIsBoolean() throws Exception {
        Class<?> granteeClass = null;
        for (Class<?> c : RightBearer.class.getDeclaredClasses()) {
            if ("Grantee".equals(c.getSimpleName())) {
                granteeClass = c;
                break;
            }
        }
        assertNotNull(granteeClass);
        Method m = RightBearer.class.getDeclaredMethod(
                "matchesGrantee",
                granteeClass,
                ZimbraACE.class);
        assertEquals(boolean.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // GlobalAdmin inner class — additional checks
    // ---------------------------------------------------------------

    @Test
    public void testGlobalAdmin_isPublic() throws Exception {
        Class<?> globalAdmin = null;
        for (Class<?> c : RightBearer.class.getDeclaredClasses()) {
            if ("GlobalAdmin".equals(c.getSimpleName())) {
                globalAdmin = c;
                break;
            }
        }
        assertNotNull(globalAdmin);
        assertTrue(Modifier.isPublic(globalAdmin.getModifiers()));
    }

    @Test
    public void testGlobalAdmin_isStaticInnerClass() throws Exception {
        Class<?> globalAdmin = null;
        for (Class<?> c : RightBearer.class.getDeclaredClasses()) {
            if ("GlobalAdmin".equals(c.getSimpleName())) {
                globalAdmin = c;
                break;
            }
        }
        assertNotNull(globalAdmin);
        assertTrue(Modifier.isStatic(globalAdmin.getModifiers()));
    }

    @Test
    public void testGrantee_isStaticInnerClass() throws Exception {
        Class<?> grantee = null;
        for (Class<?> c : RightBearer.class.getDeclaredClasses()) {
            if ("Grantee".equals(c.getSimpleName())) {
                grantee = c;
                break;
            }
        }
        assertNotNull(grantee);
        assertTrue(Modifier.isStatic(grantee.getModifiers()));
    }

    // ---------------------------------------------------------------
    // isValidGranteeForAdminRights — return type confirms boolean
    // ---------------------------------------------------------------

    @Test
    public void testIsValidGranteeForAdminRights_parameterTypes() throws Exception {
        Method m = RightBearer.class.getDeclaredMethod(
                "isValidGranteeForAdminRights",
                GranteeType.class,
                com.zimbra.cs.account.NamedEntry.class);
        Class<?>[] params = m.getParameterTypes();
        assertEquals(2, params.length);
        assertEquals(GranteeType.class, params[0]);
        assertEquals(com.zimbra.cs.account.NamedEntry.class, params[1]);
    }

    // ---------------------------------------------------------------
    // Concrete mock account helpers for isValidGranteeForAdminRights
    // ---------------------------------------------------------------

    /** Returns an Account whose getBooleanAttr() responds to the three admin flags. */
    private static Account mockAccount(final boolean isAdmin,
                                       final boolean isDelegated,
                                       final boolean isAdminGroup) {
        return new Account("test@test.com", UUID.randomUUID().toString(), null, null, null) {
            @Override
            public boolean getBooleanAttr(String name, boolean defaultValue) {
                if (Provisioning.A_zimbraIsAdminAccount.equalsIgnoreCase(name))           return isAdmin;
                if (Provisioning.A_zimbraIsDelegatedAdminAccount.equalsIgnoreCase(name)) return isDelegated;
                if (Provisioning.A_zimbraIsAdminGroup.equalsIgnoreCase(name))            return isAdminGroup;
                return defaultValue;
            }
        };
    }

    // ---------------------------------------------------------------
    // isValidGranteeForAdminRights — functional tests
    // These call the package-private static method directly.
    // ---------------------------------------------------------------

    /**
     * GT_USER with zimbraIsDelegatedAdminAccount=true and zimbraIsAdminAccount=false → true.
     */
    @Test
    public void testIsValidGranteeForAdminRights_GT_USER_delegatedAdmin_returnsTrue() {
        assertTrue(RightBearer.isValidGranteeForAdminRights(GranteeType.GT_USER,
                mockAccount(false, true, false)));
    }

    /**
     * GT_USER with zimbraIsAdminAccount=true → false (global admins cannot receive grants).
     */
    @Test
    public void testIsValidGranteeForAdminRights_GT_USER_globalAdmin_returnsFalse() {
        assertFalse(RightBearer.isValidGranteeForAdminRights(GranteeType.GT_USER,
                mockAccount(true, true, false)));
    }

    /**
     * GT_USER with neither admin flag set → false.
     */
    @Test
    public void testIsValidGranteeForAdminRights_GT_USER_regularAccount_returnsFalse() {
        assertFalse(RightBearer.isValidGranteeForAdminRights(GranteeType.GT_USER,
                mockAccount(false, false, false)));
    }

    /**
     * GT_USER with zimbraIsDelegatedAdminAccount=false → false.
     */
    @Test
    public void testIsValidGranteeForAdminRights_GT_USER_notDelegatedAdmin_returnsFalse() {
        assertFalse(RightBearer.isValidGranteeForAdminRights(GranteeType.GT_USER,
                mockAccount(false, false, false)));
    }

    /**
     * GT_GROUP with zimbraIsAdminGroup=true → true.
     */
    @Test
    public void testIsValidGranteeForAdminRights_GT_GROUP_adminGroup_returnsTrue() {
        assertTrue(RightBearer.isValidGranteeForAdminRights(GranteeType.GT_GROUP,
                mockAccount(false, false, true)));
    }

    /**
     * GT_GROUP with zimbraIsAdminGroup=false → false.
     */
    @Test
    public void testIsValidGranteeForAdminRights_GT_GROUP_notAdminGroup_returnsFalse() {
        assertFalse(RightBearer.isValidGranteeForAdminRights(GranteeType.GT_GROUP,
                mockAccount(false, false, false)));
    }

    /**
     * GT_EXT_GROUP → always true regardless of grantee attributes.
     */
    @Test
    public void testIsValidGranteeForAdminRights_GT_EXT_GROUP_alwaysTrue() {
        assertTrue(RightBearer.isValidGranteeForAdminRights(GranteeType.GT_EXT_GROUP,
                mockAccount(false, false, false)));
    }

    /**
     * GT_DOMAIN → always false.
     */
    @Test
    public void testIsValidGranteeForAdminRights_GT_DOMAIN_returnsFalse() {
        assertFalse(RightBearer.isValidGranteeForAdminRights(GranteeType.GT_DOMAIN,
                mockAccount(false, false, false)));
    }

    /**
     * GT_AUTHUSER → always false.
     */
    @Test
    public void testIsValidGranteeForAdminRights_GT_AUTHUSER_returnsFalse() {
        assertFalse(RightBearer.isValidGranteeForAdminRights(GranteeType.GT_AUTHUSER,
                mockAccount(false, false, false)));
    }

    /**
     * GT_PUBLIC → always false.
     */
    @Test
    public void testIsValidGranteeForAdminRights_GT_PUBLIC_returnsFalse() {
        assertFalse(RightBearer.isValidGranteeForAdminRights(GranteeType.GT_PUBLIC,
                mockAccount(false, false, false)));
    }

    // ---------------------------------------------------------------
    // Grantee.clearGranteeCache — smoke test (no exception expected)
    // ---------------------------------------------------------------

    /**
     * clearGranteeCache() must not throw even when the cache is null
     * (which is the default in a unit-test environment with no server config).
     */
    @Test
    public void testClearGranteeCache_doesNotThrow() {
        // In a unit-test environment GRANTEE_CACHE is null → the method is a no-op
        RightBearer.Grantee.clearGranteeCache();
    }
}
