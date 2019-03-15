/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.FeedManager.RemoteDataInfo;
import com.zimbra.cs.service.FeedManager.SubscriptionData;

public class FeedManagerTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

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

    @Test
    public void testIsBlockedFeedAddress() throws Exception {
        // loopback
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://localhost/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://localhost:8085/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://127.0.0.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://127.0.0.1:8085/feed")));

        // private
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://172.16.150.140/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://172.25.150.140/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://user:pass@192.168.5.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://192.168.5.1:8080/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://192.168.166.150/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://192.168.166.150:8081/feed")));

        // public blacklisted
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new HttpURL("http://198.51.100.230:8081/feed")));
        LC.zimbra_feed_manager_ip_blacklist.setDefault("198.51.100.230");
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://198.51.100.230:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpsURL("https://198.51.100.230:8082/feed")));

        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new HttpURL("http://198.51.100.231:8081/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new HttpURL("http://198.51.100.220:8081/feed")));
        LC.zimbra_feed_manager_ip_blacklist.setDefault("198.51.100.230,198.51.100.231,198.51.100.220");
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://198.51.100.230:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpsURL("https://198.51.100.230:8082/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://198.51.100.220:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://198.51.100.231:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new HttpURL("http://user:pass@198.51.100.231:8081/feed")));

        // public non-blacklisted
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new HttpURL("http://zimbra.test:8080/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new HttpsURL("https://zimbra.test/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new HttpsURL("https://user:pass@zimbra.test/feed")));
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
