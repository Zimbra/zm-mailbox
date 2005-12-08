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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.AccountUtil;

/**
 * Invite
 * 
 * Invite isn't really the right name for this class, it should be called CalendarComponent 
 * or something...
 * 
 * An Invite represents a single component entry of an Appointment -- ie a single VEvent or a VTodo or whatever
 * This is our smallest "chunk" of raw iCal data -- it has a single UUID, etc etc
 */
public class Invite {
    
    static Log sLog = LogFactory.getLog(Invite.class);
    
    /**
     * Constructs an Invite object. This is called when an invite
     * is being retrieved from the database.
     * @param end
     * @param start
     */
    Invite(
            String methodStr,
            TimeZoneMap tzmap,
            Appointment appt,
            String uid,
            String status,
            String freebusy,
            String transp,
            ParsedDateTime start,
            ParsedDateTime end,
            ParsedDuration duration,
            Recurrence.IRecurrence recurrence,
            ZOrganizer org,
            List attendees,
            String name, 
            String comment, 
            String loc,
            int flags,
            String partStat,
            RecurId recurrenceId,
            long dtstamp,
            int seqno,
            int mailboxId,
            int mailItemId,
            int componentNum,
            boolean sentByMe,
            String fragment)
            {
        mMethod = lookupMethod(methodStr);
        mTzMap = tzmap;
        mAppt = appt;
        mUid = uid;
        mStatus = status;
        mFreeBusy = freebusy;
        mTransparency = transp;
        mStart = start;
        mEnd = end;
        mDuration = duration;
        mRecurrence = recurrence;
        mOrganizer = org;
        mAttendees = attendees;
        mName = name != null ? name : "";
        mComment = comment != null ? comment : "";
        mLocation = loc != null ? loc : "";
        mFlags = flags;
        mPartStat = partStat;
        mRecurrenceId = recurrenceId;
        mDTStamp = dtstamp;
        mSeqNo = seqno;
        
        mMailboxId = mailboxId;
        mMailItemId = mailItemId;
        mComponentNum = componentNum;
        mSentByMe = sentByMe;
        mFragment = fragment != null ? fragment : "";
            }
    
    private Recurrence.IRecurrence mRecurrence;
    public Recurrence.IRecurrence getRecurrence() { return mRecurrence; }
    public void setRecurrence(Recurrence.IRecurrence recur) { mRecurrence = recur; setIsRecurrence(mRecurrence != null); }
    private boolean mSentByMe;
    private String mFragment;
    public String getFragment() { return mFragment; }
    public void setFragment(String fragment) { mFragment = fragment; }
    
    /**
     * Create an Invite object which will then be added to a mailbox Mailbox.addInvite()     
     *  
     * @param method usually "REQUEST" or else CANCEL/REPLY/PUBLISH
     * @param tzMap TimeZoneMap which contains every timezone referenced in DtStart, DtEnd, Duration or Recurrence
     * @param uid UID of this appointment
     * @param status IcalXmlStrMap.STATUS_* RFC2445 status: eg TENTATIVE/CONFIRMED/CANCELLED
     * @param freeBusy IcalXmlStrMap.FB* (F/B/T/U -- show time as Free/Busy/Tentative/Unavailable)
     * @param transparency IcalXmlStrMap.TRANSP_* RFC2445 Transparency
     * @param allDayEvent TRUE if this is an all-day-event, FALSE otherwise.  This will override the Time part of DtStart and DtEnd, and will throw an ServiceException.FAILURE if the Duration is not Days or Weeks
     * @param dtStart Start time 
     * @param dtEndOrNull End time OR NULL (duration must be specified if this is null)
     * @param durationOrNull Duration (may not be specified if dtEnd is specified)
     * @param recurID If this invite is an EXCEPTION, the ID of the instance being excepted
     * @param recurrenceOrNull IRecurrence rule tree 
     * @param organizer RFC2445 Organizer: see Invite.createOrganizer
     * @param attendees list of RFC2445 Attendees: see Invite.createAttendee
     * @param name Name of this appointment
     * @param location Location of this appointment
     * @param fragment Description of this appointment
     * @param dtStampOrZero RFC2445 sequencing. If 0, then will use current timestamp
     * @param sequenceNoOrZero RFC2445 sequencying.  If 0, then will use current highest sequence no, or 1
     * @param needsReply TRUE if this mailbox is expected to reply to this invite
     * @param partStat IcalXMLStrMap.PARTSTAT_* RFC2445 Participant Status of this mailbox
     * @param sentByMe TRUE if this mailbox sent this invite 
     */
    public static Invite createInvite(
            int mailboxId,
            String method,
            TimeZoneMap tzMap, 
            String uidOrNull,       
            String status,
            String freeBusy,
            String transparency,
            boolean allDayEvent,
            ParsedDateTime dtStart,
            ParsedDateTime dtEndOrNull,
            ParsedDuration durationOrNull,
            RecurId recurId,
            Recurrence.IRecurrence recurrenceOrNull,
            ZOrganizer organizer,
            List /* ZAttendee */ attendees,
            String name,
            String comment, 
            String location,
            String fragment,
            int dtStampOrZero,
            int sequenceNoOrZero,
            boolean needsReply,
            String partStat,
            boolean sentByMe) throws ServiceException
    {
        return new Invite(
                method,
                tzMap,
                null, // no appointment yet
                uidOrNull,
                status,
                freeBusy,
                transparency,
                dtStart,
                dtEndOrNull,
                durationOrNull,
                recurrenceOrNull,
                organizer,
                attendees,
                name,
                comment,
                location,
                Invite.APPT_FLAG_EVENT | (needsReply ? Invite.APPT_FLAG_NEEDS_REPLY : 0) | (allDayEvent ? Invite.APPT_FLAG_ALLDAY : 0),
                partStat,
                recurId,
                dtStampOrZero,
                sequenceNoOrZero,
                mailboxId,
                0, // mailItemId MUST BE SET
                0, // component num
                sentByMe,
                fragment
        );
        
    }
    
    /**
     * Called by Mailbox.addInvite once it has an ID for this invite
     * 
     * @param invId
     */
    public void setInviteId(int invId) {
        this.mMailItemId = invId;
        if (mRecurrence != null) {
            mRecurrence.setInviteId(new InviteInfo(this));
        }
    }
    

