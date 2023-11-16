/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016, 2023 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.AuthToken;
import com.zimbra.soap.type.ZmBoolean;

@XmlRootElement(name=AccountConstants.E_ENABLE_TWO_FACTOR_AUTH_REQUEST)
@XmlType(propOrder = {})
public class EnableTwoFactorAuthRequest {

    public EnableTwoFactorAuthRequest() {}

    /**
     * @zm-api-field-description The name of the account for which to enable two-factor auth
     */
    @XmlElement(name=AccountConstants.E_NAME, required=true)
    private String acctName;

    /**
     * @zm-api-field-description Password to use in conjunction with an account
     */
    @XmlElement(name=AccountConstants.E_PASSWORD, required=false)
    private String password;

    /**
     * @zm-api-field-description Auth token issued during the first 2FA enablement step
     */
    @XmlElement(name=AccountConstants.E_AUTH_TOKEN, required=false)
    private AuthToken authToken;

    /**
     * @zm-api-field-description Method to be used for 2FA
     */
    @XmlElement(name=AccountConstants.E_METHOD, required=false)
    private String method;

    /**
     * @zm-api-field-description Email of account to be used for 2FA
     */
    @XmlElement(name=AccountConstants.E_EMAIL, required=false)
    private String email;

    @XmlElement(name=AccountConstants.E_TWO_FACTOR_CODE, required=false)
    private String twoFactorCode;

    /**
     * @zm-api-field-description Whether the client supports the CSRF token
     */
    @XmlAttribute(name=AccountConstants.A_CSRF_SUPPORT, required=false)
    private ZmBoolean csrfSupported;

    public String getPassword() { return password; }
    public EnableTwoFactorAuthRequest setPassword(String password) { this.password = password; return this; }

    public String getName() { return acctName; }
    public EnableTwoFactorAuthRequest setName(String acctName) { this.acctName = acctName; return this; }

    public String getTwoFactorCode() { return twoFactorCode; }
    public EnableTwoFactorAuthRequest setTwoFactorCode(String code) { this.twoFactorCode = code; return this; }

    public AuthToken getAuthToken() { return authToken; }
    public EnableTwoFactorAuthRequest setAuthToken(AuthToken authToken) { this.authToken = authToken; return this; }

    public ZmBoolean getCsrfSupported() { return csrfSupported; }

    public EnableTwoFactorAuthRequest setCsrfSupported(Boolean csrfSupported) {
        this.csrfSupported = ZmBoolean.fromBool(csrfSupported);
        return this;
    }

    public String getMethod() { return method; }

    public void setMethod(String method) { this.method = method; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

}