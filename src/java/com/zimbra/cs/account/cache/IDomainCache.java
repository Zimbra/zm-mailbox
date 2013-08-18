/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
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
