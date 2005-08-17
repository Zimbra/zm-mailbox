package com.zimbra.cs.mailbox;

import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.property.Attendee;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.ExRule;
import net.fortuna.ical4j.model.property.Method;
import net.fortuna.ical4j.model.property.Organizer;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.RecurrenceId;
import net.fortuna.ical4j.model.property.Sequence;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.mailbox.calendar.ParsedDuration;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.TimeZoneMap;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.AccountUtil;

/**
 * Invite 
 * 
 * Represents a single component entry of this Invite -- ie a VEvent or a VTodo or whatever
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
            Method method,
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
            Organizer org,
            List attendees,
            String name, 
            String loc,
            int flags,
            String partStat,
            RecurId recurrenceId,
            long dtstamp,
            int seqno,
            int mailboxId,
            int mailItemId,
            int componentNum,
            boolean sentByMe)
            {
        mMethod = method;
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
        mAttendees = attendees;
        mOrganizer = org;
        mName = name;
        mLocation = loc;
        mFlags = flags;
        mPartStat = partStat;
        mRecurrenceId = recurrenceId;
        mDTStamp = dtstamp;
        mSeqNo = seqno;
        
        mMailboxId = mailboxId;
        mMailItemId = mailItemId;
        mComponentNum = componentNum;
        mSentByMe = sentByMe;
            }
    
    private Recurrence.IRecurrence mRecurrence;
    public Recurrence.IRecurrence getRecurrence() { return mRecurrence; }
    private boolean mSentByMe;
    
    
//    
//    public Recurrence.IRecurrence getRecurrence() throws ServiceException
//    {
//        if (!hasDetailsLoaded) {
//            loadDetailedInfo(this.mInvMsg, null);
//        }
//        return mRecurrence;
//    }
    
    private static final String FN_INVMSGID = "mid";
    private static final String FN_SENTBYME = "byme";
    
    static Metadata encodeMetadata(Invite inv) {
        Metadata meta = new Metadata();
        
        meta.put(Metadata.FN_UID, inv.getUid());
        meta.put(FN_INVMSGID, inv.getMailItemId());
        meta.put(FN_COMPNUM, inv.getComponentNum());
        meta.put(FN_SENTBYME, inv.mSentByMe);
        meta.put(Metadata.FN_STATUS, inv.getStatus());
        meta.put(Metadata.FN_APPT_FREEBUSY, inv.getFreeBusy());
        meta.put(Metadata.FN_TRANSP, inv.getTransparency());
        meta.put(Metadata.FN_START, inv.mStart);
        meta.put(Metadata.FN_END, inv.mEnd);
        meta.put(Metadata.FN_DURATION, inv.mDuration);
        meta.put(Metadata.FN_METHOD, inv.mMethod.getValue());
        
        if (inv.mRecurrence != null) {
            meta.put(FN_RECURRENCE, inv.mRecurrence.encodeMetadata());
        }
        
        meta.put(Metadata.FN_NAME, inv.getName());
        
        meta.put(Metadata.FN_LOCATION, inv.mLocation);
        meta.put(Metadata.FN_APPT_FLAGS, inv.getFlags());
        meta.put(Metadata.FN_PARTSTAT, inv.getPartStat());
        
        meta.put(Metadata.FN_TZMAP, inv.mTzMap.encodeAsMetadata());
        
        if (inv.hasRecurId()) {
            meta.put(Metadata.FN_RECUR_ID, inv.getRecurId().encodeMetadata());
        }
        meta.put(Metadata.FN_DTSTAMP, inv.getDTStamp());
        meta.put(Metadata.FN_SEQ_NO, inv.getSeqNo());
        
        meta.put(Metadata.FN_ORGANIZER, encodeAsMetadata(inv.getOrganizer()));
        
        List ats = inv.getAttendees();
        meta.put(Metadata.FN_NUM_ATTENDEES, String.valueOf(ats.size()));
        int i = 0;
        for (Iterator iter = ats.iterator(); iter.hasNext(); i++) {
            Attendee at = (Attendee)iter.next();
            meta.put(Metadata.FN_ATTENDEE + i, encodeAsMetadata(at));
        }
        
        return meta;
    }
    
    public static final Map METHOD_MAP = new HashMap();
    static {
        METHOD_MAP.put(Method.REQUEST.getValue(), Method.REQUEST);
        METHOD_MAP.put(Method.CANCEL.getValue(), Method.CANCEL);
        METHOD_MAP.put(Method.REPLY.getValue(), Method.REPLY);
    }
    public static Method lookupMethod(String methodName) {
        Method toRet = (Method)(METHOD_MAP.get(methodName));
        if (toRet == null) {
            return Method.PUBLISH;
        }
        return toRet;
    }
    
    private static final String FN_RECURRENCE = "recurrence";
    private static final String FN_COMPNUM = "comp"; 
    
    static Invite decodeMetadata(Metadata meta, Appointment appt, ICalTimeZone accountTZ) 
    throws ServiceException {
        String uid = meta.get(Metadata.FN_UID, null);
        int mailItemId = (int)meta.getLong(FN_INVMSGID);
        int componentNum = (int)meta.getLong(FN_COMPNUM);
        String status = meta.get(Metadata.FN_STATUS, IcalXmlStrMap.STATUS_CONFIRMED);
        String freebusy = meta.get(Metadata.FN_APPT_FREEBUSY, IcalXmlStrMap.FBTYPE_BUSY);
        String transp = meta.get(Metadata.FN_TRANSP, IcalXmlStrMap.TRANSP_OPAQUE);
        boolean sentByMe = meta.getBool(FN_SENTBYME);
        
        ParsedDateTime dtStart = null;
        ParsedDateTime dtEnd = null;
        ParsedDuration duration = null;
        
        RecurId recurrenceId = null;
        
        TimeZoneMap tzMap = TimeZoneMap.decodeFromMetadata(meta.getMap(Metadata.FN_TZMAP), accountTZ);
        
        Metadata metaRecur = meta.getMap(FN_RECURRENCE, true);
        Recurrence.IRecurrence recurrence = null; 
        if (metaRecur != null) {
            recurrence = Recurrence.decodeRule(appt.getMailbox(), metaRecur, tzMap);
        }
        
        Method method = lookupMethod(meta.get(Metadata.FN_METHOD, Method.PUBLISH.getValue()));
        
        try {
            // DtStart
            dtStart = ParsedDateTime.parse(meta.get(Metadata.FN_START, null), tzMap);
            // DtEnd
            dtEnd = ParsedDateTime.parse(meta.get(Metadata.FN_END, null), tzMap);
            // Duration
            duration = ParsedDuration.parse(meta.get(Metadata.FN_DURATION, null));
            
            if (meta.containsKey(Metadata.FN_RECUR_ID)) {
                Metadata rdata = meta.getMap(Metadata.FN_RECUR_ID);
                
                recurrenceId = RecurId.decodeMetadata(rdata, tzMap);
            }
            
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Error parsing metadata for invite " + mailItemId+"-"+ componentNum + " in appt " + appt.getId(), e);
        }
        
        String name = meta.get(Metadata.FN_NAME);
        String loc = meta.get(Metadata.FN_LOCATION, null);
        
        int flags = (int) meta.getLong(Metadata.FN_APPT_FLAGS, 0);
        // For existing invites with no partstat, default to ACCEPTED status.
        String partStat = meta.get(Metadata.FN_PARTSTAT, IcalXmlStrMap.PARTSTAT_ACCEPTED);
        long dtstamp = meta.getLong(Metadata.FN_DTSTAMP, 0);
        int seqno = (int) meta.getLong(Metadata.FN_SEQ_NO, 0);
        
        Organizer org = null;
        try {
            org = parseOrgFromMetadata(meta.getMap(Metadata.FN_ORGANIZER, true));
        } catch (ServiceException e) {
            sLog.warn("Problem decoding organizer for appt " + appt.getId() + " invite "+mailItemId+"-" + componentNum);
        }
        
        ArrayList attendees = new ArrayList();
        long numAts = meta.getLong(Metadata.FN_NUM_ATTENDEES, 0);
        for (int i = 0; i < numAts; i++) {
            try {
                Attendee at = parseAtFromMetadata(meta.getMap(Metadata.FN_ATTENDEE + i, true));
                attendees.add(at);
            } catch (ServiceException e) {
                sLog.warn("Problem decoding attendee " + i + " for appointment " + appt.getId() + " invite "+mailItemId+"-" + componentNum);
            }
        }
            
        return new Invite(method, tzMap, appt, uid, status, freebusy, transp,
                dtStart, dtEnd, duration, recurrence, org, attendees,
                name, loc, flags, partStat,
                recurrenceId, dtstamp, seqno,
                appt.getMailboxId(), mailItemId, componentNum, sentByMe);
    }
    
    public boolean needsReply() {
        return ((mFlags & APPT_FLAG_NEEDS_REPLY)!=0);
    }
    
    
    /**
     * WARNING - internal version does NOT save the metadata.  Make sure you know that it is being
     * saved if you call this func.
     * 
     * @param needsReply
     */
    void setNeedsReply(boolean needsReply) {
        if (needsReply) {
            mFlags |= APPT_FLAG_NEEDS_REPLY;
        } else {
            mFlags &= ~APPT_FLAG_NEEDS_REPLY;
        }
    }
    
