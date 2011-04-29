/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
