/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.mime;

import java.io.Reader;

import javax.mail.internet.InternetAddress;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for {@link Mime}.
 *
 * @author ysasaki
 */
public class MimeTest {

    @Test
    public void getTextReader() throws Exception {
        Reader reader = Mime.getTextReader(getClass().getResourceAsStream("zimbra-shift-jis.txt"), "text/plain", null);
        String result = IOUtils.toString(reader);
        Assert.assertTrue(result.startsWith("Zimbra Collaboration Suite\uff08ZCS\uff09\u306f\u3001Zimbra, Inc. " +
                "\u304c\u958b\u767a\u3057\u305f\u30b3\u30e9\u30dc\u30ec\u30fc\u30b7\u30e7\u30f3\u30bd\u30d5\u30c8" +
                "\u30a6\u30a7\u30a2\u88fd\u54c1\u3002"));
        Assert.assertTrue(result.endsWith("\u65e5\u672c\u3067\u306f\u4f4f\u53cb\u5546\u4e8b\u304c\u7dcf\u8ca9\u58f2" +
                "\u4ee3\u7406\u5e97\u3068\u306a\u3063\u3066\u3044\u308b\u3002"));

        // ICU4J thinks it's UTF-32 with confidence 25. We only trust if the confidence is greater than 50.
        reader = Mime.getTextReader(getClass().getResourceAsStream("p4-notification.txt"), "text/plain", null);
        result = IOUtils.toString(reader);
        Assert.assertTrue(result.startsWith("Change 259706"));
    }

    @Test
    public void parseAddressHeader() throws Exception {
        InternetAddress[] addrs = Mime.parseAddressHeader("\" <test@zimbra.com>");
        Assert.assertEquals(1, addrs.length);
        // Only verify an exception is not thrown. The new parser and the old parser don't get the same result.
    }

}
