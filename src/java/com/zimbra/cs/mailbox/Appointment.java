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
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.property.Method;

import com.zimbra.cs.db.DbMailItem;

import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.mime.TnefConverter;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.store.Blob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.JMSession;
import com.zimbra.cs.util.ZimbraLog;

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
    private String mUid;

    List /* Invite */ mInvites;
    
    // the time IN MSEC UTC that this appointment "starts"
    private long mStartTime;

    // the time IN MSEC UTC that this appointment "ends"
    private long mEndTime;

    private Recurrence.IRecurrence mRecurrence;
    
    public Recurrence.IRecurrence getRecurrence() { return mRecurrence; }

    private static Log sLog = LogFactory.getLog(Appointment.class);

    public Appointment(Mailbox mbox, UnderlyingData data)
            throws ServiceException {
        super(mbox, data);
        if (mData.type != TYPE_APPOINTMENT) {
            throw new IllegalArgumentException();
        }
    }

    public boolean isRecurring() {
        return (mRecurrence != null);
    }

    
    boolean isTaggable() {
        return true;
    }

    boolean isCopyable() {
        return false;
    }

    boolean isMovable() {
        return false;
    }

    boolean isMutable() {
        return true;
    }

    boolean isIndexed() {
        return true;
    }

    boolean canHaveChildren() {
        return false;
    }

    private TimeZoneMap mTzMap;
    
