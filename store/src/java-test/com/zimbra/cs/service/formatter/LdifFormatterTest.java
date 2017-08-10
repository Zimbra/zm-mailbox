/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import junit.framework.Assert;

public class LdifFormatterTest {

    @Test
    public void testContactLDIFFormat() throws Exception {
        try {
            List<String> fields = new ArrayList<String>();
            fields.add("email");
            fields.add("firstName");
            fields.add("fullName");
            fields.add("homeStreet");
            fields.add("mobilePhone");

            Map<String, String> contact = new HashMap<String, String>();
            contact.put("email", "user2@zimbra.com");
            contact.put("firstName", "user2");
            contact.put("fullName", "user2 test ");// test value gets base64 encoded if it contains last SPACE character
            contact.put("homeStreet", "Lane 1,\nAirport rd");// test value gets base64 encoded if it contains Non SAFE-CHAR(\n)
            contact.put("mobilePhone", "<9876543210>");// test value gets base64 encoded if it contains Non SAFE-INIT-CHAR(<)

            StringBuilder sb = new StringBuilder();
            LdifFormatter formatter = new LdifFormatter();
            formatter.toLDIFContact(fields, contact, sb);
            String expectedResult = "email: user2@zimbra.com\r\n" + "firstName: user2\r\n"
                + "fullName:: dXNlcjIgdGVzdCA=\r\n" + "homeStreet:: TGFuZSAxLApBaXJwb3J0IHJk\r\n"
                + "mobilePhone:: PDk4NzY1NDMyMTA+\r\n";
            Assert.assertEquals(expectedResult, sb.toString());
        } catch (Exception e) {
            Assert.fail("Exception should not be thrown");
        }
    }
}