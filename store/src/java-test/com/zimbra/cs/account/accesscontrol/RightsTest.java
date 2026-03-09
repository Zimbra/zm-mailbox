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
 * Unit tests covering:
 * <ul>
 *   <li>{@link Rights} bridge class (Admin/User inner classes)</li>
 *   <li>{@link Right.RightType} enum (fromString, isUserDefinable)</li>
 *   <li>{@link UserRight} type identification and behaviour</li>
 *   <li>{@link Right} cacheability</li>
 * </ul>
 */
public class RightsTest {

    // ---------------------------------------------------------------
    // Rights bridge class — generated rights are null without RightManager
    // ---------------------------------------------------------------

    @Test
    public void testRights_Admin_R_crossDomainAdmin_nullWithoutRightManagerInit() {
        assertNull(Rights.Admin.R_crossDomainAdmin);
    }

    @Test
    public void testRights_User_R_loginAs_nullWithoutRightManagerInit() {
        assertNull(Rights.User.R_loginAs);
    }

    @Test
    public void testRights_Admin_R_adminLoginAs_nullWithoutRightManagerInit() {
        assertNull(Rights.Admin.R_adminLoginAs);
    }

    // ---------------------------------------------------------------
    // Right.RightType — fromString
    // ---------------------------------------------------------------

    @Test
    public void testRightType_fromString_preset() throws ServiceException {
        assertEquals(Right.RightType.preset, Right.RightType.fromString("preset"));
    }

    @Test
    public void testRightType_fromString_getAttrs() throws ServiceException {
        assertEquals(Right.RightType.getAttrs, Right.RightType.fromString("getAttrs"));
    }

    @Test
    public void testRightType_fromString_setAttrs() throws ServiceException {
        assertEquals(Right.RightType.setAttrs, Right.RightType.fromString("setAttrs"));
    }

    @Test
    public void testRightType_fromString_combo() throws ServiceException {
        assertEquals(Right.RightType.combo, Right.RightType.fromString("combo"));
    }

    @Test(expected = ServiceException.class)
    public void testRightType_fromString_invalid_throwsServiceException() throws ServiceException {
        Right.RightType.fromString("notAValidType");
    }

    @Test(expected = ServiceException.class)
    public void testRightType_fromString_empty_throwsServiceException() throws ServiceException {
        Right.RightType.fromString("");
    }

    // ---------------------------------------------------------------
    // Right.RightType — isUserDefinable
    // ---------------------------------------------------------------

    @Test
    public void testRightType_preset_isNotUserDefinable() {
        assertFalse(Right.RightType.preset.isUserDefinable());
    }

    @Test
    public void testRightType_getAttrs_isUserDefinable() {
        assertTrue(Right.RightType.getAttrs.isUserDefinable());
    }

    @Test
    public void testRightType_setAttrs_isUserDefinable() {
        assertTrue(Right.RightType.setAttrs.isUserDefinable());
    }

    @Test
    public void testRightType_combo_isUserDefinable() {
        assertTrue(Right.RightType.combo.isUserDefinable());
    }

    @Test
    public void testRightType_values_hasFourEntries() {
        assertEquals(4, Right.RightType.values().length);
    }

    // ---------------------------------------------------------------
    // UserRight type identification
    // ---------------------------------------------------------------

    @Test
    public void testUserRight_isUserRight_returnsTrue() {
        assertTrue(new UserRight("r").isUserRight());
    }

    @Test
    public void testUserRight_isPresetRight_returnsTrue() {
        // UserRight acts as a preset right for grantable-on purposes
        assertTrue(new UserRight("r").isPresetRight());
    }

    @Test
    public void testUserRight_isAttrRight_returnsFalse() {
        assertFalse(new UserRight("r").isAttrRight());
    }

    @Test
    public void testUserRight_isComboRight_returnsFalse() {
        assertFalse(new UserRight("r").isComboRight());
    }

    @Test
    public void testUserRight_getRightType_returnsPreset() {
        assertEquals(Right.RightType.preset, new UserRight("r").getRightType());
    }

    @Test
    public void testUserRight_getRightClass_returnsUser() {
        assertEquals(RightClass.USER, new UserRight("r").getRightClass());
    }

    // ---------------------------------------------------------------
    // UserRight overlaps
    // ---------------------------------------------------------------

    @Test
    public void testUserRight_overlaps_sameInstance_returnsTrue() throws ServiceException {
        UserRight r = new UserRight("r");
        assertTrue(r.overlaps(r));
    }

    @Test
    public void testUserRight_overlaps_differentInstance_returnsFalse() throws ServiceException {
        UserRight r1 = new UserRight("r1");
        UserRight r2 = new UserRight("r2");
        assertFalse(r1.overlaps(r2));
    }

    // ---------------------------------------------------------------
    // Right compareTo
    // ---------------------------------------------------------------

    @Test
    public void testRight_compareTo_alphabeticalOrdering() {
        UserRight alpha = new UserRight("alpha");
        UserRight beta  = new UserRight("beta");
        assertTrue(alpha.compareTo(beta) < 0);
        assertTrue(beta.compareTo(alpha) > 0);
        assertEquals(0, alpha.compareTo(new UserRight("alpha")));
    }

    // ---------------------------------------------------------------
    // Right cacheability
    // ---------------------------------------------------------------

    @Test
    public void testRight_isCacheable_false_byDefault() {
        assertFalse(new UserRight("r").isCacheable());
    }

    @Test
    public void testRight_isCacheable_true_afterSetCacheable() {
        UserRight r = new UserRight("r");
        r.setCacheable();
        assertTrue(r.isCacheable());
    }

    @Test
    public void testRight_getCacheIndex_distinctAfterSetCacheable() {
        UserRight r1 = new UserRight("r1");
        UserRight r2 = new UserRight("r2");
        r1.setCacheable();
        r2.setCacheable();
        assertNotEquals(r1.getCacheIndex(), r2.getCacheIndex());
    }
}
