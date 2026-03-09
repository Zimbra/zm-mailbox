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
 * Unit tests for {@link GranteeFlag} constants.
 *
 * Verifies that each flag has the expected bit-mask value, that all flags are
 * distinct powers of two (no overlap), and that the set of all flags can be
 * combined without collision.
 */
public class GranteeFlagTest {

    // ---------------------------------------------------------------
    // Exact bit-value assertions
    // ---------------------------------------------------------------

    @Test
    public void testF_ADMIN_value() {
        assertEquals(0x0001, GranteeFlag.F_ADMIN);
    }

    @Test
    public void testF_INDIVIDUAL_value() {
        assertEquals(0x0002, GranteeFlag.F_INDIVIDUAL);
    }

    @Test
    public void testF_GROUP_value() {
        assertEquals(0x0004, GranteeFlag.F_GROUP);
    }

    @Test
    public void testF_DOMAIN_value() {
        assertEquals(0x0008, GranteeFlag.F_DOMAIN);
    }

    @Test
    public void testF_AUTHUSER_value() {
        assertEquals(0x0010, GranteeFlag.F_AUTHUSER);
    }

    @Test
    public void testF_PUBLIC_value() {
        assertEquals(0x0020, GranteeFlag.F_PUBLIC);
    }

    @Test
    public void testF_IS_ZIMBRA_ENTRY_value() {
        assertEquals(0x0040, GranteeFlag.F_IS_ZIMBRA_ENTRY);
    }

    @Test
    public void testF_HAS_SECRET_value() {
        assertEquals(0x0080, GranteeFlag.F_HAS_SECRET);
    }

    // ---------------------------------------------------------------
    // All flags are distinct (bitwise AND of any two is zero)
    // ---------------------------------------------------------------

    @Test
    public void testAllFlagsDistinct_noOverlap() {
        short[] flags = {
            GranteeFlag.F_ADMIN,
            GranteeFlag.F_INDIVIDUAL,
            GranteeFlag.F_GROUP,
            GranteeFlag.F_DOMAIN,
            GranteeFlag.F_AUTHUSER,
            GranteeFlag.F_PUBLIC,
            GranteeFlag.F_IS_ZIMBRA_ENTRY,
            GranteeFlag.F_HAS_SECRET
        };

        for (int i = 0; i < flags.length; i++) {
            for (int j = i + 1; j < flags.length; j++) {
                int overlap = flags[i] & flags[j];
                assertEquals("Flags at index " + i + " and " + j + " overlap", 0, overlap);
            }
        }
    }

    // ---------------------------------------------------------------
    // All flags are powers of 2
    // ---------------------------------------------------------------

    @Test
    public void testAllFlags_arePowersOfTwo() {
        short[] flags = {
            GranteeFlag.F_ADMIN,
            GranteeFlag.F_INDIVIDUAL,
            GranteeFlag.F_GROUP,
            GranteeFlag.F_DOMAIN,
            GranteeFlag.F_AUTHUSER,
            GranteeFlag.F_PUBLIC,
            GranteeFlag.F_IS_ZIMBRA_ENTRY,
            GranteeFlag.F_HAS_SECRET
        };

        for (short flag : flags) {
            assertTrue("Flag 0x" + Integer.toHexString(flag) + " is not a power of two",
                    flag > 0 && (flag & (flag - 1)) == 0);
        }
    }

    // ---------------------------------------------------------------
    // Combined mask covers all bits without collision
    // ---------------------------------------------------------------

    @Test
    public void testAllFlags_combinedMask() {
        int combined = GranteeFlag.F_ADMIN
                | GranteeFlag.F_INDIVIDUAL
                | GranteeFlag.F_GROUP
                | GranteeFlag.F_DOMAIN
                | GranteeFlag.F_AUTHUSER
                | GranteeFlag.F_PUBLIC
                | GranteeFlag.F_IS_ZIMBRA_ENTRY
                | GranteeFlag.F_HAS_SECRET;

        assertEquals(0x00FF, combined);
    }
}
