/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2021 Synacor, Inc.
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
package com.zimbra.cs.mailbox.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxOperation;

public class MergedEventLog extends ItemEventLog {

    private static final int chunkSize = 100;
    private final HashSet<ItemEventLog> logs;
    private final ArrayList<MailboxEvent> mergedEvents;

    @Override
    public int getEventCount() throws ServiceException {
        int count = 0;
        for (ItemEventLog log : logs) {
            count += log.getEventCount();
        }
        return count;
    }

    @Override
    public Collection<MailboxEvent> getEvents(int offset, int count) throws ServiceException {
        ArrayList<MailboxEvent> ret = new ArrayList<MailboxEvent>();
        int chunk = chunkSize;
        if (mergedEvents.size() == 0) {
            if (chunk < offset + count) {
                chunk = offset + count;
            }
            fetchInitialEvents(chunk);
        }

        if (mergedEvents.size() == 0) {
            return ret;
        }
        int end = offset + count;
        if (end < mergedEvents.size()) {
            // we already have the data in mergedEvents
            for (int i = 0; i < count; i++) {
                ret.add(mergedEvents.get(offset + i));
            }
        } else {
            // we need to fill mergedEvents
            if (end - mergedEvents.size() > chunk) {
                chunk = end - mergedEvents.size();
            }
            fetchNextEvents(chunk);
            if (mergedEvents.size() < offset + count) {
                count = mergedEvents.size() - offset;
            }
            for (int i = 0; i < count; i++) {
                ret.add(mergedEvents.get(offset + i));
            }
        }
        return ret;
    }

    @Override
    public Collection<MailboxEvent> getEventsBefore(long timestamp, int count)
            throws ServiceException {
        ArrayList<MailboxEvent> ret = new ArrayList<MailboxEvent>();
        Iterator<MailboxEvent> iter = fetchEventsBefore(timestamp, count).iterator();
        while (iter.hasNext()) {
            ret.add(iter.next());
        }
        return ret;
    }

    @Override
    public Collection<MailboxOperation> getLoggedOps() throws ServiceException {
        HashSet<MailboxOperation> ops = new HashSet<MailboxOperation>();
        for (ItemEventLog log : logs) {
            ops.addAll(log.getLoggedOps());
        }
        return ops;
    }

    @Override
    public Collection<String> getLoggedUsers() throws ServiceException {
        HashSet<String> users = new HashSet<String>();
        for (ItemEventLog log : logs) {
            users.addAll(log.getLoggedUsers());
        }
        return users;
    }

    @Override
    public void addEvent(MailboxEvent event) throws ServiceException {
        throw ServiceException.UNSUPPORTED();
    }

    public void merge(ItemEventLog log) throws ServiceException {
        logs.add(log);
        mergedEvents.clear();
    }

    private void fetchInitialEvents(int count) throws ServiceException {
        // fetch the events from each logs, sort, then trim
        TreeSet<MailboxEvent> sortedEvents = new TreeSet<MailboxEvent>();
        for (ItemEventLog log : logs) {
            sortedEvents.addAll(log.getEvents(0, count));
        }
        Iterator<MailboxEvent> iter = sortedEvents.iterator();
        for (int i = 0; i < count && iter.hasNext(); i++) {
            mergedEvents.add(iter.next());
        }
    }

    private void fetchNextEvents(int count) throws ServiceException {
        // fetch the next set of events from each logs
        long ts = mergedEvents.get(mergedEvents.size() - 1).getTimestamp();
        Iterator<MailboxEvent> iter = fetchEventsBefore(ts, count).iterator();
        for (int i = 0; i < count && iter.hasNext(); i++) {
            mergedEvents.add(iter.next());
        }
    }

    private Collection<MailboxEvent> fetchEventsBefore(long ts, int count) throws ServiceException {
        TreeSet<MailboxEvent> sortedEvents = new TreeSet<MailboxEvent>();
        for (ItemEventLog log : logs) {
            sortedEvents.addAll(log.getEventsBefore(ts, count));
        }
        if (sortedEvents.size() > count) {
            Iterator<MailboxEvent> iter = sortedEvents.iterator();
            for (int i = 0; i < count; i++) {
                iter.next();
            }
            return sortedEvents.headSet(iter.next());
        }
        return sortedEvents;
    }

    public MergedEventLog() {
        logs = new HashSet<ItemEventLog>();
        mergedEvents = new ArrayList<MailboxEvent>();
    }
}
