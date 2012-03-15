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
import com.zimbra.soap.type.AccountSelector;

/**
 * @zm-api-command-description Returns custom loggers created for the given account since the
 *  last server start.  If the request is sent to a server other than the
 *  one that the account resides on, it is proxied to the correct server.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ACCOUNT_LOGGERS_REQUEST)
public class GetAccountLoggersRequest {

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

    public GetAccountLoggersRequest() {
        this((AccountSelector)null);
    }

    public GetAccountLoggersRequest(AccountSelector account) {
        this.account = account;
    }

    @Deprecated
    public void setId(String id) { this.id = id; }
    public void setAccount(AccountSelector account) { this.account = account; }
    @Deprecated
    public String getId() { return id; }
    public AccountSelector getAccount() { return account; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("account", account);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
