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

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

import com.zimbra.common.util.ZimbraLog;

/**
 * Ref Counting wrapper around a Lucene IndexReader
 */
final class RefCountedIndexReader {
    private ILuceneIndex mIdx;
    private Object mCommitPoint;
    private IndexReader mReader;
    private int mCount = 1;
    private long mAccessTime;

    RefCountedIndexReader(ILuceneIndex idx, IndexReader reader) {
        mIdx = idx;
        mCommitPoint = mIdx.getCurrentCommitPoint();
        mReader= reader;
        mAccessTime = System.currentTimeMillis();
    }
    
    Object getCommitPoint() {
        return mCommitPoint;
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
            ZimbraLog.im.debug("Caught exception while closing IndexReader: ", e);
        } finally {
            mReader= null;
            mIdx.onClose(this);
        }
    }

    protected void finalize() {
        if (mReader != null) {
            throw new java.lang.RuntimeException("Reader isn't closed in RefCountedIndexReader's finalizer!");
        }
    }
}