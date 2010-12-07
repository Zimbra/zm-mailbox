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

import org.apache.lucene.index.IndexReader;

/**
 * Reference to {@link IndexReader} that supports reference count.
 */
final class IndexReaderRef {
    private final LuceneIndex index;
    private final IndexReader reader;
    private int count = 1;
    private long lastAccessTime;
    private boolean stale = false; // reopen if stale

    IndexReaderRef(LuceneIndex index, IndexReader reader) {
        this.index = index;
        this.reader = reader;
        lastAccessTime = System.currentTimeMillis();
    }

    IndexReader get() {
        return reader;
    }

    /**
     * Increments the reference counter.
     */
    synchronized void inc() {
        lastAccessTime = System.currentTimeMillis();
        count++;
    }

    /**
     * Decrements the reference counter.
     * <p>
     * When the reference counter reached to 0, it closes the underlying
     * {@link IndexReader}.
     */
    synchronized void dec() {
        assert count > 0 : count;
        count--;
        if (0 == count) {
            index.closeReader(reader);
        }
    }

    synchronized void stale() {
        stale = true;
    }

    synchronized boolean isStale() {
        return stale;
    }

    synchronized long getAccessTime() {
        return lastAccessTime;
    }

}
