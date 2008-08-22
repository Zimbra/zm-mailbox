/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

/*
 * Created on 2004. 8. 6.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog.logger;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.op.RedoableOp;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FileLogReader {

    private FileHeader mHeader;
    private boolean mHeaderRead;

    private File mFile;
	private RandomAccessFile mRAF;
	private RedoLogInput mIN;
	private boolean mReadOnly;
    private long mFileSizeAtOpen;
    private long mLastOpStartOffset;


	public FileLogReader(File logfile) {
		this(logfile, false);
	}

	public FileLogReader(File logfile, boolean writable) {
        mHeader = new FileHeader();
        mHeaderRead = false;
		mFile = logfile;
		mReadOnly = !writable;
	}

	public synchronized void open() throws IOException {
		mRAF = new RandomAccessFile(mFile, mReadOnly ? "r" : "rw");
		mIN = new RedoLogInput(mRAF, mFile.getPath());
        mHeader.read(mRAF);
        mHeaderRead = true;
        mFileSizeAtOpen = mRAF.length();
	}

	public synchronized void close() throws IOException {
		mRAF.close();
	}

    public synchronized FileHeader getHeader() throws IOException {
        if (mHeaderRead)
        	return mHeader;

        open();
        close();
        return mHeader;
    }

	public synchronized RedoableOp getNextOp() throws IOException {
        long pos = mRAF.getFilePointer();
        if (pos == mFileSizeAtOpen) {
            // EOF reached.
            return null;
        }

        boolean first = true;
        long currPos = pos;
        while (true) {
            try {
                RedoableOp op = RedoableOp.deserializeOp(mIN);
                mLastOpStartOffset = currPos;
                if (!first) {
                    String msg = String.format(
                            "Skipped bad bytes in redolog %s; resuming at offset 0x%08x after skipping %d bytes",
                            mFile.getAbsolutePath(), currPos, currPos - pos);
                    ZimbraLog.redolog.warn(msg);
                }
                return op;
            } catch (IOException e) {
                if (e instanceof EOFException)
                    throw e;
                if (first) {
                    String msg = String.format(
                            "Error while parsing redolog %s, offset=0x%08x; bad bytes will be skipped",
                            mFile.getAbsolutePath(), pos);
                    ZimbraLog.redolog.warn(msg, e);
                }
            }
            first = false;
            // Skip over bad bytes by looking for the next occurrence of "ZMREDO" redo op marker.
            mRAF.seek(currPos + 1);
            if (searchInRAF(RedoableOp.REDO_MAGIC.getBytes()))  {
                currPos = mRAF.getFilePointer();
            } else {
                String msg = String.format(
                        "Found %d junk bytes from offset 0x%08x to end of file, in redolog %s",
                        mFileSizeAtOpen - pos, pos, mFile.getAbsolutePath());
                throw new IOException(msg);
            }
        }
	}

	public synchronized long getSize() throws IOException {
		return mRAF.length();
	}

	public synchronized long position() throws IOException {
		return mRAF.getFilePointer();
	}

	public synchronized long getLastOpStartOffset() throws IOException {
	    return mLastOpStartOffset;
	}

    /**
     * To call this method the FileLogReader must have been created as
     * writable.
     * @param size
     * @throws IOException
     */
	public synchronized void truncate(long size) throws IOException {
		if (size < mRAF.length()) {
			mRAF.setLength(size);
			FileHeader hdr = getHeader();
			hdr.setFileSize(size);
			hdr.write(mRAF);
			mRAF.seek(size);
		}
	}

	/**
	 * Search the pattern in mRAF, positioning the pointer at the beginning of the pattern.
	 * @param pattern
	 * @return true if pattern was found, false if not found
	 */
	private boolean searchInRAF(byte[] pattern) throws IOException {
	    int viewSize = 4 * 1024;
	    if (pattern.length > viewSize)
	        return false;
	    byte view[] = new byte[viewSize * 2];  // double the size for pattern appearing on single view boundary
	    long rafPos = mRAF.getFilePointer();
	    long viewBaseOffset = rafPos - (rafPos % viewSize);
	    mRAF.seek(viewBaseOffset);
	    int startOffset = (int) (rafPos - viewBaseOffset);

	    boolean atFileEnd = false;
	    int bytesRead;
	    while (!atFileEnd && (bytesRead = mRAF.read(view, 0, view.length)) != -1) {
    	    if (bytesRead < pattern.length)
    	        break;
    	    atFileEnd = viewBaseOffset + bytesRead >= mFileSizeAtOpen;  // don't read past end of file at open time
    	    if (atFileEnd)
    	        bytesRead = (int) (mFileSizeAtOpen - viewBaseOffset);
    	    int endOffset = Math.min(view.length, bytesRead);
    	    int matchAt = searchByteArray(view, startOffset, endOffset, pattern);
    	    if (matchAt != -1) {
    	        mRAF.seek(viewBaseOffset + matchAt);
    	        return true;
    	    }
    	    // bring in the next chunk of data from file
    	    viewBaseOffset += viewSize;
            mRAF.seek(viewBaseOffset);
    	    startOffset = 0;
	    }

	    mRAF.seek(rafPos);  // move the pointer back to where we were before the failed search
	    return false;
	}

	// Returns the index in searchIn array that matches pattern array, starting from startOffset.
	// Returns -1 if no match is found.
	private static int searchByteArray(byte[] searchIn, int startOffset, int endOffset, byte[] pattern) {
	    int len = pattern.length;
	    endOffset = Math.min(endOffset, searchIn.length);
	    byte firstByte = pattern[0];
	    int i = startOffset;
	    int lastIndex = endOffset - len;
	    while (i < lastIndex) {
	        while (searchIn[i] != firstByte && i < lastIndex) {
	            i++;
	        }
	        if (i >= lastIndex)
	            break;
	        boolean matches = true;
	        for (int j = 1; j < len; j++) {
	            if (searchIn[i + j] != pattern[j]) {
	                matches = false;
	                break;
	            }
	        }
	        if (matches)
	            return i;
	        else
	            i++;
	    }
	    return -1;
    }
}
