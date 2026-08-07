/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.mime;

import java.io.UnsupportedEncodingException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MimeUtilTest {

    @Test
    public void plainTextTest() throws Exception {
        String str = MimeUtil.encodeWord("Hello", "UTF-8", null, true);
        assertNotNull(str);
    }

    @Test
    public void encodingtestingWithSlashRTest() throws Exception {
        String str = MimeUtil.encodeWord("Hello, this is a test  special chars: àéîõü\r", "UTF-8", null, true);
        assertNotNull(str);
    }

    @Test
    public void encodeWordWithB() throws Exception {
        String str = MimeUtil.encodeWord("\r\nHello àéîõü", "UTF-8", "B", true);
        assertNotNull(str);
        String expectedBcode = "=?UTF-8?B?DQpIZWxsbyDDoMOpw67DtcO8?=";
        assertEquals(expectedBcode, str);
    }

    @Test
    public void encodeWithCharSetNull() throws Exception {
        String str = MimeUtil.encodeWord("\nHello àéîõü", null, "B", true);
        assertNotNull(str);
        String expected = "=?UTF-8?B?CkhlbGxvIMOgw6nDrsO1w7w=?=";
        assertEquals(expected, str);
    }

    @Test(expected = UnsupportedEncodingException.class)
    public void invalidEncodingTest() throws Exception {
        String str = MimeUtil.encodeWord("\nHello", "UTF-8", "ABC", true);
    }

    @Test
    public void encodeWordWithQ() throws Exception {
        String str = MimeUtil.encodeWord("\r\nHello àéîõü", "UTF-8", "Q", true);
        assertNotNull(str);
        String expectedBcode = "=?UTF-8?Q?=0D=0AHello_=C3=A0=C3=A9=C3=AE=C3=B5=C3=BC?=";
        assertEquals(expectedBcode, str);
    }

    @Test
    public void encodeWordQWithFalse() throws Exception {
        String str = MimeUtil.encodeWord("Hello, this is a test  special chars\n", "UTF-8", "Q", false);
        String expectedBcode = "=?UTF-8?Q?Hello,_this_is_a_test__special_chars=0A?=";
        assertEquals(expectedBcode, str);
    }
}
