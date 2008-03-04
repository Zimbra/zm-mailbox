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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.stats.ZimbraPerf;

// TODO: [done] cache hit/miss stat
// TODO: [done] last-modified time update/check at calendar folder level
// TODO: [done] reasonable handling/caching/merging of requests outside current time range
// TODO: [done] alarm attributes
// TODO: [done] search code change to use the calendar cache (remove my hack in Search.java)
// TODO: [done] persistence - filesystem or database?  what's the IO impact?
// TODO: [done] versioning
// TODO: cache moving window of configurable size (e.g. 6 months beginning with last month)
// TODO: caching remote calendars
// TODO: TTL instead of last-modified time check, if folder configured that way or remote
// TODO: high cache stickiness (or dedicated non-LRU cache) for heavily shared, almost read-only calendars
// TODO: CLI or other mechanism to force invalidation of specific calendar/mailbox
// TODO: is the cached data friendly to JSON2 serialization?
//       currently missing: CATEGORY, CREATED, LAST-MODIFIED, DESCRIPTION, STREET, CSZ, PHONE,
//       RECUR,
//       REMINDERs, ATTENDEEs, GEO, CATEGORIES (how's this different from CATEGORY?), COMMENT,
//       RESOURCES, CONTACT, RELATED-TO
// TODO: caching free/busy responses
// TODO: DUE property of tasks
// TODO: review other TODOs throughout code (in calendar.cache package)
public class CalendarCache {

    // mSummaryCache
    //
    // key = "mboxId:folderId" (local mailbox) or "accountId:folderId" (remote mailbox)
    // value = CalendarData type
    //
    // CalendarData = {
    //   folderId, range start, range end,
    //   // list of CalendarItemData objects (one for each appointment in range)
    //   [
    //     CalendarItemData {
    //       calItemId, folderId, flags, tags, item type,
    //       last modified,
    //       actual range start, actual range end,
    //       uid, isRecurring, isPublic, alarm,
    //       default instance data (FullInstanceData type),
    //       // list of instances (FullInstanceData if an exception, InstanceData if not)
    //       [
    //         InstanceData/FullInstanceData, ...
    //       ]
    //     },
    //     CalendarItemData, ...
    //   ]

    // map entry key is "mailboxId:folderId" (local mailbox) or "accountId:folderId" (remote mailbox)
    public static class SummaryCacheKey {
        private String mKeyVal;
        public SummaryCacheKey(int mboxId, int folderId) {
            mKeyVal = mboxId + ":" + folderId;
        }
        public SummaryCacheKey(String accountId, int folderId) {
            mKeyVal = accountId + ":" + folderId;
        }
        public SummaryCacheKey(String fullyQualifiedItemId) {
            mKeyVal = fullyQualifiedItemId;
        }

        public String getKey() { return mKeyVal; }

        public boolean equals(Object other) {
            if (other instanceof SummaryCacheKey) {
                SummaryCacheKey otherKey = (SummaryCacheKey) other;
                return mKeyVal.equals(otherKey.getKey());
            }
            return false;
        }

        public int hashCode() {
            return mKeyVal.hashCode();
        }
    }

