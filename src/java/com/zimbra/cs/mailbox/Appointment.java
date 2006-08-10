/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Feb 17, 2005
 */
package com.zimbra.cs.mailbox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
import com.zimbra.cs.mailbox.calendar.FreeBusy;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.MimeVisitor;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.op.CreateAppointmentPlayer;
import com.zimbra.cs.redolog.op.CreateAppointmentRecorder;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.DateTimeUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.cs.util.Pair;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.util.L10nUtil.MsgKey;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * An APPOINTMENT consists of one or more INVITES in the same series -- ie that
 * have the same UID. From the appointment you can get the INSTANCES which are
 * the start/end times of each occurence.
 * 
 * Sample Appointment: APPOINTMENT UID=1234 (two INVITES above) ...Instances on
 * every monday with name "Gorilla Discussion" EXCEPT for the 21st, where we
 * talk about lefties instead. CANCELED for the 28th
 */
public class Appointment extends MailItem {

    static Log sLog = LogFactory.getLog(Appointment.class);

    private String mUid;

    /** the time IN MSEC UTC that this appointment "starts" */
    private long mStartTime;
    /** the time IN MSEC UTC that this appointment "ends" */
    private long mEndTime;

    private Recurrence.IRecurrence mRecurrence;
    private TimeZoneMap mTzMap;

    private List<Invite> mInvites;
    
    private ReplyList mReplyList;

    public TimeZoneMap getTimeZoneMap() { return mTzMap; }

