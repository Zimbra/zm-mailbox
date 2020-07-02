/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Metadata;

public class CalendarData {
    private final int mFolderId;
    private final int mModSeq;  // last-modified sequence of the folder
    private final long mRangeStart;
    private final long mRangeEnd;
    private final List<CalendarItemData> mCalendarItems;
    private final Map<Integer, CalendarItemData> mCalendarItemsMap;
    private final Set<Integer> mStaleItemIds;

    CalendarData(int folderId, int modSeq, long rangeStart, long rangeEnd) {
        mFolderId = folderId;
        mModSeq = modSeq;
        mRangeStart = rangeStart;
        mRangeEnd = rangeEnd;
        mCalendarItems = new ArrayList<CalendarItemData>();
        mCalendarItemsMap = new HashMap<Integer, CalendarItemData>();
        mStaleItemIds = new HashSet<Integer>();
    }

    void addCalendarItem(CalendarItemData calItemData) {
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
            if (itemSubRange != null) {
                calData.addCalendarItem(itemSubRange);
                int itemId = itemSubRange.getCalItemId();
                if (isItemStale(itemId))
                    calData.markItemStale(itemId);
            }
        }
        return calData;
    }

    synchronized int getNumStaleItems() {
        return mStaleItemIds.size();
    }

    synchronized int markItemStale(int calItemId) {
        mStaleItemIds.add(calItemId);
        return mStaleItemIds.size();
    }

    synchronized boolean isItemStale(int calItemId) {
        return mStaleItemIds.contains(calItemId);
    }

    synchronized void copyStaleItemIdsTo(Set<Integer> copyTo) {
        copyTo.addAll(mStaleItemIds);
    }

    private static final String FN_FOLDER_ID = "fid";
    private static final String FN_MODSEQ = "modSeq";
    private static final String FN_RANGE_START = "rgStart";
    private static final String FN_RANGE_END = "rgEnd";
    private static final String FN_NUM_CALITEMS = "numCi";
    private static final String FN_CALITEM = "ci";
    private static final String FN_NUM_STALEITEMS = "numStale";
    private static final String FN_STALEITEM = "stale";

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
        int numStaleItems = (int) meta.getLong(FN_NUM_STALEITEMS, 0);
        if (numStaleItems > 0) {
            mStaleItemIds = new HashSet<>(numStaleItems);
            for (int i = 0; i < numStaleItems; i++) {
                int staleId = meta.getInt(FN_STALEITEM + i, -1);
                if (staleId != -1) {
                    mStaleItemIds.add(staleId);
                }
            }
        } else {
            mStaleItemIds = new HashSet<>(0);
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
        meta.put(FN_NUM_STALEITEMS, mStaleItemIds.size());
        i = 0;
        for (Integer staleId : mStaleItemIds) {
            meta.put(FN_STALEITEM + i, staleId);
            i++;
        }
        return meta;
    }

    @Override
    public String toString() {
        MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this)
                .add("folderId", mFolderId)
                .add("modSeq", mModSeq)
                .add("rangeStart", mRangeStart)
                .add("rangeEnd", mRangeEnd);
        if (mCalendarItems.size() > 20) {
            helper.add("calendarItems.size", mCalendarItems.size());
        } else {
            helper.add("calendarItems", mCalendarItems);
        }
        if (mStaleItemIds.size() > 20) {
            helper.add("staleItemIds.size", mStaleItemIds.size());
        } else {
            helper.add("staleItemIds", mStaleItemIds);
        }
        return helper.toString();
    }
}
