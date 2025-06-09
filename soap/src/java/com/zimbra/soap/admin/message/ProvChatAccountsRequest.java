/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2025 Synacor, Inc.
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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainSelector;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Provision Chat accounts
 * <br />
 * If account is specified, the account is provisioned on Chat. <br />
 * If not, all accounts in the domain are provisioned.
 * An Account whose zimbraAccountStatus is not active, zimbraIsSystemAccount is TRUE
 * or zimbraFeatureZulipChatEnabled is FALSE is skipped.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_PROV_CHAT_ACCOUNTS_REQUEST)
public class ProvChatAccountsRequest {
    /**
     * @zm-api-field-description Domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN /* zimbra domin */, required=true)
    private final DomainSelector domain;

    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT /* zimbra domin */, required=false)
    private final AccountSelector account;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ProvChatAccountsRequest() {
        this((DomainSelector) null, (AccountSelector) null);
    }

    public ProvChatAccountsRequest(DomainSelector domain, AccountSelector account) {
        this.domain = domain;
        this.account = account;
    }

    public DomainSelector getDomain() {
        return domain;
    }

    public AccountSelector getAccount() {
        return account;
    }
}