//    public TimeZoneMap getTimeZoneMap() { return mTzMap; }

    static Appointment create(int id, Folder folder, String tags, String uid,
            ParsedMessage pm, Invite firstInvite) throws ServiceException {

        Mailbox mbox = folder.getMailbox();

        List invites = new ArrayList();
        invites.add(firstInvite);

        UnderlyingData data = new UnderlyingData();

        Recurrence.IRecurrence recur = firstInvite.getRecurrence();
        long startTime, endTime;
        if (recur != null) {
            startTime = recur.getStartTime().getUtcTime();
            endTime = recur.getEndTime().getUtcTime();
        } else {
            startTime = firstInvite.getStartTime().getUtcTime();
            endTime = startTime;
            if (firstInvite.getEndTime() != null) {
                endTime = firstInvite.getEndTime().getUtcTime();
            } else {
                if (firstInvite.getDuration() != null) {
                    endTime = firstInvite.getDuration().addToTime(startTime);
                }
            }
        }

        data.id = id;
        data.type = TYPE_APPOINTMENT;
        data.folderId = folder.getId();
        data.indexId = id;
        data.date = mbox.getOperationTimestamp();
        data.tags = tagsToBitmask(tags);
        data.sender = uid;
        data.metadata = encodeMetadata(uid, startTime, endTime, recur,invites, firstInvite.getTimeZoneMap());
        data.modMetadata = mbox.getOperationChangeID();
        data.modContent = mbox.getOperationChangeID();
        DbMailItem.create(mbox, data);

        Appointment appt = new Appointment(mbox, data);
        appt.createBlob(pm, firstInvite);
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
            mRecurrence = (Recurrence.RecurrenceRule)(firstInv.getRecurrence().clone());
            
            // now, go through the list of invites and find all the exceptions
            for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
                Invite cur = (Invite) iter.next();
                
                if (cur != firstInv) {
                    
                    if (cur.getMethod().equals(Method.REQUEST.getValue())) {
                        
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
                    } else if (cur.getMethod().equals(Method.CANCEL.getValue())) {
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
            startTime = firstInv.getStartTime().getUtcTime();
            endTime = firstInv.getEffectiveEndTime().getUtcTime();
        }
        
        if (mStartTime != startTime || mEndTime != endTime) {
            mStartTime = startTime;
            mEndTime = endTime;
            DbMailItem.updateInAppointmentTable(this);
        }
        return true;
    }

    public static final String FN_APPT_RECURRENCE = "apptRecur";

    Metadata decodeMetadata(String metadata) throws ServiceException {
        Metadata meta = new Metadata(metadata, this);

        int mdVersion = meta.getVersion();
        

        mUid = meta.get(Metadata.FN_UID, null);
        mInvites = new ArrayList();

        ICalTimeZone accountTZ = getMailbox().getAccount().getTimeZone();
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
                mInvites.add(Invite.decodeMetadata(md, this, accountTZ));
            }

            Metadata metaRecur = meta.getMap(FN_APPT_RECURRENCE, true);
            if (metaRecur != null)
                mRecurrence = Recurrence.decodeRule(getMailbox(), metaRecur, mTzMap);
        }
        return meta;
    }

    String encodeMetadata() {
        return encodeMetadata(mUid, mStartTime, mEndTime, mRecurrence, mInvites, mTzMap);
    }
    
    static String encodeMetadata(String uid, long startTime, long endTime, Recurrence.IRecurrence recurrence, 
                                 List /*Invite */ invs, TimeZoneMap tzmap) {
        Metadata meta = new Metadata();

        meta.put(Metadata.FN_TZMAP, tzmap.encodeAsMetadata());
        meta.put(Metadata.FN_UID, uid);
        meta.put(Metadata.FN_APPT_START, startTime);
        meta.put(Metadata.FN_APPT_END, endTime);
        meta.put(Metadata.FN_NUM_COMPONENTS, invs.size());

        int num = 0;
        for (Iterator iter = invs.iterator(); iter.hasNext();) {
            Invite comp = (Invite) iter.next();
            String compName = Metadata.FN_INV + num++;
            meta.put(compName, Invite.encodeMetadata(comp));
        }

        if (recurrence != null) {
            meta.put(FN_APPT_RECURRENCE, recurrence.encodeMetadata());
        }

        return meta.toString();
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    // /**
    // * Expand all the instances for the time period from start to end
    // *
    // * @param start
    // * @param end
    // * @return list of Instances for the specified time period
    // */
    public Collection /* Instance */expandInstances(long start, long end)
            throws ServiceException {
        List /* Instance */instances = new ArrayList();

        if (mRecurrence != null) {
            instances = mRecurrence.expandInstances(this, start, end);
        } else {
            if (getStartTime() < end && getEndTime() > start) {
                instances.add(new Instance(this, new InviteInfo(getDefaultInvite()),getStartTime(), getEndTime(), false));
            }
        }
        return instances;
    }

    public static class Instance implements Comparable {
        private long mStart; // calculated start time of this instance

        private long mEnd; // calculated end time of this instance

        private boolean mIsException; // TRUE if this instance is an exception
                                        // to a recurrance
        // private InviteMessage.Invite mInvite;

        private InviteInfo mInvId;
        
        private int mApptId;

        public Instance(Appointment appt, InviteInfo invInfo, long _start, long _end,
                boolean _exception) 
        {
            mInvId = invInfo;
            mApptId = appt.getId();
            mStart = _start;
            mEnd = _end;
            mIsException = _exception;
        }

        public int compareTo(Object o) {
            Instance other = (Instance) o;

            long toRet = mApptId - other.mApptId;
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
            StringBuffer toRet = new StringBuffer("INST(");
            Date dstart = new Date(mStart);
            Date dend = new Date(mEnd);
            toRet.append(dstart).append(",").append(dend).append(",").append(
                    mIsException);
            toRet.append(",ID=").append(mInvId.getMsgId()).append("-").append(
                    mInvId.getComponentId());
            toRet.append(")");
            return toRet.toString();
        }
        
        public int getAppointmentId() { return mApptId; }
        public int getMailItemId() { return mInvId.getMsgId(); }
        public int getComponentNum() { return mInvId.getComponentId(); }
        public long getStart() { return mStart; }
        public long getEnd() { return mEnd; }
        public boolean isException() { return mIsException; }
        public void setIsException(boolean isException) { mIsException = isException; } 
        public InviteInfo getInviteInfo() { return mInvId; } 
    }

    public void setUid(String uid) {
        mUid = uid;
    }

    public String getUid() {
        return mUid;
    }

    public Invite getInvite(InviteInfo id) throws ServiceException {
        for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
            Invite inv = (Invite)iter.next();
            InviteInfo inf = new InviteInfo(inv);
            if (inf.compareTo(id) == 0) {
                return inv;
            }
        }
        return null;
    }
    
    public Invite getInvite(int invId, int compNum) throws ServiceException {
        for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
            Invite inv = (Invite)iter.next();
            if (inv.getMailItemId() == invId && inv.getComponentNum() == compNum) {
                return inv;
            }
        }
        return null;
    }
    
    public Invite[] getInvites(int invId) throws ServiceException {
        ArrayList toRet = new ArrayList();
        for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
            Invite inv = (Invite)iter.next();
            if (inv.getMailItemId() == invId) {
                toRet.add(inv);
            }
        }
        return (Invite[])toRet.toArray(new Invite[0]);
    }
    
    public int numInvites() {
        return mInvites.size();
    }

    public Invite getInvite(int num) throws ServiceException {
        return (Invite)(mInvites.get(num));
    }
    
    public Invite getDefaultInvite() throws ServiceException {
        for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
            Invite cur = (Invite) iter.next();
            
            if (!cur.hasRecurId()) {
                return cur;
            }
        }
        return null;
    }

    /**
     * A new Invite has come in, take a look at it and see what needs to happen.
     * Maybe we need to send updates out. Maybe we need to modify the
     * Appointment table. Perhaps we need to ride around and around on a large
     * pony screaming "yippie! yippie!"
     * 
     * @param invite
     */
    void processNewInvite(ParsedMessage pm, Invite invite) throws ServiceException {
        String method = invite.getMethod();
        if (method.equals("REQUEST") || method.equals("CANCEL")) {
            processNewInviteRequestOrCancel(pm, invite);
        } else if (method.equals("REPLY")) {
            processNewInviteReply(pm, invite);
        }
    }

    void processNewInviteRequestOrCancel(ParsedMessage pm, Invite newInvite)
            throws ServiceException {
        String method = newInvite.getMethod();

        // Remove everyone that is made obselete by this request
        boolean addNewOne = true;
        boolean isCancel = false;
        if (method.equals(Method.CANCEL.getValue())) {
            isCancel = true;
        }

        boolean modifiedAppt = false;
        Invite prev = null; // (the first) invite which has been made obscelete by the new one coming in
        
        ArrayList /* Invite */ toRemove = new ArrayList(); // Invites to remove from our blob store

        ArrayList /* Integer */ idxsToRemove = new ArrayList(); // indexes to remove from mInvites
        
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
                if ((cur.getSeqNo() < newInvite.getSeqNo())
                        // in the CANCEL case, accept one with the exact same
                        // send cancel's with the same DTSTAMP as the request
                        // being cancelled
                        || (
                                (!isCancel && (cur.getSeqNo() == newInvite.getSeqNo() && cur .getDTStamp() < newInvite.getDTStamp())) 
                                || 
                                (isCancel && (cur.getSeqNo() == newInvite.getSeqNo() && cur.getDTStamp() <= newInvite.getDTStamp())))) 
                {
                    Invite inf = (Invite)mInvites.get(i);
                    toRemove.add(inf);
                    
                    // add to FRONT of list, so when we iterate for the removals we go from HIGHER TO LOWER
                    // that way the numbers all match up as the list contracts!
                    idxsToRemove.add(0, new Integer(i));
                    
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
            }
        }

        if (addNewOne) {
            newInvite.setAppointment(this);
            
            if (prev!=null && !newInvite.thisAcctIsOrganizer(getMailbox().getAccount())) {
                if (newInvite.sentByMe()) {
                    // A non-organizer attendee is modifying data on his/her
                    // appointment.  Any information that is tracked in
                    // metadata rather than in the iCal MIME part must be
                    // carried over from the last invite to the new one.
                    newInvite.setPartStat(prev.getPartStat());
                    newInvite.setNeedsReply(prev.needsReply());
                    newInvite.getAppointment().saveMetadata();
                    // No need to mark invite as modified item in mailbox as
                    // it has already been marked as a created item.
                }
            }

            mInvites.add(newInvite);
            
            // the appointment stores an uber-tzmap, for its uber-recurrence
            // this might give us problems if we had two invites with conflicting TZ
            // defs....should be very unlikely
            mTzMap.add(newInvite.getTimeZoneMap());
            
            modifyBlob(toRemove, pm, newInvite);
            modifiedAppt = true;
        } else {
            modifyBlob(toRemove, null, null);
        }
        
        // now remove the inviteid's from our list  
        for (Iterator iter = idxsToRemove.iterator(); iter.hasNext();) {
            Integer i = (Integer)iter.next();
            mInvites.remove(i.intValue());
        }
        
        boolean hasRequests = false;
        for (Iterator iter = mInvites.iterator(); iter.hasNext();) {
            Invite cur = (Invite)iter.next();
            if (cur.getMethod().equals(Method.REQUEST.getValue())) {
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
                    this.saveMetadata();
                }
            }
        }
    }
    
    void setContent(ParsedMessage pm, String digest, int size) throws ServiceException {
        // mark the old blob as ready for deletion
        PendingDelete info = getDeletionInfo();
        info.itemIds.clear();  info.unreadIds.clear();
        mMailbox.markOtherItemDirty(info);

        markItemModified(Change.MODIFIED_CONTENT  | Change.MODIFIED_DATE |
                         Change.MODIFIED_SIZE);
        
        mData.blobDigest = digest;
        mData.date       = mMailbox.getOperationTimestamp();
        mData.modContent = mMailbox.getOperationChangeID();
        mBlob = null;
        
        // rewrite the DB row to reflect our new view
        saveData(pm.getParsedSender().getSortString());
    }
    
    private static class PMDataSource implements DataSource {
        
        private ParsedMessage mPm;
        
        public PMDataSource(ParsedMessage pm) {
            mPm = pm;
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
                return new ByteArrayInputStream(mPm.getRawData());
            } catch (MessagingException e) {
                IOException ioe = new IOException("getInputStream");
                ioe.initCause(e);
                throw ioe;
            }
        }

        public String getName() {
            // TODO should we just return null?
            return mPm.getMessageID();
        }

        public OutputStream getOutputStream() throws IOException {
            throw new UnsupportedOperationException();
        }

    }
    
    private void storeUpdatedBlob(MimeMessage mm) throws ServiceException, MessagingException, IOException
    {
        ParsedMessage pm = new ParsedMessage(mm, true);
        
        byte[] data;
        String digest;
        int size;
        try {
            data = pm.getRawData();  digest = pm.getRawDigest();  size = pm.getRawSize();
        } catch (MessagingException me) {
            throw MailServiceException.MESSAGE_PARSE_ERROR(me);
        }
        
        setContent(pm, digest, size);

        short volumeId = getVolumeId();
        StoreManager sm = StoreManager.getInstance();
        Blob blob = sm.storeIncoming(data, digest, null, volumeId);
        MailboxBlob mb = sm.renameTo(blob, getMailbox(), getId(), getSavedSequence(), volumeId);
    }
    
    
    private void createBlob(ParsedMessage invPm, Invite firstInvite) throws ServiceException 
    {
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
            
            storeUpdatedBlob(mm);
            
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException "+e, e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException "+e, e);
        }
    }
    
    private void modifyBlob(ArrayList /* Invite */ toRemove, ParsedMessage invPm, Invite invite) throws ServiceException
    {
        // TODO - should check to see if the invite's MM is already in here!
        try { 
            // now, make sure the message is in our blob already...
            MimeMessage mm;
            
            try {
                mm = new MimeMessage(getMimeMessage());
            } catch (ServiceException e) {
                // oops, blob isn't there!  old data!  create one
                
                if (invPm != null) {
                    createBlob(invPm, invite);
                }
                return;
            }
            
            // it'll be multipart/digest
            MimeMultipart mmp = (MimeMultipart)mm.getContent();
            
            boolean updated = false;

            // remove invites
            for (Iterator iter = toRemove.iterator(); iter.hasNext();) {
                Invite remInv = (Invite)iter.next();
                
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
                        if (hdrs != null && hdrs.length > 0 && (Integer.parseInt(hdrs[0])==remInv.getMailItemId())) {
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

            if (invPm != null) {
                // add new message
                MimeBodyPart mbp = new MimeBodyPart();
                mbp.setDataHandler(new DataHandler(new PMDataSource(invPm)));
                mmp.addBodyPart(mbp);
                mbp.addHeader("invId", Integer.toString(invite.getMailItemId()));
                updated = true;
            }
            
            if (!updated) {
                return;
            }
            
            // must call this explicitly or else new part won't be added...
            mm.setContent(mmp);

            mm.saveChanges();
            
            storeUpdatedBlob(mm);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException", e);
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException", e);
        }
        
    }
    
    private void processNewInviteReply(ParsedMessage pm, Invite reply)
            throws ServiceException {
        // unique ID: UID+RECURRENCE_ID

        for (int i = 0; i < numInvites(); i++) {
            Invite cur = getInvite(i);

            // UID already matches...next check if RecurId matches
            // if so, then seqNo is next
            // finally use DTStamp
            //
            // See RFC2446: 2.1.5 Message Sequencing
            //
            if (cur.getRecurId() == reply.getRecurId()
                    && cur.getSeqNo() == reply.getSeqNo()
                    && cur.getDTStamp() <= reply.getDTStamp()) {
                // this is the one they're replying to!

                // update the ATTENDEE record in the invite
                cur.updateMatchingAttendees(reply);

                saveMetadata();
                return; // there can only be one match...
            }
        }
    }
    
    public InputStream getRawMessage() throws IOException, ServiceException {
        MailboxBlob msgBlob = getBlob();
        if (msgBlob == null) {
            StringBuffer sb = new StringBuffer("Missing message content: mailbox=");
            sb.append(mMailbox.getId()).append(", msg=").append(mId);
            throw ServiceException.FAILURE(sb.toString(), null);
        }
        return StoreManager.getInstance().getContent(msgBlob);
    }
    
    public void appendRawCalendarData(Calendar cal) throws ServiceException {
        for (Iterator invIter = mInvites.iterator(); invIter.hasNext();) {
            Invite inv = (Invite)invIter.next();
            
            Calendar invCal = getCalendar(inv.getMailItemId());
            
            for (Iterator compIter = invCal.getComponents().iterator(); compIter.hasNext();) {
                Component comp = (Component)compIter.next();
                cal.getComponents().add(comp);
            }
        }
    }
    
    Calendar getCalendar(int invId) throws ServiceException 
    {
        try {
            MimeMessage mm = getMimeMessage(invId);
            
            List parts = Mime.getParts(mm);
            for (int i=0; i<parts.size(); i++) {
                MPartInfo partInfo = (MPartInfo) parts.get(i);
                if (partInfo.getContentType().match(Mime.CT_TEXT_CALENDAR)) {
                    java.io.InputStream is = partInfo.getMimePart().getInputStream();
                    CalendarBuilder builder = new CalendarBuilder();
                    Calendar iCal = builder.build(is);
                    return iCal;
                }
            }
            throw ServiceException.FAILURE("Could not parse detailed iCalendar data for mailbox "+
                    getMailboxId()+" appt "+getId(), null);
        } catch (IOException e) {
            throw ServiceException.FAILURE("reading mime message", e);
        } catch (ParserException e) {
            throw ServiceException.FAILURE("parsing iCalendar", e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("reading mime message", e);
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
                TnefConverter conv = new TnefConverter();
                Mime.accept(conv, mm);
            } catch (Exception e) {
                // If the conversion bombs for any reason, revert to the original
                ZimbraLog.mailbox.info(
                    "Unable to convert TNEF attachment for message " + getId(), e);
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
    
    public MimeMessage getMimeMessage(int subId) throws ServiceException {
        InputStream is = null;
        MimeMessage mm = null;
        try {
            is = getRawMessage();
            mm = new MimeMessage(JMSession.getSession(), is);
            is.close();

            try {
                TnefConverter conv = new TnefConverter();
                Mime.accept(conv, mm);
            } catch (Exception e) {
                // If the conversion bombs for any reason, revert to the original
                ZimbraLog.mailbox.info(
                    "Unable to convert TNEF attachment for message " + getId(), e);
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
                
                if (hdrs != null && hdrs.length > 0 && (Integer.parseInt(hdrs[0])==subId)) {
                    
                    return (MimeMessage)mbp.getContent();
                }
            }
            return null;
        } catch (IOException e) {
            throw ServiceException.FAILURE("IOException while getting MimeMessage for item " + mId, e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("MessagingException while getting MimeMessage for item " + mId, e);
        }
    }
    

};
