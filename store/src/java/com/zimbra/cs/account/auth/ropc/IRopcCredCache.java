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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.auth.PasswordUtil.SSHA512;
import java.util.concurrent.TimeUnit;

import static com.zimbra.cs.account.auth.ropc.IRopcConstants.TTL_MS;

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

    private static final int MAX_REJECTION_COUNT = 3;

    private static final int REJECTION_MAX_SIZE = 5000;

    /**
     * credential cache: device key or IP key to {@link CacheEntry}.
     * Guava TTL is a safety net only; real expiry is per-entry via
     * {@link CacheEntry#isExpired()}.
     */
    private static final Cache<String, CacheEntry> CRED_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(TTL_MS, TimeUnit.MILLISECONDS)
            .build();

    /**
     * rejection counter cache: device/IP key to rejection count.
     * Separate from credentials for type safety.
     * Resets on successful Okta auth via store().
     */
    private static final Cache<String, Integer> REJECTION_CACHE = CacheBuilder.newBuilder()
            .maximumSize(REJECTION_MAX_SIZE)
            .expireAfterWrite(TTL_MS, TimeUnit.MILLISECONDS)
            .build();

    private IRopcCredCache() {
    }

    /**
     * holds a salted password hash and its per-entry expiry timestamp.
     * expiry is derived from Okta's {@code expires_in} (seconds) at the time of store.
     * falls back to {@code TTL_MS} if {@code expires_in} is not provided.
     */
    private static final class CacheEntry {

        final String hash;

        /**
         * absolute wall-clock timestamp (ms) after which this entry is considered expired.
         * computed as: System.currentTimeMillis() + (expiresInSeconds * 1000)
         */
        final long expiryTimestamp;

        CacheEntry(String hash, long expiresInSeconds) {
            this.hash = hash;
            this.expiryTimestamp = System.currentTimeMillis() + (expiresInSeconds * 1000);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTimestamp;
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

        long ttl = (expiresInSeconds > 0)
                ? expiresInSeconds
                : TimeUnit.MILLISECONDS.toSeconds(TTL_MS);

        String effectivePassword = getEffectivePassword(password);
        String key = buildKey(email, userAgent, protocol, provider, ip, deviceId);

        // store the successful credential
        String saltedHash = SSHA512.generateSSHA512(effectivePassword, null);
        CacheEntry entry = new CacheEntry(saltedHash, ttl);
        CRED_CACHE.put(key, entry);

        // store IP key when deviceId is present
        if (isNotEmpty(deviceId) && isNotEmpty(ip)) {
            String ipKey = buildKey(email, userAgent, protocol, provider, ip, null);
            CRED_CACHE.put(ipKey, entry);
        }

        // Reset rejection count on successful Okta auth
        REJECTION_CACHE.invalidate(key);
        if (isNotEmpty(ip)) {
            String ipKey = buildKey(email, userAgent, protocol, provider, ip, null);
            REJECTION_CACHE.invalidate(ipKey);
        }

        ZimbraLog.account.debug(
                "IRopcCredCache: stored credential for %s, expires in %ds",
                email, ttl);
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
     * @return true if the cached credential matches and is not expired, false otherwise
     */
    public static boolean isValid(String email, String password, String userAgent,
            String protocol, String provider, String ip, String deviceId) {
        if (email == null || password == null) {
            return false;
        }
        String effectivePassword = getEffectivePassword(password);
        String deviceKey = buildKey(email, userAgent, protocol, provider, ip, deviceId);

        // 1. Direct hit — device key (Outlook) or IP key (native)
        CacheEntry entry = CRED_CACHE.getIfPresent(deviceKey);
        if (isMatch(entry, deviceKey, effectivePassword)) {
            ZimbraLog.account.debug("IRopcCredCache: direct cache hit for %s", email);
            return true;
        }

        // 2. IP bridge fallback — upgrade to device key (OPCC to real deviceId)
        if (isNotEmpty(deviceId) && isNotEmpty(ip)) {
            String ipKey = buildKey(email, userAgent, protocol, provider, ip, null);
            CacheEntry ipEntry = CRED_CACHE.getIfPresent(ipKey);

            if (isMatch(ipEntry, ipKey, effectivePassword)) {
                CRED_CACHE.put(deviceKey, ipEntry);
                CRED_CACHE.invalidate(ipKey);
                ZimbraLog.account.debug(
                        "IRopcCredCache: upgraded IP-based cache to DeviceId for %s", email);
                return true;
            }
        }
        return false;
    }

    /**
     * invalidates the cache for a specific device context (Logout).
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
        REJECTION_CACHE.invalidate(key);

        // Also remove the IP bridge entry to prevent fallback after invalidation
        if (isNotEmpty(ip)) {
            String ipBridgeKey = buildKey(email, userAgent, protocol, provider, ip, null);
            CRED_CACHE.invalidate(ipBridgeKey);
            REJECTION_CACHE.invalidate(ipBridgeKey);
        }

        ZimbraLog.account.info(
                "IRopcCredCache: invalidated session and rejection blocks for %s", email);
    }

    /**
     * records a push rejection for this context.
     *
     * @param email     the user's email address
     * @param userAgent the client's user agent string
     * @param protocol  the protocol being used
     * @param provider  the authentication provider
     * @param ip        the originating client IP address
     * @param deviceId  the unique device identifier
     */
    public static void storeRejection(String email, String userAgent, String protocol,
            String provider, String ip, String deviceId) {
        if (email == null) {
            return;
        }
        String key = buildKey(email, userAgent, protocol, provider, ip, deviceId);
        Integer val = REJECTION_CACHE.getIfPresent(key);
        int count = (val == null) ? 1 : val + 1;
        REJECTION_CACHE.put(key, count);
        ZimbraLog.account.info(
                "IRopcCredCache: recorded push rejection #%d for %s", count, email);
    }

    /**
     * checks if the user has exceeded the rejection limit.
     *
     * @param email     the user's email address
     * @param userAgent the client's user agent string
     * @param protocol  the protocol being used
     * @param provider  the authentication provider
     * @param ip        the originating client IP address
     * @param deviceId  the unique device identifier
     * @throws AuthFailedServiceException if the rejection limit is reached
     */
    public static void checkRejectionLimit(String email, String userAgent, String protocol,
            String provider, String ip, String deviceId) throws AuthFailedServiceException {
        if (email == null) {
            return;
        }
        String key = buildKey(email, userAgent, protocol, provider, ip, deviceId);
        Integer count = REJECTION_CACHE.getIfPresent(key);
        if (count != null && count >= MAX_REJECTION_COUNT) {
            ZimbraLog.account.warn(
                    "IRopcCredCache: auth blocked for %s;"
                            + " push rejection limit reached (%d)", email, count);
            throw AuthFailedServiceException.AUTH_FAILED(
                    email, "MFA push rejection limit reached");
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

    /**
     * validates entry against password. Immediately invalidates expired entries.
     */
    private static boolean isMatch(CacheEntry entry, String key, String password) {
        if (entry == null) {
            return false;
        }
        if (entry.isExpired()) {
            CRED_CACHE.invalidate(key); // remove immediately, don't wait for safety net
            return false;
        }
        try {
            return SSHA512.verifySSHA512(entry.hash, password);
        } catch (Exception e) {
            return false;
        }
    }

    private static String buildKey(String email, String ua, String proto,
            String prov, String ip, String did) {
        String normEmail = (email != null) ? email.trim().toLowerCase() : "unknown";
        StringBuilder sb = new StringBuilder(normEmail);
        sb.append('|').append(ua != null ? ua.hashCode() : "");
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
