/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.GregorianCalendar;

import javax.xml.bind.DatatypeConverter;

import junit.framework.TestCase;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;

import com.google.common.base.Strings;
import com.google.common.io.Closeables;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZFolder.View;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavServlet;

public class TestCalDav extends TestCase {

    static final boolean runningOutsideZimbra = false;  // set to true if running inside an IDE instead of from RunUnitTests
    static final TimeZoneRegistry tzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
    private static String DAV1 = "dav1";
    private static String DAV2 = "dav2";
    private static String DAV3 = "dav3";

    public static class MkColMethod extends EntityEnclosingMethod {
        @Override
        public String getName() {
            return "MKCOL";
        }
        public  MkColMethod(String uri) {
            super(uri);
        }
    }

    public static ParsedDateTime parsedDateTime(int year, int month, int day, int hour, int min,
            ICalTimeZone icalTimeZone) {
        GregorianCalendar date = new GregorianCalendar();
        date.set(java.util.Calendar.YEAR, year);
        date.set(java.util.Calendar.MONTH, month);
        date.set(java.util.Calendar.DAY_OF_MONTH, month);
        date.set(java.util.Calendar.HOUR_OF_DAY, hour);
        date.set(java.util.Calendar.MINUTE, min);
        date.set(java.util.Calendar.SECOND, 0);
        return ParsedDateTime.fromUTCTime(date.getTimeInMillis(), icalTimeZone);
    }

    public static ZProperty attendee(Account acct, ICalTok role, ICalTok cutype, ICalTok partstat) {
        ZProperty att = new ZProperty(ICalTok.ATTENDEE, "mailto:" + acct.getName());
        String displayName = acct.getDisplayName();
        if (Strings.isNullOrEmpty(displayName)) {
            displayName = acct.getName().substring(0, acct.getName().indexOf("@"));
        }
        att.addParameter(new ZParameter(ICalTok.CN, displayName));
        att.addParameter(new ZParameter(ICalTok.ROLE,role.toString()));
        att.addParameter(new ZParameter(ICalTok.CUTYPE,cutype.toString()));
        att.addParameter(new ZParameter(ICalTok.PARTSTAT,partstat.toString()));
        return att;
    }

    public static ZProperty organizer(Account acct) {
        ZProperty org = new ZProperty(ICalTok.ORGANIZER, "mailto:" + acct.getName());
        String displayName = acct.getDisplayName();
        if (Strings.isNullOrEmpty(displayName)) {
            displayName = acct.getName().substring(0, acct.getName().indexOf("@"));
        }
        org.addParameter(new ZParameter(ICalTok.CN, displayName));
        return org;
    }

    public String exampleCancelIcal(Account organizer, Account attendee1, Account attendee2) throws IOException {
        ZVCalendar vcal = new ZVCalendar();
        vcal.addVersionAndProdId();

        vcal.addProperty(new ZProperty(ICalTok.METHOD, ICalTok.CANCEL.toString()));
        ICalTimeZone tz = ICalTimeZone.lookupByTZID("Africa/Harare");
        vcal.addComponent(tz.newToVTimeZone());
        ZComponent vevent = new ZComponent(ICalTok.VEVENT);
        ParsedDateTime dtstart = parsedDateTime(2020, java.util.Calendar.APRIL, 1, 9, 0, tz);
        vevent.addProperty(dtstart.toProperty(ICalTok.DTSTART, false));
        ParsedDateTime dtend = parsedDateTime(2020, java.util.Calendar.APRIL, 1, 13, 0, tz);
        vevent.addProperty(dtend.toProperty(ICalTok.DTEND, false));
        vevent.addProperty(new ZProperty(ICalTok.DTSTAMP, "20140108T224700Z"));
        vevent.addProperty(new ZProperty(ICalTok.SUMMARY, "Meeting for fun"));
        vevent.addProperty(new ZProperty(ICalTok.UID, "d123f102-42a7-4283-b025-3376dabe53b3"));
        vevent.addProperty(new ZProperty(ICalTok.STATUS, ICalTok.CANCELLED.toString()));
        vevent.addProperty(new ZProperty(ICalTok.SEQUENCE, "1"));
        vevent.addProperty(organizer(organizer));
        vevent.addProperty(attendee(attendee1, ICalTok.REQ_PARTICIPANT, ICalTok.INDIVIDUAL, ICalTok.NEEDS_ACTION));
        vevent.addProperty(attendee(attendee2, ICalTok.REQ_PARTICIPANT, ICalTok.INDIVIDUAL, ICalTok.NEEDS_ACTION));
        vcal.addComponent(vevent);
        StringWriter calWriter = new StringWriter();
        vcal.toICalendar(calWriter);
        String icalString = calWriter.toString();
        Closeables.closeQuietly(calWriter);
        return icalString;
    }

