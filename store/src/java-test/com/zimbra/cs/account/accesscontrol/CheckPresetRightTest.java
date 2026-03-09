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

import com.zimbra.common.service.ServiceException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CheckPresetRight}.
 *
 * {@code CheckPresetRight.check()} is a complex method that traverses LDAP
 * ACLs and requires a fully wired provisioning environment; it cannot be
 * exercised here without an integration harness.
 *
 * What CAN be tested without LDAP:
 * <ul>
 *   <li>The class is accessible from the same package.</li>
 *   <li>The private inner class {@code SeenRight} exists and works correctly
 *       – verified indirectly through the observable behaviour of the outer
 *       class constructor (which instantiates SeenRight).</li>
 *   <li>The relationship between {@code CheckPresetRight} and its super-class
 *       {@link CheckRight} is preserved.</li>
 * </ul>
 */
public class CheckPresetRightTest {

    // ---------------------------------------------------------------
    // Class hierarchy
    // ---------------------------------------------------------------

    @Test
    public void testCheckPresetRight_extendsCheckRight() {
        assertTrue(CheckRight.class.isAssignableFrom(CheckPresetRight.class));
    }

    // ---------------------------------------------------------------
    // SeenRight inner class – indirect behaviour tests
    // SeenRight is private, but its state drives the return value of
    // checkRight(): if seenRight() is false AND no ACL matched, null
    // is returned; if true AND no ACL matched, Boolean.FALSE is returned.
    // We can verify this via reflection on SeenRight directly.
    // ---------------------------------------------------------------

    @Test
    public void testSeenRight_defaultState_isFalse() throws Exception {
        // Obtain SeenRight class via reflection (it is a private static inner)
        Class<?> seenRightClass = null;
        for (Class<?> inner : CheckPresetRight.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("SeenRight")) {
                seenRightClass = inner;
                break;
            }
        }
        assertNotNull("SeenRight inner class must exist", seenRightClass);

//        seenRightClass.setAccessible(true);
        Object seenRight = seenRightClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Method seenRightMethod =
                seenRightClass.getDeclaredMethod("seenRight");
        seenRightMethod.setAccessible(true);

        // Before calling setSeenRight(), seenRight() must be false
        assertFalse((Boolean) seenRightMethod.invoke(seenRight));
    }

    @Test
    public void testSeenRight_afterSetSeenRight_isTrue() throws Exception {
        Class<?> seenRightClass = null;
        for (Class<?> inner : CheckPresetRight.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("SeenRight")) {
                seenRightClass = inner;
                break;
            }
        }
        assertNotNull(seenRightClass);

//        seenRightClass.setAccessible(true);
        Object seenRight = seenRightClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Method setSeenRight =
                seenRightClass.getDeclaredMethod("setSeenRight");
        setSeenRight.setAccessible(true);
        setSeenRight.invoke(seenRight);

        java.lang.reflect.Method seenRightMethod =
                seenRightClass.getDeclaredMethod("seenRight");
        seenRightMethod.setAccessible(true);

        assertTrue((Boolean) seenRightMethod.invoke(seenRight));
    }

    @Test
    public void testSeenRight_setSeenRight_isIdempotent() throws Exception {
        Class<?> seenRightClass = null;
        for (Class<?> inner : CheckPresetRight.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("SeenRight")) {
                seenRightClass = inner;
                break;
            }
        }
        assertNotNull(seenRightClass);
//        seenRightClass.setAccessible(true);
        Object seenRight = seenRightClass.getDeclaredConstructor().newInstance();

        java.lang.reflect.Method setSeenRight =
                seenRightClass.getDeclaredMethod("setSeenRight");
        setSeenRight.setAccessible(true);
        java.lang.reflect.Method seenRightMethod =
                seenRightClass.getDeclaredMethod("seenRight");
        seenRightMethod.setAccessible(true);

        setSeenRight.invoke(seenRight);
        setSeenRight.invoke(seenRight); // call twice – should still be true
        assertTrue((Boolean) seenRightMethod.invoke(seenRight));
    }

    // ---------------------------------------------------------------
    // check() method signature – ensure the method is accessible
    // ---------------------------------------------------------------

    @Test
    public void testCheck_methodExists_withExpectedSignature() throws Exception {
        java.lang.reflect.Method checkMethod = CheckPresetRight.class.getMethod(
                "check",
                com.zimbra.cs.account.MailTarget.class,
                com.zimbra.cs.account.Entry.class,
                Right.class,
                boolean.class,
                com.zimbra.cs.account.AccessManager.ViaGrant.class);
        assertNotNull(checkMethod);
        assertEquals(Boolean.class, checkMethod.getReturnType());
    }

    // ---------------------------------------------------------------
    // check() is public static
    // ---------------------------------------------------------------

    @Test
    public void testCheck_isPublicAndStatic() throws Exception {
        java.lang.reflect.Method m = CheckPresetRight.class.getMethod(
                "check",
                com.zimbra.cs.account.MailTarget.class,
                com.zimbra.cs.account.Entry.class,
                Right.class,
                boolean.class,
                com.zimbra.cs.account.AccessManager.ViaGrant.class);
        assertTrue(java.lang.reflect.Modifier.isPublic(m.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(m.getModifiers()));
    }

    // ---------------------------------------------------------------
    // CheckPresetRight class-level structural tests
    // ---------------------------------------------------------------

    @Test
    public void testCheckPresetRight_isPublicConcreteClass() {
        assertTrue(java.lang.reflect.Modifier.isPublic(CheckPresetRight.class.getModifiers()));
        assertFalse(java.lang.reflect.Modifier.isAbstract(CheckPresetRight.class.getModifiers()));
    }

    // ---------------------------------------------------------------
    // SeenRight: isPrivateStaticInnerClass + setSeenRight returns void
    // ---------------------------------------------------------------

    @Test
    public void testSeenRight_isPrivateStaticInnerClass() throws Exception {
        Class<?> seenRightClass = null;
        for (Class<?> inner : CheckPresetRight.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("SeenRight")) {
                seenRightClass = inner;
                break;
            }
        }
        assertNotNull(seenRightClass);
        assertTrue(java.lang.reflect.Modifier.isPrivate(seenRightClass.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isStatic(seenRightClass.getModifiers()));
    }

    @Test
    public void testSeenRight_setSeenRight_returnsVoid() throws Exception {
        Class<?> seenRightClass = null;
        for (Class<?> inner : CheckPresetRight.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("SeenRight")) {
                seenRightClass = inner;
                break;
            }
        }
        assertNotNull(seenRightClass);
        java.lang.reflect.Method m = seenRightClass.getDeclaredMethod("setSeenRight");
        assertEquals(void.class, m.getReturnType());
    }
}
