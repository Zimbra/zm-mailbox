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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.lucene.document.Field;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.calendar.CalendarMailSender;
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
import com.zimbra.cs.redolog.op.IndexItem;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.cs.util.JMSession;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;


/**
 * An APPOINTMENT consists of one or more INVITES in the same series -- ie that
 * have the same UID. From the appointment you can get the INSTANCES which are
 * the start/end times of each occurence.
 * 
 * Sample Appointment: APPOINTMENT UID=1234 (two INVITES above) ...Instances on
 * every monday with name "Gorilla Discussion" EXCEPT for the 21st, where we
 * talk about lefties instead. CANCELED for the 28th
 */
public abstract class CalendarItem extends MailItem {

    static Log sLog = LogFactory.getLog(CalendarItem.class);

    private String mUid;

    /** the time IN MSEC UTC that this appointment/task "starts" */
    private long mStartTime;
    /** the time IN MSEC UTC that this appointment/task "ends" */
    private long mEndTime;

    private Recurrence.IRecurrence mRecurrence;
    private TimeZoneMap mTzMap;

    private List<Invite> mInvites;
    
    private ReplyList mReplyList;
    protected ReplyList getReplyList() { return mReplyList; }

    public TimeZoneMap getTimeZoneMap() { return mTzMap; }

