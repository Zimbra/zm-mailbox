/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ThreadPool;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.util.Zimbra;

/**
 * Model writers via a state machine.
 * <p>
 * 4 possible states:
 * <ul>
 *  <li>CLOSED
 *  <li>WRITING
 *  <li>IDLE
 *  <li>FLUSHING
 * </ul>
 * <p>
 * Valid state transitions:
 * <ul>
 *  <li>I-&gt;W - BeginWriting
 *  <li>C-&gt;W - BeginWriting
 *  <li>W-&gt;I - DoneWriting
 *  <li>I-&gt;F - BeginFlush
 *  <li>F-&gt;C - FlushCompleted
 * </ul>
 * <p>
 * All writes must happen between a {@link #beginWriting(CacheEntry)} and
 * {@link #doneWriting(CacheEntry)} pair. The pair should be inside a
 * synchronized block.
 * <p>
 * Threads block in the following places:
 * <ul>
 *  <li>If they're trying to open, and need a slot ({@link #mWaitingForSlot} list)
 *  <li>In various states, if they're being flushed by another thread (latch on
 *  the writer)
 *  <li>The sweeper waits on the global cache lock
 * </ul>
 */
class IndexWritersCache {
    /**
     * All open writers. A writer is added before starting writing, and removed
     * after flushing (NOT after writing). This set may grow up to
     * {@link #mMaxOpen}. {@link #beginWriting(CacheEntry)} makes you wait until
     * a slot is available for you.
     */
    private Set<CacheEntry> mOpenWriters = new LinkedHashSet<CacheEntry>();

    /**
     * Writers in the {@link WriterState#IDLE} state.
     */
    private Set<CacheEntry> mIdleWriters = new LinkedHashSet<CacheEntry>();

    /**
     * How often do we walk the list of open {@link CacheEntry} looking for idle
     * writers to close. On very busy systems, the default time might be too long.
     */
    private final long mSweeperTimeout = LC.zimbra_index_sweep_frequency.intValueWithinRange(1, 3600) * Constants.MILLIS_PER_SECOND;

    /**
     * After we add a document to it, how long do we hold an index open for
     * writing before closing it (and therefore flushing the writes to disk)?
     *
     * Note that there are other things that might cause us to flush the index
     * to disk -- e.g. if the user does a search on the index, or if the system
     * decides there are too many open IndexWriters (see sLRUSize)
     */
    private final long mMaxIdleTime = 1000 * LC.zimbra_index_idle_flush_time.intValueWithinRange(1, 60 * 60 * 24);

    /**
     * How many open {@link CacheEntry} do we allow?  This value is here so that
     * the # open file descriptors and amount of buffered index memory is controlled.
     */
    private final int mMaxOpen = LC.zimbra_index_lru_size.intValueWithinRange(10, Integer.MAX_VALUE);   // maximum allowed in the LRU
    private final int mFlushPoolSize = LC.zimbra_index_flush_pool_size.intValueWithinRange(1, 100);

    private int mNumFlushing = 0; // number queued or running async flush tasks
    private boolean mShutdown = false;
    private Thread mSweeperThread = null;
    private List<CountDownLatch> mWaitingForSlot = new ArrayList<CountDownLatch>();
    private ThreadPool mPool = new ThreadPool("IndexWriterFlush", mFlushPoolSize);

    private enum WriterState {
        CLOSED,
        WRITING,
        IDLE,
        FLUSHING;
    }

    IndexWritersCache() {
        Runnable sweeper = new Runnable() {
            public void run() {
                doSweep(mSweeperTimeout);
            }
        };
        mSweeperThread = new Thread(sweeper, "IndexWriterSweeper");
        mSweeperThread.start();
    }

    static abstract class CacheEntry {
        private WriterState mState = WriterState.CLOSED;
        private CountDownLatch mFlushWaiterLatch = new CountDownLatch(1);

        /**
         * Implementations must handle the case where the writer is already open
         * (presumably a NoOp).
         *
         * @throws IOException IO error
         */
        abstract void doWriterOpen() throws IOException;

        abstract void doWriterClose();

        abstract long getLastWriteTime();

        private WriterState getState() {
            return mState;
        }

        private void setState(WriterState state) {
            mState = state;
        }

        private CountDownLatch getFlushWaiterLatch() {
            return mFlushWaiterLatch;
        }

        private void resetFlushWaiterLatch() {
            mFlushWaiterLatch = new CountDownLatch(1);
        }
    }

    /**
     * Called when the system needs to quiesce the index volume,
     * e.g. after crash recovery.
     */
    void flushAllWriters() {
        List<CacheEntry> toFlush = new ArrayList<CacheEntry>();
        synchronized (this) {
            toFlush.addAll(mOpenWriters);
        }
        for (CacheEntry writer : toFlush) {
            flush(writer);
        }
    }

