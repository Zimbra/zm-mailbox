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
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbTag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Metadata;

// an appointment/task and expanded instances over a time range
public class CalendarItemData {
    // ZCS-specific meta data
    private MailItem.Type type;  // APPOINTMENT or TASK
    private int mFolderId;
    private int mCalItemId;
    private String mFlags;
    private String[] mTags;
    private String mTagIds;
    private boolean mIsPublic;
    private int mModMetadata; // mod_metadata db column; this serves the function of last-modified-time
    private long mModContent;  // mod_content db column
    private long mDate;       // date db column; unix time in millis
    private long mChangeDate; // change_date db column; unix time in millis
    private long mSize;       // size db column

    // change management info
    private String mUid;

    // time/recurrence
    private boolean mIsRecurring;
    private boolean mHasExceptions;

    // alarm
    private AlarmData mAlarm;

    // default instance data
    private FullInstanceData mDefaultData;

    // expanded/overridden instances
    private List<InstanceData> mInstances;

    // actual time range covered by expanded instances
    private long mActualRangeStart;
    private long mActualRangeEnd;

    public MailItem.Type getType() {
        return type;
    }

    public int getFolderId()  { return mFolderId; }
    public int getCalItemId() { return mCalItemId; }
    public String getFlags()  { return mFlags; }
    public String[] getTags()   { return mTags; }
    public String getTagIds()   { return mTagIds; }
    public boolean isPublic() { return mIsPublic; }
    public int getModMetadata()  { return mModMetadata; }
    public int getModContent()   { return (int) mModContent; }
    public long getModContentLong()   { return mModContent; }
    public long getDate()         { return mDate; }
    public long getChangeDate()   { return mChangeDate; }
    public long getSize()         { return mSize; }

    public String getUid()   { return mUid; }

    public boolean isRecurring()   { return mIsRecurring; }
    public boolean hasExceptions() { return mHasExceptions; }

    public AlarmData getAlarm() { return mAlarm; }
    public boolean hasAlarm()   { return mAlarm != null; }

    public FullInstanceData getDefaultData() { return mDefaultData; }
    public Iterator<InstanceData> instanceIterator() { return mInstances.iterator(); }

    public long getActualRangeStart() { return mActualRangeStart; }
    public long getActualRangeEnd()   { return mActualRangeEnd; }
    void setActualRange(long start, long end) {
        mActualRangeStart = start;
        mActualRangeEnd = end;
    }

    CalendarItemData(MailItem.Type type, int folderId, int calItemId, String flags, String[] tags, String tagIds, int modMetadata,
            long modContent, long date, long changeDate, long size, String uid, boolean isRecurring, boolean hasExceptions, boolean isPublic,
            AlarmData alarm, FullInstanceData defaultData) {
        this.type = type;
        mFolderId = folderId;
        mCalItemId = calItemId;
        mFlags = flags;
        mTags = tags;
        mTagIds = tagIds;
        mModMetadata = modMetadata;
        mModContent = modContent;
        mDate = date;
        mChangeDate = changeDate;
        mSize = size;
        mUid = uid;
        mIsRecurring = isRecurring;
        mHasExceptions = hasExceptions;
        mIsPublic = isPublic;
        mAlarm = alarm;
        mDefaultData = defaultData;
        mInstances = new ArrayList<InstanceData>();
    }

    void addInstance(InstanceData instance) {
        mInstances.add(instance);
    }

    public int getNumInstances() {
        return mInstances.size();
    }

    public CalendarItemData getSubRange(long rangeStart, long rangeEnd) {
        if (rangeStart <= mActualRangeStart && rangeEnd >= mActualRangeEnd)
            return this;
        CalendarItemData calItemData = new CalendarItemData(type, mFolderId, mCalItemId, mFlags, mTags, mTagIds, mModMetadata,
                mModContent, mDate, mChangeDate, mSize, mUid, mIsRecurring, mHasExceptions, mIsPublic, mAlarm, mDefaultData);
        long defaultDuration =
            mDefaultData.getDuration() != null ? mDefaultData.getDuration().longValue() : 0;
        for (InstanceData inst : mInstances) {
            long alarmAt = inst.getAlarmAt() != null ? inst.getAlarmAt().longValue() : 0;
            if (rangeStart <= alarmAt && alarmAt < rangeEnd) {
                // Instance start time is outside the range but its alarm time is within range.
                calItemData.addInstance(inst);
            } else if (inst.getDtStart() != null) {
                long instStart = inst.getDtStart().longValue();
                Long instDuration = inst.getDuration();
                long duration = instDuration != null ? instDuration.longValue() : defaultDuration;
                long instEnd = instStart + duration;
                if ((instStart == 0 && instEnd == 0) ||
                    (instStart < rangeEnd && instEnd > rangeStart)) {
                    calItemData.addInstance(inst);
                }
            } else {
                // Tasks don't always have start time.  These timeless task instances are
                // always included.
                calItemData.addInstance(inst);
            }
        }
        if (calItemData.getNumInstances() > 0)
            return calItemData;
        else
            return null;
    }

