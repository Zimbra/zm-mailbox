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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.admin.type.AccountInfo;
import com.zimbra.soap.admin.type.AdminObjectInterface;
import com.zimbra.soap.admin.type.AliasInfo;
import com.zimbra.soap.admin.type.CalendarResourceInfo;
import com.zimbra.soap.admin.type.CosInfo;
import com.zimbra.soap.admin.type.DomainInfo;
import com.zimbra.soap.admin.type.DistributionListInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SEARCH_ACCOUNTS_RESPONSE)
public class SearchAccountsResponse {
    // TODO: SearchAccountsResponse/SearchDirectoryResponse are very similar.
    //       Worth extending a base class?

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description <b>1 (true)</b> if more accounts left to return
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
     * @zm-api-field-description Search hits
     */
    @XmlElements({
        @XmlElement(name=AccountConstants.E_CALENDAR_RESOURCE, type=CalendarResourceInfo.class),
        @XmlElement(name=AdminConstants.E_DL, type=DistributionListInfo.class),
        @XmlElement(name=AdminConstants.E_ALIAS, type=AliasInfo.class),
        @XmlElement(name=AdminConstants.E_ACCOUNT, type=AccountInfo.class),
        @XmlElement(name=AdminConstants.E_DOMAIN, type=DomainInfo.class),
        @XmlElement(name=AdminConstants.E_COS, type=CosInfo.class)
    })
    private List<AdminObjectInterface> entries = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SearchAccountsResponse() {
        this(false, -1);
    }

    public SearchAccountsResponse(boolean more, int searchTotal) {
        this.more = ZmBoolean.fromBool(more);
        this.searchTotal = searchTotal;
    }

    public SearchAccountsResponse setEntries(Collection<AdminObjectInterface> entries) {
        this.entries.clear();
        if (entries != null) {
            this.entries.addAll(entries);
        }
        return this;
    }

    public SearchAccountsResponse addEntry(AdminObjectInterface entry) {
        entries.add(entry);
        return this;
    }

    public List<AdminObjectInterface> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public boolean getMore() { return ZmBoolean.toBool(more); }
    public int getSearchTotal() { return searchTotal; }

    public List<CalendarResourceInfo> getCalendarResources() {
        List<CalendarResourceInfo> subset = Lists.newArrayList();
        for (AdminObjectInterface entry : entries) {
            if (entry instanceof CalendarResourceInfo)
                subset.add((CalendarResourceInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }

    public List<DistributionListInfo> getDistributionLists() {
        List<DistributionListInfo> subset = Lists.newArrayList();
        for (AdminObjectInterface entry : entries) {
            if (entry instanceof DistributionListInfo)
                subset.add((DistributionListInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }

    public List<AliasInfo> getAliases() {
        List<AliasInfo> subset = Lists.newArrayList();
        for (AdminObjectInterface entry : entries) {
            if (entry instanceof AliasInfo)
                subset.add((AliasInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }

    public List<AccountInfo> getAccounts() {
        List<AccountInfo> subset = Lists.newArrayList();
        for (AdminObjectInterface entry : entries) {
            if (entry instanceof AccountInfo)
                subset.add((AccountInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }

    public List<DomainInfo> getDomains() {
        List<DomainInfo> subset = Lists.newArrayList();
        for (AdminObjectInterface entry : entries) {
            if (entry instanceof DomainInfo)
                subset.add((DomainInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }

    public List<CosInfo> getCOSes() {
        List<CosInfo> subset = Lists.newArrayList();
        for (AdminObjectInterface entry : entries) {
            if (entry instanceof CosInfo)
                subset.add((CosInfo) entry);
        }
        return Collections.unmodifiableList(subset);
    }
}
