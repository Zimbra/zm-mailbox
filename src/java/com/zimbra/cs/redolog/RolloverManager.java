/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 8. 5.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.zimbra.cs.redolog;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

import com.zimbra.common.util.FileUtil;

/**
 * @author jhahm
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RolloverManager {

    private static Log mLog = LogFactory.getLog(RolloverManager.class);

	private RedoLogManager mRedoLogMgr;
	private File mRedoLogFile;

    // Monotonically increasing sequence number for redolog files.
    // Sequence starts at 0 and increments without gap, and may
    // eventually wraparound.
    private long mSequence;

	public RolloverManager(RedoLogManager redoLogMgr, File redolog) {
		mRedoLogMgr = redoLogMgr;
		mRedoLogFile = redolog;
        mSequence = 0;
	}

	/**
	 * Recovers from a previous process crash in the middle of
	 * RolloverManager.rollover().
	 */
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
		        if (mostRecent.renameTo(mRedoLogFile))
			        mLog.info("Renamed " + name + " to " + currName);
		        else
		        	throw new IOException("Unable to rename " + name + " to " + currName);

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
	        	if (log.renameTo(newLog))
	        		mLog.info("Renamed " + oldName + " to " + newName);
	        	else {
	        		numErrors++;
	        		mLog.error("Unable to rename " + oldName + " to " + newName);
	        	}
	        }

	        if (numErrors > 0)
	        	throw new IOException("Error(s) occurred while renaming temporary redo log files");
		}
	}


	/**
	 * Returns the archive log files in the specified directory, sorted
	 * by the sequence number encoded in the filename.
	 * @param archiveDir
	 * @return
	 */
	public static File[] getArchiveLogs(File archiveDir) {
        return getArchiveLogs(archiveDir, Long.MIN_VALUE, Long.MAX_VALUE);
	}

    public static File[] getArchiveLogs(File archiveDir, long from) {
        return getArchiveLogs(archiveDir, from, Long.MAX_VALUE);
    }

    public static File[] getArchiveLogs(File archiveDir, final long from, final long to) {
        File logs[] = archiveDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.indexOf(ARCH_FILENAME_PREFIX) == 0 &&
                    name.lastIndexOf(FILENAME_SUFFIX) == name.length() - FILENAME_SUFFIX.length()) {
                    long seq = getSeqForFile(new File(dir, name));
                    if (from <= seq && seq <= to)
                        return true;
                }
                return false;
            }
        });
        if (logs != null && logs.length > 0)
            RolloverManager.sortArchiveLogFiles(logs);
        return logs;
    }

    /**
	 * Sorts an array of archive log files by the timestamp encoded into
	 * the filenames.
	 * @param files
	 */
	public static void sortArchiveLogFiles(File[] files) {
		ArchiveLogFilenameComparator comp = new ArchiveLogFilenameComparator();
		Arrays.sort(files, comp);
	}

	/*
	 * Orders archive log filenames by parsing the timestamp embedded in
	 * the filename.  Earlier-stamped file is considered "less" by the
	 * comparison.
	 */
	private static class ArchiveLogFilenameComparator
	implements Comparator<File> {
		public int compare(File f1, File f2) {
			long t1 = getSeqForFile(f1);
			long t2 = getSeqForFile(f2);
			if (t1 < t2)
				return -1;
			else if (t1 > t2)
				return 1;

			// We should never get here, but let's be safe.
			t1 = getEndTimeForFile(f1);
			t2 = getEndTimeForFile(f2);
			if (t1 < t2)
				return -1;
			else if (t1 > t2)
				return 1;
			return 0;
		}
	}

	public static long getSeqForFile(File f) {
        //FileLogReader logReader = new FileLogReader(f);
        //return logReader.getHeader().getSequence();
		String fname = f.getName();
		int start = fname.lastIndexOf(SEQUENCE_PREFIX);
		if (start == -1) return -1;
		start += SEQUENCE_PREFIX.length();
		int end = fname.indexOf(FILENAME_SUFFIX, start);
		if (end == -1) return -1;
		try {
			String val = fname.substring(start, end);
			return Long.parseLong(val);
		} catch (StringIndexOutOfBoundsException se) {
			return -1;
		} catch (NumberFormatException ne) {
			return -1;
		}
	}

	public static long getEndTimeForFile(File f) {
        DateFormat fmt = new SimpleDateFormat(TIMESTAMP_FORMAT);
        fmt.setLenient(false);
        int prefixLen = ARCH_FILENAME_PREFIX.length();
        try {
            Date d = fmt.parse(f.getName().substring(prefixLen));
            return d.getTime();
        } catch(ParseException e) {
            return f.lastModified();
        }
    }

    public static String toArchiveLogFilename(Date date, long seq) {
        StringBuilder fname = new StringBuilder(ARCH_FILENAME_PREFIX);
        DateFormat fmt = new SimpleDateFormat(TIMESTAMP_FORMAT);
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        fname.append(fmt.format(date));
        fname.append(SEQUENCE_PREFIX).append(seq);
        fname.append(FILENAME_SUFFIX);
        // "redo-<yyyyMMdd.HHmmss>-s<seq>.log"
        return fname.toString();
    }

	private static final String ARCH_FILENAME_PREFIX = "redo-";
	private static final String TEMP_FILENAME_PREFIX = "~tmp-redo-";
	private static final String SEQUENCE_PREFIX = "-seq";
	private static final String FILENAME_SUFFIX = ".log";
	private static final String TIMESTAMP_FORMAT = "yyyyMMdd.HHmmss.SSS";

	private static class TempLogFilenameFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			if (name.indexOf(TEMP_FILENAME_PREFIX) == 0 &&
				name.lastIndexOf(FILENAME_SUFFIX) == name.length() - FILENAME_SUFFIX.length())
				return true;
			else
				return false;
		}
	}

	public File getRolloverFile(long seq) {
        String fname = toArchiveLogFilename(new Date(), seq);
        File destDir = mRedoLogMgr.getRolloverDestDir();
        if (!destDir.exists()) {
            // Guard against someone messing around on server and
            // deleting the directory manually.
            if (!destDir.mkdir() && !destDir.exists()) {
                mLog.error("Unable to create rollover destination directory " + destDir.getAbsolutePath());
            }
        }
		return new File(destDir, fname);
	}

	public String getTempFilename(long seq) {
        StringBuilder fname = new StringBuilder(TEMP_FILENAME_PREFIX);
        DateFormat fmt = new SimpleDateFormat(TIMESTAMP_FORMAT);
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        fname.append(fmt.format(new Date()));
        fname.append(SEQUENCE_PREFIX).append(seq);
        fname.append(FILENAME_SUFFIX);
        // "~tmp-redo-<yyyyMMdd.HHmmss>-s<seq>.log"
        return fname.toString();
	}

    public synchronized long getCurrentSequence() {
    	return mSequence;
    }

    public synchronized void initSequence(long seq) {
        mSequence = seq;
    }
    
    public synchronized long incrementSequence() {
        if (mSequence < Long.MAX_VALUE)
            ++mSequence;
        else
            mSequence = 0;
        return mSequence;
    }

}
