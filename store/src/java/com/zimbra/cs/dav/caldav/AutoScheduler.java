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
package com.zimbra.cs.dav.caldav;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.CalendarItem.ReplyInfo;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.SetCalendarItemData;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.FriendlyCalendaringDescription;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.OrganizerInviteChanges;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.util.AccountUtil;

public abstract class AutoScheduler {
    public static final String CANCEL_PREFIX = "Cancel: ";
    protected final Mailbox userMailbox;
    protected final Account calendarAccount;
    protected final Invite origInvites[];
    protected final SetCalendarItemData scidDefault;
    protected final SetCalendarItemData scidExceptions[];
    // newInvites is a convenient collection of invites from scidDefault/scidExceptions
    protected final List<Invite> newInvites = Lists.newArrayList();
    protected final int calendarMailItemId;
    protected final int flags;
    protected final String[] tags;
    protected final List<ReplyInfo> replies;
    protected final DavContext ctxt;
    protected final Address from;
    protected final Address sender;
    protected final ZOrganizer organizer;

    /**
     * @param calendarMailbox mailbox for the calendar that is being updated
     * @param origInvites invites corresponding to Calendar entry BEFORE this update
     * @param scidDefault default invite information from this update
     * @param scidExceptions exceptions invite information from this update
     * @param ctxt
     */
    protected AutoScheduler(Mailbox userMailbox, Mailbox calendarMailbox,
            Invite origInvites[], int calendarMailItemId, int flags,
            String[] tags, SetCalendarItemData scidDefault, SetCalendarItemData scidExceptions[],
            List<ReplyInfo> replies, DavContext ctxt)
    throws ServiceException {
        this.userMailbox = userMailbox;
        if (calendarMailbox == null) {
            calendarAccount = null;
            from = null;
            sender = null;
        } else {
            calendarAccount = calendarMailbox.getAccount();
            from = AccountUtil.getFriendlyEmailAddress(calendarAccount);
            // Only set sender if not same as from
            if (ctxt.getAuthAccount().equals(calendarAccount)) {
                sender = null;
            } else {
                sender = AccountUtil.getFriendlyEmailAddress(ctxt.getAuthAccount());
            }
        }
        this.origInvites = (origInvites == null) ? new Invite[0] : origInvites;
        this.calendarMailItemId = calendarMailItemId;
        this.flags = flags;
        this.tags = tags;
        this.scidDefault = scidDefault;
        this.scidExceptions = scidExceptions;
        this.replies = replies;
        if (scidDefault != null) {
            newInvites.add(scidDefault.invite);
            if (scidExceptions != null) {
                for (SetCalendarItemData scid : scidExceptions) {
                    newInvites.add(scid.invite);
                }
            }
            organizer = scidDefault.invite.getOrganizer();
        } else if (origInvites.length >=1) {
            organizer = origInvites[0].getOrganizer();
        } else {
            organizer = null;
        }
        this.ctxt = ctxt;
    }

    private static boolean isEmpty(Invite invites[]) {
        return (invites == null) || (invites.length <= 0);
    }

    public abstract CalendarItem doSchedulingActions() throws ServiceException;

    private static void addSchedulingMsg(List<AutoScheduleMsg> msgs, AutoScheduleMsg msg) {
        if (msg == null) {
            return;
        }
        if (ZimbraLog.dav.isDebugEnabled()) {
            Invite inv = msg.calendarInvite;
            if (inv != null) {
                String recurId = inv.getRecurId() == null ? "NONE" : inv.getRecurId().toString();
                ZimbraLog.dav.debug("Will schedule msg with method %s for UID %s Recurrence ID %s\n%s",
                        msg.msgMethod, inv.getUid(), recurId, ZimbraLog.getStackTrace(5));
            }
        }
        msgs.add(msg);
    }

    /**
     * Used for deletions
     */
    public static AutoScheduler getAutoScheduler(Mailbox userMailbox, Mailbox calendarMailbox,
            Invite origInvites[], int calendarMailItemId, DavContext context) {
        return getAutoScheduler(userMailbox, calendarMailbox,
            origInvites, calendarMailItemId, 0, null /* tags */, null, null, null, context);
    }

