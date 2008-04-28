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
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.store;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Set;

import javax.mail.internet.SharedInputStream;

import org.jivesoftware.util.ConcurrentHashSet;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;


public class BlobInputStream extends InputStream
implements SharedInputStream {
    
    private static int BUFFER_SIZE = Math.max(LC.zimbra_blob_input_stream_buffer_size_kb.intValue(), 1) * 1024;

    private File mFile;
    
    private RandomAccessFile mRAF;
    
    // All indexes are relative to the file, not relative to mStart/mEnd.
    
    /**
     * Position of the last call to {@link #mark}.
     */
    private Long mMarkPos;
    
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
    
    /**
     * Contains this stream and all related streams created with {@link #newStream}.
     * This set is shared between the original stream and all substreams.
     */
    private Set<BlobInputStream> mSubStreams;

    /**
     * Read buffer.
     */
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
    
    /**
     * Constructs a <tt>BlobInputStream</tt> that reads an entire file.
     */
    public BlobInputStream(File file)
    throws IOException {
        this(file, null, null, null);
    }

    /**
     * Constructs a <tt>BlobInputStream</tt> that reads a section of a file.
     * @param file the file
     * @param start starting index, or <tt>null</tt> for beginning of file
     * @param end ending index (exclusive), or <tt>null</tt> for end of file
     */
    public BlobInputStream(File file, Long start, Long end)
    throws IOException {
        this(file, start, end, null);
    }
    
    /**
     * Constructs a <tt>BlobInputStream</tt> that reads a section of a file.
     * @param file the file
     * @param start starting index, or <tt>null</tt> for beginning of file
     * @param end ending index (exclusive), or <tt>null</tt> for end of file
     * @param subStreams the <tt>Set</tt> of substreams, or <tt>null</tt>
     * if this is the original stream
     */
    private BlobInputStream(File file, Long start, Long end, Set<BlobInputStream> subStreams)
    throws IOException {
        if (start != null && end != null && start > end) {
            String msg = String.format("Start index %d for file %s is larger than end index %d", start, file.getPath(), end);
            throw new IOException(msg);
        }
        mFile = file;
        if (start == null) {
            mStart = 0;
            mPos = 0;
        } else {
            mStart = start;
            mPos = start;
        }
        if (end == null) {
            mEnd = mFile.length();
        } else {
            if (end > mFile.length()) {
                String msg = String.format("End index %d for file %s exceeded file size %d", end, mFile.getPath(), mFile.length());
                throw new IOException(msg);
            }
            mEnd = end;
        }
        mSubStreams = subStreams;
        if (mSubStreams == null) {
            mSubStreams = new ConcurrentHashSet<BlobInputStream>();
        }
        mSubStreams.add(this);
    }
    
    /**
     * Opens the file if necessary and seeks to the current position.
     */
    private void initFile()
    throws IOException {
        if (mRAF == null) {
            mRAF = new RandomAccessFile(mFile, "r");
            if (mPos != 0) {
                mRAF.seek(mPos);
            }
        } else {
            mRAF.seek(mPos);
        }
    }

    /**
     * Closes the file descriptor for this stream only.  A subsequent call
     * to <tt>read()</tt>, <tt>skip</tt>, etc. may reopen it.
     */
    private void closeMyFile()
    throws IOException {
        if (mRAF != null) {
            mRAF.close();
            mRAF = null;
        }
    }
    
    /**
     * Closes the file descriptors used by this stream and its substreams.
     * If someone continues to read from this stream, the file descriptor
     * is automatically reopened. 
     */
    public void closeFile()
    throws IOException {
        for (BlobInputStream subStream : mSubStreams) {
            subStream.closeMyFile();
        }
    }
    
    /**
     * Updates this stream and all substreams with a new file location.
     */
    public void fileMoved(File newFile)
    throws IOException {
        for (BlobInputStream subStream : mSubStreams) {
            subStream.closeMyFile();
            subStream.mFile = newFile;
        }
    }
    
    ////////////// InputStream methods //////////////
    
    @Override
    public int available() {
        return (int) (mEnd - mPos);
    }

    @Override
    public void close() throws IOException {
        closeMyFile();
        mSubStreams.remove(this);
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
            closeMyFile();
            return -1;
        }
        
        // Return data from the read buffer if we're in the range.
        int bufIndex = getBufferIndex();
        if (bufIndex >= 0) {
            byte b = mBuf[bufIndex];
            mPos++;
            return b;
        }
        
        // Read next byte from file.
        int numRead = readNextChunkIntoBuffer();
        if (numRead < 0) {
            return numRead;
        }
        
        mPos++;
        return mBuf[0];
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (mPos >= mEnd) {
            closeMyFile();
            return -1;
        }
        if (len <= 0) {
            return 0;
        }
        
        // Make sure we don't read past the endpoint passed to the constructor
        len = (int) Math.min(len, mEnd - mPos);

        // Copy from buffer first
        int numReadFromBuffer = 0;
        int bufIndex = getBufferIndex();
        if (bufIndex >= 0) {
            numReadFromBuffer = Math.min(mBufSize - bufIndex, len);
            if (numReadFromBuffer > 0) {
                System.arraycopy(mBuf, bufIndex, b, off, numReadFromBuffer);
                mPos += numReadFromBuffer;
            }
            if (numReadFromBuffer == len) {
                // Got all data from buffer
                return numReadFromBuffer;
            }
        }

        // Need to read the rest from the file
        int numToReadFromFile = len - numReadFromBuffer;
        int numReadFromFile = 0;
        if (numToReadFromFile <= mBuf.length) {
            // Buffer if we're reading a small number of bytes. 
            numReadFromFile = readNextChunkIntoBuffer();
            if (numReadFromFile > 0) {
                numReadFromFile = Math.min(numReadFromFile, numToReadFromFile);
                System.arraycopy(mBuf, 0, b, off + numReadFromBuffer, numReadFromFile);
                mPos += numReadFromFile;
            }
        } else {
            // Number of bytes requested is greater than the buffer size, so
            // read the next chunk without buffering.
            initFile();
        	numReadFromFile = mRAF.read(b, off + numReadFromBuffer, numToReadFromFile);
        	if (numReadFromFile >= 0) {
        		mPos += numReadFromFile;
        	} else {
                // Hit the end of the file.
        		closeMyFile();
        	}
        }

        if (numReadFromFile >= 0) {
        	// Read from file, possibly from buffer too.
        	return numReadFromBuffer + numToReadFromFile;
        } else {
        	if (numReadFromBuffer > 0) {
        		// Read from buffer, hit EOF in file.
        		return numReadFromBuffer;
        	} else {
        		// Didn't read from buffer, hit EOF in file.
        		return numReadFromFile;
        	}
        }
    }

    /**
     * Returns the current buffer index, or <tt>-1</tt> if the
     * current position is outside the buffer.
     */
    private int getBufferIndex() {
        if (mBufSize > 0 &&                     // Buffer has been initialized
            mPos >= mBufStartPos &&             // Position is past the beginning of the buffer
            mPos < (mBufStartPos + mBufSize)) { // Position is not past the end of the buffer
            return (int) (mPos - mBufStartPos);
        } else {
            return -1;
        }
    }
    
    /**
     * Fills the read buffer with data from the file on disk, starting
     * at position {@link #mPos}.
     */
    private int readNextChunkIntoBuffer()
    throws IOException {
        initFile();
        int numRead = mRAF.read(mBuf);
        if (numRead < 0) {
            // No more data available.
            closeMyFile();
        } else {
            // Read something.  Update the indexes.
            mBufStartPos = mPos;
            mBufSize = numRead;
        }
        return numRead;
    }
    
    @Override
    public synchronized void reset() throws IOException {
        if (mMarkPos == null) {
            throw new IOException("reset() called before mark()");
        }
        if (mPos - mMarkPos > mMarkReadLimit) {
            throw new IOException("Mark position was invalidated because more than " + mMarkReadLimit + " bytes were read.");
        }
        mPos = mMarkPos;
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        long newPos = Math.min(mPos + n, mEnd);
        if (newPos == mEnd) {
            closeMyFile();
        }
        long numSkipped = newPos - mPos;
        mPos = newPos;
        return numSkipped;
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
            newStream = new BlobInputStream(mFile, start, end, mSubStreams);
        } catch (IOException e) {
            ZimbraLog.misc.warn("Unable to create substream for %s", mFile.getPath(), e);
        }
        
        return newStream;
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
