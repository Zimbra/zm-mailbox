/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