    protected CalendarItem(Mailbox mbox, UnderlyingData data) throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_APPOINTMENT && mData.type != TYPE_TASK)
            throw new IllegalArgumentException();
    }

    public Recurrence.IRecurrence getRecurrence() {
        return mRecurrence;
    }

    public boolean isRecurring() {
        return (mRecurrence != null);
    }

    @Override
    public String getSender() {
        String sender = null;
        Invite firstInvite = this.getDefaultInviteOrNull();
        if (firstInvite != null) {
            ZOrganizer org = firstInvite.getOrganizer();
            if (org != null) 
                sender = org.getIndexString();
        }
        if (sender == null)
            sender = "";
        return sender;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }
    
    public void saveMetadata() throws ServiceException {
//        super.saveMetadata();
        reanalyze(null);
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
    
    @Override
    public void reindex(IndexItem redo, boolean deleteFirst, Object indexData) throws ServiceException {
        if (DebugConfig.disableIndexing)
            return;
        
        List<org.apache.lucene.document.Document> docs = getLuceneDocuments();
        
        mMailbox.getMailboxIndex().indexCalendarItem(mMailbox, redo, deleteFirst, this, 
            docs, this.getDate());
    }
    
    protected List<org.apache.lucene.document.Document> getLuceneDocuments() throws ServiceException {
        List<org.apache.lucene.document.Document> toRet = 
            new ArrayList<org.apache.lucene.document.Document>();
        
        Invite defaultInvite = this.getDefaultInviteOrNull();
        
        String defaultLocation = "";
        if (defaultInvite != null && defaultInvite.getLocation() != null)
            defaultLocation = defaultInvite.getLocation();
        
        for (Invite inv : this.getInvites()) {
            MimeMessage mm = inv.getMimeMessage();
            if (mm == null)
                continue;
            
            try {
                StringBuilder s = new StringBuilder();
                for (ZAttendee at : inv.getAttendees()) {
                    mm.addRecipient(RecipientType.TO, at.getFriendlyAddress());
                    s.append(at.getIndexString()).append(' ');
                }
                
                if (inv.getLocation() != null) {
                    s.append(inv.getLocation()).append(' ');
                }  else {
                    s.append(defaultLocation).append(' ');
                }
                
                mm.saveChanges();
                
                ParsedMessage pm = new ParsedMessage(mm, mMailbox.attachmentsIndexingEnabled());
                List<org.apache.lucene.document.Document> docs = pm.getLuceneDocuments();
                for (org.apache.lucene.document.Document doc : docs) {
                    doc.add(new Field(LuceneFields.L_CONTENT, s.toString(), Field.Store.NO, Field.Index.TOKENIZED));
                    toRet.add(doc);
                }
            } catch(MessagingException e) {
                throw ServiceException.FAILURE("Failure Indexing: "+this.toString(), e);
            }
        }
        
        return toRet;
    }
    
    static CalendarItem create(int id, Folder folder, short volumeId, int flags, long tags, String uid, ParsedMessage pm, Invite firstInvite)
    throws ServiceException {
        if (!folder.canAccess(ACL.RIGHT_INSERT))
            throw ServiceException.PERM_DENIED("you do not have the required rights on the folder");
        Mailbox mbox = folder.getMailbox();
        
        if (pm.hasAttachments()) {
            firstInvite.setHasAttachment(true);
            flags |= Flag.BITMASK_ATTACHED;
        } else {
            firstInvite.setHasAttachment(false);
            flags &= ~Flag.BITMASK_ATTACHED;
        }

        byte type = firstInvite.isEvent() ? TYPE_APPOINTMENT : TYPE_TASK;
        
        String sender = null;
        ZOrganizer org = firstInvite.getOrganizer();
        if (org != null) 
            sender = org.getIndexString();
        if (sender == null)
            sender = "";
        
        String subject = firstInvite.getName();
        if (subject == null)
            subject= "";

        List<Invite> invites = new ArrayList<Invite>();
        invites.add(firstInvite);

        Recurrence.IRecurrence recur = firstInvite.getRecurrence();
        long startTime, endTime;
        if (recur != null) {
            ParsedDateTime dtStart = recur.getStartTime();
            startTime = dtStart != null ? dtStart.getUtcTime() : 0;
            ParsedDateTime dtEnd = recur.getEndTime();
            endTime = dtEnd != null ? dtEnd.getUtcTime() : 0;
        } else {
            ParsedDateTime dtStart = firstInvite.getStartTime();
            startTime = dtStart != null ? dtStart.getUtcTime() : 0;
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
        data.type     = type;
        data.folderId = folder.getId();
        data.indexId  = id;
        data.imapId   = id;
        data.volumeId = volumeId;
        data.date     = mbox.getOperationTimestamp();
        data.flags    = flags & Flag.FLAGS_GENERIC;
        data.tags     = tags;
        data.sender   = sender;
        data.subject = subject;
        data.metadata = encodeMetadata(DEFAULT_COLOR, uid, startTime, endTime, recur, invites, firstInvite.getTimeZoneMap(), new ReplyList());
        data.contentChanged(mbox);
        DbMailItem.create(mbox, data);

        CalendarItem item =
            type == TYPE_APPOINTMENT ? new Appointment(mbox, data) : new Task(mbox, data);
        item.processPartStat(firstInvite,
                             pm != null ? pm.getMimeMessage() : null,
                             true,
                             IcalXmlStrMap.PARTSTAT_NEEDS_ACTION);
        item.createBlob(pm, firstInvite, volumeId);
        item.finishCreation(null);

        DbMailItem.addToCalendarItemTable(item);
        return item;
    }

    private boolean updateRecurrence() throws ServiceException {
        long startTime, endTime;

        // update our recurrence rule, start with the initial rule
        Invite firstInv = getDefaultInviteOrNull();
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
                        
                        if (cur.hasRecurId() && cur.getStartTime() != null) {
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
            
            // update the start and end time in the CalendarItem table if
            // necessary
            ParsedDateTime dtStartTime = mRecurrence.getStartTime();
            ParsedDateTime dtEndTime = mRecurrence.getEndTime();
            
            startTime = dtStartTime != null ? dtStartTime.getUtcTime() : 0;
            endTime = dtEndTime != null ? dtEndTime.getUtcTime() : 0;
        } else {
            mRecurrence = null;
            ParsedDateTime dtStart = firstInv.getStartTime();
            ParsedDateTime dtEnd = firstInv.getEffectiveEndTime();
            
            startTime = dtStart != null ? dtStart.getUtcTime() : 0;
            endTime = dtEnd != null ? dtEnd.getUtcTime() : 0;
        }
        
        if (mStartTime != startTime || mEndTime != endTime) {
            mStartTime = startTime;
            mEndTime = endTime;
            DbMailItem.updateInCalendarItemTable(this);
        }
        return true;
    }

    public static final String FN_CALITEM_RECURRENCE = "apptRecur";

    void decodeMetadata(Metadata meta) throws ServiceException {
        super.decodeMetadata(meta);

        int mdVersion = meta.getVersion();

        mUid = meta.get(Metadata.FN_UID, null);
        mInvites = new ArrayList<Invite>();
        
        ICalTimeZone accountTZ = ICalTimeZone.getAccountTimeZone(getMailbox().getAccount()); 
        if (mdVersion < 6) {
            mStartTime = 0;
            mEndTime = 0;
        } else {
            mTzMap = TimeZoneMap.decodeFromMetadata(meta.getMap(Metadata.FN_TZMAP), accountTZ);

            // appointment/task start and end
            mStartTime = meta.getLong(Metadata.FN_CALITEM_START, 0);
            mEndTime = meta.getLong(Metadata.FN_CALITEM_END, 0);

            // invite ID's
            long numComp = meta.getLong(Metadata.FN_NUM_COMPONENTS);
            for (int i = 0; i < numComp; i++) {
                Metadata md = meta.getMap(Metadata.FN_INV + i);
                mInvites.add(Invite.decodeMetadata(getMailboxId(), md, this, accountTZ));
            }

            Metadata metaRecur = meta.getMap(FN_CALITEM_RECURRENCE, true);
            if (metaRecur != null) {
                mRecurrence = Recurrence.decodeMetadata(metaRecur, mTzMap);
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
        meta.put(Metadata.FN_CALITEM_START, startTime);
        meta.put(Metadata.FN_CALITEM_END, endTime);
        meta.put(Metadata.FN_NUM_COMPONENTS, invs.size());
        
        meta.put(Metadata.FN_REPLY_LIST, replyList.encodeAsMetadata());
        
        int num = 0;
        for (Iterator iter = invs.iterator(); iter.hasNext();) {
            Invite comp = (Invite) iter.next();
            String compName = Metadata.FN_INV + num++;
            meta.put(compName, Invite.encodeMetadata(comp));
        }

        if (recur != null)
            meta.put(FN_CALITEM_RECURRENCE, recur.encodeMetadata());

        return MailItem.encodeMetadata(meta, color);
    }

    // /**
    // * Expand all the instances for the time period from start to end
    // *
    // * @param start
    // * @param end
    // * @return list of Instances for the specified time period
    // */
    public Collection<Instance> expandInstances(long start, long end) {
        List<Instance> instances = new ArrayList<Instance>();

        if (mRecurrence != null) {
            instances = mRecurrence.expandInstances(this, start, end);
        } else {
            if (mInvites != null) {
                for (Iterator iter = mInvites.iterator(); iter.hasNext(); ) {
                    Invite inv = (Invite) iter.next();
                    ParsedDateTime dtStart = inv.getStartTime();
                    long invStart = dtStart != null ? dtStart.getUtcTime() : 0;
                    ParsedDateTime dtEnd = inv.getEffectiveEndTime();
                    long invEnd = dtEnd != null ? dtEnd.getUtcTime() : 0;
                    if ((invStart < end && invEnd > start) || (dtStart == null)) {
                        Instance inst = new Instance(this, new InviteInfo(inv),
                                                     dtStart == null,
                                                     invStart, invEnd,
                                                     inv.hasRecurId());
                        instances.add(inst);
                    }
                }
            }
        }
        return instances;
    }

    public static class Instance implements Comparable<Instance> {
        private boolean mTimeless;  // if true, this instance has no start/end time (e.g. VTODO with no DTSTART)
        private long mStart;        // calculated start time of this instance
        private long mEnd;          // calculated end time of this instance

        private boolean mIsException; // TRUE if this instance is an exception
                                      // to a recurrence

        private InviteInfo mInvId;
        
        private CalendarItem mCalItem;

        /**
         * Create an Instance object using data in an Invite that points to
         * a specific instance of an CalendarItem.
         * @param inv
         * @return
         */
        public static Instance fromInvite(CalendarItem calItem, Invite inv) {
            ParsedDateTime dtStart = inv.getStartTime();
            long start = dtStart != null ? dtStart.getUtcTime() : 0;
            ParsedDateTime dtEnd = inv.getEffectiveEndTime();
            long end = dtEnd != null ? dtEnd.getUtcTime() : 0;
            return new Instance(calItem, new InviteInfo(inv), dtStart == null, start, end, inv.hasRecurId());
        }

        public Instance(CalendarItem calItem, InviteInfo invInfo,
                boolean timeless,
                long _start, long _end,
                boolean _exception) 
        {
            mInvId = invInfo;
            mCalItem = calItem;
            mTimeless = timeless;
            if (mTimeless) {
                mStart = mEnd = 0;
            } else {
                mStart = _start;
                mEnd = _end;
            }
            mIsException = _exception;
        }

        public int compareTo(Instance other) {
            long toRet = mCalItem.getId() - other.mCalItem.getId();
            if (toRet == 0) {
                if (mTimeless == other.mTimeless) {
                    toRet = mStart - other.mStart;
                    if (toRet == 0) {
                        toRet = mEnd - other.mEnd;
                        if (toRet == 0) {
                            toRet = mInvId.compareTo(other.mInvId);
                        }
                    }
                } else {
                    if (mTimeless)
                        toRet = 1;
                    else
                        toRet = -1;
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
            return (mTimeless == other.mTimeless) && (mStart == other.mStart) && (mEnd == other.mEnd)
                    && (mInvId.equals(other.mInvId));
        }

        public String toString() {
            StringBuilder toRet = new StringBuilder("INST(");
            Date dstart = new Date(mStart);
            Date dend = new Date(mEnd);
            toRet.append(mTimeless).append(",");
            toRet.append(dstart).append(",").append(dend).append(",").append(
                    mIsException);
            toRet.append(",ID=").append(mInvId.getMsgId()).append("-").append(
                    mInvId.getComponentId());
            toRet.append(")");
            return toRet.toString();
        }
        
        public CalendarItem getCalendarItem() { return mCalItem; }
        public int getMailItemId() { return mInvId.getMsgId(); }
        public int getComponentNum() { return mInvId.getComponentId(); }
        public long getStart() { return mStart; }
        public long getEnd() { return mEnd; }
        public boolean isTimeless() { return mTimeless; }
        public boolean isException() { return mIsException; }
        public void setIsException(boolean isException) { mIsException = isException; } 
        public InviteInfo getInviteInfo() { return mInvId; } 

        public static class StartTimeComparator implements Comparator<Instance> {
            public int compare(Instance a, Instance b) {
                long as = a.getStart();
                long bs = b.getStart();
                if (as < bs)
                    return -1;
                else if (as == bs)
                    return 0;
                else
                    return 1;
            }
        }
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
    
    public Invite getDefaultInviteOrNull() {
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
                    "Invalid state: appointment/task " + getId() + " in mailbox " + getMailbox().getId() + " has no default invite; " +
                    (mInvites != null ? ("invite count = " + mInvites.size()) : "null invite list"));
        return first;
    }

    /**
     * @param pm
     * @param invite
     * @param force
     * @param folderId
     * @param volumeId
     * @return 
     *            TRUE if an update calendar was written, FALSE if the CalendarItem is 
     *            unchanged or deleted 
     * @throws ServiceException
     */
    boolean processNewInvite(ParsedMessage pm,
                          Invite invite,
                          boolean force, int folderId, short volumeId)
    throws ServiceException {
        return processNewInvite(pm, invite, force, folderId, volumeId, false);
    }

    /**
     * A new Invite has come in, take a look at it and see what needs to happen.
     * Maybe we need to send updates out. Maybe we need to modify the
     * CalendarItem table.
     * 
     * @param invite
     * @param force if true, then force update to this appointment/task,
     *              otherwise use RFC2446 sequencing rules
     * @return 
     *            TRUE if an update calendar was written, FALSE if the CalendarItem is 
     *            unchanged or deleted 
     */
    boolean processNewInvite(ParsedMessage pm,
                          Invite invite,
                          boolean force, int folderId, short volumeId,
                          boolean replaceExistingInvites)
    throws ServiceException {
        ZOrganizer originalOrganizer = null;
        Invite defInv = getDefaultInviteOrNull();
        if (defInv != null)
            originalOrganizer = defInv.getOrganizer();
        if (replaceExistingInvites) {
            mInvites.clear();
            //saveMetadata();
        }
        invite.setHasAttachment(pm.hasAttachments());
        
        String method = invite.getMethod();
        if (method.equals(ICalTok.REQUEST.toString()) || method.equals(ICalTok.CANCEL.toString()) || method.equals(ICalTok.PUBLISH.toString())) {
            return processNewInviteRequestOrCancel(originalOrganizer, pm, invite, force, folderId, volumeId);
        } else if (method.equals("REPLY")) {
            return processNewInviteReply(invite, force);
        }
        return false;
    }
    
    private boolean processNewInviteRequestOrCancel(ZOrganizer originalOrganizer,
                                                 ParsedMessage pm,
                                                 Invite newInvite,
                                                 boolean force,
                                                 int folderId,
                                                 short volumeId)
    throws ServiceException {

        // Remove everyone that is made obselete by this request
        boolean addNewOne = true;
        boolean isCancel = newInvite.isCancel();

        if (!canAccess(isCancel ? ACL.RIGHT_DELETE : ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on this appointment/task");

        // Look for potentially malicious change in organizer.
        if (newInvite.hasOrganizer()) {
            String newOrgAddr = newInvite.getOrganizer().getAddress();
            if (originalOrganizer == null) {
                // If organizer was previously unset, it means the appointment/task
                // had no attendee, i.e. a personal event.  When attendees are
                // added to a personal event, organizer must be set and only
                // to self.
                if (!newInvite.thisAcctIsOrganizer(getAccount()))
                    throw ServiceException.INVALID_REQUEST(
                            "Changing organizer of an appointment/task to another user is not allowed: old=(unspecified), new=" + newOrgAddr, null);
            } else {
                // Both old and new organizers are set.  They must be the
                // same address.
                String origOrgAddr = originalOrganizer.getAddress();
                if (!newOrgAddr.equalsIgnoreCase(origOrgAddr))
                    throw ServiceException.INVALID_REQUEST(
                            "Changing organizer of an appointment/task is not allowed: old=" + origOrgAddr + ", new=" + newOrgAddr, null);
            }
        } else {
            // Allow unsetting organizer only when previously set organizer
            // is self.
            if (originalOrganizer != null &&
                !AccountUtil.addressMatchesAccount(getAccount(), originalOrganizer.getAddress()))
                throw ServiceException.INVALID_REQUEST(
                        "Removing organizer of an appointment/task is not allowed", null);
        }

        // If modifying recurrence series (rather than an instance) and the
        // start time (HH:MM:SS) is changing, we need to update the time
        // component of RECURRENCE-ID in all exception instances.
        boolean needRecurrenceIdUpdate = false;
        ParsedDateTime oldDtStart = null;
        ParsedDuration dtStartMovedBy = null;
        ArrayList<Invite> toUpdate = new ArrayList<Invite>();
        if (!isCancel && newInvite.isRecurrence()) {
            Invite defInv = getDefaultInviteOrNull();
            if (defInv != null) {
                oldDtStart = defInv.getStartTime();
                ParsedDateTime newDtStart = newInvite.getStartTime();
                if (newDtStart != null && oldDtStart != null && !newDtStart.sameTime(oldDtStart)) {
                    needRecurrenceIdUpdate = true;
                    dtStartMovedBy = newDtStart.difference(oldDtStart);
                }
            }
        }

        boolean modifiedCalItem = false;
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
                        (cur.getSeqNo() < newInvite.getSeqNo()) ||
                        (cur.getSeqNo() == newInvite.getSeqNo() && cur.getDTStamp() <= newInvite.getDTStamp())) 
                {
                    Invite inf = mInvites.get(i);
                    toRemove.add(inf);
                    
                    // add to FRONT of list, so when we iterate for the removals we go from HIGHER TO LOWER
                    // that way the numbers all match up as the list contracts!
                    idxsToRemove.add(0, new Integer(i));
                    
                    // clean up any old REPLYs that have been made obscelete by this new invite
                    mReplyList.removeObsoleteEntries(newInvite.getRecurId(), newInvite.getSeqNo(), newInvite.getDTStamp());
                    
                    prev = cur;
                    modifiedCalItem = true;
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
                // Translate the date/time in RECURRENCE-ID to the timezone in
                // original recurrence DTSTART.  If they have the same HHMMSS
                // part, the RECURRENCE-ID need to be adjusted by the diff of
                // old and new recurrence DTSTART.
                if (rid != null && rid.getDt() != null) {
                    ParsedDateTime ridDt = (ParsedDateTime) rid.getDt().clone();
                    ICalTimeZone oldTz = oldDtStart.getTimeZone();
                    if (oldTz != null) {
                        ridDt.toTimeZone(oldTz);
                        if (ridDt.sameTime(oldDtStart))
                            toUpdate.add(cur);
                    }
                }
            }
        }

        boolean callProcessPartStat = false;
        if (addNewOne) {
            newInvite.setCalendarItem(this);
            Account account = getMailbox().getAccount();
            boolean thisAcctIsOrganizer =
                newInvite.thisAcctIsOrganizer(account);
            if (prev!=null && !thisAcctIsOrganizer && newInvite.sentByMe()) {
                // A non-organizer attendee is modifying data on his/her
                // appointment/task.  Any information that is tracked in
                // metadata rather than in the iCal MIME part must be
                // carried over from the last invite to the new one.
                newInvite.setPartStat(prev.getPartStat());
                newInvite.setRsvp(prev.getRsvp());
                newInvite.getCalendarItem().saveMetadata();
                // No need to mark invite as modified item in mailbox as
                // it has already been marked as a created item.
            } else {
                callProcessPartStat = true;
            }

            mInvites.add(newInvite);
            
            // the appointment/task stores an uber-tzmap, for its uber-recurrence
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
                    if (inv.isCancel())
                        inv.setDtStart(dt);
                }
            }

            modifyBlob(toRemove, toUpdate, pm, newInvite, volumeId);
            modifiedCalItem = true;
        } else {
            modifyBlob(toRemove, toUpdate, null, null, volumeId);
        }
        
        // now remove the inviteid's from our list  
        for (Iterator iter = idxsToRemove.iterator(); iter.hasNext();) {
            Integer i = (Integer)iter.next();
            mInvites.remove(i.intValue());
        }

        if (getFolderId() != folderId) {
            // Move appointment/task to a different folder.
            Folder folder = getMailbox().getFolderById(folderId);
            move(folder);
        }
        
        boolean hasAttachments = false;
        boolean hasRequests = false;
        for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
            Invite cur = (Invite)iter.next();
            if (cur.getMethod().equals(ICalTok.REQUEST.toString()) ||
                    cur.getMethod().equals(ICalTok.PUBLISH.toString())) {
                hasRequests = true;
            }
            if (cur.hasAttachment()) {
                hasAttachments = true;
            }
        }
        
        if (!hasRequests) {
            this.delete(); // delete this appointment/task from the table, it
                            // doesn't have anymore REQUESTs!
            return false;
        } else {
            if (modifiedCalItem) {
                if (!updateRecurrence()) {
                    // no default invite!  This appointment/task no longer valid
                    this.delete();
                    return false;
                } else {
                    if (callProcessPartStat) {
                        // processPartStat() must be called after
                        // updateRecurrence() has been called.  (bug 8072)
                        processPartStat(newInvite,
                                        pm != null ? pm.getMimeMessage() : null,
                                        false,
                                        newInvite.getPartStat());
                    }
                    if (hasAttachments) {
                        this.mData.flags |= Flag.BITMASK_ATTACHED;
                    } else {
                        this.mData.flags &= ~Flag.BITMASK_ATTACHED;
                    }
                    this.saveMetadata();
                    return true;
                }
            } else {
                return false;
            }
        }
    }
    
    /**
     * ParsedMessage DataSource -- for writing a ParsedMessage (new invite)
     * into our combined multipart/alternative Appointment store
     */
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
        ParsedMessage pm = new ParsedMessage(mm, mMailbox.attachmentsIndexingEnabled());
        try {
            setContent(pm.getRawData(), pm.getRawDigest(), volumeId, pm);
        } catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
        }
    }
    
    void reanalyze(Object data) throws ServiceException {
        String subject = null;
        Invite firstInvite = this.getDefaultInviteOrNull();
        if (firstInvite != null) {
            subject = firstInvite.getName();
        }
        if (subject == null)
            subject= "";
        
        mData.subject = subject;
        saveData(getSender());
    }

    /**
     * The Blob for the appointment/task is currently single Mime multipart/digest which has
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
     * Upate the Blob for this CalendarItem object: possibly remove one or more entries from it, 
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
        //         if a single incoming Message has multiple invites in it, all for this CalendarItem)
        try { 
            // now, make sure the message is in our blob already...
            MimeMessage mm;
            
            try {
                mm = getMimeMessage();
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
                markBlobForDeletion();
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
            Metadata metaAttendee = md.getMap(FN_ATTENDEE);
            toRet.mAttendee = metaAttendee != null ? new ZAttendee(metaAttendee) : null;
            
            return toRet;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("seq=").append(mSeqNo);
            sb.append(", dtstamp=").append(mDtStamp);
            if (mRecurId != null)
                sb.append(", recurId=\"").append(mRecurId).append("\"");
            sb.append(", attendee=").append(mAttendee);
            return sb.toString();
        }
    }

    /**
     * @author tim
     * 
     * Internal class for storing replies 
     *
     */
    protected static class ReplyList {
        List<ReplyInfo> mReplies;

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[ReplyList]\n");
            if (mReplies != null) {
                for (ReplyInfo ri : mReplies) {
                    sb.append(ri.toString()).append("\n");
                }
            }
            sb.append("[end]\n");
            return sb.toString();
        }

        public ReplyList() {
            mReplies = new ArrayList<ReplyInfo>();
        }
        
        private static final String FN_NUM_REPLY_INFO = "n";
        private static final String FN_REPLY_INFO = "i";
        
        Metadata encodeAsMetadata() {
            Metadata meta = new Metadata();
            
            meta.put(FN_NUM_REPLY_INFO, mReplies.size());
            for (int i = 0; i < mReplies.size(); i++) {
                String fn = FN_REPLY_INFO + i;
                
                meta.put(fn, mReplies.get(i).encodeAsMetadata());
            }
            return meta;
        }
        
        static ReplyList decodeFromMetadata(Metadata md, TimeZoneMap tzMap) throws ServiceException {
            ReplyList toRet = new ReplyList();
            int numReplies = (int) md.getLong(FN_NUM_REPLY_INFO);
            toRet.mReplies = new ArrayList<ReplyInfo>(numReplies);
            for (int i = 0; i < numReplies; i++) {
                ReplyInfo inf = ReplyInfo.decodeFromMetadata(md.getMap(FN_REPLY_INFO+i), tzMap);
                toRet.mReplies.add(i, inf);
            }
            return toRet;
        }
        
        void removeObsoleteEntries(RecurId recurId, int seqNo, long dtStamp) {
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
                                cur.mAttendee.getSentBy(),
                                cur.mAttendee.getDir(),
                                cur.mAttendee.getLanguage(),
                                cutypeStr,
                                roleStr,
                                partStatStr,
                                needsReply,
                                cur.mAttendee.getMember(),
                                cur.mAttendee.getDelegatedTo(),
                                cur.mAttendee.getDelegatedFrom()
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
            inf.mAttendee =
                new ZAttendee(addressStr, cnStr, null, null, null,
                              cutypeStr, roleStr, partStatStr, needsReply,
                              null, null, null);
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
        
        List<ReplyInfo> getReplyInfo(Invite inv) {
            int mboxId = -1;
            int calItemId = -1;
            int invId = -1;
            if (inv != null) {
                mboxId = inv.getMailboxId();
                invId = inv.getMailItemId();
                try {
                    CalendarItem citem = inv.getCalendarItem();
                    if (citem != null)
                        calItemId = citem.getId();
                } catch (ServiceException e) {}
            }

            List<ReplyInfo> toRet = new ArrayList<ReplyInfo>();

            int outdated = 0;
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
                    outdated++;
                    if (ZimbraLog.calendar.isDebugEnabled())
                        ZimbraLog.calendar.debug(
                                "ReplyList of appt/task " + calItemId +
                                ", inv " + invId +
                                " in mailbox " + mboxId +
                                " has outdated entries:\n" + toString());
                }
            }
            if (ZimbraLog.calendar.isDebugEnabled())
                ZimbraLog.calendar.debug(
                        "Found " + toRet.size() + " ReplyList entries for appt/task " +
                        calItemId + ", inv " + invId + " in mailbox " + mboxId);
            if (outdated > 0)
                ZimbraLog.calendar.warn(
                        "Found " + outdated + " outdated ReplyInfo entries for appt/task " +
                        calItemId + ", inv " + invId + " in mailbox " + mboxId);
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
    public List<ReplyInfo> getReplyInfo(Invite inv) {
        return mReplyList.getReplyInfo(inv);
    }
    
    /**
     * Returns the effective PartStat given the Invite and Instance.
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
     * Used when we're sending out a reply -- we add a "reply" record to this appointment/task...this
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
    
    public boolean processNewInviteReply(Invite reply, boolean force)
    throws ServiceException {
        if (!canAccess(ACL.RIGHT_ACTION))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions to change this appointment/task's state");

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
                    return false;
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
            return true;
        } else {
            return false;
        }
    }
    
    public InputStream getRawMessage() throws ServiceException {
        return MessageCache.getRawContent(this);
    }

    void appendRawCalendarData(ZVCalendar cal,
                               boolean useOutlookCompatMode,
                               boolean ignoreErrors)
    throws ServiceException {
        for (Iterator invIter = mInvites.iterator(); invIter.hasNext();) {
            Invite inv = (Invite)invIter.next();
            try {
                cal.addComponent(inv.newToVComponent(useOutlookCompatMode));
            } catch (ServiceException e) {
                if (ignoreErrors) {
                    ZimbraLog.calendar.warn(
                            "Error retrieving iCalendar data for item " +
                            inv.getMailItemId() + ": " + e.getMessage(), e);
                } else
                    throw e;
            }
        }
    }
    
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


    protected abstract String processPartStat(Invite invite,
                                              MimeMessage mmInv,
                                              boolean forCreate,
                                              String defaultPartStat)
    throws ServiceException;
}
