/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.store;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.internet.SharedInputStream;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

public class BlobInputStream extends InputStream
implements SharedInputStream {
    
    private static final Log sLog = LogFactory.getLog(BlobInputStream.class);

    /**
     * The file that stores the content of this stream.  Only the parent
     * stream stores the file.  All child objects get the path from the top-level
     * parent.
     */
    private File mFile;

    /**
     * Size before any compression
     */
    private long mRawSize;

    // All indexes are relative to the file, not relative to mStart/mEnd.
    
    /**
     * Position of the last call to {@link #mark}.
     */
    private long mMarkPos = Long.MIN_VALUE;
    
    /**
     * Position of the next byte to read.
     */
    private long mPos;
    
    /**
     * Maximum bytes that can be read before reset() stops working. 
     */
    private int mMarkReadLimit;
    
    /**
     * Start index of this stream (inclusive).
     */
    private long mStart;
    
    /**
     * End index of this stream (exclusive).
     */
    private long mEnd;

    private BlobInputStream mRoot;

    private static int BUFFER_SIZE = Math.max(LC.zimbra_blob_input_stream_buffer_size_kb.intValue(), 1) * 1024;
    
    /**
     * Read buffer.
     */
    private byte[] mBuf = new byte[BUFFER_SIZE];
    
    /**
     * Buffer start position, relative to the file on disk, not {@link #mPos}.
     */
    private long mBufPos = 0;
    
    /**
     * Read buffer size (may be less than <tt>mBuf.length</tt>).  A value of
     * 0 means that the buffer is not initialized.
     */
    private int mBufSize = 0;
    
    /**
     * Constructs a <tt>BlobInputStream</tt> that reads an entire blob.
     */
    public BlobInputStream(Blob blob)
    throws IOException {
        this(blob.getFile(), blob.getRawSize(), null, null, null);
    }

    /**
     * Constructs a <tt>BlobInputStream</tt> that reads an entire file.
     * @param file
     * @param rawSize size of file, before any compression
     */
    public BlobInputStream(File file, long rawSize)
    throws IOException {
        this(file, rawSize, null, null, null);
    }

    /**
     * Constructs a <tt>BlobInputStream</tt> that reads a section of a file.
     * @param file the file
     * @param rawSize size of file, before any compression
     * @param start starting index, or <tt>null</tt> for beginning of file
     * @param end ending index (exclusive), or <tt>null</tt> for end of file
     */
    public BlobInputStream(File file, long rawSize, Long start, Long end)
    throws IOException {
        this(file, rawSize, start, end, null);
    }
    
    /**
     * Constructs a <tt>BlobInputStream</tt> that reads a section of a file.
     * @param file the file.  Only used if <tt>parent</tt> is <tt>null</tt>.
     * @param rawSize size of file, before any compression
     * @param start starting index, or <tt>null</tt> for beginning of file
     * @param end ending index (exclusive), or <tt>null</tt> for end of file
     * @param parent the parent stream, or <tt>null</tt> if this is the first stream.
     * If non-null, the file from the parent is used.
     */
    private BlobInputStream(File file, long rawSize, Long start, Long end, BlobInputStream parent)
    throws IOException {
        if (parent == null) {
            // Top-level stream.
            mFile = file;
            mRoot = this;
        } else {
            // New stream.  Get settings from the parent and add this stream to the group.
            mRoot = parent.mRoot;
            file = mRoot.mFile;
        }
        mRawSize = rawSize;
        
        if (!file.exists()) {
            throw new IOException(file.getPath() + " does not exist.");
        }
        if (start != null && end != null && start > end) {
            String msg = String.format("Start index %d for file %s is larger than end index %d", start, file.getPath(), end);
            throw new IOException(msg);
        }
        if (start == null) {
            mStart = 0;
            mPos = 0;
        } else {
            mStart = start;
            mPos = start;
        }
        if (end == null) {
            mEnd = mRawSize;
        } else {
            if (end > mRawSize) {
                String msg = String.format("End index %d for %s exceeded file size %d", end, file.getPath(), mRawSize);
                throw new IOException(msg);
            }
            mEnd = end;
        }

        sLog.debug("Created %s: file=%s, length=%d, uncompressed length=%d, start=%d, end=%d, parent=%s, mStart=%d, mEnd=%d.",
            this, file.getPath(), file.length(), mRawSize, start, end, parent, mStart, mEnd);
    }

    private static FileDescriptorCache mFileDescriptorCache;

    public static void setFileDescriptorCache(FileDescriptorCache fdcache) {
        mFileDescriptorCache = fdcache;
    }

    public static FileDescriptorCache getFileDescriptorCache() {
        return mFileDescriptorCache;
    }
    
    /**
     * Closes the file descriptor referenced by this stream.
     */
    public void closeFile() {
        getFileDescriptorCache().remove(mRoot.mFile.getPath());
    }

    /**
     * Updates this stream group with a new file location.
     */
    public void fileMoved(File newFile) {
        closeFile();
        mRoot.mFile = newFile;
    }
    
    ////////////// InputStream methods //////////////
    
    @Override
    public int available() {
        return (int) (mEnd - mPos);
    }

    @Override
    public void close() {
        mPos = mEnd;
    }

    @Override
    public synchronized void mark(int readlimit) {
        mMarkPos = mPos;
        mMarkReadLimit = readlimit;
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        if (mPos >= mEnd) {
            return -1;
        }
        if (mPos < mBufPos || mPos >= (mBufPos + mBufSize)) {
            // Tried to read outside buffer bounds.
            int numRead = fillBuffer(mPos);
            if (numRead <= 0) {
                return -1;
            }
        }
        byte b = mBuf[(int) (mPos - mBufPos)];
        mPos++;
        return b;
    }
    
    /**
     * Fills the buffer at the given position in the file.
     * @return number of bytes read
     */
    private int fillBuffer(long pos) throws IOException {
        int numToRead = (int) Math.min(mBuf.length, mEnd - pos);
        int numRead = getFileDescriptorCache().read(mRoot.mFile.getPath(), mRawSize, pos, mBuf, 0, numToRead);
        if (numRead > 0) {
            mBufPos = pos;
            mBufSize = numRead;
        } else {
            mBufSize = 0;
        }
        return numRead;
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (mPos >= mEnd) {
            return -1;
        }
        if (len <= 0) {
            return 0;
        }
        
        // Make sure we don't read past the endpoint passed to the constructor.
        len = (int) Math.min(len, mEnd - mPos);
        int numRead = 0;
        
        if (mPos >= mBufPos && mPos < (mBufPos + mBufSize)) {
            // Current position is inside the buffer.  Read from the buffer.
            numRead = (int) Math.min(len, mBufPos + mBufSize - mPos); // Don't read past the end of the buffer.
            int bufStartIndex = (int) (mPos - mBufPos);
            System.arraycopy(mBuf, bufStartIndex, b, off, numRead);
        } else {
            if (len > mBuf.length) {
                // Read directly from the file.
                numRead = getFileDescriptorCache().read(mRoot.mFile.getPath(), mRawSize, mPos, b, off, len);
            } else {
                // Fill the buffer and copy data.
                int numReadIntoBuffer = fillBuffer(mPos);
                if (numReadIntoBuffer <= 0) {
                    return -1;
                }
                System.arraycopy(mBuf, 0, b, off, len);
                numRead = len;
            }
        }
        if (numRead > 0) {
            mPos += numRead;
        } else {
            close();
        }
        return numRead;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (mMarkPos == Long.MIN_VALUE) {
            throw new IOException("reset() called before mark()");
        }
        if (mPos - mMarkPos > mMarkReadLimit) {
            throw new IOException("Mark position was invalidated because more than " + mMarkReadLimit + " bytes were read.");
        }
        mPos = mMarkPos;
    }

    @Override
    public long skip(long n) {
        if (n <= 0) {
            return 0;
        }
        long newPos = Math.min(mPos + n, mEnd);
        long numSkipped = newPos - mPos;
        mPos = newPos;
        return numSkipped;
    }

    @Override
    /**
     * Ensures that the file descriptor gets closed when this object is garbage
     * collected.  We generally don't like finalizers, but we make an exception
     * in this case because we have no control over how JavaMail uses BlobInputStream. 
     */
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    ////////////// SharedInputStream methods //////////////

    public long getPosition() {
        // If this is a substream, return the position relative to the
        // starting point.  If this is the main stream, mStart = 0.
        return mPos - mStart;
    }

    public InputStream newStream(long start, long end) {
        if (start < 0) {
            throw new IllegalArgumentException("start cannot be less than 0");
        }
        // The start and end markers are relative to this
        // stream's view of the file, not necessarily the entire file.
        // Calculate the actual start/end offsets in the file.
        start += mStart;
        if (end < 0) {
            end = mEnd;
        } else {
            end += mStart;
        }
        
        BlobInputStream newStream = null;
        try {
            newStream = new BlobInputStream(null, mRawSize, start, end, this);
        } catch (IOException e) {
            sLog.warn("Unable to create substream for %s", mRoot.mFile.getPath(), e);
        }
        
        return newStream;
    }
}
