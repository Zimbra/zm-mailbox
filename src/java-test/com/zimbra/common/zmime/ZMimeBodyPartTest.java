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
package com.zimbra.common.zmime;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.ByteUtil;

public class ZMimeBodyPartTest {

    @Test
    public void setStringContent() throws Exception {
        String content = "these are the times\nthat try men's souls";
        ZMimeBodyPart mbp = new ZMimeBodyPart();

        mbp.setContent(content, "text/plain");
        Assert.assertArrayEquals(content.getBytes(), ByteUtil.getContent(mbp.getInputStream(), -1));

        mbp.setText(content);
        Assert.assertArrayEquals(content.getBytes(), ByteUtil.getContent(mbp.getInputStream(), -1));

        mbp.setContent(content, "text/plain; charset=utf-8");
        Assert.assertArrayEquals(content.getBytes("utf-8"), ByteUtil.getContent(mbp.getInputStream(), -1));

        mbp.setText(content, "utf-8");
        Assert.assertArrayEquals(content.getBytes("utf-8"), ByteUtil.getContent(mbp.getInputStream(), -1));

        mbp.setHeader("Content-Type", "xml/x-zimbra-share");
        Assert.assertArrayEquals(content.getBytes(), ByteUtil.getContent(mbp.getInputStream(), -1));

        mbp.writeTo(System.out);
    }

    private void testEncodingSelection(String msg, String content, ZTransferEncoding cteExpected) throws Exception {
        ZMimeBodyPart mbp = new ZMimeBodyPart();
        mbp.setText(content);
        mbp.updateHeaders();
        Assert.assertEquals(msg, cteExpected.toString(), mbp.getHeader("Content-Transfer-Encoding", null));
    }

    @Test
    public void encoding() throws Exception {
        testEncodingSelection("empty content", "", ZTransferEncoding.SEVEN_BIT);
        testEncodingSelection("short content", "these are the days", ZTransferEncoding.SEVEN_BIT);
        testEncodingSelection("multiline content", "who\r\nare\r\nyou?", ZTransferEncoding.SEVEN_BIT);

        testEncodingSelection("single non-ASCII", "cyb\u00c8le illus.", ZTransferEncoding.QUOTED_PRINTABLE);
        testEncodingSelection("all non-ASCII", "\u00c8", ZTransferEncoding.BASE64);
        testEncodingSelection("single NUL", "cyb\u0000le illus.", ZTransferEncoding.QUOTED_PRINTABLE);
        testEncodingSelection("single control char", "ESCAPE ME \u001B", ZTransferEncoding.SEVEN_BIT);

        StringBuilder sb = new StringBuilder(1000);
        for (int i = 0; i < 1000; i++) {
            sb.append("X");
        }
        testEncodingSelection("line too long", sb.toString(), ZTransferEncoding.QUOTED_PRINTABLE);
    }
}
