/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.stats;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.zimbra.common.localconfig.LC;

/**
 * Represents a stats file in CSV format.  Stats files are fact tables,
 * written by {@link ZimbraPerf#writeEventStats} that store statistical
 * data about individual events, such as processing time, database time,
 * and the number of SQL statements that the event generated.
 * 
 * @author bburtin
 */
public class StatsFile {

    private boolean mLogThreadLocal;
    private String mFilename;
    private String[] mStatNames = new String[0];
    private File mFile;

    private static final SimpleDateFormat TIMESTAMP_FORMATTER =
        new SimpleDateFormat("yyyyMMdd-HHmm");

    /**
     * @param filename the name of this <code>StatsFile</code>.  The actual filename
     *        stored in {@link LC#zimbra_log_directory} is <code>[name]-[timestamp].csv</code>.
     * @param statNames the names of any extra stat columns, or <code>null</code>
     *        if none
     * @param logThreadLocal <code>true</code> if statistics from {@link ThreadLocalData}
     *        should be logged
     */
    public StatsFile(String name, String[] statNames, boolean logThreadLocal) {
        mFilename = name + "-" + TIMESTAMP_FORMATTER.format(new Date()) + ".csv";
        mFile = new File(LC.zimbra_log_directory.value() + "/" + mFilename);
        if (statNames != null) {
            mStatNames = statNames;
        }
        mLogThreadLocal = logThreadLocal;
    }

    String getFilename() {
        return mFilename;
    }

    File getFile() {
        return mFile;
    }
    
    /**
     * @return the stat names, or an empty array if none are defined
     */
    String[] getStatNames() {
        return mStatNames;
    }
    
    boolean shouldLogThreadLocal() {
        return mLogThreadLocal;
    }
}
