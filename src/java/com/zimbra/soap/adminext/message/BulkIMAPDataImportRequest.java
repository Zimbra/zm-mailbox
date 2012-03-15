/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.AdminExtConstants;
import com.zimbra.soap.adminext.type.Name;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-description Import data for multiple accounts via IMAP
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminExtConstants.E_BULK_IMAP_DATA_IMPORT_REQUEST)
@XmlType(propOrder = {"sourceType", "attachmentID", "accounts",
    "connectionType", "sourceServerType", "IMAPHost", "IMAPPort",
    "indexBatchSize", "useAdminLogin", "IMAPAdminLogin", "IMAPAdminPassword"})
public class BulkIMAPDataImportRequest {

    // Other operations exist but aren't used (yet?) - see ZimbraBulkProvisionExt
    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description Operation - <b>preview|startImport|dismissImport</b>
     */
    @XmlAttribute(name=AdminExtConstants.A_op /* op */, required=false)
    private String op;

    /**
     * @zm-api-field-tag source-type
     * @zm-api-field-description Source type - <b>bulkxml|zimbra</b>
     */
    @XmlElement(name=AdminExtConstants.A_sourceType /* sourceType */, required=false)
    private String sourceType;

    /**
     * @zm-api-field-tag attachment-id
     * @zm-api-field-description Uploaded attachment ID
     */
    @XmlElement(name=AdminExtConstants.E_attachmentID /* aid */, required=false)
    private String attachmentID;

    /**
     * @zm-api-field-description Account specification - where name attributes are email addresses
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT /* account */, required=false)
    private List<Name> accounts = Lists.newArrayList();

    /**
     * @zm-api-field-tag connection-type
     * @zm-api-field-description Connection type
     */
    @XmlElement(name=AdminExtConstants.E_connectionType /* ConnectionType */, required=false)
    private String connectionType;

    /**
     * @zm-api-field-tag source-svr-type
     * @zm-api-field-description Source server type
     */
    @XmlElement(name=AdminExtConstants.E_sourceServerType /* sourceServerType */, required=false)
    private String sourceServerType;

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

    /**
     * @zm-api-field-tag use-admin-login-flag
     * @zm-api-field-description Flag to choose whether to use Admin login or not.  Set to "1" to set.
     * Default is false
     */
    @XmlElement(name=AdminExtConstants.E_useAdminLogin /* UseAdminLogin */, required=false)
    private String useAdminLogin;

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

    public BulkIMAPDataImportRequest() {
    }

    public void setOp(String op) { this.op = op; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public void setAttachmentID(String attachmentID) { this.attachmentID = attachmentID; }
    public void setAccounts(Iterable <Name> accounts) {
        this.accounts.clear();
        if (accounts != null) {
            Iterables.addAll(this.accounts,accounts);
        }
    }

    public void addAccount(Name account) {
        this.accounts.add(account);
    }

    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }
    public void setSourceServerType(String sourceServerType) { this.sourceServerType = sourceServerType; }
    public void setIMAPHost(String IMAPHost) { this.IMAPHost = IMAPHost; }
    public void setIMAPPort(String IMAPPort) { this.IMAPPort = IMAPPort; }
    public void setIndexBatchSize(String indexBatchSize) { this.indexBatchSize = indexBatchSize; }
    public void setUseAdminLogin(String useAdminLogin) { this.useAdminLogin = useAdminLogin; }
    public void setIMAPAdminLogin(String IMAPAdminLogin) { this.IMAPAdminLogin = IMAPAdminLogin; }
    public void setIMAPAdminPassword(String IMAPAdminPassword) { this.IMAPAdminPassword = IMAPAdminPassword; }
    public String getOp() { return op; }
    public String getSourceType() { return sourceType; }
    public String getAttachmentID() { return attachmentID; }
    public List<Name> getAccounts() {
        return accounts;
    }
    public String getConnectionType() { return connectionType; }
    public String getSourceServerType() { return sourceServerType; }
    public String getIMAPHost() { return IMAPHost; }
    public String getIMAPPort() { return IMAPPort; }
    public String getIndexBatchSize() { return indexBatchSize; }
    public String getUseAdminLogin() { return useAdminLogin; }
    public String getIMAPAdminLogin() { return IMAPAdminLogin; }
    public String getIMAPAdminPassword() { return IMAPAdminPassword; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("op", op)
            .add("sourceType", sourceType)
            .add("attachmentID", attachmentID)
            .add("accounts", accounts)
            .add("connectionType", connectionType)
            .add("sourceServerType", sourceServerType)
            .add("IMAPHost", IMAPHost)
            .add("IMAPPort", IMAPPort)
            .add("indexBatchSize", indexBatchSize)
            .add("useAdminLogin", useAdminLogin)
            .add("IMAPAdminLogin", IMAPAdminLogin)
            .add("IMAPAdminPassword", IMAPAdminPassword);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
