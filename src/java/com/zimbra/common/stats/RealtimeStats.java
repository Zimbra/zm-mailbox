/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
