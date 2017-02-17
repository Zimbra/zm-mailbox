/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.accesscontrol;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.GuestAccount;
import com.zimbra.cs.account.MailTarget;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.admin.type.CacheEntryType;

public class PermissionCache {

    private static boolean cacheEnabled = LC.acl_cache_enabled.booleanValue();

    enum CachedPermission {
        NOT_CACHED(null, (short)0),
        NO_MATCHING_ACL(null, PermCacheManager.CachedPerms.MASK_NO_MATCHING_ACL),
        ALLOWED(Boolean.TRUE, PermCacheManager.CachedPerms.MASK_ALLOWED),
        DENIED(Boolean.FALSE, PermCacheManager.CachedPerms.MASK_DENIED);

        private final Boolean result;
        private final short cacheMask;

        private CachedPermission(Boolean result, short cacheMask) {
            this.result = result;
            this.cacheMask = cacheMask;
        }

        Boolean getResult() {
            assert(this != NOT_CACHED);
            return result;
        }

        short getCacheMask() {
            return cacheMask;
        }
    }

    public static void invalidateCache() {
        RightBearer.Grantee.clearGranteeCache();
        RightCommand.clearAllEffectiveRightsCache();
        PermCacheManager.getInstance().invalidateCache();
    }

    /**
     * Invoked from the milter server/JVM.
     *
     * In addition to just invalidating the permission cache, we also need to
     * invalidate account, aclgroup, domain, and globalgrant LDAP entry caches
     * where the ACL is cached.
     * On mailbox server those caches are automatically updated when the
     * modification (grant, revoke, member changes, group/account/domain
     * creation/deletion etc) happens.
     */
    public static void invalidateAllCache() {
    	// clear all LDAP entry caches
        Provisioning prov = Provisioning.getInstance();
        try {
            prov.flushCache(CacheEntryType.all, null);
        } catch (ServiceException e) {
            ZimbraLog.acl.warn("unable to flush cache", e);
        }

        // clear the permission cache
        invalidateCache();
    }

    public static void invalidateCache(Entry target) {
        PermCacheManager.getInstance().invalidateCache(target);
    }

    public static double getHitRate() {
        return PermCacheManager.getInstance().getHitRate();
    }

    static CachedPermission cacheGet(MailTarget grantee, Entry target, Right rightNeeded, boolean canDelegateNeeded) {
        if (!cacheEnabled) {
            return CachedPermission.NOT_CACHED;
        }

        String cacheKey = buildCacheKey(grantee, rightNeeded, canDelegateNeeded);
        if (cacheKey == null) {
            // not cachable
            return CachedPermission.NOT_CACHED;
        }

        CachedPermission perm = PermCacheManager.getInstance().get(target, cacheKey, rightNeeded);

        if (ZimbraLog.acl.isDebugEnabled()) {
            ZimbraLog.acl.debug("PermissionCache get: " + perm.toString() +
                    " (target=" + target.getLabel() + ", grantee=" + grantee.getName() +
                    ", right=" + rightNeeded.getName() + ", canDelegateNeeded=" + canDelegateNeeded + ")");
        }

        return perm;
    }

    static void cachePut(MailTarget grantee, Entry target, Right rightNeeded, boolean canDelegateNeeded,
            Boolean allowed) {

        if (!cacheEnabled) {
            return;
        }

        String cacheKey = buildCacheKey(grantee, rightNeeded, canDelegateNeeded);
        if (cacheKey == null) {
            return;  // not cacheable
        }

        CachedPermission perm = (allowed == null) ? CachedPermission.NO_MATCHING_ACL :
            allowed.booleanValue() ? CachedPermission.ALLOWED : CachedPermission.DENIED;

        PermCacheManager.getInstance().put(target, cacheKey, rightNeeded, perm);

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
     * <GRANTEE-IDENTIFIER><ADMIN-FLAG><CAN-DELEDATE-NEEDED>
     *
     * GRANTEE-IDENTIFIER         := <GUEST-ACCOUNT-BY-USER-PASS>|<GUEST-ACCOUNT-BY-ACCESSKEY>|<zimra-account-id>
     *
     * GUEST-ACCOUNT-BY-USER-PASS := <user-pass-digest>G
     *
     * GUEST-ACCOUNT-BY-ACCESSKEY := <accesskey>K
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
     * d3a5c239-bac9-45ca-87b3-441a990c931b 0 0
     * cv30B19SfmLg1HYQd2CX4qZp908=G 0 0
     */
    static String buildCacheKey(MailTarget grantee, Right rightNeeded, boolean canDelegateNeeded) {

        if (!rightNeeded.isCacheable())
            return null;

        //
        // to conserve caching slots, cache only user rights and the adminLoginAs admin right
        // sanity check in case someone marks arbitrary admin rights cacheable in right xml files
        //
        if (!rightNeeded.isUserRight() && Admin.R_adminLoginAs != rightNeeded)
            return null;

        String id = null;
        if (grantee instanceof GuestAccount) {
            // note: do NOT use account id as part of the cache key for GuestAccount,
            //       the account id is always 999...
            // put "G"/"K" at the end (instead of the beginning) for better key distribution for the hash
            id = ((GuestAccount) grantee).getDigest();
            if (id != null) {
                id = id + "G";
            } else {
                id = ((GuestAccount) grantee).getAccessKey();
                if (id != null) {
                    id = id + "K";
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

        char adminFlag;
        if (grantee instanceof Account) {
            Account granteeAcct = (Account) grantee;
            adminFlag = granteeAcct.isIsAdminAccount() ? '2' : granteeAcct.isIsDelegatedAdminAccount() ? '1' : '0';
        } else {
            adminFlag = '0';
        }
        char canDelegate = canDelegateNeeded ? '1' : '0';
        return id + adminFlag + canDelegate;
    }

}
