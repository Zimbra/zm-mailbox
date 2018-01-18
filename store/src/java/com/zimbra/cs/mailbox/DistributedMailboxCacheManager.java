package com.zimbra.cs.mailbox;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import java.util.*;

public class DistributedMailboxCacheManager implements MailboxCacheManager {

    /**
     * Maps account IDs (<code>String</code>s) to mailbox IDs
     * (<code>Integer</code>s).  <i>Every</i> mailbox in existence on the
     * server appears in this mapping.
     */
    private final Map<String, Integer> mailboxIds;

    /**
     * Maps mailbox IDs ({@link Integer}s) to either
     * <ul>
     * <li>a loaded {@link Mailbox}, or
     * <li>a {@link SoftReference} to a loaded {@link Mailbox}, or
     * <li>a {@link MaintenanceContext} for the mailbox.
     * </ul>
     * Mailboxes are faulted into memory as needed, but may drop from memory when the SoftReference expires due to
     * memory pressure combined with a lack of outstanding references to the {@link Mailbox}.  Only one {@link Mailbox}
     * per user is cached, and only that {@link Mailbox} can process user requests.
     */
    private final MailboxMap cache;

    public DistributedMailboxCacheManager() {
        DbPool.DbConnection conn = null;
        try {
            conn = DbPool.getConnection();
            mailboxIds = DbMailbox.listMailboxes(conn, null);
            cache = createCache();
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("creating cache manager: ", e);
            throw new CacheManagerException("creating cache manager: ", e.getCause());
        } finally {
            DbPool.quietClose(conn);
        }
    }

    private synchronized Map<String, Integer> listMailboxes() {
        DbPool.DbConnection conn = null;
        try {
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
    public Set<Map.Entry<String, Integer>> getMailboxIdsByAccountId() {
        return listMailboxes().entrySet();
    }

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

    @Override
    public void removeMailboxId(String accountId) {
        //noop
    }

    @Override
    public synchronized Object retrieveFromCache(int mailboxId, boolean trackGC, MailboxManager mailboxManager) throws MailServiceException {
        return getMailbox(mailboxId);

    }

    @Override
    public void cacheMailbox(int mailboxId, MailboxMaintenance maint) {
        //noop
    }

    @Override
    public Object getMailbox(int mailboxId) {
        Mailbox.MailboxData data;
        DbPool.DbConnection conn = null;
        try {
            conn = DbPool.getConnection();
            data = DbMailbox.getMailboxStats(conn, mailboxId);
            if (data == null) {
                throw MailServiceException.NO_SUCH_MBOX(mailboxId);
            }
        } catch (MailServiceException e) {
            ZimbraLog.mailbox.error("fetching mailbox: ", e);
            throw new CacheManagerException("fetching mailbox: ", e.getCause());
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("fetching mailbox: ", e);
            throw new CacheManagerException("fetching mailbox: ", e.getCause());
        } finally {
            conn.closeQuietly();
        }
        return new Mailbox(data);
    }

    @Override
    public void cacheMailbox(Mailbox mailbox) {
    }

    @Override
    public void removeMailbox(int mailboxId) {
        //noop
    }

    @Override
    public void cacheAccount(String accountId, int mailboxId) {
        //noop
    }

    @Override
    public int getMailboxCacheSize() {
        return getMailboxCount();
    }

    @Override
    public List<Object> getAllLoadedMailboxes() {
        List<Mailbox.MailboxData> mailboxDataList = getAllMailboxRawData();
        List<Object> mailboxList = new ArrayList<>();
        for(Mailbox.MailboxData mailboxData: mailboxDataList){
            mailboxList.add(new Mailbox(mailboxData));
        }
        return mailboxList;
    }

    @Override
    public Set<Map.Entry<Integer, Object>> getMailboxesById() {
        List<Mailbox.MailboxData> mailboxDataList = getAllMailboxRawData();
        Map mailboxList = new HashMap();
        for(Mailbox.MailboxData mailboxData: mailboxDataList){
            mailboxList.put(mailboxData.id,new Mailbox(mailboxData));
        }
        return mailboxList.keySet();
    }

    private synchronized List<Mailbox.MailboxData> getAllMailboxRawData(){
        List<Mailbox.MailboxData> result;
        DbPool.DbConnection conn = null;
        try {
            conn = DbPool.getConnection();
            result = DbMailbox.getMailboxRawData(conn);
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("fetching mailboxes raw data: ", e);
            throw new CacheManagerException("fetching mailboxes raw data: ", e.getCause());
        }finally {
            DbPool.quietClose(conn);
        }
        return result;
    }

    @VisibleForTesting
    public void clearCache() {
        //noop
    }


    public MailboxMap createCache() {
        return new MailboxMap(LC.zimbra_mailbox_manager_hardref_cache.intValue());
    }

    protected static class MailboxMap {
        @SuppressWarnings("serial")
        MailboxMap(int hardSize) {
            // noop
        }
    }
}
