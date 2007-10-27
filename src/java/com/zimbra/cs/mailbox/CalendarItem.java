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
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Alarm;
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
import com.zimbra.common.util.ByteUtil;
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

    // these are special values indexed in the L_FIELD structured index field, they allow us to 
    // restrict lucene searches keyed off of public/private settings
    //
    // the constants should be all-lowercase if possible, saves us doing a toLower if we need to
    // manually build a query to search for them w/o using our custom FieldTokenStream 
    public static final String INDEX_FIELD_ITEM_CLASS_PUBLIC = "_calendaritemclass:public"; 
    public static final String INDEX_FIELD_ITEM_CLASS_PRIVATE = "_calendaritemclass:private"; 
    
    static Log sLog = LogFactory.getLog(CalendarItem.class);

    private String mUid;

    /** the time IN MSEC UTC that this appointment/task "starts" */
    private long mStartTime;
    /** the time IN MSEC UTC that this appointment/task "ends" */
    private long mEndTime;

    private AlarmData mAlarmData;  // next/last alarm info

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
        Invite firstInvite = getDefaultInviteOrNull();
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

    public AlarmData getAlarmData() {
        return mAlarmData;
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
            docs, getDate());
    }
    
    protected List<org.apache.lucene.document.Document> getLuceneDocuments() throws ServiceException {
        List<org.apache.lucene.document.Document> toRet = 
            new ArrayList<org.apache.lucene.document.Document>();

        // Special case to prevent getDefaultInviteOrNull() from logging an error
        // when this method is called during commit of cancel operation.
        if (numInvites() < 1)
            return toRet;

        Invite defaultInvite = getDefaultInviteOrNull();
        
        String defaultLocation = "";
        if (defaultInvite != null && defaultInvite.getLocation() != null)
            defaultLocation = defaultInvite.getLocation();
        
        for (Invite inv : getInvites()) {
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
                throw ServiceException.FAILURE("Failure Indexing: " + toString(), e);
            }
        }
        
        // set the "public" flag in the index for this appointment
        String itemClass;
        if (this.isPublic())
            itemClass = INDEX_FIELD_ITEM_CLASS_PUBLIC;
        else
            itemClass = INDEX_FIELD_ITEM_CLASS_PRIVATE;
        for (org.apache.lucene.document.Document doc : toRet) {
            doc.add(new Field(LuceneFields.L_FIELD, itemClass, Field.Store.NO, Field.Index.TOKENIZED));
        }
        
        return toRet;
    }
    
    static CalendarItem create(int id, Folder folder, short volumeId, int flags, long tags,
                               String uid, ParsedMessage pm, Invite firstInvite,
                               long nextAlarm)
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
        data.metadata = encodeMetadata(DEFAULT_COLOR, 1, uid, startTime, endTime,
                                       recur, invites, firstInvite.getTimeZoneMap(),
                                       new ReplyList(), null);
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

        if (firstInvite.hasAlarm()) {
            item.recomputeNextAlarm(nextAlarm);
            item.saveMetadata();
        }

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
            for (Invite cur : mInvites) {
                if (cur != firstInv) {
                    String method = cur.getMethod();
                    if (cur.isCancel()) {
                        assert(cur.hasRecurId());
                        if (cur.hasRecurId()) {
                            Recurrence.CancellationRule cancelRule =   
                                new Recurrence.CancellationRule(cur.getRecurId());
                            
                            ((Recurrence.RecurrenceRule) mRecurrence).addException(cancelRule);
                        }
                    } else if (method.equals(ICalTok.REQUEST.toString()) ||
                        method.equals(ICalTok.PUBLISH.toString())) {
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

            Metadata metaAlarmData = meta.getMap(Metadata.FN_ALARM_DATA, true);
            if (metaAlarmData != null)
                mAlarmData = AlarmData.decodeMetadata(metaAlarmData);
        }
    }

    Metadata encodeMetadata(Metadata meta) {
        return encodeMetadata(meta, mColor, mVersion, mUid, mStartTime, mEndTime,
                mRecurrence, mInvites, mTzMap, mReplyList, mAlarmData);
    }
    private static String encodeMetadata(byte color, int version, String uid, long startTime, long endTime,
                                         Recurrence.IRecurrence recur, List<Invite> invs, TimeZoneMap tzmap,
                                         ReplyList replyList, AlarmData alarmData) {
        return encodeMetadata(new Metadata(), color, version, uid, startTime, endTime, recur,
                              invs, tzmap, replyList, alarmData).toString();
    }
    static Metadata encodeMetadata(Metadata meta, byte color, int version, String uid,
                                   long startTime, long endTime,
                                   Recurrence.IRecurrence recur,
                                   List<Invite> invs, TimeZoneMap tzmap,
                                   ReplyList replyList, AlarmData alarmData) {
        meta.put(Metadata.FN_TZMAP, tzmap.encodeAsMetadata());
        meta.put(Metadata.FN_UID, uid);
        meta.put(Metadata.FN_CALITEM_START, startTime);
        meta.put(Metadata.FN_CALITEM_END, endTime);
        meta.put(Metadata.FN_NUM_COMPONENTS, invs.size());
        
        meta.put(Metadata.FN_REPLY_LIST, replyList.encodeAsMetadata());

        int num = 0;
        for (Invite comp : invs)
            meta.put(Metadata.FN_INV + num++, Invite.encodeMetadata(comp));

        if (recur != null)
            meta.put(FN_CALITEM_RECURRENCE, recur.encodeMetadata());

        if (alarmData != null)
            meta.put(Metadata.FN_ALARM_DATA, alarmData.encodeMetadata());

        return MailItem.encodeMetadata(meta, color, version);
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
                for (Invite inv : mInvites) {
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
        for (Invite inv : mInvites) {
            InviteInfo inf = new InviteInfo(inv);
            if (inf.compareTo(id) == 0) {
                return inv;
            }
        }
        return null;
    }
    
    public Invite getInvite(int invId, int compNum) {
        for (Invite inv : mInvites) {
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

    public Invite getInvite(int index) {
        return mInvites.get(index);
    }
    
    public Invite getDefaultInviteOrNull() {
        Invite first = null;
        for (Invite cur : mInvites) {
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
     * Returns true if all invites in the calendar item have CLASS:PUBLIC.
     * @return
     */
    public boolean isPublic() {
        boolean result = true;
        Invite[] invs = getInvites();
        if (invs != null && invs.length > 0) {
            for (Invite i : invs) {
                if (!i.isPublic()) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Returns true if authAccount should be allowed access to private data in this appointment.
     * Returns true if authAccount is the account that owns the appointment, or authAccount has
     * admin rights over the owner account.
     * @param authAccount
     * @return
     * @throws ServiceException
     */
    public boolean allowPrivateAccess(Account authAccount) throws ServiceException {
        return getAccount().allowPrivateAccess(authAccount);
    }

    boolean processNewInvite(ParsedMessage pm, Invite invite, boolean force,
                             int folderId, short volumeId)
    throws ServiceException {
        return processNewInvite(pm, invite, force, folderId, volumeId, 0, false);
    }

    boolean processNewInvite(ParsedMessage pm, Invite invite, boolean force,
                             int folderId, short volumeId, long nextAlarm)
    throws ServiceException {
        return processNewInvite(pm, invite, force, folderId, volumeId, nextAlarm, false);
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
                             long nextAlarm,
                             boolean replaceExistingInvites)
    throws ServiceException {
        invite.setHasAttachment(pm.hasAttachments());

        String method = invite.getMethod();
        if (method.equals(ICalTok.REQUEST.toString()) ||
            method.equals(ICalTok.CANCEL.toString()) ||
            method.equals(ICalTok.PUBLISH.toString())) {
            return processNewInviteRequestOrCancel(pm, invite, force, folderId, volumeId,
                                                   nextAlarm, replaceExistingInvites);
        } else if (method.equals("REPLY")) {
            return processNewInviteReply(invite, force);
        }

        ZimbraLog.calendar.warn("Unsupported METHOD " + method);
        return false;
    }

    private boolean processNewInviteRequestOrCancel(ParsedMessage pm,
                                                    Invite newInvite,
                                                    boolean force,
                                                    int folderId,
                                                    short volumeId,
                                                    long nextAlarm,
                                                    boolean replaceExistingInvites)
    throws ServiceException {
        // Remove everyone that is made obsolete by this request
        boolean addNewOne = true;
        boolean isCancel = newInvite.isCancel();

        if (!canAccess(isCancel ? ACL.RIGHT_DELETE : ACL.RIGHT_WRITE))
            throw ServiceException.PERM_DENIED("you do not have sufficient permissions on this appointment/task");

        // Don't allow creating/editing a private appointment on behalf of another user,
        // unless that other user is a calendar resource.
        boolean isCalendarResource = getMailbox().getAccount() instanceof CalendarResource;
        OperationContext octxt = getMailbox().getOperationContext();
        Account authAccount = octxt != null ? octxt.getAuthenticatedUser() : null;
        boolean denyPrivateAccess = authAccount != null && !allowPrivateAccess(authAccount);
        if (denyPrivateAccess && !newInvite.isPublic() && !isCalendarResource)
            throw ServiceException.PERM_DENIED("private appointment/task cannot be created/edited on behalf of another user");

        boolean organizerChanged = organizerChangeCheck(newInvite, isCancel);
        ZOrganizer newOrganizer = newInvite.getOrganizer();

        // If modifying recurrence series (rather than an instance) and the
        // start time (HH:MM:SS) is changing, we need to update the time
        // component of RECURRENCE-ID in all exception instances.
        boolean needRecurrenceIdUpdate = false;
        ParsedDateTime oldDtStart = null;
        ParsedDuration dtStartMovedBy = null;
        ArrayList<Invite> toUpdate = new ArrayList<Invite>();
        if (!replaceExistingInvites && !isCancel && newInvite.isRecurrence()) {
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
            if (replaceExistingInvites ||
                (cur.getRecurId() != null && cur.getRecurId().equals(newInvite.getRecurId())) ||
                (cur.getRecurId() == null && newInvite.getRecurId() == null)) {
                if (replaceExistingInvites || force ||
                    (cur.getSeqNo() < newInvite.getSeqNo()) ||
                    (cur.getSeqNo() == newInvite.getSeqNo() && cur.getDTStamp() <= newInvite.getDTStamp())) 
                {
                    Invite inf = mInvites.get(i);
                    toRemove.add(inf);
                    
                    // add to FRONT of list, so when we iterate for the removals we go from HIGHER TO LOWER
                    // that way the numbers all match up as the list contracts!
                    idxsToRemove.add(0, new Integer(i));
                    
                    // clean up any old REPLYs that have been made obsolete by this new invite
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
//              break; // don't stop here!  new Invite *could* obsolete multiple existing ones! 
            } else if (needRecurrenceIdUpdate || organizerChanged) {
                // If organizer is changing on any invite, change it on all invites.
                boolean added = false;
                if (organizerChanged) {
                    cur.setOrganizer(newOrganizer);
                    toUpdate.add(cur);
                    added = true;
                }
                // Translate the date/time in RECURRENCE-ID to the timezone in
                // original recurrence DTSTART.  If they have the same HHMMSS
                // part, the RECURRENCE-ID need to be adjusted by the diff of
                // old and new recurrence DTSTART.
                RecurId rid = cur.getRecurId();
                if (rid != null && rid.getDt() != null && oldDtStart != null) {
                    ParsedDateTime ridDt = (ParsedDateTime) rid.getDt().clone();
                    ICalTimeZone oldTz = oldDtStart.getTimeZone();
                    if (oldTz != null) {
                        ridDt.toTimeZone(oldTz);
                        if (ridDt.sameTime(oldDtStart) && !added)
                            toUpdate.add(cur);
                    }
                }
            }
        }

        boolean callProcessPartStat = false;
        if (addNewOne) {
            newInvite.setCalendarItem(this);

            // Don't allow creating/editing a private appointment on behalf of another user,
            // unless that other user is a calendar resource.
            if (denyPrivateAccess && prev != null && !prev.isPublic() && !isCalendarResource)
                throw ServiceException.PERM_DENIED("you do not have sufficient permissions on this appointment/task");

            if (prev!=null && !newInvite.isOrganizer() && newInvite.sentByMe()) {
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

            newInvite.setClassPropSetByMe(newInvite.sentByMe());

            // If updating attendee's appointment/task with a new version published by organizer
            // and attendee's copy was previously set to private/confidential by himself/herself,
            // retain the value and therefore don't allow the organizer to override it.
            if (prev != null && !newInvite.isOrganizer() && !newInvite.sentByMe()) {
                if (!prev.isPublic() && prev.classPropSetByMe()) {
                    newInvite.setClassProp(prev.getClassProp());
                    newInvite.setClassPropSetByMe(true);
                }
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

            modifyBlob(toRemove, replaceExistingInvites, toUpdate, pm, newInvite, volumeId, isCancel, !denyPrivateAccess);
            modifiedCalItem = true;
        } else {
            modifyBlob(toRemove, replaceExistingInvites, toUpdate, null, null, volumeId, isCancel, !denyPrivateAccess);
        }
        
        // now remove the inviteid's from our list
        for (Iterator<Integer> iter = idxsToRemove.iterator(); iter.hasNext();) {
            Integer i = iter.next();
            mInvites.remove(i.intValue());
        }

        if (getFolderId() != folderId) {
            // Move appointment/task to a different folder.
            Folder folder = getMailbox().getFolderById(folderId);
            move(folder);
        }
        
        boolean hasAttachments = false;
        boolean hasRequests = false;
        for (Invite cur : mInvites) {
            String method = cur.getMethod();
            if (method.equals(ICalTok.REQUEST.toString()) ||
                method.equals(ICalTok.PUBLISH.toString())) {
                hasRequests = true;
            }
            if (cur.hasAttachment()) {
                hasAttachments = true;
            }
        }

        if (!hasRequests) {
            if (!isCancel)
                ZimbraLog.calendar.warn(
                        "Invalid state: deleting calendar item " + getId() +
                        " in mailbox " + getMailboxId() + " while processing a non-cancel request");
            delete();  // delete this appointment/task from the table,
                       // it doesn't have anymore REQUESTs!
            return false;
        } else {
            if (modifiedCalItem) {
                if (!updateRecurrence()) {
                    // no default invite!  This appointment/task no longer valid
                    delete();
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
                        mData.flags |= Flag.BITMASK_ATTACHED;
                    } else {
                        mData.flags &= ~Flag.BITMASK_ATTACHED;
                    }

                    // Update alarm trigger time and related info.
                    recomputeNextAlarm(nextAlarm);

                    saveMetadata();
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    /**
     * Check to make sure the new invite doesn't change the organizer in a disallowed way.
     * @param newInvite
     * @return true if organizer change was detected, false if no change
     * @throws ServiceException
     */
    private boolean organizerChangeCheck(Invite newInvite, boolean denyChange)
    throws ServiceException {
        Invite originalInvite = null;
        if (!newInvite.hasRecurId()) {
            // New invite is not for an exception.
            originalInvite = getDefaultInviteOrNull();
        } else {
            // New invite is for an exception.
            boolean found = false;
            RecurId newRid = newInvite.getRecurId();
            for (Invite inv : mInvites) {
                if (inv.hasRecurId() && newRid.equals(inv.getRecurId())) {
                    originalInvite = inv;
                    found = true;
                    break;
                }
            }
            if (!found) {
                // If no invite with matching RECURRENCE-ID was found, use the default invite.
                originalInvite = getDefaultInviteOrNull();
            }
        }
        if (originalInvite == null) {
            // If no "default" invite was found, use the first one.
            if (mInvites.size() > 0)
                originalInvite = mInvites.get(0);
            if (originalInvite == null) {
                // Still no invite.  Something is seriously wrong.  Return without any further
                // checks in this method.
                return false;
            }
        }

        boolean changed = false;
        ZOrganizer originalOrganizer = originalInvite.getOrganizer();
        if (!originalInvite.isOrganizer()) {
            // This account WAS NOT the organizer.  Prevent organizer change.
            if (newInvite.hasOrganizer()) {
                String newOrgAddr = newInvite.getOrganizer().getAddress();
                if (originalOrganizer == null) {
                    if (denyChange) {
                        throw ServiceException.INVALID_REQUEST(
                                "Changing organizer of an appointment/task to another user is not allowed: old=(unspecified), new=" + newOrgAddr, null);
                    } else {
                        changed = true;
                    }
                } else {
                    // Both old and new organizers are set.  They must be the
                    // same address.
                    String origOrgAddr = originalOrganizer.getAddress();
                    if (newOrgAddr == null || !newOrgAddr.equalsIgnoreCase(origOrgAddr)) {
                        if (denyChange) {
                            throw ServiceException.INVALID_REQUEST(
                                    "Changing organizer of an appointment/task is not allowed: old=" + origOrgAddr + ", new=" + newOrgAddr, null);
                        } else {
                            changed = true;
                        }
                    }
                }
            } else if (originalOrganizer != null) {
                if (denyChange) {
                    throw ServiceException.INVALID_REQUEST(
                            "Removing organizer of an appointment/task is not allowed", null);
                } else {
                    changed = true;
                }
            }
        } else {
            // Even for the organizer account, don't allow changing the organizer field
            // to an arbitrary address.
            if (newInvite.hasOrganizer()) {
                if (!newInvite.isOrganizer()) {
                    if (denyChange) {
                        String newOrgAddr = newInvite.getOrganizer().getAddress();
                        if (originalOrganizer != null) {
                            String origOrgAddr = originalOrganizer.getAddress();
                            throw ServiceException.INVALID_REQUEST(
                                    "Changing organizer of an appointment/task to another user is not allowed: old=" +
                                    origOrgAddr + ", new=" + newOrgAddr, null);
                        } else {
                            throw ServiceException.INVALID_REQUEST(
                                    "Changing organizer of an appointment/task to another user is not allowed: old=(unspecified), new=" +
                                    newOrgAddr, null);
                        }
                    } else {
                        changed = true;
                    }
                }
            }
        }
        return changed;
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
            byte[] data = pm.getRawData();
            if (data == null)
                ZimbraLog.calendar.warn(
                        "Invalid state: updating blob with null data for calendar item " + getId() +
                        " in mailbox " + getMailboxId());
            setContent(data, pm.getRawDigest(), volumeId, pm);
        } catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
        }
    }
    
    void reanalyze(Object data) throws ServiceException {
        String subject = null;
        Invite firstInvite = getDefaultInviteOrNull();
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
     * @param removeAllExistingInvites if true, all existing invites are removed regardless of
     *                                 what's passed in toRemove list
     * @param toUpdate
     * @param invPm
     * @param newInv
     * @param volumeId
     * @param isCancel if the method is being called while processing a cancel request
     * @throws ServiceException
     */
    private void modifyBlob(List<Invite> toRemove,
                            boolean removeAllExistingInvites,
                            List<Invite> toUpdate,
                            ParsedMessage invPm,
                            Invite newInv,
                            short volumeId,
                            boolean isCancel,
                            boolean allowPrivateAccess)
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
                ZimbraLog.calendar.error("Error reading blob for calendar item " + getId() +
                                         " in mailbox " + getMailboxId(), e);
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
            if (removeAllExistingInvites) {
                int numParts = mmp.getCount();
                if (numParts > 0) {
                    for (int i = numParts - 1; i >= 0; i--) {
                        mmp.removeBodyPart(i);
                    }
                    updated = true;
                }
            } else {
                // Remove specific invites.
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
                    ZVCalendar cal = inv.newToICalendar(allowPrivateAccess);
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
                if (!isCancel)
                    ZimbraLog.calendar.warn("Invalid state: deleting blob for calendar item " + getId() +
                            " in mailbox " + getMailboxId() + " while processing a non-cancel request");
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
        private ZAttendee mAttendee; // attendee record w/ PartStat
        private int mSeqNo;
        private long mDtStamp;
        private RecurId mRecurId;

        private static final String FN_RECURID = "r";
        private static final String FN_SEQNO = "s";
        private static final String FN_DTSTAMP = "d";
        private static final String FN_ATTENDEE = "at";

        public ReplyInfo(ZAttendee at, int seq, long dtstamp, RecurId recurId) {
            mAttendee = at;
            mSeqNo = seq;
            mDtStamp = dtstamp;
            mRecurId = recurId;
        }

        public ZAttendee getAttendee() { return mAttendee; }
        public int getSeq() { return mSeqNo; }
        public long getDtStamp() { return mDtStamp; }
        public RecurId getRecurId() { return mRecurId; }

        public Metadata encodeAsMetadata() {
            Metadata meta = new Metadata();
            
            if (mRecurId != null) {
                meta.put(FN_RECURID, mRecurId.encodeMetadata());
            }
            
            meta.put(FN_SEQNO, mSeqNo);
            meta.put(FN_DTSTAMP, mDtStamp);
            meta.put(FN_ATTENDEE, mAttendee.encodeAsMetadata());

            return meta;
        }
        
        public static ReplyInfo decodeFromMetadata(Metadata md, TimeZoneMap tzMap)
        throws ServiceException {
            RecurId recurId;
            if (md.containsKey(FN_RECURID)) {
                recurId = RecurId.decodeMetadata(md.getMap(FN_RECURID), tzMap);
            } else {
                recurId = null;
            }
            int seq = (int) md.getLong(FN_SEQNO);
            long dtstamp = md.getLong(FN_DTSTAMP);
            Metadata metaAttendee = md.getMap(FN_ATTENDEE);
            ZAttendee at = metaAttendee != null ? new ZAttendee(metaAttendee) : null;
            ReplyInfo ri = new ReplyInfo(at, seq, dtstamp, recurId);
            return ri;
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

        public ReplyList(List<ReplyInfo> list) {
            mReplies = list;
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
            for (Iterator<ReplyInfo> iter = mReplies.iterator(); iter.hasNext();) {
                ReplyInfo cur = iter.next();
                
                if (recurMatches(cur.mRecurId, recurId)) {
                    if (cur.mSeqNo <= seqNo && cur.mDtStamp <= dtStamp) {
                        iter.remove();
                    }
                }
            }
        }
        
        boolean maybeStoreNewReply(Invite inv, ZAttendee at) {
            for (Iterator<ReplyInfo> iter = mReplies.iterator(); iter.hasNext();) {
                ReplyInfo cur = (ReplyInfo)iter.next();
                
                if (at.addressesMatch(cur.mAttendee)) {
                    if (recurMatches(inv.getRecurId(), cur.mRecurId)) {
                        if (inv.getSeqNo() >= cur.mSeqNo) {
                            if (inv.getDTStamp() >= cur.mDtStamp) {
                                iter.remove();
                                ReplyInfo toAdd = new ReplyInfo(
                                        at, inv.getSeqNo(), inv.getDTStamp(), inv.getRecurId());
                                mReplies.add(toAdd);
                                return true;
                            }
                        }
                        return false;
                    }
                } // attendee check
            }

            // if we get here, we didn't find one at all.  add a new one...
            ReplyInfo toAdd = new ReplyInfo(at, inv.getSeqNo(), inv.getDTStamp(), inv.getRecurId());
            mReplies.add(toAdd);            
            return true;
        }
        
        void modifyPartStat(Account acctOrNull, RecurId recurId, String cnStr, String addressStr, String cutypeStr, String roleStr, 
                String partStatStr, Boolean needsReply, int seqNo, long dtStamp)  throws ServiceException {
            for (ReplyInfo cur : mReplies) {
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
            ZAttendee at =
                new ZAttendee(addressStr, cnStr, null, null, null,
                              cutypeStr, roleStr, partStatStr, needsReply,
                              null, null, null);
            ReplyInfo ri = new ReplyInfo(at, seqNo, dtStamp, recurId);
            mReplies.add(ri);
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

            for (ReplyInfo cur : mReplies) {
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
            List<ReplyInfo> toRet = new ArrayList<ReplyInfo>();

            if (inv == null) {
                // all replies requested
                toRet.addAll(mReplies);
            } else {
                long invDtStart = -1;
                ParsedDateTime dtStart = inv.getStartTime();
                if (dtStart != null)
                    invDtStart = dtStart.getUtcTime();
                // Look for an exact match first.
                for (ReplyInfo reply : mReplies) {
                    // Ignore reply to series and replies to outdated invites.
                    if (reply.mRecurId != null &&
                        inv.getSeqNo() <= reply.mSeqNo && inv.getDTStamp() <= reply.mDtStamp) {
                        ParsedDateTime dt = reply.mRecurId.getDt();
                        if (dt != null) {
                            if (dt.getUtcTime() == invDtStart)
                                toRet.add(reply);
                        }
                    }
                }
                if (toRet.size() < 1) {
                    // No specific match.  Use the series reply, if any.
                    for (ReplyInfo reply : mReplies) {
                        if (inv.getSeqNo() <= reply.mSeqNo && inv.getDTStamp() <= reply.mDtStamp &&
                            reply.mRecurId == null)
                            toRet.add(reply);
                    }
                }
            }
            return toRet;
        }
    } // class ReplyList
            
    /**
     * Get all of the Reply data corresponding to this invite.  Pass null to get all replies.
     * 
     * @param inv
     * @return
     * @throws ServiceException
     */
    public List<ReplyInfo> getReplyInfo(Invite inv) {
        return mReplyList.getReplyInfo(inv);
    }

    /**
     * Set the replies list to the given list.  Existing list is replaced.
     * @param replies
     */
    public void setReplies(List<ReplyInfo> replies) throws ServiceException {
        mReplyList = new ReplyList(replies);
        saveMetadata();
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
        if (at == null || inv.isOrganizer()) {
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
        List<ZAttendee> attendees = reply.getAttendees();
        for (ZAttendee at : attendees) {
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
                               boolean ignoreErrors,
                               boolean allowPrivateAccess)
    throws ServiceException {
        for (Invite inv : mInvites) {
            try {
                cal.addComponent(inv.newToVComponent(useOutlookCompatMode, allowPrivateAccess));
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
            ByteUtil.closeStream(is);

            try {
                for (Class<? extends MimeVisitor> visitor : MimeVisitor.getConverters())
                    visitor.newInstance().accept(mm);
            } catch (Exception e) {
                // If the conversion bombs for any reason, revert to the original
                ZimbraLog.mailbox.warn(
                    "MIME converter failed for message " + getId(), e);
                is = getRawMessage();
                mm = new MimeMessage(JMSession.getSession(), is);
                ByteUtil.closeStream(is);
            }
            
            return mm;
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException while getting MimeMessage for item " + mId, e);
        } finally {
            ByteUtil.closeStream(is);
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
            ByteUtil.closeStream(is);

            try {
                for (Class<? extends MimeVisitor> visitor : MimeVisitor.getConverters())
                    visitor.newInstance().accept(mm);
            } catch (Exception e) {
                // If the conversion bombs for any reason, revert to the original
                ZimbraLog.mailbox.warn("MIME converter failed for message " + getId(), e);
                is = getRawMessage();
                mm = new MimeMessage(JMSession.getSession(), is);
                ByteUtil.closeStream(is);
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
        } finally {
            ByteUtil.closeStream(is);
        }
    }


    protected abstract String processPartStat(Invite invite,
                                              MimeMessage mmInv,
                                              boolean forCreate,
                                              String defaultPartStat)
    throws ServiceException;


    public static class AlarmData {
        private long mNextAt = Long.MAX_VALUE;
        private long mNextInstStart;  // start time of the instance that mNextAt alarm is for
        private int mInvId;
        private int mCompNum;
        private Alarm mAlarm;

        public AlarmData(long next, long nextInstStart, int invId, int compNum, Alarm alarm) {
            mNextAt = next;
            mNextInstStart = nextInstStart;
            mInvId = invId;
            mCompNum = compNum;
            mAlarm = alarm;
        }

        public long getNextAt() { return mNextAt; }
        public long getNextInstanceStart() { return mNextInstStart; }
        public int getInvId() { return mInvId; }
        public int getCompNum() { return mCompNum; }
        public Alarm getAlarm() { return mAlarm; }

        private static final String FNAME_NEXT_AT = "na";
        private static final String FNAME_NEXT_INSTANCE_START = "nis";
        private static final String FNAME_INV_ID = "invId";
        private static final String FNAME_COMP_NUM = "compNum";
        private static final String FNAME_ALARM = "alarm";

        private static AlarmData decodeMetadata(Metadata meta)
        throws ServiceException {
            long nextAt = meta.getLong(FNAME_NEXT_AT);
            long nextInstStart = meta.getLong(FNAME_NEXT_INSTANCE_START);
            int invId = (int) meta.getLong(FNAME_INV_ID);
            int compNum = (int) meta.getLong(FNAME_COMP_NUM);
            Alarm alarm = null;
            Metadata metaAlarm = meta.getMap(FNAME_ALARM, true);
            if (metaAlarm != null)
                alarm = Alarm.decodeMetadata(metaAlarm);
            return new AlarmData(nextAt, nextInstStart, invId, compNum, alarm);
        }

        private Metadata encodeMetadata() {
            Metadata meta = new Metadata();
            meta.put(FNAME_NEXT_AT, mNextAt);
            meta.put(FNAME_NEXT_INSTANCE_START, mNextInstStart);
            meta.put(FNAME_INV_ID, mInvId);
            meta.put(FNAME_COMP_NUM, mCompNum);
            if (mAlarm != null)
                meta.put(FNAME_ALARM, mAlarm.encodeMetadata());
            return meta;
        }
    }

    public boolean hasAlarm() {
        if (mInvites != null && mInvites.size() > 0) {
            for (Invite inv : mInvites) {
                if (inv.hasAlarm())
                    return true;
            }
        }
        return false;
    }

    /**
     * Recompute the next alarm trigger time that is at or later than "nextAlarm".
     * @param nextAlarm
     */
    public void recomputeNextAlarm(long nextAlarm) {
        if (!hasAlarm()) {
            mAlarmData = null;
            return;
        }

        // Special case nextAlarm == 0, which means to preserve last dismissed alarm time.
        if (nextAlarm <= 0) {
            if (mAlarmData != null)
                nextAlarm = mAlarmData.getNextAt();
        }

        long triggerAt = Long.MAX_VALUE;
        Instance alarmInstance = null;
        Alarm theAlarm = null;
        Collection<Instance> instances = expandInstances(nextAlarm, getEndTime());
        for (Instance inst : instances) {
            InviteInfo invId = inst.getInviteInfo();
            Invite inv = getInvite(invId.getMsgId(), invId.getComponentId());
            Pair<Long, Alarm> curr =
                getAlarmTriggerTime(nextAlarm, inv.alarmsIterator(), inst.getStart(), inst.getEnd());
            if (curr != null) {
                long currAt = curr.getFirst();
                if (nextAlarm <= currAt && currAt < triggerAt) {
                    triggerAt = currAt;
                    theAlarm = curr.getSecond();
                    alarmInstance = inst;
                }
            }
        }
        if (alarmInstance != null) {
            InviteInfo invInfo = alarmInstance.getInviteInfo();
            if (invInfo != null)
                mAlarmData = new AlarmData(triggerAt, alarmInstance.getStart(),
                                           invInfo.getMsgId(), invInfo.getComponentId(), theAlarm);
        } else {
            mAlarmData = null;
        }
    }

    private static Pair<Long, Alarm> getAlarmTriggerTime(
            long nextAlarm, Iterator<Alarm> alarms, long instStart, long instEnd) {
        long triggerAt = Long.MAX_VALUE;
        Alarm theAlarm = null;
        for (; alarms.hasNext(); ) {
            Alarm alarm = alarms.next();
            long currTrigger = alarm.getTriggerTime(instStart, instEnd);
            if (nextAlarm <= currTrigger && currTrigger < triggerAt) {
                triggerAt = currTrigger;
                theAlarm = alarm;
            }
        }
        if (theAlarm != null)
            return new Pair<Long, Alarm>(triggerAt, theAlarm);
        else
            return null;
    }
}
