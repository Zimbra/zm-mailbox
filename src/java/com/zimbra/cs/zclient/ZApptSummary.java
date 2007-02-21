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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.zclient.event.ZModifyEvent;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class ZApptSummary implements ZItem {

    public static final String FBA_FREE = "F";
    public static final String FBA_BUSY = "B";
    public static final String FBA_TENTATIVE = "T";
    public static final String FBA_UNAVAILABLE = "U";

    public static final String TRANSP_OPAQUE = "O";
    public static final String TRANSP_TRANSPARENT = "T";

    public static final String STATUS_TENTATIVE = "TENT";
    public static final String STATUS_CONFIRMED = "CONF";
    public static final String STATUS_CANCELLED = "CANC";

    public static final String PSTATUS_NEEDS_ACTION = "NE";
    public static final String PSTATUS_TENTATIVE = "TE";
    public static final String PSTATUS_ACCEPT = "AC";
    public static final String PSTATUS_DECLINED = "DE";
    public static final String PSTATUS_DELEGATED = "DG";

    public enum Flag {
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
    private String mPartStatus;
    private boolean mIsAllDay;
    private boolean mIsOtherAttendees;
    private boolean mIsAlarm;
    private boolean mIsRecurring;
    private String mName;
    private String mLocation;
    private String mInviteId;
    private String mSeriesInviteId;
    private String mInviteComponentNumber;
    private boolean mIsOrganizer;
    private String mPriority;
    private String mPercentComplete;
    private long mDuration;
    private String mFragment;
    
    private long mStartTime;
    private long mEndTime;
    private long mTimeZoneOffset;
    private boolean mIsException;

    private String mFolderId;

    private ZApptSummary() {

    }
    
    public static void addInstances(Element e, List<ZApptSummary> appts, String folderId, TimeZone timeZone) throws ServiceException {
        String id = e.getAttribute(MailConstants.A_ID);
        String freeBusyActual = e.getAttribute(MailConstants.A_APPT_FREEBUSY_ACTUAL, null);
        String transparency = e.getAttribute(MailConstants.A_APPT_TRANSPARENCY, null);
        String status = e.getAttribute(MailConstants.A_CAL_STATUS, null);
        String pstatus = e.getAttribute(MailConstants.A_CAL_PARTSTAT, null);
        boolean isAllDay = e.getAttributeBool(MailConstants.A_CAL_ALLDAY, false);
        boolean otherAtt = e.getAttributeBool(MailConstants.A_CAL_OTHER_ATTENDEES, false);
        boolean isAlarm = e.getAttributeBool(MailConstants.A_CAL_ALARM, false);
        boolean isRecurring = e.getAttributeBool(MailConstants.A_CAL_RECUR, false);
        String flags = e.getAttribute(MailConstants.A_FLAGS, null);
        String tags = e.getAttribute(MailConstants.A_TAGS, null);
        String name = e.getAttribute(MailConstants.A_NAME, null);
        String location = e.getAttribute(MailConstants.A_CAL_LOCATION, null);
        String inviteId = e.getAttribute(MailConstants.A_CAL_INV_ID, null);
        String inviteCompNumber = e.getAttribute(MailConstants.A_CAL_COMPONENT_NUM, null);
        boolean isOrganizer = e.getAttributeBool(MailConstants.A_CAL_ISORG, false);
        String priority = e.getAttribute(MailConstants.A_CAL_PRIORITY, null);
        String percentComplete = e.getAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, null);
        long duration = e.getAttributeLong(MailConstants.A_CAL_DURATION, 0);

        Element fragmentEl = e.getOptionalElement(MailConstants.E_FRAG);
        String fragment = (fragmentEl != null) ? fragmentEl.getText() : null;

        for (Element inst : e.listElements(MailConstants.E_INSTANCE)) {
            ZApptSummary appt = new ZApptSummary();
            appt.mFolderId = folderId;
            appt.mId = id;

            appt.mIsAllDay = inst.getAttributeBool(MailConstants.A_CAL_ALLDAY, isAllDay);
            appt.mTimeZoneOffset = inst.getAttributeLong(MailConstants.A_CAL_TZ_OFFSET, 0);

            appt.mStartTime = inst.getAttributeLong(MailConstants.A_CAL_START_TIME, 0);

            if (appt.mIsAllDay) {
                long adjustMsecs = appt.mTimeZoneOffset - timeZone.getOffset(appt.mStartTime);
                appt.mStartTime += adjustMsecs;
            }

            appt.mIsException = inst.getAttributeBool(MailConstants.A_CAL_IS_EXCEPTION, false);

            appt.mFreeBusyActual = inst.getAttribute(MailConstants.A_APPT_FREEBUSY_ACTUAL, freeBusyActual);
            appt.mTransparency = inst.getAttribute(MailConstants.A_APPT_TRANSPARENCY, transparency);
            appt.mStatus = inst.getAttribute(MailConstants.A_CAL_STATUS, status);
            appt.mPartStatus = inst.getAttribute(MailConstants.A_CAL_PARTSTAT, pstatus);

            appt.mIsOtherAttendees = inst.getAttributeBool(MailConstants.A_CAL_OTHER_ATTENDEES, otherAtt);
            appt.mIsAlarm = inst.getAttributeBool(MailConstants.A_CAL_ALARM, isAlarm);
            appt.mIsRecurring = inst.getAttributeBool(MailConstants.A_CAL_RECUR, isRecurring);

            appt.mFlags = inst.getAttribute(MailConstants.A_FLAGS, flags);
            appt.mTags = inst.getAttribute(MailConstants.A_TAGS, tags);
            appt.mName = inst.getAttribute(MailConstants.A_NAME, name);
            appt.mLocation = inst.getAttribute(MailConstants.A_CAL_LOCATION, location);

            appt.mInviteId = inviteId;
            appt.mSeriesInviteId = inst.getAttribute(MailConstants.A_CAL_INV_ID, inviteId);

            appt.mInviteComponentNumber = inst.getAttribute(MailConstants.A_CAL_COMPONENT_NUM, inviteCompNumber);
            
            appt.mIsOrganizer = inst.getAttributeBool(MailConstants.A_CAL_ISORG, isOrganizer);
            appt.mPriority = inst.getAttribute(MailConstants.A_CAL_PRIORITY, priority);
            appt.mPercentComplete = inst.getAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, percentComplete);
            appt.mDuration = inst.getAttributeLong(MailConstants.A_CAL_DURATION, duration);

            appt.mEndTime = appt.mStartTime + appt.mDuration;

            Element instFragmentEl = inst.getOptionalElement(MailConstants.E_FRAG);
            appt.mFragment = (instFragmentEl != null) ? instFragmentEl.getText() : fragment;
            appts.add(appt);
        }
    }

    public void modifyNotification(ZModifyEvent event) throws ServiceException {

    }

    public String getId() {
        return mId;
    }

    public String getFolderId() {
        return mFolderId;
    }

    public String toString() {
        ZSoapSB sb = new ZSoapSB();
        sb.beginStruct();
        sb.add("id", mId);
        sb.add("folderId", mFolderId);
        sb.add("tags", mTags);
        sb.add("flags", mFlags);
        sb.add("name", mName);
        sb.add("location", mLocation);
        sb.add("inviteId", mInviteId);
        sb.add("inviteComponentNumber", mInviteComponentNumber);
        sb.add("freeBusyActual", mFreeBusyActual);
        sb.add("transparency", mTransparency);
        sb.add("status", mStatus);
        sb.add("participantStatus", mPartStatus);
        sb.add("allDay", mIsAllDay);
        sb.add("otherAttendees", mIsOtherAttendees);
        sb.add("alarm", mIsAlarm);
        sb.add("recurring", mIsRecurring);
        sb.add("isOrganizer", mIsOrganizer);
        sb.add("priority", mPriority);
        sb.add("percentComplete", mPercentComplete);
        sb.add("duration", mDuration);
        sb.add("startTime", mStartTime);
        sb.add("timeZoneOffset", mTimeZoneOffset);
        sb.add("exception", mIsException);
        sb.add("fragment", mFragment);
        sb.endStruct();
        return sb.toString();
    }

    // fba
    public String getFreeBusyActual() { return mFreeBusyActual; }
    public boolean isFreeBusyActualFree() { return FBA_FREE.equals(mFreeBusyActual); }
    public boolean isFreeBusyActualBusy() { return FBA_BUSY.equals(mFreeBusyActual); }
    public boolean isFreeBusyActualTentative() { return FBA_TENTATIVE.equals(mFreeBusyActual); }
    public boolean isFreeBusyActualUnavailable() { return FBA_UNAVAILABLE.equals(mFreeBusyActual); }

    // transp
    public String getTransparency() { return mTransparency; }
    public boolean isTransparencyOpaque() { return TRANSP_OPAQUE.equals(mTransparency); }
    public boolean isTransparencyTransparent() { return TRANSP_TRANSPARENT.equals(mTransparency); }

    // status
    public String getStatus() { return mStatus; }
    public boolean isStatusTentative() { return STATUS_TENTATIVE.equals(mStatus); }
    public boolean isStatusCancelled() { return STATUS_CANCELLED.equals(mStatus); }
    public boolean isStatusConfirmed() { return STATUS_CONFIRMED.equals(mStatus); }

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

    public String getInviteId() { return mInviteId; }

    public String getInviteComponentNumber() { return mInviteComponentNumber; }

    public boolean isOrganizer() { return mIsOrganizer; }

    public String getPriority() { return mPriority; }

    public String getPercentComplete() { return mPercentComplete; }

    public long getDuration() { return mDuration; }

    public long getStartTime() { return mStartTime; }

    /* computed from start+duration */
    public long getEndTime() { return mEndTime; }

    public Date getStartDate() { return new Date(mStartTime); }

    public Date getEndDate() { return new Date(mEndTime); }

    public long getTimeZoneOffset() { return mTimeZoneOffset; }

    public boolean isException() { return mIsException; }

    public String getFragment() { return mFragment; }

    public String getTagIds() { return mTags; }

    public boolean hasFlags() {
        return mFlags != null && mFlags.length() > 0;
    }

    public boolean hasTags() { return mTags != null && mTags.length() > 0; }

    public boolean hasAttachment() { return hasFlags() && mFlags.indexOf(ZApptSummary.Flag.attachment.getFlagChar()) != -1; }

    public boolean isInRange(long start, long end) {
        return mStartTime < end && mEndTime > start;
    }

    public boolean isOverLapping(ZApptSummary that) {
        return this.mStartTime < that.mEndTime && this.mEndTime > that.mStartTime;
    }

    public boolean isOverLapping(ZApptSummary that, long msecsIncr) {
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
            if (!(obja instanceof ZApptSummary && objb instanceof ZApptSummary) )
                return 0;
            ZApptSummary a = (ZApptSummary) obja;
            ZApptSummary b = (ZApptSummary) objb;
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
