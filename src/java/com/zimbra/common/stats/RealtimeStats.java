/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.util.ArrayUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

/**
 * This implementation of <code>Accumulator</code> is used to retrieve
 * the current value of a statistic.  When a system component initializes,
 * it calls {@link #setCallback} 
 * @author bburtin
 *
 */
public class RealtimeStats implements Accumulator {

    private List<String> mNames;
    private List<RealtimeStatsCallback> mCallbacks = new ArrayList<RealtimeStatsCallback>();
    
    public RealtimeStats(String[] names) {
        if (ArrayUtil.isEmpty(names)) {
            throw new IllegalArgumentException("names cannot be null or empty");
        }
        mNames = new ArrayList<String>();
        for (String name : names) {
            mNames.add(name);
        }
    }
    
    public void addName(String name) {
        mNames.add(name);
    }
    
    public void addCallback(RealtimeStatsCallback callback) {
        mCallbacks.add(callback);
    }
    
    public List<String> getNames() {
        return mNames;
    }

    public List<Object> getData() {
        List<Object> data = new ArrayList<Object>();
        
        // Collect stats from all callbacks
        Map<String, Object> callbackResults = new HashMap<String, Object>();
        for (RealtimeStatsCallback callback : mCallbacks) {
            Map<String, Object> callbackData = callback.getStatData();
            if (callbackData != null) {
                callbackResults.putAll(callbackData);
            }
        }
        
        // Populate data based on callback results
        for (String name : mNames) {
            data.add(callbackResults.remove(name));
        }
        if (callbackResults.size() > 0) {
            ZimbraLog.perf.warn("Detected unexpected realtime stats: " +
                StringUtil.join(", ", callbackResults.keySet()));
            
        }
        return data;
    }

    public void reset() {
    }
}
