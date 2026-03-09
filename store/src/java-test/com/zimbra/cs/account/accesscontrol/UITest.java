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
 * Unit tests for {@link UI}.
 */
public class UITest {

    @Test
    public void testGetName_returnsConstructorArg() {
        UI ui = new UI("testUI");
        assertEquals("testUI", ui.getName());
    }

    @Test
    public void testGetDesc_default_isNull() {
        UI ui = new UI("testUI");
        assertNull(ui.getDesc());
    }

    @Test
    public void testSetDescAndGetDesc_roundTrips() {
        UI ui = new UI("testUI");
        ui.setDesc("A description");
        assertEquals("A description", ui.getDesc());
    }

    @Test
    public void testSetDesc_overwritesPreviousValue() {
        UI ui = new UI("testUI");
        ui.setDesc("first");
        ui.setDesc("second");
        assertEquals("second", ui.getDesc());
    }

    // ---------------------------------------------------------------
    // validate()
    // ---------------------------------------------------------------

    @Test
    public void testValidate_noException_whenDescIsSet() throws ServiceException {
        UI ui = new UI("testUI");
        ui.setDesc("some description");
        ui.validate(); // must not throw
    }

    @Test(expected = ServiceException.class)
    public void testValidate_throws_whenDescIsNull() throws ServiceException {
        UI ui = new UI("testUI");
        ui.validate(); // desc is null → PARSE_ERROR
    }

    // ---------------------------------------------------------------
    // compareTo()
    // ---------------------------------------------------------------

    @Test
    public void testCompareTo_alphabeticalOrdering() {
        UI alpha = new UI("alpha");
        UI beta  = new UI("beta");

        assertTrue(alpha.compareTo(beta) < 0);
        assertTrue(beta.compareTo(alpha) > 0);
    }

    @Test
    public void testCompareTo_sameNameReturnsZero() {
        UI a = new UI("sameUI");
        UI b = new UI("sameUI");
        assertEquals(0, a.compareTo(b));
    }

    @Test
    public void testCompareTo_reflexive() {
        UI ui = new UI("testUI");
        assertEquals(0, ui.compareTo(ui));
    }
}
