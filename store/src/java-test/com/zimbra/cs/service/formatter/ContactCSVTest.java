/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
 * Note that successful imports for ContactCSV will require /opt/zimbra/conf/zimbra-contact-fields.xml
 * Better to have any tests requiring that in com.zimbra.qa.unittest.TestContactCSV
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
            Assert.assertEquals("invalid format - header field 1 of 1 is too long (length=500)", e.getMessage());
        }
    }

}
