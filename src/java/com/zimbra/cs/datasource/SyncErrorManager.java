/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2010 Zimbra, Inc.
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
package com.zimbra.cs.datasource;

import com.zimbra.cs.account.DataSource;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;

/**
 * Manage reporting of offline data source item sync failures.
 */
public final class SyncErrorManager {
    private static final int MAX_ENTRIES = 64 * 1000;

    public enum ItemType {
        ITEM_ID, REMOTE_ID, REMOTE_PATH
    }

    private static final Map<String, AtomicInteger> ERRORS =
        new LinkedHashMap<String, AtomicInteger>() {
            protected boolean removeEldestEntry(Map.Entry e) {
                return size() > MAX_ENTRIES;
            }
        };

    private static String key(DataSource ds, ItemType type, Object id) {
        return String.format("%s:%s:%s", ds.getId(), type, id);
    }

    public static void clearErrors(DataSource ds) {
        String prefix = ds.getId() + ":";
        synchronized (ERRORS) {
            Iterator<String> it = ERRORS.keySet().iterator();
            while (it.hasNext()) {
                if (it.next().startsWith(prefix)) {
                    it.remove();
                }
            }
        }
    }

    public static int incrementErrorCount(DataSource ds, int itemId) {
        return incrementErrorCount(ds, ItemType.ITEM_ID, itemId);
    }

    public static int incrementErrorCount(DataSource ds, String remoteId) {
        return incrementErrorCount(ds, ItemType.REMOTE_ID, remoteId);
    }
    
    public static int incrementErrorCount(DataSource ds, ItemType type, Object id) {
        String key = key(ds, type, id);
        synchronized (ERRORS) {
            AtomicInteger count = ERRORS.get(key);
            if (count == null) {
                count = new AtomicInteger();
                ERRORS.put(key, count);
            }
            return count.incrementAndGet();
        }
    }

    public static void clearError(DataSource ds, int itemId) {
        clearError(ds, ItemType.ITEM_ID, itemId);
    }

    public static void clearError(DataSource ds, String remoteId) {
        clearError(ds, ItemType.REMOTE_ID, remoteId);
    }
    
    public static void clearError(DataSource ds, ItemType type, Object id) {
        synchronized (ERRORS) {
            ERRORS.remove(key(ds, type, id));
        }
    }

    public static int getErrorCount(DataSource ds, int itemId) {
        return getErrorCount(ds, ItemType.ITEM_ID, itemId);
    }

    public static int getErrorCount(DataSource ds, String remoteId) {
        return getErrorCount(ds, ItemType.REMOTE_ID, remoteId);
    }
    
    public static int getErrorCount(DataSource ds, ItemType type, Object id) {
        synchronized (ERRORS) {
            AtomicInteger count = ERRORS.get(key(ds, type, id));
            return count != null ? count.get() : 0;
        }
    }
}
