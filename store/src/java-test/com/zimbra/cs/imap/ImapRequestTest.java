/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.nio.charset.Charset;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Charsets;

/**
 * Unit test for {@link ImapRequest}.
 *
 * @author ysasaki
 */
public final class ImapRequestTest {

    @Test
    public void readNonAsciiAstring() throws Exception {
        NioImapRequest req = new NioImapRequest(null);
        String result = "\u65e5\u672c\u8a9e";
        String raw = new String(result.getBytes("ISO-2022-JP"), Charsets.ISO_8859_1);
        req.parse("\"" + raw.replace("\\", "\\\\") + "\"\r\n");
        Assert.assertEquals(result, req.readAstring(Charset.forName("ISO-2022-JP")));
    }

}
