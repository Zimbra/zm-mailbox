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
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.PooledExecutionServiceConfigurationBuilder;
import org.ehcache.config.builders.WriteBehindConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.imap.ImapFolder;
import com.zimbra.cs.memcached.MemcachedConnector;

/**
 * Ehcache configurator.
 *
 * As of Ehcache 2.0, disk cache only configuration is no longer supported. But, {@code maxElementsInMemory = 1} is
 * virtually disk cache only. {@code maxElementsInMemory = 0} gives an infinite capacity.
 *
 * @author ysasaki
 */
public final class EhcacheManager {
    private static EhcacheManager SINGLETON = null;

    private CacheManager cacheManager;

    public static final String IMAP_ACTIVE_SESSION_CACHE = "imap-active-session-cache";
    public static final String IMAP_INACTIVE_SESSION_CACHE = "imap-inactive-session-cache";
    public static final String SYNC_STATE_ITEM_CACHE = "sync-state-item-cache";
    public static final String IMAP_INACTIVE_LARGE_FOLDER_SESSION_CACHE  = "imap-inactive-large-folder-session-cache";

    private EhcacheManager(Service service) {
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(LC.zimbra_home.value() + File.separator + "data" + File.separator + service.val))
                .using(PooledExecutionServiceConfigurationBuilder.newPooledExecutionServiceConfigurationBuilder()
                        .defaultPool("dflt", 1, 5)
                        .pool("heavyWritePool", 1, 64)
                        .build())
                .withCache(IMAP_ACTIVE_SESSION_CACHE, createImapActiveSessionCache())
                .build(true);

