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
/**
 * 
 */
package com.zimbra.common.stats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.StringUtil;

/**
 * Returns thread count statistics.  Threads whose names start with
 * known prefixes are counted together.  All other threads are grouped
 * in the "other" category.
 */
public class ThreadStats
implements StatsDumperDataSource
{
    private String mFilename;
    private String[] mThreadNamePrefixes;

    /**
     * Creates a new <tt>ThreadStats</tt> object.
     *  
     * @param threadNamePrefixes known thread name prefixes
     * @param threadsCsvFile CSV file that stats are written to 
     */
    public ThreadStats(String[] threadNamePrefixes, String filename) {
        mThreadNamePrefixes = threadNamePrefixes;
        mFilename = filename;
    }
    
    public String getFilename() {
        return mFilename; 
    }
    
    public String getHeader() {
        return StringUtil.join(",", mThreadNamePrefixes) + ",other,total";
    }

    /**
     * Returns a single line of data.  Each column corresponds to a known
     * thread name prefix.  The last two columns are "other" and "total".
     */
    public Collection<String> getDataLines() {
        // Tally threads by name
        Map<String, Integer> threadCount = new LinkedHashMap<String, Integer>();
        for (String prefix : mThreadNamePrefixes) {
            threadCount.put(prefix, 0);
        }
        threadCount.put("other", 0);
        
        // Find the root thread group
        ThreadGroup root = Thread.currentThread().getThreadGroup().getParent();
        while (root.getParent() != null) {
            root = root.getParent();
        }
        
        // Iterate all threads and increment counters
        Thread[] threads = new Thread[root.activeCount() * 2];
        int numThreads = root.enumerate(threads, true);
        for (int i = 0; i < numThreads; i++) {
            Thread thread = threads[i];
            String threadName = thread.getName();
            boolean found = false;
            for (String prefix : mThreadNamePrefixes) {
                // Increment count for the prefix if found
                if (threadName != null && threadName.startsWith(prefix)) {
                    threadCount.put(prefix, threadCount.get(prefix) + 1);
                    found = true;
                    break;
                }
            }
            if (!found) {
                threadCount.put("other", threadCount.get("other") + 1);
            }
        }
        
        StringBuilder buf = new StringBuilder();
        for (String prefix : threadCount.keySet()) {
            buf.append(threadCount.get(prefix));
            buf.append(',');
        }
        buf.append(numThreads);
        
        // Return value
        List<String> retVal = new ArrayList<String>(1);
        retVal.add(buf.toString());
        return retVal;
    }

    public boolean hasTimestampColumn() {
        return true;
    }
}