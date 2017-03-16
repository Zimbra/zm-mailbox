/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.account.oauth.OAuthTokenCache;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.RevokeOAuthConsumerRequest;
import com.zimbra.soap.account.message.RevokeOAuthConsumerResponse;

public class RevokeOAuthConsumer extends AccountDocumentHandler {
    private String accessTokenToBeRemoved;

    private Account account;

    private static final Log LOG = ZimbraLog.extensions;

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        account = getRequestedAccount(zsc);

        RevokeOAuthConsumerRequest revokeRequest = zsc.elementToJaxb(request);
        accessTokenToBeRemoved = revokeRequest.getAccessToken();

        removeOAuthAccessor();
        removeAccessTokenFromForeignPrincipal();

        //Remove from cache
        OAuthTokenCache.remove(accessTokenToBeRemoved, OAuthTokenCache.ACCESS_TOKEN_TYPE);

        RevokeOAuthConsumerResponse response = new RevokeOAuthConsumerResponse();
        return zsc.jaxbToElement(response);
    }

    private void removeOAuthAccessor() throws ServiceException {
        String accessors[] = account.getOAuthAccessor();

        for (String val : accessors) {
            if (val.startsWith(accessTokenToBeRemoved)) {
                //Retrieve zauthtoken from accessor data
                String zauthToken = val.substring(val.indexOf(",zauthtoken"), val.indexOf(",verifier")).substring(12);
                try {
                    AuthToken token = ZimbraAuthToken.getAuthToken(zauthToken);
                    token.deRegister();
                } catch (AuthTokenException ate) {
                    LOG.error("In Revoke: Failed to deregister zimbra auth token.", ate);
                }
                account.removeOAuthAccessor(val);
                LOG.debug("OAuth accessor for access token %s is removed", accessTokenToBeRemoved);
                break;
            }
        }
    }

    private void removeAccessTokenFromForeignPrincipal() throws ServiceException {
        String data[] = account.getForeignPrincipal();

        for (String val : data) {
            //Access token is stored in foreign principle attribute in format "oAuthAccessToken:<token>"
            //e.g. oAuthAccessToken:91d6967c0b79b6b3c7f18891346d4d69
            if (val.startsWith("oAuthAccessToken") && val.split(":")[1].equals(accessTokenToBeRemoved)) {
                account.removeForeignPrincipal("oAuthAccessToken:" + accessTokenToBeRemoved);
                LOG.debug("oAuthAccessToken %s from foreign prinicple is removed", accessTokenToBeRemoved);
                break;
            }
        }
    }
}
