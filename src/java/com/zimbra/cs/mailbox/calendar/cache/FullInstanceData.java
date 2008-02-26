/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;

// default instance, or instance overridden by an exception
// null value returned by a getter means to inherit from default instance of appointment/task
public class FullInstanceData extends InstanceData {
    // ZCS-specific meta data
    private int mInvId;
    private int mCompNum;

    // change management info
    private long mRecurrenceId;
    private int mSequence;
    private long mDtStamp;

    // organizer/attendees
    private ZOrganizer mOrganizer;
    private Boolean mIsOrganizer;
    private Integer mNumAttendees;
    private List<ZAttendee> mAttendees;

    // summary/location/description
    private String mSummary;
    private String mLocation;
    private String mDescription;
    private String mFragment;

    // time/recurrence
    private Boolean mIsAllDay;

    // common meta data
    private String mStatus;
    private String mPriority;
    private String mClassProp;

    // appointment-only meta data
    private String mFreeBusyIntended;
    private String mTransparency;


    public int getInvId()     { return mInvId; }
    public int getCompNum()   { return mCompNum; }

    public long getRecurrenceId() { return mRecurrenceId; }
    public int getSequence()      { return mSequence; }
    public long getDtStamp()      { return mDtStamp; }

    public ZOrganizer getOrganizer()    { return mOrganizer; }
    public Boolean isOrganizer()          { return mIsOrganizer; }
    public Integer getNumAttendees()      { return mNumAttendees; }
    public List<ZAttendee> getAttendees() { return mAttendees; }

    public String getSummary()     { return mSummary; }
    public String getLocation()    { return mLocation; }
    public String getDescription() { return mDescription; }
    public String getFragment()    { return mFragment; }

    public Boolean isAllDay() { return mIsAllDay; }

    public String getStatus() { return mStatus; }
    public String getPriority() { return mPriority; }
    public String getClassProp() { return mClassProp; }

    public String getFreeBusyIntended() { return mFreeBusyIntended; }
    public String getTransparency() { return mTransparency; }

    public FullInstanceData(
            long dtStart, long duration, long alarmAt, long tzOffset,
            String partStat, String freeBusyActual, String percentComplete,
            int invId, int compNum,
            long recurrenceId, int sequence, long dtStamp,
            ZOrganizer organizer, Boolean isOrganizer, List<ZAttendee> attendees,
            String summary, String location, String description, String fragment,
            Boolean isAllDay,
            String status, String priority, String classProp,
            String freeBusyIntended, String transparency) {
        super(dtStart, duration, alarmAt, tzOffset, partStat, freeBusyActual, percentComplete);
        init(invId, compNum, recurrenceId, sequence, dtStamp,
             organizer, isOrganizer, attendees, summary, location, description, fragment,
             isAllDay, status, priority, classProp, freeBusyIntended, transparency);
    }

    private void init(
            int invId, int compNum,
            long recurrenceId, int sequence, long dtStamp,
            ZOrganizer organizer, Boolean isOrganizer, List<ZAttendee> attendees,
            String summary, String location, String description, String fragment,
            Boolean isAllDay,
            String status, String priority, String classProp,
            String freeBusyIntended, String transparency) {
        mInvId = invId; mCompNum = compNum;
        mRecurrenceId = recurrenceId; mSequence = sequence; mDtStamp = dtStamp;
        mOrganizer = organizer; mIsOrganizer = isOrganizer;
        mAttendees = attendees;
        mNumAttendees = attendees != null ? (Integer) attendees.size() : null;
        mSummary = summary; mLocation = location; mDescription = description; mFragment = fragment;
        mIsAllDay = isAllDay;
        mStatus = status; mPriority = priority; mClassProp = classProp;
        mFreeBusyIntended = freeBusyIntended; mTransparency = transparency;
    }