    private static final String FN_ADDRESS         = "a";
    private static final String FN_APPT_FLAGS      = "af";
    private static final String FN_ATTENDEE        = "at";
    private static final String FN_SENTBYME        = "byme";
    private static final String FN_COMPNUM         = "comp";
    private static final String FN_ICAL_COMMENT    = "cmt";
    private static final String FN_FRAGMENT        = "frag";
    private static final String FN_DTSTAMP         = "dts";
    private static final String FN_DURATION        = "duration";
    private static final String FN_END             = "et";
    private static final String FN_APPT_FREEBUSY   = "fb";
    private static final String FN_LOCATION        = "l";
    private static final String FN_INVMSGID        = "mid";
    private static final String FN_METHOD          = "mthd";
    private static final String FN_NAME            = "n";
    private static final String FN_NUM_ATTENDEES   = "numAt";
    private static final String FN_ORGANIZER       = "org";
    private static final String FN_PARTSTAT        = "ptst";
    private static final String FN_RECURRENCE = "recurrence";
    private static final String FN_RECUR_ID        = "rid";
    private static final String FN_SEQ_NO          = "seq";
    private static final String FN_STATUS          = "status";  // calendar: event/todo/journal status
    private static final String FN_START           = "st";
    private static final String FN_TRANSP          = "tr";
    private static final String FN_TZMAP           = "tzm"; // calendaring: timezone map
    private static final String FN_UID             = "u";
        
    
    /**
     * This is only really public to support serializing RedoOps -- you
     * really don't want to call this API from anywhere else 
     * 
     * @param inv
     * @return
     */
    public static Metadata encodeMetadata(Invite inv) {
        Metadata meta = new Metadata();
        
        meta.put(FN_UID, inv.getUid());
        meta.put(FN_INVMSGID, inv.getMailItemId());
        meta.put(FN_COMPNUM, inv.getComponentNum());
        meta.put(FN_SENTBYME, inv.mSentByMe);
        meta.put(FN_STATUS, inv.getStatus());
        meta.put(FN_APPT_FREEBUSY, inv.getFreeBusy());
        meta.put(FN_TRANSP, inv.getTransparency());
        meta.put(FN_START, inv.mStart);
        meta.put(FN_END, inv.mEnd);
        meta.put(FN_DURATION, inv.mDuration);
        meta.put(FN_METHOD, inv.mMethod.toString());
        meta.put(FN_FRAGMENT, inv.mFragment);
        meta.put(FN_ICAL_COMMENT, inv.mComment);
        
        if (inv.mRecurrence != null) {
            meta.put(FN_RECURRENCE, inv.mRecurrence.encodeMetadata());
        }
        
        meta.put(FN_NAME, inv.getName());
        
        meta.put(FN_LOCATION, inv.mLocation);
        meta.put(FN_APPT_FLAGS, inv.getFlags());
        meta.put(FN_PARTSTAT, inv.getPartStat());
        
        meta.put(FN_TZMAP, inv.mTzMap.encodeAsMetadata());
        
        if (inv.hasRecurId()) {
            meta.put(FN_RECUR_ID, inv.getRecurId().encodeMetadata());
        }
        meta.put(FN_DTSTAMP, inv.getDTStamp());
        meta.put(FN_SEQ_NO, inv.getSeqNo());
        
        if (inv.getOrganizer() != null) {
            meta.put(FN_ORGANIZER, inv.getOrganizer().encodeAsMetadata());
        }
        
        List ats = inv.getAttendees();
        meta.put(FN_NUM_ATTENDEES, String.valueOf(ats.size()));
        int i = 0;
        for (Iterator iter = ats.iterator(); iter.hasNext(); i++) {
            ZAttendee at = (ZAttendee)iter.next();
            meta.put(FN_ATTENDEE + i, at.encodeAsMetadata());
        }
        
        return meta;
    }
    
//    public static final Map METHOD_MAP = new HashMap();
//    static {
//        METHOD_MAP.put(Method.REQUEST.getValue(), Method.REQUEST);
//        METHOD_MAP.put(Method.CANCEL.getValue(), Method.CANCEL);
//        METHOD_MAP.put(Method.REPLY.getValue(), Method.REPLY);
//        METHOD_MAP.put(Method.PUBLISH.getValue(), Method.PUBLISH);
//    }
    public static ICalTok lookupMethod(String methodName) {
        ICalTok toRet = ICalTok.valueOf(methodName);
        switch (toRet) {
        case REQUEST:
        case PUBLISH:
        case REPLY:
        case ADD:
        case CANCEL:
        case REFRESH:
        case COUNTER:
        case DECLINECOUNTER:
            return toRet;
        default:
            return ICalTok.PUBLISH;
        }
    }
    
    /**
     * This API is public for RedoLogging to call into it -- you probably don't want to call it from
     * anywhere else! 
     * 
     * @param mailboxId
     * @param meta
     * @param appt
     * @param accountTZ
     * @return
     * @throws ServiceException
     */
    public static Invite decodeMetadata(int mailboxId, Metadata meta, Appointment appt, ICalTimeZone accountTZ) 
    throws ServiceException {
        String uid = meta.get(FN_UID, null);
        int mailItemId = (int)meta.getLong(FN_INVMSGID);
        int componentNum = (int)meta.getLong(FN_COMPNUM);
        String status = meta.get(FN_STATUS, IcalXmlStrMap.STATUS_CONFIRMED);
        String freebusy = meta.get(FN_APPT_FREEBUSY, IcalXmlStrMap.FBTYPE_BUSY);
        String transp = meta.get(FN_TRANSP, IcalXmlStrMap.TRANSP_OPAQUE);
        boolean sentByMe = meta.getBool(FN_SENTBYME);
        String fragment = meta.get(FN_FRAGMENT, "");
        String comment = meta.get(FN_ICAL_COMMENT, "");
        
        ParsedDateTime dtStart = null;
        ParsedDateTime dtEnd = null;
        ParsedDuration duration = null;
        
        RecurId recurrenceId = null;
        
        TimeZoneMap tzMap = TimeZoneMap.decodeFromMetadata(meta.getMap(FN_TZMAP), accountTZ);
        
        Metadata metaRecur = meta.getMap(FN_RECURRENCE, true);
        Recurrence.IRecurrence recurrence = null; 
        if (metaRecur != null) {
            recurrence = Recurrence.decodeRule(metaRecur, tzMap);
        }
        
        String methodStr = meta.get(FN_METHOD, ICalTok.PUBLISH.toString());
        
        try {
            // DtStart
            dtStart = ParsedDateTime.parse(meta.get(FN_START, null), tzMap);
            // DtEnd
            dtEnd = ParsedDateTime.parse(meta.get(FN_END, null), tzMap);
            // Duration
            duration = ParsedDuration.parse(meta.get(FN_DURATION, null));
            
            if (meta.containsKey(FN_RECUR_ID)) {
                Metadata rdata = meta.getMap(FN_RECUR_ID);
                
                recurrenceId = RecurId.decodeMetadata(rdata, tzMap);
            }
            
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Error parsing metadata for invite " + mailItemId+"-"+ componentNum + " in appt " + appt!=null ? Integer.toString(appt.getId()) : "(null)", e);
        }
        
        String name = meta.get(FN_NAME, "");
        String loc = meta.get(FN_LOCATION, null);
        
        int flags = (int) meta.getLong(FN_APPT_FLAGS, 0);
        // For existing invites with no partstat, default to ACCEPTED status.
        String partStat = meta.get(FN_PARTSTAT, IcalXmlStrMap.PARTSTAT_ACCEPTED);
        long dtstamp = meta.getLong(FN_DTSTAMP, 0);
        int seqno = (int) meta.getLong(FN_SEQ_NO, 0);
        
        ZOrganizer org = null;
        try {
            org = ZOrganizer.parseOrgFromMetadata(meta.getMap(FN_ORGANIZER, true));
        } catch (ServiceException e) {
            sLog.warn("Problem decoding organizer for appt " 
                    + appt!=null ? Integer.toString(appt.getId()) : "(null)"
                    + " invite "+mailItemId+"-" + componentNum);
        }
        
        ArrayList attendees = new ArrayList();
        long numAts = meta.getLong(FN_NUM_ATTENDEES, 0);
        for (int i = 0; i < numAts; i++) {
            try {
                ZAttendee at = ZAttendee.parseAtFromMetadata(meta.getMap(FN_ATTENDEE + i, true));
                attendees.add(at);
            } catch (ServiceException e) {
                sLog.warn("Problem decoding attendee " + i + " for appointment " 
                        + appt!=null ? Integer.toString(appt.getId()) : "(null)"
                        + " invite "+mailItemId+"-" + componentNum);
            }
        }
            
        return new Invite(methodStr, tzMap, appt, uid, status, freebusy, transp,
                dtStart, dtEnd, duration, recurrence, org, attendees,
                name, comment, loc, flags, partStat,
                recurrenceId, dtstamp, seqno,
                mailboxId, mailItemId, componentNum, sentByMe, fragment);
    }
    
    public boolean needsReply() {
        return ((mFlags & APPT_FLAG_NEEDS_REPLY)!=0);
    }
    
    
    /**
     * WARNING - does NOT save the metadata.  Make sure you know that it is being
     * saved if you call this func.
     * 
     * @param needsReply
     */
    public void setNeedsReply(boolean needsReply) {
        if (needsReply) {
            mFlags |= APPT_FLAG_NEEDS_REPLY;
        } else {
            mFlags &= ~APPT_FLAG_NEEDS_REPLY;
        }
    }
    
    /**
     * The public version updates the metadata in the DB as well
     * @param flag -- flag to up
     * @param add TRUE means set bit (OR with value) FALSE means unset bit
     */
    void modifyFlag(Mailbox mbx, int flag, boolean add) throws ServiceException {
        boolean changed = false;
        if (add) {
            if ((mFlags & flag) == 0) {
                mFlags |= flag;
                changed = true;
            }
        } else {
            if ((mFlags & flag) != 0) {
                mFlags &= ~flag;
                changed = true;
            }
        }
        
        if (changed) {
            mAppt.saveMetadata();
            if (mbx != null) {
                mAppt.markItemModified(Change.MODIFIED_INVITE);
            }
        }
    } 
    
