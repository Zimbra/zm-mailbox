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
import java.time.ZoneId;
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
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.ScheduledTaskManager;
import com.zimbra.cs.mailbox.calendar.Util;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

import junit.framework.Assert;

public class ModifyCalendarItemTest {
    private Account account;
    LocalDateTime now;
    Mailbox mbox;

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
        //MailboxTestUtil.clearData();
        account = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        now = LocalDateTime.now();
        MailboxTestUtil.cleanupIndexStore(mbox);
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testChangeApptColor() throws Exception {
        Element createAppointmentRequest = generateCreateAppointmentRequest(null, null);
        CreateCalendarItem cci = new CreateCalendarItem() {
            protected Element getResponseElement(ZimbraSoapContext zsc) {
                return new Element.JSONElement("CreateAppointmentResponse");
            }
        };
        Map<String, Object> context = ServiceTestUtil.getRequestContext(account);
        Element createAppointmentResponse = cci.handle(createAppointmentRequest, context);
        String invId = createAppointmentResponse.getAttribute(MailConstants.A_CAL_INV_ID, "empty");
        String ms = createAppointmentResponse.getAttribute(MailConstants.A_MODIFIED_SEQUENCE, "empty");
        String rev = createAppointmentResponse.getAttribute(MailConstants.A_REVISION, "empty");
        Assert.assertFalse(invId.equals("empty"));
        Assert.assertFalse(ms.equals("empty"));
        Assert.assertFalse(rev.equals("empty"));
        Element getMsgRequest = generateGetMsgRequest(invId);
        Element getMsgResponse = new GetMsg().handle(getMsgRequest, context);
        String uid = getMsgResponse.getElement(MailConstants.E_MSG).getElement(MailConstants.E_INVITE)
            .getElement(MailConstants.A_CAL_COMP).getAttribute(MailConstants.A_UID, "empty");
        Assert.assertFalse(uid.equals("empty"));
        Element comp = getMsgResponse.getElement(MailConstants.E_MSG).getElement(MailConstants.E_INVITE)
                .getElement(MailConstants.A_CAL_COMP);
        String color = comp.getAttribute(MailConstants.A_COLOR, "empty");
        String rgb = comp.getAttribute(MailConstants.A_RGB, "empty").toLowerCase();
        Assert.assertEquals("empty", color);
        Assert.assertEquals("empty", rgb);

        // Change to mapped color
        Element modifyAppointmentRequest = generateModifyAppointmentRequest(invId, ms, rev, uid, "1", null);
        ModifyCalendarItem mci = new ModifyCalendarItem() {
            protected Element getResponseElement(ZimbraSoapContext zsc) {
                return new Element.JSONElement("ModifyAppointmentResponse");
            }
        };
        Element modifyAppointmentResponse = mci.handle(modifyAppointmentRequest, context);
        String invId_2 = modifyAppointmentResponse.getAttribute(MailConstants.A_CAL_INV_ID, "empty");
        Assert.assertEquals(invId, invId_2);
        ms = modifyAppointmentResponse.getAttribute(MailConstants.A_MODIFIED_SEQUENCE, "empty");
        rev = modifyAppointmentResponse.getAttribute(MailConstants.A_REVISION, "empty");
        getMsgRequest = generateGetMsgRequest(invId);
        getMsgResponse = new GetMsg().handle(getMsgRequest, context);
        comp = getMsgResponse.getElement(MailConstants.E_MSG).getElement(MailConstants.E_INVITE)
                .getElement(MailConstants.A_CAL_COMP);
        color = comp.getAttribute(MailConstants.A_COLOR, "empty");
        rgb = comp.getAttribute(MailConstants.A_RGB, "empty").toLowerCase();
        Assert.assertEquals("1", color);
        Assert.assertEquals("empty", rgb);

        // Change to custom color
        modifyAppointmentRequest = generateModifyAppointmentRequest(invId, ms, rev, uid, null, "#f1a23d");
        modifyAppointmentResponse = mci.handle(modifyAppointmentRequest, context);
        invId_2 = modifyAppointmentResponse.getAttribute(MailConstants.A_CAL_INV_ID, "empty");
        Assert.assertEquals(invId, invId_2);
        ms = modifyAppointmentResponse.getAttribute(MailConstants.A_MODIFIED_SEQUENCE, "empty");
        rev = modifyAppointmentResponse.getAttribute(MailConstants.A_REVISION, "empty");
        getMsgRequest = generateGetMsgRequest(invId);
        getMsgResponse = new GetMsg().handle(getMsgRequest, context);
        comp = getMsgResponse.getElement(MailConstants.E_MSG).getElement(MailConstants.E_INVITE)
                .getElement(MailConstants.A_CAL_COMP);
        color = comp.getAttribute(MailConstants.A_COLOR, "empty");
        rgb = comp.getAttribute(MailConstants.A_RGB, "empty").toLowerCase();
        Assert.assertEquals("empty", color);
        Assert.assertEquals("#f1a23d", rgb);
    }

    private Element generateCreateAppointmentRequest(String color, String rgb) throws Exception {
        Element request = new Element.JSONElement(MailConstants.CREATE_APPOINTMENT_REQUEST);
        generateAdditionalElements(request, null, color, rgb);
        return request;
    }

    private Element generateModifyAppointmentRequest(String id, String ms, String rev, String uid, String color, String rgb) throws Exception {
        Element request = new Element.JSONElement(MailConstants.MODIFY_APPOINTMENT_REQUEST);
        request.addAttribute(MailConstants.A_CAL_COMP, "0")
            .addAttribute(MailConstants.A_ID, id)
            .addAttribute(MailConstants.A_MODIFIED_SEQUENCE, ms)
            .addAttribute(MailConstants.A_REVISION, rev);
        generateAdditionalElements(request, uid, color, rgb);
        return request;
    }

    private Element generateAdditionalElements(Element parent, String uid, String color, String rgb) throws Exception {
        Element m = parent.addUniqueElement(MailConstants.E_MSG)
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
        return m;
    }

    private Element generateGetMsgRequest(String id) throws Exception {
        Element request = new Element.JSONElement(MailConstants.GET_MSG_REQUEST);
        request.addUniqueElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ID, id);
        return request;
    }
}
