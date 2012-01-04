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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_ACCOUNT_DISTRIBUTION_LISTS_REQUEST)
public class GetAccountDistributionListsRequest {
    
    public static enum MemberOfSelector {
        all,
        none,
        directOnly;
        
        public static MemberOfSelector fromString(String s) throws ServiceException {
            try {
                return MemberOfSelector.valueOf(s);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown NeedMemberOf: "+s, e);
            }
        }
    };

    @XmlAttribute(name=AccountConstants.A_OWNER_OF, required=false)
    private ZmBoolean ownerOf;
    @XmlAttribute(name=AccountConstants.A_MEMBER_OF, required=false)
    private MemberOfSelector memberOf;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetAccountDistributionListsRequest() {
        this((Boolean) null, (MemberOfSelector) null);
    }

    public GetAccountDistributionListsRequest(Boolean owner, MemberOfSelector member) {
        this.setOwnerOf(owner);
        this.setMemberOf(member);
    }

    public void setOwnerOf(Boolean owner) { this.ownerOf = ZmBoolean.fromBool(owner); }
    public Boolean getOwnerOf() { return ZmBoolean.toBool(ownerOf); }
    
    public void setMemberOf(MemberOfSelector member) { this.memberOf = member; }
    public MemberOfSelector getMemberOf() { return memberOf; }
}
