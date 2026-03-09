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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link CheckAttrRight}.
 *
 * The bulk of CheckAttrRight's logic requires a fully initialised LDAP
 * provisioning environment.  This test class therefore focuses on the
 * package-private {@link CheckAttrRight.CollectAttrsResult} enum, which
 * is pure in-memory and needs no infrastructure.
 */
public class CheckAttrRightTest {

    // ---------------------------------------------------------------
    // CollectAttrsResult.isAll()
    // ---------------------------------------------------------------

    @Test
    public void testCollectAttrsResult_SOME_isAll_returnsFalse() {
        assertFalse(CheckAttrRight.CollectAttrsResult.SOME.isAll());
    }

    @Test
    public void testCollectAttrsResult_ALLOW_ALL_isAll_returnsTrue() {
        assertTrue(CheckAttrRight.CollectAttrsResult.ALLOW_ALL.isAll());
    }

    @Test
    public void testCollectAttrsResult_DENY_ALL_isAll_returnsTrue() {
        assertTrue(CheckAttrRight.CollectAttrsResult.DENY_ALL.isAll());
    }

    // ---------------------------------------------------------------
    // CollectAttrsResult – enum completeness
    // ---------------------------------------------------------------

    @Test
    public void testCollectAttrsResult_values_hasThreeEntries() {
        assertEquals(3, CheckAttrRight.CollectAttrsResult.values().length);
    }

    @Test
    public void testCollectAttrsResult_names_areExpected() {
        CheckAttrRight.CollectAttrsResult[] vals = CheckAttrRight.CollectAttrsResult.values();
        assertEquals("SOME",      vals[0].name());
        assertEquals("ALLOW_ALL", vals[1].name());
        assertEquals("DENY_ALL",  vals[2].name());
    }

    // ---------------------------------------------------------------
    // CollectAttrsResult – isAll is the inverse for SOME
    // ---------------------------------------------------------------

    @Test
    public void testCollectAttrsResult_onlySOmE_isNotAll() {
        int notAllCount = 0;
        for (CheckAttrRight.CollectAttrsResult r : CheckAttrRight.CollectAttrsResult.values()) {
            if (!r.isAll()) {
                notAllCount++;
            }
        }
        // only SOME has isAll() == false
        assertEquals(1, notAllCount);
    }

    @Test
    public void testCollectAttrsResult_allAllVariants_isAll() {
        int allCount = 0;
        for (CheckAttrRight.CollectAttrsResult r : CheckAttrRight.CollectAttrsResult.values()) {
            if (r.isAll()) {
                allCount++;
            }
        }
        // ALLOW_ALL and DENY_ALL both return true
        assertEquals(2, allCount);
    }

    // ---------------------------------------------------------------
    // CollectAttrsResult – valueOf / name round-trip
    // ---------------------------------------------------------------

    @Test
    public void testCollectAttrsResult_valueOf_SOME() {
        assertEquals(CheckAttrRight.CollectAttrsResult.SOME,
                CheckAttrRight.CollectAttrsResult.valueOf("SOME"));
    }

    @Test
    public void testCollectAttrsResult_valueOf_ALLOW_ALL() {
        assertEquals(CheckAttrRight.CollectAttrsResult.ALLOW_ALL,
                CheckAttrRight.CollectAttrsResult.valueOf("ALLOW_ALL"));
    }

    @Test
    public void testCollectAttrsResult_valueOf_DENY_ALL() {
        assertEquals(CheckAttrRight.CollectAttrsResult.DENY_ALL,
                CheckAttrRight.CollectAttrsResult.valueOf("DENY_ALL"));
    }
}