    private static CalendarItemData reloadCalendarItemOverRange(
            Mailbox mbox, CalendarItem calItem, long rangeStart, long rangeEnd)
    throws ServiceException {
        CalendarItemData calItemData = null;
        try {
            boolean rangeValid = (rangeStart >= 0 && rangeEnd > 0 && rangeStart < rangeEnd);
            if (!rangeValid)
                return null;
            
            Invite defaultInvite = calItem.getDefaultInviteOrNull();
            if (defaultInvite == null) {
                ZimbraLog.calendar.info(
                        "Could not load defaultinfo for calendar item with id=" +
                        calItem.getId() + "; SKIPPING");
                return null;
            }
            String defaultFba = null;
            if (calItem instanceof Appointment)
                defaultFba = ((Appointment) calItem).getEffectiveFreeBusyActual(defaultInvite, null);

            AlarmData alarm = null;
            CalendarItem.AlarmData calItemAlarmData = calItem.getAlarmData();
            long alarmTime = 0;
            long alarmInst = 0;
            if (calItemAlarmData != null) {
                alarmTime = calItemAlarmData.getNextAt();
                alarmInst = calItemAlarmData.getNextInstanceStart();

                int alarmInvId = calItemAlarmData.getInvId();
                int alarmCompNum = calItemAlarmData.getCompNum();
                String summary = null, location = null;
                Invite alarmInv = calItem.getInvite(alarmInvId, alarmCompNum);
                if (alarmInv != null) {
                    summary = alarmInv.getName();
                    location = alarmInv.getLocation();
                }
                alarm = new AlarmData(calItemAlarmData.getNextAt(), calItemAlarmData.getNextInstanceStart(),
                                      alarmInvId, alarmCompNum, summary, location, calItemAlarmData.getAlarm());
            }

            Long defDtStartLong = null;
            Long defDurationLong = null;
            ParsedDateTime defDtStart = defaultInvite.getStartTime();
            if (defDtStart != null) {
                defDtStartLong = new Long(defDtStart.getUtcTime());
                ParsedDateTime defDtEnd = defaultInvite.getEffectiveEndTime();
                if (defDtEnd != null)
                    defDurationLong = new Long(defDtEnd.getUtcTime() - defDtStartLong.longValue());
            }
            String defaultEffectivePartStat = calItem.getEffectivePartStat(defaultInvite, null);
            FullInstanceData defaultData =
                new FullInstanceData(defaultInvite, defDtStartLong, defDurationLong,
                                     defaultEffectivePartStat, defaultFba, null, null);
            calItemData = new CalendarItemData(
                    calItem.getModifiedSequence(),
                    calItem.getType(), calItem.getFolderId(), calItem.getId(),
                    calItem.getFlagString(), calItem.getTagString(),
                    defaultInvite.getUid(), defaultInvite.isRecurrence(), calItem.isPublic(),
                    alarm, defaultData);

            long actualRangeStart = 0;
            long actualRangeEnd = 0;
            int numInstances = 0;
            Collection<CalendarItem.Instance> instances = calItem.expandInstances(rangeStart, rangeEnd, true);
            for (CalendarItem.Instance inst : instances) {
                try {
                    long instStart = inst.getStart();
                    long duration = inst.getEnd() - instStart;
                    Long instStartLong = instStart > 0 ? new Long(instStart) : null;
                    Long durationLong = duration > 0 ? new Long(duration) : null;

                    // For an instance whose alarm time is within the time range, we must
                    // include it even if its start time is after the range.
                    long startOrAlarm = instStart == alarmInst ? alarmTime : instStart;

                    if (!inst.isTimeless() &&
                        (startOrAlarm >= rangeEnd || inst.getEnd() <= rangeStart)) {
                        continue;
                    }
                    numInstances++;

                    if (!inst.isTimeless()) {
                        if (actualRangeStart == 0 || startOrAlarm < actualRangeStart)
                            actualRangeStart = startOrAlarm;
                        if (inst.getEnd() > actualRangeEnd)
                            actualRangeEnd = inst.getEnd();
                    }

                    InviteInfo invId = inst.getInviteInfo();
                    Invite inv = calItem.getInvite(invId.getMsgId(), invId.getComponentId());
                    Long alarmAt = instStart == alarmInst ? new Long(alarmTime) : null;

                    String fba = inv.getFreeBusyActual();
                    if (calItem instanceof Appointment)
                        fba = ((Appointment) calItem).getEffectiveFreeBusyActual(inv, inst);
                    String effectivePartStat = calItem.getEffectivePartStat(inv, inst);
                    InstanceData instData;
                    if (!inst.isException()) {
                        Long tzOffset = instStartLong != null ? Util.getTZOffsetForInvite(inv, instStart) : null;
                        instData = new InstanceData(
                                instStartLong, durationLong, alarmAt, tzOffset,
                                effectivePartStat, fba, inv.getPercentComplete(),
                                defaultData);
                    } else {
                        instData = new FullInstanceData(inv, instStartLong, durationLong, effectivePartStat, fba, alarmAt, defaultData);
                    }
                    calItemData.addInstance(instData);
                } catch (MailServiceException.NoSuchItemException e) {
                    ZimbraLog.calendar.info("Error could not get instance "+inst.getMailItemId()+"-"+inst.getComponentNum()+
                        " for appt "+calItem.getId(), e);
                }
            }
            if (numInstances < 1)
                return null;
            calItemData.setActualRange(actualRangeStart, actualRangeEnd);
        } catch(MailServiceException.NoSuchItemException e) {
            ZimbraLog.calendar.info("Error could not get default invite for calendar item: "+ calItem.getId(), e);
        } catch (RuntimeException e) {
            ZimbraLog.calendar.info("Caught Exception "+e+ " while getting summary info for calendar item: "+calItem.getId(), e);
        }
        return calItemData;
    }

