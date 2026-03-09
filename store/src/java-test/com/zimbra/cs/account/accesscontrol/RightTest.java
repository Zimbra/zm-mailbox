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
 * Unit tests for {@link Right} and its inner enum {@link Right.RightType}.
 *
 * Right is abstract; concrete behaviour is exercised through {@link UserRight},
 * which is the simplest subclass available in this package.
 */
public class RightTest {

    // ---------------------------------------------------------------
    // helper — build a minimal UserRight
    // ---------------------------------------------------------------

    private static UserRight makeRight(String name, TargetType tt) throws ServiceException {
        UserRight r = new UserRight(name);
        r.setTargetType(tt);
        r.setDesc("desc");
        return r;
    }

    // ---------------------------------------------------------------
    // RightType.fromString
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
    public void testRightType_fromString_unknown_throwsServiceException() throws ServiceException {
        Right.RightType.fromString("bogusType");
    }

    @Test(expected = ServiceException.class)
    public void testRightType_fromString_empty_throwsServiceException() throws ServiceException {
        Right.RightType.fromString("");
    }

    // ---------------------------------------------------------------
    // RightType.isUserDefinable
    // ---------------------------------------------------------------

    @Test
    public void testRightType_isUserDefinable_preset_returnsFalse() {
        assertFalse(Right.RightType.preset.isUserDefinable());
    }

    @Test
    public void testRightType_isUserDefinable_getAttrs_returnsTrue() {
        assertTrue(Right.RightType.getAttrs.isUserDefinable());
    }

    @Test
    public void testRightType_isUserDefinable_setAttrs_returnsTrue() {
        assertTrue(Right.RightType.setAttrs.isUserDefinable());
    }

    @Test
    public void testRightType_isUserDefinable_combo_returnsTrue() {
        assertTrue(Right.RightType.combo.isUserDefinable());
    }

    // ---------------------------------------------------------------
    // RightType values count
    // ---------------------------------------------------------------

    @Test
    public void testRightType_fourValues() {
        assertEquals(4, Right.RightType.values().length);
    }

    // ---------------------------------------------------------------
    // Right.getName / getDesc / setDesc
    // ---------------------------------------------------------------

    @Test
    public void testGetName() throws ServiceException {
        assertEquals("myRight", makeRight("myRight", TargetType.account).getName());
    }

    @Test
    public void testSetAndGetDesc() throws ServiceException {
        UserRight r = new UserRight("r1");
        r.setDesc("detailed description");
        assertEquals("detailed description", r.getDesc());
    }

    @Test
    public void testGetDesc_nullByDefault() {
        UserRight r = new UserRight("r1");
        assertNull(r.getDesc());
    }

    // ---------------------------------------------------------------
    // Right.getRightType
    // ---------------------------------------------------------------

    @Test
    public void testGetRightType_userRight_isPreset() throws ServiceException {
        assertEquals(Right.RightType.preset, makeRight("r1", TargetType.account).getRightType());
    }

    // ---------------------------------------------------------------
    // Right.getRightClass
    // ---------------------------------------------------------------

    @Test
    public void testGetRightClass_userRight_returnsUserClass() throws ServiceException {
        assertEquals(RightClass.USER, makeRight("r1", TargetType.account).getRightClass());
    }

    // ---------------------------------------------------------------
    // Right.isCacheable / setCacheable / getCacheIndex
    // ---------------------------------------------------------------

    @Test
    public void testIsCacheable_before_setCacheable_returnsFalse() throws ServiceException {
        assertFalse(makeRight("r1", TargetType.account).isCacheable());
    }

    @Test
    public void testIsCacheable_after_setCacheable_returnsTrue() throws ServiceException {
        UserRight r = makeRight("r1", TargetType.account);
        r.setCacheable();
        assertTrue(r.isCacheable());
    }

    @Test
    public void testGetCacheIndex_after_setCacheable_nonNegative() throws ServiceException {
        UserRight r = makeRight("r1", TargetType.account);
        r.setCacheable();
        assertTrue(r.getCacheIndex() >= 0);
    }

    @Test
    public void testGetMaxCacheIndex_incrementsAfterCacheable() throws ServiceException {
        int before = Right.getMaxCacheIndex();
        UserRight r = makeRight("r1", TargetType.account);
        r.setCacheable();
        assertEquals(before + 1, Right.getMaxCacheIndex());
    }

    // ---------------------------------------------------------------
    // Right.compareTo
    // ---------------------------------------------------------------

    @Test
    public void testCompareTo_sameNameEqualsZero() throws ServiceException {
        UserRight r1 = makeRight("alpha", TargetType.account);
        UserRight r2 = makeRight("alpha", TargetType.account);
        assertEquals(0, r1.compareTo(r2));
    }

