/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.AttributeSelectorImpl;
import com.zimbra.soap.type.ZmBoolean;

@XmlRootElement(name=AdminConstants.E_GET_ACCOUNT_REQUEST)
public class GetAccountRequest extends AttributeSelectorImpl {

    @XmlAttribute(name=AdminConstants.A_APPLY_COS, required=false)
    private ZmBoolean applyCos = ZmBoolean.ONE /* true */;
    @XmlElement(name=AdminConstants.E_ACCOUNT)
    private AccountSelector account;

    public GetAccountRequest() {
    }

    public GetAccountRequest(AccountSelector account) {
        this(account, (Boolean) null, (Iterable<String>) null);
    }

    public GetAccountRequest(AccountSelector account, Boolean applyCos) {
        this(account, applyCos, (Iterable<String>) null);
    }

    public GetAccountRequest(AccountSelector account, Boolean applyCos,
            Iterable<String> attrs) {
        super(attrs);
        this.account = account;
        this.applyCos = ZmBoolean.fromBool(applyCos);
    }

    public void setAccount(AccountSelector account) {
        this.account = account;
    }

    public void setApplyCos(Boolean applyCos) {
        this.applyCos = ZmBoolean.fromBool(applyCos);
    }

    public AccountSelector getAccount() { return account; }
    public Boolean isApplyCos() { return ZmBoolean.toBool(applyCos); }
}
