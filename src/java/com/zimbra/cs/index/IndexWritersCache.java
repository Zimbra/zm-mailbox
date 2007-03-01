/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * Self-Sweeping (using it's own thread) Cache of open IndexWriters.  
 */
final class IndexWritersCache extends Thread {
    private static Log sLog = LogFactory.getLog(IndexWritersCache.class);
    private static final int MIN_FREE_AFTER_SWEEP = 10;
    
    private boolean mShutdown = false;
    private long mSweepIntervalMS;
    private long mIdleMS;
    private int mMaxSize;
    
    // List of MailboxIndex objects that are waiting for room to free up in
    // sOpenIndexWriters map before opening index writer.  See openIndexWriter()
    // method.
    private Object mOpenWaiters;
    
    // Key is MailboxIndex and value is always null.  LinkedHashMap class is
    // used for its access-order feature.
    private LinkedHashMap<MailboxIndex, Object> mOpenIndexWriters;
    
    IndexWritersCache(long intervalMS, long idleMS, int maxSize) {
        super("IndexWritersCache-Sweeper");
        mSweepIntervalMS = intervalMS;
        mIdleMS = idleMS;
        mMaxSize = maxSize;
        mOpenIndexWriters = new LinkedHashMap<MailboxIndex, Object>(200, 0.75f, false);
        mOpenWaiters = new Object();
    }

    /**
     * Shutdown the sweeper thread
     */
    synchronized void signalShutdown() {
        mShutdown = true;
        wakeupSweeperThread();
    }

    /**
     * Wake up the sweeper now (instead of waiting for its normal wakeup) -- and
     * tell it to close some IndexWriters.  This is usually called when a server 
     * thread realizes the LRU is full and it wants to open an index now.
     */
    private void wakeupSweeperThread() {
        synchronized(this) {
            notify();
        }
    }
    
    /**
     * Add or Update the MailboxIndex in the LRU (update means change the last 
     * accessed time) the passed-in MailboxIndex to the LRU.  This function will 
     * coordinate with the sweeper thread and may block the calling thread until
     * the sweeper thread can free up an LRU "slot"
     * 
     * @param idx
     */
    public void putIndexWriter(MailboxIndex idx) {
        int sizeAfter = 0;
        while (true) {
            synchronized (mOpenIndexWriters) {
                int numOpenWriters = mOpenIndexWriters.size();
                ZimbraPerf.COUNTER_IDX_WRT.increment(numOpenWriters);
                // Make sure there is room for it in sOpenIndexWriters map.
                if (numOpenWriters < MailboxIndex.sLRUSize) {
                    assert(!mOpenIndexWriters.containsKey(idx));
                    mOpenIndexWriters.put(idx, idx);
                    sizeAfter = mOpenIndexWriters.size();
                    // Proceed.
                    break;
                }
            }
            // If we get to here, then the Map is full.  Add current thread to waiter 
            // list and wait until notified when there's room in the map.
            synchronized (mOpenWaiters) {
                wakeupSweeperThread();
                try {
                    mOpenWaiters.wait(5000);
                } catch (InterruptedException e) {}
            }
        }
        
        assert(this.contains(idx));
        
        if (sLog.isDebugEnabled())
            sLog.debug("openIndexWriter: map size after open = " + sizeAfter);
    }
    
    boolean contains(MailboxIndex idx) {
        synchronized(mOpenIndexWriters) {
            return mOpenIndexWriters.containsKey(idx);
        }
    }
    
    /**
     * Called when an IndexWriter closes itself, to remove itself from the LRU
     * 
     * @param idx
     */
    public void removeIndexWriter(MailboxIndex idx) {
        int sizeAfter;
        Object removed;
        synchronized(mOpenIndexWriters) {
            removed = mOpenIndexWriters.remove(idx);
            sizeAfter = mOpenIndexWriters.size();
            assert(!mOpenIndexWriters.containsKey(idx));
        }
        if (removed != null) {
            synchronized(mOpenWaiters) {
                // Notify a waiter that was waiting for room to free up in map.
                mOpenWaiters.notify();
            }
            if (sLog.isDebugEnabled())
                sLog.debug("closeIndexWriter: map size after close = " + sizeAfter);
        }
    }
    
    /**
     * Flush and close all the IndexWriters in our cache
     */
    public void flushAllIndexWriters() {
        sLog.info("Flushing all open index writers");
        ArrayList<MailboxIndex> toRemove = new ArrayList<MailboxIndex>();
        synchronized(mOpenIndexWriters) {
            for (Iterator it = mOpenIndexWriters.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry) it.next();
                MailboxIndex mi = (MailboxIndex) entry.getKey();
                toRemove.add(mi);
            }
        }
        for (Iterator it = toRemove.iterator(); it.hasNext(); ) {
            MailboxIndex mi = (MailboxIndex) it.next();
            mi.flush();
        }
    }
    
    /**
     * Main loop for the Sweeper thread.  This thread does a sweep automatically
     * every (mSweepIntervalMS) ms, or it will run a sweep when woken up
     * bia the wakeupSweeperThread() API
     */
    public void run() {
        sLog.info(getName() + " thread starting");

        boolean full = false;
        boolean shutdown = false;
        long startTime = System.currentTimeMillis();
        ArrayList<MailboxIndex> toRemove = new ArrayList<MailboxIndex>();

        while (!shutdown) {
            // Sleep until next scheduled wake-up time, or until notified.
            synchronized (this) {
                if (!mShutdown && !full) {  // Don't go into wait() if shutting down.  (bug 1962)
                    long now = System.currentTimeMillis();
                    long until = startTime + mSweepIntervalMS;
                    if (until > now) {
                        try {
                            wait(until - now);
                        } catch (InterruptedException e) {}
                    }
                }
                shutdown = mShutdown;
            }

            startTime = System.currentTimeMillis();

            int sizeBefore;

            // Flush out index writers that have been idle too long.
            toRemove.clear();
            synchronized (mOpenIndexWriters) {
                sizeBefore = mOpenIndexWriters.size();
                long cutoffTime = startTime - mIdleMS;
                for (Iterator it = mOpenIndexWriters.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry) it.next();
                    MailboxIndex mi = (MailboxIndex) entry.getKey();
                    if (mi.getLastWriteTime() < cutoffTime) {
                        toRemove.add(mi);
                    }
                }
            }
            int removed = closeWriters(toRemove);

            // Flush out more index writers if map is still too big.
            toRemove.clear();
            synchronized (mOpenIndexWriters) {
                int excess = mOpenIndexWriters.size() - (mMaxSize - MIN_FREE_AFTER_SWEEP);

                // just error check, make sure the excess isn't bigger than the indexes we have
                if (excess > mOpenIndexWriters.size()) 
                    excess = mOpenIndexWriters.size();

                if (excess > 0) {
                    int num = 0;
                    for (Iterator it = mOpenIndexWriters.entrySet().iterator();
                    it.hasNext() && num < excess;
                    num++) {
                        Map.Entry entry = (Map.Entry) it.next();
                        toRemove.add((MailboxIndex)(entry.getKey()));
                    }
                }
            }
            removed += closeWriters(toRemove);

            // Get final map size at the end of sweep.
            int sizeAfter;
            synchronized (mOpenIndexWriters) {
                sizeAfter = mOpenIndexWriters.size();
            }
            long elapsed = System.currentTimeMillis() - startTime;

            if (removed > 0 || sizeAfter > 0)
                sLog.info("open index writers sweep: before=" + sizeBefore +
                            ", closed=" + removed +
                            ", after=" + sizeAfter + " (" + elapsed + "ms)");

            full = sizeAfter >= mMaxSize;
            
            // Wake up some threads that were waiting for room to insert in map.
            if (sizeAfter < sizeBefore) {
                int howmany = sizeBefore - sizeAfter;
                for (int i = 0; i < howmany; i++) {
                    synchronized(mOpenWaiters) {
                        mOpenWaiters.notify();
                    }
                }
            }
        }

        sLog.info(getName() + " thread exiting");
    }

    private int closeWriters(List<MailboxIndex> writers) {
        int toRet = 0;
        for (MailboxIndex mi : writers) {
            if (sLog.isDebugEnabled()) 
                sLog.debug("Flushing index writer: " + mi);
            mi.flush();
            toRet++;
        }
        return toRet;
    }
}