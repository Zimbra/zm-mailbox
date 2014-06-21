/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

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
import com.google.common.collect.Lists;
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
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavServlet;

public class TestCalDav extends TestCase {

    static final boolean runningOutsideZimbra = false;  // set to true if running inside an IDE instead of from RunUnitTests
    static final TimeZoneRegistry tzRegistry = TimeZoneRegistryFactory.getInstance().createRegistry();
    private static String NAME_PREFIX = "TestCalDav";
    private static String USER_NAME = "user1";
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

    public static class PropPatchMethod extends EntityEnclosingMethod {
        @Override
        public String getName() {
            return "PROPPATCH";
        }
        public  PropPatchMethod(String uri) {
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

        public String getResponseAsString() {
            return new String(responseBodyBytes);
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

    public static String getSchedulingInboxUrl(Account auth, Account target) throws ServiceException {
        StringBuilder sb = getLocalServerRoot();
        sb.append(UrlNamespace.getSchedulingInboxUrl(auth.getName(), target.getName()));
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

    public String makeFreeBusyRequestIcal(Account organizer, List<Account> attendees, Date start, Date end)
    throws IOException {
        ZVCalendar vcal = new ZVCalendar();
        vcal.addVersionAndProdId();

        vcal.addProperty(new ZProperty(ICalTok.METHOD, ICalTok.REQUEST.toString()));
        ZComponent vfreebusy = new ZComponent(ICalTok.VFREEBUSY);
        ParsedDateTime dtstart = ParsedDateTime.fromUTCTime(start.getTime());
        vfreebusy.addProperty(dtstart.toProperty(ICalTok.DTSTART, false));
        ParsedDateTime dtend = ParsedDateTime.fromUTCTime(end.getTime());
        vfreebusy.addProperty(dtend.toProperty(ICalTok.DTEND, false));
        vfreebusy.addProperty(new ZProperty(ICalTok.DTSTAMP, "20140108T224700Z"));
        vfreebusy.addProperty(new ZProperty(ICalTok.UID, "d123f102-42a7-4283-b025-3376dabe53b3"));
        vfreebusy.addProperty(organizer(organizer));
        for (Account attendee : attendees) {
            vfreebusy.addProperty(new ZProperty(ICalTok.ATTENDEE, "mailto:" + attendee.getName()));
        }
        vcal.addComponent(vfreebusy);
        StringWriter calWriter = new StringWriter();
        vcal.toICalendar(calWriter);
        String icalString = calWriter.toString();
        Closeables.closeQuietly(calWriter);
        return icalString;
    }

    public HttpMethodExecutor doFreeBusyCheck(Account organizer, List<Account> attendees, Date start, Date end)
    throws ServiceException, IOException {
        HttpClient client = new HttpClient();
        String outboxurl = getSchedulingOutboxUrl(organizer, organizer);
        PostMethod postMethod = new PostMethod(outboxurl);
        postMethod.addRequestHeader("Content-Type", "text/calendar");
        postMethod.addRequestHeader("Originator", "mailto:" + organizer.getName());
        for (Account attendee : attendees) {
            postMethod.addRequestHeader("Recipient", "mailto:" + attendee.getName());
        }

        addBasicAuthHeaderForUser(postMethod, organizer);
        String fbIcal = makeFreeBusyRequestIcal(organizer, attendees, start, end);
        postMethod.setRequestEntity(new ByteArrayRequestEntity(fbIcal.getBytes(), MimeConstants.CT_TEXT_CALENDAR));

        return HttpMethodExecutor.execute(client, postMethod, HttpStatus.SC_OK);
    }

    public HttpMethodExecutor doPropPatch(Account account, String url, String body)
    throws IOException {
        HttpClient client = new HttpClient();
        PropPatchMethod propPatchMethod = new PropPatchMethod(url);
        addBasicAuthHeaderForUser(propPatchMethod, account);
        propPatchMethod.addRequestHeader("Content-Type", MimeConstants.CT_TEXT_XML);
        propPatchMethod.setRequestEntity(
                new ByteArrayRequestEntity(body.getBytes(), MimeConstants.CT_TEXT_XML));
        return HttpMethodExecutor.execute(client, propPatchMethod, HttpStatus.SC_MULTI_STATUS);
    }

    /**
     * http://tools.ietf.org/html/draft-desruisseaux-caldav-sched-03#section-5.3.1
     * 5.3. Scheduling Inbox Properties
     * 5.3.1. CALDAV:calendar-free-busy-set Property
     * Purpose:  Identify the calendars that contribute to the free-busy information for the calendar user associated
     * with the scheduling Inbox.
     *
     * If the list is empty - NO calendars affect freebusy.
     * If the list is not empty, each listed calendar affects freebusy.
     * Bug 85275 - Apple Calendar specifies URLs with "@" encoded as %40 - causing us to drop all calendar from FB set
     */
    public void testPropPatchCalendarFreeBusySetSettingUsingEscapedUrls() throws Exception {
        String disableFreeBusyXml =
                "<A:propertyupdate xmlns:A=\"DAV:\">" +
                "  <A:set>" +
                "    <A:prop>" +
                "      <B:calendar-free-busy-set xmlns:B=\"urn:ietf:params:xml:ns:caldav\"/>" +
                "    </A:prop>" +
                "  </A:set>" +
                "</A:propertyupdate>";
        String enableFreeBusyTemplateXml =
                "<A:propertyupdate xmlns:A=\"DAV:\">" +
                "  <A:set>" +
                "    <A:prop>" +
                "      <B:calendar-free-busy-set xmlns:B=\"urn:ietf:params:xml:ns:caldav\">" +
                "        <A:href>/dav/%s/Tasks/</A:href>" +
                "        <A:href>/dav/%s/Calendar/</A:href>" +
                "      </B:calendar-free-busy-set>" +
                "    </A:prop>" +
                "  </A:set>" +
                "</A:propertyupdate>";

        Account dav1 = TestUtil.createAccount(DAV1);

        // Create an event in Dav1's calendar
        ZMailbox organizer = TestUtil.getZMailbox(DAV1);
        String subject = NAME_PREFIX + " testInvite request 1";
        Date startDate = new Date(System.currentTimeMillis() + Constants.MILLIS_PER_DAY);
        Date endDate = new Date(startDate.getTime() + Constants.MILLIS_PER_HOUR);
        Date fbStartDate = new Date(startDate.getTime() - (Constants.MILLIS_PER_DAY * 2));
        Date fbEndDate = new Date(endDate.getTime() + (Constants.MILLIS_PER_DAY * 3));
        String busyTentativeMarker = "FREEBUSY;FBTYPE=BUSY";

        // seed an appointment in dav1's calendar
        TestUtil.createAppointment(organizer, subject, dav1.getName(), startDate, endDate);

        String fbResponse;
        fbResponse = doFreeBusyCheck(dav1, Lists.newArrayList(dav1), fbStartDate, fbEndDate).getResponseAsString();
        assertTrue(String.format("First FB check Response [%s] should contain [%s]", fbResponse, busyTentativeMarker),
                fbResponse.contains(busyTentativeMarker));

        String inboxurl = getSchedulingInboxUrl(dav1, dav1);
        doPropPatch(dav1, inboxurl, disableFreeBusyXml);

        fbResponse = doFreeBusyCheck(dav1, Lists.newArrayList(dav1), fbStartDate, fbEndDate).getResponseAsString();
        assertFalse(String.format("2nd FB check after disabling - Response [%s] should NOT contain [%s]",
                fbResponse, busyTentativeMarker), fbResponse.contains(busyTentativeMarker));

        String enableWithRawAt = String.format(enableFreeBusyTemplateXml, dav1.getName(), dav1.getName());
        String encodedName = dav1.getName().replace("@", "%40");
        String enableWithEncodedAt = String.format(enableFreeBusyTemplateXml, encodedName, encodedName);

        doPropPatch(dav1, inboxurl, enableWithRawAt);

        fbResponse = doFreeBusyCheck(dav1, Lists.newArrayList(dav1), fbStartDate, fbEndDate).getResponseAsString();
        assertTrue(String.format("3rd FB check after enabling Response [%s] should contain [%s]",
                fbResponse, busyTentativeMarker), fbResponse.contains(busyTentativeMarker));

        doPropPatch(dav1, inboxurl, disableFreeBusyXml);

        fbResponse = doFreeBusyCheck(dav1, Lists.newArrayList(dav1), fbStartDate, fbEndDate).getResponseAsString();
        assertFalse(String.format("4th FB check after disabling - Response [%s] should NOT contain [%s]",
                fbResponse, busyTentativeMarker), fbResponse.contains(busyTentativeMarker));

        doPropPatch(dav1, inboxurl, enableWithEncodedAt);

        fbResponse = doFreeBusyCheck(dav1, Lists.newArrayList(dav1), fbStartDate, fbEndDate).getResponseAsString();
        assertTrue(String.format("4th FB check after enabling (encoded urls) Response [%s] should contain [%s]",
                fbResponse, busyTentativeMarker), fbResponse.contains(busyTentativeMarker));
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
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
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
