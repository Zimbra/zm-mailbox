/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
public class SharedFile {

    private File mFile;
    private RandomAccessFile mRAF;
    private long mPos = 0;
    
    /**
     * Keep track of the number of threads that are reading from this file.
     * We do this so that we don't delete a file that's being read on
     * Windows (bug 43497).
     */
    private int mNumReaders;
    
    /**
     * Remember the file's length, in case we have an open file descriptor and the
     * uncompressed cache deletes this file from disk.
     */
    private long mLength;

    /**
     * Creates a new <tt>SharedFile</tt> and opens the underlying
     * file descriptor.
     */
    SharedFile(File file)
    throws IOException {
        if (file == null) {
            throw new NullPointerException("file cannot be null");
        }
        if (!file.exists()) {
            throw new IOException(file.getPath() + " does not exist.");
        }
        mFile = file;
        mLength = file.length();
        openIfNecessary();
    }

    synchronized long getLength() {
        return mLength;
    }
    
    synchronized int read(long fileOffset, byte[] b, int off, int len)
    throws IOException {
        int numRead = 0;
        boolean seeked = false;
        openIfNecessary();
        
        if (mPos != fileOffset) {
            mRAF.seek(fileOffset);
            mPos = fileOffset;
            seeked = true;
        }
        numRead = mRAF.read(b, off, len);
        mPos += numRead;
        
        if (seeked) {
            ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_SEEK_RATE.increment(100);
        } else {
            ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_SEEK_RATE.increment(0);
        }
        ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_READ.increment();
        return numRead;
    }
    
    synchronized void aboutToRead() {
        mNumReaders++;
    }
    
    synchronized void doneReading() {
        if (mNumReaders > 0) {
            mNumReaders--;
        }
    }
    
    synchronized int getNumReaders() {
        return mNumReaders;
    }
    
    private synchronized void openIfNecessary()
    throws IOException {
        if (mRAF == null) {
            if (!mFile.exists()) {
                throw new IOException(mFile.getPath() + " does not exist.");
            }
            mRAF = new RandomAccessFile(mFile, "r");
            mPos = 0;
        }
    }
    
    synchronized void close()
    throws IOException {
        if (mRAF != null) {
            mRAF.close();
            mPos = 0;
            mRAF = null;
        }
    }
    
    public String toString() {
        return mFile.toString();
    }
}
