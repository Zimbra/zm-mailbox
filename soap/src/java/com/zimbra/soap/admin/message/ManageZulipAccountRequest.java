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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Manage Zulip account
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MANAGE_ZULIP_ACCOUNT_REQUEST)
public class ManageZulipAccountRequest {
    /**
     * @zm-api-field-tag "get|activate|deactivate"
     * @zm-api-field-description Action to perform
     * <table>
     * <tr> <td> <b>get</b> </td> <td> get Zulip account</td> </tr>
     * <tr> <td> <b>activate</b> </td> <td> activate Zulip account</td> </tr>
     * <tr> <td> <b>deactivate</b> </td> <td> deactivate Zulip account</td> </tr>
     * <tr> <td> <b>delete</b> </td> <td> delete Zulip account</td> </tr>
     * </table>
     */
    @XmlAttribute(name=AdminConstants.E_ACTION, required=true)
    private final String action;

    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=true)
    private AccountSelector account;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ManageZulipAccountRequest() {
        this((AccountSelector) null, (String) null);
    }

    public ManageZulipAccountRequest(AccountSelector account, String action) {
        this.account = account;
        this.action = action;
    }

    public AccountSelector getAccount() { return account; }

    public String getAction() { return action; }

}