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