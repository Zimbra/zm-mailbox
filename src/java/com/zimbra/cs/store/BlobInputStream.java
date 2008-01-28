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
import java.util.HashSet;
import java.util.Set;

import javax.mail.internet.SharedInputStream;

import com.zimbra.common.util.ZimbraLog;


public class BlobInputStream extends InputStream
implements SharedInputStream {
    
	private static final int BUFFER_SIZE = 1024;
	
    private File mFile;
    private RandomAccessFile mRAF;
    private Long mMarkPos;
    private long mPos;
    private int mMarkReadLimit;
    private long mStart;
    private long mEnd;
    private Set<BlobInputStream> mSubStreams;
    
    private byte[] mBuf = new byte[BUFFER_SIZE];
    private int mBufPos = 0;
    private int mBufSize = 0;
    
    /**
     * Constructs a <tt>BlobInputStream</tt> that reads an entire file.
     */
    public BlobInputStream(File file)
    throws IOException {
        this(file, null, null);
    }

    /**
     * Constructs a <tt>BlobInputStream</tt> that reads a section of a file.
     * @param file the file
     * @param start starting index
     * @param end ending index (exclusive)
     */
    public BlobInputStream(File file, Long start, Long end)
    throws IOException {
        if (start != null && end != null && start > end) {
            String msg = String.format("Start index %d is larger than end index %d", start, end);
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
                String msg = String.format("End index %d exceeded file size %d", end, mFile.length());
                throw new IOException(msg);
            }
            mEnd = end;
        }
    }
    
    /**
     * Initializes the file descriptor if necessary.
     */
    private void openFile()
    throws IOException {
        if (mRAF != null) {
            return;
        }
        mRAF = new RandomAccessFile(mFile, "r");
        mRAF.seek(mPos);
        resetBuffer();
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
            resetBuffer();
        }
    }
    
    private void resetBuffer() {
    	mBufPos = 0;
    	mBufSize = 0;
    }
    
    /**
     * Closes the file descriptors used by this stream and its substreams.
     * If someone continues to read from this stream, the file descriptor
     * is automatically reopened. 
     */
    public void closeFile()
    throws IOException {
        closeMyFile();
        if (mSubStreams != null) {
            for (BlobInputStream subStream : mSubStreams) {
                subStream.closeFile();
            }
        }
    }
    
    /**
     * Updates this stream and all substreams with a new file location.
     */
    public void fileMoved(File newFile)
    throws IOException {
        closeMyFile();
        mFile = newFile;
        if (mSubStreams != null) {
            for (BlobInputStream subStream : mSubStreams) {
                subStream.fileMoved(newFile);
            }
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
        openFile();
        
        if (mBufPos >= mBufSize) {
        	// Read next chunk into buffer
        	int numRead = mRAF.read(mBuf);
        	if (numRead < 0) {
        		closeMyFile();
        		resetBuffer();
        		return numRead;
        	}
        	mBufPos = 0;
        	mBufSize = numRead;
        }

        mPos++;
        return mBuf[mBufPos++];
    }
    
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (mPos >= mEnd) {
            closeMyFile();
            return -1;
        }
        openFile();
        
        // Make sure we don't read past the endpoint passed to the constructor
        len = (int) Math.min(len, mEnd - mPos);

        // Copy from buffer first
        int numReadFromBuffer = Math.min(mBufSize - mBufPos, len);
        if (numReadFromBuffer > 0) {
        	System.arraycopy(mBuf, mBufPos, b, off, numReadFromBuffer);
        	mBufPos += len;
        	mPos += len;
        }

        int numReadFromFile = 0;
        if (numReadFromBuffer < len) {
        	// Read additional chunk
        	numReadFromFile = mRAF.read(b, off + numReadFromBuffer, len - numReadFromBuffer);
        	if (numReadFromFile >= 0) {
        		mPos += numReadFromFile;
        	} else {
        		closeMyFile();
        	}
        }

        if (numReadFromFile >= 0) {
        	// Read from file, possibly from buffer too 
        	return numReadFromBuffer + numReadFromFile;
        } else {
        	if (numReadFromBuffer > 0) {
        		// Read from buffer, hit EOF in file
        		return numReadFromBuffer;
        	} else {
        		// Didn't read from buffer, hit EOF in file
        		return numReadFromFile;
        	}
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        if (mMarkPos == null) {
            throw new IOException("reset() called before mark()");
        }
        if (mPos - mMarkPos > mMarkReadLimit) {
            throw new IOException("Mark position was invalidated because more than " + mMarkReadLimit + " bytes were read.");
        }
        openFile();
        mRAF.seek(mMarkPos);
        mPos = mMarkPos;
        resetBuffer();
    }

    @Override
    public long skip(long n) throws IOException {
        if (n <= 0) {
            return 0;
        }
        if (mPos >= mEnd) {
            closeMyFile();
            return 0;
        }
        openFile();
        
        long newPos = Math.min(mPos + n, mEnd);
        long numSkipped = 0;
        if (newPos != mPos) {
        	mRAF.seek(newPos);
        	numSkipped = newPos - mPos;
        	mPos = newPos;
        	resetBuffer();
        } else {
            closeMyFile();
        }
        return numSkipped;
    }
    
    @Override
    protected void finalize() throws Throwable {
        closeMyFile();
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
            newStream = new BlobInputStream(mFile, start, end);
        } catch (IOException e) {
            ZimbraLog.misc.warn("Unable to create substream for %s", mFile.getPath(), e);
        }

        if (mSubStreams == null) {
            mSubStreams = new HashSet<BlobInputStream>();
        }
        mSubStreams.add(newStream);
        return newStream;
    }
}
