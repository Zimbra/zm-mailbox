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

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CrossDomain#validateCrossDomainAdminGrant}.
 *
 * <p>Note: {@code Admin.R_crossDomainAdmin} is {@code null} in this test environment
 * because {@code RightManager.init()} has not been called.  The method under test uses
 * reference equality ({@code right == Admin.R_crossDomainAdmin}), so passing {@code null}
 * exercises the "is crossDomainAdmin right" branch.
 *
 * <p>Methods that require live Provisioning/Domain objects ({@code crossDomainOK},
 * {@code checkCrossDomainAdminRight}, {@code checkCrossDomain}) are not tested here
 * as they cannot run without LDAP.
 */
public class CrossDomainTest {

    // ---------------------------------------------------------------
    // helper — a non-null Right mock (i.e. NOT the crossDomainAdmin right)
    // ---------------------------------------------------------------

    private static Right someOtherRight() {
        Right r = Mockito.mock(Right.class);
        Mockito.when(r.getName()).thenReturn("someOtherRight");
        return r;
    }

    // ---------------------------------------------------------------
    // validateCrossDomainAdminGrant — right == crossDomainAdmin (null)
    // ---------------------------------------------------------------

    /**
     * When right == crossDomainAdmin AND granteeType == GT_DOMAIN → returns true (no exception).
     * In the test environment Admin.R_crossDomainAdmin is null, so passing null exercises
     * the same reference-equality path.
     */
    @Test
    public void testValidateCrossDomainAdminGrant_crossDomainRight_domainGranteeType_returnsTrue()
            throws ServiceException {
        // null stands in for Admin.R_crossDomainAdmin (both are null here)
        boolean result = CrossDomain.validateCrossDomainAdminGrant(null, GranteeType.GT_DOMAIN);
        assertTrue(result);
    }

    // ---------------------------------------------------------------
    // validateCrossDomainAdminGrant — right != crossDomainAdmin (non-null mock)
    // ---------------------------------------------------------------

    /**
     * When right != crossDomainAdmin AND granteeType == GT_DOMAIN → INVALID_REQUEST.
     */
    @Test(expected = ServiceException.class)
    public void testValidateCrossDomainAdminGrant_nonCrossDomainRight_domainGranteeType_throws()
            throws ServiceException {
        CrossDomain.validateCrossDomainAdminGrant(someOtherRight(), GranteeType.GT_DOMAIN);
    }

    /**
     * When right != crossDomainAdmin AND granteeType != GT_DOMAIN → returns false.
     */
    @Test
    public void testValidateCrossDomainAdminGrant_nonCrossDomainRight_nonDomainGranteeType_returnsFalse()
            throws ServiceException {
        boolean result = CrossDomain.validateCrossDomainAdminGrant(someOtherRight(), GranteeType.GT_USER);
        assertFalse(result);
    }

    /**
     * Other non-domain grantee types (GT_GROUP) also return false for a non-crossDomainAdmin right.
     */
    @Test
    public void testValidateCrossDomainAdminGrant_nonCrossDomainRight_groupGranteeType_returnsFalse()
            throws ServiceException {
        boolean result = CrossDomain.validateCrossDomainAdminGrant(someOtherRight(), GranteeType.GT_GROUP);
        assertFalse(result);
    }

    /**
     * GT_AUTHUSER grantee type also returns false for a non-crossDomainAdmin right.
     */
    @Test
    public void testValidateCrossDomainAdminGrant_nonCrossDomainRight_authUserGranteeType_returnsFalse()
            throws ServiceException {
        boolean result = CrossDomain.validateCrossDomainAdminGrant(someOtherRight(), GranteeType.GT_AUTHUSER);
        assertFalse(result);
    }

    /**
     * GT_PUBLIC grantee type returns false for a non-crossDomainAdmin right.
     */
    @Test
    public void testValidateCrossDomainAdminGrant_nonCrossDomainRight_publicGranteeType_returnsFalse()
            throws ServiceException {
        boolean result = CrossDomain.validateCrossDomainAdminGrant(someOtherRight(), GranteeType.GT_PUBLIC);
        assertFalse(result);
    }

    /**
     * When right == crossDomainAdmin (null) AND granteeType != GT_DOMAIN → INVALID_REQUEST.
     */
    @Test(expected = ServiceException.class)
    public void testValidateCrossDomainAdminGrant_crossDomainRight_nonDomainGranteeType_throws()
            throws ServiceException {
        // null stands in for Admin.R_crossDomainAdmin; GT_USER is not GT_DOMAIN → should throw
        CrossDomain.validateCrossDomainAdminGrant(null, GranteeType.GT_USER);
    }

    // ---------------------------------------------------------------
    // CrossDomain class-level structural tests
    // ---------------------------------------------------------------

    @Test
    public void testCrossDomain_isPublicClass() {
        assertTrue(Modifier.isPublic(CrossDomain.class.getModifiers()));
    }

    @Test
    public void testCrossDomain_isConcreteClass() {
        assertFalse(Modifier.isAbstract(CrossDomain.class.getModifiers()));
        assertFalse(CrossDomain.class.isInterface());
    }

    @Test
    public void testCrossDomain_isNotEnum() {
        assertFalse(CrossDomain.class.isEnum());
    }

    @Test
    public void testValidateCrossDomainAdminGrant_isPublicAndStatic() throws Exception {
        Method m = CrossDomain.class.getMethod(
                "validateCrossDomainAdminGrant", Right.class, GranteeType.class);
        assertNotNull(m);
        assertTrue(Modifier.isPublic(m.getModifiers()));
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals(boolean.class, m.getReturnType());
    }
}
