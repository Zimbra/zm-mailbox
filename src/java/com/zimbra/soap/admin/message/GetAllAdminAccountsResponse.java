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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AccountInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_ALL_ADMIN_ACCOUNTS_RESPONSE)
@XmlType(propOrder = {})
public class GetAllAdminAccountsResponse {

    /**
     * @zm-api-field-description Information on accounts
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=false)
    private List <AccountInfo> accountList = Lists.newArrayList();

    public GetAllAdminAccountsResponse() {
    }

    public void setAccountList(Iterable <AccountInfo> accounts) {
        this.accountList.clear();
        if (accounts != null) {
            Iterables.addAll(this.accountList, accounts);
        }
    }

    public void addAccount(AccountInfo account ) {
        this.accountList.add(account);
    }

    public List <AccountInfo> getAccountList() {
        return Collections.unmodifiableList(accountList);
    }
}
