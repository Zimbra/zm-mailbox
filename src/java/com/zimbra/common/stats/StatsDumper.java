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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.stats;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.Callable;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.FileUtil;
import com.zimbra.common.util.TaskScheduler;

/**
 * Writes data to a file at a scheduled interval.  Data and headers are retrieved
 * from a {@link StatsDumperDataSource}.
 */
public class StatsDumper
implements Callable<Void> {
    
    private static final File STATS_DIR = new File(LC.zmstat_log_directory.value());

    private static TaskScheduler<Void> sTaskScheduler = new TaskScheduler<Void>("StatsDumper", 1, 3);
    
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
    public static void schedule(StatsDumperDataSource dataSource, long intervalMillis) {
        StatsDumper dumper = new StatsDumper(dataSource);
        sTaskScheduler.schedule(dataSource.getFilename(), dumper, true, intervalMillis, intervalMillis);
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
        if (file.exists() && (now.get(Calendar.YEAR) != mLastRollover.get(Calendar.YEAR) ||
            now.get(Calendar.DAY_OF_YEAR) != mLastRollover.get(Calendar.DAY_OF_YEAR))) {
            rollover();
        }
        
        // Determine if header needs to be written
        boolean writeHeader = false;
        if (!file.exists()) {
            writeHeader = true;
        }
        FileWriter writer = new FileWriter(file, true);
        if (writeHeader) {
            if (mDataSource.hasTimestampColumn()) {
                writer.write("timestamp,");
            }
            writer.write(mDataSource.getHeader());
            writer.write("\n");
        }
        
        // Write data and close
        writer.write(buf.toString());
        writer.close();
        return null;
    }
}
