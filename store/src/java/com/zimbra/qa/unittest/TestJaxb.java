/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.qa.unittest;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.Marshaller;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZDateTime;
import com.zimbra.client.ZFolder;
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
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.message.BrowseRequest;
import com.zimbra.soap.mail.message.BrowseResponse;
import com.zimbra.soap.mail.message.CounterAppointmentRequest;
import com.zimbra.soap.mail.message.CounterAppointmentResponse;
import com.zimbra.soap.mail.message.CreateAppointmentRequest;
import com.zimbra.soap.mail.message.CreateAppointmentResponse;
import com.zimbra.soap.mail.message.GetFolderRequest;
import com.zimbra.soap.mail.message.GetFolderResponse;
import com.zimbra.soap.mail.message.GetMsgRequest;
import com.zimbra.soap.mail.message.GetMsgResponse;
import com.zimbra.soap.mail.message.ModifyAppointmentRequest;
import com.zimbra.soap.mail.message.ModifyAppointmentResponse;
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

public class TestJaxb {

    @Rule
    public TestName testInfo = new TestName();

    private String USER_NAME = null;
    private String ATTENDEE1 = null;
    private String ATTENDEE2 = null;
    private String ORGANIZER = null;
    private static final String NAME_PREFIX = TestJaxb.class.getSimpleName();

    private Marshaller marshaller;

    @Before
    public void setUp() throws Exception {
        marshaller = JaxbUtil.createMarshaller();
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        USER_NAME = prefix + "user1";
        ATTENDEE1 = USER_NAME;
        ATTENDEE2 = prefix + "attendee2";
        ORGANIZER = prefix + "organizer";
        tearDown();
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(ATTENDEE1);
        TestUtil.deleteAccountIfExists(ATTENDEE2);
        TestUtil.deleteAccountIfExists(ORGANIZER);
    }

    private static String getCN(ZMailbox mbox) throws ServiceException {
        String cn = mbox.getAccountInfo(false).getAttrs().get("cn").get(0);
        if (cn == null) {
            cn = mbox.getName();
        }
        return cn;
    }

