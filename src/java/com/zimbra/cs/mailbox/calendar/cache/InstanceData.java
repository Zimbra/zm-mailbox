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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Metadata;

// instance that is expanded from recurrence but not overridden by an exception
// null value returned by a getter means to inherit from default instance of appointment/task
public class InstanceData {
    // time
    private String mRecurIdZ;  // RECURRENCE-ID of this instance in "Z" (UTC) timezone
    private Long mDtStart;
    private Long mDuration;
    private Long mAlarmAt;  // set if next alarm on calendar item is on this instance
    private Long mTZOffset;
    // common meta data
    private String mPartStat;  // this user's partStat (result of accept/decline)
    // appointment-only meta data
    private String mFreeBusyActual;
    // task-only meta data
    private String mPercentComplete;

    public String getRecurIdZ() { return mRecurIdZ; }
    public Long getDtStart()  { return mDtStart; }
    public Long getDuration() { return mDuration; }
    public Long getAlarmAt()  { return mAlarmAt; }
    public Long getTZOffset() { return mTZOffset; }
    public String getPartStat() { return mPartStat; }
    public String getFreeBusyActual() { return mFreeBusyActual; }
    public String getPercentComplete() { return mPercentComplete; }

    public InstanceData(String recurIdZ, Long dtStart, Long duration, Long alarmAt, Long tzOffset,
                        String partStat, String freeBusyActual, String percentComplete) {
        init(recurIdZ, dtStart, duration, alarmAt, tzOffset, partStat, freeBusyActual, percentComplete);
    }

    private void init(String recurIdZ, Long dtStart, Long duration, Long alarmAt, Long tzOffset,
                      String partStat, String freeBusyActual, String percentComplete) {
        mRecurIdZ = recurIdZ;
        mDtStart = dtStart != null && dtStart.longValue() != 0 ? dtStart : null;
        mDuration = duration != null && duration.longValue() != 0 ? duration : null;
        mAlarmAt = alarmAt != null && alarmAt.longValue() != 0 ? alarmAt : null;
        mTZOffset = tzOffset;
        mPartStat = partStat;
        mFreeBusyActual = freeBusyActual;
        mPercentComplete = percentComplete;
    }

    public InstanceData(String recurIdZ, Long dtStart, Long duration, Long alarmAt, Long tzOffset,
                        String partStat, String freeBusyActual, String percentComplete,
                        InstanceData defaultInstance) {
        this(recurIdZ, dtStart, duration, alarmAt, tzOffset, partStat, freeBusyActual, percentComplete);
        clearUnchangedFields(defaultInstance);
    }

    protected void clearUnchangedFields(InstanceData other) {
        if (other != null) {
            // Check all fields except mRecurIdZ and mDtStart.
            if (Util.sameValues(mDuration, other.getDuration()))
                mDuration = null;
            if (Util.sameValues(mAlarmAt, other.getAlarmAt()))
                mAlarmAt = null;
            if (Util.sameValues(mTZOffset, other.getTZOffset()))
                mTZOffset = null;
            if (Util.sameValues(mPartStat, other.getPartStat()))
                mPartStat = null;
            if (Util.sameValues(mFreeBusyActual, other.getFreeBusyActual()))
                mFreeBusyActual = null;
            if (Util.sameValues(mPercentComplete, other.getPercentComplete()))
                mPercentComplete = null;
        }
    }

    private static final String FN_RECURRENCE_ID_Z = "ridZ";
    private static final String FN_DTSTART = "st";
    private static final String FN_DURATION = "dur";
    private static final String FN_ALARM_AT = "alarm";
    private static final String FN_TZOFFSET = "tzo";
    private static final String FN_PARTSTAT = "ptst";
    private static final String FN_FREEBUSY_ACTUAL = "fba";
    private static final String FN_PERCENT_COMPLETE = "pctcomp";

    InstanceData(Metadata meta) throws ServiceException {
        String recurIdZ = meta.get(FN_RECURRENCE_ID_Z, null);
        Long dtStart = null, duration = null, alarmAt = null, tzOffset = null;
        if (meta.containsKey(FN_DTSTART))
            dtStart = new Long(meta.getLong(FN_DTSTART));
        if (meta.containsKey(FN_DURATION))
            duration = new Long(meta.getLong(FN_DURATION));
        if (meta.containsKey(FN_ALARM_AT))
            alarmAt = new Long(meta.getLong(FN_ALARM_AT));
        if (meta.containsKey(FN_TZOFFSET))
            tzOffset = new Long(meta.getLong(FN_TZOFFSET));
        String ptst = meta.get(FN_PARTSTAT, null);
        String fba = meta.get(FN_FREEBUSY_ACTUAL, null);
        String pctComp = meta.get(FN_PERCENT_COMPLETE, null);
        init(recurIdZ, dtStart, duration, alarmAt, tzOffset, ptst, fba, pctComp);
    }

    Metadata encodeMetadata() {
        Metadata meta = new Metadata();
        meta.put(FN_RECURRENCE_ID_Z, mRecurIdZ);
        if (mDtStart != null)
            meta.put(FN_DTSTART, mDtStart.longValue());
        if (mDuration != null)
            meta.put(FN_DURATION, mDuration.longValue());
        if (mAlarmAt != null)
            meta.put(FN_ALARM_AT, mAlarmAt.longValue());
        if (mTZOffset != null)
            meta.put(FN_TZOFFSET, mTZOffset.longValue());
        meta.put(FN_PARTSTAT, mPartStat);
        meta.put(FN_FREEBUSY_ACTUAL, mFreeBusyActual);
        meta.put(FN_PERCENT_COMPLETE, mPercentComplete);
        return meta;
    }
}
