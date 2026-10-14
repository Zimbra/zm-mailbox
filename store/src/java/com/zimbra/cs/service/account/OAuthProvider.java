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

package com.zimbra.cs.service.account;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthToken.Usage;
import java.util.Map;

public interface OAuthProvider {

    /**
     * Generic token issuance for Native OAuth flows.
     *
     * @param acct      Target Zimbra account
     * @param usage     Usage.AUTH (access token) or Usage.REFRESH_TOKEN (refresh token)
     * @param request   Incoming SOAP AuthRequest element
     * @param response  SOAP response element (can be null when minting pre-response access token)
     * @param context   SOAP context map
     * @return Generated AuthToken instance
     */
    AuthToken issueToken(Account acct, Usage usage, Element request, Element response, Map<String, Object> context)
            throws ServiceException;

    /**
     * Handles refresh token exchange flow.
     *
     * @param acct      Target Zimbra account
     * @param request   Incoming SOAP AuthRequest element
     * @param context   SOAP context map
     * @return Element containing the new token and related information
     */
    Element handleRefresh(Account acct, Element request, Map<String, Object> context)
            throws ServiceException;
}
