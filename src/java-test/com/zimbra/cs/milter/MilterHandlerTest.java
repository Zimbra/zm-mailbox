/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.milter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.mime.InternetAddress;
import com.zimbra.common.mime.MimeAddressHeader;


/**
 * @author zimbra
 *
 */
public class MilterHandlerTest{
    @Test
    public void testGetToCcAddressHeaderNonAscii() throws IOException{
        String to = "To" +'\0' + "R\u00e9\u4f60\u597d < toadmin@example.com>";
        String cc = "CC" +'\0' + "\ud835\udd18\ud835\udd2b\ud835\udd26\ud835\udd20\ud835\udd2c\ud835\udd21\u4f60\u597d <ccadmin@example.com>";
        String from = "from" +'\0' + "\u00e9\u0326\u4e16\u754c <admin@example.com>";
        MimeAddressHeader mh = MilterHandler.getToCcAddressHeader(to.getBytes("utf-8"));
        Assert.assertNotNull(mh);
        Assert.assertEquals( "toadmin@example.com", mh.getAddresses().get(0).getAddress());
        mh = MilterHandler.getToCcAddressHeader(cc.getBytes("utf-8"));
        Assert.assertNotNull(mh);
        Assert.assertEquals(1, mh.getAddresses().size());
        Assert.assertEquals("ccadmin@example.com", mh.getAddresses().get(0).getAddress());
        mh = MilterHandler.getToCcAddressHeader(from.getBytes("utf-8"));
        Assert.assertNull(mh);
    }

    @Test
    public void testGetToCcAddressHeaderNonAscii1() throws IOException{
        List<String> expctedEmails = new ArrayList<String>();
        expctedEmails.add("toadmin@example.com");
        expctedEmails.add("test@example.com");
        expctedEmails.add("test2@example.com");
        String to = "To" +'\0' + "R\u00e9\u4f60\u597d <toadmin@example.com>,test@example.com,<test2@example.com>";
        MimeAddressHeader mh = MilterHandler.getToCcAddressHeader(to.getBytes("iso-8859-1"));
        Assert.assertEquals(3, mh.getAddresses().size());
        for (InternetAddress addrs : mh.getAddresses()) {
            expctedEmails.contains(addrs.getAddress());
        }
    }

    @Test
    public void testGetToCcAddressHeaderAscii() throws IOException{
        String to = "To" +'\0' + "admin@example.com; admin2@example.com";
        List<String> expctedEmails = new ArrayList<String>();
        expctedEmails.add("admin@example.com");
        expctedEmails.add("admin2@example.com");
        MimeAddressHeader mh = MilterHandler.getToCcAddressHeader(to.getBytes("iso-8859-1"));
        Assert.assertEquals(2, mh.getAddresses().size());
        for (InternetAddress addrs : mh.getAddresses()) {
            expctedEmails.contains(addrs.getAddress());
        }
        to = "To" +'\0';
        mh = MilterHandler.getToCcAddressHeader(to.getBytes("iso-8859-1"));
        Assert.assertNull(mh);
    }
    @Test
    public void testParseMicro() throws IOException {
        String to = "To" +'\0' + "admin@example.com; admin2@example.com";
        byte [] b = to.getBytes("iso-8859-1");
        IoBuffer buf = getIoBuffer(b);
        Map<String, String> address = MilterHandler.parseMacros(buf);
        Assert.assertEquals(1, address.size());
    }

    @Test
    public void testParseMicroNonAscii() throws IOException {
        String to = "To" +'\0' + "R\u00e9\u4f60\u597d <toadmin@example.com>";
        byte [] b = to.getBytes("iso-8859-1");
        IoBuffer buf = getIoBuffer(b);
        Map<String, String> address = MilterHandler.parseMacros(buf);
        Assert.assertEquals(0, address.size());
    }

    static IoBuffer getIoBuffer(byte [] b) {
        IoBuffer buf = IoBuffer.allocate(b.length, false);
        buf.put(b);
        buf.flip();
        return buf;
    }

}
