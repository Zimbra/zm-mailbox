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
 * Unit tests for {@link AttrRight}.
 *
 * AttrRight has a package-private constructor accessible from this same-package test.
 */
public class AttrRightTest {

    // ---------------------------------------------------------------
    // Constructor validation
    // ---------------------------------------------------------------

    @Test
    public void testConstructor_getAttrs_succeeds() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        assertNotNull(r);
    }

    @Test
    public void testConstructor_setAttrs_succeeds() throws ServiceException {
        AttrRight r = new AttrRight("setR", Right.RightType.setAttrs);
        assertNotNull(r);
    }

    @Test(expected = ServiceException.class)
    public void testConstructor_preset_throwsServiceException() throws ServiceException {
        new AttrRight("badR", Right.RightType.preset);
    }

    @Test(expected = ServiceException.class)
    public void testConstructor_combo_throwsServiceException() throws ServiceException {
        new AttrRight("badR", Right.RightType.combo);
    }

    // ---------------------------------------------------------------
    // Type identification
    // ---------------------------------------------------------------

    @Test
    public void testIsAttrRight_returnsTrue() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        assertTrue(r.isAttrRight());
    }

    @Test
    public void testIsPresetRight_returnsFalse() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        assertFalse(r.isPresetRight());
    }

    @Test
    public void testIsComboRight_returnsFalse() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        assertFalse(r.isComboRight());
    }

    @Test
    public void testIsUserRight_returnsFalse() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        assertFalse(r.isUserRight());
    }

    @Test
    public void testGetRightType_getAttrs() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        assertEquals(Right.RightType.getAttrs, r.getRightType());
    }

    @Test
    public void testGetRightType_setAttrs() throws ServiceException {
        AttrRight r = new AttrRight("setR", Right.RightType.setAttrs);
        assertEquals(Right.RightType.setAttrs, r.getRightType());
    }

    // ---------------------------------------------------------------
    // allAttrs / addAttr / getAttrs
    // ---------------------------------------------------------------

    @Test
    public void testAllAttrs_true_byDefault() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        assertTrue(r.allAttrs());
    }

    @Test
    public void testAllAttrs_false_afterAddAttr() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        r.addAttr("zimbraId");
        assertFalse(r.allAttrs());
    }

    @Test
    public void testAddAttr_getAttrs_storesAttr() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        r.addAttr("zimbraId");
        assertTrue(r.getAttrs().contains("zimbraId"));
    }

    @Test
    public void testAddAttr_multipleAttrs_allStored() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        r.addAttr("zimbraId");
        r.addAttr("zimbraMailAlias");
        assertEquals(2, r.getAttrs().size());
        assertTrue(r.getAttrs().contains("zimbraId"));
        assertTrue(r.getAttrs().contains("zimbraMailAlias"));
    }

    @Test(expected = ServiceException.class)
    public void testAddAttr_setAttrs_forbiddenAttr_throws() throws ServiceException {
        // zimbraIsAdminAccount is on the hard-coded forbidden list
        AttrRight r = new AttrRight("setR", Right.RightType.setAttrs);
        r.addAttr("zimbraIsAdminAccount");
    }

    @Test
    public void testAddAttr_setAttrs_nonForbiddenAttr_succeeds() throws ServiceException {
        AttrRight r = new AttrRight("setR", Right.RightType.setAttrs);
        r.addAttr("zimbraMailAlias"); // not forbidden
        assertTrue(r.getAttrs().contains("zimbraMailAlias"));
    }

    @Test
    public void testAddAttr_getAttrs_forbiddenAttr_doesNotThrow() throws ServiceException {
        // For getAttrs, forbidden check is NOT applied
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        r.addAttr("zimbraIsAdminAccount"); // must not throw for getAttrs
        assertTrue(r.getAttrs().contains("zimbraIsAdminAccount"));
    }

    // ---------------------------------------------------------------
    // setTargetType / getTargetType / getTargetTypes
    // ---------------------------------------------------------------

    @Test
    public void testSetTargetType_addsToSet() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        r.setTargetType(TargetType.account);
        assertTrue(r.getTargetTypes().contains(TargetType.account));
    }

    @Test
    public void testSetTargetType_canAddMultiple() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        r.setTargetType(TargetType.account);
        r.setTargetType(TargetType.domain);
        assertEquals(2, r.getTargetTypes().size());
    }

    @Test(expected = ServiceException.class)
    public void testGetTargetType_alwaysThrows() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        r.getTargetType();
    }

    // ---------------------------------------------------------------
    // executableOnTargetType
    // ---------------------------------------------------------------

    @Test
    public void testExecutableOnTargetType_matchesAddedTargetType() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        r.setTargetType(TargetType.account);
        assertTrue(r.executableOnTargetType(TargetType.account));
        assertFalse(r.executableOnTargetType(TargetType.domain));
    }

    // ---------------------------------------------------------------
    // suitableFor
    // ---------------------------------------------------------------

    @Test
    public void testSuitableFor_getAttrs_forGetNeeded_true() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        assertTrue(r.suitableFor(Right.RightType.getAttrs));
    }

    @Test
    public void testSuitableFor_getAttrs_forSetNeeded_false() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        assertFalse(r.suitableFor(Right.RightType.setAttrs));
    }

    @Test
    public void testSuitableFor_setAttrs_forGetNeeded_true() throws ServiceException {
        // setAttrs is broader → suitable for getAttrs
        AttrRight r = new AttrRight("setR", Right.RightType.setAttrs);
        assertTrue(r.suitableFor(Right.RightType.getAttrs));
    }

    @Test
    public void testSuitableFor_setAttrs_forSetNeeded_true() throws ServiceException {
        AttrRight r = new AttrRight("setR", Right.RightType.setAttrs);
        assertTrue(r.suitableFor(Right.RightType.setAttrs));
    }

    // ---------------------------------------------------------------
    // overlaps
    // ---------------------------------------------------------------

    @Test
    public void testOverlaps_sameInstance_returnsTrue() throws ServiceException {
        AttrRight r = new AttrRight("getR", Right.RightType.getAttrs);
        r.setTargetType(TargetType.account);
        assertTrue(r.overlaps(r));
    }

    @Test
    public void testOverlaps_presetRight_returnsFalse() throws ServiceException {
        AttrRight attr = new AttrRight("getR", Right.RightType.getAttrs);
        attr.setTargetType(TargetType.account);
        PresetRight preset = new PresetRight("p");
        preset.setTargetType(TargetType.account);
        assertFalse(attr.overlaps(preset));
    }
}
