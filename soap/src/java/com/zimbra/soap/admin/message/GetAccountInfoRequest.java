/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get information about an account
 * <br />
 * Currently only 2 attrs are returned:
 * <table>
 * <tr> <td> <b>zimbraId</b> </td> <td> the unique UUID of the zimbra account </td> </tr>
 * <tr> <td> <b>zimbraMailHost</b> </td> <td> the server on which this user's mail resides </td> </tr>
 * </table>
 * <b>Access</b>: domain admin sufficient
 */
@XmlRootElement(name=AdminConstants.E_GET_ACCOUNT_INFO_REQUEST)
public class GetAccountInfoRequest {

    @XmlElement(name=AdminConstants.E_ACCOUNT, required=true)
    private AccountSelector account;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetAccountInfoRequest() {
    }

    public GetAccountInfoRequest(AccountSelector account) {
        this.account = account;
    }

    public AccountSelector getAccount() { return account; }
}