    /**
     * Called at the beginning of a write operation. The mailbox lock is held
     * when this call begins.
     *
     * The implementation is responsible for calling {@link CacheEntry#doWriterClose()}
     * if necessary to open the writer.
     *
     * {@link #doneWriting(CacheEntry)} will be called as a pair to this
     * operation.
     *
     * @param writer writer cache entry
     * @throws IOException IO error
     */
    void beginWriting(CacheEntry writer) throws IOException {
        assert(!Thread.holdsLock(this));
        boolean done = false;
        ZimbraPerf.COUNTER_IDX_WRT_OPENED.increment();
        do {
            CountDownLatch latch = null;
            WriterState curState;
            synchronized (this) {
                curState = writer.getState();
                switch (curState) {
                    case FLUSHING:
                        latch = writer.getFlushWaiterLatch();
                        assert(!mIdleWriters.contains(writer));
                        break;
                    case IDLE:
                        assert(mIdleWriters.contains(writer));
                        mIdleWriters.remove(writer);
                        assert(!mIdleWriters.contains(writer));
                        writer.setState(WriterState.WRITING);
                        done = true;
                        ZimbraPerf.COUNTER_IDX_WRT_OPENED_CACHE_HIT.increment();
                        break;
                    case WRITING:
                        assert(false);
                        done = true; // TODO throw IllegalStateException?
                        break;
                    case CLOSED:
                        assert(!mIdleWriters.contains(writer));
                        // not already idle -- find a slot for us

                        // is there a slot for us?
                        if (mOpenWriters.size() < mMaxOpen) {
                            done = true;
                            // at this point we have a slot!
                            mOpenWriters.add(writer);
                            writer.setState(WriterState.WRITING);
                        } else { // nope
                            latch = new CountDownLatch(1);
                            mWaitingForSlot.add(latch);
                            this.notifyAll();
                        }
                        break;
                }
            }
            if (!done) {
                try {
                    if (ZimbraLog.index.isDebugEnabled()) {
                        ZimbraLog.index.debug(
                                "Blocked in beginWriting for " + writer +
                                " because state was " + curState +
                                " NumFlushing=" + mNumFlushing);
                    }
                    latch.await();
                } catch (InterruptedException e) {
                }
            }
            ZimbraPerf.COUNTER_IDX_WRT.increment(mOpenWriters.size());
        } while (!done);
        try {
            // at this point we have a slot, and we're in the active list
            writer.doWriterOpen();
        } catch (IOException e) {
            openFailed(writer);
            throw e;
        }
    }

    private synchronized void openFailed(CacheEntry writer) {
        writer.setState(WriterState.CLOSED);
        mIdleWriters.remove(writer);
        mOpenWriters.remove(writer);
        ZimbraPerf.COUNTER_IDX_WRT.increment(mOpenWriters.size());
        if (!mWaitingForSlot.isEmpty()) {
            CountDownLatch l = mWaitingForSlot.remove(0);
            l.countDown();
        }
        writer.getFlushWaiterLatch().countDown();
        writer.resetFlushWaiterLatch();
        this.notifyAll();
    }

    /**
     * Called as a pair to {@link #beginWriting(CacheEntry)} after the write
     * has been completed.
     *
     * @param writer writer cache entry
     */
    synchronized void doneWriting(CacheEntry writer) {
        assert(writer.getState() == WriterState.WRITING);
        assert(!mIdleWriters.contains(writer));
        assert(mOpenWriters.contains(writer));
        writer.setState(WriterState.IDLE);
        mIdleWriters.add(writer);
        this.notifyAll();
    }

    /**
     * Called at system shutdown
     */
    void shutdown() {
        synchronized (this) {
            mShutdown = true;
            this.notifyAll();
            while (!mOpenWriters.isEmpty()) {
                this.notifyAll();
                try {
                    this.wait(100);
                } catch (InterruptedException e) {
                }
            }
        }
        try {
            mSweeperThread.join();
            mPool.shutdown();
        } catch (InterruptedException e) {
        }
    }