        if (MemcachedConnector.isConnected()) {
            ZimbraLog.imap.info("Using Memcached for inactive session cache");
        } else {
            cacheManager.createCache(IMAP_INACTIVE_SESSION_CACHE, createImapInactiveSessionCache());
            cacheManager.createCache(SYNC_STATE_ITEM_CACHE, createActiveSyncStateItemCache());
            cacheManager.createCache(IMAP_INACTIVE_LARGE_FOLDER_SESSION_CACHE,
                    createImapInactiveLageFolderSessionCache());
        }
    }

    public static EhcacheManager getInstance() {
        return getInstance(Service.MAILBOX);
    }

    public synchronized static EhcacheManager getInstance(Service service) {
        if(SINGLETON == null) {
            SINGLETON = new EhcacheManager(service);
        }
        return SINGLETON;
    }

    public void startup() {
    }

    private CacheConfiguration<String, ImapFolder> createImapActiveSessionCache() {
        long maxBytesOnLocalDisk;
        try {
            maxBytesOnLocalDisk = Provisioning.getInstance().getLocalServer().getImapActiveSessionEhcacheMaxDiskSize();
        } catch (ServiceException e) {
            ZimbraLog.imap.error("Exception while fetching attribute %s", Provisioning.A_zimbraImapActiveSessionEhcacheMaxDiskSize, e);
            maxBytesOnLocalDisk = new MemoryUnitUtil().convertToBytes("100GB");
        }
        return CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                ImapFolder.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(1, EntryUnit.ENTRIES)
                .disk(maxBytesOnLocalDisk, MemoryUnit.B, false))  // disk backed not persistent
                .build();
    }

    private CacheConfiguration<String, ImapFolder> createImapInactiveSessionCache() {
        long maxBytesOnLocalDisk;
        long inactiveSessionCache;
        try {
            maxBytesOnLocalDisk = Provisioning.getInstance().getLocalServer().getImapInactiveSessionCacheMaxDiskSize();
        } catch (ServiceException e) {
            ZimbraLog.imap.error("Exception while fetching attribute %s", Provisioning.A_zimbraImapInactiveSessionCacheMaxDiskSize, e);
            maxBytesOnLocalDisk = new MemoryUnitUtil().convertToBytes("100GB");;
        }
        try {
            inactiveSessionCache = Provisioning.getInstance().getLocalServer().getImapInactiveSessionEhcacheSize();
        } catch (ServiceException e) {
            ZimbraLog.imap.error("Exception while fetching attribute %s", Provisioning.A_zimbraImapInactiveSessionEhcacheSize, e);
            inactiveSessionCache = new MemoryUnitUtil().convertToBytes("10MB");
        }
        int expiryMinutes = Math.max(LC.imap_folder_ehcache_expiry_mins.intValue(), 0);

        CacheConfigurationBuilder<String, ImapFolder> builder = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class,
                ImapFolder.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(LC.imap_ehcache_heap_size.intValue(), EntryUnit.ENTRIES)
                        .offheap(inactiveSessionCache, MemoryUnit.B)
                        .disk(maxBytesOnLocalDisk, MemoryUnit.B, true));

        if (expiryMinutes > 0) {
            builder = builder.withExpiry(Expirations.timeToLiveExpiration(
                    new Duration(expiryMinutes, TimeUnit.MINUTES)));
        }

        return builder.build();
    }

    private CacheConfiguration<String, ImapFolder> createImapInactiveLageFolderSessionCache() {
        long maxBytesOnLocalDisk;
        long inactiveSessionCache;
        try {
            maxBytesOnLocalDisk = Provisioning.getInstance().getLocalServer().getImapInactiveSessionCacheMaxDiskSize();
        } catch (ServiceException e) {
            ZimbraLog.imap.error("Exception while fetching attribute %s", Provisioning.A_zimbraImapInactiveSessionCacheMaxDiskSize, e);
            maxBytesOnLocalDisk = new MemoryUnitUtil().convertToBytes("100GB");;
        }
        try {
            inactiveSessionCache = Provisioning.getInstance().getLocalServer().getImapInactiveSessionEhcacheSize();
        } catch (ServiceException e) {
            ZimbraLog.imap.error("Exception while fetching attribute %s", Provisioning.A_zimbraImapInactiveSessionEhcacheSize, e);
            inactiveSessionCache = new MemoryUnitUtil().convertToBytes("10MB");
        }

        int expiryMinutes = Math.max(LC.imap_folder_ehcache_expiry_mins.intValue(), 0);

        CacheConfigurationBuilder<String, ImapFolder> builder = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                String.class,
                ImapFolder.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                        .heap(LC.imap_ehcache_heap_size.intValue(), EntryUnit.ENTRIES)
                        .offheap(inactiveSessionCache, MemoryUnit.B)
                        .disk(maxBytesOnLocalDisk, MemoryUnit.B, true))
                .withLoaderWriter(new NoOpWriter())
                .add(WriteBehindConfigurationBuilder
                        .newBatchedWriteBehindConfiguration(1, TimeUnit.SECONDS,
                                LC.imap_ehcache_write_behind_batch_size.intValue())
                        .useThreadPool("heavyWritePool")
                        .queueSize(LC.imap_ehcache_write_behind_queue_size.intValue())
                        .concurrencyLevel(LC.imap_ehcache_write_behind_concurrency_level.intValue())
                        .build());


        if (expiryMinutes > 0) {
            builder = builder.withExpiry(Expirations.timeToLiveExpiration(
                    new Duration(expiryMinutes, TimeUnit.MINUTES)));
        }

        return builder.build();
    }

    private CacheConfiguration<String, String> createActiveSyncStateItemCache() {
        long heapSize;
        long timeout;
        long diskSize;
        try {
            heapSize = Provisioning.getInstance().getLocalServer().getActiveSyncEhcacheHeapSize();
        } catch (ServiceException e) {
            ZimbraLog.sync.error("Exception while fetching attribute %s", Provisioning.A_zimbraActiveSyncEhcacheHeapSize, e);
            heapSize = new MemoryUnitUtil().convertToBytes("10MB");
        }
        try {
            timeout = Provisioning.getInstance().getLocalServer().getActiveSyncEhcacheExpiration();
        } catch (ServiceException e) {
            ZimbraLog.sync.error("Exception while fetching attribute %s", Provisioning.A_zimbraActiveSyncEhcacheExpiration, e);
            timeout = 5 * 60 * 1000; // 5 minutes in milliseconds
        }
        try {
            diskSize = Provisioning.getInstance().getLocalServer().getActiveSyncEhcacheMaxDiskSize();
        } catch (ServiceException e) {
            ZimbraLog.sync.error("Exception while fetching attribute %s", Provisioning.A_zimbraActiveSyncEhcacheMaxDiskSize, e);
            diskSize = new MemoryUnitUtil().convertToBytes("100GB");
        }

        return CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class,
                String.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                .heap(heapSize, MemoryUnit.B)
                .disk(diskSize, MemoryUnit.B, true)) // disk backed persistent store
                .withExpiry(Expirations.timeToLiveExpiration(Duration.of(timeout, TimeUnit.MILLISECONDS)))
                .build();
    }

    public Cache<String, ImapFolder> getEhcache(String cacheName) {
        return cacheManager.getCache(cacheName, String.class, ImapFolder.class);
    }

    public Cache<String, String> getSyncStateEhcache() {
        return cacheManager.getCache(SYNC_STATE_ITEM_CACHE, String.class, String.class);
    }

    public void shutdown() {
        cacheManager.close();
    }

    public static enum Service {
        MAILBOX("mailboxd"),
        IMAP("imap");
        private String val;

        private Service(String val) {
            this.val = val;
        }
    }
}
