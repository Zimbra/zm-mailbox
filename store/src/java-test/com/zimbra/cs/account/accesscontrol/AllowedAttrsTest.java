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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AllowedAttrs}.
 */
public class AllowedAttrsTest {

    // ---------------------------------------------------------------
    // ALLOW_ALL_ATTRS factory
    // ---------------------------------------------------------------

    @Test
    public void testAllowAll_resultIsAllowAll() {
        AllowedAttrs aa = AllowedAttrs.ALLOW_ALL_ATTRS();
        assertEquals(AllowedAttrs.Result.ALLOW_ALL, aa.getResult());
    }

    @Test
    public void testAllowAll_allowAttr_anyAttrReturnsTrue() {
        AllowedAttrs aa = AllowedAttrs.ALLOW_ALL_ATTRS();
        assertTrue(aa.allowAttr("zimbraMailHost"));
        assertTrue(aa.allowAttr("cn"));
        assertTrue(aa.allowAttr("nonExistentAttr"));
        assertTrue(aa.allowAttr("+zimbraMailHost"));  // prefixed variant
        assertTrue(aa.allowAttr("-zimbraMailHost"));  // prefixed variant
    }

    @Test
    public void testAllowAll_getAllowed_returnsNull() {
        // getAllowed() is only meaningful for ALLOW_SOME; for others it may be null
        AllowedAttrs aa = AllowedAttrs.ALLOW_ALL_ATTRS();
        assertNull(aa.getAllowed());
    }

    // ---------------------------------------------------------------
    // DENY_ALL_ATTRS factory
    // ---------------------------------------------------------------

    @Test
    public void testDenyAll_resultIsDenyAll() {
        AllowedAttrs da = AllowedAttrs.DENY_ALL_ATTRS();
        assertEquals(AllowedAttrs.Result.DENY_ALL, da.getResult());
    }

    @Test
    public void testDenyAll_allowAttr_anyAttrReturnsFalse() {
        AllowedAttrs da = AllowedAttrs.DENY_ALL_ATTRS();
        assertFalse(da.allowAttr("zimbraMailHost"));
        assertFalse(da.allowAttr("cn"));
        assertFalse(da.allowAttr("nonExistentAttr"));
        assertFalse(da.allowAttr("+zimbraMailHost"));
        assertFalse(da.allowAttr("-zimbraMailHost"));
    }

    @Test
    public void testDenyAll_getAllowed_returnsNull() {
        AllowedAttrs da = AllowedAttrs.DENY_ALL_ATTRS();
        assertNull(da.getAllowed());
    }

    // ---------------------------------------------------------------
    // ALLOW_SOME_ATTRS factory
    // ---------------------------------------------------------------

    @Test
    public void testAllowSome_resultIsAllowSome() {
        Set<String> attrs = new HashSet<String>(Arrays.asList("cn", "sn"));
        AllowedAttrs as = AllowedAttrs.ALLOW_SOME_ATTRS(attrs);
        assertEquals(AllowedAttrs.Result.ALLOW_SOME, as.getResult());
    }

    @Test
    public void testAllowSome_getAllowed_returnsProvidedSet() {
        Set<String> attrs = new HashSet<String>(Arrays.asList("cn", "sn", "zimbraMailHost"));
        AllowedAttrs as = AllowedAttrs.ALLOW_SOME_ATTRS(attrs);
        assertEquals(attrs, as.getAllowed());
    }

    @Test
    public void testAllowSome_allowAttr_presentAttr_returnsTrue() {
        Set<String> attrs = new HashSet<String>(Arrays.asList("cn", "sn"));
        AllowedAttrs as = AllowedAttrs.ALLOW_SOME_ATTRS(attrs);
        assertTrue(as.allowAttr("cn"));
        assertTrue(as.allowAttr("sn"));
    }

    @Test
    public void testAllowSome_allowAttr_absentAttr_returnsFalse() {
        Set<String> attrs = new HashSet<String>(Arrays.asList("cn", "sn"));
        AllowedAttrs as = AllowedAttrs.ALLOW_SOME_ATTRS(attrs);
        assertFalse(as.allowAttr("mail"));
        assertFalse(as.allowAttr("zimbraMailHost"));
    }

    @Test
    public void testAllowSome_allowAttr_prefixPlus_stripped() {
        // attr with '+' prefix: the actual attr name is after the '+'
        Set<String> attrs = new HashSet<String>(Arrays.asList("cn"));
        AllowedAttrs as = AllowedAttrs.ALLOW_SOME_ATTRS(attrs);
        assertTrue(as.allowAttr("+cn"));
        assertFalse(as.allowAttr("+sn"));
    }

    @Test
    public void testAllowSome_allowAttr_prefixMinus_stripped() {
        // attr with '-' prefix: the actual attr name is after the '-'
        Set<String> attrs = new HashSet<String>(Arrays.asList("sn"));
        AllowedAttrs as = AllowedAttrs.ALLOW_SOME_ATTRS(attrs);
        assertTrue(as.allowAttr("-sn"));
        assertFalse(as.allowAttr("-cn"));
    }

    @Test
    public void testAllowSome_emptySet_allowAttr_alwaysFalse() {
        AllowedAttrs as = AllowedAttrs.ALLOW_SOME_ATTRS(new HashSet<String>());
        assertFalse(as.allowAttr("cn"));
        assertFalse(as.allowAttr("sn"));
    }

    // ---------------------------------------------------------------
    // dump
    // ---------------------------------------------------------------

    @Test
    public void testDump_allowAll_containsResult() {
        String dump = AllowedAttrs.ALLOW_ALL_ATTRS().dump();
        assertNotNull(dump);
        assertTrue(dump.contains("ALLOW_ALL"));
    }

    @Test
    public void testDump_denyAll_containsResult() {
        String dump = AllowedAttrs.DENY_ALL_ATTRS().dump();
        assertNotNull(dump);
        assertTrue(dump.contains("DENY_ALL"));
    }

    @Test
    public void testDump_allowSome_containsAttrNames() {
        Set<String> attrs = new HashSet<String>(Arrays.asList("cn", "sn"));
        String dump = AllowedAttrs.ALLOW_SOME_ATTRS(attrs).dump();
        assertNotNull(dump);
        assertTrue(dump.contains("ALLOW_SOME"));
        assertTrue(dump.contains("cn"));
        assertTrue(dump.contains("sn"));
    }

    // ---------------------------------------------------------------
    // Result enum
    // ---------------------------------------------------------------

    @Test
    public void testResult_threeValues() {
        assertEquals(3, AllowedAttrs.Result.values().length);
    }

    @Test
    public void testResult_distinctValues() {
        assertNotSame(AllowedAttrs.Result.ALLOW_ALL, AllowedAttrs.Result.DENY_ALL);
        assertNotSame(AllowedAttrs.Result.ALLOW_ALL, AllowedAttrs.Result.ALLOW_SOME);
        assertNotSame(AllowedAttrs.Result.DENY_ALL,  AllowedAttrs.Result.ALLOW_SOME);
    }
}
