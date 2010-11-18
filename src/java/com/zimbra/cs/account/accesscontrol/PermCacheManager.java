/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.stats.Counter;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.LruMap;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.accesscontrol.PermissionCache.CachedPermission;

class PermCacheManager {

    /*
     * Permission cache is a two level LRU map with a TTL.
     * It caches ACL checking result for a target-credential-right permission check.
     * 
     * e.g. "key":   Does user1(credential) have the loginAs(right) right on user2(target)?
     *      "value": The cache lookup result is an instance of CachedPermission.
     * 
     * 
     * Two level cache lookup:
     * 
     * Cache entries are put into buckets, each bucket holds entries for a unique target.
     * e.g. all cache entries for target user1 are put into the same bucket.
     * 
     * This makes invalidating cache for one specific target easy and efficient.  
     * 
     * The first level is an LRU map:
     *   key:      target id
     *   value:    the "bucket" of cache entries for the target
     *   max size: LC key acl_cache_target_maxsize
     *   
     * The second level is also an LRU map (the "bucket" for each specific target):
     *   key:      credential of the accessing account
     *   value:    byte array.  One nibble (half byte) per cacheable rights.
     *             Currently there are about 7 cacheable rights, so 4 bytes in the byte array.
     *   max size: LC key acl_cache_credential_maxsize
     * 
     */
        
    private static final int ACL_CACHE_TARGET_MAXSIZE = LC.acl_cache_target_maxsize.intValue();
    private static final long ACL_CACHE_TARGET_MAXAGE = LC.acl_cache_target_maxage.intValue() * Constants.MILLIS_PER_MINUTE;
    private static final int ACL_CACHE_CREDENTIAL_MAXSIZE = LC.acl_cache_credential_maxsize.intValue();
    
    private static PermCacheManager theInstance = new PermCacheManager();
    
    private LruMap<String, PermCache> targetCache;
    
    private Counter hitRate = new Counter();
    
    // timestamp at which permission cache is invalidated
    // any permission cached prior to this time will be thrown away 
    private long invalidatedAt;  
    
    static PermCacheManager getInstance() {
        return theInstance;
    }
    
    private PermCacheManager() {
        targetCache = MapUtil.newLruMap(ACL_CACHE_TARGET_MAXSIZE);
        invalidateCache();
    }
    
    
    /**
     * invalidate permission cache on all entries
     * 
     * Note: permission cache is invalidated only on the server on which the permission 
     *       changing event is executed.
     */
    synchronized void invalidateCache() {
        invalidatedAt = System.currentTimeMillis();
        targetCache.clear();
    }
    
    /**
     * invalidate permission cache when a permission changing event happens on a target
     *   - invalidate permission cache on the specified entry if it is an entry from 
     *     which no right can be inherited
     *   - invalidate all permission cache otherwise
     * 
     * possible permission changing event:
     *   - granting/revoking rights 
     *   - adding/removing members on dl
     *   - renaming account/dl in/out a domain
     *   - that target is deleted
     *
     * Note: permission cache is invalidated only on the server on which the permission 
     *       changing event is executed.
     *
     * @param target
     */
    void invalidateCache(Entry target) {
        boolean invalidateAll = true; 
        
        try {
            invalidateAll = TargetType.canBeInheritedFrom(target);
        } catch (ServiceException e) {
            ZimbraLog.acl.debug("unable to determine if all permission cache should be invalidated, " + 
                    "invalidating permission cache on all entries", e);
        }
        
        if (invalidateAll) {
            invalidateCache();
        } else {
            PermCache permCache = getPermCache(target, false);
            
            if (permCache != null) {
                permCache.reset();
            }
        }
    }
    
    /*
     * returns a PermCache for the target
     */
    private synchronized PermCache getPermCache(Entry target, boolean createIfNotExist) {
        String cacheKey = getCacheKey(target);
        PermCache permCache = targetCache.get(cacheKey);
        
        if (permCache == null && createIfNotExist) {
            permCache = new PermCache();
            targetCache.put(cacheKey, permCache);
        }

        if (permCache != null) {
            permCache.resetIfExpired(invalidatedAt);
        }
        return permCache;
    }
    
    // TODO: refine
    private String getCacheKey(Entry target) {
        if (target instanceof NamedEntry)
            return ((NamedEntry) target).getId();
        else
            return target.getLabel(); 
    }
    
    private void updateHitRate(boolean hit) {
        hitRate.increment(hit ? 100 : 0);
    }
    
    double getHitRate() {
        return hitRate.getAverage();
    }
    
