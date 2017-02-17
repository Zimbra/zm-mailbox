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

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_AGGR_QUOTA_USAGE_ON_SERVER_RESPONSE)
public class GetAggregateQuotaUsageOnServerResponse {

    /**
     * @zm-api-field-description Aggregate quota information reported per domain
     */
    @XmlElement(name=AdminConstants.E_DOMAIN /* domain */, required=false)
    private List <DomainAggregateQuotaInfo> domainQuotas = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private GetAggregateQuotaUsageOnServerResponse() {
        this(null);
    }

    public GetAggregateQuotaUsageOnServerResponse(Collection<DomainAggregateQuotaInfo> domainQuotas) {
        setDomainQuotas(domainQuotas);
    }

    public GetAggregateQuotaUsageOnServerResponse setDomainQuotas(
            Collection<DomainAggregateQuotaInfo> domainQuotas) {
        this.domainQuotas.clear();
        if (domainQuotas != null) {
            this.domainQuotas.addAll(domainQuotas);
        }
        return this;
    }

    public GetAggregateQuotaUsageOnServerResponse addDomainQuota(DomainAggregateQuotaInfo domainQuota) {
        domainQuotas.add(domainQuota);
        return this;
    }

    public List<DomainAggregateQuotaInfo> getDomainQuotas() {
        return Collections.unmodifiableList(domainQuotas);
    }
}