    /**
     * Called in the sweeper thread
     */
    private void doSweep(long sweepTime) {
        try {
            do {
                Set<CacheEntry> toFlush = new HashSet<CacheEntry>();
                long idleCutoff = System.currentTimeMillis() - mMaxIdleTime;
                synchronized (this) {
                    if (mShutdown && mOpenWriters.isEmpty()) {
                        return;
                    }

                    // timeouts
                    for (CacheEntry writer : mIdleWriters) {
                        assert(writer.getState() == WriterState.IDLE);
                        if (mShutdown || (writer.getLastWriteTime() < idleCutoff)) {
                            toFlush.add(writer);
                        }
                    }

                    int additionalNeeded = mWaitingForSlot.size() - (mNumFlushing + toFlush.size());
                    if (additionalNeeded > 0) {
                        for (CacheEntry writer : mIdleWriters) {
                            assert(writer.getState() == WriterState.IDLE);
                            if (additionalNeeded <= 0)
                                break;
                            if (!toFlush.contains(writer)) {
                                additionalNeeded--;
                                toFlush.add(writer);
                            }
                        }
                    }

                    if (toFlush.isEmpty()) {
                        try {
                            if (sweepTime > 0) {
                                this.wait(sweepTime);
                            }
                        } catch (InterruptedException e) {
                        }
                    } else {
                        for (CacheEntry writer : toFlush) {
                            assert(writer.getState() == WriterState.IDLE);
                            mNumFlushing++;
                            writer.setState(WriterState.FLUSHING);
                            mIdleWriters.remove(writer);
                        }
                    }
                }

                // submit outside the global lock, so we don't deadlock if the
                // work queue fills up
                for (CacheEntry writer : toFlush) {
                    AsyncFlush af = new AsyncFlush(writer);
                    try {
                        mPool.execute(af);
                    } catch (OutOfMemoryError e) {
                        Zimbra.halt("OutOfMemory in IndexWritersCache.doSweep", e);
                    } catch (RejectedExecutionException e) {
                        ZimbraLog.index.debug("Sweeper hit interruptedException attempting to async flush " +
                                writer + ". Flushing synchronously.");
                        flushInternal(writer);
                    } catch (Throwable t) {
                        System.err.println("Error! "+t);
                        synchronized (this) {
                            mNumFlushing--;
                            writer.setState(WriterState.IDLE);
                            writer.getFlushWaiterLatch().countDown();
                            writer.resetFlushWaiterLatch();
                            this.notifyAll();
                            mIdleWriters.add(writer);
                        }
                    }
                }
            } while (sweepTime > 0);
        } catch (OutOfMemoryError e) {
            Zimbra.halt("OutOfMemory in IndexWritersCache.doSweep", e);
        }
    }

    class AsyncFlush implements Runnable {
        private CacheEntry mWriter;

        AsyncFlush(CacheEntry writer) {
            mWriter = writer;
        }

        public void run() {
            try {
                flushInternal(mWriter);
            } catch (OutOfMemoryError e) {
                Zimbra.halt("OutOfMemory in IndexWritersCache.AsyncFlush", e);
            } catch (Throwable t) {
                ZimbraLog.index.warn("Caught exception in Async Index Flush: ", t);
            }
        }
    }

    /**
     * Called when we need to flush an index from the cache. Implementations are
     * responsible for making sure that {@link CacheEntry#doWriterClose()} is
     * called (exactly once) to close the index file if necessary.
     *
     * @param writer writer cache entry
     */
    public void flush(CacheEntry writer) {
        synchronized (this) {
            while (writer.getState() == WriterState.FLUSHING) {
                // already flushing...have to wait for that flush to finish
                // so that we can return
                try {
                    this.wait(100);
                } catch (InterruptedException e) {}
            }
            if (writer.getState() == WriterState.CLOSED) {
                return;
            }
            if (writer.getState() == WriterState.WRITING) {
                doneWriting(writer);
            }

            assert(writer.getState() == WriterState.IDLE);
            mNumFlushing++;
            writer.setState(WriterState.FLUSHING);
            mIdleWriters.remove(writer);
        }
        flushInternal(writer);
    }

    private void flushInternal(CacheEntry writer) {
        assert(!Thread.holdsLock(this));
        synchronized (this) {
            assert(writer.getState() == WriterState.FLUSHING);
        }
        try {
            writer.doWriterClose();
        } finally {
            synchronized (this) {
                assert(writer.getState() == WriterState.FLUSHING);
                assert(mOpenWriters.contains(writer));
                assert(!mIdleWriters.contains(writer));
                mOpenWriters.remove(writer);
                ZimbraPerf.COUNTER_IDX_WRT.increment(mOpenWriters.size());
                writer.setState(WriterState.CLOSED);
                mNumFlushing--;
                if (!mWaitingForSlot.isEmpty()) {
                    CountDownLatch l = mWaitingForSlot.remove(0);
                    l.countDown();
                }
                writer.getFlushWaiterLatch().countDown();
                writer.resetFlushWaiterLatch();
                this.notifyAll();
            }
        }
    }

    /**
     * For testing.
     */
    private static class TestCacheEntry extends CacheEntry {

        Random random;
        boolean opened = false;

