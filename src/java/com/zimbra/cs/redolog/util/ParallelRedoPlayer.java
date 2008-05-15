/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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

package com.zimbra.cs.redolog.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.RedoPlayer;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.util.Zimbra;

public class ParallelRedoPlayer extends RedoPlayer {

    private PlayerThread[] mPlayerThreads;

    public ParallelRedoPlayer(boolean writable, int numThreads, int queueCapacity) {
        this(writable, false, false, numThreads, queueCapacity);
    }

    public ParallelRedoPlayer(boolean writable, boolean unloggedReplay, boolean ignoreReplayErrors,
                              int numThreads, int queueCapacity) {
        super(writable, unloggedReplay, ignoreReplayErrors);
        ZimbraLog.redolog.debug("Starting ParallelRedoPlayer");
        numThreads = Math.max(numThreads, 1);
        mPlayerThreads = new PlayerThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            String name = "RedoPlayer-" + Integer.toString(i);
            PlayerThread player = new PlayerThread(queueCapacity);
            mPlayerThreads[i] = player;
            player.setName(name);
            player.start();
        }
    }

    public void shutdown() {
        ZimbraLog.redolog.debug("Shutting down ParallelRedoPlayer");
        try {
            super.shutdown();
        } finally {
            for (int i = 0; i < mPlayerThreads.length; i++) {
                mPlayerThreads[i].shutdown();
            }
        }
        ZimbraLog.redolog.debug("ParallelRedoPlayer shutdown complete");
    }

    protected void playOp(RedoableOp op) throws Exception {
        checkError();
        int mboxId = op.getMailboxId();
        if (mboxId == RedoableOp.MAILBOX_ID_ALL || mboxId == RedoableOp.UNKNOWN_ID) {
            // Multi-mailbox ops are executed by the main thread to prevent later ops
            // that depend on this op's result aren't run out of order.
            if (ZimbraLog.redolog.isDebugEnabled())
                ZimbraLog.redolog.info("Executing: " + op.toString());
            op.redo();
        } else {
            // Ops for the same mailbox must be played back in order.  To ensure that,
            // all ops for the same mailbox are sent to the same player thread.  The
            // ops are added to the thread's internal queue and played back in order.
            // This assignment of ops to threads will result in uneven distribution.
            int index = Math.abs(mboxId % mPlayerThreads.length);
            PlayerThread player = mPlayerThreads[index];
            RedoTask task = new RedoTask(op);
            if (ZimbraLog.redolog.isDebugEnabled())
                ZimbraLog.redolog.info("Enqueuing: " + op.toString());
            try {
                player.enqueue(task);
            } catch (InterruptedException e) {}
        }
    }

    private Throwable mError = null;
    private final Object mErrorLock = new Object();

    private void raiseError(Throwable t) {
        synchronized (mErrorLock) {
            mError = t;
        }
    }

    private void checkError() throws ServiceException {
        synchronized (mErrorLock) {
            if (mError != null)
                throw ServiceException.FAILURE(
                        "Redo playback stopped due to an earlier error: " + mError.getMessage(), mError);
        }
    }

    private boolean hadError() {
        synchronized (mErrorLock) {
            return mError != null;
        }
    }

    private static class RedoTask {
        private RedoableOp mOp;
        public RedoTask(RedoableOp op)  { mOp = op; }
        public RedoableOp getOp()       { return mOp; }
        public boolean isShutdownTask() { return false; }
    }

    /**
     * Special task to tell the queue drain thread to go away.
     */
    private static class ShutdownTask extends RedoTask {
        public ShutdownTask() { super(null); }
        @Override
        public boolean isShutdownTask() { return true; }
    }

    private class PlayerThread extends Thread {
        private BlockingQueue<RedoTask> mQueue;

        private PlayerThread(int queueCapacity) {
            queueCapacity = Math.max(queueCapacity, 1);
            mQueue = new LinkedBlockingQueue<RedoTask>(queueCapacity);
        }

        public void enqueue(RedoTask task) throws InterruptedException {
            mQueue.put(task);
        }

        public void shutdown() {
            if (hadError())
                mQueue.clear();  // Ensure mQueue.put() below will not block.
            ShutdownTask t = new ShutdownTask();
            try {
                mQueue.put(t);
            } catch (InterruptedException e) {}
            try {
                join();
            } catch (InterruptedException e) {}
        }

        public void run() {
            while (true) {
                RedoTask task;
                try {
                    task = mQueue.take();
                } catch (InterruptedException e) {
                    break;
                }
                if (task.isShutdownTask())
                    break;

                if (hadError()) {
                    // If there was an error, keep consuming from the queue without executing anything.
                    // This thread must consume all tasks until shutdown task is received.  If this
                    // thread stopped consuming, the producer may not be able to enqueue the shutdown
                    // task.
                    continue;
                }

                RedoableOp op = task.getOp();
                try {
                    if (ZimbraLog.redolog.isDebugEnabled())
                        ZimbraLog.redolog.info("Executing: " + op.toString());
                    op.redo();
                } catch (OutOfMemoryError oome) {
                    Zimbra.halt("Out of memory while executing redo op", oome);
                } catch (Throwable e) {
                    ZimbraLog.redolog.error("Unable to execute redo op: " + op.toString(), e);
                    if (!ignoreReplayErrors())
                        raiseError(e);
                }
            }
        }
    }
}
