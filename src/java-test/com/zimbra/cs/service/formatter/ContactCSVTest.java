/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
            Assert.assertEquals("invalid format - header field 1 of 1 is too long (length=500)", e.getMessage());
        }
    }

    /**
     * Ensure that we autodetect the use of ";" as a separator when the fields are quoted.
     * Also ensure that a comma inside the quotes for the first field doesn't confuse things
     */
    @Test
    public void semicolonSepWithQuotes() throws Exception {
        String hdr = "\"Junk with, comma\";\"Title\";\"First Name\";\"Last Name\";\"E-mail Address\";\"User 1\"";
        String line1 = ";;\"Di\";\"Burns\";\"di@example.test\";\"Misc comment\"";
        String line2 = ";;\"Su\";\"James\";\"su@example.test\";\"Street musician\"";
        StringReader reader = new StringReader(String.format("%s\n%s\n%s\n", hdr, line1, line2));
        List<Map<String, String>> contacts = ContactCSV.getContacts(new BufferedReader(reader), null);
        Assert.assertNotNull("getContacts return null", contacts);
        Assert.assertEquals("getContacts return list length", 2, contacts.size());
        Map<String, String> firstContact = contacts.get(0);
        if (4 != firstContact.size()) {
            for (Entry<String, String> entry : firstContact.entrySet()) {
                System.err.println(String.format("Key=%s Value=%s", entry.getKey(), entry.getValue()));
            }
        }
        Assert.assertEquals("getContacts returned first contact map size", 4, firstContact.size());
    }

    /**
     * Ensure that we autodetect the use of ";" as a separator when the header fields are not quoted.
     */
    @Test
    public void semicolonSepNoQuotes() throws Exception {
        String hdr = "Voornaam;Achternaam;Naam;E-mailadres";
        String line1 = "Fred;Bloggs;Frederick Bloggs;fred@example.test";
        String line2 = "Blaise;Pascal;B. Pascal;blaise.pascal@example.test";
        StringReader reader = new StringReader(String.format("%s\n%s\n%s\n", hdr, line1, line2));
        List<Map<String, String>> contacts = ContactCSV.getContacts(new BufferedReader(reader), null);
        Assert.assertNotNull("getContacts return null", contacts);
        Assert.assertEquals("getContacts return list length", 2, contacts.size());
        Map<String, String> firstContact = contacts.get(0);
        if (3 != firstContact.size()) {
            for (Entry<String, String> entry : firstContact.entrySet()) {
                System.err.println(String.format("Key=%s Value=%s", entry.getKey(), entry.getValue()));
            }
        }
        Assert.assertEquals("getContacts returned first contact map size", 3, firstContact.size());
    }

}