    private static CalendarData reloadCalendarOverRange(OperationContext octxt, Mailbox mbox, int folderId,
                                                        byte itemType, long rangeStart, long rangeEnd)
    throws ServiceException {
        if (rangeEnd < rangeStart)
            throw ServiceException.INVALID_REQUEST("End time must be after Start time", null);
        long days = (rangeEnd - rangeStart) / MSEC_PER_DAY;
        if (days > MAX_PERIOD_SIZE_IN_DAYS)
            throw ServiceException.INVALID_REQUEST("Requested range is too large (Maximum " +
                                                   MAX_PERIOD_SIZE_IN_DAYS + " days)", null);

        Folder folder = mbox.getFolderById(octxt, folderId);
        CalendarData calData = new CalendarData(folderId, folder.getImapMODSEQ(), rangeStart, rangeEnd);
        Collection<CalendarItem> calItems =
            mbox.getCalendarItemsForRange(itemType, octxt, rangeStart, rangeEnd, folderId, null);
        for (CalendarItem calItem : calItems) {
            CalendarItemData calItemData = reloadCalendarItemOverRange(mbox, calItem, rangeStart, rangeEnd);
            if (calItemData != null)
                calData.addCalendarItem(calItemData);
        }
        return calData;  // return a non-null object even if there are no items in the range
    }


    private static CalendarCache sInstance;
    public static CalendarCache getInstance() { return sInstance; }

    private static final int sRangeMonthFrom;
    private static final int sRangeNumMonths;
    private static final int sLRUCapacity;

    private static final long MSEC_PER_DAY = 1000 * 60 * 60 * 24;
    private static final long MAX_PERIOD_SIZE_IN_DAYS = 366;

    static {
        sRangeMonthFrom = DebugConfig.calendarCacheRangeMonthFrom;
        sRangeNumMonths = DebugConfig.calendarCacheRangeMonths;
        sLRUCapacity = DebugConfig.calendarCacheLRUSize;
        sInstance = new CalendarCache(sLRUCapacity);
    }

    // LRU cache containing range-limited calendar summary by calendar folder
    private Map<SummaryCacheKey, CalendarData> mSummaryCache;

    private CalendarCache(final int capacity) {
        mSummaryCache = new LinkedHashMap<SummaryCacheKey, CalendarData>(capacity, 1.0f, true) {
            protected boolean removeEldestEntry(Map.Entry<SummaryCacheKey, CalendarData> eldest) {
                return size() > capacity;
            }
        };
    }

    private static enum CacheLevel { Memory, File, Miss }

