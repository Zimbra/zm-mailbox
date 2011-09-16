/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.formatter;

import java.io.BufferedReader;
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Strings;

/**
 * Unit test for {@link ContactCSV}.
 *
 * @author ysasaki
 */
public final class ContactCSVTest {

    @Test
    public void invalidFormat() throws Exception {
        StringReader reader = new StringReader(Strings.repeat("a\tb\tc", 100));
        try {
            ContactCSV.getContacts(new BufferedReader(reader), null);
            Assert.fail();
        } catch (ContactCSV.ParseException e) {
            Assert.assertEquals("invalid format", e.getMessage());
        }
    }

}
