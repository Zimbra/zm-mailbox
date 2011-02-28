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
package com.zimbra.common.mime;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class MimeBodyPartTest {

    private void testEncodingSelection(String msg, String content, ContentTransferEncoding cteExpected) throws IOException {
        MimeBodyPart mbp = new MimeBodyPart((ContentType) null).setText(content);
        Assert.assertEquals(msg, cteExpected, mbp.getTransferEncoding());
    }

    @Test
    public void encoding() throws IOException {
        testEncodingSelection("empty content", "", ContentTransferEncoding.SEVEN_BIT);
        testEncodingSelection("short content", "these are the days", ContentTransferEncoding.SEVEN_BIT);
        testEncodingSelection("multiline content", "who\r\nare\r\nyou?", ContentTransferEncoding.SEVEN_BIT);

        testEncodingSelection("single non-ASCII", "cyb\u00c8le illus.", ContentTransferEncoding.QUOTED_PRINTABLE);
        testEncodingSelection("all non-ASCII", "\u00c8", ContentTransferEncoding.BASE64);

        StringBuilder sb = new StringBuilder(1000);
        for (int i = 0; i < 1000; i++) {
            sb.append("X");
        }
        testEncodingSelection("line too long", sb.toString(), ContentTransferEncoding.QUOTED_PRINTABLE);
    }
}
