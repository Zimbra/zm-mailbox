/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountNameSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Puts the mailbox of the specified account into maintenance lockout or removes it from maintenance lockout
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_LOCKOUT_MAILBOX_REQUEST)
public class LockoutMailboxRequest {

    /**
     * @zm-api-field-description Account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=true)
    private AccountNameSelector account;

    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description one of 'start' or 'end'
     */
    @XmlAttribute(name=AdminConstants.A_OPERATION)
    private String operation;


    private LockoutMailboxRequest() {
    }

    private LockoutMailboxRequest(AccountNameSelector account) {
        setAccount(account);
    }

    public static LockoutMailboxRequest create(AccountNameSelector account) {
        return new LockoutMailboxRequest(account);
    }

    public void setAccount(AccountNameSelector account) { this.account = account; }
    public AccountNameSelector getAccount() { return account; }

    public void setOperation(String operation) { this.operation = operation; }
    public String getOperation() { return operation; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("account", account)
            .add("operation", operation);

    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
