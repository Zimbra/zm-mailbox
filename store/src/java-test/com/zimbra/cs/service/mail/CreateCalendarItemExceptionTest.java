/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
package com.zimbra.cs.service.mail;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.ScheduledTaskManager;
import com.zimbra.cs.mailbox.calendar.Util;
import com.zimbra.soap.ZimbraSoapContext;

import junit.framework.Assert;

public class CreateCalendarItemExceptionTest {
    private Account account;
    private LocalDateTime now;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_mail, "test@zimbra.com");
        prov.createAccount("test@zimbra.com", "secret", attrs);
        ScheduledTaskManager.startup();
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        account = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        now = LocalDateTime.now();
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateApptExceptionWithMappedColor() throws Exception {
        Element createAppointmentRequest = generateCreateAppointmentRequest(null, null);
        CreateCalendarItem cci = new CreateCalendarItem() {
            protected Element getResponseElement(ZimbraSoapContext zsc) {
                return new Element.JSONElement("CreateAppointmentResponse");
            }
        };
        Map<String, Object> context = ServiceTestUtil.getRequestContext(account);
        Element createAppointmentResponse = cci.handle(createAppointmentRequest, context);
        String id = createAppointmentResponse.getAttribute(MailConstants.A_CAL_ID, "empty");
        String invId = createAppointmentResponse.getAttribute(MailConstants.A_CAL_INV_ID, "empty");
        Assert.assertFalse("empty".equals(id));
        Assert.assertFalse("empty".equals(invId));
        Element getMsgRequest = generateGetMsgRequest(invId);
        Element getMsgResponse = new GetMsg().handle(getMsgRequest, context);
        String ms = getMsgResponse.getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_MODIFIED_SEQUENCE, "empty");
        String rev = getMsgResponse.getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_REVISION, "empty");
        String uid = getMsgResponse.getElement(MailConstants.E_MSG).getElement(MailConstants.E_INVITE)
                        .getElement(MailConstants.E_INVITE_COMPONENT).getAttribute(MailConstants.A_UID, "empty");
        Assert.assertFalse("empty".equals(uid));
        Assert.assertFalse("empty".equals(ms));
        Assert.assertFalse("empty".equals(rev));
        // Change color from None to Blue
        Element createAppointmentExceptionRequest = generateCreateAppointmentExceptionRequest(invId, ms, rev, uid, "1", null);
        CreateCalendarItemException ccie = new CreateCalendarItemException() {
            protected Element getResponseElement(ZimbraSoapContext zsc) {
                return new Element.JSONElement("CreateAppointmentExceptionResponse");
            }
        };
        Element createAppointmentExceptionResponse = ccie.handle(createAppointmentExceptionRequest, context);
        String id_2 = createAppointmentExceptionResponse.getAttribute(MailConstants.A_CAL_ID, "empty");
        String invId_2 = createAppointmentExceptionResponse.getAttribute(MailConstants.A_CAL_INV_ID, "empty");
        Assert.assertEquals(id, id_2);
        Assert.assertFalse(invId.equals(invId_2));
        getMsgRequest = generateGetMsgRequest(invId_2);
        getMsgResponse = new GetMsg().handle(getMsgRequest, context);
        Element comp = getMsgResponse.getElement(MailConstants.E_MSG).getElement(MailConstants.E_INVITE)
                .getElement(MailConstants.A_CAL_COMP);
        uid = comp.getAttribute(MailConstants.A_UID, "empty");
        Assert.assertFalse("empty".equals(uid));
        String color = comp.getAttribute(MailConstants.A_COLOR, "empty");
        String rgb = comp.getAttribute(MailConstants.A_RGB, "empty").toLowerCase();
        Assert.assertEquals("1", color);
        Assert.assertEquals("empty", rgb);
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testCreateApptExceptionWithCustomColor() throws Exception {
        Element createAppointmentRequest = generateCreateAppointmentRequest(null, null);
        CreateCalendarItem cci = new CreateCalendarItem() {
            protected Element getResponseElement(ZimbraSoapContext zsc) {
                return new Element.JSONElement("CreateAppointmentResponse");
            }
        };
        Map<String, Object> context = ServiceTestUtil.getRequestContext(account);
        Element createAppointmentResponse = cci.handle(createAppointmentRequest, context);
        String id = createAppointmentResponse.getAttribute(MailConstants.A_CAL_ID, "empty");
        String invId = createAppointmentResponse.getAttribute(MailConstants.A_CAL_INV_ID, "empty");
        Assert.assertFalse("empty".equals(id));
        Assert.assertFalse("empty".equals(invId));
        Element getMsgRequest = generateGetMsgRequest(invId);
        Element getMsgResponse = new GetMsg().handle(getMsgRequest, context);
        String ms = getMsgResponse.getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_MODIFIED_SEQUENCE, "empty");
        String rev = getMsgResponse.getElement(MailConstants.E_MSG).getAttribute(MailConstants.A_REVISION, "empty");
        String uid = getMsgResponse.getElement(MailConstants.E_MSG).getElement(MailConstants.E_INVITE)
                        .getElement(MailConstants.E_INVITE_COMPONENT).getAttribute(MailConstants.A_UID, "empty");
        Assert.assertFalse("empty".equals(uid));
        Assert.assertFalse("empty".equals(ms));
        Assert.assertFalse("empty".equals(rev));
        // Change color from None to Blue
        Element createAppointmentExceptionRequest = generateCreateAppointmentExceptionRequest(invId, ms, rev, uid, null, "#1a2bfc");
        CreateCalendarItemException ccie = new CreateCalendarItemException() {
            protected Element getResponseElement(ZimbraSoapContext zsc) {
                return new Element.JSONElement("CreateAppointmentExceptionResponse");
            }
        };
        Element createAppointmentExceptionResponse = ccie.handle(createAppointmentExceptionRequest, context);
        String id_2 = createAppointmentExceptionResponse.getAttribute(MailConstants.A_CAL_ID, "empty");
        String invId_2 = createAppointmentExceptionResponse.getAttribute(MailConstants.A_CAL_INV_ID, "empty");
        Assert.assertEquals(id, id_2);
        Assert.assertFalse(invId.equals(invId_2));
        getMsgRequest = generateGetMsgRequest(invId_2);
        getMsgResponse = new GetMsg().handle(getMsgRequest, context);
        Element comp = getMsgResponse.getElement(MailConstants.E_MSG).getElement(MailConstants.E_INVITE)
                .getElement(MailConstants.A_CAL_COMP);
        uid = comp.getAttribute(MailConstants.A_UID, "empty");
        Assert.assertFalse("empty".equals(uid));
        String color = comp.getAttribute(MailConstants.A_COLOR, "empty");
        String rgb = comp.getAttribute(MailConstants.A_RGB, "empty").toLowerCase();
        Assert.assertEquals("empty", color);
        Assert.assertEquals("#1a2bfc", rgb);
    }

    private Element generateCreateAppointmentRequest(String color, String rgb) throws Exception {
        // CreateAppointmentRequest
        Element request = new Element.JSONElement(MailConstants.CREATE_APPOINTMENT_REQUEST);
        Element m = request.addUniqueElement(MailConstants.E_MSG)
                    .addAttribute(MailConstants.A_SUBJECT, "test-appt")
                    .addAttribute(MailConstants.A_FOLDER, Mailbox.ID_FOLDER_CALENDAR);
        //m.addUniqueElement(MailConstants.E_EMAIL);
        Element comp = m.addUniqueElement(MailConstants.E_INVITE)
                        .addNonUniqueElement(MailConstants.E_INVITE_COMPONENT);
        comp.addNonUniqueElement(MailConstants.E_CAL_ALARM)
            .addAttribute(MailConstants.A_ACTION, "DISPLAY")
            .addUniqueElement(MailConstants.E_CAL_ALARM_TRIGGER)
                .addUniqueElement(MailConstants.E_CAL_ALARM_RELATIVE)
                    .addAttribute(MailConstants.A_CAL_DURATION_MINUTES, 5)
                    .addAttribute(MailConstants.A_CAL_DURATION_NEGATIVE, "1")
                    .addAttribute(MailConstants.A_CAL_ALARM_RELATED, "START");
        comp.addAttribute(MailConstants.A_CAL_ALLDAY, "0")
            .addAttribute(MailConstants.A_CAL_CLASS, "PUB")
            .addAttribute(MailConstants.A_CAL_DRAFT, "0")
            .addAttribute(MailConstants.A_APPT_FREEBUSY, "B")
            .addAttribute(MailConstants.A_CAL_LOCATION, "")
            .addAttribute(MailConstants.A_NAME, "appt-test")
            .addAttribute(MailConstants.A_CAL_STATUS, "CONF")
            .addAttribute(MailConstants.A_APPT_TRANSPARENCY, "0");
        TimeZoneMap tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        ParsedDateTime dtStart = ParsedDateTime.parse(now.toString(), tzMap, null, tzMap.getLocalTimeZone());
        ParsedDateTime dtEnd = ParsedDateTime.parse(now.plusMinutes(30).toString(), tzMap, null, tzMap.getLocalTimeZone());
        String timeZone = account.getAttr(Provisioning.A_zimbraPrefTimeZoneId);
        comp.addUniqueElement(MailConstants.E_CAL_END_TIME)
            .addAttribute(MailConstants.A_CAL_DATETIME, dtEnd.getDateTimePartString())
            .addAttribute(MailConstants.A_CAL_TIMEZONE, timeZone);
        comp.addUniqueElement(MailConstants.E_CAL_START_TIME)
            .addAttribute(MailConstants.A_CAL_DATETIME, dtStart.getDateTimePartString())
            .addAttribute(MailConstants.A_CAL_TIMEZONE, timeZone);
        comp.addUniqueElement(MailConstants.E_CAL_ORGANIZER)
            .addAttribute(MailConstants.A_ADDRESS, "test@zimbra.com");
        Element rule = new Element.JSONElement(MailConstants.E_CAL_RULE);
        rule.addAttribute(MailConstants.A_CAL_RULE_FREQ, "DAI")
            .addUniqueElement(MailConstants.A_COUNT).addAttribute(MailConstants.A_CAL_RULE_COUNT_NUM, "5");
        rule.addUniqueElement(MailConstants.E_CAL_RULE_INTERVAL).addAttribute(MailConstants.A_CAL_RULE_INTERVAL_IVAL, 1);
        comp.addUniqueElement(MailConstants.E_CAL_RECUR)
            .addUniqueElement(MailConstants.E_CAL_ADD)
                .addUniqueElement(rule);
        if (color != null) {
            comp.addAttribute(MailConstants.A_COLOR, color);
        }
        if (rgb != null) {
            comp.addAttribute(MailConstants.A_RGB, rgb);
        }
        //inv.addNonUniqueElement(MailConstants.A_CAL_ATTENDEE);
        m.addUniqueElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "multipart/alternative")
            .addNonUniqueElement(MailConstants.E_MIMEPART)
                .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
                .addAttribute(MailConstants.E_CAL_CONTENT, "");
        return request;
    }

    private Element generateGetMsgRequest(String id) throws Exception {
        // GetMsgRequest
        Element request = new Element.JSONElement(MailConstants.GET_MSG_REQUEST);
        request.addUniqueElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ID, id);
        return request;
    }

    private Element generateCreateAppointmentExceptionRequest(String id, String ms, String rev, String uid, String color, String rgb) throws Exception {
        // CreateAppointmentRequest
        Element request = new Element.JSONElement(MailConstants.CREATE_APPOINTMENT_EXCEPTION_REQUEST);
        request.addAttribute(MailConstants.A_CAL_COMP, "0")
            .addAttribute(MailConstants.A_ID, id)
            .addAttribute(MailConstants.A_MODIFIED_SEQUENCE, ms)
            .addAttribute(MailConstants.A_REVISION, rev);
        Element m = request.addUniqueElement(MailConstants.E_MSG)
                    .addAttribute(MailConstants.A_SUBJECT, "test-appt")
                    .addAttribute(MailConstants.A_FOLDER, Mailbox.ID_FOLDER_CALENDAR);
        //m.addUniqueElement(MailConstants.E_EMAIL);
        Element comp = m.addUniqueElement(MailConstants.E_INVITE)
                .addAttribute(MailConstants.A_UID, uid)
                .addNonUniqueElement(MailConstants.E_INVITE_COMPONENT);
        comp.addNonUniqueElement(MailConstants.E_CAL_ALARM)
            .addAttribute(MailConstants.A_ACTION, "DISPLAY")
            .addUniqueElement(MailConstants.E_CAL_ALARM_TRIGGER)
                .addUniqueElement(MailConstants.E_CAL_ALARM_RELATIVE)
                    .addAttribute(MailConstants.A_CAL_DURATION_MINUTES, 5)
                    .addAttribute(MailConstants.A_CAL_DURATION_NEGATIVE, "1")
                    .addAttribute(MailConstants.A_CAL_ALARM_RELATED, "START");
        comp.addAttribute(MailConstants.A_CAL_ALLDAY, "0")
            .addAttribute(MailConstants.A_CAL_CLASS, "PUB")
            .addAttribute(MailConstants.A_CAL_DRAFT, "0")
            .addAttribute(MailConstants.A_APPT_FREEBUSY, "B")
            .addAttribute(MailConstants.A_CAL_LOCATION, "")
            .addAttribute(MailConstants.A_NAME, "appt-test")
            .addAttribute(MailConstants.A_CAL_STATUS, "CONF")
            .addAttribute(MailConstants.A_APPT_TRANSPARENCY, "0");
        TimeZoneMap tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        ParsedDateTime dtStart = ParsedDateTime.parse(now.plusDays(1).toString(), tzMap, null, tzMap.getLocalTimeZone());
        ParsedDateTime dtEnd = ParsedDateTime.parse(now.plusDays(1).plusMinutes(30).toString(), tzMap, null, tzMap.getLocalTimeZone());
        String timeZone = account.getAttr(Provisioning.A_zimbraPrefTimeZoneId);
        comp.addUniqueElement(MailConstants.E_CAL_END_TIME)
            .addAttribute(MailConstants.A_CAL_DATETIME, dtEnd.getDateTimePartString())
            .addAttribute(MailConstants.A_CAL_TIMEZONE, timeZone);
        comp.addUniqueElement(MailConstants.E_CAL_START_TIME)
            .addAttribute(MailConstants.A_CAL_DATETIME, dtStart.getDateTimePartString())
            .addAttribute(MailConstants.A_CAL_TIMEZONE, timeZone);
        comp.addUniqueElement(MailConstants.E_CAL_EXCEPTION_ID)
            .addAttribute(MailConstants.A_CAL_DATETIME, dtStart.getDateTimePartString())
            .addAttribute(MailConstants.A_CAL_TIMEZONE, timeZone);
        comp.addUniqueElement(MailConstants.E_CAL_ORGANIZER)
            .addAttribute(MailConstants.A_ADDRESS, "test@zimbra.com");
        if (color != null) {
            comp.addAttribute(MailConstants.A_COLOR, color);
        }
        if (rgb != null) {
            comp.addAttribute(MailConstants.A_RGB, rgb);
        }
        //inv.addNonUniqueElement(MailConstants.A_CAL_ATTENDEE);
        m.addUniqueElement(MailConstants.E_MIMEPART)
            .addAttribute(MailConstants.A_CONTENT_TYPE, "multipart/alternative")
            .addNonUniqueElement(MailConstants.E_MIMEPART)
                .addAttribute(MailConstants.A_CONTENT_TYPE, "text/plain")
                .addAttribute(MailConstants.E_CAL_CONTENT, "");
        return request;
    }
}
