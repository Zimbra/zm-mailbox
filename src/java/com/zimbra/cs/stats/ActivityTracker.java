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
import java.io.FileWriter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.stats.StatUtil;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.Zimbra;


public class ActivityTracker {

    private static Map<String, ActivityTracker> sAllTrackers =
        new ConcurrentHashMap<String, ActivityTracker>();

    static {
        Thread logger = new Thread(new LoggerThread());
        logger.setDaemon(true);
        logger.start();
    }

    private String mFilename;
    private ConcurrentHashMap<String, Counter> mCounterMap =
        new ConcurrentHashMap<String, Counter>();
    
    private ActivityTracker(String filename) {
        mFilename = filename;
        // schedule here
    }
    
    /**
     * Gets or creates the <tt>ActivityTracker</tt> associated with
     * the given filename.
     * @param filename a simple filename, relative to the log directory
     */
    public static ActivityTracker getInstance(String filename) {
        ActivityTracker tracker = sAllTrackers.get(filename);
        if (tracker == null) {
            tracker = new ActivityTracker(filename);
            sAllTrackers.put(filename, tracker);
        }
        return tracker;
    }
    
    private String getFilename() {
        return mFilename;
    }
    
    private Map<String, Counter> getCounterMap() {
        return mCounterMap;
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
    
    private static class LoggerThread
    implements Runnable {
        public void run() {
            while (true) {
                // Sleep first so that stuff happens before logging
                try {
                    Thread.sleep(Constants.MILLIS_PER_MINUTE);
                } catch (InterruptedException e) {
                }
                if (Thread.currentThread().isInterrupted()) {
                    ZimbraLog.perf.info("%s shutting down", getClass().getName());
                    return;
                }

                for (ActivityTracker tracker : sAllTrackers.values()) {
                    try {
                        logStats(tracker);
                    } catch (Throwable t) {
                        if (t instanceof OutOfMemoryError) {
                            Zimbra.halt("Ran out of memory while logging user activity stats", t);
                        }
                        ZimbraLog.perf.warn("Unable to write to write stats to %s", tracker.getFilename(), t);
                    }
                }
            }
        }
        
        private void logStats(ActivityTracker tracker)
        throws Throwable {
            String path = LC.zimbra_log_directory.value() + "/" + tracker.getFilename();
            File file = new File(path);
            boolean writeHeader = false;
            if (!file.exists()) {
                writeHeader = true;
            }
            StringBuilder buf = new StringBuilder();
            Map<String, Counter> counterMap = tracker.getCounterMap();
            for (String command : counterMap.keySet()) {
                Counter counter = counterMap.get(command);
                if (counter.getCount() > 0) {
                    // This code is not thread-safe, but should be good enough 99.9% of the time.
                    // We avoid synchronization at the risk of the numbers being slightly off
                    // during a race condition.
                    long count = counter.getCount();
                    long avg = (long) counter.getAverage();
                    counter.reset();
                    buf.append(String.format(Locale.US, "%s,%s,%d,%d\n",
                        StatUtil.getTimestampString(), command, count, avg)); 
                }
            }
            FileWriter writer = new FileWriter(file, true);
            if (writeHeader) {
                writer.write("timestamp,command,exec_count,exec_ms_avg\n");
            }
            writer.write(buf.toString());
            writer.close();
        }
    }
}
