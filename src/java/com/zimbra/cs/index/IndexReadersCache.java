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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;

/**
 * Self-sweeping (with it's own sweeper thread) LRU cache of {@link IndexReaderRef}.
 *
 * @author tim
 * @author ysasaki
 */
final class IndexReadersCache {
    private static final Log LOG = LogFactory.getLog(IndexReadersCache.class);

    private final int maxReaders;
    private final Map<LuceneIndex, IndexReaderRef> readers;
    private final ScheduledExecutorService sweeper = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactoryBuilder().setNameFormat("IndexReadersCache-Sweeper").build());
    private final long ttl;

    IndexReadersCache(int max, long ttl, long sweepInterval) {
        Preconditions.checkArgument(ttl >= 0, "ttl must be >= 0: " + ttl);
        Preconditions.checkArgument(sweepInterval >= 100, "sweepInterval must be >= 100: " + sweepInterval);

        this.ttl = ttl;
        maxReaders = max;
        readers = new LinkedHashMap<LuceneIndex, IndexReaderRef>(maxReaders) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<LuceneIndex, IndexReaderRef> eldest) {
                if (size() > maxReaders) {
                    ZimbraLog.index.debug("Prune IndexReader (overflow): %s", eldest.getKey());
                    eldest.getValue().dec();
                    return true;
                } else {
                    return false;
                }
            }
        };
        if (maxReaders > 0) {
            sweeper.scheduleWithFixedDelay(new Sweeper(), sweepInterval, sweepInterval, TimeUnit.MILLISECONDS);
        }
    }

    void shutdown() {
        sweeper.shutdownNow();
        synchronized (this) {
            for (IndexReaderRef reader: readers.values()) {
                reader.dec();
            }
            readers.clear();
        }
    }

    /**
     * Calls {@link IndexReaderRef#inc()} and puts it into the cache.
     *
     * @param index cache key
     * @param ref cache value
     */
    void put(LuceneIndex index, IndexReaderRef ref) {
        if (maxReaders <= 0) { // cache disabled
            return;
        }
        ref.inc();
        synchronized (this) {
            ref = readers.put(index, ref);
        }
        if (ref != null) {
            ref.dec();
        }
    }

    void remove(LuceneIndex index) {
        if (maxReaders <= 0) { // cache disabled
            return;
        }
        IndexReaderRef ref;
        synchronized (this) {
            ref = readers.remove(index);
        }
        if (ref != null) {
            ref.dec();
        }
    }

    /**
     * Marks the cache entry stale.
     */
    void stale(LuceneIndex index) {
        if (maxReaders <= 0) { // cache disabled
            return;
        }

        IndexReaderRef ref ;
        synchronized (this) {
            ref = readers.get(index);
        }
        if (ref != null) {
            ZimbraLog.index.debug("Stale IndexReader: %s", index);
            ref.stale();
        }
    }

    /**
     * Returns the cache value, or null if not cached.
     *
     * @param index cache key
     * @return cache value or null
     */
    IndexReaderRef get(LuceneIndex index) {
        IndexReaderRef ref;
        synchronized (this) {
            ref = readers.get(index);
        }
        if (ref != null) {
            ref.inc();
        }
        return ref;
    }

    private final class Sweeper implements Runnable {
        @Override
        public void run() {
            LOG.debug("begin");

            long cutoff = System.currentTimeMillis() - ttl;
            List<IndexReaderRef> remove = new ArrayList<IndexReaderRef>(readers.size());
            synchronized (IndexReadersCache.this) {
                Iterator<Map.Entry<LuceneIndex, IndexReaderRef>> itr = readers.entrySet().iterator();
                while (itr.hasNext()) {
                    Entry<LuceneIndex, IndexReaderRef> entry = itr.next();
                    if (entry.getValue().getAccessTime() < cutoff) {
                        LOG.debug("Prune IndexReader (time out): %s", entry.getKey());
                        remove.add(entry.getValue());
                        itr.remove();
                    }
                }
            }

            // close outside of synchronized block
            for (IndexReaderRef ref : remove) {
                ref.dec();
            }

            LOG.debug("end");
        }
    }

}
