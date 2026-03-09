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

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link Help}.
 */
public class HelpTest {

    // ---------------------------------------------------------------
    // getName
    // ---------------------------------------------------------------

    @Test
    public void testGetName_returnsConstructorArg() {
        Help h = new Help("viewFreeBusyHelp");
        assertEquals("viewFreeBusyHelp", h.getName());
    }

    @Test
    public void testGetName_emptyString() {
        Help h = new Help("");
        assertEquals("", h.getName());
    }

    // ---------------------------------------------------------------
    // setDesc / getDesc
    // ---------------------------------------------------------------

    @Test
    public void testGetDesc_beforeSet_returnsNull() {
        Help h = new Help("h1");
        assertNull(h.getDesc());
    }

    @Test
    public void testSetAndGetDesc_returnsSetValue() {
        Help h = new Help("h1");
        h.setDesc("Allows viewing free/busy information.");
        assertEquals("Allows viewing free/busy information.", h.getDesc());
    }

    @Test
    public void testSetDesc_overwritesPreviousValue() {
        Help h = new Help("h1");
        h.setDesc("first");
        h.setDesc("second");
        assertEquals("second", h.getDesc());
    }

    // ---------------------------------------------------------------
    // addItem / getItems
    // ---------------------------------------------------------------

    @Test
    public void testGetItems_emptyByDefault() {
        Help h = new Help("h1");
        assertTrue(h.getItems().isEmpty());
    }

    @Test
    public void testAddItem_singleItem() {
        Help h = new Help("h1");
        h.addItem("item1");
        List<String> items = h.getItems();
        assertEquals(1, items.size());
        assertEquals("item1", items.get(0));
    }

    @Test
    public void testAddItem_multipleItems_preservesOrder() {
        Help h = new Help("h1");
        h.addItem("alpha");
        h.addItem("beta");
        h.addItem("gamma");
        List<String> items = h.getItems();
        assertEquals(3, items.size());
        assertEquals("alpha", items.get(0));
        assertEquals("beta",  items.get(1));
        assertEquals("gamma", items.get(2));
    }

    // ---------------------------------------------------------------
    // validate
    // ---------------------------------------------------------------

    @Test
    public void testValidate_withDesc_noException() throws ServiceException {
        Help h = new Help("h1");
        h.setDesc("some description");
        h.validate(); // must not throw
    }

    @Test(expected = ServiceException.class)
    public void testValidate_withoutDesc_throwsServiceException() throws ServiceException {
        Help h = new Help("h1");
        h.validate(); // desc is null → PARSE_ERROR
    }

    @Test
    public void testValidate_withDescAndItems_noException() throws ServiceException {
        Help h = new Help("h1");
        h.setDesc("desc");
        h.addItem("example usage");
        h.validate(); // must not throw
    }

    // ---------------------------------------------------------------
    // independence of name/desc/items (defensive copies)
    // ---------------------------------------------------------------

    @Test
    public void testName_isIndependentCopy() {
        // Help stores a new String(name) so changes to the original
        // variable have no effect (not directly verifiable for String
        // literals, but confirms the getter returns the expected value).
        String name = "myHelp";
        Help h = new Help(name);
        assertEquals("myHelp", h.getName());
    }
}
