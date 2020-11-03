/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.CalendarUtil;
import com.zimbra.common.calendar.Geo;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ICalTimeZone.SimpleOnset;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.calendar.TZIDMapper;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.calendar.ZCalendar;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.gal.GalGroup;
import com.zimbra.cs.gal.GalGroupMembers;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.CalendarItem.ReplyInfo;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.Alarm;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.Period;
import com.zimbra.cs.mailbox.calendar.RdateExdate;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;
import com.zimbra.cs.mailbox.calendar.Util;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ZRecur;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.util.AccountUtil.AccountAddressMatcher;
import com.zimbra.soap.base.CalTZInfoInterface;
import com.zimbra.soap.type.TzOnsetInfo;

public class CalendarUtils {
    // Start of Microsoft time
    // Likely to encounter problems with any dates before this in Outlook connector
    // Value obtained from:
    //     ParsedDateTime.parseUtcOnly("16010101T000000").getDate().getTime();
    public static final long MICROSOFT_EPOC_START_MS_SINCE_EPOC = -11644473600000L;

    /**
     * Useful for sync and import, parse an <inv> that is specified using raw
     * iCalendar data in the format: <inv> <content uid="UID" summary="summary">
     * RAW VCALENDAR </content> </inv>
     *
     * @param account
     * @param inviteElem
     * @return
     * @throws ServiceException
     */
    static ParseMimeMessage.InviteParserResult parseInviteRaw(Account account,
            Element inviteElem) throws ServiceException {
        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();

        Element content = inviteElem.getElement(MailConstants.E_CONTENT /* content */);
        toRet.mUid = content.getAttribute(MailConstants.A_UID /* uid */);
        toRet.mSummary = content.getAttribute(MailConstants.A_SUMMARY /* summary */);
        toRet.mCal = ZCalendarBuilder.build(content.getText());

        List<Invite> invs = Invite.createFromCalendar(account, toRet.mSummary,
                toRet.mCal, false);

        toRet.mInvite = invs.get(0);

        return toRet;
    }

    public static final boolean RECUR_NOT_ALLOWED = false;

    public static final boolean RECUR_ALLOWED = true;

    /**
     * Parse an <inv> element
     *
     * @param account
     * @param inviteElem
     * @param tzMap
     *            TimeZoneMap of invite we might want to refer to (eg we are an
     *            Exception to it)
     * @param uid
     * @param recurrenceIdAllowed
     * @return
     * @throws ServiceException
     */
    static ParseMimeMessage.InviteParserResult parseInviteForCreate(
            Account account, MailItem.Type type, Element inviteElem, TimeZoneMap tzMap, String uid,
            boolean recurrenceIdAllowed, boolean recurAllowed)
            throws ServiceException {
        if (tzMap == null) {
            tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        }
        Invite create = new Invite(ICalTok.PUBLISH.toString(), tzMap, false);
        create.setSentByMe(true);

        CalendarUtils.parseInviteElementCommon(account, type, inviteElem, create, recurrenceIdAllowed, recurAllowed);

        // DTSTAMP
        if (create.getDTStamp() == 0) { //zdsync
            create.setDtStamp(new Date().getTime());
        }

        // UID
        if (uid != null && uid.length() > 0) {
            create.setUid(uid);
        } else {
            String uidParsed = create.getUid();
            if (uidParsed == null || uidParsed.length() == 0)
                create.setUid(LdapUtil.generateUUID());
        }

        ZVCalendar iCal = create.newToICalendar(true);

        String summaryStr = create.getName() != null ? create.getName() : "";

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = create.getUid();
        toRet.mSummary = summaryStr;
        toRet.mInvite = create;

        return toRet;
    }

    private static String parseInviteChanges(Element inviteElem) {
        Element compElem = inviteElem.getOptionalElement(MailConstants.E_INVITE_COMPONENT);
        if (compElem == null)
            compElem = inviteElem;
        String changes = compElem.getAttribute(MailConstants.A_CAL_CHANGES, null);
        if (changes != null && changes.length() > 0)
            return changes;
        else
            return null;
    }

    static ParseMimeMessage.InviteParserResult parseInviteForCreateException(Account account, MailItem.Type type,
            Element inviteElem, TimeZoneMap tzMap, String uid, Invite defaultInv) throws ServiceException {
        if (tzMap == null) {
            tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        }
        Invite create = new Invite(ICalTok.PUBLISH.toString(), tzMap, false);
        create.setSentByMe(true);

        CalendarUtils.parseInviteElementCommon(account, type, inviteElem, create, true, false);

        // DTSTAMP
        if (create.getDTStamp() == 0) { //zdsync
            create.setDtStamp(new Date().getTime());
        }

        // UID
        if (uid != null && uid.length() > 0)
            create.setUid(uid);

        // SEQUENCE - Make sure it's greater than the series sequence.  (bugs 19111 and 35036)
        create.setSeqNo(Math.max(create.getSeqNo(), defaultInv.getSeqNo() + 1));

        // Don't allow changing organizer in an exception instance.
        create.setOrganizer(defaultInv.hasOrganizer()
                            ? new ZOrganizer(defaultInv.getOrganizer()) : null);
        create.setIsOrganizer(account);

        // change tracking
        String changes = parseInviteChanges(inviteElem);
        if (changes != null) {
            // Set the changes as x-prop in the serialized iCalendar object, but not to the parsed Invite object.
            create.addXProp(new ZProperty(ICalTok.X_ZIMBRA_CHANGES, changes));
        }
        ZVCalendar iCal = create.newToICalendar(true);
        create.removeXProp(ICalTok.X_ZIMBRA_CHANGES.toString());  // Don't set the changes x-prop in the parsed Invite.

        String summaryStr = create.getName() != null ? create.getName() : "";

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = create.getUid();
        toRet.mSummary = summaryStr;
        toRet.mInvite = create;

        return toRet;
    }

    /**
     * Parse an <inv> element in a Modify context -- existing UID, etc
     *
     * @param inviteElem
     * @param oldInv
     *            is the Default Invite of the appointment we are modifying
     * @return
     * @throws ServiceException
     */
    static ParseMimeMessage.InviteParserResult parseInviteForModify(Account account, MailItem.Type type,
            Element inviteElem, Invite oldInv, Invite seriesInv, List<ZAttendee> attendeesAdded,
            List<ZAttendee> attendeesToCancel, boolean recurAllowed) throws ServiceException {
        Invite mod = new Invite(ICalTok.PUBLISH.toString(), oldInv.getTimeZoneMap(), false);
        mod.setSentByMe(true);

        CalendarUtils.parseInviteElementCommon(account, type, inviteElem, mod, oldInv.hasRecurId(), recurAllowed);

        // UID
        mod.setUid(oldInv.getUid());

        attendeesToCancel.addAll(getRemovedAttendees(oldInv.getAttendees(), mod.getAttendees(), true, account));
        attendeesAdded.addAll(getRemovedAttendees(mod.getAttendees(), oldInv.getAttendees(), false, account));  // reverse of who's being canceled

        // SEQUENCE and DTSTAMP
        if (mod.isOrganizer()) {
            // Set sequence to the max of 1) what the modifying client specified (if any),
            // 2) current sequence + 1, and 3) current sequence of the series, if we're modifying
            // an exception instance.  This is to satisfy Outlook's requirement that an up-to-date
            // exception instance should have a sequence greater than or equal to the series sequence.
            // (bug 19111)
            int seriesSeq = seriesInv != null ? seriesInv.getSeqNo() : 0;
            int newSeq = Math.max(Math.max(mod.getSeqNo(), oldInv.getSeqNo() + 1), seriesSeq);
            // Increment sequence by one more if there are attendees to whom to send cancel notice.  If old sequence
            // is N, cancel notice will have sequence N+1 and the new invite will have sequence N+2.  This ensures
            // the update takes precedence over cancel in case someone receives both, regardless of delivery order. (bug 56642)
            if (!attendeesToCancel.isEmpty())
                ++newSeq;
            mod.setSeqNo(newSeq);
            mod.setDtStamp(new Date().getTime());
        } else {
            // If attendee is modifying his copy, preserve SEQUENCE and DTSTAMP.
            // If these were advanced, existing replies will get invalidated and
            // we don't want that.
            mod.setSeqNo(oldInv.getSeqNo());
            mod.setDtStamp(oldInv.getDTStamp());
        }

        if (oldInv.hasRecurId()) {
            mod.setRecurId(oldInv.getRecurId());
        }

        // change tracking
        String changes = parseInviteChanges(inviteElem);
        if (changes != null) {
            // Set the changes as x-prop in the serialized iCalendar object, but not to the parsed Invite object.
            mod.addXProp(new ZProperty(ICalTok.X_ZIMBRA_CHANGES, changes));
        }
        ZVCalendar iCal = mod.newToICalendar(true);
        mod.removeXProp(ICalTok.X_ZIMBRA_CHANGES.toString());  // Don't set the changes x-prop in the parsed Invite.

        String summaryStr = "";
        if (mod.getName() != null) {
            summaryStr = mod.getName();
        }

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = mod.getUid();
        toRet.mSummary = summaryStr;
        toRet.mInvite = mod;

        return toRet;
    }

