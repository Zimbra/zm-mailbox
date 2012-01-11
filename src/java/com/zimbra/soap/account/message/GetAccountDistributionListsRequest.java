/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.MemberOfSelector;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_ACCOUNT_DISTRIBUTION_LISTS_REQUEST)
public class GetAccountDistributionListsRequest {
    private static Joiner COMMA_JOINER = Joiner.on(",");
    
    @XmlAttribute(name=AccountConstants.A_OWNER_OF, required=false)
    private ZmBoolean ownerOf;
    @XmlAttribute(name=AccountConstants.A_MEMBER_OF, required=false)
    private MemberOfSelector memberOf;
    
    private List<String> attrs = Lists.newArrayList();
    @XmlAttribute(name=AccountConstants.A_ATTRS) public String getAttrs() {
        return COMMA_JOINER.join(attrs);
    }
    
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetAccountDistributionListsRequest() {
        this((Boolean) null, (MemberOfSelector) null, (Iterable<String>) null);
    }

    public GetAccountDistributionListsRequest(Boolean owner, MemberOfSelector member,
            Iterable<String> attrs) {
        setOwnerOf(owner);
        setMemberOf(member);
        setAttrs(attrs);
    }

    public void setOwnerOf(Boolean owner) { this.ownerOf = ZmBoolean.fromBool(owner); }
    public Boolean getOwnerOf() { return ZmBoolean.toBool(ownerOf); }
    
    public void setMemberOf(MemberOfSelector member) { this.memberOf = member; }
    public MemberOfSelector getMemberOf() { return memberOf; }
    
    public void setAttrs(Iterable<String> attrs) {
        if (attrs != null) {
            this.attrs = Lists.newArrayList();
            for (String attr : attrs) {
                this.attrs.add(attr);
            }
        }
    }

}
