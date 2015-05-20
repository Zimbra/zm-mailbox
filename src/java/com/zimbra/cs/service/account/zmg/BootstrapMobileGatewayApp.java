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
import com.zimbra.common.util.UUIDUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ZimbraAuthToken;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.service.account.AccountDocumentHandler;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.message.BootstrapMobileGatewayAppRequest;
import com.zimbra.soap.account.message.BootstrapMobileGatewayAppResponse;
import org.apache.commons.codec.binary.Hex;

import java.security.SecureRandom;
import java.util.Map;

/**
 */
public class BootstrapMobileGatewayApp extends AccountDocumentHandler {

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

        BootstrapMobileGatewayAppRequest req = JaxbUtil.elementToJaxb(request);

        String appUuid = UUIDUtil.generateUUID();

        // Generate a secret for the app
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[20];
        random.nextBytes(keyBytes);
        String key = new String(Hex.encodeHex(keyBytes));

        BootstrapMobileGatewayAppResponse resp = new BootstrapMobileGatewayAppResponse(appUuid, key);

        if (req.getWantAppToken()) {
            // Return an "anticipatory auth token" to the app

            Provisioning prov = Provisioning.getInstance();
            Domain domain = prov.getDefaultZMGDomain();
            if (domain == null) {
                ZimbraLog.misc.info("zimbraMobileGatewayDefaultAppAccountDomainId has not been configured.");
                throw ServiceException.FORBIDDEN("Operation disallowed");
            }

            // Generate an account id that would be used at a later point in time to create account for the app
            String appAccountId = LdapUtil.generateUUID();

            // This would be used whenever we need to identify the account using app credentials (appUuid & key)
            String appIdentifier = AuthToken.generateDigest(appUuid, key);

            long tokenLifetime = prov.getDefaultCOS(domain).getAuthTokenLifetime();
            long tokenExpires = System.currentTimeMillis() + tokenLifetime;
            AuthToken authToken = new ZimbraAuthToken(appAccountId, true, null, null, appIdentifier, tokenExpires);
            String authTokenEncoded;
            try {
                authTokenEncoded = authToken.getEncoded();
            } catch (AuthTokenException e) {
                throw ServiceException.FAILURE("Error generating auth token for app", e);
            }

            com.zimbra.soap.account.type.AuthToken jaxbToken =
                    new com.zimbra.soap.account.type.AuthToken(authTokenEncoded, null);
            jaxbToken.setLifetime(tokenLifetime);
            resp.setAuthToken(jaxbToken);
        }

        return zsc.jaxbToElement(resp);
    }
}
