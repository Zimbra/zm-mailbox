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
