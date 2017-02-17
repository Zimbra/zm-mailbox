/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.datasource;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbPop3Message;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;

public class PopMessage extends DataSourceMapping {
    public PopMessage(DataSource ds, DataSourceItem dsi) throws ServiceException {
        super(ds, dsi);
    }
    
    public PopMessage(DataSource ds, int itemId) throws ServiceException {
        super(ds, itemId);
    }
    
    public PopMessage(DataSource ds, String uid) throws ServiceException {
        super(ds, uid);
    }
    
    public PopMessage(DataSource ds, int itemId, String uid) throws
        ServiceException {
        super(ds, ds.getFolderId(), itemId, uid);
    }
    
    public static Set<PopMessage> getMappings(DataSource ds, String[] remoteIds)
        throws ServiceException {
        Collection<DataSourceItem> mappings = DbDataSource.getReverseMappings(ds,
            Arrays.asList(remoteIds));
        Set<PopMessage> matchingMsgs = new HashSet<PopMessage>();
        
        if (mappings.isEmpty()) {
            Map<Integer, String> oldMappings = DbPop3Message.getMappings(
                DataSourceManager.getInstance().getMailbox(ds), ds.getId());

            for (Integer itemId : oldMappings.keySet()) {
                String uid = oldMappings.get(itemId);
                PopMessage mapping = new PopMessage(ds, itemId, uid);
                
                mapping.add();
                for (String remoteId : remoteIds) {
                    if (remoteId.equals(uid))
                        matchingMsgs.add(mapping);
                }
            }
            if (!oldMappings.isEmpty())
                DbPop3Message.deleteUids(DataSourceManager.getInstance().getMailbox(ds), ds.getName());
        } else {
            for (DataSourceItem mapping : mappings)
                matchingMsgs.add(new PopMessage(ds, mapping));
        }
        return matchingMsgs;
    }

    public static Set<String> getMatchingUids(DataSource ds, String[] remoteIds)
        throws ServiceException {
        Set<PopMessage> matchingMsgs = getMappings(ds, remoteIds);
        Set<String> matchingUids = new HashSet<String>(matchingMsgs.size());
               
        for (PopMessage msg : matchingMsgs)
            matchingUids.add(msg.getRemoteId());
        return matchingUids;
    }
}

