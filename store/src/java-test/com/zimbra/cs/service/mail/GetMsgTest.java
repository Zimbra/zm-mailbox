/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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

import static org.junit.Assert.assertTrue;
import org.junit.Ignore;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.AddInviteData;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.message.CreateMountpointRequest;
import com.zimbra.soap.mail.message.FolderActionRequest;
import com.zimbra.soap.mail.message.GetMsgRequest;
import com.zimbra.soap.mail.type.FolderActionSelector;
import com.zimbra.soap.mail.type.MsgSpec;
import com.zimbra.soap.mail.type.NewMountpointSpec;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class GetMsgTest {
    private static final String desc = "The following is a new meeting request";
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {

        MailboxTestUtil.initServer();
        MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test2@zimbra.com", "secret", attrs);

        // this MailboxManager does everything except actually send mail
        MailboxManager.setInstance(new MailboxManager() {
            @Override
            protected Mailbox instantiateMailbox(MailboxData data) {
                return new Mailbox(data) {
                    @Override
                    public MailSender getMailSender() {
                        return new MailSender() {
                            @Override
                            protected Collection<Address> sendMessage(Mailbox mbox, MimeMessage mm,
                                    Collection<RollbackData> rollbacks) {
                                try {
                                    return Arrays.asList(getRecipients(mm));
                                } catch (Exception e) {
                                    return Collections.emptyList();
                                }
                            }
                        };
                    }
                };
            }
        });
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testHandle() throws Exception {

        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");

        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);

        Folder calendarFolder = mbox1.getCalendarFolders(null, SortBy.NONE).get(0);
        String fragment = "Some message";
        ZVCalendar calendar = new ZVCalendar();
        calendar.addDescription(desc, null);
        ZComponent comp = new ZComponent("VEVENT");
        calendar.addComponent(comp);

        Invite invite = MailboxTestUtil.generateInvite(acct1, fragment, calendar);
        ICalTimeZone ical = invite.getTimeZoneMap().getLocalTimeZone();
        long utc = 5 * 60 * 60 * 1000;
        ParsedDateTime s = ParsedDateTime.fromUTCTime(System.currentTimeMillis() + utc, ical);
        ParsedDateTime e = ParsedDateTime.fromUTCTime(System.currentTimeMillis() + (30 * 60 * 1000) + utc, ical);
        invite.setDtStart(s);
        invite.setDtEnd(e);
        invite.setPriority("5");
        invite.setClassProp("PRI");
        invite.setOrganizer(new ZOrganizer("test@zimbra.com", null));
        invite.setUid(UUID.randomUUID().toString());
        invite.setMethod("REQUEST");
        invite.setName("Testing");
        invite.setFreeBusy("B");
        invite.setIsOrganizer(true);
        invite.setItemType(MailItem.Type.APPOINTMENT);
        invite.setUid(UUID.randomUUID().toString());

        AddInviteData inviteData = mbox1.addInvite(null, invite, calendarFolder.getId());
        calendarFolder = mbox1.getCalendarFolders(null, SortBy.NONE).get(0);

        MsgSpec msgSpec = new MsgSpec(acct1.getId() + ":" + inviteData.calItemId + "-" + inviteData.invId);
        msgSpec.setWantHtml(true);
        msgSpec.setNeedCanExpand(true);
        GetMsgRequest getMsgReq = new GetMsgRequest(msgSpec);
        Element response = new GetMsg().handle(JaxbUtil.jaxbToElement(getMsgReq), ServiceTestUtil.getRequestContext(acct1));
        Element organizer = response.getElement("m").getElement("inv").getElement("comp").getElement("or");
        String organizerString = organizer.prettyPrint();
        assertTrue(organizerString.contains("a=\"test@zimbra.com\" url=\"test@zimbra.com\""));

        mbox1.grantAccess(null, 10, acct2.getId(), ACL.GRANTEE_USER, ACL.RIGHT_READ, null);

        NewMountpointSpec  mpSpec = new NewMountpointSpec("sharedcal");
        mpSpec.setFlags("#");
        mpSpec.setDefaultView("appoinment");
        mpSpec.setFolderId("10");
        mpSpec.setOwnerName("test@zimbra.com");
        mpSpec.setReminderEnabled(false);
        mpSpec.setPath("/Calendar");
        CreateMountpointRequest createMpReq = new CreateMountpointRequest(mpSpec);

        response = new CreateMountpoint().handle(JaxbUtil.jaxbToElement(createMpReq), ServiceTestUtil.getRequestContext(acct2));
        String mptId = response.getElement("link").getAttribute("id");

        msgSpec = new MsgSpec(acct1.getId() + ":" + inviteData.calItemId + "-" + mptId);
        msgSpec.setWantHtml(true);
        msgSpec.setNeedCanExpand(true);
        getMsgReq = new GetMsgRequest(msgSpec);
        response = new GetMsg().handle(JaxbUtil.jaxbToElement(getMsgReq), ServiceTestUtil.getRequestContext(acct2, acct1));

        organizerString = response.getElement("m").prettyPrint();
        assertTrue(!organizerString.contains("a=\"test@zimbra.com\" url=\"test@zimbra.com\""));

        FolderActionSelector action = new FolderActionSelector(mptId, "delete");
        FolderActionRequest folderActionReq = new FolderActionRequest(action);

        response = new FolderAction().handle(JaxbUtil.jaxbToElement(folderActionReq), ServiceTestUtil.getRequestContext(acct2));
        mbox1.revokeAccess(null, 10, acct2.getId());
    }
}
