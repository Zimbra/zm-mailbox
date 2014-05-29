/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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