    public Appointment(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_APPOINTMENT)
            throw new IllegalArgumentException();
    }

    public Recurrence.IRecurrence getRecurrence() {
        return mRecurrence;
    }

    public boolean isRecurring() {
        return (mRecurrence != null);
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }
    
    public void saveMetadata() throws ServiceException {
        super.saveMetadata();
    }
    
    public void markItemModified(int reason) {
        mMailbox.markItemModified(this, reason);
    }
    

    boolean isTaggable()       { return true; }
    boolean isCopyable()       { return false; }
    boolean isMovable()        { return true; }
    public boolean isMutable() { return true; }
    boolean isIndexed()        { return true; }
    boolean canHaveChildren()  { return false; }


    static Appointment create(int id, Folder folder, short volumeId, String tags,
                              String uid, ParsedMessage pm, Invite firstInvite)
    throws ServiceException {
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        Mailbox mbox = folder.getMailbox();

        List<Invite> invites = new ArrayList<Invite>();
        invites.add(firstInvite);

        Recurrence.IRecurrence recur = firstInvite.getRecurrence();
        long startTime, endTime;
        if (recur != null) {
            startTime = recur.getStartTime().getUtcTime();
            endTime = recur.getEndTime().getUtcTime();
        } else {
            startTime = firstInvite.getStartTime().getUtcTime();
            if (firstInvite.getEndTime() != null)
                endTime = firstInvite.getEndTime().getUtcTime();
            else if (firstInvite.getDuration() != null)
                endTime = firstInvite.getDuration().addToTime(startTime);
            else
                endTime = startTime;
        }

        Account account = mbox.getAccount();
        firstInvite.updateMyPartStat(account, firstInvite.getPartStat());

        UnderlyingData data = new UnderlyingData();
        data.id       = id;
        data.type     = TYPE_APPOINTMENT;
        data.folderId = folder.getId();
        data.indexId  = id;
        data.imapId   = id;
        data.volumeId = volumeId;
        data.date     = mbox.getOperationTimestamp();
        data.tags     = Tag.tagsToBitmask(tags);
        data.sender   = uid;
        data.metadata = encodeMetadata(DEFAULT_COLOR, uid, startTime, endTime, recur, invites, firstInvite.getTimeZoneMap(), new ReplyList());
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        Appointment appt = new Appointment(mbox, data);
        appt.processPartStat(firstInvite,
                             pm != null ? pm.getMimeMessage() : null,
                             true,
                             IcalXmlStrMap.PARTSTAT_NEEDS_ACTION);
        appt.createBlob(pm, firstInvite, volumeId);
        appt.finishCreation(null);

        DbMailItem.addToAppointmentTable(appt);
        return appt;
    }

    private boolean updateRecurrence() throws ServiceException {
        long startTime, endTime;

        // update our recurrence rule, start with the initial rule
        Invite firstInv = getDefaultInvite();
        if (firstInv == null) {
            return false;
        }
        if (firstInv.getRecurrence() != null) {
            mRecurrence = (Recurrence.RecurrenceRule) firstInv.getRecurrence().clone();
            
            // now, go through the list of invites and find all the exceptions
            for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
                Invite cur = (Invite) iter.next();
                
                if (cur != firstInv) {
                    
                    if (cur.getMethod().equals(ICalTok.REQUEST.toString()) || (cur.getMethod().equals(ICalTok.PUBLISH.toString()))) {
                        assert (cur.hasRecurId());
                        
                        if (cur.hasRecurId()) {
                            Recurrence.ExceptionRule exceptRule = (Recurrence.ExceptionRule) cur.getRecurrence();
                            if (exceptRule == null) {
                                // create a false ExceptionRule wrapper around the
                                // single-instance
                                exceptRule = new Recurrence.ExceptionRule(
                                        cur.getRecurId(),
                                        cur.getStartTime(), 
                                        cur.getEffectiveDuration(),
                                        new InviteInfo(cur)
                                        );
                            } else {
                                exceptRule = (Recurrence.ExceptionRule) exceptRule.clone();
                            }
                            ((Recurrence.RecurrenceRule) mRecurrence).addException(exceptRule);
                        } else {
                            sLog.debug("Got second invite with no RecurID: " + cur.toString());
                        }
                    } else if (cur.getMethod().equals(ICalTok.CANCEL.toString())) {
                        assert(cur.hasRecurId());
                        if (cur.hasRecurId()) {
                            Recurrence.CancellationRule cancelRule =   
                                new Recurrence.CancellationRule(cur.getRecurId());
                            
                            ((Recurrence.RecurrenceRule) mRecurrence).addException(cancelRule);
                        }
                    }
                }
            }
            
            // update the start and end time in the Appointment table if
            // necessary
            ParsedDateTime dtStartTime = mRecurrence.getStartTime();
            ParsedDateTime dtEndTime = mRecurrence.getEndTime();
            
            startTime = dtStartTime.getUtcTime();
            endTime = dtEndTime.getUtcTime();
        } else {
            mRecurrence = null;
            ParsedDateTime dtStart = firstInv.getStartTime();
            ParsedDateTime dtEnd = firstInv.getEffectiveEndTime();
            
            startTime = dtStart.getUtcTime();
            endTime = dtEnd.getUtcTime();
        }
        
        if (mStartTime != startTime || mEndTime != endTime) {
            mStartTime = startTime;
            mEndTime = endTime;
            DbMailItem.updateInAppointmentTable(this);
        }
        return true;
    }

    public static final String FN_APPT_RECURRENCE = "apptRecur";

    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);

        int mdVersion = meta.getVersion();

        mUid = meta.get(Metadata.FN_UID, null);
        mInvites = new ArrayList<Invite>();
        
        ICalTimeZone accountTZ = Provisioning.getInstance().getTimeZone(getMailbox().getAccount()); 
        if (mdVersion < 6) {
            mStartTime = 0;
            mEndTime = 0;
        } else {
            mTzMap = TimeZoneMap.decodeFromMetadata(meta.getMap(Metadata.FN_TZMAP), accountTZ);

            // appointment start and end
            mStartTime = meta.getLong(Metadata.FN_APPT_START, 0);
            mEndTime = meta.getLong(Metadata.FN_APPT_END, 0);

            // invite ID's
            long numComp = meta.getLong(Metadata.FN_NUM_COMPONENTS);
            for (int i = 0; i < numComp; i++) {
                Metadata md = meta.getMap(Metadata.FN_INV + i);
                mInvites.add(Invite.decodeMetadata(getMailboxId(), md, this, accountTZ));
            }

            Metadata metaRecur = meta.getMap(FN_APPT_RECURRENCE, true);
            if (metaRecur != null) {
                mRecurrence = Recurrence.decodeRule(metaRecur, mTzMap);
            }
            
            if (meta.containsKey(Metadata.FN_REPLY_LIST)) {
                mReplyList = ReplyList.decodeFromMetadata(meta.getMap(Metadata.FN_REPLY_LIST), mTzMap);
            } else {
                mReplyList = new ReplyList();
            }
        }
    }

    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mUid, mStartTime, mEndTime, mRecurrence, mInvites, mTzMap, mReplyList);
    }
    private static String encodeMetadata(byte color, String uid, long startTime, long endTime,
                                         Recurrence.IRecurrence recur, List /*Invite */ invs, TimeZoneMap tzmap, ReplyList replyList) {
        return encodeMetadata(new Metadata(), color, uid, startTime, endTime, recur, invs, tzmap, replyList).toString();
    }
    static Metadata encodeMetadata(Metadata meta, byte color, String uid, long startTime, long endTime,
                                   Recurrence.IRecurrence recur, List /*Invite */ invs, TimeZoneMap tzmap, ReplyList replyList) {
        meta.put(Metadata.FN_TZMAP, tzmap.encodeAsMetadata());
        meta.put(Metadata.FN_UID, uid);
        meta.put(Metadata.FN_APPT_START, startTime);
        meta.put(Metadata.FN_APPT_END, endTime);
        meta.put(Metadata.FN_NUM_COMPONENTS, invs.size());
        
        meta.put(Metadata.FN_REPLY_LIST, replyList.encodeAsMetadata());
        
        int num = 0;
        for (Iterator iter = invs.iterator(); iter.hasNext();) {
            Invite comp = (Invite) iter.next();
            String compName = Metadata.FN_INV + num++;
            meta.put(compName, Invite.encodeMetadata(comp));
        }

        if (recur != null)
            meta.put(FN_APPT_RECURRENCE, recur.encodeMetadata());

        return MailItem.encodeMetadata(meta, color);
    }

    // /**
    // * Expand all the instances for the time period from start to end
    // *
    // * @param start
    // * @param end
    // * @return list of Instances for the specified time period
    // */
    public Collection /* Instance */expandInstances(long start, long end) {
        List<Instance> instances = new ArrayList<Instance>();

        if (mRecurrence != null) {
            instances = mRecurrence.expandInstances(this, start, end);
        } else {
            if (mInvites != null) {
                for (Iterator iter = mInvites.iterator(); iter.hasNext(); ) {
                    Invite inv = (Invite) iter.next();
                    assert(inv.getStartTime() != null);
                    long invStart = inv.getStartTime().getUtcTime();
                    long invEnd = inv.getEffectiveEndTime().getUtcTime();
                    if (invStart < end && invEnd > start) {
                        Instance inst = new Instance(this, new InviteInfo(inv),
                                                     invStart, invEnd,
                                                     inv.hasRecurId());
                        instances.add(inst);
                    }
                }
            }
        }
        return instances;
    }

    public static class Instance implements Comparable {
        private long mStart; // calculated start time of this instance

        private long mEnd; // calculated end time of this instance

        private boolean mIsException; // TRUE if this instance is an exception
                                        // to a recurrence

        private InviteInfo mInvId;
        
        private Appointment mAppt;

        /**
         * Create an Instance object using data in an Invite that points to
         * a specific instance of an Appointment.
         * @param inv
         * @return
         */
        public static Instance fromInvite(Appointment appt, Invite inv) {
            return new Instance(appt,
                                new InviteInfo(inv),
                                inv.getStartTime().getUtcTime(),
                                inv.getEffectiveEndTime().getUtcTime(),
                                inv.hasRecurId());
        }

        public Instance(Appointment appt, InviteInfo invInfo, long _start, long _end,
                boolean _exception) 
        {
            mInvId = invInfo;
            mAppt = appt;
            mStart = _start;
            mEnd = _end;
            mIsException = _exception;
        }

        public int compareTo(Object o) {
            Instance other = (Instance) o;

            long toRet = mAppt.getId() - other.mAppt.getId();
            if (toRet == 0) {
                toRet = mStart - other.mStart;
                if (toRet == 0) {
                    toRet = mEnd - other.mEnd;
                    if (toRet == 0) {
                        toRet = mInvId.compareTo(other.mInvId);
                    }
                }
            }
            if (toRet > 0) {
                return 1;
            } else if (toRet < 0) {
                return -1;
            }
            return 0;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Instance)) {
                return false;
            }

            Instance other = (Instance) o;
            return (mStart == other.mStart) && (mEnd == other.mEnd)
                    && (mInvId.equals(other.mInvId));
        }

        public String toString() {
            StringBuilder toRet = new StringBuilder("INST(");
            Date dstart = new Date(mStart);
            Date dend = new Date(mEnd);
            toRet.append(dstart).append(",").append(dend).append(",").append(
                    mIsException);
            toRet.append(",ID=").append(mInvId.getMsgId()).append("-").append(
                    mInvId.getComponentId());
            toRet.append(")");
            return toRet.toString();
        }
        
        public Appointment getAppointment() { return mAppt; }
        public int getMailItemId() { return mInvId.getMsgId(); }
        public int getComponentNum() { return mInvId.getComponentId(); }
        public long getStart() { return mStart; }
        public long getEnd() { return mEnd; }
        public boolean isException() { return mIsException; }
        public void setIsException(boolean isException) { mIsException = isException; } 
        public InviteInfo getInviteInfo() { return mInvId; } 
    }

    void setUid(String uid) {
        mUid = uid;
    }

    public String getUid() {
        return mUid;
    }

    public Invite getInvite(InviteInfo id) {
        for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
            Invite inv = (Invite)iter.next();
            InviteInfo inf = new InviteInfo(inv);
            if (inf.compareTo(id) == 0) {
                return inv;
            }
        }
        return null;
    }
    
    public Invite getInvite(int invId, int compNum) {
        for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
            Invite inv = (Invite)iter.next();
            if (inv.getMailItemId() == invId && inv.getComponentNum() == compNum) {
                return inv;
            }
        }
        return null;
    }
    
    public Invite[] getInvites() {
        ArrayList<Invite> toRet = new ArrayList<Invite>();
        for (Invite inv : mInvites)
            toRet.add(inv);
        return toRet.toArray(new Invite[0]);
    }
    
    public Invite[] getInvites(int invId) {
        ArrayList<Invite> toRet = new ArrayList<Invite>();
        for (Invite inv : mInvites)
            if (inv.getMailItemId() == invId)
                toRet.add(inv);
        return toRet.toArray(new Invite[0]);
    }
    
    public int numInvites() {
        return mInvites.size();
    }

    public Invite getInvite(int num) {
        return mInvites.get(num);
    }
    
    public Invite getDefaultInvite() {
        Invite first = null;
        for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
            Invite cur = (Invite) iter.next();
            if (!cur.hasRecurId())
                return cur;
            if (first == null)
                first = cur;
        }
        if (first == null)
            ZimbraLog.calendar.error(
                    "Invalid state: appointment " + getId() + " in mailbox " + getMailbox().getId() + " has no default invite; " +
                    (mInvites != null ? ("invite count = " + mInvites.size()) : "null invite list"));
        return first;
    }

    void processNewInvite(ParsedMessage pm,
                          Invite invite,
                          boolean force, int folderId, short volumeId)
    throws ServiceException {
        processNewInvite(pm, invite, force, folderId, volumeId, false);
    }

    /**
     * A new Invite has come in, take a look at it and see what needs to happen.
     * Maybe we need to send updates out. Maybe we need to modify the
     * Appointment table.
     * 
     * @param invite
     * @param force if true, then force update to this appointment,
     *              otherwise use RFC2446 sequencing rules
     */
    void processNewInvite(ParsedMessage pm,
                          Invite invite,
                          boolean force, int folderId, short volumeId,
                          boolean replaceExistingInvites)
    throws ServiceException {
        ZOrganizer originalOrganizer = getDefaultInvite().getOrganizer();
        if (replaceExistingInvites) {
            mInvites.clear();
            //saveMetadata();
        }
        String method = invite.getMethod();
        if (method.equals(ICalTok.REQUEST.toString()) || method.equals(ICalTok.CANCEL.toString()) || method.equals(ICalTok.PUBLISH.toString())) {
            processNewInviteRequestOrCancel(originalOrganizer, pm, invite, force, folderId, volumeId);
        } else if (method.equals("REPLY")) {
            processNewInviteReply(pm, invite, force);
        }
    }
    
    private void processNewInviteRequestOrCancel(ZOrganizer originalOrganizer,
                                                 ParsedMessage pm,
                                                 Invite newInvite,
                                                 boolean force,
                                                 int folderId,
                                                 short volumeId)
    throws ServiceException {
        String method = newInvite.getMethod();

        // Remove everyone that is made obselete by this request
        boolean addNewOne = true;
        boolean isCancel = method.equals(ICalTok.CANCEL.toString());

        if (!canAccess(isCancel ? ACL.RIGHT_DELETE : ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on this appointment");

        // If we're doing a modify rather than cancel, make sure the organizer
        // in the new Invite is the same as the original organizer.
        if (!isCancel) {
            if (newInvite.hasOrganizer()) {
                String newOrgAddr = newInvite.getOrganizer().getAddress();
                if (originalOrganizer == null)
                    throw ServiceException.INVALID_REQUEST(
                            "Changing organizer of an appointment is not allowed: old=(unspecified), new=" + newOrgAddr, null);
                String origOrgAddr = originalOrganizer.getAddress();
                if (!newOrgAddr.equalsIgnoreCase(origOrgAddr))
                    throw ServiceException.INVALID_REQUEST(
                            "Changing organizer of an appointment is not allowed: old=" + origOrgAddr + ", new=" + newOrgAddr, null);
            } else {
                if (originalOrganizer != null)
                    throw ServiceException.INVALID_REQUEST(
                            "Removing organizer of an appointment is not allowed", null);
            }
        }

        // If modifying recurrence series (rather than an instance) and the
        // start time (HH:MM:SS) is changing, we need to update the time
        // component of RECURRENCE-ID in all exception instances.
        boolean needRecurrenceIdUpdate = false;
        ParsedDateTime oldDtStart = null;
        ParsedDuration dtStartMovedBy = null;
        ArrayList<Invite> toUpdate = new ArrayList<Invite>();
        if (!isCancel && newInvite.isRecurrence()) {
            Invite defInv = getDefaultInvite();
            if (defInv != null) {
                oldDtStart = defInv.getStartTime();
                ParsedDateTime newDtStart = newInvite.getStartTime();
                if (!newDtStart.sameTime(oldDtStart)) {
                    needRecurrenceIdUpdate = true;
                    dtStartMovedBy = newInvite.getStartTime().difference(oldDtStart);
                }
            }
        }

        boolean modifiedAppt = false;
        Invite prev = null; // (the first) invite which has been made obsolete by the new one coming in
        
        ArrayList<Invite> toRemove = new ArrayList<Invite>(); // Invites to remove from our blob store

        ArrayList<Integer> idxsToRemove = new ArrayList<Integer>(); // indexes to remove from mInvites
        
        for (int i = 0; i < numInvites(); i++) {
            Invite cur = getInvite(i);

            // UID already matches...next check if RecurId matches
            // if so, then seqNo is next
            // finally use DTStamp
            //
            // See RFC2446: 2.1.5 Message Sequencing
            //
            if ((cur.getRecurId() != null && cur.getRecurId().equals(newInvite.getRecurId())) ||
                    (cur.getRecurId() == null && newInvite.getRecurId() == null)) {
                if (force ||
                        ((cur.getSeqNo() < newInvite.getSeqNo())
                        // in the CANCEL case, accept one with the exact same DTSTAMP as the request
                        // being cancelled ( >= instead of strictly > for DTSTAMP)
                        || (
                                (!isCancel && (cur.getSeqNo() == newInvite.getSeqNo() && cur.getDTStamp() < newInvite.getDTStamp())) 
                                || 
                                (isCancel && (cur.getSeqNo() == newInvite.getSeqNo() && cur.getDTStamp() <= newInvite.getDTStamp()))))) 
                {
                    Invite inf = mInvites.get(i);
                    toRemove.add(inf);
                    
                    // add to FRONT of list, so when we iterate for the removals we go from HIGHER TO LOWER
                    // that way the numbers all match up as the list contracts!
                    idxsToRemove.add(0, new Integer(i));
                    
                    // clean up any old REPLYs that have been made obscelete by this new invite
                    mReplyList.removeObsceleteEntries(newInvite.getRecurId(), newInvite.getSeqNo(), newInvite.getDTStamp());
                    
                    prev = cur;
                    modifiedAppt = true;
                    if (isCancel && !newInvite.hasRecurId()) {
                        addNewOne = false; // can't CANCEL just the recurId=0 entry -- we must be deleting the whole series
                    }
                } else {
                    // found a more-recent invite already here -- so don't add
                    // the passed-in one!
                    addNewOne = false;
                }
//              break; // don't stop here!  new Invite *could* obscelete multiple existing ones! 
            } else if (needRecurrenceIdUpdate) {
                RecurId rid = cur.getRecurId();
                if (rid != null && rid.getDt().sameTime(oldDtStart))
                    toUpdate.add(cur);
            }
        }

        boolean callProcessPartStat = false;
        if (addNewOne) {
            newInvite.setAppointment(this);
            Account account = getMailbox().getAccount();
            boolean thisAcctIsOrganizer =
                newInvite.thisAcctIsOrganizer(account);
            if (prev!=null && !thisAcctIsOrganizer && newInvite.sentByMe()) {
                // A non-organizer attendee is modifying data on his/her
                // appointment.  Any information that is tracked in
                // metadata rather than in the iCal MIME part must be
                // carried over from the last invite to the new one.
                newInvite.setPartStat(prev.getPartStat());
                newInvite.setRsvp(prev.getRsvp());
                newInvite.getAppointment().saveMetadata();
                // No need to mark invite as modified item in mailbox as
                // it has already been marked as a created item.
            } else {
                callProcessPartStat = true;
            }

            mInvites.add(newInvite);
            
            // the appointment stores an uber-tzmap, for its uber-recurrence
            // this might give us problems if we had two invites with conflicting TZ
            // defs....should be very unlikely
            mTzMap.add(newInvite.getTimeZoneMap());

            // Adjust DTSTART of RECURRENCE-ID for exceptions if DTSTART of
            // series is changing.  Notice we're only doing this when
            // addNewOne is true.  We don't want to make any updates if the
            // new invite ends up getting ignored due to older SEQUENCE.
            if (needRecurrenceIdUpdate) {
                for (Invite inv: toUpdate) {
                    RecurId rid = inv.getRecurId();
                    ParsedDateTime dt = rid.getDt().add(dtStartMovedBy);
                    RecurId newRid = new RecurId(dt, rid.getRange());
                    inv.setRecurId(newRid);

                    // For CANCELLED instances, set DTSTART to the same time
                    // used in RECURRENCE-ID.
                    if (ICalTok.CANCEL.toString().equals(inv.getMethod()))
                        inv.setDtStart(dt);
                }
            }

            modifyBlob(toRemove, toUpdate, pm, newInvite, volumeId);
            modifiedAppt = true;
        } else {
            modifyBlob(toRemove, toUpdate, null, null, volumeId);
        }
        
        // now remove the inviteid's from our list  
        for (Iterator iter = idxsToRemove.iterator(); iter.hasNext();) {
            Integer i = (Integer)iter.next();
            mInvites.remove(i.intValue());
        }

        if (getFolderId() != folderId) {
        	// Move appointment to a different folder.
        	Folder folder = getMailbox().getFolderById(folderId);
        	move(folder);
        }

        boolean hasRequests = false;
        for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
            Invite cur = (Invite)iter.next();
            if (cur.getMethod().equals(ICalTok.REQUEST.toString()) ||
                    cur.getMethod().equals(ICalTok.PUBLISH.toString())) 
            {
                hasRequests = true;
                break;
            }
        }
        
        if (!hasRequests) {
            this.delete(); // delete this appointment from the table, it
                            // doesn't have anymore REQUESTs!
        } else {
            if (modifiedAppt) {
                if (!updateRecurrence()) {
                    // no default invite!  This appointment no longer valid
                    this.delete();
                } else {
                    if (callProcessPartStat) {
                        // processPartStat() must be called after
                        // updateRecurrence() has been called.  (bug 8072)
                        processPartStat(newInvite,
                                        pm != null ? pm.getMimeMessage() : null,
                                        false,
                                        newInvite.getPartStat());
                    }
                    this.saveMetadata();
                }
            }
        }
    }
    
    private static class PMDataSource implements DataSource {
        
        private ParsedMessage mPm;
        
        public PMDataSource(ParsedMessage pm) {
            mPm = pm;
        }

        public String getName() {
            // TODO should we just return null?
            return mPm.getMessageID();
        }

        public String getContentType() {
            return "message/rfc822";
        }

        /**
         * Returns the InputStream for this blob. Note that this method 
         * needs a database connection and will obtain/release one
         * automatically if needed, or use the one passed to it from
         * the constructor.
         * @throws IOException
         */
        public InputStream getInputStream() throws IOException {
            try {
                if (mPm == null) {
                    return new ByteArrayInputStream(new byte[0]);
                } else {
                    return new ByteArrayInputStream(mPm.getRawData());
                }
            } catch (MessagingException e) {
                IOException ioe = new IOException("getInputStream");
                ioe.initCause(e);
                throw ioe;
            }
        }

        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }
    }
    
    private void storeUpdatedBlob(MimeMessage mm, short volumeId)
    throws ServiceException, IOException {
        ParsedMessage pm = new ParsedMessage(mm, true);
        try {
            setContent(pm.getRawData(), pm.getRawDigest(), volumeId, pm);
        } catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
        }
    }
    
    void reanalyze(Object data) throws ServiceException {
        if (!(data instanceof ParsedMessage))
            throw ServiceException.FAILURE("cannot reanalyze non-ParsedMessage object", null);
        ParsedMessage pm = (ParsedMessage) data;
        saveData(pm.getParsedSender().getSortString());
    }

    /**
     * The Blob for the appointment is currently single Mime multipart/digest which has
     * each invite's MimeMessage stored as a part.  
     * 
     * @param invPm
     * @param firstInvite
     * @param volumeId
     * @throws ServiceException
     */
    private void createBlob(ParsedMessage invPm, Invite firstInvite, short volumeId)
    throws ServiceException {
        try { 
            // create the toplevel multipart/digest...
            MimeMessage mm = new MimeMessage(JMSession.getSession());            
            MimeMultipart mmp = new MimeMultipart("digest");
            mm.setContent(mmp);
            
            
            // add the invite
            MimeBodyPart mbp = new MimeBodyPart();
            mbp.setDataHandler(new DataHandler(new PMDataSource(invPm)));
            mmp.addBodyPart(mbp);
            mbp.addHeader("invId", Integer.toString(firstInvite.getMailItemId()));
            
            mm.saveChanges();
            
            storeUpdatedBlob(mm, volumeId);
            
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException "+e, e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException "+e, e);
        }
    }
    
    /**
     * Upate the Blob for this Appointment object: possibly remove one or more entries from it, 
     * possibly add an entry to it.
     * 
     * It IS okay to have newInv != null and invPm==null....this would mean an invite-add where the
     * new invite had no ParsedMessage: IE because it didn't actually come in via an RFC822 msg
     * 
     * @param toRemove
     * @param toUpdate
     * @param invPm
     * @param newInv
     * @param volumeId
     * @throws ServiceException
     */
    private void modifyBlob(List<Invite> toRemove,
                            List<Invite> toUpdate,
                            ParsedMessage invPm,
                            Invite newInv,
                            short volumeId)
    throws ServiceException
    {
        // TODO - as an optimization, should check to see if the invite's MM is already in here! (ie
        //         if a single incoming Message has multiple invites in it, all for this Appointment)
        try { 
            // now, make sure the message is in our blob already...
            MimeMessage mm;
            
            try {
                mm = new MimeMessage(getMimeMessage());
            } catch (ServiceException e) {
                if (newInv != null) {
                    // if the blob isn't already there, and we're going to add one, then
                    // just go into create
                    createBlob(invPm, newInv, volumeId);
                }
                return;
            }
            
            // it'll be multipart/digest
            MimeMultipart mmp = (MimeMultipart)mm.getContent();
            
            boolean updated = false;

            // remove invites
            for (Invite inv: toRemove) {
                int matchedIdx;
                do {
                    // find the matching parts...
                    int numParts = mmp.getCount();
                    matchedIdx = -1;
                    for (int i = 0; i < numParts; i++) {
                        MimeBodyPart mbp = (MimeBodyPart)mmp.getBodyPart(i);
                        String[] hdrs = mbp.getHeader("invId");
                        if (hdrs == null || hdrs.length == 0) {
                            matchedIdx = i;
                            break;
                        }
                        if (hdrs != null && hdrs.length > 0 && (Integer.parseInt(hdrs[0])==inv.getMailItemId())) {
                            matchedIdx = i;
                            break;
                        }
                    }
                    if (matchedIdx > -1) {
                        mmp.removeBodyPart(matchedIdx);
                        updated = true;
                    }
                } while(matchedIdx > -1);
            }

            // Update some invites.
            for (Invite inv: toUpdate) {
                MimeBodyPart mbpInv = null;
                int numParts = mmp.getCount();
                for (int i = 0; i < numParts; i++) {
                    MimeBodyPart mbp = (MimeBodyPart) mmp.getBodyPart(i);
                    String[] hdrs = mbp.getHeader("invId");
                    if (hdrs != null && hdrs.length > 0 &&
                        (Integer.parseInt(hdrs[0]) == inv.getMailItemId())) {
                        mbpInv = mbp;
                        break;
                    }
                }
                if (mbpInv == null)
                    continue;

                // Find the text/calendar part and replace it.
                String mbpInvCt = mbpInv.getContentType();
                Object objInv = mbpInv.getContent();
                if (!(objInv instanceof MimeMessage)) continue;
                MimeMessage mmInv = (MimeMessage) objInv;
                Object objMulti = mmInv.getContent();
                if (!(objMulti instanceof MimeMultipart)) continue;
                MimeMultipart multi = (MimeMultipart) objMulti;
                int numSubParts = multi.getCount();
                int icalPartNum = -1;
                for (int j = 0; j < numSubParts; j++) {
                    MimeBodyPart part = (MimeBodyPart) multi.getBodyPart(j);
                    ContentType ct = new ContentType(part.getContentType());
                    if (ct.match(Mime.CT_TEXT_CALENDAR)) {
                        icalPartNum = j;
                        break;
                    }
                }
                if (icalPartNum != -1) {
                    updated = true;
                    multi.removeBodyPart(icalPartNum);
                    ZVCalendar cal = inv.newToICalendar();
                    String test = cal.toString();
                    MimeBodyPart icalPart = CalendarMailSender.makeICalIntoMimePart(inv.getUid(), cal);
                    multi.addBodyPart(icalPart, icalPartNum);
                    // Courtesy of JavaMail.  All three lines are necessary.
                    // Reasons unclear from JavaMail docs.
                    mmInv.setContent(multi);
                    mmInv.saveChanges();
                    mbpInv.setContent(mmInv, mbpInvCt);
                    // End of courtesy of JavaMail
                }
            }

            if (newInv != null) {
                MimeBodyPart mbp = new MimeBodyPart();
                mbp.setDataHandler(new DataHandler(new PMDataSource(invPm)));
                mmp.addBodyPart(mbp);
                mbp.addHeader("invId", Integer.toString(newInv.getMailItemId()));
                updated = true;
            }

            if (!updated)
                return;
            
            if (mmp.getCount() == 0) {
                StoreManager sm = StoreManager.getInstance();
                sm.delete(getBlob());
            } else {
                // must call this explicitly or else new part won't be added...
                mm.setContent(mmp);
                
                mm.saveChanges();
                
                storeUpdatedBlob(mm, volumeId);
            }
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException", e);
        }
        
    }

    public static class ReplyInfo {
        public RecurId mRecurId;
        public int mSeqNo;
        public long mDtStamp;
        public ZAttendee mAttendee; // attendee record w/ PartStat
        
        private static final String FN_RECURID = "r";
        private static final String FN_SEQNO = "s";
        private static final String FN_DTSTAMP = "d";
        private static final String FN_ATTENDEE = "at";
        
        Metadata encodeAsMetadata() {
            Metadata meta = new Metadata();
            
            if (mRecurId != null) {
                meta.put(FN_RECURID, mRecurId.encodeMetadata());
            }
            
            meta.put(FN_SEQNO, mSeqNo);
            meta.put(FN_DTSTAMP, mDtStamp);
            meta.put(FN_ATTENDEE, mAttendee.encodeAsMetadata());

            return meta;
        }
        
        static ReplyInfo decodeFromMetadata(Metadata md, TimeZoneMap tzMap) throws ServiceException {
            ReplyInfo toRet = new ReplyInfo();
            
            if (md.containsKey(FN_RECURID)) {
                toRet.mRecurId = RecurId.decodeMetadata(md.getMap(FN_RECURID), tzMap);
            } else {
                toRet.mRecurId = null;
            }
            toRet.mSeqNo = (int)md.getLong(FN_SEQNO);
            toRet.mDtStamp = md.getLong(FN_DTSTAMP);
            toRet.mAttendee = ZAttendee.parseAtFromMetadata(md.getMap(FN_ATTENDEE));
            
            return toRet;
        }
    }

    /**
     * @author tim
     * 
     * Internal class for storing replies 
     *
     */
    private static class ReplyList {
        List /* ReplyInfo */ mReplies;
        
        public ReplyList() {
            mReplies = new ArrayList();
        }
        
        private static final String FN_NUM_REPLY_INFO = "n";
        private static final String FN_REPLY_INFO = "i";
        
        Metadata encodeAsMetadata() {
            Metadata meta = new Metadata();
            
            meta.put(FN_NUM_REPLY_INFO, mReplies.size());
            for (int i = 0; i < mReplies.size(); i++) {
                String fn = FN_REPLY_INFO + i;
                
                meta.put(fn, ((ReplyInfo)(mReplies.get(i))).encodeAsMetadata());
            }
            return meta;
        }
        
        static ReplyList decodeFromMetadata(Metadata md, TimeZoneMap tzMap) throws ServiceException {
            ReplyList toRet = new ReplyList();
            int numReplies = (int) md.getLong(FN_NUM_REPLY_INFO);
            toRet.mReplies = new ArrayList(numReplies);
            for (int i = 0; i < numReplies; i++) {
                ReplyInfo inf = ReplyInfo.decodeFromMetadata(md.getMap(FN_REPLY_INFO+i), tzMap);
                toRet.mReplies.add(i, inf);
            }
            return toRet;
        }
        
        void removeObsceleteEntries(RecurId recurId, int seqNo, long dtStamp) {
            for (Iterator iter = mReplies.iterator(); iter.hasNext();) {
                ReplyInfo cur = (ReplyInfo)iter.next();
                
                if (recurMatches(cur.mRecurId, recurId)) {
                    if (cur.mSeqNo <= seqNo && cur.mDtStamp <= dtStamp) {
                        iter.remove();
                    }
                }
            }
        }
        
        boolean maybeStoreNewReply(Invite inv, ZAttendee at) {
            for (Iterator iter = mReplies.iterator(); iter.hasNext();) {
                ReplyInfo cur = (ReplyInfo)iter.next();
                
                if (at.addressesMatch(cur.mAttendee)) {
                    if (recurMatches(inv.getRecurId(), cur.mRecurId)) {
                        if (inv.getSeqNo() >= cur.mSeqNo) {
                            if (inv.getDTStamp() >= cur.mDtStamp) {
                                iter.remove();
                                
                                ReplyInfo toAdd = new ReplyInfo();
                                toAdd.mRecurId = inv.getRecurId();
                                toAdd.mSeqNo = inv.getSeqNo();
                                toAdd.mDtStamp = inv.getDTStamp();
                                toAdd.mAttendee = at;
                                
                                mReplies.add(toAdd);

                                return true;
                            }
                        }
                        return false;
                    }
                } // attendee check
            }
            
            // if we get here, we didn't find one at all.  add a new one...
            ReplyInfo toAdd = new ReplyInfo();
            toAdd.mRecurId = inv.getRecurId();
            toAdd.mSeqNo = inv.getSeqNo();
            toAdd.mDtStamp = inv.getDTStamp();
            toAdd.mAttendee = at;
            
            mReplies.add(toAdd);
            
            return true;
        }
        
        void modifyPartStat(Account acctOrNull, RecurId recurId, String cnStr, String addressStr, String cutypeStr, String roleStr, 
                String partStatStr, Boolean needsReply, int seqNo, long dtStamp)  throws ServiceException {
            for (Iterator iter = mReplies.iterator(); iter.hasNext();) {
                ReplyInfo cur = (ReplyInfo)iter.next();
                
                if ( (cur.mRecurId == null && recurId == null) ||
                        (cur.mRecurId != null && cur.mRecurId.withinRange(recurId))) {                    
                    if (
                            (acctOrNull != null && (AccountUtil.addressMatchesAccount(acctOrNull, cur.mAttendee.getAddress()))) ||
                            (acctOrNull == null && cur.mAttendee.addressMatches(addressStr))
                            ) 
                    {
                        if (cur.mAttendee.hasCn()) {
                            cnStr = cur.mAttendee.getCn();
                        }

                        if (cur.mAttendee.hasCUType()) {
                        	cutypeStr = cur.mAttendee.getCUType();
                        }

                        if (cur.mAttendee.hasRole()) {
                            roleStr = cur.mAttendee.getRole();
                        }
                        
                        ZAttendee newAt = new ZAttendee(
                                cur.mAttendee.getAddress(),
                                cnStr,
                                cutypeStr,
                                roleStr,
                                partStatStr,
                                needsReply
                                );
                        
                        cur.mAttendee = newAt;
                        cur.mSeqNo = seqNo;
                        cur.mDtStamp = dtStamp;
                        return;
                    }
                }
            }
            
            // no existing partstat for this instance...add a new one 
            ReplyInfo inf = new ReplyInfo();
            inf.mAttendee = new ZAttendee(addressStr, cnStr, cutypeStr, roleStr, partStatStr, needsReply);
            inf.mRecurId = recurId;
            inf.mDtStamp = dtStamp;
            inf.mSeqNo = seqNo;
            mReplies.add(inf);
        }
        
        boolean recurMatches(RecurId lhs, RecurId rhs) {
            if (lhs == null) {
                return (rhs==null);
            }
            return lhs.equals(rhs);
        }
        
        /**
         * Find the "Effective" attendee for a particular Instance
         * 
         * Three possible returns
         *    1) an exact match (reply is to default and asking for default OR reply is to specific instance and asking for it)
         *    2) the default, if one is set
         *    3) null
         * 
         * @param acct
         * @param inv 
         * @param inst Instance to look up, or "null" for the default instance
         * @return
         * @throws ServiceException
         */
        ZAttendee getEffectiveAttendee(Account acct, Invite inv, Instance inst) throws ServiceException {
            ZAttendee defaultAt = null;
            
            for (Iterator iter = mReplies.iterator(); iter.hasNext();) {
                ReplyInfo cur = (ReplyInfo)iter.next();
                
                if (AccountUtil.addressMatchesAccount(acct, cur.mAttendee.getAddress())) {
                    if (
                            (cur.mRecurId == null && inst == null) || // asking for default instance
                            (inst!=null && cur.mRecurId != null && cur.mRecurId.withinRange(inst.getStart())) // matches specific requested instance
                    ) {
                        //
                        // we found exactly what they're looking for!  Either the default (and they asked for it) or the
                        // specific one they asked for
                        //
                        if (inv.getSeqNo() <= cur.mSeqNo) {
                            if (inv.getDTStamp() <= cur.mDtStamp) {
                                return cur.mAttendee;
                            }
                        }
                    }
                    
                    if (cur.mRecurId == null) {
                        // save the default reply if it is valid: we will return it if we don't find an exact match
                        if ((inv.getSeqNo() <= cur.mSeqNo) && (inv.getDTStamp() <= cur.mDtStamp))
                            defaultAt = cur.mAttendee;
                    }
                }
            }
            
            if (defaultAt == null)
                return inv.getMatchingAttendee(acct);
            else
                return defaultAt;
        }
        
        List /* ReplyInfo */ getReplyInfo(Invite inv) {
            ArrayList toRet = new ArrayList();
            
            for (Iterator iter = mReplies.iterator(); iter.hasNext();) {
                ReplyInfo cur = (ReplyInfo)iter.next();
                
                if (inv == null || 
                        ((inv.getSeqNo() <= cur.mSeqNo) 
                                && (inv.getDTStamp() <= cur.mDtStamp) 
                                && (
                                        (inv.getRecurId() == null && cur.mRecurId == null) 
                                        || ((inv.getRecurId() != null) && (inv.getRecurId().equals(cur.mRecurId)))))) {
                    toRet.add(cur);
                } else {
                    sLog.info("ReplyList "+this.toString()+" has outdated entries in its Replies list");
                }
            }
            return toRet;
        }
        

    } // class ReplyList
            
    /**
     * Get all of the Reply data corresponding to this invite
     * 
     * @param inv
     * @return
     * @throws ServiceException
     */
    public List /*ReplyInfo*/ getReplyInfo(Invite inv) {
        return mReplyList.getReplyInfo(inv);
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
        ZAttendee at = mReplyList.getEffectiveAttendee(acct, inv, inst);
        if (at == null || inv.isOrganizer(at)) {
            return inv.getFreeBusyActual();
        }

        if (at.hasPartStat()) {
            return inv.partStatToFreeBusyActual(at.getPartStat());
        } else {
            return inv.getFreeBusyActual();
        }
    }
    
    /**
     * Returns the effective PartStat given the Invite and Instance.  See the description of
     * getEffectiveFreeBusyActual above for more info
     * 
     * @param inv
     * @param inst
     * @return
     * @throws ServiceException
     */
    public String getEffectivePartStat(Invite inv, Instance inst) throws ServiceException {
        Account acct = getMailbox().getAccount();
        ZAttendee at = mReplyList.getEffectiveAttendee(acct, inv, inst);
        if (at == null || inv.isOrganizer(at)) {
            return inv.getPartStat();
        }

        if (at.hasPartStat()) {
            return at.getPartStat();
        } else {
            return IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;
        }
    }
    
    /**
     * 
     * Used when we're sending out a reply -- we add a "reply" record to this appointment...this
     * ends up affecting our "Effective PartStat" (ie if we ACCEPT a meeting, then our effective partstat
     * changes)
     * 
     * @param acctOrNull
     * @param recurId
     * @param cnStr
     * @param addressStr
     * @param cutypeStr
     * @param roleStr
     * @param partStatStr
     * @param rsvp
     * @param seqNo
     * @param dtStamp
     * @throws ServiceException
     */
    void modifyPartStat(Account acctOrNull, RecurId recurId, String cnStr, String addressStr, String cutypeStr, String roleStr,
            String partStatStr, Boolean needsReply, int seqNo, long dtStamp) throws ServiceException {
        mReplyList.modifyPartStat(acctOrNull, recurId, cnStr, addressStr, cutypeStr, roleStr, partStatStr, needsReply, seqNo, dtStamp);
    }
    
    private void processNewInviteReply(ParsedMessage pm, Invite reply, boolean force)
    throws ServiceException {
        if (!canAccess(ACL.RIGHT_ACTION))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions to change this appointment's state");

        boolean dirty = false;
        // unique ID: UID+RECURRENCE_ID

        for (int i = 0; i < numInvites(); i++) {
            Invite cur = getInvite(i);

            // UID already matches...next check if RecurId matches
            // if so, then seqNo is next
            // finally use DTStamp
            //
            // See RFC2446: 2.1.5 Message Sequencing
            //

            
            // If any of the existing Invites have recurIDs which exactly match the reply,
            // then we should do a sequence-check against them before deciding to accept
            // this reply
            // FIXME should check for cur.recurID WITHIN_RANGE (THISANDFUTURE-type support)
            if ((cur.getRecurId() != null && cur.getRecurId().equals(reply.getRecurId())) ||
                    (cur.getRecurId() == null && reply.getRecurId() == null)) {
                
                // they're replying to this invite!
                
                if (cur.getSeqNo() >= reply.getSeqNo()
                        && cur.getDTStamp() > reply.getDTStamp()) 
                {
                    
                    sLog.info("Invite-Reply "+reply.toString()+" is outdated, ignoring!");
                    // this reply is OLD, ignore it
                    return;
                }
                
                // update the ATTENDEE record in the invite
                cur.updateMatchingAttendees(reply);
                dirty = true;
                
                break; // found a match, fall through to below and process it!
            }
        }
            
        // if we got here then we have validated the sequencing against all of our Invites, 
        // OR alternatively we looked and couldn't find one with a matching RecurID (therefore 
        // they must be replying to a arbitrary instance)
        List attendees = reply.getAttendees();
        for (Iterator iter = attendees.iterator(); iter.hasNext();) {
            ZAttendee at = (ZAttendee)iter.next();
            if (mReplyList.maybeStoreNewReply(reply, at)) {
                dirty = true;
            }
        }
        
        if (dirty) {
            saveMetadata();
            getMailbox().markItemModified(this, Change.MODIFIED_INVITE);
        }
    }
    
    public InputStream getRawMessage() throws ServiceException {
        return MessageCache.getRawContent(this);
    }
    
