/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.io.File;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.ZimbraLog;
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

    public static final String IMAP_ACTIVE_SESSION_CACHE = "imap-active-session-cache";
    public static final String IMAP_INACTIVE_SESSION_CACHE = "imap-inactive-session-cache";
    public static final String SYNC_STATE_ITEM_CACHE = "sync-state-item-cache";

    private EhcacheManager() {
        Configuration conf = new Configuration();
        DiskStoreConfiguration disk = new DiskStoreConfiguration();
        disk.setPath(LC.zimbra_home.value() + File.separator + "data" + File.separator + "mailboxd");
        conf.addDiskStore(disk);
        conf.addCache(createImapActiveSessionCache());
        if (MemcachedConnector.isConnected()) {
            ZimbraLog.imap.info("Using Memcached for inactive session cache");
        } else {
            conf.addCache(createImapInactiveSessionCache());
            conf.addCache(createSyncStateItemCache());
        }
        conf.setUpdateCheck(false);
        CacheManager.create(conf);
    }

    public static EhcacheManager getInstance() {
        return SINGLETON;
    }

    public void startup() {
    }

    private CacheConfiguration createImapActiveSessionCache() {
        CacheConfiguration conf = new CacheConfiguration();
        conf.setName(IMAP_ACTIVE_SESSION_CACHE);
        conf.setOverflowToDisk(true);
        conf.setDiskPersistent(false);
        conf.setMaxElementsInMemory(1); // virtually disk cache only
        conf.setMaxElementsOnDisk(0); // infinite, but essentially limited by max concurrent IMAP connections
        return conf;
    }

    private CacheConfiguration createImapInactiveSessionCache() {
        CacheConfiguration conf = new CacheConfiguration();
        conf.setName(IMAP_INACTIVE_SESSION_CACHE);
        conf.setOverflowToDisk(true);
        conf.setDiskPersistent(true);
        conf.setMaxElementsInMemory(1); // virtually disk cache only
        conf.setMaxElementsOnDisk(LC.imap_inactive_session_cache_size.intValue());
        return conf;
    }

    private CacheConfiguration createSyncStateItemCache() {
        CacheConfiguration conf = new CacheConfiguration();
        conf.setName(SYNC_STATE_ITEM_CACHE);
        conf.setOverflowToDisk(true);
        conf.setDiskPersistent(true);
        conf.setMaxBytesLocalHeap(LC.zimbra_activesync_syncstate_item_cache_heap_size.value());
        conf.setMaxElementsOnDisk(0); //infinite
        conf.setTimeToLiveSeconds(LC.zimbra_activesync_metadata_cache_expiration.intValue());
        return conf;
    }

    public void shutdown() {
        CacheManager.getInstance().shutdown();
    }
}
