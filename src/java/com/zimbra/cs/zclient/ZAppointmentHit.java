/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.mailbox.calendar.Geo;
import com.zimbra.cs.zclient.ZInvite.ZComponent;
import com.zimbra.cs.zclient.ZInvite.ZStatus;
import com.zimbra.cs.zclient.ZMailbox.ZFreeBusyTimeSlot;
import com.zimbra.cs.zclient.event.ZModifyAppointmentEvent;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

public class ZAppointmentHit implements ZSearchHit {

    public static final String FBA_FREE = "F";
    public static final String FBA_BUSY = "B";
    public static final String FBA_TENTATIVE = "T";
    public static final String FBA_UNAVAILABLE = "U";
    public static final String FBA_NODATA= "N";

    public static final String TRANSP_OPAQUE = "O";
    public static final String TRANSP_TRANSPARENT = "T";

    public static final String STATUS_TENTATIVE = "TENT";
    public static final String STATUS_CONFIRMED = "CONF";
    public static final String STATUS_CANCELLED = "CANC";


    public static final String STATUS_COMPLETED = "COMP";
    public static final String STATUS_DEFERRED = "DEFERRED";
    public static final String STATUS_INPROGRESS = "INPR";
    public static final String STATUS_NOT_STARTED = "NEED";
    public static final String STATUS_WAITING = "WAITING";
    
    public static final String PSTATUS_NEEDS_ACTION = "NE";
    public static final String PSTATUS_TENTATIVE = "TE";
    public static final String PSTATUS_ACCEPT = "AC";
    public static final String PSTATUS_DECLINED = "DE";
    public static final String PSTATUS_DELEGATED = "DG";

    public static final String CLASS_PUBLIC = "PUB";
    public static final String CLASS_PRIVATE = "PRI";
    public static final String CLASS_CONFIDENTIAL = "CON";

    public enum Flag {
        flagged('f'),
        attachment('a');

        private char mFlagChar;

        public char getFlagChar() { return mFlagChar; }

        public static String toNameList(String flags) {
            if (flags == null || flags.length() == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i=0; i < flags.length(); i++) {
                String v = null;
                for (Flag f : Flag.values()) {
                    if (f.getFlagChar() == flags.charAt(i)) {
                        v = f.name();
                        break;
                    }
                }
                if (sb.length() > 0) sb.append(", ");
                sb.append(v == null ? flags.substring(i, i+1) : v);
            }
            return sb.toString();
        }

        Flag(char flagChar) {
            mFlagChar = flagChar;

        }
    }

    private String mId;
    private String mFlags;
    private String mTags;
    private String mFreeBusyActual;
    private String mTransparency;
    private String mStatus;
    private String mClass;
    private String mPartStatus;
    private boolean mIsAllDay;
    private boolean mIsOtherAttendees;
    private boolean mIsAlarm;
    private boolean mIsRecurring;
    private String mName;
    private String mLocation;
    private List<String> mCategories;
    private Geo mGeo;
    private String mInviteId;
    private String mSeriesInviteId;
    private String mSeriesComponentNumber;
    private String mInviteComponentNumber;
    private boolean mIsOrganizer;
    private String mPriority;
    private String mPercentComplete;
    private long mDueDate;
    private long mDuration;
    private String mFragment;
    private String mSortField;
    private float mScore;
    private long mSize;
    private String mConvId;
    private long mHitDate;
    private boolean mInstanceExpanded;
    private boolean mIsTask;
    
    private long mStartTime;
    private long mEndTime;
    private long mTimeZoneOffset;
    private boolean mIsException;
    private String mRecurrenceIdZ;  // RECURRENCE-ID in "Z" (UTC) timezone

    private String mFolderId;
    private String mUid;
    private long mModifiedSeq;
    private long mModifiedDate;
    private long mSavedSeq;

    private boolean mIsFromFreeBusy;

    ZAppointmentHit() {

    }

