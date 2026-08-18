/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.auth.ropc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.auth.PasswordUtil;
import com.zimbra.cs.account.auth.ropc.store.CacheRopcTokenStore;
import com.zimbra.cs.account.auth.ropc.store.DbRopcTokenStore;
import com.zimbra.cs.account.auth.ropc.store.IRopcSessionRecord;
import com.zimbra.cs.account.auth.ropc.store.IRopcTokenStore;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.CACHE_EXPIRY_MIN_DURATION;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.CACHE_GRACE_PERIOD_MIN_DURATION;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.FULL_AUTH;

/**
 * protocol-aware credential cache for IdP ROPC (MFA) authentication.
 *
 * <h3>design:</h3>
 * <p>Uses two caches: one for credentials, one for rejection tracking.</p>
 * <ul>
 *   <li><b>CRED_CACHE:</b> Stores {@link CacheEntry} (SSHA512 hash + per-entry expiry
 *       timestamp) keyed by device or IP context. No size limit — bounded naturally by
 *       TTL expiry. Guava TTL acts as a safety net; real expiry is driven by Okta's
 *       {@code expires_in} stored per entry.</li>
 *   <li><b>REJECTION_CACHE:</b> Tracks push rejection counts per device/IP context.
 *       Separate from credentials for type safety and isolation.</li>
 * </ul>
 *
 * <h3>Key format:</h3>
 * <ul>
 *   <li>With deviceId:    email|uaHash|protocol|provider|deviceId</li>
 *   <li>Without deviceId: email|uaHash|protocol|provider|ip (IP key)</li>
 * </ul>
 *
 * <h3>behaviors:</h3>
 * <ul>
 *   <li><b>IP-to-Device Upgrade:</b> Handles the transition from IP-only
 *       requests (like OPTIONS) to DeviceId-based requests (like SYNC).</li>
 *   <li><b>Single Device Logout:</b> Invalidation is context-aware.</li>
 *   <li><b>Anti-Flood:</b> Tracks push rejections in a separate REJECTION_CACHE.</li>
 *   <li><b>Per-entry expiry:</b> Each entry respects Okta's {@code expires_in}.
 *       Expired entries are invalidated immediately on access.</li>
 * </ul>
 */
public final class IRopcCredCache {

    private static final long GRACE_PERIOD = Duration.ofMinutes(
            Math.max(LC.mfa_idp_max_cache_grace_period_in_minutes.intValue(),
            CACHE_GRACE_PERIOD_MIN_DURATION)).toMillis();

    private static final long CACHE_TIMEOUT = Duration.ofMinutes(
            Math.max(LC.mfa_idp_max_cred_cache_timeout_in_minutes.intValue(),
            CACHE_EXPIRY_MIN_DURATION)).toMillis();

    private static final long REJECTION_CACHE_TIMEOUT = Duration.ofMinutes(
            Math.max(LC.mfa_idp_max_rejection_cache_timeout_in_minutes.intValue(),
                    CACHE_EXPIRY_MIN_DURATION)).toMillis();

    /**
     * Credential cache: device key or IP key to {@link CacheEntry}.
     * No size limit — entries are bounded naturally by TTL expiry.
     * Guava TTL is a safety net only; real expiry is per-entry via
     * {@link CacheEntry#isExpired()}.
     */
    private static final Cache<String, CacheEntry> CRED_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(CACHE_TIMEOUT
                    + GRACE_PERIOD, TimeUnit.MILLISECONDS)
            .build();

    /**
     * Rejection counter cache: device/IP key to rejection count.
     * Separate from credentials for type safety.
     * Resets on successful Okta auth via store().
     */
    private static final Cache<String, Integer> REJECTION_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite((REJECTION_CACHE_TIMEOUT), TimeUnit.MILLISECONDS)
            .build();

    public static final IRopcTokenStore STORE = LC.mfa_idp_enable_inmemory_store.booleanValue() ?
            new CacheRopcTokenStore() : new DbRopcTokenStore();

