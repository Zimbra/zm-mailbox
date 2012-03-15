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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminExtConstants;
import com.zimbra.soap.adminext.type.AttrsImpl;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-description Import accounts in bulk
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminExtConstants.E_BULK_IMPORT_ACCOUNTS_REQUEST)
public class BulkImportAccountsRequest extends AttrsImpl {

    /**
     * @zm-api-field-tag operation
     * @zm-api-field-description Operation - <b>preview|startImport|abortImport|getStatus</b>
     */
    @XmlAttribute(name=AdminExtConstants.A_op /* op */, required=false)
    private String op;

    /**
     * @zm-api-field-tag create-domains-flag
     * @zm-api-field-description Create domains if value is "true" (case insensitive) else false
     */
    @XmlElement(name=AdminExtConstants.E_createDomains /* createDomains */, required=true)
    private String createDomains;

    /**
     * @zm-api-field-tag SMTP-hostname
     * @zm-api-field-description SMTP hostname
     */
    @XmlElement(name=AdminExtConstants.E_SMTPHost /* SMTPHost */, required=false)
    private String SMTPHost;

    /**
     * @zm-api-field-tag SMTP-port
     * @zm-api-field-description SMTP port
     */
    @XmlElement(name=AdminExtConstants.E_SMTPPort /* SMTPPort */, required=false)
    private String SMTPPort;

    /**
     * @zm-api-field-tag source-type
     * @zm-api-field-description Source type
     */
    @XmlElement(name=AdminExtConstants.A_sourceType /* sourceType */, required=false)
    private String sourceType;

    /**
     * @zm-api-field-tag attachment-id
     * @zm-api-field-description Attachment ID
     */
    @XmlElement(name=AdminExtConstants.E_attachmentID /* aid */, required=false)
    private String attachmentID;

    /**
     * @zm-api-field-tag password
     * @zm-api-field-description Password
     */
    @XmlElement(name=AdminExtConstants.A_password /* password */, required=false)
    private String password;

    /**
     * @zm-api-field-tag gen-password-len
     * @zm-api-field-description Password length for generated passwords
     */
    @XmlElement(name=AdminExtConstants.A_genPasswordLength /* genPasswordLength */, required=false)
    private Integer genPasswordLength;

    /**
     * @zm-api-field-tag generate-password-flag
     * @zm-api-field-description Flags whether to generate passwords. Ttrue if "true" (case insensitive) else
     * false - default value "false"
     */
    @XmlElement(name=AdminExtConstants.A_generatePassword /* generatePassword */, required=false)
    private String generatePassword;

    /**
     * @zm-api-field-tag max-results
     * @zm-api-field-description Maximum number of results
     */
    @XmlElement(name=AdminExtConstants.A_maxResults /* maxResults */, required=false)
    private Integer maxResults;

    /**
     * @zm-api-field-tag must-change-password-flag
     * @zm-api-field-description Flags whether user must change the password.  True if "true" (case insensitive) else
     * false
     */
    @XmlElement(name=AdminExtConstants.E_mustChangePassword /* mustChangePassword */, required=true)
    private String mustChangePassword;

    public BulkImportAccountsRequest() {
    }

    public void setOp(String op) { this.op = op; }
    public void setCreateDomains(String createDomains) { this.createDomains = createDomains; }
    public void setSMTPHost(String SMTPHost) { this.SMTPHost = SMTPHost; }
    public void setSMTPPort(String SMTPPort) { this.SMTPPort = SMTPPort; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public void setAttachmentID(String attachmentID) { this.attachmentID = attachmentID; }
    public void setPassword(String password) { this.password = password; }
    public void setGenPasswordLength(Integer genPasswordLength) { this.genPasswordLength = genPasswordLength; }
    public void setGeneratePassword(String generatePassword) { this.generatePassword = generatePassword; }
    public void setMaxResults(Integer maxResults) { this.maxResults = maxResults; }
    public void setMustChangePassword(String mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public String getOp() { return op; }
    public String getCreateDomains() { return createDomains; }
    public String getSMTPHost() { return SMTPHost; }
    public String getSMTPPort() { return SMTPPort; }
    public String getSourceType() { return sourceType; }
    public String getAttachmentID() { return attachmentID; }
    public String getPassword() { return password; }
    public Integer getGenPasswordLength() { return genPasswordLength; }
    public String getGeneratePassword() { return generatePassword; }
    public Integer getMaxResults() { return maxResults; }
    public String getMustChangePassword() { return mustChangePassword; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("op", op)
            .add("createDomains", createDomains)
            .add("SMTPHost", SMTPHost)
            .add("SMTPPort", SMTPPort)
            .add("sourceType", sourceType)
            .add("attachmentID", attachmentID)
            .add("password", password)
            .add("genPasswordLength", genPasswordLength)
            .add("generatePassword", generatePassword)
            .add("maxResults", maxResults)
            .add("mustChangePassword", mustChangePassword);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
