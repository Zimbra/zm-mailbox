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

import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link UserRight}.
 *
 * UserRight has a package-private constructor, accessible from this test package.
 * setTargetType / setGrantTargetType are also package-private.
 */
public class UserRightTest {

    /** Build a UserRight with the given name and target type. */
    private static UserRight makeRight(String name, TargetType tt) throws ServiceException {
        UserRight r = new UserRight(name);
        r.setTargetType(tt);
        r.setDesc("test right");
        return r;
    }

    // ---------------------------------------------------------------
    // isUserRight / isPresetRight
    // ---------------------------------------------------------------

    @Test
    public void testIsUserRight_returnsTrue() throws ServiceException {
        assertTrue(makeRight("r1", TargetType.account).isUserRight());
    }

    @Test
    public void testIsPresetRight_returnsTrue() throws ServiceException {
        assertTrue(makeRight("r1", TargetType.account).isPresetRight());
    }

    @Test
    public void testIsAttrRight_returnsFalse() throws ServiceException {
        assertFalse(makeRight("r1", TargetType.account).isAttrRight());
    }

    @Test
    public void testIsComboRight_returnsFalse() throws ServiceException {
        assertFalse(makeRight("r1", TargetType.account).isComboRight());
    }

    // ---------------------------------------------------------------
    // allowSubDomainModifier — always false for UserRight
    // ---------------------------------------------------------------

    @Test
    public void testAllowSubDomainModifier_returnsFalse() throws ServiceException {
        // even when target is domain, UserRight always returns false
        assertFalse(makeRight("r1", TargetType.domain).allowSubDomainModifier());
        assertFalse(makeRight("r2", TargetType.account).allowSubDomainModifier());
    }

    // ---------------------------------------------------------------
    // overlaps — reference equality only
    // ---------------------------------------------------------------

    @Test
    public void testOverlaps_sameInstance_returnsTrue() throws ServiceException {
        UserRight r = makeRight("r1", TargetType.account);
        assertTrue(r.overlaps(r));
    }

    @Test
    public void testOverlaps_differentInstance_returnsFalse() throws ServiceException {
        UserRight r1 = makeRight("r1", TargetType.account);
        UserRight r2 = makeRight("r2", TargetType.account);
        assertFalse(r1.overlaps(r2));
        assertFalse(r2.overlaps(r1));
    }

    // ---------------------------------------------------------------
    // executableOnTargetType — with calresource/group disguise
    // ---------------------------------------------------------------

    @Test
    public void testExecOnTargetType_directMatch() throws ServiceException {
        UserRight r = makeRight("r1", TargetType.account);
        assertTrue(r.executableOnTargetType(TargetType.account));
    }

    @Test
    public void testExecOnTargetType_noMatch() throws ServiceException {
        UserRight r = makeRight("r1", TargetType.account);
        assertFalse(r.executableOnTargetType(TargetType.domain));
    }

    @Test
    public void testExecOnTargetType_calresourceDisguisedAsAccount() throws ServiceException {
        // calresource is disguised as account before checking
        UserRight r = makeRight("r1", TargetType.account);
        assertTrue(r.executableOnTargetType(TargetType.calresource));
    }

    @Test
    public void testExecOnTargetType_groupDisguisedAsDl() throws ServiceException {
        // group is disguised as dl before checking
        UserRight r = makeRight("r1", TargetType.dl);
        assertTrue(r.executableOnTargetType(TargetType.group));
    }

    @Test
    public void testExecOnTargetType_dlNotDisguised() throws ServiceException {
        UserRight r = makeRight("r1", TargetType.dl);
        assertTrue(r.executableOnTargetType(TargetType.dl));
        assertFalse(r.executableOnTargetType(TargetType.account));
    }

    // ---------------------------------------------------------------
    // isValidTargetForCustomDynamicGroup
    // ---------------------------------------------------------------

    @Test
    public void testIsValidTarget_group_returnsTrue() throws ServiceException {
        assertTrue(makeRight("r1", TargetType.group).isValidTargetForCustomDynamicGroup());
    }

    @Test
    public void testIsValidTarget_dl_returnsTrue() throws ServiceException {
        assertTrue(makeRight("r1", TargetType.dl).isValidTargetForCustomDynamicGroup());
    }

    @Test
    public void testIsValidTarget_account_returnsFalse() throws ServiceException {
        assertFalse(makeRight("r1", TargetType.account).isValidTargetForCustomDynamicGroup());
    }

    @Test
    public void testIsValidTarget_domain_returnsFalse() throws ServiceException {
        assertFalse(makeRight("r1", TargetType.domain).isValidTargetForCustomDynamicGroup());
    }