    private IRopcCredCache() {
    }

    /**
     * holds a salted password hash and its per-entry expiry timestamp.
     * expiry is derived from Okta's {@code expires_in} (seconds) at the time of store.
     * falls back to {@code TTL_MS} if {@code expires_in} is not provided.
     */
    private static final class CacheEntry {

        private final String hash;

        /**
         * absolute wall-clock timestamp (ms) after which this entry is considered expired.
         * computed as: System.currentTimeMillis() + (expiresInSeconds * 1000)
         */
        private final long expiryTimestamp;

        CacheEntry(String hash, long expiresInSeconds) {
            this.hash = hash;
            this.expiryTimestamp = System.currentTimeMillis() + (expiresInSeconds * 1000);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > (expiryTimestamp + GRACE_PERIOD);
        }

        boolean isActive() {
            return System.currentTimeMillis() < expiryTimestamp;
        }

        boolean inGracePeriod() {
            return System.currentTimeMillis() > expiryTimestamp &&
                    System.currentTimeMillis() < (expiryTimestamp + GRACE_PERIOD);
        }
    }

    /**
     * stores the successful credential in the cache and resets rejection counts.
     *
     * @param email            the user's email address
     * @param password         the user's password
     * @param userAgent        the client's user agent string
     * @param protocol         the protocol being used
     * @param provider         the authentication provider
     * @param ip               the originating client IP address
     * @param deviceId         the unique device identifier
     * @param expiresInSeconds token lifetime from Okta's expires_in (0 = use TTL_MS)
     */
    public static void store(String email, String password, String userAgent,
                             String protocol, String provider, String ip, String deviceId,
                             long expiresInSeconds) {
        if (email == null || password == null) {
            return;
        }

        String key = buildKey(email, userAgent, protocol, provider, ip, deviceId);

        CacheEntry entry = new CacheEntry(password, expiresInSeconds);
        CRED_CACHE.put(key, entry);
        CRED_CACHE.put(email, entry);
    }

    /**
     * validates the password against the cache. Handles IP-to-Device upgrades.
     * expired entries are invalidated immediately on access.
     *
     * @param email     the user's email address
     * @param password  the user's password
     * @param userAgent the client's user agent string
     * @param protocol  the protocol being used
     * @param provider  the authentication provider
     * @param ip        the originating client IP address
     * @param deviceId  the unique device identifier
     * @param account
     * @param optionsReq
     * @return CacheResponse
     */
    public static CacheResponse isValid(String email, String password, String userAgent,
                                        String protocol, String provider, String ip, String deviceId,
                                        Account account, boolean optionsReq) throws ServiceException {
        if (email == null || password == null) {
            return new CacheResponse(false);
        }
        String effectivePassword = getEffectivePassword(password);
        String deviceKey = buildKey(email, userAgent, protocol, provider, ip, deviceId);

        // 1. Direct hit — device key (Outlook) or IP key (native)
        CacheEntry entry = CRED_CACHE.getIfPresent(deviceKey);
        if (entry != null) {
            boolean passwordMatch = isMatch(entry, deviceKey, effectivePassword);
            if (entry.isActive()) {
                return passwordMatch ? new CacheResponse(true) : new CacheResponse(false);
            } else if (entry.inGracePeriod()) {
                return passwordMatch ? new CacheResponse(false, true) : new CacheResponse(false);
            } else if (entry.isExpired()) {
                CRED_CACHE.invalidate(deviceKey);
                return new CacheResponse(false);
            }
        }

        // 2. for OPTIONS request - first check in CRED_CRED with username as key
        // if not found lookup in db.
        // It will be validated only on the basis of username and password.
        if (optionsReq) {
            CacheEntry usernameEntry = CRED_CACHE.getIfPresent(email);
            if (usernameEntry != null) {
                return new CacheResponse(isMatch(usernameEntry, email, effectivePassword));
            }
            IRopcSessionRecord candidate = STORE.findLatestPasswordByUsername(account, email);

            if (candidate == null || candidate.getPasswordHash() == null) {
                return new CacheResponse(false);
            }

            CacheEntry cacheEntry = new CacheEntry(candidate.getPasswordHash(), (CACHE_TIMEOUT
                    + GRACE_PERIOD) / 1000);
            CRED_CACHE.put(email, cacheEntry);
            boolean isValidPassword = PasswordUtil.SSHA512.verifySSHA512(candidate.getPasswordHash(), password);
            return new CacheResponse(isValidPassword);
        }

        // 2. IP bridge fallback — upgrade to device key (OPCC to real deviceId)
        if (isNotEmpty(deviceId) && isNotEmpty(ip)) {
            String ipKey = buildKey(email, userAgent, protocol, provider, ip, null);
            CacheEntry ipEntry = CRED_CACHE.getIfPresent(ipKey);
            if (ipEntry != null) {
                boolean passwordMatch = isMatch(ipEntry, ipKey, effectivePassword);

                if (ipEntry.isActive() && passwordMatch) {
                    return processIpBridgeUpgrade(email, password, userAgent, protocol, provider,
                            ip, deviceId, account, optionsReq, deviceKey, ipEntry);
                }

                if (ipEntry.isExpired() && passwordMatch) {
                    CRED_CACHE.invalidate(ipKey);
                    return new CacheResponse(false);
                }
            }

        }
        return new CacheResponse(false);
    }

