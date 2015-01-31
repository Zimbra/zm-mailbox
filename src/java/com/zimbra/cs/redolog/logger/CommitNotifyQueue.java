/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.redolog.logger;

import java.io.IOException;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.RedoCommitCallback;
import com.zimbra.cs.util.Zimbra;

class CommitNotifyQueue {
    private Notif[] queue = new Notif[100];
    private int head; // points to first entry
    private int tail; // points to just after last entry (first empty slot)
    private boolean full;

    public CommitNotifyQueue(int size) {
        queue = new Notif[size];
        head = tail = 0;
        full = false;
    }

    public synchronized void push(Notif notif) throws IOException {
        if (notif != null) {
            if (full)
                flush(); // queue is full
            assert (!full);
            queue[tail] = notif;
            tail++;
            tail %= queue.length;
            full = tail == head;
        }
    }

    private synchronized Notif pop() {
        if (head == tail && !full)
            return null; // queue is empty
        Notif n = queue[head];
        queue[head] = null; // help with GC
        head++;
        head %= queue.length;
        full = false;
        return n;
    }

    public synchronized void flush() throws IOException {
        Notif notif;
        while ((notif = pop()) != null) {
            RedoCommitCallback cb = notif.getCallback();
            assert (cb != null);
            try {
                cb.callback(notif.getCommitId());
            } catch (OutOfMemoryError e) {
                Zimbra.halt("out of memory", e);
            } catch (Throwable t) {
                ZimbraLog.misc.error("Error while making commit callback", t);
            }
        }
    }
}
