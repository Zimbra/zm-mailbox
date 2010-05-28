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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DataSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class SyncStateManager {
    private final Map<String, SyncState> entries;

    private static final SyncStateManager INSTANCE = new SyncStateManager();

    public static SyncStateManager getInstance() {
        return INSTANCE;
    }
    
    private SyncStateManager() {
        entries = Collections.synchronizedMap(new HashMap<String, SyncState>());
    }

    public SyncState getSyncState(DataSource ds) {
        return entries.get(ds.getId());
    }
    
    public SyncState getOrCreateSyncState(DataSource ds) throws ServiceException {
        synchronized (entries) {
            SyncState ss = entries.get(ds.getId());
            if (ss == null) {
                ss = new SyncState(ds.getMailbox());
                entries.put(ds.getId(), ss);
            }
            return ss;
        }
    }

    public void removeSyncState(String dataSourceId) {
        entries.remove(dataSourceId);
    }
}
