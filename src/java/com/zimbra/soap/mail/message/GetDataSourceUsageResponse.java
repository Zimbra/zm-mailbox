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
