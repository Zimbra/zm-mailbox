/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.LoggerInfo;
import com.zimbra.soap.type.AccountSelector;

/**
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("account", account)
            .add("logger", logger);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
