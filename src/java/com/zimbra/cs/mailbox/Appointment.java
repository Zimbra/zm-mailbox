/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Feb 17, 2005
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.mail.internet.MimeMessage;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.fb.FreeBusy.FBInstance;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCache;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.op.CreateCalendarItemPlayer;
import com.zimbra.cs.redolog.op.CreateCalendarItemRecorder;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateTimeUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;


/**
 * An APPOINTMENT consists of one or more INVITES in the same series -- ie that
 * have the same UID. From the appointment you can get the INSTANCES which are
 * the start/end times of each occurence.
 * 
 * Sample Appointment: APPOINTMENT UID=1234 (two INVITES above) ...Instances on
 * every monday with name "Gorilla Discussion" EXCEPT for the 21st, where we
 * talk about lefties instead. CANCELED for the 28th
 */
public class Appointment extends CalendarItem {

    public Appointment(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_APPOINTMENT)
            throw new IllegalArgumentException();
    }

    /**
     * Return this accounts "effective" FBA data -- ie the FBA that is the result of the most recent and 
     * most specific (specific b/c some replies might be for just one instance, some might be for recurrence-id=0, 
     * etc) given the requested Invite and Instance to check against.
     * 
     * For example, imagine an appt with no exceptions, but two replies:
     *    RECUR=0, REPLY=accept (reply to the default invite, accept it)
     *    RECUR=20051010 REPLY=decline (reply to DECLINE the instance on 10/10/2005
     * 
     * The FBA for the 10/10 instance will obviously be different than the one for any other instance.  If you
     * add Exceptions into the mix, then there are even more permutations.
     * 
     * @param inv
     * @param inst
     * @return
     * @throws ServiceException
     */
    public String getEffectiveFreeBusyActual(Invite inv, Instance inst) throws ServiceException {
        Account acct = getMailbox().getAccount();
        ZAttendee at = getReplyList().getEffectiveAttendee(acct, inv, inst);
        if (at == null || inv.isOrganizer()) {
            return inv.getFreeBusyActual();
        }

        if (at.hasPartStat()) {
            return inv.partStatToFreeBusyActual(at.getPartStat());
        } else {
            return inv.getFreeBusyActual();
        }
    }

    // code related to calendar resources
    // TODO: move this stuff to its own class(es)

    public static class Availability {
        private long mStart;
        private long mEnd;
        private FreeBusy mFreeBusy;
        private String mFreeBusyStatus;

        public Availability(long start, long end, String fbStatus, FreeBusy fb) {
            mStart = start;
            mEnd = end;
            mFreeBusyStatus = fbStatus;
            mFreeBusy = fb;
        }

        public long getStartTime() { return mStart; }
        public long getEndTime() { return mEnd; }
        public FreeBusy getFreeBusy() { return mFreeBusy; }
        public boolean isBusy() {
            return
                IcalXmlStrMap.FBTYPE_BUSY.equals(mFreeBusyStatus) ||
                IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE.equals(mFreeBusyStatus);                
        }

        public static boolean isAvailable(List<Availability> list) {
            for (Availability avail : list) {
                if (avail.isBusy()) return false;
            }
            return true;
        }

        public static final int MAX_CONFLICT_LIST_SIZE = 5;

        public static String getBusyTimesString(
                OperationContext octxt, Mailbox mbox, List<Availability> list, TimeZone tz, Locale lc)
        throws ServiceException {
            StringBuilder sb = new StringBuilder();
            boolean hasMoreConflicts = false;
            int conflictCount = 0;
            for (Availability avail : list) {
                if (conflictCount >= MAX_CONFLICT_LIST_SIZE) {
                    hasMoreConflicts = true;
                    break;
                }
                if (!avail.isBusy()) continue;

                // List conflicting appointments and their organizers.
                FreeBusy fb = avail.getFreeBusy();
                LinkedHashSet<FBInstance> instances = fb.getAllInstances();
                for (FBInstance instance : instances) {
                    if (conflictCount >= MAX_CONFLICT_LIST_SIZE) {
                        hasMoreConflicts = true;
                        break;
                    }

                    Date startDate = new Date(instance.getStartTime());
                    Date endDate = new Date(instance.getEndTime());
                    String start = CalendarMailSender.formatDateTime(startDate, tz, lc);
                    sb.append(" * ").append(start);
                    String end;
                    if (DateTimeUtil.sameDay(startDate, endDate, tz)) {
                        end = CalendarMailSender.formatTime(endDate, tz, lc);
                        sb.append(" - ").append(end);
                    } else {
                        end = CalendarMailSender.formatDateTime(endDate, tz, lc);
                        sb.append("\r\n   - ").append(end);
                    }

                    int apptId = instance.getApptId();
                    long recurIdDt = instance.getRecurIdDt();
                    CalendarItem appt = mbox.getCalendarItemById(octxt, apptId);
                    Invite inv = appt.getInviteForRecurId(recurIdDt);
                    if (inv != null && inv.hasOrganizer()) {
                        ZOrganizer organizer = inv.getOrganizer();
                        String orgDispName;
                        if (organizer.hasCn())
                            orgDispName = organizer.getCn() + " <" + organizer.getAddress() + ">";
                        else
                            orgDispName = organizer.getAddress();
                        sb.append(L10nUtil.getMessage(MsgKey.calendarResourceConflictScheduledBy, lc, orgDispName));
                    }
                    sb.append("\r\n");
                    conflictCount++;
                }
            }
            if (hasMoreConflicts)
                sb.append(" * ...\r\n");
            return sb.toString();
        }
    }

    // TODO: Running free/busy search over many recurring instances is
    // very expensive...  Recurrence expansion itself is expensive, and
    // each F/B call is expensive as well.  Need a more efficient way.
    private List<Availability> checkAvailability()
    throws ServiceException {

        // Only look between now and appointment end time.  Resource is
        // available if end time is in the past.
        long now = System.currentTimeMillis();
        long st = Math.max(getStartTime(), now);
        long et = getEndTime();
        if (et < now)
            return null;

        Collection<Instance> instances = expandInstances(st, et, false);
        List<Availability> list = new ArrayList<Availability>(instances.size());
        int numConflicts = 0;
        try {
            for (Instance inst : instances) {
                if (numConflicts > Availability.MAX_CONFLICT_LIST_SIZE)
                    break;
                long start = inst.getStart();
                long end = inst.getEnd();
                FreeBusy fb =
                    com.zimbra.cs.fb.LocalFreeBusyProvider.getFreeBusyList(getMailbox(), start, end, this);
                String status = fb.getBusiest();
                if (!IcalXmlStrMap.FBTYPE_FREE.equals(status)) {
                    list.add(new Availability(start, end, status, fb));
                    numConflicts++;
                }
            }
        } finally {
            // Ugly hack because availability check during appointment update
            // ends up updating calendar cache with not-yet-committed modseq.
            // If we don't clear the cache here, the next read from the cache
            // won't show the current appointment being created/updated.
            //
            // We'll cheat and only invalidate the cache for the folder containing
            // the current appointment.  The other calendar folders' modseq are
            // being advanced unnecessarily, but that shouldn't cause harm because
            // we're doing the present update with the entire mailbox locked.
            //
            // The proper fix for all of this is to do auto-accept check outside
            // and prior to the appointment create/update transaction.
            CalendarCache.getInstance().invalidateSummary(getMailbox(), getFolderId());
        }
        return list;
    }

    protected String processPartStat(Invite invite,
                                    MimeMessage mmInv,
                                    boolean forCreate,
                                    String defaultPartStat)
    throws ServiceException {
        Mailbox mbox = getMailbox();
        OperationContext octxt = mbox.getOperationContext();
        CreateCalendarItemPlayer player =
            octxt != null ? (CreateCalendarItemPlayer) octxt.getPlayer() : null;

        Account account = getMailbox().getAccount();
        Locale lc;
        Account organizer = invite.getOrganizerAccount();
        if (organizer != null)
            lc = organizer.getLocale();
        else
            lc = account.getLocale();

        String partStat = defaultPartStat;
        if (player != null) {
            String p = player.getCalendarItemPartStat();
            if (p != null) partStat = p;
        }

        RedoLogProvider redoProvider = RedoLogProvider.getInstance();
        boolean needResourceAutoReply =
            redoProvider.isMaster() &&
            (player == null || redoProvider.getRedoLogManager().getInCrashRecovery()) &&
            !invite.isCancel() &&
            !invite.isTodo();

        if (invite.isOrganizer()) {
            // Organizer always accepts.
            partStat = IcalXmlStrMap.PARTSTAT_ACCEPTED;
        } else if (account instanceof CalendarResource && needResourceAutoReply) {
            CalendarResource resource = (CalendarResource) account;
            if (resource.autoAcceptDecline()) {
                partStat = IcalXmlStrMap.PARTSTAT_ACCEPTED;
                if (isRecurring() && resource.autoDeclineRecurring()) {
                    partStat = IcalXmlStrMap.PARTSTAT_DECLINED;
                    if (invite.hasOrganizer()) {
                        String reason =
                            L10nUtil.getMessage(MsgKey.calendarResourceDeclineReasonRecurring, lc);
                        CalendarMailSender.sendReply(
                                octxt, mbox, false,
                                CalendarMailSender.VERB_DECLINE,
                                reason + "\r\n",
                                this, invite, mmInv);
                    }
                } else if (resource.autoDeclineIfBusy()) {
                    List<Availability> avail = checkAvailability();
                    if (avail != null && !Availability.isAvailable(avail)) {
                        partStat = IcalXmlStrMap.PARTSTAT_DECLINED;
                        if (invite.hasOrganizer()) {
                            // Figure out the timezone to use for expressing start/end times for
                            // conflicting meetings.  Do our best to use a timezone familiar to the
                            // organizer.
                            ICalTimeZone tz = invite.getStartTime().getTimeZone();
                            if (tz == null && invite.isAllDayEvent()) {
                                // floating time: use resource's timezone
                                tz = ICalTimeZone.getAccountTimeZone(account);
                                if (tz == null)
                                    ICalTimeZone.getUTC();
                            } else {
                                // tz != null || !allday
                                if (tz == null || tz.sameAsUTC()) {
                                    if (organizer != null) {
                                        // For this case, let's assume the sender didn't really mean UTC.
                                        // This happens with Outlook and possibly more clients.
                                        tz = ICalTimeZone.getAccountTimeZone(organizer);
                                    } else {
                                        // If organizer is not a local user, use resource's timezone.
                                        tz = ICalTimeZone.getAccountTimeZone(account);
                                        if (tz == null)
                                            ICalTimeZone.getUTC();
                                    }
                                } else {
                                    // Timezone is not UTC.  We can safely assume the client sent the
                                    // correct local timezone.
                                }
                            }

                            String msg =
                                L10nUtil.getMessage(MsgKey.calendarResourceDeclineReasonConflict, lc) +
                                "\r\n\r\n" +
                                Availability.getBusyTimesString(octxt, mbox, avail, tz, lc);
                            CalendarMailSender.sendReply(
                                    octxt, mbox, false,
                                    CalendarMailSender.VERB_DECLINE,
                                    msg,
                                    this, invite, mmInv);
                        }
                    }
                }
                if (IcalXmlStrMap.PARTSTAT_ACCEPTED.equals(partStat)) {
                    if (invite.hasOrganizer()) {
                        CalendarMailSender.sendReply(
                                octxt, mbox, false,
                                CalendarMailSender.VERB_ACCEPT,
                                null,
                                this, invite, mmInv);
                    }
                }
            }
        }

        CreateCalendarItemRecorder recorder =
            (CreateCalendarItemRecorder) mbox.getRedoRecorder();
        recorder.setCalendarItemPartStat(partStat);

        invite.updateMyPartStat(account, partStat);
        if (forCreate) {
            Invite defaultInvite = getDefaultInviteOrNull();
            if (defaultInvite != null && !defaultInvite.equals(invite) &&
                !partStat.equals(defaultInvite.getPartStat())) {
                defaultInvite.updateMyPartStat(account, partStat);
                saveMetadata();
            }
        }

        return partStat;
    }
}
