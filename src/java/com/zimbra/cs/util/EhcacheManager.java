/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.util;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.Configuration;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;

import com.zimbra.cs.imap.ImapFolder;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.memcached.MemcachedConnector;

/**
 * Ehcache configurator.
 *
 * As of Ehcache 2.0, disk cache only configuration is no longer supported. But, {@code maxElementsInMemory = 1} is
 * virtually disk cache only. {@code maxElementsInMemory = 0} gives an infinite capacity.
 *
 * TODO Byte size based limit is only available from Ehcache 2.5.
 *
 * @author ysasaki
 */
public final class EhcacheManager {
    private static final EhcacheManager SINGLETON = new EhcacheManager();

    private CacheManager cacheManager;

    public static final String IMAP_ACTIVE_SESSION_CACHE = "imap-active-session-cache";
    public static final String IMAP_INACTIVE_SESSION_CACHE = "imap-inactive-session-cache";
    public static final String SYNC_STATE_ITEM_CACHE = "sync-state-item-cache";

    private EhcacheManager() {
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(LC.zimbra_home.value() + File.separator + "data" + File.separator + "mailboxd"))
                .withCache(IMAP_ACTIVE_SESSION_CACHE, createImapActiveSessionCache())
                .build(true);

        if (MemcachedConnector.isConnected()) {
            ZimbraLog.imap.info("Using Memcached for inactive session cache");
        } else {
            cacheManager.createCache(IMAP_INACTIVE_SESSION_CACHE, createImapInactiveSessionCache());
            cacheManager.createCache(SYNC_STATE_ITEM_CACHE, createSyncStateItemCache());
        }
    }

    public static EhcacheManager getInstance() {
        return SINGLETON;
    }

    public void startup() {
    }

    private CacheConfiguration createImapActiveSessionCache() {
        return CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                ImapFolder.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(1, EntryUnit.ENTRIES)
                .disk(100, MemoryUnit.GB, false))  // disk backed not persistent
                .build();
    }

    private CacheConfiguration createImapInactiveSessionCache() {
        long maxBytesOnLocalDisk;
        try {
            maxBytesOnLocalDisk = Provisioning.getInstance().getLocalServer().getImapInactiveSessionCacheMaxDiskSize();
        } catch (ServiceException e) {
            ZimbraLog.imap.error("Exception while fetching attribute imap ImapInactiveSessionCacheMaxDiskSize", e);
            maxBytesOnLocalDisk = 10737418240L;
        }

        return CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                ImapFolder.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(1, EntryUnit.ENTRIES)
                .offheap(LC.imap_inactive_session_cache_size.intValue(), MemoryUnit.B) 
                .disk(maxBytesOnLocalDisk, MemoryUnit.B, true)) // disk backed persistent store
                .build();

        // conf.setMaxElementsOnDisk(LC.imap_inactive_session_cache_size.intValue());
    }

    private CacheConfiguration createSyncStateItemCache() {
        return CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                ImapFolder.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(LC.zimbra_activesync_syncstate_item_cache_heap_size.intValue(), MemoryUnit.B)
                .disk(100, MemoryUnit.GB, true)) // disk backed persistent store
                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(LC.zimbra_activesync_metadata_cache_expiration.intValue(), TimeUnit.SECONDS)))
                .build();
    }

    public Cache getEhcache(String cacheName) {
        return cacheManager.getCache(cacheName, String.class, ImapFolder.class);
    }

    public void shutdown() {
        cacheManager.close();
    }
}
