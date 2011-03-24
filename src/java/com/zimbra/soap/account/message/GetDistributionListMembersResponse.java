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

package com.zimbra.soap.account.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AccountConstants.E_GET_DISTRIBUTION_LIST_MEMBERS_RESPONSE)
public class GetDistributionListMembersResponse {

    @XmlAttribute(name=AccountConstants.A_MORE, required=false)
    private Boolean more;

    @XmlAttribute(name=AccountConstants.A_TOTAL, required=false)
    private Integer total;

    @XmlElement(name=AccountConstants.E_DLM, required=false)
    private List<String> dlMembers = Lists.newArrayList();

    public GetDistributionListMembersResponse() {
    }

    public void setMore(Boolean more) { this.more = more; }
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

    public Boolean getMore() { return more; }
    public Integer getTotal() { return total; }
    public List<String> getDlMembers() {
        return Collections.unmodifiableList(dlMembers);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("more", more)
            .add("total", total)
            .add("dlMembers", dlMembers)
            .toString();
    }
}
