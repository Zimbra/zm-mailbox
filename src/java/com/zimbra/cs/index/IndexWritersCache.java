/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
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
  
  Model writers via a state machine.  4 possible states:
    - CLOSED    
    - WRITING
    - IDLE
    - FLUSHING
  
   Valid state transitions:
      I->W - BeginWriting 
      C->W - BeginWriting
      W->I - DoneWriting
      I->F - BeginFlush
      F->C - FlushCompleted

    All writes must happen between a BeginWriting()...DoneWriting() pair.  The pair should be inside a synchronized block.
    
    Threads block in the following places:
      1) If they're trying to open, and need a slot (mWaitingForSlot list)
      2) In various states, if they're being flushed by another thread (latch on the writer)
      3) The sweeper waits on the global cache lock
      
 */
class IndexWritersCache {
    // all open writers
    private Set<IndexWriter> mOpenWriters = new LinkedHashSet<IndexWriter>();

    // writers in the IDLE state
    private Set<IndexWriter> mIdleWriters = new LinkedHashSet<IndexWriter>();

    /**
     * How often do we walk the list of open IndexWriters looking for idle writers
     * to close.  On very busy systems, the default time might be too long.
     */
    private final long mSweeperTimeout = LC.zimbra_index_sweep_frequency.intValueWithinRange(1, 3600) * Constants.MILLIS_PER_SECOND;


    /**
     * After we add a document to it, how long do we hold an index open for writing before closing it 
     * (and therefore flushing the writes to disk)?
     * 
     * Note that there are other things that might cause us to flush the index to disk -- e.g. if the user
     * does a search on the index, or if the system decides there are too many open IndexWriters (see 
     * sLRUSize) 
     */
    private final long mMaxIdleTime = 1000 * LC.zimbra_index_idle_flush_time.intValueWithinRange(1,60*60*24);
    
    /**
     * How many open indexWriters do we allow?  This value is here so that the # open file 
     * descriptors and amount of buffered index memory is controlled.
     */
    private final int mMaxOpen = LC.zimbra_index_lru_size.intValueWithinRange(10, Integer.MAX_VALUE);   // maximum allowed in the LRU
    private final int mFlushPoolSize = LC.zimbra_index_flush_pool_size.intValueWithinRange(1, 100);
    
    private int mNumFlushing = 0; // number queued or running async flush tasks
    private boolean mShutdown = false;
    private Thread mSweeperThread = null;
    private List<CountDownLatch> mWaitingForSlot = new ArrayList<CountDownLatch>();
    
    private ThreadPool mPool = new ThreadPool("IndexWriterFlush", mFlushPoolSize);
    
    enum WriterState {
        CLOSED,
        WRITING,
        IDLE,
        FLUSHING;
    }

    
    public IndexWritersCache() {
        Runnable sweeper = new Runnable() {
            public void run() {
                doSweep();
            }
        };
        mSweeperThread = new Thread(sweeper, "IndexWriterSweeper");
        mSweeperThread.start();
    }
    
    static abstract class IndexWriter {
        /**
         * Must handle (presumably a NoOp) the case where the writer is already open
         * @throws IOException
         */
        abstract void doWriterOpen() throws IOException;
        abstract void doWriterClose();
        abstract long getLastWriteTime();
        
        public WriterState getState() {
            return mState;
        }
        public void setState(WriterState state) {
            mState = state;
        }
        public CountDownLatch getFlushWaiterLatch() {
            return mFlushWaiterLatch;
        }
        public void resetFlushWaiterLatch() {
            mFlushWaiterLatch = new CountDownLatch(1);
        }
        
        private WriterState mState = WriterState.CLOSED;
        private CountDownLatch mFlushWaiterLatch = new CountDownLatch(1);
    }
    
    private int mNumCacheHits = 0;
    private int mNumCacheOpens = 0;
    
    public synchronized boolean isWriterOpen(IndexWriter w) {
        return (w.getState() != WriterState.CLOSED);
    }
    
    public void flushAllWriters() {
        List<IndexWriter> toFlush = new ArrayList<IndexWriter>();
        synchronized(this) {
            toFlush.addAll(mOpenWriters);
        }
        for (IndexWriter w : toFlush) {
            flush(w);
        }
    }
    
