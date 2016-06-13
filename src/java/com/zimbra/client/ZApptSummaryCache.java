/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.client;

import com.zimbra.client.ZMailbox.ZApptSummaryResult;
import com.zimbra.client.event.ZEventHandler;
import com.zimbra.client.event.ZRefreshEvent;
import com.zimbra.client.event.ZCreateEvent;
import com.zimbra.client.event.ZModifyEvent;
import com.zimbra.client.event.ZDeleteEvent;
import com.zimbra.client.event.ZCreateAppointmentEvent;
import com.zimbra.client.event.ZModifyAppointmentEvent;
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
