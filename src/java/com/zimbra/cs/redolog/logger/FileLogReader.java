/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.redolog.op.RedoableOp;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class FileLogReader {

    private static Log mLog = LogFactory.getLog(FileLogReader.class);

    private FileHeader mHeader;
    private boolean mHeaderRead;

    private File mFile;
	private RandomAccessFile mRAF;
	private int mLogCount;
	private boolean mReadOnly;
    private long mFileSizeAtOpen;


	public FileLogReader(File logfile) {
		this(logfile, true);
	}

	public FileLogReader(File logfile, boolean writable) {
        mHeader = new FileHeader();
        mHeaderRead = false;
		mFile = logfile;
		mLogCount = 0;
		mReadOnly = !writable;
	}

	public synchronized void open() throws IOException {
		mRAF = new RandomAccessFile(mFile, mReadOnly ? "r" : "rw");
        mHeader.read(mRAF);
        mHeaderRead = true;
        mFileSizeAtOpen = mRAF.length();
	}

	public synchronized void close() throws IOException {
		mRAF.close();

		// Write some stats, so we can see how many times we were able
		// to avoid calling fsync.
		if (mLog.isDebugEnabled())
			mLog.debug("Read " + mLogCount + " log entries from logfile " + mFile.getAbsolutePath());
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

        RedoableOp op = RedoableOp.deserializeOp(mRAF);
		mLogCount++;
		return op;
	}

	public synchronized long getSize() throws IOException {
		return mRAF.length();
	}

	public synchronized long position() throws IOException {
		return mRAF.getFilePointer();
	}

    /**
     * To call this method the FileLogReader must have been created as
     * writable.
     * @param size
     * @throws IOException
     */
	public synchronized void truncate(long size) throws IOException {
		if (size <= mRAF.getFilePointer()) {
			mRAF.setLength(size);
		}
	}
}
