/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2010 Zimbra, Inc.
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
/**
 * 
 */
package com.zimbra.cs.store;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.zimbra.cs.stats.ZimbraPerf;

/**
 * Synchronized container for a <tt>RandomAccessFile</tt> object.  Used by multiple
 * <tt>BlobInputStream</tt> objects that share a single file descriptor.
 */
class SharedFile {
    private File mFile;
    private RandomAccessFile mRAF;
    private long mPos = 0;
    
    SharedFile(File file) {
        if (file == null) {
            throw new NullPointerException();
        }
        mFile = file;
    }
    
    synchronized File getFile() {
        return mFile;
    }
    
    synchronized int read(long fileOffset, byte[] buf, int bufferOffset, int len)
    throws IOException {
        if (mRAF == null) {
            mRAF = new RandomAccessFile(mFile, "r");
            mPos = 0;
        }
        if (mPos != fileOffset) {
            mRAF.seek(fileOffset);
            mPos = fileOffset;
            ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_SEEK_RATE.increment(100);
        } else {
            ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_SEEK_RATE.increment(0);
        }
        int numRead = mRAF.read(buf, bufferOffset, len);
        if (numRead > 0) {
            mPos += numRead;
        }
        
        ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_READ.increment();
        return numRead;
    }
    
    synchronized void fileMoved(File newFile)
    throws IOException {
        if (newFile == null) {
            throw new NullPointerException();
        }
        close();
        mFile = newFile;
    }

    synchronized void close()
    throws IOException {
        if (mRAF != null) {
            mRAF.close();
            mPos = 0;
            mRAF = null;
        }
    }
    
}