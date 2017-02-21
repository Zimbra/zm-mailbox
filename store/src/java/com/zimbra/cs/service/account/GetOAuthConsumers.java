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

import net.oauth.OAuthAccessor;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.oauth.OAuthAccessorSerializer;
import com.zimbra.cs.service.AuthProviderException;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.GetOAuthConsumersResponse;
import com.zimbra.soap.account.message.OAuthConsumer;

public class GetOAuthConsumers extends AccountDocumentHandler {
    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        GetOAuthConsumersResponse response = new GetOAuthConsumersResponse();
        encodeResponse(account, response);
        return zsc.jaxbToElement(response);
    }

    private void encodeResponse(Account account, GetOAuthConsumersResponse response) throws ServiceException {
        String[] accessors = account.getOAuthAccessor();
        OAuthAccessor accessor = null;
        for (String val : accessors) {
            try {
                String accessToken = val.substring(0, val.indexOf("::"));
                accessor = new OAuthAccessorSerializer().deserialize(val.substring(val.indexOf("::") + 2));
                OAuthConsumer zcsConsumer = createConsumer(accessToken, accessor);
                response.addConsumer(zcsConsumer);
            } catch (ServiceException e) {
                throw AuthProviderException.FAILURE("Error in deserializing OAuth accessor");
            }
        }
    }

    private OAuthConsumer createConsumer(String accessToken, OAuthAccessor accessor) {
        OAuthConsumer zcsConsumer = new OAuthConsumer();
        zcsConsumer.setAccessToken(accessToken);
        zcsConsumer.setApplicationName((String) accessor.consumer.getProperty("app_name"));
        zcsConsumer.setApprovedOn((String) accessor.consumer.getProperty("approved_on"));
        zcsConsumer.setDevice((String) accessor.consumer.getProperty("device"));
        return zcsConsumer;
    }
}
