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