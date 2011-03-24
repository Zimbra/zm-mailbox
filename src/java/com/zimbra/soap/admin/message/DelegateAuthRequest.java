/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;

import com.zimbra.soap.type.AccountSelector;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_DELEGATE_AUTH_REQUEST)
@XmlType(propOrder = {})
public class DelegateAuthRequest {

    @XmlElement(name=AdminConstants.E_ACCOUNT, required=true)
    private AccountSelector account;
    @XmlAttribute(name=AdminConstants.A_DURATION, required=false)
    private long duration;

    public DelegateAuthRequest() {
    }

    public DelegateAuthRequest(AccountSelector account) {
        this(account, null);
    }

    public DelegateAuthRequest(AccountSelector account, Long duration) {
        this.account = account;
        if (duration != null)
            this.duration = duration;
    }

    public void setAccount(AccountSelector account) {
        this.account = account;
    }

    public AccountSelector getAccount() {
        return account;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }
}
