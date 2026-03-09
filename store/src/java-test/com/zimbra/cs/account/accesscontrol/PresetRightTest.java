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
 * Unit tests for {@link PresetRight}.
 *
 * PresetRight has a package-private constructor and is accessible
 * directly from this same-package test.
 */
public class PresetRightTest {

    /** Helper: create a PresetRight with a target type already set. */
    private static PresetRight rightOn(String name, TargetType targetType) throws ServiceException {
        PresetRight r = new PresetRight(name);
        r.setTargetType(targetType);
        return r;
    }

    // ---------------------------------------------------------------
    // Type identification
    // ---------------------------------------------------------------

    @Test
    public void testIsPresetRight_returnsTrue() {
        PresetRight r = new PresetRight("p");
        assertTrue(r.isPresetRight());
    }

    @Test
    public void testIsAttrRight_returnsFalse() {
        PresetRight r = new PresetRight("p");
        assertFalse(r.isAttrRight());
    }

    @Test
    public void testIsComboRight_returnsFalse() {
        PresetRight r = new PresetRight("p");
        assertFalse(r.isComboRight());
    }

    @Test
    public void testIsUserRight_returnsFalse() {
        PresetRight r = new PresetRight("p");
        assertFalse(r.isUserRight());
    }

    @Test
    public void testGetRightType_returnsPreset() {
        PresetRight r = new PresetRight("p");
        assertEquals(Right.RightType.preset, r.getRightType());
    }

    @Test
    public void testGetName_returnsConstructorArg() {
        PresetRight r = new PresetRight("myRight");
        assertEquals("myRight", r.getName());
    }

    // ---------------------------------------------------------------
    // setTargetType / verifyTargetType
    // ---------------------------------------------------------------

    @Test
    public void testSetTargetType_firstCallSucceeds() throws ServiceException {
        PresetRight r = new PresetRight("p");
        r.setTargetType(TargetType.account); // must not throw
    }

    @Test(expected = ServiceException.class)
    public void testSetTargetType_secondCall_throws() throws ServiceException {
        PresetRight r = new PresetRight("p");
        r.setTargetType(TargetType.account);
        r.setTargetType(TargetType.domain); // duplicate → PARSE_ERROR
    }

    // ---------------------------------------------------------------
    // grantableOnTargetType
    // ---------------------------------------------------------------

    @Test
    public void testGrantableOnTargetType_domainTarget_accountRight_returnsTrue() throws ServiceException {
        // domain.isInheritedBy(account) = true because domain's
        // mInheritedByTargetTypes contains account
        PresetRight r = rightOn("accountRight", TargetType.account);
        assertTrue(r.grantableOnTargetType(TargetType.domain));
    }

    @Test
    public void testGrantableOnTargetType_globalTarget_anyRight_returnsTrue() throws ServiceException {
        // global.isInheritedBy(*) = true for all target types
        PresetRight r = rightOn("domainRight", TargetType.domain);
        assertTrue(r.grantableOnTargetType(TargetType.global));
    }

    @Test
    public void testGrantableOnTargetType_cosTarget_accountRight_returnsFalse() throws ServiceException {
        // cos.isInheritedBy(account) = false (cos inherits only from cos)
        PresetRight r = rightOn("accountRight", TargetType.account);
        assertFalse(r.grantableOnTargetType(TargetType.cos));
    }

    @Test
    public void testGrantableOnTargetType_accountTarget_accountRight_returnsTrue() throws ServiceException {
        PresetRight r = rightOn("accountRight", TargetType.account);
        assertTrue(r.grantableOnTargetType(TargetType.account));
    }

    // ---------------------------------------------------------------
    // getGrantableTargetTypes
    // ---------------------------------------------------------------

    @Test
    public void testGetGrantableTargetTypes_accountRight_containsExpectedTypes() throws ServiceException {
        PresetRight r = rightOn("accountRight", TargetType.account);
        Set<TargetType> grantable = r.getGrantableTargetTypes();
        // account.inheritFrom() = {account, dl, group, domain, global}
        assertTrue(grantable.contains(TargetType.global));
        assertTrue(grantable.contains(TargetType.domain));
        assertTrue(grantable.contains(TargetType.dl));
        assertTrue(grantable.contains(TargetType.account));
    }

    @Test
    public void testGetGrantableTargetTypes_cosRight_containsOnlyCosAndGlobal() throws ServiceException {
        PresetRight r = rightOn("cosRight", TargetType.cos);
        Set<TargetType> grantable = r.getGrantableTargetTypes();
        // cos.inheritFrom() = {cos, global}
        assertTrue(grantable.contains(TargetType.cos));
        assertTrue(grantable.contains(TargetType.global));
        assertFalse(grantable.contains(TargetType.account));
        assertFalse(grantable.contains(TargetType.domain));
    }

    // ---------------------------------------------------------------
    // overlaps
    // ---------------------------------------------------------------

    @Test
    public void testOverlaps_sameInstance_returnsTrue() throws ServiceException {
        PresetRight r = rightOn("r1", TargetType.account);
        assertTrue(r.overlaps(r));
    }

    @Test
    public void testOverlaps_differentPresetRight_returnsFalse() throws ServiceException {
        PresetRight r1 = rightOn("r1", TargetType.account);
        PresetRight r2 = rightOn("r2", TargetType.account);
        assertFalse(r1.overlaps(r2));
    }

    @Test
    public void testOverlaps_attrRight_returnsFalse() throws ServiceException {
        PresetRight preset = rightOn("p", TargetType.account);
        AttrRight attr = new AttrRight("a", Right.RightType.getAttrs);
        assertFalse(preset.overlaps(attr));
    }

    @Test
    public void testOverlaps_emptyComboRight_returnsFalse() throws ServiceException {
        PresetRight preset = rightOn("p", TargetType.account);
        ComboRight combo = new ComboRight("c");
        // empty combo contains no preset rights → containsPresetRight(preset) = false
        assertFalse(preset.overlaps(combo));
    }
}