    private static CacheResponse processIpBridgeUpgrade(String email, String password, String userAgent,
                                                         String protocol, String provider, String ip, String deviceId,
                                                         Account account, boolean optionsReq, String deviceKey,
                                                         CacheEntry ipEntry) throws ServiceException {
        Integer count = REJECTION_CACHE.getIfPresent(email);
        if (count != null && count >= LC.mfa_idp_auth_fail_count.intValue()) {
            return new CacheResponse(false);
        }

        List<IRopcSessionRecord> candidates = STORE.findByIp(account, email, userAgent,
                provider, protocol, ip);
        if (candidates == null || candidates.isEmpty()) {
            CRED_CACHE.invalidate(buildKey(email, userAgent, protocol, provider, ip, null));
            return new CacheResponse(false);
        }
        IRopcSessionRecord recordToUpgrade = null;
        IRopcSessionRecord recordMached = null;
        for (IRopcSessionRecord candidate : candidates) {
            if (deviceId.equals(candidate.getDeviceId())) {
                recordMached = candidate;
            } else if (candidate.getDeviceId() == null) {
                if (candidate.getPasswordHash() != null &&
                        PasswordUtil.SSHA512.verifySSHA512(candidate.getPasswordHash(), password)) {
                    recordToUpgrade = candidate;
                }
            }
        }

        if (recordToUpgrade != null) {
            recordToUpgrade.setDeviceId(deviceId);
            STORE.updateDeviceId(account, recordToUpgrade);
            CRED_CACHE.put(deviceKey, ipEntry);
            CRED_CACHE.invalidate(buildKey(email, userAgent, protocol, provider, ip, null));
            ZimbraLog.account.debug(
                    "IRopcCredCache: upgraded IP-based cache to DeviceId for %s", email);
            return new CacheResponse(true);
        }

        if (recordMached != null) {
            return new CacheResponse(false, true);
        }
        // if ip is found in DB with this user, but device id does not match
        // rare edge case, will trigger full auth and it contains the actual device id as it's not a OPTIONS req
        // so save the device id in DB
        return new CacheResponse(false, FULL_AUTH);
    }