//    void appendRawCalendarData(Calendar cal) throws ServiceException {
//        for (Iterator invIter = mInvites.iterator(); invIter.hasNext();) {
//            Invite inv = (Invite)invIter.next();
//            
////            Calendar invCal = getCalendar(inv.getMailItemId());
////            Calendar invCal = inv.toICalendar();
//            
////            for (Iterator compIter = invCal.getComponents().iterator(); compIter.hasNext();) {
////                Component comp = (Component)compIter.next();
////                cal.getComponents().add(comp);
////            }
//            Component comp = inv.toVEvent();
//            cal.getComponents().add(comp);
//        }
//    }
    

    void appendRawCalendarData(ZVCalendar cal) throws ServiceException {
        for (Iterator invIter = mInvites.iterator(); invIter.hasNext();) {
            Invite inv = (Invite)invIter.next();
            
//            Calendar invCal = getCalendar(inv.getMailItemId());
//            Calendar invCal = inv.toICalendar();
            
//            for (Iterator compIter = invCal.getComponents().iterator(); compIter.hasNext();) {
//                Component comp = (Component)compIter.next();
//                cal.getComponents().add(comp);
//            }
//            Component comp = inv.toVEvent();
//            cal.getComponents().add(comp);
            cal.addComponent(inv.newToVEvent());
        }
    }
    
    
