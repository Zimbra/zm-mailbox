/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
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
/**
 * 
 */
package com.zimbra.cs.store;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.stats.ZimbraPerf;

/**
 * Synchronized container for a <tt>RandomAccessFile</tt> object.  Used by multiple
 * <tt>BlobInputStream</tt> objects that share a single file descriptor.
 */
public class SharedFile {
    private static int BUFFER_SIZE = Math.max(LC.zimbra_blob_input_stream_buffer_size_kb.intValue(), 1) * 1024;

    private File mFile;
    private RandomAccessFile mRAF;
    private long mPos = 0;
    
    /**
     * Remember the file's length, in case we have an open filehandle and the
     * uncompressed cache deletes this file from disk.
     */
    private long mLength;
    
    /** Read buffer. */
    private byte[] mBuf = new byte[BUFFER_SIZE];
    
    /**
     * Position of the start of the read buffer, relative to the file
     * on disk.
     */
    private long mBufStartPos = 0;
    
    /**
     * Read buffer size (may be less than <tt>mBuf.length</tt>).  A value of
     * 0 means that the buffer is not initialized.
     */
    private int mBufSize = 0;
    
    SharedFile(File file)
    throws IOException {
        if (file == null) {
            throw new NullPointerException();
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
    
    synchronized int read(long fileOffset)
    throws IOException {
        if (fileOffset >= mBufStartPos && 
            fileOffset < (mBufStartPos + mBufSize)) {
            // File offset is in the buffer range.
            int bufOffset = (int) (fileOffset - mBufStartPos);
            ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_SEEK_RATE.increment(0);
            return mBuf[bufOffset] & 0xff;
        } else {
            // Read from file.
            byte[] buf = new byte[1];
            int retVal = read(fileOffset, buf, 0, 1);
            if (retVal < 0) {
                return retVal;
            } else {
                return buf[0] & 0xff;
            }
        }
    }
    
    synchronized int read(long fileOffset, byte[] b, int off, int len)
    throws IOException {
        int numRead = 0;
        boolean seeked = false;
        openIfNecessary();

        if (len <= BUFFER_SIZE) {
            // Read from buffer.
            if (fileOffset >= mBufStartPos &&
                fileOffset < (mBufStartPos + mBufSize)) {
                // Read entirely from memory.  Number of bytes read will
                // be smaller than len if we hit the end of the buffer.
                int bufOffset = (int) (fileOffset - mBufStartPos);
                numRead = Math.min(len, mBufSize - bufOffset);
                System.arraycopy(mBuf, bufOffset, b, off, numRead);
            } else {
                // Update buffer contents from disk.
                if (mPos != fileOffset) {
                    mRAF.seek(fileOffset);
                    mPos = fileOffset;
                    seeked = true;
                }
                numRead = mRAF.read(mBuf);
                if (numRead <= 0) {
                    mBufSize = 0; 
                } else {
                    mBufSize = numRead;
                    mBufStartPos = mPos;
                    mPos += mBufSize;
                    numRead = Math.min(len, mBufSize);
                    System.arraycopy(mBuf, 0, b, off, numRead);
                }
            }
        } else {
            // Read from file.
            if (mPos != fileOffset) {
                mRAF.seek(fileOffset);
                mPos = fileOffset;
                seeked = true;
            }
            numRead = mRAF.read(b, off, len);
            mPos += numRead;
        }
        
        if (seeked) {
            ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_SEEK_RATE.increment(100);
        } else {
            ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_SEEK_RATE.increment(0);
        }
        ZimbraPerf.COUNTER_BLOB_INPUT_STREAM_READ.increment();
        return numRead;
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
    
    /**
     * Sets the the size of the read buffer used by <tt>BlobInputStream</tt>.
     * To be used for unit testing only.
     */
    public static void setBufferSize(int bufferSize) {
        if (bufferSize < 1) {
            throw new IllegalArgumentException("Buffer size " + bufferSize + " must be at least 1");
        }
        BUFFER_SIZE = bufferSize;
    }
}
