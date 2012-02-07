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

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.admin.type.Name;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=BackupConstants.E_RESTORE_RESPONSE)
@XmlType(propOrder = {})
public class RestoreResponse {

    /**
     * @zm-api-field-description Status - one of <b>ok|interrupted|err</b>
     */
    @XmlAttribute(name=BackupConstants.A_STATUS /* status */, required=false)
    private String status;

    /**
     * @zm-api-field-tag rebuilt-schema
     * @zm-api-field-description Flag whether schema was rebuilt
     */
    @XmlAttribute(name=BackupConstants.A_REBUILTSCHEMA /* rebuiltSchema */, required=false)
    private ZmBoolean rebuildSchema;

    /**
     * @zm-api-field-description Accounts
     */
    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=false)
    private List<Name> accounts = Lists.newArrayList();

    public RestoreResponse() {
    }

    public void setStatus(String status) { this.status = status; }
    public void setRebuildSchema(Boolean rebuildSchema) { this.rebuildSchema = ZmBoolean.fromBool(rebuildSchema); }
    public void setAccounts(Iterable <Name> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public void addAccount(Name account) {
        this.accounts.add(account);
    }

    public String getStatus() { return status; }
    public Boolean getRebuildSchema() { return ZmBoolean.toBool(rebuildSchema); }
    public List<Name> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("status", status)
            .add("rebuildSchema", rebuildSchema)
            .add("accounts", accounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
