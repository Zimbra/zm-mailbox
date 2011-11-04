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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.type.GranteeChooser;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AccountConstants.E_GET_SHARE_INFO_REQUEST)
public class GetShareInfoRequest {

    @XmlAttribute(name=AccountConstants.A_INTERNAL, required=false)
    private ZmBoolean internal;

    @XmlAttribute(name=AccountConstants.A_INCLUDE_SELF, required=false)
    private ZmBoolean includeSelf;

    @XmlElement(name=AccountConstants.E_GRANTEE, required=false)
    private GranteeChooser grantee;

    @XmlElement(name=AccountConstants.E_OWNER, required=false)
    private AccountSelector owner;

    public GetShareInfoRequest() {
    }

    public void setInternal(Boolean internal) { this.internal = ZmBoolean.fromBool(internal); }
    public void setIncludeSelf(Boolean includeSelf) { this.includeSelf = ZmBoolean.fromBool(includeSelf); }

    public void setGrantee(GranteeChooser grantee) { this.grantee = grantee; }
    public void setOwner(AccountSelector owner) { this.owner = owner; }
    public Boolean getInternal() { return ZmBoolean.toBool(internal); }
    public Boolean getIncludeSelf() { return ZmBoolean.toBool(includeSelf); }
    public GranteeChooser getGrantee() { return grantee; }
    public AccountSelector getOwner() { return owner; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("internal", internal)
            .add("includeSelf", includeSelf)
            .add("grantee", grantee)
            .add("owner", owner)
            .toString();
    }
}
