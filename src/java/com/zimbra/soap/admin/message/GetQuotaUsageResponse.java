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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_QUOTA_USAGE_RESPONSE)
public class GetQuotaUsageResponse {

    @XmlAttribute(name=AdminConstants.A_MORE, required=true)
    private final ZmBoolean more;
    @XmlAttribute(name=AdminConstants.A_SEARCH_TOTAL, required=true)
    private final int searchTotal;
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
