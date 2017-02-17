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
package com.zimbra.cs.stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.common.stats.Counter;
import com.zimbra.common.stats.StatsDumperDataSource;


/**
 * ActivityTrackers get their own output file (e.g. soap.csv) and track a set of "commands" and their "total elapsed time" 
 * for each counter stat period, one on a line.
 */
public class ActivityTracker
implements StatsDumperDataSource {

    private String mFilename;
    private ConcurrentHashMap<String, Counter> mCounterMap =
        new ConcurrentHashMap<String, Counter>();
    
    public ActivityTracker(String filename) {
        mFilename = filename;
    }
    
    public void addStat(String commandName, long startTime) {
        Counter counter = getCounter(commandName);
        counter.increment(System.currentTimeMillis() - startTime);
    }
    
    private Counter getCounter(String commandName) {
        Counter counter = mCounterMap.get(commandName);
        if (counter == null) {
            counter = new Counter();
            
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

    public String getFilename() {
        return mFilename;
    }

    public String getHeader() {
        return "command,exec_count,exec_ms_avg";
    }

    public boolean hasTimestampColumn() {
        return true;
    }
}
