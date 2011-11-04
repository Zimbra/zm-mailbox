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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.BackupConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"fileCopier", "accounts"})
public class BackupSpec {

    @XmlAttribute(name=BackupConstants.A_METHOD /* method */, required=false)
    private String method;

    @XmlAttribute(name=BackupConstants.A_BACKUP_TARGET /* target */, required=false)
    private String target;

    @XmlAttribute(name=BackupConstants.A_LABEL /* label */, required=false)
    private String label;

    @XmlAttribute(name=BackupConstants.A_BEFORE /* before */, required=false)
    private String before;

    @XmlAttribute(name=BackupConstants.A_SYNC /* sync */, required=false)
    private ZmBoolean sync;

    // Valid values are config/include/exclude (case insensitive)
    @XmlAttribute(name=BackupConstants.A_SEARCH_INDEX /* searchIndex */, required=false)
    private String searchIndex;

    // Valid values are config/include/exclude (case insensitive)
    @XmlAttribute(name=BackupConstants.A_BLOBS /* blobs */, required=false)
    private String blobs;

    // Valid values are config/include/exclude (case insensitive)
    @XmlAttribute(name=BackupConstants.A_SECONDARY_BLOBS /* secondaryBlobs */, required=false)
    private String secondaryBlobs;

    @XmlAttribute(name=BackupConstants.A_ZIP /* zip */, required=false)
    private ZmBoolean zip;

    @XmlAttribute(name=BackupConstants.A_ZIP_STORE /* zipStore */, required=false)
    private ZmBoolean zipStore;

    @XmlElement(name=BackupConstants.E_FILE_COPIER /* fileCopier */, required=false)
    private FileCopierSpec fileCopier;

    @XmlElement(name=BackupConstants.E_ACCOUNT /* account */, required=false)
    private List<Name> accounts = Lists.newArrayList();

    public BackupSpec() {
    }

    public void setMethod(String method) { this.method = method; }
    public void setTarget(String target) { this.target = target; }
    public void setLabel(String label) { this.label = label; }
    public void setBefore(String before) { this.before = before; }
    public void setSync(Boolean sync) { this.sync = ZmBoolean.fromBool(sync); }
    public void setSearchIndex(String searchIndex) { this.searchIndex = searchIndex; }
    public void setBlobs(String blobs) { this.blobs = blobs; }
    public void setSecondaryBlobs(String secondaryBlobs) {
        this.secondaryBlobs = secondaryBlobs;
    }
    public void setZip(Boolean zip) { this.zip = ZmBoolean.fromBool(zip); }
    public void setZipStore(Boolean zipStore) { this.zipStore = ZmBoolean.fromBool(zipStore); }
    public void setFileCopier(FileCopierSpec fileCopier) { this.fileCopier = fileCopier; }
    public void setAccounts(Iterable <Name> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public void addAccount(Name account) {
        this.accounts.add(account);
    }

    public String getMethod() { return method; }
    public String getTarget() { return target; }
    public String getLabel() { return label; }
    public String getBefore() { return before; }
    public Boolean getSync() { return ZmBoolean.toBool(sync); }
    public String getSearchIndex() { return searchIndex; }
    public String getBlobs() { return blobs; }
    public String getSecondaryBlobs() { return secondaryBlobs; }
    public Boolean getZip() { return ZmBoolean.toBool(zip); }
    public Boolean getZipStore() { return ZmBoolean.toBool(zipStore); }
    public FileCopierSpec getFileCopier() { return fileCopier; }
    public List<Name> getAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("method", method)
            .add("target", target)
            .add("label", label)
            .add("before", before)
            .add("sync", sync)
            .add("searchIndex", searchIndex)
            .add("blobs", blobs)
            .add("secondaryBlobs", secondaryBlobs)
            .add("zip", zip)
            .add("zipStore", zipStore)
            .add("fileCopier", fileCopier)
            .add("accounts", accounts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
