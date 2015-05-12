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

import com.google.common.base.Objects;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.AuthToken;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = AccountConstants.E_BOOTSTRAP_MOBILE_GATEWAY_APP_RESPONSE)
public class BootstrapMobileGatewayAppResponse {

    /**
     * @zm-api-field-tag app-id
     * @zm-api-field-description Unique app ID for the app
     */
    @ZimbraUniqueElement
    @XmlElement(name = AccountConstants.E_APP_ID, required=true)
    private final String appId;

    /**
     * @zm-api-field-tag app-key
     * @zm-api-field-description an app key (or a secret) to enable the app to authenticate itself in the future
     */
    @ZimbraUniqueElement
    @XmlElement(name = AccountConstants.E_APP_KEY, required = true)
    private final String appKey;

    /**
     * @zm-api-field-tag auth-token
     * @zm-api-field-description "Anticipatory" app account auth token
     */
    @ZimbraUniqueElement
    @XmlElement(name = AccountConstants.E_AUTH_TOKEN, required = false)
    private AuthToken authToken;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private BootstrapMobileGatewayAppResponse() {
        this(null, null);
    }

    public BootstrapMobileGatewayAppResponse(String appId, String appKey) {
        this.appId = appId;
        this.appKey = appKey;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppKey() {
        return appKey;
    }

    public AuthToken getAuthToken() {
        return authToken;
    }

    public void setAuthToken(AuthToken authToken) {
        this.authToken = authToken;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper.
                add("appId", appId).
                add("appKey", appKey).
                add("authToken", authToken);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