    static ParseMimeMessage.InviteParserResult parseInviteForCancel(Account account, Folder folder, MailItem.Type type,
            Element inviteElem, TimeZoneMap tzMap, boolean recurrenceIdAllowed, boolean recurAllowed)
            throws ServiceException {
        if (tzMap == null) {
            tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        }
        Invite cancel = new Invite(ICalTok.CANCEL.toString(), tzMap, false);
        cancel.setSentByMe(true);

        CalendarUtils.parseInviteElementCommon(
                account, type, inviteElem, cancel, recurrenceIdAllowed, recurAllowed);

        String uid = cancel.getUid();
        if (uid == null || uid.length() == 0)
            throw ServiceException.INVALID_REQUEST("Missing uid in a cancel invite", null);

        Invite sanitized =
            cancelInvite(account, null, false, false, folder, cancel, null,
                         cancel.getAttendees(), cancel.getRecurId(),
                         false);

        sanitized.setInviteId(cancel.getMailItemId()); //zdsync
        sanitized.setDtStamp(cancel.getDTStamp()); //zdsync

        ZVCalendar iCal = sanitized.newToICalendar(true);

        String summaryStr = sanitized.getName() != null ? sanitized.getName() : "";

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = sanitized.getUid();
        toRet.mSummary = summaryStr;
        toRet.mInvite = sanitized;

        return toRet;
    }

    static ParseMimeMessage.InviteParserResult parseInviteForAddInvite(Account account, MailItem.Type type,
            Element inviteElem, TimeZoneMap tzMap) throws ServiceException {
        if (tzMap == null)
            tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        Invite inv = new Invite(ICalTok.PUBLISH.toString(), tzMap, false);

        CalendarUtils.parseInviteElementCommon(account, type, inviteElem, inv, true, true);

        String uid = inv.getUid();
        if (uid == null || uid.length() == 0)
            throw ServiceException.INVALID_REQUEST("Missing uid in an add invite", null);

        // DTSTAMP
        if (inv.getDTStamp() == 0) { //zdsync
            inv.setDtStamp(new Date().getTime());
        }

        ZVCalendar iCal = inv.newToICalendar(true);

        String summaryStr = inv.getName() != null ? inv.getName() : "";

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = inv.getUid();
        toRet.mSummary = summaryStr;
        toRet.mInvite = inv;

        return toRet;
    }

    static ParseMimeMessage.InviteParserResult parseInviteForCounter(Account account, Invite oldInvite, MailItem.Type type,
            Element inviteElem) throws ServiceException {
        TimeZoneMap tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        Invite inv = new Invite(ICalTok.COUNTER.toString(), tzMap, false);

        CalendarUtils.parseInviteElementCommon(account, type, inviteElem, inv, true, true);

        // Get the existing invite to populate X-MS-OLK-ORIGINALSTART and X-MS-OLK-ORIGINALEND
        if (oldInvite == null) {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            CalendarItem calItem = mbox.getCalendarItemByUid(null, inv.getUid());
            if (calItem != null)
                oldInvite = calItem.getInvite(inv.getRecurId());
        }

        if (oldInvite != null) {
            // Add TZIDs from oldInvite to inv
            inv.getTimeZoneMap().add(oldInvite.getTimeZoneMap());
            // Add ORIGINALSTART x-prop
            ParsedDateTime dt = oldInvite.getStartTime();
            if (dt != null) {
                ZCalendar.ZProperty prop = new ZCalendar.ZProperty("X-MS-OLK-ORIGINALSTART");
                prop.setValue(dt.getDateTimePartString());
                if (dt.getTZName() != null)
                    prop.addParameter(new ZParameter(ICalTok.TZID, dt.getTZName()));
                inv.addXProp(prop);
            }
            // Add ORIGINALEND x-prop
            dt = oldInvite.getEffectiveEndTime();
            if (dt != null) {
                ZCalendar.ZProperty prop = new ZCalendar.ZProperty("X-MS-OLK-ORIGINALEND");
                prop.setValue(dt.getDateTimePartString());
                if (dt.getTZName() != null)
                    prop.addParameter(new ZParameter(ICalTok.TZID, dt.getTZName()));
                inv.addXProp(prop);
            }
            // Add LOCATION if not already exist.
            if (inv.getLocation() == null || inv.getLocation().isEmpty())
                inv.setLocation(oldInvite.getLocation());
        }

        // UID
        String uid = inv.getUid();
        if (uid == null || uid.length() == 0)
            throw ServiceException.INVALID_REQUEST("Missing uid in a counter invite", null);

        // ORGANIZER
        if (!inv.hasOrganizer())
            throw ServiceException.INVALID_REQUEST("Missing organizer in a counter invite", null);

        // DTSTAMP
        if (inv.getDTStamp() == 0) { //zdsync
            inv.setDtStamp(new Date().getTime());
        }

        // DTSTART
        if (inv.getStartTime() == null)
            throw ServiceException.INVALID_REQUEST("Missing dtstart in a counter invite", null);

        // Workaround an Outlook bug.  Outlook 2007 will get MIME parse error if COUNTER
        // iCalendar object doesn't have an ATTENDEE property.  RFC2446 doesn't require one.
        if (!inv.hasOtherAttendees()) {
            ZAttendee at = new ZAttendee(account.getMail());
            at.setPartStat(IcalXmlStrMap.PARTSTAT_TENTATIVE);
            inv.addAttendee(at);
        }

        inv.setLocalOnly(false);
        ZVCalendar iCal = inv.newToICalendar(true);

        String summaryStr = inv.getName() != null ? inv.getName() : "";

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = inv.getUid();
        toRet.mSummary = summaryStr;
        toRet.mInvite = inv;

        return toRet;
    }

    static ParseMimeMessage.InviteParserResult parseInviteForDeclineCounter(Account account, MailItem.Type type,
            Element inviteElem) throws ServiceException {
        TimeZoneMap tzMap = new TimeZoneMap(Util.getAccountTimeZone(account));
        Invite inv = new Invite(ICalTok.DECLINECOUNTER.toString(), tzMap, false);

        CalendarUtils.parseInviteElementCommon(account, type, inviteElem, inv, true, true);

        // UID
        String uid = inv.getUid();
        if (uid == null || uid.length() == 0)
            throw ServiceException.INVALID_REQUEST("Missing uid in a decline counter invite", null);

        // ORGANIZER
        if (!inv.hasOrganizer())
            throw ServiceException.INVALID_REQUEST("Missing organizer in a decline counter invite", null);

        // DTSTAMP
        if (inv.getDTStamp() == 0) { //zdsync
            inv.setDtStamp(new Date().getTime());
        }

        inv.setLocalOnly(false);
        ZVCalendar iCal = inv.newToICalendar(true);

        String summaryStr = inv.getName() != null ? inv.getName() : "";

        ParseMimeMessage.InviteParserResult toRet = new ParseMimeMessage.InviteParserResult();
        toRet.mCal = iCal;
        toRet.mUid = inv.getUid();
        toRet.mSummary = summaryStr;
        toRet.mInvite = inv;

        return toRet;
    }

