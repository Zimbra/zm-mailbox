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
@XmlRootElement(name=AdminExtConstants.E_BULK_IMPORT_ACCOUNTS_RESPONSE)
@XmlType(propOrder = {})
public class BulkImportAccountsResponse {

    @XmlElement(name=AdminExtConstants.E_totalCount
                /* totalCount */, required=false)
    private Integer totalCount;

    @XmlElement(name=AdminExtConstants.E_skippedAccountCount
                /* skippedAccountCount */, required=false)
    private Integer skippedAccountCount;

    @XmlElement(name=AdminExtConstants.E_SMTPHost
                /* SMTPHost */, required=false)
    private String SMTPHost;

    @XmlElement(name=AdminExtConstants.E_SMTPPort
                /* SMTPPort */, required=false)
    private String SMTPPort;

    @XmlElement(name=AdminExtConstants.E_status
                /* status */, required=false)
    private Integer status;

    @XmlElement(name=AdminExtConstants.E_provisionedCount
                /* provisionedCount */, required=false)
    private Integer provisionedCount;

    @XmlElement(name=AdminExtConstants.E_skippedCount
                /* skippedCount */, required=false)
    private Integer skippedCount;

    @XmlElement(name=AdminExtConstants.E_errorCount
                /* errorCount */, required=false)
    private Integer errorCount;

    @XmlElement(name=AdminExtConstants.E_reportFileToken
                /* fileToken */, required=false)
    private String reportFileToken;

    public BulkImportAccountsResponse() {
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
    public void setSkippedAccountCount(Integer skippedAccountCount) {
        this.skippedAccountCount = skippedAccountCount;
    }
    public void setSMTPHost(String SMTPHost) { this.SMTPHost = SMTPHost; }
    public void setSMTPPort(String SMTPPort) { this.SMTPPort = SMTPPort; }
    public void setStatus(Integer status) { this.status = status; }
    public void setProvisionedCount(Integer provisionedCount) {
        this.provisionedCount = provisionedCount;
    }
    public void setSkippedCount(Integer skippedCount) {
        this.skippedCount = skippedCount;
    }
    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }
    public void setReportFileToken(String reportFileToken) {
        this.reportFileToken = reportFileToken;
    }
    public Integer getTotalCount() { return totalCount; }
    public Integer getSkippedAccountCount() { return skippedAccountCount; }
    public String getSMTPHost() { return SMTPHost; }
    public String getSMTPPort() { return SMTPPort; }
    public Integer getStatus() { return status; }
    public Integer getProvisionedCount() { return provisionedCount; }
    public Integer getSkippedCount() { return skippedCount; }
    public Integer getErrorCount() { return errorCount; }
    public String getReportFileToken() { return reportFileToken; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
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
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
