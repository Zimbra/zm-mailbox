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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.bind.DatatypeConverter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import junit.framework.TestCase;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZFolder.View;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.calendar.ZCalendar;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.soap.XmlParseException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.dav.service.DavServlet;

public class TestCalDav extends TestCase {

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

    public static class PropFindMethod extends EntityEnclosingMethod {
        @Override
        public String getName() {
            return "PROPFIND";
        }
        public  PropFindMethod(String uri) {
            super(uri);
        }
    }

    public static class ReportMethod extends EntityEnclosingMethod {
        @Override
        public String getName() {
            return "REPORT";
        }
        public  ReportMethod(String uri) {
            super(uri);
        }
    }

    private static final Map<String,String> caldavNSMap;
    static {
        Map<String, String> aMap = Maps.newHashMapWithExpectedSize(2);
        aMap.put("D", DavElements.WEBDAV_NS_STRING);
        aMap.put("C", DavElements.CALDAV_NS_STRING);
        caldavNSMap = Collections.unmodifiableMap(aMap);
    }

    public static class NamespaceContextForXPath implements javax.xml.namespace.NamespaceContext {
        private final Map<String,String> nsMap;

        public NamespaceContextForXPath(Map<String,String> nsMap) {
            this.nsMap = nsMap;
        }
        public static NamespaceContextForXPath forCalDAV() {
            return new NamespaceContextForXPath(caldavNSMap);
        }

        @Override
        public String getNamespaceURI(String prefix) {
            if (prefix == null) {
                throw new NullPointerException("Null prefix");
            }

            String nsURI = nsMap.get(prefix);
            if (nsURI == null) {
                nsURI  = XMLConstants.NULL_NS_URI;
                ZimbraLog.test.info("NamespaceContextForXPath.getNamespaceURI(prefix) - Unexpected prefix %s", prefix);
            }
            return nsURI;
        }

        /**
         * Not used by XPath
         */
        @Override
        public String getPrefix(String uri) {
            throw new UnsupportedOperationException();
        }

