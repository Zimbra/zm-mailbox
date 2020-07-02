/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.BaseItemInfo;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxManager.FetchMode;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.acl.FolderACL;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.mailbox.util.TagUtil;
import com.zimbra.cs.service.mail.CalendarUtils;
import com.zimbra.cs.session.PendingLocalModifications;
import com.zimbra.cs.session.PendingModifications.Change;
import com.zimbra.cs.session.PendingModifications.ModificationKey;
import com.zimbra.cs.stats.ZimbraPerf;

// TODO: caching remote calendars
// TODO: TTL instead of last-modified time check, if folder configured that way or remote
// TODO: high cache stickiness (or dedicated non-LRU cache) for heavily shared, almost read-only calendars
// TODO: is the cached data friendly to JSON2 serialization?
//       currently missing: CATEGORY, CREATED, LAST-MODIFIED, DESCRIPTION, STREET, CSZ, PHONE,
//       RECUR,
//       REMINDERs, ATTENDEEs, GEO, CATEGORIES (how's this different from CATEGORY?), COMMENT,
//       RESOURCES, CONTACT, RELATED-TO
// TODO: caching free/busy responses
// TODO: DUE property of tasks
// TODO: review other TODOs throughout code (in calendar.cache package)

public class CalSummaryCache {

    public static CalendarItemData reloadCalendarItemOverRange(CalendarItem calItem, long rangeStart, long rangeEnd)
    throws ServiceException {
        CalendarItemData calItemData = null;
        try {
            boolean rangeValid = (rangeStart >= CalendarUtils.MICROSOFT_EPOC_START_MS_SINCE_EPOC &&
                    rangeEnd > CalendarUtils.MICROSOFT_EPOC_START_MS_SINCE_EPOC && rangeStart < rangeEnd);
            if (!rangeValid) {
                return null;
            }

            Invite defaultInvite = calItem.getDefaultInviteOrNull();
            if (defaultInvite == null) {
                ZimbraLog.calendar.info(
                        "Could not load defaultinfo for calendar item with id=" + calItem.getId() + "; SKIPPING");
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
                defDtStartLong = Long.valueOf(defDtStart.getUtcTime());
                ParsedDateTime defDtEnd = defaultInvite.getEffectiveEndTime();
                if (defDtEnd != null)
                    defDurationLong = Long.valueOf(defDtEnd.getUtcTime() - defDtStartLong.longValue());
            }
            String defaultEffectivePartStat = calItem.getEffectivePartStat(defaultInvite, null);
            FullInstanceData defaultData =
                new FullInstanceData(defaultInvite, null, defDtStartLong, defDurationLong,
                                     defaultEffectivePartStat, defaultFba, null);
            calItemData = new CalendarItemData(
                    calItem.getType(), calItem.getFolderId(), calItem.getId(),
                    calItem.getFlagString(), calItem.getTags(), TagUtil.getTagIdString(calItem),
                    calItem.getModifiedSequence(), calItem.getSavedSequence(),
                    calItem.getDate(), calItem.getChangeDate(), calItem.getSize(),
                    defaultInvite.getUid(), defaultInvite.isRecurrence(), calItem.hasExceptions(), calItem.isPublic(),
                    alarm, defaultData);

            long actualRangeStart = 0;
            long actualRangeEnd = 0;
            int numInstances = 0;
            Collection<CalendarItem.Instance> instances = calItem.expandInstances(rangeStart, rangeEnd, true);
            for (CalendarItem.Instance inst : instances) {
                try {
                    long instStart = inst.getStart();
                    long duration = inst.getEnd() - instStart;
                    // 0 means "no DTSTART", however, note that negative numbers are valid
                    Long instStartLong = instStart != 0 ? Long.valueOf(instStart) : null;
                    Long durationLong = duration > 0 ? Long.valueOf(duration) : null;

                    // For an instance whose alarm time is within the time range, we must
                    // include it even if its start time is after the range.
                    long startOrAlarm = instStart == alarmInst ? alarmTime : instStart;
                    boolean hasTimes = inst.hasStart() && inst.hasEnd();
                    if (hasTimes &&
                        (startOrAlarm >= rangeEnd || inst.getEnd() <= rangeStart)) {
                        continue;
                    }
                    numInstances++;

                    if (hasTimes) {
                        if (actualRangeStart == 0 || startOrAlarm < actualRangeStart)
                            actualRangeStart = startOrAlarm;
                        if (inst.getEnd() > actualRangeEnd)
                            actualRangeEnd = inst.getEnd();
                    }

                    InviteInfo invId = inst.getInviteInfo();
                    Invite inv = calItem.getInvite(invId.getMsgId(), invId.getComponentId());
                    Long alarmAt = instStart == alarmInst ? Long.valueOf(alarmTime) : null;

                    String fba = inv.getFreeBusyActual();
                    if (calItem instanceof Appointment)
                        fba = ((Appointment) calItem).getEffectiveFreeBusyActual(inv, inst);
                    String effectivePartStat = calItem.getEffectivePartStat(inv, inst);
                    InstanceData instData;
                    if (!inst.isException()) {
                        String ridZ = inst.getRecurIdZ();
                        Long tzOffset = instStartLong != null && inst.isAllDay() ? Long.valueOf(inst.getStartTzOffset()) : null;
                        instData = new InstanceData(
                                ridZ, instStartLong, durationLong, alarmAt, tzOffset,
                                effectivePartStat, fba, inv.getPercentComplete(),
                                defaultData);
                    } else {
                        String ridZ = null;
                        if (inv.hasRecurId())
                            ridZ = inv.getRecurId().getDtZ();
                        instData = new FullInstanceData(inv, ridZ, instStartLong, durationLong, effectivePartStat, fba, alarmAt);
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
            MailItem.Type type, long rangeStart, long rangeEnd, CalendarData prevCalData, boolean incrementalUpdate)
            throws ServiceException {
        if (rangeEnd < rangeStart)
            throw ServiceException.INVALID_REQUEST("End time must be after Start time", null);
        long days = (rangeEnd - rangeStart) / MSEC_PER_DAY;
        if (days > sMaxSearchDays)
            throw ServiceException.INVALID_REQUEST("Requested range is too large (Maximum " +
                    sMaxSearchDays + " days)", null);

        if (prevCalData == null || (rangeStart < prevCalData.getRangeStart() || rangeEnd > prevCalData.getRangeEnd())) {
            // Ignore existing data if its range doesn't include the entire requested range.
            // We have to scan the calendar folder.
            return reloadCalendarOverRangeWithFolderScan(octxt, mbox, folderId, type, rangeStart, rangeEnd, null);
        }
        if (!incrementalUpdate || prevCalData.getNumStaleItems() > sMaxStaleItems) {
            // If incremental update of stale items is disabled, do it with folder scan.
            // If there are too many stale items, do it with folder scan as that may be faster.
            return reloadCalendarOverRangeWithFolderScan(octxt, mbox, folderId, type, rangeStart, rangeEnd, prevCalData);
        }

        Set<Integer> staleItemIds = new HashSet<Integer>();
        prevCalData.copyStaleItemIdsTo(staleItemIds);
        Folder folder = mbox.getFolderById(octxt, folderId);
        CalendarData calData = new CalendarData(folderId, folder.getImapMODSEQ(), rangeStart, rangeEnd);
        for (Iterator<CalendarItemData> iter = prevCalData.calendarItemIterator(); iter.hasNext(); ) {
            CalendarItemData existing = iter.next();
            int calItemId = existing.getCalItemId();
            if (!staleItemIds.contains(calItemId)) {
                // Still good.  Reuse it.
                calData.addCalendarItem(existing);
            } else {
                // Our item is either modified or deleted.  Refetch from backend to see which case it is.
                CalendarItemData calItemData =
                    fetchCalendarItemData(octxt, mbox, folderId, calItemId, rangeStart, rangeEnd);
                staleItemIds.remove(calItemId);
                if (calItemData != null) {
                    // Special check for renumbered item.  ZDesktop can renumber items and it causes a lookup
                    // by old id to return a MailItem object having the new id.  We must ignore it here.  If
                    // we don't, we'll end up with duplicates because the new id is also in the stale item ids
                    // list.
                    if (calItemData.getCalItemId() == calItemId) {
                        calData.addCalendarItem(calItemData);
                    }
                }
            }
        }
        // Any stale items left in staleItemIds set are newly added items.
        for (int calItemId : staleItemIds) {
            CalendarItemData calItemData =
                fetchCalendarItemData(octxt, mbox, folderId, calItemId, rangeStart, rangeEnd);
            if (calItemData != null) {
                // Special check for renumbered item.  ZDesktop can renumber items and it causes a lookup
                // by old id to return a MailItem object having the new id.  We must ignore it here.  If
                // we don't, we'll end up with duplicates because the new id is also in the stale item ids
                // list.
                if (calItemData.getCalItemId() == calItemId) {
                    calData.addCalendarItem(calItemData);
                }
            }
        }
        return calData;  // return a non-null object even if there are no items in the range
    }

    private static CalendarItemData fetchCalendarItemData(OperationContext octxt, Mailbox mbox,
                                                          int folderId, int calItemId,
                                                          long rangeStart, long rangeEnd)
    throws ServiceException {
        CalendarItem calItem = null;
        try {
            calItem = mbox.getCalendarItemById(octxt, calItemId);
        } catch (MailServiceException e) {
            if (!e.getCode().equals(MailServiceException.NO_SUCH_ITEM)
                && !e.getCode().equals(MailServiceException.NO_SUCH_APPT)
                && !e.getCode().equals(MailServiceException.NO_SUCH_TASK))
                throw e;
            // NO_SUCH_ITEM/APPT/TASK exception is an expected condition.  This happens when a calendar item
            // was hard deleted from a sync client.  Calendar cache doesn't process the delete notification
            // because the notification doesn't indicate which folder the item is getting deleted from.
        }
        CalendarItemData calItemData = null;
        // If folder is different, this item must have been moved out of our folder.  Ignore it.
        if (calItem != null && calItem.getFolderId() == folderId)
            calItemData = reloadCalendarItemOverRange(calItem, rangeStart, rangeEnd);
        return calItemData;
    }

    // Reload by first calling Mailbox.getCalendarItemsForRange(), which runs a query on appointment table.
    private static CalendarData reloadCalendarOverRangeWithFolderScan(OperationContext octxt, Mailbox mbox,
            int folderId, MailItem.Type type, long rangeStart, long rangeEnd, CalendarData prevCalData)
            throws ServiceException {
        if (rangeEnd < rangeStart)
            throw ServiceException.INVALID_REQUEST("End time must be after Start time", null);
        long days = (rangeEnd - rangeStart) / MSEC_PER_DAY;
        if (days > sMaxSearchDays)
            throw ServiceException.INVALID_REQUEST("Requested range is too large (Maximum " +
                    sMaxSearchDays + " days)", null);

        // Ignore existing data if its range doesn't include the entire requested range.
        if (prevCalData != null
            && (rangeStart < prevCalData.getRangeStart() || rangeEnd > prevCalData.getRangeEnd()))
            prevCalData = null;

        Folder folder = mbox.getFolderById(octxt, folderId);
        CalendarData calData = new CalendarData(folderId, folder.getImapMODSEQ(), rangeStart, rangeEnd);
        Collection<CalendarItem> calItems = mbox.getCalendarItemsForRange(octxt, type, rangeStart, rangeEnd,
                folderId, null);
        for (CalendarItem calItem : calItems) {
            if (prevCalData != null) {
                // Reuse the appointment if it didn't change.
                CalendarItemData cur = prevCalData.getCalendarItemData(calItem.getId());
                if (cur != null && cur.getModMetadata() == calItem.getModifiedSequence()) {
                    calData.addCalendarItem(cur);
                    continue;
                }
            }
            // We couldn't reuse the existing data.  Get it the hard way.
            CalendarItemData calItemData = reloadCalendarItemOverRange(calItem, rangeStart, rangeEnd);
            if (calItemData != null)
                calData.addCalendarItem(calItemData);
        }
        return calData;  // return a non-null object even if there are no items in the range
    }

    private static final int sRangeMonthFrom;
    private static final int sRangeNumMonths;
    private static final int sMaxStaleItems;
    private static final int sMaxStaleItemsBeforeInvalidatingCalendar;
    private static final int sMaxSearchDays;

    private static final long MSEC_PER_DAY = 1000 * 60 * 60 * 24;

    static {
        sRangeMonthFrom = LC.calendar_cache_range_month_from.intValue();
        sRangeNumMonths = LC.calendar_cache_range_months.intValue();
        sMaxStaleItems = LC.calendar_cache_max_stale_items.intValue();
        sMaxStaleItemsBeforeInvalidatingCalendar = 100;
        sMaxSearchDays = LC.calendar_search_max_days.intValueWithinRange(0, 3660);
    }

    private final RedisCalSummaryCache redisSummaryCache;

    CalSummaryCache(final int capacity) {
        redisSummaryCache = new RedisCalSummaryCache();
    }

    private static enum CacheLevel { Redis, Miss }

    public class CalendarDataResult {
        public CalendarData data;
        public boolean allowPrivateAccess;  // whether caller has permission to view private data
    }

    // get summary for all appts/tasks in a calendar folder
    public CalendarDataResult getCalendarSummary(OperationContext octxt, String targetAcctId, int folderId,
            MailItem.Type type, long rangeStart, long rangeEnd, boolean computeSubRange) throws ServiceException {
        if (rangeStart > rangeEnd)
            throw ServiceException.INVALID_REQUEST("End time must be after Start time", null);

        Account targetAcct = Provisioning.getInstance().get(AccountBy.id, targetAcctId);
        if (targetAcct == null)
            return null;
        boolean targetAcctOnLocalServer = Provisioning.onLocalServer(targetAcct);

        CalendarDataResult result = new CalendarDataResult();

        if (!LC.calendar_cache_enabled.booleanValue()) {
            ZimbraPerf.COUNTER_CALENDAR_CACHE_HIT.increment(0);
            ZimbraPerf.COUNTER_CALENDAR_CACHE_MEM_HIT.increment(0);
            if (!targetAcctOnLocalServer)
                return null;
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(targetAcct);
            Folder folder = mbox.getFolderById(octxt, folderId);
            Account authAcct = octxt != null ? octxt.getAuthenticatedUser() : null;
            boolean asAdmin = octxt != null ? octxt.isUsingAdminPrivileges() : false;
            result.allowPrivateAccess = CalendarItem.allowPrivateAccess(folder, authAcct, asAdmin);
            result.data = reloadCalendarOverRangeWithFolderScan(octxt, mbox, folderId, type, rangeStart, rangeEnd, null);
            if (ZimbraLog.calendar.isDebugEnabled()) {
                ZimbraLog.calendar.debug("Calendar Summary for %s reloaded (no cache) - %s items private=%s",
                        folder.getName(), result.data.getNumItems(), result.allowPrivateAccess);
            }
            return result;
        }

        // Check if we have read permission.
        short perms;
        try {
            FolderACL facl = new FolderACL(octxt, targetAcctId, folderId);
            perms = facl.getEffectivePermissions();
        } catch (ServiceException se) {
            ZimbraLog.calendar.warn("Problem discovering ACLs for folder %s:%s", targetAcctId, folderId, se);
            throw ServiceException.PERM_DENIED(
                    "problem determining whether you have sufficient permissions on folder " +
                    targetAcctId + ":" + folderId + " (" + se.getMessage() + ")");
        }
        if ((short) (perms & ACL.RIGHT_READ) != ACL.RIGHT_READ)
            throw ServiceException.PERM_DENIED(
                    "you do not have sufficient permissions on folder " + targetAcctId + ":" + folderId);
        result.allowPrivateAccess = (short) (perms & ACL.RIGHT_PRIVATE) == ACL.RIGHT_PRIVATE;

        if (!targetAcctOnLocalServer) {
            if (ZimbraLog.calendar.isDebugEnabled()) {
                ZimbraLog.calendar.debug("Calendar Summary - ignoring non-local %s:%s",
                        targetAcctId, folderId);
            }
            return null;
        }

        CalendarData calData = null;
        int lruSize = 0;
        CacheLevel dataFrom = CacheLevel.Redis;
        boolean incrementalUpdate = sMaxStaleItems > 0;

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(targetAcctId);
        Folder folder = mbox.getFolderById(octxt, folderId);  // ACL check occurs here.
        // All subsequent mailbox access is done as owner to avoid permission errors.
        OperationContext ownerOctxt = new OperationContext(targetAcct);
        int currentModSeq = folder.getImapMODSEQ();

        // Lookup from Redis cache
        calData = redisSummaryCache.get(targetAcctId, folderId);
        ZimbraLog.calendar.debug("CalSummaryCache.getCalendarSummary(%s,%s...) REDIS calData=%s",
                targetAcctId, folderId, calData);
        if (calData != null) {
            // Sanity check: Cached data can't be newer than the backend data.
            if (calData.getModSeq() > currentModSeq) {
                ZimbraLog.calendar.debug(
                        "CalSummaryCache.getCalendarSummary Discard REDIS calData - calData.modSeq=%s > currModSeq=%s",
                        calData.getModSeq(), currentModSeq);
                calData = null;
            } else {
                dataFrom = CacheLevel.Redis;
                // Data loaded from heap LRU supports incremental update for stale items.
                incrementalUpdate = sMaxStaleItems > 0;
            }
        }

        CalendarData reusableCalData = null;

        Pair<Long, Long> defaultRange = null;
        if (calData != null) {
            if (calData.getModSeq() != currentModSeq || calData.getNumStaleItems() > 0) {
                // Cached data is stale.
                // Something changed on the calendar, but most of the data is probably still current.
                // Let's keep a reference to the current data and reuse what we can, but only if the
                // current data's range covers the requested range.
                if (rangeStart >= calData.getRangeStart() && rangeEnd <= calData.getRangeEnd())
                    reusableCalData = calData;
                calData = null;  // force recompute further down
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
            if (defaultRange == null) {
                defaultRange = Util.getMonthsRange(System.currentTimeMillis(), sRangeMonthFrom, sRangeNumMonths);
            }
            calData = reloadCalendarOverRange(ownerOctxt, mbox, folderId, type,
                    defaultRange.getFirst(), defaultRange.getSecond(), reusableCalData, incrementalUpdate);
            ZimbraLog.calendar.debug("CalSummaryCache.getCalendarSummary(%s,%s) PUT_REDIS(RECOMPUTED) calData=%s",
                    targetAcctId, folderId, calData);
            redisSummaryCache.put(targetAcctId, folderId, calData);
            dataFrom = CacheLevel.Miss;
        }

        assert(calData != null);

        if (rangeStart >= calData.getRangeStart() && rangeEnd <= calData.getRangeEnd()) {
            // Requested range is within cached range.
            if (computeSubRange) {
                result.data = calData.getSubRange(rangeStart, rangeEnd);
            } else {
                result.data = calData;
            }
        } else {
            // Requested range is outside the currently cached range.
            dataFrom = CacheLevel.Miss;
            result.data = reloadCalendarOverRange(ownerOctxt, mbox, folderId, type, rangeStart, rangeEnd,
                    reusableCalData, incrementalUpdate);
        }

        // hit/miss tracking
        // COUNTER_CALENDAR_CACHE_HIT - A hit is a successful lookup from either memory or file.
        // TODO - remove this? COUNTER_CALENDAR_CACHE_MEM_HIT - A hit is a successful lookup from memory only.
        switch (dataFrom) {
        case Redis:
            ZimbraPerf.COUNTER_CALENDAR_CACHE_HIT.increment(1);
            break;
        case Miss:
        default:
            ZimbraPerf.COUNTER_CALENDAR_CACHE_HIT.increment(0);
            break;
        }
        ZimbraPerf.COUNTER_CALENDAR_CACHE_LRU_SIZE.increment(lruSize);

        if (ZimbraLog.calendar.isDebugEnabled()) {
            ZimbraLog.calendar.debug("Calendar Summary for %s:%s reloaded (dataFrom=%s) - %s items private=%s",
                    targetAcctId, folderId, dataFrom, (result.data == null) ? 0 : result.data.getNumItems(),
                    result.allowPrivateAccess);
        }
        return result;
    }

    private void invalidateSummary(Mailbox mbox, int folderId) {
        if (!LC.calendar_cache_enabled.booleanValue()) {
            return;
        }
        redisSummaryCache.put(mbox.getAccountId(), folderId, null);
    }

    private void invalidateItem(Mailbox mbox, int folderId, int calItemId) {
        if (!LC.calendar_cache_enabled.booleanValue()) {
            return;
        }
        CalendarData calData = redisSummaryCache.get(mbox.getAccountId(), folderId);
        // Invalidate the item from the calendar.
        if (calData != null) {
            int numStaleItems = calData.markItemStale(calItemId);
            // If there are too many stale items, purge the calendar from cache to avoid accumulating
            // too many stale item ids.
            if (numStaleItems > sMaxStaleItemsBeforeInvalidatingCalendar) {
                invalidateSummary(mbox, folderId);
            } else {
                redisSummaryCache.put(mbox.getAccountId(), folderId, calData);
            }
        }
    }

    private void invalidateCalendarItem(Object obj) {
        if (obj instanceof CalendarItem) {
            CalendarItem calItem = (CalendarItem) obj;
            int folderId = calItem.getFolderId();
            try {
                invalidateItem(calItem.getMailbox(), folderId, calItem.getId());
            } catch (ServiceException e) {
                ZimbraLog.calendar.warn("Failed invalidating cache item (cannot get mailbox for calendar item %s)",
                        calItem.getId(), e);
            }
        }
    }

    void notifyCommittedChanges(PendingLocalModifications mods, int changeId) {
        ZimbraLog.calendar.debug("CalSummaryCache.notifyCommittedChanges mods=%s changeId=%s", mods, changeId);
        if (mods.created != null) {
            for (Map.Entry<ModificationKey, BaseItemInfo> entry : mods.created.entrySet()) {
                invalidateCalendarItem(entry.getValue());
            }
        }
        if (mods.modified != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.modified.entrySet()) {
                Change change = entry.getValue();
                Object whatChanged = change.what;
                if (whatChanged instanceof CalendarItem) {
                    invalidateCalendarItem(whatChanged);
                    // If this is a folder move, invalidate the item from the old folder too.
                    if ((change.why & Change.FOLDER) != 0) {
                        invalidateCalendarItem(change.preModifyObj);
                    }
                }
            }
        }
        if (mods.deleted != null) {
            String lastAcctId = null;
            Mailbox lastMbox = null;
            List<Folder> calFolders = null;
            for (Map.Entry<ModificationKey, Change> entry : mods.deleted.entrySet()) {
                MailItem.Type type = (MailItem.Type) entry.getValue().what;
                if (type == MailItem.Type.APPOINTMENT || type == MailItem.Type.TASK) {
                    // Don't have old parent calendar information.  Assume it could have been in any calendar
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null) {
                        continue;  // just to be safe
                    }
                    Mailbox mbox = null;
                    if (acctId.equals(lastAcctId)) {
                        // Deletion by id list usually happens because of a folder getting emptied.
                        // It's highly likely the items all belong to the same mailbox, let alone folder.
                        mbox = lastMbox;
                    } else {
                        calFolders = null;
                        try {
                            mbox = MailboxManager.getInstance().getMailboxByAccountId(acctId, FetchMode.DO_NOT_AUTOCREATE);
                            if (mbox != null) {
                                calFolders = mbox.getCalendarFolders(null, SortBy.NONE);
                            }
                        } catch (ServiceException e) {
                            ZimbraLog.calendar.error("Error getting calendar list for delete notification: account=%s",
                                    acctId, e);
                            calFolders = null;
                        }
                    }
                    if ((mbox == null) || (calFolders == null)) {
                        continue;
                    }
                    lastAcctId = acctId;
                    lastMbox = mbox;
                    int itemId = entry.getKey().getItemId();
                    for (Folder calFolder : calFolders) {
                        invalidateItem(mbox, calFolder.getId(), itemId);
                    }
                }
            }
        }
    }

    void purgeMailbox(Mailbox mbox) throws ServiceException {
        redisSummaryCache.purge(mbox.getAccountId());
    }
}