    public static class HttpMethodExecutor {
        public int respCode;
        public int statusCode;
        public String statusLine;
        public Header[] respHeaders;
        public byte[] responseBodyBytes;

        public HttpMethodExecutor(HttpClient client, HttpMethod method, int expectedCode) throws IOException {
            try {
                respCode = HttpClientUtil.executeMethod(client, method);
                statusCode = method.getStatusCode();
                statusLine = method.getStatusLine().toString();

                respHeaders = method.getResponseHeaders();
                StringBuilder hdrsSb = new StringBuilder();
                for (Header hdr : respHeaders) {
                    hdrsSb.append(hdr.toString());
                }
                responseBodyBytes = ByteUtil.getContent(method.getResponseBodyAsStream(), -1);
                ZimbraLog.test.debug("RESPONSE:\n%s\n%s\n\n", statusLine, hdrsSb.toString(),
                        new String(responseBodyBytes));
                assertEquals("Response code", expectedCode, respCode);
                assertEquals("Status code", expectedCode, statusCode);
            } catch (IOException e) {
                ZimbraLog.test.debug("Exception thrown", e);
                fail("Unexpected Exception" + e);
                throw e;
            } finally {
                method.releaseConnection();
            }
        }
        public static HttpMethodExecutor execute(HttpClient client, HttpMethod method, int expectedCode)
                throws IOException {
            return new HttpMethodExecutor(client, method, expectedCode);
        }
    }

    public void testPostToSchedulingOutbox() throws Exception {
        Account dav1 = TestUtil.createAccount(DAV1);
        Account dav2 = TestUtil.createAccount(DAV2);
        Account dav3 = TestUtil.createAccount(DAV3);
        String url = getSchedulingOutboxUrl(dav1, dav1);
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(url);
        addBasicAuthHeaderForUser(method, dav1);
        method.addRequestHeader("Content-Type", "text/calendar");
        method.addRequestHeader("Originator", "mailto:" + dav1.getName());
        method.addRequestHeader("Recipient", "mailto:" + dav2.getName());
        method.addRequestHeader("Recipient", "mailto:" + dav3.getName());

        method.setRequestEntity(new ByteArrayRequestEntity(exampleCancelIcal(dav1, dav2, dav3).getBytes(),
                MimeConstants.CT_TEXT_CALENDAR));

        HttpMethodExecutor.execute(client, method, HttpStatus.SC_OK);
    }

    public void testBadPostToSchedulingOutbox() throws Exception {
        Account dav1 = TestUtil.createAccount(DAV1);
        Account dav2 = TestUtil.createAccount(DAV2);
        Account dav3 = TestUtil.createAccount(DAV3);
        String url = getSchedulingOutboxUrl(dav2, dav2);
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(url);
        addBasicAuthHeaderForUser(method, dav2);
        method.addRequestHeader("Content-Type", "text/calendar");
        method.addRequestHeader("Originator", "mailto:" + dav2.getName());
        method.addRequestHeader("Recipient", "mailto:" + dav3.getName());

        method.setRequestEntity(new ByteArrayRequestEntity(exampleCancelIcal(dav1, dav2, dav3).getBytes(),
                MimeConstants.CT_TEXT_CALENDAR));

        HttpMethodExecutor.execute(client, method, HttpStatus.SC_BAD_REQUEST);
    }

