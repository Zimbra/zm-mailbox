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

    public synchronized boolean isFinished() {
        return mWaiting == true && mCompleted >= mRequested;
    }
}
