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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Change Account
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_CHANGE_PRIMARY_EMAIL_REQUEST)
@XmlType(propOrder = {})
public class ChangePrimaryEmailRequest {

    /**
     * @zm-api-field-description Specifies the account to be changed
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=true)
    private AccountSelector account;

    /**
     * @zm-api-field-tag new-account-name
     * @zm-api-field-description New account name
     */
    @XmlElement(name=AdminConstants.E_NEW_NAME, required=true)
    private final String newName;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ChangePrimaryEmailRequest() {
        this(null, null);
    }

    public ChangePrimaryEmailRequest(AccountSelector account, String newName) {
        this.account = account;
        this.newName = newName;
    }

    public AccountSelector getAccount() { return account; }
    public String getNewName() { return newName; }
}
