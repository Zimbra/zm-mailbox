/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.mailbox.lock;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Extension of ReentrantReadWriteLock which provides printStackTrace capability via protected methods
 *
 */
public class ZLock extends ReentrantReadWriteLock {

    private static final long serialVersionUID = 6961797355322055852L;

    public ZLock() {
        super();
    }

    public void printStackTrace(StringBuilder out) {
        Thread owner = getOwner();
        if (owner != null) {
            out.append("Write Lock Owner - ");
            printStackTrace(owner, out);
        }
        int readCount = getReadLockCount();
        if (readCount > 0) {
            out.append("Reader Count - " + readCount + "\n");
        }
        for (Thread waiter : getQueuedThreads()) {
            out.append("Lock Waiter - ");
            printStackTrace(waiter, out);
        }
    }

    protected void printStackTrace(Thread thread, StringBuilder out) {
        out.append(thread.getName());
        if (thread.isDaemon()) {
            out.append(" daemon");
        }
        out.append(" prio=").append(thread.getPriority());
        out.append(" id=").append(thread.getId());
        out.append(" state=").append(thread.getState());
        out.append('\n');
        for (StackTraceElement el : thread.getStackTrace()) {
            out.append("\tat ").append(el.toString()).append('\n');
        }
    }
}