    /**
     * Invalidates the cache for a specific device context (Logout).
     *
     * @param email     the user's email address
     * @param userAgent the client's user agent string
     * @param protocol  the protocol being used
     * @param provider  the authentication provider
     * @param ip        the originating client IP address
     * @param deviceId  the unique device identifier
     */
    public static void invalidate(String email, String userAgent, String protocol,
                                  String provider, String ip, String deviceId) {
        if (email == null) {
            return;
        }
        String key = buildKey(email, userAgent, protocol, provider, ip, deviceId);
        CRED_CACHE.invalidate(key);

        // Also remove the IP bridge entry to prevent fallback after invalidation
        if (isNotEmpty(ip)) {
            String ipBridgeKey = buildKey(email, userAgent, protocol, provider, ip, null);
            CRED_CACHE.invalidate(ipBridgeKey);
        }

        CRED_CACHE.invalidate(email);

        ZimbraLog.account.info(
                "IRopcCredCache: invalidated session for %s", email);
    }

    /**
     * Clears rejection cache for a specific user.
     * @param email the user's email address
     * @return true if an entry existed and was removed
     */
    public static boolean invalidateRejectionCacheByUsername(String email) {
        if (email == null) {
            return false;
        }
        boolean exists = REJECTION_CACHE.getIfPresent(email) != null;
        REJECTION_CACHE.invalidate(email);

        ZimbraLog.account.info("IRopcCredCache: invalidated rejection cache for %s (existed=%b)", email, exists);
        return exists;
    }

    /**
     * Clears the entire rejection cache only.
     * Used by admin ClearRejectionCache SOAP handler.
     *
     * @return number of entries that were in the rejection cache before clearing
     */
    public static long invalidateAllRejectionCache() {
        long size = REJECTION_CACHE.size();
        REJECTION_CACHE.invalidateAll();

        ZimbraLog.account.info(
                "IRopcCredCache: cleared entire rejection cache. Entries removed: %d", size);
        return size;
    }

    /**
     * records a push rejection for this context.
     *
     * @param email the user's email address
     */
    public static void storeRejection(String email) {
        if (email == null) {
            return;
        }
        Integer val = REJECTION_CACHE.getIfPresent(email);
        int count = (val == null) ? 1 : val + 1;
        REJECTION_CACHE.put(email, count);
        ZimbraLog.account.debug(
                "IRopcCredCache: recorded auth failure , updated rejection cache with #%d for %s", count, email);
    }

    /**
     * checks if the user has exceeded the rejection limit.
     *
     * @param email     the user's email address
     * @throws AuthFailedServiceException if the rejection limit is reached
     */
    public static void checkRejectionLimit(String email)
            throws ServiceException {
        if (email == null) {
            return;
        }
        Integer count = REJECTION_CACHE.getIfPresent(email);
        if (count != null && count >= LC.mfa_idp_auth_fail_count.intValue()) {
            ZimbraLog.account.warn(
                    "IRopcCredCache: auth blocked for %s;"
                            + " auth rejection limit reached (%d)", email, count);
            throw ServiceException.FORBIDDEN("MFA auth rejection limit reached");
        }
    }

    /**
     * clears all caches. Used for testing only.
     */
    public static void clearAll() {
        CRED_CACHE.invalidateAll();
        REJECTION_CACHE.invalidateAll();
    }

    // Internal Helpers

    private static String getEffectivePassword(String passwordField) {
        return passwordField; // Future: strip TOTP here
    }

    private static boolean isMatch(CacheEntry entry, String key, String password) {
        return PasswordUtil.SSHA512.verifySSHA512(entry.hash, password);

    }

    private static String buildKey(String email, String ua, String proto,
                                   String prov, String ip, String did) {
        String normEmail = (email != null) ? email.trim().toLowerCase() : "unknown";
        StringBuilder sb = new StringBuilder(normEmail);
        sb.append('|').append(ua != null ? ua : "");
        sb.append('|').append(proto != null ? proto : "");
        sb.append('|').append(prov != null ? prov : "");
        if (isNotEmpty(did)) {
            sb.append('|').append(did);
        } else {
            sb.append('|').append(ip != null ? ip : "");
        }
        return sb.toString();
    }

    private static boolean isNotEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
