/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

/**
 * @zm-api-response-description Response to a ZCO exchange-token redemption request.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_IDP_AUTH_RESPONSE)
@XmlType(propOrder = {})
public class IdPAuthResponse {

    /**
     * @zm-api-field-description The newly issued session authorization token.
     */
    @XmlElement(name=AccountConstants.E_AUTH_TOKEN /* authToken */, required=true)
    private String authToken;

    /**
     * @zm-api-field-description Lifetime, in milliseconds, of the newly issued session token.
     */
    @ZimbraJsonAttribute
    @XmlElement(name=AccountConstants.E_LIFETIME /* lifetime */, required=true)
    private long lifetime;

    public IdPAuthResponse() {
    }

    public IdPAuthResponse(String authToken, long lifetime) {
        this.authToken = authToken;
        this.lifetime = lifetime;
    }

    public String getAuthToken() { return authToken; }
    public IdPAuthResponse setAuthToken(String authToken) { this.authToken = authToken; return this; }

    public long getLifetime() { return lifetime; }
    public IdPAuthResponse setLifetime(long lifetime) { this.lifetime = lifetime; return this; }
}
