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
@XmlRootElement(name=AdminExtConstants.E_BULK_IMPORT_ACCOUNTS_RESPONSE)
public class BulkImportAccountsResponse {

    /**
     * @zm-api-field-tag total-count
     * @zm-api-field-description Total count
     */
    @XmlElement(name=AdminExtConstants.E_totalCount /* totalCount */, required=false)
    private Integer totalCount;

    /**
     * @zm-api-field-tag skipped-acct-count
     * @zm-api-field-description Count of number of skipped accounts
     */
    @XmlElement(name=AdminExtConstants.E_skippedAccountCount /* skippedAccountCount */, required=false)
    private Integer skippedAccountCount;

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
     * @zm-api-field-tag status
     * @zm-api-field-description Status
     */
    @XmlElement(name=AdminExtConstants.E_status /* status */, required=false)
    private Integer status;

    /**
     * @zm-api-field-tag provisioned-count
     * @zm-api-field-description Provisioned count
     */
    @XmlElement(name=AdminExtConstants.E_provisionedCount /* provisionedCount */, required=false)
    private Integer provisionedCount;

    /**
     * @zm-api-field-tag skipped-count
     * @zm-api-field-description Skipped count
     */
    @XmlElement(name=AdminExtConstants.E_skippedCount /* skippedCount */, required=false)
    private Integer skippedCount;

    /**
     * @zm-api-field-tag error-count
     * @zm-api-field-description Error count
     */
    @XmlElement(name=AdminExtConstants.E_errorCount /* errorCount */, required=false)
    private Integer errorCount;

    /**
     * @zm-api-field-tag report-file-token
     * @zm-api-field-description Report file token
     */
    @XmlElement(name=AdminExtConstants.E_reportFileToken /* fileToken */, required=false)
    private String reportFileToken;

    public BulkImportAccountsResponse() {
    }

    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
    public void setSkippedAccountCount(Integer skippedAccountCount) { this.skippedAccountCount = skippedAccountCount; }
    public void setSMTPHost(String SMTPHost) { this.SMTPHost = SMTPHost; }
    public void setSMTPPort(String SMTPPort) { this.SMTPPort = SMTPPort; }
    public void setStatus(Integer status) { this.status = status; }
    public void setProvisionedCount(Integer provisionedCount) { this.provisionedCount = provisionedCount; }
    public void setSkippedCount(Integer skippedCount) { this.skippedCount = skippedCount; }
    public void setErrorCount(Integer errorCount) { this.errorCount = errorCount; }
    public void setReportFileToken(String reportFileToken) { this.reportFileToken = reportFileToken; }
    public Integer getTotalCount() { return totalCount; }
    public Integer getSkippedAccountCount() { return skippedAccountCount; }
    public String getSMTPHost() { return SMTPHost; }
    public String getSMTPPort() { return SMTPPort; }
    public Integer getStatus() { return status; }
    public Integer getProvisionedCount() { return provisionedCount; }
    public Integer getSkippedCount() { return skippedCount; }
    public Integer getErrorCount() { return errorCount; }
    public String getReportFileToken() { return reportFileToken; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("totalCount", totalCount)
            .add("skippedAccountCount", skippedAccountCount)
            .add("SMTPHost", SMTPHost)
            .add("SMTPPort", SMTPPort)
            .add("status", status)
            .add("provisionedCount", provisionedCount)
            .add("skippedCount", skippedCount)
            .add("errorCount", errorCount)
            .add("reportFileToken", reportFileToken);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
