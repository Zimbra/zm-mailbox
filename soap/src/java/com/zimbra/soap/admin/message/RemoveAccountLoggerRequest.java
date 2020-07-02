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

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.LoggerInfo;
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Removes one or more custom loggers.  If both the account and logger are
 * specified, removes the given account logger if it exists.  If only the account
 * is specified or the category is "all", removes all custom loggers from that
 * account.  If only the logger is specified, removes that custom logger from all
 * accounts.  If neither element is specified, removes all custom loggers from
 * all accounts on the server that receives the request.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_REMOVE_ACCOUNT_LOGGER_REQUEST)
public class RemoveAccountLoggerRequest {

    /**
     * @zm-api-field-description Deprecated - use account instead
     */
    @XmlElement(name=AdminConstants.E_ID /* id */, required=false)
    @Deprecated
    private String id;

    /**
     * @zm-api-field-description Use to select account
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required=false)
    private AccountSelector account;

    /**
     * @zm-api-field-description Logger category
     */
    @XmlElement(name=AdminConstants.E_LOGGER /* logger */, required=false)
    private LoggerInfo logger;

    public RemoveAccountLoggerRequest() {
        this((AccountSelector) null, (LoggerInfo) null);
    }

    public RemoveAccountLoggerRequest(AccountSelector account, LoggerInfo logger) {
        setAccount(account);
        setLogger(logger);
    }

    public static RemoveAccountLoggerRequest createForAccountAndLogger(AccountSelector account, LoggerInfo logger) {
        return new RemoveAccountLoggerRequest(account, logger);
    }

    @Deprecated
    public void setId(String id) { this.id = id; }
    public void setAccount(AccountSelector account) { this.account = account; }
    public void setLogger(LoggerInfo logger) { this.logger = logger; }
    @Deprecated
    public String getId() { return id; }
    public AccountSelector getAccount() { return account; }
    public LoggerInfo getLogger() { return logger; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("account", account)
            .add("logger", logger);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