    /**
     * This API modifies the user's attendee participation status, but only for the
     * in-memory version of the Invite.  No changes are written to the metadata.
     * 
     * Update this user's attendee participation status.  The
     * APPT_FLAG_NEEDS_REPLY flag is cleared.  Metadata is updated
     * in DB.
     * @param mbx
     * @param partStat "AC" (acceptec), "TE" (tentative), "DE" (declined),
     *                 "DG" (delegated), "CO" (completed),
     *                 "IN" (in-process)
     * @throws ServiceException
     */
    public void modifyPartStatInMemory(boolean needsReply, String partStat)
    throws ServiceException {
        int oldFlags = mFlags;
        boolean oldNeedsReply = needsReply();
        setNeedsReply(needsReply);
        if (needsReply() != oldNeedsReply || mFlags != oldFlags || !mPartStat.equals(partStat)) {
            mPartStat = partStat;
        }
    }
    
    public void setPartStat(String partStat) { mPartStat = partStat; }
    
    /**
     * The Invite datastructure caches "my" partstat so that I don't have to search through all
     * of the Attendee records every time I want to know my status....this function updates the
     * catched PartStat data when I receive a new Invite 
     * 
     * @param iAmOrganizer
     */
    public void updateMyPartStat(Account acct) throws ServiceException {
        boolean iAmOrganizer = thisAcctIsOrganizer(acct);
        if (iAmOrganizer) {
            setPartStat(IcalXmlStrMap.PARTSTAT_ACCEPTED);
            setNeedsReply(false);
        } else {
            ZAttendee at = getMatchingAttendee(acct);
            if (at != null) {
                if (at.getPartStat().equals(IcalXmlStrMap.PARTSTAT_NEEDS_ACTION) &&
                        (mMethod == ICalTok.REQUEST || mMethod == ICalTok.COUNTER)) {
                    setNeedsReply(true);
                }
            } else {
                // if this is the first time we're parsing this, and we can't find ourself on the
                // attendee list, then allow a reply...
                setNeedsReply(true);
            }
        }
    }
    
    /**
     * @return the Appointment object, or null if one could not be found
     */
    public Appointment getAppointment() throws ServiceException
    {
        return mAppt;
    }
    
    public void setAppointment(Appointment appt) {
        mAppt = appt;
    }
    
    public void setIsAllDayEvent(boolean allDayEvent) {
        if (allDayEvent) {
            mFlags |= APPT_FLAG_ALLDAY;
        } else {
            mFlags &= ~APPT_FLAG_ALLDAY;
        }
    }
    public int getComponentNum() { return mComponentNum; }
    void setComponentNum(int num) { mComponentNum = num; }
    void setMailboxId(int id) { mMailboxId = id; }
    public void setMailItemId(int id) { mMailItemId = id; }
//    void setCalendar(Calendar cal) { miCal = cal; }
    public int getFlags() { return mFlags; }
    public String getPartStat() { return mPartStat; }
    public String getUid() { return mUid; };
    public void setUid(String uid) { mUid = uid; }
    public int getMailboxId() { return mMailboxId; }
    public int getMailItemId() { return mMailItemId; }
    public String getName() { return mName; };
    public void setName(String name) { mName = name; }
    public String getComment() { return mComment; }
    public void setComment(String comment) { mComment = comment; }
    public String getStatus() { return mStatus; }
    public void setStatus(String status) { mStatus = status; }
    public String getFreeBusy() { return mFreeBusy; }
    public void setFreeBusy(String fb) { mFreeBusy = fb; }
    public String getTransparency() { return mTransparency; }
    public boolean isTransparent() { return IcalXmlStrMap.TRANSP_TRANSPARENT.equals(mTransparency); }
    public void setTransparency(String transparency) { mTransparency = transparency; }
    public RecurId getRecurId() { return mRecurrenceId; }
    public void setRecurId(RecurId rid) { mRecurrenceId = rid; }
    public boolean hasRecurId() { return mRecurrenceId != null; }
    public long getDTStamp() { return mDTStamp; }
    public void setDtStamp(long stamp) { mDTStamp = stamp; }
    public int getSeqNo() { return mSeqNo; }
    public void setSeqNo(int seqNo) { mSeqNo = seqNo; } 
    public ParsedDateTime getStartTime() { return mStart; }
    public void setDtStart(ParsedDateTime dtStart) { mStart = dtStart; }
    public ParsedDateTime getEndTime() { return mEnd; }
    public void setDtEnd(ParsedDateTime dtend) { mEnd = dtend; }
    public ParsedDuration getDuration() { return mDuration; }
    public void setDuration(ParsedDuration dur) { mDuration = dur; }
    
    public String getFreeBusyActual() {
        assert(mFreeBusy != null);
        
        return partStatToFreeBusyActual(mPartStat);
    }
    
    /**
     * Returns actual free-busy status taking into account the free-busy 
     * setting of the event, the user's participation status, and the 
     * scheduling status of the event.
     * 
     * The getFreeBusy() method simply returns the event's free-busy
     * setting.
     * @return
     */
    public String partStatToFreeBusyActual(String partStat) 
    {
        // If event itself is FBTYPE_FREE, it doesn't matter whether
        // invite was accepted or declined.  It shows up as free time.
        if (IcalXmlStrMap.FBTYPE_FREE.equals(mFreeBusy))
            return IcalXmlStrMap.FBTYPE_FREE;
        
        // If invite was accepted, use event's free-busy status.
        if (IcalXmlStrMap.PARTSTAT_ACCEPTED.equals(partStat))
            return mFreeBusy;
        
        // If invite was received but user hasn't acted on it yet
        // (NEEDS_ACTION), or if the user tentatively accepted it,
        // or if the event was only tentatively scheduled rather
        // than confirmed, then he/she is tentatively busy regardless
        // of the free-busy status of the event.  (Unless event specified
        // FBTYPE_FREE, but that case was already taken care of above.
        if (IcalXmlStrMap.PARTSTAT_NEEDS_ACTION.equals(partStat) ||
                IcalXmlStrMap.PARTSTAT_TENTATIVE.equals(partStat) ||
                IcalXmlStrMap.STATUS_TENTATIVE.equals(mStatus))
            return IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE;
        
        // If invite was declined or delegated to someone else, or if
        // this is a cancelled event, the user is free.
        if (IcalXmlStrMap.PARTSTAT_DECLINED.equals(partStat) ||
                IcalXmlStrMap.PARTSTAT_DELEGATED.equals(partStat) ||
                IcalXmlStrMap.STATUS_CANCELLED.equals(mStatus))
            return IcalXmlStrMap.FBTYPE_FREE;
        
        return mFreeBusy;
    }
    
    /**
     * Calculate the "Effective End" of this event: that is, the value of DtEnd if set,
     * or the value of DtStart+Duration if that is set...or alternatively just return
     * the starting time...
     * 
     * @return 
     */
    public ParsedDateTime getEffectiveEndTime() {
        if (mEnd != null) {
            return mEnd;
        } else {
            if (mDuration != null) {
                return mStart.add(mDuration);
            } else {
                return mStart;
            }
        }
    }
    
    /**
     * 
     * Try to calculate the effective "default duration" of this event..this is either the DURATION
     * that was specified, or it is the DtEnd-DtStart of the first instance if they exist -- or, if they
     * don't exist, then this isn't an Event per se and you don't need the answer anyway: so we'll return NULL
     * 
     * @return
     */
    public ParsedDuration getEffectiveDuration() throws ServiceException {
        if (mDuration != null) {
            return mDuration;
        } else {
            if (mEnd != null) {
                ParsedDuration dur = mEnd.difference(mStart); 
                return dur;  
            } else {
                return null;
            }
        }
    }
    
