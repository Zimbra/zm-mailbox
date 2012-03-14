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
package com.zimbra.common.util;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.HttpUtil.Browser;

/**
 * Unit test for {@link HttpUtil}.
 * 
 * @author ysasaki
 */
public final class HttpUtilTest {

    @Test
    public void encodeFilename() {
        String filename = "document.pdf";
        Assert.assertEquals("\"document.pdf\"", HttpUtil.encodeFilename(Browser.IE, filename));
        Assert.assertEquals("\"document.pdf\"", HttpUtil.encodeFilename(Browser.FIREFOX, filename));
        Assert.assertEquals("\"document.pdf\"", HttpUtil.encodeFilename(Browser.SAFARI, filename));
        Assert.assertEquals("\"document.pdf\"", HttpUtil.encodeFilename(Browser.UNKNOWN, filename));

        filename = "\u65e5\u672c\u8a9e.pdf";
        Assert.assertEquals("%E6%97%A5%E6%9C%AC%E8%AA%9E.pdf", HttpUtil.encodeFilename(Browser.IE, filename));
        Assert.assertEquals("\"=?utf-8?B?5pel5pys6KqeLnBkZg==?=\"", HttpUtil.encodeFilename(Browser.FIREFOX, filename));
        Assert.assertEquals("", HttpUtil.encodeFilename(Browser.SAFARI, filename));
        Assert.assertEquals("\"=?utf-8?B?5pel5pys6KqeLnBkZg==?=\"", HttpUtil.encodeFilename(Browser.UNKNOWN, filename));

        filename = "\u65e5 \u672c \u8a9e.pdf";
        Assert.assertEquals("%E6%97%A5%20%E6%9C%AC%20%E8%AA%9E.pdf", HttpUtil.encodeFilename(Browser.IE, filename));
        Assert.assertEquals("\"=?utf-8?B?5pelIOacrCDoqp4ucGRm?=\"", HttpUtil.encodeFilename(Browser.FIREFOX, filename));
        Assert.assertEquals("", HttpUtil.encodeFilename(Browser.SAFARI, filename));
        Assert.assertEquals("\"=?utf-8?B?5pelIOacrCDoqp4ucGRm?=\"", HttpUtil.encodeFilename(Browser.UNKNOWN, filename));
    }

    @Test
    public void testUrlEscape() {
        Assert.assertEquals("%20", HttpUtil.urlEscape(" "));
        Assert.assertEquals("%22", HttpUtil.urlEscape("\""));
        Assert.assertEquals("%23", HttpUtil.urlEscape("#"));
        Assert.assertEquals("%25", HttpUtil.urlEscape("%"));
        Assert.assertEquals("%26", HttpUtil.urlEscape("&"));
        Assert.assertEquals("%3C", HttpUtil.urlEscape("<"));
        Assert.assertEquals("%3E", HttpUtil.urlEscape(">"));
        Assert.assertEquals("%3F", HttpUtil.urlEscape("?"));
        Assert.assertEquals("%5B", HttpUtil.urlEscape("["));
        Assert.assertEquals("%5C", HttpUtil.urlEscape("\\"));
        Assert.assertEquals("%5D", HttpUtil.urlEscape("]"));
        Assert.assertEquals("%5E", HttpUtil.urlEscape("^"));
        Assert.assertEquals("%60", HttpUtil.urlEscape("`"));
        Assert.assertEquals("%7B", HttpUtil.urlEscape("{"));
        Assert.assertEquals("%7C", HttpUtil.urlEscape("|"));
        Assert.assertEquals("%7D", HttpUtil.urlEscape("}"));
        Assert.assertEquals("%2B", HttpUtil.urlEscape("+"));
    }

    @Test
    public void testUrlUnescape() {
        Assert.assertEquals("\"", HttpUtil.urlUnescape("%22"));
        Assert.assertEquals("#", HttpUtil.urlUnescape("%23"));
        Assert.assertEquals("%", HttpUtil.urlUnescape("%25"));
        Assert.assertEquals("&", HttpUtil.urlUnescape("%26"));
        Assert.assertEquals("<", HttpUtil.urlUnescape("%3C"));
        Assert.assertEquals(">", HttpUtil.urlUnescape("%3E"));
        Assert.assertEquals("?", HttpUtil.urlUnescape("%3F"));
        Assert.assertEquals("[", HttpUtil.urlUnescape("%5B"));
        Assert.assertEquals("\\", HttpUtil.urlUnescape("%5C"));
        Assert.assertEquals("]", HttpUtil.urlUnescape("%5D"));
        Assert.assertEquals("^", HttpUtil.urlUnescape("%5E"));
        Assert.assertEquals("`", HttpUtil.urlUnescape("%60"));
        Assert.assertEquals("{", HttpUtil.urlUnescape("%7B"));
        Assert.assertEquals("|", HttpUtil.urlUnescape("%7C"));
        Assert.assertEquals("}", HttpUtil.urlUnescape("%7D"));
        Assert.assertEquals("+", HttpUtil.urlUnescape("%2B"));
        Assert.assertEquals("+", HttpUtil.urlUnescape("+"));
    }
}
