/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 VMware, Inc.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedInputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.jetty.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.FeedManager.RemoteDataInfo;
import com.zimbra.cs.service.FeedManager.SubscriptionData;

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

    @Test
    public void socialcastAtomFeed() throws Exception {
        long lastModified = 0;
        String expectedCharset = MimeConstants.P_CHARSET_UTF8;
        BufferedInputStream content = new BufferedInputStream(getClass().getResourceAsStream("socialcastAtomFeed.xml"));
        RemoteDataInfo rdi = new RemoteDataInfo(HttpStatus.OK_200, 0, content, expectedCharset, lastModified);
        SubscriptionData<?> subsData = FeedManager.retrieveRemoteDatasource(null, rdi, null);
        List<?> subs = subsData.getItems();
        Assert.assertNotNull("List of subscriptions", subs);
        Assert.assertEquals("Number of items", 1, subs.size());
        for (Object obj : subs) {
            if (obj instanceof ParsedMessage) {
                ParsedMessage pm = (ParsedMessage) obj;
                List<MPartInfo> parts = pm.getMessageParts();
                Assert.assertEquals("Number of message parts", 1, parts.size());
                String msgContent = streamToString(parts.get(0).getMimePart().getInputStream(), Charsets.UTF_8);
                Assert.assertTrue("Text from inside <div>", msgContent.indexOf("Congratulations for passing!") > 0);
                Assert.assertTrue("Article reference",
                        msgContent.indexOf(
                                "https://pink.socialcast.com/messages/15629747-active-learner-thanks-to-cccc") > 0);
            } else {
                Assert.fail("Expecting a ParsedMessage where is " + obj.getClass().getName());
            }
        }
    }

    @Test
    public void atomEnabledOrg() throws Exception {
        long lastModified = 0;
        String expectedCharset = MimeConstants.P_CHARSET_UTF8;
        BufferedInputStream content = new BufferedInputStream(getClass().getResourceAsStream("atomEnabledOrg.xml"));
        RemoteDataInfo rdi = new RemoteDataInfo(HttpStatus.OK_200, 0, content, expectedCharset, lastModified);
        SubscriptionData<?> subsData = FeedManager.retrieveRemoteDatasource(null, rdi, null);
        List<?> subs = subsData.getItems();
        Assert.assertNotNull("List of subscriptions", subs);
        Assert.assertEquals("Number of items", 2, subs.size());
        Object obj;
        obj = subs.get(0);
        if (obj instanceof ParsedMessage) {
            ParsedMessage pm = (ParsedMessage) obj;
            List<MPartInfo> parts = pm.getMessageParts();
            Assert.assertEquals("Number of message parts", 1, parts.size());
            String msgContent = streamToString(parts.get(0).getMimePart().getInputStream(), Charsets.UTF_8);
            Assert.assertTrue("Some content text",
                    msgContent.indexOf("Rev 0.9 of the AtomAPI has just been posted") > 0);
        } else {
            Assert.fail("Expecting a ParsedMessage where is " + obj.getClass().getName());
        }
        obj = subs.get(1);
        if (obj instanceof ParsedMessage) {
            ParsedMessage pm = (ParsedMessage) obj;
            List<MPartInfo> parts = pm.getMessageParts();
            Assert.assertEquals("Number of message parts", 1, parts.size());
            String msgContent = streamToString(parts.get(0).getMimePart().getInputStream(), Charsets.UTF_8);
            Assert.assertTrue("Some content text", msgContent.indexOf("AtomAPI at ApacheCon in Las Vegas") > 0);
        } else {
            Assert.fail("Expecting a ParsedMessage where is " + obj.getClass().getName());
        }
    }

    public static String streamToString(InputStream stream, Charset cs)
    throws IOException {
        try {
            Reader reader = new BufferedReader(
                    new InputStreamReader(stream, cs));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } finally {
            stream.close();
        }
    }
}
