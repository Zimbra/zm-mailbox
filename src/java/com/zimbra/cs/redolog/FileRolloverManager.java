/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.util.RedoLogFileUtil;
import com.zimbra.cs.redolog.util.RedoLogFileUtil.TempLogFilenameFilter;

public class FileRolloverManager implements RolloverManager {

    private static Log log4j = ZimbraLog.redolog;

	private FileRedoLogManager mRedoLogMgr;
	private File mRedoLogFile;

    // Monotonically increasing sequence number for redolog files.
    // Sequence starts at 0 and increments without gap, and may
    // eventually wraparound.
    private long mSequence;

	public FileRolloverManager(FileRedoLogManager redoLogMgr, File redolog) {
		mRedoLogMgr = redoLogMgr;
		mRedoLogFile = redolog;
        mSequence = 0;
	}

	/**
	 * Recovers from a previous process crash in the middle of
	 * RolloverManager.rollover().
	 */
	@Override
    public void crashRecovery() throws IOException {
		File logs[] = mRedoLogFile.getParentFile().listFiles(new TempLogFilenameFilter());
		if (logs.length > 0) {
			FileUtil.sortFilesByModifiedTime(logs);

	        // If "redo.log" file doesn't exist, either this is the very first time the
	        // server is running, or we must have crashed during the last rollover
	        // just before renaming temp log to "redo.log".  The temp log in question
	        // should be the most recent one.  Rename it to "redo.log" to finish what
	        // the rollover() method was doing.
	        if (!mRedoLogFile.exists()) {
		        File mostRecent = logs[logs.length - 1];
		        String name = mostRecent.getName();
		        String currName = mRedoLogFile.getName();
		        if (mostRecent.renameTo(mRedoLogFile)) {
		            log4j.info("Renamed " + name + " to " + currName);
		        } else {
		        	throw new IOException("Unable to rename " + name + " to " + currName);
		        }

		        logs[logs.length - 1] = null;	// remove most recent temp log from array
	        }

	        // Rename all remaining temp logs with ".bak" suffix so they
	        // aren't matched by TempLogFilenameFilter the next time this
	        // method is run.
	        int numErrors = 0;
	        for (int i = 0; i < logs.length && logs[i] != null; i++) {
	        	File log = logs[i];
	        	String oldName = log.getName();
	        	String newName = oldName + ".bak";
	        	File newLog = new File(log.getParentFile(), newName);
	        	if (log.renameTo(newLog)) {
	        	    log4j.info("Renamed " + oldName + " to " + newName);
	        	} else {
	        		numErrors++;
	        		log4j.error("Unable to rename " + oldName + " to " + newName);
	        	}
	        }

	        if (numErrors > 0) {
	        	throw new IOException("Error(s) occurred while renaming temporary redo log files");
	        }
		}
	}

	public File getRolloverFile(long seq) {
        String fname = RedoLogFileUtil.toArchiveLogFilename(new Date(), seq);
        File destDir = mRedoLogMgr.getRolloverDestDir();
        if (!destDir.exists()) {
            // Guard against someone messing around on server and
            // deleting the directory manually.
            if (!destDir.mkdir() && !destDir.exists()) {
                log4j.error("Unable to create rollover destination directory " + destDir.getAbsolutePath());
            }
        }
		return new File(destDir, fname);
	}

    @Override
    public synchronized long getCurrentSequence() {
    	return mSequence;
    }

    @Override
    public synchronized void initSequence(long seq) {
        mSequence = seq;
    }

    @Override
    public synchronized long incrementSequence() {
        if (mSequence < Long.MAX_VALUE) {
            ++mSequence;
        } else {
            mSequence = 0;
        }
        return mSequence;
    }

}
