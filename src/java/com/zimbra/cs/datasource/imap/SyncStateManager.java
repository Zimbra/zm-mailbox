/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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
