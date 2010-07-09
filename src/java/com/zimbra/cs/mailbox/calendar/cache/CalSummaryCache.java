/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailboxManager.FetchMode;
import com.zimbra.cs.mailbox.acl.FolderACL;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.session.PendingModifications;
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

    // mSummaryCache
    //
    // key = "accountId:folderId"
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
                new FullInstanceData(defaultInvite, null, defDtStartLong, defDurationLong,
                                     defaultEffectivePartStat, defaultFba, null, null);
            calItemData = new CalendarItemData(
                    calItem.getType(), calItem.getFolderId(), calItem.getId(),
                    calItem.getFlagString(), calItem.getTagString(),
                    calItem.getModifiedSequence(), calItem.getSavedSequence(),
                    calItem.getDate(), calItem.getChangeDate(), calItem.getSize(),
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
                        String ridZ = inst.getRecurIdZ();
                        Long tzOffset = instStartLong != null ? new Long(inst.getTzOffset()) : null;
                        instData = new InstanceData(
                                ridZ, instStartLong, durationLong, alarmAt, tzOffset,
                                effectivePartStat, fba, inv.getPercentComplete(),
                                defaultData);
                    } else {
                        String ridZ = null;
                        if (inv.hasRecurId())
                            ridZ = inv.getRecurId().getDtZ();
                        instData = new FullInstanceData(inv, ridZ, instStartLong, durationLong, effectivePartStat, fba, alarmAt, defaultData);
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
                                                        byte itemType, long rangeStart, long rangeEnd,
                                                        CalendarData prevCalData, boolean incrementalUpdate)
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
            return reloadCalendarOverRangeWithFolderScan(octxt, mbox, folderId, itemType, rangeStart, rangeEnd, null);
        }
        if (!incrementalUpdate || prevCalData.getNumStaleItems() > sMaxStaleItems) {
            // If incremental update of stale items is disabled, do it with folder scan.
            // If there are too many stale items, do it with folder scan as that may be faster.
            return reloadCalendarOverRangeWithFolderScan(octxt, mbox, folderId, itemType, rangeStart, rangeEnd, prevCalData);
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
                    if (calItemData.getCalItemId() == calItemId)
                        calData.addCalendarItem(calItemData);
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
                if (calItemData.getCalItemId() == calItemId)
                    calData.addCalendarItem(calItemData);
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
            calItemData = reloadCalendarItemOverRange(mbox, calItem, rangeStart, rangeEnd);
        return calItemData;
    }

    // Reload by first calling Mailbox.getCalendarItemsForRange(), which runs a query on appointment table.
    private static CalendarData reloadCalendarOverRangeWithFolderScan(
            OperationContext octxt, Mailbox mbox, int folderId,
            byte itemType, long rangeStart, long rangeEnd,
            CalendarData prevCalData)
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
        Collection<CalendarItem> calItems =
            mbox.getCalendarItemsForRange(octxt, itemType, rangeStart, rangeEnd, folderId, null);
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
            CalendarItemData calItemData = reloadCalendarItemOverRange(mbox, calItem, rangeStart, rangeEnd);
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

    @SuppressWarnings("serial")
    private static class SummaryLRU extends LinkedHashMap<CalSummaryKey, CalendarData> {
        private int mMaxAllowed;

        // map that keeps track of which calendar folders are cached for each account
        // This map is updated every time a calendar folder is added, removed, or aged out
        // of the LRU.
        private Map<String /* account id */, Set<Integer> /* folder ids */> mAccountFolders;

        private SummaryLRU(int capacity) {
            super(capacity + 1, 1.0f, true);
            mMaxAllowed = Math.max(capacity, 1);
            mAccountFolders = new HashMap<String, Set<Integer>>();
        }

        @Override
        public void clear() {
            super.clear();
            mAccountFolders.clear();
        }

        @Override
        public CalendarData put(CalSummaryKey key, CalendarData value) {
            CalendarData prevVal = super.put(key, value);
            if (prevVal == null)
                registerWithAccount(key);
            return prevVal;
        }

        @Override
        public void putAll(Map<? extends CalSummaryKey, ? extends CalendarData> t) {
            super.putAll(t);
            for (CalSummaryKey key : t.keySet()) {
                registerWithAccount(key);
            }
        }

        @Override
        public CalendarData remove(Object key) {
            CalendarData prevVal = super.remove(key);
            if (prevVal != null && key instanceof CalSummaryKey) {
                CalSummaryKey k = (CalSummaryKey) key;
                deregisterFromAccount(k);
            }
            return prevVal;
        }        

        @Override
        protected boolean removeEldestEntry(Map.Entry<CalSummaryKey, CalendarData> eldest) {
            boolean remove = size() > mMaxAllowed;
            if (remove)
                deregisterFromAccount(eldest.getKey());
            return remove;
        }

        private void registerWithAccount(CalSummaryKey key) {
            String accountId = key.getAccountId();
            int folderId = key.getFolderId();
            Set<Integer> folders = mAccountFolders.get(accountId);
            if (folders == null) {
                folders = new HashSet<Integer>();
                mAccountFolders.put(accountId, folders);
            }
            folders.add(folderId);
        }

        private void deregisterFromAccount(CalSummaryKey key) {
            String accountId = key.getAccountId();
            int folderId = key.getFolderId();
            Set<Integer> folders = mAccountFolders.get(accountId);
            if (folders != null) {
                folders.remove(folderId);
                // If no folders are cached for the account, drop the account entry from the map to save memory.
                if (folders.isEmpty())
                    mAccountFolders.remove(accountId);
            }
        }

        public static final int FOLDER_NOT_FOUND = -1;

        public int getFolderForItem(String accountId, int itemId) {
            int retval = FOLDER_NOT_FOUND;
            Set<Integer> folders = mAccountFolders.get(accountId);
            if (folders != null) {
                for (int folderId : folders) {
                    CalSummaryKey key = new CalSummaryKey(accountId, folderId);
                    CalendarData calData = get(key);
                    if (calData != null) {
                        CalendarItemData ci = calData.getCalendarItemData(itemId);
                        if (ci != null) {
                            retval = folderId;
                            break;
                        }
                    }
                }
            }
            return retval;
        }

        /**
         * Toss all folders of the account from the LRU.
         * @param mboxId
         */
        public void removeAccount(String accountId) {
            Set<Integer> folders = mAccountFolders.get(accountId);
            if (folders != null) {
                // Get a copy of the folder list to avoid ConcurrentModificationException on mMboxFolders.
                Integer[] fids = folders.toArray(new Integer[0]);
                for (int folderId : fids) {
                    CalSummaryKey key = new CalSummaryKey(accountId, folderId);
                    remove(key);
                }
            }
        }
    }

    // LRU cache containing range-limited calendar summary by calendar folder
    private SummaryLRU mSummaryCache;
    private int mLRUCapacity;
    private CalSummaryMemcachedCache mMemcachedCache;

    CalSummaryCache(final int capacity) {
        mLRUCapacity = capacity;
        mSummaryCache = new SummaryLRU(capacity);
        mMemcachedCache = new CalSummaryMemcachedCache();
    }

    private static enum CacheLevel { Memory, Memcached, File, Miss }

    public class CalendarDataResult {
        public CalendarData data;
        public boolean allowPrivateAccess;  // whether caller has permission to view private data
    }

    // get summary for all appts/tasks in a calendar folder
    public CalendarDataResult getCalendarSummary(OperationContext octxt, String targetAcctId, int folderId,
    									   byte itemType, long rangeStart, long rangeEnd,
    									   boolean computeSubRange)
    throws ServiceException {
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
            result.data = reloadCalendarOverRangeWithFolderScan(octxt, mbox, folderId, itemType, rangeStart, rangeEnd, null);
            return result;
        }

        // Check if we have read permission.
        FolderACL facl = new FolderACL(octxt, targetAcctId, folderId);
        short perms = facl.getEffectivePermissions();
        if ((short) (perms & ACL.RIGHT_READ) != ACL.RIGHT_READ)
            throw ServiceException.PERM_DENIED(
                    "you do not have sufficient permissions on folder " + targetAcctId + ":" + folderId);
        result.allowPrivateAccess = (short) (perms & ACL.RIGHT_PRIVATE) == ACL.RIGHT_PRIVATE;

        // Look up from memcached.
        CalSummaryKey key = new CalSummaryKey(targetAcctId, folderId);
            CalendarData calData = mMemcachedCache.getForRange(key, rangeStart, rangeEnd);
            if (calData != null) {
                ZimbraPerf.COUNTER_CALENDAR_CACHE_HIT.increment(1);
                ZimbraPerf.COUNTER_CALENDAR_CACHE_MEM_HIT.increment(1);
            result.data = calData;
            return result;
            }
        // If not found in memcached and account is not on local server, we're done.
        if (!targetAcctOnLocalServer)
            return null;

        int lruSize = 0;
        CacheLevel dataFrom = CacheLevel.Memory;
        boolean incrementalUpdate = sMaxStaleItems > 0;

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(targetAcctId);
        Folder folder = mbox.getFolderById(octxt, folderId);  // ACL check occurs here.
        // All subsequent mailbox access is done as owner to avoid permission errors.
        OperationContext ownerOctxt = new OperationContext(targetAcct);
        int currentModSeq = folder.getImapMODSEQ();

        // Lookup from heap LRU.
            synchronized (mSummaryCache) {
                if (mLRUCapacity > 0) {
                    calData = mSummaryCache.get(key);
                    lruSize = mSummaryCache.size();
                }
            }
            if (calData != null) {
                // Sanity check: Cached data can't be newer than the backend data.
                if (calData.getModSeq() > currentModSeq) {
                    calData = null;
                } else {
                    dataFrom = CacheLevel.Memory;
                    // Data loaded from heap LRU supports incremental update for stale items.
                    incrementalUpdate = sMaxStaleItems > 0;
                }
            }

        if (calData == null) {
            // Load from file.
            try {
                calData = FileStore.loadCalendarData(mbox.getId(), folderId, currentModSeq);
                if (calData != null) {
                    // If data is up to date, add to LRU.
                    if (calData.getModSeq() == currentModSeq) {
                        if (mLRUCapacity > 0) {
                            synchronized (mSummaryCache) {
                                mSummaryCache.put(key, calData);
                                lruSize = mSummaryCache.size();
                            }
                        }
                    } else {
                        // Data loaded from file doesn't have stale items list.  It can't be refreshed incrementally.
                        incrementalUpdate = false;
                    }
                    dataFrom = CacheLevel.File;
                }
            } catch (ServiceException e) {
                ZimbraLog.calendar.warn("Error loading cached calendar summary", e);
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
            if (defaultRange == null)
                defaultRange = Util.getMonthsRange(System.currentTimeMillis(),
                                                   sRangeMonthFrom, sRangeNumMonths);
            calData = reloadCalendarOverRange(ownerOctxt, mbox, folderId, itemType,
                                              defaultRange.getFirst(), defaultRange.getSecond(), reusableCalData, incrementalUpdate);
            synchronized (mSummaryCache) {
                if (mLRUCapacity > 0) {
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

        assert(calData != null);

        // Put data in memcached if it didn't come from memcached.
        if (!CacheLevel.Memcached.equals(dataFrom))
                mMemcachedCache.put(key, calData);

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
            result.data = reloadCalendarOverRange(ownerOctxt, mbox, folderId, itemType, rangeStart, rangeEnd, reusableCalData, incrementalUpdate);
        }

        // hit/miss tracking
        // COUNTER_CALENDAR_CACHE_HIT - A hit is a successful lookup from either memory or file.
        // COUNTER_CALENDAR_CACHE_MEM_HIT - A hit is a successful lookup from memory only.
        switch (dataFrom) {
        case Memory:
        case Memcached:
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

        return result;
    }

    private void invalidateSummary(Mailbox mbox, int folderId) {
        if (!LC.calendar_cache_enabled.booleanValue())
            return;
        long mboxId = mbox.getId();
        CalSummaryKey key = new CalSummaryKey(mbox.getAccountId(), folderId);
        synchronized (mSummaryCache) {
            mSummaryCache.remove(key);
        }
        try {
            FileStore.deleteCalendarData(mboxId, folderId);
        } catch (ServiceException e) {
            ZimbraLog.calendar.warn("Error deleting calendar summary cache", e);
        }
    }

    private void invalidateItem(Mailbox mbox, int folderId, int calItemId) {
        if (!LC.calendar_cache_enabled.booleanValue())
            return;
        CalSummaryKey key = new CalSummaryKey(mbox.getAccountId(), folderId);
        CalendarData calData = null;
        synchronized (mSummaryCache) {
            if (mLRUCapacity > 0) {
                calData = mSummaryCache.get(key);
            }
        }
        // Invalidate the item from the calendar.
        if (calData != null) {
            int numStaleItems = calData.markItemStale(calItemId);
            // If there are too many stale items, purge the calendar from cache to avoid accumulating
            // too many stale item ids.
            if (numStaleItems > sMaxStaleItemsBeforeInvalidatingCalendar)
                invalidateSummary(mbox, folderId);
        }
    }

    void notifyCommittedChanges(PendingModifications mods, int changeId) {
        if (mods.created != null) {
            for (Map.Entry<ModificationKey, MailItem> entry : mods.created.entrySet()) {
                MailItem item = entry.getValue();
                if (item instanceof CalendarItem) {
                    int folderId = item.getFolderId();
                    invalidateItem(item.getMailbox(), folderId, item.getId());
                }
            }
        }
        if (mods.modified != null) {
            for (Map.Entry<ModificationKey, Change> entry : mods.modified.entrySet()) {
                Change change = entry.getValue();
                Object whatChanged = change.what;
                if (whatChanged instanceof CalendarItem) {
                    CalendarItem item = (CalendarItem) whatChanged;
                    Mailbox mbox = item.getMailbox();
                    int folderId = item.getFolderId();
                    int itemId = item.getId();
                    invalidateItem(mbox, folderId, itemId);

                    // If this is a folder move, invalidate the item from the old folder too.
                    if ((change.why & Change.MODIFIED_FOLDER) != 0) {
                        String accountId = mbox.getAccountId();
                        int prevFolderId;
                        synchronized (mSummaryCache) {
                            prevFolderId = mSummaryCache.getFolderForItem(accountId, itemId);
                        }
                        if (prevFolderId != folderId && prevFolderId != SummaryLRU.FOLDER_NOT_FOUND)
                            invalidateItem(mbox, prevFolderId, itemId);
                    }
                }
            }
        }
        if (mods.deleted != null) {
            // This code gets called even for non-calendar items, for example it's called for every email
            // being emptied from Trash.  But there's no way to short circuit out of here because the delete
            // notification doesn't tell us the item type of what's being deleted.  Oh well.
            String lastAcctId = null;
            Mailbox lastMbox = null;
            for (Map.Entry<ModificationKey, Object> entry : mods.deleted.entrySet()) {
                Object deletedObj = entry.getValue();
                if (deletedObj instanceof CalendarItem) {
                    CalendarItem item = (CalendarItem) deletedObj;
                    Mailbox mbox = item.getMailbox();
                    invalidateItem(mbox, item.getFolderId(), item.getId());
                    lastAcctId = mbox.getAccountId();
                    lastMbox = mbox;
                } else if (deletedObj instanceof Integer) {
                    // We only have item id.  Look up the folder id of the item in the cache.
                    Mailbox mbox = null;
                    String acctId = entry.getKey().getAccountId();
                    if (acctId == null) continue;  // just to be safe
                    if (acctId.equals(lastAcctId)) {
                        // Deletion by id list usually happens because of a folder getting emptied.
                        // It's highly likely the items all belong to the same mailbox, let alone folder.
                        mbox = lastMbox;
                    } else {
                        try {
                            mbox = MailboxManager.getInstance().getMailboxByAccountId(acctId, FetchMode.DO_NOT_AUTOCREATE);
                        } catch (ServiceException e) {
                            ZimbraLog.calendar.error("Error looking up the mailbox of account in delete notification: account=" + acctId, e);
                            continue;
                        }
                    }
                    if (mbox != null) {
                        lastAcctId = acctId;
                        lastMbox = mbox;
                        int itemId = ((Integer) deletedObj).intValue();
                        String accountId = mbox.getAccountId();
                        int folderId;
                        synchronized (mSummaryCache) {
                            folderId = mSummaryCache.getFolderForItem(accountId, itemId);
                        }
                        if (folderId != SummaryLRU.FOLDER_NOT_FOUND)
                            invalidateItem(mbox, folderId, itemId);
                    }
                }
            }
        }

        if (MemcachedConnector.isConnected())
            mMemcachedCache.notifyCommittedChanges(mods, changeId);
    }

    void purgeMailbox(Mailbox mbox) throws ServiceException {
        synchronized (mSummaryCache) {
            mSummaryCache.removeAccount(mbox.getAccountId());
        }
        if (MemcachedConnector.isConnected())
            mMemcachedCache.purgeMailbox(mbox);
        FileStore.removeMailbox(mbox.getId());
    }
}
