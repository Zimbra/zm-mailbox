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

package com.zimbra.cs.account.auth.ropc.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.auth.PasswordUtil;
import com.zimbra.cs.account.auth.ropc.IRopcCredCache;
import com.zimbra.cs.account.auth.ropc.store.IRopcSessionRecord;
import com.zimbra.cs.account.auth.ropc.store.IRopcTokenStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.CONVERT_TO_MILLI;

/**
 * Utility methods for the ROPC authentication flow.
 */
public class IRopcUtil {

    public static Optional<Long> parseToMillis(String value) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .flatMap(s -> {
                    try {
                        return Optional.of(Long.parseLong(s) * CONVERT_TO_MILLI);
                    } catch (NumberFormatException e) {
                        return Optional.empty();
                    }
                })
                .filter(millis -> millis > 0);
    }

    public static Map<String, String> extractConfigsFromArgs(List<String> args) throws ServiceException {
        Map<String, String> configMap = new HashMap<>();

        try {
            if (args == null || args.isEmpty()) {
                return configMap;
            }

            for (String arg : args) {
                if (arg == null) {
                    continue;
                }
                String[] parts = arg.split("=", 2);

                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    if (key.isEmpty()) {
                        ZimbraLog.account.debug("Skipping configuration arguments with missing keys");
                        continue;
                    }
                    configMap.put(key, value);
                } else {
                    ZimbraLog.account.debug("Invalid Configuration arguments format. Expected 'key=value'");
                }
            }
            return configMap;
        } catch (Exception e) {
            ZimbraLog.account.error("Authentication failed while extracting args.", e);
            throw ServiceException.FAILURE("Authentication failed while extracting args .", null);
        }
    }

    public static IRopcSessionRecord findInStore(Account account, String user, String userAgent, String provider,
                                                 String proto, String deviceId, String ip, String password,
                                                 IRopcTokenStore store) throws ServiceException {
        List<IRopcSessionRecord> candidates = store.find(account, user, userAgent, provider, proto, deviceId);
        IRopcSessionRecord matchedRec = null;

        if (candidates != null) {
            for (IRopcSessionRecord candidate : candidates) {
                if (candidate.getPasswordHash() != null &&
                        PasswordUtil.SSHA512.verifySSHA512(candidate.getPasswordHash(), password)) {
                    matchedRec = candidate;
                    break;
                }
            }
        }

        return matchedRec;
    }

    /*
    * Completely removes all the authentication session data associated with a specific
    * device for a given account.
    * <p>
    * Thi method performs a synchronised cleanup across two stages layer
    * <ol>
    *   <li>It fetches existing session candidate to explicitly invalidate their corresponding
    * key in the in memory cache.</li>
    *   <li>It performs an optimized bulk deletion fo all associated records from teh underlying
    *  persistent database store.</li>
    * </ol>
    * </p>
    *
    * @param account
    * @param deviceId
    * @throws ServiceException
    */
    public static void removeDataForDevice(Account account, String deviceId) throws ServiceException {
        if (account == null || deviceId == null) {
            ZimbraLog.account.error("Error while flushing auth data from DB for user." +
                    "As provided account or device id is null");
            return;
        }

        try {
            List<IRopcSessionRecord> candidates = IRopcCredCache.STORE.findByDeviceIdAndUsername(account, deviceId);
            if (candidates != null && !candidates.isEmpty()) {
                for (IRopcSessionRecord candidate : candidates) {
                    // invalidate from in-memory cache
                    IRopcCredCache.invalidate(candidate.getUsername(), candidate.getUserAgent(),
                            candidate.getProtocol(), candidate.getProvider(), candidate.getIp(),
                            candidate.getDeviceId());
                }
                IRopcCredCache.STORE.deleteByDeviceIdAndUsername(account, deviceId);
            }
            ZimbraLog.account.info("Auth token data successfully flushed for user %s", account.getName());
        } catch (Exception e) {
            ZimbraLog.account.error("Error while flushing auth data from DB for user %s", account.getName(), e);
        }
    }

    /*
    * Normalizes the User-Agent string by stripping dynamic versioning and build dates.
    * This prevents OS or app store updates from breaking active MFA cache sessions.
    *
    * @param userAgent
    * @return normalizes userAgent
     */
    public static String normalizeUserAgent(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return "";
        }
        int slashIndex = userAgent.indexOf('/');
        if (slashIndex > -1) {
            return userAgent.substring(0, slashIndex);
        }
        return userAgent;
    }
}
