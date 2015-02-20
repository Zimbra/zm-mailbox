/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.redolog.util;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

import com.zimbra.cs.redolog.FilesystemRedoLogFile;

public class RedoLogFileUtil {

    /**
     * Returns the archive log files in the specified directory, sorted
     * by the sequence number encoded in the filename.
     * @param archiveDir
     * @return
     */
    public static FilesystemRedoLogFile[] getArchiveLogs(File archiveDir) {
        return getArchiveLogs(archiveDir, Long.MIN_VALUE, Long.MAX_VALUE);
    }
    /**
     * Returns the archive log files in the specified directory, sorted
     * by the sequence number encoded in the filename, starting from the specified sequence number
     * @param archiveDir
     * @param from
     * @return
     */
    public static FilesystemRedoLogFile[] getArchiveLogs(File archiveDir, long from) {
        return getArchiveLogs(archiveDir, from, Long.MAX_VALUE);
    }

    /**
     * Returns the archive log files in the specified directory, sorted
     * by the sequence number encoded in the filename, between the from and to sequence numbers
     * @param archiveDir
     * @param from
     * @param to
     * @return
     */
    public static FilesystemRedoLogFile[] getArchiveLogs(File archiveDir, final long from, final long to) {
        File[] logs = getArchiveLogFiles(archiveDir, from, to);
        if (logs != null && logs.length > 0) {
            sortArchiveLogFiles(logs);
            FilesystemRedoLogFile[] files = new FilesystemRedoLogFile[logs.length];
            int i = 0;
            for (File log : logs) {
                files[i++] = new FilesystemRedoLogFile(log);
            }
            return files;
        } else {
            return new FilesystemRedoLogFile[0];
        }
    }


    public static File[] getArchiveLogFiles(File archiveDir, final long from, final long to) {
        //TODO: refactor this to private visibility
        File logs[] = archiveDir.listFiles(new FilenameFilter() {
            @Override
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
        if (logs != null && logs.length > 0) {
            sortArchiveLogFiles(logs);
        }
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
        @Override
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

    public static class TempLogFilenameFilter implements FilenameFilter {
        @Override
        public boolean accept(File dir, String name) {
            if (name.indexOf(TEMP_FILENAME_PREFIX) == 0 &&
                name.lastIndexOf(FILENAME_SUFFIX) == name.length() - FILENAME_SUFFIX.length())
                return true;
            else
                return false;
        }
    }

    public static String getTempFilename(long seq) {
        StringBuilder fname = new StringBuilder(TEMP_FILENAME_PREFIX);
        DateFormat fmt = new SimpleDateFormat(TIMESTAMP_FORMAT);
        fmt.setTimeZone(TimeZone.getTimeZone("GMT"));
        fname.append(fmt.format(new Date()));
        fname.append(SEQUENCE_PREFIX).append(seq);
        fname.append(FILENAME_SUFFIX);
        // "~tmp-redo-<yyyyMMdd.HHmmss>-s<seq>.log"
        return fname.toString();
    }

}
