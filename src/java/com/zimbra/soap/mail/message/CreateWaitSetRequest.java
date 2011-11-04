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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
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

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.WaitSetAddSpec;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_CREATE_WAIT_SET_REQUEST)
public class CreateWaitSetRequest {

    @XmlAttribute(name=MailConstants.A_DEFTYPES /* defTypes */, required=true)
    private final String defaultInterests;

    @XmlAttribute(name=MailConstants.A_ALL_ACCOUNTS /* allAccounts */, required=false)
    private ZmBoolean allAccounts;

    @XmlElementWrapper(name=MailConstants.E_WAITSET_ADD /* add */, required=false)
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private List<WaitSetAddSpec> accounts = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CreateWaitSetRequest() {
        this((String) null);
    }

    public CreateWaitSetRequest(String defaultInterests) {
        this.defaultInterests = defaultInterests;
    }

    public void setAllAccounts(Boolean allAccounts) {
        this.allAccounts = ZmBoolean.fromBool(allAccounts);
    }
    public void setAccounts(Iterable <WaitSetAddSpec> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public CreateWaitSetRequest addAccount(WaitSetAddSpec account) {
        this.accounts.add(account);
        return this;
    }

    public String getDefaultInterests() { return defaultInterests; }
    public Boolean getAllAccounts() { return ZmBoolean.toBool(allAccounts); }
    public List<WaitSetAddSpec> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("defaultInterests", defaultInterests)
            .add("allAccounts", allAccounts)
            .add("accounts", accounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