    public void beginWriting(IndexWriter w) throws IOException {
        assert(!Thread.holdsLock(this));
        boolean done = false;
        ZimbraPerf.COUNTER_IDX_WRT_OPENED.increment();
        do {
            CountDownLatch l = null;
            synchronized(this) {
                switch (w.getState()) {
                    case FLUSHING:
                        l = w.getFlushWaiterLatch();
                        assert(!mIdleWriters.contains(w));
                        break;
                    case IDLE:
                        assert(mIdleWriters.contains(w));
                        mIdleWriters.remove(w);
                        assert(!mIdleWriters.contains(w));
                        w.setState(WriterState.WRITING);
                        done = true;
                        ZimbraPerf.COUNTER_IDX_WRT_OPENED_CACHE_HIT.increment();
                        break;
                    case WRITING:
                        assert(false);
                        done = true;
                        break;
                    case CLOSED:
                        assert(!mIdleWriters.contains(w));
                        // not already idle -- find a slot for us
                    
                        // is there a slot for us?
                        if (mOpenWriters.size() < mMaxOpen) {
                            done = true;
                            // at this point we have a slot!
                            mOpenWriters.add(w);
                            w.setState(WriterState.WRITING);
                        } else {
                            // nope
                            l = new CountDownLatch(1);
                            mWaitingForSlot.add(l);
                            this.notifyAll();
                        }
                        break;
                }
            }
            if (!done) {
                try {
                    l.await();
                } catch (InterruptedException e) {}
            }
            
            ZimbraPerf.COUNTER_IDX_WRT.increment(mOpenWriters.size());
        } while (!done);
        try {
            // at this point we have a slot, and we're in the active list
            w.doWriterOpen();
        } catch (IOException e) {
            openFailed(w);
            throw e;
        }
    }
    
    private synchronized void openFailed(IndexWriter w) {
        w.setState(WriterState.CLOSED);
        mIdleWriters.remove(w);
        mOpenWriters.remove(w);
        ZimbraPerf.COUNTER_IDX_WRT.increment(mOpenWriters.size());
        if (!mWaitingForSlot.isEmpty()) {
            CountDownLatch l = mWaitingForSlot.remove(0);
            l.countDown();
        }
        w.getFlushWaiterLatch().countDown();
        w.resetFlushWaiterLatch();
        this.notifyAll();
    }
    
    public synchronized void doneWriting(IndexWriter w) {
        assert(w.getState() == WriterState.WRITING);
        assert(!mIdleWriters.contains(w));
        assert(mOpenWriters.contains(w));
        w.setState(WriterState.IDLE);
        mIdleWriters.add(w);
        this.notifyAll();
    }
    
    public void shutdown() {
        synchronized(this) {
            mShutdown = true;
            this.notifyAll();
            while (!mOpenWriters.isEmpty()) {
            	this.notifyAll();
            	try {
            		this.wait(100);
            	} catch (InterruptedException e) {}
            }
        }
        try {
            mSweeperThread.join();
            mPool.shutdown();
        } catch (InterruptedException e) {}
    }
    
    /**
     * Called in the sweeper thread
     */
    private void doSweep() {
        try {
            while(true) {
                Set<IndexWriter> toFlush = new HashSet<IndexWriter>();
                long idleCutoff = System.currentTimeMillis() - mMaxIdleTime;
                synchronized(this) {
                    if (mShutdown && mOpenWriters.isEmpty())
                        return;
                    
                    // timeouts
                    for (IndexWriter w : mIdleWriters) {
                        assert(w.getState() == WriterState.IDLE);
                        if (mShutdown || (w.getLastWriteTime() < idleCutoff)) {
                            toFlush.add(w);
                        }
                    }

                    int additionalNeeded = mWaitingForSlot.size() - (mNumFlushing + toFlush.size());
                    if (additionalNeeded > 0) {
                        for (IndexWriter w : mIdleWriters) {
                            assert(w.getState() == WriterState.IDLE);
                            if (additionalNeeded <= 0)
                                break;
                            if (!toFlush.contains(w)) {
                                additionalNeeded--;
                                toFlush.add(w);
                            }
                        }
                    }
                    
                    if (toFlush.isEmpty()) {
                        try {
                            this.wait(mSweeperTimeout);
                        } catch (InterruptedException e) { }
                    } else {
                        for (IndexWriter w : toFlush) {
                        	assert(w.getState() == WriterState.IDLE);
                        	mNumFlushing++;
                        	w.setState(WriterState.FLUSHING);
                        	mIdleWriters.remove(w);
                        }
                    }
                }
                
                // submit outside the global lock, so we don't deadlock if the
                // work queue fills up
                for (IndexWriter w : toFlush) {
                    AsyncFlush af = new AsyncFlush(w);
                    try {
                        mPool.execute(af);
                    } catch (OutOfMemoryError e) {
                    	Zimbra.halt("OutOfMemory in IndexWritersCache.doSweep", e);
                    } catch (RejectedExecutionException e) {
                    	ZimbraLog.index.debug("Sweeper hit interruptedException attempting to async flush "+w+". Flushing synchronously.");
                    	flushInternal(w);
                    } catch (Throwable t) {
                        System.err.println("Error! "+t);
                        synchronized(this) {
                        	mNumFlushing--;
                        	w.setState(WriterState.IDLE);
                        	mIdleWriters.add(w);
                        }
                    }
                }
            }
        } catch (OutOfMemoryError e) {
        	Zimbra.halt("OutOfMemory in IndexWritersCache.doSweep", e);
        }
    }
    
