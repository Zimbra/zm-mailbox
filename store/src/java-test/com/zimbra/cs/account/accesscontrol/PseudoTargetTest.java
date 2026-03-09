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
 * Unit tests for {@link PseudoTarget}.
 *
 * Covers the package-private {@code PseudoZimbraId} inner class (pure logic,
 * no LDAP dependency) and the public static {@code isPseudoEntry(Entry)} method.
 *
 * Pseudo* target subclasses (PseudoAccount, PseudoDomain, etc.) require a live
 * Provisioning environment to construct fully, so they are verified only via
 * structural/reflection checks.
 */
public class PseudoTargetTest {

    // ---------------------------------------------------------------
    // PseudoZimbraId – getPseudoZimbraId
    // ---------------------------------------------------------------

    @Test
    public void testGetPseudoZimbraId_returnsExpectedConstant() {
        String id = PseudoTarget.PseudoZimbraId.getPseudoZimbraId();
        assertEquals("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", id);
    }

    @Test
    public void testGetPseudoZimbraId_hasCorrectLength() {
        String id = PseudoTarget.PseudoZimbraId.getPseudoZimbraId();
        // standard UUID string format: 8-4-4-4-12 = 36 chars
        assertEquals(36, id.length());
    }

    @Test
    public void testGetPseudoZimbraId_containsHyphens() {
        String id = PseudoTarget.PseudoZimbraId.getPseudoZimbraId();
        assertTrue(id.contains("-"));
    }

    @Test
    public void testGetPseudoZimbraId_sameReferenceEveryCall() {
        String first  = PseudoTarget.PseudoZimbraId.getPseudoZimbraId();
        String second = PseudoTarget.PseudoZimbraId.getPseudoZimbraId();
        assertSame(first, second);
    }

    // ---------------------------------------------------------------
    // PseudoZimbraId – isPseudoZimrbaId (note original typo preserved)
    // ---------------------------------------------------------------

    @Test
    public void testIsPseudoZimrbaId_withPseudoId_returnsTrue() {
        String pseudoId = PseudoTarget.PseudoZimbraId.getPseudoZimbraId();
        assertTrue(PseudoTarget.PseudoZimbraId.isPseudoZimrbaId(pseudoId));
    }

    @Test
    public void testIsPseudoZimrbaId_withRealId_returnsFalse() {
        assertFalse(PseudoTarget.PseudoZimbraId.isPseudoZimrbaId(
                "12345678-1234-1234-1234-123456789abc"));
    }

    @Test
    public void testIsPseudoZimrbaId_withEmptyString_returnsFalse() {
        assertFalse(PseudoTarget.PseudoZimbraId.isPseudoZimrbaId(""));
    }

    @Test
    public void testIsPseudoZimrbaId_withNull_returnsFalse() {
        assertFalse(PseudoTarget.PseudoZimbraId.isPseudoZimrbaId(null));
    }

    @Test
    public void testIsPseudoZimrbaId_withPartialMatch_returnsFalse() {
        assertFalse(PseudoTarget.PseudoZimbraId.isPseudoZimrbaId("aaaaaaaa-aaaa"));
    }

    @Test
    public void testIsPseudoZimrbaId_roundtrip_withGetPseudoZimbraId() {
        // getPseudoZimbraId() → isPseudoZimrbaId() must always be true
        assertTrue(PseudoTarget.PseudoZimbraId.isPseudoZimrbaId(
                PseudoTarget.PseudoZimbraId.getPseudoZimbraId()));
    }

    // ---------------------------------------------------------------
    // isPseudoEntry – public static method
    // ---------------------------------------------------------------

    @Test
    public void testIsPseudoEntry_withNull_returnsFalse() {
        assertFalse(PseudoTarget.isPseudoEntry(null));
    }

    @Test
    public void testIsPseudoEntry_methodIsPublicAndStatic() throws Exception {
        Method m = PseudoTarget.class.getMethod(
                "isPseudoEntry",
                com.zimbra.cs.account.Entry.class);
        assertNotNull(m);
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertTrue(Modifier.isPublic(m.getModifiers()));
        assertEquals(boolean.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // PseudoTarget class-level structural tests
    // ---------------------------------------------------------------

    @Test
    public void testPseudoTarget_isPublicClass() {
        assertTrue(Modifier.isPublic(PseudoTarget.class.getModifiers()));
    }

    @Test
    public void testPseudoTarget_isConcreteClass() {
        assertFalse(Modifier.isAbstract(PseudoTarget.class.getModifiers()));
    }

    // ---------------------------------------------------------------
    // PseudoZimbraId – inner class structural tests
    // ---------------------------------------------------------------

    @Test
    public void testPseudoZimbraId_innerClassExists() {
        Class<?>[] inners = PseudoTarget.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("PseudoZimbraId".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("PseudoZimbraId inner class must exist", found);
    }

    @Test
    public void testPseudoAccount_innerClassExists() {
        Class<?>[] inners = PseudoTarget.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("PseudoAccount".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("PseudoAccount inner class must exist", found);
    }

    @Test
    public void testPseudoDomain_innerClassExists() {
        Class<?>[] inners = PseudoTarget.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("PseudoDomain".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("PseudoDomain inner class must exist", found);
    }

    @Test
    public void testPseudoCos_innerClassExists() {
        Class<?>[] inners = PseudoTarget.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("PseudoCos".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("PseudoCos inner class must exist", found);
    }

    @Test
    public void testPseudoAccount_extendsAccount() throws Exception {
        Class<?> pseudoAccount = null;
        for (Class<?> c : PseudoTarget.class.getDeclaredClasses()) {
            if ("PseudoAccount".equals(c.getSimpleName())) {
                pseudoAccount = c;
                break;
            }
        }
        assertNotNull(pseudoAccount);
        assertEquals(com.zimbra.cs.account.Account.class, pseudoAccount.getSuperclass());
    }

    @Test
    public void testPseudoDistributionList_innerClassExists() {
        Class<?>[] inners = PseudoTarget.class.getDeclaredClasses();
        boolean found = false;
        for (Class<?> c : inners) {
            if ("PseudoDistributionList".equals(c.getSimpleName())) {
                found = true;
                break;
            }
        }
        assertTrue("PseudoDistributionList inner class must exist", found);
    }
}