        long randomLong(long max) {
            synchronized (random) {
                long l = random.nextLong();
                if (l < 0) {
                    l = -1 * l;
                }
                return l % max;
            }
        }

        @Override
        void doWriterOpen() throws IOException {
            if (!opened) {
                if (random.nextFloat() > 0.9) {
                    throw new IOException("Foo");
                }
                opened = true;
            }
        }

        @Override
        void doWriterClose() {
            assert(opened);
            try {
                if (random.nextFloat() > 0.999) {
                    throw new IllegalArgumentException("foo");
                }
                long length = randomLong(100);
                if (length > 0) {
                    Thread.sleep(length);
                }
                opened = false;
            } catch (InterruptedException e) {
            }
        }

        @Override
        long getLastWriteTime() {
            return mLastAccessTime;
        }

        TestCacheEntry(Random random) {
            this.random = random;
        }

        long mLastAccessTime = System.currentTimeMillis();
    }

    private static class TestThread extends Thread {
        int mNumIters;
        Random mR;
        IndexWritersCache mCache;
        List<TestCacheEntry> mWriters;
        int mWritePct;
        int mFlushPct;
        volatile int mCurrentIteration;

        TestThread(IndexWritersCache cache, List<TestCacheEntry> writers,
                int numIters, int writePct, int flushPct) {
            mCache = cache;
            mWriters = writers;
            mWritePct = writePct;
            mFlushPct = flushPct;
            mNumIters = numIters;
            mR = new Random(System.currentTimeMillis()+this.getId());
        }

        @Override
        public void run() {
            for (int i = 0; i < mNumIters; i++) {
                mCurrentIteration = i;

                int dpct = mR.nextInt(1000);
                if (dpct <= (mWritePct + mFlushPct)) {
                    TestCacheEntry writer = mWriters.get(mR.nextInt(mWriters.size()));
                    synchronized (writer) {
                        if (dpct <= mFlushPct) {
                        } else {
                            try {
                                mCache.beginWriting(writer);
                                try {
                                    Thread.sleep(5);
                                } catch (InterruptedException e) {
                                }
                                mCache.doneWriting(writer);
                            } catch (IOException e) {
                            }
                        }
                    }
                } else {
                    System.out.println("Sleeping...");
                }
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        int cacheSize = 20;
        int numWriters = 200;
        int numThreads = 10;
        int sweepFrequencyS = 1;
        int idleFlushTimeS = 2000;

        int flushDeciPercent = 1; // 1%
        int writeDeciPercent = 999; // 99%

        ZimbraLog.toolSetupLog4j("WARN", "/tmp/iwc_test.txt");
        LC.zimbra_index_lru_size.setDefault(Integer.toString(cacheSize));
        LC.zimbra_index_sweep_frequency.setDefault(Integer.toString(sweepFrequencyS));
        LC.zimbra_index_idle_flush_time.setDefault(Integer.toString(idleFlushTimeS));
        // LC.zimbra_index_flush_pool_size.setDefault(Integer.toString(100));

        IndexWritersCache cache = new IndexWritersCache();
        Random random = new Random(System.currentTimeMillis());

        List<TestCacheEntry> writers = new ArrayList<TestCacheEntry>();
        for (int i = 0; i < numWriters; i++) {
            writers.add(new TestCacheEntry(random));
        }

        long start = System.currentTimeMillis();

        System.out.println("Beginning run w/ cache size = " + cacheSize +
                " numWriters=" + numWriters + " numThreads=" + numThreads);

        TestThread[] t = new TestThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            t[i] = new TestThread(cache, writers, Integer.MAX_VALUE,
                    writeDeciPercent, flushDeciPercent);
        }

        for (int i = 0; i < numThreads; i++) {
            t[i].start();
        }

        boolean stop = false;
        while(!stop) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }
            int lowestIter = Integer.MAX_VALUE;
            int highestIter = -1;
            for (TestThread tt : t) {
                int threadIter = tt.mCurrentIteration;
                lowestIter = Math.min(lowestIter, threadIter);
                highestIter = Math.max(highestIter, threadIter);
            }
            System.out.println("Lowest iter is " + lowestIter +
                    " Highest=" + highestIter +
                    " NumFlushing=" + cache.mNumFlushing);
        }

        for (int i = 0; i < numThreads; i++) {
            try {
                t[i].join();
            } catch (InterruptedException e) {
                System.err.println("Interrupted: " + e);
                e.printStackTrace();
            }
        }

        cache.mShutdown = true;
        long end  = System.currentTimeMillis();
        long total = end - start;
        System.out.println("Test Done w/ cache="+cacheSize+" Took "+ total+"ms");

        cache.shutdown();
    }
}
