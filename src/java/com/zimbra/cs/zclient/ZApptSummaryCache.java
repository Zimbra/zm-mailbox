/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is: Zimbra Collaboration Suite Server.
 *
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
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
import com.zimbra.common.service.ServiceException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

public class ZApptSummaryCache extends ZEventHandler {

    private static final long MSECS_PER_MINUTE = 1000*60;
    private static final long MSECS_PER_HOUR = MSECS_PER_MINUTE * 60;
    private static final long MSECS_PER_DAY = MSECS_PER_HOUR * 24;
    private static final long MSECS_PER_MONTH_GRID = MSECS_PER_DAY * 42; // month grid is 7x6

    // key start:end:folderId
    private Map<String, ZApptSummaryResult> mResults;

    // set of all ids in our cache
    private Set<String> mIds;

    public ZApptSummaryCache() {
        mResults = new HashMap<String, ZApptSummaryResult>();
        mIds = new HashSet<String>();
    }

    private String makeKey(long start, long end, String folderId) {
        return start+":"+end+":"+folderId;
    }

    public synchronized void add(ZApptSummaryResult result) {
        for (ZApptSummary appt : result.getAppointments()) {
            mIds.add(appt.getId());
        }
        mResults.put(makeKey(result.getStart(), result.getEnd(), result.getFolderId()), result);
    }

    public synchronized ZApptSummaryResult get(long start, long end, String folderId) {
        ZApptSummaryResult result = mResults.get(makeKey(start, end, folderId));
        if (result == null && (end-start) < MSECS_PER_MONTH_GRID) {
            // let's see if results might potentially be contained within another result
            for (ZApptSummaryResult cached : mResults.values()) {
                if (cached.getFolderId().equals(folderId) && (cached.getStart() <= start && end <= cached.getEnd())) {
                    List<ZApptSummary> appts = new ArrayList<ZApptSummary>();
                    for (ZApptSummary appt : cached.getAppointments()) {
                        if (appt.isInRange(start, end))
                            appts.add(appt);
                    }
                    return new ZApptSummaryResult(start, end, folderId, appts);
                }
            }
        }
        return result;
    }

    public synchronized void clear() {
        mIds.clear();
        mResults.clear();
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
        if (event instanceof ZCreateAppointmentEvent) {
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