    public static AutoScheduler getAutoScheduler(Mailbox userMailbox, Mailbox calendarMailbox,
            Invite origInvites[], int calendarMailItemId, int flags, String[] tags, SetCalendarItemData scidDefault,
            SetCalendarItemData scidExceptions[], List<ReplyInfo> replies, DavContext context) {
        if (!DavResource.isCalendarAutoSchedulingEnabled()) {
            return null;
        }
        if (calendarMailbox == null) {
            return null;  // We don't know who to act as
        }
        ZOrganizer organizer = null;
        Account organizerAcct = null;
        try {
            if (scidDefault == null || scidDefault.invite == null) {
                if (isEmpty(origInvites)) {
                    return null;  // No new or old invites!
                }
                organizer = origInvites[0].getOrganizer();
                organizerAcct = origInvites[0].getOrganizerAccount();
            } else {
                organizer = scidDefault.invite.getOrganizer();
                organizerAcct = scidDefault.invite.getOrganizerAccount();
            }
            if (organizer == null) {
                return null; // no organizer means no scheduling
            }
            // TODO:  Does the auth user have scheduling capability for the calendar
            if (calendarMailbox.getAccount().sameAccount(organizerAcct)) {
                return new OrganizerAutoScheduler(userMailbox, calendarMailbox, origInvites,
                        calendarMailItemId, flags, tags, scidDefault, scidExceptions, replies, context);
            } else {
                return new AttendeeAutoScheduler(userMailbox, calendarMailbox, origInvites,
                        calendarMailItemId, flags, tags, scidDefault, scidExceptions, replies, context);
            }
        } catch (ServiceException e) {
            ZimbraLog.dav.debug("Hit this whilst getting AutoScheduler", e);
        }
        return null;
    }

    protected CalendarItem persistToCalendar() throws ServiceException {
        if (null == scidDefault) {
            return null;  // Presumably a DELETE
        }
        return userMailbox.setCalendarItem(ctxt.getOperationContext(), calendarMailItemId, flags, tags,
                    scidDefault, scidExceptions, replies, CalendarItem.NEXT_ALARM_KEEP_CURRENT);
    }

    protected CalendarItem processSchedulingMessages(List<AutoScheduleMsg> msgs) throws ServiceException {
        for (AutoScheduleMsg msg : msgs) {
            msg.updateCalendarInfo();
        }
        CalendarItem newCalItem = persistToCalendar();

        for (AutoScheduleMsg msg : msgs) {
            msg.sendSchedulingMsg();
        }
        return newCalItem;
    }

    protected class AutoScheduleMsg {
        Invite calendarInvite;
        ICalTok msgMethod;
        List<Address> recipients;
        String subject;
        protected AutoScheduleMsg(Invite calendarInvite, ICalTok msgMethod, List<Address> recipients, String subject) {
            this.calendarInvite = calendarInvite;
            this.msgMethod = msgMethod;
            this.recipients = recipients;
            this.subject = subject;
        }

        public void updateCalendarInfo() {
            if (recipients == null || recipients.isEmpty()) {
                return;
            }
            if (ICalTok.REQUEST.equals(msgMethod)) {
                // Ios7 and Ios8 write a calendar entry without any setting for RSVP
                calendarInvite.setRsvp(true);
                calendarInvite.setMethod(msgMethod.toString());  // Also needed to force it to be treated as invite?
                for (ZAttendee attendee : calendarInvite.getAttendees()) {
                    if (!attendee.hasRsvp() || !attendee.getRsvp()) {
                        for (Address recip : recipients) {
                            try {
                                attendee.getFriendlyAddress();
                            } catch (ServiceException e) {
                                ZimbraLog.dav.debug("Problem getting friendly address for attendee %s", attendee, e);
                            }
                            if (attendee.addressMatches(recip)) {
                                attendee.setRsvp(true);
                            }
                        }
                    }
                    if (Strings.isNullOrEmpty(attendee.getRole())) {
                        attendee.setRole(IcalXmlStrMap.ROLE_REQUIRED);  // ios8 doesn't set a role
                    }
                    if (Strings.isNullOrEmpty(attendee.getPartStat())) {
                        attendee.setPartStat(IcalXmlStrMap.PARTSTAT_NEEDS_ACTION); // ios8 doesn't set a partstat
                    }
                }
            }
        }

