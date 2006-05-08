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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.util.ArrayUtil;
import com.zimbra.cs.util.StringUtil;
import com.zimbra.cs.util.ZimbraLog;

/**
 * This implementation of <code>Accumulator</code> is used to retrieve
 * the current value of a statistic.  When a system component initializes,
 * it calls {@link #setCallback} 
 * @author bburtin
 *
 */
class RealtimeStats implements Accumulator {

    private List<String> mNames;
    private List<RealtimeStatsCallback> mCallbacks = new ArrayList<RealtimeStatsCallback>();
    
    RealtimeStats(String[] names) {
        if (ArrayUtil.isEmpty(names)) {
            throw new IllegalArgumentException("names cannot be null or empty");
        }
        mNames = new ArrayList<String>();
        for (String name : names) {
            mNames.add(name);
        }
    }
    
    void addCallback(RealtimeStatsCallback callback) {
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
