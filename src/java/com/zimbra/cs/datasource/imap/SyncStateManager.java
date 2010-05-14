/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.datasource.imap;

import com.zimbra.cs.account.DataSource;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

final class SyncStateManager extends HashMap<Integer, SyncState>{
    private static final int MAX_ENTRIES = 64;
    
    private static final Map<String, SyncStateManager> instances =
        new LinkedHashMap<String, SyncStateManager>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, SyncStateManager> eldest) {
                return size() > MAX_ENTRIES;
            }
        };

    public static SyncStateManager getInstance(DataSource ds) {
        if (!ds.isOffline()) {                
            return null; // Only enabled for offline data sources
        }
        synchronized (instances) {
            SyncStateManager ssm = instances.get(ds.getId());
            if (ssm == null) {
                ssm = new SyncStateManager();
                instances.put(ds.getId(), ssm);
            }
            return ssm;
        }
    }

    public static void removeInstance(DataSource ds) {
        synchronized (instances) {
            instances.remove(ds.getId());
        }
    }

    private SyncStateManager() {}
}
