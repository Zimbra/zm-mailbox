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
import com.zimbra.soap.admin.type.Id;
import com.zimbra.soap.admin.type.WaitSetAddSpec;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_ADMIN_WAIT_SET_REQUEST)
public class AdminWaitSetRequest {

    @XmlAttribute(name=MailConstants.A_WAITSET_ID, required=true)
    private final String waitSetId;

    @XmlAttribute(name=MailConstants.A_SEQ, required=true)
    private final String lastKnownSeqNo;

    @XmlAttribute(name=MailConstants.A_BLOCK, required=false)
    private final Boolean block;

    // default interest types required for "All" waitsets
    @XmlAttribute(name=MailConstants.A_DEFTYPES, required=false)
    private final String defaultInterests;

    @XmlAttribute(name=MailConstants.A_TIMEOUT, required=true)
    private final Long timeout;

    @XmlElementWrapper(name=MailConstants.E_WAITSET_ADD, required=false)
    private List<WaitSetAddSpec> addAccounts = Lists.newArrayList();

    @XmlElementWrapper(name=MailConstants.E_WAITSET_UPDATE, required=false)
    @XmlElement(name=MailConstants.E_A, required=false)
    private List<WaitSetAddSpec> updateAccounts = Lists.newArrayList();

    @XmlElementWrapper(name=MailConstants.E_WAITSET_REMOVE, required=false)
    @XmlElement(name=MailConstants.E_A, required=false)
    private List<Id> removeAccounts = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AdminWaitSetRequest() {
        this((String) null, (String) null, (Boolean) null,
                (String) null, (Long) null);
    }

    public AdminWaitSetRequest(String waitSetId, String lastKnownSeqNo,
                Boolean block, String defaultInterests, Long timeout) {
        this.waitSetId = waitSetId;
        this.lastKnownSeqNo = lastKnownSeqNo;
        this.block = block;
        this.defaultInterests = defaultInterests;
        this.timeout = timeout;
    }

    public void setAddAccounts(Iterable <WaitSetAddSpec> addAccounts) {
        this.addAccounts.clear();
        if (addAccounts != null) {
            Iterables.addAll(this.addAccounts,addAccounts);
        }
    }

    public AdminWaitSetRequest addAddAccount(WaitSetAddSpec addAccount) {
        this.addAccounts.add(addAccount);
        return this;
    }

    public void setUpdateAccounts(Iterable <WaitSetAddSpec> updateAccounts) {
        this.updateAccounts.clear();
        if (updateAccounts != null) {
            Iterables.addAll(this.updateAccounts,updateAccounts);
        }
    }

    public AdminWaitSetRequest addUpdateAccount(WaitSetAddSpec updateAccount) {
        this.updateAccounts.add(updateAccount);
        return this;
    }

    public void setRemoveAccounts(Iterable <Id> removeAccounts) {
        this.removeAccounts.clear();
        if (removeAccounts != null) {
            Iterables.addAll(this.removeAccounts,removeAccounts);
        }
    }

    public AdminWaitSetRequest addRemoveAccount(Id removeAccount) {
        this.removeAccounts.add(removeAccount);
        return this;
    }

    public String getWaitSetId() { return waitSetId; }
    public String getLastKnownSeqNo() { return lastKnownSeqNo; }
    public Boolean getBlock() { return block; }
    public String getDefaultInterests() { return defaultInterests; }
    public Long getTimeout() { return timeout; }
    public List<WaitSetAddSpec> getAddAccounts() {
        return Collections.unmodifiableList(addAccounts);
    }
    public List<WaitSetAddSpec> getUpdateAccounts() {
        return Collections.unmodifiableList(updateAccounts);
    }
    public List<Id> getRemoveAccounts() {
        return Collections.unmodifiableList(removeAccounts);
    }
}
