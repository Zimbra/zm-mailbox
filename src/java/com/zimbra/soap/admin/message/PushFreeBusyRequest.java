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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.Id;
import com.zimbra.soap.admin.type.Names;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_PUSH_FREE_BUSY_REQUEST)
public class PushFreeBusyRequest {

    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private Names domains;

    @XmlElement(name=AdminConstants.E_ACCOUNT, required=false)
    private List<Id> accounts = Lists.newArrayList();

    public PushFreeBusyRequest() {
    }

    public void setDomains(Names domains) { this.domains = domains; }
    public void setAccounts(Iterable <Id> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public PushFreeBusyRequest addAccount(Id account) {
        this.accounts.add(account);
        return this;
    }

    public Names getDomains() { return domains; }
    public List<Id> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }
}
