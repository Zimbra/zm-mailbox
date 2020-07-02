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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import javax.xml.bind.Marshaller;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dom4j.Document;
import org.dom4j.io.DocumentResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZAppointmentHit;
import com.zimbra.client.ZDateTime;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZInvite;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZSearchParams;
import com.zimbra.common.calendar.ParsedDateTime;
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
import com.zimbra.soap.mail.type.DateTimeStringAttr;
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
     * @param attendeeZMbox - null if no attendee needed
     */
    public static InviteComponent createInviteComponentSkeleton(String subject, String location,
            ZMailbox organizerZMbox, ZMailbox... attendeeZMboxes) throws ServiceException {
        InviteComponent inviteComp = new InviteComponent();
        if (attendeeZMboxes != null) {
            for (ZMailbox attendeeZMbox : attendeeZMboxes) {
                inviteComp.addAttendee(
                        CalendarAttendee.createForAddressDisplaynameRolePartstatRsvp(
                                attendeeZMbox.getName(), getCN(attendeeZMbox), "REQ",
                                ZAppointmentHit.PSTATUS_NEEDS_ACTION, true));
            }
        }
        inviteComp.setStatus("CONF");
        inviteComp.setFreeBusy("B");
        inviteComp.setCalClass("PUB");
        inviteComp.setTransparency("O");
        inviteComp.setIsDraft(false);
        inviteComp.setIsAllDay(false);
        inviteComp.setName(subject);
        inviteComp.setLocation(location);
        inviteComp.setOrganizer(CalOrganizer.createForAddress(organizerZMbox.getName()));
        return inviteComp;
    }

    public static MimePartInfo makeTextAndHtmlAlternatives(String text) {
        MimePartInfo mimePart = MimePartInfo.createForContentType("multipart/alternative");
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent("text/plain", text));
        mimePart.addMimePart(MimePartInfo.createForContentTypeAndContent(
                "text/html", String.format("<html><body><p><b>%s</b></p></body></html>", text)));
        return mimePart;
    }

    public static Msg createMsgForAppointmentRequest(String subject, InviteComponent inviteComp,
            ZMailbox...  attendeeZMboxes) throws ServiceException {
        Msg msg = new Msg();
        msg.setFolderId("10");
        msg.setInvite(InvitationInfo.create(inviteComp));
        if (attendeeZMboxes != null) {
            for (ZMailbox attendeeZMbox : attendeeZMboxes) {
                msg.addEmailAddress(EmailAddrInfo.createForAddressPersonalAndAddressType(
                        attendeeZMbox.getName(), getCN(attendeeZMbox), "t"));
            }
        }
        msg.setSubject(subject);
        msg.setMimePart(makeTextAndHtmlAlternatives("invite body"));
        return msg;
    }

    public static CreateAppointmentResponse sendMeetingRequest(ZMailbox zmbox, Msg msg)
            throws ServiceException {
        CreateAppointmentResponse caResp = zmbox.invokeJaxb(CreateAppointmentRequest.create(msg));
        Assert.assertNotNull("JAXB CreateAppointmentResponse object", caResp);
        Assert.assertNotNull("JAXB CreateAppointmentResponse calItemId", caResp.getCalItemId());
        Assert.assertNotNull("JAXB CreateAppointmentResponse invId", caResp.getCalInvId());
        Assert.assertNotNull("JAXB CreateAppointmentResponse modified sequence ms",
                caResp.getModifiedSequence());
        Assert.assertNotNull("JAXB CreateAppointmentResponse rev", caResp.getRevision());
        return caResp;
    }

    public static ZMessage waitForInvite(ZMailbox zmbox, String subject) throws Exception {
        ZMessage msg = TestUtil.waitForMessage(zmbox, String.format("subject:\"%s\"", subject));
        Assert.assertNotNull(String.format("ZMessage from waitForInvite(mbox=%s, subject='%s')",
                zmbox.getName(), subject), msg);
        ZInvite invite = msg.getInvite();
        Assert.assertNotNull(String.format("ZInvite from waitForInvite(mbox=%s, subject='%s')",
                zmbox.getName(), subject), invite);
        return msg;
    }

    public static String dateTime(Calendar cal) {
        return String.format("%04d%02d%02dT%02d%02d00",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1 /* is zero based in Calendar */,
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE));
    }

    public static String prettyIshDateTime(Calendar cal) {
        return String.format("%04d-%02d-%02d %02d:%02",
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1 /* is zero based in Calendar */,
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE));
    }

    public static RecurrenceInfo recurrence(String frequency, int interval, String until) {
        SimpleRepeatingRule repeatingRule = SimpleRepeatingRule.createFromFrequencyAndInterval(
                frequency, IntervalRule.create(interval));
        if (until != null) {
            repeatingRule.setUntil(new DateTimeStringAttr(until));
        }
        return RecurrenceInfo.create(AddRecurrenceInfo.create(repeatingRule));
    }

    public static void setStartAndEnd(InviteComponent inviteComp, String tzName,
            GregorianCalendar start, GregorianCalendar end) {
        inviteComp.setDtStart(DtTimeInfo.createForDatetimeAndZone(dateTime(start), tzName));
        inviteComp.setDtEnd(DtTimeInfo.createForDatetimeAndZone(dateTime(end), tzName));
    }

    public static GregorianCalendar startTime(
            String tzName, int year, int monthZeroBased, int day, int hour, int minute) {
        TimeZone tz = TimeZone.getTimeZone(tzName);
        GregorianCalendar start = new GregorianCalendar(tz);
        start.clear() /* otherwise milliseconds left at some random value! */;
        start.set(year, monthZeroBased, day, hour, minute, 0);
        return start;
    }

    public static GregorianCalendar plus(GregorianCalendar start, int field, int amount) {
        GregorianCalendar end = (GregorianCalendar) start.clone();
        end.add(field, amount);
        return end;
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
    public void proposeNewTimeWorkflow() throws Exception {
        TestUtil.createAccount(ORGANIZER);
        TestUtil.createAccount(ATTENDEE1);
        String subject = NAME_PREFIX + " attendee will cause time to change";
        ZMailbox organizerBox = TestUtil.getZMailbox(ORGANIZER);
        ZMailbox attendeeBox = TestUtil.getZMailbox(ATTENDEE1);

        // Create and send the meeting request
        InviteComponent inviteComp = createInviteComponentSkeleton(subject, "room 101",
                organizerBox, attendeeBox);
        long soon = ((System.currentTimeMillis() / 1000) + 10) * 1000;  /* to nearest second + 10 */
        Date startDate = new Date(soon + Constants.MILLIS_PER_DAY);
        ZDateTime start = new ZDateTime(startDate.getTime(), false, organizerBox.getPrefs().getTimeZone());
        Date endDate = new Date(startDate.getTime() + Constants.MILLIS_PER_HOUR);
        ZDateTime end = new ZDateTime(endDate.getTime(), false, organizerBox.getPrefs().getTimeZone());
        Date newStartDate = new Date(soon + 2 * Constants.MILLIS_PER_DAY);
        ZDateTime newStart = new ZDateTime(newStartDate.getTime(), false, organizerBox.getPrefs().getTimeZone());
        Date newEndDate = new Date(newStartDate.getTime() + Constants.MILLIS_PER_HOUR);
        ZDateTime newEnd = new ZDateTime(newEndDate.getTime(), false, organizerBox.getPrefs().getTimeZone());
        inviteComp.setDtStart(DtTimeInfo.createForDatetimeAndZone(start.getDateTime(), start.getTimeZoneId()));
        inviteComp.setDtEnd(DtTimeInfo.createForDatetimeAndZone(end.getDateTime(), end.getTimeZoneId()));
        sendMeetingRequest(organizerBox,
                        createMsgForAppointmentRequest(subject, inviteComp, attendeeBox));

        waitForInvite(attendeeBox, subject);
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

    private static ZMessage sendInviteReplyAndWaitForIt(ZMailbox attendeeBox, ZMailbox organizerBox,
            String inviteMsgId, String subjectSuffix, String verb, DtTimeInfo exceptionId)
    throws Exception {
        SendInviteReplyRequest sirReq = new SendInviteReplyRequest(inviteMsgId, 0 /* componentNum */, verb);
        sirReq.setIdentityId(attendeeBox.getAccountInfo(false).getId());
        // ZWC 8.6 and earlier used to set this to false.  Now sets it to true.
        sirReq.setUpdateOrganizer(true);
        EmailAddrInfo attendeeAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                attendeeBox.getName(), getCN(attendeeBox), "f");
        EmailAddrInfo orgAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                organizerBox.getName(), organizerBox.getName(), "t");
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
        Msg msg = new Msg();
        msg.setReplyType("r");
        msg.setIdentityId(attendeeBox.getAccountInfo(false).getId());
        msg.addEmailAddress(orgAddr);
        msg.addEmailAddress(attendeeAddr);
        msg.setSubject(subject);
        msg.setMimePart(makeTextAndHtmlAlternatives(plainText));
        if (exceptionId != null) {
            sirReq.setExceptionId(exceptionId);
        }
        sirReq.setMsg(msg);
        SendInviteReplyResponse sirResp = attendeeBox.invokeJaxb(sirReq);
        Assert.assertNotNull("JAXB SendInviteReplyResponse object", sirResp);
        return waitForInvite(organizerBox, subject);
    }

    private static ZMessage acceptInvite(ZMailbox attendeeBox, ZMailbox organizerBox, ZMessage inviteMsg,
            String subject) throws Exception {
        return sendInviteReplyAndWaitForIt(attendeeBox, organizerBox, inviteMsg.getId(), subject, "ACCEPT", null);
    }

    /**
     * @param hit - From search response - represents the organizer's calendar copy
     * @throws ServiceException
     */
    private ZMessage organizerChangeTimeForMeeting(ZMailbox attendeeBox, ZMailbox organizerBox,
            ZDateTime newStart, ZDateTime newEnd, AppointmentHitInfo hit, String subject)
    throws Exception
    {
        InviteComponent inviteComp = createInviteComponentSkeleton(subject, "room 101",
                organizerBox, attendeeBox);
        inviteComp.setDtStart(DtTimeInfo.createForDatetimeAndZone(newStart.getDateTime(),
                newStart.getTimeZoneId()));
        inviteComp.setDtEnd(DtTimeInfo.createForDatetimeAndZone(newEnd.getDateTime(),
                newEnd.getTimeZoneId()));
        InvitationInfo invite = new InvitationInfo();
        invite.setUid(hit.getUid());
        invite.setInviteComponent(inviteComp);
        Msg msg = new Msg();
        msg.setFolderId(hit.getFolderId());
        msg.setInvite(invite);
        EmailAddrInfo attendeeAddr = EmailAddrInfo.createForAddressPersonalAndAddressType(
                attendeeBox.getName(), getCN(attendeeBox), "t");
        msg.addEmailAddress(attendeeAddr);
        msg.setSubject(subject);
        msg.setMimePart(makeTextAndHtmlAlternatives(
                String.format("The following meeting has been modified:\n\n%s",subject)));
        ModifyAppointmentRequest maReq = ModifyAppointmentRequest.createForIdModseqRevCompnumMsg(
                hit.getInvId(), hit.getModifiedSequence(), hit.getRevision(), hit.getComponentNum(), msg);
        ModifyAppointmentResponse maResp = organizerBox.invokeJaxb(maReq);
        Assert.assertNotNull("JAXB ModifyAppointmentResponse", maResp);
        return waitForInvite(attendeeBox, subject);
    }

    private List<AppointmentHitInfo> findMatchingExpandedAppointments(ZMailbox mbox,
        Date startDate, Date endDate, String search, int expected) throws ServiceException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setSortBy("none");
        searchRequest.setLimit(500);
        searchRequest.setLocale("en_US");
        searchRequest.setCalItemExpandStart(startDate.getTime() - 2 * 60 * 60 * 1000);
        searchRequest.setCalItemExpandEnd(endDate.getTime() + 2 * 60 * 60 * 1000);
        searchRequest.setSearchTypes("appointment");
        searchRequest.setOffset(0);
        searchRequest.setQuery(search);
        SearchResponse searchResp = mbox.invokeJaxb(searchRequest);
        Assert.assertNotNull("JAXB SearchResponse object", searchResp);
        List<SearchHit> hits = searchResp.getSearchHits();
        Assert.assertNotNull("JAXB SearchResponse hits", hits);
        Assert.assertEquals("JAXB SearchResponse hits", expected, hits.size());
        List<AppointmentHitInfo> apptHits = new ArrayList<>(hits.size());
        for (SearchHit hit : hits) {
            AppointmentHitInfo apptHit = (AppointmentHitInfo) hit;
            apptHits.add(apptHit);
        }
        return apptHits;
    }

    private AppointmentHitInfo findMatchingAppointment(ZMailbox mbox,
        Date startDate, Date endDate, String subject) throws ServiceException {
        List<AppointmentHitInfo> hits = findMatchingExpandedAppointments(mbox,
            startDate, endDate, subject, 1);
        AppointmentHitInfo hit = hits.get(0);
        Assert.assertEquals("Matching first instance start time",
                startDate.getTime(), hit.getInstances().get(0).getStartTime().longValue());
        return hits.get(0);
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
        String subject = "New Time Proposed: " + subjectSuffix;
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
        msg.setMimePart(makeTextAndHtmlAlternatives("New Time Proposed."));
        msg.setInvite(invite);
        CounterAppointmentResponse sirResp = attendeeBox.invokeJaxb(
             CounterAppointmentRequest.createForMsgModseqRevIdCompnum(
                 msg, hit.getModifiedSequence(), hit.getRevision(), hit.getInvId(), hit.getComponentNum()));
        Assert.assertNotNull("JAXB CounterAppointmentResponse object", sirResp);
        return waitForInvite(organizerBox, subject);
    }

    private void validateException(ZMailbox zmbox, InstanceDataInfo instance,
            Map<ZMailbox, String> partstats, GregorianCalendar recurIdTime) throws ServiceException {
        String exceptionInviteId = instance.getInvId();
        Assert.assertNotNull("exception invite id", exceptionInviteId);
        String exceptionRidZ = instance.getRecurIdZ();
        Assert.assertNotNull("exception invite RecurIdZ", exceptionRidZ);
        Assert.assertEquals("RecurIdZ",
                ParsedDateTime.fromUTCTime(recurIdTime.getTimeInMillis()).getDateTimePartString(false),
                exceptionRidZ);

        // Do a GetMsg for the exception in the Organizer's calendar
        MsgSpec gmeMsgSpec = new MsgSpec(exceptionInviteId);
        gmeMsgSpec.setRecurIdZ(exceptionRidZ);
        GetMsgRequest gmeReq = new GetMsgRequest(gmeMsgSpec);
        GetMsgResponse gmeResp = zmbox.invokeJaxb(gmeReq);
        List<InviteComponentWithGroupInfo> eInviteComps = gmeResp.getMsg().getInvite().getInviteComponents();
        Assert.assertEquals("number of components in exception", 1, eInviteComps.size());
        List<CalendarAttendeeWithGroupInfo> eAttendees = eInviteComps.get(0).getAttendees();
        Assert.assertEquals("number of attendees in exception", 2, eAttendees.size());
        for (CalendarAttendeeWithGroupInfo eAttendee : eAttendees) {
            String addr = eAttendee.getAddress();
            String ptst = eAttendee.getPartStat();
            boolean found = false;
            for (Entry<ZMailbox, String> entry : partstats.entrySet()) {
                if (addr.equals(entry.getKey().getName())) {
                    Assert.assertEquals(String.format("Exception partstat for %s", entry.getKey()),
                            entry.getValue(), ptst);
                    found = true;
                }
            }
            Assert.assertTrue(String.format("Unexpected attendee in exception [%s]", addr), found);
        }
    }

    /**
     * Bug 94018.  Accepting series, then declining single instance leads to inconsistent display of attendee
     * status for organizer copy for the declined instance.
     *
     * Pseudo exceptions should be created for each reply to an individual instance of the series.
     *
     * At the end of this, check that exceptions have been created.  Check that the exceptions register
     * the correct statuses for the users.
     */
    @Test
    public void acceptAndTentativeSeriesDeclineInstanceAcceptInstance()
    throws Exception {
        TestUtil.createAccount(ORGANIZER);
        TestUtil.createAccount(ATTENDEE1);
        TestUtil.createAccount(ATTENDEE2);
        String subject = NAME_PREFIX + " every 2 days";
        ZMailbox organizerZMbox = TestUtil.getZMailbox(ORGANIZER);
        ZMailbox attendee1ZMbox = TestUtil.getZMailbox(ATTENDEE1);
        ZMailbox attendee2ZMbox = TestUtil.getZMailbox(ATTENDEE2);

        String londonTZ = "Europe/London";
        int year = Calendar.getInstance().get(Calendar.YEAR) + 1;

        // Create and send meeting that occurs every 2 days
        InviteComponent inviteComp = createInviteComponentSkeleton(subject, "room 101",
            organizerZMbox, attendee1ZMbox, attendee2ZMbox);
        GregorianCalendar start = startTime(londonTZ, year, 1 /* February */, 2 /* day */, 14 /* hour */, 0);
        GregorianCalendar end = plus(start, Calendar.MINUTE, 30);
        setStartAndEnd(inviteComp, londonTZ, start, end);
        inviteComp.setRecurrence(
                recurrence("DAI", 2 /* every 2 days */, String.format("%d0529", year) /* until */));
        sendMeetingRequest(organizerZMbox,
            createMsgForAppointmentRequest(subject, inviteComp, attendee1ZMbox, attendee2ZMbox));

        // attendee 1 wait for original invite
        ZMessage seriesMsg = waitForInvite(attendee1ZMbox, subject);
        // attendee 1 accepts the original invite to the whole series
        sendInviteReplyAndWaitForIt(
                attendee1ZMbox, organizerZMbox, seriesMsg.getId(), subject, "ACCEPT", null);

        // attendee 1 declines one instance of the daily meeting - 2 + 4 --> 6th Feb
        GregorianCalendar exceptionTime = plus(start, Calendar.DAY_OF_MONTH, 4);
        sendInviteReplyAndWaitForIt(
                attendee1ZMbox, organizerZMbox, seriesMsg.getId(), subject, "DECLINE",
                DtTimeInfo.createForDatetimeAndZone(dateTime(exceptionTime), londonTZ));

        // attendee 2 wait for original invite
        seriesMsg = waitForInvite(attendee2ZMbox, subject);
        // attendee 2 tentatively accepts the original invite to the whole series
        sendInviteReplyAndWaitForIt(
                attendee2ZMbox, organizerZMbox, seriesMsg.getId(), subject, "TENTATIVE", null);

        // attendee 2 declines one instance of the daily meeting 2 + 8 --> 10th Feb
        GregorianCalendar exceptionTime2 = plus(start, Calendar.DAY_OF_MONTH, 8);
        sendInviteReplyAndWaitForIt(
                attendee2ZMbox, organizerZMbox, seriesMsg.getId(), subject + " " + ATTENDEE2, "DECLINE",
                DtTimeInfo.createForDatetimeAndZone(dateTime(exceptionTime2), londonTZ));

        // Search for a range of instances of the organizer's calendar entry
        SearchRequest sReq = new SearchRequest();
        sReq.setSearchTypes(ZSearchParams.TYPE_APPOINTMENT);
        /* 2nd Feb + 1 --> 3rd, 2nd Feb + 13 --> 15th.  So should match instances on:
         *     4th, 6th, 8th, 10th, 12th, 14th. */
        List<AppointmentHitInfo> apptHits = findMatchingExpandedAppointments(organizerZMbox,
                plus(start, Calendar.DAY_OF_MONTH, 1).getTime(),
                plus(start, Calendar.DAY_OF_MONTH, 13).getTime(),
                String.format("in:Calendar and subject:%s", subject), 1);
        AppointmentHitInfo orgApptHit = apptHits.get(0);
        Assert.assertNotNull("Organizer Calendar at end - series invite id", orgApptHit.getInvId());
        List<InstanceDataInfo> instances = orgApptHit.getInstances();
        Assert.assertNotNull("Organizer Calendar at end - instances in expansion", instances);
        Assert.assertEquals("Organizer Calendar at end - number of instances in expansion",
                6, instances.size());

        // The 2nd entry in the list should be for an exception
        Map<ZMailbox,String> partstatMap = new HashMap<>(2);
        partstatMap.put(attendee1ZMbox, ZAppointmentHit.PSTATUS_DECLINED);   /* for this instance */
        partstatMap.put(attendee2ZMbox, ZAppointmentHit.PSTATUS_TENTATIVE);  /* for the series */
        validateException(organizerZMbox, instances.get(1), partstatMap, exceptionTime);

        // The 4th entry in the list should also be for an exception
        partstatMap.put(attendee1ZMbox, ZAppointmentHit.PSTATUS_ACCEPT);    /* for the series */
        partstatMap.put(attendee2ZMbox, ZAppointmentHit.PSTATUS_DECLINED);  /* for the instance */
        validateException(organizerZMbox, instances.get(3), partstatMap, exceptionTime2);

        // Do a GetMsg for the series in the Organizer's calendar
        MsgSpec gmsMsgSpec = new MsgSpec(orgApptHit.getInvId());
        GetMsgRequest gmsReq = new GetMsgRequest(gmsMsgSpec);
        GetMsgResponse gmsResp = organizerZMbox.invokeJaxb(gmsReq);
        List<InviteComponentWithGroupInfo> sInviteComps = gmsResp.getMsg().getInvite().getInviteComponents();
        Assert.assertEquals("number of components in series", 1, sInviteComps.size());
        List<CalendarAttendeeWithGroupInfo> sAttendees = sInviteComps.get(0).getAttendees();
        Assert.assertEquals("number of attendees in exception", 2, sAttendees.size());
        for (CalendarAttendeeWithGroupInfo sAttendee : sAttendees) {
            String addr = sAttendee.getAddress();
            String ptst = sAttendee.getPartStat();
            if (addr.equals(attendee1ZMbox.getName())) {
                Assert.assertEquals("exception attendee1 partstat", ZAppointmentHit.PSTATUS_ACCEPT, ptst);
            } else if (addr.equals(attendee2ZMbox.getName())) {
                Assert.assertEquals("exception attendee2 partstat", ZAppointmentHit.PSTATUS_TENTATIVE, ptst);
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
            Assert.fail(String.format("Failed to convert string %s to long - exception %s",
                    ymdString, e.getMessage()));
        }
        return 0;
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
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(TestUtil.getSoapUrl() + requestCommand);
        post.setEntity(new StringEntity(requestBody, "application/soap+xml", "UTF-8"));
        HttpResponse response = HttpClientUtil.executeMethod(client, post);
        int respCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals(200, respCode);
        Element envelope = W3cDomUtil.parseXML(response.getEntity().getContent());
        SoapProtocol proto = SoapProtocol.determineProtocol(envelope);
        Element doc = proto.getBodyElement(envelope);
        return JaxbUtil.elementToJaxb(doc);
    }

    private Element sendReqExpectingFail(String requestBody, String requestCommand, int expectedRespCode)
    throws HttpException, IOException, ServiceException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(TestUtil.getSoapUrl() + requestCommand);
        post.setEntity(new StringEntity(requestBody, "application/soap+xml", "UTF-8"));
        HttpResponse response = HttpClientUtil.executeMethod(client, post);
        int respCode = response.getStatusLine().getStatusCode();
        Assert.assertEquals(expectedRespCode, respCode);
        return W3cDomUtil.parseXML(response.getEntity().getContent());
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
}
