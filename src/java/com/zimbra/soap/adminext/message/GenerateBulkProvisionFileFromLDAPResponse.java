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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminExtConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminExtConstants.E_GENERATE_BULK_PROV_FROM_LDAP_RESPONSE)
public class GenerateBulkProvisionFileFromLDAPResponse {

    /**
     * @zm-api-field-tag total-count
     * @zm-api-field-description Total count
     */
    @XmlElement(name=AdminExtConstants.E_totalCount /* totalCount */, required=false)
    private Integer totalCount;

    /**
     * @zm-api-field-tag domain-count
     * @zm-api-field-description Domain count
     */
    @XmlElement(name=AdminExtConstants.E_domainCount /* domainCount */, required=false)
    private Integer domainCount;

    /**
     * @zm-api-field-tag skipped-account-count
     * @zm-api-field-description Count of number of skipped accounts
     */
    @XmlElement(name=AdminExtConstants.E_skippedAccountCount /* skippedAccountCount */, required=false)
    private Integer skippedAccountCount;

    /**
     * @zm-api-field-tag skipped-domain-count
     * @zm-api-field-description Count of number of skipped domains
     */
    @XmlElement(name=AdminExtConstants.E_skippedDomainCount /* skippedDomainCount */, required=false)
    private Integer skippedDomainCount;

    /**
     * @zm-api-field-tag SMTP-host
     * @zm-api-field-description SMTP host
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
     * @zm-api-field-tag file-token
     * @zm-api-field-description File token
     */
    @XmlElement(name=AdminExtConstants.E_fileToken /* fileToken */, required=false)
    private String fileToken;

    public GenerateBulkProvisionFileFromLDAPResponse() {
    }

    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public void setDomainCount(Integer domainCount) { this.domainCount = domainCount; }
    public void setSkippedAccountCount(Integer skippedAccountCount) { this.skippedAccountCount = skippedAccountCount; }
    public void setSkippedDomainCount(Integer skippedDomainCount) { this.skippedDomainCount = skippedDomainCount; }
    public void setSMTPHost(String SMTPHost) { this.SMTPHost = SMTPHost; }
    public void setSMTPPort(String SMTPPort) { this.SMTPPort = SMTPPort; }
    public void setFileToken(String fileToken) { this.fileToken = fileToken; }
    public Integer getTotalCount() { return totalCount; }
    public Integer getDomainCount() { return domainCount; }
    public Integer getSkippedAccountCount() { return skippedAccountCount; }
    public Integer getSkippedDomainCount() { return skippedDomainCount; }
    public String getSMTPHost() { return SMTPHost; }
    public String getSMTPPort() { return SMTPPort; }
    public String getFileToken() { return fileToken; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("totalCount", totalCount)
            .add("domainCount", domainCount)
            .add("skippedAccountCount", skippedAccountCount)
            .add("skippedDomainCount", skippedDomainCount)
            .add("SMTPHost", SMTPHost)
            .add("SMTPPort", SMTPPort)
            .add("fileToken", fileToken);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
