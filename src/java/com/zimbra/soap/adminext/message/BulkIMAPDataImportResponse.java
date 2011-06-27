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

package com.zimbra.soap.adminext.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.AdminExtConstants;
import com.zimbra.soap.adminext.type.NameId;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminExtConstants.E_BULK_IMAP_DATA_IMPORT_RESPONSE)
@XmlType(propOrder = {})
public class BulkIMAPDataImportResponse {

    @XmlElementWrapper(name=AdminExtConstants.E_runningAccounts
                /* runningAccounts */, required=false)
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required=false)
    private List<NameId> runningAccounts = Lists.newArrayList();

    @XmlElement(name=AdminExtConstants.E_totalCount
                /* totalCount */, required=false)
    private Integer totalCount;

    @XmlElement(name=AdminExtConstants.E_idleCount
                /* idleCount */, required=false)
    private Integer idleCount;

    @XmlElement(name=AdminExtConstants.E_runningCount
                /* runningCount */, required=false)
    private Integer runningCount;

    @XmlElement(name=AdminExtConstants.E_finishedCount
                /* finishedCount */, required=false)
    private Integer finishedCount;

    @XmlElement(name=AdminExtConstants.E_connectionType
                /* ConnectionType */, required=false)
    private String connectionType;

    @XmlElement(name=AdminExtConstants.E_IMAPHost
                /* IMAPHost */, required=false)
    private String IMAPHost;

    @XmlElement(name=AdminExtConstants.E_IMAPPort
                /* IMAPPort */, required=false)
    private String IMAPPort;

    @XmlElement(name=AdminExtConstants.E_indexBatchSize
                /* indexBatchSize */, required=false)
    private String indexBatchSize;

    // 1 means true and 0 means false
    @XmlElement(name=AdminExtConstants.E_useAdminLogin
                /* UseAdminLogin */, required=false)
    private Integer useAdminLogin;

    @XmlElement(name=AdminExtConstants.E_IMAPAdminLogin
                /* IMAPAdminLogin */, required=false)
    private String IMAPAdminLogin;

    @XmlElement(name=AdminExtConstants.E_IMAPAdminPassword
                /* IMAPAdminPassword */, required=false)
    private String IMAPAdminPassword;

    public BulkIMAPDataImportResponse() {
    }

    public void setRunningAccounts(Iterable <NameId> runningAccounts) {
        this.runningAccounts.clear();
        if (runningAccounts != null) {
            Iterables.addAll(this.runningAccounts,runningAccounts);
        }
    }

    public void addRunningAccount(NameId runningAccount) {
        this.runningAccounts.add(runningAccount);
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
    public void setIdleCount(Integer idleCount) {
        this.idleCount = idleCount;
    }
    public void setRunningCount(Integer runningCount) {
        this.runningCount = runningCount;
    }
    public void setFinishedCount(Integer finishedCount) {
        this.finishedCount = finishedCount;
    }
    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }
    public void setIMAPHost(String IMAPHost) { this.IMAPHost = IMAPHost; }
    public void setIMAPPort(String IMAPPort) { this.IMAPPort = IMAPPort; }
    public void setIndexBatchSize(String indexBatchSize) {
        this.indexBatchSize = indexBatchSize;
    }
    public void setUseAdminLogin(Integer useAdminLogin) {
        this.useAdminLogin = useAdminLogin;
    }
    public void setIMAPAdminLogin(String IMAPAdminLogin) {
        this.IMAPAdminLogin = IMAPAdminLogin;
    }
    public void setIMAPAdminPassword(String IMAPAdminPassword) {
        this.IMAPAdminPassword = IMAPAdminPassword;
    }
    public List<NameId> getRunningAccounts() {
        return Collections.unmodifiableList(runningAccounts);
    }
    public Integer getTotalCount() { return totalCount; }
    public Integer getIdleCount() { return idleCount; }
    public Integer getRunningCount() { return runningCount; }
    public Integer getFinishedCount() { return finishedCount; }
    public String getConnectionType() { return connectionType; }
    public String getIMAPHost() { return IMAPHost; }
    public String getIMAPPort() { return IMAPPort; }
    public String getIndexBatchSize() { return indexBatchSize; }
    public Integer getUseAdminLogin() { return useAdminLogin; }
    public String getIMAPAdminLogin() { return IMAPAdminLogin; }
    public String getIMAPAdminPassword() { return IMAPAdminPassword; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("runningAccounts", runningAccounts)
            .add("totalCount", totalCount)
            .add("idleCount", idleCount)
            .add("runningCount", runningCount)
            .add("finishedCount", finishedCount)
            .add("connectionType", connectionType)
            .add("IMAPHost", IMAPHost)
            .add("IMAPPort", IMAPPort)
            .add("indexBatchSize", indexBatchSize)
            .add("useAdminLogin", useAdminLogin)
            .add("IMAPAdminLogin", IMAPAdminLogin)
            .add("IMAPAdminPassword", IMAPAdminPassword);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
