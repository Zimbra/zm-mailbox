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
import com.zimbra.common.soap.AdminConstants;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link RightModifier}.
 */
public class RightModifierTest {

    // ---------------------------------------------------------------
    // fromChar
    // ---------------------------------------------------------------

    @Test
    public void testFromChar_deny() throws ServiceException {
        assertEquals(RightModifier.RM_DENY, RightModifier.fromChar('-'));
    }

    @Test
    public void testFromChar_canDelegate() throws ServiceException {
        assertEquals(RightModifier.RM_CAN_DELEGATE, RightModifier.fromChar('+'));
    }

    @Test
    public void testFromChar_subDomain() throws ServiceException {
        assertEquals(RightModifier.RM_SUBDOMAIN, RightModifier.fromChar('*'));
    }

    @Test
    public void testFromChar_disinheritSubGroups() throws ServiceException {
        assertEquals(RightModifier.RM_DISINHERIT_SUB_GROUPS, RightModifier.fromChar('^'));
    }

    @Test
    public void testFromChar_unknown_returnsNull() throws ServiceException {
        assertNull(RightModifier.fromChar('x'));
    }

    @Test
    public void testFromChar_letter_returnsNull() throws ServiceException {
        assertNull(RightModifier.fromChar('a'));
    }

    @Test
    public void testFromChar_digit_returnsNull() throws ServiceException {
        assertNull(RightModifier.fromChar('1'));
    }

    @Test
    public void testFromChar_space_returnsNull() throws ServiceException {
        assertNull(RightModifier.fromChar(' '));
    }

    // ---------------------------------------------------------------
    // getModifier
    // ---------------------------------------------------------------

    @Test
    public void testGetModifier_deny() {
        assertEquals('-', RightModifier.RM_DENY.getModifier());
    }

    @Test
    public void testGetModifier_canDelegate() {
        assertEquals('+', RightModifier.RM_CAN_DELEGATE.getModifier());
    }

    @Test
    public void testGetModifier_subDomain() {
        assertEquals('*', RightModifier.RM_SUBDOMAIN.getModifier());
    }

    @Test
    public void testGetModifier_disinheritSubGroups() {
        assertEquals('^', RightModifier.RM_DISINHERIT_SUB_GROUPS.getModifier());
    }

    // ---------------------------------------------------------------
    // getSoapAttrMapping
    // ---------------------------------------------------------------

    @Test
    public void testGetSoapAttrMapping_deny() {
        assertEquals(AdminConstants.A_DENY, RightModifier.RM_DENY.getSoapAttrMapping());
    }

    @Test
    public void testGetSoapAttrMapping_canDelegate() {
        assertEquals(AdminConstants.A_CAN_DELEGATE, RightModifier.RM_CAN_DELEGATE.getSoapAttrMapping());
    }

    @Test
    public void testGetSoapAttrMapping_subDomain() {
        assertEquals(AdminConstants.A_SUB_DOMAIN, RightModifier.RM_SUBDOMAIN.getSoapAttrMapping());
    }

    @Test
    public void testGetSoapAttrMapping_disinheritSubGroups() {
        assertEquals(AdminConstants.A_DISINHERIT_SUB_GROUPS, RightModifier.RM_DISINHERIT_SUB_GROUPS.getSoapAttrMapping());
    }

    // ---------------------------------------------------------------
    // getDescription
    // ---------------------------------------------------------------

    @Test
    public void testGetDescription_notNull() {
        for (RightModifier rm : RightModifier.values()) {
            assertNotNull("description should not be null for " + rm, rm.getDescription());
            assertFalse("description should not be empty for " + rm, rm.getDescription().isEmpty());
        }
    }

    @Test
    public void testGetDescription_deny() {
        assertTrue(RightModifier.RM_DENY.getDescription().contains("denied"));
    }

    @Test
    public void testGetDescription_canDelegate() {
        assertTrue(RightModifier.RM_CAN_DELEGATE.getDescription().contains("delegate"));
    }

    @Test
    public void testGetDescription_subDomain() {
        assertTrue(RightModifier.RM_SUBDOMAIN.getDescription().contains("sub domain"));
    }

    // ---------------------------------------------------------------
    // round-trip: fromChar(getModifier()) == original
    // ---------------------------------------------------------------

    @Test
    public void testFromChar_roundTrip() throws ServiceException {
        for (RightModifier rm : RightModifier.values()) {
            assertEquals(rm, RightModifier.fromChar(rm.getModifier()));
        }
    }

    // ---------------------------------------------------------------
    // enum values count
    // ---------------------------------------------------------------

    @Test
    public void testValues_count() {
        assertEquals(4, RightModifier.values().length);
    }
}
