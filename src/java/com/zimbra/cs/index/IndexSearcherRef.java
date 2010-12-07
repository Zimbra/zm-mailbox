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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

/**
 * Reference to {@link IndexSearcher} that supports reference count.
 */
class IndexSearcherRef {
    private final Searcher searcher;
    private final IndexReaderRef reader;
    private int count = 1;
    private Sort sort = null;

    IndexSearcherRef(IndexReaderRef reader) {
        this.reader = reader;
        searcher = new IndexSearcher(reader.get());
    }

    synchronized void setSort(Sort sort) {
        this.sort = sort;
    }

    synchronized Sort getSort() {
        return sort;
    }

    Searcher getSearcher() {
        return searcher;
    }

    /**
     * Decrements the reference counter.
     * <p>
     * When the reference counter reached to 0, it closes the underlying
     * {@link IndexReader}.
     */
    synchronized void dec() {
        sort = null;
        count--;
        assert(count >= 0);
        if (0 == count) {
            reader.dec();
        }
    }

    /**
     * Increments the reference counter.
     *
     * @return underlying {@link IndexSearcher} object
     */
    synchronized void inc() {
        assert(count > 0);
        count++;
    }

    synchronized TopDocs search(Query query, Filter filter, int num) throws IOException {
        if (sort == null) {
            return getSearcher().search(query, filter, num);
        } else {
            return getSearcher().search(query, filter, num, sort);
        }
    }
}
