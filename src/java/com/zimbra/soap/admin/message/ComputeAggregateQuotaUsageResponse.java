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

package com.zimbra.soap.admin.message;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DomainAggregateQuotaInfo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_COMPUTE_AGGR_QUOTA_USAGE_RESPONSE)
public class ComputeAggregateQuotaUsageResponse {

    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private List <DomainAggregateQuotaInfo> domainQuotas = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private ComputeAggregateQuotaUsageResponse() {
        this(null);
    }

    public ComputeAggregateQuotaUsageResponse(Collection<DomainAggregateQuotaInfo> domainQuotas) {
        setDomainQuotas(domainQuotas);
    }

    public ComputeAggregateQuotaUsageResponse setDomainQuotas(
            Collection<DomainAggregateQuotaInfo> domainQuotas) {
        this.domainQuotas.clear();
        if (domainQuotas != null) {
            this.domainQuotas.addAll(domainQuotas);
        }
        return this;
    }

    public ComputeAggregateQuotaUsageResponse addDomainQuota(DomainAggregateQuotaInfo domainQuota) {
        domainQuotas.add(domainQuota);
        return this;
    }

    public List<DomainAggregateQuotaInfo> getDomainQuotas() {
        return Collections.unmodifiableList(domainQuotas);
    }
}
