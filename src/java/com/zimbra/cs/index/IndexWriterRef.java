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

import org.apache.lucene.index.IndexWriter;

/**
 * {@link IndexWriter} wrapper that supports a reference counter.
 *
 * @author ysasaki
 */
final class IndexWriterRef {

    private final LuceneIndex index;
    private final IndexWriter writer;
    private int count = 1;

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

    synchronized void inc() {
        assert(count > 0);
        count++;
    }

    synchronized void dec() {
        assert(count > 0);
        count--;
        if (count == 0) {
            index.closeWriter();
        }
    }

}
