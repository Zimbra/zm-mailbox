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
import com.zimbra.common.util.ListUtil;
import com.zimbra.common.zclient.ZClientException;
import com.zimbra.cs.mailbox.calendar.Geo;
import com.zimbra.cs.mailbox.calendar.ZCalendar.ZParameter;
import com.zimbra.cs.service.mail.CalendarUtils;
import com.zimbra.cs.service.mail.ToXML;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ZInvite implements ToZJSONObject {

    private List<ZTimeZone> mTimeZones;
    private List<ZComponent> mComponents;
    private ZInviteType mType;

    public ZInvite() {
        mTimeZones = new ArrayList<ZTimeZone>();
        mComponents = new ArrayList<ZComponent>();
    }
    
    public ZInvite(Element e) throws ServiceException {
        mTimeZones = new ArrayList<ZTimeZone>();
        mType = ZInviteType.fromString(e.getAttribute(MailConstants.A_TYPE, ZInviteType.appt.name()));
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

    public void setType(ZInviteType type) {
        mType = type;
    }
    
    public ZInviteType getType() {
        return mType;
    }

    public void setComponents(List<ZComponent> components) {
        mComponents = components;
    }

    public List<ZComponent> getComponents() {
        return mComponents;
    }

    public ZComponent getComponent() {
        return (mComponents == null || mComponents.isEmpty()) ? null : mComponents.get(0);
    }

    public boolean getHasAcceptableComponent(){
        if (ListUtil.isEmpty(mComponents)) return false;
        for (int i = 0 ; i < mComponents.size(); i++) {
            if (mComponents.get(i) != null && !mComponents.get(i).getStatus().isCancelled()) {
                return true;
            }
        }
        return false;
    }

    public boolean getHasInviteReplyMethod(){
        if(getComponent() != null && (getComponent().getMethod().isRequest() || getComponent().getMethod().isPublish())){
            return true;
        }
        return false;
    }

    public Element toElement(Element parent) {
        Element invEl = parent.addElement(MailConstants.E_INVITE);
        for (ZTimeZone tz : mTimeZones)
            tz.toElement(invEl);
        for (ZComponent comp : mComponents)
            comp.toElement(invEl);
        return invEl;
    }

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("type", mType.name());
        zjo.put("timezones", mTimeZones);
        zjo.put("components", mComponents);
        return zjo;
    }

    public String toString() {
        return String.format("[ZInvite %s]", getComponent().getName());
    }

    public String dump() {
        return ZJSONObject.toString(this);
    }

    public enum ZInviteType {
        appt, task;

        public static ZInviteType fromString(String s) throws ServiceException {
            try {
                return ZInviteType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid type "+s+", valid values: "+ Arrays.asList(ZInviteType.values()), e);
            }
        }

        public boolean isTask() { return equals(task); }
        public boolean isAppointment() { return equals(appt); }
    }

    public static class ZComponent implements ToZJSONObject {

        private ZStatus mStatus;
        private ZClass mClass;
        private ZFreeBusyStatus mFreeBusyStatus;
        private ZFreeBusyStatus mActualFreeBusyStatus;
        private ZTransparency mTransparency;
        private boolean mIsAllDay;
        private String mName;
        private String mLocation;
        private List<String> mCategories;
        private List<String> mComments;
        private List<String> mContacts;
        private Geo mGeo;
        private String mUrl;
        private boolean mIsException;
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
        private ZRecurrence mRecurrence;
        private String mComponentNum;
        private List<ZAlarm> mAlarms;
        private String mRecurrenceIdZ;
        private String mDescription;
        private String mDescriptionHtml;
        private ZMethod mMethod;
        private boolean mIsNoBlob;

        public ZComponent() {
            mStatus = ZStatus.CONF;
            mClass = ZClass.PUB;
            mFreeBusyStatus = ZFreeBusyStatus.B;
            mTransparency = ZTransparency.O;
            mReplies = new ArrayList<ZReply>();
            mAttendees = new ArrayList<ZAttendee>();
            mAlarms = new ArrayList<ZAlarm>();
        }
        
        public ZComponent(Element e) throws ServiceException {
            mStatus = ZStatus.fromString(e.getAttribute(MailConstants.A_CAL_STATUS, ZStatus.CONF.name()));
            mClass = ZClass.fromString(e.getAttribute(MailConstants.A_CAL_CLASS, ZClass.PUB.name()));
            mFreeBusyStatus = ZFreeBusyStatus.fromString(e.getAttribute(MailConstants.A_APPT_FREEBUSY, ZFreeBusyStatus.B.name()));
            mActualFreeBusyStatus = ZFreeBusyStatus.fromString(e.getAttribute(MailConstants.A_APPT_FREEBUSY_ACTUAL, ZFreeBusyStatus.B.name()));
            mTransparency = ZTransparency.fromString(e.getAttribute(MailConstants.A_APPT_TRANSPARENCY, "O"));
            mIsAllDay = e.getAttributeBool(MailConstants.A_CAL_ALLDAY, false);
            mName = e.getAttribute(MailConstants.A_NAME, null);
            mLocation = e.getAttribute(MailConstants.A_CAL_LOCATION, null);
            mMethod = ZMethod.fromString(e.getAttribute(MailConstants.A_CAL_METHOD, ZMethod.PUBLISH.name()));

            Iterator<Element> catIter = e.elementIterator(MailConstants.E_CAL_CATEGORY);
            if (catIter.hasNext()) {
                List<String> categories = new ArrayList<String>();
                for (; catIter.hasNext(); ) {
                    String cat = catIter.next().getTextTrim();
                    categories.add(cat);
                }
                mCategories = categories;
            }
            Iterator<Element> cmtIter = e.elementIterator(MailConstants.E_CAL_COMMENT);
            if (cmtIter.hasNext()) {
                List<String> comments = new ArrayList<String>();
                for (; cmtIter.hasNext(); ) {
                    String cmt = cmtIter.next().getTextTrim();
                    comments.add(cmt);
                }
                mComments = comments;
            }
            Iterator<Element> cnIter = e.elementIterator(MailConstants.E_CAL_CONTACT);
            if (cnIter.hasNext()) {
                List<String> contacts = new ArrayList<String>();
                for (; cnIter.hasNext(); ) {
                    String cn = cnIter.next().getTextTrim();
                    contacts.add(cn);
                }
                mContacts = contacts;
            }
            Element geoElem = e.getOptionalElement(MailConstants.E_CAL_GEO);
            if (geoElem != null)
                mGeo = Geo.parse(geoElem);
            mUrl = e.getAttribute(MailConstants.A_CAL_URL, null);

            mIsException = e.getAttributeBool(MailConstants.A_CAL_IS_EXCEPTION, false);
            mIsOrganizer = e.getAttributeBool(MailConstants.A_CAL_ISORG, false);
            mSequenceNumber = e.getAttributeLong(MailConstants.A_CAL_SEQUENCE, 0);
            mPriority = e.getAttribute(MailConstants.A_CAL_PRIORITY, "0");
            mPercentCompleted = e.getAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, "0");
            mCompleted = e.getAttribute(MailConstants.A_TASK_COMPLETED, null);
            mComponentNum = e.getAttribute(MailConstants.A_CAL_COMPONENT_NUM, "0");
            mReplies = new ArrayList<ZReply>();
            mAlarms = new ArrayList<ZAlarm>();
            
            Element repliesEl = e.getOptionalElement(MailConstants.E_CAL_REPLIES);
            if (repliesEl != null) {
                for (Element replyEl : repliesEl.listElements(MailConstants.E_CAL_REPLY)) {
                    mReplies.add(new ZReply(replyEl));
                }
            }
            Element startEl = e.getOptionalElement(MailConstants.E_CAL_START_TIME);
            if (startEl != null)
                mStart = new ZDateTime(startEl);

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

            mRecurrenceIdZ = e.getAttribute(MailConstants.A_CAL_RECURRENCE_ID_Z, null);

            Element recurEl = e.getOptionalElement(MailConstants.E_CAL_RECUR);
            if (recurEl != null)
                mRecurrence = new ZRecurrence(recurEl);
            Element alarmEl = e.getOptionalElement(MailConstants.A_CAL_ALARM);
            if (alarmEl != null) {
                for (Element alarm : e.listElements(MailConstants.A_CAL_ALARM)) {
                    mAlarms.add(new ZAlarm(alarm));
                }
            }

            mIsNoBlob = e.getAttributeBool(MailConstants.A_CAL_NO_BLOB, false);

            Element descEl = e.getOptionalElement(MailConstants.E_CAL_DESCRIPTION);
            mDescription = descEl != null ? descEl.getText() : null;

            Element descHtmlElem = e.getOptionalElement(MailConstants.E_CAL_DESC_HTML);
            mDescriptionHtml = descHtmlElem != null ? descHtmlElem.getText() : null;

            
        }


        public Element toElement(Element parent) {
            Element compEl = parent.addElement(MailConstants.E_INVITE_COMPONENT);
            if (mStatus != null) compEl.addAttribute(MailConstants.A_CAL_STATUS, mStatus.name());
            if (mClass != null) compEl.addAttribute(MailConstants.A_CAL_CLASS, mClass.name());
            if (mFreeBusyStatus != null) compEl.addAttribute(MailConstants.A_APPT_FREEBUSY, mFreeBusyStatus.name());
            if (mActualFreeBusyStatus != null) compEl.addAttribute(MailConstants.A_APPT_FREEBUSY_ACTUAL, mActualFreeBusyStatus.name());
            if (mTransparency != null) compEl.addAttribute(MailConstants.A_APPT_TRANSPARENCY, mTransparency.name());
            if (mIsAllDay) compEl.addAttribute(MailConstants.A_CAL_ALLDAY, mIsAllDay);
            if (mName != null) compEl.addAttribute(MailConstants.A_NAME, mName);
            if (mLocation != null) compEl.addAttribute(MailConstants.A_CAL_LOCATION, mLocation);
            if (mCategories != null) {
                for (String cat : mCategories) {
                    compEl.addElement(MailConstants.E_CAL_CATEGORY).setText(cat);
                }
            }
            if (mComments != null) {
                for (String cmt : mComments) {
                    compEl.addElement(MailConstants.E_CAL_COMMENT).setText(cmt);
                }
            }
            if (mContacts != null) {
                for (String cn : mContacts) {
                    compEl.addElement(MailConstants.E_CAL_CONTACT).setText(cn);
                }
            }
            if (mGeo != null)
                mGeo.toXml(compEl);
            if (mUrl != null)
                compEl.addAttribute(MailConstants.A_CAL_URL, mUrl);

            if (mIsOrganizer) compEl.addAttribute(MailConstants.A_CAL_ISORG, mIsOrganizer);
            if (mSequenceNumber > 0) compEl.addAttribute(MailConstants.A_CAL_SEQUENCE, mSequenceNumber);
            if (mPriority != null) compEl.addAttribute(MailConstants.A_CAL_PRIORITY, mPriority);
            if (mPercentCompleted != null)  compEl.addAttribute(MailConstants.A_TASK_PERCENT_COMPLETE, mPercentCompleted);
            if (mCompleted != null)  compEl.addAttribute(MailConstants.A_TASK_COMPLETED, mCompleted);
            if (mMethod != null)  compEl.addAttribute(MailConstants.A_CAL_METHOD, mMethod.name());


            if (mReplies != null && !mReplies.isEmpty()) {
                Element repliesEl = compEl.addElement(MailConstants.E_CAL_REPLIES);
                for (ZReply reply : mReplies) {
                    reply.toElement(repliesEl);
                }
            }

            if (mStart != null)
                mStart.toElement(MailConstants.E_CAL_START_TIME, compEl);
            if (mEnd != null)
                mEnd.toElement(MailConstants.E_CAL_END_TIME, compEl);
            else if (mDuration != null)
                mDuration.toElement(compEl);

            if (mOrganizer != null)
                    mOrganizer.toElement(compEl);
            
            if (mAttendees != null && !mAttendees.isEmpty()) {
                for (ZAttendee attendee : mAttendees)
                    attendee.toElement(compEl);
            }

            if (mRecurrence != null)
                mRecurrence.toElement(compEl);

            for (ZAlarm alarm : mAlarms)
                alarm.toElement(compEl);
            
            return compEl;
        }

        public ZRecurrence getRecurrence() {
            return mRecurrence;
        }

        public ZSimpleRecurrence getSimpleRecurrence() {
            return new ZSimpleRecurrence(mRecurrence);
        }

        public void setRecurrence(ZRecurrence recurrence) {
            mRecurrence = recurrence;
        }

        public ZStatus getStatus() {
            return mStatus;
        }

        public void setStatus(ZStatus status) {
            mStatus = status;
        }

        public ZMethod getMethod() {
            return mMethod;
        }

        public void setMethod(ZMethod method) {
            mMethod =  method;
        }

        public ZClass getClassProp() {
            return mClass;
        }

        public void setClassProp(ZClass cl) {
            mClass = cl;
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

        public boolean isAllDay() {
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

        public List<String> getCategories() {
            return mCategories;
        }

        public void setCategories(List<String> categories) {
            mCategories = categories;
        }

        public List<String> getComments() {
            return mComments;
        }

        public void setComments(List<String> comments) {
            mComments = comments;
        }

        public List<String> getContacts() {
            return mContacts;
        }

        public void setContacts(List<String> contacts) {
            mContacts = contacts;
        }

        public Geo getGeo() {
            return mGeo;
        }

        public void setGeo(Geo geo) {
            mGeo = geo;
        }

        public String getUrl() {
            return mUrl;
        }

        public void setUrl(String url) {
            mUrl = url;
        }

        public boolean isException() {
            return mIsException;
        }

        public boolean  getIsOrganizer() {
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

        public Date getComputedEndDate() {
          if (getEnd() != null)
              return getEnd().getDate();
          else if (getDuration() != null)
			  return getDuration().addToDate(getStart().getDate());
		  else // simply return the start date
			  return getStart().getDate();
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

        public void setComponentNumber(String compNum) {
            mComponentNum = compNum;
        }

        public String getComponentNumber() {
            return mComponentNum;
        }

        public List<ZAlarm> getAlarms(){
            return mAlarms;
        }

        public void setAlarms(List <ZAlarm> alarms){
            this.mAlarms = alarms;
        }

        public void addAlarm(ZAlarm alarm){
            this.mAlarms.add(alarm);
        }

        public String getRecurrenceIdZ() {
            return mRecurrenceIdZ;
        }

        public String getDescription() {
            return mDescription;
        }

        public void setDescription(String desc) {
            mDescription = desc;
        }

        public String getDescriptionHtml() {
            return mDescriptionHtml;
        }

        public void setDescriptionHtml(String descHtml) {
            mDescriptionHtml = descHtml;
        }

        public boolean getIsNoBlob() {
            return mIsNoBlob;
        }

        public void setIsNoBlob(boolean noBlob) {
            mIsNoBlob = noBlob;
        }
        
        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("status", mStatus.name());
            zjo.put("class", mClass.name());
            zjo.put("freeBusyStatus", mFreeBusyStatus.name());
            zjo.put("actualFreeBusyStatus", mActualFreeBusyStatus.name());
            zjo.put("transparency", mTransparency.name());
            zjo.put("isAllDay", mIsAllDay);
            zjo.put("name", mName);
            zjo.put("method", mMethod.name());
            zjo.put("compNum", mComponentNum);
            zjo.put("location", mLocation);
            zjo.putList("categories", mCategories);
            zjo.putList("comments", mComments);
            zjo.putList("contacts", mContacts);
            if (mGeo != null) zjo.put("geo", mGeo.toString());
            if (mUrl != null) zjo.put("url", mUrl);
            zjo.put("isOrganizer", mIsOrganizer);
            zjo.put("sequenceNumber", mSequenceNumber);
            zjo.put("priority", mPriority);
            zjo.put("percentCompleted", mPercentCompleted);
            zjo.put("completed", mCompleted);
            zjo.put("replies", mReplies);
            zjo.put("start", mStart);
            zjo.put("end", mEnd);
            zjo.put("duration", mDuration);
            zjo.put("organizer", mOrganizer);
            zjo.put("attendees", mAttendees);
            zjo.put("recurrence", mRecurrence);
            zjo.put("recurrenceId", mRecurrenceIdZ);
            zjo.put("desc",mDescription);
            zjo.put("deschtml",mDescriptionHtml);
            zjo.put("noblob",mIsNoBlob);
            return zjo;
        }

        public String toString() {
            return String.format("[ZComponent %s]", getName());
        }
        
        public String dump() {
            return ZJSONObject.toString(this);
        }

        public static class ZReply implements ToZJSONObject {

            private long mDate;
            private String mAttendee;
            private ZParticipantStatus mParticipantStatus;
            private int mRangeType;
            private String mRecurrenceId;
            private String mTimeZone;

            public ZReply() {

            }

            public ZJSONObject toZJSONObject() throws JSONException {
                ZJSONObject zjo = new ZJSONObject();
                zjo.put("date", mDate);
                zjo.put("attendee", mAttendee);
                zjo.put("participantStatus", mParticipantStatus.name());
                return zjo;
            }
            
            public String toString() {
                return String.format("[ZReply %s]", mAttendee);
            }

            public String dump() {
                return ZJSONObject.toString(this);
            }

            public ZReply(Element e) throws ServiceException {
                mDate = e.getAttributeLong(MailConstants.A_DATE, 0);
                mAttendee = e.getAttribute(MailConstants.A_CAL_ATTENDEE, null);
                mParticipantStatus = ZParticipantStatus.fromString(e.getAttribute(MailConstants.A_CAL_PARTSTAT, ZParticipantStatus.AC.name()));
                mRangeType = (int) e.getAttributeLong(MailConstants.A_CAL_RECURRENCE_RANGE_TYPE, 0);
                mRecurrenceId = e.getAttribute(MailConstants.A_CAL_RECURRENCE_ID, null);
                mTimeZone = e.getAttribute(MailConstants.A_CAL_TIMEZONE, null);
            }

            public Element toElement(Element parent) {
                Element replyEl = parent.addElement(MailConstants.E_CAL_REPLY);
                replyEl.addAttribute(MailConstants.A_DATE, mDate);
                if (mAttendee != null) replyEl.addAttribute(MailConstants.A_CAL_ATTENDEE, mAttendee);
                if (mParticipantStatus != null) replyEl.addAttribute(MailConstants.A_CAL_PARTSTAT, mParticipantStatus.name());
                replyEl.addAttribute(MailConstants.A_CAL_RECURRENCE_RANGE_TYPE, mRangeType);
                if (mRecurrenceId != null) replyEl.addAttribute(MailConstants.A_CAL_RECURRENCE_ID, mRecurrenceId);
                if (mTimeZone != null) replyEl.addAttribute(MailConstants.A_CAL_TIMEZONE, mTimeZone);
                return replyEl;
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

    public static class ZTimeZone implements ToZJSONObject {

        private String mId;
        private long mStandardOffset;
        private long mDaylightSavingsOffset;
        private ZTransitionRule mStandard;
        private ZTransitionRule mDaylight;
        private String mStandardTzname;
        private String mDaylightTzname;

        public ZTimeZone() {

        }
        
        public ZTimeZone(Element e) throws ServiceException {
            mId = e.getAttribute(MailConstants.A_ID);
            mStandardTzname = e.getAttribute(MailConstants.A_CAL_TZ_STDNAME, null);
            mDaylightTzname = e.getAttribute(MailConstants.A_CAL_TZ_DAYNAME, null);
            mStandardOffset = e.getAttributeLong(MailConstants.A_CAL_TZ_STDOFFSET, 0);
            mDaylightSavingsOffset = e.getAttributeLong(MailConstants.A_CAL_TZ_DAYOFFSET, -1);
            Element standardEl = e.getOptionalElement(MailConstants.E_CAL_TZ_STANDARD);
            if (standardEl != null)
                mStandard = new ZTransitionRule(standardEl);
            Element daylightEl = e.getOptionalElement(MailConstants.E_CAL_TZ_DAYLIGHT);
            if (daylightEl != null)
                mDaylight = new ZTransitionRule(daylightEl);
        }

        public Element toElement(Element parent) {
            Element tzEl = parent.addElement(MailConstants.E_CAL_TZ);
            tzEl.addAttribute(MailConstants.A_ID, mId);
            tzEl.addAttribute(MailConstants.A_CAL_TZ_STDNAME, mStandardTzname);
            tzEl.addAttribute(MailConstants.A_CAL_TZ_DAYNAME, mDaylightTzname);
            tzEl.addAttribute(MailConstants.A_CAL_TZ_STDOFFSET, mStandardOffset);
            if (mDaylightSavingsOffset != -1)
                tzEl.addAttribute(MailConstants.A_CAL_TZ_DAYOFFSET, mDaylightSavingsOffset);
            if (mStandard != null)
                mStandard.toElement(MailConstants.E_CAL_TZ_STANDARD, tzEl);

            if (mDaylight != null)
                mDaylight.toElement(MailConstants.E_CAL_TZ_DAYLIGHT, tzEl);
            
            return tzEl;
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

        public String getStandardTzname() {
            return mStandardTzname;
        }

        public String getDaylightTzname() {
            return mDaylightTzname;
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

        public void setStandardTzname(String name) {
            mStandardTzname = name;
        }

        public void setDaylightTzname(String name) {
            mDaylightTzname = name;
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

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("id", mId);
            zjo.put("standardOffset", mStandardOffset);
            zjo.put("daylightOffset", mDaylightSavingsOffset);
            zjo.put("standardRule", mStandard);
            zjo.put("daylightRule", mDaylight);
            zjo.put("standardTzname", mStandardTzname);
            zjo.put("daylightTzname", mDaylightTzname);
            return zjo;
        }

        public String toString() {
            return String.format("[ZTimeZone %s]", mId);
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }

        public static class ZTransitionRule implements ToZJSONObject {
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

            public Element toElement(String name, Element parent) {
                Element trEl = parent.addElement(name);
                if (mWeek != 0) trEl.addAttribute(MailConstants.A_CAL_TZ_WEEK, mWeek);
                if (mDayOfWeek != 0) trEl.addAttribute(MailConstants.A_CAL_TZ_DAYOFWEEK, mDayOfWeek);
                if (mDayOfMonth != 0) trEl.addAttribute(MailConstants.A_CAL_TZ_DAYOFMONTH, mMonth);
                trEl.addAttribute(MailConstants.A_CAL_TZ_MONTH, mMonth);
                trEl.addAttribute(MailConstants.A_CAL_TZ_HOUR, mHour);
                trEl.addAttribute(MailConstants.A_CAL_TZ_MINUTE, mMinute);
                trEl.addAttribute(MailConstants.A_CAL_TZ_SECOND, mSecond);
                return trEl;
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

            public ZJSONObject toZJSONObject() throws JSONException {
                ZJSONObject zjo = new ZJSONObject();
                zjo.put("week", mWeek);
                zjo.put("dayOfWeek", mDayOfWeek);
                zjo.put("dayOfMonth", mDayOfMonth);
                zjo.put("month", mMonth);
                zjo.put("hour", mHour);
                zjo.put("minute", mMinute);
                zjo.put("second", mSecond);
                return zjo;
            }

            public String toString() {
                return "[ZTransitionRule]";
            }

            public String dump() {
                return ZJSONObject.toString(this);
            }
        }
    }

    public static class ZDuration implements ToZJSONObject {

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

        Date addToDate(Date date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int mult = mNegative ? -1 : 1;
            cal.add(java.util.Calendar.WEEK_OF_YEAR, mult*mWeeks);
            cal.add(java.util.Calendar.DAY_OF_YEAR, mult*mDays);
            cal.add(java.util.Calendar.HOUR_OF_DAY, mult*mHours);
            cal.add(java.util.Calendar.MINUTE, mult*mMinutes);
            cal.add(java.util.Calendar.SECOND, mult*mSeconds);
            return cal.getTime();
        }

        public long addToTime(long utcTime) {
            return addToDate(new Date(utcTime)).getTime();
        }

        public Element toElement(Element parent) {
            Element durEl = parent.addElement(MailConstants.E_CAL_DURATION);
            durEl.addAttribute(MailConstants.A_CAL_DURATION_NEGATIVE, mNegative);
            if (mWeeks != 0)
                durEl.addAttribute(MailConstants.A_CAL_DURATION_WEEKS, mWeeks);
            else {
                if (mDays != 0) durEl.addAttribute(MailConstants.A_CAL_DURATION_DAYS, mDays);
                if (mHours != 0) durEl.addAttribute(MailConstants.A_CAL_DURATION_HOURS, mHours);
                if (mMinutes != 0) durEl.addAttribute(MailConstants.A_CAL_DURATION_MINUTES, mMinutes);
                if (mSeconds != 0) durEl.addAttribute(MailConstants.A_CAL_DURATION_SECONDS, mSeconds);
            }
            return durEl;
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

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("negative", mNegative);
            zjo.put("weeks", mWeeks);
            zjo.put("days", mDays);
            zjo.put("hours", mHours);
            zjo.put("minutes", mMinutes);
            zjo.put("seconds", mSeconds);
            return zjo;
        }

        public String toString() {
            return "[ZDuration]"; // TODO
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }

    }

    public enum ZMethod {
        PUBLISH, REQUEST, REPLY, ADD, CANCEL, REFRESH, COUNTER, DECLINECOUNTER;

        public static ZMethod fromString(String s) throws ServiceException {
            try {
                return ZMethod.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid method "+s+", valid values: "+ Arrays.asList(ZMethod.values()), e);
            }
        }

        public boolean isPublish() { return equals(PUBLISH); }
        public boolean isRequest() { return equals(REQUEST); }
        public boolean isReply() { return equals(REPLY); }
        public boolean isAdd() { return equals(ADD); }
        public boolean isCancel() { return equals(CANCEL); }
        public boolean isRefresh() { return equals(REFRESH); }
        public boolean isCounter() { return equals(COUNTER); }
        public boolean isDeclineCounter() { return equals(DECLINECOUNTER); }
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

    public enum ZClass {
        PUB, PRI, CON;

        public static ZClass fromString(String s) throws ServiceException {
            try {
                return ZClass.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid class " + s + ", valid values: " + Arrays.asList(ZClass.values()), e);
            }
        }

        public boolean isPublic() { return equals(PUB); }
        public boolean isPrivate() { return equals(PRI); }
        public boolean isConfidential() { return equals(CON); }
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

    public static class ZCalendarUser implements ToZJSONObject {
        private String mAddress;
        private String mUrl;
        private String mPersonalName;
        private String mSentBy;
        private String mDirectoryUrl;
        private String mLanguage;
        private List<ZParameter> mXParams;

        public ZCalendarUser() {
            mXParams = new ArrayList<ZParameter>();
        }

        public ZCalendarUser(Element e) throws ServiceException {
            mAddress = e.getAttribute(MailConstants.A_ADDRESS, null);
            mUrl = e.getAttribute(MailConstants.A_URL, null);
            mPersonalName = e.getAttribute(MailConstants.A_DISPLAY, null);
            mSentBy = e.getAttribute(MailConstants.A_CAL_SENTBY, null);
            mDirectoryUrl = e.getAttribute(MailConstants.A_CAL_DIR, null);
            mLanguage = e.getAttribute(MailConstants.A_CAL_LANGUAGE, null);
            mXParams = CalendarUtils.parseXParams(e);
        }

        public String getAddress() {
            return mAddress;
        }

        /**
         *
         * @return a ZEMailAddress constructed from the personal name and address.
         */
        public ZEmailAddress getEmailAddress() {
            return new ZEmailAddress(mAddress, null, mPersonalName, ZEmailAddress.EMAIL_TYPE_TO);
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

        public String getDirectoryUrl() {
            return mDirectoryUrl;
        }

        public void setDirectoryUrl(String dir) {
            mDirectoryUrl = dir;
        }

        public String getLanguage() {
            return mLanguage;
        }

        public void setLanguage(String language) {
            mLanguage = language;
        }

        public Iterator<ZParameter> xparamsIterator() {
            return mXParams.iterator();
        }

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("address", mAddress);
            zjo.put("url", mUrl);
            zjo.put("personalName", mPersonalName);
            zjo.put("sentBy", mSentBy);
            zjo.put("dir", mDirectoryUrl);
            zjo.put("language", mLanguage);
            return zjo;
        }

        public String toString() {
            return String.format("[ZCalendarUser %s]", mAddress);
        }
        
        public String dump() {
            return ZJSONObject.toString(this);
        }
    }

    public static class ZOrganizer extends ZCalendarUser {

        public ZOrganizer() {
            super();
        }

        public ZOrganizer(String address) {
            super();
            setAddress(address);
        }

        public ZOrganizer(Element e) throws ServiceException {
            super(e);
        }

        public Element toElement(Element parent) {
            Element orEl = parent.addElement(MailConstants.E_CAL_ORGANIZER);

            if (getAddress() != null) orEl.addAttribute(MailConstants.A_ADDRESS, getAddress());
            if (getUrl() != null) orEl.addAttribute(MailConstants.A_URL, getUrl());
            if (getPersonalName() != null) orEl.addAttribute(MailConstants.A_DISPLAY, getPersonalName());
            if (getSentBy() != null) orEl.addAttribute(MailConstants.A_CAL_SENTBY, getSentBy());
            if (getDirectoryUrl() != null) orEl.addAttribute(MailConstants.A_CAL_DIR, getDirectoryUrl());
            if (getLanguage() != null) orEl.addAttribute(MailConstants.A_CAL_LANGUAGE, getLanguage());
            ToXML.encodeXParams(orEl, xparamsIterator());
            return orEl;
        }

        public String toString() {
            return String.format("[ZOrganizer %s]", getAddress());
        }

    }

    public static class ZAttendee extends ZCalendarUser implements ToZJSONObject {
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
            mCalendarUserType = ZCalendarUserType.fromString(e.getAttribute(MailConstants.A_CAL_CUTYPE, ZCalendarUserType.IND.name()));
            mMember = e.getAttribute(MailConstants.A_CAL_MEMBER, null);
            mDelegatedTo = e.getAttribute(MailConstants.A_CAL_DELEGATED_TO, null);
            mDelegatedFrom = e.getAttribute(MailConstants.A_CAL_DELEGATED_FROM, null);
        }

        public Element toElement(Element parent) {
            Element attEl = parent.addElement(MailConstants.E_CAL_ATTENDEE);

            if (getAddress() != null) attEl.addAttribute(MailConstants.A_ADDRESS, getAddress());
            if (getUrl() != null) attEl.addAttribute(MailConstants.A_URL, getUrl());
            if (getPersonalName() != null) attEl.addAttribute(MailConstants.A_DISPLAY, getPersonalName());
            if (getSentBy() != null) attEl.addAttribute(MailConstants.A_CAL_SENTBY, getSentBy());
            if (getDirectoryUrl() != null) attEl.addAttribute(MailConstants.A_CAL_DIR, getDirectoryUrl());
            if (getLanguage() != null) attEl.addAttribute(MailConstants.A_CAL_LANGUAGE, getLanguage());
            if (mRole != null) attEl.addAttribute(MailConstants.A_CAL_ROLE, mRole.name());
            if (mParticipantStatus != null) attEl.addAttribute(MailConstants.A_CAL_PARTSTAT, mParticipantStatus.name());
            if (mRSVP) attEl.addAttribute(MailConstants.A_CAL_RSVP, mRSVP);
            if (mCalendarUserType != null && mCalendarUserType != ZCalendarUserType.IND) attEl.addAttribute(MailConstants.A_CAL_CUTYPE, mCalendarUserType.name());
            if (mMember != null) attEl.addAttribute(MailConstants.A_CAL_MEMBER, mMember);
            if (mDelegatedTo != null) attEl.addAttribute(MailConstants.A_CAL_DELEGATED_TO, mDelegatedTo);
            if (mDelegatedFrom != null) attEl.addAttribute(MailConstants.A_CAL_DELEGATED_FROM, mDelegatedFrom);
            ToXML.encodeXParams(attEl, xparamsIterator());
            return attEl;
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

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = super.toZJSONObject();
            zjo.put("role", mRole.name());
            zjo.put("participantStatus", mParticipantStatus.name());
            zjo.put("rsvp", mRSVP);
            zjo.put("calendarUserType", mCalendarUserType.name());
            zjo.put("member", mMember);
            zjo.put("delegatedFrom", mDelegatedFrom);
            zjo.put("delegatedTo", mDelegatedTo);
            return zjo;
        }

        public String toString() {
            return String.format("[ZAttendee %s]", getAddress());
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }

    }

    public static class ZRecurrenceDate implements ToZJSONObject {
        private ZDateTime mStart;
        private ZDateTime mEnd;
        private ZDuration mDuration;

        public ZRecurrenceDate() {

        }
        
        public ZRecurrenceDate(Element e) throws ServiceException {
            mStart = new ZDateTime(e.getElement(MailConstants.E_CAL_START_TIME));
            Element endEl = e.getOptionalElement(MailConstants.E_CAL_END_TIME);
            if (endEl != null)
                mEnd = new ZDateTime(endEl);
            Element durEl = e.getOptionalElement(MailConstants.E_CAL_DURATION);
            if (durEl != null)
                mDuration = new ZDuration(durEl);
        }

        public Element toElement(Element parent) {
            Element dtvalEl = parent.addElement(MailConstants.E_CAL_DATE_VAL);
            mStart.toElement(MailConstants.E_CAL_START_TIME, dtvalEl);
            if (mEnd != null)
                mEnd.toElement(MailConstants.E_CAL_END_TIME, dtvalEl);
            else if (mDuration != null)
                mDuration.toElement(dtvalEl);
            return dtvalEl;
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

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("start", mStart);
            zjo.put("end", mEnd);
            zjo.put("duration", mDuration);
            return zjo;
        }

        public String toString() {
            return String.format("[ZRecurrenceDate]"); // TODO
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }
    }

    public static class ZRecurrenceDates implements ToZJSONObject {
        private String mTimeZoneId;
        private List<ZRecurrenceDate> mDates;

        public ZRecurrenceDates() {

        }

        public ZRecurrenceDates(Element e) throws ServiceException {
            mDates = new ArrayList<ZRecurrenceDate>();
            mTimeZoneId = e.getAttribute(MailConstants.A_CAL_TIMEZONE, null);
            for (Element dtvalEl : e.listElements(MailConstants.E_CAL_DATE_VAL)) {
                mDates.add(new ZRecurrenceDate(dtvalEl));
            }
        }

        public Element toElement(Element parent) {
            Element datesEl = parent.addElement(MailConstants.E_CAL_DATES);
            if (mTimeZoneId != null)
                datesEl.addAttribute(MailConstants.A_CAL_TIMEZONE, mTimeZoneId);
            for (ZRecurrenceDate rdate : mDates) {
                rdate.toElement(datesEl);
            }
            return datesEl;
        }

        public String getTimeZoneId() {
            return mTimeZoneId;
        }

        public void setTimeZoneId(String timeZoneId) {
            mTimeZoneId = timeZoneId;
        }

        public List<ZRecurrenceDate> getDates() {
            return mDates;
        }

        public void setDates(List<ZRecurrenceDate> dates) {
            mDates = dates;
        }

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("timeZoneId", mTimeZoneId);
            zjo.put("dates", mDates);
            return zjo;
        }

        public String toString() {
            return String.format("[ZRecurrenceDates]"); // TODO
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }
    }

    public enum ZFrequency {
        SEC, MIN, HOU, DAI, WEE, MON, YEA;

        public static ZFrequency fromString(String s) throws ServiceException {
            try {
                return ZFrequency.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid frequency "+s+", valid values: "+ Arrays.asList(ZFrequency.values()), e);
            }
        }

        public boolean isSecond() { return equals(SEC); }
        public boolean isMinute() { return equals(MIN); }
        public boolean isHour() { return equals(HOU); }
        public boolean isDaily() { return equals(DAI); }
        public boolean isWeek() { return equals(WEE); }
        public boolean isMonth() { return equals(MON); }
        public boolean isYear() { return equals(YEA); }
    }

    public enum ZWeekDay {
        SU, MO, TU, WE, TH, FR, SA;

        public static ZWeekDay fromString(String s) throws ServiceException {
            try {
                return ZWeekDay.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid weekday "+s+", valid values: "+ Arrays.asList(ZWeekDay.values()), e);
            }
        }

        public int getOrdinal() { return ordinal(); }

        public static ZWeekDay fromOrdinal(int ord) throws ServiceException {
            if (ord < 0 || ord >= ZWeekDay.values().length)
                throw ZClientException.CLIENT_ERROR("invalid weekday ordinal: "+ord, null);
            else
                return ZWeekDay.values()[ord];
        }
        
        public boolean isSunday() { return equals(SU); }
        public boolean isMonday() { return equals(MO); }
        public boolean isTuesday() { return equals(TU); }
        public boolean isWednesday() { return equals(WE); }
        public boolean isThursday() { return equals(TH); }
        public boolean isFriday() { return equals(FR); }
        public boolean isSaturday() { return equals(SA); }
    }

    public enum ZByType {
        BY_SECOND, BY_MINUTE, BY_HOUR, BY_DAY, BY_MONTHDAY, BY_YEARDAY, BY_WEEKNO, BY_MONTH, BY_SETPOS;

        public static ZByType fromString(String s) throws ServiceException {
            try {
                return ZByType.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ZClientException.CLIENT_ERROR("invalid by type "+s+", valid values: "+ Arrays.asList(ZByType.values()), e);
            }
        }

        public boolean isBySecond() { return equals(BY_SECOND); }
        public boolean isByMinute() { return equals(BY_MINUTE); }
        public boolean isByHour() { return equals(BY_HOUR); }
        public boolean isByDay() { return equals(BY_DAY); }
        public boolean isByMonthDay() { return equals(BY_MONTHDAY); }
        public boolean isByYearDay() { return equals(BY_YEARDAY); }
        public boolean isByWeekNo() { return equals(BY_WEEKNO); }
        public boolean isByMonth() { return equals(BY_MONTH); }
        public boolean isBySetPos() { return equals(BY_SETPOS); }
    }

    public static class ZByDayWeekDay implements ToZJSONObject {

        private int mWeekOrd;
        private ZWeekDay mDay;

        public static List<ZByDayWeekDay> getList(ZWeekDay... days) {
            List<ZByDayWeekDay> result = new ArrayList<ZByDayWeekDay>(days.length);
            for (ZWeekDay day : days)
                result.add(new ZByDayWeekDay(day, 0));
            return result;
        }

        public static List<ZByDayWeekDay> getList(List<ZWeekDay> days) {
            List<ZByDayWeekDay> result = new ArrayList<ZByDayWeekDay>(days.size());
            for (ZWeekDay day : days)
                result.add(new ZByDayWeekDay(day, 0));
            return result;
        }

        public ZByDayWeekDay() {

        }

        public ZByDayWeekDay(ZWeekDay day, int weekOrd) {
            mDay = day;
            mWeekOrd = weekOrd;

        }
        
        public ZByDayWeekDay(Element e) throws ServiceException {
            mWeekOrd = (int) e.getAttributeLong(MailConstants.A_CAL_RULE_BYDAY_WKDAY_ORDWK, 0);
            mDay = ZWeekDay.fromString(e.getAttribute(MailConstants.A_CAL_RULE_DAY));
        }

        public Element toElement(Element parent) {
            Element wkdayEl = parent.addElement(MailConstants.E_CAL_RULE_BYDAY_WKDAY);
            if (mWeekOrd != 0)
                wkdayEl.addAttribute(MailConstants.A_CAL_RULE_BYDAY_WKDAY_ORDWK, mWeekOrd);
            wkdayEl.addAttribute(MailConstants.A_CAL_RULE_DAY, mDay.name());
            return wkdayEl;
        }

        public int getWeekOrd() {
            return mWeekOrd;
        }

        public void setWeekOrd(int weekOrd) {
            mWeekOrd = weekOrd;
        }

        public ZWeekDay getDay() {
            return mDay;
        }

        public void setDay(ZWeekDay day) {
            mDay = day;
        }


        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("weekOrd", mWeekOrd);
            zjo.put("day", mDay.name());
            return zjo;
        }

        public String toString() {
            return String.format("[ZByDayWeekDay weekOrd=%d day=%s]", mWeekOrd, mDay);
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }
    }
    
    public static class ZByRule implements ToZJSONObject {
        private ZByType mType;
        private String mList;
        private List<ZByDayWeekDay> mWeekDays;

        public ZByRule(ZByType type, String list, List<ZByDayWeekDay> weekDays) {
            mType = type;
            mList = list;
            mWeekDays = weekDays;
        }

        public ZByRule(Element e) throws ServiceException {
            String ename = e.getName();
            if (ename.equals(MailConstants.E_CAL_RULE_BYSECOND)) {

                mType = ZByType.BY_SECOND;
                mList = e.getAttribute(MailConstants.A_CAL_RULE_BYSECOND_SECLIST, null);

            } else if (ename.equals(MailConstants.E_CAL_RULE_BYMINUTE)) {

                mType = ZByType.BY_MINUTE;
                mList = e.getAttribute(MailConstants.A_CAL_RULE_BYMINUTE_MINLIST, null);

            } else if (ename.equals(MailConstants.E_CAL_RULE_BYHOUR)) {

                mType = ZByType.BY_HOUR;
                mList = e.getAttribute(MailConstants.A_CAL_RULE_BYHOUR_HRLIST, null);

            } else if (ename.equals(MailConstants.E_CAL_RULE_BYDAY)) {

                mType = ZByType.BY_DAY;
                mWeekDays = new ArrayList<ZByDayWeekDay>();
                for (Element wkdayEl : e.listElements(MailConstants.E_CAL_RULE_BYDAY_WKDAY)) {
                    mWeekDays.add(new ZByDayWeekDay(wkdayEl));
                }

            } else if (ename.equals(MailConstants.E_CAL_RULE_BYMONTHDAY)) {

                mType = ZByType.BY_MONTHDAY;
                mList = e.getAttribute(MailConstants.A_CAL_RULE_BYMONTHDAY_MODAYLIST, null);

            } else if (ename.equals(MailConstants.E_CAL_RULE_BYYEARDAY)) {

                mType = ZByType.BY_YEARDAY;
                mList = e.getAttribute(MailConstants.A_CAL_RULE_BYYEARDAY_YRDAYLIST, null);

            } else if (ename.equals(MailConstants.E_CAL_RULE_BYWEEKNO)) {

                mType = ZByType.BY_WEEKNO;
                mList = e.getAttribute(MailConstants.A_CAL_RULE_BYWEEKNO_WKLIST, null);

            } else if (ename.equals(MailConstants.E_CAL_RULE_BYMONTH)) {

                mType = ZByType.BY_MONTH;
                mList = e.getAttribute(MailConstants.A_CAL_RULE_BYMONTH_MOLIST, null);

            } else if (ename.equals(MailConstants.E_CAL_RULE_BYSETPOS)) {

                mType = ZByType.BY_SETPOS;
                mList = e.getAttribute(MailConstants.A_CAL_RULE_BYSETPOS_POSLIST, null);

            }
        }

        public Element toElement(Element parent) {
            String elName = null;
            String listName = null;

            switch (mType) {
                case BY_SECOND:
                    elName = MailConstants.E_CAL_RULE_BYSECOND;
                    listName = MailConstants.A_CAL_RULE_BYSECOND_SECLIST;
                    break;
                case BY_MINUTE:
                    elName = MailConstants.E_CAL_RULE_BYMINUTE;
                    listName = MailConstants.A_CAL_RULE_BYMINUTE_MINLIST;
                    break;
                case BY_HOUR:
                    elName = MailConstants.E_CAL_RULE_BYHOUR;
                    listName = MailConstants.A_CAL_RULE_BYHOUR_HRLIST;
                    break;
                case BY_DAY:
                    elName = MailConstants.E_CAL_RULE_BYDAY;
                    break;
                case BY_MONTHDAY:
                    elName = MailConstants.E_CAL_RULE_BYMONTHDAY;
                    listName = MailConstants.A_CAL_RULE_BYMONTHDAY_MODAYLIST;
                    break;
                case BY_YEARDAY:
                    elName = MailConstants.E_CAL_RULE_BYYEARDAY;
                    listName = MailConstants.A_CAL_RULE_BYYEARDAY_YRDAYLIST;
                    break;
                case BY_WEEKNO:
                    elName = MailConstants.E_CAL_RULE_BYWEEKNO;
                    listName = MailConstants.A_CAL_RULE_BYWEEKNO_WKLIST;
                    break;
                case BY_MONTH:
                    elName = MailConstants.E_CAL_RULE_BYMONTH;
                    listName = MailConstants.A_CAL_RULE_BYMONTH_MOLIST;
                    break;
               case BY_SETPOS:
                    elName = MailConstants.E_CAL_RULE_BYSETPOS;
                    listName = MailConstants.A_CAL_RULE_BYSETPOS_POSLIST;
                    break;
            }

            Element byEl = parent.addElement(elName);
            if (mList != null && listName != null) byEl.addAttribute(listName, mList);
            if (mType == ZByType.BY_DAY) {
                if (mWeekDays != null) {
                    for (ZByDayWeekDay wd : mWeekDays) {
                        wd.toElement(byEl);
                    }
                }
            }

            return byEl;
        }

        public ZByType getType() {
            return mType;
        }

        public String getListValue() {
            return mList;
        }

        public String[] getList() {
            if (mList == null || mList.length() == 0)
                return new String[0];
            else
                return mList.split(",");
        }

        /**
         *
         * @return list of week days (only valid with BY_DAY)
         */
        public List<ZByDayWeekDay> getByDayWeekDays() {
            return mWeekDays;
        }

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("type", mType.name());
            zjo.put("list", mList);
            zjo.put("weekdays", mWeekDays);
            return zjo;
        }

        public String toString() {
            return String.format("[ZByRule]"); //TODO
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }
    }

    public static class ZRecurrenceRule implements ToZJSONObject {
        private ZFrequency mFrequency;
        private ZDateTime mUntilDate;
        private int mCount;
        private int mInterval = 1;
        private List<ZByRule> mByRules;
        private ZWeekDay mWeekStart;

        public ZRecurrenceRule() {

        }
        
        public ZRecurrenceRule(Element e) throws ServiceException {
            mFrequency = ZFrequency.fromString(e.getAttribute(MailConstants.A_CAL_RULE_FREQ));
            mByRules = new ArrayList<ZByRule>();

            for (Element childEl : e.listElements()) {
                if (childEl.getName().equals(MailConstants.E_CAL_RULE_UNTIL)) {
                    mUntilDate = new ZDateTime(childEl);
                } else if (childEl.getName().equals(MailConstants.E_CAL_RULE_COUNT)) {
                    mCount = (int) childEl.getAttributeLong(MailConstants.A_CAL_RULE_COUNT_NUM, 1);
                } else if (childEl.getName().equals(MailConstants.E_CAL_RULE_INTERVAL)) {
                    mInterval = (int) childEl.getAttributeLong(MailConstants.A_CAL_RULE_INTERVAL_IVAL, 1);
                } else if (childEl.getName().equals(MailConstants.E_CAL_RULE_WKST)) {
                    mWeekStart = ZWeekDay.fromString(childEl.getAttribute(MailConstants.A_CAL_RULE_DAY));
                } else if (childEl.getName().startsWith("by")) {
                    mByRules.add(new ZByRule(childEl));
                }
            }
        }

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject zjo = new ZJSONObject();
            zjo.put("frequency", mFrequency.name());
            zjo.put("until", mUntilDate);
            zjo.put("count", mCount);
            zjo.put("interval", mInterval);
            zjo.put("byRules", mByRules);
            if (mWeekStart != null) zjo.put("weekStart", mWeekStart.name());
            return zjo;
        }

        public String toString() {
            return String.format("[ZRecurrenceRule]"); // TODO
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }

        public Element toElement(Element parent) {
            Element ruleEl = parent.addElement(MailConstants.E_CAL_RULE);
            if (mFrequency != null)
                ruleEl.addAttribute(MailConstants.A_CAL_RULE_FREQ, mFrequency.name());
            if (mUntilDate != null)
                mUntilDate.toElement(MailConstants.E_CAL_RULE_UNTIL, ruleEl);
            else if (mCount != 0)
                ruleEl.addElement(MailConstants.E_CAL_RULE_COUNT).addAttribute(MailConstants.A_CAL_RULE_COUNT_NUM, mCount);
            if (mInterval != 0)
                ruleEl.addElement(MailConstants.E_CAL_RULE_INTERVAL).addAttribute(MailConstants.A_CAL_RULE_INTERVAL_IVAL, mInterval);
            if (mWeekStart != null)
                ruleEl.addElement(MailConstants.E_CAL_RULE_WKST).addAttribute(MailConstants.A_CAL_RULE_DAY, mWeekStart.name());
            if (mByRules != null && !mByRules.isEmpty()) {
                for (ZByRule rule : mByRules) {
                    rule.toElement(ruleEl);
                }
            }

            return ruleEl;

        }

        public ZFrequency getFrequency() {
            return mFrequency;
        }

        public void setFrequency(ZFrequency frequency) {
            mFrequency = frequency;
        }

        public ZDateTime getUntilDate() {
            return mUntilDate;
        }

        public void setUntilDate(ZDateTime untilDate) {
            mUntilDate = untilDate;
        }

        public int getCount() {
            return mCount;
        }

        public void setCount(int count) {
            mCount = count;
        }

        public int getInterval() {
            return mInterval;
        }

        public void setInterval(int interval) {
            mInterval = interval;
        }

        public List<ZByRule> getByRules() {
            return mByRules;
        }

        public void setByRules(List<ZByRule> byRules) {
            mByRules = byRules;
        }

        public ZWeekDay getWeekStart() {
            return mWeekStart;
        }

        public void setWeekStart(ZWeekDay weekStart) {
            mWeekStart = weekStart;
        }

    }


    public static class ZRecurrence implements ToZJSONObject {

        private List<ZRecurrenceRule> mRules;
        private List<ZRecurrenceDates> mDates;
        
        private List<ZRecurrenceRule> mExRules;
        private List<ZRecurrenceDates> mExDates;

        public ZRecurrence() {

        }

        public ZRecurrence(Element e) throws ServiceException {
            mRules = new ArrayList<ZRecurrenceRule>();
            mExRules = new ArrayList<ZRecurrenceRule>();
            mDates = new ArrayList<ZRecurrenceDates>();
            mExDates = new ArrayList<ZRecurrenceDates>();
            
            for (Element addEl : e.listElements(MailConstants.E_CAL_ADD)) {
                for (Element ruleEl : addEl.listElements(MailConstants.E_CAL_RULE))
                    mRules.add(new ZRecurrenceRule(ruleEl));

                for (Element datesEl : addEl.listElements(MailConstants.E_CAL_DATES))
                    mDates.add(new ZRecurrenceDates(datesEl));
            }

            for (Element excludeEl : e.listElements(MailConstants.E_CAL_EXCLUDE)) {
                for (Element ruleEl : excludeEl.listElements(MailConstants.E_CAL_RULE))
                    mExRules.add(new ZRecurrenceRule(ruleEl));

                for (Element datesEl : excludeEl.listElements(MailConstants.E_CAL_DATES))
                    mExDates.add(new ZRecurrenceDates(datesEl));
            }
        }

        public Element toElement(Element parent) {
            Element recurEl = parent.addElement(MailConstants.E_CAL_RECUR);
            if ((mRules != null && !mRules.isEmpty()) ||
                    (mDates != null && !mDates.isEmpty())) {
                Element addEl = recurEl.addElement(MailConstants.E_CAL_ADD);

                if (mRules != null) {
                    for (ZRecurrenceRule rule : mRules) {
                        rule.toElement(addEl);
                    }
                }

                if (mDates != null) {
                    for (ZRecurrenceDates dates : mDates) {
                        dates.toElement(addEl);
                    }
                }
            }

            if ((mExRules != null && !mExRules.isEmpty()) ||
                    (mExDates != null && !mExDates.isEmpty())) {
                Element exEl = recurEl.addElement(MailConstants.E_CAL_EXCLUDE);

                if (mExRules != null) {
                    for (ZRecurrenceRule rule : mExRules) {
                        rule.toElement(exEl);
                    }
                }

                if (mExDates != null) {
                    for (ZRecurrenceDates dates : mExDates) {
                        dates.toElement(exEl);
                    }
                }
                
            }

            return recurEl;
        }

        public List<ZRecurrenceRule> getRules() {
            return mRules;
        }

        public void setRules(List<ZRecurrenceRule> rules) {
            mRules = rules;
        }

        public List<ZRecurrenceDates> getDates() {
            return mDates;
        }

        public void setDates(List<ZRecurrenceDates> dates) {
            mDates = dates;
        }

        public List<ZRecurrenceRule> getExRules() {
            return mExRules;
        }

        public void setExRules(List<ZRecurrenceRule> exRules) {
            mExRules = exRules;
        }

        public List<ZRecurrenceDates> getExDates() {
            return mExDates;
        }

        public void setExDates(List<ZRecurrenceDates> exDates) {
            mExDates = exDates;
        }

        public ZJSONObject toZJSONObject() throws JSONException {
            ZJSONObject jso = new ZJSONObject();
            jso.put("rules", mRules);
            jso.put("dates", mDates);
            jso.put("exRules", mExRules);
            jso.put("exDates", mExDates);
            return jso;
        }

        public String toString() {
            return String.format("[ZRecurrence]");
        }

        public String dump() {
            return ZJSONObject.toString(this);
        }
    }

}