    public String getLocation() { return mLocation; }
    public void setLocation(String location) { mLocation = location; }
    public boolean isAllDayEvent() { return ((mFlags & APPT_FLAG_ALLDAY)!=0); }
    void setHasOtherAttendees(boolean hasOtherAttendees) {
        if (hasOtherAttendees) {
            mFlags |= APPT_FLAG_OTHER_ATTENDEES;
        } else {
            mFlags &= ~APPT_FLAG_OTHER_ATTENDEES;
        }
    }
    public boolean hasOtherAttendees() { return ((mFlags & APPT_FLAG_OTHER_ATTENDEES)!=0); }
    void setIsRecurrence(boolean isRecurrence) {
        if (isRecurrence) {
            mFlags |= APPT_FLAG_ISRECUR;
        } else {
            mFlags &= ~APPT_FLAG_ISRECUR;
        }
    }
    public boolean isRecurrence() { return ((mFlags & APPT_FLAG_ISRECUR)!=0); }
    void setHasAlarm(boolean hasAlarm) {
        if (hasAlarm) {
            mFlags |= APPT_FLAG_HASALARM;
        } else {
            mFlags &= ~APPT_FLAG_HASALARM;
        }
    }
    public boolean hasAlarm() { return ((mFlags & APPT_FLAG_HASALARM)!=0); }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{ ");
        sb.append("mboxid: ").append(this.mMailboxId);
        sb.append(", mailitem: ").append(this.mMailItemId);
        sb.append(", compnum: ").append(this.mComponentNum);
        sb.append(", uid: ").append(this.mUid);
        sb.append(", status: ").append(getStatus());
        sb.append(", partStat: ").append(getPartStat());
        sb.append(", freeBusy: ").append(getFreeBusy());
        sb.append(", transp: ").append(getTransparency());
        sb.append(", start: ").append(this.mStart);
        sb.append(", end: ").append(this.mEnd);
        sb.append(", duration: ").append(this.mDuration);
        sb.append(", name: ").append(this.mName);
        sb.append(", comment: ").append(this.mComment);
        sb.append(", location: ").append(this.mLocation);
        sb.append(", allDay: ").append(isAllDayEvent());
        sb.append(", otherAts: ").append(hasOtherAttendees());
        sb.append(", hasAlarm: ").append(hasAlarm());
        sb.append(", needsReply: ").append(needsReply());
        sb.append(", isRecur: ").append(isRecurrence());
        sb.append(", recurId: ").append(getRecurId());
        sb.append(", DTStamp: ").append(mDTStamp);
        sb.append(", mSeqNo ").append(mSeqNo);
        
        sb.append("}");
        return sb.toString();
    }
    
    public static final int APPT_FLAG_TODO            = 0x01;
    public static final int APPT_FLAG_EVENT           = 0x02;
    public static final int APPT_FLAG_ALLDAY          = 0x04;
    public static final int APPT_FLAG_OTHER_ATTENDEES = 0x08;
    public static final int APPT_FLAG_HASALARM        = 0x10;
    public static final int APPT_FLAG_ISRECUR         = 0x20;
    public static final int APPT_FLAG_NEEDS_REPLY     = 0x40;
    
    protected Appointment mAppt = null;
    
    // all of these are loaded from / stored in the meta
    protected String mUid;
    protected String mStatus = IcalXmlStrMap.STATUS_CONFIRMED;
    protected String mFreeBusy = IcalXmlStrMap.FBTYPE_BUSY;  // (F)ree, (B)usy, (T)entative, (U)navailable
    protected String mTransparency = IcalXmlStrMap.TRANSP_OPAQUE;  // transparent or opaque
    protected ParsedDateTime mStart = null;
    protected ParsedDateTime mEnd = null;
    protected ParsedDuration mDuration = null;
    
    protected String mName; /* name of the invite, aka "subject" */
    protected String mComment;  /* RFC2445 'comment' */ 
    protected String mLocation;
    protected int mFlags = APPT_FLAG_EVENT;
    protected RecurId mRecurrenceId = null; // RECURRENCE_ID
    protected long mDTStamp = 0;
    protected int mSeqNo = 0;
    
    // Participation status for this calendar user.  Values are the
    // 2-character strings in ICalXmlStrMap.sPartStatMap, not the longer
    // iCalendar PARTSTAT values.
    // For meeting organizer, this should always be "AC".  (accepted)
    protected String mPartStat = IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;
    
    // not in metadata:
    protected int mMailboxId = 0;
    protected int mMailItemId = 0;
    protected int mComponentNum = 0;
    
    private List /* ZAttendee */ mAttendees = new ArrayList();
    private ZOrganizer mOrganizer;
    private ArrayList /* VAlarm */ mAlarms = new ArrayList();
//    private Method mMethod;
    private ICalTok mMethod;
    
    public Invite(String method, String fragment, TimeZoneMap tzMap) {
        mMethod = lookupMethod(method);
        mFragment = fragment != null ? fragment : "";
        mTzMap = tzMap;
    }
    
    public Invite(String method, TimeZoneMap tzMap) {
        mMethod = lookupMethod(method);
        mTzMap = tzMap;
        mFragment = "";
    }

    public Invite(TimeZoneMap tzMap) {
        mMethod = ICalTok.PUBLISH;
        mTzMap = tzMap;
        mFragment = "";
    }
    
    
    public String getMethod() { return mMethod.toString(); }
    
    public void setMethod(String methodStr) { mMethod = lookupMethod(methodStr); }
    
    public boolean sentByMe() { return mSentByMe; }
    void setSentByMe(boolean sentByMe) { mSentByMe = sentByMe; }
    
    /**
     * @param acct
     * @return TRUE if this account is the "organizer" of the Event
     * @throws ServiceException
     */
    public boolean thisAcctIsOrganizer(Account acct) throws ServiceException {
        if (getOrganizer() == null) {
            return true; // assume we are...is this right?
        }
        String addr = getOrganizer().getAddress();
        return AccountUtil.addressMatchesAccount(acct, addr);
    }
    
    /**
     * Find the (first) Attendee in our list that matches the passed-in account
     * 
     * @param acct
     * @return The first matching attendee
     * @throws ServiceException
     */
    public ZAttendee getMatchingAttendee(Account acct) throws ServiceException {
        // Find my ATTENDEE record in the Invite, it must be in our response
        List attendees = getAttendees();
        
        for (Iterator iter = attendees.iterator(); iter.hasNext();) {
            ZAttendee at = (ZAttendee)(iter.next());
            
            String thisAtEmail = at.getAddress();
            if (AccountUtil.addressMatchesAccount(acct, thisAtEmail)) {
                return at;
            } 
        }
        return null;
    }
    
    /**
     * Find the (first) Attendee in our list that matches the passed-in name
     * 
     * @param acct
     * @return The first matching attendee
     * @throws ServiceException
     */
    public ZAttendee getMatchingAttendee(String atName) throws ServiceException {
        // Find my ATTENDEE record in the Invite, it must be in our response
        List attendees = getAttendees();
        
        for (Iterator iter = attendees.iterator(); iter.hasNext();) {
            ZAttendee at = (ZAttendee)(iter.next());
            
            String thisAtEmail = at.getAddress();
            if (thisAtEmail.equalsIgnoreCase(atName)) {
                return at;
            }
        }
        return null;
    }
    
    
    
