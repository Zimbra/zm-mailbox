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
 * Unit tests for {@link RightClass}.
 */
public class RightClassTest {

    // ---------------------------------------------------------------
    // fromString — valid values
    // ---------------------------------------------------------------

    @Test
    public void testFromString_ALL() throws ServiceException {
        assertEquals(RightClass.ALL, RightClass.fromString("ALL"));
    }

    @Test
    public void testFromString_ADMIN() throws ServiceException {
        assertEquals(RightClass.ADMIN, RightClass.fromString("ADMIN"));
    }

    @Test
    public void testFromString_USER() throws ServiceException {
        assertEquals(RightClass.USER, RightClass.fromString("USER"));
    }

    // ---------------------------------------------------------------
    // fromString — invalid values
    // ---------------------------------------------------------------

    @Test(expected = ServiceException.class)
    public void testFromString_invalid() throws ServiceException {
        RightClass.fromString("UNKNOWN");
    }

    @Test(expected = ServiceException.class)
    public void testFromString_lowercase() throws ServiceException {
        // enum valueOf is case-sensitive
        RightClass.fromString("all");
    }

    @Test(expected = ServiceException.class)
    public void testFromString_empty() throws ServiceException {
        RightClass.fromString("");
    }

    @Test(expected = ServiceException.class)
    public void testFromString_null() throws ServiceException {
        RightClass.fromString(null);
    }

    // ---------------------------------------------------------------
    // allValuesInString
    // ---------------------------------------------------------------

    @Test
    public void testAllValuesInString_commaDelimiter() {
        String result = RightClass.allValuesInString(",");
        assertTrue(result.contains("ALL"));
        assertTrue(result.contains("ADMIN"));
        assertTrue(result.contains("USER"));
        // commas separate the values
        assertTrue(result.contains(","));
    }

    @Test
    public void testAllValuesInString_pipeDelimiter() {
        String result = RightClass.allValuesInString("|");
        assertTrue(result.contains("ALL"));
        assertTrue(result.contains("ADMIN"));
        assertTrue(result.contains("USER"));
        assertTrue(result.contains("|"));
    }

    @Test
    public void testAllValuesInString_spaceDelimiter() {
        String result = RightClass.allValuesInString(" ");
        String[] parts = result.split(" ");
        assertEquals(3, parts.length);
    }

    @Test
    public void testAllValuesInString_noTrailingDelimiter() {
        String delimiter = ",";
        String result = RightClass.allValuesInString(delimiter);
        // Should not start or end with the delimiter
        assertFalse(result.startsWith(delimiter));
        assertFalse(result.endsWith(delimiter));
    }

    @Test
    public void testAllValuesInString_containsAllValues() {
        String result = RightClass.allValuesInString("-");
        for (RightClass rc : RightClass.values()) {
            assertTrue("Missing value: " + rc.name(), result.contains(rc.name()));
        }
    }

    // ---------------------------------------------------------------
    // enum values count
    // ---------------------------------------------------------------

    @Test
    public void testValues_count() {
        assertEquals(3, RightClass.values().length);
    }

    // ---------------------------------------------------------------
    // round-trip: fromString(name()) == original
    // ---------------------------------------------------------------

    @Test
    public void testFromString_roundTrip() throws ServiceException {
        for (RightClass rc : RightClass.values()) {
            assertEquals(rc, RightClass.fromString(rc.name()));
        }
    }
}
