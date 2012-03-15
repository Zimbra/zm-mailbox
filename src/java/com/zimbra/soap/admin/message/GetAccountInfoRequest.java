/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;

/**
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