    /**
     * Updates the ATTENDEE entries in this invite which match entries in the other one -- presumably 
     * because the attendee has sent us a reply to change his status.  This function writes the MetaData 
     * through to SQL and also sends a notification of MailItem change.
     * 
     * @param other
     * @return
     * @throws ServiceException
     */
    public boolean updateMatchingAttendees(Invite other) throws ServiceException {
        // Find my ATTENDEE record in the Invite, it must be in our response
        List attendees = getAttendees();
        
        ArrayList /*ZAttendee */ toAdd = new ArrayList();
        
        boolean modified = false;
        
        OUTER: 
            for (Iterator otherIter = other.getAttendees().iterator(); otherIter.hasNext();) {
                ZAttendee otherAt = (ZAttendee)otherIter.next();
                
                for (Iterator iter = attendees.iterator(); iter.hasNext();) {
                    ZAttendee at = (ZAttendee)(iter.next());
                    
                    if (otherAt.addressesMatch(at)) {
                        if (!otherAt.getRole().equals(at.getRole())) {
                            at.setRole(otherAt.getRole());
                            modified = true;
                        }
                        
                        if (!otherAt.getPartStat().equals(at.getPartStat())) {
                            at.setPartStat(otherAt.getPartStat());
                            modified = true;
                        }
                        
                        if (!otherAt.getRsvp().equals(at.getRsvp())) {
                            at.setRsvp(otherAt.getRsvp());
                            modified = true;
                        }
                        continue OUTER;
                    }
                }
                
                toAdd.add(otherAt);
            }
        
        if (toAdd.size() > 0) {
            for (Iterator iter = toAdd.iterator(); iter.hasNext();) {
                modified = true;
                ZAttendee add = (ZAttendee)iter.next();
                attendees.add(add);
            }
        }
        
        if (modified) {
            mAppt.saveMetadata();
            Mailbox mbx = mAppt.getMailbox();
            if (mbx != null) {
                mAppt.markItemModified(Change.MODIFIED_INVITE);
            }
            return true;
        } else {
            return false;
        }
    }

    // Microsoft's proprietary all-day-event implementation uses
    // an X- property.
    static final String MICROSOFT_ALL_DAY_EVENT = "X-MICROSOFT-CDO-ALLDAYEVENT";
    
    // how MS Outlook sets free-busy type of a meeting
    public static final String MICROSOFT_BUSYSTATUS = "X-MICROSOFT-CDO-BUSYSTATUS";
    public static final String MICROSOFT_INTENDEDSTATUS = "X-MICROSOFT-CDO-INTENDEDSTATUS";
    
//    void parseVEvent(VEvent vevent) throws ServiceException 
//    {
//        assert(mTzMap != null);
//        try {
//            // Allowed Sub-Components: VALARM
//            ComponentList comps = vevent.getAlarms();
//            
//            for (Iterator iter = comps.iterator(); iter.hasNext();) {
//                Component comp = (Component)iter.next();
//                
//                if (comp.getName().equals(Component.VALARM)) {
//                    VAlarm alarm = (VAlarm) comp;
//                    mAlarms.add(alarm);
//                }
//            }
//            
//            if (mAlarms.size() > 0) {
//                setHasAlarm(true);
//            }
//            
//            // DEFAULT values:
//            setStatus(IcalXmlStrMap.STATUS_CONFIRMED);
//            setTransparency(IcalXmlStrMap.TRANSP_OPAQUE);
//            mName = "";
//            mLocation = "";
//            
//            ArrayList /* Recur */ addRecurs = new ArrayList();
//            ArrayList /* Recur */ subRecurs = new ArrayList();
//            
//            // Calendar Properties
//            PropertyList props = vevent.getProperties();
//            for (Iterator it = props.iterator(); it.hasNext(); ) {
//                Property prop = (Property) it.next();
//                String propName = prop.getName();
//                if (propName.equals(Property.ORGANIZER)) {
//                    net.fortuna.ical4j.model.property.Organizer org = (net.fortuna.ical4j.model.property.Organizer) prop;
//                    mOrganizer = org;
//                } else if (propName.equals(Property.ATTENDEE)) {
//                    net.fortuna.ical4j.model.property.Attendee attendee = (net.fortuna.ical4j.model.property.Attendee)prop;
//                    mAttendees.add(new ZAttendee(attendee));
//                } else if (propName.equals(Property.DTSTAMP)) {
//                    mDTStamp = ((DtStamp) prop).getDateTime().getTime();
//                } else if (propName.equals(Property.RECURRENCE_ID)) {
//                    mRecurrenceId = RecurId.parse((RecurrenceId)prop, mTzMap);
//                } else if (propName.equals(Property.SEQUENCE)) {
//                    mSeqNo = ((Sequence) prop).getSequenceNo();
//                } else if (propName.equals(Property.DTSTART)) {
//                    mStart = ParsedDateTime.parse(prop, mTzMap);
//                    if (!mStart.hasTime()) {
//                        setIsAllDayEvent(true);
//                    }
//                } else if (propName.equals(Property.DTEND)) {
//                    mEnd = ParsedDateTime.parse(prop, mTzMap);
//                } else if (propName.equals(Property.DURATION)) {
//                    mDuration = ParsedDuration.parse(prop);
//                } else if (propName.equals(Property.LOCATION)) {
//                    mLocation = prop.getValue();
//                } else if (propName.equals(Property.SUMMARY)) {
//                    mName = prop.getValue();
//                } else if (propName.equals(Property.DESCRIPTION)) {
//                    mFragment = prop.getValue();
//                } else if (propName.equals(Property.COMMENT)) {
//                    mComment = prop.getValue();
//                } else if (propName.equals(Property.UID)) {
//                    mUid = prop.getValue();
//                } else if (propName.equals(Property.RRULE)) {
//                    setIsRecurrence(true);
//                    addRecurs.add(((RRule)prop).getRecur());
//                } else if (propName.equals(Property.RDATE)) {
//                    setIsRecurrence(true);
//                    addRecurs.add(prop);
//                } else if (propName.equals(Property.EXRULE)) {
//                    setIsRecurrence(true);
//                    subRecurs.add(((ExRule)prop).getRecur());
//                } else if (propName.equals(Property.EXDATE)) {
//                    setIsRecurrence(true);
//                    subRecurs.add(prop);
//                } else if (propName.equals(Property.STATUS)) {
//                    String status = IcalXmlStrMap.sStatusMap.toXml(prop.getValue());
//                    if (status != null)
//                        setStatus(status);
//                } else if (propName.equals(Property.TRANSP)) {
//                    String transp = IcalXmlStrMap.sTranspMap.toXml(prop.getValue());
//                    if (transp!=null) {
//                        setTransparency(transp);
//                    }
//                } else if (propName.equals(MICROSOFT_ALL_DAY_EVENT)) {
//                    if ("TRUE".equals(prop.getValue()))
//                        setIsAllDayEvent(true);
//                } else if (propName.equals(MICROSOFT_BUSYSTATUS)) {
//                    String fb = IcalXmlStrMap.sOutlookFreeBusyMap.toXml(prop.getValue());
//                    if (fb != null)
//                        setFreeBusy(fb);
//                }
//            }
//            
//            ParsedDuration duration = mDuration;
//            
//            if (duration == null) {
//                if (mEnd != null) {
//                    duration = mEnd.difference(mStart);
//                }
//            }
//            
//            ArrayList /* IInstanceGeneratingRule */ addRules = new ArrayList();
//            if (addRecurs.size() > 0) {
//                for (Iterator iter = addRecurs.iterator(); iter.hasNext();) {
//                    Object next = iter.next();
//                    if (next instanceof Recur) {
//                        ZRecur cur = new ZRecur(((Recur)next).toString(), mTzMap);
//                        addRules.add(new Recurrence.SimpleRepeatingRule(mStart, duration, cur, new InviteInfo(this)));
//                    } else {
//                        RDate cur = (RDate)next;
//                        // TODO add the dates here!
//                    }
//                }
//            }
//            ArrayList /* IInstanceGeneratingRule */  subRules = new ArrayList();
//            if (subRules.size() > 0) {
//                for (Iterator iter = subRules.iterator(); iter.hasNext();) {
//                    Object next = iter.next();
//                    if (next instanceof Recur) {
//                        ZRecur cur = new ZRecur(((Recur)next).toString(), mTzMap);
//                        subRules.add(new Recurrence.SimpleRepeatingRule(mStart, duration, cur, new InviteInfo(this)));
//                    } else {
//                        ExDate cur = (ExDate)next;
//                        // TODO add the dates here!
//                    }
//                }
//            }
//            
//            if (hasRecurId()) {
//                if (addRules.size() > 0) { 
//                    mRecurrence = new Recurrence.ExceptionRule(getRecurId(),  
//                            mStart, duration, new InviteInfo(this), addRules, subRules);
//                }
//            } else {
//                if (addRules.size() > 0) { // since exclusions can't affect DtStart, just ignore them if there are no add rules
//                    mRecurrence = new Recurrence.RecurrenceRule(mStart, duration, new InviteInfo(this), addRules, subRules);
//                }
//            }
//            
//            if (mAttendees.size() > 1) {
//                setHasOtherAttendees(true);
//            }
//        } catch(ParseException e) {
//            throw MailServiceException.ICALENDAR_PARSE_ERROR(vevent.toString(), e);
//        }
//    }
    