    CachedPermission get(Entry target, String key, Right right) {
        PermCache permCache = getPermCache(target, false);
        if (permCache == null) {
            updateHitRate(false);
            return CachedPermission.NOT_CACHED;
        }
        
        CachedPermission perm = permCache.get(key, right);
        updateHitRate(CachedPermission.NOT_CACHED != perm);
        return perm;
    }
    
    void put(Entry target, String key, Right right, CachedPermission perm) {
        PermCache permCache = getPermCache(target, true);
        permCache.put(key, right, perm);
    }
    
    /*
    synchronized void dump() {
        Iterator<Map.Entry<String, PermCache>> iEntries = targetCache.entrySet().iterator();
        while (iEntries.hasNext()) {
            Map.Entry<String, PermCache> entry = iEntries.next();
            String key = entry.getKey();
            PermCache permCache = entry.getValue();
            ZimbraLog.acl.debug("perm cache target: " + key);
        }
        System.out.println();
    }
    */
    
    // all methods can only be called from the PermCacheManager instance
    private static class PermCache {
        
        private long resetAt;
        
        private LruMap<String, byte[]> credentialToPermissionMap;;
        
        private PermCache() {
            credentialToPermissionMap = MapUtil.newLruMap(ACL_CACHE_CREDENTIAL_MAXSIZE);
            reset();
        }
                
        private synchronized boolean isExpired(long timestamp) {
            return (resetAt <= timestamp || 
                    resetAt + ACL_CACHE_TARGET_MAXAGE < System.currentTimeMillis());
        }
        
        private synchronized void reset() {
            // +1 so we will never get a timestamp that is the same as the timestamp 
            // when the cache is expired globally if we come from resetIfExpired.
            // is it ever possible?
            resetAt = System.currentTimeMillis() + 1;
            credentialToPermissionMap.clear();
        }
        
        private synchronized void resetIfExpired(long timestamp) {
            if (isExpired(timestamp)) {
                reset();
            }
        }

        private synchronized CachedPermission get(String credential, Right right) {
            byte[] cachedPerms = credentialToPermissionMap.get(credential);
            if (cachedPerms == null)
                return CachedPermission.NOT_CACHED;
            return CachedPerms.get(cachedPerms, right);
        }
        
        private synchronized void put(String credential, Right right, CachedPermission perm) {
            byte[] cachedPerms = credentialToPermissionMap.get(credential);
            if (cachedPerms == null) {
                cachedPerms = new byte[CachedPerms.getMaxPermArraySize()];
                credentialToPermissionMap.put(credential, cachedPerms);
            }
            CachedPerms.put(cachedPerms, right, perm);
        }

    }
    
    static class CachedPerms {
        private static final int RIGHTS_PER_BYTE = 2;  
        
        static final short MASK_NO_MATCHING_ACL = 0;
        static final short MASK_ALLOWED = 1;
        static final short MASK_DENIED = 2;
        
        private static final byte[][] MASKS_PERMS = {
            {0x10, 0x20, 0x40},
            {0x01, 0x02, 0x04}
        };
        
        private static final byte[] MASKS_CLEAR = {
            0x0F,
            0x70  // can't do F0, would overflow.  left-most bit is not used
        };
        
        static int getMaxPermArraySize() {
            return (Right.getMaxCacheIndex() / RIGHTS_PER_BYTE) + 1;
        }
        
        static CachedPermission get(byte[] cachedPerms, Right right) {
            int rightIdx = right.getCacheIndex();
            int idx = rightIdx / RIGHTS_PER_BYTE;
            byte byteForRight = cachedPerms[idx];
            int mask = rightIdx % RIGHTS_PER_BYTE;
            
            if ((MASKS_PERMS[mask][MASK_NO_MATCHING_ACL] & byteForRight) != 0)
                return CachedPermission.NO_MATCHING_ACL;
            else if ((MASKS_PERMS[mask][MASK_ALLOWED] & byteForRight) != 0)
                return CachedPermission.ALLOWED;
            else if ((MASKS_PERMS[mask][MASK_DENIED] & byteForRight) != 0)
                return CachedPermission.DENIED;
            else
                return CachedPermission.NOT_CACHED;
        }
        
        static void put(byte[] cachedPerms, Right right, CachedPermission perm) {
            int rightIdx = right.getCacheIndex();
            int idx = rightIdx / RIGHTS_PER_BYTE;
            byte byteForRight = cachedPerms[idx];
            int mask = rightIdx % RIGHTS_PER_BYTE;
            
            byteForRight &= MASKS_CLEAR[mask];
            byteForRight |= MASKS_PERMS[mask][perm.getCacheMask()];
            cachedPerms[idx] = byteForRight;
        }

    }

}