    ZAppointmentHit(ZFreeBusyTimeSlot slot) {
        switch (slot.getType()) {
            case BUSY:
                mFreeBusyActual = FBA_BUSY;
                break;
            case TENTATIVE:
                mFreeBusyActual = FBA_TENTATIVE;
                break;
            case UNAVAILABLE:
                mFreeBusyActual = FBA_UNAVAILABLE;
                break;
            case NODATA:
                mFreeBusyActual = FBA_NODATA;
                break;
            default:
                mFreeBusyActual = FBA_FREE;
        }
        mStartTime = slot.getStartTime();
        mEndTime = slot.getEndTime();
        mDuration = mEndTime - mStartTime;
        mIsFromFreeBusy = true;
    }

    static void addInstances(Element e, List<ZSearchHit> appts, TimeZone timeZone, boolean isTask) throws ServiceException {
        String id = e.getAttribute(MailConstants.A_ID);
        String freeBusyActual = e.getAttribute(MailConstants.A_APPT_FREEBUSY_ACTUAL, null);
        String transparency = e.getAttribute(MailConstants.A_APPT_TRANSPARENCY, null);
        String status = e.getAttribute(MailConstants.A_CAL_STATUS, null);
        String classProp = e.getAttribute(MailConstants.A_CAL_CLASS, null);
        String pstatus = e.getAttribute(MailConstants.A_CAL_PARTSTAT, null);
        boolean isAllDay = e.getAttributeBool(MailConstants.A_CAL_ALLDAY, false);
        boolean otherAtt = e.getAttributeBool(MailConstants.A_CAL_OTHER_ATTENDEES, false);
        boolean isAlarm = e.getAttributeBool(MailConstants.A_CAL_ALARM, false);
        boolean isRecurring = e.getAttributeBool(MailConstants.A_CAL_RECUR, false);
        String flags = e.getAttribute(MailConstants.A_FLAGS, null);
        String tags = e.getAttribute(MailConstants.A_TAGS, null);
        String name = e.getAttribute(MailConstants.A_NAME, null);
        String location = e.getAttribute(MailConstants.A_CAL_LOCATION, null);

        List<String> categories = null;
        Iterator<Element> catIter = e.elementIterator(MailConstants.E_CAL_CATEGORY);
        if (catIter.hasNext()) {
            categories = new ArrayList<String>();
            for (; catIter.hasNext(); ) {
                String cat = catIter.next().getTextTrim();
                categories.add(cat);
            }
        }
        Geo geo = null;
        Element geoElem = e.getOptionalElement(MailConstants.E_CAL_GEO);
        if (geoElem != null)
            geo = Geo.parse(geoElem);

        String inviteId = e.getAttribute(MailConstants.A_CAL_INV_ID, null);
        String inviteCompNumber = e.getAttribute(MailConstants.A_CAL_COMPONENT_NUM, null);
        boolean isOrganizer = e.getAttributeBool(MailConstants.A_CAL_ISORG, false);
        String priority = e.getAttribute(MailConstants.A_CAL_PRIORITY, null);
        String percentComplete = e.getAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, null);
        long dueDate = e.getAttributeLong(MailConstants.A_TASK_DUE_DATE, 0);

        long duration = e.getAttributeLong(MailConstants.A_CAL_NEW_DURATION, 0);
        long hitDate = e.getAttributeLong(MailConstants.A_DATE, 0);


        String sortField = e.getAttribute(MailConstants.A_SORT_FIELD, null);
        long size = (int) e.getAttributeLong(MailConstants.A_SIZE, 0);
        String convId = e.getAttribute(MailConstants.A_CONV_ID, null);
        float score = (float) e.getAttributeDouble(MailConstants.A_SCORE, 0);
        String folderId = e.getAttribute(MailConstants.A_FOLDER, null);

        String fragment = e.getAttribute(MailConstants.E_FRAG, null);
        String uid = e.getAttribute(MailConstants.A_UID, null);
        long ms = e.getAttributeLong(MailConstants.A_MODIFIED_SEQUENCE, 0);
        long md = e.getAttributeLong(MailConstants.A_DATE, 0);
        long ss = e.getAttributeLong(MailConstants.A_REVISION, 0);

