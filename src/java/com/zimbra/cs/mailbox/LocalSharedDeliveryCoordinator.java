/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * @since Jun 13, 2004
 */
public class LocalSharedDeliveryCoordinator implements SharedDeliveryCoordinator {
    static final int DEFAULT_WAIT_SLEEP_MS = 3000;
    protected Map<String, State> stateByAccountId = new ConcurrentHashMap<>();
    protected int waitSleepMs = DEFAULT_WAIT_SLEEP_MS;

    public LocalSharedDeliveryCoordinator() {}

    @VisibleForTesting
    public void flush() {
        stateByAccountId.clear();
    }

    protected State getOrCreateState(Mailbox mbox) {
        State state = stateByAccountId.get(mbox.getAccountId());
        if (state == null) {
            state = new State();
            stateByAccountId.put(mbox.getAccountId(), state);
        }
        return state;
    }

    /**
     * Puts mailbox in shared delivery mode.  A shared delivery is delivery of
     * a message to multiple recipients.  Conflicting op on mailbox is disallowed
     * while mailbox is in shared delivery mode.  (See bug 2187)
     * Conversely, a shared delivery may not start on a mailbox that is
     * currently being operated on or when there is a pending op request.
     * For example, thread A puts mailbox in shared delivery mode.  Thread B
     * then tries to backup the mailbox.  Backup cannot start until thread A is
     * done, but mailbox is immediately put into backup-pending mode.
     * Thread C then tries to do another shared delivery on the mailbox, but
     * is not allowed to do so because of thread B's pending backup request.
     * A thread that calls this method must call endSharedDelivery() after
     * delivering the message.
     * @return true if shared delivery may begin; false if shared delivery may
     *         not begin because of a pending backup request
     */
    @Override
    public boolean beginSharedDelivery(Mailbox mbox) throws ServiceException {
        State state = getOrCreateState(mbox);
        assert(state.numDelivs.get() >= 0);

        // If request for other ops is pending on this mailbox, don't allow
        // any more shared deliveries from starting.
        if (!state.sharedDeliveryAllowed.get()) {
            return false;
        }

        state.numDelivs.incrementAndGet();
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            ZimbraLog.mailbox.debug("# of shared deliv incr to " + state.numDelivs +
                        " for mailbox " + mbox.getId());
        }
        return true;
    }

    /**
     * @see com.zimbra.cs.mailbox.SharedDeliveryCoordinator#beginSharedDelivery()
     */
    @Override
    public void endSharedDelivery(Mailbox mbox) throws ServiceException {
        State state = getOrCreateState(mbox);
        int numDelivs = state.numDelivs.decrementAndGet();
        if (ZimbraLog.mailbox.isDebugEnabled()) {
            ZimbraLog.mailbox.debug("# of shared deliv decr to " + numDelivs +
                        " for mailbox " + mbox.getId());
        }
        assert(numDelivs >= 0);

        // Wake up any waiting backup thread
        if (numDelivs == 0) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    /** Returns whether shared delivery is allowed. Defaults to true, if it was never explicitly set. */
    @Override
    public boolean isSharedDeliveryAllowed(Mailbox mbox) throws ServiceException {
        State state = getOrCreateState(mbox);
        return state.sharedDeliveryAllowed.get();
    }

    /**
     * Turns shared delivery on/off.  If turning off, waits until the op can begin,
     * i.e. until all currently ongoing shared deliveries finish.  A thread
     * turning shared delivery off must turn it on at the end of the operation, otherwise
     * no further shared deliveries are possible to the mailbox.
     */
    @Override
    public void setSharedDeliveryAllowed(Mailbox mbox, boolean allow) throws ServiceException {
        State state = getOrCreateState(mbox);
        state.sharedDeliveryAllowed.set(allow);

        // All waiters should re-evaluate
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Wait until shared delivery is completed on this mailbox.  Other conflicting ops may begin when
     * there is no shared delivery in progress.  Call setSharedDeliveryAllowed(false)
     * before calling this method.
     *
     */
    @Override
    public void waitUntilSharedDeliveryCompletes(Mailbox mbox) throws ServiceException {
        State state = getOrCreateState(mbox);
        while (state.numDelivs.get() > 0) {
            try {
                synchronized (this) {
                    wait(waitSleepMs);
                }
                ZimbraLog.misc.info("wake up from wait for completion of shared delivery; mailbox=" + mbox.getId() +
                            " # of shared deliv=" + state.numDelivs.get());
            } catch (InterruptedException e) {}
        }
    }

    /**
     * Tests whether shared delivery is completed on this mailbox.  Other conflicting ops may begin when
     * there is no shared delivery in progress.
     */
    @Override
    public boolean isSharedDeliveryComplete(Mailbox mbox) throws ServiceException {
        State state = getOrCreateState(mbox);
        return state.numDelivs.get() < 1;
    }

    @VisibleForTesting
    public void setWaitSleepMs(int waitSleepMs) {
        this.waitSleepMs = waitSleepMs;
    }


    class State {
        AtomicInteger numDelivs = new AtomicInteger(0);
        AtomicBoolean sharedDeliveryAllowed = new AtomicBoolean(true);
    }
}
