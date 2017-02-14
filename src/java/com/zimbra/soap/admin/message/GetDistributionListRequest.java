/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DistributionListSelector;
import com.zimbra.soap.type.AttributeSelectorImpl;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get a Distribution List
 * <br />
 * <b>Access</b>: domain admin sufficient
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_DISTRIBUTION_LIST_REQUEST)
public class GetDistributionListRequest extends AttributeSelectorImpl {

    /**
     * @zm-api-field-tag limit
     * @zm-api-field-description The maximum number of accounts to return (0 is default and means all)
     */
    @XmlAttribute(name=AdminConstants.A_LIMIT, required=false)
    private Integer limit;

    /**
     * @zm-api-field-tag starting-offset
     * @zm-api-field-description The starting offset (0, 25 etc)
     */
    @XmlAttribute(name=AdminConstants.A_OFFSET, required=false)
    private Integer offset;

    /**
     * @zm-api-field-tag sort-ascending
     * @zm-api-field-description Flag whether to sort in ascending order <b>1 (true)</b> is the default
     */
    @XmlAttribute(name=AdminConstants.A_SORT_ASCENDING, required=false)
    private ZmBoolean sortAscending;

    /**
     * @zm-api-field-description Distribution List
     */
    @XmlElement(name=AdminConstants.E_DL, required=false)
    private DistributionListSelector dl;

    public GetDistributionListRequest() {
        this((DistributionListSelector) null,
            (Integer) null, (Integer) null, (Boolean) null);
    }

    public GetDistributionListRequest(DistributionListSelector dl) {
        this(dl, (Integer) null, (Integer) null, (Boolean) null);
    }

    public GetDistributionListRequest(DistributionListSelector dl,
            Integer limit, Integer offset, Boolean sortAscending) {
        setDl(dl);
        setLimit(limit);
        setOffset(offset);
        setSortAscending(sortAscending);
    }

    public void setDl(DistributionListSelector dl) { this.dl = dl; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public void setSortAscending(Boolean sortAscending) {
        this.sortAscending = ZmBoolean.fromBool(sortAscending);
    }

    public DistributionListSelector getDl() { return dl; }
    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
    public Boolean isSortAscending() { return ZmBoolean.toBool(sortAscending); }
}
