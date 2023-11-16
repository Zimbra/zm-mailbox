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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;

@XmlRootElement(name=AccountConstants.E_DISABLE_TWO_FACTOR_AUTH_REQUEST)
@XmlType(propOrder = {})
public class DisableTwoFactorAuthRequest {
    /**
     * @zm-api-field-description 2FA method to be disabled
     */
    @XmlElement(name=AccountConstants.E_METHOD, required=false)
    private String method;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

}
