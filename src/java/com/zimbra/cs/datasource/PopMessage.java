/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
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