        public void sendSchedulingMsg() {
            if (recipients == null || recipients.isEmpty()) {
                ZimbraLog.dav.trace("Not generating a scheduling message for UID=%s start=%s - no recipients",
                        calendarInvite.getUid(), calendarInvite.getStartTime());
                return;
            }
            try {
                Invite msgInvite = calendarInvite.newCopy();
                msgInvite.setMethod(msgMethod.toString());
                if (ICalTok.REQUEST.equals(msgMethod)) {
                    for (ZAttendee attendee : msgInvite.getAttendees()) {
                        attendee.setRsvp(true);
                        attendee.setPartStat(IcalXmlStrMap.PARTSTAT_NEEDS_ACTION);
                    }
                } else if (ICalTok.CANCEL.equals(msgMethod)) {
                    msgInvite.setStatus(IcalXmlStrMap.STATUS_CANCELLED);
                    msgInvite.setTransparency(IcalXmlStrMap.TRANSP_TRANSPARENT);
                    for (ZAttendee attendee : msgInvite.getAttendees()) {
                        attendee.setRsvp(null);
                        attendee.setPartStat(IcalXmlStrMap.PARTSTAT_NEEDS_ACTION);
                    }
                } else if (ICalTok.REPLY.equals(msgMethod)) {
                    // Only replying on behalf of self, so remove other attendees
                    List<ZAttendee> attendees = msgInvite.getAttendees();
                    Iterator<ZAttendee> attendeeIter = attendees.iterator();
                    while (attendeeIter.hasNext()) {
                        ZAttendee attendee = attendeeIter.next();
                        if (!attendee.addressMatches(from)) {
                            attendeeIter.remove();
                        }
                    }
                }
                FriendlyCalendaringDescription friendlyDesc = new FriendlyCalendaringDescription(
                        msgInvite, ctxt.getAuthAccount());
                String desc = friendlyDesc.getAsPlainText();
                String descHtml = friendlyDesc.getAsHtml();
                String uid = msgInvite.getUid();
                ZVCalendar cal = msgInvite.newToICalendar(true);
                Account acct = Provisioning.getInstance().getAccountByName(ctxt.getUser());
                if (acct == null) {
                    throw ServiceException.FAILURE("Could not load account for "+ctxt.getUser(), null);
                }
                Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
                MimeMessage mm = CalendarMailSender.createCalendarMessage(ctxt.getAuthAccount(), from, sender,
                        recipients, subject, desc, descHtml, uid, cal, msgInvite.getIcalendarAttaches(), true);
                mbox.getMailSender().setSendPartial(true).sendMimeMessage(
                        ctxt.getOperationContext(), mbox, true, mm, null, null, null, null, false);
            } catch (ServiceException e) {
                ZimbraLog.dav.info("Exception thrown when creating auto scheduling message", e);
            }
        }
    }

    public static class OrganizerAutoScheduler extends AutoScheduler {
        protected OrganizerAutoScheduler(Mailbox userMailbox, Mailbox calendarMailbox, Invite origInvites[],
                int calendarMailItemId, int flags, String[] tags,
                SetCalendarItemData scidDefault, SetCalendarItemData scidExceptions[],
                List<ReplyInfo> replies, DavContext ctxt)
        throws ServiceException {
            super(userMailbox, calendarMailbox, origInvites,
                    calendarMailItemId, flags, tags, scidDefault, scidExceptions, replies, ctxt);
        }

        @Override
        public CalendarItem doSchedulingActions() throws ServiceException {
            if (isEmpty(origInvites)) {
                return doSchedulingActionsForNewCreate();
            } else {
                return doSchedulingActionsForUpdate();
            }
        }

        private CalendarItem doSchedulingActionsForNewCreate() throws ServiceException {
            List<AutoScheduleMsg> msgs = Lists.newArrayList();
            // Always deal with series first
            Invite seriesOrSingletonInvite = Invite.matchingInvite(newInvites, null);
            List<Address> seriesAttendees = Lists.newArrayListWithCapacity(0); // don't like null
            if (seriesOrSingletonInvite != null) {
                seriesAttendees = getRecipientsForAttendees(seriesOrSingletonInvite);
                if (!seriesAttendees.isEmpty()) {
                    addSchedulingMsg(msgs, new AutoScheduleMsg(seriesOrSingletonInvite, ICalTok.REQUEST,
                            seriesAttendees, seriesOrSingletonInvite.getName() /*subject*/));
                }
            }
            for (Invite inv : newInvites) {
                if (inv != seriesOrSingletonInvite) {
                    List<Address> exceptAttendees = getRecipientsForAttendees(inv);
                    addSchedulingMsg(msgs,
                            new AutoScheduleMsg(inv, ICalTok.REQUEST, exceptAttendees, inv.getName() /*subject*/));
                    // Send cancels to any attendees in the series who aren't invited to this.
                    seriesAttendees.removeAll(exceptAttendees);
                    if (!seriesAttendees.isEmpty()) {
                        addSchedulingMsg(msgs, new AutoScheduleMsg(inv, ICalTok.CANCEL, seriesAttendees,
                                CANCEL_PREFIX + inv.getName() /*subject*/));
                    }
                }
            }
            return processSchedulingMessages(msgs);
        }