    // ---------------------------------------------------------------
    // grantableOnTargetType — without explicit grant target type
    // ---------------------------------------------------------------

    @Test
    public void testGrantableOnTargetType_accountRight_onAccountTarget() throws ServiceException {
        // account.isInheritedBy(account) == true
        UserRight r = makeRight("r1", TargetType.account);
        assertTrue(r.grantableOnTargetType(TargetType.account));
    }

    @Test
    public void testGrantableOnTargetType_accountRight_onDlTarget() throws ServiceException {
        // dl inherits to account → dl.isInheritedBy(account) == true
        UserRight r = makeRight("r1", TargetType.account);
        assertTrue(r.grantableOnTargetType(TargetType.dl));
    }

    @Test
    public void testGrantableOnTargetType_accountRight_onCosTarget() throws ServiceException {
        // cos does NOT inherit to account
        UserRight r = makeRight("r1", TargetType.account);
        assertFalse(r.grantableOnTargetType(TargetType.cos));
    }

    @Test
    public void testGrantableOnTargetType_calresourceDisguisedAsAccount() throws ServiceException {
        // calresource is disguised to account before checking
        UserRight r = makeRight("r1", TargetType.account);
        assertTrue(r.grantableOnTargetType(TargetType.calresource));
    }

    @Test
    public void testGrantableOnTargetType_groupDisguisedAsDl() throws ServiceException {
        // group is disguised to dl; dl.isInheritedBy(dl) == true
        UserRight r = makeRight("r1", TargetType.dl);
        assertTrue(r.grantableOnTargetType(TargetType.group));
    }

    // ---------------------------------------------------------------
    // grantableOnTargetType — with explicit grant target type
    // ---------------------------------------------------------------

    @Test
    public void testGrantableOnTargetType_withGrantTargetType_matchReturnsTrue() throws ServiceException {
        UserRight r = makeRight("r1", TargetType.account);
        r.setGrantTargetType(TargetType.account); // restrict to account only
        assertTrue(r.grantableOnTargetType(TargetType.account));
    }

    @Test
    public void testGrantableOnTargetType_withGrantTargetType_mismatchReturnsFalse() throws ServiceException {
        UserRight r = makeRight("r1", TargetType.account);
        r.setGrantTargetType(TargetType.account);
        assertFalse(r.grantableOnTargetType(TargetType.dl)); // dl != account
    }

    // ---------------------------------------------------------------
    // getGrantableTargetTypes
    // ---------------------------------------------------------------

    @Test
    public void testGetGrantableTargetTypes_withGrantTargetType_singletonSet() throws ServiceException {
        UserRight r = makeRight("r1", TargetType.account);
        r.setGrantTargetType(TargetType.account);
        Set<TargetType> grantable = r.getGrantableTargetTypes();
        assertEquals(1, grantable.size());
        assertTrue(grantable.contains(TargetType.account));
    }

    @Test
    public void testGetGrantableTargetTypes_withoutGrantTargetType_usesInheritFrom() throws ServiceException {
        // account inherits from: account, dl, group, domain, global
        UserRight r = makeRight("r1", TargetType.account);
        Set<TargetType> grantable = r.getGrantableTargetTypes();
        assertFalse(grantable.isEmpty());
        assertTrue(grantable.contains(TargetType.account));
    }

    // ---------------------------------------------------------------
    // setTargetType — cannot be set twice
    // ---------------------------------------------------------------

    @Test(expected = ServiceException.class)
    public void testSetTargetType_twice_throwsServiceException() throws ServiceException {
        UserRight r = new UserRight("r1");
        r.setTargetType(TargetType.account);
        r.setTargetType(TargetType.domain); // second call must throw
    }

    // ---------------------------------------------------------------
    // getRightClass — user right → USER
    // ---------------------------------------------------------------

    @Test
    public void testGetRightClass_returnsUser() throws ServiceException {
        assertEquals(RightClass.USER, makeRight("r1", TargetType.account).getRightClass());
    }

    // ---------------------------------------------------------------
    // getName / getDesc / getRightType
    // ---------------------------------------------------------------

    @Test
    public void testGetName() throws ServiceException {
        assertEquals("myRight", makeRight("myRight", TargetType.account).getName());
    }

    @Test
    public void testGetDesc() throws ServiceException {
        UserRight r = new UserRight("r1");
        r.setDesc("my description");
        assertEquals("my description", r.getDesc());
    }

    @Test
    public void testGetRightType_preset() throws ServiceException {
        assertEquals(Right.RightType.preset, makeRight("r1", TargetType.account).getRightType());
    }
}
