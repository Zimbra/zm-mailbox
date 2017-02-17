/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.stats;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Callable;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * Writes data to a file at a scheduled interval.  Data and headers are retrieved
 * from a {@link StatsDumperDataSource}.
 */
public class StatsDumper
implements Callable<Void> {
    
    public final static int SYSLOG_ELIDING_THRESHOLD = 800;
    private static final File STATS_DIR = new File("/opt/zimbra/zmstat");
    private static final ThreadGroup statsGroup = new ThreadGroup("ZimbraPerf Stats");

    private StatsDumperDataSource mDataSource;
    private Calendar mLastRollover = Calendar.getInstance();
    private StatsDumper(StatsDumperDataSource dataSource) {
        mDataSource = dataSource;
    }
    
    private File getFile()
    throws IOException {
        FileUtil.ensureDirExists(STATS_DIR);
        return new File(STATS_DIR, mDataSource.getFilename());
    }

    /**
     * Return the archive directory, based on {@link #mLastRollover}.
     */
    private File getArchiveDir()
    throws IOException {
        String dirName = String.format("%1$tY-%1$tm-%1$td", mLastRollover);
        File dir = new File(STATS_DIR, dirName);
        FileUtil.ensureDirExists(dir);
        return dir;
    }

    /**
     * Schedules a new stats task.
     * 
     * @param dataSource the data source
     * @param intervalMillis interval between writes.  The first write is delayed by
     * this interval.
     */
    public static void schedule(final StatsDumperDataSource dataSource, final long intervalMillis) {
        // Stop using TaskScheduler (bug 22978)
        final StatsDumper dumper = new StatsDumper(dataSource);
        Runnable r = new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(intervalMillis);
                        try {
                            dumper.call();
                        }
                        catch (Exception e) {
                            ZimbraLog.perf.warn("Exception in stats thread: %s", dataSource.getFilename(), e);
                        }
                    }
                    catch (InterruptedException e) {
                        ZimbraLog.perf.info("Stats thread interrupted: %s", dataSource.getFilename(), e);
                    }
                    if (Thread.currentThread().isInterrupted())
                        ZimbraLog.perf.info("Stats thread was interrupted: %s", dataSource.getFilename());
                }
            }
        };
        new Thread(statsGroup, r, dataSource.getFilename()).start();
    }
    
    private void rollover()
    throws IOException {
        File sourceFile = getFile();
        File archiveDir = getArchiveDir();
        File archiveFile = new File(archiveDir, mDataSource.getFilename() + ".gz");
        FileUtil.compress(sourceFile, archiveFile, true);
        sourceFile.delete();
    }

    /**
     * Gets the latest data from the data source and writes it to the file.
     * If this is a new file, writes the header first.
     */
    public Void call() throws Exception {
        Collection<String> lines = mDataSource.getDataLines();
        if (lines == null || lines.size() == 0) {
            return null;
        }
        
        // Assemble data lines
        StringBuilder buf = new StringBuilder();
        String timestamp = StatUtil.getTimestampString();
        for (String line : lines) {
            if (mDataSource.hasTimestampColumn()) {
                buf.append(timestamp).append(",");
            }
            buf.append(line).append("\n");
        }

        // Rollover is necessary
        File file = getFile();
        Calendar now = Calendar.getInstance();
        if (file.exists() && now.get(Calendar.DAY_OF_YEAR) != mLastRollover.get(Calendar.DAY_OF_YEAR)) {
            rollover();
            mLastRollover = now;
        }
        
        // Determine if header needs to be written
        boolean writeHeader = false;
        if (!file.exists()) {
            writeHeader = true;
        }
        FileWriter writer = new FileWriter(file, true);
        String header = mDataSource.getHeader();
        if (writeHeader) {
            if (mDataSource.hasTimestampColumn()) {
                writer.write("timestamp,");
            }
            writer.write(header);
            writer.write("\n");
        }
        
        // Write data and close
        writer.write(buf.toString());
        writer.close();
        for (String line : lines) {
            String logLine = mDataSource.getFilename() + ": " +
                    (mDataSource.hasTimestampColumn() ? "timestamp," : "") +
                    header + ":: " + timestamp + "," + line;
            if (logLine.length() <= SYSLOG_ELIDING_THRESHOLD) {
                ZimbraLog.slogger.info(logLine);
            } else {
                StringBuilder b = new StringBuilder(logLine);
                String lastUuid = null;
                do {
                    String sub = b.substring(0, SYSLOG_ELIDING_THRESHOLD);
                    b.delete(0, SYSLOG_ELIDING_THRESHOLD);
                    if (lastUuid != null) {
                        sub = ":::" + lastUuid + ":::" + sub;
                    }
                    lastUuid = UUID.randomUUID().toString();
                    ZimbraLog.slogger.info(sub + ":::" + lastUuid + ":::");
                } while (b.length() > SYSLOG_ELIDING_THRESHOLD);
                ZimbraLog.slogger.info(":::" + lastUuid + ":::" + b.toString());
            }
        }
        return null;
    }
}
