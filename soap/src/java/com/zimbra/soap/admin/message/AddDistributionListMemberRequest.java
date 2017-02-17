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

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Adding members to a distribution list
 * <br />
 * Access: domain admin sufficient
 * <br />
 * <br />
 * Adding existing members is allowed, even if it may result in this request being a no-op because all &lt;dlm> 
 * addrs are already members.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_ADD_DISTRIBUTION_LIST_MEMBER_REQUEST)
public class AddDistributionListMemberRequest {

    /**
     * @zm-api-field-tag value-of-zimbra-id
     * @zm-api-field-description Zimbra ID
     */
    @XmlAttribute(name=AdminConstants.E_ID, required=true)
    private String id;

    /**
     * @zm-api-field-description Members
     */
    @XmlElement(name=AdminConstants.E_DLM, required=true)
    private List <String> members = Lists.newArrayList();

    public AddDistributionListMemberRequest() {
        this((String) null, (Collection<String>) null);
    }

    public AddDistributionListMemberRequest(String id, Collection<String> members) {
        setId(id);
        setMembers(members);
    }

    public AddDistributionListMemberRequest setMembers(Collection<String> members) {
        this.members.clear();
        if (members != null) {
            this.members.addAll(members);
        }
        return this;
    }

    public AddDistributionListMemberRequest addMember(String member) {
        members.add(member);
        return this;
    }

    public List<String> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }
}
