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
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DiscoverUserRights}.
 *
 * Only the constructor is unit-testable in isolation; the handle() method
 * requires a live LDAP/Provisioning stack and is covered by integration tests.
 */
public class DiscoverUserRightsTest {

    private static Right mockRight(String name) {
        Right r = Mockito.mock(Right.class);
        Mockito.when(r.getName()).thenReturn(name);
        return r;
    }

    // ---------------------------------------------------------------
    // constructor — empty rights set
    // ---------------------------------------------------------------

    @Test(expected = ServiceException.class)
    public void testConstructor_emptyRights_throwsServiceException() throws ServiceException {
        Set<Right> emptyRights = new HashSet<Right>();
        new DiscoverUserRights(null, emptyRights, false);
    }

    // ---------------------------------------------------------------
    // constructor — non-empty rights set
    // ---------------------------------------------------------------

    @Test
    public void testConstructor_withOneRight_doesNotThrow() throws ServiceException {
        Set<Right> rights = new HashSet<Right>();
        rights.add(mockRight("viewFreeBusy"));
        // Should not throw — just verify object creation succeeds
        DiscoverUserRights dur = new DiscoverUserRights(null, rights, false);
        assertNotNull(dur);
    }

    @Test
    public void testConstructor_withMultipleRights_doesNotThrow() throws ServiceException {
        Set<Right> rights = new HashSet<Right>();
        rights.add(mockRight("viewFreeBusy"));
        rights.add(mockRight("invite"));
        rights.add(mockRight("modifyAccount"));
        DiscoverUserRights dur = new DiscoverUserRights(null, rights, false);
        assertNotNull(dur);
    }

    @Test
    public void testConstructor_onMasterTrue_doesNotThrow() throws ServiceException {
        Set<Right> rights = new HashSet<Right>();
        rights.add(mockRight("viewFreeBusy"));
        DiscoverUserRights dur = new DiscoverUserRights(null, rights, true);
        assertNotNull(dur);
    }

    @Test
    public void testConstructor_onMasterFalse_doesNotThrow() throws ServiceException {
        Set<Right> rights = new HashSet<Right>();
        rights.add(mockRight("viewFreeBusy"));
        DiscoverUserRights dur = new DiscoverUserRights(null, rights, false);
        assertNotNull(dur);
    }

    // ---------------------------------------------------------------
    // Structural tests — class-level API contract
    // ---------------------------------------------------------------

    @Test
    public void testDiscoverUserRights_isPublicClass() {
        assertTrue(Modifier.isPublic(DiscoverUserRights.class.getModifiers()));
    }

    @Test
    public void testDiscoverUserRights_isConcreteClass() {
        assertFalse(Modifier.isAbstract(DiscoverUserRights.class.getModifiers()));
        assertFalse(DiscoverUserRights.class.isInterface());
    }

    @Test
    public void testHandle_methodExists_returnsMap() throws Exception {
        Method m = DiscoverUserRights.class.getDeclaredMethod("handle");
        assertNotNull(m);
        // handle() is package-private
        assertFalse(Modifier.isPublic(m.getModifiers()));
        assertFalse(Modifier.isStatic(m.getModifiers()));
        assertEquals(Map.class, m.getReturnType());
    }

    @Test
    public void testConstructor_isPackagePrivate() throws Exception {
        java.lang.reflect.Constructor<?> ctor = DiscoverUserRights.class.getDeclaredConstructor(
                com.zimbra.cs.account.Account.class,
                Set.class,
                boolean.class);
        assertNotNull(ctor);
        assertFalse(Modifier.isPublic(ctor.getModifiers()));
    }

    // ---------------------------------------------------------------
    // constructor: null rights set → NullPointerException or ServiceException
    // ---------------------------------------------------------------

    @Test(expected = Exception.class)
    public void testConstructor_nullRightsSet_throwsException() throws ServiceException {
        // null set → size() call will throw NPE, or ServiceException
        new DiscoverUserRights(null, null, false);
    }
}
