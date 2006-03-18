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
package com.zimbra.cs.stats;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import com.zimbra.cs.util.StringUtil;

/**
 * Subclasses <code>DailyRollingFileAppender</code> and writes the column
 * names as the first line of the CSV file.  This class currently only
 * supports zimbrastats.csv.
 * 
 * @author bburtin
 */
public class ZimbraStatsAppender
extends DailyRollingFileAppender {

    private boolean mIsNewFile = false;

    /**
     * Keeps track of whether we're creating a new file.
     */
    public synchronized void setFile(String fileName, boolean append, boolean bufferedIO, int bufferSize)
    throws IOException {
        File file = new File(fileName);
        if (file.length() == 0) {
            mIsNewFile = true;
        }
        super.setFile(fileName, append, bufferedIO, bufferSize);
    }

    /**
     * Overrides the parent implementation to log the header
     * if this is a new logfile.
     */
    protected void subAppend(LoggingEvent event) {
        if (mIsNewFile) {
            mIsNewFile = false;
            Logger logger = Logger.getLogger(event.getLoggerName());
            String header = StringUtil.join(",", ZimbraPerf.getZimbraStatsColumns());
            LoggingEvent headerEvent = new LoggingEvent(
                ZimbraStatsAppender.class.getName(), logger, event.getLevel(), header, null);
            super.subAppend(headerEvent);
        }
        super.subAppend(event);
    }
}
