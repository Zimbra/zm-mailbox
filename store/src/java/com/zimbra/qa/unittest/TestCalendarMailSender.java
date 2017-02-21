/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.google.common.collect.Maps;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.admin.type.DomainSelector.DomainBy;

public class TestCalendarMailSender extends TestCase {

    private static String ORGANIZERACCT = "organizerAcct";
    private static String ATTENDEEACCT = "attendeeAcct";
    private static String SYSRESOURCEACCT = "sysResourceAcct";
    private static String DIFFDOMAIN = "calmailsndr.example.test";
    private static String DIFFDOMAINACCT = "diffDomainAcct@" + DIFFDOMAIN;

    public void testInviteAutoDeclinePrefsUnset() throws Exception {
        Account organizerAccount = TestUtil.createAccount(ORGANIZERACCT);
        Account senderAccount = organizerAccount;
        Account attendeeAccount = TestUtil.createAccount(ATTENDEEACCT);
        Mailbox mbox = TestUtil.getMailbox(ATTENDEEACCT);
        String senderEmail = senderAccount.getName();
        ZAttendee matchingAttendee = new ZAttendee(attendeeAccount.getName(), attendeeAccount.getCn() /* cn */,
                     senderAccount.getName() /* sentBy */,
                     null /* dir */, null /* language */, IcalXmlStrMap.CUTYPE_INDIVIDUAL /* cutype */,
                     null /* role */, IcalXmlStrMap.PARTSTAT_NEEDS_ACTION /* ptst */,
                     true /* rsvp */, null /* member */, null /* delegatedTo */, null /* delegatedFrom */, null);
        boolean allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertFalse(String.format("Should NOT be allowed to send auto-decline because default value for %s is false",
                    Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply), allowed);
    }

