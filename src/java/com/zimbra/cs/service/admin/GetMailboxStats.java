package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbMailbox.MailboxRawData;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.service.admin.GetAllMailboxes.MailboxesParams;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.Session;
import com.zimbra.soap.ZimbraSoapContext;

public class GetMailboxStats extends AdminDocumentHandler {
    private static final String GET_MAILBOX_STATS_CACHE_KEY = "GetMailboxStats";
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        
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
        List<MailboxRawData> mailboxes = doSearch();
        MailboxStats stats = new MailboxStats();
        
        for (MailboxRawData m : mailboxes) {
            stats.mNumMboxes++;
            stats.mTotalSize += m.size_checkpoint;
        }
        
        return stats;
    }
    
    private List<MailboxRawData> doSearch() throws ServiceException {
        Connection conn = null;
        List <MailboxRawData> result = null;
        
        try {
            conn = DbPool.getConnection();
            result = DbMailbox.getMailboxRawData(conn);
        } finally {
            DbPool.quietClose(conn);
        }
        return result;
    }
}
