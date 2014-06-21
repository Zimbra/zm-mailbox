/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.ByteArrayInputStream;
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
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.FmtType;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.parameter.XParameter;
import net.fortuna.ical4j.model.property.Attach;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;

import com.zimbra.client.ZDocument;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
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

    /**
     * Test that can import into a new calendar from ICALENDAR containing an inline ATTACHment.
     * Test that it is possible to export calendar entry with an attachment with the attachment
     * inlined if icalAttach=inline or ignoring the attachment if icalAttach=none
     */
    public void testIcsImportExport() throws IOException, ValidationException, ServiceException {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String calName = NAME_PREFIX + "2ndCalendar";
        String calUri = String.format("/%s?fmt=ics", calName);
        TestUtil.createFolder(mbox, calName, ZFolder.View.appointment);
        net.fortuna.ical4j.model.Calendar calendar = new net.fortuna.ical4j.model.Calendar();
        calendar.getProperties().add(new ProdId("-//ZimbraTest 1.0//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);
        java.util.Calendar start = java.util.Calendar.getInstance();
        start.set(java.util.Calendar.MONTH, java.util.Calendar.SEPTEMBER);
        start.set(java.util.Calendar.DAY_OF_MONTH, 03);

        VEvent wwII = new VEvent(new Date(start.getTime()), NAME_PREFIX + " Declarations of war");
        wwII.getProperties().getProperty(Property.DTSTART).getParameters().add(Value.DATE);
        wwII.getProperties().add(new Uid("3-14159"));
        Attach attach = new Attach("Attachment.\nIsn't it short.".getBytes(MimeConstants.P_CHARSET_ASCII));
        attach.getParameters().add(new XParameter("X-APPLE-FILENAME", "short.txt"));
        attach.getParameters().add(new FmtType(MimeConstants.CT_TEXT_PLAIN));
        wwII.getProperties().add(attach);
        calendar.getComponents().add(wwII);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        CalendarOutputter outputter = new CalendarOutputter();
        outputter.setValidating(false);
        outputter.output(calendar, buf);
        URI uri = mbox.getRestURI(calUri);
        HttpClient client = mbox.getHttpClient(uri);
        PostMethod post = new PostMethod(uri.toString());
        post.setRequestEntity(new InputStreamRequestEntity(new ByteArrayInputStream(buf.toByteArray()),
                MimeConstants.CT_TEXT_CALENDAR));
        ZimbraLog.test.info("testIcsImportExport:ICS to be imported:%s", new String(buf.toByteArray()));
        TestCalDav.HttpMethodExecutor.execute(client, post, HttpStatus.SC_OK);
        uri = mbox.getRestURI(calUri + "&icalAttach=inline");
        GetMethod get = new GetMethod(uri.toString());
        TestCalDav.HttpMethodExecutor executor = new TestCalDav.HttpMethodExecutor(client, get, HttpStatus.SC_OK);
        String respIcal = new String(executor.responseBodyBytes, MimeConstants.P_CHARSET_UTF8);
        ZimbraLog.test.info("testIcsImportExport:ICS exported (with icalAttach=inline):%s", respIcal);
        int attachNdx = respIcal.indexOf("ATTACH;");
        assertTrue("ATTACH should be present", -1 != attachNdx);
        String fromAttach = respIcal.substring(attachNdx);
        assertTrue("BINARY should be present", -1 != fromAttach.indexOf("VALUE=BINARY"));
        uri = mbox.getRestURI(calUri + "&icalAttach=none");
        get = new GetMethod(uri.toString());
        executor = new TestCalDav.HttpMethodExecutor(client, get, HttpStatus.SC_OK);
        respIcal = new String(executor.responseBodyBytes, MimeConstants.P_CHARSET_UTF8);
        ZimbraLog.test.info("testIcsImportExport:ICS exported (with icalAttach=none):%s", respIcal);
        assertTrue("ATTACH should be present", -1 == respIcal.indexOf("ATTACH;"));
        uri = mbox.getRestURI(calUri);
        get = new GetMethod(uri.toString());
        executor = new TestCalDav.HttpMethodExecutor(client, get, HttpStatus.SC_OK);
        respIcal = new String(executor.responseBodyBytes, MimeConstants.P_CHARSET_UTF8);
        ZimbraLog.test.info("testIcsImportExport:ICS exported (default - same as icalAttach=none):%s", respIcal);
        assertTrue("ATTACH should be present", -1 == respIcal.indexOf("ATTACH;"));
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
