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
import java.util.Date;

import junit.framework.TestCase;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.common.account.ZAttrProvisioning;
import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.account.message.ModifyPrefsRequest;
import com.zimbra.soap.account.message.ModifyPrefsResponse;
import com.zimbra.soap.account.type.Pref;
import com.zimbra.soap.mail.message.CreateMountpointRequest;
import com.zimbra.soap.mail.message.CreateMountpointResponse;
import com.zimbra.soap.mail.message.FolderActionRequest;
import com.zimbra.soap.mail.message.FolderActionResponse;
import com.zimbra.soap.mail.message.GetMsgRequest;
import com.zimbra.soap.mail.message.GetMsgResponse;
import com.zimbra.soap.mail.type.ActionGrantSelector;
import com.zimbra.soap.mail.type.FolderActionSelector;
import com.zimbra.soap.mail.type.InviteWithGroupInfo;
import com.zimbra.soap.mail.type.MsgSpec;
import com.zimbra.soap.mail.type.MsgWithGroupInfo;
import com.zimbra.soap.mail.type.NewMountpointSpec;

public class TestInvite extends TestCase {

    private SoapProvisioning prov = null;
    static final boolean runningOutsideZimbra = false;  // set to true if running inside an IDE instead of from RunUnitTests
    private static String NAME_PREFIX = "TestInvite";
    private static String USER_NAME = "user1";
    private static String ORGANIZER = "tiorganizer";
    private static String ATTENDEE = "tiattendee";
    private static String DELEGATE = "tidelegate";

    public void testBug86864DelegateInboxInviteLooksLikeInvite() throws Exception {
        TestUtil.createAccount(ORGANIZER);
        Account attendee = TestUtil.createAccount(ATTENDEE);
        Account delegate = TestUtil.createAccount(DELEGATE);
        ZMailbox mboxOrganizer = TestUtil.getZMailbox(ORGANIZER);
        ZMailbox mboxAttendee = TestUtil.getZMailbox(ATTENDEE);
        ZMailbox mboxDelegate = TestUtil.getZMailbox(DELEGATE);
        String subject = NAME_PREFIX + " for testing treatment at delegate inbox";
        Date startDate = new Date(System.currentTimeMillis() + Constants.MILLIS_PER_DAY);
        Date endDate = new Date(startDate.getTime() + Constants.MILLIS_PER_HOUR);

        String  calendarId = Integer.valueOf(Mailbox.ID_FOLDER_CALENDAR).toString();
        FolderActionSelector action = new FolderActionSelector(calendarId /* id */, "grant");
        ActionGrantSelector grant = new ActionGrantSelector("rwidxa" /* perm */, "usr" /* gt */);
        grant.setDisplayName(delegate.getName());
        grant.setPassword("");
        action.setGrant(grant);
        FolderActionRequest folderActionReq = new FolderActionRequest(action);
        FolderActionResponse folderActionResp = mboxAttendee.invokeJaxb(folderActionReq);
        assertNotNull("null FolderAction Response used to share folder", folderActionResp);

        ModifyPrefsRequest modPrefsReq = new ModifyPrefsRequest();
        Pref fwdInvitesPref = Pref.createPrefWithNameAndValue(
                ZAttrProvisioning.A_zimbraPrefCalendarForwardInvitesTo, delegate.getName());
        modPrefsReq.addPref(fwdInvitesPref);
        Pref noAutoAddPref = Pref.createPrefWithNameAndValue(
                ZAttrProvisioning.A_zimbraPrefCalendarAutoAddInvites, Boolean.FALSE.toString().toUpperCase());
        modPrefsReq.addPref(noAutoAddPref);
        ModifyPrefsResponse modPrefsResp = mboxAttendee.invokeJaxb(modPrefsReq);
        assertNotNull("null ModifyPrefs Response for forwarding calendar invites/no auto-add", modPrefsResp);

        NewMountpointSpec  mpSpec = new NewMountpointSpec("Shared Calendar");
        mpSpec.setFlags("#");
        mpSpec.setRemoteId(Mailbox.ID_FOLDER_CALENDAR);
        mpSpec.setColor((byte) 4);
        mpSpec.setOwnerId(attendee.getId());
        mpSpec.setFolderId(Integer.valueOf(Mailbox.ID_FOLDER_USER_ROOT).toString());
        mpSpec.setDefaultView(MailItem.Type.APPOINTMENT.toString());
        CreateMountpointRequest createMpReq = new CreateMountpointRequest(mpSpec);
        CreateMountpointResponse createMpResp = mboxDelegate.invokeJaxb(createMpReq);
        assertNotNull("null ModifyPrefs Response for forwarding calendar invites", createMpResp);

        TestUtil.createAppointment(mboxOrganizer, subject, attendee.getName(), startDate, endDate);
        ZMessage inviteMsg = TestUtil.waitForMessage(mboxDelegate, "in:inbox " + subject);
        assertNotNull("null inviteMsg in delegate inbox", inviteMsg);

        MsgSpec msgSpec = new MsgSpec(inviteMsg.getId());
        msgSpec.setWantHtml(true);
        msgSpec.setNeedCanExpand(true);
        msgSpec.setMaxInlinedLength(250000);
        GetMsgRequest getMsgReq = new GetMsgRequest(msgSpec);
        GetMsgResponse getMsgResp = mboxDelegate.invokeJaxb(getMsgReq);
        assertNotNull("null GetMsgResponse in delegate inbox", getMsgResp);
        MsgWithGroupInfo msg = getMsgResp.getMsg();
        assertNotNull("null message in GetMsgResponse in delegate inbox", msg);
        InviteWithGroupInfo invite = msg.getInvite();
        assertNotNull("null invite in message in GetMsgResponse in delegate inbox regression to Bug 86864?", invite);
    }

    @Override
    public void setUp() throws Exception {
        if (runningOutsideZimbra) {
            TestUtil.cliSetup();
            String tzFilePath = LC.timezone_file.value();
            File tzFile = new File(tzFilePath);
            WellKnownTimeZones.loadFromFile(tzFile);
        }
        if (prov == null) {
            prov = TestUtil.newSoapProvisioning();
        }
        tearDown();
    }

    @Override
    public void tearDown() throws Exception {
        if (prov == null) {
            prov = TestUtil.newSoapProvisioning();
        }
        TestUtil.deleteAccount(ORGANIZER);
        TestUtil.deleteAccount(ATTENDEE);
        TestUtil.deleteAccount(DELEGATE);
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        try {
            TestUtil.runTest(TestInvite.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
