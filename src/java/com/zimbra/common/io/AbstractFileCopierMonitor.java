/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.io;

public abstract class AbstractFileCopierMonitor implements FileCopierCallback {

    private long mRequested;
    private long mCompleted;
    private boolean mWaiting;
    private boolean mDenyFutureOperations;

    protected abstract boolean fileCopierMonitorBegin(Object cbarg);

    protected abstract void fileCopierMonitorEnd(Object cbarg, Throwable err);

    public synchronized boolean fileCopierCallbackBegin(Object cbarg) {
        if (mDenyFutureOperations)
            return false;
        boolean allowed = fileCopierMonitorBegin(cbarg);
        if (allowed)
            mRequested++;
        return allowed;
    }

    public synchronized void fileCopierCallbackEnd(Object cbarg, Throwable err) {
        fileCopierMonitorEnd(cbarg, err);
        mCompleted++;
        if (mWaiting && mCompleted >= mRequested)
            notifyAll();
    }

    public synchronized void waitForCompletion() {
        mWaiting = true;
        if (mCompleted < mRequested) {
            try {
                wait();
            } catch (InterruptedException e) {}
        }
        mWaiting = false;
    }

    public synchronized void denyFutureOperations() {
        mDenyFutureOperations = true;
    }

    public synchronized long getRequested() {
        return mRequested;
    }

    public synchronized long getCompleted() {
        return mCompleted;
    }

    public synchronized long getPending() {
        return mRequested - mCompleted;
    }
}