    public static void addBasicAuthHeaderForUser(HttpMethod method, Account acct) throws UnsupportedEncodingException {
        String basicAuthEncoding = DatatypeConverter.printBase64Binary(
                String.format("%s:%s", acct.getName(), "test123").getBytes("UTF-8"));
        method.addRequestHeader("Authorization", "Basic " + basicAuthEncoding);
    }

    public static StringBuilder getLocalServerRoot() throws ServiceException {
        Server localServer = Provisioning.getInstance().getLocalServer();
        StringBuilder sb = new StringBuilder();
        sb.append("http://localhost:").append(localServer.getIntAttr(Provisioning.A_zimbraMailPort, 0));
        return sb;
    }

    public static String getSchedulingOutboxUrl(Account auth, Account target) throws ServiceException {
        StringBuilder sb = getLocalServerRoot();
        sb.append(UrlNamespace.getSchedulingOutboxUrl(auth.getName(), target.getName()));
        return sb.toString();
    }

    public void testSimpleMkcol() throws Exception {
        Account dav1 = TestUtil.createAccount(DAV1);
        StringBuilder url = getLocalServerRoot();
        url.append(DavServlet.DAV_PATH).append("/").append(dav1.getName()).append("/simpleMkcol/");
        MkColMethod method = new MkColMethod(url.toString());
        addBasicAuthHeaderForUser(method, dav1);
        HttpClient client = new HttpClient();
        HttpMethodExecutor.execute(client, method, HttpStatus.SC_CREATED);
    }

    public void testMkcol4addressBook() throws Exception {
        String xml = "<D:mkcol xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:carddav\">" +
                "     <D:set>" +
                "       <D:prop>" +
                "         <D:resourcetype>" +
                "           <D:collection/>" +
                "           <C:addressbook/>" +
                "         </D:resourcetype>" +
                "         <D:displayname>OtherContacts</D:displayname>" +
                "         <C:addressbook-description xml:lang=\"en\">Extra Contacts</C:addressbook-description>" +
                "       </D:prop>" +
                "     </D:set>" +
                "</D:mkcol>";
        Account dav1 = TestUtil.createAccount(DAV1);
        StringBuilder url = getLocalServerRoot();
        url.append(DavServlet.DAV_PATH).append("/").append(dav1.getName()).append("/OtherContacts/");
        MkColMethod method = new MkColMethod(url.toString());
        addBasicAuthHeaderForUser(method, dav1);
        HttpClient client = new HttpClient();
        method.addRequestHeader("Content-Type", MimeConstants.CT_TEXT_XML);
        method.setRequestEntity(new ByteArrayRequestEntity(xml.getBytes(), MimeConstants.CT_TEXT_XML));
        HttpMethodExecutor.execute(client, method, HttpStatus.SC_MULTI_STATUS);

        ZMailbox.Options options = new ZMailbox.Options();
        options.setAccount(dav1.getName());
        options.setAccountBy(AccountBy.name);
        options.setPassword(TestUtil.DEFAULT_PASSWORD);
        options.setUri(TestUtil.getSoapUrl());
        options.setNoSession(true);
        ZMailbox mbox = ZMailbox.getMailbox(options);
        ZFolder folder = mbox.getFolderByPath("/OtherContacts");
        assertEquals("OtherContacts", folder.getName());
        assertEquals("OtherContacts default view", View.contact, folder.getDefaultView());
    }

    @Override
    public void setUp() throws Exception {
        if (runningOutsideZimbra) {
            TestUtil.cliSetup();
            String tzFilePath = LC.timezone_file.value();
            File tzFile = new File(tzFilePath);
            WellKnownTimeZones.loadFromFile(tzFile);
        }
        cleanUp();
    }

    public void cleanUp()
    throws Exception {
        TestUtil.deleteAccount(DAV1);
        TestUtil.deleteAccount(DAV2);
        TestUtil.deleteAccount(DAV3);
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        try {
            TestUtil.runTest(TestCalDav.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