    // Compare the old and new attendee lists to figure out which attendees are being removed.
    // Distribution lists are taken into consideration if requested.
    public static List<ZAttendee> getRemovedAttendees(List<ZAttendee> oldAttendees, List<ZAttendee> newAttendees,
                                                      boolean checkListMembership, Account account)
    throws ServiceException {
        List<ZAttendee> list = new ArrayList<ZAttendee>();
        Provisioning prov = Provisioning.getInstance();
        // compare the new attendee list with the existing one...
        // if attendees have been removed, then we need to send them individual cancellation messages
        for (ZAttendee old : oldAttendees) {
            boolean matches = false;
            String oldAddr = old.getAddress();
            if (oldAddr != null) {
                Account oldAcct = prov.get(AccountBy.name, oldAddr);
                if (oldAcct != null) {
                    // local user - consider aliases
                    AccountAddressMatcher acctMatcher = new AccountAddressMatcher(oldAcct);
                    for (ZAttendee newAt : newAttendees) {
                        if (acctMatcher.matches(newAt.getAddress())) {
                            matches = true;
                            break;
                        }
                    }
                } else {
                    // external email - simple string comparison of email addresses
                    for (ZAttendee newAt : newAttendees) {
                        if (oldAddr.equalsIgnoreCase(newAt.getAddress())) {
                            matches = true;
                            break;
                        }
                    }
                }
            }
            if (!matches)
                list.add(old);
        }
        if (list.isEmpty())
            return list;

        //bug 68728, skip checking in ZD
        checkListMembership = checkListMembership && LC.check_dl_membership_enabled.booleanValue();
        // Find out which of the new attendees are local distribution lists or GAL groups.
        if (checkListMembership) {
            List<DistributionList> newAtsDL = new ArrayList<DistributionList>();
            List<String /* GAL group email */> newAtsGALGroup = new ArrayList<String>();
            for (ZAttendee at : newAttendees) {
                String addr = at.getAddress();
                if (addr != null) {
                    DistributionList dl = prov.get(Key.DistributionListBy.name, addr);
                    if (dl != null)
                        newAtsDL.add(dl);
                    else if (GalGroup.isGroup(addr, account))
                        newAtsGALGroup.add(addr);
                }
            }

            // Check if attendees to be removed are members of GAL groups or local DLs.  An attendee being
            // dropped in the new invite isn't really removed if he/she is a member of DL/GAL group.

            // GAL groups: Iterate over GAL groups first because fetching member list is expensive.
            for (String galAddr : newAtsGALGroup) {
                if (list.isEmpty())
                    break;
                Set<String> galMembers = GalGroupMembers.getGroupMembers(galAddr, account);
                for (Iterator<ZAttendee> removedIter = list.iterator(); removedIter.hasNext(); ) {
                    ZAttendee removedAt = removedIter.next();
                    String addr = removedAt.getAddress();
                    if (addr != null && galMembers.contains(addr))
                        removedIter.remove();
                }
            }

            Set<String> remoteAddrs = new HashSet<String>();

            // local DLs: Iterate over attendees first to get each attendee's DL list.  This loop only deals with
            // attendees who are local Accounts.  This case works for indirect list membership as well as membership
            // via alias address.
            for (Iterator<ZAttendee> removedIter = list.iterator(); removedIter.hasNext(); ) {
                ZAttendee removedAt = removedIter.next();
                String addr = removedAt.getAddress();
                if (addr != null) {
                    Account removedAcct = prov.get(AccountBy.name, addr);
                    if (removedAcct != null) {
                        Set<String> acctDLs = prov.getDistributionLists(removedAcct);
                        for (DistributionList dl : newAtsDL) {
                            if (acctDLs.contains(dl.getId())) {
                                removedIter.remove();
                                break;
                            }
                        }
                    } else {
                        // Removed address is not a local account.
                        remoteAddrs.add(addr);
                    }
                }
            }

            // Check non-local attendee membership in local DLs.  Only direct membership is checked.
            if (!remoteAddrs.isEmpty()) {
                for (DistributionList dl : newAtsDL) {
                    // Get list members.  We won't do recursive expansion; let's keep it sane.
                    String[] members = dl.getAllMembers();
                    if (members != null && members.length > 0) {
                        Set<String> membersLower = new HashSet<String>();
                        for (String member : members) {
                            membersLower.add(member.toLowerCase());
                        }
                        for (Iterator<ZAttendee> removedIter = list.iterator(); removedIter.hasNext(); ) {
                            ZAttendee removedAt = removedIter.next();
                            String addr = removedAt.getAddress();
                            if (addr != null && remoteAddrs.contains(addr) && membersLower.contains(addr.toLowerCase())) {
                                removedIter.remove();
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    static RecurId parseRecurId(Element e, TimeZoneMap tzmap) throws ServiceException {
        String range = e.getAttribute(MailConstants.A_CAL_RANGE, null);

        ParsedDateTime dt = parseDateTime(e, tzmap);
        return new RecurId(dt, range);
    }

    /**
     * Parse a date from the enclosed element. If the element has a TimeZone
     * reference, then
     *
     * @param e
     * @param referencedTimeZones
     * @return obj[0] is a Date, obj[1] is a TimeZone
     * @throws ServiceException
     */
    static ParsedDateTime parseDateTime(Element e, TimeZoneMap invTzMap) throws ServiceException {
        String d = e.getAttribute(MailConstants.A_CAL_DATETIME, null);
        String tz = e.getAttribute(MailConstants.A_CAL_TIMEZONE, null);
        return parseDateTime(e.getName(), d, tz, invTzMap);
    }

    private static ParsedDateTime parseDateTime(String eltName, String d,
            String tzName, TimeZoneMap invTzMap)
            throws ServiceException {
        try {
            ICalTimeZone zone = null;
            if (tzName != null) {
                zone = lookupAndAddToTzList(tzName, invTzMap, null);
            }
            ICalTimeZone localTz = invTzMap != null ? invTzMap.getLocalTimeZone() : null;
            return ParsedDateTime.parse(d, invTzMap, zone, localTz);
        } catch (ParseException ex) {
            throw ServiceException.INVALID_REQUEST("could not parse time "
                    + d + " in element " + eltName, ex);
        }
    }

    private static ICalTimeZone lookupAndAddToTzList(String tzId,
            TimeZoneMap invTzMap, Invite inv) throws ServiceException {
        // Workaround for bug in Outlook, which double-quotes TZID parameter
        // value in properties like DTSTART, DTEND, etc. Use unquoted tzId.
        int len = tzId.length();
        if (len >= 2 && tzId.charAt(0) == '"' && tzId.charAt(len - 1) == '"') {
            tzId = tzId.substring(1, len - 1);
        }

        ICalTimeZone zone = null;

        if (tzId.equals("")) {
            return null;
        }

        if (!DebugConfig.disableCalendarTZMatchByID)
            tzId = TZIDMapper.canonicalize(tzId);

        zone = WellKnownTimeZones.getTimeZoneById(tzId);

        if (zone == null) {
            // Could be a custom TZID during modify operation of invite from
            // external calendar system. Look up the TZID from the invite.
            if (invTzMap != null) {
                zone = invTzMap.getTimeZone(tzId);
            }

            if (zone == null) {
                throw ServiceException.INVALID_REQUEST("invalid time zone \"" + tzId + "\"", null);
            }
        }
        if (inv != null && !inv.getTimeZoneMap().contains(zone))
            inv.getTimeZoneMap().add(zone);
        return zone;
    }

    static Recurrence.IRecurrence parseRecur(Element recurElt, TimeZoneMap invTzMap,
                ParsedDateTime dtStart, ParsedDateTime dtEnd, ParsedDuration dur, RecurId recurId)
                throws ServiceException {
        return parseRecur(recurElt, invTzMap, dtStart, dtEnd, dur, recurId, false);
    }

    static Recurrence.IRecurrence parseRecur(Element recurElt, TimeZoneMap invTzMap,
                                                    ParsedDateTime dtStart, ParsedDateTime dtEnd,
                                                    ParsedDuration dur, RecurId recurId, boolean allDay)
    throws ServiceException {
        if (dur == null && dtStart != null && dtEnd != null)
            dur = dtEnd.difference(dtStart);

        ArrayList<IRecurrence> addRules = new ArrayList<IRecurrence>();
        ArrayList<IRecurrence> subRules = new ArrayList<IRecurrence>();

        for (Iterator iter= recurElt.elementIterator(); iter.hasNext();) {
            Element e = (Element)iter.next();

            boolean exclude = false;

            if (e.getName().equals(MailConstants.E_CAL_EXCLUDE)) {
                exclude = true;
            } else {
                if (!e.getName().equals(MailConstants.E_CAL_ADD)) {
                    continue;
                }
            }

            for (Iterator intIter = e.elementIterator(); intIter.hasNext();)
            {
                Element intElt = (Element)intIter.next();

                if (intElt.getName().equals(MailConstants.E_CAL_DATES)) {
                    // handle RDATE or EXDATE
                    String tzid = intElt.getAttribute(MailConstants.A_CAL_TIMEZONE, null);
                    ICalTimeZone tz = tzid != null ? invTzMap.lookupAndAdd(tzid) : null;
                    RdateExdate rexdate = new RdateExdate(exclude ? ICalTok.EXDATE : ICalTok.RDATE, tz);

                    ICalTok valueType = null;
                    for (Iterator<Element> dtvalIter = intElt.elementIterator(MailConstants.E_CAL_DATE_VAL);
                         dtvalIter.hasNext(); ) {
                        ICalTok dtvalValueType = null;
                        Element dtvalElem = dtvalIter.next();
                        Element dtvalStartElem = dtvalElem.getElement(MailConstants.E_CAL_START_TIME);
                        String dtvalStartDateStr = dtvalStartElem.getAttribute(MailConstants.A_CAL_DATETIME);
                        ParsedDateTime dtvalStart =
                            parseDateTime(dtvalElem.getName(), dtvalStartDateStr, tzid, invTzMap);

                        Element dtvalEndElem = dtvalElem.getOptionalElement(MailConstants.E_CAL_END_TIME);
                        Element dtvalDurElem = dtvalElem.getOptionalElement(MailConstants.E_CAL_DURATION);
                        if (dtvalEndElem == null && dtvalDurElem == null) {
                            if (dtvalStart.hasTime())
                                dtvalValueType = ICalTok.DATE_TIME;
                            else
                                dtvalValueType = ICalTok.DATE;
                            rexdate.addValue(dtvalStart);
                        } else {
                            dtvalValueType = ICalTok.PERIOD;
                            if (dtvalEndElem != null) {
                                String dtvalEndDateStr = dtvalEndElem.getAttribute(MailConstants.A_CAL_DATETIME);
                                ParsedDateTime dtvalEnd =
                                    parseDateTime(dtvalElem.getName(), dtvalEndDateStr, tzid, invTzMap);
                                Period p = new Period(dtvalStart, dtvalEnd);
                                rexdate.addValue(p);
                            } else {
                                ParsedDuration d = ParsedDuration.parse(dtvalDurElem);
                                Period p = new Period(dtvalStart, d);
                                rexdate.addValue(p);
                            }
                        }

                        if (valueType == null) {
                            valueType = dtvalValueType;
                            rexdate.setValueType(valueType);
                        } else if (valueType != dtvalValueType)
                            throw ServiceException.INVALID_REQUEST(
                                    "Cannot mix different value types in a single <" +
                                    intElt.getName() + "> element", null);
                    }

                    Recurrence.SingleDates sd = new Recurrence.SingleDates(rexdate, dur);
                    if (exclude)
                        subRules.add(sd);
                    else
                        addRules.add(sd);

                } else if (intElt.getName().equals(MailConstants.E_CAL_RULE)) {
                    // handle RRULE or EXRULE

                    // Turn XML into iCal RECUR string, which will then be
                    // parsed by ical4j Recur object.

                    StringBuilder recurBuf = new StringBuilder(100);

                    String freq = IcalXmlStrMap.sFreqMap.toIcal(
                                      intElt.getAttribute(MailConstants.A_CAL_RULE_FREQ));
                    recurBuf.append("FREQ=").append(freq);

                    for (Iterator ruleIter = intElt.elementIterator(); ruleIter.hasNext(); ) {
                        Element ruleElt = (Element) ruleIter.next();
                        String ruleEltName = ruleElt.getName();
                        if (ruleEltName.equals(MailConstants.E_CAL_RULE_UNTIL)) {
                            recurBuf.append(";UNTIL=");
                            String d = ruleElt.getAttribute(MailConstants.A_CAL_DATETIME);
                            recurBuf.append(d);

                            // If UNTIL has time part it must be specified
                            // as UTC time, i.e. ending in "Z".
                            // (RFC2445 Section 4.3.10 Recurrence Rule)
                            if (d.indexOf("T") >= 0)
                                if (d.indexOf("Z") <0)
                                    recurBuf.append('Z');


                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_COUNT)) {
                            int num = (int) ruleElt.getAttributeLong(MailConstants.A_CAL_RULE_COUNT_NUM, -1);
                            if (num > 0) {
                                recurBuf.append(";COUNT=").append(num);
                            } else {
                                throw ServiceException.INVALID_REQUEST(
                                    "Expected positive num attribute in <recur> <rule> <count>", null);
                            }
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_INTERVAL)) {
                            String ival = ruleElt.getAttribute(MailConstants.A_CAL_RULE_INTERVAL_IVAL);
                            recurBuf.append(";INTERVAL=").append(ival);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYSECOND)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYSECOND_SECLIST);
                            recurBuf.append(";BYSECOND=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYMINUTE)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYMINUTE_MINLIST);
                            recurBuf.append(";BYMINUTE=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYHOUR)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYHOUR_HRLIST);
                            recurBuf.append(";BYHOUR=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYDAY)) {
                            recurBuf.append(";BYDAY=");
                            int pos = 0;
                            for (Iterator bydayIter = ruleElt.elementIterator(MailConstants.E_CAL_RULE_BYDAY_WKDAY);
                                 bydayIter.hasNext();
                                 pos++) {
                                Element wkdayElt = (Element) bydayIter.next();
                                if (pos > 0)
                                    recurBuf.append(",");
                                String ordwk = wkdayElt.getAttribute(MailConstants.A_CAL_RULE_BYDAY_WKDAY_ORDWK, null);
                                if (ordwk != null)
                                    recurBuf.append(ordwk);
                                String day = wkdayElt.getAttribute(MailConstants.A_CAL_RULE_DAY);
                                if (day == null || day.length() == 0)
                                    throw ServiceException.INVALID_REQUEST("Missing " +
                                                                               MailConstants.A_CAL_RULE_DAY + " in <" +
                                                                               ruleEltName + ">",
                                                                               null);
                                recurBuf.append(day);
                            }
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYMONTHDAY)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYMONTHDAY_MODAYLIST);
                            recurBuf.append(";BYMONTHDAY=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYYEARDAY)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYYEARDAY_YRDAYLIST);
                            recurBuf.append(";BYYEARDAY=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYWEEKNO)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYWEEKNO_WKLIST);
                            recurBuf.append(";BYWEEKNO=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYMONTH)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYMONTH_MOLIST);
                            recurBuf.append(";BYMONTH=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_BYSETPOS)) {
                            String list = ruleElt.getAttribute(MailConstants.A_CAL_RULE_BYSETPOS_POSLIST);
                            recurBuf.append(";BYSETPOS=").append(list);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_WKST)) {
                            String day = ruleElt.getAttribute(MailConstants.A_CAL_RULE_DAY);
                            recurBuf.append(";WKST=").append(day);
                        } else if (ruleEltName.equals(MailConstants.E_CAL_RULE_XNAME)) {
                            String name = ruleElt.getAttribute(MailConstants.A_CAL_RULE_XNAME_NAME, null);
                            if (name != null) {
                                String value = ruleElt.getAttribute(MailConstants.A_CAL_RULE_XNAME_VALUE, "");
                                // TODO: Escape/unescape value according to
                                // "text" rule.
                                recurBuf.append(";").append(name).append("=").append(value);
                            }
                        }
                    }  // iterate inside <rule>

                    try {
                        ZRecur recur = new ZRecur(recurBuf.toString(), invTzMap);
                        if (recur.getCount() > 0) {
                            int estimatedCount = recur.getEstimatedCount(dtStart);
                            ZimbraLog.calendar.debug("Estimated count = %s", String.valueOf(estimatedCount));
                            recur.setCount(estimatedCount);
                        } else {
                            Date estimatedEnd = recur.getEstimatedEndTime(dtStart, allDay);
                            ZimbraLog.calendar.debug("Estimated end date = %s", estimatedEnd == null ? "" : estimatedEnd.toString());
                            recur.setUntil(ParsedDateTime.fromUTCTime(estimatedEnd.getTime()));
                        }
                        if (exclude) {
                            subRules.add(new Recurrence.SimpleRepeatingRule(dtStart, dur, recur, null));
                        } else {
                            addRules.add(new Recurrence.SimpleRepeatingRule(dtStart, dur, recur, null));
                        }
                    } catch (ServiceException ex) {
                        throw ServiceException.INVALID_REQUEST("Exception parsing <recur> <rule>", ex);
                    }

                } else {
                    throw ServiceException.INVALID_REQUEST("Expected <date> or <rule> inside of "+e.getName()+", got "+
                            intElt.getName(), null);
                }
            }    // iterate inside <add> or <exclude>
        } // iter inside <recur>

        if (recurId != null) {
            return new Recurrence.ExceptionRule(recurId, dtStart, dur, null, addRules, subRules);
        } else {
            return new Recurrence.RecurrenceRule(dtStart, dur, null, addRules, subRules);
        }
    }

    static ParsedDateTime parseDtElement(Element e, TimeZoneMap tzMap,
            Invite inv) throws ServiceException {
        String d = e.getAttribute(MailConstants.A_CAL_DATETIME);
        String tzId = e.getAttribute(MailConstants.A_CAL_TIMEZONE, null);
        ICalTimeZone timezone = null;
        if (tzId != null) {
            timezone = lookupAndAddToTzList(tzId, tzMap, inv);
        }

        try {
            return ParsedDateTime.parse(d, tzMap, timezone, inv.getTimeZoneMap()
                    .getLocalTimeZone());
        } catch (ParseException pe) {
            throw ServiceException.INVALID_REQUEST("Caught ParseException: " + pe, pe);
        }
    }

    static void parseTimeZones(Element parent, TimeZoneMap tzMap) throws ServiceException {
        assert (tzMap != null);
        for (Iterator iter = parent.elementIterator(MailConstants.E_CAL_TZ); iter.hasNext();) {
            Element tzElem = (Element) iter.next();
            ICalTimeZone tz = parseTzElement(tzElem);
            tzMap.add(tz);
        }
    }

    /**
     * Parse a <tz> definition, as described in soap-calendar.txt and soap.txt (SearchRequest)
     */
    public static ICalTimeZone parseTimeZone(CalTZInfoInterface calTZ) throws ServiceException {
        String tzid = calTZ.getId();
        if (null == calTZ.getTzStdOffset()) {
            throw ServiceException.INVALID_REQUEST("Unknown TZ: \"" + tzid +
                    "\" and no " + MailConstants.A_CAL_TZ_STDOFFSET + " specified", null);
        }
        int standardOffset = calTZ.getTzStdOffset();
        int daylightOffset = MoreObjects.firstNonNull(calTZ.getTzDayOffset(), standardOffset);
        // minutes to milliseconds
        standardOffset *= 60 * 1000;
        daylightOffset *= 60 * 1000;

        SimpleOnset standardOnset = null;
        SimpleOnset daylightOnset = null;
        if (daylightOffset != standardOffset) {
            TzOnsetInfo standard = calTZ.getStandardTzOnset();
            TzOnsetInfo daylight = calTZ.getDaylightTzOnset();
            if (standard == null || daylight == null)
                throw ServiceException.INVALID_REQUEST(
                                "DST time zone missing standard and/or daylight onset",
                                null);
            standardOnset = parseSimpleOnset(standard);
            daylightOnset = parseSimpleOnset(daylight);
        }

        String standardTzname = calTZ.getStandardTZName();
        String daylightTzname = calTZ.getDaylightTZName();
        return ICalTimeZone.lookup(tzid, standardOffset, standardOnset, standardTzname, daylightOffset, daylightOnset, daylightTzname);
    }

    private static SimpleOnset parseSimpleOnset(TzOnsetInfo onsetInfo)
    throws ServiceException {
        int week = MoreObjects.firstNonNull(onsetInfo.getWeek(), 0);
        int wkday = MoreObjects.firstNonNull(onsetInfo.getDayOfWeek(), 0);
        if (null == onsetInfo.getMonth()) {
                throw ServiceException.INVALID_REQUEST("Timezone onset information missing month", null);
        }
        int month = onsetInfo.getMonth();
        int mday = MoreObjects.firstNonNull(onsetInfo.getDayOfMonth(), 0);
        int hour = MoreObjects.firstNonNull(onsetInfo.getHour(), 0);
        int minute = MoreObjects.firstNonNull(onsetInfo.getMinute(), 0);
        int second = MoreObjects.firstNonNull(onsetInfo.getSecond(), 0);
        return new SimpleOnset(week, wkday, month, mday, hour, minute, second);
    }

    /**
     * Parse a <tz> definition, as described in soap-calendar.txt and soap.txt (SearchRequest)
     *
     * @param tzElem
     * @return
     * @throws ServiceException
     */
    public static ICalTimeZone parseTzElement(Element tzElem) throws ServiceException {
        String tzid = tzElem.getAttribute(MailConstants.A_ID);
        int standardOffset = (int) tzElem
                .getAttributeLong(MailConstants.A_CAL_TZ_STDOFFSET);
        int daylightOffset = (int) tzElem.getAttributeLong(
                MailConstants.A_CAL_TZ_DAYOFFSET, standardOffset);
        // minutes to milliseconds
        standardOffset *= 60 * 1000;
        daylightOffset *= 60 * 1000;

        SimpleOnset standardOnset = null;
        SimpleOnset daylightOnset = null;
        if (daylightOffset != standardOffset) {
            Element standard = tzElem
                    .getOptionalElement(MailConstants.E_CAL_TZ_STANDARD);
            Element daylight = tzElem
                    .getOptionalElement(MailConstants.E_CAL_TZ_DAYLIGHT);
            if (standard == null || daylight == null)
                throw ServiceException.INVALID_REQUEST(
                                "DST time zone missing standard and/or daylight onset",
                                null);
            standardOnset = parseSimpleOnset(standard);
            daylightOnset = parseSimpleOnset(daylight);
        }

        String standardTzname = tzElem.getAttribute(MailConstants.A_CAL_TZ_STDNAME, null);
        String daylightTzname = tzElem.getAttribute(MailConstants.A_CAL_TZ_DAYNAME, null);

        return ICalTimeZone.lookup(tzid, standardOffset, standardOnset, standardTzname, daylightOffset, daylightOnset, daylightTzname);
    }

    private static SimpleOnset parseSimpleOnset(Element element)
            throws ServiceException {
        int week = (int) element.getAttributeLong(MailConstants.A_CAL_TZ_WEEK, 0);
        int wkday = (int) element
                .getAttributeLong(MailConstants.A_CAL_TZ_DAYOFWEEK, 0);
        int month = (int) element.getAttributeLong(MailConstants.A_CAL_TZ_MONTH);
        int mday = (int) element.getAttributeLong(MailConstants.A_CAL_TZ_DAYOFMONTH, 0);
        int hour = (int) element.getAttributeLong(MailConstants.A_CAL_TZ_HOUR);
        int minute = (int) element
                .getAttributeLong(MailConstants.A_CAL_TZ_MINUTE);
        int second = (int) element
                .getAttributeLong(MailConstants.A_CAL_TZ_SECOND);
        return new SimpleOnset(week, wkday, month, mday, hour, minute, second);
    }

    /**
     * UID, DTSTAMP, and SEQUENCE **MUST** be set by caller
     *
     * @param account
     *            user receiving invite
     * @param element
     *            invite XML element
     * @param newInv
     *            Invite we are currently building up
     * @param oldTzMap
     *            time zone map from A DIFFERENT invite; if this method is
     *            called during modify operation, this map contains time zones
     *            before the modification; null if called during create
     *            operation
     * @return
     * @throws ServiceException
     */
    private static void parseInviteElementCommon(Account account, MailItem.Type type, Element element, Invite newInv,
            boolean recurrenceIdAllowed, boolean recurAllowed) throws ServiceException {

        String invId = element.getAttribute(MailConstants.A_ID, null); //zdsync

        Element compElem = element.getOptionalElement(MailConstants.E_INVITE_COMPONENT);
        if (compElem != null) {
            element = compElem;
        }

        String dts = element.getAttribute(MailConstants.A_CAL_DATETIME, null); //zdsync

        TimeZoneMap tzMap = newInv.getTimeZoneMap();
        parseTimeZones(element.getParent(), tzMap);

        newInv.setItemType(type);

        // UID
        String uid = element.getAttribute(MailConstants.A_UID, null);
        if (uid != null && uid.length() > 0)
            newInv.setUid(uid);

        // RECURRENCE-ID
        if (recurrenceIdAllowed) {
            Element e = element.getOptionalElement(MailConstants.E_CAL_EXCEPTION_ID);
            if (e != null) {
                ParsedDateTime dt = parseDateTime(e, tzMap);
                RecurId recurId = new RecurId(dt, RecurId.RANGE_NONE);
                newInv.setRecurId(recurId);
            }
        } else {
            if (element.getOptionalElement(MailConstants.E_CAL_EXCEPTION_ID) != null) {
                throw ServiceException.INVALID_REQUEST(
                        "May not specify an <exceptId> in this request",
                        null);
            }
        }

        String name = element.getAttribute(MailConstants.A_NAME, "");
        String location = element.getAttribute(MailConstants.A_CAL_LOCATION, "");

        // CATEGORIES
        for (Iterator<Element> catIter = element.elementIterator(MailConstants.E_CAL_CATEGORY); catIter.hasNext(); ) {
            String cat = catIter.next().getText();
            newInv.addCategory(cat);
        }

        // COMMENTs
        for (Iterator<Element> cmtIter = element.elementIterator(MailConstants.E_CAL_COMMENT); cmtIter.hasNext(); ) {
            String cmt = cmtIter.next().getText();
            newInv.addComment(cmt);
        }

        // CONTACTs
        for (Iterator<Element> cnIter = element.elementIterator(MailConstants.E_CAL_CONTACT); cnIter.hasNext(); ) {
            String contact = cnIter.next().getTextTrim();
            newInv.addContact(contact);
        }

        // GEO
        Element geoElem = element.getOptionalElement(MailConstants.E_CAL_GEO);
        if (geoElem != null) {
            Geo geo = Geo.parse(geoElem);
            newInv.setGeo(geo);
        }

        // URL
        String url = element.getAttribute(MailConstants.A_CAL_URL, null);
        newInv.setUrl(url);

        // SEQUENCE
        int seq = (int) element.getAttributeLong(MailConstants.A_CAL_SEQUENCE, 0);
        newInv.setSeqNo(seq);

        // SUMMARY (aka Name or Subject)
        newInv.setName(name);

        // DESCRIPTION
        Element descElem = element.getOptionalElement(MailConstants.E_CAL_DESCRIPTION);
        String desc = descElem != null ? descElem.getText() : null;
        Element descHtmlElem = element.getOptionalElement(MailConstants.E_CAL_DESC_HTML);
        String descHtml = descHtmlElem != null ? descHtmlElem.getText() : null;
        newInv.setDescription(desc, descHtml);

        boolean allDay = element.getAttributeBool(MailConstants.A_CAL_ALLDAY, false);
        newInv.setIsAllDayEvent(allDay);

        // DTSTART
        Element startElem;
        if (newInv.isTodo())
            startElem = element.getOptionalElement(MailConstants.E_CAL_START_TIME);
        else
            startElem = element.getElement(MailConstants.E_CAL_START_TIME);
        if (startElem != null) {
            ParsedDateTime dt = parseDtElement(startElem, tzMap, newInv);
            // fixup for bug 30121
            if (allDay && dt.hasTime()) {
                // If this is supposed to be an all-day event but DTSTART has time part, clear the time part.
                dt.setHasTime(false);
            } else if (!allDay && !dt.hasTime()) {
                // If the event isn't marked as all-day but DTSTART is date-only, the client simply forgot
                // to mark it all-day.  Do all-day implicitly.
                allDay = true;
                newInv.setIsAllDayEvent(allDay);
            }
            newInv.setDtStart(dt);
        }

        // DTEND (for VEVENT) or DUE (for VTODO)
        Element endElem = element.getOptionalElement(MailConstants.E_CAL_END_TIME);
        if (endElem != null) {
            ParsedDateTime dt = parseDtElement(endElem, tzMap, newInv);
            // fixup for bug 30121
            if (allDay && dt.hasTime()) {
                // If this is supposed to be an all-day event but DTEND has time part, clear the time part.
                dt.setHasTime(false);
            } else if (!allDay && !dt.hasTime()) {
                // If the event isn't marked as all-day but DTEND is date-only, the client simply forgot
                // to mark it all-day.  Do all-day implicitly.
                allDay = true;
                newInv.setIsAllDayEvent(allDay);
            }

            if (allDay && !newInv.isTodo()) {
                // HACK ALERT: okay, campers, here's the deal.
                // By definition, our end dates are EXCLUSIVE: DTEND is not
                // included.. eg a meeting 7-8pm actually stops at 7:59
                //
                // This makes sense for normal appointments, but apparently
                // this rule is confusing to people when making
                // all-day-events
                //
                // For all-day-events, people want to say that a 1-day-long
                // appointment starts on 11/1 and ends on 11/1, for example
                // this is inconsistent (and incompatible with RFC2445) but
                // it is what people want. Sooo, we to a bit of a hacky
                // translation when sending/receiving all-day-events.
                //
                dt = dt.add(ParsedDuration.ONE_DAY);
            }
            newInv.setDtEnd(dt);
        } else {
            // DURATION
            Element d = element.getOptionalElement(MailConstants.E_CAL_DURATION);
            if (d != null) {
                ParsedDuration pd = ParsedDuration.parse(d);
                newInv.setDuration(pd);
            }
        }

        // LOCATION
        newInv.setLocation(location);

        // STATUS
        String status = element.getAttribute(MailConstants.A_CAL_STATUS,
                newInv.isEvent() ? IcalXmlStrMap.STATUS_CONFIRMED : IcalXmlStrMap.STATUS_NEEDS_ACTION);
        validateAttr(IcalXmlStrMap.sStatusMap, MailConstants.A_CAL_STATUS, status);
        newInv.setStatus(status);

        // CLASS
        String classProp = element.getAttribute(MailConstants.A_CAL_CLASS, IcalXmlStrMap.CLASS_PUBLIC);
        validateAttr(IcalXmlStrMap.sClassMap, MailConstants.A_CAL_CLASS, classProp);
        newInv.setClassProp(classProp);

        // PRIORITY
        String priority = element.getAttribute(MailConstants.A_CAL_PRIORITY, null);
        newInv.setPriority(priority);

        if (newInv.isEvent()) {
            // FreeBusy
            String fb = element.getAttribute(MailConstants.A_APPT_FREEBUSY, null);
            if (fb != null) {
                newInv.setFreeBusy(fb);
                // Intended F/B takes precedence over TRANSP.
                if (IcalXmlStrMap.FBTYPE_FREE.equals(fb))
                    newInv.setTransparency(IcalXmlStrMap.TRANSP_TRANSPARENT);
                else
                    newInv.setTransparency(IcalXmlStrMap.TRANSP_OPAQUE);
            } else {
                // TRANSP is examined only when intended F/B is not supplied.
                String transp = element.getAttribute(MailConstants.A_APPT_TRANSPARENCY,
                        IcalXmlStrMap.TRANSP_OPAQUE);
                validateAttr(IcalXmlStrMap.sTranspMap, MailConstants.A_APPT_TRANSPARENCY,
                        transp);
                newInv.setTransparency(transp);

                // If transparent, set intended f/b to free.
                // If opaque, don't set intended f/b because there are multiple possibilities.
                if (newInv.isTransparent())
                    newInv.setFreeBusy(IcalXmlStrMap.FBTYPE_FREE);
            }
        }

        if (newInv.isTodo()) {
            // PERCENT-COMPLETE
            String pctComplete = element.getAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, null);
            newInv.setPercentComplete(pctComplete);

            // COMPLETED
            String completed = element.getAttribute(MailConstants.A_TASK_COMPLETED, null);
            if (completed != null) {
                try {
                    ParsedDateTime c = ParsedDateTime.parseUtcOnly(completed);
                    newInv.setCompleted(c.getUtcTime());
                } catch (ParseException e) {
                    throw ServiceException.INVALID_REQUEST("Invalid COMPLETED value: " + completed, e);
                }
            } else if (status.equals(IcalXmlStrMap.STATUS_COMPLETED)) {
                newInv.setCompleted(System.currentTimeMillis());
            } else {
                newInv.setCompleted(0);
            }
        }

        // ATTENDEEs
        boolean hasAttendees = false;
        for (Iterator<Element> iter = element
                .elementIterator(MailConstants.E_CAL_ATTENDEE); iter.hasNext();) {
            ZAttendee at = ZAttendee.parse(iter.next());
            newInv.addAttendee(at);
            hasAttendees = true;
        }
        if (hasAttendees &&
            newInv.getMethod().equals(ICalTok.PUBLISH.toString())) {
            newInv.setMethod(ICalTok.REQUEST.toString());
        }

        // ORGANIZER
        Element orgElt = element
                .getOptionalElement(MailConstants.E_CAL_ORGANIZER);
        if (orgElt != null) {
            ZOrganizer org = ZOrganizer.parse(orgElt);
            newInv.setOrganizer(org);
        }

        // Once we have organizer and attendee information, we can tell if this account is the
        // organizer in this invite or not.
        newInv.setIsOrganizer(account);

        if (!newInv.isCancel()) {
            // draft flag
            // True means invite has changes that haven't been sent to attendees.
            boolean draft = element.getAttributeBool(MailConstants.A_CAL_DRAFT, false);
            newInv.setDraft(draft);
            // neverSent flag
            // True means attendees have never been notified for this invite.
            boolean neverSent = element.getAttributeBool(MailConstants.A_CAL_NEVER_SENT, false);
            newInv.setNeverSent(neverSent);
        }

        // RECUR
        Element recur = element.getOptionalElement(MailConstants.A_CAL_RECUR);
        if (recur != null) {
            if (!recurAllowed) {
                throw ServiceException.INVALID_REQUEST(
                        "No <recur> allowed in an exception", null);
            }
            // Ensure DTSTART is set if doing recurrence.
            ParsedDateTime st = newInv.getStartTime();
            if (st == null) {
                ParsedDateTime et = newInv.getEndTime();
                if (et != null) {
                    if (et.hasTime())
                        st = et.add(ParsedDuration.NEGATIVE_ONE_SECOND);
                    else
                        st = et.add(ParsedDuration.NEGATIVE_ONE_DAY);
                    newInv.setDtStart(st);
                } else {
                    // Both DTSTART and DTEND are unspecified.  Recurrence makes no sense!
                    throw ServiceException.INVALID_REQUEST("recurrence used without DTSTART", null);
                }
            }
            Recurrence.IRecurrence recurrence = parseRecur(
                    recur, tzMap, newInv.getStartTime(), newInv.getEndTime(),
                    newInv.getDuration(), newInv.getRecurId(), allDay);
            newInv.setRecurrence(recurrence);
        }

        // VALARMs
        Iterator<Element> alarmsIter = element.elementIterator(MailConstants.E_CAL_ALARM);
        while (alarmsIter.hasNext()) {
            Alarm alarm = Alarm.parse(alarmsIter.next());
            if (alarm != null)
                newInv.addAlarm(alarm);
        }

        List<ZProperty> xprops = parseXProps(element);
        for (ZProperty prop : xprops)
            newInv.addXProp(prop);

        newInv.validateDuration();

        //zdsync: must set this only after recur is processed
        if (invId != null) {
            try {
                int invIdInt = Integer.parseInt(invId);
                newInv.setInviteId(invIdInt);
            } catch (NumberFormatException e) {
                // ignore if invId is not a number, e.g. refers to a remote account
            }
        }
        if (dts != null) {
            newInv.setDtStamp(Long.parseLong(dts));
        }
        Element fragment = element.getOptionalElement(MailConstants.E_FRAG);
        if (fragment != null) {
            newInv.setFragment(fragment.getText());
        }
    }

    public static List<ZProperty> parseXProps(Element element) throws ServiceException {
        List<ZProperty> props = new ArrayList<ZProperty>();
        for (Iterator<Element> propIter = element.elementIterator(MailConstants.E_CAL_XPROP);
             propIter.hasNext(); ) {
            Element propElem = propIter.next();
            String propName = propElem.getAttribute(MailConstants.A_NAME);
            String propValue = propElem.getAttribute(MailConstants.A_VALUE, null);
            ZProperty xprop = new ZProperty(propName);
            xprop.setValue(propValue);
            List<ZParameter> xparams = CalendarUtil.parseXParams(propElem);
            for (ZParameter xparam : xparams) {
                xprop.addParameter(xparam);
            }
            props.add(xprop);
        }
        return props;
    }

    public static List<ReplyInfo> parseReplyList(Element element, TimeZoneMap tzMap)
    throws ServiceException {
        List<ReplyInfo> list = new ArrayList<ReplyInfo>();
        for (Iterator<Element> iter = element.elementIterator(MailConstants.E_CAL_REPLY);
             iter.hasNext(); ) {
            Element riElem = iter.next();
            String addr = riElem.getAttribute(MailConstants.A_CAL_ATTENDEE);
            ZAttendee at = new ZAttendee(addr);
            String sentBy = riElem.getAttribute(MailConstants.A_CAL_SENTBY, null);
            if (sentBy != null)
                at.setSentBy(sentBy);
            String partStat = riElem.getAttribute(MailConstants.A_CAL_PARTSTAT, null);
            if (partStat != null)
                at.setPartStat(partStat);
            int seq = (int) riElem.getAttributeLong(MailConstants.A_SEQ);
            long dtStamp = riElem.getAttributeLong(MailConstants.A_DATE);
            RecurId recurId = RecurId.fromXml(riElem, tzMap);
            ReplyInfo ri = new ReplyInfo(at, seq, dtStamp, recurId);
            list.add(ri);
        }
        return list;
    }

    private static void validateAttr(IcalXmlStrMap map, String attrName,
            String value) throws ServiceException {
        if (!map.validXml(value)) {
            throw ServiceException.INVALID_REQUEST("Invalid value '"
                    + value + "' specified for attribute:" + attrName, null);
        }

    }

    public static Invite buildCancelInviteCalendar(
            Account acct, Account senderAcct, boolean asAdmin,
            boolean onBehalfOf, CalendarItem calItem, Invite inv,
            String comment, List<ZAttendee> forAttendees) throws ServiceException {
        return cancelInvite(acct, senderAcct, asAdmin, onBehalfOf, calItem, inv, comment, forAttendees, null);
    }

    public static Invite buildCancelInviteCalendar(
            Account acct, Account senderAcct, boolean asAdmin,
            boolean onBehalfOf, CalendarItem calItem, Invite inv,
            String comment) throws ServiceException {
        return cancelInvite(acct, senderAcct, asAdmin, onBehalfOf, calItem, inv, comment, null, null);
    }

    public static Invite buildCancelInstanceCalendar(
            Account acct, Account senderAcct, boolean asAdmin,
            boolean onBehalfOf, CalendarItem calItem, Invite inv,
            String comment, RecurId recurId) throws ServiceException {
        return cancelInvite(acct, senderAcct, asAdmin, onBehalfOf, calItem, inv, comment, null, recurId);
    }

    /**
     * See 4.2.10
     *
     * Cancel an Invite for specified Attendees (or for ALL attendees if NULL
     * is passed)
     *
     * See RFC2446 4.2.9
     *
     * BEGIN:VCALENDAR PRODID:-//ACME/DesktopCalendar//EN METHOD:CANCEL
     * VERSION:2.0 BEGIN:VEVENT ORGANIZER:Mailto:A@example.com
     * ATTENDEE;TYPE=INDIVIDUAL;Mailto:A@example.com
     * ATTENDEE;TYPE=INDIVIDUAL:Mailto:B@example.com
     * ATTENDEE;TYPE=INDIVIDUAL:Mailto:C@example.com
     * ATTENDEE;TYPE=INDIVIDUAL:Mailto:D@example.com COMMENT:Mr. B cannot
     * attend. It's raining. Lets cancel.
     * UID:calsrv.example.com-873970198738777@example.com SEQUENCE:1
     * STATUS:CANCELLED DTSTAMP:19970613T190000Z END:VEVENT END:VCALENDAR
     *
     *
     * @param inv
     *            Invite being replied to
     * @param comment
     *            Comment message to be included in the response
     * @param forAttendee
     *            The Attendee to be removed from the meeting, or NULL if all
     *            attendees (meeting is cancelled)
     * @param recurId -
     *            the particular instance we are supposed to be cancelling. If
     *            this is nonzero, then inv must be the DEFAULT (recurId=0)
     *            invite!
     * @return
     * @throws ServiceException
     */
    static Invite cancelInvite(
            Account acct, Account senderAcct, boolean asAdmin,
            boolean onBehalfOf,
            CalendarItem calItem, Invite inv, String comment,
            List<ZAttendee> forAttendees, RecurId recurId)
    throws ServiceException {
        boolean allowPrivateAccess = calItem.allowPrivateAccess(senderAcct, asAdmin);
        return cancelInvite(acct, senderAcct, allowPrivateAccess, onBehalfOf,
                            inv, comment, forAttendees, recurId, true);
    }

    private static Invite cancelInvite(
            Account acct, Account senderAcct, boolean asAdmin,
            boolean onBehalfOf,
            Folder folder, Invite inv, String comment,
            List<ZAttendee> forAttendees, RecurId recurId,
            boolean incrementSeq)
    throws ServiceException {
        boolean allowPrivateAccess = CalendarItem.allowPrivateAccess(folder, senderAcct, asAdmin);
        return cancelInvite(acct, senderAcct, allowPrivateAccess, onBehalfOf,
                            inv, comment, forAttendees, recurId, incrementSeq);
    }

    private static Invite cancelInvite(
            Account acct, Account senderAcct, boolean allowPrivateAccess,
            boolean onBehalfOf,
            Invite inv, String comment,
            List<ZAttendee> forAttendees, RecurId recurId,
            boolean incrementSeq)
    throws ServiceException {
        Invite cancel = new Invite(inv.getItemType(), ICalTok.CANCEL.toString(),
                                   inv.getTimeZoneMap(), inv.isOrganizer());

        // ORGANIZER
        if (inv.hasOrganizer()) {
            ZOrganizer org = new ZOrganizer(inv.getOrganizer());
            if (onBehalfOf && senderAcct != null)
                org.setSentBy(senderAcct.getName());
            cancel.setOrganizer(org);
        }

        // ATTENDEEs
        List<ZAttendee> attendees =
            forAttendees != null ? forAttendees : inv.getAttendees();
        for (ZAttendee a : attendees)
            cancel.addAttendee(a);

        cancel.setClassProp(inv.getClassProp());
        boolean showAll = inv.isPublic() || allowPrivateAccess;
        Locale locale = acct.getLocale();
        if (!showAll) {
            // SUMMARY
            String sbj = L10nUtil.getMessage(MsgKey.calendarSubjectWithheld, locale);
            cancel.setName(CalendarMailSender.getCancelSubject(sbj, locale));
        } else {
            // SUMMARY
            cancel.setName(CalendarMailSender.getCancelSubject(inv.getName(), locale));

            // COMMENT
            if (comment != null && !comment.equals(""))
                cancel.addComment(comment);
        }

        // UID
        cancel.setUid(inv.getUid());

        // RECURRENCE-ID
        if (inv.hasRecurId()) {
            // FIXME: if RECURRENCE-ID can be a range (THISANDFUTURE) then we'll
            // need to be smarter here
            cancel.setRecurId(inv.getRecurId());
        } else {
            if (recurId != null) {
                cancel.setRecurId(recurId);
            }
        }

        // all-day
        cancel.setIsAllDayEvent(inv.isAllDayEvent());  // bug 30121

        // DTSTART, DTEND, and LOCATION (Outlook seems to require these, even
        // though they are optional according to RFC2446.)
        ParsedDateTime dtStart = recurId == null ? inv.getStartTime() : recurId.getDt();
        if (dtStart != null) {
            cancel.setDtStart(dtStart);
            ParsedDuration dur = inv.getEffectiveDuration();
            if (dur != null)
                cancel.setDtEnd(dtStart.add(dur));
        }

        // LOCATION
        cancel.setLocation(inv.getLocation());

        // SEQUENCE
        int seq = inv.getSeqNo();
        if (incrementSeq) {
            // Increment only if this account is the organizer.  If this
            // account is a non-organizer attendee, leave the sequence at
            // present value.  (bug 8465)
            if (acct != null && inv.isOrganizer())
                seq++;
        }
        cancel.setSeqNo(seq);

        // STATUS
        cancel.setStatus(IcalXmlStrMap.STATUS_CANCELLED);

        // DTSTAMP
        cancel.setDtStamp(new Date().getTime());

        return cancel;
    }

    /**
     * Move appointments from TASKS type folders to Calendar folder.
     * Also, move tasks from APPOINTMENT type folders to Tasks folder.
     * @param mbox
     * @throws ServiceException
     */

    public static void migrateAppointmentsAndTasks(Mailbox mbox) throws ServiceException {
        // get the list of folders.
        List<Folder> folderList = mbox.getFolderList(null, SortBy.NONE);

        for (Folder folder : folderList) {
            int targetId;
            TypedIdList idlist;
            MailItem.Type type;

            if (folder.getDefaultView() == MailItem.Type.APPOINTMENT) {
                // get tasks from this folder and move them to TASKS folder.
                idlist = mbox.listCalendarItemsForRange(null, MailItem.Type.TASK, -1, -1, folder.getId());
                targetId = Mailbox.ID_FOLDER_TASKS;
                type = MailItem.Type.TASK;
            } else if (folder.getDefaultView() == MailItem.Type.TASK) {
                // get appointments from this folder and move them to Calendar folder.
                idlist = mbox.listCalendarItemsForRange(null, MailItem.Type.APPOINTMENT, -1, -1, folder.getId());
                targetId = Mailbox.ID_FOLDER_CALENDAR;
                type = MailItem.Type.APPOINTMENT;
            } else {
                continue;
            }

            if (!idlist.isEmpty()) {
                if (type == MailItem.Type.APPOINTMENT)
                    ZimbraLog.calendar.info("Migrating " + idlist.size() + " Appointment(s) from '" +
                            folder.getName() + "' to 'Calendar' folder for mailbox " + mbox.getId());
                else
                    ZimbraLog.calendar.info("Migrating " + idlist.size() + " Task(s) from '" +
                            folder.getName() + "' to 'Tasks' folder for mailbox " + mbox.getId());

                int[] items = new int[idlist.size()];
                int i = 0;
                for (Integer id : idlist.getAllIds()) {
                    items[i] = id.intValue();
                    i++;
                }
                mbox.move(null, items, type, targetId, null);
            }
        }
    }

    /**
     * Checks whether two given addresses belong to same account
     * @param address1
     * @param address2
     * @return true if addresses belong to same account, false otherwise.
     * @throws ServiceException
     */

    public static boolean belongToSameAccount(String address1, String address2) throws ServiceException {
        boolean isSameAccount = false;
        if (address1 != null && address2 != null) {
            // check if the addresses are equal
            isSameAccount = address1.equalsIgnoreCase(address2);
            if (!isSameAccount) { // check if the addresses belong to same account
                Account acct = Provisioning.getInstance().get(AccountBy.name, address1);
                if (acct != null) {
                    // local user - consider aliases
                    AccountAddressMatcher acctMatcher = new AccountAddressMatcher(acct);
                    isSameAccount = acctMatcher.matches(address2);
                }
            }
        }
        return isSameAccount;
    }
}
