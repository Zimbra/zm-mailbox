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

import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AppSpecificPassword;

public interface AppSpecificPasswords extends SecondFactor {
    public boolean isEnabled() throws ServiceException;
    public AppSpecificPassword generatePassword(String name) throws ServiceException;
    public Set<AppSpecificPasswordData> getPasswords() throws ServiceException;
    public void revoke(String appName) throws ServiceException;
    public void revokeAll() throws ServiceException;
    public String getAppNameByPassword(String password) throws ServiceException;
}
