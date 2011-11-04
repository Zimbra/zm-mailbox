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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.WaitSetAddSpec;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_ADMIN_CREATE_WAIT_SET_REQUEST)
public class AdminCreateWaitSetRequest {

    @XmlAttribute(name=MailConstants.A_DEFTYPES, required=true)
    private final String defaultInterests;

    @XmlAttribute(name=MailConstants.A_ALL_ACCOUNTS, required=false)
    private final ZmBoolean allAccounts;

    @XmlElementWrapper(name=MailConstants.E_WAITSET_ADD)
    @XmlElement(name=MailConstants.E_A, required=false)
    private List<WaitSetAddSpec> accounts = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AdminCreateWaitSetRequest() {
        this((String) null, (Boolean) null);
    }

    public AdminCreateWaitSetRequest(String defaultInterests,
                    Boolean allAccounts) {
        this.defaultInterests = defaultInterests;
        this.allAccounts = ZmBoolean.fromBool(allAccounts);
    }

    public void setAccounts(Iterable <WaitSetAddSpec> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public AdminCreateWaitSetRequest addAccount(WaitSetAddSpec account) {
        this.accounts.add(account);
        return this;
    }

    public String getDefaultInterests() { return defaultInterests; }
    public Boolean getAllAccounts() { return ZmBoolean.toBool(allAccounts); }
    public List<WaitSetAddSpec> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }
}
