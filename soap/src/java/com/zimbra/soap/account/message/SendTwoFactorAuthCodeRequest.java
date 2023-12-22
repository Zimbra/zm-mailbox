/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.account.message;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.AuthToken;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name= AccountConstants.E_SEND_TWO_FACTOR_AUTH_CODE_REQUEST)
public class SendTwoFactorAuthCodeRequest {

    public SendTwoFactorAuthCodeRequest() {

    }

    public SendTwoFactorAuthCodeRequest(SendTwoFactorAuthCodeAction action) {
        setAction(action);
    }

    public SendTwoFactorAuthCodeAction getAction() {
        return action;
    }

    public void setAction(SendTwoFactorAuthCodeAction action) {
        this.action = action;
    }

    @XmlElement(name=AccountConstants.E_ACTION)
    private SendTwoFactorAuthCodeAction action;


    @XmlElement(name=AccountConstants.E_AUTH_TOKEN /* authToken */, required=true)
    private AuthToken authToken;

    public AuthToken getAuthToken() {
        return authToken;
    }

    public SendTwoFactorAuthCodeRequest setAuthToken(AuthToken authToken) {
        this.authToken = authToken;
        return this;
    }

    @XmlEnum
    public enum SendTwoFactorAuthCodeAction {

        @XmlEnumValue("email") EMAIL("email"),
        @XmlEnumValue("reset") RESET("reset");
        private final String action;

        private SendTwoFactorAuthCodeAction(String action) {
            this.action = action;
        }

        @Override
        public String toString() {
            return action;
        }
    }
}