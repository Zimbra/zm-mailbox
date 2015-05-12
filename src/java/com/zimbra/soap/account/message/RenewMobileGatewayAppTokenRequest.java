/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.soap.account.message;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @zm-api-command-auth-required false
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description When the app auth token expires, the app can request a new auth token.
 */
@XmlRootElement(name = AccountConstants.E_RENEW_MOBILE_GATEWAY_APP_TOKEN_REQUEST)
public class RenewMobileGatewayAppTokenRequest {

    /**
     * @zm-api-field-tag app-id
     * @zm-api-field-description App ID
     */
    @ZimbraUniqueElement
    @XmlElement(name = AccountConstants.E_APP_ID /* appId */, required = true)
    private String appId;

    /**
     * @zm-api-field-tag app-key
     * @zm-api-field-description App secret key
     */
    @ZimbraUniqueElement
    @XmlElement(name = AccountConstants.E_APP_KEY /* appKey */, required = true)
    private String appKey;

    public RenewMobileGatewayAppTokenRequest(String appId, String appKey) {
        this.appId = appId;
        this.appKey = appKey;
    }

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RenewMobileGatewayAppTokenRequest() {
        this (null, null);
    }

    public String getAppId() {
        return appId;
    }

    public String getAppKey() {
        return appKey;
    }
}
