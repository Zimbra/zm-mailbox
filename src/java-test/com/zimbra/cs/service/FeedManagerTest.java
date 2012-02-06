/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.service;

import org.junit.Assert;
import org.junit.Test;

public class FeedManagerTest {
    @Test
    public void subject() throws Exception {
        Assert.assertEquals("null", "", FeedManager.stripXML(null));
        Assert.assertEquals("no transform", "test subject", FeedManager.stripXML("test subject"));
        Assert.assertEquals("link", "test subject test", FeedManager.stripXML("test <a>subject</a> test"));
        Assert.assertEquals("embed link", "test subject", FeedManager.stripXML("test su<a>bject</a>"));
        Assert.assertEquals("bold", "test subject test", FeedManager.stripXML("test <b>subject</b> test"));
        Assert.assertEquals("break", "test subject", FeedManager.stripXML("test<br>subject"));
        Assert.assertEquals("space break", "test subject", FeedManager.stripXML("test <br>subject"));
    }
}
