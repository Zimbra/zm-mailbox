/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.zclient;

import com.zimbra.cs.zclient.ZMailbox.ZApptSummaryResult;
import com.zimbra.cs.zclient.event.ZEventHandler;
import com.zimbra.cs.zclient.event.ZRefreshEvent;
import com.zimbra.cs.zclient.event.ZCreateEvent;
import com.zimbra.cs.zclient.event.ZModifyEvent;
import com.zimbra.cs.zclient.event.ZDeleteEvent;
import com.zimbra.cs.zclient.event.ZCreateAppointmentEvent;
import com.zimbra.cs.zclient.event.ZModifyAppointmentEvent;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Arrays;
import java.util.Collections;

public class ZApptSummaryCache extends ZEventHandler {

    private static final long MSECS_PER_MINUTE = 1000*60;
    private static final long MSECS_PER_HOUR = MSECS_PER_MINUTE * 60;
    private static final long MSECS_PER_DAY = MSECS_PER_HOUR * 24;
    private static final long MSECS_PER_MONTH_GRID = MSECS_PER_DAY * 42; // month grid is 7x6

    // key start:end:folderId
    private Map<String, ZApptSummaryResult> mResults;

    // set of all ids in our cache
    private Set<String> mIds;

    // cache of mini-cal results
    private Map<String, Set<String>> mMiniCalCache;

    public ZApptSummaryCache() {
        mResults = new HashMap<String, ZApptSummaryResult>();
        mIds = new HashSet<String>();
        mMiniCalCache = new HashMap<String, Set<String>>();
    }

    private String makeKey(long start, long end, String folderId, TimeZone timezone, String query) {
        if (query == null) query = "";
        return start+":"+end+":"+folderId+":"+timezone.getID() + ":"+ query;
    }

    private String makeMiniCalKey(long start, long end, String folderIds[]) {
        if (folderIds.length == 1) {
            return start+":"+end+":"+folderIds[0];
        } else {
            List<String> folders = Arrays.asList(folderIds);
            Collections.sort(folders);
            return start+":"+end+":"+folders;
        }
    }

    synchronized void add(ZApptSummaryResult result, TimeZone timezone) {
        for (ZAppointmentHit appt : result.getAppointments()) {
            mIds.add(appt.getId());
        }
        mResults.put(makeKey(result.getStart(), result.getEnd(), result.getFolderId(), timezone, result.getQuery()), result);
    }

    synchronized ZApptSummaryResult get(long start, long end, String folderId, TimeZone timezone, String query) {
        if (query == null) query = "";
        ZApptSummaryResult result = mResults.get(makeKey(start, end, folderId, timezone, query));
        if (result == null && (end-start) < MSECS_PER_MONTH_GRID) {
            // let's see if results might potentially be contained within another result
            for (ZApptSummaryResult cached : mResults.values()) {
                if (cached.getQuery().equals(query) && cached.getTimeZone().getID().equals(timezone.getID()) && cached.getFolderId().equals(folderId) && (cached.getStart() <= start && end <= cached.getEnd())) {
                    List<ZAppointmentHit> appts = new ArrayList<ZAppointmentHit>();
                    for (ZAppointmentHit appt : cached.getAppointments()) {
                        if (appt.isInRange(start, end))
                            appts.add(appt);
                    }
                    return new ZApptSummaryResult(start, end, folderId, timezone, appts, query);
                }
            }
        }
        return result;
    }

    synchronized void putMiniCal(Set<String> result, long start, long end, String folderIds[]) {
        mMiniCalCache.put(makeMiniCalKey(start, end, folderIds), result);
    }

    synchronized Set<String> getMiniCal(long start, long end, String folderIds[]) {
        return mMiniCalCache.get(makeMiniCalKey(start, end, folderIds));
    }

    public synchronized void clear() {
        mIds.clear();
        mResults.clear();
        mMiniCalCache.clear();
    }

    /**
     *
     * @param refreshEvent the refresh event
     * @param mailbox the mailbox that had the event
     */
    public void handleRefresh(ZRefreshEvent refreshEvent, ZMailbox mailbox) throws ServiceException {
        clear();
    }

    /**
     *
     * @param event the create event
     * @param mailbox the mailbox that had the event
     */
    public void handleCreate(ZCreateEvent event, ZMailbox mailbox) throws ServiceException {
        if (event instanceof ZCreateAppointmentEvent) {
            clear();
        }
    }

    /**
     * @param event the modify event
     * @param mailbox the mailbox that had the event
     */
    public void handleModify(ZModifyEvent event, ZMailbox mailbox) throws ServiceException {
        if (event instanceof ZModifyAppointmentEvent) {
            clear();
        }
    }

    /**
     *
     * default implementation is a no-op
     *
     * @param event the delete event
     * @param mailbox the mailbox that had the event
     */
    public synchronized void handleDelete(ZDeleteEvent event, ZMailbox mailbox) throws ServiceException {
        for (String id : event.toList()) {
            if (mIds.contains(id)) {
                clear();
                return;
            }
        }
    }
}
