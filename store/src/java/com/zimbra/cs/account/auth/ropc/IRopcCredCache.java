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

    /**
     * Grace period in milliseconds, applied after a token's nominal expiry, during which
     * cached credentials remain usable. Derived from
     * {@link LC#mfa_idp_max_cache_grace_period_in_minutes}, bounded below by
     * {@link IRopcConstants#CACHE_GRACE_PERIOD_MIN_DURATION}.
     */
    private static final long GRACE_PERIOD = Duration.ofMinutes(
            Math.max(LC.mfa_idp_max_cache_grace_period_in_minutes.intValue(),
                    CACHE_GRACE_PERIOD_MIN_DURATION)).toMillis();

    /**
     * Maximum credential cache TTL in milliseconds. Used as the Guava write-expiry
     * for {@link #CRED_CACHE} (together with {@link #GRACE_PERIOD}). Derived from
     * {@link LC#mfa_idp_max_cred_cache_timeout_in_minutes}, bounded below by
     * {@link IRopcConstants#CACHE_EXPIRY_MIN_DURATION}.
     */
    private static final long CACHE_TIMEOUT = Duration.ofMinutes(
            Math.max(LC.mfa_idp_max_cred_cache_timeout_in_minutes.intValue(),
                    CACHE_EXPIRY_MIN_DURATION)).toMillis();

    /**
     * Maximum rejection cache TTL in milliseconds. Used as the Guava write-expiry
     * for {@link #REJECTION_CACHE}. Derived from
     * {@link LC#mfa_idp_max_rejection_cache_timeout_in_minutes}, bounded below by
     * {@link IRopcConstants#CACHE_EXPIRY_MIN_DURATION}.
     */
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

    /**
     * Backing token store used for persistent session lookups and upgrades.
     * Resolved at class-load time: uses an in-memory {@link CacheRopcTokenStore}
     * when {@link LC#mfa_idp_enable_inmemory_store} is {@code true}, otherwise
     * uses the database-backed {@link DbRopcTokenStore}.
     */
    public static final IRopcTokenStore STORE = LC.mfa_idp_enable_inmemory_store.booleanValue() ?
            new CacheRopcTokenStore() : new DbRopcTokenStore();

    private IRopcCredCache() {
    }

    /**
     * Holds a salted password hash and its per-entry expiry timestamp.
     * Expiry is derived from Okta's {@code expires_in} (seconds) at the time of store.
     * Falls back to {@code CACHE_TIMEOUT + GRACE_PERIOD} if {@code expires_in} is not provided.
     */
    private static final class CacheEntry {

        private final String hash;

        /**
         * Absolute wall-clock timestamp (ms) after which this entry is considered expired.
         * Computed as: {@code System.currentTimeMillis() + (expiresInSeconds * 1000)}.
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
     * Stores a successfully authenticated credential in the cache and resets any
     * existing rejection count for the given context.
     *
     * <p>Two entries are written: one keyed by the full device/IP context key
     * (see class-level key format) and one keyed by {@code email} alone, to support
     * OPTIONS-request lookups that carry no device context.</p>
     *
     * @param email            the user's email address
     * @param password         the SSHA512-hashed password to cache
     * @param userAgent        the client's User-Agent string
     * @param protocol         the protocol being used (e.g., {@code EAS}, {@code IMAP})
     * @param provider         the authentication provider identifier
     * @param ip               the originating client IP address
     * @param deviceId         the unique device identifier; may be {@code null} for IP-only contexts
     * @param expiresInSeconds token lifetime in seconds from Okta's {@code expires_in};
     *                         pass {@code 0} to fall back to the configured cache TTL
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
     * Validates the password against the cache for the given authentication context.
     *
     * <p>Lookup order: (1) direct device/IP key hit, (2) OPTIONS — email key then
     * backing store, (3) IP bridge upgrade via {@link #processIpBridgeUpgrade}.</p>
     *
     * @param email      user's email address
     * @param password   plaintext password to verify against the cached hash
     * @param userAgent  client User-Agent string
     * @param protocol   protocol in use (e.g., {@code EAS}, {@code IMAP})
     * @param provider   authentication provider identifier
     * @param ip         originating client IP address
     * @param deviceId   unique device identifier; {@code null} for IP-only contexts
     * @param account    resolved {@link Account} for the authenticating user
     * @param optionsReq {@code true} if this is an OPTIONS pre-flight request
     * @return {@link CacheResponse} indicating hit, grace-period, or miss
     * @throws ServiceException if a store lookup or device-ID upgrade fails
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

    /**
     * Promotes an IP-keyed cache entry to a device-keyed entry when a client
     * presents a {@code deviceId} for the first time after an OPTIONS pre-flight.
     * Returns a grace-period response if the device is already known, or
     * {@link IRopcConstants#FULL_AUTH} if no matching record is found.
     *
     * @param email      user's email address
     * @param password   plaintext password presented by the client
     * @param userAgent  client User-Agent string
     * @param protocol   protocol in use
     * @param provider   authentication provider identifier
     * @param ip         originating client IP address
     * @param deviceId   device identifier now presented by the client
     * @param account    resolved {@link Account} for the authenticating user
     * @param optionsReq {@code true} if this is an OPTIONS request
     * @param deviceKey  fully-qualified device cache key for the current context
     * @param ipEntry    existing IP-keyed {@link CacheEntry} to be promoted
     * @return {@link CacheResponse} reflecting the upgrade outcome
     * @throws ServiceException if a store lookup or update fails
     */
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
     * Invalidates cache entries for the given device context (e.g., on logout).
     * Also evicts the IP-bridge key and the email-keyed entry.
     *
     * @param email     user's email address
     * @param userAgent client User-Agent string
     * @param protocol  protocol in use
     * @param provider  authentication provider identifier
     * @param ip        originating client IP address
     * @param deviceId  unique device identifier; {@code null} for IP-only contexts
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
     * Clears the rejection cache entry for the given user.
     *
     * @param email user's email address
     * @return {@code true} if an entry existed and was removed
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
     * Clears the entire rejection cache. Used by the admin ClearRejectionCache SOAP handler.
     * Does not affect the credential cache.
     *
     * @return number of entries removed
     */
    public static long invalidateAllRejectionCache() {
        long size = REJECTION_CACHE.size();
        REJECTION_CACHE.invalidateAll();

        ZimbraLog.account.info(
                "IRopcCredCache: cleared entire rejection cache. Entries removed: %d", size);
        return size;
    }

    /**
     * Increments the push-rejection counter for the given user.
     * Once the counter reaches {@link LC#mfa_idp_auth_fail_count}, further
     * attempts are blocked by {@link #checkRejectionLimit(String)}.
     *
     * @param email user's email address
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
     * Throws {@link ServiceException#FORBIDDEN} if the user's rejection count
     * meets or exceeds {@link LC#mfa_idp_auth_fail_count}.
     *
     * @param email user's email address
     * @throws ServiceException if the rejection limit has been reached
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
