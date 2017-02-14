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
import com.zimbra.soap.type.ZmBoolean;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @zm-api-command-auth-required false
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description This request is used by a mobile gateway app/client to bootstrap/initialize itself.
 */
@XmlRootElement(name = AccountConstants.E_BOOTSTRAP_MOBILE_GATEWAY_APP_REQUEST)
public class BootstrapMobileGatewayAppRequest {

    /**
     * @zm-api-field-tag want-app-token
     * @zm-api-field-description Whether an "anticipatory app account" auth token is desired.<br />
     *     Default is false.
     */
    @XmlAttribute(name = AccountConstants.A_WANT_APP_TOKEN /* wantAppToken */, required = false)
    private ZmBoolean wantAppToken;

    public boolean getWantAppToken() {
        return ZmBoolean.toBool(wantAppToken, false);
    }

    public void setWantAppToken(ZmBoolean wantAppToken) {
        this.wantAppToken = wantAppToken;
    }
}
