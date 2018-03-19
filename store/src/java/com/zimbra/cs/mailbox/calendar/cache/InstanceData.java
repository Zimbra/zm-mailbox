/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Metadata;

// instance that is expanded from recurrence but not overridden by an exception
// null value returned by a getter means to inherit from default instance of appointment/task
public class InstanceData {
    private static final String FN_RECURRENCE_ID_Z = "ridZ";
    private static final String FN_DTSTART = "st";
    private static final String FN_DURATION = "dur";
    private static final String FN_ALARM_AT = "alarm";
    private static final String FN_TZOFFSET = "tzo";
    private static final String FN_PARTSTAT = "ptst";
    private static final String FN_FREEBUSY_ACTUAL = "fba";
    private static final String FN_PERCENT_COMPLETE = "pctcomp";
    private static final String FN_COLOR = "c";

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
    private Color mRGBColor;

    public String getRecurIdZ() { return mRecurIdZ; }
    public Long getDtStart()  { return mDtStart; }
    public Long getDuration() { return mDuration; }
    public Long getAlarmAt()  { return mAlarmAt; }
    public Long getTZOffset() { return mTZOffset; }
    public String getPartStat() { return mPartStat; }
    public String getFreeBusyActual() { return mFreeBusyActual; }
    public String getPercentComplete() { return mPercentComplete; }
    public Color getRgbColor() { return mRGBColor; }

    public InstanceData(String recurIdZ, Long dtStart, Long duration, Long alarmAt, Long tzOffset,
            String partStat, String freeBusyActual, String percentComplete) {
        this(recurIdZ, dtStart, duration, alarmAt, tzOffset, partStat, freeBusyActual, percentComplete, (Color)null);
    }

    public InstanceData(String recurIdZ, Long dtStart, Long duration, Long alarmAt, Long tzOffset,
                        String partStat, String freeBusyActual, String percentComplete, Color color) {
        init(recurIdZ, dtStart, duration, alarmAt, tzOffset, partStat, freeBusyActual, percentComplete, color);
    }

    private void init(String recurIdZ, Long dtStart, Long duration, Long alarmAt, Long tzOffset,
                      String partStat, String freeBusyActual, String percentComplete, Color color) {
        mRecurIdZ = recurIdZ;
        mDtStart = dtStart != null && dtStart.longValue() != 0 ? dtStart : null;
        mDuration = duration != null && duration.longValue() != 0 ? duration : null;
        mAlarmAt = alarmAt != null && alarmAt.longValue() != 0 ? alarmAt : null;
        mTZOffset = tzOffset;
        mPartStat = partStat;
        mFreeBusyActual = freeBusyActual;
        mPercentComplete = percentComplete;
        mRGBColor = color;
    }

    public InstanceData(String recurIdZ, Long dtStart, Long duration, Long alarmAt, Long tzOffset,
            String partStat, String freeBusyActual, String percentComplete,
            InstanceData defaultInstance) {
        this(recurIdZ, dtStart, duration, alarmAt, tzOffset, partStat, freeBusyActual, percentComplete, defaultInstance, (Color)null);
    }

    public InstanceData(String recurIdZ, Long dtStart, Long duration, Long alarmAt, Long tzOffset,
                        String partStat, String freeBusyActual, String percentComplete,
                        InstanceData defaultInstance, Color color) {
        this(recurIdZ, dtStart, duration, alarmAt, tzOffset, partStat, freeBusyActual, percentComplete, color);
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

    InstanceData(Metadata meta) throws ServiceException {
        String recurIdZ = meta.get(FN_RECURRENCE_ID_Z, null);
        Long dtStart = null, duration = null, alarmAt = null, tzOffset = null;
        if (meta.containsKey(FN_DTSTART))
            dtStart = Long.valueOf(meta.getLong(FN_DTSTART));
        if (meta.containsKey(FN_DURATION))
            duration = Long.valueOf(meta.getLong(FN_DURATION));
        if (meta.containsKey(FN_ALARM_AT))
            alarmAt = Long.valueOf(meta.getLong(FN_ALARM_AT));
        if (meta.containsKey(FN_TZOFFSET))
            tzOffset = Long.valueOf(meta.getLong(FN_TZOFFSET));
        String ptst = meta.get(FN_PARTSTAT, null);
        String fba = meta.get(FN_FREEBUSY_ACTUAL, null);
        String pctComp = meta.get(FN_PERCENT_COMPLETE, null);
        Color color = Color.fromMetadata(meta.getLong(FN_COLOR, MailItem.DEFAULT_COLOR));
        init(recurIdZ, dtStart, duration, alarmAt, tzOffset, ptst, fba, pctComp, color);
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
        if (mRGBColor != null && mRGBColor.getMappedColor() != MailItem.DEFAULT_COLOR) {
            meta.put(FN_COLOR, mRGBColor.toMetadata());
        }
        return meta;
    }
}
