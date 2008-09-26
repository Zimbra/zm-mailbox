/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.tar.TarEntry;
import com.zimbra.common.util.tar.TarInputStream;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.zclient.ZMailbox;


public class TestUserServlet
extends TestCase {

    private static final String NAME_PREFIX = TestUserServlet.class.getSimpleName();
    private static final String USER_NAME = "user1";
    
    public void setUp()
    throws Exception {
        cleanUp();
        
        // Add a test message, in case the account is empty.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
    }
    
    public void testTarFormatter()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        byte[] tarball = TestUtil.getRESTResource(mbox, "//?fmt=tgz");
        verifyTarball(tarball, true, true);
        tarball = TestUtil.getRESTResource(mbox, "//?fmt=tgz&body=1");
        verifyTarball(tarball, true, true);
        tarball = TestUtil.getRESTResource(mbox, "//?fmt=tgz&body=0");
        verifyTarball(tarball, true, false);
        tarball = TestUtil.getRESTResource(mbox, "//?fmt=tgz&meta=1");
        verifyTarball(tarball, true, true);
        tarball = TestUtil.getRESTResource(mbox, "//?fmt=tgz&meta=0");
        verifyTarball(tarball, false, true);
    }
    
    private void verifyTarball(byte[] data, boolean hasMeta, boolean hasBody)
    throws Exception {
        assertTrue("No data", data.length > 0);
        
        TarInputStream in = new TarInputStream(new GZIPInputStream(new ByteArrayInputStream(data)), "UTF-8");
        TarEntry entry = null;
        boolean foundMeta = false;
        boolean foundMessage = false;
        while ((entry = in.getNextEntry()) != null) {
            if (entry.getName().endsWith(".meta")) {
                assertTrue("Fround " + entry.getName(), hasMeta);
                foundMeta = true;
            }
            if (entry.getName().endsWith(".eml")) {
                byte[] content = new byte[(int) entry.getSize()];
                assertEquals(content.length, in.read(content));
                MimeMessage message = new MimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content));
                byte[] body = ByteUtil.getContent(message.getInputStream(), 0);
                if (hasBody) {
                    assertTrue(body.length > 0);
                } else {
                    assertEquals(entry.getName() + " has a body", 0, body.length);
                }
                foundMessage = true;
            }
        }
        in.close();
        assertTrue(foundMessage);
        if (hasMeta) {
            assertTrue(foundMeta);
        }
    }
    
    public void testZipFormatter()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        byte[] zipFile = TestUtil.getRESTResource(mbox, "/Inbox/?fmt=zip");
        verifyZipFile(zipFile, true);
        zipFile = TestUtil.getRESTResource(mbox, "/Inbox/?fmt=zip&body=1");
        verifyZipFile(zipFile, true);
        zipFile = TestUtil.getRESTResource(mbox, "/Inbox/?fmt=zip&body=0");
        verifyZipFile(zipFile, false);
    }
    
    private void verifyZipFile(byte[] data, boolean hasBody)
    throws Exception {
        assertTrue("No data", data.length > 0);
        
        ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(data));
        ZipEntry entry;
        boolean foundMessage = false;
        while ((entry = in.getNextEntry()) != null) {
            if (entry.getName().endsWith(".eml")) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                ByteUtil.copy(in, false, buf, true);
                byte[] content = buf.toByteArray();
                MimeMessage message = new MimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content));
                byte[] body = ByteUtil.getContent(message.getInputStream(), 0);
                if (hasBody) {
                    assertTrue(body.length > 0);
                } else {
                    assertEquals(entry.getName() + " has a body", 0, body.length);
                }
                foundMessage = true;
            }
        }
        in.close();
        assertTrue(foundMessage);
    }
    
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestUserServlet.class);
    }
}
