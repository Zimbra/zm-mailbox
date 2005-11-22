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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.util;

import java.io.File;

import com.zimbra.cs.localconfig.LC;

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

    /**
     * @param filename the filename, stored in {@link LC#zimbra_log_directory}
     * @param statNames the names of any extra stat columns, or <code>null</code>
     *        if none
     * @param logThreadLocal <code>true</code> if statistics from {@link ThreadLocalData}
     *        should be logged
     */
    public StatsFile(String filename, String[] statNames, boolean logThreadLocal) {
        mFilename = filename;
        mFile = new File(LC.zimbra_log_directory.value() + "/" + filename);
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
