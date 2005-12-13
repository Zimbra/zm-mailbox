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

package com.zimbra.cs.stats;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TimerTask;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.util.Constants;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraLog;

/**
 * Abstraction for a statistic that increases over time.  A single
 * accumulator might keep track multiple statistics, eg, elapsed
 * time and average time.
 *
 * Required magic in log4j.properties to log statistics.
 * 
 *   # Appender STATS writes to file "stats"
 *   log4j.appender.STATS=org.apache.log4j.FileAppender
 *   log4j.appender.STATS.File=/temp/stats.log
 *   log4j.appender.STATS.Append=false
 *   log4j.appender.STATS.layout=org.apache.log4j.PatternLayout
 *   log4j.appender.STATS.layout.ConversionPattern=%r,%m%n
 *   log4j.additivity.com.zimbra.cs.stats=false
 *   log4j.logger.com.zimbra.cs.stats=DEBUG,STATS
 */
public abstract class Accumulator {

    protected abstract List<String> getLabels();
    protected abstract List getData();
    abstract void reset();

    private String mName;
    
    public String getName() {
        return mName;
    }
    
    //
    // Factory support
    //
    private static SortedMap<String, Accumulator> mAccumulators = new TreeMap<String, Accumulator>();

    protected static Accumulator getInstance(String name, Class clz) {
        synchronized (mAccumulators) {
            Accumulator accum = mAccumulators.get(name);
            if (accum != null) {
                return accum;
            }
            
            try {
                accum = (Accumulator)clz.newInstance();
            } catch (InstantiationException ie) {
                throw new RuntimeException("failed to create Accumulator for" + name, ie);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException("failed to create Accumulator for" + name, iae);
            }
            accum.mName = name;
            mAccumulators.put(name, accum);
            return accum;
        }
    }

    //
    // Logging support
    //
    
    private static Log mLog = LogFactory.getLog(Accumulator.class);

    private void formatLabels(StringBuffer sb) {
        String labels = StringUtil.join(",", getLabels());
        sb.append(labels);
    }
    
    private void formatData(StringBuffer sb) {
        String data = StringUtil.join(",", getData());
        sb.append(data);
    }

    static final long DUMP_FREQUENCY = Constants.MILLIS_PER_MINUTE;
    
    static  {
        Zimbra.sTimer.scheduleAtFixedRate(new Dumper(mLog), DUMP_FREQUENCY, DUMP_FREQUENCY);
    }
    
    private static final class Dumper extends TimerTask {
        
        private static final SimpleDateFormat TIMESTAMP_FORMATTER =
            new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

        private Log mLog;
    
        Dumper(Log log) { mLog = log; };
        
        private int mNumColumns = 0;
        
        public boolean cancel() {
            System.out.println("DumpAccumulators canceled!!!");
            return super.cancel();
        }
        
        public void run() {
            synchronized (mAccumulators) {
                int numWatches = mAccumulators.size();
                if (0 == numWatches) {
                    return;
                }
                
                try {
                    if (numWatches != mNumColumns) {
                        StringBuffer sb = new StringBuffer("timestamp");
                        for (Iterator iter = mAccumulators.values().iterator(); iter.hasNext();) {
                            sb.append(',');
                            Accumulator a = (Accumulator)iter.next();
                            a.formatLabels(sb);
                        }
                        mLog.info(sb);
                        mNumColumns = numWatches;
                    }
                    
                    StringBuffer sb = new StringBuffer(TIMESTAMP_FORMATTER.format(new Date()));
                    for (Iterator iter = mAccumulators.values().iterator(); iter.hasNext();) {
                        sb.append(",");
                        Accumulator a = (Accumulator)iter.next();
                        synchronized (a) {
                            a.formatData(sb);
                            a.reset();
                        }
                    }
                    mLog.info(sb);
                } catch (Throwable t) {
                    if (t instanceof OutOfMemoryError) {
                        throw (OutOfMemoryError) t;
                    }
                    ZimbraLog.misc.error("Accumulator error", t);
                }
            }
        }
        
    }
}
