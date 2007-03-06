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

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class ZInvite {

    private List<ZTimeZone> mTimeZones;
    private List<ZComponent> mComponents;
    private Type mType;


    public ZInvite() throws ServiceException {

    }
    
    public ZInvite(Element e) throws ServiceException {
        mTimeZones = new ArrayList<ZTimeZone>();
        mType = Type.fromString(e.getAttribute(MailConstants.A_TYPE, Type.appt.name()));
        for (Element tzEl : e.listElements(MailConstants.E_CAL_TZ)) {
            mTimeZones.add(new ZTimeZone(tzEl));
        }
        mComponents = new ArrayList<ZComponent>();
        for (Element compEl : e.listElements(MailConstants.E_INVITE_COMPONENT)) {
            mComponents.add(new ZComponent(compEl));
        }
    }

    public void setTimeZones(List<ZTimeZone> timeZones) {
        mTimeZones = timeZones;
    }
    
    public List<ZTimeZone> getTimeZones() {
        return mTimeZones;
    }

    public Type getType() {
        return mType;
    }

    ZSoapSB toString(ZSoapSB sb) {
        sb.beginStruct();
        sb.add("type", mType.name());
        sb.add("timezones", mTimeZones, false, false);
        sb.add("components", mComponents, false, false);
        sb.endStruct();
        return sb;
    }

    public String toString() {
        return toString(new ZSoapSB()).toString();
    }

    public enum Type {
        appt, task;

        public static Type fromString(String s) throws ServiceException {
            try {
                return Type.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid type "+s+", valid values: "+ Arrays.asList(Type.values()), e);
            }
        }

        public boolean isTask() { return equals(task); }
        public boolean isAppointment() { return equals(appt); }
    }

    public static class ZComponent {

        private ZStatus mStatus;
        private ZFreeBusyStatus mFreeBusyStatus;
        private ZFreeBusyStatus mActualFreeBusyStatus;
        private ZTransparency mTransparency;
        private boolean mIsAllDay;
        private String mName;
        private String mLocation;
        private boolean mIsOrganizer;
        private long mSequenceNumber;
        private String mPriority;
        private String mPercentCompleted;
        private String mCompleted;
        private List<ZReply> mReplies;
        private ZDateTime mStart;
        private ZDateTime mEnd;
        private ZDuration mDuration;
        private ZOrganizer mOrganizer;
        private List<ZAttendee> mAttendees;

        public ZComponent(Element e) throws ServiceException {
            mStatus = ZStatus.fromString(e.getAttribute(MailConstants.A_CAL_STATUS, ZStatus.CONF.name()));
            mFreeBusyStatus = ZFreeBusyStatus.fromString(e.getAttribute(MailConstants.A_APPT_FREEBUSY, ZFreeBusyStatus.B.name()));
            mActualFreeBusyStatus = ZFreeBusyStatus.fromString(e.getAttribute(MailConstants.A_APPT_FREEBUSY_ACTUAL, ZFreeBusyStatus.B.name()));
            mTransparency = ZTransparency.fromString(e.getAttribute(MailConstants.A_APPT_TRANSPARENCY, "O"));
            mIsAllDay = e.getAttributeBool(MailConstants.A_CAL_ALLDAY, false);
            mName = e.getAttribute(MailConstants.A_NAME, null);
            mLocation = e.getAttribute(MailConstants.A_CAL_LOCATION, null);
            mIsOrganizer = e.getAttributeBool(MailConstants.A_CAL_ISORG, false);
            mSequenceNumber = e.getAttributeLong(MailConstants.A_CAL_SEQUENCE, 0);
            mPriority = e.getAttribute(MailConstants.A_CAL_PRIORITY, "0");
            mPercentCompleted = e.getAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, "0");
            mCompleted = e.getAttribute(MailConstants.A_TASK_COMPLETED, null);
            mReplies = new ArrayList<ZReply>();
            Element repliesEl = e.getOptionalElement(MailConstants.E_CAL_REPLIES);
            if (repliesEl != null) {
                for (Element replyEl : repliesEl.listElements(MailConstants.E_CAL_REPLY)) {
                    mReplies.add(new ZReply(replyEl));
                }
            }
            mStart = new ZDateTime(e.getElement(MailConstants.E_CAL_START_TIME));
            Element endEl = e.getOptionalElement(MailConstants.E_CAL_END_TIME);
            if (endEl != null)
                mEnd = new ZDateTime(endEl);
            Element durEl = e.getOptionalElement(MailConstants.E_CAL_DURATION);
            if (durEl != null)
                mDuration = new ZDuration(durEl);
            Element orEl = e.getOptionalElement(MailConstants.E_CAL_ORGANIZER);
            if (orEl != null)
                mOrganizer = new ZOrganizer(orEl);
            mAttendees = new ArrayList<ZAttendee>();
            for (Element attendeeEl : e.listElements(MailConstants.E_CAL_ATTENDEE)) {
                mAttendees.add(new ZAttendee(attendeeEl));
            }

        }

        public ZStatus getStatus() {
            return mStatus;
        }

        public void setStatus(ZStatus status) {
            mStatus = status;
        }

        public ZFreeBusyStatus getFreeBusyStatus() {
            return mFreeBusyStatus;
        }

        public void setFreeBusyStatus(ZFreeBusyStatus freeBusyStatus) {
            mFreeBusyStatus = freeBusyStatus;
        }

        public ZFreeBusyStatus getActualFreeBusyStatus() {
            return mActualFreeBusyStatus;
        }

        public void setActualFreeBusyStatus(ZFreeBusyStatus actualFreeBusyStatus) {
            mActualFreeBusyStatus = actualFreeBusyStatus;
        }

        public ZTransparency getTransparency() {
            return mTransparency;
        }

        public void setTransparency(ZTransparency transparency) {
            mTransparency = transparency;
        }

        public boolean isIsAllDay() {
            return mIsAllDay;
        }

        public void setIsAllDay(boolean isAllDay) {
            mIsAllDay = isAllDay;
        }

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            mName = name;
        }

        public String getLocation() {
            return mLocation;
        }

        public void setLocation(String location) {
            mLocation = location;
        }

        public boolean isIsOrganizer() {
            return mIsOrganizer;
        }

        public void setIsOrganizer(boolean isOrganizer) {
            mIsOrganizer = isOrganizer;
        }

        public long getSequenceNumber() {
            return mSequenceNumber;
        }

        public void setSequenceNumber(long sequenceNumber) {
            mSequenceNumber = sequenceNumber;
        }

        public String getPriority() {
            return mPriority;
        }

        public void setPriority(String priority) {
            mPriority = priority;
        }

        public String getPercentCompleted() {
            return mPercentCompleted;
        }

        public void setPercentCompleted(String percentCompleted) {
            mPercentCompleted = percentCompleted;
        }

        public String getCompleted() {
            return mCompleted;
        }

        public void setCompleted(String completed) {
            mCompleted = completed;
        }

        public List<ZReply> getReplies() {
            return mReplies;
        }

        public void setReplies(List<ZReply> replies) {
            mReplies = replies;
        }

        public ZDateTime getStart() {
            return mStart;
        }

        public void setStart(ZDateTime start) {
            mStart = start;
        }

        public ZDateTime getEnd() {
            return mEnd;
        }

        public void setEnd(ZDateTime end) {
            mEnd = end;
        }

        public ZDuration getDuration() {
            return mDuration;
        }

        public void setDuration(ZDuration duration) {
            mDuration = duration;
        }

        public ZOrganizer getOrganizer() {
            return mOrganizer;
        }

        public void setOrganizer(ZOrganizer organizer) {
            mOrganizer = organizer;
        }

        public List<ZAttendee> getAttendees() {
            return mAttendees;
        }

        public void setAttendees(List<ZAttendee> attendees) {
            mAttendees = attendees;
        }

        ZSoapSB toString(ZSoapSB sb) {
            sb.beginStruct();
            sb.add("status", mStatus.name());
            sb.add("freeBusyStatus", mFreeBusyStatus.name());
            sb.add("actualFreeBusyStatus", mActualFreeBusyStatus.name());
            sb.add("transparency", mTransparency.name());
            sb.add("isAllDay", mIsAllDay);
            sb.add("name", mName);
            sb.add("locaiton", mLocation);
            sb.add("isOrganizer", mIsOrganizer);
            sb.add("sequenceNumber", mSequenceNumber);
            sb.add("priority", mPriority);
            sb.add("percentCompleted", mPercentCompleted);
            sb.add("completed", mCompleted);
            sb.add("replies", mReplies, false, false);
            if (mStart != null) sb.addStruct("start", mStart.toString());
            if (mEnd != null) sb.addStruct("end", mEnd.toString());
            if (mDuration != null) sb.addStruct("duration", mDuration.toString());
            sb.addStruct("organizer", mOrganizer.toString());
            sb.add("attendees", mAttendees, false, false);
            sb.endStruct();
            return sb;
        }

        public String toString() {
            return toString(new ZSoapSB()).toString();
        }

        public static class ZReply {

            private long mDate;
            private String mAttendee;
            private ZParticipantStatus mParticipantStatus;
            private int mRangeType;
            private String mRecurrenceId;
            private String mTimeZone;

            public ZReply() {

            }
            
            public ZReply(Element e) throws ServiceException {
                mDate = e.getAttributeLong(MailConstants.A_DATE, 0);
                mAttendee = e.getAttribute(MailConstants.A_CAL_ATTENDEE, null);
                mParticipantStatus = ZParticipantStatus.fromString(e.getAttribute(MailConstants.A_CAL_PARTSTAT, ZParticipantStatus.AC.name()));
                mRangeType = (int) e.getAttributeLong(MailConstants.A_CAL_RECURRENCE_RANGE_TYPE, 0);
                mRecurrenceId = e.getAttribute(MailConstants.A_CAL_RECURRENCE_ID, null);
                mTimeZone = e.getAttribute(MailConstants.A_CAL_TIMEZONE, null);
            }

            public long getDate() {
                return mDate;
            }

            public void setDate(long date) {
                mDate = date;
            }

            public String getAttendee() {
                return mAttendee;
            }

            public void setAttendee(String attendee) {
                mAttendee = attendee;
            }

            public ZParticipantStatus getParticipantStatus() {
                return mParticipantStatus;
            }

            public void setParticipantStatus(ZParticipantStatus participantStatus) {
                mParticipantStatus = participantStatus;
            }

            public int getRangeType() {
                return mRangeType;
            }

            public void setRangeType(int rangeType) {
                mRangeType = rangeType;
            }

            public String getRecurrenceId() {
                return mRecurrenceId;
            }

            public void setRecurrenceId(String recurrenceId) {
                mRecurrenceId = recurrenceId;
            }

            public String getTimeZone() {
                return mTimeZone;
            }

            public void setTimeZone(String timeZone) {
                mTimeZone = timeZone;
            }

        }

    }

    public static class ZTimeZone {

        private String mId;
        private long mStandardOffset;
        private long mDaylightSavingsOffset;
        private ZTransitionRule mStandard;
        private ZTransitionRule mDaylight;

        public ZTimeZone() {

        }
        
        public ZTimeZone(Element e) throws ServiceException {
            mId = e.getAttribute(MailConstants.A_ID);
            mStandardOffset = e.getAttributeLong(MailConstants.A_CAL_TZ_STDOFFSET, 0);
            mDaylightSavingsOffset = e.getAttributeLong(MailConstants.A_CAL_TZ_DAYOFFSET, -1);
            Element standardEl = e.getOptionalElement(MailConstants.E_CAL_TZ_STANDARD);
            if (standardEl != null)
                mStandard = new ZTransitionRule(standardEl);
            Element daylightEl = e.getOptionalElement(MailConstants.E_CAL_TZ_STANDARD);
            if (daylightEl != null)
                mDaylight = new ZTransitionRule(daylightEl);
        }

        /**
         * @return daylight savings offset, or -1 if not present.
         *
         */
        public long getDaylightSavingsOffset() {
            return mDaylightSavingsOffset;
        }

        public long getStandardOffset() {
            return mStandardOffset;
        }

        public String getId() {
            return mId;
        }

        /**
         *
         * @return rule, or NULL is no DST
         */
        public ZTransitionRule getStandardRule() {
            return mStandard;
        }

        /**
         *
         * @return rule, or NULL if no DST
         *
         */
        public ZTransitionRule getDaylightRule() {
            return mDaylight;
        }

        public void setId(String id) {
            mId = id;
        }

        public void setStandardOffset(long standardOffset) {
            mStandardOffset = standardOffset;
        }

        public void setDaylightSavingsOffset(long daylightSavingsOffset) {
            mDaylightSavingsOffset = daylightSavingsOffset;
        }

        public void setStandard(ZTransitionRule standard) {
            mStandard = standard;
        }

        public void setDaylight(ZTransitionRule daylight) {
            mDaylight = daylight;
        }

        ZSoapSB toString(ZSoapSB sb) {
            sb.beginStruct();
            sb.add("id", mId);
            sb.add("standardOffset", mStandardOffset);
            sb.add("daylightOffset", mDaylightSavingsOffset);
            if (mStandard != null)
                sb.addStruct("standardRule", mStandard.toString());
            if (mDaylight != null)
                sb.addStruct("daylightRule", mDaylight.toString());
            sb.endStruct();
            return sb;
        }

        public String toString() {
            return toString(new ZSoapSB()).toString();
        }

        public static class ZTransitionRule {
            private int mWeek;
            private int mDayOfWeek;
            private int mMonth;
            private int mDayOfMonth;
            private int mHour;
            private int mMinute;
            private int mSecond;

            public ZTransitionRule() {

            }
            
            public ZTransitionRule(Element e) throws ServiceException {
                mWeek = (int) e.getAttributeLong(MailConstants.A_CAL_TZ_WEEK,  0);
                mDayOfWeek = (int) e.getAttributeLong(MailConstants.A_CAL_TZ_DAYOFWEEK,  0);
                mDayOfMonth = (int) e.getAttributeLong(MailConstants.A_CAL_TZ_DAYOFMONTH,  0);
                mMonth = (int) e.getAttributeLong(MailConstants.A_CAL_TZ_MONTH,  0);
                mHour = (int) e.getAttributeLong(MailConstants.A_CAL_TZ_HOUR,  0);
                mMinute = (int) e.getAttributeLong(MailConstants.A_CAL_TZ_MINUTE,  0);
                mSecond = (int) e.getAttributeLong(MailConstants.A_CAL_TZ_SECOND,  0);
            }

            public int getWeek() {
                return mWeek;
            }

            public void setWeek(int week) {
                mWeek = week;
            }

            public int getDayOfWeek() {
                return mDayOfWeek;
            }

            public void setDayOfWeek(int dayOfWeek) {
                mDayOfWeek = dayOfWeek;
            }

            public int getMonth() {
                return mMonth;
            }

            public void setMonth(int month) {
                mMonth = month;
            }

            public int getDayOfMonth() {
                return mDayOfMonth;
            }

            public void setDayOfMonth(int dayOfMonth) {
                mDayOfMonth = dayOfMonth;
            }

            public int getHour() {
                return mHour;
            }

            public void setHour(int hour) {
                mHour = hour;
            }

            public int getMinute() {
                return mMinute;
            }

            public void setMinute(int minute) {
                mMinute = minute;
            }

            public int getSecond() {
                return mSecond;
            }

            public void setSecond(int second) {
                mSecond = second;
            }

            ZSoapSB toString(ZSoapSB sb) {
                sb.beginStruct();
                sb.add("week", mWeek);
                sb.add("dayOfWeek", mDayOfWeek);
                sb.add("dayOfMonth", mDayOfMonth);
                sb.add("month", mMonth);
                sb.add("hour", mHour);
                sb.add("minute", mMinute);
                sb.add("second", mSecond);
                sb.endStruct();
                return sb;
            }

            public String toString() {
                return toString(new ZSoapSB()).toString();
            }
        }
    }

    public static class ZDateTime {

        private String mDateTime;
        private String mTimeZoneId;

        public ZDateTime() {

        }
        
        public ZDateTime(Element e) throws ServiceException {
            mDateTime = e.getAttribute(MailConstants.A_CAL_DATETIME);
            mTimeZoneId = e.getAttribute(MailConstants.A_CAL_TIMEZONE, null);
        }

        public String getDateTime() {
            return mDateTime;
        }

        public void setDateTime(String dateTime) {
            mDateTime = dateTime;
        }

        public String getTimeZoneId() {
            return mTimeZoneId;
        }

        public void setTimeZoneId(String timeZoneId) {
            mTimeZoneId = timeZoneId;
        }

        ZSoapSB toString(ZSoapSB sb) {
            sb.beginStruct();
            sb.add("dateTime", mDateTime);
            sb.add("timeZoneId", mTimeZoneId);
            sb.endStruct();
            return sb;
        }

        public String toString() {
            return toString(new ZSoapSB()).toString();
        }

    }

    public static class ZDuration {

        private boolean mNegative;
        private int mWeeks;
        private int mDays;
        private int mHours;
        private int mMinutes;
        private int mSeconds;

        public ZDuration() {

        }
        public ZDuration(Element e) throws ServiceException {
            mNegative = e.getAttributeBool(MailConstants.A_CAL_DURATION_NEGATIVE, false);
            mWeeks = (int) e.getAttributeLong(MailConstants.A_CAL_DURATION_WEEKS, 0);
            mDays= (int) e.getAttributeLong(MailConstants.A_CAL_DURATION_DAYS, 0);
            mHours= (int) e.getAttributeLong(MailConstants.A_CAL_DURATION_HOURS, 0);
            mMinutes = (int) e.getAttributeLong(MailConstants.A_CAL_DURATION_MINUTES, 0);
            mSeconds = (int) e.getAttributeLong(MailConstants.A_CAL_DURATION_SECONDS, 0);
        }

        public boolean isNegative() {
            return mNegative;
        }

        public void setNegative(boolean negative) {
            mNegative = negative;
        }

        public int getWeeks() {
            return mWeeks;
        }

        public void setWeeks(int weeks) {
            mWeeks = weeks;
        }

        public int getDays() {
            return mDays;
        }

        public void setDays(int days) {
            mDays = days;
        }

        public int getHours() {
            return mHours;
        }

        public void setHours(int hours) {
            mHours = hours;
        }

        public int getMinutes() {
            return mMinutes;
        }

        public void setMinutes(int minutes) {
            mMinutes = minutes;
        }

        public int getSeconds() {
            return mSeconds;
        }

        public void setSeconds(int seconds) {
            mSeconds = seconds;
        }

        ZSoapSB toString(ZSoapSB sb) {
            sb.beginStruct();
            sb.add("negative", mNegative);
            sb.add("weeks", mWeeks);
            sb.add("days", mDays);
            sb.add("hours", mHours);
            sb.add("minutes", mMinutes);
            sb.add("seconds", mSeconds);
            sb.endStruct();
            return sb;
        }

        public String toString() {
            return toString(new ZSoapSB()).toString();
        }

    }

    public enum ZStatus {
        TENT, CONF, CANC, NEED, COMP, INPR, WAITING, DEFERRED;

        public static ZStatus fromString(String s) throws ServiceException {
            try {
                return ZStatus.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid status "+s+", valid values: "+ Arrays.asList(ZStatus.values()), e);
            }
        }

        public boolean isTentative() { return equals(TENT); }
        public boolean isConfirmed() { return equals(CONF); }
        public boolean isCancelled() { return equals(CANC); }
        public boolean isNeedsAction() { return equals(NEED); }
        public boolean isCompleted() { return equals(COMP); }
        public boolean isInProgress() { return equals(INPR); }
        public boolean isWaiting() { return equals(WAITING); }
        public boolean isDeferred() { return equals(DEFERRED); }
    }

    public enum ZFreeBusyStatus {
        F, B, T, O;

        public static ZFreeBusyStatus fromString(String s) throws ServiceException {
            try {
                return ZFreeBusyStatus.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid free busy status "+s+", valid values: "+ Arrays.asList(ZFreeBusyStatus.values()), e);
            }
        }

        public boolean isFree() { return equals(F); }
        public boolean isBusy() { return equals(B); }
        public boolean isTentative() { return equals(T); }
        public boolean isOutOfOffice() { return equals(O); }
    }

    public enum ZParticipantStatus {
        AC, DE, TE, NE, DG;

        public static ZParticipantStatus fromString(String s) throws ServiceException {
            try {
                return ZParticipantStatus.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid participant status "+s+", valid values: "+ Arrays.asList(ZParticipantStatus.values()), e);
            }
        }

        public boolean isAccepted() { return equals(AC); }
        public boolean isDeclined() { return equals(DE); }
        public boolean isTentative() { return equals(TE); }
        public boolean isNew() { return equals(NE); }
        public boolean isDelegated() { return equals(DG); }
    }

    public enum ZTransparency {
        O, T;

        public static ZTransparency fromString(String s) throws ServiceException {
            try {
                return ZTransparency.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid transparency "+s+", valid values: "+ Arrays.asList(ZTransparency.values()), e);
            }
        }

        public boolean isTransparent() { return equals(T); }
        public boolean isOpaque() { return equals(O); }
    }

    public enum ZRole {
        CHA, REQ, OPT, NON;

        public static ZRole fromString(String s) throws ServiceException {
            try {
                return ZRole.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid role "+s+", valid values: "+ Arrays.asList(ZRole.values()), e);
            }
        }

        public boolean isChair() { return equals(CHA); }
        public boolean isRequired() { return equals(REQ); }
        public boolean isOptional() { return equals(OPT); }
        public boolean isNonParticipant() { return equals(NON); }
    }

    public enum ZCalendarUserType {
        IND, GRO, RES, ROO, UNK;

        public static ZCalendarUserType fromString(String s) throws ServiceException {
            try {
                return ZCalendarUserType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid role "+s+", valid values: "+ Arrays.asList(ZCalendarUserType.values()), e);
            }
        }

        public boolean isIndividual() { return equals(IND); }
        public boolean isGroup() { return equals(GRO); }
        public boolean isResource() { return equals(RES); }
        public boolean isRoom() { return equals(ROO); }
        public boolean isUnknown() { return equals(UNK); }
    }

    public static class ZCalendarUser {
        private String mAddress;
        private String mUrl;
        private String mPersonalName;
        private String mSentBy;
        private String mDir;
        private String mLanguage;

        public ZCalendarUser() {

        }
        
        public ZCalendarUser(Element e) throws ServiceException {
            mAddress = e.getAttribute(MailConstants.A_ADDRESS, null);
            mUrl = e.getAttribute(MailConstants.A_URL, null);
            mPersonalName = e.getAttribute(MailConstants.A_DISPLAY, null);
            mSentBy = e.getAttribute(MailConstants.A_CAL_SENTBY, null);
            mDir = e.getAttribute(MailConstants.A_CAL_DIR, null);
            mLanguage = e.getAttribute(MailConstants.A_CAL_LANGUAGE, null);
        }

        public String getAddress() {
            return mAddress;
        }

        public void setAddress(String address) {
            mAddress = address;
        }

        public String getUrl() {
            return mUrl;
        }

        public void setUrl(String url) {
            mUrl = url;
        }

        public String getPersonalName() {
            return mPersonalName;
        }

        public void setPersonalName(String personalName) {
            mPersonalName = personalName;
        }

        public String getSentBy() {
            return mSentBy;
        }

        public void setSentBy(String sentBy) {
            mSentBy = sentBy;
        }

        public String getDir() {
            return mDir;
        }

        public void setDir(String dir) {
            mDir = dir;
        }

        public String getLanguage() {
            return mLanguage;
        }

        public void setLanguage(String language) {
            mLanguage = language;
        }

        void toString(ZSoapSB sb) {
            sb.add("address", mAddress);
            sb.add("url", mUrl);
            sb.add("personalName", mPersonalName);
            sb.add("sentBy", mSentBy);
            sb.add("dir", mDir);
            sb.add("language", mLanguage);
        }

        public String toString() {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            toString(sb);
            sb.endStruct();
            return sb.toString();
        }
    }

    public static class ZOrganizer extends ZCalendarUser {

        public ZOrganizer() {
            super();
        }

        public ZOrganizer(Element e) throws ServiceException {
            super(e);
        }
    }

    public static class ZAttendee extends ZCalendarUser {
        private ZRole mRole;
        private ZParticipantStatus mParticipantStatus;
        private boolean mRSVP;
        private ZCalendarUserType mCalendarUserType;
        private String mMember;
        private String mDelegatedTo;
        private String mDelegatedFrom;

        public ZAttendee() {

        }

        public ZAttendee(Element e) throws ServiceException {
            super(e);
            mRole = ZRole.fromString(e.getAttribute(MailConstants.A_CAL_ROLE, ZRole.OPT.name()));
            mParticipantStatus = ZParticipantStatus.fromString(e.getAttribute(MailConstants.A_CAL_PARTSTAT, ZParticipantStatus.AC.name()));
            mRSVP = e.getAttributeBool(MailConstants.A_CAL_RSVP, false);
            mCalendarUserType = ZCalendarUserType.fromString(e.getAttribute(MailConstants.A_CAL_CUTYPE, ZCalendarUserType.UNK.name()));
            mMember = e.getAttribute(MailConstants.A_CAL_MEMBER, null);
            mDelegatedTo = e.getAttribute(MailConstants.A_CAL_DELEGATED_TO, null);
            mDelegatedFrom = e.getAttribute(MailConstants.A_CAL_DELEGATED_FROM, null);
        }

        public ZRole getRole() {
            return mRole;
        }

        public void setRole(ZRole role) {
            mRole = role;
        }

        public ZParticipantStatus getParticipantStatus() {
            return mParticipantStatus;
        }

        public void setParticipantStatus(ZParticipantStatus participantStatus) {
            mParticipantStatus = participantStatus;
        }

        public boolean isRSVP() {
            return mRSVP;
        }

        public void setRSVP(boolean RSVP) {
            mRSVP = RSVP;
        }

        public ZCalendarUserType getCalendarUserType() {
            return mCalendarUserType;
        }

        public void setCalendarUserType(ZCalendarUserType calendarUserType) {
            mCalendarUserType = calendarUserType;
        }

        public String getMember() {
            return mMember;
        }

        public void setMember(String member) {
            mMember = member;
        }

        public String getDelegatedTo() {
            return mDelegatedTo;
        }

        public void setDelegatedTo(String delegatedTo) {
            mDelegatedTo = delegatedTo;
        }

        public String getDelegatedFrom() {
            return mDelegatedFrom;
        }

        public void setDelegatedFrom(String delegatedFrom) {
            mDelegatedFrom = delegatedFrom;
        }

        void toString(ZSoapSB sb) {
            super.toString(sb);
            
        }

        public String toString() {
            ZSoapSB sb = new ZSoapSB();
            sb.beginStruct();
            toString(sb);
            sb.add("role", mRole.name());
            sb.add("participantStatus", mParticipantStatus.name());
            sb.add("rsvp", mRSVP);
            sb.add("calendarUserType", mCalendarUserType.name());
            sb.add("member", mMember);
            sb.add("delegatedFrom", mDelegatedFrom);
            sb.add("delegatedTo", mDelegatedTo);
            sb.endStruct();
            return sb.toString();
        }
    }
}
