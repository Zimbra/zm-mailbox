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

package com.zimbra.soap.account.message;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper.
                add("appId", appId).
                add("appKey", appKey).
                add("authToken", authToken);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
