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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminExtConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminExtConstants.E_GENERATE_BULK_PROV_FROM_LDAP_RESPONSE)
@XmlType(propOrder = {})
public class GenerateBulkProvisionFileFromLDAPResponse {

    @XmlElement(name=AdminExtConstants.E_totalCount
                /* totalCount */, required=false)
    private Integer totalCount;

    @XmlElement(name=AdminExtConstants.E_domainCount
                /* domainCount */, required=false)
    private Integer domainCount;

    @XmlElement(name=AdminExtConstants.E_skippedAccountCount
                /* skippedAccountCount */, required=false)
    private Integer skippedAccountCount;

    @XmlElement(name=AdminExtConstants.E_skippedDomainCount
                /* skippedDomainCount */, required=false)
    private Integer skippedDomainCount;

    @XmlElement(name=AdminExtConstants.E_SMTPHost
                /* SMTPHost */, required=false)
    private String SMTPHost;

    @XmlElement(name=AdminExtConstants.E_SMTPPort
                /* SMTPPort */, required=false)
    private String SMTPPort;

    @XmlElement(name=AdminExtConstants.E_fileToken
                /* fileToken */, required=false)
    private String fileToken;

    public GenerateBulkProvisionFileFromLDAPResponse() {
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
    public void setDomainCount(Integer domainCount) {
        this.domainCount = domainCount;
    }
    public void setSkippedAccountCount(Integer skippedAccountCount) {
        this.skippedAccountCount = skippedAccountCount;
    }
    public void setSkippedDomainCount(Integer skippedDomainCount) {
        this.skippedDomainCount = skippedDomainCount;
    }
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
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
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
