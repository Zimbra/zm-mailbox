/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
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
 * Primary Search Interface
 */
class RefCountedIndexSearcher {
    private Searcher mSearcher;
    private RefCountedIndexReader mReader;
    private int mCount = 1;
    private Sort mSort = null;
    
    RefCountedIndexSearcher(RefCountedIndexReader reader) {
        mReader= reader;
        mSearcher = new IndexSearcher(mReader.getReader());
    }
    synchronized void setSort(Sort sort) {
        mSort = sort;
    }
    synchronized Sort getSort() {
        return mSort;
    }
    synchronized Searcher getSearcher() { return mSearcher; }
    synchronized IndexReader getReader() { return mReader.getReader(); }
    synchronized void forceClose() {
        mReader.forceClose();
        mReader = null;
    }
    synchronized void release() {
        mSort = null;
        mCount--;
        assert(mCount >= 0);
        if (0 == mCount) {
            mReader.release();
            mReader= null;
        }
    }
    synchronized RefCountedIndexSearcher addRef() {
        assert(mCount > 0);
        mCount++;
        return this;
    }
    
    synchronized TopDocs search(Query query, Filter filter, int num) throws IOException {
        if (mSort == null) {
            return getSearcher().search(query, filter, num);            
        } else {
            return getSearcher().search(query, filter, num, mSort);
        }
    }
}