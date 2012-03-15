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

/**
 * @zm-api-command-description Returns groups the user is either a member or an owner of.
 * <br />
 * Notes: 
 * <ul>
 * <li> <b>isOwner</b> is returned only if <b>ownerOf</b> on the request is 1 (true).
 * <li> <b>isMember</b> is returned only if <b>memberOf</b> on the request is not "none".
 * </ul>
 * For example, if isOwner="1" and isMember="none" on the request, and user is an owner and a member of a group,
 * the returned entry for the group will have isOwner="1", but isMember will not be present.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_ACCOUNT_DISTRIBUTION_LISTS_REQUEST)
public class GetAccountDistributionListsRequest {
    private static Joiner COMMA_JOINER = Joiner.on(",");

    /**
     * @zm-api-field-tag owner-of
     * @zm-api-field-description Set to 1 if the response should include groups the user is an owner of.
     * <br />Set to 0 (default) if do not need to know which groups the user is an owner of.
     */
    @XmlAttribute(name=AccountConstants.A_OWNER_OF, required=false)
    private ZmBoolean ownerOf;

    /**
     * @zm-api-field-tag member-of
     * @zm-api-field-description Possible values:
     * <ol>
     * <li> <b>all</b> - need all groups the user is a direct or indirect member of.
     * <li> <b>none</b> - do not need groups the user is a member of.
     * <li> <b>directOnly</b> (default) - need groups the account is a direct member of.
     * </ol>
     */
    @XmlAttribute(name=AccountConstants.A_MEMBER_OF, required=false)
    private MemberOfSelector memberOf;

    private List<String> attrs = Lists.newArrayList();

    /**
     * @zm-api-field-tag attrs
     * @zm-api-field-description comma-seperated attributes to return.  Note: non-owner user can see only certain 
     * attributes of a group.  If a specified attribute is not visible to the user, it will not be returned.
     */
    @XmlAttribute(name=AccountConstants.A_ATTRS)
    public String getAttrs() {
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
