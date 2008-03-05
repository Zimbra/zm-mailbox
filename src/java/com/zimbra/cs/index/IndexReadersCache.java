/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.apache.lucene.index.IndexReader;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

/**
 * Self-sweeping (with it's own sweeper thread) LRU cache of open index readers 
 */
class IndexReadersCache extends Thread {
    private static Log sLog = LogFactory.getLog(IndexReadersCache.class);

    private final int mMaxOpenReaders;
    private LinkedHashMap<ILuceneIndex,RefCountedIndexReader> mOpenIndexReaders;
    private boolean mShutdown;
    private long mSweepIntervalMS;
    private long mMaxReaderOpenTimeMS;
    
    private static boolean sUseReaderReopen = LC.zimbra_index_use_reader_reopen.booleanValue();
    
    IndexReadersCache(int maxOpenReaders, long maxReaderOpenTime, long sweepIntervalMS) {
        super("IndexReadersCache-Sweeper");
        if (maxReaderOpenTime < 0)
            maxReaderOpenTime = 0;
        if (sweepIntervalMS < 100)
            sweepIntervalMS = 100;
        mMaxReaderOpenTimeMS = maxReaderOpenTime;
        mMaxOpenReaders = maxOpenReaders;
        mOpenIndexReaders = new LinkedHashMap<ILuceneIndex,RefCountedIndexReader>(mMaxOpenReaders);
        mShutdown = false;
        mSweepIntervalMS = sweepIntervalMS;
    }
    
    /**
     * Shut down the sweeper thread and clear the cached
     */
    synchronized void signalShutdown() {
        mShutdown = true;
        notify();
    }
    
    /**
     * Put the passed-in IndexReader into the cache, if applicable.  This function
     * will automatically AddRef the IndexReader if it stores it in it's cache
     *  
     * @param idx
     * @param reader
     */
    synchronized void putIndexReader(ILuceneIndex idx, RefCountedIndexReader reader) {
        // special case disabled index reader cache:
        if (mMaxOpenReaders <= 0) {
            return;
        }
        // +1 b/c we haven't added the new one yet
        int toRemove = ((mOpenIndexReaders.size()+1) - mMaxOpenReaders); 
        if (toRemove > 0) {
            // remove extra (above our limit) readers
            for (Iterator<Entry<ILuceneIndex,RefCountedIndexReader>> iter = mOpenIndexReaders.entrySet().iterator(); toRemove > 0; toRemove--) {
                Entry<ILuceneIndex,RefCountedIndexReader> entry = iter.next();
                entry.getValue().release();
                if (sLog.isDebugEnabled())
                    sLog.debug("Releasing index reader for index: "+entry.getKey().toString()+" from cache (too many open)");
                iter.remove();
            }
        }
        assert(toRemove <= 0);
        reader.addRef();
        mOpenIndexReaders.put(idx,reader);
    }
    
    /**
     * Called by the LuceneIndex when it closes the reader itself (e.g. when there is
     * write activity to the index)
     * 
     * @param idx
     */
    synchronized void removeIndexReader(ILuceneIndex idx) {
        if (mMaxOpenReaders <= 0)
            return;
        RefCountedIndexReader reader = mOpenIndexReaders.get(idx);
        if (reader != null && !reader.requiresReopen()) {
            if (sUseReaderReopen && reader.markForReopen()) {
                if (sLog.isDebugEnabled())
                    sLog.debug("IndexReader successfully marked for re-open: "+idx.toString());
                return; // can be reopened: leave in cache and return
            }
            
            // can't reopen in, someone's really using it...
            RefCountedIndexReader removed = mOpenIndexReaders.remove(idx);
            if (removed != null) {
                removed.release();
                if (sLog.isDebugEnabled())
                    sLog.debug("Closing index reader for index: "+idx.toString()+" (removed)");
            }
        }
    }
    
    /**
     * @param idx
     * @return an ALREADY ADDREFED IndexReader, or NULL if there is not one cached
     */
    synchronized RefCountedIndexReader getIndexReader(ILuceneIndex idx) {
        RefCountedIndexReader toRet = mOpenIndexReaders.get(idx);
        if (toRet != null) {
            if (toRet.requiresReopen()) {
                try {
                    IndexReader oldReader = toRet.getReader();
                    IndexReader newReader = oldReader.reopen();
                    if (newReader != null && newReader != oldReader) {
                        // reader changed, must close old one
                        oldReader.close();
                        if (sLog.isDebugEnabled()) 
                            sLog.debug("Reopened new indexreader instance: "+newReader);
                        toRet.reopened(newReader);
                    } else {
                        if (sLog.isDebugEnabled()) 
                            sLog.debug("Attempted reopen but reader was current: "+oldReader);
                        toRet.reopened(oldReader);
                    }
                } catch (IOException e) {
                    ZimbraLog.im.debug("Caught exception while attempting to reopen IndexReader", e);
                    toRet.release();
                    return null;
                }
            }
            toRet.addRef();
        }
        return toRet;
    }
    
    /**
     * @param idx
     * @return
     */
    synchronized boolean containsKey(ILuceneIndex idx) {
        return mOpenIndexReaders.containsKey(idx);
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
                // Sleep until next scheduled wake-up time, or until notified.
                synchronized (this) {
                    long startTime = System.currentTimeMillis();
                    
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
                    
                    if (!shutdown) {
                        long now = System.currentTimeMillis();
                        long cutoff = now - mMaxReaderOpenTimeMS; 
                        
                        for (Iterator<Entry<ILuceneIndex,RefCountedIndexReader>> iter = mOpenIndexReaders.entrySet().iterator(); iter.hasNext(); ) {
                            Entry<ILuceneIndex,RefCountedIndexReader> entry = iter.next();
                            if (entry.getValue().getAccessTime() < cutoff) {
                                if (sLog.isDebugEnabled())
                                    sLog.debug("Releasing cached index reader for index: "+entry.getKey().toString()+" (timed out)");
                                entry.getValue().release();
                                iter.remove();
                            }
                        }
                    } // if (!shutdown)
                } // synchronized(this)
            } // while !shutdown
            
            // Shutdown time: clear the cache now
            synchronized(this) {
                for (RefCountedIndexReader reader: mOpenIndexReaders.values()) {
                    reader.release();
                }
                mOpenIndexReaders.clear();
            }
            sLog.info(getName() + " thread exiting");
        }
    }
}
