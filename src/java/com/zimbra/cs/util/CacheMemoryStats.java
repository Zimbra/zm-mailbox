/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.cs.instrument.JavaAgent;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MessageCache;
import com.zimbra.cs.stats.ZimbraPerf;
import com.zimbra.cs.store.FileBlobStore;
import com.zimbra.cs.store.StoreManager;

/**
 * Real-time poller for cache memory size statistics
 */
public final class CacheMemoryStats implements RealtimeStatsCallback {

    private final static String MESSAGE_CACHE_SIZE = "msg_cache_sz";
    private final static String FD_CACHE_SIZE = "fd_cache_sz";
    private final static String MAILBOX_CACHE_SIZE = "mailbox_cache_sz";
    private final static String ACCOUNT_CACHE_SIZE = "acct_cache_sz";

    private static CacheMemoryStats sInstance = null;
    
    private final FileBlobStore blobStore;
    
    private final boolean hasJavaAgent;

    public static void shutdown() {
        sInstance.doShutdown();
        sInstance = null;
    }
    
    
    /**
     * Register us with the ZimbraPerf object so that we can log memory stats periodically to the stats file
     */
    public static void startup() {
        sInstance = new CacheMemoryStats();
    }


    private CacheMemoryStats() {
        ZimbraPerf.addStatsCallback(this);

        Object instance = StoreManager.getInstance();
        if (instance instanceof FileBlobStore) {
            ZimbraPerf.addRealtimeStatName(FD_CACHE_SIZE);
            blobStore = (FileBlobStore) instance;
        } else {
            blobStore = null;
        }
        
        boolean hasAgent = false;
        try {
            Class<?> c = Class.forName("com.zimbra.cs.instrument.JavaAgent");
            if (c != null)
                hasAgent = JavaAgent.isEnabled();
        }
        catch (Exception e) {
            hasAgent = false;
        }
        hasJavaAgent = hasAgent;
        ZimbraPerf.addRealtimeStatName(MAILBOX_CACHE_SIZE);
        ZimbraPerf.addRealtimeStatName(MESSAGE_CACHE_SIZE);
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.common.stats.RealtimeStatsCallback#getStatData()
     */
    public Map<String, Object> getStatData() {
        Map<String, Object> toRet = new HashMap<String, Object>();

        if (hasJavaAgent) {
            if (blobStore != null) {
                toRet.put(FD_CACHE_SIZE,
                        JavaAgent.deeplyInspectObjectSize(blobStore.getFileDescriptorCache()));
            }
            toRet.put(MESSAGE_CACHE_SIZE,
                    JavaAgent.deeplyInspectObjectSize(MessageCache.getBackingMap()));
            try {
                toRet.put(MESSAGE_CACHE_SIZE,
                        JavaAgent.deeplyInspectObjectSize(MailboxManager.getInstance()));
            }
            catch (ServiceException e) { } // ignore, unable to ascertain size
        } else {
            if (blobStore != null) {
                toRet.put(FD_CACHE_SIZE,
                        blobStore.getFileDescriptorCache().size());
            }
            toRet.put(MESSAGE_CACHE_SIZE,
                    MessageCache.getBackingMap().size());
            try {
                toRet.put(MAILBOX_CACHE_SIZE, MailboxManager.getInstance().getCacheSize());
            }
            catch (ServiceException e) { } // ignore, unable to ascertain size
        }
        
        return toRet;
    }

    private void doShutdown() { }

}
