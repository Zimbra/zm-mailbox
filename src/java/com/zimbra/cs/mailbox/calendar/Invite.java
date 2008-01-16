/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ICalTok;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZComponent;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZProperty;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZVCalendar;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;

/**
 * Invite
 * 
 * Invite isn't really the right name for this class, it should be called CalendarComponent 
 * or something...
 * 
 * An Invite represents a single component entry of an CalendarItem -- ie a single VEvent or a VTodo or whatever
 * This is our smallest "chunk" of raw iCal data -- it has a single UUID, etc etc
 */
public class Invite {

    private static final boolean OUTLOOK_COMPAT_ALLDAY =
        LC.calendar_outlook_compatible_allday_events.booleanValue();
    
    static Log sLog = LogFactory.getLog(Invite.class);
    
    /**
     * Constructs an Invite object. This is called when an invite
     * is being retrieved from the database.
     * @param end
     * @param start
     */
    Invite(
            byte itemType,
            String methodStr,
            TimeZoneMap tzmap,
            CalendarItem calItem,
            String uid,
            String status,
            String priority,
            String pctComplete,
            long completed,
            String freebusy,
            String transp,
            String classProp,
            ParsedDateTime start,
            ParsedDateTime end,
            ParsedDuration duration,
            Recurrence.IRecurrence recurrence,
            boolean isOrganizer,
            ZOrganizer org,
            List<ZAttendee> attendees,
            String name, 
            String comment, 
            String loc,
            int flags,
            String partStat,
            boolean rsvp,
            RecurId recurrenceId,
            long dtstamp,
            int seqno,
            int mailboxId,
            int mailItemId,
            int componentNum,
            boolean sentByMe,
            String description,
            String fragment)
            {
        setItemType(itemType);
        mMethod = lookupMethod(methodStr);
        mTzMap = tzmap;
        mCalItem = calItem;
        mUid = uid;
        mStatus = status;
        mPriority = priority;
        mPercentComplete = pctComplete;
        mCompleted = completed;
        mFreeBusy = freebusy;
        mTransparency = transp;
        mClass = classProp;
        mClassSetByMe = sentByMe;
        mStart = start;
        mEnd = end;
        mDuration = duration;
        mRecurrence = recurrence;
        mIsOrganizer = isOrganizer;
        mOrganizer = org;
        mAttendees = attendees;
        mName = name != null ? name : "";
        mComment = comment != null ? comment : "";
        mLocation = loc != null ? loc : "";
        mFlags = flags;
        mPartStat = partStat;
        mRsvp = rsvp;
        mRecurrenceId = recurrenceId;
        mSeqNo = seqno;
        setDtStamp(dtstamp);

        mMailboxId = mailboxId;
        mMailItemId = mailItemId;
        mComponentNum = componentNum;
        mSentByMe = sentByMe;
        mDescription = description;
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
     * @param uid UID of this calendar item
     * @param status IcalXmlStrMap.STATUS_* RFC2445 status: eg TENTATIVE/CONFIRMED/CANCELLED
     * @param freeBusy IcalXmlStrMap.FB* (F/B/T/U -- show time as Free/Busy/Tentative/Unavailable)
     * @param transparency IcalXmlStrMap.TRANSP_* RFC2445 Transparency
     * @param classProp IcalXmlStrMap.CLASS_*
     * @param allDayEvent TRUE if this is an all-day-event, FALSE otherwise.  This will override the Time part of DtStart and DtEnd, and will throw an ServiceException.FAILURE if the Duration is not Days or Weeks
     * @param dtStart Start time 
     * @param dtEndOrNull End time OR NULL (duration must be specified if this is null)
     * @param durationOrNull Duration (may not be specified if dtEnd is specified)
     * @param recurID If this invite is an EXCEPTION, the ID of the instance being excepted
     * @param recurrenceOrNull IRecurrence rule tree 
     * @param organizer RFC2445 Organizer: see Invite.createOrganizer
     * @param attendees list of RFC2445 Attendees: see Invite.createAttendee
     * @param name Name of this calendar item
     * @param location Location of this calendar item
     * @param description Description of this calendar item
     * @param dtStampOrZero RFC2445 sequencing. If 0, then will use current timestamp
     * @param sequenceNoOrZero RFC2445 sequencying.  If 0, then will use current highest sequence no, or 1
     * @param partStat IcalXMLStrMap.PARTSTAT_* RFC2445 Participant Status of this mailbox
     * @param rsvp RFC2445 RSVP
     * @param sentByMe TRUE if this mailbox sent this invite 
     */
    public static Invite createInvite(
            int mailboxId,
            byte itemType,
            String method,
            TimeZoneMap tzMap, 
            String uidOrNull,
            String status,
            String priority,
            String pctComplete,
            long completed,
            String freeBusy,
            String transparency,
            String classProp,
            boolean allDayEvent,
            ParsedDateTime dtStart,
            ParsedDateTime dtEndOrNull,
            ParsedDuration durationOrNull,
            RecurId recurId,
            Recurrence.IRecurrence recurrenceOrNull,
            boolean isOrganizer,
            ZOrganizer organizer,
            List<ZAttendee> attendees,
            String name,
            String comment, 
            String location,
            String description,
            int dtStampOrZero,
            int sequenceNoOrZero,
            String partStat,
            boolean rsvp,
            boolean sentByMe)
    {
        return new Invite(
                itemType,
                method,
                tzMap,
                null, // no calendar item yet
                uidOrNull,
                status,
                priority,
                pctComplete,
                completed,
                freeBusy,
                transparency,
                classProp,
                dtStart,
                dtEndOrNull,
                durationOrNull,
                recurrenceOrNull,
                isOrganizer,
                organizer,
                attendees,
                name,
                comment,
                location,
                Invite.APPT_FLAG_EVENT | (allDayEvent ? Invite.APPT_FLAG_ALLDAY : 0),
                partStat,
                rsvp,
                recurId,
                dtStampOrZero,
                sequenceNoOrZero,
                mailboxId,
                0, // mailItemId MUST BE SET
                0, // component num
                sentByMe,
                description,
                Fragment.getFragment(description, true)
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


    //private static final String FN_ADDRESS         = "a";
    private static final String FN_ITEMTYPE        = "it";
    private static final String FN_APPT_FLAGS      = "af";
    private static final String FN_ATTENDEE        = "at";
    private static final String FN_SENTBYME        = "byme";
    private static final String FN_CLASS           = "cl";
    private static final String FN_CLASS_SETBYME   = "clSetByMe";
    private static final String FN_COMPLETED       = "completed";
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
    private static final String FN_NUM_XPROPS_OR_XPARAMS = "numX";
    private static final String FN_ORGANIZER       = "org";
    private static final String FN_IS_ORGANIZER    = "isOrg";
    private static final String FN_PARTSTAT        = "ptst";
    private static final String FN_RSVP            = "rsvp";
    private static final String FN_RECURRENCE = "recurrence";
    private static final String FN_RECUR_ID        = "rid";
    private static final String FN_SEQ_NO          = "seq";
    private static final String FN_STATUS          = "status";  // calendar: event/todo/journal status
    private static final String FN_START           = "st";
    private static final String FN_TRANSP          = "tr";
    private static final String FN_TZMAP           = "tzm"; // calendaring: timezone map
    private static final String FN_UID             = "u";
    private static final String FN_PRIORITY        = "prio";
    private static final String FN_PCT_COMPLETE    = "pctcompl";
    private static final String FN_VALUE           = "v";
    private static final String FN_NUM_ALARMS      = "numAl";
    private static final String FN_ALARM           = "al";
    private static final String FN_XPROP_OR_XPARAM = "x";
    private static final String FN_DONT_INDEX_MM   = "noidxmm";

    /**
     * This is only really public to support serializing RedoOps -- you
     * really don't want to call this API from anywhere else 
     * 
     * @param inv
     * @return
     */
    public static Metadata encodeMetadata(Invite inv) {
        Metadata meta = new Metadata();

        meta.put(FN_ITEMTYPE, inv.getItemType());
        meta.put(FN_UID, inv.getUid());
        meta.put(FN_INVMSGID, inv.getMailItemId());
        meta.put(FN_COMPNUM, inv.getComponentNum());
        meta.put(FN_SENTBYME, inv.mSentByMe);
        if (!inv.isPublic())
            meta.put(FN_CLASS, inv.getClassProp());
        meta.put(FN_CLASS_SETBYME, inv.classPropSetByMe());
        meta.put(FN_STATUS, inv.getStatus());
        meta.put(FN_APPT_FREEBUSY, inv.getFreeBusy());
        meta.put(FN_TRANSP, inv.getTransparency());
        meta.put(FN_START, inv.mStart);
        meta.put(FN_END, inv.mEnd);
        if (inv.mCompleted != 0)
            meta.put(FN_COMPLETED, inv.mCompleted);
        meta.put(FN_DURATION, inv.mDuration);
        meta.put(FN_METHOD, inv.mMethod.toString());
        meta.put(FN_FRAGMENT, inv.mFragment);
        // Don't put mDescription in metadata because it may be too big.
        meta.put(FN_ICAL_COMMENT, inv.mComment);
        
        if (inv.mRecurrence != null) {
            meta.put(FN_RECURRENCE, inv.mRecurrence.encodeMetadata());
        }
        
        meta.put(FN_NAME, inv.getName());
        
        meta.put(FN_LOCATION, inv.mLocation);
        meta.put(FN_APPT_FLAGS, inv.getFlags());
        meta.put(FN_PARTSTAT, inv.getPartStat());
        meta.put(FN_RSVP, inv.getRsvp());
        
        meta.put(FN_TZMAP, inv.mTzMap.encodeAsMetadata());
        
        if (inv.hasRecurId()) {
            meta.put(FN_RECUR_ID, inv.getRecurId().encodeMetadata());
        }
        meta.put(FN_DTSTAMP, inv.getDTStamp());
        meta.put(FN_SEQ_NO, inv.getSeqNo());
        
        if (inv.hasOrganizer()) {
            meta.put(FN_ORGANIZER, inv.getOrganizer().encodeMetadata());
        }
        meta.put(FN_IS_ORGANIZER, inv.isOrganizer());

        List<ZAttendee> ats = inv.getAttendees();
        meta.put(FN_NUM_ATTENDEES, String.valueOf(ats.size()));
        int i = 0;
        for (Iterator<ZAttendee> iter = ats.iterator(); iter.hasNext(); i++) {
            ZAttendee at = iter.next();
            meta.put(FN_ATTENDEE + i, at.encodeAsMetadata());
        }

        meta.put(FN_PRIORITY, inv.getPriority());
        meta.put(FN_PCT_COMPLETE, inv.getPercentComplete());

        if (!inv.mAlarms.isEmpty()) {
            meta.put(FN_NUM_ALARMS, inv.mAlarms.size());
            i = 0;
            for (Iterator<Alarm> iter = inv.mAlarms.iterator(); iter.hasNext(); i++) {
                Alarm alarm = iter.next();
                meta.put(FN_ALARM + i, alarm.encodeMetadata());
            }
        }

        if (inv.mXProps.size() > 0)
            encodeXPropsAsMetadata(meta, inv.xpropsIterator());
        
        if (inv.mDontIndexMimeMessage)
            meta.put(FN_DONT_INDEX_MM, true);
        return meta;
    }

    private static void encodeXPropsAsMetadata(Metadata meta,
                                               Iterator<ZProperty> xpropsIter) {
        int xpropCount = 0;
        for (; xpropsIter.hasNext(); ) {
            ZProperty xprop = xpropsIter.next();
            String propName = xprop.getName();
            if (propName == null) continue;
            Metadata propMeta = new Metadata();
            propMeta.put(FN_NAME, propName);
            String propValue = xprop.getValue();
            if (propValue != null)
                propMeta.put(FN_VALUE, propValue);

            int xparamCount = 0;
            for (Iterator<ZParameter> paramIter = xprop.parameterIterator();
                 paramIter.hasNext(); ) {
                ZParameter xparam = paramIter.next();
                String paramName = xparam.getName();
                if (paramName == null) continue;
                Metadata paramMeta = new Metadata();
                paramMeta.put(FN_NAME, paramName);
                String paramValue = xparam.getValue();
                if (paramValue != null)
                    paramMeta.put(FN_VALUE, paramValue);
                propMeta.put(FN_XPROP_OR_XPARAM + xparamCount, paramMeta);
                xparamCount++;
            }
            if (xparamCount > 0)
                propMeta.put(FN_NUM_XPROPS_OR_XPARAMS, xparamCount);

            meta.put(FN_XPROP_OR_XPARAM + xpropCount, propMeta);
            xpropCount++;
        }
        if (xpropCount > 0)
            meta.put(FN_NUM_XPROPS_OR_XPARAMS, xpropCount);
    }

    public static ICalTok lookupMethod(String methodName) {
        ICalTok toRet;
        String methodNameUpper = methodName.toUpperCase();  // work around livemeeting.com bug
        try {
            toRet = ICalTok.valueOf(methodNameUpper);
        } catch (IllegalArgumentException e) {
            toRet = ICalTok.PUBLISH;
            // Apple iCal generates non-standard "METHOD:EXPORT".
            if (methodNameUpper.compareToIgnoreCase("EXPORT") != 0)
                sLog.warn("Invalid METHOD " + methodName +
                          "; assuming PUBLISH", e);
        }
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
     * @param calItem
     * @param accountTZ
     * @return
     * @throws ServiceException
     */
    public static Invite decodeMetadata(int mailboxId, Metadata meta, CalendarItem calItem, ICalTimeZone accountTZ) 
    throws ServiceException {
        byte itemType = (byte) meta.getLong(FN_ITEMTYPE, MailItem.TYPE_APPOINTMENT);
        String uid = meta.get(FN_UID, null);
        int mailItemId = (int)meta.getLong(FN_INVMSGID);
        int componentNum = (int)meta.getLong(FN_COMPNUM);
        String classProp = meta.get(FN_CLASS, IcalXmlStrMap.CLASS_PUBLIC);
        boolean classPropSetByMe = meta.getBool(FN_CLASS_SETBYME, false);
        String status = meta.get(FN_STATUS, IcalXmlStrMap.STATUS_CONFIRMED);
        String freebusy = meta.get(FN_APPT_FREEBUSY, IcalXmlStrMap.FBTYPE_BUSY);
        String transp = meta.get(FN_TRANSP, IcalXmlStrMap.TRANSP_OPAQUE);
        boolean sentByMe = meta.getBool(FN_SENTBYME);
        String fragment = meta.get(FN_FRAGMENT, "");
        // Metadata never contains mDescription because it can be too big.
        String comment = meta.get(FN_ICAL_COMMENT, "");
        long completed = meta.getLong(FN_COMPLETED, 0);

        ParsedDateTime dtStart = null;
        ParsedDateTime dtEnd = null;
        ParsedDuration duration = null;
        
        RecurId recurrenceId = null;
        
        TimeZoneMap tzMap = TimeZoneMap.decodeFromMetadata(meta.getMap(FN_TZMAP), accountTZ);
        
        Metadata metaRecur = meta.getMap(FN_RECURRENCE, true);
        Recurrence.IRecurrence recurrence = null; 
        if (metaRecur != null) {
            recurrence = Recurrence.decodeMetadata(metaRecur, tzMap);
        }

        String methodStr = meta.get(FN_METHOD, ICalTok.PUBLISH.toString());
        if (ICalTok.CANCEL.toString().equals(methodStr))
            status = IcalXmlStrMap.STATUS_CANCELLED;
        
        int flags = (int) meta.getLong(FN_APPT_FLAGS, 0);
        try {
            // DtStart
            dtStart = ParsedDateTime.parse(meta.get(FN_START, null), tzMap);
            // DtEnd
            dtEnd = ParsedDateTime.parse(meta.get(FN_END, null), tzMap);
            if ((flags & APPT_FLAG_ALLDAY)!=0) {
                // Fixup historic data with incorrect all-day start/end format.
                if (dtStart != null)
                    dtStart.forceDateOnly();
                if (dtEnd != null)
                    dtEnd.forceDateOnly();
            }
            // Duration
            duration = ParsedDuration.parse(meta.get(FN_DURATION, null));
            
            if (meta.containsKey(FN_RECUR_ID)) {
                Metadata rdata = meta.getMap(FN_RECUR_ID);
                
                recurrenceId = RecurId.decodeMetadata(rdata, tzMap);
            }
            
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Error parsing metadata for invite " + mailItemId+"-"+ componentNum + " in calItem " + calItem!=null ? Integer.toString(calItem.getId()) : "(null)", e);
        }
        
        String name = meta.get(FN_NAME, "");
        String loc = meta.get(FN_LOCATION, null);
        
        // For existing invites with no partstat, default to ACCEPTED status.
        String partStat = meta.get(FN_PARTSTAT, IcalXmlStrMap.PARTSTAT_ACCEPTED);
        // For existing invites with no RSVP, default to true.
        boolean rsvp = meta.getBool(FN_RSVP, true);
        long dtstamp = meta.getLong(FN_DTSTAMP, 0);
        int seqno = (int) meta.getLong(FN_SEQ_NO, 0);

        ZOrganizer org = null;
        try {
            Metadata metaOrg = meta.getMap(FN_ORGANIZER, true);
            org = metaOrg != null ? new ZOrganizer(metaOrg) : null;
        } catch (ServiceException e) {
            sLog.warn("Problem decoding organizer for calItem " 
                    + calItem!=null ? Integer.toString(calItem.getId()) : "(null)"
                    + " invite "+mailItemId+"-" + componentNum);
        }

        long numAts = meta.getLong(FN_NUM_ATTENDEES, 0);
        ArrayList<ZAttendee> attendees = new ArrayList<ZAttendee>((int) numAts);
        for (int i = 0; i < numAts; i++) {
            try {
                Metadata metaAttendee = meta.getMap(FN_ATTENDEE + i, true);
                if (metaAttendee != null)
                    attendees.add(new ZAttendee(metaAttendee));
            } catch (ServiceException e) {
                ZimbraLog.calendar.warn("Problem decoding attendee " + i + " for calendar item " 
                        + calItem!=null ? Integer.toString(calItem.getId()) : "(null)"
                        + " invite "+mailItemId+"-" + componentNum);
            }
        }

        boolean isOrganizer = false;
        if (meta.containsKey(FN_IS_ORGANIZER)) {
            isOrganizer = meta.getBool(FN_IS_ORGANIZER);
        } else {
            // backward compat for invites created before FN_IS_ORGANIZER was introduced
            if (org != null) {
                String orgAddr = org.getAddress();
                Account account = MailboxManager.getInstance().getMailboxById(mailboxId).getAccount();
                isOrganizer = AccountUtil.addressMatchesAccount(account, orgAddr);
            } else {
                // If there are other attendees, it's an Outlook POP/IMAP bug.  If not,
                // it's a properly formatted single-user event.  See isOrganizer()
                // method for more info.
                isOrganizer = numAts < 1;
            }
        }

        String priority = meta.get(FN_PRIORITY, null);
        String pctComplete = meta.get(FN_PCT_COMPLETE, null);

        Invite invite = new Invite(itemType, methodStr, tzMap, calItem, uid, status,
                priority, pctComplete, completed, freebusy, transp, classProp,
                dtStart, dtEnd, duration, recurrence, isOrganizer, org, attendees,
                name, comment, loc, flags, partStat, rsvp,
                recurrenceId, dtstamp, seqno,
                mailboxId, mailItemId, componentNum, sentByMe, null, fragment);

        invite.setClassPropSetByMe(classPropSetByMe);

        long numAlarms = meta.getLong(FN_NUM_ALARMS, 0);
        for (int i = 0; i < numAlarms; i++) {
            try {
                Metadata metaAlarm = meta.getMap(FN_ALARM + i, true);
                if (metaAlarm != null) {
                    Alarm alarm = Alarm.decodeMetadata(metaAlarm);
                    if (alarm != null)
                        invite.addAlarm(alarm);
                }
            } catch (ServiceException e) {
                ZimbraLog.calendar.warn("Problem decoding alarm " + i + " for calendar item " 
                        + calItem!=null ? Integer.toString(calItem.getId()) : "(null)"
                        + " invite "+mailItemId+"-" + componentNum, e);
            }
        }

        List<ZProperty> xprops = decodeXPropsFromMetadata(meta);
        if (xprops != null) {
            for (ZProperty xprop : xprops) {
                invite.addXProp(xprop);
            }
        }
        
        invite.setDontIndexMimeMessage(meta.getBool(FN_DONT_INDEX_MM, false));

        return invite;
    }

    private static List<ZProperty> decodeXPropsFromMetadata(Metadata meta)
    throws ServiceException {
        int xpropCount = (int) meta.getLong(FN_NUM_XPROPS_OR_XPARAMS, 0);
        if (xpropCount > 0) {
            List<ZProperty> list = new ArrayList<ZProperty>(xpropCount);
            for (int propNum = 0; propNum < xpropCount; propNum++) {
                Metadata propMeta = meta.getMap(FN_XPROP_OR_XPARAM + propNum, true);
                if (propMeta == null) continue;
                String propName = propMeta.get(FN_NAME, null);
                if (propName == null) continue;
                ZProperty xprop = new ZProperty(propName);
                String propValue = propMeta.get(FN_VALUE, null);
                if (propValue != null)
                    xprop.setValue(propValue);
                int xparamCount = (int) propMeta.getLong(FN_NUM_XPROPS_OR_XPARAMS, 0);
                if (xparamCount > 0) {
                    for (int paramNum = 0; paramNum < xparamCount; paramNum++) {
                        Metadata paramMeta = propMeta.getMap(FN_XPROP_OR_XPARAM + paramNum, true);
                        if (paramMeta == null) continue;
                        String paramName = paramMeta.get(FN_NAME, null);
                        if (paramName == null) continue;
                        String paramValue = paramMeta.get(FN_VALUE, null);
                        ZParameter xparam = new ZParameter(paramName, paramValue);
                        xprop.addParameter(xparam);
                    }
                }
                list.add(xprop);
            }
            return list;
        } else
            return null;
    }


    private static final int CACHED_DESC_MAXLEN = 1024;
    private String mDescription;
    
    /**
     * An optimization for indexing, if this is set then we don't need to try to fetch 
     * this Invite's MimeMessage in order to reindex it.
     * 
     * This is TRUE if the Invite had no accompanying MimeMessage when it was stored,
     * e.g. if it came from a REST-imported ICS file. 
     */
    private boolean mDontIndexMimeMessage = false;
    
    public synchronized void setDontIndexMimeMessage(boolean truthiness) { mDontIndexMimeMessage = truthiness; }
    public synchronized boolean getDontIndexMimeMessage() { return mDontIndexMimeMessage; }

    /**
     * Returns the meeting notes.  Meeting notes is the text/plain part in an
     * invite.  It typically includes CUA-generated meeting summary as well as
     * text entered by the user.
     *
     * This method can be called for existing invites only.
     * Null is returned if this method is called on an incoming invite that
     * hasn't been committed to the backend yet.
     *
     * @return null if notes is not found
     * @throws ServiceException
     */
    public synchronized String getDescription() throws ServiceException {
        if (mDescription != null) return mDescription;
        if (mCalItem == null) return null;
        MimeMessage mmInv = mCalItem.getSubpartMessage(mMailItemId);
        String desc = getDescription(mmInv);
        if (desc != null && desc.length() <= CACHED_DESC_MAXLEN)
            mDescription = desc;
        return desc;
    }

    public synchronized void setDescription(String desc) {
        mDescription = desc;
    }

    /**
     * Returns the meeting notes.  Meeting notes is the text/plain part in an
     * invite.  It typically includes CUA-generated meeting summary as well as
     * text entered by the user.
     *
     * @return null if notes is not found
     * @throws ServiceException
     */
    public static String getDescription(MimeMessage mmInv) throws ServiceException {
        if (mmInv == null) return null;
        try {
            // If top-level is text/icalendar, parse the iCalendar object and return
            // the DESCRIPTION of the first VEVENT/VTODO encountered.
            String mmCtStr = mmInv.getContentType();
            if (mmCtStr != null) {
                ContentType mmCt = new ContentType(mmCtStr);
                if (mmCt.match(Mime.CT_TEXT_CALENDAR)) {
                    Object mmInvContent = mmInv.getContent();
                    Reader reader = null;
                    try {
                        if (mmInvContent instanceof InputStream) {
                            String charset = mmCt.getParameter(Mime.P_CHARSET);
                            if (charset == null)
                                charset = Mime.P_CHARSET_UTF8;
                            reader = new InputStreamReader((InputStream) mmInvContent, charset);
                        } else if (mmInvContent instanceof String)
                            reader = new StringReader((String) mmInvContent);
                        if (reader != null) {
                            ZVCalendar iCal = ZCalendarBuilder.build(reader);
                            for (Iterator<ZComponent> compIter = iCal.getComponentIterator(); compIter.hasNext(); ) {
                                ZComponent component = compIter.next();
                                ICalTok compTypeTok = component.getTok();
                                if (compTypeTok == ICalTok.VEVENT || compTypeTok == ICalTok.VTODO) {
                                    for (Iterator<ZProperty> propIter = component.getPropertyIterator(); propIter.hasNext(); ) {
                                        ZProperty prop = propIter.next();
                                        if (prop.getToken() == ICalTok.DESCRIPTION) {
                                            return prop.getValue();
                                        }
                                    }
                                }
                            }
                        }
                    } finally {
                        if (reader != null)
                            reader.close();
                    }
                }
            }

            Object mmInvContent = mmInv.getContent();
            if (!(mmInvContent instanceof MimeMultipart))
                return null;

            // If top-level is multipart, get description from text/plain part.
            MimeMultipart mm = (MimeMultipart) mmInvContent;
            int numParts = mm.getCount();
            BodyPart textPlain = null;
            String charset = null;
            for (int i  = 0; i < numParts; i++) {
                BodyPart part = mm.getBodyPart(i);
                ContentType ct = new ContentType(part.getContentType());
                if (ct.match(Mime.CT_TEXT_PLAIN)) {
                    textPlain = part;
                    charset = ct.getParameter(Mime.P_CHARSET);
                    if (charset == null) charset = Mime.P_CHARSET_DEFAULT;
                    break;
                }
            }
            if (textPlain == null) return null;

            byte[] descBytes = ByteUtil.getContent(textPlain.getInputStream(), textPlain.getSize());
            return new String(descBytes, charset);
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to get calendar item notes MIME part", e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Unable to get calendar item notes MIME part", e);
        }
    }

    /**
     * Returns the MimeMessage that corresponds to this invite.  This method
     * should be called only for invites that have been created on the server
     * already.
     * @return can return null
     */
    public MimeMessage getMimeMessage() throws ServiceException {
        if (mCalItem == null || mMailItemId <= 0) return null;
        return mCalItem.getSubpartMessage(mMailItemId);
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
            mCalItem.saveMetadata();
            if (mbx != null) {
                mCalItem.markItemModified(Change.MODIFIED_INVITE);
            }
        }
    } 
    
    public void setPartStat(String partStat) { mPartStat = partStat; }
    
    /**
     * The Invite datastructure caches "my" partstat so that I don't have to search through all
     * of the Attendee records every time I want to know my status....this function updates the
     * catched PartStat data when I receive a new Invite 
     */
    public void updateMyPartStat(Account acct, String partStat)
    throws ServiceException {
        if (mIsOrganizer) {
            setPartStat(IcalXmlStrMap.PARTSTAT_ACCEPTED);
            setRsvp(false);
        } else {
            ZAttendee at = getMatchingAttendee(acct);
            if (at != null) {
            	setRsvp(at.hasRsvp() ? at.getRsvp().booleanValue() : false);
            	//
            	// for BUG 4866 -- basically, if the incoming invite doesn't have a
            	// PARTSTAT for us, assume it is "NEEDS-ACTION" iff this Invite supports
            	// NEEDS_ACTION (ie is a Request or a Counter)
            	//
            	if (mMethod == ICalTok.REQUEST || mMethod == ICalTok.COUNTER) {
                    setPartStat(partStat);
                    at.setPartStat(partStat);
            	}
            } else {
                // if this is the first time we're parsing this, and we can't find ourself on the
                // attendee list, then allow a reply...
                setRsvp(true);
            }
        }
    }
    
    /**
     * @return the CalendarItem object, or null if one could not be found
     */
    public CalendarItem getCalendarItem() throws ServiceException
    {
        return mCalItem;
    }
    
    public void setCalendarItem(CalendarItem calItem) {
        mCalItem = calItem;
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
    public int getFlags() { return mFlags; }
    public void setFlags(int flags) { mFlags = flags; }
    public String getPartStat() { return mPartStat; }
    public boolean getRsvp() { return mRsvp; }
    public void setRsvp(boolean rsvp) { mRsvp = rsvp; }
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
    public String getClassProp() { return mClass; }
    public void setClassProp(String classProp) { mClass = classProp; }
    public boolean classPropSetByMe() { return mClassSetByMe; }
    public void setClassPropSetByMe(boolean b) { mClassSetByMe = b; }
    public RecurId getRecurId() { return mRecurrenceId; }
    public void setRecurId(RecurId rid) { mRecurrenceId = rid; }
    public boolean hasRecurId() { return mRecurrenceId != null; }
    public long getCompleted() { return mCompleted; }
    public void setCompleted(long completed) { mCompleted = completed; }
    public int getSeqNo() { return mSeqNo; }
    public void setSeqNo(int seqNo) { mSeqNo = seqNo; } 
    public ParsedDateTime getStartTime() { return mStart; }
    public void setDtStart(ParsedDateTime dtStart) { mStart = dtStart; }
    public ParsedDateTime getEndTime() { return mEnd; }
    public void setDtEnd(ParsedDateTime dtend) { mEnd = dtend; }
    public ParsedDuration getDuration() { return mDuration; }
    public void setDuration(ParsedDuration dur) { mDuration = dur; }
    public String getPriority() { return mPriority; }
    public void setPriority(String prio) { mPriority = prio; }
    public String getPercentComplete() { return mPercentComplete; }
    public void setPercentComplete(String pct) { mPercentComplete = pct; }

    public long getDTStamp() { return mDTStamp; }
    public void setDtStamp(long stamp) {
        mDTStamp = stamp / 1000 * 1000;  // IMPORTANT: Remove millis resolution. (bug 20641)
    }

    public boolean isPublic() {
        return IcalXmlStrMap.CLASS_PUBLIC.equals(mClass);
    }

    public boolean isCancel() {
        return ICalTok.CANCEL.toString().equals(mMethod) ||
               IcalXmlStrMap.STATUS_CANCELLED.equals(mStatus);
    }

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
            if (mEnd != null && mStart != null) {
                ParsedDuration dur = mEnd.difference(mStart); 
                return dur;  
            } else {
                return null;
            }
        }
    }

    public String getEffectivePartStat() throws ServiceException {
        if (mCalItem == null) return getPartStat();
        Instance inst = Instance.fromInvite(mCalItem, this);
        return mCalItem.getEffectivePartStat(this, inst);
    }

    public String getLocation() { return mLocation; }
    public void setLocation(String location) { mLocation = location; }
    public boolean isAllDayEvent() { return ((mFlags & APPT_FLAG_ALLDAY)!=0); }
    public boolean hasOrganizer() { return mOrganizer != null; }
    public boolean hasOtherAttendees() {
    	return ((mAttendees != null) && (mAttendees.size() > 0));
    }
    void setIsRecurrence(boolean isRecurrence) {
        if (isRecurrence) {
            mFlags |= APPT_FLAG_ISRECUR;
        } else {
            mFlags &= ~APPT_FLAG_ISRECUR;
        }
    }
    public boolean isRecurrence() { return ((mFlags & APPT_FLAG_ISRECUR)!=0); }

    public boolean hasAlarm() {
        return !mAlarms.isEmpty();
    }
    
    public boolean hasAttachment() { return ((mFlags & APPT_FLAG_HAS_ATTACHMENT)!=0); }
    public void setHasAttachment(boolean hasAttachment) {
        if (hasAttachment) {
            mFlags |= APPT_FLAG_HAS_ATTACHMENT;
        } else {
            mFlags &= ~APPT_FLAG_HAS_ATTACHMENT;
        }
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{ ");
        sb.append("mboxid: ").append(this.mMailboxId);
        sb.append(", mailitem: ").append(this.mMailItemId);
        sb.append(", compnum: ").append(this.mComponentNum);
        sb.append(", uid: ").append(this.mUid);
        sb.append(", status: ").append(getStatus());
        sb.append(", partStat: ").append(getPartStat());
        sb.append(", rsvp: ").append(getRsvp());
        sb.append(", freeBusy: ").append(getFreeBusy());
        sb.append(", transp: ").append(getTransparency());
        sb.append(", class: ").append(getClassProp());
        sb.append(", classSetByMe: ").append(classPropSetByMe());
        sb.append(", sentByMe: ").append(sentByMe());
        sb.append(", start: ").append(this.mStart);
        sb.append(", end: ").append(this.mEnd);
        sb.append(", duration: ").append(this.mDuration);
        sb.append(", organizer: ");
        if (hasOrganizer())
            sb.append(getOrganizer().getAddress());
        else
            sb.append("(not specified)");
        sb.append(", name: ").append(this.mName);
        sb.append(", comment: ").append(this.mComment);
        sb.append(", location: ").append(this.mLocation);
        sb.append(", allDay: ").append(isAllDayEvent());
        sb.append(", otherAts: ").append(hasOtherAttendees());
        sb.append(", hasAlarm: ").append(hasAlarm());
        sb.append(", isRecur: ").append(isRecurrence());
        sb.append(", recurId: ").append(getRecurId());
        sb.append(", DTStamp: ").append(mDTStamp);
        sb.append(", mSeqNo ").append(mSeqNo);

        for (Alarm alarm : mAlarms) {
            sb.append(", alarm: ").append(alarm.toString());
        }

        for (ZProperty xprop : mXProps) {
            sb.append(", ").append(xprop.toString());
        }
        
        sb.append("}");
        return sb.toString();
    }
    
    public static final int APPT_FLAG_TODO            = 0x01;
    public static final int APPT_FLAG_EVENT           = 0x02;
    public static final int APPT_FLAG_ALLDAY          = 0x04;
    
    // TIM: removed this, wasn't being reliably set, and isn't necessary
    //  [instead we just check for mAttendees.size()>0 ]
    //public static final int APPT_FLAG_OTHER_ATTENDEES = 0x08;
    
    public static final int APPT_FLAG_HASALARM        = 0x10;  // obsolete
    public static final int APPT_FLAG_ISRECUR         = 0x20;
    public static final int APPT_FLAG_NEEDS_REPLY     = 0x40;  // obsolete
    public static final int APPT_FLAG_HAS_ATTACHMENT  = 0x80;  // obsolete
    
    protected CalendarItem mCalItem = null;
    
    // all of these are loaded from / stored in the meta
    protected String mUid;
    protected String mStatus = IcalXmlStrMap.STATUS_CONFIRMED;
    protected String mFreeBusy = IcalXmlStrMap.FBTYPE_BUSY;  // (F)ree, (B)usy, (T)entative, (U)navailable
    protected String mTransparency = IcalXmlStrMap.TRANSP_OPAQUE;  // transparent or opaque
    protected String mClass = IcalXmlStrMap.CLASS_PUBLIC;  // public, private, confidential
    protected boolean mClassSetByMe;
    protected ParsedDateTime mStart = null;
    protected ParsedDateTime mEnd = null;
    protected ParsedDuration mDuration = null;
    protected long mCompleted = 0;  // COMPLETED DATE-TIME of VTODO
    
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

    protected boolean mRsvp = false;

    // not in metadata:
    protected int mMailboxId = 0;
    protected int mMailItemId = 0;
    protected int mComponentNum = 0;

    private List<ZAttendee> mAttendees = new ArrayList<ZAttendee>();
    private ZOrganizer mOrganizer;
    private boolean mIsOrganizer;

    private String mPriority;         // 0 .. 9
    private String mPercentComplete;  // 0 .. 100

    // MailItem type of calendar item containing this invite
    private byte mItemType = MailItem.TYPE_APPOINTMENT;

    private ICalTok mMethod;

    private List<Alarm> mAlarms = new ArrayList<Alarm>();

    private List<ZProperty> mXProps = new ArrayList<ZProperty>();

    public Invite(String method, TimeZoneMap tzMap, boolean isOrganizer) {
        setItemType(MailItem.TYPE_APPOINTMENT);
        mMethod = lookupMethod(method);
        if (ICalTok.CANCEL.equals(mMethod))
            mStatus = IcalXmlStrMap.STATUS_CANCELLED;
        mTzMap = tzMap;
        mIsOrganizer = isOrganizer;
        mFragment = "";
    }

    public Invite(byte itemType, String method, TimeZoneMap tzMap, boolean isOrganizer) {
        setItemType(itemType);
        mMethod = lookupMethod(method);
        if (ICalTok.CANCEL.equals(mMethod))
            mStatus = IcalXmlStrMap.STATUS_CANCELLED;
        mTzMap = tzMap;
        mIsOrganizer = isOrganizer;
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
    private boolean thisAcctIsOrganizer(Account acct) throws ServiceException {
        if (hasOrganizer()) {
            String addr = getOrganizer().getAddress();
            return AccountUtil.addressMatchesAccount(acct, addr);
        } else {
            // If there are other attendees, it's an Outlook POP/IMAP bug.  If not,
            // it's a properly formatted single-user event.  See isOrganizer()
            // method for more info.
            return !hasOtherAttendees();
        }
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
        List<ZAttendee> attendees = getAttendees();
        for (ZAttendee at : attendees) {
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
        List<ZAttendee> attendees = getAttendees();
        for (ZAttendee at : attendees) {
            String thisAtEmail = at.getAddress();
            if (thisAtEmail != null && thisAtEmail.equalsIgnoreCase(atName)) {
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
        List<ZAttendee> attendees = getAttendees();
        
        ArrayList<ZAttendee> toAdd = new ArrayList<ZAttendee>();
        
        boolean modified = false;
        
        OUTER: 
            for (ZAttendee otherAt : other.getAttendees()) {
                for (ZAttendee at : attendees) {
                    if (otherAt.addressesMatch(at)) {
                    	
                    	// BUG:4911  When an invitee responds they include an ATTENDEE record, but
                    	// it doesn't have to have all fields.  In particular, we don't want to let them
                    	// change their ROLE...
//                        if (otherAt.hasRole() && !otherAt.getRole().equals(at.getRole())) {
//                            at.setRole(otherAt.getRole());
//                            modified = true;
//                        }
                        
                        if (otherAt.hasPartStat() && !otherAt.getPartStat().equals(at.getPartStat())) {
                            at.setPartStat(otherAt.getPartStat());
                            modified = true;
                        }
                        
                        if (otherAt.hasRsvp() && !otherAt.getRsvp().equals(at.getRsvp())) {
                            at.setRsvp(otherAt.getRsvp());
                            modified = true;
                        }
                        continue OUTER;
                    }
                }
                
                toAdd.add(otherAt);
            }
        
        if (toAdd.size() > 0) {
            for (ZAttendee add : toAdd) {
                modified = true;
                attendees.add(add);
            }
        }
        
        if (modified) {
            mCalItem.saveMetadata();
            Mailbox mbx = mCalItem.getMailbox();
            if (mbx != null) {
                mCalItem.markItemModified(Change.MODIFIED_INVITE);
            }
            return true;
        } else {
            return false;
        }
    }

    public List<ZAttendee> getAttendees() {
        return mAttendees;
    }
    
    public void clearAttendees() {
    	mAttendees.clear();
    }
    
    public void addAttendee(ZAttendee at) {
        mAttendees.add(at);
    }

    public void setOrganizer(ZOrganizer org) throws ServiceException {
        mOrganizer = org;
    }
    
    
    public ZOrganizer getOrganizer() {
        // Be careful!  Don't assume this is non-null.
        return mOrganizer;
    }

    /**
     * Returns Account object for the invite's organizer.
     * @return null if organizer info is missing or organizer is not an
     *         internal user
     * @throws ServiceException
     */
    public Account getOrganizerAccount() throws ServiceException {
        Account account = null;
        if (mIsOrganizer && mCalItem != null)
            return mCalItem.getAccount();
        if (hasOrganizer()) {
            String address = getOrganizer().getAddress();
            if (address != null) {
                account = Provisioning.getInstance().get(AccountBy.name, address);
            }
        } else if (mCalItem != null) {
            account = mCalItem.getAccount();
        }
        return account;
    }

    /**
     * Returns whether the invite was created for the organizer account.
     * @return
     */
    public boolean isOrganizer() {
        return mIsOrganizer;
    }
    public void setIsOrganizer(Account acct) throws ServiceException {
        mIsOrganizer = thisAcctIsOrganizer(acct);
    }

    public boolean isEvent()  { return mItemType == MailItem.TYPE_APPOINTMENT; }
    public boolean isTodo()   { return mItemType == MailItem.TYPE_TASK; }
    public byte getItemType() { return mItemType; }
    public void setItemType(byte t) {
        mItemType = t;
        // If mStatus is set to default appointment status but we have a task
        // invite, change to default task status.
        if (mItemType == MailItem.TYPE_TASK && IcalXmlStrMap.STATUS_CONFIRMED.equals(mStatus))
            mStatus = IcalXmlStrMap.STATUS_NEEDS_ACTION;
    }

    private TimeZoneMap mTzMap;
    
    public TimeZoneMap getTimeZoneMap() { return mTzMap; }

    public ZVCalendar newToICalendar(boolean includePrivateData) throws ServiceException {
        return newToICalendar(OUTLOOK_COMPAT_ALLDAY, includePrivateData);
    }

    public ZVCalendar newToICalendar(boolean useOutlookCompatMode, boolean includePrivateData)
    throws ServiceException {
        ZVCalendar vcal = new ZVCalendar();
        
        vcal.addProperty(new ZProperty(ICalTok.METHOD, mMethod.toString()));
        
        
        // timezones
        ICalTimeZone local = mTzMap.getLocalTimeZone();
        if (!mTzMap.contains(local)) {
            vcal.addComponent(local.newToVTimeZone());
        }
        
        for (Iterator<ICalTimeZone> iter = mTzMap.tzIterator(); iter.hasNext();) {
            ICalTimeZone cur = (ICalTimeZone) iter.next();
            vcal.addComponent(cur.newToVTimeZone());
        }
        
        
        vcal.addComponent(newToVComponent(useOutlookCompatMode, includePrivateData));
        return vcal;
    }

    public static interface InviteVisitor {
        public void visit(Invite inv) throws ServiceException;
    }

    public static List<Invite> createFromCalendar(Account account, String fragment, ZVCalendar cal, boolean sentByMe)
    throws ServiceException {
        return createFromCalendar(account, fragment, cal, sentByMe, null, 0);
    }

    public static List<Invite> createFromCalendar(
            Account account, String fragment, ZVCalendar cal, boolean sentByMe, Mailbox mbx, int mailItemId)
    throws ServiceException {
        List<Invite> list = new ArrayList<Invite>();
        createFromCalendar(list, account, fragment, cal, sentByMe, mbx, mailItemId, false, null);
        return list;
    }

    public static List<Invite> createFromCalendar(
            Account account, String fragment, List<ZVCalendar> cals, boolean sentByMe)
    throws ServiceException {
        return createFromCalendar(account, fragment, cals, sentByMe, false, null);
    }

    public static List<Invite> createFromCalendar(
            Account account, String fragment, List<ZVCalendar> cals, boolean sentByMe,
            boolean continueOnError, InviteVisitor visitor)
    throws ServiceException {
        List<Invite> list = new ArrayList<Invite>();
        for (ZVCalendar cal : cals) {
            createFromCalendar(list, account, fragment, cal, sentByMe, null, 0,
                               continueOnError, visitor);
        }
        return list;
    }

    private static void createFromCalendar(
            List<Invite> toAdd, Account account, String fragment, ZVCalendar cal, boolean sentByMe,
            Mailbox mbx, int mailItemId,
            boolean continueOnError, InviteVisitor visitor)
    throws ServiceException {
        TimeZoneMap tzmap = new TimeZoneMap(ICalTimeZone.getAccountTimeZone(account));
        
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
        
        // now, process the other components (currently, only VEVENT and VTODO)
        for (ZComponent comp : cal.mComponents) {
            Invite newInv = null;
            try {
                byte type;
                ICalTok compTypeTok = comp.getTok();
                if (ICalTok.VTODO.equals(compTypeTok))
                    type = MailItem.TYPE_TASK;
                else
                    type = MailItem.TYPE_APPOINTMENT;
    
                switch (compTypeTok) {
                case VEVENT:
                case VTODO:
                    boolean isEvent = ICalTok.VEVENT.equals(compTypeTok);
                    boolean isTodo = ICalTok.VTODO.equals(compTypeTok);
                    try {
                        newInv = new Invite(type, methodStr, tzmap, false);
    
                        toAdd.add(newInv);
    
                        List<Object> addRecurs = new ArrayList<Object>();
                        List<Object> subRecurs = new ArrayList<Object>();
    
                        newInv.setComponentNum(compNum);
                        if (mbx != null)
                            newInv.setMailboxId(mbx.getId());
                        newInv.setMailItemId(mailItemId);
                        newInv.setSentByMe(sentByMe);
                        compNum++;
    
                        for (ZComponent subcomp : comp.mComponents) {
                            ICalTok subCompTypeTok = subcomp.getTok();
                            switch (subCompTypeTok) {
                            case VALARM:
                                Alarm alarm = Alarm.parse(subcomp);
                                if (alarm != null)
                                    newInv.addAlarm(alarm);
                                break;
                            default:
                                // ignore all other sub components
                            }
                        }
    
                        for (ZProperty prop : comp.mProperties) {
                            if (prop.mTok == null) {
                                String name = prop.getName();
                                if (name.startsWith("X-")) {
                                    newInv.addXProp(prop);
                                }
                                continue;
                            }
    
                            switch (prop.mTok) {
                            case ORGANIZER:
                                newInv.setOrganizer(new ZOrganizer(prop));
                                break;
                            case ATTENDEE:
                                newInv.addAttendee(new ZAttendee(prop));
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
                                if (!dtstart.hasTime()) 
                                	newInv.setIsAllDayEvent(true);
                                break;
                            case DTEND:
                                if (isEvent) {
                                    ParsedDateTime dtend = ParsedDateTime.parse(prop, tzmap);
                                    newInv.setDtEnd(dtend);
                                }
                                break;
                            case DUE:
                                if (isTodo) {
                                    ParsedDateTime due = ParsedDateTime.parse(prop, tzmap);
                                    // DUE is for VTODO what DTEND is for VEVENT.
                                    newInv.setDtEnd(due);
                                }
                                break;
                            case DURATION:
                                ParsedDuration dur = ParsedDuration.parse(prop.getValue());
                                newInv.setDuration(dur);
                                break;
                            case LOCATION:
                                newInv.setLocation(prop.getValue());
                                break;
                            case SUMMARY:
                                String summary = prop.getValue();
                                if (summary != null) {
                                    // Make sure SUMMARY is a single line.
                                    summary = ZCalendar.unescape(summary);
                                    summary = summary.replaceAll("[\\\r\\\n]+", " ");
                                }
                                prop.setValue(summary);
                                newInv.setName(summary);
                                break;
                            case DESCRIPTION:
                                newInv.setDescription(prop.mValue);
                                newInv.setFragment(Fragment.getFragment(prop.mValue, true));
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
                                RdateExdate rdate = RdateExdate.parse(prop, tzmap);
                                addRecurs.add(rdate);
                                newInv.setIsRecurrence(true);
                                break;
                            case EXRULE:
                                ZRecur exrecur = new ZRecur(prop.getValue(), tzmap);
                                subRecurs.add(exrecur);
                                newInv.setIsRecurrence(true);                            
                                break;
                            case EXDATE:
                                RdateExdate exdate = RdateExdate.parse(prop, tzmap);
                                subRecurs.add(exdate);
                                newInv.setIsRecurrence(true);
                                break;
                            case STATUS:
                                String status = IcalXmlStrMap.sStatusMap.toXml(prop.getValue());
                                if (status != null) {
                                    if (IcalXmlStrMap.STATUS_IN_PROCESS.equals(status)) {
                                        String zstatus = prop.getParameterVal(ICalTok.X_ZIMBRA_STATUS, null);
                                        if (ICalTok.X_ZIMBRA_STATUS_WAITING.toString().equals(zstatus) ||
                                            ICalTok.X_ZIMBRA_STATUS_DEFERRED.toString().equals(zstatus)) {
                                            newInv.setStatus(IcalXmlStrMap.sStatusMap.toXml(zstatus));
                                        } else {
                                            newInv.setStatus(status);
                                        }
                                    } else {
                                        newInv.setStatus(status);
                                    }
                                }
                                break;
                            case TRANSP:
                                if (isEvent) {
                                    String transp = IcalXmlStrMap.sTranspMap.toXml(prop.getValue());
                                    if (transp!=null) {
                                        newInv.setTransparency(transp);
                                    }
                                }
                                break;
                            case CLASS:
                                String classProp = IcalXmlStrMap.sClassMap.toXml(prop.getValue());
                                if (classProp != null)
                                    newInv.setClassProp(classProp);
                                break;
                            case X_MICROSOFT_CDO_ALLDAYEVENT:
                                if (isEvent) {
                                    if (prop.getBoolValue()) 
                                        newInv.setIsAllDayEvent(true);
                                }
                                break;
                            case X_MICROSOFT_CDO_BUSYSTATUS:
                                if (isEvent) {
                                    String fb = IcalXmlStrMap.sOutlookFreeBusyMap.toXml(prop.getValue());
                                    if (fb != null)
                                        newInv.setFreeBusy(fb);
                                }
                                break;
                            case PRIORITY:
                                String prio = prop.getValue();
                                if (prio != null)
                                    newInv.setPriority(prio);
                                break;
                            case PERCENT_COMPLETE:
                                if (isTodo) {
                                    String pctComplete = prop.getValue();
                                    if (pctComplete != null)
                                        newInv.setPercentComplete(pctComplete);
                                }
                                break;
                            case COMPLETED:
                                if (isTodo) {
                                    ParsedDateTime completed = ParsedDateTime.parseUtcOnly(prop.getValue());
                                    newInv.setCompleted(completed.getUtcTime());
                                }
                                break;
                            }
                        }
    
                        newInv.setIsOrganizer(account);
    
                        newInv.validateDuration();
    
                        ParsedDuration duration = newInv.getDuration();
                        
                        if (duration == null) {
                            ParsedDateTime end = newInv.getEndTime();
                            if (end != null && newInv.getStartTime() != null) {
                                duration = end.difference(newInv.getStartTime());
                            }
                        }
    
                        InviteInfo inviteInfo = new InviteInfo(newInv);
                        List<IRecurrence> addRules = new ArrayList<IRecurrence>();
                        if (addRecurs.size() > 0) {
                            for (Iterator<Object> iter = addRecurs.iterator(); iter.hasNext();) {
                                Object next = iter.next();
                                if (next instanceof ZRecur) {
                                    ZRecur cur = (ZRecur) next;
                                    addRules.add(new Recurrence.SimpleRepeatingRule(newInv.getStartTime(), duration, cur, inviteInfo));
                                } else if (next instanceof RdateExdate) {
                                    RdateExdate rdate = (RdateExdate) next;
                                    addRules.add(new Recurrence.SingleDates(rdate, duration, inviteInfo));
                                }
                            }
                        }
                        List<IRecurrence> subRules = new ArrayList<IRecurrence>();
                        if (subRecurs.size() > 0) {
                            for (Iterator<Object> iter = subRecurs.iterator(); iter.hasNext();) {
                                Object next = iter.next();
                                if (next instanceof ZRecur) {
                                    ZRecur cur = (ZRecur) iter.next();
                                    subRules.add(new Recurrence.SimpleRepeatingRule(newInv.getStartTime(), duration, cur, inviteInfo));
                                } else if (next instanceof RdateExdate) {
                                    RdateExdate exdate = (RdateExdate) next;
                                    subRules.add(new Recurrence.SingleDates(exdate, duration, inviteInfo));
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
                        
                        String location = newInv.getLocation();
                        if (location == null)
                        	newInv.setLocation("");
    
                        // Process callback.
                        if (visitor != null)
                            visitor.visit(newInv);
                    } catch (ParseException e) {
                        throw ServiceException.PARSE_ERROR(
                            "Unable to parse iCalendar data: " + e.getMessage(), e);
                      
                    }
                    
                    break;
                }
            } catch (ServiceException e) {
                if (!continueOnError) throw e;
                if (newInv != null)
                    logIcsParseImportError(newInv, e);
                else
                    ZimbraLog.calendar.warn("Skipping error during ics parse/import", e);
            } catch (RuntimeException e) {
                if (!continueOnError) throw e;
                if (newInv != null)
                    logIcsParseImportError(newInv, e);
                else
                    ZimbraLog.calendar.warn("Skipping error during ics parse/import", e);
            }
        }
    }

    private static void logIcsParseImportError(Invite inv, Exception e) {
        String uid = inv.getUid();
        String recurrenceId = inv.hasRecurId() ? inv.getRecurId().toString() : null;
        int seq = inv.getSeqNo();
        String dtStart = inv.getStartTime() != null ? inv.getStartTime().toString() : null;
        String summary = inv.getName();
        ZimbraLog.calendar.warn(
                "Skipping error during ics parse/import: UID:" + uid +
                (recurrenceId != null ? ", RECURRENCE-ID:" + recurrenceId : "") +
                ", SEQUENCE:" + seq +
                (dtStart != null ? ", DTSTART:" + dtStart : "") +
                (summary != null ? ", SUMMARY:" + summary : ""),
                e);
    }
    
    public ZComponent newToVComponent(boolean useOutlookCompatMode, boolean includePrivateData)
    throws ServiceException {
        ICalTok compTok;
        if (mItemType == MailItem.TYPE_TASK) {
            compTok = ICalTok.VTODO;
            useOutlookCompatMode = false;
        } else {
            compTok = ICalTok.VEVENT;
        }
        ZComponent component = new ZComponent(compTok);

        component.addProperty(new ZProperty(ICalTok.UID, getUid()));
        
        IRecurrence recur = getRecurrence();
        if (recur != null) {
            for (Iterator iter = recur.addRulesIterator(); iter!=null && iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();

                switch (cur.getType()) { 
                case Recurrence.TYPE_SINGLE_DATES:
                    Recurrence.SingleDates sd = (Recurrence.SingleDates) cur;
                    RdateExdate rdate = sd.getRdateExdate();
                    component.addProperty(rdate.toZProperty());
                    break;
                case Recurrence.TYPE_REPEATING:
                    Recurrence.SimpleRepeatingRule srr = (Recurrence.SimpleRepeatingRule)cur;
                    component.addProperty(new ZProperty(ICalTok.RRULE, srr.getRule().toString()));
                    break;
                }
                
            }
            for (Iterator iter = recur.subRulesIterator(); iter!=null && iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();

                switch (cur.getType()) { 
                case Recurrence.TYPE_SINGLE_DATES:
                    Recurrence.SingleDates sd = (Recurrence.SingleDates) cur;
                    RdateExdate exdate = sd.getRdateExdate();
                    component.addProperty(exdate.toZProperty());
                    break;
                case Recurrence.TYPE_REPEATING:
                    Recurrence.SimpleRepeatingRule srr = (Recurrence.SimpleRepeatingRule)cur;
                    component.addProperty(new ZProperty(ICalTok.EXRULE, srr.getRule().toString()));
                    break;
                }
            }
        }
        

        if (includePrivateData || isPublic()) {
            // SUMMARY (aka Name or Subject)
            String name = getName();
            if (name != null && name.length()>0)
                component.addProperty(new ZProperty(ICalTok.SUMMARY, name));
            
            // DESCRIPTION
            String desc = getDescription();
            if (desc != null && desc.length()>0)
                component.addProperty(new ZProperty(ICalTok.DESCRIPTION, desc));
            
            // COMMENT
            String comment = getComment();
            if (comment != null && comment.length()>0) 
                component.addProperty(new ZProperty(ICalTok.COMMENT, comment));

            // LOCATION
            String location = getLocation();
            if (location != null)
                component.addProperty(new ZProperty(ICalTok.LOCATION, location.toString()));

            // ATTENDEES
            for (ZAttendee at : (List<ZAttendee>)getAttendees()) 
                component.addProperty(at.toProperty());

            // PRIORITY
            if (mPriority != null)
                component.addProperty(new ZProperty(ICalTok.PRIORITY, mPriority));

            // PERCENT-COMPLETE
            if (isTodo() && mPercentComplete != null)
                component.addProperty(new ZProperty(ICalTok.PERCENT_COMPLETE, mPercentComplete));

            // COMPLETED
            if (isTodo() && mCompleted != 0) {
                ParsedDateTime completed = ParsedDateTime.fromUTCTime(mCompleted);
                component.addProperty(completed.toProperty(ICalTok.COMPLETED, false));
            }

            // VALARMs
            for (Alarm alarm : mAlarms) {
                ZComponent alarmComp = alarm.toZComponent();
                component.addComponent(alarmComp);
            }

            // x-prop
            for (ZProperty xprop : mXProps) {
                component.addProperty(xprop);
            }
        }

        // ORGANIZER
        if (hasOrganizer())
            component.addProperty(getOrganizer().toProperty());

        // DTSTART
        ParsedDateTime dtstart = getStartTime();
        if (dtstart != null)
            component.addProperty(dtstart.toProperty(ICalTok.DTSTART, useOutlookCompatMode));
        
        // DTEND or DUE
        ParsedDateTime dtend = getEndTime();
        if (dtend != null) {
            ICalTok prop = ICalTok.DTEND;
            if (isTodo())
                prop = ICalTok.DUE;
            component.addProperty(dtend.toProperty(prop, useOutlookCompatMode));
        }
        
        // DURATION
        ParsedDuration dur = getDuration();
        if (dur != null)
            component.addProperty(new ZProperty(ICalTok.DURATION, dur.toString()));
        
        // STATUS
        String status = getStatus();
        String statusIcal = IcalXmlStrMap.sStatusMap.toIcal(status);
        if (IcalXmlStrMap.STATUS_ZCO_WAITING.equals(status) ||
            IcalXmlStrMap.STATUS_ZCO_DEFERRED.equals(status)) {
            ZParameter param = new ZParameter(ICalTok.X_ZIMBRA_STATUS, statusIcal);
            ZProperty prop = new ZProperty(ICalTok.STATUS, ICalTok.IN_PROCESS.toString());
            prop.addParameter(param);
            component.addProperty(prop);
        } else {
            component.addProperty(new ZProperty(ICalTok.STATUS, statusIcal));
        }

        // CLASS
        component.addProperty(new ZProperty(ICalTok.CLASS, IcalXmlStrMap.sClassMap.toIcal(getClassProp())));

        if (isEvent()) {
            // allDay
            if (isAllDayEvent())
                component.addProperty(new ZProperty(ICalTok.X_MICROSOFT_CDO_ALLDAYEVENT, true));
            
            // Microsoft Outlook compatibility for free-busy status
            {
                String outlookFreeBusy = IcalXmlStrMap.sOutlookFreeBusyMap.toIcal(getFreeBusy());
                component.addProperty(new ZProperty(ICalTok.X_MICROSOFT_CDO_BUSYSTATUS, outlookFreeBusy));
                component.addProperty(new ZProperty(ICalTok.X_MICROSOFT_CDO_INTENDEDSTATUS, outlookFreeBusy));
            }

            // TRANSPARENCY
            component.addProperty(new ZProperty(ICalTok.TRANSP, IcalXmlStrMap.sTranspMap.toIcal(getTransparency())));
        }
        
        
        // RECURRENCE-ID
        RecurId recurId = getRecurId();
        if (recurId != null) 
            component.addProperty(recurId.toProperty(useOutlookCompatMode));
        
        // DTSTAMP
        ParsedDateTime dtStamp = ParsedDateTime.fromUTCTime(getDTStamp());
        component.addProperty(dtStamp.toProperty(ICalTok.DTSTAMP, useOutlookCompatMode));
        
        // SEQUENCE
        component.addProperty(new ZProperty(ICalTok.SEQUENCE, getSeqNo()));

        return component;
    }

    public Iterator<Alarm> alarmsIterator() { return mAlarms.iterator(); }
    public void addAlarm(Alarm alarm) {
        mAlarms.add(alarm);
    }
    
    /**
     * Clear this Invite's alarms
     */
    public void clearAlarms() {
    	mAlarms.clear();
    }

    public Iterator<ZProperty> xpropsIterator() { return mXProps.iterator(); }
    public void addXProp(ZProperty prop) {
        mXProps.add(prop);
    }

    /**
     * RFC2445 requires end date/time to be later than start date/time but
     * some calendar clients don't honor that.  Make sure the interval between
     * start and end are at least 1 second (if date/time) or 1 day (if date-only).
     * @throws ServiceException
     */
    public void validateDuration() throws ServiceException {
        if (mStart == null)
            return;
        if (!isTodo()) {
            ParsedDuration dur =
                mStart.hasTime() ? ParsedDuration.parse(false, 0, 0, 0, 0, 1)
                                 : ParsedDuration.parse(false, 0, 1, 0, 0, 0);
            if (mEnd != null && mEnd.compareTo(mStart) <= 0) {
                mEnd = mStart.add(dur);
            } else if (mDuration != null && mDuration.getDurationAsMsecs(mStart.getDate()) <= 0) {
                mDuration = dur;
            }
        }
    }

    public Invite newCopy() {
        Invite inv = new Invite(
                mItemType, mMethod != null ? mMethod.toString() : null,
                mTzMap,
                mCalItem, mUid,
                mStatus, mPriority,
                mPercentComplete, mCompleted,
                mFreeBusy, mTransparency, mClass,
                mStart, mEnd, mDuration,
                mRecurrence,
                mIsOrganizer, mOrganizer, mAttendees,
                mName, mComment, mLocation,
                mFlags, mPartStat, mRsvp, mRecurrenceId, mDTStamp, mSeqNo,
                0, // mMailboxId
                0, // mMailItemId
                0, // mComponentNum
                mSentByMe,
                mDescription, mFragment);
        inv.setClassPropSetByMe(classPropSetByMe());
        inv.setDontIndexMimeMessage(getDontIndexMimeMessage());
        return inv;
    }
}
