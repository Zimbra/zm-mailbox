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
 * Unit tests for {@link CheckRight}.
 *
 * Covers the two package-private static helpers that contain pure logic
 * with no LDAP dependency:
 * <ul>
 *   <li>{@code rightApplicableOnTargetType(TargetType, Right, boolean)}</li>
 * </ul>
 *
 * {@code allowGroupTarget(Right)} reads {@code DebugConfig} which in turn
 * reads {@code LocalConfig}; it is not tested here to avoid a hard runtime
 * dependency on the localconfig infrastructure.
 */
public class CheckRightTest {

    // ---------------------------------------------------------------
    // rightApplicableOnTargetType — canDelegateNeeded = false
    // (uses Right.executableOnTargetType)
    // ---------------------------------------------------------------

    @Test
    public void testRightApplicable_attrRight_executableOnOwnTarget_returnsTrue()
            throws ServiceException {
        AttrRight r = new AttrRight("r", Right.RightType.getAttrs);
        r.setTargetType(TargetType.account);
        // canDelegate=false → executableOnTargetType(account) must be true
        assertTrue(CheckRight.rightApplicableOnTargetType(TargetType.account, r, false));
    }

    @Test
    public void testRightApplicable_attrRight_notExecutableOnOtherTarget_returnsFalse()
            throws ServiceException {
        AttrRight r = new AttrRight("r", Right.RightType.getAttrs);
        r.setTargetType(TargetType.account);
        // account-right is NOT executable on domain target
        assertFalse(CheckRight.rightApplicableOnTargetType(TargetType.domain, r, false));
    }

    @Test
    public void testRightApplicable_attrRight_executableOnCos_returnsTrue()
            throws ServiceException {
        AttrRight r = new AttrRight("r", Right.RightType.setAttrs);
        r.setTargetType(TargetType.cos);
        assertTrue(CheckRight.rightApplicableOnTargetType(TargetType.cos, r, false));
    }

    @Test
    public void testRightApplicable_attrRight_notExecutableOnServer_returnsFalse()
            throws ServiceException {
        AttrRight r = new AttrRight("r", Right.RightType.setAttrs);
        r.setTargetType(TargetType.account);
        assertFalse(CheckRight.rightApplicableOnTargetType(TargetType.server, r, false));
    }

    // ---------------------------------------------------------------
    // rightApplicableOnTargetType — canDelegateNeeded = true
    // (uses Right.grantableOnTargetType)
    // PresetRight.grantableOnTargetType(tt) = tt.isInheritedBy(mTargetType)
    // ---------------------------------------------------------------

    @Test
    public void testRightApplicable_presetAccountRight_grantableOnDomain_returnsTrue()
            throws ServiceException {
        PresetRight r = new PresetRight("r");
        r.setTargetType(TargetType.account);
        // domain.isInheritedBy(account) = true → grantable on domain
        assertTrue(CheckRight.rightApplicableOnTargetType(TargetType.domain, r, true));
    }

    @Test
    public void testRightApplicable_presetAccountRight_grantableOnGlobal_returnsTrue()
            throws ServiceException {
        PresetRight r = new PresetRight("r");
        r.setTargetType(TargetType.account);
        // global.isInheritedBy(account) = true → grantable on global
        assertTrue(CheckRight.rightApplicableOnTargetType(TargetType.global, r, true));
    }

    @Test
    public void testRightApplicable_presetAccountRight_notGrantableOnCos_returnsFalse()
            throws ServiceException {
        PresetRight r = new PresetRight("r");
        r.setTargetType(TargetType.account);
        // cos.isInheritedBy(account) = false → NOT grantable on cos
        assertFalse(CheckRight.rightApplicableOnTargetType(TargetType.cos, r, true));
    }

    @Test
    public void testRightApplicable_presetAccountRight_notGrantableOnServer_returnsFalse()
            throws ServiceException {
        PresetRight r = new PresetRight("r");
        r.setTargetType(TargetType.account);
        assertFalse(CheckRight.rightApplicableOnTargetType(TargetType.server, r, true));
    }

    @Test
    public void testRightApplicable_presetDomainRight_grantableOnGlobal_returnsTrue()
            throws ServiceException {
        PresetRight r = new PresetRight("domainRight");
        r.setTargetType(TargetType.domain);
        assertTrue(CheckRight.rightApplicableOnTargetType(TargetType.global, r, true));
    }

    @Test
    public void testRightApplicable_presetDomainRight_grantableOnSelf_returnsTrue()
            throws ServiceException {
        PresetRight r = new PresetRight("domainRight");
        r.setTargetType(TargetType.domain);
        assertTrue(CheckRight.rightApplicableOnTargetType(TargetType.domain, r, true));
    }

    @Test
    public void testRightApplicable_presetCosRight_notGrantableOnAccount_returnsFalse()
            throws ServiceException {
        PresetRight r = new PresetRight("cosRight");
        r.setTargetType(TargetType.cos);
        // account.isInheritedBy(cos) = false
        assertFalse(CheckRight.rightApplicableOnTargetType(TargetType.account, r, true));
    }

    // ---------------------------------------------------------------
    // rightApplicableOnTargetType — combo right (always executable)
    // ---------------------------------------------------------------

    @Test
    public void testRightApplicable_comboRight_executableOnAnyTarget_returnsTrue() {
        ComboRight combo = new ComboRight("c");
        // ComboRight.executableOnTargetType() always returns true
        for (TargetType tt : TargetType.values()) {
            assertTrue("combo should be applicable on " + tt,
                    CheckRight.rightApplicableOnTargetType(tt, combo, false));
        }
    }
}