    class AsyncFlush implements Runnable {
        private IndexWriter mTarget;
        
        AsyncFlush(IndexWriter target) {
            mTarget = target;
        }
        
        public void run() {
            try {
                flushInternal(mTarget);
            } catch (OutOfMemoryError e) {
                Zimbra.halt("OutOfMemory in IndexWritersCache.AsyncFlush", e);
            }
        }
    }
    
    void flush(IndexWriter target) {
        synchronized(this) {
            while (target.getState() == WriterState.FLUSHING) {
                // already flushing...have to wait for that flush to finish
                // so that we can return
            	try {
            		this.wait(100); 
            	} catch (InterruptedException e) {}
            }
            if (target.getState() == WriterState.CLOSED)
            	return;
            if (target.getState() == WriterState.WRITING)
                doneWriting(target);
            
            assert(target.getState() == WriterState.IDLE);
            mNumFlushing++;
            target.setState(WriterState.FLUSHING);
            mIdleWriters.remove(target);
        }
        flushInternal(target);
    }
    
    private void flushInternal(IndexWriter target) {
        assert(!Thread.holdsLock(this));
        synchronized(this) {
            assert(target.getState() == WriterState.FLUSHING);
        }
        target.doWriterClose();
        synchronized(this) {
            assert(target.getState() == WriterState.FLUSHING);
            assert(mOpenWriters.contains(target));
            assert(!mIdleWriters.contains(target));
            mOpenWriters.remove(target);
            ZimbraPerf.COUNTER_IDX_WRT.increment(mOpenWriters.size());
            target.setState(WriterState.CLOSED);
            mNumFlushing--;
            if (!mWaitingForSlot.isEmpty()) {
                CountDownLatch l = mWaitingForSlot.remove(0);
                l.countDown();
            }
            target.getFlushWaiterLatch().countDown();
            target.resetFlushWaiterLatch();
            this.notifyAll();
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////////
    // test harness code below here
    ////////////////////////////////////////////////////////////////////////////////////
    static class TestWriter extends IndexWriter {
        
        Random r;
        boolean opened = false;
        
        long randomLong(long max) {
            synchronized(r) {
                long l = r.nextLong();
                if (l < 0)
                    l = -1 * l;
                return l % max;
            }
        }
        
        public void doWriterOpen() {
            if (!opened) {
                try {
                    Thread.sleep(0);
                    opened = true;
                } catch (InterruptedException e) {}
            }
        }
        
        public void doWriterClose() {
            assert(opened);
            try {
                Thread.sleep(0);
                opened = false;
            } catch (InterruptedException e) {}
        }
        
        public long getLastWriteTime() {
            return mLastAccessTime;
        }
        
        public void write() {
            synchronized(this) {
                try {
                    mCache.beginWriting(this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                assert(opened);
                try {
                    Thread.sleep(0);
                } catch (InterruptedException e) {}
                assert(opened);
                mCache.doneWriting(this);
                mLastAccessTime = System.currentTimeMillis();
            }
        }
        
        public void flush() {
            synchronized(this) {
                mCache.flush(this);
            }
        }
        
        TestWriter(Random r, IndexWritersCache cache) {
            this.r = r;
            mCache = cache;
        }
        
        WriterState mState = WriterState.CLOSED;
        long mLastAccessTime = System.currentTimeMillis();
        IndexWritersCache mCache;
    }
    
    static class TestBase {
        IndexWritersCache mCache;
        TestWriter mWriter;
        
        TestBase(IndexWritersCache cache, TestWriter writer) {
            mCache = cache;
            mWriter = writer;
        }
        
    }
    
    static class DoWrites extends TestBase implements Runnable {
        int mNum;
        DoWrites(int num, IndexWritersCache cache, TestWriter writer) {
            super(cache, writer);
            mNum = num;
        }
        public void run() {
            try {
                for (int i=0; i< mNum; i++) { 
                    mWriter.write();
                }
//                System.out.println("Done "+mNum+" writes");
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Caught IllegalArgumentException: "+e);
            }
        }
    }
    
    static class DoFlush extends TestBase implements Runnable {
        DoFlush(IndexWritersCache cache, TestWriter writer) {
            super(cache, writer);
        }
        public void run() {
            try {
                mWriter.flush();
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Caught IllegalArgumentException: "+e);
            }
        }
    }
    
    static class DoNothing extends TestBase implements Runnable {
        DoNothing(IndexWritersCache cache, TestWriter writer) {
            super(cache, writer);
        }
        public void run() {
//            try {
//                Thread.sleep(100);                
//            } catch (InterruptedException e) {}
        }
    }
    
    /**
     * @param args
     */
   public static void main(String[] args) {
        
        int numWriters  = 10000;
        for (int testNum = 0; testNum < 10; testNum++) {
            int cacheSize = (testNum+1)*200;
            LC.zimbra_index_lru_size.setDefault(Integer.toString(cacheSize));
            IndexWritersCache cache = new IndexWritersCache();
            ThreadPool testPool = new ThreadPool("Test",10);
            Random r = new Random(1234);

            List<TestWriter> writers = new ArrayList<TestWriter>();
            for (int i = 0; i < numWriters; i++) {
                TestWriter w = new TestWriter(r, cache); 
                writers.add(w);
            }
            
            int preloadNum = Math.min(cacheSize, numWriters);
            
            System.out.println("Preloading "+preloadNum+" writers into cache"); 
            // preload
            for (int i = 0; i < preloadNum; i++) {
                TestWriter w = writers.get(i);
                synchronized(w) {
                    try {
                        cache.beginWriting(w);
                        cache.doneWriting(w);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            long start = System.currentTimeMillis();
            int numIters = 900;
            
            System.out.println("Beginning run ("+numIters+" iterations) w/ cache size = "+cacheSize+
                               " numWriters="+numWriters);
            synchronized(cache) {
                cache.mNumCacheHits = 0;
                cache.mNumCacheOpens = 0;
            }
            
            int totalOpens = 0;
            int totalHits = 0;

            for (int i = 0; i < numIters; i++) {
                if (true)
                    synchronized(cache) {
                        if ((i+1) % 100 == 0) {
                            float hitPct = (float)cache.mNumCacheHits / (float)cache.mNumCacheOpens;
                            hitPct *= 100.0;
                            System.out.println("Submitted iteration "+(i+1)+" - open="+cache.mOpenWriters.size()+
                                               " idle="+cache.mIdleWriters.size()+" flushing="+cache.mNumFlushing+
                                               " waiting="+cache.mWaitingForSlot.size()+" cache hit rate="+hitPct+"%");
                            totalOpens += cache.mNumCacheOpens;
                            totalHits += cache.mNumCacheHits;
                            cache.mNumCacheHits = 0;
                            cache.mNumCacheOpens = 0;
                        }
                    }
                int writerId = r.nextInt(writers.size());
                TestWriter w = writers.get(writerId);

                long l = r.nextLong();
                if (l < 0) 
                    l = -1 * l;
                l = l % 1000;

                int num = r.nextInt(10);
                if (num <= 8)
                    num = 1;
                else 
                    num = num-8; 

                try {
/*                    if (l > 800)
                        testPool.execute(new DoNothing(cache, w));
                    else *///if (l <= 100)
//                        testPool.execute(new DoFlush(cache, w));
//                    else
                        testPool.execute(new DoWrites(num, cache, w));
                } catch (RejectedExecutionException e) {
                    e.printStackTrace();
                    System.err.println("InterruptedException: "+e);
                }
            }

            long end  = System.currentTimeMillis();

            testPool.shutdown();

            long total = end - start;
            System.out.println("Test "+testNum+" Done w/ cache="+cacheSize+" Took "+ total+"ms");
            float hitPct = (float)totalHits / (float)totalOpens;
            hitPct *= 100.0;
            System.out.println("\t"+totalOpens+" opens, "+totalHits+" hits "+
                               hitPct+"% hit rate");

            cache.shutdown();
        }
    }
}
