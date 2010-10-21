/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
public class HttpUtilTest {

    @Test
    public void encodeFilename() {
        String filename = "document.pdf";
        Assert.assertEquals("\"document.pdf\"",
                HttpUtil.encodeFilename(Browser.IE, filename));
        Assert.assertEquals("\"document.pdf\"",
                HttpUtil.encodeFilename(Browser.FIREFOX, filename));
        Assert.assertEquals("\"document.pdf\"",
                HttpUtil.encodeFilename(Browser.SAFARI, filename));
        Assert.assertEquals("\"document.pdf\"",
                HttpUtil.encodeFilename(Browser.UNKNOWN, filename));

        filename = "\u65e5\u672c\u8a9e.pdf";
        Assert.assertEquals("%E6%97%A5%E6%9C%AC%E8%AA%9E.pdf",
                HttpUtil.encodeFilename(Browser.IE, filename));
        Assert.assertEquals("\"=?utf-8?B?5pel5pys6KqeLnBkZg==?=\"",
                HttpUtil.encodeFilename(Browser.FIREFOX, filename));
        Assert.assertEquals("", HttpUtil.encodeFilename(Browser.SAFARI, filename));
        Assert.assertEquals("\"=?utf-8?B?5pel5pys6KqeLnBkZg==?=\"",
                HttpUtil.encodeFilename(Browser.UNKNOWN, filename));
    }

}
