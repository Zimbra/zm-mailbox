/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.service;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Strings;
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.ZimbraAuthToken;

/**
 * @author pshao
 */
public class ZimbraOAuthProvider extends ZimbraAuthProvider {

    public static final String ZIMBRA_OAUTH_PROVIDER = "oauth";

    public static final String OAUTH_ACCESS_TOKEN = "access_token";

    protected ZimbraOAuthProvider() {
        super(ZIMBRA_OAUTH_PROVIDER);
    }

    @Override
    protected AuthToken authToken(HttpServletRequest req, boolean isAdminReq)
            throws AuthProviderException, AuthTokenException {
        return null;
    }

    @Override
    protected AuthToken authToken(Element soapCtxt, Map engineCtxt)
            throws AuthProviderException, AuthTokenException {
        return null;
    }

    @Override
    protected AuthToken authToken(Element authTokenElem, Account acct)
    throws AuthProviderException, AuthTokenException {
        ZAuthToken zAuthToken;
        try {
            zAuthToken = new ZAuthToken(authTokenElem, false);
        } catch (ServiceException e) {
            throw AuthProviderException.FAILURE(e.getMessage());
        }

        if (ZIMBRA_OAUTH_PROVIDER.equals(zAuthToken.getType())) {
            Map<String, String> attrs = zAuthToken.getAttrs();

            // TODO: no validation of access_token in IronMaiden D4!!!
            String accessToken = attrs.get(OAUTH_ACCESS_TOKEN);
            if (Strings.isNullOrEmpty(accessToken)) {
                throw new AuthTokenException("no oauth access token");
            }

            return authToken(acct);
        } else {
            throw AuthProviderException.NO_AUTH_DATA();
        }
    }

}
