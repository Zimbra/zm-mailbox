package com.zimbra.cs.mailbox;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface MailboxCacheManager {

    Collection<Integer> getMailboxIds();

    Set<String> getAccountIds();

    Set<Map.Entry<String, Integer>> getMailboxIdsByAccountId();

    int getMailboxCount();

    Integer getMailboxKey(String accountId);

    void removeMailboxId(String accountId);

    Object retrieveFromCache(int mailboxId, boolean trackGC, MailboxManager mailboxManager) throws MailServiceException;

    void cacheMailbox(int mailboxId, MailboxMaintenance maint);

    Object getMailbox(int mailboxId);

    Mailbox cacheMailbox(Mailbox mailbox, MailboxManager mailboxManager);

    void removeMailbox(int mailboxId);

    void cacheAccount(String accountId, int mailboxId);

    int getMailboxCacheSize();

    Collection<Object> getAllLoadedMailboxes();

    Set<Map.Entry<Integer, Object>> getMailboxesById();

    void clearCache();

}
