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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;

/**
 * Ref Counting Wrapper around Lucene IndexSearcher
 */
public class RefCountedIndexSearcher {
    private Searcher mSearcher;
    private RefCountedIndexReader mReader;
    private int mCount = 1;
    
    RefCountedIndexSearcher(RefCountedIndexReader reader) {
        mReader= reader;
        mSearcher = new IndexSearcher(mReader.getReader());
    }
    public synchronized Searcher getSearcher() { return mSearcher; }
    public synchronized IndexReader getReader() { return mReader.getReader(); }
    public synchronized void forceClose() {
        mReader.forceClose();
        mReader = null;
    }
    public synchronized void release() {
        mCount--;
        assert(mCount >= 0);
        if (0 == mCount) {
            mReader.release();
            mReader= null;
        }
    }
    public synchronized RefCountedIndexSearcher addRef() {
        assert(mCount > 0);
        mCount++;
        return this;
    }
}