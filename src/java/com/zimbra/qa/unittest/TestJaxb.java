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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.Marshaller;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.junit.Assert;
import org.junit.Test;

import com.zimbra.client.ZInvite;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZSearchParams;
import com.zimbra.common.httpclient.HttpClientUtil;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.soap.W3cDomUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.message.BrowseRequest;
import com.zimbra.soap.mail.message.BrowseResponse;
import com.zimbra.soap.mail.message.CreateAppointmentRequest;
import com.zimbra.soap.mail.message.CreateAppointmentResponse;
import com.zimbra.soap.mail.message.GetMsgRequest;
import com.zimbra.soap.mail.message.GetMsgResponse;
import com.zimbra.soap.mail.message.SearchRequest;
import com.zimbra.soap.mail.message.SearchResponse;
import com.zimbra.soap.mail.message.SendInviteReplyRequest;
import com.zimbra.soap.mail.message.SendInviteReplyResponse;
import com.zimbra.soap.mail.type.AddRecurrenceInfo;
import com.zimbra.soap.mail.type.AppointmentHitInfo;
import com.zimbra.soap.mail.type.BrowseData;
import com.zimbra.soap.mail.type.CalOrganizer;
import com.zimbra.soap.mail.type.CalendarAttendee;
import com.zimbra.soap.mail.type.CalendarAttendeeWithGroupInfo;
import com.zimbra.soap.mail.type.DtTimeInfo;
import com.zimbra.soap.mail.type.EmailAddrInfo;
import com.zimbra.soap.mail.type.InstanceDataInfo;
import com.zimbra.soap.mail.type.IntervalRule;
import com.zimbra.soap.mail.type.InvitationInfo;
import com.zimbra.soap.mail.type.InviteComponent;
import com.zimbra.soap.mail.type.InviteComponentWithGroupInfo;
import com.zimbra.soap.mail.type.MimePartInfo;
import com.zimbra.soap.mail.type.Msg;
import com.zimbra.soap.mail.type.MsgSpec;
import com.zimbra.soap.mail.type.RecurrenceInfo;
import com.zimbra.soap.mail.type.SimpleRepeatingRule;
import com.zimbra.soap.type.SearchHit;

public class TestJaxb extends TestCase {

    private static final String USER_NAME = "user1";
    private static final String ATTENDEE1 = USER_NAME;
    private static final String ATTENDEE2 = "user2";
    private static final String ORGANIZER = "user3";
    private static final String NAME_PREFIX = TestJaxb.class.getSimpleName();

    private Marshaller marshaller;

    @Override
    public void setUp() throws Exception {
        cleanUp();
        marshaller = JaxbUtil.createMarshaller();
    }

