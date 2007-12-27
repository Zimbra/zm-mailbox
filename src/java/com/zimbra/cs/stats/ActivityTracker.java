/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.stats;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.common.stats.Counter;
import com.zimbra.common.stats.StatsDumperDataSource;


public class ActivityTracker
implements StatsDumperDataSource {

    private File mCsvFile;
    private ConcurrentHashMap<String, Counter> mCounterMap =
        new ConcurrentHashMap<String, Counter>();
    
    public ActivityTracker(String csvFile) {
        mCsvFile = new File(csvFile);
    }
    
    public void addStat(String commandName, long startTime) {
        Counter counter = getCounter(commandName);
        counter.increment(System.currentTimeMillis() - startTime);
    }
    
    private Counter getCounter(String commandName) {
        Counter counter = mCounterMap.get(commandName);
        if (counter == null) {
            counter = new Counter(commandName, "ms");
            counter.setShowAverage(true);
            counter.setShowCount(true);
            counter.setShowTotal(false);
            
            Counter previousCounter = mCounterMap.putIfAbsent(commandName, counter);
            if (previousCounter != null) {
                // Another thread added the counter after the get() check.  Use it instead
                // of the one we just instantiated.
                counter = previousCounter;
            }
        }
        return counter;
    }
    
    ////////////// StatsDumperDataSource implementation //////////////
    
    public Collection<String> getDataLines() {
        if (mCounterMap == null || mCounterMap.size() == 0) {
            return null;
        }
        List<String> dataLines = new ArrayList<String>(mCounterMap.size());
        for (String command : mCounterMap.keySet()) {
            Counter counter = mCounterMap.get(command);
            if (counter.getCount() > 0) {
                // This code is not thread-safe, but should be good enough 99.9% of the time.
                // We avoid synchronization at the risk of the numbers being slightly off
                // during a race condition.
                long count = counter.getCount();
                long avg = (long) counter.getAverage();
                counter.reset();
                dataLines.add(String.format("%s,%d,%d", command, count, avg)); 
            }
        }
        return dataLines;
    }

    public File getFile() {
        return mCsvFile;
    }

    public String getHeader() {
        return "command,exec_count,exec_ms_avg";
    }

    public boolean hasTimestampColumn() {
        return true;
    }
}
