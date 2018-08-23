/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
package com.zimbra.cs.service.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;

import junit.framework.Assert;
public class JWEUtilTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Test
    public void testJWE() {
        Map<String, String> map = new HashMap<>();
        String val1 = "jwt";
        String val2 = "encryption";
        map.put("key1", val1);
        map.put("key2", val2);
        try {
            String jwe = JWEUtil.getJWE(map);
            Map<String, String> result = JWEUtil.getDecodedJWE(jwe);
            Assert.assertEquals(val1, result.get("key1"));
            Assert.assertEquals(val2, result.get("key2"));
        } catch (ServiceException se) {
            Assert.fail("testJWE failed");
        }
    }
}
