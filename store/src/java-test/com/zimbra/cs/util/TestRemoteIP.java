/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.util.RemoteIP;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.MockHttpServletRequest;

public class TestRemoteIP {

    @Before
    public void setUp() {
        ZimbraLog.clearContext();
    }

    @After
    public void tearDown() {
        ZimbraLog.clearContext();
    }

    @Test
    public void testXForwardedHeaders() throws UnsupportedEncodingException, MalformedURLException {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(RemoteIP.X_ORIGINATING_IP_HEADER, "172.16.150.11");
        headers.put(RemoteIP.X_ORIGINATING_PORT_HEADER, "8080");
        headers.put(RemoteIP.X_ORIGINATING_PROTOCOL_HEADER, "IMAP");
        MockHttpServletRequest req = new MockHttpServletRequest("test".getBytes("UTF-8"), new URL(
                "http://localhost:7070/service/FooRequest"), "", 80, "192.168.1.1", headers);
        RemoteIP remoteIp = new RemoteIP(req, new RemoteIP.TrustedIPs(new String[] { "192.168.1.1" }));
        assertEquals("wrong originating IP", "172.16.150.11", remoteIp.getOrigIP());
        assertEquals("wrong originating port", "8080", remoteIp.getOrigPort().toString());
        assertEquals("wrong originating protocol", "IMAP", remoteIp.getOrigProto());
        assertEquals("wrong request IP", "172.16.150.11", remoteIp.getRequestIP());
        assertEquals("wrong request port", "8080", remoteIp.getRequestPort().toString());
        assertEquals("wrong client IP", "192.168.1.1", remoteIp.getClientIP());
        assertEquals("wrong client port", "80", remoteIp.getClientPort().toString());
    }

    @Test
    public void testNoXForwardedHeaders() throws UnsupportedEncodingException, MalformedURLException {
        HashMap<String, String> headers = new HashMap<String, String>();
        MockHttpServletRequest req = new MockHttpServletRequest("test".getBytes("UTF-8"), new URL(
                "http://localhost:7070/service/FooRequest"), "", 80, "192.168.1.1", headers);
        RemoteIP remoteIp = new RemoteIP(req, new RemoteIP.TrustedIPs(new String[] { "192.168.1.1" }));
        assertNull("originating IP should be null", remoteIp.getOrigIP());
        assertNull("originating port should be null", remoteIp.getOrigPort());
        assertNull("originating protocol should be null", remoteIp.getOrigProto());
        assertEquals("wrong request IP", "192.168.1.1", remoteIp.getRequestIP());
        assertEquals("wrong request port", "80", remoteIp.getRequestPort().toString());
        assertEquals("wrong client IP", "192.168.1.1", remoteIp.getClientIP());
        assertEquals("wrong client port", "80", remoteIp.getClientPort().toString());
    }

    @Test
    public void testNonTrustedClientIPRemoteIP() throws UnsupportedEncodingException, MalformedURLException {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(RemoteIP.X_ORIGINATING_PROTOCOL_HEADER, "IMAP");
        MockHttpServletRequest req = new MockHttpServletRequest("test".getBytes("UTF-8"), new URL(
                "http://localhost:7070/service/FooRequest"), "", 80, "10.10.1.1", headers);
        RemoteIP remoteIp = new RemoteIP(req, new RemoteIP.TrustedIPs(new String[] { "192.168.1.1" }));
        // we should ignore X-Forwarded-XXX headers from non-trusted clients
        assertNull("originating IP should be null", remoteIp.getOrigIP());
        assertNull("originating port should be null", remoteIp.getOrigPort());
        assertNull("originating protocol should be null", remoteIp.getOrigProto());
        assertEquals("wrong request IP", "10.10.1.1", remoteIp.getRequestIP());
        assertEquals("wrong request port", "80", remoteIp.getRequestPort().toString());
        assertEquals("wrong client IP", "10.10.1.1", remoteIp.getClientIP());
        assertEquals("wrong client port", "80", remoteIp.getClientPort().toString());
    }

    @Test
    public void testTrustedIPLogString() throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(RemoteIP.X_ORIGINATING_IP_HEADER, "172.16.150.11");
        headers.put(RemoteIP.X_ORIGINATING_PORT_HEADER, "8080");
        headers.put(RemoteIP.X_ORIGINATING_PROTOCOL_HEADER, "IMAP");
        MockHttpServletRequest req = new MockHttpServletRequest("test".getBytes("UTF-8"), new URL(
                "http://localhost:7070/service/FooRequest"), "", 80, "192.168.1.1", headers);
        RemoteIP remoteIp = new RemoteIP(req, new RemoteIP.TrustedIPs(new String[] { "192.168.1.1" }));
        remoteIp.addToLoggingContext();
        String updatedLogContext = ZimbraLog.getContextString();
        assertTrue(updatedLogContext.indexOf("oip=172.16.150.11") > -1);
        assertTrue(updatedLogContext.indexOf("oport=8080") > -1);
        assertTrue(updatedLogContext.indexOf("ip=172.16.150.11") > -1);
        assertTrue(updatedLogContext.indexOf("port=8080") > -1);
        assertTrue(updatedLogContext.indexOf("oproto=IMAP") > -1);
    }

    @Test
    public void testNonTrustedIPLogString() throws Exception {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.put(RemoteIP.X_ORIGINATING_IP_HEADER, "172.16.150.11");
        headers.put(RemoteIP.X_ORIGINATING_PORT_HEADER, "8080");
        headers.put(RemoteIP.X_ORIGINATING_PROTOCOL_HEADER, "IMAP");
        MockHttpServletRequest req = new MockHttpServletRequest("test".getBytes("UTF-8"), new URL(
                "http://localhost:7070/service/FooRequest"), "", 80, "10.10.1.1", headers);
        RemoteIP remoteIp = new RemoteIP(req, new RemoteIP.TrustedIPs(new String[] { "192.168.1.1" }));
        remoteIp.addToLoggingContext();
        String updatedLogContext = ZimbraLog.getContextString();
        // we should ignore X-Forwarded-XXX headers from non-trusted clients
        assertTrue(updatedLogContext.indexOf("oip=172.16.150.11") == -1);
        assertTrue(updatedLogContext.indexOf("oport=8080") == -1);
        assertTrue(updatedLogContext.indexOf("ip=10.10.1.1") > -1);
        assertTrue(updatedLogContext.indexOf("port=80") > -1);
        assertTrue(updatedLogContext.indexOf("oproto=IMAP") == -1);
    }
}
