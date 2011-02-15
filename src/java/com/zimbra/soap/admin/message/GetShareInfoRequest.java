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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.AccountSelector;
import com.zimbra.soap.admin.type.GranteeSelector;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_SHARE_INFO_REQUEST)
public class GetShareInfoRequest {

    @XmlElement(name=AdminConstants.E_GRANTEE, required=false)
    private final GranteeSelector grantee;
    @XmlElement(name=AdminConstants.E_OWNER, required=true)
    private final AccountSelector owner;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetShareInfoRequest() {
        this((GranteeSelector) null, (AccountSelector) null);
    }

    public GetShareInfoRequest(AccountSelector owner) {
        this(null, owner);
    }

    public GetShareInfoRequest(GranteeSelector grantee, AccountSelector owner) {
        this.grantee = grantee;
        this.owner = owner;
    }

    public GranteeSelector getGrantee() { return grantee; }
    public AccountSelector getOwner() { return owner; }
}