        List<Element> instances = e.listElements(MailConstants.E_INSTANCE);
        // if empty, add self as only instance
        boolean noInstances = instances.isEmpty();
        if (noInstances) {
            instances = new ArrayList<Element>();
            instances.add(e);
        }

        for (Element inst : instances) {
            ZAppointmentHit appt = isTask ? new ZTaskHit() : new ZAppointmentHit();
            appt.mTimeZoneOffset = e.getAttributeLong(MailConstants.A_CAL_TZ_OFFSET, 0);
            appt.mInstanceExpanded = !noInstances;
            appt.mFolderId = folderId;
            appt.mId = id;
            appt.mScore = score;
            appt.mSize = size;
            appt.mSortField = sortField;
            appt.mConvId = convId;
            appt.mHitDate = hitDate;
            appt.mIsTask = isTask;

            appt.mIsAllDay = inst.getAttributeBool(MailConstants.A_CAL_ALLDAY, isAllDay);
            appt.mTimeZoneOffset = inst.getAttributeLong(MailConstants.A_CAL_TZ_OFFSET,  appt.mTimeZoneOffset);
            appt.mStartTime = inst.getAttributeLong(MailConstants.A_CAL_START_TIME, 0);

            if (appt.mIsAllDay) {
                appt.mStartTime += appt.mTimeZoneOffset - timeZone.getOffset(appt.mStartTime);
            }

            appt.mIsException = inst.getAttributeBool(MailConstants.A_CAL_IS_EXCEPTION, false);
            appt.mRecurrenceIdZ = inst.getAttribute(MailConstants.A_CAL_RECURRENCE_ID_Z, null);

            appt.mFreeBusyActual = inst.getAttribute(MailConstants.A_APPT_FREEBUSY_ACTUAL, freeBusyActual);
            appt.mTransparency = inst.getAttribute(MailConstants.A_APPT_TRANSPARENCY, transparency);
            appt.mStatus = inst.getAttribute(MailConstants.A_CAL_STATUS, status);
            appt.mClass = inst.getAttribute(MailConstants.A_CAL_CLASS, classProp);
            appt.mPartStatus = inst.getAttribute(MailConstants.A_CAL_PARTSTAT, pstatus);

            appt.mIsOtherAttendees = inst.getAttributeBool(MailConstants.A_CAL_OTHER_ATTENDEES, otherAtt);
            appt.mIsAlarm = inst.getAttributeBool(MailConstants.A_CAL_ALARM, isAlarm);
            appt.mIsRecurring = inst.getAttributeBool(MailConstants.A_CAL_RECUR, isRecurring);

            appt.mFlags = inst.getAttribute(MailConstants.A_FLAGS, flags);
            appt.mTags = inst.getAttribute(MailConstants.A_TAGS, tags);
            appt.mName = inst.getAttribute(MailConstants.A_NAME, name);
            appt.mLocation = inst.getAttribute(MailConstants.A_CAL_LOCATION, location);

            List<String> instCategories = null;
            Iterator<Element> instCatIter = inst.elementIterator(MailConstants.E_CAL_CATEGORY);
            if (instCatIter.hasNext()) {
                instCategories = new ArrayList<String>();
                for (; instCatIter.hasNext(); ) {
                    String cat = instCatIter.next().getTextTrim();
                    instCategories.add(cat);
                }
                appt.mCategories = instCategories;
            } else {
                appt.mCategories = categories;
            }
            Element instGeoElem = inst.getOptionalElement(MailConstants.E_CAL_GEO);
            if (instGeoElem != null)
                appt.mGeo = Geo.parse(instGeoElem);
            else
                appt.mGeo = geo;

            appt.mInviteId = inst.getAttribute(MailConstants.A_CAL_INV_ID, inviteId);
            appt.mSeriesInviteId = inviteId;

            appt.mInviteComponentNumber = inst.getAttribute(MailConstants.A_CAL_COMPONENT_NUM, inviteCompNumber);
            appt.mSeriesComponentNumber = inviteCompNumber;

            appt.mIsOrganizer = inst.getAttributeBool(MailConstants.A_CAL_ISORG, isOrganizer);
            appt.mPriority = inst.getAttribute(MailConstants.A_CAL_PRIORITY, priority);
            appt.mPercentComplete = inst.getAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, percentComplete);
            appt.mDueDate = inst.getAttributeLong(MailConstants.A_TASK_DUE_DATE, dueDate);
            appt.mDuration = inst.getAttributeLong(MailConstants.A_CAL_NEW_DURATION, duration);