    /**
     * Tests with original invite's organizer in same domain
     * and {@code zimbraPrefCalendarSendInviteDeniedAutoReply=true}
     */
    public void testPrefCalendarSendInviteDeniedAutoReplyTrue() throws Exception {
        Account organizerAccount = TestUtil.createAccount(ORGANIZERACCT);
        Account senderAccount = organizerAccount;
        Account attendeeAccount = TestUtil.createAccount(ATTENDEEACCT);
        Mailbox mbox = TestUtil.getMailbox(ATTENDEEACCT);
        String senderEmail = senderAccount.getName();
        Map<String, Object> prefs = Maps.newHashMap();
        prefs.put(Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply, ProvisioningConstants.TRUE);
        Provisioning.getInstance().modifyAttrs(attendeeAccount, prefs, true, null);
        ZAttendee matchingAttendee = new ZAttendee(attendeeAccount.getName(), attendeeAccount.getCn() /* cn */,
                     senderAccount.getName() /* sentBy */,
                     null /* dir */, null /* language */, IcalXmlStrMap.CUTYPE_INDIVIDUAL /* cutype */,
                     null /* role */, IcalXmlStrMap.PARTSTAT_NEEDS_ACTION /* ptst */,
                     true /* rsvp */, null /* member */, null /* delegatedTo */, null /* delegatedFrom */, null);
        boolean allowed;
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertTrue(String.format("Should be allowed to send auto-decline because %s=true",
                    Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply), allowed);

        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, false /* applyToCalendar */, matchingAttendee);
        assertFalse("Should NOT be allowed to send auto-decline because appyToCalendar=false", allowed);

        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, null /* matchingAttendee */);
        assertFalse("Should NOT be allowed to send auto-decline because no matching attendee in invite", allowed);

        senderEmail = null;
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertFalse("Should NOT be allowed to send auto-decline to null sender", allowed);

        senderEmail = "unknown@example.test";
        senderAccount = null;
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertFalse("Should NOT be allowed to send auto-decline - no account object for sender", allowed);

        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraIsSystemResource, ProvisioningConstants.TRUE);
        attrs.put(Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply, ProvisioningConstants.TRUE);
        Account resourceAcct = TestUtil.createAccount(SYSRESOURCEACCT, attrs);
        Mailbox resMbox = TestUtil.getMailbox(SYSRESOURCEACCT);
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            resMbox, resourceAcct, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertFalse("Should NOT be allowed to send auto-decline - account is a system resource", allowed);
    }

    public void testInternalPrefCalendarAllowedTargetsForInviteDeniedAutoReply() throws Exception {
        Account organizerAccount = TestUtil.createAccount(ORGANIZERACCT);
        Account senderAccount = organizerAccount;
        Account attendeeAccount = TestUtil.createAccount(ATTENDEEACCT);
        Mailbox mbox = TestUtil.getMailbox(ATTENDEEACCT);
        String senderEmail = senderAccount.getName();
        Map<String, Object> prefs = Maps.newHashMap();
        prefs.put(Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply, ProvisioningConstants.TRUE);
        prefs.put(Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply, "internal");
        Provisioning.getInstance().modifyAttrs(attendeeAccount, prefs, true, null);
        ZAttendee matchingAttendee = new ZAttendee(attendeeAccount.getName(), attendeeAccount.getCn() /* cn */,
                     senderAccount.getName() /* sentBy */,
                     null /* dir */, null /* language */, IcalXmlStrMap.CUTYPE_INDIVIDUAL /* cutype */,
                     null /* role */, IcalXmlStrMap.PARTSTAT_NEEDS_ACTION /* ptst */,
                     true /* rsvp */, null /* member */, null /* delegatedTo */, null /* delegatedFrom */, null);
        boolean allowed;
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertTrue(String.format("Should be allowed to send auto-decline because %s=true & %s=internal",
                    Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply,
                    Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply), allowed);
        senderAccount = null;
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertFalse(String.format(
                    "Should NOT be allowed to send auto-decline for non-internal because %s=true & %s=internal",
                    Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply,
                    Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply), allowed);
    }

    public void testSameDomainPrefCalendarAllowedTargetsForInviteDeniedAutoReply() throws Exception {
        Account organizerAccount = TestUtil.createAccount(ORGANIZERACCT);
        Account senderAccount = organizerAccount;
        Account attendeeAccount = TestUtil.createAccount(ATTENDEEACCT);
        Mailbox mbox = TestUtil.getMailbox(ATTENDEEACCT);
        String senderEmail = senderAccount.getName();
        Map<String, Object> prefs = Maps.newHashMap();
        prefs.put(Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply, ProvisioningConstants.TRUE);
        prefs.put(Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply, "sameDomain");
        Provisioning.getInstance().modifyAttrs(attendeeAccount, prefs, true, null);
        ZAttendee matchingAttendee = new ZAttendee(attendeeAccount.getName(), attendeeAccount.getCn() /* cn */,
                     senderAccount.getName() /* sentBy */,
                     null /* dir */, null /* language */, IcalXmlStrMap.CUTYPE_INDIVIDUAL /* cutype */,
                     null /* role */, IcalXmlStrMap.PARTSTAT_NEEDS_ACTION /* ptst */,
                     true /* rsvp */, null /* member */, null /* delegatedTo */, null /* delegatedFrom */, null);
        boolean allowed;
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertTrue(String.format("Should be allowed to send auto-decline because %s=true & %s=sameDomain",
                    Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply,
                    Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply), allowed);
        Provisioning prov = Provisioning.getInstance();
        prov.createDomain(DIFFDOMAIN, new HashMap<String, Object>());
        senderAccount = prov.createAccount(DIFFDOMAINACCT, "test123", null);
        senderEmail = senderAccount.getName();
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertFalse(String.format(
                    "Should NOT be allowed to send auto-decline to diff domain because %s=true & %s=sameDomain",
                    Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply,
                    Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply), allowed);

        senderAccount = null;
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertFalse(String.format(
                    "Should NOT be allowed to send auto-decline to non-internal because %s=true & %s=sameDomain",
                    Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply,
                    Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply), allowed);
    }

    public void testAllPrefCalendarAllowedTargetsForInviteDeniedAutoReply() throws Exception {
        Account organizerAccount = TestUtil.createAccount(ORGANIZERACCT);
        Account senderAccount = organizerAccount;
        Account attendeeAccount = TestUtil.createAccount(ATTENDEEACCT);
        Mailbox mbox = TestUtil.getMailbox(ATTENDEEACCT);
        String senderEmail = senderAccount.getName();
        Map<String, Object> prefs = Maps.newHashMap();
        prefs.put(Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply, ProvisioningConstants.TRUE);
        prefs.put(Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply, "all");
        Provisioning.getInstance().modifyAttrs(attendeeAccount, prefs, true, null);
        ZAttendee matchingAttendee = new ZAttendee(attendeeAccount.getName(), attendeeAccount.getCn() /* cn */,
                     senderAccount.getName() /* sentBy */,
                     null /* dir */, null /* language */, IcalXmlStrMap.CUTYPE_INDIVIDUAL /* cutype */,
                     null /* role */, IcalXmlStrMap.PARTSTAT_NEEDS_ACTION /* ptst */,
                     true /* rsvp */, null /* member */, null /* delegatedTo */, null /* delegatedFrom */, null);
        boolean allowed;
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertTrue(String.format("Should be allowed to send auto-decline because %s=true & %s=all",
                    Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply,
                    Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply), allowed);
        Provisioning prov = Provisioning.getInstance();
        prov.createDomain(DIFFDOMAIN, new HashMap<String, Object>());
        senderAccount = prov.createAccount(DIFFDOMAINACCT, "test123", null);
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertTrue(String.format(
                    "Should be allowed to send auto-decline to diff domain because %s=true & %s=all",
                    Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply,
                    Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply), allowed);

        senderAccount = null;
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertTrue(String.format(
                    "Should be allowed to send auto-decline to non-internal because %s=true & %s=all",
                    Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply,
                    Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply), allowed);

        senderEmail = "fred@unknown.domain";
        allowed = CalendarMailSender.allowInviteAutoDeclinedNotification(
            mbox, attendeeAccount, senderEmail, senderAccount, true /* applyToCalendar */, matchingAttendee);
        assertTrue(String.format(
                    "Should be allowed to send auto-decline to external user because %s=true & %s=all",
                    Provisioning.A_zimbraPrefCalendarSendInviteDeniedAutoReply,
                    Provisioning.A_zimbraPrefCalendarAllowedTargetsForInviteDeniedAutoReply), allowed);
    }

    @Override
    public void setUp() throws Exception {
        cleanUp();
    }

    public void cleanUp()
    throws Exception {
        TestUtil.deleteAccount(ORGANIZERACCT);
        TestUtil.deleteAccount(ATTENDEEACCT);
        TestUtil.deleteAccount(SYSRESOURCEACCT);
        TestUtil.deleteAccount(DIFFDOMAINACCT);
        Provisioning prov = Provisioning.getInstance();
        DomainSelector domSel = new DomainSelector(DomainBy.name, DIFFDOMAIN);
        Domain diffDomain = prov.get(domSel);
        if (diffDomain != null) {
            prov.deleteDomain(diffDomain.getId());
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception{
        try {
            TestUtil.runTest(TestCalendarMailSender.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