        private CalendarItem doSchedulingActionsForUpdate() throws ServiceException {
            List<AutoScheduleMsg> msgs = Lists.newArrayList();
            // Always deal with series first
            Invite oldSeriesOrSingletonInvite = Invite.matchingInvite(origInvites, null);

            Invite newInvite = Invite.matchingInvite(newInvites,
                    (oldSeriesOrSingletonInvite == null) ? null : oldSeriesOrSingletonInvite.getRecurId());
            OrganizerInviteChanges changeInfo = new OrganizerInviteChanges(oldSeriesOrSingletonInvite, newInvite);
            if (changeInfo.inviteCanceled()) {
                addSchedulingMsg(msgs, new AutoScheduleMsg(oldSeriesOrSingletonInvite, ICalTok.CANCEL,
                        getRecipientsForAttendees(oldSeriesOrSingletonInvite),
                        CANCEL_PREFIX + changeInfo.getSubject()));
            } else {
                if (changeInfo.isChanged()) {
                    List<Address> recips = changeInfo.isReplyInvalidatingChange() ?
                            getRecipientsForAttendees(newInvite) :
                                getRecipientsForAttendees(changeInfo.getAttendeesOnlyInNew());
                    if (!recips.isEmpty()) {
                        bumpSequenceNumberIfNecessary(oldSeriesOrSingletonInvite, newInvite);
                        addSchedulingMsg(msgs,
                                new AutoScheduleMsg(newInvite, ICalTok.REQUEST, recips, changeInfo.getSubject()));
                    }
                }
                if (oldSeriesOrSingletonInvite != null) {
                    List<Address> uninvited = getRecipientsForAttendees(changeInfo.getAttendeesOnlyInOld());
                    if (!uninvited.isEmpty()) {
                        addSchedulingMsg(msgs, new AutoScheduleMsg(oldSeriesOrSingletonInvite, ICalTok.CANCEL,
                                uninvited, CANCEL_PREFIX + oldSeriesOrSingletonInvite.getName()));
                    }
                }
                // handle EXDATEs.  Note:Mac OSX Mavericks Calendar single instance delete doesn't increment sequence
                // Theoretically, if the invite has changed, these should have been handled by the above but
                // ZWC UI and probably others don't make it clear that cancels have happened.
                addCancelsForNewExdates(msgs, changeInfo);
            }
            //  Look at old invites
            for (Invite inv : origInvites) {
                if (inv == oldSeriesOrSingletonInvite) {
                    continue; // already processed this
                }
                newInvite = Invite.matchingInvite(newInvites, inv.getRecurId());
                if (newInvite == null) {
                    addSchedulingMsg(msgs, new AutoScheduleMsg(inv, ICalTok.CANCEL, getRecipientsForAttendees(inv),
                            CANCEL_PREFIX + inv.getName() /*subject*/));
                } else if (newInvite.isNewerVersion(inv)) {
                    changeInfo = new OrganizerInviteChanges(inv, newInvite);
                    if (changeInfo.isChanged()) {
                        List<Address> recips = changeInfo.isReplyInvalidatingChange() ?
                                getRecipientsForAttendees(newInvite) :
                                getRecipientsForAttendees(changeInfo.getAttendeesOnlyInNew());
                        if (!recips.isEmpty()) {
                            bumpSequenceNumberIfNecessary(inv, newInvite);
                            addSchedulingMsg(msgs,
                                    new AutoScheduleMsg(newInvite, ICalTok.REQUEST, recips, changeInfo.getSubject()));
                        }
                    }
                    List<Address> uninvited = getRecipientsForAttendees(changeInfo.getAttendeesOnlyInOld());
                    if (!uninvited.isEmpty()) {
                        addSchedulingMsg(msgs,
                                new AutoScheduleMsg(inv, ICalTok.CANCEL, uninvited, CANCEL_PREFIX + inv.getName()));
                    }
                }
            }

            Invite newSeriesOrSingletonInvite = Invite.matchingInvite(newInvites, null);
            //  Look at new invites for any new ones
            for (Invite inv : newInvites) {
                Invite origInvite = Invite.matchingInvite(origInvites, inv.getRecurId());
                if (origInvite != null) {
                    continue;
                }
                List<Address> recips = getRecipientsForAttendees(inv);
                addSchedulingMsg(msgs, new AutoScheduleMsg(inv, ICalTok.REQUEST, recips, inv.getName() /*subject*/));
                List<Address> cancelRecips = getRecipientsForAttendees(newSeriesOrSingletonInvite);
                // Send a CANCEL to any ATTENDEES for the series that have been removed for this NEW instance
                for (Address addr: recips) {
                    Iterator<Address> recipsIter = cancelRecips.iterator();
                    while (recipsIter.hasNext()) {
                        Address recip = recipsIter.next();
                        if (addr.equals(recip)) {
                            recipsIter.remove();
                        }
                    }
                }
                if (!cancelRecips.isEmpty()) {
                    addSchedulingMsg(msgs,
                            new AutoScheduleMsg(inv, ICalTok.CANCEL, cancelRecips, CANCEL_PREFIX + inv.getName()));
                }
            }
            return processSchedulingMessages(msgs);
        }

