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

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link SearchGrants}.
 *
 * {@code SearchGrants} performs LDAP searches and all public behaviour
 * requires a live {@link com.zimbra.cs.account.Provisioning} environment.
 * This class therefore verifies:
 * <ul>
 *   <li>Class-level structural properties.</li>
 *   <li>Package-private constructors exist with the expected parameter lists.</li>
 *   <li>The package-private {@code addFetchAttribute} method signatures.</li>
 *   <li>The static inner class {@code GrantsOnTarget} and
 *       {@code SearchGrantsResults} structural contracts.</li>
 * </ul>
 */
public class SearchGrantsTest {

    // ---------------------------------------------------------------
    // Class-level structural tests
    // ---------------------------------------------------------------

    @Test
    public void testSearchGrants_isPublicFinalClass() {
        int mods = SearchGrants.class.getModifiers();
        assertTrue(Modifier.isPublic(mods));
        assertTrue(Modifier.isFinal(mods));
    }

    @Test
    public void testSearchGrants_isConcreteClass() {
        assertFalse(Modifier.isAbstract(SearchGrants.class.getModifiers()));
        assertFalse(SearchGrants.class.isInterface());
    }

    // ---------------------------------------------------------------
    // Package-private constructors
    // ---------------------------------------------------------------

    @Test
    public void testSearchGrants_threeArgConstructor_exists() throws Exception {
        Constructor<?> c = SearchGrants.class.getDeclaredConstructor(
                com.zimbra.cs.account.Provisioning.class,
                Set.class,
                Set.class);
        assertNotNull(c);
        // package-private (no access modifier bits set)
        assertFalse(Modifier.isPublic(c.getModifiers()));
        assertFalse(Modifier.isPrivate(c.getModifiers()));
        assertFalse(Modifier.isProtected(c.getModifiers()));
    }

    @Test
    public void testSearchGrants_fiveArgConstructor_exists() throws Exception {
        Constructor<?> c = SearchGrants.class.getDeclaredConstructor(
                com.zimbra.cs.account.Provisioning.class,
                Set.class,
                com.zimbra.cs.account.Account.class,
                Set.class,
                boolean.class);
        assertNotNull(c);
    }

    // ---------------------------------------------------------------
    // addFetchAttribute methods
    // ---------------------------------------------------------------

    @Test
    public void testAddFetchAttribute_singleStringOverload_exists() throws Exception {
        Method m = SearchGrants.class.getDeclaredMethod("addFetchAttribute", String.class);
        assertNotNull(m);
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void testAddFetchAttribute_setOverload_exists() throws Exception {
        Method m = SearchGrants.class.getDeclaredMethod("addFetchAttribute", Set.class);
        assertNotNull(m);
        assertEquals(void.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // GrantsOnTarget static inner class
    // ---------------------------------------------------------------

    @Test
    public void testGrantsOnTarget_innerClassExists() {
        Class<?>[] inners = SearchGrants.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("GrantsOnTarget".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("GrantsOnTarget inner class must exist", found);
    }

    @Test
    public void testGrantsOnTarget_isStaticAndFinal() throws Exception {
        Class<?> got = null;
        for (Class<?> c : SearchGrants.class.getDeclaredClasses()) {
            if ("GrantsOnTarget".equals(c.getSimpleName())) {
                got = c;
                break;
            }
        }
        assertNotNull(got);
        assertTrue(Modifier.isStatic(got.getModifiers()));
        assertTrue(Modifier.isFinal(got.getModifiers()));
    }

    @Test
    public void testGrantsOnTarget_getTargetEntry_methodExists() throws Exception {
        Class<?> got = null;
        for (Class<?> c : SearchGrants.class.getDeclaredClasses()) {
            if ("GrantsOnTarget".equals(c.getSimpleName())) {
                got = c;
                break;
            }
        }
        assertNotNull(got);
        Method m = got.getDeclaredMethod("getTargetEntry");
        assertNotNull(m);
        assertEquals(com.zimbra.cs.account.Entry.class, m.getReturnType());
    }

    @Test
    public void testGrantsOnTarget_getAcl_methodExists() throws Exception {
        Class<?> got = null;
        for (Class<?> c : SearchGrants.class.getDeclaredClasses()) {
            if ("GrantsOnTarget".equals(c.getSimpleName())) {
                got = c;
                break;
            }
        }
        assertNotNull(got);
        Method m = got.getDeclaredMethod("getAcl");
        assertNotNull(m);
        assertEquals(ZimbraACL.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // SearchGrantsResults static inner class
    // ---------------------------------------------------------------

    @Test
    public void testSearchGrantsResults_innerClassExists() {
        Class<?>[] inners = SearchGrants.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("SearchGrantsResults".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("SearchGrantsResults inner class must exist", found);
    }

    @Test
    public void testSearchGrantsResults_isStaticAndFinal() throws Exception {
        Class<?> sgr = null;
        for (Class<?> c : SearchGrants.class.getDeclaredClasses()) {
            if ("SearchGrantsResults".equals(c.getSimpleName())) {
                sgr = c;
                break;
            }
        }
        assertNotNull(sgr);
        assertTrue(Modifier.isStatic(sgr.getModifiers()));
        assertTrue(Modifier.isFinal(sgr.getModifiers()));
    }

    @Test
    public void testSearchGrantsResults_getResults_methodExists() throws Exception {
        Class<?> sgr = null;
        for (Class<?> c : SearchGrants.class.getDeclaredClasses()) {
            if ("SearchGrantsResults".equals(c.getSimpleName())) {
                sgr = c;
                break;
            }
        }
        assertNotNull(sgr);
        Method m = sgr.getDeclaredMethod("getResults");
        assertNotNull(m);
        assertEquals(Set.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // Additional structural verifications
    // ---------------------------------------------------------------

    @Test
    public void testSearchGrants_hasTwoInnerClasses() {
        int count = 0;
        for (Class<?> c : SearchGrants.class.getDeclaredClasses()) {
            if ("GrantsOnTarget".equals(c.getSimpleName())
                    || "SearchGrantsResults".equals(c.getSimpleName())) {
                count++;
            }
        }
        assertEquals(2, count);
    }

    @Test
    public void testAddFetchAttribute_singleArgVariant_isVoid() throws Exception {
        Method m = SearchGrants.class.getDeclaredMethod(
                "addFetchAttribute", String.class);
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void testAddFetchAttribute_listArgVariant_isVoid() throws Exception {
        Method m = SearchGrants.class.getDeclaredMethod(
                "addFetchAttribute", java.util.List.class);
        assertEquals(void.class, m.getReturnType());
    }

    @Test
    public void testGrantsOnTarget_getTargetEntry_returnType() throws Exception {
        Class<?> got = null;
        for (Class<?> c : SearchGrants.class.getDeclaredClasses()) {
            if ("GrantsOnTarget".equals(c.getSimpleName())) {
                got = c;
                break;
            }
        }
        assertNotNull(got);
        Method m = got.getDeclaredMethod("getTargetEntry");
        assertEquals(com.zimbra.cs.account.Entry.class, m.getReturnType());
    }
}
