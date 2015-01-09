/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account.zmg;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

import java.util.Map;

/**
 */
public class RenewMobileGatewayAppToken extends AccountDocumentHandler {

    /**
     * Returns whether the command's caller must be authenticated.
     */
    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return false;
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        String appUuid = request.getElement(AccountConstants.E_APP_ID).getTextTrim();
        String appKey = request.getElement(AccountConstants.E_APP_KEY).getTextTrim();

        String digest = AuthToken.generateDigest(appUuid, appKey);
        Account acct = Provisioning.getInstance().getAccountByForeignPrincipal("zmgappcreds:" + digest);
        if (acct == null) {
            ZimbraLog.account.debug("No mobile gateway app account exists for app id %s and app key %s",
                    appUuid, appKey);
            throw ServiceException.INVALID_REQUEST("Invalid credentials or app account not provisioned", null);
        }
        String authTokenEncoded;
        try {
            authTokenEncoded = AuthProvider.getAuthToken(acct).getEncoded();
        } catch (AuthTokenException e) {
            ZimbraLog.account.debug("Error in generating auth token", e);
            throw ServiceException.FAILURE("Error in generating auth token", null);
        }

        Element response = zsc.createElement(AccountConstants.RENEW_MOBILE_GATEWAY_APP_TOKEN_RESPONSE);
        Element eAuthToken = response.addUniqueElement(AccountConstants.E_AUTH_TOKEN);
        eAuthToken.addText(authTokenEncoded);
        eAuthToken.addAttribute(AccountConstants.E_LIFETIME, acct.getAuthTokenLifetime());
        return response;
    }
}
