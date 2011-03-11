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
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.admin.type.Id;
import com.zimbra.soap.admin.type.IdAndType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_ADMIN_WAIT_SET_RESPONSE)
@XmlType(propOrder = {"signalledAccounts", "errors"})
public class AdminWaitSetResponse {

    @XmlAttribute(name=MailConstants.A_WAITSET_ID, required=true)
    private final String waitSetId;

    @XmlAttribute(name=MailConstants.A_CANCELED, required=false)
    private final Boolean canceled;

    @XmlAttribute(name=MailConstants.A_SEQ, required=false)
    private final String seqNo;

    @XmlElement(name=MailConstants.E_A, required=false)
    private List<Id> signalledAccounts = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_ERROR, required=false)
    private List<IdAndType> errors = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AdminWaitSetResponse() {
        this((String) null, (Boolean) null, (String) null);
    }

    public AdminWaitSetResponse(String waitSetId, Boolean canceled,
                    String seqNo) {
        this.waitSetId = waitSetId;
        this.canceled = canceled;
        this.seqNo = seqNo;
    }

    public void setSignalledAccounts(Iterable <Id> signalledAccounts) {
        this.signalledAccounts.clear();
        if (signalledAccounts != null) {
            Iterables.addAll(this.signalledAccounts,signalledAccounts);
        }
    }

    public AdminWaitSetResponse addSignalledAccount(Id signalledAccount) {
        this.signalledAccounts.add(signalledAccount);
        return this;
    }

    public void setErrors(Iterable <IdAndType> errors) {
        this.errors.clear();
        if (errors != null) {
            Iterables.addAll(this.errors,errors);
        }
    }

    public AdminWaitSetResponse addError(IdAndType error) {
        this.errors.add(error);
        return this;
    }

    public String getWaitSetId() { return waitSetId; }
    public Boolean getCanceled() { return canceled; }
    public String getSeqNo() { return seqNo; }
    public List<Id> getSignalledAccounts() {
        return Collections.unmodifiableList(signalledAccounts);
    }
    public List<IdAndType> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