    public List /* ZAttendee */ getAttendees() {
        return mAttendees;
    }
    
    public void addAttendee(ZAttendee at) {
        mAttendees.add(at);
    }
    
//    public void setOrganizer(Organizer org) {
//        mOrganizer = org;
//    }
//    
    public void setOrganizer(ZOrganizer org) throws ServiceException {
        mOrganizer = org;
    }
    
    
    public ZOrganizer getOrganizer() {
        return mOrganizer;
    }
    
    public String getType() {
        return "event";
    }
    
    TimeZoneMap mTzMap;
    
    public TimeZoneMap getTimeZoneMap() { return mTzMap; }
    
    
//
//    static public List /* Invite */  createFromICalendar(Account acct, String fragment, Calendar cal, boolean sentByMe) throws ServiceException
//    {
//        // vevent, vtodo: ALARM, props
//        // vjournal: props
//        // vfreebusy: props
//
//        List /* Invite */ toRet = new ArrayList();
//        
//        ICalTok method = ICalTok.PUBLISH;
//        
//        PropertyList props = cal.getProperties();
//        for (Iterator iter = props.iterator(); iter.hasNext();) {
//            Property prop = (Property)iter.next();
//            if (prop.getName().equals(Property.METHOD)) {
//                method = lookupMethod(prop.getValue());
//            }
//        }
//        
//        TimeZoneMap tzmap = new TimeZoneMap(acct.getTimeZone());
//        ComponentList comps = cal.getComponents();
//        
//        for (Iterator iter = comps.iterator(); iter.hasNext();) {
//            Component comp = (Component)iter.next();
//            
//            if (comp.getName().equals(Component.VTIMEZONE)) {
//                tzmap.add((VTimeZone) comp);
//            } else if (comp.getName().equals(Component.VEVENT)) {
//                Invite inv = new Invite(method.toString(), fragment, tzmap);
//                
//                inv.setSentByMe(sentByMe);
//
//                // must do this AFTER component-num, mailbox-id and mailitem-id are set! (because the IRecurrence object needs them)
//                inv.parseVEvent((VEvent) comp);
//                
//                toRet.add(inv);
//            }
//        }
//        
//        if (toRet.size() == 0) {
//            Invite inv = new Invite(method.toString(), fragment, tzmap);
//            toRet.add(inv);
//        }
//
//        return toRet;
//    }
//
////    private static Calendar makeCalendar(String method) {
////        Calendar iCal = new Calendar();
////        // PRODID, VERSION always required
////        iCal.getProperties().add(new ProdId("Zimbra-Calendar-Provider"));
////        iCal.getProperties().add(new Method(method));
////        iCal.getProperties().add(Version.VERSION_2_0);
////
////        return iCal;
////    }
//    
//    public VEvent toVEvent() throws ServiceException {
//        VEvent event = new VEvent();
//        
//        // UID
//        event.getProperties().add(new Uid(getUid()));
//        
//        // RECUR
//        if (mRecurrence != null) {
//            for (Iterator iter = mRecurrence.addRulesIterator(); iter!=null && iter.hasNext();) {
//                IRecurrence cur = (IRecurrence)iter.next();
//
//                switch (cur.getType()) { 
//                case Recurrence.TYPE_SINGLE_INSTANCE:
//                    Recurrence.SingleInstanceRule sir = (Recurrence.SingleInstanceRule)cur;
//                    // FIXME
//                    break;
//                case Recurrence.TYPE_REPEATING:
//                    Recurrence.SimpleRepeatingRule srr = (Recurrence.SimpleRepeatingRule)cur;
//                    try {
//                        event.getProperties().add(new RRule(new Recur(srr.getRecur().toString())));
//                    } catch(ParseException e) {
//                        throw ServiceException.FAILURE("Parsing Recur Rule: "+srr.getRecur().toString(), e);
//                    }
//                    break;
//                }
//                
//            }
//            for (Iterator iter = mRecurrence.subRulesIterator(); iter!=null && iter.hasNext();) {
//                IRecurrence cur = (IRecurrence)iter.next();
//
//                switch (cur.getType()) { 
//                case Recurrence.TYPE_SINGLE_INSTANCE:
//                    Recurrence.SingleInstanceRule sir = (Recurrence.SingleInstanceRule)cur;
//                    // FIXME
//                    break;
//                case Recurrence.TYPE_REPEATING:
//                    Recurrence.SimpleRepeatingRule srr = (Recurrence.SimpleRepeatingRule)cur;
//                    try {
//                        event.getProperties().add(new ExRule(new Recur(srr.getRecur().toString())));
//                    } catch(ParseException e) {
//                        throw ServiceException.FAILURE("Parsing ExRule: "+srr.getRecur().toString(), e);
//                    }
//                        
//                    break;
//                }
//            }
//        }
//        
//        // ORGANIZER
//        if (mOrganizer != null) {
//            event.getProperties().add(mOrganizer);
//        }
//        
//        // allDay
//        if (this.isAllDayEvent()) {
//            XProperty msAllDay = new XProperty("X-MICROSOFT-CDO-ALLDAYEVENT", "TRUE");
//            event.getProperties().add(msAllDay);
//        }
//        
//        // SUMMARY (aka Name or Subject)
//        if (mName != null && !mName.equals("")) {
//            event.getProperties().add(new Summary(mName));
//        }
//        
//        // DESCRIPTION
//        if (mFragment != null && !mFragment.equals("")) {
//            event.getProperties().add(new Description(mFragment));
//        }
//        
//        // COMMENT
//        if (mComment != null && !mComment.equals("")) {
//            event.getProperties().add(new Comment(mComment));
//        }
//        
//        // DTSTART
//        {
//            DtStart dtstart = new DtStart(mStart.iCal4jDate());
//            if (mStart.isUTC()) {
//                dtstart.setUtc(true);
//            } else if (mStart.getTZName() != null) {
//                dtstart.getParameters().add(new TzId(mStart.getTZName()));
//            }
//            event.getProperties().add(dtstart);
//        }
//        
//        // DTEND
//        if (mEnd != null) {
//            DtEnd dtend = new DtEnd(mEnd.iCal4jDate());
//            if (mEnd.isUTC()) {
//                dtend.setUtc(true);
//            } else if (mEnd.getTZName() != null) {
//                dtend.getParameters().add(new TzId(mEnd.getTZName()));
//            }
//            event.getProperties().add(dtend);
//        }
//        
//        // DURATION
//        if (mDuration != null) {
//            Duration dur = new Duration();
//            dur.setValue(mDuration.toString());
//            
//            event.getProperties().add(dur);
//        }
//            
//        
//        // LOCATION
//        if (mLocation != null && !mLocation.equals("")) {
//            event.getProperties().add(new Location(mLocation));
//        }
//        
//        // STATUS
//        event.getProperties().add(new Status(IcalXmlStrMap.sStatusMap.toIcal(mStatus)));
//        
//        // Microsoft Outlook compatibility for free-busy status
//        {
//            String outlookFreeBusy = IcalXmlStrMap.sOutlookFreeBusyMap.toIcal(mFreeBusy);
//            event.getProperties().add(new XProperty(Invite.MICROSOFT_BUSYSTATUS,
//                                                    outlookFreeBusy));
//            event.getProperties().add(new XProperty(Invite.MICROSOFT_INTENDEDSTATUS,
//                                                    outlookFreeBusy));
//        }
//        
//        // TRANSPARENCY
//        event.getProperties().add(new Transp(IcalXmlStrMap.sTranspMap.toIcal(mTransparency)));
//        
//        // ATTENDEEs
//        for (Iterator iter = mAttendees.iterator(); iter.hasNext(); ) {
//            ZAttendee at = (ZAttendee)iter.next();
//            event.getProperties().add(at.iCal4jAttendee());
//        }
//        
//        // RECURRENCE-ID
//        if (mRecurrenceId != null) {
//            RecurrenceId recurId = mRecurrenceId.getRecurrenceId(mTzMap.getLocalTimeZone());
//            event.getProperties().add(recurId);
//        }
//        
//        // DTSTAMP
//        event.getProperties().add(new DtStamp(new DateTime(mDTStamp)));
//        
//        // SEQUENCE
//        event.getProperties().add(new Sequence(mSeqNo));
//        
//        return event;
//    }
    
//    
//    public Calendar toICalendar() throws ServiceException {
//        Calendar toRet = makeCalendar(mMethod.toString());
//        
//        // timezones
//        for (Iterator iter = mTzMap.tzIterator(); iter.hasNext();) {
//            ICalTimeZone cur = (ICalTimeZone) iter.next();
//            VTimeZone vtz = cur.toVTimeZone();
//            toRet.getComponents().add(vtz);
//        }
//        
//        toRet.getComponents().add(toVEvent());
//        
//        if (DebugConfig.validateOutgoingICalendar) {
//            try {
//                toRet.validate(true);
//            } catch (ValidationException e) { 
//                sLog.info("iCal Validation Exception in CreateAppointmentInviteParser", e);
//                if (e.getCause() != null) {
//                    sLog.info("\tcaused by "+e.getCause(), e.getCause());
//                }
//            }
//        }
//
//        sLog.debug("Invite.toICalendar=\n"+toRet.toString());
//        
//        return toRet;
//    }
    
