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
package com.zimbra.cs.imap;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;

/**
 * IMAP cache using Ehcache's DiskStore.
 *
 * @author ysasaki
 */
final class EhcacheImapCache implements ImapSessionManager.Cache {
    private final Ehcache ehcache;
    private final boolean active;
    private Map<String, Long> activeCacheUpdateTimes;
    private static final int ACTIVE_CACHE_THRESHOLD = 1000;

    @SuppressWarnings("serial")
    EhcacheImapCache(String name, boolean active) {
        ehcache = CacheManager.getInstance().getEhcache(name);
        this.active = active;
        if (active) {
            activeCacheUpdateTimes = new LinkedHashMap<String, Long>(ACTIVE_CACHE_THRESHOLD, 0.75f, true) {
                private boolean isExpired(long timestamp) {
                    return (timestamp < (System.currentTimeMillis() - (LC.imap_authenticated_max_idle_time.intValue() * Constants.MILLIS_PER_SECOND + 5 * Constants.MILLIS_PER_MINUTE)));
                }

                private boolean removeIfExpired(Entry<String, Long> entry) {
                    if (isExpired(entry.getValue())) {
                        doRemove(entry.getKey());
                        return true;
                    } else {
                        return false;
                    }
                }

                private void doRemove(String key) {
                    ZimbraLog.imap.debug("removing expired active cache element %s", key);
                    ehcache.remove(key);
                    remove(key);
                }

                @Override
                protected boolean removeEldestEntry(Entry<String, Long> eldest) {
                    if (removeIfExpired(eldest)) {
                        Set<String> keysToRemove = new HashSet<String>();
                        if (size() > ACTIVE_CACHE_THRESHOLD) {
                            //keep size under threshold when possible - can grow if there are many active sessions but will shrink as they 'expire'
                            for (Iterator<Entry<String, Long>> it = this.entrySet().iterator(); it.hasNext(); ) {
                                Entry<String, Long> entry = it.next();
                                if (isExpired(entry.getValue())) {
                                    keysToRemove.add(entry.getKey());
                                } else {
                                    break;
                                }
                            }
                            for (String key : keysToRemove) {
                                doRemove(key);
                            }
                        }
                    }
                    return false; //always return false per LinkedHashMap:
                    //It is permitted for this method to modify the map directly, but if it does so, it must return false
                }
            };
        }
    }

    @Override
    public void put(String key, ImapFolder folder) {
        if (active) {
            synchronized (activeCacheUpdateTimes) {
                if (!ehcache.isKeyInCache(key)) {
                    ehcache.put(new Element(key, folder));
                    ZimbraLog.imap.debug("put key %s",key);
                }
                long currentTime = System.currentTimeMillis();
                activeCacheUpdateTimes.put(key, currentTime);
            }
        } else {
            if (!ehcache.isKeyInCache(key)) {
                ehcache.put(new Element(key, folder));
            }
        }
    }

    @Override
    public ImapFolder get(String key) {
        try {
            Element el = null;
            if (active) {
                synchronized (activeCacheUpdateTimes) {
                    el = ehcache.get(key);
                    if (el != null) {
                        activeCacheUpdateTimes.put(key, System.currentTimeMillis());
                        ZimbraLog.imap.debug("got Element for key %s",key);
                    } else {
                        ZimbraLog.imap.debug("null get for key %s",key);
                    }
                }
            } else {
                el = ehcache.get(key);
            }
            return el != null ? (ImapFolder) el.getValue() : null;
        } catch (CacheException ce) {
            ZimbraLog.imap.error("IMAP cache exception - removing offending key", ce);
            remove(key, true);
            return null;
        }
    }

    private void remove(String key, boolean quiet) {
        try {
            if (active) {
                synchronized (activeCacheUpdateTimes) {
                    ZimbraLog.imap.debug("removing key %s",key);
                    activeCacheUpdateTimes.remove(key);
                }
            }
            ehcache.remove(key);
        } catch (CacheException ce) {
            if (!quiet) {
                ZimbraLog.imap.error("IMAP cache exception", ce);
            }
        }
    }

    @Override
    public void remove(String key) {
        remove(key, false);
    }

    @Override
    public void updateAccessTime(String key) {
        if (active) {
            synchronized (activeCacheUpdateTimes) {
                if (activeCacheUpdateTimes.containsKey(key)) {
                    activeCacheUpdateTimes.put(key, System.currentTimeMillis());
                } else {
                    ZimbraLog.imap.warn("active cache needed update but not found: %s",key);
                }
            }
        } else {
            //inactive expiration not time-based
        }
    }
}
