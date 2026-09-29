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
import com.zimbra.soap.account.type.AuthToken;

/**
 * @zm-api-command-auth-required false
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Redeem a single-use ZCO exchange auth token for a normal session
 * auth token. The presented token must have been minted with usage <b>zco</b>; it is destroyed on
 * first redemption so it cannot be replayed.
 *
 * <pre>
 *   &lt;IdPAuthRequest xmlns="urn:zimbraAccount">
 *     &lt;authToken>{exchange-token}&lt;/authToken>
 *   &lt;/IdPAuthRequest>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_IDP_AUTH_REQUEST)
@XmlType(propOrder = {})
public class IdPAuthRequest {

    /**
     * @zm-api-field-description The single-use ZCO exchange auth token to redeem.
     */
    @XmlElement(name=AccountConstants.E_AUTH_TOKEN /* authToken */, required=true)
    private AuthToken authToken;

    public IdPAuthRequest() {
    }

    public IdPAuthRequest(AuthToken authToken) {
        this.authToken = authToken;
    }

    public AuthToken getAuthToken() { return authToken; }
    public IdPAuthRequest setAuthToken(AuthToken authToken) { this.authToken = authToken; return this; }
}