    public ZVCalendar newToICalendar() throws ServiceException {
        ZVCalendar vcal = new ZVCalendar();
        
        vcal.addProperty(new ZProperty(ICalTok.METHOD, mMethod.toString()));
        
        
        // timezones
        ICalTimeZone local = mTzMap.getLocalTimeZone();
        if (!mTzMap.contains(local)) {
            vcal.addComponent(local.newToVTimeZone());
        }
        
        for (Iterator iter = mTzMap.tzIterator(); iter.hasNext();) {
            ICalTimeZone cur = (ICalTimeZone) iter.next();
            vcal.addComponent(cur.newToVTimeZone());
        }
        
        
        vcal.addComponent(newToVEvent());
        return vcal;
    }
    
    
//  /** This version parses the invites BEFORE the InviteMessage object itself is created -- this is
//   *  necessary because of the way the MailItem creation path works.  
//   * @param mbx - Mailbox
//   * @param cal - iCal4j Calendar object
//   * @param mailItemId
//   * 
//   * @return list of Invites (ie the mComponents list of the to-be-created InviteMessage)
//   */
//  public static List /* Invite */ parseCalendarComponentsForNewMessage(boolean sentByMe, Mailbox mbx, Calendar cal, 
//          int mailItemId, String fragment, TimeZoneMap tzmap) throws ServiceException {
//      
//      List /* Invite */ toRet = new ArrayList();
//      
//      //
//      // vevent, vtodo: ALARM, props
//      // vjournal: props
//      // vfreebusy: props
//      
//      ICalTok method = ICalTok.PUBLISH;
//      
//      PropertyList props = cal.getProperties();
//      for (Iterator iter = props.iterator(); iter.hasNext();) {
//          Property prop = (Property)iter.next();
//          if (prop.getName().equals(Property.METHOD)) {
//              method = lookupMethod(prop.getValue());
//          }
//      }
//      
//      ComponentList comps = cal.getComponents();
//      int compNum = 0;
//      
//      for (Iterator iter = comps.iterator(); iter.hasNext();) {
//          Component comp = (Component)iter.next();
//          
//          if (comp.getName().equals(Component.VTIMEZONE)) {
//                  tzmap.add((VTimeZone) comp);
//          } else if (comp.getName().equals(Component.VEVENT)) {
//              Invite invComp = null;
//              invComp = new Invite(method.toString(), fragment, tzmap);
//              toRet.add(invComp);
//              
//              invComp.setComponentNum(compNum);
//              invComp.setMailboxId(mbx.getId());
//              invComp.setMailItemId(mailItemId);
//              invComp.setSentByMe(sentByMe);
//
//              // must do this AFTER component-num, mailbox-id and mailitem-id are set! (because the IRecurrence object needs them)
//              invComp.parseVEvent((VEvent) comp);
//              compNum++;
//          }
//      }
//      return toRet;
//  }
    
    public static List<Invite> createFromCalendar(Account account, String fragment, ZVCalendar cal, boolean sentByMe)
    {
        return createFromCalendar(account, fragment, cal, sentByMe, null, 0);
    }
    
