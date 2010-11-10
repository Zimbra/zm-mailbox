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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.LruMap;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.EntryCacheDataKey;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;

public class PermissionCache {
    
    enum CachedPermission {
        NOT_CACHED(null),
        ALLOWED(Boolean.TRUE),
        DENIED(Boolean.FALSE),
        NO_MATCHING_ACL(null);
        
        Boolean result;
        
        private CachedPermission(Boolean result) {
            this.result = result;
        }
        
        Boolean getResult() {
            assert(this != NOT_CACHED);
            return result;
        }
    }
    
    private static class PermCache {
        private long resetAt;
        Map<String, CachedPermission> cache;
        Entry target;
        
        /*
         * returns a PermCache for the entry
         */
        private static PermCache getPermCache(Entry target, boolean createIfNotExist) {
            String permCacheKey = EntryCacheDataKey.PERMISSION.getKeyName();
            PermCache permCache = (PermCache) target.getCachedData(permCacheKey);
            
            if (permCache == null && createIfNotExist) {
                permCache = new PermCache(target);
                target.setCachedData(permCacheKey, permCache);
            }
            return permCache;
        }
        
        private PermCache(Entry target) {
            this.target = target;
            reset();
        }
        
        private void reset() {
            resetAt = System.currentTimeMillis();
            if (cache == null)
                cache = MapUtil.newLruMap(getCacheSize(target)); 
            else
                cache.clear();
        }
        
        private static int getCacheSize(Entry target) {
            if (target instanceof Account)
                return LC.acl_cache_account_maxsize.intValue();
            else if (target instanceof DistributionList)
                return LC.acl_cache_group_maxsize.intValue();
            else 
                return LC.acl_cache_maxsize.intValue();
        }
        
        private boolean isExpired() {
            return resetAt <= invalidatedAt;
        }
        
        private synchronized void invalidate() {
            reset();
        }
        
        private synchronized CachedPermission get(String key) {
            if (isExpired()) {
                reset();
                return CachedPermission.NOT_CACHED;
            }
            
            CachedPermission perm = cache.get(key);
            return (perm == null) ? CachedPermission.NOT_CACHED : perm;
        }
        
        private synchronized void put(String key, CachedPermission perm) {
            cache.put(key, perm);
        }
    }

    // timestamp at which permission cache is invalidated
    // any permission cached prior to this time should be thrown away 
    private static long invalidatedAt;  
    
    /**
     * invalidate permission cache on all entries
     * 
     * Note: permission cache is invalidated only on the server on which the event is executed.
     *       The effect(how soon the event will actually change ACL checking result) is the same 
     *       with or without the permission cache. 
     */
    public static synchronized void invalidateCache() {
        invalidatedAt = System.currentTimeMillis();
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
     * Note: permission cache is invalidated only on the server on which the event is executed.
     *       This does not cause any behavior change 
     * @param target
     */
    public static void invalidateCache(Entry target) {
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
            String permCacheKey = EntryCacheDataKey.PERMISSION.getKeyName();
            PermCache permCache = (PermCache) target.getCachedData(permCacheKey);
            
            if (permCache != null)
                permCache.invalidate();
        }
    }
    
    static CachedPermission checkCache(Account grantee, Entry target, Right rightNeeded, boolean canDelegateNeeded) {
        CachedPermission perm = CachedPermission.NOT_CACHED;
        
        PermCache permCache = PermCache.getPermCache(target, false);
        
        if (permCache != null) {
            String cacheKey = buildCacheKey(grantee, rightNeeded, canDelegateNeeded);
            if (cacheKey != null) {
                perm = permCache.get(cacheKey);
            }
        }
        
        if (ZimbraLog.acl.isDebugEnabled()) {
            ZimbraLog.acl.debug("PermissionCache get: " + perm.toString() + 
                    " (target=" + target.getLabel() + ", grantee=" + grantee.getName() + 
                    ", right=" + rightNeeded.getName() + ", canDelegateNeeded=" + canDelegateNeeded + ")");
        }
        
        return perm;
    }

    static void cacheResult(Account grantee, Entry target, Right rightNeeded, boolean canDelegateNeeded, 
            Boolean allowed) {
        String cacheKey = buildCacheKey(grantee, rightNeeded, canDelegateNeeded);
        if (cacheKey == null) {
            // not cachable
            return;
        }
        
        PermCache permCache = PermCache.getPermCache(target, true);
        CachedPermission perm = (allowed == null) ? CachedPermission.NO_MATCHING_ACL :
            allowed.booleanValue() ? CachedPermission.ALLOWED : CachedPermission.DENIED;

        permCache.put(cacheKey, perm);
       
        if (ZimbraLog.acl.isDebugEnabled()) {
            ZimbraLog.acl.debug("PermissionCache put: " + perm.toString() + 
                    " (target=" + target.getLabel() + ", grantee=" + grantee.getName() + 
                    ", right=" + rightNeeded.getName() + ", canDelegateNeeded=" + canDelegateNeeded + ")");
        }
    }
    
    
    /*
     * returns cache key for entries on the map cached on the entry
     * cache key is in the format of
     * 
     * <GRANTEE-IDENTIFIER><ADMIN-FLAG><right-name><CAN-DELEDATE-NEEDED>
     * 
     * GRANTEE-IDENTIFIER         := <GUEST-ACCOUNT-BY-USER-PASS>|<GUEST-ACCOUNT-BY-ACCESSKEY>|<zimra-account-id>
     * 
     * GUEST-ACCOUNT-BY-USER-PASS := G<user-pass-digest>
     * 
     * GUEST-ACCOUNT-BY-ACCESSKEY := K<accesskey>
     * 
     * ADMIN-FLAG                 := <USER-FLAG>|<DELEGATED-ADMIN-FLAG>|<GLOABL-ADMIN-FLAG>
     * 
     * USER-FLAG                  := 0
     * 
     * DELEGATED-ADMIN-FLAG       := 1   
     * 
     * GLOABL-ADMIN-FLAG          := 2
     * 
     * CAN-DELEDATE-NEEDED        := 0 | 1
     * 
     * e.g.
     * (no space in between segments in the actual key)
     * d3a5c239-bac9-45ca-87b3-441a990c931b 0 invite 0 
     * Gcv30B19SfmLg1HYQd2CX4qZp908= 0 loginAs 0
     */
    private static String buildCacheKey(Account grantee, Right rightNeeded, boolean canDelegateNeeded) {
        //
        // to conserve caching slots, cache only user rights and the adminLoginAs admin right
        //
        if (!rightNeeded.isUserRight() && Admin.R_adminLoginAs != rightNeeded)
            return null;
        
        String id = null;
        if (grantee instanceof GuestAccount) {
            // note: do NOT use account id as part of the cache key for GuestAccount, 
            //       the account id is always 999...
            id = ((GuestAccount) grantee).getDigest();
            if (id != null) {
                id = "G" + id;
            } else {
                id = ((GuestAccount) grantee).getAccessKey();
                if (id != null) {
                    id = "K" + id;
                }
            }
        } else {
            id = grantee.getId();
        }
        
        if (id == null) {
            // for some weird reason, there is no identifier for the accessing account
            ZimbraLog.acl.debug("unable to build cache key: " + grantee.getName());
            return null;
        }
        
        char adminFlag = grantee.isIsAdminAccount() ? '2' : grantee.isIsDelegatedAdminAccount() ? '1' : '0';
        char canDelegate = canDelegateNeeded ? '1' : '0';
        return id + adminFlag + rightNeeded.getName() + canDelegate;
    }
    
}
