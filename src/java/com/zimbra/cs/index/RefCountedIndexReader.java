package com.zimbra.cs.index;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

/**
 * Ref Counting wrapper around a Lucene IndexReader
 */
final class RefCountedIndexReader {
    private IndexReader mReader;
    private int mCount = 1;
    private long mAccessTime;

    RefCountedIndexReader(IndexReader reader) {
        mReader= reader;
        mAccessTime = System.currentTimeMillis();
    }
    
    public synchronized IndexReader getReader() { return mReader; }

    public synchronized void addRef() {
        mAccessTime = System.currentTimeMillis();
        mCount++;
    }

    public synchronized void forceClose() {
        closeIt();
    }

    public synchronized void release() {
        mCount--;
        assert(mCount >= 0);
        if (0 == mCount) {
            closeIt();
        }
    }
    
    synchronized long getAccessTime() {
        return mAccessTime;
    }

    private void closeIt() {
        try {
            mReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mReader= null;
        }
    }

    protected void finalize() {
        if (mReader != null) {
            throw new java.lang.RuntimeException("Reader isn't closed in RefCountedIndexReader's finalizer!");
        }
    }
}