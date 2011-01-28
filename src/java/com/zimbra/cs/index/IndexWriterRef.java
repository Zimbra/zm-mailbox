/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.index.IndexWriter;

/**
 * {@link IndexWriter} wrapper that supports a reference counter.
 *
 * @author ysasaki
 */
final class IndexWriterRef {

    private final LuceneIndex index;
    private final IndexWriter writer;
    private final AtomicInteger count = new AtomicInteger(1); // ref counter

    IndexWriterRef(LuceneIndex index, IndexWriter writer) {
        this.index = index;
        this.writer = writer;
    }

    IndexWriter get() {
        return writer;
    }

    LuceneIndex getIndex() {
        return index;
    }

    void inc() {
        count.incrementAndGet();
    }

    void dec() {
        if (count.decrementAndGet() <= 0) {
            index.closeWriter();
        }
    }

}
