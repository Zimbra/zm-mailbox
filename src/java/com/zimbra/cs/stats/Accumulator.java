/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.cs.stats;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TimerTask;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.util.Zimbra;

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
abstract class Accumulator {

    /**
     * Number of columns of statistics that this accumulator reports.
     */
    protected abstract int numColumns();
    
    /**
     * Get a label for given column.
     */
    protected abstract String getLabel(int column);

    /**
     * Get data for given column.
     */
    protected abstract String getData(int column);

    private String mName;
    
    public String getName() {
        return mName;
    }
    
    //
    // Factory support
    //
    private static SortedMap mAccumulators = new TreeMap();

    protected static Accumulator getInstance(String module, Class clz) {
        synchronized (mAccumulators) {
            Accumulator accum = (Accumulator)mAccumulators.get(module);
            if (accum != null) {
                return accum;
            }
            
            try {
                accum = (Accumulator)clz.newInstance();
            } catch (InstantiationException ie) {
                throw new RuntimeException("failed to create Accumulator for" + module, ie);
            } catch (IllegalAccessException iae) {
                throw new RuntimeException("failed to create Accumulator for" + module, iae);
            }
            accum.mName = module;
            mAccumulators.put(module, accum);
            return accum;
        }
    }

    //
    // Logging support
    //
    
    private static Log mLog = LogFactory.getLog(Accumulator.class);

    private void formatLabels(StringBuffer sb) {
        int ncolumns = numColumns();
        for (int i = 0; i < ncolumns; i++) {
            sb.append(getLabel(i));
            if (i+1 < ncolumns) {
                sb.append(',');
            }
        }
    }
    
    private void formatData(StringBuffer sb) {
        int ncolumns = numColumns();
        for (int i = 0; i < ncolumns; i++) {
            sb.append(getData(i));
            if (i+1 < ncolumns) {
                sb.append(',');
            }
        }
    }

    static final int DUMP_FREQUENCY = 60 * 1000; // every minute
    
    static {
        if (mLog.isInfoEnabled()) {
            Zimbra.sTimer.scheduleAtFixedRate(new Dumper(mLog), DUMP_FREQUENCY, DUMP_FREQUENCY);
        }
    }
    
    private static final class Dumper extends TimerTask {
        
        private Log mLog;
    
        Dumper(Log log) { mLog = log; };
        
        private int mNumColumns = 0;
        
        public boolean cancel() {
            System.out.println("DumpAccumulators canceled!!!");
            return super.cancel();
        }
        
        public void run() {
            synchronized (mAccumulators) {
                int numWatches = mAccumulators.keySet().size();
                if (0 == numWatches) {
                    return;
                }
                
                if (numWatches != mNumColumns) {
                    StringBuffer sb = new StringBuffer(numWatches * 16);
                    boolean first = true;
                    for (Iterator iter = mAccumulators.values().iterator(); iter.hasNext();) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(",");
                        }
                        Accumulator a = (Accumulator)iter.next();
                        a.formatLabels(sb);
                    }
                    mLog.info(sb);
                    mNumColumns = numWatches;
                }
                
                boolean first = true;
                StringBuffer sb = new StringBuffer(numWatches * 16);
                for (Iterator iter = mAccumulators.values().iterator(); iter.hasNext();) {
                    if (!first) {
                        sb.append(",");
                    } else {
                        first = false;
                    }
                    Accumulator a = (Accumulator)iter.next();
                    a.formatData(sb);
                }
                mLog.debug(sb);
            }
        }
        
    }
}
