/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.auth.twofactor;

import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.TrustedDevice;
import com.zimbra.cs.account.TrustedDeviceToken;

public interface TrustedDevices {
    public TrustedDeviceToken registerTrustedDevice(Map<String, Object> deviceAttrs) throws ServiceException;
    public List<TrustedDevice> getTrustedDevices() throws ServiceException;
    public void revokeTrustedDevice(TrustedDeviceToken token) throws ServiceException;
    public void revokeAllTrustedDevices() throws ServiceException;
    public void revokeOtherTrustedDevices(TrustedDeviceToken token) throws ServiceException;
    public void verifyTrustedDevice(TrustedDeviceToken token, Map<String, Object> attrs) throws ServiceException;
    public TrustedDeviceToken getTokenFromRequest(Element request, Map<String, Object> context) throws ServiceException;
    public TrustedDevice getTrustedDeviceByTrustedToken(TrustedDeviceToken token) throws ServiceException;
}
