package com.zimbra.cs.mailbox;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mailbox.redis.RedisUtils;

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
        return Lists.newArrayList(listMailboxes().values());
    }

    RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();

	private RBucket<Integer> getRedisBucket(String accountId) {
		RBucket<Integer> mailboxKeyBucket = client.getBucket(RedisUtils.createAccountRoutedKey(accountId, "MAILBOX_ID"));
		return mailboxKeyBucket;
	}

	public Integer fetchMailboxKey(String accountId) {

		getRedisBucket(accountId);
		Integer mailboxAccID = getRedisBucket(accountId).get();
		Integer mboxID = null;
		
		if (mailboxAccID != null) {
			return mailboxAccID;
		}
		else {	
			mboxID = getMailboxKeyFromDB(accountId);
			if (mboxID != null) {
				getRedisBucket(accountId).set(mboxID);
			}
			return mailboxAccID;
		}
	}

	public Integer getMailboxKeyFromDB(String accountId){
		Integer mailboxKey = null;
		DbPool.DbConnection conn = null;
		try {
			try {
				conn = DbPool.getConnection();
				mailboxKey =  DbMailbox.getMailboxKey(conn, accountId.toLowerCase());
			} catch (ServiceException e) {
				ZimbraLog.mailbox.error("fetching mailbox key: %s", accountId, e);
				throw new CacheManagerException("fetching mailbox key: ", e.getCause());
			}
		}finally {
			DbPool.quietClose(conn);
		}
		return mailboxKey;
	}

	@Override
	public Integer getMailboxKey(String accountId) {

		if (accountId == null)
			throw new IllegalArgumentException();

		Integer mailboxKey = null;
		mailboxKey = fetchMailboxKey(accountId.toLowerCase());
		return mailboxKey;
	}
}
