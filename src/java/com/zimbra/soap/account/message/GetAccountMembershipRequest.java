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

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_ACCOUNT_MEMBERSHIP_REQUEST)
public class GetAccountMembershipRequest {

    @XmlAttribute(name=AccountConstants.A_DIRECT_ONLY, required=false)
    private Boolean directOnly;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetAccountMembershipRequest() {
        this((Boolean) null);
    }

    public GetAccountMembershipRequest(Boolean directOnly) {
        this.setDirectOnly(directOnly);
    }

    public void setDirectOnly(Boolean directOnly) { this.directOnly = directOnly; }
    public Boolean getDirectOnly() { return directOnly; }
}
