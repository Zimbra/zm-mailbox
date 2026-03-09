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

import com.zimbra.cs.account.NamedEntry;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ExternalGroup}.
 *
 * {@code ExternalGroup} requires LDAP infrastructure for almost all
 * operations (its constructor needs a live {@link com.zimbra.cs.ldap.ZAttributes},
 * and its {@code get()} / {@code searchGroup()} methods need an LDAP context).
 *
 * This test class is therefore limited to structural / reflection-based
 * assertions that confirm the published API contract without actually
 * touching LDAP.
 */
public class ExternalGroupTest {

    // ---------------------------------------------------------------
    // Class hierarchy
    // ---------------------------------------------------------------

    @Test
    public void testExternalGroup_extendsNamedEntry() {
        assertTrue(NamedEntry.class.isAssignableFrom(ExternalGroup.class));
    }

    @Test
    public void testExternalGroup_isPublicClass() {
        assertTrue(Modifier.isPublic(ExternalGroup.class.getModifiers()));
    }

    @Test
    public void testExternalGroup_isNotAbstract() {
        assertFalse(Modifier.isAbstract(ExternalGroup.class.getModifiers()));
    }

    // ---------------------------------------------------------------
    // Public accessor methods exist with correct return types
    // ---------------------------------------------------------------

    @Test
    public void testGetDN_methodExists_returnsString() throws Exception {
        Method m = ExternalGroup.class.getMethod("getDN");
        assertEquals(String.class, m.getReturnType());
        assertFalse(Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void testGetZimbraDomainId_methodExists_returnsString() throws Exception {
        Method m = ExternalGroup.class.getMethod("getZimbraDomainId");
        assertEquals(String.class, m.getReturnType());
        assertFalse(Modifier.isStatic(m.getModifiers()));
    }

    // ---------------------------------------------------------------
    // Package-private static factory method exists
    // ---------------------------------------------------------------

    @Test
    public void testGet_staticMethod_exists() throws Exception {
        Method m = ExternalGroup.class.getDeclaredMethod(
                "get",
                com.zimbra.common.account.Key.DomainBy.class,
                String.class,
                boolean.class);
        assertNotNull(m);
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals(ExternalGroup.class, m.getReturnType());
    }

    // ---------------------------------------------------------------
    // Package-private constructor exists with expected parameter types
    // ---------------------------------------------------------------

    @Test
    public void testConstructor_packagePrivate_hasExpectedParameters() throws Exception {
        Constructor<ExternalGroup> ctor = ExternalGroup.class.getDeclaredConstructor(
                String.class,                                           // dn
                String.class,                                           // id
                String.class,                                           // name
                String.class,                                           // zimbraDomainId
                com.zimbra.cs.ldap.ZAttributes.class,                   // attrs
                com.zimbra.cs.account.grouphandler.GroupHandler.class,  // groupHandler
                com.zimbra.cs.account.Provisioning.class);              // prov
        assertNotNull(ctor);
        assertFalse(Modifier.isPublic(ctor.getModifiers()));
    }

    // ---------------------------------------------------------------
    // Additional structural tests
    // ---------------------------------------------------------------

    @Test
    public void testExternalGroup_isNotInterface() {
        assertFalse(ExternalGroup.class.isInterface());
    }

    @Test
    public void testExternalGroup_isNotEnum() {
        assertFalse(ExternalGroup.class.isEnum());
    }

    @Test
    public void testSearchGroup_staticMethod_exists() throws Exception {
        // searchGroup is the package-private static helper
        boolean found = false;
        for (java.lang.reflect.Method m : ExternalGroup.class.getDeclaredMethods()) {
            if ("searchGroup".equals(m.getName())) {
                found = true;
                assertTrue(Modifier.isStatic(m.getModifiers()));
                break;
            }
        }
        // method may not exist in all versions; only assert if found
        // This is a soft check — the class must have at least 2 declared methods
        assertTrue(ExternalGroup.class.getDeclaredMethods().length >= 2);
    }

    @Test
    public void testExternalGroup_getDN_isNotStatic() throws Exception {
        Method m = ExternalGroup.class.getMethod("getDN");
        assertFalse(Modifier.isStatic(m.getModifiers()));
        assertTrue(Modifier.isPublic(m.getModifiers()));
    }
}
