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
 * Unit tests for {@link AdminRight}.
 *
 * Verifies the factory method newAdminSystemRight() produces the correct
 * concrete subtype for each RightType, and confirms that pseudo rights
 * are null without RightManager.init().
 */
public class AdminRightTest {

    // ---------------------------------------------------------------
    // newAdminSystemRight factory
    // ---------------------------------------------------------------

    @Test
    public void testNewAdminSystemRight_preset_createsPresetRight() throws ServiceException {
        AdminRight r = AdminRight.newAdminSystemRight("testPreset", Right.RightType.preset);
        assertTrue(r instanceof PresetRight);
    }

    @Test
    public void testNewAdminSystemRight_getAttrs_createsAttrRight() throws ServiceException {
        AdminRight r = AdminRight.newAdminSystemRight("testGetAttrs", Right.RightType.getAttrs);
        assertTrue(r instanceof AttrRight);
        assertEquals(Right.RightType.getAttrs, r.getRightType());
    }

    @Test
    public void testNewAdminSystemRight_setAttrs_createsAttrRight() throws ServiceException {
        AdminRight r = AdminRight.newAdminSystemRight("testSetAttrs", Right.RightType.setAttrs);
        assertTrue(r instanceof AttrRight);
        assertEquals(Right.RightType.setAttrs, r.getRightType());
    }

    @Test
    public void testNewAdminSystemRight_combo_createsComboRight() throws ServiceException {
        AdminRight r = AdminRight.newAdminSystemRight("testCombo", Right.RightType.combo);
        assertTrue(r instanceof ComboRight);
    }

    @Test
    public void testNewAdminSystemRight_preservesName() throws ServiceException {
        AdminRight r = AdminRight.newAdminSystemRight("mySpecialRight", Right.RightType.preset);
        assertEquals("mySpecialRight", r.getName());
    }

    // ---------------------------------------------------------------
    // Pseudo rights are null without RightManager.init()
    // ---------------------------------------------------------------

    @Test
    public void testPseudoRights_allNullWithoutRightManagerInit() {
        // These static fields are set only inside AdminRight.init(RightManager).
        // Since we never call RightManager.getInstance() in this test suite,
        // they remain null — confirming the lazy-initialization contract.
        assertNull(AdminRight.PR_GET_ATTRS);
        assertNull(AdminRight.PR_SET_ATTRS);
        assertNull(AdminRight.PR_ALWAYS_ALLOW);
        assertNull(AdminRight.PR_SYSTEM_ADMIN_ONLY);
        assertNull(AdminRight.PR_ADMIN_PRESET_RIGHT);
    }

    // ---------------------------------------------------------------
    // AdminRight is not a user right
    // ---------------------------------------------------------------

    @Test
    public void testAdminRight_isNotUserRight() throws ServiceException {
        AdminRight preset = AdminRight.newAdminSystemRight("p", Right.RightType.preset);
        AdminRight attrs  = AdminRight.newAdminSystemRight("a", Right.RightType.getAttrs);
        AdminRight combo  = AdminRight.newAdminSystemRight("c", Right.RightType.combo);

        assertFalse(preset.isUserRight());
        assertFalse(attrs.isUserRight());
        assertFalse(combo.isUserRight());
    }

    // ---------------------------------------------------------------
    // RightClass
    // ---------------------------------------------------------------

    @Test
    public void testAdminRight_rightClassIsAdmin() throws ServiceException {
        AdminRight r = AdminRight.newAdminSystemRight("p", Right.RightType.preset);
        assertEquals(RightClass.ADMIN, r.getRightClass());
    }
}
