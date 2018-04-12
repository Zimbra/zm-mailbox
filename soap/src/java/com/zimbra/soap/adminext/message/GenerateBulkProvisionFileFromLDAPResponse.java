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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
