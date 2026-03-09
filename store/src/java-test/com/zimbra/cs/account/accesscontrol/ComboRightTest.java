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
 * Unit tests for {@link ComboRight}.
 */
public class ComboRightTest {

    // ---------------------------------------------------------------
    // Type identification
    // ---------------------------------------------------------------

    @Test
    public void testIsComboRight_returnsTrue() {
        ComboRight r = new ComboRight("c");
        assertTrue(r.isComboRight());
    }

    @Test
    public void testIsPresetRight_returnsFalse() {
        ComboRight r = new ComboRight("c");
        assertFalse(r.isPresetRight());
    }

    @Test
    public void testIsAttrRight_returnsFalse() {
        ComboRight r = new ComboRight("c");
        assertFalse(r.isAttrRight());
    }

    @Test
    public void testIsUserRight_returnsFalse() {
        ComboRight r = new ComboRight("c");
        assertFalse(r.isUserRight());
    }

    @Test
    public void testGetRightType_returnsCombo() {
        ComboRight r = new ComboRight("c");
        assertEquals(Right.RightType.combo, r.getRightType());
    }

    @Test
    public void testGetName_returnsConstructorArg() {
        ComboRight r = new ComboRight("myCombo");
        assertEquals("myCombo", r.getName());
    }

    // ---------------------------------------------------------------
    // executableOnTargetType — always true
    // ---------------------------------------------------------------

    @Test
    public void testExecutableOnTargetType_alwaysTrueForAllTargetTypes() {
        ComboRight r = new ComboRight("c");
        for (TargetType tt : TargetType.values()) {
            assertTrue("executableOnTargetType should be true for " + tt,
                    r.executableOnTargetType(tt));
        }
    }

    // ---------------------------------------------------------------
    // setTargetType / getTargetType / getTargetTypeStr — always throw/null
    // ---------------------------------------------------------------

    @Test(expected = ServiceException.class)
    public void testSetTargetType_alwaysThrows() throws ServiceException {
        new ComboRight("c").setTargetType(TargetType.account);
    }

    @Test(expected = ServiceException.class)
    public void testGetTargetType_alwaysThrows() throws ServiceException {
        new ComboRight("c").getTargetType();
    }

    @Test
    public void testGetTargetTypeStr_returnsNull() {
        assertNull(new ComboRight("c").getTargetTypeStr());
    }

    // ---------------------------------------------------------------
    // addRight
    // ---------------------------------------------------------------

    @Test(expected = ServiceException.class)
    public void testAddRight_userRight_throws() throws ServiceException {
        ComboRight combo = new ComboRight("c");
        combo.addRight(new UserRight("userR"));
    }

    @Test
    public void testAddRight_presetRight_succeeds() throws ServiceException {
        ComboRight combo = new ComboRight("c");
        PresetRight preset = new PresetRight("p");
        combo.addRight(preset);
        assertTrue(combo.getRights().contains(preset));
    }

    @Test
    public void testAddRight_attrRight_succeeds() throws ServiceException {
        ComboRight combo = new ComboRight("c");
        AttrRight attr = new AttrRight("a", Right.RightType.getAttrs);
        combo.addRight(attr);
        assertTrue(combo.getRights().contains(attr));
    }

    // ---------------------------------------------------------------
    // getRights / getAllRights — empty combo
    // ---------------------------------------------------------------

    @Test
    public void testGetRights_emptyCombo_returnsEmptySet() {
        assertTrue(new ComboRight("c").getRights().isEmpty());
    }

    @Test
    public void testGetAllRights_emptyCombo_returnsEmptySet() {
        // getAllRights is populated only after completeRight(); empty combo stays empty
        assertTrue(new ComboRight("c").getAllRights().isEmpty());
    }

    // ---------------------------------------------------------------
    // grantableOnTargetType — empty combo vacuously true
    // ---------------------------------------------------------------

    @Test
    public void testGrantableOnTargetType_emptyCombo_alwaysTrue() {
        ComboRight combo = new ComboRight("c");
        for (TargetType tt : TargetType.values()) {
            assertTrue("grantableOnTargetType should be true for empty combo on " + tt,
                    combo.grantableOnTargetType(tt));
        }
    }

    // ---------------------------------------------------------------
    // allowSubDomainModifier — empty combo false
    // ---------------------------------------------------------------

    @Test
    public void testAllowSubDomainModifier_emptyCombo_returnsFalse() {
        assertFalse(new ComboRight("c").allowSubDomainModifier());
    }

    // ---------------------------------------------------------------
    // containsPresetRight
    // ---------------------------------------------------------------

    @Test
    public void testContainsPresetRight_notContained_returnsFalse() {
        ComboRight combo = new ComboRight("c");
        PresetRight preset = new PresetRight("p");
        // preset was never added → false
        assertFalse(combo.containsPresetRight(preset));
    }
}