//    Calendar getCalendar(int invId) throws ServiceException {
//        try {
//            MimeMessage mm = getMimeMessage(invId);
//            
//            List parts = Mime.getParts(mm);
//            for (int i=0; i<parts.size(); i++) {
//                MPartInfo partInfo = (MPartInfo) parts.get(i);
//                if (partInfo.getContentType().match(Mime.CT_TEXT_CALENDAR)) {
//                    java.io.InputStream is = partInfo.getMimePart().getInputStream();
//                    CalendarBuilder builder = new CalendarBuilder();
//                    Calendar iCal = builder.build(is);
//                    return iCal;
//                }
//            }
//            throw ServiceException.FAILURE("Could not parse detailed iCalendar data for mailbox "+
//                    getMailboxId()+" appt "+getId(), null);
//        } catch (IOException e) {
//            throw ServiceException.FAILURE("reading mime message", e);
//        } catch (ParserException e) {
//            throw ServiceException.FAILURE("parsing iCalendar", e);
//        } catch (MessagingException e) {
//            throw ServiceException.FAILURE("reading mime message", e);
//        }
//    }
    

    public MimeMessage getMimeMessage() throws ServiceException {
        InputStream is = null;
        MimeMessage mm = null;
        try {
            is = getRawMessage();
            mm = new MimeMessage(JMSession.getSession(), is);
            is.close();

            try {
                for (Class visitor : MimeVisitor.getConverters())
                    ((MimeVisitor) visitor.newInstance()).accept(mm);
            } catch (Exception e) {
                // If the conversion bombs for any reason, revert to the original
                ZimbraLog.mailbox.warn(
                    "MIME converter failed for message " + getId(), e);
                is = getRawMessage();
                mm = new MimeMessage(JMSession.getSession(), is);
                is.close();
            }
            
            return mm;
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while getting MimeMessage for item " + mId, e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException while getting MimeMessage for item " + mId, e);
        }
    }
    
    /**
     * @param subId
     * @return
     * @throws ServiceException
     */
    public MimeMessage getSubpartMessage(int subId) throws ServiceException {
        try {
            MimeBodyPart mbp = findBodyBySubId(subId);
            return mbp == null ? null : (MimeMessage) Mime.getMessageContent(mbp);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while getting MimeMessage for item " + mId, e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException while getting MimeMessage for item " + mId, e);
        }
    }

    public Pair<MimeMessage,Integer> getSubpartMessageData(int subId) throws ServiceException {
        try {
            MimeBodyPart mbp = findBodyBySubId(subId);
            if (mbp == null)
                return null;
            return new Pair<MimeMessage,Integer>((MimeMessage) Mime.getMessageContent(mbp), mbp.getSize());
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while getting MimeMessage for item " + mId, e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException while getting MimeMessage for item " + mId, e);
        }
    }

    private MimeBodyPart findBodyBySubId(int subId) throws ServiceException {
        InputStream is = null;
        MimeMessage mm = null;
        try {
            is = getRawMessage();
            mm = new MimeMessage(JMSession.getSession(), is);
            is.close();

            try {
                for (Class visitor : MimeVisitor.getConverters())
                    ((MimeVisitor) visitor.newInstance()).accept(mm);
            } catch (Exception e) {
                // If the conversion bombs for any reason, revert to the original
                ZimbraLog.mailbox.warn("MIME converter failed for message " + getId(), e);
                is = getRawMessage();
                mm = new MimeMessage(JMSession.getSession(), is);
                is.close();
            }
            
            // it'll be multipart/digest
            MimeMultipart mmp = (MimeMultipart)mm.getContent();
            
            // find the matching parts...
            int numParts = mmp.getCount();
            
            for (int i = 0; i < numParts; i++) {
                MimeBodyPart mbp = (MimeBodyPart)mmp.getBodyPart(i);
                String[] hdrs = mbp.getHeader("invId");
                
                if (hdrs != null && hdrs.length > 0 && (Integer.parseInt(hdrs[0]) == subId))
                    return mbp;
            }
            return null;
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while getting MimeMessage for item " + mId, e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException while getting MimeMessage for item " + mId, e);
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

        public static String getBusyTimesString(List<Availability> list, TimeZone tz, Locale lc) {
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
                LinkedHashSet<Instance> instances = fb.getAllInstances();
                for (Instance instance : instances) {
                    if (conflictCount >= MAX_CONFLICT_LIST_SIZE) {
                        hasMoreConflicts = true;
                        break;
                    }

                    Date startDate = new Date(instance.getStart());
                    Date endDate = new Date(instance.getEnd());
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

                    Invite defInv = instance.getAppointment().getDefaultInvite();
                    if (defInv.hasOrganizer()) {
                        ZOrganizer organizer = defInv.getOrganizer();
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

        Collection instances = expandInstances(st, et);
        List<Availability> list = new ArrayList<Availability>(instances.size());
        int numConflicts = 0;
        for (Iterator iter = instances.iterator(); iter.hasNext(); ) {
            if (numConflicts > Availability.MAX_CONFLICT_LIST_SIZE)
                break;
            Instance inst = (Instance) iter.next();
            long start = inst.getStart();
            long end = inst.getEnd();
            FreeBusy fb =
                FreeBusy.getFreeBusyList(getMailbox(), start, end, this);
            String status = fb.getBusiest();
            if (!IcalXmlStrMap.FBTYPE_FREE.equals(status)) {
                list.add(new Availability(start, end, status, fb));
                numConflicts++;
            }
        }
        return list;
    }

    private String processPartStat(Invite invite,
                                   MimeMessage mmInv,
                                   boolean forCreate,
                                   String defaultPartStat)
    throws ServiceException {
        Mailbox mbox = getMailbox();
        OperationContext octxt = mbox.getOperationContext();
        CreateAppointmentPlayer player =
            octxt != null ? (CreateAppointmentPlayer) octxt.getPlayer() : null;

        Account account = getMailbox().getAccount();
        Locale lc;
        Account organizer = invite.getOrganizerAccount();
        if (organizer != null)
            lc = organizer.getLocale();
        else
            lc = account.getLocale();

        String partStat = defaultPartStat;
        if (player != null) {
            String p = player.getAppointmentPartStat();
            if (p != null) partStat = p;
        }

        RedoLogProvider redoProvider = RedoLogProvider.getInstance();
        boolean needResourceAutoReply =
            redoProvider.isMaster() &&
            (player == null || redoProvider.getRedoLogManager().getInCrashRecovery()) &&
            !ICalTok.CANCEL.toString().equals(invite.getMethod());

        if (invite.thisAcctIsOrganizer(account)) {
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
                            String msg =
                                L10nUtil.getMessage(MsgKey.calendarResourceDeclineReasonConflict, lc) +
                                "\r\n\r\n" +
                                Availability.getBusyTimesString(avail, invite.getStartTime().getTimeZone(), lc);
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

        CreateAppointmentRecorder recorder =
            (CreateAppointmentRecorder) mbox.getRedoRecorder();
        recorder.setAppointmentPartStat(partStat);

        invite.updateMyPartStat(account, partStat);
        if (forCreate) {
            Invite defaultInvite = getDefaultInvite();
            assert(defaultInvite != null);
            if (!defaultInvite.equals(invite) &&
                !partStat.equals(defaultInvite.getPartStat())) {
                defaultInvite.updateMyPartStat(account, partStat);
                saveMetadata();
            }
        }
        return partStat;
    }
}
