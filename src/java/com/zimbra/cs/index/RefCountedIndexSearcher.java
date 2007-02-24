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

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;

/**
 * Ref Counting Wrapper around Lucene IndexSearcher
 */
class RefCountedIndexSearcher {
    private Searcher mSearcher;
    private RefCountedIndexReader mReader;
    private int mCount = 1;
    
    RefCountedIndexSearcher(RefCountedIndexReader reader) {
        mReader= reader;
        mSearcher = new IndexSearcher(mReader.getReader());
    }
    public synchronized Searcher getSearcher() { return mSearcher; }
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
    
    protected void finalize() {
        if (mReader != null) {
            throw new java.lang.RuntimeException("Reader isn't closed in RefCountedIndexSearcher's finalizer!");
        }
    }
    
}