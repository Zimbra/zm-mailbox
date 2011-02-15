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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AccountSelector;
import com.zimbra.soap.admin.type.LoggerInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ADD_ACCOUNT_LOGGER_REQUEST)
public class AddAccountLoggerRequest {

    private AccountSelector account;

    @XmlElement(name=AdminConstants.E_LOGGER, required=true)
    private final LoggerInfo logger;

    public AddAccountLoggerRequest() {
        this((AccountSelector) null, (LoggerInfo) null);
    }

    public AddAccountLoggerRequest(AccountSelector account, LoggerInfo logger) {
        this.account = account;
        this.logger = logger;
    }

    @XmlElements({
        @XmlElement(name=AdminConstants.E_ID,
            type=String.class, required=false),
        @XmlElement(name=AdminConstants.E_ACCOUNT,
            type=AccountSelector.class, required=false)
    })
    public void setIdInfo(Object idInfo) { 
        if (idInfo instanceof AccountSelector)
            this.account = (AccountSelector) idInfo;
        else 
            this.account = AccountSelector.fromId(idInfo.toString());
    }

    /**
     * getter required by JAXB marshaller
     * @return
     */
    @SuppressWarnings("unused")
    private Object getIdInfo() { return account; }

    public LoggerInfo getLogger() { return logger; }
    public AccountSelector getAccount() { return account; }
}