            appt.mEndTime = appt.mStartTime + appt.mDuration;
            appt.mFragment = inst.getAttribute(MailConstants.E_FRAG, fragment);
            appt.mUid = inst.getAttribute(MailConstants.A_UID, uid);
            appt.mModifiedSeq = inst.getAttributeLong(MailConstants.A_MODIFIED_SEQUENCE, ms);
            appt.mModifiedDate = inst.getAttributeLong(MailConstants.A_MODIFIED_DATE, md);
            appt.mSavedSeq = inst.getAttributeLong(MailConstants.A_REVISION, ss);
            appts.add(appt);
        }
    }

    private String getStr(String value, String def) {
        return value == null ? def : value;
    }

    public void modifyNotification(ZModifyEvent event) throws ServiceException {
        if (event instanceof ZModifyAppointmentEvent) {

            ZModifyAppointmentEvent mevent = (ZModifyAppointmentEvent) event;
            ZAppointment appt = (ZAppointment) mevent.getItem();
            mFlags = getStr(appt.getFlags(), mFlags);
            mTags = getStr(appt.getTagIds(), mTags);
            mFolderId = getStr(appt.getFolderId(), mFolderId);

            List<ZInvite> invites = appt.getInvites();
            if (invites.size() > 0) {
                ZInvite inv = invites.get(0);
                ZComponent comp = inv.getComponent();
                if (comp != null) {
                    // just do task-related ones for now, as appts are handled differently
                    ZStatus stat = comp.getStatus();
                    if (stat != null) mStatus = stat.name();
                    mPercentComplete = getStr(comp.getPercentCompleted(), mPercentComplete);
                    mPriority = getStr(comp.getPriority(), mPriority);
                    mName = getStr(comp.getName(), mName);
                    mLocation = getStr(comp.getLocation(), mLocation);
                    if (comp.getCategories() != null)
                        mCategories = comp.getCategories();
                    if (comp.getGeo() != null)
                        mGeo = comp.getGeo();
                }
            }
        }
	}


    public boolean getIsFromFreeBusy() { return mIsFromFreeBusy; }
    
    public String getId() {
        return mId;
    }

    public String getSortField() {
        return mSortField;
    }

    public float getScore() {
        return mScore;
    }

    public String getFolderId() {
        return mFolderId;
    }

    public boolean getIsTask() {
        return mIsTask;
    }
    
    public long getSize() {
        return mSize;
    }

    public String getConversationId() {
        return mConvId;
    }

    public long getHitDate() {
        return mHitDate;
    }

    /**
     * @return returns true if this appt was expanded and has instance data (start time, etc), or not (such as an appointment
     * returned from calling SearchRequest without specifying calExpandInstStart/calExpandInstEnd).
     */
    public boolean getInstanceExpanded() {
        return mInstanceExpanded;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject jo = new ZJSONObject();
        jo.put("id", mId);
        jo.put("folderId", mFolderId);
        jo.put("tags", mTags);
        jo.put("flags", mFlags);
        jo.put("name", mName);
        jo.put("location", mLocation);
        jo.putList("categories", mCategories);
        if (mGeo != null)
            jo.put("geo", mGeo.toString());
        jo.put("inviteId", mInviteId);
        jo.put("inviteComponentNumber", mInviteComponentNumber);
        jo.put("freeBusyActual", mFreeBusyActual);
        jo.put("transparency", mTransparency);
        jo.put("status", mStatus);
        jo.put("class", mClass);
        jo.put("participantStatus", mPartStatus);
        jo.put("allDay", mIsAllDay);
        jo.put("otherAttendees", mIsOtherAttendees);
        jo.put("alarm", mIsAlarm);
        jo.put("recurring", mIsRecurring);
        jo.put("isOrganizer", mIsOrganizer);
        jo.put("priority", mPriority);
        jo.put("percentComplete", mPercentComplete);
        jo.put("duration", mDuration);
        jo.put("startTime", mStartTime);
        jo.put("timeZoneOffset", mTimeZoneOffset);
        jo.put("exception", mIsException);
        jo.put("recurrenceId", mRecurrenceIdZ);
        jo.put("fragment", mFragment);
        jo.put("sortField", mSortField);
        jo.put("score", mScore);
        jo.put("conversationId", mConvId);
        jo.put("size", mSize);
        jo.put("isTask", mIsTask);
        jo.put("hitDate", mHitDate);
        return jo;
    }

    public String toString() {
        return String.format("[ZAppointmentHit %s]", mId);
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }
    
    // fba
    public String getFreeBusyActual() { return mFreeBusyActual; }
    public boolean isFreeBusyActualFree() { return FBA_FREE.equals(mFreeBusyActual); }
    public boolean isFreeBusyActualBusy() { return FBA_BUSY.equals(mFreeBusyActual); }
    public boolean isFreeBusyActualTentative() { return FBA_TENTATIVE.equals(mFreeBusyActual); }
    public boolean isFreeBusyActualUnavailable() { return FBA_UNAVAILABLE.equals(mFreeBusyActual); }
    public boolean isFreeBusyActualNoData() { return FBA_NODATA.equals(mFreeBusyActual); }

    // transp
    public String getTransparency() { return mTransparency; }
    public boolean isTransparencyOpaque() { return TRANSP_OPAQUE.equals(mTransparency); }
    public boolean isTransparencyTransparent() { return TRANSP_TRANSPARENT.equals(mTransparency); }

    // status
    public String getStatus() { return mStatus; }
    public boolean isStatusTentative() { return STATUS_TENTATIVE.equals(mStatus); }
    public boolean isStatusCancelled() { return STATUS_CANCELLED.equals(mStatus); }
    public boolean isStatusConfirmed() { return STATUS_CONFIRMED.equals(mStatus); }

    // class
    public String getClassProp() { return mClass; }
    public boolean isClassPublic() { return CLASS_PUBLIC.equals(mClass); }
    public boolean isClassPrivate() { return CLASS_PRIVATE.equals(mClass); }
    public boolean isClassConfidential() { return CLASS_CONFIDENTIAL.equals(mClass); }

    // ptst
    public String getParticipantStatus() { return mPartStatus; }
    public boolean isPartStatusAccept() { return PSTATUS_ACCEPT.equals(mPartStatus); }
    public boolean isPartStatusDeclined() { return PSTATUS_DECLINED.equals(mPartStatus); }
    public boolean isPartStatusDelegated() { return PSTATUS_DELEGATED.equals(mPartStatus); }
    public boolean isPartStatusNeedsAction() { return PSTATUS_NEEDS_ACTION.equals(mPartStatus); }
    public boolean isPartStatusTentative() { return PSTATUS_TENTATIVE.equals(mPartStatus); }

    public boolean isAllDay() { return mIsAllDay; }

    public boolean isOtherAttendees() { return mIsOtherAttendees; }

    public boolean isAlarm() { return mIsAlarm; }

    public boolean isRecurring() { return mIsRecurring; }
    
    public String getFlags() { return mFlags; }

    public String getName() { return mName; }

    public String getLocation() { return mLocation; }

    public List<String> getCategories() { return mCategories; }

    public Geo getGeo() { return mGeo; }

    public String getInviteId() { return mInviteId; }

    public String getSeriesInviteId() { return mSeriesInviteId; }

    public String getSeriesComponentNumber() { return mSeriesComponentNumber; }

    public String getInviteComponentNumber() { return mInviteComponentNumber; }

    public boolean isOrganizer() { return mIsOrganizer; }

    public String getPriority() { return mPriority; }

    public String getPercentComplete() { return mPercentComplete; }

    public long getDuration() { return mDuration; }

    public long getStartTime() { return mStartTime; }

    public long getDueDateTime() { return mDueDate; }

    public Date getDueDate() { return new Date(mDueDate); }

    /* computed from start+duration */
    public long getEndTime() { return mEndTime; }

    public Date getStartDate() { return new Date(mStartTime); }

    public Date getEndDate() { return new Date(mEndTime); }

    public long getTimeZoneOffset() { return mTimeZoneOffset; }

    public boolean isException() { return mIsException; }

    public String getRecurrenceIdZ() { return mRecurrenceIdZ; }

    public String getFragment() { return mFragment; }

    public String getTagIds() { return mTags; }
    
    public String getUid() { return mUid; }
    
    public long getModifiedSeq() { return mModifiedSeq; }
    
    public long getModifiedDate() { return mModifiedDate; }
    
    public long getSavedSeq() { return mSavedSeq; }

    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;
    }

    public boolean getHasTags() { return mTags != null && mTags.length() > 0; }

    public boolean getHasAttachment() { return hasFlags() && mFlags.indexOf(ZAppointmentHit.Flag.attachment.getFlagChar()) != -1; }

    public boolean getIsFlagged() {
        return hasFlags() && mFlags.indexOf(ZMessage.Flag.flagged.getFlagChar()) != -1;
    }
    
    public boolean isInRange(long start, long end) {
        return mStartTime < end && mEndTime > start;
    }

    public boolean isOverLapping(ZAppointmentHit that) {
        return this.mStartTime < that.mEndTime && this.mEndTime > that.mStartTime;
    }

    public boolean isOverLapping(ZAppointmentHit that, long msecsIncr) {
        long thisStart = ((long)(this.mStartTime / msecsIncr)) * msecsIncr;
        long thisEnd = ((long)((this.mEndTime + msecsIncr - 1) / msecsIncr)) * msecsIncr;

        long thatStart = ((long)(that.mStartTime / msecsIncr)) * msecsIncr;
        long thatEnd = ((long)((that.mEndTime + msecsIncr - 1) / msecsIncr)) * msecsIncr;

        return thisStart < thatEnd && thisEnd > thatStart;
    }

    public boolean isOverLapping(long thatStartTime, long thatEndTime) {
        return this.mStartTime < thatEndTime && this.mEndTime > thatStartTime;
    }

    public static boolean isOverLapping(long start1, long end1, long start2, long end2) {
        return start1 < end2 && end1 > start2;
    }

    public static boolean isOverLapping(long start1, long end1, long start2, long end2, long msecsIncr) {
        start1 = ((long)(start1 / msecsIncr)) * msecsIncr;
        end1 = ((long)((end1 + msecsIncr - 1) / msecsIncr)) * msecsIncr;

        start2 = ((long)(start2 / msecsIncr)) * msecsIncr;
        end2 = ((long)((end2 + msecsIncr - 1) / msecsIncr)) * msecsIncr;

        return start1 < end2 && end1 > start2;
    }

    /**
     * sort two appt summaries by all day, start time, duration, folder id.
     */
    public static class SortByTimeDurationFolder implements Comparator {
        public int compare(Object obja, Object objb) {
            if (!(obja instanceof ZAppointmentHit && objb instanceof ZAppointmentHit) )
                return 0;
            ZAppointmentHit a = (ZAppointmentHit) obja;
            ZAppointmentHit b = (ZAppointmentHit) objb;
            if (!a.isAllDay() && b.isAllDay()) return 1;
            if (a.isAllDay() && !b.isAllDay()) return -1;
            if (a.getStartTime() > b.getStartTime()) return 1;
            if (a.getStartTime() < b.getStartTime()) return -1;
            if (a.getDuration() < b.getDuration()) return 1;
            if (a.getDuration() > b.getDuration()) return -1;
            return a.getFolderId().compareTo(b.getFolderId());
            /*
            String na = a.getName() != null ? a.getName() : "";
            String nb = b.getName() != null ? b.getName() : "";
            return na.compareToIgnoreCase(nb);
            */
        }
    }
}
