/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.io.File;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.memcached.MemcachedConnector;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.DiskStoreConfiguration;

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
        }
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
        conf.setDiskPersistent(true);
        conf.setTimeToIdleSeconds(LC.imap_authenticated_max_idle_time.intValue() + 5 * Constants.SECONDS_PER_MINUTE);
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

    public void shutdown() {
        CacheManager.getInstance().shutdown();
    }
}
