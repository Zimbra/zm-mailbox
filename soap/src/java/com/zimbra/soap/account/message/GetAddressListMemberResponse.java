package com.zimbra.soap.account.message;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AccountConstants.E_GET_ADDRESS_LIST_MEMBERS_RESPONSE)
public class GetAddressListMemberResponse {

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description 1 (true) if more members left to return
     */
    @XmlAttribute(name = AccountConstants.A_MORE /* more */, required = false)
    private ZmBoolean more;

    /**
     * @zm-api-field-tag total
     * @zm-api-field-description total number of distribution lists (not
     *                           affected by limit/offset)
     */
    @XmlAttribute(name = AccountConstants.A_TOTAL /* total */, required = true)
    private Integer total;

    /**
     * @zm-api-field-description Distribution list members
     */
    @XmlElement(name = AccountConstants.E_ADDRESS_LIST_MEMBERS /* alm */, required = false)
    private List<String> adlMembers = Lists.newArrayList();

    public GetAddressListMemberResponse() {

    }

    public ZmBoolean getMore() {
        return more;
    }

    public void setMore(ZmBoolean more) {
        this.more = more;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public List<String> getAdlMembers() {
        return adlMembers;
    }

    public void setAdlMembers(List<String> adlMembers) {
        this.adlMembers = adlMembers;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GetAddressListMemberResponse [");
        if (more != null) {
            builder.append("more=");
            builder.append(more);
            builder.append(", ");
        }
        if (total != null) {
            builder.append("total=");
            builder.append(total);
            builder.append(", ");
        }
        if (adlMembers != null) {
            builder.append("adlMembers=");
            builder.append(adlMembers);
        }
        builder.append("]");
        return builder.toString();
    }

    
}
