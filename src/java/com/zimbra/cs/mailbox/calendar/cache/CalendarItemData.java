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
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Metadata;

// an appointment/task and expanded instances over a time range
public class CalendarItemData {
    // ZCS-specific meta data
    private int mLastModified;  // mod_metadata db column value of appointment/task item
    private byte mType;  // MailItem.TYPE_APPOINTMENT or MailItem.TYPE_TASK
    private int mFolderId;
    private int mCalItemId;
    private String mFlags;
    private String mTags;
    private boolean mIsPublic;

    // change management info
    private String mUid;

    // time/recurrence
    private boolean mIsRecurring;

    // alarm
    private AlarmData mAlarm;

    // default instance data
    private FullInstanceData mDefaultData;

    // expanded/overridden instances
    private List<InstanceData> mInstances;

    // actual time range covered by expanded instances
    private long mActualRangeStart;
    private long mActualRangeEnd;

    public int getLastModified() { return mLastModified; }
    public byte getType()     { return mType; }
    public int getFolderId()  { return mFolderId; }
    public int getCalItemId() { return mCalItemId; }
    public String getFlags()  { return mFlags; }
    public String getTags()   { return mTags; }
    public boolean isPublic() { return mIsPublic; }

    public String getUid()   { return mUid; }

    public boolean isRecurring() { return mIsRecurring; }

    public AlarmData getAlarm() { return mAlarm; }
    public boolean hasAlarm()    { return mAlarm != null; }

    public FullInstanceData getDefaultData() { return mDefaultData; }
    public Iterator<InstanceData> instanceIterator() { return mInstances.iterator(); }

    public long getActualRangeStart() { return mActualRangeStart; }
    public long getActualRangeEnd()   { return mActualRangeEnd; }
    public void setActualRange(long start, long end) {
        mActualRangeStart = start;
        mActualRangeEnd = end;
    }

    public CalendarItemData(
            int lastModified,
            byte type, int folderId, int calItemId, String flags, String tags,
            String uid,
            boolean isRecurring, boolean isPublic,
            AlarmData alarm,
            FullInstanceData defaultData) {
        mLastModified = lastModified;
        mType = type; mFolderId = folderId; mCalItemId = calItemId;
        mFlags = flags; mTags = tags;
        mUid = uid;
        mIsRecurring = isRecurring; mIsPublic = isPublic;
        mAlarm = alarm;
        mDefaultData = defaultData;
        mInstances = new ArrayList<InstanceData>();
    }

    public void addInstance(InstanceData instance) {
        mInstances.add(instance);
    }

    public int getNumInstances() {
        return mInstances.size();
    }

    public CalendarItemData getSubRange(long rangeStart, long rangeEnd) {
        if (rangeStart <= mActualRangeStart && rangeEnd >= mActualRangeEnd)
            return this;
        CalendarItemData calItemData = new CalendarItemData(
                mLastModified, mType, mFolderId, mCalItemId, mFlags, mTags,
                mUid, mIsRecurring, mIsPublic, mAlarm,
                mDefaultData);
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

    private static final String FN_LAST_MODIFIED = "lastMod";
    private static final String FN_TYPE = "type";
    private static final String FN_FOLDER_ID = "fid";
    private static final String FN_CALITEM_ID = "ciid";
    private static final String FN_FLAGS = "flag";
    private static final String FN_TAGS = "tag";
    private static final String FN_IS_PUBLIC = "isPub";
    private static final String FN_UID = "uid";
    private static final String FN_IS_RECURRING = "isRecur";
    private static final String FN_ALARM = "alarm";
    private static final String FN_DEFAULT_INST = "defInst";
    private static final String FN_NUM_INST = "numInst";
    private static final String FN_INST = "inst";
    private static final String FN_RANGE_START = "rgStart";
    private static final String FN_RANGE_END = "rgEnd";

    CalendarItemData(Metadata meta) throws ServiceException {
        mLastModified = (int) meta.getLong(FN_LAST_MODIFIED);
        mType = (byte) meta.getLong(FN_TYPE);
        mFolderId = (int) meta.getLong(FN_FOLDER_ID);
        mCalItemId = (int) meta.getLong(FN_CALITEM_ID);
        mFlags = meta.get(FN_FLAGS, null);
        mTags = meta.get(FN_TAGS, null);
        mIsPublic = meta.getBool(FN_IS_PUBLIC);
        mUid = meta.get(FN_UID, null);
        mIsRecurring = meta.getBool(FN_IS_RECURRING);
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
        meta.put(FN_LAST_MODIFIED, mLastModified);
        meta.put(FN_TYPE, mType);
        meta.put(FN_FOLDER_ID, mFolderId);
        meta.put(FN_CALITEM_ID, mCalItemId);
        meta.put(FN_FLAGS, mFlags);
        meta.put(FN_TAGS, mTags);
        meta.put(FN_IS_PUBLIC, mIsPublic);
        meta.put(FN_UID, mUid);
        meta.put(FN_IS_RECURRING, mIsRecurring);
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