    // create a full instance, clearing fields that don't override the default instance
    public FullInstanceData(Invite inv, Long dtStart, Long duration, Long alarmAt,
                            FullInstanceData defaultInstance)
    throws ServiceException {
        super(dtStart, duration, alarmAt,
              dtStart != null ? Util.getTZOffsetForInvite(inv, dtStart) : null,
              inv.getPartStat(), inv.getFreeBusyActual(), inv.getPercentComplete());
        mInvId = inv.getMailItemId();
        mCompNum = inv.getComponentNum();
        if (inv.hasRecurId()) {
            RecurId rid = inv.getRecurId();
            mRecurrenceId = rid.getDt().getUtcTime();
        }
        mSequence = inv.getSeqNo();
        mDtStamp = inv.getDTStamp();
        if (inv.hasOrganizer())
            mOrganizer = inv.getOrganizer();
        mIsOrganizer = inv.isOrganizer();
        if (inv.hasOtherAttendees()) {
            mAttendees = inv.getAttendees();
            mNumAttendees = mAttendees.size();
        }
        mSummary = inv.getName();
        mLocation = inv.getLocation();
        mDescription = inv.getDescription();
        mFragment = inv.getFragment();
        mIsAllDay = inv.isAllDayEvent();
        mStatus = inv.getStatus();
        mPriority = inv.getPriority();
        mClassProp = inv.getClassProp();
        mFreeBusyIntended = inv.getFreeBusy();
        mTransparency = inv.getTransparency();
        clearUnchangedFields(defaultInstance);
    }

    protected void clearUnchangedFields(FullInstanceData other) {
        super.clearUnchangedFields(other);
        if (other != null) {
            if (Util.sameValues(mIsOrganizer, other.isOrganizer()))
                mIsOrganizer = null;
            if (Util.sameValues(mNumAttendees, other.getNumAttendees()))
                mNumAttendees = null;
            if (Util.sameValues(mSummary, other.getSummary()))
                 mSummary = null;
            if (Util.sameValues(mLocation, other.getLocation()))
                mLocation = null;
            if (Util.sameValues(mDescription, other.getDescription()))
                mDescription = null;
            if (Util.sameValues(mFragment, other.getFragment()))
                mFragment = null;
            if (Util.sameValues(mIsAllDay, other.isAllDay()))
                mIsAllDay = null;
            if (Util.sameValues(mStatus, other.getStatus()))
                mStatus = null;
            if (Util.sameValues(mPriority, other.getPriority()))
                mPriority = null;
            if (Util.sameValues(mClassProp, other.getClassProp()))
                mClassProp = null;
            if (Util.sameValues(mFreeBusyIntended, other.getFreeBusyIntended()))
                mFreeBusyIntended = null;
            if (Util.sameValues(mTransparency, other.getTransparency()))
                mTransparency = null;
        }
    }

    private static final String FN_IS_FULL_INSTANCE = "isFull";
    private static final String FN_INVID = "invId";
    private static final String FN_COMPNUM = "compNum";
    private static final String FN_RECURRENCE_ID = "rid";
    private static final String FN_SEQUENCE = "seq";
    private static final String FN_DTSTAMP = "dtstamp";
    private static final String FN_ORGANIZER = "org";
    private static final String FN_IS_ORGANIZER = "isOrg";
    private static final String FN_NUM_ATTENDEES = "numAt";
    private static final String FN_ATTENDEE = "at";
    private static final String FN_SUMMARY = "summ";
    private static final String FN_LOCATION = "loc";
    private static final String FN_DESCRIPTION = "desc";
    private static final String FN_FRAGMENT = "fr";
    private static final String FN_IS_ALLDAY = "allDay";
    private static final String FN_STATUS = "status";
    private static final String FN_PRIORITY = "prio";
    private static final String FN_CLASS = "class";
    private static final String FN_FREEBUSY = "fb";
    private static final String FN_TRANSPARENCY = "transp";

