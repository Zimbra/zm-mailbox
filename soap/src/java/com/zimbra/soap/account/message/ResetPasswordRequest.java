/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.util.StringUtil;

/**
 <ResetPasswordRequest>
   <password>...</password>
 </ResetPasswordRequest>
 * @zm-api-command-auth-required true - This request should be sent after authentication.
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Reset Password
*/
@XmlRootElement(name=AccountConstants.E_RESET_PASSWORD_REQUEST /* ResetPasswordRequest */)
@XmlType(propOrder = {})
public class ResetPasswordRequest {
    /**
     * @zm-api-field-description New Password to assign
     */
    @XmlElement(name=AccountConstants.E_PASSWORD /* password */, required=true)
    private String password;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String newPassword) {
        this.password = newPassword;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public void validateResetPasswordRequest() throws ServiceException {
        if (StringUtil.isNullOrEmpty(this.password)) {
            throw ServiceException.INVALID_REQUEST("Invalid or missing password", null);
        }
    }
}