    //     static public List /* Invite */  createFromICalendar(Account acct, String fragment, Calendar cal, boolean sentByMe) throws ServiceException
    public static List<Invite> createFromCalendar(Account account, String fragment, ZVCalendar cal, boolean sentByMe, Mailbox mbx, int mailItemId)
    {
        List <Invite> toRet = new ArrayList();
        
        TimeZoneMap tzmap = new TimeZoneMap(ICalTimeZone.getUTC());
        
        String methodStr = cal.getPropVal(ICalTok.METHOD, ICalTok.PUBLISH.toString());
        
        int compNum = 0;

        // process the TIMEZONE's first: everything depends on them being there...
        for (ZComponent comp : cal.mComponents) {
            switch(comp.getTok()) {
            case VTIMEZONE:
                ICalTimeZone tz = ICalTimeZone.fromVTimeZone(comp);
                tzmap.add(tz);
                break;
                
            }
        }
        
        // now, process the other components (currently, only VEVENT)
        for (ZComponent comp : cal.mComponents) {
            switch(comp.getTok()) {
            case VEVENT:
                try {
                    Invite newInv = new Invite(tzmap);
                    newInv.setMethod(methodStr);
                    
                    toRet.add(newInv);
                    
                    ArrayList addRecurs = new ArrayList();
                    ArrayList subRecurs = new ArrayList();
                    
                    newInv.setComponentNum(compNum);
                    if (mbx != null)
                        newInv.setMailboxId(mbx.getId());
                    newInv.setMailItemId(mailItemId);
                    newInv.setSentByMe(sentByMe);
                    compNum++;
                    
                    for (ZProperty prop : comp.mProperties) {
                        System.out.println(prop);

                        if (prop.mTok == null) 
                            continue;
                        
                        switch (prop.mTok) {
                        case ORGANIZER:
                            newInv.setOrganizer(ZOrganizer.fromProperty(prop));
                            break;
                        case ATTENDEE:
                            newInv.addAttendee(ZAttendee.fromProperty(prop));
                            break;
                        case DTSTAMP:
                            ParsedDateTime dtstamp = ParsedDateTime.parse(prop, tzmap);
                            newInv.setDtStamp(dtstamp.getUtcTime());
                            break;
                        case RECURRENCE_ID:
                            ParsedDateTime rid = ParsedDateTime.parse(prop, tzmap);
                            newInv.setRecurId(new RecurId(rid, RecurId.RANGE_NONE));
                            break;
                        case SEQUENCE:
                            newInv.setSeqNo(prop.getIntValue());
                            break;
                        case DTSTART:
                            ParsedDateTime dtstart = ParsedDateTime.parse(prop, tzmap);
                            newInv.setDtStart(dtstart);
                            break;
                        case DTEND:
                            ParsedDateTime dtend = ParsedDateTime.parse(prop, tzmap);
                            newInv.setDtEnd(dtend);
                            break;
                        case DURATION:
                            ParsedDuration dur = ParsedDuration.parse(prop.getValue());
                            newInv.setDuration(dur);
                            break;
                        case LOCATION:
                            newInv.setLocation(prop.getValue());
                            break;
                        case SUMMARY:
                            newInv.setName(prop.mValue);
                            break;
                        case DESCRIPTION:
                            newInv.setFragment(prop.mValue);
                            break;
                        case COMMENT:
                            newInv.setComment(prop.getValue());
                            break;
                        case UID:
                            newInv.setUid(prop.getValue());
                            break;
                        case RRULE:
                            ZRecur recur = new ZRecur(prop.getValue(), tzmap);
                            addRecurs.add(recur);
                            newInv.setIsRecurrence(true);
                            break;
                        case RDATE:
                            break;
                        case EXRULE:
                            ZRecur exrecur = new ZRecur(prop.getValue(), tzmap);
                            subRecurs.add(exrecur);
                            newInv.setIsRecurrence(true);                            
                            break;
                        case EXDATE:
                            break;
                        case STATUS:
                            String status = IcalXmlStrMap.sStatusMap.toXml(prop.getValue());
                            if (status != null)
                                newInv.setStatus(status);
                            break;
                        case TRANSP:
                            String transp = IcalXmlStrMap.sTranspMap.toXml(prop.getValue());
                            if (transp!=null) {
                                newInv.setTransparency(transp);
                            }
                            break;
                        case X_MICROSOFT_CDO_ALLDAYEVENT:
                            if (prop.getBoolValue()) 
                                newInv.setIsAllDayEvent(true);
                            break;
                        case X_MICROSOFT_CDO_BUSYSTATUS:
                            String fb = IcalXmlStrMap.sOutlookFreeBusyMap.toXml(prop.getValue());
                            if (fb != null)
                                newInv.setFreeBusy(fb);
                            break;
                        }
                    }
                    
                    ParsedDuration duration = newInv.getDuration();
                    
                    if (duration == null) {
                        ParsedDateTime end = newInv.getEndTime();
                        if (end != null) {
                            duration = end.difference(newInv.getStartTime());
                        }
                    }
                    
                    ArrayList /* IInstanceGeneratingRule */ addRules = new ArrayList();
                    if (addRecurs.size() > 0) {
                        for (Iterator iter = addRecurs.iterator(); iter.hasNext();) {
                            Object next = iter.next();
                            if (next instanceof ZRecur) {
                                ZRecur cur = (ZRecur)next;
                                addRules.add(new Recurrence.SimpleRepeatingRule(newInv.getStartTime(), duration, cur, new InviteInfo(newInv)));
                            } else {
//                                RDate cur = (RDate)next;
                                // TODO add the dates here!
                            }
                        }
                    }
                    ArrayList /* IInstanceGeneratingRule */  subRules = new ArrayList();
                    if (subRules.size() > 0) {
                        for (Iterator iter = subRules.iterator(); iter.hasNext();) {
                            Object next = iter.next();
                            if (next instanceof ZRecur) {
                                ZRecur cur = (ZRecur)iter.next();
                                subRules.add(new Recurrence.SimpleRepeatingRule(newInv.getStartTime(), duration, cur, new InviteInfo(newInv)));
                            } else {
//                                ExDate cur = (ExDate)next;
                                // TODO add the dates here!
                            }
                        }
                    }
                    
                    if (newInv.hasRecurId()) {
                        if (addRules.size() > 0) {
                            newInv.setRecurrence(new Recurrence.ExceptionRule(newInv.getRecurId(),  
                                    newInv.getStartTime(), duration, new InviteInfo(newInv), addRules, subRules));
                        }
                    } else {
                        if (addRules.size() > 0) { // since exclusions can't affect DtStart, just ignore them if there are no add rules
                            newInv.setRecurrence(new Recurrence.RecurrenceRule(newInv.getStartTime(), duration, new InviteInfo(newInv), addRules, subRules));
                        }
                    }
                    
                    if (newInv.getAttendees().size() > 1) {
                        newInv.setHasOtherAttendees(true);
                    }
                } catch (ServiceException e) {
                    System.out.println(e);
                    e.printStackTrace();
                } catch (ParseException e) {
                    System.out.println(e);
                    e.printStackTrace();
                }
                
                break;
            }
        }
        
        return toRet;
    }
    
    
    public ZComponent newToVEvent() throws ServiceException
    {
        ZComponent event = new ZComponent(ICalTok.VEVENT);
        
        event.addProperty(new ZProperty(ICalTok.UID, getUid()));
        
        IRecurrence recur = getRecurrence();
        if (recur != null) {
            for (Iterator iter = recur.addRulesIterator(); iter!=null && iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();

                switch (cur.getType()) { 
                case Recurrence.TYPE_SINGLE_INSTANCE:
                    Recurrence.SingleInstanceRule sir = (Recurrence.SingleInstanceRule)cur;
                    // FIXME
                    break;
                case Recurrence.TYPE_REPEATING:
                    Recurrence.SimpleRepeatingRule srr = (Recurrence.SimpleRepeatingRule)cur;
                    event.addProperty(new ZProperty(ICalTok.RRULE, srr.getRecur().toString()));
                    break;
                }
                
            }
            for (Iterator iter = recur.subRulesIterator(); iter!=null && iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();

                switch (cur.getType()) { 
                case Recurrence.TYPE_SINGLE_INSTANCE:
                    Recurrence.SingleInstanceRule sir = (Recurrence.SingleInstanceRule)cur;
                    // FIXME
                    break;
                case Recurrence.TYPE_REPEATING:
                    Recurrence.SimpleRepeatingRule srr = (Recurrence.SimpleRepeatingRule)cur;
                    event.addProperty(new ZProperty(ICalTok.EXRULE, srr.getRecur().toString()));
                    break;
                }
            }
        }
        
        
        // ORGANIZER
        ZOrganizer org = getOrganizer();
        if (org != null)
            event.addProperty(org.toProperty());
        
        // allDay
        if (isAllDayEvent())
            event.addProperty(new ZProperty(ICalTok.X_MICROSOFT_CDO_ALLDAYEVENT, true));
        
        // SUMMARY (aka Name or Subject)
        String name = getName();
        if (name != null && name.length()>0)
            event.addProperty(new ZProperty(ICalTok.SUMMARY, name));
        
        // DESCRIPTION
        String fragment = getFragment();
        if (fragment != null && fragment.length()>0)
            event.addProperty(new ZProperty(ICalTok.DESCRIPTION, fragment));
        
        // COMMENT
        String comment = getComment();
        if (comment != null && comment.length()>0) 
            event.addProperty(new ZProperty(ICalTok.COMMENT, comment));
        
        // DTSTART
        event.addProperty(getStartTime().toProperty(ICalTok.DTSTART));
        
        // DTEND
        ParsedDateTime dtend = getEndTime();
        if (dtend != null) 
            event.addProperty(getEndTime().toProperty(ICalTok.DTEND));
        
        // DURATION
        ParsedDuration dur = getDuration();
        if (dur != null)
            event.addProperty(new ZProperty(ICalTok.DURATION, dur.toString()));
        
        // LOCATION
        String location = getLocation();
        if (location != null)
            event.addProperty(new ZProperty(ICalTok.LOCATION, location.toString()));
        
        // STATUS
        event.addProperty(new ZProperty(ICalTok.STATUS, IcalXmlStrMap.sStatusMap.toIcal(getStatus())));
        
        // Microsoft Outlook compatibility for free-busy status
        {
            String outlookFreeBusy = IcalXmlStrMap.sOutlookFreeBusyMap.toIcal(getFreeBusy());
            event.addProperty(new ZProperty(ICalTok.X_MICROSOFT_CDO_BUSYSTATUS, outlookFreeBusy));
            event.addProperty(new ZProperty(ICalTok.X_MICROSOFT_CDO_INTENDEDSTATUS, outlookFreeBusy));
        }
        
        // TRANSPARENCY
        event.addProperty(new ZProperty(ICalTok.TRANSP, IcalXmlStrMap.sTranspMap.toIcal(getTransparency())));
        
        // ATTENDEES
        for (ZAttendee at : (List<ZAttendee>)getAttendees()) 
            event.addProperty(at.toProperty());
        
        // RECURRENCE-ID
        RecurId recurId = getRecurId();
        if (recurId != null) 
            event.addProperty(recurId.toProperty());
        
        // DTSTAMP
        ParsedDateTime dtStamp = ParsedDateTime.fromUTCTime(getDTStamp());
        event.addProperty(dtStamp.toProperty(ICalTok.DTSTAMP));
        
        // SEQUENCE
        event.addProperty(new ZProperty(ICalTok.SEQUENCE, getSeqNo()));
        
        return event;
    }
}