    @Override
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteTestData(ORGANIZER, NAME_PREFIX);
        TestUtil.deleteTestData(ATTENDEE1, NAME_PREFIX);
        TestUtil.deleteTestData(ATTENDEE2, NAME_PREFIX);
    }

    private static String getCN(ZMailbox mbox) throws ServiceException {
        String cn = mbox.getAccountInfo(false).getAttrs().get("cn").get(0);
        if (cn == null) {
            cn = mbox.getName();
        }
        return cn;
    }

    /**
     * Bug 94018.  Accepting series, then declining single instance leads to inconsistent display of attendee
     * status for organizer copy for the declined instance.
     * Test steps:
     * 1.  Invite 2 users to a daily meeting.
     * 2.  User 1 replies, accepting the daily meeting.
     * 3.  User 1 replies again, declining one of the instances in the daily meeting.
     * 4.  User 2 replies tentatively accepting the daily meeting.
     *
     * At the end of this, check that an exception has been created.  Check that that exception registers
     * the decline from user 1 AND the tentative acceptance from user2 that arrived later.
     */
    @Test
    public void testAcceptSeriesDeclineInstance()
    throws Exception {
        String subject = NAME_PREFIX + " Daily";
        ZMailbox organizerBox = TestUtil.getZMailbox(ORGANIZER);
        ZMailbox attendeeBox = TestUtil.getZMailbox(ATTENDEE1);
        ZMailbox attendee2Box = TestUtil.getZMailbox(ATTENDEE2);
        String organizerEmail = organizerBox.getName();

        // Create and send the daily meeting
        InviteComponent inviteComp = new InviteComponent();
        inviteComp.addAttendee(
                CalendarAttendee.createForAddressDisplaynameRolePartstatRsvp(
                        attendeeBox.getName(), getCN(attendeeBox), "REQ", "NE", true));
        inviteComp.addAttendee(
                CalendarAttendee.createForAddressDisplaynameRolePartstatRsvp(
                        attendee2Box.getName(), getCN(attendee2Box), "REQ", "NE", true));
        inviteComp.setStatus("CONF");
        inviteComp.setFreeBusy("B");
        inviteComp.setCalClass("PUB");
        inviteComp.setTransparency("O");
        inviteComp.setIsDraft(false);
        inviteComp.setIsAllDay(false);
        inviteComp.setDtStart(DtTimeInfo.createForDatetimeAndZone("20161008T130000", "Europe/London"));
        inviteComp.setDtEnd(DtTimeInfo.createForDatetimeAndZone("20161008T140000", "Europe/London"));
        inviteComp.setName(subject);
        inviteComp.setLocation("room 101");
        inviteComp.setOrganizer(CalOrganizer.createForAddress(organizerEmail));
        inviteComp.setRecurrence(RecurrenceInfo.create(
                AddRecurrenceInfo.create(
                        SimpleRepeatingRule.createFromFrequencyAndInterval("DAI", IntervalRule.create(1)))));
        InvitationInfo invite = new InvitationInfo();
        invite.setInviteComponent(inviteComp);
        EmailAddrInfo attendeeAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                attendeeBox.getName(), getCN(attendeeBox), "t");
        EmailAddrInfo attendeeAddr2 = EmailAddrInfo.createForAddressPersonalAndAddressType(
                attendee2Box.getName(), getCN(attendee2Box), "t");
        MimePartInfo mimePart = MimePartInfo.createForContentType("multipart/alternative");
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent("text/plain", "invite body"));
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent(
                "text/html", "<html><body><p><b>invite</b> body</p></body></html>"));
        Msg msg = new Msg();
        msg.setFolderId("10");
        msg.setInvite(invite);
        msg.addEmailAddress(attendeeAddr);
        msg.addEmailAddress(attendeeAddr2);
        msg.setSubject(subject);
        msg.setMimePart(mimePart);
        CreateAppointmentRequest createSeriesRequest = CreateAppointmentRequest.create(msg);
        CreateAppointmentResponse caResp = organizerBox.invokeJaxb(createSeriesRequest);
        Assert.assertNotNull("JAXB CreateAppointmentResponse object", caResp);
        Assert.assertNotNull("JAXB CreateAppointmentResponse calItemId", caResp.getCalItemId());
        Assert.assertNotNull("JAXB CreateAppointmentResponse invId", caResp.getCalInvId());
        Assert.assertNotNull("JAXB CreateAppointmentResponse modified sequence ms", caResp.getModifiedSequence());
        Assert.assertNotNull("JAXB CreateAppointmentResponse rev", caResp.getRevision());
        ZMessage seriesInviteMsg = TestUtil.waitForMessage(attendeeBox, subject);
        Assert.assertNotNull("ZMessage for series invite", seriesInviteMsg);
        ZInvite seriesInvite = seriesInviteMsg.getInvite();
        Assert.assertNotNull("ZInvite for series invite", seriesInvite);

        // User 1 accepts the daily meeting
        ZMessage seriesAcceptMsg = sendInviteReplyToSeries(attendeeBox, organizerBox,
                seriesInviteMsg.getId(), subject, "ACCEPT");
        Assert.assertNotNull("ZMessage for series accept", seriesAcceptMsg);

        // User 1 declines one instance of the daily meeting
        SendInviteReplyRequest sirReq = new SendInviteReplyRequest( seriesInviteMsg.getId(),
                0 /* componentNum */, "DECLINE");
        sirReq.setIdentityId(attendeeBox.getAccountInfo(false).getId());
        sirReq.setExceptionId(DtTimeInfo.createForDatetimeAndZone("20161011T130000", "Europe/London"));
        sirReq.setUpdateOrganizer(true);
        attendeeAddr.setAddressType("f");
        mimePart = MimePartInfo.createForContentType("multipart/alternative");
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent(
                "text/plain", "I won't attend on Tuesday, October 11, 2016."));
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent(
                "text/html", "<html><body><p><b>I won't attend  on Tuesday, October 11, 2016</b></p></body></html>"));
        msg = new Msg();
        msg.setReplyType("r");
        msg.setIdentityId(attendeeBox.getAccountInfo(false).getId());
        EmailAddrInfo orgAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                organizerBox.getName(), organizerBox.getName(), "t");
        msg.addEmailAddress(orgAddr);
        msg.addEmailAddress(attendeeAddr);
        String declineSubject = "Decline: " + subject;
        msg.setSubject(declineSubject);
        msg.setMimePart(mimePart);
        sirReq.setMsg(msg);
        SendInviteReplyResponse sirResp = attendeeBox.invokeJaxb(sirReq);
        Assert.assertNotNull("JAXB SendInviteReplyResponse object", sirResp);
        ZMessage instanceDeclineMsg = TestUtil.waitForMessage(
                organizerBox, String.format("subject:\"%s\"", declineSubject));
        Assert.assertNotNull("ZMessage for series accept", instanceDeclineMsg);
        seriesInviteMsg = TestUtil.waitForMessage(attendee2Box, subject);
        Assert.assertNotNull("ZMessage for series invite", seriesInviteMsg);
        seriesInvite = seriesInviteMsg.getInvite();
        Assert.assertNotNull("ZInvite for series invite", seriesInvite);

        // User 2 tentatively accepts the daily meeting
        ZMessage seriesTentativeMsg = sendInviteReplyToSeries(attendee2Box, organizerBox,
                seriesInviteMsg.getId(), subject, "TENTATIVE");
        Assert.assertNotNull("ZMessage for series tentative", seriesTentativeMsg);

        // Search for the organizer's calendar entry
        SearchRequest sReq = new SearchRequest();
        sReq.setSearchTypes(ZSearchParams.TYPE_APPOINTMENT);
        sReq.setCalItemExpandStart(ymdStringToDate("2016-10-09"));
        sReq.setCalItemExpandEnd(ymdStringToDate("2016-10-14"));
        sReq.setQuery((String.format("in:Calendar and subject:%s", subject)));
        SearchResponse sResp = organizerBox.invokeJaxb(sReq);
        List<SearchHit> hits = sResp.getSearchHits();
        Assert.assertNotNull("Organizer calendar Search hits at end", hits);
        Assert.assertEquals("Num Organizer calendar hits at end", 1, hits.size());
        SearchHit orgCalHit = hits.get(0);
        Assert.assertTrue(orgCalHit instanceof AppointmentHitInfo);
        AppointmentHitInfo orgApptHit = (AppointmentHitInfo) orgCalHit;
        String seriesInviteId = orgApptHit.getInvId();
        Assert.assertNotNull("Organizer Calendar at end - series invite id", seriesInviteId);
        List<InstanceDataInfo> instances = orgApptHit.getInstances();
        Assert.assertNotNull("Organizer Calendar at end - instances in expansion", instances);
        Assert.assertEquals("Organizer Calendar at end - number of instances in expansion", 5, instances.size());
        // The third entry in the list should be for the exception
        String exceptionInviteId = instances.get(2).getInvId();
        Assert.assertNotNull("Organizer Calendar at end - exception invite id", exceptionInviteId);
        String exceptionRidZ = instances.get(2).getRecurIdZ();
        Assert.assertNotNull("Organizer Calendar at end - exception invite RecurIdZ", exceptionRidZ);

        // Do a GetMsg for the exception in the Organizer's calendar
        MsgSpec gmeMsgSpec = new MsgSpec(exceptionInviteId);
        gmeMsgSpec.setRecurIdZ(exceptionRidZ);
        GetMsgRequest gmeReq = new GetMsgRequest(gmeMsgSpec);
        GetMsgResponse gmeResp = organizerBox.invokeJaxb(gmeReq);
        List<InviteComponentWithGroupInfo> eInviteComps = gmeResp.getMsg().getInvite().getInviteComponents();
        Assert.assertEquals("Organizer Calendar at end - number of components in exception", 1, eInviteComps.size());
        List<CalendarAttendeeWithGroupInfo> eAttendees = eInviteComps.get(0).getAttendees();
        Assert.assertEquals("Organizer Calendar at end - number of attendees in exception", 2, eAttendees.size());
        for (CalendarAttendeeWithGroupInfo eAttendee : eAttendees) {
            String addr = eAttendee.getAddress();
            String ptst = eAttendee.getPartStat();
            if (addr.equals(attendeeBox.getName())) {
                Assert.assertEquals("exception attendee1 partstat", "DE", ptst);
            } else if (addr.equals(attendee2Box.getName())) {
                Assert.assertEquals("exception attendee2 partstat", "TE", ptst);
            } else {
                Assert.fail(String.format("Unexpected attendee in exception [%s]", addr));
            }
        }

        // Do a GetMsg for the series in the Organizer's calendar
        MsgSpec gmsMsgSpec = new MsgSpec(seriesInviteId);
        GetMsgRequest gmsReq = new GetMsgRequest(gmsMsgSpec);
        GetMsgResponse gmsResp = organizerBox.invokeJaxb(gmsReq);
        List<InviteComponentWithGroupInfo> sInviteComps = gmsResp.getMsg().getInvite().getInviteComponents();
        Assert.assertEquals("Organizer Calendar at end - number of components in series", 1, sInviteComps.size());
        List<CalendarAttendeeWithGroupInfo> sAttendees = sInviteComps.get(0).getAttendees();
        Assert.assertEquals("Organizer Calendar at end - number of attendees in exception", 2, sAttendees.size());
        for (CalendarAttendeeWithGroupInfo sAttendee : sAttendees) {
            String addr = sAttendee.getAddress();
            String ptst = sAttendee.getPartStat();
            if (addr.equals(attendeeBox.getName())) {
                Assert.assertEquals("exception attendee1 partstat", "AC", ptst);
            } else if (addr.equals(attendee2Box.getName())) {
                Assert.assertEquals("exception attendee2 partstat", "TE", ptst);
            } else {
                Assert.fail(String.format("Unexpected attendee in exception [%s]", addr));
            }
        }
    }

    public static final String YMD_PATTERN = "yyyy-MM-dd";
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    private static long ymdStringToDate(String ymdString) {
        SimpleDateFormat formatter = new SimpleDateFormat(YMD_PATTERN, Locale.US);
        formatter.setTimeZone(GMT);
        Date date;
        try {
            date = formatter.parse(ymdString);
            return date.getTime();
        } catch (ParseException e) {
            Assert.fail(String.format("Failed to convert string %s to long - exception %s",  ymdString, e.getMessage()));
        }
        return 0;
    }

    private ZMessage sendInviteReplyToSeries(
            ZMailbox attendeeBox, ZMailbox organizerBox, String inviteMsgId, String subjectSuffix, String verb)
    throws Exception {
        SendInviteReplyRequest sirReq = new SendInviteReplyRequest(inviteMsgId, 0 /* componentNum */, verb);
        sirReq.setIdentityId(attendeeBox.getAccountInfo(false).getId());
        sirReq.setUpdateOrganizer(true);
        EmailAddrInfo attendeeAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                attendeeBox.getName(), getCN(attendeeBox), "f");
        EmailAddrInfo orgAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                organizerBox.getName(), organizerBox.getName(), "t");
        MimePartInfo mimePart = MimePartInfo.createForContentType("multipart/alternative");
        String plainText;
        String subject;
        if ("ACCEPT".equals(verb)) {
            plainText = "Yes, I will attend.";
            subject = "Accept: " + subjectSuffix;
        } else if ("DECLINE".equals(verb)) {
            plainText = "No, I won't attend.";
            subject = "Decline: " + subjectSuffix;
        } else {
            plainText = "Maybe, I will attend.";
            subject = "Tentative: " + subjectSuffix;
        }
        String htmlText = String.format("<html><body><p><b>%s</b></p></body></html>", plainText);
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent("text/plain", plainText));
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent("text/html", htmlText));
        Msg msg = new Msg();
        msg.setReplyType("r");
        msg.setIdentityId(attendeeBox.getAccountInfo(false).getId());
        msg.addEmailAddress(orgAddr);
        msg.addEmailAddress(attendeeAddr);
        msg.setSubject(subject);
        msg.setMimePart(mimePart);
        sirReq.setMsg(msg);
        SendInviteReplyResponse sirResp = attendeeBox.invokeJaxb(sirReq);
        Assert.assertNotNull("JAXB SendInviteReplyResponse object", sirResp);
        return TestUtil.waitForMessage(organizerBox, String.format("subject:\"%s\"", subject));
    }

    private String envelope(String authToken, String requestBody) {
        return "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\">" +
        "<soap:Header>"+
        "<context xmlns=\"urn:zimbra\">"+
        "<userAgent name=\"Zimbra Junit\" version=\"0.0\"/>"+
        "<authToken>" + authToken + "</authToken>" +
        "<authTokenControl voidOnExpired=\"1\"/>" +
        "<nosession/>"+
        "</context>"+
        "</soap:Header>"+
        "<soap:Body>"+
        requestBody +
        "</soap:Body>"+
        "</soap:Envelope>";
    }

    private Object sendReq(String requestBody, String requestCommand)
    throws HttpException, IOException, ServiceException {
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(TestUtil.getSoapUrl() + requestCommand);
        post.setRequestEntity(new StringRequestEntity(requestBody, "application/soap+xml", "UTF-8"));
        int respCode = HttpClientUtil.executeMethod(client, post);
        Assert.assertEquals(200, respCode);
        Element envelope = W3cDomUtil.parseXML(post.getResponseBodyAsStream());
        SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
        Element doc = proto.getBodyElement(envelope);
        return JaxbUtil.elementToJaxb(doc);
    }

    private Element sendReqExpectingFail(String requestBody, String requestCommand, int expectedRespCode)
    throws HttpException, IOException, ServiceException {
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(TestUtil.getSoapUrl() + requestCommand);
        post.setRequestEntity(new StringRequestEntity(requestBody, "application/soap+xml", "UTF-8"));
        int respCode = HttpClientUtil.executeMethod(client, post);
        Assert.assertEquals(expectedRespCode, respCode);
        return W3cDomUtil.parseXML(post.getResponseBodyAsStream());
    }

    public BrowseResponse doBrowseRequest(BrowseRequest browseRequest) throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String authToken = mbox.getAuthToken().getValue();
        DocumentResult dr = new DocumentResult();
        marshaller.marshal(browseRequest, dr);
        Document doc = dr.getDocument();
        ZimbraLog.test.info(doc.getRootElement().asXML());
        return (BrowseResponse) sendReq(envelope(authToken, doc.getRootElement().asXML()), "BrowseRequest");
    }

    @Test
    public void testBrowseRequestDomains() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
        BrowseRequest browseRequest = new BrowseRequest("domains" /* browseBy */, "" /* regex */, 10);
        BrowseResponse browseResponse = doBrowseRequest(browseRequest);
        Assert.assertNotNull("JAXB BrowseResponse object", browseResponse);
        List<BrowseData> datas = browseResponse.getBrowseDatas();
        Assert.assertNotNull("JAXB BrowseResponse datas", datas);
    }

    @Test
    public void testBrowseRequestAttachments() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
        BrowseRequest browseRequest = new BrowseRequest("attachments" /* browseBy */, "" /* regex */, 10);
        BrowseResponse browseResponse = doBrowseRequest(browseRequest);
        Assert.assertNotNull("JAXB BrowseResponse object", browseResponse);
        List<BrowseData> datas = browseResponse.getBrowseDatas();
        Assert.assertNotNull("JAXB BrowseResponse datas", datas);
    }

    static String bodyWithObject = "contains http://www.example.com/fun so treat as containing com_zimbra_url object";
    @Test
    public void testBrowseRequestObjects() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        MessageBuilder mb = new MessageBuilder();
        String raw = mb.withSubject(NAME_PREFIX).withBody(bodyWithObject)
                .withContentType(MimeConstants.CT_TEXT_PLAIN).create();
        TestUtil.addRawMessage(mbox, raw);
        BrowseRequest browseRequest = new BrowseRequest("objects" /* browseBy */, "" /* regex */, 10);
        BrowseResponse browseResponse = doBrowseRequest(browseRequest);
        Assert.assertNotNull("JAXB BrowseResponse object", browseResponse);
        List<BrowseData> datas = browseResponse.getBrowseDatas();
        Assert.assertNotNull("JAXB BrowseResponse datas", datas);
        Assert.assertTrue("BrowseDatas size should be greater than 1", datas.size() >= 1);
    }

    public Element doBadBrowseRequest(BrowseRequest browseRequest) throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String authToken = mbox.getAuthToken().getValue();
        DocumentResult dr = new DocumentResult();
        marshaller.marshal(browseRequest, dr);
        Document doc = dr.getDocument();
        ZimbraLog.test.info(doc.getRootElement().asXML());
        return sendReqExpectingFail(envelope(authToken, doc.getRootElement().asXML()), "BrowseRequest", 500);
    }

    static String BAD_REGEX = ".*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*822";
    /** BrowseRequest should fail as regex is too complex */
    @Test
    public void testBrowseRequestDomainsBadRegex() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
        BrowseRequest browseRequest = new BrowseRequest("domains" /* browseBy */, BAD_REGEX /* regex */, 10);
        Element envelope = doBadBrowseRequest(browseRequest);
        Assert.assertNotNull("Envelope", envelope);
        Assert.assertTrue("Error contained in SOAP response",
            envelope.toString().contains("regular expression match involved more than 100000 accesses for pattern"));
    }

    /** BrowseRequest should fail as regex is too complex */
    @Test
    public void testBrowseRequestObjectsBadRegex() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        MessageBuilder mb = new MessageBuilder();
        String raw = mb.withSubject(NAME_PREFIX).withBody(bodyWithObject)
                .withContentType(MimeConstants.CT_TEXT_PLAIN).create();
        TestUtil.addRawMessage(mbox, raw);
        BrowseRequest browseRequest = new BrowseRequest("objects" /* browseBy */, "" /* regex */, 10);
        BrowseResponse browseResponse = doBrowseRequest(browseRequest);
        Assert.assertTrue("BrowseDatas size should be greater than 1", browseResponse.getBrowseDatas().size() >= 1);
        browseRequest = new BrowseRequest("objects" /* browseBy */, BAD_REGEX /* regex */, 10);
        Element envelope = doBadBrowseRequest(browseRequest);
        Assert.assertNotNull("Envelope", envelope);
        Assert.assertTrue("Error contained in SOAP response",
            envelope.toString().contains("regular expression match involved more than 100000 accesses for pattern"));
    }

    /** BrowseRequest should fail as regex is too complex */
    @Test
    public void testBrowseRequestAttachmentsBadRegex() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        TestUtil.addMessage(mbox, NAME_PREFIX);
        BrowseRequest browseRequest = new BrowseRequest("attachments" /* browseBy */, BAD_REGEX /* regex */, 10);
        Element envelope = doBadBrowseRequest(browseRequest);
        Assert.assertNotNull("Envelope", envelope);
        Assert.assertTrue("Error contained in SOAP response",
            envelope.toString().contains("regular expression match involved more than 100000 accesses for pattern"));
    }
}
