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

package com.zimbra.soap.account.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.HABGroupMember;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_DISTRIBUTION_LIST_MEMBERS_RESPONSE)
public class GetDistributionListMembersResponse {

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description 1 (true) if more members left to return
     */
    @XmlAttribute(name=AccountConstants.A_MORE /* more */, required=false)
    private ZmBoolean more;

    /**
     * @zm-api-field-tag total
     * @zm-api-field-description total number of distribution lists (not affected by limit/offset)
     */
    @XmlAttribute(name=AccountConstants.A_TOTAL /* total */, required=false)
    private Integer total;

    /**
     * @zm-api-field-description Distribution list members
     */
    @XmlElement(name=AccountConstants.E_DLM /* dlm */, required=false)
    private List<String> dlMembers = Lists.newArrayList();

    /**
     * @zm-api-field-description HAB Group members
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=AccountConstants.E_HAB_GROUP_MEMBERS /* groupMembers */, required=false)
    @XmlElement(name=AccountConstants.E_HAB_GROUP_MEMBER /* groupMember */, required=false)
    private List<HABGroupMember> habGroupMembers;

    public GetDistributionListMembersResponse() {
    }

    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }
    public void setTotal(Integer total) { this.total = total; }
    public void setDlMembers(Iterable <String> dlMembers) {
        this.dlMembers.clear();
        if (dlMembers != null) {
            Iterables.addAll(this.dlMembers,dlMembers);
        }
    }

    public GetDistributionListMembersResponse addDlMember(String dlMember) {
        this.dlMembers.add(dlMember);
        return this;
    }

    public Boolean getMore() { return ZmBoolean.toBool(more); }
    public Integer getTotal() { return total; }
    public List<String> getDlMembers() {
        return Collections.unmodifiableList(dlMembers);
    }

    public void setHABGroupMembers(Iterable <HABGroupMember> habGroupMembers) {
        this.habGroupMembers.clear();
        if (habGroupMembers != null) {
            Iterables.addAll(this.habGroupMembers,habGroupMembers);
        }
    }

    public GetDistributionListMembersResponse addHABGroupMember(HABGroupMember habGroupMember) {
        this.habGroupMembers.add(habGroupMember);
        return this;
    }

    public List<HABGroupMember> getHABGroupMembers() {
        return Collections.unmodifiableList(habGroupMembers);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("more", more)
            .add("total", total)
            .add("dlMembers", dlMembers)
            .add("habGroupMembers", habGroupMembers)
            .toString();
    }
}
