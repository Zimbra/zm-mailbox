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
package com.zimbra.soap.account.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AccountConstants.E_DL_SUBS_REQ)
public class DistributionListSubscribeReq {
    
    @XmlAttribute(name=AccountConstants.A_OP, required=true)
    private DistributionListSubscribeOp op;
    @XmlAttribute(name=AccountConstants.A_BCC_OWNERS, required=false)
    private boolean bccOwners = true;
    @XmlValue
    private String memberEmail;
    
    public DistributionListSubscribeReq() {
        this((DistributionListSubscribeOp) null, (String) null);
    }
    
    public DistributionListSubscribeReq(DistributionListSubscribeOp op, String memberEmail) {
        setOp(op);
        setMemberEmail(memberEmail);
    }
    
    public void setOp(DistributionListSubscribeOp op) {
        this.op = op;
    }
    
    public DistributionListSubscribeOp getOp() {
        return op;
    }
    
    public void setBccOwners(boolean bccOwners) {
        this.bccOwners = bccOwners;
    }
    
    public boolean getBccOwners() {
        return bccOwners;
    }
    
    public void setMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }
    
    public String getMemberEmail() {
        return memberEmail;
    }

}
