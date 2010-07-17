/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Feb 17, 2005
 */
package com.zimbra.cs.mailbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.mail.internet.MimeMessage;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.fb.FreeBusy.FBInstance;
import com.zimbra.cs.fb.FreeBusy.Interval;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender.Verb;
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
        if (!inv.isOrganizer()) {
            ZAttendee at = getReplyList().getEffectiveAttendee(getMailbox().getAccount(), inv, inst);
            if (at != null) {
                if (at.hasPartStat())
                    return inv.partStatToFreeBusyActual(at.getPartStat());
                else
                    return inv.getFreeBusyActual();
            }
        }
        return inv.getFreeBusyActual();
    }

    public static class Conflict {
        private Instance mInstance;
        private FreeBusy mFreeBusy;
        private String mFreeBusyStatus;

        public Conflict(Instance inst, String fbStatus, FreeBusy fb) {
            mInstance = inst;
            mFreeBusyStatus = fbStatus;
            mFreeBusy = fb;
        }

        public Instance getInstance() { return mInstance; }
        public FreeBusy getFreeBusy() { return mFreeBusy; }
        public boolean isBusy() { return isBusy(mFreeBusyStatus); }

        public static boolean isBusy(String fbStatus) {
            return IcalXmlStrMap.FBTYPE_BUSY.equals(fbStatus)
                   || IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE.equals(fbStatus);
        }
    }

    private static String getBusyTimesString(
            OperationContext octxt, Mailbox mbox, List<Conflict> list, TimeZone tz, Locale lc,
            boolean hasMoreConflicts)
    throws ServiceException {
        StringBuilder sb = new StringBuilder();
        int conflictCount = 0;
        for (Conflict avail : list) {
            if (!avail.isBusy()) continue;

            // List conflicting appointments and their organizers.
            FreeBusy fb = avail.getFreeBusy();
            List<FBInstance> instances = new ArrayList<FBInstance>();
            for (Iterator<Interval> iter = fb.iterator(); iter.hasNext(); ) {
                Interval interval = iter.next();
                // busy intervals only
                if (Conflict.isBusy(interval.getStatus())) {
                    // busy appointments only
                    for (FBInstance fbinst : interval.getInstances()) {
                        if (Conflict.isBusy(fbinst.getFreeBusy()))
                            instances.add(fbinst);
                    }
                }
            }
            for (FBInstance instance : instances) {
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

    private static final int TWENTY_FIVE_HOURS = 25 * 60 * 60 * 1000;
    private static final int TWO_HOURS = 2 * 60 * 60 * 1000;

    private static String getDeclinedTimesString(
            OperationContext octxt, Mailbox mbox, List<Conflict> conflicts, boolean allDay,
            TimeZone tz, Locale lc)
    throws ServiceException {
        StringBuilder sb = new StringBuilder();
        int conflictCount = 0;
        for (Conflict conflict : conflicts) {
            // List declined instances' start times.
            Instance instance = conflict.getInstance();
            Date startDate = new Date(instance.getStart());
            Date endDate = new Date(instance.getEnd());
            if (!allDay) {
                String start = CalendarMailSender.formatDateTime(startDate, tz, lc);
                sb.append(" * ").append(start);
                if (DateTimeUtil.sameDay(startDate, endDate, tz)) {
                    String end = CalendarMailSender.formatTime(endDate, tz, lc);
                    sb.append(" - ").append(end);
                } else {
                    String end = CalendarMailSender.formatDateTime(endDate, tz, lc);
                    sb.append("\r\n   - ").append(end);
                }
            } else {
                // all-day appointments
                String start = CalendarMailSender.formatDate(startDate, tz, lc);
                sb.append(" * ").append(start);
                // If each instance is two days or longer, show the end date.
                long duration = endDate.getTime() - startDate.getTime();
                if (duration > TWENTY_FIVE_HOURS) {  // longest 1-day = 25 hours on DST "fall back" day
                    // Bring back end date by 2 hours so the displayed date is inclusive end date,
                    // not exclusive.  Without DST, we can bring it back by just 1 millisecond, but
                    // if the instance spans the DST "fall back" date we have a 25-hour day.  Negating
                    // that requires pulling back end date by at least 1 hour and 1 millisecond.
                    // Pulling back by 2 hours should work in all cases.
                    endDate = new Date(instance.getEnd() - TWO_HOURS);
                    String end = CalendarMailSender.formatDate(endDate, tz, lc);
                    sb.append(" - ").append(end);
                }
            }
            sb.append("\r\n");
            conflictCount++;
        }
        return sb.toString();
    }

    private static class ConflictCheckResult {
        private List<Conflict> mConflicts;
        private boolean mTooManyConflicts;
        private boolean mHasMoreConflicts;

        public ConflictCheckResult(List<Conflict> conflicts, boolean tooManyConflicts, boolean hasMoreConflicts) {
            mConflicts = conflicts;
            mTooManyConflicts = tooManyConflicts;
            mHasMoreConflicts = hasMoreConflicts;
        }

        public List<Conflict> getConflicts() { return mConflicts; }
        public boolean tooManyConflicts() { return mTooManyConflicts; }
        public boolean hasMoreConflicts() { return mHasMoreConflicts; }
    }

    private static final int MIN_CONFLICT_LIST_SIZE = 5;

    private ConflictCheckResult checkAvailability(long now, Invite invite, int maxNumConflicts, int maxPctConflicts)
    throws ServiceException {

        long st, et;
        if (invite.isRecurrence()) {
            // Resource is getting invited to a recurring appointment.
            st = getStartTime();
            et = getEndTime();
        } else {
            // Resource is getting invited to a single instance.
            ParsedDateTime dtStart = invite.getStartTime();
            ParsedDateTime dtEnd = invite.getEffectiveEndTime();
            if (dtStart != null && dtEnd != null) {
                st = dtStart.getUtcTime();
                et = dtEnd.getUtcTime();
            } else {
                // Start time and/or end time can't be determined.  Give up.
                return null;
            }
        }
        // Ignore conflicts in the past.
        st = Math.max(st, now);
        if (et <= st)
            return null;

        OperationContext octxt = new OperationContext(getAccount());
        Collection<Instance> instances;
        if (invite.isRecurrence()) {
            instances = expandInstances(st, et, false);
        } else {
            instances = new ArrayList<Instance>(1);
            instances.add(Instance.fromInvite(getId(), invite));
        }
        if (instances == null || instances.isEmpty())
            return null;

        int maxByPct = maxPctConflicts * instances.size() / 100;
        int maxConflicts = Math.min(maxNumConflicts, maxByPct);

        List<Conflict> list = new ArrayList<Conflict>();
        int numConflicts = 0;
        boolean hasMoreConflicts = false;
        for (Instance inst : instances) {
            if (numConflicts > Math.max(maxConflicts, MIN_CONFLICT_LIST_SIZE - 1)) {
                hasMoreConflicts = true;
                break;
            }
            long start = inst.getStart();
            long end = inst.getEnd();
            // Run free/busy search of this user between instance start/end times.
            FreeBusy fb = getMailbox().getFreeBusy(octxt, start, end, this);
            String status = fb.getBusiest();
            if (Conflict.isBusy(status)) {
                list.add(new Conflict(inst, status, fb));
                numConflicts++;
            }
        }
        return new ConflictCheckResult(list, numConflicts > maxConflicts, hasMoreConflicts);
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
        long opTime = octxt != null ? octxt.getTimestamp() : System.currentTimeMillis();

        Account account = getMailbox().getAccount();
        boolean onBehalfOf = false;
        Account authAcct = account;
        if (octxt != null) {
            Account authuser = octxt.getAuthenticatedUser();
            if (authuser != null) {
                onBehalfOf = !account.getId().equalsIgnoreCase(authuser.getId());
                if (onBehalfOf)
                    authAcct = authuser;
            }
        }
        boolean asAdmin = octxt != null ? octxt.isUsingAdminPrivileges() : false;
        boolean allowPrivateAccess = allowPrivateAccess(authAcct, asAdmin);

        String partStat = defaultPartStat;
        if (player != null) {
            String p = player.getCalendarItemPartStat();
            if (p != null) partStat = p;
        }

        // See if we have RSVP=FALSE for the attendee.  Let's assume RSVP was requested unless it is
        // explicitly set to FALSE.
        boolean rsvpRequested = true;
        ZAttendee attendee = invite.getMatchingAttendee(account);
        if (attendee != null) {
            Boolean rsvp = attendee.getRsvp();
            if (rsvp != null)
                rsvpRequested = rsvp.booleanValue();
        }
        RedoLogProvider redoProvider = RedoLogProvider.getInstance();
        // Don't send reply emails if we're not on master (in redo-driven master/replica setup).
        // Don't send reply emails if we're replaying redo for reasons other than crash recovery.
        // In other words, we DO want to send emails during crash recovery, because we're completing
        // an interrupted transaction.  But we don't send emails during redo reply phase of restore.
        // Also don't send emails for cancel invites.  (Organizer doesn't expect reply for cancels.)
        // And don't send emails for task requests.
        boolean needReplyEmail =
            rsvpRequested &&
            redoProvider.isMaster() &&
            (player == null || redoProvider.getRedoLogManager().getInCrashRecovery()) &&
            invite.hasOrganizer() &&
            !invite.isCancel() &&
            !invite.isTodo();

        if (invite.isOrganizer()) {
            // Organizer always accepts.
            partStat = IcalXmlStrMap.PARTSTAT_ACCEPTED;
        } else if (account instanceof CalendarResource && octxt == null) {
            // Auto accept/decline processing should only occur during email delivery.  In particular,
            // don't do it if we're here during ics import.  We're in email delivery if octxt == null.
            // (There needs to be a better way to determine that...)

            boolean replySent = false;
            CalendarResource resource = (CalendarResource) account;
            Locale lc;
            Account organizer = invite.getOrganizerAccount();
            if (organizer != null)
                lc = organizer.getLocale();
            else
                lc = resource.getLocale();
            if (resource.autoAcceptDecline() || resource.autoDeclineIfBusy() || resource.autoDeclineRecurring()) {
                boolean replyListUpdated = false;
                // We'll accept unless one of the checks below fails.
                // If auto-accept is enabled, assume it'll be accepted until it gets declined.
                if (resource.autoAcceptDecline())
                    partStat = IcalXmlStrMap.PARTSTAT_ACCEPTED;
                if (isRecurring() && resource.autoDeclineRecurring()) {
                    // Decline because resource is configured to decline all recurring appointments.
                    partStat = IcalXmlStrMap.PARTSTAT_DECLINED;
                    if (needReplyEmail) {
                        String reason =
                            L10nUtil.getMessage(MsgKey.calendarResourceDeclineReasonRecurring, lc);
                        Invite replyInv = makeReplyInvite(
                                account, authAcct, lc, onBehalfOf, allowPrivateAccess, invite, invite.getRecurId(),
                                CalendarMailSender.VERB_DECLINE);
                        CalendarMailSender.sendResourceAutoReply(
                                octxt, mbox, true,
                                CalendarMailSender.VERB_DECLINE, false,
                                reason + "\r\n",
                                this, invite, new Invite[] { replyInv }, mmInv);
                        replySent = true;
                    }
                } else if (resource.autoDeclineIfBusy()) {
                    // Auto decline is enabled.  Let's check for conflicts.
                    int maxNumConflicts = resource.getMaxNumConflictsAllowed();
                    int maxPctConflicts = resource.getMaxPercentConflictsAllowed();
                    ConflictCheckResult checkResult = checkAvailability(opTime, invite, maxNumConflicts, maxPctConflicts);
                    if (checkResult != null) {
                        List<Conflict> conflicts = checkResult.getConflicts();
                        if (conflicts.size() > 0) {
                            if (invite.isRecurrence() && !checkResult.tooManyConflicts()) {
                                // There are some conflicts, but within resource's allowed limit.
                                if (resource.autoAcceptDecline()) {
                                    // Let's accept partially.  (Accept the series and decline conflicting instances.)
                                    List<Invite> replyInvites = new ArrayList<Invite>();
                                    // the REPLY for the ACCEPT of recurrence series
                                    Invite acceptInv = makeReplyInvite(
                                            account, authAcct, lc, onBehalfOf, allowPrivateAccess, invite, invite.getRecurId(),
                                            CalendarMailSender.VERB_ACCEPT);
    
                                    for (Conflict conflict : conflicts) {
                                        Instance inst = conflict.getInstance();
                                        InviteInfo invInfo = inst.getInviteInfo();
                                        Invite inv = getInvite(invInfo.getMsgId(), invInfo.getComponentId());
                                        RecurId rid = inst.makeRecurId(inv);
    
                                        // Record the decline status in reply list.
                                        getReplyList().modifyPartStat(
                                                resource, rid, null, resource.getName(), null, null,
                                                IcalXmlStrMap.PARTSTAT_DECLINED, false, invite.getSeqNo(), opTime);
                                        replyListUpdated = true;
    
                                        // Make REPLY VEVENT for the declined instance.
                                        Invite replyInv = makeReplyInvite(
                                                account, authAcct, lc, onBehalfOf, allowPrivateAccess, inv, rid,
                                                CalendarMailSender.VERB_DECLINE);
                                        replyInvites.add(replyInv);
                                    }
    
                                    if (needReplyEmail) {
                                        ICalTimeZone tz = chooseReplyTZ(invite);
                                        // Send one email to accept the series.
                                        String declinedInstances = getDeclinedTimesString(
                                                octxt, mbox, conflicts, invite.isAllDayEvent(), tz, lc);
                                        String msg =
                                            L10nUtil.getMessage(MsgKey.calendarResourceDeclinedInstances, lc) +
                                            "\r\n\r\n" + declinedInstances;
                                        CalendarMailSender.sendResourceAutoReply(
                                                octxt, mbox, true,
                                                CalendarMailSender.VERB_ACCEPT, true,
                                                msg,
                                                this, invite, new Invite[] { acceptInv }, mmInv);
                                        // Send another email to decline instances, all in one email.
                                        String conflictingTimes = getBusyTimesString(octxt, mbox, conflicts, tz, lc, false);
                                        msg =
                                            L10nUtil.getMessage(MsgKey.calendarResourceDeclinedInstances, lc) +
                                            "\r\n\r\n" + declinedInstances + "\r\n" +
                                            L10nUtil.getMessage(MsgKey.calendarResourceDeclineReasonConflict, lc) +
                                            "\r\n\r\n" + conflictingTimes;
                                        CalendarMailSender.sendResourceAutoReply(
                                                octxt, mbox, true,
                                                CalendarMailSender.VERB_DECLINE, true,
                                                msg,
                                                this, invite, replyInvites.toArray(new Invite[0]), mmInv);
                                        replySent = true;
                                    }
                                } else {
                                    // Auto-accept is not enabled.  Auto-decline is enabled, but there weren't
                                    // enough conflicting instances to decline outright.  So we just stay
                                    // silent and let the human admin deal with it.  This case is rather
                                    // ambiguous, and can be avoided by configuring the resource to allow
                                    // zero conflicting instance.
                                }
                            } else {
                                // Too many conflicts.  Decline outright.
                                partStat = IcalXmlStrMap.PARTSTAT_DECLINED;
                                if (needReplyEmail) {
                                    ICalTimeZone tz = chooseReplyTZ(invite);
                                    String msg =
                                        L10nUtil.getMessage(MsgKey.calendarResourceDeclineReasonConflict, lc) +
                                        "\r\n\r\n" +
                                        getBusyTimesString(octxt, mbox, conflicts, tz, lc, checkResult.hasMoreConflicts());
                                    Invite replyInv = makeReplyInvite(
                                            account, authAcct, lc, onBehalfOf, allowPrivateAccess, invite, invite.getRecurId(),
                                            CalendarMailSender.VERB_DECLINE);
                                    CalendarMailSender.sendResourceAutoReply(
                                            octxt, mbox, true,
                                            CalendarMailSender.VERB_DECLINE, false,
                                            msg,
                                            this, invite, new Invite[] { replyInv }, mmInv);
                                    replySent = true;
                                }
                            }
                        }
                    }
                }
                if (!replySent && IcalXmlStrMap.PARTSTAT_ACCEPTED.equals(partStat)) {
                    if (needReplyEmail) {
                        Invite replyInv = makeReplyInvite(
                                account, authAcct, lc, onBehalfOf, allowPrivateAccess, invite, invite.getRecurId(),
                                CalendarMailSender.VERB_ACCEPT);
                        CalendarMailSender.sendResourceAutoReply(
                                octxt, mbox, true,
                                CalendarMailSender.VERB_ACCEPT, false,
                                null,
                                this, invite, new Invite[] { replyInv }, mmInv);
                    }
                }
                // Record the final outcome in the replies list.
                if (IcalXmlStrMap.PARTSTAT_NEEDS_ACTION.equals(partStat)) {
                    getReplyList().modifyPartStat(
                            resource, invite.getRecurId(), null, resource.getName(), null, null,
                            partStat, false, invite.getSeqNo(), opTime);
                    replyListUpdated = true;
                }
                if (forCreate && replyListUpdated)
                    saveMetadata();
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

    // Figure out the timezone to use for expressing start/end times for
    // conflicting meetings.  Do our best to use a timezone familiar to the
    // organizer.
    private ICalTimeZone chooseReplyTZ(Invite invite) throws ServiceException {
        Account account = getMailbox().getAccount();
        Account organizer = invite.getOrganizerAccount();
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
        return tz;
    }

    private Invite makeReplyInvite(Account account, Account authAccount, Locale lc,
                                   boolean onBehalfOf, boolean allowPrivateAccess,
                                   Invite inv, RecurId rid, Verb verb)
    throws ServiceException {
        boolean hidePrivate = !inv.isPublic() && !allowPrivateAccess;
        String subject;
        if (hidePrivate)
            subject = L10nUtil.getMessage(MsgKey.calendarSubjectWithheld, lc);
        else
            subject = inv.getName();
        String replySubject = CalendarMailSender.getReplySubject(verb, subject, lc);
        ParsedDateTime ridDt = rid != null ? rid.getDt() : null;
        Invite replyInv = CalendarMailSender.replyToInvite(
                account, authAccount, onBehalfOf, allowPrivateAccess, inv, verb, replySubject, ridDt);
        return replyInv;
    }
}
