/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.mail.message;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;


@XmlRootElement(name=MailConstants.E_GET_DATA_SOURCE_USAGE_RESPONSE)
public class GetDataSourceUsageResponse {

    @XmlElements({
        @XmlElement(name=AccountConstants.E_DATA_SOURCE_USAGE, type=DataSourceUsage.class),
    })
    private List<DataSourceUsage> usages = Lists.newArrayList();

    @XmlElement(name=AccountConstants.E_DS_QUOTA, required=true)
    private Long dataSourceQuota;

    @XmlElement(name=AccountConstants.E_DS_TOTAL_QUOTA, required=true)
    private Long totalQuota;

    public GetDataSourceUsageResponse() {}

    public void addDataSourceUsage(DataSourceUsage usage) {
        usages.add(usage);
    }

    public List<DataSourceUsage> getUsages() { return usages; }

    public void setDataSourceQuota(Long quota) { this.dataSourceQuota = quota; }
    public Long getDataSourceQuota() { return dataSourceQuota; }

    public void setDataSourceTotalQuota(Long quota) { this.totalQuota = quota; }
    public Long getDataSourceTotalQuota() { return totalQuota; }
}
