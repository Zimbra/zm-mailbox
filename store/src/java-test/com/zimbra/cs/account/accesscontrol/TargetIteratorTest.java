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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link TargetIterator}.
 *
 * {@code TargetIterator} is an abstract class; its concrete subclasses
 * ({@code AccountTargetIterator}, {@code DomainTargetIterator}, etc.) all
 * require a live {@link com.zimbra.cs.account.Provisioning} to build a target
 * chain.  The static factory method {@code getTargetIeterator()} similarly
 * requires a valid {@link com.zimbra.cs.account.Entry}.
 *
 * This class therefore verifies:
 * <ul>
 *   <li>Class-level structural properties of {@code TargetIterator}.</li>
 *   <li>Presence and modifiers of the package-private {@code next()} method.</li>
 *   <li>Presence and signature of the static factory method.</li>
 *   <li>Structural contracts of the public static inner iterator classes.</li>
 * </ul>
 */
public class TargetIteratorTest {

    // ---------------------------------------------------------------
    // Class-level structural tests
    // ---------------------------------------------------------------

    @Test
    public void testTargetIterator_isAbstract() {
        assertTrue(Modifier.isAbstract(TargetIterator.class.getModifiers()));
    }

    @Test
    public void testTargetIterator_isPublicClass() {
        assertTrue(Modifier.isPublic(TargetIterator.class.getModifiers()));
    }

    @Test
    public void testTargetIterator_isNotInterface() {
        assertFalse(TargetIterator.class.isInterface());
    }

    // ---------------------------------------------------------------
    // next() – package-private method
    // ---------------------------------------------------------------

    @Test
    public void testNext_methodExists() throws Exception {
        Method m = TargetIterator.class.getDeclaredMethod("next");
        assertNotNull(m);
        // package-private: not public, not private, not protected
        assertFalse(Modifier.isPublic(m.getModifiers()));
        assertFalse(Modifier.isPrivate(m.getModifiers()));
        assertFalse(Modifier.isProtected(m.getModifiers()));
    }

