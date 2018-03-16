package com.zimbra.cs.mailbox;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;

public class DistributedMailboxCacheManager extends MailboxCacheManager {

    private synchronized Map<String, Integer> listMailboxes() {
        DbPool.DbConnection conn = null;
        try {
            conn = DbPool.getConnection();
            return DbMailbox.listMailboxes(conn, null);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("fetching mailboxes: ", e);
            throw new CacheManagerException("fetching mailboxes: ", e.getCause());
        } finally {
            DbPool.quietClose(conn);
        }
    }

    @Override
    public int getMailboxCount(){
        Integer mailboxCount = null;
        DbPool.DbConnection conn = null;
        try {
            try {
                conn = DbPool.getConnection();
                mailboxCount =  DbMailbox.getMailboxCount(conn);
            } catch (ServiceException e) {
                ZimbraLog.mailbox.error("getting mailbox count: ", e);
                throw new CacheManagerException("getting mailbox count: ", e.getCause());
            }
        }finally {
            DbPool.quietClose(conn);
        }
        return mailboxCount;
    }

    @Override
    public Set<String> getAccountIds(){
        return listMailboxes().keySet();
    }

    @Override
    public List<Integer> getMailboxIds(){
        return (List<Integer>) listMailboxes().values();
    }

    @Override
    public Integer getMailboxKey(String accountId){
        Integer mailboxKey = null;
        DbPool.DbConnection conn = null;
        try {
            try {
                conn = DbPool.getConnection();
                mailboxKey =  DbMailbox.getMailboxKey(conn, accountId.toLowerCase());
            } catch (ServiceException e) {
                ZimbraLog.mailbox.error("fetching mailbox key: ", e);
                throw new CacheManagerException("fetching mailbox key: ", e.getCause());
            }
        }finally {
            DbPool.quietClose(conn);
        }
        return mailboxKey;
    }
}