    FullInstanceData(Metadata meta) throws ServiceException {
        super(meta);
        int invId = (int) meta.getLong(FN_INVID);
        int compNum = (int) meta.getLong(FN_COMPNUM);
        long recurId = meta.getLong(FN_RECURRENCE_ID);
        int seq = (int) meta.getLong(FN_SEQUENCE);
        long dtStamp = meta.getLong(FN_DTSTAMP);

        ZOrganizer org = null;
        Metadata metaOrg = meta.getMap(FN_ORGANIZER, true);
        if (metaOrg != null)
            org = new ZOrganizer(metaOrg);
        Boolean isOrg = null;
        if (meta.containsKey(FN_IS_ORGANIZER))
            isOrg = new Boolean(meta.getBool(FN_IS_ORGANIZER));

        List<ZAttendee> attendees = null;
        if (meta.containsKey(FN_NUM_ATTENDEES)) {
            int num = (int) meta.getLong(FN_NUM_ATTENDEES);
            if (num > 0) {
                attendees = new ArrayList<ZAttendee>(num);
                for (int i = 0; i < num; i++) {
                    Metadata metaAt = meta.getMap(FN_ATTENDEE + i, true);
                    if (metaAt != null)
                        attendees.add(new ZAttendee(metaAt));
                }
            }
        }

        String summary = meta.get(FN_SUMMARY, null);
        String location = meta.get(FN_LOCATION, null);
        String description = meta.get(FN_DESCRIPTION, null);
        String fragment = meta.get(FN_FRAGMENT, null);

        Boolean isAllDay = null;
        if (meta.containsKey(FN_IS_ALLDAY))
            isAllDay = new Boolean(meta.getBool(FN_IS_ALLDAY));

        String status = meta.get(FN_STATUS, null);
        String priority = meta.get(FN_PRIORITY, null);
        String classProp = meta.get(FN_CLASS, null);
        String fb = meta.get(FN_FREEBUSY, null);
        String transp = meta.get(FN_TRANSPARENCY, null);

        init(invId, compNum, recurId, seq, dtStamp, org, isOrg, attendees,
             summary, location, description, fragment, isAllDay,
             status, priority, classProp, fb, transp);
    }

    Metadata encodeMetadata() {
        Metadata meta = super.encodeMetadata();

        meta.put(FN_IS_FULL_INSTANCE, true);
        meta.put(FN_INVID, mInvId);
        meta.put(FN_COMPNUM, mCompNum);
        meta.put(FN_RECURRENCE_ID, mRecurrenceId);
        meta.put(FN_SEQUENCE, mSequence);
        meta.put(FN_DTSTAMP, mDtStamp);

        if (mOrganizer != null)
            meta.put(FN_ORGANIZER, mOrganizer.encodeMetadata());
        if (mIsOrganizer != null)
            meta.put(FN_IS_ORGANIZER, mIsOrganizer.booleanValue());
        if (mAttendees != null) {
            meta.put(FN_NUM_ATTENDEES, mAttendees.size());
            int i = 0;
            for (ZAttendee at : mAttendees) {
                meta.put(FN_ATTENDEE + i, at.encodeAsMetadata());
                i++;
            }
        }

        meta.put(FN_SUMMARY, mSummary);
        meta.put(FN_LOCATION, mLocation);
        meta.put(FN_DESCRIPTION, mDescription);
        meta.put(FN_FRAGMENT, mFragment);

        if (mIsAllDay != null)
            meta.put(FN_IS_ALLDAY, mIsAllDay.booleanValue());

        meta.put(FN_STATUS, mStatus);
        meta.put(FN_PRIORITY, mPriority);
        meta.put(FN_CLASS, mClassProp);
        meta.put(FN_FREEBUSY, mFreeBusyIntended);
        meta.put(FN_TRANSPARENCY, mTransparency);

        return meta;
    }

    public static boolean isFullInstanceMeta(Metadata meta) throws ServiceException {
        return meta.getBool(FN_IS_FULL_INSTANCE, false);
    }
}