    // get summary for all appts/tasks in a calendar folder
    public CalendarData getCalendarSummary(OperationContext octxt, Mailbox mbox, int folderId,
    									   byte itemType, long rangeStart, long rangeEnd,
    									   boolean computeSubRange)
    throws ServiceException {
        if (rangeStart > rangeEnd)
            throw ServiceException.INVALID_REQUEST("End time must be after Start time", null);

        if (!DebugConfig.calendarEnableCache) {
            ZimbraPerf.COUNTER_CALENDAR_CACHE_HIT.increment(0);
            ZimbraPerf.COUNTER_CALENDAR_CACHE_MEM_HIT.increment(0);
            return reloadCalendarOverRange(octxt, mbox, folderId, itemType, rangeStart, rangeEnd);
        }

        int lruSize = 0;
        CacheLevel dataFrom = CacheLevel.Memory;

        SummaryCacheKey key = new SummaryCacheKey(mbox.getId(), folderId);
        CalendarData calData = null;
        synchronized (mSummaryCache) {
            if (sLRUCapacity > 0) {
                calData = mSummaryCache.get(key);
                lruSize = mSummaryCache.size();
            }
        }

        Folder folder = mbox.getFolderById(octxt, folderId);
        int currentModSeq = folder.getImapMODSEQ();

        if (calData == null) {
            // Load from file.
            try {
                calData = FileStore.loadCalendarData(mbox.getId(), folderId, currentModSeq);
                if (calData != null && sLRUCapacity > 0) {
                    synchronized (mSummaryCache) {
                        mSummaryCache.put(key, calData);
                        lruSize = mSummaryCache.size();
                    }
                }
                dataFrom = CacheLevel.File;
            } catch (ServiceException e) {
                ZimbraLog.calendar.warn("Error loading cached calendar summary", e);
            }
        }

        Pair<Long, Long> defaultRange = null;
        if (calData != null) {
            if (calData.getModSeq() < currentModSeq) {
                // Cached data is stale.
                calData = null;
            } else if (rangeStart < calData.getRangeStart() || rangeEnd > calData.getRangeEnd()) {
                // Requested range is not within cached range.  Recompute cached range in the hope
                // that the new range will cover the requested range.
                defaultRange = Util.getMonthsRange(System.currentTimeMillis(),
                                                   sRangeMonthFrom, sRangeNumMonths);
                if (calData.getRangeStart() != defaultRange.getFirst() ||
                    calData.getRangeEnd() != defaultRange.getSecond()) {
                    calData = null;
                }
            }
        }

        // Recompute data if we must, and add to cache.
        if (calData == null) {
            if (defaultRange == null)
                defaultRange = Util.getMonthsRange(System.currentTimeMillis(),
                                                   sRangeMonthFrom, sRangeNumMonths);
            calData = reloadCalendarOverRange(octxt, mbox, folderId, itemType,
                                              defaultRange.getFirst(), defaultRange.getSecond());
            synchronized (mSummaryCache) {
                if (sLRUCapacity > 0) {
                    mSummaryCache.put(key, calData);
                    lruSize = mSummaryCache.size();
                }
            }
            dataFrom = CacheLevel.Miss;
            try {
                FileStore.saveCalendarData(mbox.getId(), calData);  // persist it
            } catch (ServiceException e) {
                ZimbraLog.calendar.warn("Error persisting calendar summary cache", e);
            }
        }

        CalendarData retval;
        if (rangeStart >= calData.getRangeStart() && rangeEnd <= calData.getRangeEnd()) {
            // Requested range is within cached range.
            if (computeSubRange) {
                retval = calData.getSubRange(rangeStart, rangeEnd);
            } else {
                retval = calData;
            }
        } else {
            // Requested range is outside the currently cached range.
            dataFrom = CacheLevel.Miss;
            retval = reloadCalendarOverRange(octxt, mbox, folderId, itemType, rangeStart, rangeEnd);
        }

        // hit/miss tracking
        // COUNTER_CALENDAR_CACHE_HIT - A hit is a successful lookup from either memory or file.
        // COUNTER_CALENDAR_CACHE_MEM_HIT - A hit is a successful lookup from memory only.
        switch (dataFrom) {
        case Memory:
            ZimbraPerf.COUNTER_CALENDAR_CACHE_HIT.increment(1);
            ZimbraPerf.COUNTER_CALENDAR_CACHE_MEM_HIT.increment(1);
            break;
        case File:
            ZimbraPerf.COUNTER_CALENDAR_CACHE_HIT.increment(1);
            ZimbraPerf.COUNTER_CALENDAR_CACHE_MEM_HIT.increment(0);
            break;
        case Miss:
        default:
            ZimbraPerf.COUNTER_CALENDAR_CACHE_HIT.increment(0);
            ZimbraPerf.COUNTER_CALENDAR_CACHE_MEM_HIT.increment(0);
            break;
        }
        ZimbraPerf.COUNTER_CALENDAR_CACHE_LRU_SIZE.increment(lruSize);

        return retval;
    }

    public void invalidateSummary(Mailbox mbox, int folderId) {
        if (!DebugConfig.calendarEnableCache)
            return;

        int mboxId = mbox.getId();
        SummaryCacheKey key = new SummaryCacheKey(mboxId, folderId);
        synchronized (mSummaryCache) {
            mSummaryCache.remove(key);
        }
        try {
            FileStore.deleteCalendarData(mboxId, folderId);
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Error deleting calendar summary cache", e);
        }
    }
}
