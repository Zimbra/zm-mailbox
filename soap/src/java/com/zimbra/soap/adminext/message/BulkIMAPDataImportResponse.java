/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.adminext.message;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.AdminExtConstants;
import com.zimbra.soap.adminext.type.NameId;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminExtConstants.E_BULK_IMAP_DATA_IMPORT_RESPONSE)
public class BulkIMAPDataImportResponse {

    /**
     * @zm-api-field-description Running accounts
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AdminExtConstants.E_runningAccounts /* runningAccounts */, required=false)
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required=false)
    private List<NameId> runningAccounts = Lists.newArrayList();

    /**
     * @zm-api-field-tag total-count
     * @zm-api-field-description Total count
     */
    @XmlElement(name=AdminExtConstants.E_totalCount /* totalCount */, required=false)
    private Integer totalCount;

    /**
     * @zm-api-field-tag idle-count
     * @zm-api-field-description Idle count
     */
    @XmlElement(name=AdminExtConstants.E_idleCount /* idleCount */, required=false)
    private Integer idleCount;

    /**
     * @zm-api-field-tag running-count
     * @zm-api-field-description Running count
     */
    @XmlElement(name=AdminExtConstants.E_runningCount /* runningCount */, required=false)
    private Integer runningCount;

    /**
     * @zm-api-field-tag finished-count
     * @zm-api-field-description Finished count
     */
    @XmlElement(name=AdminExtConstants.E_finishedCount /* finishedCount */, required=false)
    private Integer finishedCount;

    /**
     * @zm-api-field-tag connection-type
     * @zm-api-field-description Connection type
     */
    @XmlElement(name=AdminExtConstants.E_connectionType /* ConnectionType */, required=false)
    private String connectionType;

    /**
     * @zm-api-field-tag imap-hostname
     * @zm-api-field-description IMAP hostname
     */
    @XmlElement(name=AdminExtConstants.E_IMAPHost /* IMAPHost */, required=false)
    private String IMAPHost;

    /**
     * @zm-api-field-tag imap-port
     * @zm-api-field-description IMAP port
     */
    @XmlElement(name=AdminExtConstants.E_IMAPPort /* IMAPPort */, required=false)
    private String IMAPPort;

    /**
     * @zm-api-field-tag index-batch-size
     * @zm-api-field-description Index batch size
     */
    @XmlElement(name=AdminExtConstants.E_indexBatchSize /* indexBatchSize */, required=false)
    private String indexBatchSize;

    // 1 means true and 0 means false
    /**
     * @zm-api-field-tag use-admin-login-flag
     * @zm-api-field-description Whether Admin login is in use or not.  "1" means true, "0" means false
     */
    @XmlElement(name=AdminExtConstants.E_useAdminLogin /* UseAdminLogin */, required=false)
    private Integer useAdminLogin;

    /**
     * @zm-api-field-tag imap-admin-login
     * @zm-api-field-description IMAP Admin login name
     */
    @XmlElement(name=AdminExtConstants.E_IMAPAdminLogin /* IMAPAdminLogin */, required=false)
    private String IMAPAdminLogin;

    /**
     * @zm-api-field-tag imap-admin-password
     * @zm-api-field-description IMAP Admin password
     */
    @XmlElement(name=AdminExtConstants.E_IMAPAdminPassword /* IMAPAdminPassword */, required=false)
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

    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public void setIdleCount(Integer idleCount) { this.idleCount = idleCount; }
    public void setRunningCount(Integer runningCount) { this.runningCount = runningCount; }
    public void setFinishedCount(Integer finishedCount) { this.finishedCount = finishedCount; }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }
    public void setIMAPHost(String IMAPHost) { this.IMAPHost = IMAPHost; }
    public void setIMAPPort(String IMAPPort) { this.IMAPPort = IMAPPort; }
    public void setIndexBatchSize(String indexBatchSize) { this.indexBatchSize = indexBatchSize; }
    public void setUseAdminLogin(Integer useAdminLogin) { this.useAdminLogin = useAdminLogin; }
    public void setIMAPAdminLogin(String IMAPAdminLogin) { this.IMAPAdminLogin = IMAPAdminLogin; }
    public void setIMAPAdminPassword(String IMAPAdminPassword) { this.IMAPAdminPassword = IMAPAdminPassword; }
    public List<NameId> getRunningAccounts() {
        return runningAccounts;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