//    public Message getInviteMessage() { return mInvMsg; }
    
//    void setInviteMessage(Message msg) {
//        mInvMsg = msg;
//    }
    
    /**
     * The public version updates the metadata in the DB as well
     * @param flag -- flag to up
     * @param add TRUE means set bit (OR with value) FALSE means unset bit
     */
    public void modifyFlag(Mailbox mbx, int flag, boolean add) throws ServiceException {
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
                mbx.markItemModified(mAppt, Change.MODIFIED_INVITE);
            }
        }
    }
    
    /**
     * Update this user's attendee participation status.  The
     * APPT_FLAG_NEEDS_REPLY flag is cleared.  Metadata is updated
     * in DB.
     * @param mbx
     * @param partStat "AC" (acceptec), "TE" (tentative), "DE" (declined),
     *                 "DG" (delegated), "CO" (completed),
     *                 "IN" (in-process)
     * @throws ServiceException
     */
    public void modifyPartStat(Mailbox mbx, String partStat)
    throws ServiceException {
        int oldFlags = mFlags;
        setNeedsReply(false);
        if (mFlags != oldFlags && !mPartStat.equals(partStat)) {
            mPartStat = partStat;
            mAppt.saveMetadata();
            if (mbx != null) {
                mbx.markItemModified(mAppt, Change.MODIFIED_INVITE);
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
    
    void setAppointment(Appointment appt) {
        mAppt = appt;
    }
    
    void setIsAllDayEvent(boolean allDayEvent) {
        if (allDayEvent) {
            mFlags |= APPT_FLAG_ALLDAY;
        } else {
            mFlags &= ~APPT_FLAG_ALLDAY;
        }
    }
    public int getComponentNum() { return mComponentNum; }
    void setComponentNum(int num) { mComponentNum = num; }
    void setMailboxId(int id) { mMailboxId = id; }
    void setMailItemId(int id) { mMailItemId = id; }
//    void setCalendar(Calendar cal) { miCal = cal; }
    public int getFlags() { return mFlags; }
    public String getPartStat() { return mPartStat; }
    void setPartStat(String partStat) { mPartStat = partStat; }
    public String getUid() { return mUid; };
    public int getMailboxId() { return mMailboxId; }
    public int getMailItemId() { return mMailItemId; }
    public String getName() { return mName; };
    public String getStatus() { return mStatus; }
    void setStatus(String status) { mStatus = status; }
    public String getFreeBusy() { return mFreeBusy; }
    void setFreeBusy(String fb) { mFreeBusy = fb; }
    public String getTransparency() { return mTransparency; }
    public boolean isTransparent() { return IcalXmlStrMap.TRANSP_TRANSPARENT.equals(mTransparency); }
    void setTransparency(String transparency) { mTransparency = transparency; }
    public RecurId getRecurId() { return mRecurrenceId; }
    public boolean hasRecurId() { return mRecurrenceId != null; }
    public long getDTStamp() { return mDTStamp; }
    public int getSeqNo() { return mSeqNo; }
    public ParsedDateTime getStartTime() { return mStart; }
    public ParsedDateTime getEndTime() { return mEnd; }
    public ParsedDuration getDuration() { return mDuration; }
    
    /**
     * Returns actual free-busy status based on free-busy setting of the
     * event, user's participation status, and the scheduling status of
     * the event.
     * 
     * The getFreeBusy() method simply returns the event's free-busy
     * setting.
     * @return
     */
    public String getFreeBusyActual() {
        assert(mFreeBusy != null);
        
        // If event itself is FBTYPE_FREE, it doesn't matter whether
        // invite was accepted or declined.  It shows up as free time.
        if (IcalXmlStrMap.FBTYPE_FREE.equals(mFreeBusy))
            return IcalXmlStrMap.FBTYPE_FREE;
        
        // If invite was accepted, use event's free-busy status.
        if (IcalXmlStrMap.PARTSTAT_ACCEPTED.equals(mPartStat))
            return mFreeBusy;
        
        // If invite was received but user hasn't acted on it yet
        // (NEEDS_ACTION), or if the user tentatively accepted it,
        // or if the event was only tentatively scheduled rather
        // than confirmed, then he/she is tentatively busy regardless
        // of the free-busy status of the event.  (Unless event specified
        // FBTYPE_FREE, but that case was already taken care of above.
        if (IcalXmlStrMap.PARTSTAT_NEEDS_ACTION.equals(mPartStat) ||
                IcalXmlStrMap.PARTSTAT_TENTATIVE.equals(mPartStat) ||
                IcalXmlStrMap.STATUS_TENTATIVE.equals(mStatus))
            return IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE;
        
        // If invite was declined or delegated to someone else, or if
        // this is a cancelled event, the user is free.
        if (IcalXmlStrMap.PARTSTAT_DECLINED.equals(mPartStat) ||
                IcalXmlStrMap.PARTSTAT_DELEGATED.equals(mPartStat) ||
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
//              if (isAllDayEvent()) {
//              return dur.add(ParsedDuration.DAYS, 1);
//              } 
                return dur;  
            } else {
                return null;
            }
        }
    }
    
    public String getLocation() { return mLocation; }
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
    
    private List /* Attendee */ mAttendees = new ArrayList();
    private Organizer mOrganizer;
    private ArrayList /* VAlarm */ mAlarms = new ArrayList();
    private Method mMethod;

    Invite(Calendar cal, Method method) {
        mMethod = method;
    }
    
    public String getMethod() {
        return mMethod.getValue();
    }
    
    private static Organizer parseOrgFromMetadata(Metadata meta) {
        if (meta == null)
            return null;
        String addressStr = meta.get("a", null);
        return new Organizer(URI.create(addressStr));
    }
    
    private static Attendee parseAtFromMetadata(Metadata meta) throws ServiceException {
        if (meta == null)
            return null;
        String addressStr = meta.get("a", null);
        String roleStr = meta.get("r", null);
        String partStatStr = meta.get(Metadata.FN_PARTSTAT, null);
        Boolean rsvpBool = Boolean.FALSE;
        if (meta.getBool("v", false))
            rsvpBool = Boolean.TRUE;
        
        ParameterList p = new ParameterList();
        
        if (roleStr != null && !roleStr.equals("")) {
            Role role = new Role(IcalXmlStrMap.sRoleMap.toIcal(roleStr));
            p.add(role);
        }
        
        if (partStatStr != null && !partStatStr.equals("")) {
            PartStat partStat = new PartStat(IcalXmlStrMap.sPartStatMap.toIcal(partStatStr));
            p.add(partStat);
        }
        
        Rsvp rsvp = new Rsvp(rsvpBool);
        p.add(rsvp);
        
        return new Attendee(p, URI.create(addressStr));
    }
    
    private static Metadata encodeAsMetadata(Organizer org) {
        Metadata meta = new Metadata();
        meta.put("a", org.getCalAddress());
        return meta;
    }
    
    private static Metadata encodeAsMetadata(Attendee at) {
        Metadata meta = new Metadata();
        ParameterList params = at.getParameters();
        
        // address
        meta.put("a", at.getCalAddress());
        
        // role
        Role role = (Role) params.getParameter(Parameter.ROLE);
        if (role != null)
            meta.put("r", IcalXmlStrMap.sRoleMap.toXml(role.getValue()));
        
        // partstat
        PartStat partStat = (PartStat) params.getParameter(Parameter.PARTSTAT);
        if (partStat != null)
            meta.put(Metadata.FN_PARTSTAT, IcalXmlStrMap.sPartStatMap.toXml(partStat.getValue()));
        
        // rsvp?
        boolean rsvp = false;
        Parameter rsvpParam = params.getParameter(Parameter.RSVP);
        if (rsvpParam != null)
            rsvp = ((Rsvp) rsvpParam).getRsvp().booleanValue();
        if (rsvp)
            meta.put("v", "1");
        
        return meta;
    }
    
    
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
        return AccountUtil.addressMatchesAccount(acct, getOrganizer().getCalAddress().getSchemeSpecificPart());
    }
    
    /**
     * Find the (first) Attendee in our list that matches the passed-in account
     * 
     * @param acct
     * @return The first matching attendee
     * @throws ServiceException
     */
    public Attendee getMatchingAttendee(Account acct) throws ServiceException {
        // Find my ATTENDEE record in the Invite, it must be in our response
        List attendees = getAttendees();
        
        for (Iterator iter = attendees.iterator(); iter.hasNext();) {
            Attendee at = (Attendee)(iter.next());
            
            String thisAtEmail = at.getCalAddress().getSchemeSpecificPart();
            if (AccountUtil.addressMatchesAccount(acct, thisAtEmail)) {
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
    boolean updateMatchingAttendees(Invite other) throws ServiceException {
        // Find my ATTENDEE record in the Invite, it must be in our response
        List attendees = getAttendees();
        
        boolean modified = false;
        
        for (Iterator otherIter = other.getAttendees().iterator(); otherIter.hasNext();) {
            Attendee otherAt = (Attendee)otherIter.next();
            
            for (Iterator iter = attendees.iterator(); iter.hasNext();) {
                Attendee at = (Attendee)(iter.next());
                
                URI lhs = otherAt.getCalAddress();
                URI rhs = at.getCalAddress();
                if (lhs.equals(rhs)) {
                    
                    ParameterList otherParams = otherAt.getParameters();
                    
                    ParameterList atParams = at.getParameters();
                    Parameter p;
                    
                    /////////
                    // update Role if it has changed
                    if ((p=otherParams.getParameter(Parameter.ROLE)) != null) 
                    {
                        Parameter toRemove = atParams.getParameter(Parameter.ROLE);
                        atParams.remove(toRemove);
                        
                        atParams.add(p);
                    }
                    
                    /////////
                    // update RSVP to "no"
                    p = atParams.getParameter(Parameter.RSVP);
                    if (p!= null) {
                        atParams.remove(p);
                    }
                    atParams.add(Rsvp.FALSE);
                    
                    
                    /////////
                    // update PartStat if it has changed
                    p = otherParams.getParameter(Parameter.PARTSTAT);
                    if (p!= null) {
                        Parameter toRemove = atParams.getParameter(Parameter.PARTSTAT);
                        atParams.remove(toRemove);
                        
                        atParams.add(p);
                    }
                    
                    modified = true;
                }
            }
        }
        
        if (modified) {
            mAppt.saveMetadata();
            Mailbox mbx = mAppt.getMailbox();
            if (mbx != null) {
                mbx.markItemModified(mAppt, Change.MODIFIED_INVITE);
            }
            return true;
        } else {
            return false;
        }
    }
    
    // microsoft does the whole all day event "their own way" ie by setting this X- property...
    static final String MICROSOFT_ALL_DAY_EVENT = "X-MICROSOFT-CDO-ALLDAYEVENT";
    
    // how MS Outlook sets free-busy type of a meeting
    public static final String MICROSOFT_BUSYSTATUS = "X-MICROSOFT-CDO-BUSYSTATUS";
    public static final String MICROSOFT_INTENDEDSTATUS = "X-MICROSOFT-CDO-INTENDEDSTATUS";
    
    void parseVEvent(VEvent vevent, TimeZoneMap tzmap,
            Method method,
            Account acct,
            boolean reParsing)
    throws ServiceException 
    {
        mTzMap = tzmap;
        
        try {
            boolean iAmTheOrganizer = false;
            
            // Allowed Sub-Components: VALARM
            ComponentList comps = vevent.getAlarms();
            
            for (Iterator iter = comps.iterator(); iter.hasNext();) {
                Component comp = (Component)iter.next();
                
                if (comp.getName().equals(Component.VALARM)) {
                    VAlarm alarm = (VAlarm) comp;
                    mAlarms.add(alarm);
                }
            }
            
            if (mAlarms.size() > 0) {
                setHasAlarm(true);
            }
            
            // DEFAULT values:
            setStatus(IcalXmlStrMap.STATUS_CONFIRMED);
            setTransparency(IcalXmlStrMap.TRANSP_OPAQUE);
            mName = "";
            mLocation = "";
            
            ArrayList /* Recur */ addRecurs = new ArrayList();
            ArrayList /* Recur */ subRecurs = new ArrayList();
            
            // Calendar Properties
            PropertyList props = vevent.getProperties();
            for (Iterator it = props.iterator(); it.hasNext(); ) {
                Property prop = (Property) it.next();
                String propName = prop.getName();
                if (propName.equals(Property.ORGANIZER)) {
                    if (!reParsing) {
                        net.fortuna.ical4j.model.property.Organizer org = (net.fortuna.ical4j.model.property.Organizer) prop;
                        mOrganizer = org;
                        iAmTheOrganizer = thisAcctIsOrganizer(acct);
                    }
                } else if (propName.equals(Property.ATTENDEE)) {
                    if (!reParsing) {
                        net.fortuna.ical4j.model.property.Attendee attendee = (net.fortuna.ical4j.model.property.Attendee)prop; 
                        mAttendees.add(attendee);
                    }
                } else if (propName.equals(Property.DTSTAMP)) {
                    mDTStamp = ((DtStamp) prop).getDateTime().getTime();
                } else if (propName.equals(Property.RECURRENCE_ID)) {
//                  mRecurrenceId = ((RecurrenceId) prop).getTime().getTime();
                    mRecurrenceId = RecurId.parse((RecurrenceId)prop, tzmap);
                } else if (propName.equals(Property.SEQUENCE)) {
                    mSeqNo = ((Sequence) prop).getSequenceNo();
                } else if (propName.equals(Property.DTSTART)) {
//                  DtStart start = (DtStart)prop;
//                  Parameter param = start.getParameters().getParameter("VALUE");
//                  if (param != null && param.getValue().equals("DATE")) {
//                  setIsAllDayEvent(true);
//                  }
//                  mStart = ((DtStart) prop).getTime().getTime();
                    mStart = ParsedDateTime.parse(prop, tzmap);
                    if (!mStart.hasTime()) {
                        setIsAllDayEvent(true);
                    }
                } else if (propName.equals(Property.DTEND)) {
//                  mEnd = ((DtEnd) prop).getTime().getTime();
                    mEnd = ParsedDateTime.parse(prop, tzmap);
                } else if (propName.equals(Property.DURATION)) {
//                  mDuration = ((Duration) prop).getDuration();
                    mDuration = ParsedDuration.parse(prop);
                } else if (propName.equals(Property.LOCATION)) {
                    mLocation = prop.getValue();
                } else if (propName.equals(Property.SUMMARY)) {
                    mName = prop.getValue();
                } else if (propName.equals(Property.UID)) {
                    mUid = prop.getValue();
                } else if (propName.equals(Property.RRULE)) {
                    setIsRecurrence(true);
                    addRecurs.add(((RRule)prop).getRecur());
                } else if (propName.equals(Property.RDATE)) {
                    setIsRecurrence(true);
                } else if (propName.equals(Property.EXRULE)) {
                    setIsRecurrence(true);
                    subRecurs.add(((ExRule)prop).getRecur());
                } else if (propName.equals(Property.EXDATE)) {
                    setIsRecurrence(true);
                } else if (propName.equals(Property.STATUS)) {
                    String status = IcalXmlStrMap.sStatusMap.toXml(prop.getValue());
                    if (status != null)
                        setStatus(status);
                } else if (propName.equals(Property.TRANSP)) {
                    String transp = IcalXmlStrMap.sTranspMap.toXml(prop.getValue());
                    if (transp!=null) {
                        setTransparency(transp);
                    }
                } else if (propName.equals(MICROSOFT_ALL_DAY_EVENT)) {
                    if ("TRUE".equals(prop.getValue()))
                        setIsAllDayEvent(true);
                } else if (propName.equals(MICROSOFT_BUSYSTATUS)) {
                    String fb = IcalXmlStrMap.sOutlookFreeBusyMap.toXml(prop.getValue());
                    if (fb != null)
                        setFreeBusy(fb);
                }
            }
            
            ParsedDuration duration = mDuration;
            
            if (duration == null) {
                if (mEnd != null) {
                    duration = mEnd.difference(mStart);
                }
            }
            
            ArrayList addRules = new ArrayList();
            if (addRecurs.size() > 0) {
                for (Iterator iter = addRecurs.iterator(); iter.hasNext();) {
                    Recur cur = (Recur)iter.next();
                    addRules.add(new Recurrence.SimpleRepeatingRule(mStart, duration, cur, new InviteInfo(this)));
                }
            }
            ArrayList subRules = new ArrayList();
            if (subRules.size() > 0) {
                for (Iterator iter = subRules.iterator(); iter.hasNext();) {
                    Recur cur = (Recur)iter.next();
                    addRules.add(new Recurrence.SimpleRepeatingRule(mStart, duration, cur, new InviteInfo(this)));
                }
            }
            
            if (hasRecurId()) {
                if (addRules.size() > 0) { 
                    mRecurrence = new Recurrence.ExceptionRule(getRecurId(),  
                            mStart, duration, new InviteInfo(this), addRules, subRules);
                }
            } else {
                if (addRules.size() > 0) { // since exclusions can't affect DtStart, just ignore them if there are no add rules
                    mRecurrence = new Recurrence.RecurrenceRule(mStart, duration, new InviteInfo(this), addRules, subRules);
                }
            }
            
            if (!reParsing && mAttendees.size() > 1) {
                setHasOtherAttendees(true);
            }
            
            if (iAmTheOrganizer) {
                if (!reParsing) {
                    setPartStat(IcalXmlStrMap.PARTSTAT_ACCEPTED);
                    setNeedsReply(false);
                }
            } else {
                if (!reParsing) {
                    Attendee at = getMatchingAttendee(acct);
                    if (at != null) {
                        PartStat stat = (PartStat)(at.getParameters().getParameter(Parameter.PARTSTAT));
                        if ((stat == null || stat.equals(PartStat.NEEDS_ACTION)) &&
                                (method == Method.REQUEST || method == Method.COUNTER)) {
                            setNeedsReply(true);
                        }
                    } else {
                        // if this is the first time we're parsing this, and we can't find ourself on the
                        // attendee list, then allow a reply...
                        setNeedsReply(true);
                    }
                }
            }
        } catch(ParseException e) {
            throw MailServiceException.ICALENDAR_PARSE_ERROR(vevent.toString(), e);
        }
    }
    
    public List /* Attendee */ getAttendees() {
        return mAttendees;
    }
    
    public Organizer getOrganizer() {
        return mOrganizer;
    }
    
    public String getType() {
        return "event";
    }
    
    TimeZoneMap mTzMap;
    
    public TimeZoneMap getTimeZoneMap() { return mTzMap; }
    
    String detailsToString() {
        StringBuffer sb = new StringBuffer();
        sb.append("details:\n");
        if (mOrganizer != null) {
            sb.append("org:");
            sb.append(mOrganizer.toString());
            sb.append("\n");
        }
        for (Iterator iter = mAttendees.iterator(); iter.hasNext();) {
            Attendee at = (Attendee)iter.next();
            sb.append("ATTENDEE;"+at.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
    
    
    /** This version parses the invites BEFORE the InviteMessage object itself is created -- this is
     *  necessary because of the way the MailItem creation path works.  
     * @param mbx - Mailbox
     * @param cal - iCal4j Calendar object
     * @param mailItemId
     * 
     * @return list of Invites (ie the mComponents list of the to-be-created InviteMessage)
     */
    static List /* Invite */ parseCalendarComponentsForNewMessage(boolean sentByMe, Mailbox mbx, Calendar cal, 
            int mailItemId, TimeZoneMap tzmap) throws ServiceException {
        
        List /* Invite */ toRet = new ArrayList();
        
        //
        // vevent, vtodo: ALARM, props
        // vjournal: props
        // vfreebusy: props
        
        Method method = Method.PUBLISH;
        
        PropertyList props = cal.getProperties();
        for (Iterator iter = props.iterator(); iter.hasNext();) {
            Property prop = (Property)iter.next();
            String name = prop.getName();
            if (name.equals(Property.METHOD)) {
                method = (Method)prop;
                if (method.getValue().equals("REQUEST")) {
                    method = Method.REQUEST;
                } else if (method.getValue().equals("COUNTER")) {
                    method = Method.COUNTER;
                }
            }
        }
        
        ComponentList comps = cal.getComponents();
        int compNum = 0;
        
        for (Iterator iter = comps.iterator(); iter.hasNext();) {
            Component comp = (Component)iter.next();
            
            if (comp.getName().equals(Component.VTIMEZONE)) {
                    tzmap.add((VTimeZone) comp);
            } else if (comp.getName().equals(Component.VEVENT)) {
                Invite invComp = null;
                invComp = new Invite(cal, method);
                toRet.add(invComp);
                
                invComp.setComponentNum(compNum);
                invComp.setMailboxId(mbx.getId());
                invComp.setMailItemId(mailItemId);
                invComp.setSentByMe(sentByMe);

                // must do this AFTER component-num, mailbox-id and mailitem-id are set!
                invComp.parseVEvent((VEvent) comp, tzmap, method, mbx.getAccount(), false);
                compNum++;
            }
        }
        return toRet;
    }
    
    
    
}