        /**
         * Not used by XPath
         */
        @Override
        public Iterator getPrefixes(String uri) {
            throw new UnsupportedOperationException();
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
                try (InputStream responseStream = method.getResponseBodyAsStream()) {
                    if (responseStream == null) {
                        responseBodyBytes = null;
                        ZimbraLog.test.debug("RESPONSE (no content):\n%s\n%s\n\n", statusLine, hdrsSb);
                    } else {
                        responseBodyBytes = ByteUtil.getContent(responseStream, -1);
                        ZimbraLog.test.debug("RESPONSE:\n%s\n%s\n%s\n\n", statusLine, hdrsSb,
                                new String(responseBodyBytes));
                    }
                }
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

    public void testBadBasicAuth() throws Exception {
        Account dav1 = TestUtil.createAccount(DAV1);
        String calFolderUrl = getFolderUrl(dav1, "Calendar").replaceAll("@", "%40");
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod(calFolderUrl);
        addBasicAuthHeaderForUser(method, dav1, "badPassword");
        HttpMethodExecutor.execute(client, method, HttpStatus.SC_UNAUTHORIZED);
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

    public static void addBasicAuthHeaderForUser(HttpMethod method, Account acct, String password)
    throws UnsupportedEncodingException {
        String basicAuthEncoding = DatatypeConverter.printBase64Binary(
                String.format("%s:%s", acct.getName(), password).getBytes("UTF-8"));
        method.addRequestHeader("Authorization", "Basic " + basicAuthEncoding);
    }

    public static void addBasicAuthHeaderForUser(HttpMethod method, Account acct) throws UnsupportedEncodingException {
        addBasicAuthHeaderForUser(method, acct, "test123");
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

    public static String getFolderUrl(Account auth, String folderName) throws ServiceException {
        StringBuilder sb = getLocalServerRoot();
        sb.append(UrlNamespace.getFolderUrl(auth.getName(), folderName));
        return sb.toString();
    }

    static final String calendar_query_etags_by_vevent =
            "<calendar-query xmlns:D=\"DAV:\" xmlns=\"urn:ietf:params:xml:ns:caldav\">\n" +
            "  <D:prop>\n" +
            "    <D:getetag/>\n" +
            "  </D:prop>\n" +
            "  <filter>\n" +
            "    <comp-filter name=\"VCALENDAR\">\n" +
            "      <comp-filter name=\"VEVENT\"/>\n" +
            "    </comp-filter>\n" +
            "  </filter>\n" +
            "</calendar-query>";

    public static Document calendarQuery(String url, Account acct) throws IOException, XmlParseException {
        ReportMethod method = new ReportMethod(url);
        addBasicAuthHeaderForUser(method, acct);
        HttpClient client = new HttpClient();
        TestCalDav.HttpMethodExecutor executor;
        method.addRequestHeader("Content-Type", MimeConstants.CT_TEXT_XML);
        method.setRequestEntity(new ByteArrayRequestEntity(calendar_query_etags_by_vevent.getBytes(),
                MimeConstants.CT_TEXT_XML));
        executor = new TestCalDav.HttpMethodExecutor(client, method, HttpStatus.SC_MULTI_STATUS);
        String respBody = new String(executor.responseBodyBytes, MimeConstants.P_CHARSET_UTF8);
        Document doc = W3cDomUtil.parseXMLToDoc(respBody);
        org.w3c.dom.Element docElement = doc.getDocumentElement();
        assertEquals("Report node name", DavElements.P_MULTISTATUS, docElement.getLocalName());
        return doc;
    }

    /**
     * @param acct
     * @param UID - null or empty if don't care
     * @param expected - false if don't expect a matching item to be in collection within timeout time
     * @return href of first matching item found
     * @throws ServiceException
     * @throws IOException
     */
    public static String waitForItemInCalendarCollectionByUID(String url, Account acct, String UID, boolean expected,
            int timeout_millis)
    throws ServiceException, IOException {
        int orig_timeout_millis = timeout_millis;
        while (timeout_millis > 0) {
            Document doc = calendarQuery(url, acct);
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(TestCalDav.NamespaceContextForXPath.forCalDAV());
            XPathExpression xPathExpr;
            try {
                xPathExpr = xpath.compile("/D:multistatus/D:response/D:href/text()");
                NodeList result = (NodeList) xPathExpr.evaluate(doc, XPathConstants.NODESET);
                if ( 1 <= result.getLength()) {
                    for (int ndx = 0; ndx < result.getLength(); ndx++) {
                        Node item = result.item(ndx);
                        String nodeValue = item.getNodeValue();
                        if ((Strings.isNullOrEmpty(UID)) || (nodeValue.contains(UID))) {
                            if (!expected) {
                                fail(String.format(
                                        "item with UID '%s' unexpectedly arrived in collection '%s' within %d millisecs",
                                        Strings.nullToEmpty(UID), url, orig_timeout_millis - timeout_millis));

                            }
                            return nodeValue;
                        }
                    }
                }
            } catch (XPathExpressionException e1) {
                ZimbraLog.test.debug("xpath problem", e1);
            }
            try {
                if (timeout_millis > 100) {
                    Thread.sleep(100);
                    timeout_millis = timeout_millis - 100;
                } else {
                    Thread.sleep(timeout_millis);
                    timeout_millis = 0;

                }
            } catch (InterruptedException e) {
                ZimbraLog.test.debug("sleep got interrupted", e);
            }
        }
        if (expected) {
            fail(String.format("item with UID '%s' didn't arrive in collection '%s' within %d millisecs",
                    Strings.nullToEmpty(UID), url, orig_timeout_millis));
        }
        return null;

    }
    /**
     * Note: as currently we don't show replies in the scheduling inbox, this ONLY works for requests
     *
     * @param acct
     * @param UID - null or empty if don't care
     * @return href of first matching item found
     * @throws ServiceException
     * @throws IOException
     */
    public static String waitForNewSchedulingRequestByUID(Account acct, String UID)
            throws ServiceException, IOException {
        String url = getSchedulingInboxUrl(acct, acct);
        return waitForItemInCalendarCollectionByUID(url, acct, UID, true, 10000);
    }

    public void testCalendarQueryOnInbox() throws Exception {
        Account dav1 = TestUtil.createAccount(DAV1);
        String url = getSchedulingInboxUrl(dav1, dav1);
        Document doc = calendarQuery(url, dav1);
        org.w3c.dom.Element rootElem = doc.getDocumentElement();
        assertFalse("response when there are no items should have no child elements", rootElem.hasChildNodes());

        // Send an invite from user1 and check tags.
        ZMailbox organizer = TestUtil.getZMailbox(USER_NAME);
        String subject = NAME_PREFIX + " testInvite request 1";
        Date startDate = new Date(System.currentTimeMillis() + Constants.MILLIS_PER_DAY);
        Date endDate = new Date(startDate.getTime() + Constants.MILLIS_PER_HOUR);
        TestUtil.createAppointment(organizer, subject, dav1.getName(), startDate, endDate);

        waitForNewSchedulingRequestByUID(dav1, "");
        doc = calendarQuery(url, dav1);
        rootElem = doc.getDocumentElement();
        assertTrue("response should have child elements", rootElem.hasChildNodes());
    }

    public void testCalendarQueryOnOutbox() throws Exception {
        Account dav1 = TestUtil.createAccount(DAV1);
        ZMailbox dav1mbox = TestUtil.getZMailbox(USER_NAME);
        String url = getSchedulingOutboxUrl(dav1, dav1);
        Document doc = calendarQuery(url, dav1);
        org.w3c.dom.Element rootElem = doc.getDocumentElement();
        assertFalse("response when there are no items should have no child elements", rootElem.hasChildNodes());

        // Send an invite to user2 and check tags.
        ZMailbox recipient = TestUtil.getZMailbox(USER_NAME);
        String subject = NAME_PREFIX + " testInvite request 1";
        Date startDate = new Date(System.currentTimeMillis() + Constants.MILLIS_PER_DAY);
        Date endDate = new Date(startDate.getTime() + Constants.MILLIS_PER_HOUR);
        TestUtil.createAppointment(dav1mbox, subject, recipient.getName(), startDate, endDate);

        doc = calendarQuery(url, dav1);
        rootElem = doc.getDocumentElement();
        // We didn't support this report when we only offered caldav-schedule and caldav-auto-schedule
        // doesn't require calendar-query support on the outbox - so we just treat it as always empty.
        assertFalse("response for items in outbox should have no child elements, even though we sent an invite",
        rootElem.hasChildNodes());
    }

    public void testPropFindSupportedReportSetOnInbox() throws Exception {
        Account user1 = TestUtil.getAccount(USER_NAME);
        checkPropFindSupportedReportSet(user1, getSchedulingInboxUrl(user1, user1),
                UrlNamespace.getSchedulingInboxUrl(user1.getName(), user1.getName()));
    }

    public void testPropFindSupportedReportSetOnOutbox() throws Exception {
        Account user1 = TestUtil.getAccount(USER_NAME);
        checkPropFindSupportedReportSet(user1, getSchedulingOutboxUrl(user1, user1),
                UrlNamespace.getSchedulingOutboxUrl(user1.getName(), user1.getName()));
    }

    String propFindSupportedReportSet =
            "<x0:propfind xmlns:x0=\"DAV:\" xmlns:x1=\"urn:ietf:params:xml:ns:caldav\">\n" +
            "  <x0:prop>\n" +
            "    <x0:supported-report-set/>\n" +
            "  </x0:prop>\n" +
            "</x0:propfind>";
    public void checkPropFindSupportedReportSet(Account user, String fullurl, String shorturl) throws Exception {
        PropFindMethod method = new PropFindMethod(fullurl);
        addBasicAuthHeaderForUser(method, user);
        HttpClient client = new HttpClient();
        TestCalDav.HttpMethodExecutor executor;
        String respBody;
        Element respElem;
        method.addRequestHeader("Content-Type", MimeConstants.CT_TEXT_XML);
        method.setRequestEntity(new ByteArrayRequestEntity(propFindSupportedReportSet.getBytes(), MimeConstants.CT_TEXT_XML));
        executor = new TestCalDav.HttpMethodExecutor(client, method, HttpStatus.SC_MULTI_STATUS);
        respBody = new String(executor.responseBodyBytes, MimeConstants.P_CHARSET_UTF8);
        respElem = Element.XMLElement.parseXML(respBody);
        assertEquals("name of top element in response", DavElements.P_MULTISTATUS, respElem.getName());
        assertTrue("top element response should have child elements", respElem.hasChildren());
        checkSupportedReportSet(respElem, shorturl);
    }

    private void checkSupportedReportSet(Element respElem, String shorturl) {
        boolean supportsCalendarQuery = false;
        for (Element r : respElem.listElements()) {
            assertEquals("name of sub-element in response", DavElements.P_RESPONSE, r.getName());
            for (Element respEntry : r.listElements()) {
                if (DavElements.P_HREF.equals(respEntry.getName())) {
                    String hrefText = respEntry.getText();
                    // assertTrue(hrefText + " should end with /Inbox/", hrefText.endsWith("/Inbox/"));
                    // assertTrue(hrefText + " should start with /dav/", hrefText.startsWith("/dav/"));
                    assertEquals("HREF", shorturl, hrefText);
                } else if (DavElements.P_PROPSTAT.equals(respEntry.getName())) {
                    for (Element psEntry : respEntry.listElements()) {
                        if (DavElements.P_STATUS.equals(psEntry.getName())) {
                            assertEquals("propstat/status", "HTTP/1.1 200 OK", psEntry.getText());
                        } else if (DavElements.P_PROP.equals(psEntry.getName())) {
                            for (Element propEntry : psEntry.listElements()) {
                                if (DavElements.P_SUPPORTED_REPORT_SET.equals(propEntry.getName())) {
                                    for (Element suppRepSetEntry : propEntry.listElements()) {
                                        assertEquals("supported-report-set child",
                                                DavElements.P_SUPPORTED_REPORT, suppRepSetEntry.getName());
                                        for (Element suppRepEntry : suppRepSetEntry.listElements()) {
                                            assertEquals("supported-report child",
                                                    DavElements.P_REPORT, suppRepEntry.getName());
                                            for (Element repEntry : suppRepEntry.listElements()) {
                                                if (DavElements.E_CALENDAR_QUERY.equals(repEntry.getQName())) {
                                                    supportsCalendarQuery = true;
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    fail("Unexpected element '" + propEntry.getName() + "' under " + DavElements.P_PROP);
                                }
                            }
                        } else {
                            fail("Unexpected element '" + psEntry.getName() + "' under " + DavElements.P_PROPSTAT);
                        }
                    }
                } else {
                    fail("Unexpected element '" + respEntry.getName() + "' under " + DavElements.P_RESPONSE);
                }
            }
        }
        assertTrue("calendar-query report should be advertised", supportsCalendarQuery);
    }

    final String propFindSupportedCalendarComponentSet =
            "<D:propfind xmlns:D=\"DAV:\" xmlns:C=\"urn:ietf:params:xml:ns:caldav\">\n" +
            "  <D:prop>\n" +
            "     <C:supported-calendar-component-set/>\n" +
            "  </D:prop>\n" +
            "</D:propfind>";
    public void checkPropFindSupportedCalendarComponentSet(Account user, String fullurl, String shorturl,
            String[] compNames)
    throws Exception {
        PropFindMethod method = new PropFindMethod(fullurl);
        addBasicAuthHeaderForUser(method, user);
        HttpClient client = new HttpClient();
        TestCalDav.HttpMethodExecutor executor;
        String respBody;
        method.addRequestHeader("Content-Type", MimeConstants.CT_TEXT_XML);
        method.setRequestEntity(new ByteArrayRequestEntity(propFindSupportedCalendarComponentSet.getBytes(),
                MimeConstants.CT_TEXT_XML));
        executor = new TestCalDav.HttpMethodExecutor(client, method, HttpStatus.SC_MULTI_STATUS);
        respBody = new String(executor.responseBodyBytes, MimeConstants.P_CHARSET_UTF8);
        Document doc = W3cDomUtil.parseXMLToDoc(respBody);
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(TestCalDav.NamespaceContextForXPath.forCalDAV());
        XPathExpression xPathExpr;
        String text;
        NodeList result;
        xPathExpr = xpath.compile("/D:multistatus/D:response/D:href/text()");
        result = (NodeList) xPathExpr.evaluate(doc, XPathConstants.NODESET);
        text = (String) xPathExpr.evaluate(doc, XPathConstants.STRING);
        assertEquals(String.format("href '%s' should be same as '%s'", text, shorturl), shorturl, text);
        xPathExpr = xpath.compile("/D:multistatus/D:response/D:propstat/D:status/text()");
        text = (String) xPathExpr.evaluate(doc, XPathConstants.STRING);
        assertEquals("status", "HTTP/1.1 200 OK", text);
        xPathExpr = xpath.compile("/D:multistatus/D:response/D:propstat/D:prop/C:supported-calendar-component-set/C:comp");
        result = (NodeList) xPathExpr.evaluate(doc, XPathConstants.NODESET);
        assertEquals("Number of comp nodes under supported-calendar-component-set", compNames.length, result.getLength());
        List<String> names = Arrays.asList(compNames);
        for (int ndx = 0; ndx < result.getLength(); ndx++) {
            org.w3c.dom.Element child = (org.w3c.dom.Element) result.item(ndx);
            String name = child.getAttribute("name");
            assertNotNull("comp should have a 'name' attribute", name);
            assertTrue(String.format("comp 'name' attribute '%s' should be one of the expected names", name),
                    names.contains(name));
        }
    }

    private final String[] componentsForBothTasksAndEvents = {"VEVENT", "VTODO", "VFREEBUSY"};
    private final String[] eventComponents = {"VEVENT", "VFREEBUSY"};
    private final String[] todoComponents = {"VTODO", "VFREEBUSY"};
    public void testPropFindSupportedCalendarComponentSetOnInbox() throws Exception {
        Account user1 = TestUtil.getAccount(USER_NAME);
        checkPropFindSupportedCalendarComponentSet(user1, getSchedulingInboxUrl(user1, user1),
                UrlNamespace.getSchedulingInboxUrl(user1.getName(), user1.getName()), componentsForBothTasksAndEvents);
    }

    public void testPropFindSupportedCalendarComponentSetOnOutbox() throws Exception {
        Account user1 = TestUtil.getAccount(USER_NAME);
        checkPropFindSupportedCalendarComponentSet(user1, getSchedulingOutboxUrl(user1, user1),
                UrlNamespace.getSchedulingOutboxUrl(user1.getName(), user1.getName()), componentsForBothTasksAndEvents);
    }

    public void testPropFindSupportedCalendarComponentSetOnCalendar() throws Exception {
        Account user1 = TestUtil.getAccount(USER_NAME);
        checkPropFindSupportedCalendarComponentSet(user1, getFolderUrl(user1, "Calendar"),
                UrlNamespace.getFolderUrl(user1.getName(), "Calendar"), eventComponents);
    }

    public void testPropFindSupportedCalendarComponentSetOnTasks() throws Exception {
        Account user1 = TestUtil.getAccount(USER_NAME);
        checkPropFindSupportedCalendarComponentSet(user1, getFolderUrl(user1, "Tasks"),
                UrlNamespace.getFolderUrl(user1.getName(), "Tasks"), todoComponents);
    }

    /**
     *  dav - sending http error 302 because: wrong url - redirecting to:
     *  http://pan.local:7070/dav/dav1@pan.local/Calendar/d123f102-42a7-4283-b025-3376dabe53b3.ics
     *  com.zimbra.cs.dav.DavException: wrong url - redirecting to:
     *  http://pan.local:7070/dav/dav1@pan.local/Calendar/d123f102-42a7-4283-b025-3376dabe53b3.ics
     *      at com.zimbra.cs.dav.resource.CalendarCollection.createItem(CalendarCollection.java:431)
     *      at com.zimbra.cs.dav.service.method.Put.handle(Put.java:49)
     *      at com.zimbra.cs.dav.service.DavServlet.service(DavServlet.java:322)
     */
    public void testCreateUsingClientChosenName() throws ServiceException, IOException {
        Account dav1 = TestUtil.createAccount(DAV1);
        String davBaseName = "clientInvented.now";
        String calFolderUrl = getFolderUrl(dav1, "Calendar");
        String url = String.format("%s%s", calFolderUrl, davBaseName);
        HttpClient client = new HttpClient();
        PutMethod putMethod = new PutMethod(url);
        addBasicAuthHeaderForUser(putMethod, dav1);
        putMethod.addRequestHeader("Content-Type", "text/calendar");

        putMethod.setRequestEntity(new ByteArrayRequestEntity(simpleEvent(dav1).getBytes(),
                MimeConstants.CT_TEXT_CALENDAR));
        if (DebugConfig.enableDAVclientCanChooseResourceBaseName) {
            HttpMethodExecutor.execute(client, putMethod, HttpStatus.SC_CREATED);
        } else {
            HttpMethodExecutor.execute(client, putMethod, HttpStatus.SC_MOVED_TEMPORARILY);
            // Not testing much in this mode but...
            return;
        }

        GetMethod getMethod = new GetMethod(url);
        addBasicAuthHeaderForUser(getMethod, dav1);
        HttpMethodExecutor.execute(client, getMethod, HttpStatus.SC_OK);

        PropFindMethod propFindMethod = new PropFindMethod(getFolderUrl(dav1, "Calendar"));
        addBasicAuthHeaderForUser(propFindMethod, dav1);
        TestCalDav.HttpMethodExecutor executor;
        String respBody;
        Element respElem;
        propFindMethod.addRequestHeader("Content-Type", MimeConstants.CT_TEXT_XML);
        propFindMethod.addRequestHeader("Depth", "1");
        propFindMethod.setRequestEntity(new ByteArrayRequestEntity(propFindEtagResType.getBytes(),
                MimeConstants.CT_TEXT_XML));
        executor = new TestCalDav.HttpMethodExecutor(client, propFindMethod, HttpStatus.SC_MULTI_STATUS);
        respBody = new String(executor.responseBodyBytes, MimeConstants.P_CHARSET_UTF8);
        respElem = Element.XMLElement.parseXML(respBody);
        assertEquals("name of top element in propfind response", DavElements.P_MULTISTATUS, respElem.getName());
        assertTrue("propfind response should have child elements", respElem.hasChildren());
        Iterator<Element> iter = respElem.elementIterator();
        boolean hasCalendarHref = false;
        boolean hasCalItemHref = false;
        while (iter.hasNext()) {
            Element child = iter.next();
            if (DavElements.P_RESPONSE.equals(child.getName())) {
                Iterator<Element> hrefIter = child.elementIterator(DavElements.P_HREF);
                while (hrefIter.hasNext()) {
                    Element href = hrefIter.next();
                    calFolderUrl.endsWith(href.getText());
                    hasCalendarHref = hasCalendarHref || calFolderUrl.endsWith(href.getText());
                    hasCalItemHref = hasCalItemHref || url.endsWith(href.getText());
                }
            }
        }
        assertTrue("propfind response contained entry for calendar", hasCalendarHref);
        assertTrue("propfind response contained entry for calendar entry ", hasCalItemHref);
        DeleteMethod deleteMethod = new DeleteMethod(url);
        addBasicAuthHeaderForUser(deleteMethod, dav1);
        HttpMethodExecutor.execute(client, deleteMethod, HttpStatus.SC_NO_CONTENT);
    }

    private static String androidSeriesMeetingTemplate =
            "BEGIN:VCALENDAR\n" +
            "VERSION:2.0\n" +
            "PRODID:-//dmfs.org//mimedir.icalendar//EN\n" +
            "BEGIN:VTIMEZONE\n" +
            "TZID:Europe/London\n" +
            "X-LIC-LOCATION:Europe/London\n" +
            "BEGIN:DAYLIGHT\n" +
            "TZOFFSETFROM:+0000\n" +
            "TZOFFSETTO:+0100\n" +
            "TZNAME:BST\n" +
            "DTSTART:19700329T010000\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\n" +
            "END:DAYLIGHT\n" +
            "BEGIN:STANDARD\n" +
            "TZOFFSETFROM:+0100\n" +
            "TZOFFSETTO:+0000\n" +
            "TZNAME:GMT\n" +
            "DTSTART:19701025T020000\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\n" +
            "END:STANDARD\n" +
            "END:VTIMEZONE\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART;TZID=Europe/London:20141022T190000\n" +
            "DESCRIPTION:Giggle\n" +
            "SUMMARY:testAndroidMeetingSeries\n" +
            "RRULE:FREQ=DAILY;COUNT=15;WKST=MO\n" +
            "LOCATION:Egham Leisure Centre\\, Vicarage Road\\, Egham\\, United Kingdom\n" +
            "TRANSP:OPAQUE\n" +
            "STATUS:CONFIRMED\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED;RSVP=TRUE;ROLE=REQ-PARTICIPANT:mailto:%%ORG%%\n" +
            "ATTENDEE;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;ROLE=REQ-PARTICIPANT:mailto:%%ATT%%\n" +
            "DURATION:PT1H\n" +
            "LAST-MODIFIED:20141021T145905Z\n" +
            "DTSTAMP:20141021T145905Z\n" +
            "ORGANIZER:mailto:%%ORG%%\n" +
            "CREATED:20141021T145905Z\n" +
            "UID:%%UID%%\n" +
            "BEGIN:VALARM\n" +
            "TRIGGER;VALUE=DURATION:-PT15M\n" +
            "ACTION:DISPLAY\n" +
            "DESCRIPTION:Default Event Notification\n" +
            "X-WR-ALARMUID:790cd474-6135-4705-b1a0-24d4b4fc3cf5\n" +
            "END:VALARM\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n";
            public String androidSeriesMeetingUid = "6db50587-d283-49a1-9cf4-63aa27406829";
    public void testAndroidMeetingSeries() throws Exception {
        Account dav1 = TestUtil.createAccount(DAV1);
        Account dav2 = TestUtil.createAccount(DAV2);
        String calFolderUrl = getFolderUrl(dav1, "Calendar");
        String url = String.format("%s%s.ics", calFolderUrl, androidSeriesMeetingUid);
        HttpClient client = new HttpClient();
        PutMethod putMethod = new PutMethod(url);
        addBasicAuthHeaderForUser(putMethod, dav1);
        putMethod.addRequestHeader("Content-Type", "text/calendar");

        String body = androidSeriesMeetingTemplate.replace("%%ORG%%", dav1.getName())
                .replace("%%ATT%%", dav2.getName())
                .replace("%%UID%%", androidSeriesMeetingUid);
        putMethod.setRequestEntity(new ByteArrayRequestEntity(body.getBytes(),
                MimeConstants.CT_TEXT_CALENDAR));
        HttpMethodExecutor.execute(client, putMethod, HttpStatus.SC_CREATED);

        String inboxhref = TestCalDav.waitForNewSchedulingRequestByUID(dav2, androidSeriesMeetingUid);
        assertTrue("Found meeting request for newly created item", inboxhref.contains(androidSeriesMeetingUid));

        GetMethod getMethod = new GetMethod(url);
        addBasicAuthHeaderForUser(getMethod, dav1);
        HttpMethodExecutor.execute(client, getMethod, HttpStatus.SC_OK);

        PropFindMethod propFindMethod = new PropFindMethod(getFolderUrl(dav1, "Calendar"));
        addBasicAuthHeaderForUser(propFindMethod, dav1);
        TestCalDav.HttpMethodExecutor executor;
        String respBody;
        Element respElem;
        propFindMethod.addRequestHeader("Content-Type", MimeConstants.CT_TEXT_XML);
        propFindMethod.addRequestHeader("Depth", "1");
        propFindMethod.setRequestEntity(new ByteArrayRequestEntity(propFindEtagResType.getBytes(),
                MimeConstants.CT_TEXT_XML));
        executor = new TestCalDav.HttpMethodExecutor(client, propFindMethod, HttpStatus.SC_MULTI_STATUS);
        respBody = new String(executor.responseBodyBytes, MimeConstants.P_CHARSET_UTF8);
        respElem = Element.XMLElement.parseXML(respBody);
        assertEquals("name of top element in propfind response", DavElements.P_MULTISTATUS, respElem.getName());
        assertTrue("propfind response should have child elements", respElem.hasChildren());
        Iterator<Element> iter = respElem.elementIterator();
        boolean hasCalendarHref = false;
        boolean hasCalItemHref = false;
        while (iter.hasNext()) {
            Element child = iter.next();
            if (DavElements.P_RESPONSE.equals(child.getName())) {
                Iterator<Element> hrefIter = child.elementIterator(DavElements.P_HREF);
                while (hrefIter.hasNext()) {
                    Element href = hrefIter.next();
                    calFolderUrl.endsWith(href.getText());
                    hasCalendarHref = hasCalendarHref || calFolderUrl.endsWith(href.getText());
                    hasCalItemHref = hasCalItemHref || url.endsWith(href.getText());
                }
            }
        }
        assertTrue("propfind response contained entry for calendar", hasCalendarHref);
        assertTrue("propfind response contained entry for calendar entry ", hasCalItemHref);

        DeleteMethod deleteMethod = new DeleteMethod(url);
        addBasicAuthHeaderForUser(deleteMethod, dav1);
        HttpMethodExecutor.execute(client, deleteMethod, HttpStatus.SC_NO_CONTENT);
    }

    static String propFindEtagResType = "<x0:propfind xmlns:x0=\"DAV:\">" +
                               " <x0:prop>" +
                               "  <x0:getetag/>" +
                               "  <x0:resourcetype/>" +
                               " </x0:prop>" +
                               "</x0:propfind>";

    public String simpleEvent(Account organizer) throws IOException {
        ZVCalendar vcal = new ZVCalendar();
        vcal.addVersionAndProdId();

        vcal.addProperty(new ZProperty(ICalTok.METHOD, ICalTok.PUBLISH.toString()));
        ICalTimeZone tz = ICalTimeZone.lookupByTZID("Africa/Harare");
        vcal.addComponent(tz.newToVTimeZone());
        ZComponent vevent = new ZComponent(ICalTok.VEVENT);
        ParsedDateTime dtstart = parsedDateTime(2020, java.util.Calendar.APRIL, 1, 9, 0, tz);
        vevent.addProperty(dtstart.toProperty(ICalTok.DTSTART, false));
        ParsedDateTime dtend = parsedDateTime(2020, java.util.Calendar.APRIL, 1, 13, 0, tz);
        vevent.addProperty(dtend.toProperty(ICalTok.DTEND, false));
        vevent.addProperty(new ZProperty(ICalTok.DTSTAMP, "20140108T224700Z"));
        vevent.addProperty(new ZProperty(ICalTok.SUMMARY, "Simple Event"));
        vevent.addProperty(new ZProperty(ICalTok.UID, "d1102-42a7-4283-b025-3376dabe53b3"));
        vevent.addProperty(new ZProperty(ICalTok.STATUS, ICalTok.CONFIRMED.toString()));
        vevent.addProperty(new ZProperty(ICalTok.SEQUENCE, "1"));
        // vevent.addProperty(organizer(organizer));
        vcal.addComponent(vevent);
        StringWriter calWriter = new StringWriter();
        vcal.toICalendar(calWriter);
        String icalString = calWriter.toString();
        Closeables.closeQuietly(calWriter);
        return icalString;
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

    private static String VtimeZoneGMT_0600_0500 =
            "BEGIN:VCALENDAR\n" +
            "BEGIN:VTIMEZONE\n" +
            "TZID:GMT-06.00/-05.00\n" +
            "BEGIN:STANDARD\n" +
            "DTSTART:16010101T010000\n" +
            "TZOFFSETTO:-0600\n" +
            "TZOFFSETFROM:-0500\n" +
            "RRULE:FREQ=YEARLY;INTERVAL=1;BYMONTH=11;BYDAY=1SU;WKST=MO\n" +
            "END:STANDARD\n" +
            "BEGIN:DAYLIGHT\n" +
            "DTSTART:16010101T030000\n" +
            "TZOFFSETTO:-0500\n" +
            "TZOFFSETFROM:-0600\n" +
            "RRULE:FREQ=YEARLY;INTERVAL=1;BYMONTH=3;BYDAY=2SU;WKST=MO\n" +
            "END:DAYLIGHT\n" +
            "END:VTIMEZONE\n" +
            "END:VCALENDAR\n";
    public void testFuzzyTimeZoneMatchGMT_06() throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(VtimeZoneGMT_0600_0500.getBytes())) {
            ZVCalendar tzcal = ZCalendar.ZCalendarBuilder.build(bais, MimeConstants.P_CHARSET_UTF8);
            assertNotNull("tzcal", tzcal);
            ZComponent tzcomp = tzcal.getComponent(ICalTok.VTIMEZONE);
            assertNotNull("tzcomp", tzcomp);
            ICalTimeZone tz = ICalTimeZone.fromVTimeZone(tzcomp);
            ICalTimeZone matchtz = ICalTimeZone.lookupMatchingWellKnownTZ(tz);
            assertEquals("ID of Timezone which fuzzy matches GMT=06.00/-05.00", "America/Chicago", matchtz.getID());
        }
    }

    private static String VtimeZoneGMT_0800_0700 =
            "BEGIN:VCALENDAR\n" +
            "BEGIN:VTIMEZONE\n" +
            "TZID:GMT-08.00/-07.00\n" +
            "BEGIN:STANDARD\n" +
            "DTSTART:16010101T010000\n" +
            "TZOFFSETTO:-0800\n" +
            "TZOFFSETFROM:-0700\n" +
            "RRULE:FREQ=YEARLY;INTERVAL=1;BYMONTH=11;BYDAY=1SU;WKST=MO\n" +
            "END:STANDARD\n" +
            "BEGIN:DAYLIGHT\n" +
            "DTSTART:16010101T030000\n" +
            "TZOFFSETTO:-0700\n" +
            "TZOFFSETFROM:-0800\n" +
            "RRULE:FREQ=YEARLY;INTERVAL=1;BYMONTH=3;BYDAY=2SU;WKST=MO\n" +
            "END:DAYLIGHT\n" +
            "END:VTIMEZONE\n" +
            "END:VCALENDAR\n";
    public void testFuzzyTimeZoneMatchGMT_08() throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(VtimeZoneGMT_0800_0700.getBytes())) {
            ZVCalendar tzcal = ZCalendar.ZCalendarBuilder.build(bais, MimeConstants.P_CHARSET_UTF8);
            assertNotNull("tzcal", tzcal);
            ZComponent tzcomp = tzcal.getComponent(ICalTok.VTIMEZONE);
            assertNotNull("tzcomp", tzcomp);
            ICalTimeZone tz = ICalTimeZone.fromVTimeZone(tzcomp);
            ICalTimeZone matchtz = ICalTimeZone.lookupMatchingWellKnownTZ(tz);
            assertEquals("ID of Timezone which fuzzy matches GMT=08.00/-07.00", "America/Los_Angeles", matchtz.getID());
        }
    }

    private void attendeeDeleteFromCalendar(boolean suppressReply) throws Exception {
        Account dav1 = TestUtil.createAccount(DAV1);
        Account dav2 = TestUtil.createAccount(DAV2);
        String url = getSchedulingInboxUrl(dav1, dav1);
        ReportMethod method = new ReportMethod(url);
        addBasicAuthHeaderForUser(method, dav1);

        ZMailbox organizer = TestUtil.getZMailbox(DAV2);
        String subject = String.format("%s %s", NAME_PREFIX,
                suppressReply ? "testInvite which shouldNOT be replied to" : "testInvite to be auto-declined");
        Date startDate = new Date(System.currentTimeMillis() + Constants.MILLIS_PER_DAY);
        Date endDate = new Date(startDate.getTime() + Constants.MILLIS_PER_HOUR);
        TestUtil.createAppointment(organizer, subject, dav1.getName(), startDate, endDate);

        // Wait for appointment to arrive
        String href = waitForNewSchedulingRequestByUID(dav1, "");
        assertNotNull("href for inbox invitation", href);
        String uid = href.substring(href.lastIndexOf('/') + 1);
        uid = uid.substring(0, uid.indexOf(',') -1);
        String calFolderUrl = getFolderUrl(dav1, "Calendar");
        String delurl = waitForItemInCalendarCollectionByUID(calFolderUrl, dav1, uid, true, 5000);
        StringBuilder sb = getLocalServerRoot().append(delurl);
        DeleteMethod delMethod = new DeleteMethod(sb.toString());
        addBasicAuthHeaderForUser(delMethod, dav1);
        if (suppressReply) {
            delMethod.addRequestHeader(DavProtocol.HEADER_SCHEDULE_REPLY, "F");
        }

        HttpClient client = new HttpClient();
        HttpMethodExecutor.execute(client, delMethod, HttpStatus.SC_NO_CONTENT);
        List<ZMessage> msgs;
        if (suppressReply) {
            // timeout may be a bit short but don't want long time wastes in test suite.
            msgs = TestUtil.waitForMessages(organizer, "is:invite is:unread inid:2 after:\"-1month\"", 0, 2000);
            if (msgs != null) {
                assertEquals("Should be no DECLINE reply msg", 0, msgs.size());
            }
        } else {
            msgs = TestUtil.waitForMessages(organizer, "is:invite is:unread inid:2 after:\"-1month\"", 1, 10000);
            assertNotNull("inbox DECLINE reply msgs", msgs);
            assertEquals("Should be 1 DECLINE reply msg", 1, msgs.size());
            assertNotNull("inbox DECLINE reply msg invite", msgs.get(0).getInvite());
        }
    }

    public void testAttendeeAutoDecline() throws Exception {
        attendeeDeleteFromCalendar(false /* suppressReply */);
    }

    public void testAttendeeSuppressedAutoDecline() throws Exception {
        attendeeDeleteFromCalendar(true /* suppressReply */);
    }

    @Override
    public void setUp() throws Exception {
        if (!TestUtil.fromRunUnitTests) {
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
