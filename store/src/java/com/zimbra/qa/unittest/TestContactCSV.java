/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import com.zimbra.cs.service.formatter.ContactCSV;

/**
 * Unit test for {@link ContactCSV}.
 * Note that ContactCSV import requires /opt/zimbra/conf/zimbra-contact-fields.xml 
 *
 */
public final class TestContactCSV
extends TestCase {

    /**
     * Ensure that we autodetect the use of ";" as a separator when the fields are quoted.
     * Also ensure that a comma inside the quotes for the first field doesn't confuse things
     */
    public void testSemicolonSepWithQuotes() throws Exception {
        String hdr = "\"Junk with, comma\";\"Title\";\"First Name\";\"Last Name\";\"E-mail Address\";\"User 1\"";
        String line1 = ";;\"Di\";\"Burns\";\"di@example.test\";\"Misc comment\"";
        String line2 = ";;\"Su\";\"James\";\"su@example.test\";\"Street musician\"";
        StringReader reader = new StringReader(String.format("%s\n%s\n%s\n", hdr, line1, line2));
        List<Map<String, String>> contacts = ContactCSV.getContacts(new BufferedReader(reader), null);
        assertNotNull("getContacts return null", contacts);
        assertEquals("getContacts return list length", 2, contacts.size());
        Map<String, String> firstContact = contacts.get(0);
        if (4 != firstContact.size()) {
            for (Entry<String, String> entry : firstContact.entrySet()) {
                System.err.println(String.format("Key=%s Value=%s", entry.getKey(), entry.getValue()));
            }
        }
        assertEquals("getContacts returned first contact map size", 4, firstContact.size());
    }

    /**
     * Ensure that we autodetect the use of ";" as a separator when the header fields are not quoted.
     */
    public void testSemicolonSepNoQuotes() throws Exception {
        String hdr = "Voornaam;Achternaam;Naam;E-mailadres";
        String line1 = "Fred;Bloggs;Frederick Bloggs;fred@example.test";
        String line2 = "Blaise;Pascal;B. Pascal;blaise.pascal@example.test";
        StringReader reader = new StringReader(String.format("%s\n%s\n%s\n", hdr, line1, line2));
        List<Map<String, String>> contacts = ContactCSV.getContacts(new BufferedReader(reader), null);
        assertNotNull("getContacts return null", contacts);
        assertEquals("getContacts return list length", 2, contacts.size());
        Map<String, String> firstContact = contacts.get(0);
        if (3 != firstContact.size()) {
            for (Entry<String, String> entry : firstContact.entrySet()) {
                System.err.println(String.format("Key=%s Value=%s", entry.getKey(), entry.getValue()));
            }
        }
        assertEquals("getContacts returned first contact map size", 3, firstContact.size());
    }

    public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
    }

    public static void main(String[] args)
    throws Exception {
        // TestUtil.cliSetup();
        TestUtil.runTest(TestContactCSV.class);
    }
}
