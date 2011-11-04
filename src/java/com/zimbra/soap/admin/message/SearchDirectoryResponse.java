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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.admin.type.AdminObjectInfo;
import com.zimbra.soap.admin.type.AccountInfo;
import com.zimbra.soap.admin.type.AliasInfo;
import com.zimbra.soap.admin.type.CalendarResourceInfo;
import com.zimbra.soap.admin.type.CosInfo;
import com.zimbra.soap.admin.type.DomainInfo;
import com.zimbra.soap.admin.type.DistributionListInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_SEARCH_DIRECTORY_RESPONSE)
@XmlType(propOrder = {})
public class SearchDirectoryResponse {

    @XmlAttribute(name=AdminConstants.A_MORE, required=true)
    private ZmBoolean more;
    @XmlAttribute(name=AdminConstants.A_SEARCH_TOTAL, required=true)
    private long searchTotal;
    @XmlElements({
        @XmlElement(name=AccountConstants.E_CALENDAR_RESOURCE, type=CalendarResourceInfo.class),
        @XmlElement(name=AdminConstants.E_DL, type=DistributionListInfo.class),
        @XmlElement(name=AdminConstants.E_ALIAS, type=AliasInfo.class),
        @XmlElement(name=AdminConstants.E_ACCOUNT, type=AccountInfo.class),
        @XmlElement(name=AdminConstants.E_DOMAIN, type=DomainInfo.class),
        @XmlElement(name=AdminConstants.E_COS, type=CosInfo.class)
    })
    private List<AdminObjectInfo> entries = Lists.newArrayList();

    public SearchDirectoryResponse() {
    }

    public SearchDirectoryResponse setEntries(Collection<AdminObjectInfo> entries) {
        this.entries.clear();
        if (entries != null) {
            this.entries.addAll(entries);
        }
        return this;
    }

    public void setMore(boolean more) {
        this.more = ZmBoolean.fromBool(more);
    }

    public void setSearchTotal(long searchTotal) {
        this.searchTotal = searchTotal;
    }

    public SearchDirectoryResponse addEntry(AdminObjectInfo entry) {
        entries.add(entry);
        return this;
    }

    public List<AdminObjectInfo> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public long getSearchTotal() { return searchTotal; }
    public boolean isMore() { return ZmBoolean.toBool(more); }

    public List<CalendarResourceInfo> getCalendarResources() {
        List<CalendarResourceInfo> subset = Lists.newArrayList();
        for (AdminObjectInfo entry : entries) {
            if (entry instanceof CalendarResourceInfo)
                subset.add((CalendarResourceInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }

    public List<DistributionListInfo> getDistributionLists() {
        List<DistributionListInfo> subset = Lists.newArrayList();
        for (AdminObjectInfo entry : entries) {
            if (entry instanceof DistributionListInfo)
                subset.add((DistributionListInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }

    public List<AliasInfo> getAliases() {
        List<AliasInfo> subset = Lists.newArrayList();
        for (AdminObjectInfo entry : entries) {
            if (entry instanceof AliasInfo)
                subset.add((AliasInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }

    public List<AccountInfo> getAccounts() {
        List<AccountInfo> subset = Lists.newArrayList();
        for (AdminObjectInfo entry : entries) {
            if (entry instanceof AccountInfo)
                subset.add((AccountInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }

    public List<DomainInfo> getDomains() {
        List<DomainInfo> subset = Lists.newArrayList();
        for (AdminObjectInfo entry : entries) {
            if (entry instanceof DomainInfo)
                subset.add((DomainInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }

    public List<CosInfo> getCOSes() {
        List<CosInfo> subset = Lists.newArrayList();
        for (AdminObjectInfo entry : entries) {
            if (entry instanceof CosInfo)
                subset.add((CosInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }
}