    @Test
    public void testCompareTo_lexicographicOrder() throws ServiceException {
        UserRight rA = makeRight("alpha", TargetType.account);
        UserRight rB = makeRight("beta",  TargetType.account);
        assertTrue(rA.compareTo(rB) < 0);
        assertTrue(rB.compareTo(rA) > 0);
    }

    // ---------------------------------------------------------------
    // Right.isTheSameRight — base uses reference equality
    // ---------------------------------------------------------------

    @Test
    public void testIsTheSameRight_sameInstance_returnsTrue() throws ServiceException {
        UserRight r = makeRight("r1", TargetType.account);
        assertTrue(r.isTheSameRight(r));
    }

    @Test
    public void testIsTheSameRight_differentInstance_returnsFalse() throws ServiceException {
        UserRight r1 = makeRight("r1", TargetType.account);
        UserRight r2 = makeRight("r2", TargetType.account);
        assertFalse(r1.isTheSameRight(r2));
    }

    // ---------------------------------------------------------------
    // Right.dump
    // ---------------------------------------------------------------

    @Test
    public void testDump_notNullAndContainsName() throws ServiceException {
        UserRight r = makeRight("dumpRight", TargetType.account);
        String dump = r.dump(null);
        assertNotNull(dump);
        assertTrue(dump.contains("dumpRight"));
    }

    // ---------------------------------------------------------------
    // Right.getTargetType / getTargetTypeStr
    // ---------------------------------------------------------------

    @Test
    public void testGetTargetType_returnsSetType() throws ServiceException {
        assertEquals(TargetType.account, makeRight("r1", TargetType.account).getTargetType());
        assertEquals(TargetType.domain,  makeRight("r2", TargetType.domain).getTargetType());
    }

    @Test
    public void testGetTargetTypeStr_returnsCode() throws ServiceException {
        assertEquals("account", makeRight("r1", TargetType.account).getTargetTypeStr());
        assertEquals("domain",  makeRight("r2", TargetType.domain).getTargetTypeStr());
    }

    // ---------------------------------------------------------------
    // Right.allowDisinheritSubGroupsModifier
    // ---------------------------------------------------------------

    @Test
    public void testAllowDisinheritSubGroupsModifier_dlTarget_returnsTrue() throws ServiceException {
        assertTrue(makeRight("r1", TargetType.dl).allowDisinheritSubGroupsModifier());
    }

    @Test
    public void testAllowDisinheritSubGroupsModifier_accountTarget_returnsTrue() throws ServiceException {
        assertTrue(makeRight("r1", TargetType.account).allowDisinheritSubGroupsModifier());
    }

    @Test
    public void testAllowDisinheritSubGroupsModifier_domainTarget_returnsFalse() throws ServiceException {
        assertFalse(makeRight("r1", TargetType.domain).allowDisinheritSubGroupsModifier());
    }

    // ---------------------------------------------------------------
    // Right.verifyTargetType / completeRight
    // ---------------------------------------------------------------

    @Test(expected = ServiceException.class)
    public void testVerifyTargetType_withoutSettingType_throwsServiceException() throws ServiceException {
        UserRight r = new UserRight("r1");
        r.verifyTargetType(); // mTargetType is null → PARSE_ERROR
    }

    @Test(expected = ServiceException.class)
    public void testCompleteRight_withoutDesc_throwsServiceException() throws ServiceException {
        UserRight r = new UserRight("r1");
        r.setTargetType(TargetType.account);
        // desc is null → PARSE_ERROR
        r.completeRight();
    }

    @Test
    public void testCompleteRight_withDescAndTargetType_noException() throws ServiceException {
        UserRight r = new UserRight("r1");
        r.setTargetType(TargetType.account);
        r.setDesc("valid description");
        r.completeRight(); // must not throw
    }

    // ---------------------------------------------------------------
    // Right.setDefault / getDefault
    // ---------------------------------------------------------------

    @Test
    public void testGetDefault_nullByDefault() throws ServiceException {
        assertNull(makeRight("r1", TargetType.account).getDefault());
    }

    @Test
    public void testSetAndGetDefault() throws ServiceException {
        UserRight r = makeRight("r1", TargetType.account);
        r.setDefault(Boolean.TRUE);
        assertEquals(Boolean.TRUE, r.getDefault());
        r.setDefault(Boolean.FALSE);
        assertEquals(Boolean.FALSE, r.getDefault());
    }

    // ---------------------------------------------------------------
    // Right.getGrantTargetTypeStr
    // ---------------------------------------------------------------

    @Test
    public void testGetGrantTargetTypeStr_nullWhenNotSet() throws ServiceException {
        assertNull(makeRight("r1", TargetType.account).getGrantTargetTypeStr());
    }
}
