/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.calendar.Geo;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
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
    private Boolean mHasAlarm;

    private Boolean mHasAttachment;
    private Boolean mDraft;
    private Boolean mNeverSent;

    // summary/location/fragment
    private String mSummary;
    private String mLocation;
    private String mFragment;

    // description
    // mDesc is the plain text description.
    // mDescHtml is the HTML version of the description.
    // mDescInMeta tells if the description values are in metadata or blob file.  This applies to
    // both mDesc and mDescHtml.
    // If mDescInMeta is false, mDesc and mDescHtml are set to null.
    // If mDescInMeta is true, mDesc and mDescHtml have the value but one or both can still be null
    // depending on what data was present in the original calendar component.  Both will be null in
    // a simple appointment with no meeting notes.
    private String mDesc;
    private String mDescHtml;
    private Boolean mDescInMeta;

    // time/recurrence
    private Boolean mIsAllDay;

    // common meta data
    private String mStatus;
    private String mPriority;
    private String mClassProp;
    private List<String> mCategories;
    private Geo mGeo;

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
    public Boolean hasAlarm()             { return mHasAlarm; }
    public Boolean hasAttachment()        { return mHasAttachment; }
    public Boolean isDraft()              { return mDraft; }
    public Boolean isNeverSent()          { return mNeverSent; }

    public String getSummary()     { return mSummary; }
    public String getLocation()    { return mLocation; }
    public String getFragment()    { return mFragment; }
    public Boolean descInMeta()    { return mDescInMeta; }
    public String getDesc()        { return mDesc; }
    public String getDescHtml()    { return mDescHtml; }

    public Boolean isAllDay() { return mIsAllDay; }

    public String getStatus() { return mStatus; }
    public String getPriority() { return mPriority; }
    public String getClassProp() { return mClassProp; }
    public List<String> getCategories() { return mCategories; }
    public Geo getGeo() { return mGeo; }

    public String getFreeBusyIntended() { return mFreeBusyIntended; }
    public String getTransparency() { return mTransparency; }

    public boolean isPublic(FullInstanceData defaultInstance) {
        if (mClassProp != null)
            return IcalXmlStrMap.CLASS_PUBLIC.equals(mClassProp);
        if (defaultInstance != null)
            return defaultInstance.isPublic(null);
        return true;
    }

    public FullInstanceData(
            String recurIdZ, long dtStart, long duration, long alarmAt, long tzOffset,
            String partStat, String freeBusyActual, String percentComplete,
            int invId, int compNum,
            long recurrenceId, int sequence, long dtStamp,
            ZOrganizer organizer, Boolean isOrganizer, List<ZAttendee> attendees,
            Boolean hasAlarm, Boolean hasAttachment, Boolean draft, Boolean neverSent,
            String summary, String location, String fragment, Boolean descInMeta, String desc, String descHtml,
            Boolean isAllDay,
            String status, String priority, String classProp,
            String freeBusyIntended, String transparency, List<String> categories, Geo geo) {
        this(recurIdZ, dtStart, duration, alarmAt, tzOffset,
            partStat, freeBusyActual, percentComplete,
            invId, compNum,
            recurrenceId, sequence,dtStamp,
            organizer, isOrganizer, attendees,
            hasAlarm, hasAttachment, draft, neverSent,
            summary, location, fragment, descInMeta, desc, descHtml,
            isAllDay,
            status, priority, classProp,
            freeBusyIntended, transparency, categories, geo, (Color)null);
    }

    public FullInstanceData(
            String recurIdZ, long dtStart, long duration, long alarmAt, long tzOffset,
            String partStat, String freeBusyActual, String percentComplete,
            int invId, int compNum,
            long recurrenceId, int sequence, long dtStamp,
            ZOrganizer organizer, Boolean isOrganizer, List<ZAttendee> attendees,
            Boolean hasAlarm, Boolean hasAttachment, Boolean draft, Boolean neverSent,
            String summary, String location, String fragment, Boolean descInMeta, String desc, String descHtml,
            Boolean isAllDay,
            String status, String priority, String classProp,
            String freeBusyIntended, String transparency, List<String> categories, Geo geo, Color color) {
        super(recurIdZ, dtStart, duration, alarmAt, tzOffset, partStat, freeBusyActual, percentComplete, color);
        init(invId, compNum, recurrenceId, sequence, dtStamp,
             organizer, isOrganizer, attendees, hasAlarm, hasAttachment, draft, neverSent,
             summary, location, fragment, descInMeta, desc, descHtml,
             isAllDay, status, priority, classProp, freeBusyIntended, transparency, categories, geo);
    }

    private void init(
            int invId, int compNum,
            long recurrenceId, int sequence, long dtStamp,
            ZOrganizer organizer, Boolean isOrganizer, List<ZAttendee> attendees,
            Boolean hasAlarm, Boolean hasAttachment, Boolean draft, Boolean neverSent,
            String summary, String location, String fragment, Boolean descInMeta, String desc, String descHtml,
            Boolean isAllDay,
            String status, String priority, String classProp,
            String freeBusyIntended, String transparency, List<String> categories, Geo geo) {
        mInvId = invId; mCompNum = compNum;
        mRecurrenceId = recurrenceId;
        mSequence = sequence; mDtStamp = dtStamp;
        mOrganizer = organizer; mIsOrganizer = isOrganizer;
        mAttendees = attendees;
        mNumAttendees = attendees != null ? (Integer) attendees.size() : null;
        mHasAlarm = hasAlarm; mHasAttachment = hasAttachment; mDraft = draft; mNeverSent = neverSent;
        mSummary = summary; mLocation = location; mFragment = fragment;
        mDescInMeta = descInMeta; mDesc = desc; mDescHtml = descHtml;
        mIsAllDay = isAllDay;
        mStatus = status; mPriority = priority; mClassProp = classProp;
        mFreeBusyIntended = freeBusyIntended; mTransparency = transparency;
        mCategories = categories;
        mGeo = geo;
    }

    // create a full instance, clearing fields that don't override the default instance
    public FullInstanceData(Invite inv, String recurIdZ, Long dtStart, Long duration,
                            String partStat, String freeBusyActual, Long alarmAt)
    throws ServiceException {
        super(recurIdZ, dtStart, duration, alarmAt,
              dtStart != null ? Util.getTZOffsetForInvite(inv, dtStart) : null,
              partStat, freeBusyActual, inv.getPercentComplete(), inv.getRgbColor());
        long recurId = 0;
        if (inv.hasRecurId()) {
            RecurId rid = inv.getRecurId();
            ParsedDateTime ridDt = rid.getDt();
            recurId = ridDt.getUtcTime();
        }
        List<ZAttendee> attendees = null;
        if (inv.hasOtherAttendees())
            attendees = inv.getAttendees();
        String desc = null, descHtml = null;
        boolean descInMeta = inv.descInMeta();
        if (descInMeta) {
            // Important: Call Invite.getDescription[Html]() only when descInMeta is true.
            // Otherwise, this will cause expensive file read and MIME parsing.
            desc = inv.getDescription();
            descHtml = inv.getDescriptionHtml();
        }
        init(inv.getMailItemId(), inv.getComponentNum(), recurId, inv.getSeqNo(), inv.getDTStamp(),
             inv.getOrganizer(), inv.isOrganizer(), attendees, inv.hasAlarm(), inv.hasAttachment(),
             inv.isDraft(), inv.isNeverSent(),
             inv.getName(), inv.getLocation(), inv.getFragment(), descInMeta, desc, descHtml,
             inv.isAllDayEvent(), inv.getStatus(), inv.getPriority(), inv.getClassProp(),
             inv.getFreeBusy(), inv.getTransparency(), inv.getCategories(), inv.getGeo());
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
    private static final String FN_HAS_ALARM = "ha";
    private static final String FN_HAS_ATTACHMENT = "hAttach";
    private static final String FN_DRAFT = "draft";
    private static final String FN_NEVER_SENT = "neverSent";
    private static final String FN_SUMMARY = "summ";
    private static final String FN_LOCATION = "loc";
    private static final String FN_FRAGMENT = "fr";
    private static final String FN_DESC_IN_META    = "dinM";
    private static final String FN_DESC            = "desc";
    private static final String FN_DESC_HTML       = "descH";
    private static final String FN_IS_ALLDAY = "allDay";
    private static final String FN_STATUS = "status";
    private static final String FN_PRIORITY = "prio";
    private static final String FN_CLASS = "class";
    private static final String FN_FREEBUSY = "fb";
    private static final String FN_TRANSPARENCY = "transp";
    private static final String FN_NUM_CATEGORIES = "numCat";
    private static final String FN_CATEGORY = "cat";
    private static final String FN_GEO = "geo";

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

        Boolean hasAlarm = null;
        if (meta.containsKey(FN_HAS_ALARM))
        	hasAlarm = new Boolean(meta.getBool(FN_HAS_ALARM));
        Boolean hasAttachment = null;
        if (meta.containsKey(FN_HAS_ATTACHMENT))
            hasAttachment = new Boolean(meta.getBool(FN_HAS_ATTACHMENT));
        Boolean draft = null;
        if (meta.containsKey(FN_DRAFT))
            draft = new Boolean(meta.getBool(FN_DRAFT));
        Boolean neverSent = null;
        if (meta.containsKey(FN_NEVER_SENT))
            neverSent = new Boolean(meta.getBool(FN_NEVER_SENT));

        String summary = meta.get(FN_SUMMARY, null);
        String location = meta.get(FN_LOCATION, null);
        String fragment = meta.get(FN_FRAGMENT, null);
        Boolean descInMeta = null;
        if (meta.containsKey(FN_DESC_IN_META))
            descInMeta = new Boolean(meta.getBool(FN_DESC_IN_META));
        String desc = meta.get(FN_DESC, null);
        String descHtml = meta.get(FN_DESC_HTML, null);

        Boolean isAllDay = null;
        if (meta.containsKey(FN_IS_ALLDAY))
            isAllDay = new Boolean(meta.getBool(FN_IS_ALLDAY));

        String status = meta.get(FN_STATUS, null);
        String priority = meta.get(FN_PRIORITY, null);
        String classProp = meta.get(FN_CLASS, null);
        String fb = meta.get(FN_FREEBUSY, null);
        String transp = meta.get(FN_TRANSPARENCY, null);

        List<String> categories = null;
        int numCat = (int) meta.getLong(FN_NUM_CATEGORIES, 0);
        if (numCat > 0) {
            categories = new ArrayList<String>();
            for (int i = 0; i < numCat; i++) {
                String cat = meta.get(FN_CATEGORY + i, null);
                if (cat != null)
                    categories.add(cat);
            }
        }
        Geo geo = null;
        Metadata metaGeo = meta.getMap(FN_GEO, true);
        if (metaGeo != null)
            geo = com.zimbra.cs.mailbox.calendar.Util.decodeGeoFromMetadata(metaGeo);

        init(invId, compNum, recurId, seq, dtStamp, org, isOrg, attendees, hasAlarm, hasAttachment, draft, neverSent,
             summary, location, fragment, descInMeta, desc, descHtml,
             isAllDay, status, priority, classProp, fb, transp, categories, geo);
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
        if (mHasAlarm != null)
        	meta.put(FN_HAS_ALARM, mHasAlarm.booleanValue());
        if (mHasAttachment != null)
            meta.put(FN_HAS_ATTACHMENT, mHasAttachment.booleanValue());
        if (mDraft != null)
            meta.put(FN_DRAFT, mDraft.booleanValue());
        if (mNeverSent != null)
            meta.put(FN_NEVER_SENT, mNeverSent.booleanValue());

        meta.put(FN_SUMMARY, mSummary);
        meta.put(FN_LOCATION, mLocation);
        meta.put(FN_FRAGMENT, mFragment);
        if (mDescInMeta != null)
            meta.put(FN_DESC_IN_META, mDescInMeta.booleanValue());
        meta.put(FN_DESC, mDesc);
        meta.put(FN_DESC_HTML, mDescHtml);

        if (mIsAllDay != null)
            meta.put(FN_IS_ALLDAY, mIsAllDay.booleanValue());

        meta.put(FN_STATUS, mStatus);
        meta.put(FN_PRIORITY, mPriority);
        meta.put(FN_CLASS, mClassProp);
        meta.put(FN_FREEBUSY, mFreeBusyIntended);
        meta.put(FN_TRANSPARENCY, mTransparency);

        if (mCategories != null) {
            int numCat = mCategories.size();
            if (numCat > 0) {
                meta.put(FN_NUM_CATEGORIES, numCat);
                int i = 0;
                for (String cat : mCategories) {
                    meta.put(FN_CATEGORY + i, cat);
                    i++;
                }
            }
        }
        if (mGeo != null) {
            meta.put(FN_GEO, com.zimbra.cs.mailbox.calendar.Util.encodeMetadata(mGeo));
        }

        return meta;
    }

    public static boolean isFullInstanceMeta(Metadata meta) throws ServiceException {
        return meta.getBool(FN_IS_FULL_INSTANCE, false);
    }
}
