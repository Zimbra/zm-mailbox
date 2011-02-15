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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.admin.type.DistributionListSelector;
import com.zimbra.soap.admin.type.AccountSelector;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_PUBLISHED_SHARE_INFO_REQUEST)
public class GetPublishedShareInfoRequest {

    @XmlElement(name=AdminConstants.E_DL, required=true)
    private final DistributionListSelector dl;
    @XmlElement(name=AccountConstants.E_OWNER, required=false)
    private final AccountSelector owner;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetPublishedShareInfoRequest() {
        this((DistributionListSelector) null, (AccountSelector) null);
    }

    public GetPublishedShareInfoRequest(DistributionListSelector dl, 
            AccountSelector owner) {
        this.dl = dl;
        this.owner = owner;
    }


    public DistributionListSelector getDl() { return dl; }
    public AccountSelector getOwner() { return owner; }
}
