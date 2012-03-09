package com.zimbra.cs.account.cache;

import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.cache.DomainCache.GetFromDomainCacheOption;

public interface IDomainCache extends IEntryCache {
    public void clear();
    public void remove(Domain entry);
    public void replace(Domain entry);
    public void removeFromNegativeCache(DomainBy domainBy, String key);
    public void put(DomainBy domainBy, String key, Domain entry);
    public Domain getById(String key, GetFromDomainCacheOption option);
    public Domain getByName(String key, GetFromDomainCacheOption option);
    public Domain getByVirtualHostname(String key, GetFromDomainCacheOption option);
    public Domain getByForeignName(String key, GetFromDomainCacheOption option);
    public Domain getByKrb5Realm(String key, GetFromDomainCacheOption option);
}