    private static final String FN_TYPE = "type";
    private static final String FN_FOLDER_ID = "fid";
    private static final String FN_CALITEM_ID = "ciid";
    private static final String FN_FLAGS = "flag";
    private static final String FN_TAGS = "tag";
    private static final String FN_IS_PUBLIC = "isPub";
    private static final String FN_MOD_METADATA = "modM";
    private static final String FN_MOD_CONTENT = "modC";
    private static final String FN_DATE = "d";
    private static final String FN_CHANGE_DATE = "cd";
    private static final String FN_SIZE = "sz";
    private static final String FN_UID = "uid";
    private static final String FN_IS_RECURRING = "isRecur";
    private static final String FN_HAS_EXCEPTIONS = "hasEx";
    private static final String FN_ALARM = "alarm";
    private static final String FN_DEFAULT_INST = "defInst";
    private static final String FN_NUM_INST = "numInst";
    private static final String FN_INST = "inst";
    private static final String FN_RANGE_START = "rgStart";
    private static final String FN_RANGE_END = "rgEnd";

    CalendarItemData(Metadata meta) throws ServiceException {
        type = MailItem.Type.of((byte) meta.getLong(FN_TYPE));
        mFolderId = (int) meta.getLong(FN_FOLDER_ID);
        mCalItemId = (int) meta.getLong(FN_CALITEM_ID);
        mFlags = meta.get(FN_FLAGS, null);
        mTags = DbTag.deserializeTags(meta.get(FN_TAGS, null));
        mIsPublic = meta.getBool(FN_IS_PUBLIC);
        mModMetadata = (int) meta.getLong(FN_MOD_METADATA);
        mModContent = (int) meta.getLong(FN_MOD_CONTENT);
        mDate = meta.getLong(FN_DATE);
        mChangeDate = meta.getLong(FN_CHANGE_DATE);
        mSize = meta.getLong(FN_SIZE);
        mUid = meta.get(FN_UID, null);
        mIsRecurring = meta.getBool(FN_IS_RECURRING);
        mHasExceptions = meta.getBool(FN_HAS_EXCEPTIONS, false);
        Metadata metaAlarm = meta.getMap(FN_ALARM, true);
        if (metaAlarm != null)
            mAlarm = new AlarmData(metaAlarm);
        Metadata metaDefInst = meta.getMap(FN_DEFAULT_INST, true);
        if (metaDefInst != null)
            mDefaultData = new FullInstanceData(metaDefInst);
        int numInst = (int) meta.getLong(FN_NUM_INST, 0);
        if (numInst > 0) {
            List<InstanceData> instances = new ArrayList<InstanceData>(numInst);
            for (int i = 0; i < numInst; i++) {
                Metadata metaInst = meta.getMap(FN_INST + i, true);
                if (metaInst != null) {
                    InstanceData inst = null;
                    if (FullInstanceData.isFullInstanceMeta(metaInst))
                        inst = new FullInstanceData(metaInst);
                    else
                        inst = new InstanceData(metaInst);
                    instances.add(inst);
                }
            }
            mInstances = instances;
        } else {
            mInstances = new ArrayList<InstanceData>(0);
        }
        mActualRangeStart = meta.getLong(FN_RANGE_START);
        mActualRangeEnd = meta.getLong(FN_RANGE_END);
    }

    Metadata encodeMetadata() {
        Metadata meta = new Metadata();
        meta.put(FN_TYPE, type.toByte());
        meta.put(FN_FOLDER_ID, mFolderId);
        meta.put(FN_CALITEM_ID, mCalItemId);
        meta.put(FN_FLAGS, mFlags);
        meta.put(FN_TAGS, DbTag.serializeTags(mTags));
        meta.put(FN_IS_PUBLIC, mIsPublic);
        meta.put(FN_MOD_METADATA, mModMetadata);
        meta.put(FN_MOD_CONTENT, mModContent);
        meta.put(FN_DATE, mDate);
        meta.put(FN_CHANGE_DATE, mChangeDate);
        meta.put(FN_SIZE, mSize);
        meta.put(FN_UID, mUid);
        meta.put(FN_IS_RECURRING, mIsRecurring);
        meta.put(FN_HAS_EXCEPTIONS, mHasExceptions);
        if (mAlarm != null)
            meta.put(FN_ALARM, mAlarm.encodeMetadata());
        if (mDefaultData != null)
            meta.put(FN_DEFAULT_INST, mDefaultData.encodeMetadata());
        if (mInstances != null) {
            meta.put(FN_NUM_INST, mInstances.size());
            int i = 0;
            for (InstanceData inst : mInstances) {
                meta.put(FN_INST + i, inst.encodeMetadata());
                i++;
            }
        }
        meta.put(FN_RANGE_START, mActualRangeStart);
        meta.put(FN_RANGE_END, mActualRangeEnd);
        return meta;
    }
}