    @Test
    public void testNext_returnTypeIsEntry() throws Exception {
        Method m = TargetIterator.class.getDeclaredMethod("next");
        assertEquals(com.zimbra.cs.account.Entry.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // getTargetIeterator() – static factory (note original typo preserved)
    // ---------------------------------------------------------------

    @Test
    public void testGetTargetIeterator_methodExists() throws Exception {
        Method m = TargetIterator.class.getDeclaredMethod(
                "getTargetIeterator",
                com.zimbra.cs.account.Provisioning.class,
                com.zimbra.cs.account.Entry.class,
                boolean.class);
        assertNotNull(m);
    }

    @Test
    public void testGetTargetIeterator_isStatic() throws Exception {
        Method m = TargetIterator.class.getDeclaredMethod(
                "getTargetIeterator",
                com.zimbra.cs.account.Provisioning.class,
                com.zimbra.cs.account.Entry.class,
                boolean.class);
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void testGetTargetIeterator_returnTypeIsTargetIterator() throws Exception {
        Method m = TargetIterator.class.getDeclaredMethod(
                "getTargetIeterator",
                com.zimbra.cs.account.Provisioning.class,
                com.zimbra.cs.account.Entry.class,
                boolean.class);
        assertEquals(TargetIterator.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // AccountTargetIterator public static inner class
    // ---------------------------------------------------------------

    @Test
    public void testAccountTargetIterator_exists() {
        Class<?>[] inners = TargetIterator.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("AccountTargetIterator".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("AccountTargetIterator inner class must exist", found);
    }

    @Test
    public void testAccountTargetIterator_extendsTargetIterator2() throws Exception {
        Class<?> ati = null;
        for (Class<?> c : TargetIterator.class.getDeclaredClasses()) {
            if ("AccountTargetIterator".equals(c.getSimpleName())) {
                ati = c;
                break;
            }
        }
        assertNotNull(ati);
        assertEquals(TargetIterator.class, ati.getSuperclass());
    }

    @Test
    public void testAccountTargetIterator_isPublicAndStatic() throws Exception {
        Class<?> ati = null;
        for (Class<?> c : TargetIterator.class.getDeclaredClasses()) {
            if ("AccountTargetIterator".equals(c.getSimpleName())) {
                ati = c;
                break;
            }
        }
        assertNotNull(ati);
        assertTrue(Modifier.isPublic(ati.getModifiers()));
        assertTrue(Modifier.isStatic(ati.getModifiers()));
    }

    // ---------------------------------------------------------------
    // DomainTargetIterator public static inner class
    // ---------------------------------------------------------------

    @Test
    public void testDomainTargetIterator_exists() {
        Class<?>[] inners = TargetIterator.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("DomainTargetIterator".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("DomainTargetIterator inner class must exist", found);
    }

    @Test
    public void testDomainTargetIterator_extendsTargetIterator2() throws Exception {
        Class<?> dti = null;
        for (Class<?> c : TargetIterator.class.getDeclaredClasses()) {
            if ("DomainTargetIterator".equals(c.getSimpleName())) {
                dti = c;
                break;
            }
        }
        assertNotNull(dti);
        assertEquals(TargetIterator.class, dti.getSuperclass());
    }

    // ---------------------------------------------------------------
    // CosTargetIterator public static inner class
    // ---------------------------------------------------------------

    @Test
    public void testCosTargetIterator_exists() {
        Class<?>[] inners = TargetIterator.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("CosTargetIterator".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("CosTargetIterator inner class must exist", found);
    }

    // ---------------------------------------------------------------
    // GlobalGrantTargetIterator public static inner class
    // ---------------------------------------------------------------

    @Test
    public void testGlobalGrantTargetIterator_exists() {
        Class<?>[] inners = TargetIterator.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("GlobalGrantTargetIterator".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("GlobalGrantTargetIterator inner class must exist", found);
    }

    @Test
    public void testGlobalGrantTargetIterator_extendsTargetIterator() throws Exception {
        Class<?> ggti = null;
        for (Class<?> c : TargetIterator.class.getDeclaredClasses()) {
            if ("GlobalGrantTargetIterator".equals(c.getSimpleName())) {
                ggti = c;
                break;
            }
        }
        assertNotNull(ggti);
        assertEquals(TargetIterator.class, ggti.getSuperclass());
    }

    // ---------------------------------------------------------------
    // Protected fields – structural confirmation
    // ---------------------------------------------------------------

    @Test
    public void testTargetIterator_mProvField_isProtected() throws Exception {
        java.lang.reflect.Field f = TargetIterator.class.getDeclaredField("mProv");
        assertNotNull(f);
        assertTrue(Modifier.isProtected(f.getModifiers()));
    }

    @Test
    public void testTargetIterator_mNoMoreField_isProtected() throws Exception {
        java.lang.reflect.Field f = TargetIterator.class.getDeclaredField("mNoMore");
        assertNotNull(f);
        assertTrue(Modifier.isProtected(f.getModifiers()));
    }

    @Test
    public void testTargetIterator_mCheckedSelfField_isProtected() throws Exception {
        java.lang.reflect.Field f = TargetIterator.class.getDeclaredField("mCheckedSelf");
        assertNotNull(f);
        assertTrue(Modifier.isProtected(f.getModifiers()));
    }

    // ---------------------------------------------------------------
    // Additional field checks
    // ---------------------------------------------------------------

    @Test
    public void testTargetIterator_mTargetField_isProtected() throws Exception {
        java.lang.reflect.Field f = TargetIterator.class.getDeclaredField("mTarget");
        assertNotNull(f);
        assertTrue(Modifier.isProtected(f.getModifiers()));
    }

    @Test
    public void testTargetIterator_mCurTargetTypeField_isProtected() throws Exception {
        java.lang.reflect.Field f = TargetIterator.class.getDeclaredField("mCurTargetType");
        assertNotNull(f);
        assertTrue(Modifier.isProtected(f.getModifiers()));
    }

    @Test
    public void testTargetIterator_hasAtLeastFourInnerClasses() {
        assertTrue(TargetIterator.class.getDeclaredClasses().length >= 4);
    }

    // ---------------------------------------------------------------
    // next() method signature
    // ---------------------------------------------------------------

    @Test
    public void testNext_methodExists_isPackagePrivate() throws Exception {
        Method m = TargetIterator.class.getDeclaredMethod("next");
        assertNotNull(m);
        assertFalse(Modifier.isPublic(m.getModifiers()));
        assertFalse(Modifier.isStatic(m.getModifiers()));
        assertEquals(com.zimbra.cs.account.Entry.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // Inner classes extend TargetIterator
    // ---------------------------------------------------------------

    @Test
    public void testAccountTargetIterator_extendsTargetIterator() throws Exception {
        Class<?> inner = null;
        for (Class<?> c : TargetIterator.class.getDeclaredClasses()) {
            if ("AccountTargetIterator".equals(c.getSimpleName())) {
                inner = c;
                break;
            }
        }
        assertNotNull(inner);
        assertEquals(TargetIterator.class, inner.getSuperclass());
    }

    @Test
    public void testDomainTargetIterator_extendsTargetIterator() throws Exception {
        Class<?> inner = null;
        for (Class<?> c : TargetIterator.class.getDeclaredClasses()) {
            if ("DomainTargetIterator".equals(c.getSimpleName())) {
                inner = c;
                break;
            }
        }
        assertNotNull(inner);
        assertEquals(TargetIterator.class, inner.getSuperclass());
    }
}
