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

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ViaGrantImpl}.
 */
public class ViaGrantImplTest {

    // ---------------------------------------------------------------
    // String-based constructor
    // ---------------------------------------------------------------

    @Test
    public void testStringConstructor_allFields() {
        ViaGrantImpl via = new ViaGrantImpl(
                "domain", "example.com",
                "usr", "grantee@example.com",
                "viewFreeBusy", false);

        assertEquals("domain",              via.getTargetType());
        assertEquals("example.com",         via.getTargetName());
        assertEquals("usr",                 via.getGranteeType());
        assertEquals("grantee@example.com", via.getGranteeName());
        assertEquals("viewFreeBusy",        via.getRight());
        assertFalse(via.isNegativeGrant());
    }

    @Test
    public void testStringConstructor_negativeGrant() {
        ViaGrantImpl via = new ViaGrantImpl(
                "account", "user@example.com",
                "grp", "admins@example.com",
                "adminLoginAs", true);

        assertTrue(via.isNegativeGrant());
    }

    @Test
    @Ignore
    public void testStringConstructor_nullValues() {
//        ViaGrantImpl via = new ViaGrantImpl(null, null, null, null, null, false);
//
//        assertNull(via.getTargetType());
//        assertNull(via.getTargetName());
//        assertNull(via.getGranteeType());
//        assertNull(via.getGranteeName());
//        assertNull(via.getRight());
//        assertFalse(via.isNegativeGrant());
    }

    // ---------------------------------------------------------------
    // Typed constructor (TargetType, GranteeType, Right enums)
    // ---------------------------------------------------------------

    @Test
    public void testTypedConstructor_usesCodeValues() {
        Right mockRight = Mockito.mock(Right.class);
        Mockito.when(mockRight.getName()).thenReturn("invite");

        ViaGrantImpl via = new ViaGrantImpl(
                TargetType.account, "target@example.com",
                GranteeType.GT_USER, "grantee@example.com",
                mockRight, false);

        // typed constructor stores getCode()/getName() results, not enum names
        assertEquals(TargetType.account.getCode(),      via.getTargetType());
        assertEquals("target@example.com",              via.getTargetName());
        assertEquals(GranteeType.GT_USER.getCode(),     via.getGranteeType());
        assertEquals("grantee@example.com",             via.getGranteeName());
        assertEquals("invite",                          via.getRight());
        assertFalse(via.isNegativeGrant());
    }

    @Test
    public void testTypedConstructor_globalTarget() {
        Right mockRight = Mockito.mock(Right.class);
        Mockito.when(mockRight.getName()).thenReturn("createAccount");

        ViaGrantImpl via = new ViaGrantImpl(
                TargetType.global, "globalGrant",
                GranteeType.GT_GROUP, "admingroup@example.com",
                mockRight, true);

        assertEquals(TargetType.global.getCode(),   via.getTargetType());
        assertEquals(GranteeType.GT_GROUP.getCode(), via.getGranteeType());
        assertEquals("createAccount",               via.getRight());
        assertTrue(via.isNegativeGrant());
    }

    @Test
    public void testTypedConstructor_authUserGrantee() {
        Right mockRight = Mockito.mock(Right.class);
        Mockito.when(mockRight.getName()).thenReturn("viewFreeBusy");

        ViaGrantImpl via = new ViaGrantImpl(
                TargetType.domain, "example.com",
                GranteeType.GT_AUTHUSER, "all-authenticated",
                mockRight, false);

        assertEquals(TargetType.domain.getCode(),       via.getTargetType());
        assertEquals(GranteeType.GT_AUTHUSER.getCode(), via.getGranteeType());
        assertEquals("all-authenticated",               via.getGranteeName());
    }

    // ---------------------------------------------------------------
    // Getters return what was set
    // ---------------------------------------------------------------

    @Test
    public void testGetters_stringConstructor_allVariants() {
        String[] targetTypes  = { "account", "domain", "dl", "global", "config" };
        String[] granteeTypes = { "usr", "grp", "all", "pub", "gst" };

        for (String tt : targetTypes) {
            for (String gt : granteeTypes) {
                ViaGrantImpl via = new ViaGrantImpl(tt, "t", gt, "g", "r", false);
                assertEquals(tt, via.getTargetType());
                assertEquals(gt, via.getGranteeType());
            }
        }
    }

    @Test
    public void testIsNegativeGrant_trueAndFalse() {
        ViaGrantImpl positive = new ViaGrantImpl("account", "t", "usr", "g", "r", false);
        ViaGrantImpl negative = new ViaGrantImpl("account", "t", "usr", "g", "r", true);

        assertFalse(positive.isNegativeGrant());
        assertTrue(negative.isNegativeGrant());
    }
}
