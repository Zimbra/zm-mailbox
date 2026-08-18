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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import java.util.List;

/**
 * Persistence contract for IdP token records.
 */
public interface IRopcTokenStore {
    List<IRopcSessionRecord> find(Account account, String username, String userAgent, String provider, String protocol,
                                  String deviceId) throws ServiceException;

    List<IRopcSessionRecord> findByIp(Account account, String username, String userAgent, String provider,
                                      String protocol, String ip) throws ServiceException;

    List<IRopcSessionRecord> findByDeviceIdAndUsername(Account account, String deviceId) throws ServiceException;

    IRopcSessionRecord findLatestPasswordByUsername(Account account, String username) throws ServiceException;

    void upsert(Account account, IRopcSessionRecord session) throws ServiceException;

    void updateDeviceId(Account account, IRopcSessionRecord session) throws ServiceException;

    void delete(Account account, Long id, String username, String userAgent, String provider, String protocol,
                String deviceId) throws ServiceException;

    void deleteByDeviceIdAndUsername(Account account, String deviceId) throws ServiceException;
}