        /**
         * Apple Mac OS X Yosemite Calendar client doesn't seem to like incrementing the sequence number, even for
         * changes like moving the start date.  This can cause an update not too look like it needs a response and for
         * the calendar of an attendee to be left unchanged.  This is a horrible hack and should be removed if
         * possible in the future.
         *
         * Only call this if we are aware of a major change done by or on the Organizer's behalf
         *
         * @param oldInvite
         * @param newInvite
         */
        private void bumpSequenceNumberIfNecessary(Invite oldInvite, Invite newInvite) {
            if ((oldInvite == null) || (newInvite == null)) {
                return;
            }
            int newSeqNum = newInvite.getSeqNo();
            if (newSeqNum == oldInvite.getSeqNo()) {
                ZimbraLog.dav.debug("Update has same SEQUENCE %d as previous rev. Incrementing on behalf of client",
                        newSeqNum);
                newInvite.setSeqNo(newSeqNum + 1);
            }
        }

        private void addCancelsForNewExdates(List<AutoScheduleMsg> msgs,
                OrganizerInviteChanges changeInfo) {
            for (Instance exdateOnlyInNew: changeInfo.getExdatesOnlyInNew()) {
                Invite cancel = changeInfo.newInvite.newCopy();
                cancel.setRecurrence(null);
                ParsedDateTime start = ParsedDateTime.fromUTCTime(exdateOnlyInNew.getStart());
                cancel.setDtStart(start);
                cancel.setRecurId(new RecurId(start, RecurId.RANGE_NONE));
                cancel.setDtEnd(ParsedDateTime.fromUTCTime(exdateOnlyInNew.getEnd()));
                addSchedulingMsg(msgs, new AutoScheduleMsg(cancel, ICalTok.CANCEL, getRecipientsForAttendees(cancel),
                        CANCEL_PREFIX + cancel.getName() /*subject*/));
            }
        }

        /**
         * Will always return a list
         */
        public List<Address> getRecipientsForAttendees(List<ZAttendee> attendees) {
            if (attendees == null || attendees.isEmpty()) {
                return Lists.newArrayListWithCapacity(0);
            }
            List<Address> recipients = Lists.newArrayListWithCapacity(attendees.size());
            for (ZAttendee attendee : attendees) {
                try {
                    if (AccountUtil.addressMatchesAccount(calendarAccount, attendee.getAddress())) {
                        continue;  // Don't send to the organizer
                    }
                } catch (ServiceException se) {
                }
                try {
                    recipients.add(attendee.getFriendlyAddress());
                } catch (MailServiceException mse) {
                    ZimbraLog.dav.info("Problem adding attendee %s to recipient list - ignoring", attendee, mse);
                }
            }
            return recipients;
        }

        public List<Address> getRecipientsForAttendees(Invite inv) {
            if (inv == null) {
                return Lists.newArrayListWithCapacity(0);
            }
            return getRecipientsForAttendees(inv.getAttendees());
        }
    }

    public static class AttendeeAutoScheduler extends AutoScheduler {
        private boolean scheduleReplyWanted = true;
        protected AttendeeAutoScheduler(Mailbox userMailbox, Mailbox calendarMailbox, Invite origInvites[], int calendarMailItemId,
                int flags, String[] tags, SetCalendarItemData scidDefault, SetCalendarItemData scidExceptions[],
                List<ReplyInfo> replies, DavContext ctxt)
        throws ServiceException {
            super(userMailbox, calendarMailbox, origInvites,
                    calendarMailItemId, flags, tags, scidDefault, scidExceptions, replies, ctxt);
            HttpServletRequest httpRequest = ctxt.getRequest();
            if ((httpRequest != null) && ("DELETE".equalsIgnoreCase(httpRequest.getMethod()))) {
                String hdrScheduleReply = httpRequest.getHeader(DavProtocol.HEADER_SCHEDULE_REPLY);
                scheduleReplyWanted = ((hdrScheduleReply == null) || !"F".equals(hdrScheduleReply));
            }
        }

