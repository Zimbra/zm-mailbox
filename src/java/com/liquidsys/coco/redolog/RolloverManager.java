/*
 * Created on 2004. 8. 5.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.liquidsys.coco.redolog;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.liquidsys.coco.db.DbPool;
import com.liquidsys.coco.db.DbRedologSequence;
import com.liquidsys.coco.db.DbPool.Connection;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.FileUtil;

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
	 * by the timestamp encoded into the filenames.
	 * @param archiveDir
	 * @return
	 */
	public static File[] getArchiveLogs(File archiveDir) {
        File logs[] = archiveDir.listFiles(new ArchiveLogFilenameFilter());
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
	private static class ArchiveLogFilenameComparator implements Comparator {
		public int compare(Object o1, Object o2) {
			File f1 = (File) o1;
			File f2 = (File) o2;

			long t1 = getEndTimeForFile(f1);
			long t2 = getEndTimeForFile(f2);

			long diff = t1 - t2;
			if (diff < 0)
				return -1;
			else if (diff == 0)
				return 0;
			else
				return 1;
		}
	}

    public static long getEndTimeForFile(File f) {
        DateFormat fmt = new SimpleDateFormat(TIMESTAMP_FORMAT);
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        fmt.setLenient(false);
        int prefixLen = ARCH_FILENAME_PREFIX.length();
        try {
            Date d = fmt.parse(f.getName().substring(prefixLen));
            return d.getTime();
        } catch(ParseException e) {
            return f.lastModified();
        }
    }

    public static String toArchiveLogFilename(long date) {
    	return toArchiveLogFilename(new Date(date));
    }

    public static String toArchiveLogFilename(Date date) {
        DateFormat fmt = new SimpleDateFormat(TIMESTAMP_FORMAT);
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        String fname = ARCH_FILENAME_PREFIX + fmt.format(date) + ".log";
        return fname;
    }

	private static final String ARCH_FILENAME_PREFIX = "arch.";
	private static final String TEMP_FILENAME_PREFIX = "~tmp-redo.";
	private static final String TIMESTAMP_FORMAT = "yyyyMMdd.HHmmss.SSS";

	public static class ArchiveLogFilenameFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			if (name.indexOf(ARCH_FILENAME_PREFIX) == 0 &&
				name.lastIndexOf(".log") == name.length() - 4)
				return true;
			else
				return false;
		}
	}

	private static class TempLogFilenameFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			if (name.indexOf(TEMP_FILENAME_PREFIX) == 0 &&
				name.lastIndexOf(".log") == name.length() - 4)
				return true;
			else
				return false;
		}
	}

	public File getRolloverFile() {
        String fname = toArchiveLogFilename(new Date());
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

	public String getTempFilename() {
		DateFormat fmt = new SimpleDateFormat(TIMESTAMP_FORMAT);
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
		return TEMP_FILENAME_PREFIX + fmt.format(new Date()) + ".log";
	}

    public synchronized long getCurrentSequence() {
    	return mSequence;
    }

    // KC: 1711: to be removed
    public synchronized void initSequence(long seq) {
        mSequence = seq;
    }
    
    // KC: 1711: to be removed 
    public synchronized long incrementSequence() {
        if (mSequence < Long.MAX_VALUE)
            ++mSequence;
        else
            mSequence = 0;
        return mSequence;
    }

    public synchronized void initSequence() throws ServiceException {
        Connection conn = null;
        try {
            conn = DbPool.getConnection();
            mSequence = DbRedologSequence.getSequence(conn);
        } finally {
            if (conn != null)
                DbPool.quietClose(conn);
        }
        
    }

//    public synchronized long incrementSequence() throws ServiceException {
//        Connection conn = null;
//        try {
//            conn = DbPool.getConnection();
//            mSequence = DbRedologSequence.incrementSequence(conn);
//            return mSequence;
//        } finally {
//            if (conn != null)
//                DbPool.quietClose(conn);
//        }
//    }

    public void resetSequence(long seq) throws ServiceException {
        Connection conn = null;
        try {
            conn = DbPool.getConnection();
            mSequence = DbRedologSequence.setSequence(conn, seq);
        } finally {
            if (conn != null)
                DbPool.quietClose(conn);
        }
    }
}
