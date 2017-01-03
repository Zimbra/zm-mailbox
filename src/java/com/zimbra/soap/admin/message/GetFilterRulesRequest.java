/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Get filter rules
 */
@XmlRootElement(name=AdminConstants.E_GET_FILTER_RULES_REQUEST)
public class GetFilterRulesRequest {
    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT)
    private AccountSelector account;

    public GetFilterRulesRequest() {
    }

    public GetFilterRulesRequest(AccountSelector accountSelector) {
        this.account = accountSelector;
    }

    /**
     * @return the accountSelector
     */
    public AccountSelector getAccount() {
        return account;
    }

    /**
     * @param accountSelector the accountSelector to set
     */
    public void setAccount(AccountSelector accountSelector) {
        this.account = accountSelector;
    }

}