        @Override
        public CalendarItem doSchedulingActions() throws ServiceException {
            List<AutoScheduleMsg> msgs = Lists.newArrayList();
            if (organizer == null) {
                return persistToCalendar();
            }
            if (!scheduleReplyWanted) {
                ZimbraLog.dav.debug("Skipping scheduling actions because HTTP header '%s=F'",
                        DavProtocol.HEADER_SCHEDULE_REPLY);
                return persistToCalendar();
            }
            Address organizerAddress;
            try {
                organizerAddress = organizer.getFriendlyAddress();
            } catch (MailServiceException mse) {
                ZimbraLog.dav.info("Problem creating Address for organizer %s - ignoring", organizer, mse);
                return persistToCalendar();
            }
            for (Invite inv : newInvites) {
                ZAttendee attendee = getMatchingAttendee(inv.getAttendees());
                if (attendee == null) {
                    continue;
                }
                String partStat = attendee.getPartStat();
                if ((partStat == null) || !REPLY_PARTSTAT_SUBJECT_MAP.containsKey(partStat)) {
                    continue;
                }
                if (!isEmpty(origInvites)) {
                    Invite origInvite = Invite.matchingInvite(origInvites, inv.getRecurId());
                    if (origInvite != null) {
                        ZAttendee origAttendee = getMatchingAttendee(origInvite.getAttendees());
                        if (origAttendee != null) {
                            if (partStat.equals(origAttendee.getPartStat())) {
                                continue; // No change
                            }
                        }
                    }
                }
                StringBuilder subject = new StringBuilder();
                subject.append(REPLY_PARTSTAT_SUBJECT_MAP.get(partStat)).append(inv.getName());
                addSchedulingMsg(msgs, new AutoScheduleMsg(inv, ICalTok.REPLY, Lists.newArrayList(organizerAddress),
                        subject.toString()));
            }
            // Process deletions
            for (Invite inv : origInvites) {
                Invite newInvite = Invite.matchingInvite(newInvites, inv.getRecurId());
                if (newInvite == null) {
                    Invite decline = inv.newCopy();
                    List<ZAttendee> attendees = decline.getAttendees();
                    Iterator<ZAttendee> attendeeIter = attendees.iterator();
                    while (attendeeIter.hasNext()) {
                        ZAttendee attendee = attendeeIter.next();
                        if (attendee.addressMatches(from)) {
                            attendee.setPartStat(IcalXmlStrMap.PARTSTAT_DECLINED);
                        } else {
                            attendeeIter.remove();
                        }
                    }
                    String subject = new StringBuilder(
                            REPLY_PARTSTAT_SUBJECT_MAP.get(IcalXmlStrMap.PARTSTAT_DECLINED))
                            .append(decline.getName()).toString();
                    decline.setName(subject);
                    addSchedulingMsg(msgs,
                            new AutoScheduleMsg(decline, ICalTok.REPLY, Lists.newArrayList(organizerAddress), subject));
                }
            }
            return processSchedulingMessages(msgs);
        }

        private ZAttendee getMatchingAttendee(List<ZAttendee> attendees) {
            if (attendees == null || attendees.isEmpty()) {
                return null;
            }
            for (ZAttendee attendee : attendees) {
                try {
                    if (AccountUtil.addressMatchesAccount(calendarAccount, attendee.getAddress())) {
                        return attendee;
                    }
                } catch (ServiceException e) {
                    ZimbraLog.dav.debug("problem matching attendee '%s' to account '%s'",
                            attendee, calendarAccount.getName(), e);
                }
            }
            return null;
        }

        static final Map<String, String> REPLY_PARTSTAT_SUBJECT_MAP = ImmutableMap.of(
            IcalXmlStrMap.PARTSTAT_ACCEPTED, "Accept: ",
            IcalXmlStrMap.PARTSTAT_TENTATIVE, "Tentative: ",
            IcalXmlStrMap.PARTSTAT_DECLINED, "Decline: "
        );
    }
}
