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
import com.zimbra.cs.mailbox.calendar.Alarm;

public class AlarmData {
    private long mNextAt = Long.MAX_VALUE;
    private long mNextInstStart;  // start time of the instance that mNextAt alarm is for
    private int mInvId;
    private int mCompNum;
    private String mSummary;  // meeting subject
    private String mLocation;
    private Alarm mAlarm;

    public AlarmData(long next, long nextInstStart, int invId, int compNum,
                      String summary, String location, Alarm alarm) {
        init(next, nextInstStart, invId, compNum, summary, location, alarm);
    }

    private void init(long next, long nextInstStart, int invId, int compNum,
                      String summary, String location, Alarm alarm) {
        mNextAt = next;
        mNextInstStart = nextInstStart;
        mInvId = invId;
        mCompNum = compNum;
        mSummary = summary;
        mLocation = location;
        mAlarm = alarm;
    }

    public long getNextAt() { return mNextAt; }
    public long getNextInstanceStart() { return mNextInstStart; }
    public int getInvId() { return mInvId; }
    public int getCompNum() { return mCompNum; }
    public String getSummary() { return mSummary; }
    public String getLocation() { return mLocation; }
    public Alarm getAlarm() { return mAlarm; }

    private static final String FN_NEXT_AT = "na";
    private static final String FN_NEXT_INSTANCE_START = "nis";
    private static final String FN_INV_ID = "invId";
    private static final String FN_COMP_NUM = "compNum";
    private static final String FN_SUMMARY = "summ";
    private static final String FN_LOCATION = "loc";
    private static final String FN_ALARM = "alarm";

    AlarmData(Metadata meta) throws ServiceException {
        long nextAt = meta.getLong(FN_NEXT_AT);
        long nextInstStart = meta.getLong(FN_NEXT_INSTANCE_START);
        int invId = (int) meta.getLong(FN_INV_ID);
        int compNum = (int) meta.getLong(FN_COMP_NUM);
        String summary = meta.get(FN_SUMMARY, null);
        String location = meta.get(FN_LOCATION, null);
        Alarm alarm = null;
        Metadata metaAlarm = meta.getMap(FN_ALARM, true);
        if (metaAlarm != null)
            alarm = Alarm.decodeMetadata(metaAlarm);
        init(nextAt, nextInstStart, invId, compNum, summary, location, alarm);
    }

    Metadata encodeMetadata() {
        Metadata meta = new Metadata();
        meta.put(FN_NEXT_AT, mNextAt);
        meta.put(FN_NEXT_INSTANCE_START, mNextInstStart);
        meta.put(FN_INV_ID, mInvId);
        meta.put(FN_COMP_NUM, mCompNum);
        meta.put(FN_SUMMARY, mSummary);
        meta.put(FN_LOCATION, mLocation);
        if (mAlarm != null)
            meta.put(FN_ALARM, mAlarm.encodeMetadata());
        return meta;
    }
}
