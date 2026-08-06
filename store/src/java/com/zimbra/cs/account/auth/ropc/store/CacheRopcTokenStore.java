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

package com.zimbra.cs.account.auth.ropc.store;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.HARD_EXPIRY_DEFAULT;

public final class CacheRopcTokenStore implements IRopcTokenStore {

    private final Cache<String, List<IRopcSessionRecord>> tokenCache;

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    public CacheRopcTokenStore() {

        int expiryDays = LC.mfa_idp_hard_reauth_in_days.intValue();
        if (expiryDays <= 0) {
            expiryDays = HARD_EXPIRY_DEFAULT;
        }
        this.tokenCache = CacheBuilder.newBuilder()
                .expireAfterWrite(expiryDays, TimeUnit.DAYS)
                .build();
    }

    private String generateKey(String username, String userAgent, String provider, String protocol, String deviceId) {
        return userAgent + "|" + username + "|" + provider + "|"
                + protocol + "|"
                + (deviceId == null ? "" : deviceId);
    }

    @Override
    public List<IRopcSessionRecord> find(Account account, String username, String userAgent, String provider,
                                         String protocol, String deviceId) {
        return tokenCache.getIfPresent(generateKey(username, userAgent, provider, protocol, deviceId));
    }

    @Override
    public List<IRopcSessionRecord>  findByIp(Account account, String username, String userAgent, String provider,
                                              String protocol, String ip) {
        return tokenCache.getIfPresent(generateKey(username, userAgent, provider, protocol, ip));
    }

    @Override
    public void upsert(Account account, IRopcSessionRecord session) throws ServiceException {
        if (session == null) {
            throw ServiceException.INVALID_REQUEST("ROPC token session missing required fields", null);
        }

        if (session.getId() == null) {
            session.setId(ID_GENERATOR.getAndIncrement());
        }

        String routingValue = session.getDeviceId() != null ? session.getDeviceId() : session.getIp();
        String key = generateKey(session.getUsername(), session.getUserAgent(), session.getProvider(),
                session.getProtocol(), routingValue);

        tokenCache.asMap().compute(key, (k, existingList) -> {
            // first time key is used
            if (existingList == null) {
                List<IRopcSessionRecord> newList = new ArrayList<>();
                newList.add(session);
                return newList;
            }

            // the list exists.Find the exact record by id and update it
            boolean isUpdated = false;
            for (int i = 0; i < existingList.size(); i++) {
                if (existingList.get(i).getId().equals(session.getId())) {
                    existingList.set(i, session);
                    isUpdated = true;
                    break;
                }
            }

            // the list exist but this is a new session under the same deviceId/ip. Add it.
            if (!isUpdated) {
                if (session.getDeviceId() != null) {
                    existingList.clear();
                }
                existingList.add(session);
            }
            return existingList;
        });
    }

    @Override
    public void updateDeviceId(Account account, IRopcSessionRecord session)
            throws ServiceException {
        if (session == null || session.getId() == null || session.getDeviceId() == null) {
            return;
        }

        String oldKey =  generateKey(session.getUsername(), session.getUserAgent(), session.getProvider(),
                session.getProtocol(), session.getIp());

        final IRopcSessionRecord[] extractedRecord = new IRopcSessionRecord[1];
        tokenCache.asMap().computeIfPresent(oldKey, (k, existingList) -> {

            // find the exact record in the old list
            Iterator<IRopcSessionRecord> it = existingList.iterator();
            while (it.hasNext()) {
                IRopcSessionRecord record = it.next();
                if (record.getId().equals(session.getId())) {
                    extractedRecord[0] = record;
                    it.remove();
                    break;
                }
            }
            // if the oldbucket is now empty, return null to delete the key from the cache entirely
            return existingList.isEmpty() ? null : existingList;
        });

        if (extractedRecord[0] != null) {
            extractedRecord[0].setDeviceId(session.getDeviceId());
            extractedRecord[0].setLastUpdatedAt(session.getLastUpdatedAt());
            // upsert will generate a new key and insert the new data
            upsert(account, extractedRecord[0]);
        }
    }

    @Override
    public void delete(Account account, Long id, String username, String userAgent, String provider, String protocol,
                       String deviceId) throws ServiceException {
        if (id == null) {
            return;
        }
        String deviceKey = generateKey(username, userAgent, provider, protocol, deviceId);

        tokenCache.asMap().computeIfPresent(deviceKey, (k, existingList) -> {
            existingList.removeIf(record ->
                    record.getId() != null && record.getId().equals(id)
            );
            return existingList.isEmpty() ? null : existingList;
        });
        // deleting the ip based key too
        String ipKey = generateKey(username, userAgent, provider, protocol, deviceId);

        tokenCache.asMap().computeIfPresent(ipKey, (k, existingList) -> {
            existingList.removeIf(record ->
                    record.getId() != null && record.getId().equals(id)
            );
            return existingList.isEmpty() ? null : existingList;
        });
    }
}
