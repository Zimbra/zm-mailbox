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
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Assert;
import org.junit.Before;
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

    @Before
    public void setUp() {
        LC.zimbra_feed_manager_blacklist.setDefault("10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,fd00::/8");
        LC.zimbra_feed_manager_whitelist.setDefault("");
    }

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
    public void testIsInRangePrivateAddressesIPv4() throws Exception {
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("9.0.0.0"), "10.0.0.0/8"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("9.255.255.255"), "10.0.0.0/8"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("10.0.0.0"), "10.0.0.0/8"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("10.50.50.55"), "10.0.0.0/8"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("10.50.0.255"), "10.0.0.0/8"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("10.50.255.0"), "10.0.0.0/8"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("10.255.255.255"), "10.0.0.0/8"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("11.0.0.0"), "10.0.0.0/8"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("11.255.255.255"), "10.0.0.0/8"));

        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("172.15.255.255"), "172.16.0.0/12"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("172.15.0.0"), "172.16.0.0/12"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("172.16.0.0"), "172.16.0.0/12"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("172.16.50.50"), "172.16.0.0/12"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("172.16.0.255"), "172.16.0.0/12"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("172.16.255.0"), "172.16.0.0/12"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("172.31.255.255"), "172.16.0.0/12"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("172.32.0.0"), "172.16.0.0/12"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("172.32.255.255"), "172.16.0.0/12"));

        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("192.167.0.0"), "192.168.0.0/16"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("192.167.255.255"), "192.168.0.0/16"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("192.168.0.0"), "192.168.0.0/16"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("192.168.1.131"), "192.168.0.0/16"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("192.168.0.255"), "192.168.0.0/16"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("192.168.255.0"), "192.168.0.0/16"));
        Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("192.168.255.255"), "192.168.0.0/16"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("192.169.255.255"), "192.168.0.0/16"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("192.169.0.0"), "192.168.0.0/16"));
    }

    @Test
    public void testIsInRangeTestAddressesIPv4() throws Exception {
        for (int i = 0; i < 256; i++) {
            Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("198.51.100." + i), "198.51.100.0/24"));
        }
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("198.50.100.0"), "198.51.100.0/24"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("198.50.100.255"), "198.51.100.0/24"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("198.52.100.0"), "198.51.100.0/24"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("198.52.100.255"), "198.51.100.0/24"));

        for (int i = 0; i < 256; i++) {
            Assert.assertTrue(FeedManager.isAddressInRange(InetAddress.getByName("203.0.113." + i), "203.0.113.0/24"));
        }
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("203.0.112.0"), "203.0.113.0/24"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("203.0.112.255"), "203.0.113.0/24"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("203.0.114.0"), "203.0.113.0/24"));
        Assert.assertFalse(FeedManager.isAddressInRange(InetAddress.getByName("203.0.114.255"), "203.0.113.0/24"));
    }

    @Test
    public void testIsInRangePrivateAddressesIPv6() throws Exception {
        Assert.assertFalse(FeedManager.isAddressInRange(
            InetAddress.getByName("fedd:0d17:76f7:3e82:0000:0000:0000:0000"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertFalse(FeedManager.isAddressInRange(
            InetAddress.getByName("fedd:0d17:76f7:3e82:ffff:ffff:ffff:ffff"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertTrue(FeedManager.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:0000:0000:0000:0000"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertTrue(FeedManager.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:1111:1234:5678:abcd"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertTrue(FeedManager.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:ffff:ffff:ffff:ffff"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertFalse(FeedManager.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e83:0000:0000:0000:0000"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertFalse(FeedManager.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e83:ffff:ffff:ffff:ffff"), "fddd:0d17:76f7:3e82::/64"));

        Assert.assertFalse(FeedManager.isAddressInRange(
            InetAddress.getByName("fcdd:0d17:76f7:3e82:0000:0000:0000:0000"), "fd00::/8"));
        Assert.assertFalse(FeedManager.isAddressInRange(
            InetAddress.getByName("fcdd:0d17:76f7:3e82:ffff:ffff:ffff:ffff"), "fd00::/8"));
        Assert.assertTrue(FeedManager.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:0000:0000:0000:0000"), "fd00::/8"));
        Assert.assertTrue(FeedManager.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:1111:755f:ffff:0d17"), "fd00::/8"));
        Assert.assertTrue(FeedManager.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:ffff:ffff:ffff:ffff"), "fd00::/8"));
        Assert.assertFalse(FeedManager.isAddressInRange(
            InetAddress.getByName("fedd:0d17:76f7:3e82:0000:0000:0000:0000"), "fd00::/8"));
        Assert.assertFalse(FeedManager.isAddressInRange(
            InetAddress.getByName("fedd:0d17:76f7:3e82:ffff:ffff:ffff:ffff"), "fd00::/8"));
    }

    @Test
    public void testIsInRangeSingleAddressIPv4() throws Exception {
        Assert.assertTrue(FeedManager.isAddressInRange(
            InetAddress.getByName("192.168.1.0"), "192.168.1.0"));
        for (int i = 1; i < 256; i++) {
            Assert.assertTrue(FeedManager.isAddressInRange(
                InetAddress.getByName("192.168.1." + i), "192.168.1." + i));
            Assert.assertFalse(FeedManager.isAddressInRange(
                InetAddress.getByName("192.168.1." + i), "192.168.1.0"));
        }
    }

    @Test
    public void testIsInRangeSingleAddressIPv6() throws Exception {
        Assert.assertTrue(FeedManager.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:0000:0000:ffff:ffff"),
            "fddd:0d17:76f7:3e82:0000:0000:ffff:ffff"));
        Assert.assertTrue(FeedManager.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:1234:5678:abcd:efff"),
            "fddd:0d17:76f7:3e82:1234:5678:abcd:efff"));
        Assert.assertFalse(FeedManager.isAddressInRange(
            InetAddress.getByName("fddd:0d17:0000:3e82:0000:0000:ffff:ffff"),
            "fddd:0d17:76f7:3e82:0000:0000:ffff:0000"));
    }

    @Test
    public void testIsBlockedFeedAddressDefaultBlacklist() throws Exception {
        // loopback
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://localhost/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://localhost:8085/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://127.0.0.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://127.0.0.1:8085/feed")));

        // private
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://172.16.150.140/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://172.25.150.140/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://user:pass@192.168.5.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.5.1:8080/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.166.150/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.166.150:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://10.0.0.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://10.15.150.140/feed")));
    }

    @Test
    public void testIsBlockedFeedAddressDefaultBlacklistWithWhitelistedIp() throws Exception {
        LC.zimbra_feed_manager_whitelist.setDefault("192.168.1.106");

        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.1.106/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.1.106:8080/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://user:pass@192.168.1.106/feed")));

        // loopback
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://localhost/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://localhost:8085/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://127.0.0.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://127.0.0.1:8085/feed")));

        // private
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://172.16.150.140/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://172.25.150.140/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://user:pass@192.168.5.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.5.1:8080/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.166.150/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.166.150:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://10.0.0.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://10.15.150.140/feed")));
    }

    @Test
    public void testIsBlockedFeedAddressDefaultBlacklistWithWhitelistedRange() throws Exception {
        LC.zimbra_feed_manager_whitelist.setDefault("192.168.100.0/25");

        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.100.0/feed")));
        for (int i = 1; i < 128; i++) {
            Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.100." + i + "/feed")));
        }
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.100.128/feed")));

        // loopback
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://localhost/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://localhost:8085/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://127.0.0.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://127.0.0.1:8085/feed")));

        // private
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://172.16.150.140/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://172.25.150.140/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://user:pass@192.168.5.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.5.1:8080/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.166.150/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.166.150:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://10.0.0.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://10.15.150.140/feed")));
    }

    @Test
    public void testIsBlockedFeedAddressDefaultBlacklistWithWhitelistedMultiple() throws Exception {
        LC.zimbra_feed_manager_whitelist.setDefault("192.168.100.0/25,192.168.105.122,10.12.150.101");

        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.105.122/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://10.12.150.101/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.100.0/feed")));
        for (int i = 1; i < 128; i++) {
            Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.100." + i + "/feed")));
        }
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.100.128/feed")));

        // loopback
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://localhost/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://localhost:8085/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://127.0.0.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://127.0.0.1:8085/feed")));

        // private
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://172.16.150.140/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://172.25.150.140/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://user:pass@192.168.5.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.5.1:8080/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.166.150/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.166.150:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://10.0.0.1/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://10.15.150.140/feed")));
    }

    @Test
    public void testIsBlockedFeedAddressPublicBlacklisted() throws Exception {
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.230:8081/feed")));
        LC.zimbra_feed_manager_blacklist.setDefault("198.51.100.230");
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.230:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("https://198.51.100.230:8082/feed")));
    }

    @Test
    public void testIsBlockedFeedAddressPublicBlacklistedMultiple() throws Exception {
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.231:8081/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.220:8081/feed")));
        LC.zimbra_feed_manager_blacklist.setDefault("198.51.100.230,198.51.100.231,198.51.100.220");
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.230:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("https://198.51.100.230:8082/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.220:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.231:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://user:pass@198.51.100.231:8081/feed")));
    }

    @Test
    public void testIsBlockedFeedAddressPublicBlacklistCIDR() throws Exception {
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.130:8081/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.120:8081/feed")));
        LC.zimbra_feed_manager_blacklist.setDefault("198.51.100.0/25");
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.95:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("https://198.51.100.95:8082/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.101:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.127:8081/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.128:8081/feed")));
    }

    @Test
    public void testIsBlockedFeedAddressPublicBlacklistCIDRMultiple() throws Exception {
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://203.0.113.167:8081/feed")));
        LC.zimbra_feed_manager_blacklist.setDefault("198.51.100.0/24,203.0.113.0/24,192.0.2.121");
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://203.0.113.227:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://203.0.113.167:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://198.51.100.120:8081/feed")));
        Assert.assertTrue(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.0.2.121:8081/feed")));
    }

    @Test
    public void testIsBlockedFeedAddressPublicTest() throws Exception {
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://zimbra.test:8080/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("https://zimbra.test/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("https://user:pass@zimbra.test/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("https://user:pass@192.0.2.165/feed")));
    }

    @Test
    public void testIsBlockedFeedAddressPrivateNoBlacklist() throws Exception {
        LC.zimbra_feed_manager_blacklist.setDefault("");
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://10.15.150.140/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://172.16.150.140/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://192.168.5.1/feed")));
    }

    @Test
    public void testIsBlockedFeedAddressUnknownAddressNoBlacklist() throws Exception {
        LC.zimbra_feed_manager_blacklist.setDefault("");
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://fake.test/feed")));
        Assert.assertFalse(FeedManager.isBlockedFeedAddress(new URIBuilder("http://example.test/feed")));
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
