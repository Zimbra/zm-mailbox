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
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.RenewMobileGatewayAppTokenRequest;
import com.zimbra.soap.account.message.RenewMobileGatewayAppTokenResponse;

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
        RenewMobileGatewayAppTokenRequest req = JaxbUtil.elementToJaxb(request);
        String appUuid = req.getAppId();
        String appKey = req.getAppKey();

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

        com.zimbra.soap.account.type.AuthToken jaxbToken =
                new com.zimbra.soap.account.type.AuthToken(authTokenEncoded, null);
        jaxbToken.setLifetime(acct.getAuthTokenLifetime());
        RenewMobileGatewayAppTokenResponse resp = new RenewMobileGatewayAppTokenResponse(jaxbToken);
        return zsc.jaxbToElement(resp);
    }
}
