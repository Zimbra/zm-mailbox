/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.ZimbraSoapContext;

public class GetMailboxStats extends AdminDocumentHandler {
    private static final String GET_MAILBOX_STATS_CACHE_KEY = "GetMailboxStats";
    
    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
        Server localServer = Provisioning.getInstance().getLocalServer();
        checkRight(zsc, context, localServer, Admin.R_getMailboxStats);
        
        MailboxStats stats = null;
        
        AdminSession session = (AdminSession) getSession(zsc, Session.Type.ADMIN);
        if (session != null) {
            MailboxStats cachedStats = (MailboxStats) session.getData(GET_MAILBOX_STATS_CACHE_KEY);
            if (cachedStats == null) {
                stats = doStats();
                session.setData(GET_MAILBOX_STATS_CACHE_KEY, stats);
            } else {
                stats = cachedStats;
            }
        } else {
            stats = doStats();
        }
        
        Element response = zsc.createElement(AdminConstants.GET_MAILBOX_STATS_RESPONSE);
        Element statsElem = response.addElement(AdminConstants.E_STATS);
        statsElem.addAttribute(AdminConstants.A_NUM_MBOXES, stats.mNumMboxes);
        statsElem.addAttribute(AdminConstants.A_TOTAL_SIZE, stats.mTotalSize);
        return response;
    }
    
    private static class MailboxStats {
        long mNumMboxes = 0;
        long mTotalSize = 0;
    }
   
    private MailboxStats doStats() throws ServiceException {
        List<Mailbox.MailboxData> mailboxes = doSearch();
        MailboxStats stats = new MailboxStats();
        
        for (Mailbox.MailboxData m : mailboxes) {
            stats.mNumMboxes++;
            stats.mTotalSize += m.size;
        }
        
        return stats;
    }
    
    private List<Mailbox.MailboxData> doSearch() throws ServiceException {
        List <Mailbox.MailboxData> result = null;
        synchronized (DbMailbox.getSynchronizer()) {
            Connection conn = null;
            try {
                conn = DbPool.getConnection();
                result = DbMailbox.getMailboxRawData(conn);
            } finally {
                DbPool.quietClose(conn);
            }
        }
        return result;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_getMailboxStats);
    }
}
