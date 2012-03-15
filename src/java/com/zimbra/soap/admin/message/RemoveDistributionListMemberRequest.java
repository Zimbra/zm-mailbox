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

/**
 * @zm-api-command-description Remove Distribution List Member
 * <br />
 * Unlike add, remove of a non-existent member causes an exception and no modification to the list.
 * <br />
 * <b>Access</b>: domain admin sufficient
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_REMOVE_DISTRIBUTION_LIST_MEMBER_REQUEST)
public class RemoveDistributionListMemberRequest {

    /**
     * @zm-api-field-tag value-of-zimbra-id
     * @zm-api-field-description Zimbra ID
     */
    @XmlAttribute(name=AdminConstants.E_ID, required=true)
    private String id;

    /**
     * @zm-api-field-tag member
     * @zm-api-field-description Members
     */
    @XmlElement(name=AdminConstants.E_DLM, required=true)
    private List <String> members = Lists.newArrayList();

    public RemoveDistributionListMemberRequest() {
        this((String) null, (Collection<String>) null);
    }

    public RemoveDistributionListMemberRequest(String id, Collection<String> members) {
        setId(id);
        setMembers(members);
    }

    public RemoveDistributionListMemberRequest setMembers(Collection<String> members) {
        this.members.clear();
        if (members != null) {
            this.members.addAll(members);
        }
        return this;
    }

    public RemoveDistributionListMemberRequest addMember(String member) {
        members.add(member);
        return this;
    }

    public List<String> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void setId(String id) { this.id = id; }
    public String getId() { return id; }
}
