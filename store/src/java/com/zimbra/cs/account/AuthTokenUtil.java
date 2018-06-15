/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;

public class AuthTokenUtil {

    public static void encodeAuthResp(AuthToken at, Element parent, boolean isAdmin)  throws ServiceException {
        parent.addNonUniqueElement(isAdmin ? AdminConstants.E_AUTH_TOKEN : AccountConstants.E_AUTH_TOKEN).setText(getOrigAuthData(at));
    }

    public static String getOrigAuthData(AuthToken at) throws ServiceException {
        String origAuthData = null;
        try {
            origAuthData = at.getEncoded();
            if (origAuthData == null) {
                throw ServiceException.FAILURE("unable to get encoded auth token", null);
            }
        } catch (AuthTokenException e) {
            throw ServiceException.FAILURE("unable to get encoded auth token", e);
        }
        return origAuthData;
    }

    public static boolean isZimbraUser(String type) {
        return StringUtil.isNullOrEmpty(type) || AuthTokenProperties.C_TYPE_ZIMBRA_USER.equals(type) || AuthTokenProperties.C_TYPE_ZMG_APP.equals(type);
    }

    public static AuthTokenKey getCurrentKey() throws AuthTokenException {
        try {
            AuthTokenKey key = AuthTokenKey.getCurrentKey();
            return key;
        } catch (ServiceException e) {
            ZimbraLog.account.error("unable to get latest AuthTokenKey", e);
            throw new AuthTokenException("unable to get AuthTokenKey", e);
        }
    }
}
