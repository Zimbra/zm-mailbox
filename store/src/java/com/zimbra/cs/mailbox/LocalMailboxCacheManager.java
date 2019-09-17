package com.zimbra.cs.mailbox;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalMailboxCacheManager extends MailboxCacheManager {

    @Override
    public List<Integer> getMailboxIds() {
        return (List<Integer>) mailboxIds.values();
    }

    @Override
    public Map<String, Integer> getAccountIdToMailboxIdMap() {
        return mailboxIds;
    }

    @Override
    public Set<String> getAccountIds(){
        return mailboxIds.keySet();
    }

    @Override
    public int getMailboxCount(){
        return mailboxIds.size();
    }

    @Override
    public Integer getMailboxKey(String accountId){
        return mailboxIds.get(accountId.toLowerCase());
    }
}