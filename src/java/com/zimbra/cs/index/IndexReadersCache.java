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

import java.util.Iterator;
import java.util.LinkedHashSet;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

/**
 * Self-sweeping (with it's own sweeper thread) LRU cache of open index readers 
 */
class IndexReadersCache extends Thread {
    private static Log sLog = LogFactory.getLog(IndexReadersCache.class);

    private final int mMaxOpenReaders;
    private LinkedHashSet<MailboxIndex> mOpenIndexReaders;
    private boolean mShutdown;
    private long mSweepIntervalMS;
    private long mMaxReaderOpenTimeMS;
    
    IndexReadersCache(int maxOpenReaders, long maxReaderOpenTime, long sweepIntervalMS) {
        super("IndexReadersCache-Sweeper");
        if (maxReaderOpenTime < 0)
            maxReaderOpenTime = 0;
        if (sweepIntervalMS < 100)
            sweepIntervalMS = 100;
        mMaxReaderOpenTimeMS = maxReaderOpenTime;
        mMaxOpenReaders = maxOpenReaders;
        mOpenIndexReaders = new LinkedHashSet<MailboxIndex>(mMaxOpenReaders);
        mShutdown = false;
        mSweepIntervalMS = sweepIntervalMS;
    }
    
    public void signalShutdown() {
        synchronized(this) {
            mShutdown = true;
            notify();
        }
    }
    
    public void putIndexReader(MailboxIndex idx) {
        // special case disabled index reader cache:
        if (mMaxOpenReaders <= 0) {
            idx.clearCachedIndexReader();
            return;
        }
        synchronized (mOpenIndexReaders) {
            // +1 b/c we haven't added the new one yet
            int toRemove = ((mOpenIndexReaders.size()+1) - mMaxOpenReaders); 
            if (toRemove > 0) {
                // remove extra (above our limit) readers
                for (Iterator<MailboxIndex> iter = mOpenIndexReaders.iterator(); toRemove > 0; toRemove--) {
                    MailboxIndex cur = iter.next();
                    if (sLog.isDebugEnabled())
                        sLog.debug("Closing index reader for index: "+cur.toString()+" (too many open)");
                    cur.clearCachedIndexReader();
                    iter.remove();
                }
            }
            assert(toRemove <= 0);
            mOpenIndexReaders.add(idx);
        }
    }
    
    /**
     * Called by the MailboxIndex when it closes the reader itself (e.g. when there is
     * write activity to the index)
     * 
     * @param idx
     */
    public void removeIndexReader(MailboxIndex idx) {
        if (mMaxOpenReaders <= 0)
            return;
        
        synchronized (mOpenIndexReaders) {
            if (sLog.isDebugEnabled())
                sLog.debug("Closing index reader for index: "+idx.toString()+" (removed)");
            mOpenIndexReaders.remove(idx);
        }
    }
    
    /**
     * Sweeper thread entry point
     */
    public void run() {
        if (mMaxOpenReaders <= 0) {
            sLog.info(getName() + " thread disabled (Max open IndexReaders set to 0)");
            return;
        } else {
            sLog.info(getName() + " thread starting");
            
            boolean shutdown = false;
            while (!shutdown) {
                long startTime = System.currentTimeMillis();
                
                // Sleep until next scheduled wake-up time, or until notified.
                synchronized (this) {
                    if (!mShutdown) {  // Don't go into wait() if shutting down.  (bug 1962)
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
                
                if (!shutdown) {
                    long now = System.currentTimeMillis();
                    long cutoff = now - mMaxReaderOpenTimeMS; 
                    
                    synchronized (mOpenIndexReaders) {
                        for (Iterator<MailboxIndex> iter = mOpenIndexReaders.iterator(); iter.hasNext();) {
                            MailboxIndex cur = iter.next();
                            if (cur.getIndexReaderAccessTime() < cutoff) {
                                if (sLog.isDebugEnabled())
                                    sLog.debug("Closing index reader for index: "+cur.toString()+" (timed out)");
                                cur.clearCachedIndexReader();
                                iter.remove();
                            }
                        }
                    } // synchronized(mOpenIndexReaders)
                } // if (!shutdown)
            } // while !shutdown
            
            // clear the cache now
            for (MailboxIndex mbidx : mOpenIndexReaders) {
                mbidx.clearCachedIndexReader();
            }
            sLog.info(getName() + " thread exiting");
        }
    }
}