    /**
     * Bug 96748:
     * 1. user1 sends meeting invite to user2
     * 2. user2 proposes new time
     * 3. user1 accepts the proposed new time and new invite is sent to user2
     * 4. user2 accepts the new invite
     * At step 4, no acceptance message was being generated
     */
    @Test
    public void testProposeNewTimeWorkflow()
    throws Exception
    {
        TestUtil.createAccount(ORGANIZER);
        TestUtil.createAccount(ATTENDEE1);
        String subject = NAME_PREFIX + " attendee will cause time to change";
        ZMailbox organizerBox = TestUtil.getZMailbox(ORGANIZER);
        ZMailbox attendeeBox = TestUtil.getZMailbox(ATTENDEE1);
        String organizerEmail = organizerBox.getName();

        // Create and send the meeting request
        InviteComponent inviteComp = new InviteComponent();
        inviteComp.addAttendee(
                CalendarAttendee.createForAddressDisplaynameRolePartstatRsvp(
                        attendeeBox.getName(), getCN(attendeeBox), "REQ", "NE", true));
        inviteComp.setStatus("CONF");
        inviteComp.setFreeBusy("B");
        inviteComp.setCalClass("PUB");
        inviteComp.setTransparency("O");
        inviteComp.setIsDraft(false);
        inviteComp.setIsAllDay(false);
        Date startDate = new Date(System.currentTimeMillis() + Constants.MILLIS_PER_DAY);
        ZDateTime start = new ZDateTime(startDate.getTime(), false, organizerBox.getPrefs().getTimeZone());
        Date endDate = new Date(startDate.getTime() + Constants.MILLIS_PER_HOUR);
        ZDateTime end = new ZDateTime(endDate.getTime(), false, organizerBox.getPrefs().getTimeZone());
        Date newStartDate = new Date(System.currentTimeMillis() + 2 * Constants.MILLIS_PER_DAY);
        ZDateTime newStart = new ZDateTime(newStartDate.getTime(), false, organizerBox.getPrefs().getTimeZone());
        Date newEndDate = new Date(newStartDate.getTime() + Constants.MILLIS_PER_HOUR);
        ZDateTime newEnd = new ZDateTime(newEndDate.getTime(), false, organizerBox.getPrefs().getTimeZone());
        inviteComp.setDtStart(DtTimeInfo.createForDatetimeAndZone(start.getDateTime(), start.getTimeZoneId()));
        inviteComp.setDtEnd(DtTimeInfo.createForDatetimeAndZone(end.getDateTime(), end.getTimeZoneId()));
        inviteComp.setName(subject);
        inviteComp.setLocation("room 101");
        inviteComp.setOrganizer(CalOrganizer.createForAddress(organizerEmail));
        InvitationInfo invite = new InvitationInfo();
        invite.setInviteComponent(inviteComp);
        EmailAddrInfo attendeeAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                attendeeBox.getName(), getCN(attendeeBox), "t");
        MimePartInfo mimePart = MimePartInfo.createForContentType("multipart/alternative");
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent("text/plain", "invite body"));
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent(
                "text/html", "<html><body><p><b>invite</b> body</p></body></html>"));
        Msg msg = new Msg();
        msg.setFolderId("10");
        msg.setInvite(invite);
        msg.addEmailAddress(attendeeAddr);
        msg.setSubject(subject);
        msg.setMimePart(mimePart);
        CreateAppointmentRequest createApptReq = CreateAppointmentRequest.create(msg);
        CreateAppointmentResponse caResp = organizerBox.invokeJaxb(createApptReq);
        Assert.assertNotNull("JAXB CreateAppointmentResponse object", caResp);
        Assert.assertNotNull("JAXB CreateAppointmentResponse calItemId", caResp.getCalItemId());
        Assert.assertNotNull("JAXB CreateAppointmentResponse invId", caResp.getCalInvId());
        Assert.assertNotNull("JAXB CreateAppointmentResponse modified sequence ms", caResp.getModifiedSequence());
        Assert.assertNotNull("JAXB CreateAppointmentResponse rev", caResp.getRevision());
        ZMessage seriesInviteMsg = TestUtil.waitForMessage(attendeeBox, subject);
        Assert.assertNotNull("ZMessage for series invite", seriesInviteMsg);
        ZInvite seriesInvite = seriesInviteMsg.getInvite();
        Assert.assertNotNull("ZInvite for series invite", seriesInvite);
        AppointmentHitInfo hit = findMatchingAppointment(attendeeBox, startDate, endDate, subject);
        // User 1 proposes new time for meeting
        ZMessage propNewTimeMsg = attendeeProposeNewTimeForMeeting(attendeeBox, organizerBox,
                newStart, newEnd, hit, subject);
        Assert.assertNotNull("ZMessage for propose new time", propNewTimeMsg);
        hit = findMatchingAppointment(organizerBox, startDate, endDate, subject);
        // Organizer changes the meeting to the new proposed time
        subject = NAME_PREFIX + " attendee CAUSED time to change"; // easier to find unique inbox entry
        ZMessage attendee2ndInvite = organizerChangeTimeForMeeting(attendeeBox, organizerBox,
                newStart, newEnd, hit, subject);
        Assert.assertNotNull("attendee 2nd invite", attendee2ndInvite);
        hit = findMatchingAppointment(attendeeBox, newStartDate, newEndDate, "inid:10");
        acceptInvite(attendeeBox, organizerBox, attendee2ndInvite, subject);
    }

    private ZMessage acceptInvite(ZMailbox attendeeBox, ZMailbox organizerBox, ZMessage inviteMsg, String subject)
    throws Exception {
        SendInviteReplyRequest sirReq = new SendInviteReplyRequest(inviteMsg.getId(), 0 /* componentNum */, "ACCEPT");
        sirReq.setIdentityId(attendeeBox.getAccountInfo(false).getId());
        // ZWC 8.6 and earlier used to set this to false.  Now sets it to true.
        sirReq.setUpdateOrganizer(true);
        MimePartInfo mimePart = MimePartInfo.createForContentType("multipart/alternative");
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent("text/plain", "Accepting"));
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent(
                "text/html", "<html><body><p><b>Accepting</b></p></body></html>"));
        Msg msg = new Msg();
        msg.setReplyType("r");
        msg.setIdentityId(attendeeBox.getAccountInfo(false).getId());
        EmailAddrInfo orgAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                organizerBox.getName(), organizerBox.getName(), "t");
        EmailAddrInfo attendeeAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                attendeeBox.getName(), getCN(attendeeBox), "f");
        msg.addEmailAddress(orgAddr);
        msg.addEmailAddress(attendeeAddr);
        String acceptSubject = "Accept: " + subject;
        msg.setSubject(acceptSubject);
        msg.setMimePart(mimePart);
        sirReq.setMsg(msg);
        SendInviteReplyResponse sirResp = attendeeBox.invokeJaxb(sirReq);
        Assert.assertNotNull("JAXB SendInviteReplyResponse object", sirResp);
        ZMessage inboxMsg = TestUtil.waitForMessage(
                organizerBox, String.format("subject:\"%s\"", acceptSubject));
        Assert.assertNotNull("ZMessage for accept", inboxMsg);
        return inboxMsg;
    }

    /**
     * @param hit - From search response - represents the organizer's calendar copy
     * @throws ServiceException
     */
    private ZMessage organizerChangeTimeForMeeting(ZMailbox attendeeBox, ZMailbox organizerBox,
            ZDateTime newStart, ZDateTime newEnd, AppointmentHitInfo hit, String subject)
    throws Exception
    {
        String organizerEmail = organizerBox.getName();
        InviteComponent inviteComp = new InviteComponent();
        inviteComp.addAttendee(
                CalendarAttendee.createForAddressDisplaynameRolePartstatRsvp(attendeeBox.getName(), getCN(attendeeBox),
                        "REQ", "NE", true));   /* Note Tentative, not Needs action */
        inviteComp.setStatus("CONF");
        inviteComp.setFreeBusy("B");
        inviteComp.setCalClass("PUB");
        inviteComp.setTransparency("O");
        inviteComp.setIsDraft(false);
        inviteComp.setIsAllDay(false);
        inviteComp.setDtStart(DtTimeInfo.createForDatetimeAndZone(newStart.getDateTime(), newStart.getTimeZoneId()));
        inviteComp.setDtEnd(DtTimeInfo.createForDatetimeAndZone(newEnd.getDateTime(), newEnd.getTimeZoneId()));
        inviteComp.setName(subject);
        inviteComp.setLocation("room 101");
        inviteComp.setOrganizer(CalOrganizer.createForAddress(organizerEmail));
        InvitationInfo invite = new InvitationInfo();
        invite.setUid(hit.getUid());
        invite.setInviteComponent(inviteComp);
        MimePartInfo mimePart = MimePartInfo.createForContentType("multipart/alternative");
        String bodyText =
                String.format("The following meeting has been modified:\n\n%s",subject);
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent("text/plain", bodyText));
        Msg msg = new Msg();
        msg.setFolderId(hit.getFolderId());
        msg.setInvite(invite);
        EmailAddrInfo attendeeAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                attendeeBox.getName(), getCN(attendeeBox), "t");
        msg.addEmailAddress(attendeeAddr);
        msg.setSubject(subject);
        msg.setMimePart(mimePart);
        ModifyAppointmentRequest maReq = ModifyAppointmentRequest.createForIdModseqRevCompnumMsg(
                hit.getInvId(), hit.getModifiedSequence(), hit.getRevision(), hit.getComponentNum(), msg);
        ModifyAppointmentResponse maResp = organizerBox.invokeJaxb(maReq);
        Assert.assertNotNull("JAXB ModifyAppointmentResponse", maResp);
        return TestUtil.waitForMessage(attendeeBox, String.format("subject:\"%s\"", subject));
    }

    private AppointmentHitInfo findMatchingAppointment(ZMailbox mbox,
        Date startDate, Date endDate, String subject) throws ServiceException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setSortBy("none");
        searchRequest.setLimit(500);
        searchRequest.setLocale("en_US");
        searchRequest.setCalItemExpandStart(startDate.getTime() - 1000);
        searchRequest.setCalItemExpandEnd(endDate.getTime() + 1000);
        searchRequest.setSearchTypes("appointment");
        searchRequest.setOffset(0);
        searchRequest.setQuery(subject);
        SearchResponse searchResp = mbox.invokeJaxb(searchRequest);
        Assert.assertNotNull("JAXB SearchResponse object", searchResp);
        List<SearchHit> hits = searchResp.getSearchHits();
        Assert.assertNotNull("JAXB SearchResponse hits", hits);
        Assert.assertEquals("JAXB SearchResponse hits", 1, hits.size());
        return (AppointmentHitInfo) hits.get(0);
    }

    /**
     * @param hit - From search response - represents the attendee's calendar copy
     * @return Message representing the Organizer's intray copy of the new proposal, once it has arrived
     */
    private ZMessage attendeeProposeNewTimeForMeeting(ZMailbox attendeeBox, ZMailbox organizerBox,
            ZDateTime newStart, ZDateTime newEnd, AppointmentHitInfo hit, String subjectSuffix)
    throws Exception {
        EmailAddrInfo orgAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                organizerBox.getName(), organizerBox.getName(), "t");
        MimePartInfo mimePart = MimePartInfo.createForContentType("multipart/alternative");
        String plainText = "New Time Proposed.";
        String subject = "New Time Proposed: " + subjectSuffix;
        String htmlText = String.format("<html><body><p><b>%s</b></p></body></html>", plainText);
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent("text/plain", plainText));
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent("text/html", htmlText));
        Msg msg = new Msg();
        InviteComponent compo = new InviteComponent();
        compo.setName(subjectSuffix);
        compo.setUid(hit.getUid());
        compo.setIsAllDay(hit.getAllDay());
        compo.setOrganizer(CalOrganizer.createForAddress(organizerBox.getName()));
        compo.setDtStart(DtTimeInfo.createForDatetimeAndZone(newStart.getDateTime(), newStart.getTimeZoneId()));
        compo.setDtEnd(DtTimeInfo.createForDatetimeAndZone(newEnd.getDateTime(), newEnd.getTimeZoneId()));
        InvitationInfo invite = InvitationInfo.create(compo);
        msg.addEmailAddress(orgAddr);  /* replying to the organizer */
        msg.setSubject(subject);
        msg.setMimePart(mimePart);
        msg.setInvite(invite);
        CounterAppointmentResponse sirResp = attendeeBox.invokeJaxb(
             CounterAppointmentRequest.createForMsgModseqRevIdCompnum(
                 msg, hit.getModifiedSequence(), hit.getRevision(), hit.getInvId(), hit.getComponentNum()));
        Assert.assertNotNull("JAXB CounterAppointmentResponse object", sirResp);
        return TestUtil.waitForMessage(organizerBox, String.format("subject:\"%s\"", subject));
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
        TestUtil.createAccount(ORGANIZER);
        TestUtil.createAccount(ATTENDEE1);
        TestUtil.createAccount(ATTENDEE2);
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
        ZimbraLog.test.debug(doc.getRootElement().asXML());
        return (BrowseResponse) sendReq(envelope(authToken, doc.getRootElement().asXML()), "BrowseRequest");
    }

    @Test
    public void testBrowseRequestDomains() throws Exception {
        TestUtil.createAccount(USER_NAME);
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
        TestUtil.createAccount(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        MessageBuilder mb = new MessageBuilder();
        String raw = mb.withSubject(NAME_PREFIX).withBody(bodyWithObject)
                .withContentType(MimeConstants.CT_APPLICATION_PDF).create();
        TestUtil.addRawMessage(zmbox, raw);
        forceIndexing(USER_NAME);
        BrowseRequest browseRequest = new BrowseRequest("attachments" /* browseBy */, "" /* regex */, 10);
        BrowseResponse browseResponse = doBrowseRequest(browseRequest);
        Assert.assertNotNull("JAXB BrowseResponse object", browseResponse);
        List<BrowseData> datas = browseResponse.getBrowseDatas();
        Assert.assertNotNull("JAXB BrowseResponse datas", datas);
        Assert.assertTrue("JAXB BrowseResponse datas", datas.size() >=1);
    }

    static String bodyWithObject = "contains http://www.example.com/fun so treat as containing com_zimbra_url object";
    @Test
    public void testBrowseRequestObjects() throws Exception {
        TestUtil.createAccount(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        MessageBuilder mb = new MessageBuilder();
        String raw = mb.withSubject(NAME_PREFIX).withBody(bodyWithObject)
                .withContentType(MimeConstants.CT_TEXT_PLAIN).create();
        TestUtil.addRawMessage(zmbox, raw);
        forceIndexing(USER_NAME);
        BrowseRequest browseRequest = new BrowseRequest("objects" /* browseBy */, "" /* regex */, 10);
        BrowseResponse browseResponse = doBrowseRequest(browseRequest);
        Assert.assertNotNull("JAXB BrowseResponse object", browseResponse);
        List<BrowseData> datas = browseResponse.getBrowseDatas();
        Assert.assertNotNull("JAXB BrowseResponse datas", datas);
        Assert.assertTrue("BrowseDatas size should be greater than 0", datas.size() >= 1);
    }

    public Element doBadBrowseRequest(BrowseRequest browseRequest) throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String authToken = mbox.getAuthToken().getValue();
        DocumentResult dr = new DocumentResult();
        marshaller.marshal(browseRequest, dr);
        Document doc = dr.getDocument();
        ZimbraLog.test.debug(doc.getRootElement().asXML());
        return sendReqExpectingFail(envelope(authToken, doc.getRootElement().asXML()), "BrowseRequest", 500);
    }

    static String BAD_REGEX = ".*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*822";
    /** BrowseRequest should fail as regex is too complex */
    @Test
    public void testBrowseRequestDomainsBadRegex() throws Exception {
        TestUtil.createAccount(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        // If there are no messages to match at all, the browse will pass because the regex doesn't get
        // used.  Need some messages containing email addresses with domains in to match against
        TestUtil.addMessage(zmbox, NAME_PREFIX);
        forceIndexing(USER_NAME);
        BrowseRequest browseRequest = new BrowseRequest("domains" /* browseBy */, BAD_REGEX /* regex */, 10);
        Element envelope = doBadBrowseRequest(browseRequest);
        Assert.assertNotNull("Envelope", envelope);
        Assert.assertTrue("Error contained in SOAP response",
            envelope.toString().contains("regular expression match involved more than 100000 accesses for pattern"));
    }

    /** BrowseRequest should fail as regex is too complex */
    @Test
    public void testBrowseRequestObjectsBadRegex() throws Exception {
        TestUtil.createAccount(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        MessageBuilder mb = new MessageBuilder();
        String raw = mb.withSubject(NAME_PREFIX).withBody(bodyWithObject)
                .withContentType(MimeConstants.CT_TEXT_PLAIN).create();
        TestUtil.addRawMessage(zmbox, raw);
        forceIndexing(USER_NAME);
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
        TestUtil.createAccount(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        MessageBuilder mb = new MessageBuilder();
        String raw = mb.withSubject(NAME_PREFIX).withBody(bodyWithObject)
                .withContentType(MimeConstants.CT_APPLICATION_PDF).create();
        TestUtil.addRawMessage(zmbox, raw);
        forceIndexing(USER_NAME);
        BrowseRequest browseRequest = new BrowseRequest("attachments" /* browseBy */, BAD_REGEX /* regex */, 10);
        Element envelope = doBadBrowseRequest(browseRequest);
        Assert.assertNotNull("Envelope", envelope);
        Assert.assertTrue("Error contained in SOAP response",
            envelope.toString().contains("regular expression match involved more than 100000 accesses for pattern"));
    }

    private static final String FOLDER_F1 = "/" + NAME_PREFIX + "-f1";
    private static final String SUB_FOLDER_F2 = FOLDER_F1 + "/" + NAME_PREFIX + "-subf2";

    @Test
    public void testGetFolderWithCacheContainingNullParent() throws Exception {
        TestUtil.createAccount(USER_NAME);
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder f1 = TestUtil.createFolder(zmbox, FOLDER_F1);
        ZFolder f2 = TestUtil.createFolder(zmbox, SUB_FOLDER_F2);
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        OperationContext octxt = new OperationContext(mbox);
        ItemId f1ItemId = new ItemId(mbox, Integer.parseInt(f1.getId()));
        ItemId f2ItemId = new ItemId(mbox, Integer.parseInt(f2.getId()));
        // ItemId rootItemId = new ItemId(mbox, Mailbox.ID_FOLDER_USER_ROOT);
        // mbox.getFolderTree(octxt, rootItemId, false);
        Folder f1folder = mbox.getFolderById(octxt, f1ItemId.getId());
        Folder f2folder = mbox.getFolderById(octxt, f2ItemId.getId());
        // Bug 100588 f1folder/f2folder will have been retrieved from the folder cache.  Deliberately poison it
        // by setting the parent folder to null, and ensure that can still successfully do a GetFolderRequest
        f1folder.setParent(null);
        f2folder.setParent(null);
        GetFolderResponse gfResp = zmbox.invokeJaxb(new GetFolderRequest());
        Assert.assertNotNull("GetFolderResponse null", gfResp);
    }

    /**
     * Newly created items don't seem to be seen immediately by BrowseRequest, unless we do this.
     * TODO: Should BrowseRequest do this itself?
     * @throws ServiceException
     */
    private void forceIndexing(String acctName) throws ServiceException {
        Mailbox mbox = TestUtil.getMailbox(acctName);
        mbox.index.indexDeferredItems();
    }
}
