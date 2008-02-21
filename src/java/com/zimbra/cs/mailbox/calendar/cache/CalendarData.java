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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Metadata;

public class CalendarData {
    private int mFolderId;
    private int mModSeq;  // last-modified sequence of the folder
    private long mRangeStart;
    private long mRangeEnd;
    private List<CalendarItemData> mCalendarItems;
    private Map<Integer, CalendarItemData> mCalendarItemsMap;

    public CalendarData(int folderId, int modSeq, long rangeStart, long rangeEnd) {
        mFolderId = folderId;
        mModSeq = modSeq;
        mRangeStart = rangeStart;
        mRangeEnd = rangeEnd;
        mCalendarItems = new ArrayList<CalendarItemData>();
        mCalendarItemsMap = new HashMap<Integer, CalendarItemData>();
    }

    public void addCalendarItem(CalendarItemData calItemData) {
        mCalendarItems.add(calItemData);
        mCalendarItemsMap.put(calItemData.getCalItemId(), calItemData);
    }

    public CalendarItemData getCalendarItemData(int calItemId) {
        return mCalendarItemsMap.get(calItemId);
    }

    public int getFolderId()    { return mFolderId; }
    public int getModSeq()      { return mModSeq; }
    public long getRangeStart() { return mRangeStart; }
    public long getRangeEnd()   { return mRangeEnd; }

    public Iterator<CalendarItemData> calendarItemIterator() { return mCalendarItems.iterator(); }
    public int getNumItems() { return mCalendarItems.size(); }

    public CalendarData getSubRange(long rangeStart, long rangeEnd) {
        if (rangeStart <= mRangeStart && rangeEnd >= mRangeEnd)
            return this;
        CalendarData calData = new CalendarData(mFolderId, mModSeq, rangeStart, rangeEnd);
        for (CalendarItemData calItemData : mCalendarItems) {
            CalendarItemData itemSubRange = calItemData.getSubRange(rangeStart, rangeEnd);
            if (itemSubRange != null)
                calData.addCalendarItem(itemSubRange);
        }
        return calData;
    }

    private static final String FN_FOLDER_ID = "fid";
    private static final String FN_MODSEQ = "modSeq";
    private static final String FN_RANGE_START = "rgStart";
    private static final String FN_RANGE_END = "rgEnd";
    private static final String FN_NUM_CALITEMS = "numCi";
    private static final String FN_CALITEM = "ci";

    CalendarData(Metadata meta) throws ServiceException {
        mFolderId = (int) meta.getLong(FN_FOLDER_ID);
        mModSeq = (int) meta.getLong(FN_MODSEQ);
        mRangeStart = meta.getLong(FN_RANGE_START);
        mRangeEnd = meta.getLong(FN_RANGE_END);
        int numCalItems = (int) meta.getLong(FN_NUM_CALITEMS);
        if (numCalItems > 0) {
            mCalendarItems = new ArrayList<CalendarItemData>(numCalItems);
            mCalendarItemsMap = new HashMap<Integer, CalendarItemData>(numCalItems);
            for (int i = 0; i < numCalItems; i++) {
                Metadata metaCalItem = meta.getMap(FN_CALITEM + i, true);
                if (metaCalItem != null) {
                    CalendarItemData calItemData = new CalendarItemData(metaCalItem);
                    addCalendarItem(calItemData);
                }
            }
        } else {
            mCalendarItems = new ArrayList<CalendarItemData>(0);
            mCalendarItemsMap = new HashMap<Integer, CalendarItemData>(0);
        }
    }

    Metadata encodeMetadata() {
        Metadata meta = new Metadata();
        meta.put(FN_FOLDER_ID, mFolderId);
        meta.put(FN_MODSEQ, mModSeq);
        meta.put(FN_RANGE_START, mRangeStart);
        meta.put(FN_RANGE_END, mRangeEnd);
        meta.put(FN_NUM_CALITEMS, mCalendarItems.size());
        int i = 0;
        for (CalendarItemData calItemData : mCalendarItems) {
            meta.put(FN_CALITEM + i, calItemData.encodeMetadata());
            i++;
        }
        return meta;
    }
}
