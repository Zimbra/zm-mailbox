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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AccountQuotaInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_QUOTA_USAGE_RESPONSE)
public class GetQuotaUsageResponse {

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description <b>1 (true)</b> if there are more accounts left to return
     */
    @XmlAttribute(name=AdminConstants.A_MORE, required=true)
    private final ZmBoolean more;

    /**
     * @zm-api-field-tag search-total
     * @zm-api-field-description Total number of accounts that matched search (not affected by limit/offset)
     */
    @XmlAttribute(name=AdminConstants.A_SEARCH_TOTAL, required=true)
    private final int searchTotal;

    /**
     * @zm-api-field-description Account quota information
     */
    @XmlElement(name=AdminConstants.E_ACCOUNT, required=false)
    private List <AccountQuotaInfo> accountQuotas = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
     @SuppressWarnings("unused")
    private GetQuotaUsageResponse() {
        this(false, -1);
    }

    public GetQuotaUsageResponse(boolean more, int searchTotal) {
        this(more, searchTotal, (Collection <AccountQuotaInfo>) null);
    }

    public GetQuotaUsageResponse(boolean more, int searchTotal,
            Collection <AccountQuotaInfo> accountQuotas) {
        setAccountQuotas(accountQuotas);
        this.more = ZmBoolean.fromBool(more);
        this.searchTotal = searchTotal;
    }

    public GetQuotaUsageResponse setAccountQuotas(
            Collection <AccountQuotaInfo> accountQuotas) {
        this.accountQuotas.clear();
        if (accountQuotas != null) {
            this.accountQuotas.addAll(accountQuotas);
        }
        return this;
    }

    public GetQuotaUsageResponse addAccountQuota(AccountQuotaInfo accountQuota) {
        accountQuotas.add(accountQuota);
        return this;
    }

    public List<AccountQuotaInfo> getAccountQuotas() {
        return Collections.unmodifiableList(accountQuotas);
    }

    public boolean isMore() { return ZmBoolean.toBool(more); }
    public int getSearchTotal() { return searchTotal; }
}
