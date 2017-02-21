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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.DistributionListInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_DISTRIBUTION_LIST_RESPONSE)
public class GetDistributionListResponse {

    /*
     * more/total are only present if the list of members is given
     */
    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description <b>1 (true)</b> if more mailboxes left to return
     * <br />
     * Only present if the list of members is given
     */
    @XmlAttribute(name=AdminConstants.A_MORE, required=false)
    ZmBoolean more;

    /**
     * @zm-api-field-tag total-members
     * @zm-api-field-description  Total number of members (not affected by limit/offset)
     * <br />
     * Only present if the list of members is given
     */
    @XmlAttribute(name=AdminConstants.A_TOTAL, required=false)
    Integer total;

    /**
     * @zm-api-field-description Information about distribution list
     */
    @XmlElement(name=AdminConstants.E_DL, required=false)
    DistributionListInfo dl;

    public GetDistributionListResponse() {
        this((DistributionListInfo)null, (Boolean) null, (Integer) null);
    }

    public GetDistributionListResponse(DistributionListInfo dl,
            Boolean more, Integer total) {
        setDl(dl);
        setMore(more);
        setTotal(total);
    }

    public void setDl(DistributionListInfo dl) { this.dl = dl; }
    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }
    public void setTotal(Integer total) { this.total = total; }

    public DistributionListInfo getDl() { return dl; }
    public Boolean isMore() { return ZmBoolean.toBool(more); }
    public Integer getTotal() { return total; }
}
