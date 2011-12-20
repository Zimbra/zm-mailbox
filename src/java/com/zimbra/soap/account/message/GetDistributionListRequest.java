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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.AttrsImpl;
import com.zimbra.soap.type.DistributionListSelector;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_DISTRIBUTION_LIST_REQUEST)
public class GetDistributionListRequest extends AttrsImpl {

    @XmlAttribute(name=AccountConstants.A_NEED_OWNERS, required=false)
    private ZmBoolean needOwners;
    
    @XmlAttribute(name=AccountConstants.A_NEED_RIGHTS, required=false)
    private String needRights;
    
    @XmlElement(name=AccountConstants.E_DL, required=true)
    private DistributionListSelector dl;

    public GetDistributionListRequest() {
        this((DistributionListSelector) null, (Boolean) null, (String) null);
    }
    
    public GetDistributionListRequest(DistributionListSelector dl, Boolean needOwners) {
        this(dl, needOwners, null);
    }

    public GetDistributionListRequest(DistributionListSelector dl, Boolean needOwners, 
            String needRights) {
        this.setDl(dl);
        this.setNeedOwners(needOwners);
        this.setNeedRights(needRights);
    }

    public void setNeedOwners(Boolean needOwners) { 
        this.needOwners = ZmBoolean.fromBool(needOwners); 
    }
    
    public void setNeedRights(String needRights) { 
        this.needRights = needRights;
    }

    public Boolean getNeedOwners() { return ZmBoolean.toBool(needOwners); }
    
    public String getNeedRights() { return needRights; }

    public void setDl(DistributionListSelector dl) { this.dl = dl; }
    public DistributionListSelector getDl() { return dl; }

}
