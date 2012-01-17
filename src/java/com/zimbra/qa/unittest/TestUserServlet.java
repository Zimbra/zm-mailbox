/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.qa.unittest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import com.zimbra.client.ZDocument;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.tar.TarEntry;
import com.zimbra.common.util.tar.TarInputStream;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.JMSession;


public class TestUserServlet
extends TestCase {

    private static final String NAME_PREFIX = TestUserServlet.class.getSimpleName();
    private static final String USER_NAME = "user1";

    private String originalSanitizeHtml;

    @Override
    public void setUp()
    throws Exception {
        cleanUp();

        // Add a test message, in case the account is empty.
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
        originalSanitizeHtml = TestUtil.getAccountAttr(USER_NAME, Provisioning.A_zimbraNotebookSanitizeHtml);
    }

    public void testTarFormatter()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        verifyTarball(mbox, "//?fmt=tgz", true, true);
        verifyTarball(mbox, "//?fmt=tgz&body=1", true, true);
        verifyTarball(mbox, "//?fmt=tgz&body=0", true, false);
        verifyTarball(mbox, "//?fmt=tgz&meta=1", true, true);
        verifyTarball(mbox, "//?fmt=tgz&meta=0", false, true);
    }

    private void verifyTarball(ZMailbox mbox, String relativePath, boolean hasMeta, boolean hasBody)
    throws Exception {
        InputStream in = mbox.getRESTResource(relativePath);
        TarInputStream tarIn = new TarInputStream(new GZIPInputStream(in), "UTF-8");
        TarEntry entry = null;
        boolean foundMeta = false;
        boolean foundMessage = false;
        while ((entry = tarIn.getNextEntry()) != null) {
            if (entry.getName().endsWith(".meta")) {
                assertTrue("Fround " + entry.getName(), hasMeta);
                foundMeta = true;
            }
            if (entry.getName().endsWith(".eml")) {
                byte[] content = new byte[(int) entry.getSize()];
                assertEquals(content.length, tarIn.read(content));
                MimeMessage message = new ZMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content));
                byte[] body = ByteUtil.getContent(message.getInputStream(), 0);
                if (hasBody) {
                    assertTrue(entry.getName() + " has no body", body.length > 0);
                } else {
                    assertEquals(entry.getName() + " has a body", 0, body.length);
                }
                foundMessage = true;
            }
        }
        tarIn.close();
        assertTrue(foundMessage);
        if (hasMeta) {
            assertTrue(foundMeta);
        }
    }

    public void testZipFormatter()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);

        verifyZipFile(mbox, "/Inbox/?fmt=zip", true);
        verifyZipFile(mbox, "/Inbox/?fmt=zip&body=1", true);
        verifyZipFile(mbox, "/Inbox/?fmt=zip&body=0", false);
    }

    private void verifyZipFile(ZMailbox mbox, String relativePath, boolean hasBody)
    throws Exception {
        InputStream in = mbox.getRESTResource(relativePath);
        ZipInputStream zipIn = new ZipInputStream(in);
        ZipEntry entry;
        boolean foundMessage = false;
        while ((entry = zipIn.getNextEntry()) != null) {
            if (entry.getName().endsWith(".eml")) {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                ByteUtil.copy(zipIn, false, buf, true);
                byte[] content = buf.toByteArray();
                MimeMessage message = new ZMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(content));
                byte[] body = ByteUtil.getContent(message.getInputStream(), 0);
                if (hasBody) {
                    assertTrue(entry.getName() + " has no body", body.length > 0);
                } else {
                    assertEquals(entry.getName() + " has a body", 0, body.length);
                }
                foundMessage = true;
            }
        }
        zipIn.close();
        assertTrue(foundMessage);
    }

    /**
     * Verifies that the value of {@code zimbraNotebookSanitizeHtml} does not
     * affect the {@code Content-Type} header (bug 67752).
     */
    public void testSanitizeHtmlContentType() throws ServiceException, IOException {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZDocument doc = TestUtil.createDocument(mbox,
            Integer.toString(Mailbox.ID_FOLDER_BRIEFCASE), NAME_PREFIX + " testSanitizeHtmlContentType.txt",
            "text/plain", "testSanitizeHtmlContentType".getBytes());

        Account account = TestUtil.getAccount(USER_NAME);
        account.setNotebookSanitizeHtml(false);
        checkContentType(mbox, doc);
        account.setNotebookSanitizeHtml(true);
        checkContentType(mbox, doc);
    }

    private void checkContentType(ZMailbox mbox, ZDocument doc) throws ServiceException, IOException {
        URI uri = mbox.getRestURI("?id=" + doc.getId());
        HttpClient client = mbox.getHttpClient(uri);
        GetMethod get = new GetMethod(uri.toString());
        int statusCode = HttpClientUtil.executeMethod(client, get);
        get.releaseConnection();
        assertEquals(200, statusCode);
        assertEquals("text/plain", get.getResponseHeader("Content-Type").getValue());
    }

    @Override
    public void tearDown()
    throws Exception {
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraNotebookSanitizeHtml, originalSanitizeHtml);
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
