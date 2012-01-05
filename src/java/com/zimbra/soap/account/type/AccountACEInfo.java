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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.GranteeType;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class AccountACEInfo {

    @XmlAttribute(name=AccountConstants.A_ZIMBRA_ID /* zid */, required=false)
    private String zimbraId;

    @XmlAttribute(name=AccountConstants.A_GRANT_TYPE /* gt */, required=true)
    private final GranteeType granteeType;

    @XmlAttribute(name=AccountConstants.A_RIGHT /* right */, required=true)
    private final String right;

    @XmlAttribute(name=AccountConstants.A_DISPLAY /* d */, required=false)
    private String displayName;

    @XmlAttribute(name=AccountConstants.A_ACCESSKEY /* key */, required=false)
    private String accessKey;

    @XmlAttribute(name=AccountConstants.A_PASSWORD /* pw */, required=false)
    private String password;

    @XmlAttribute(name=AccountConstants.A_DENY /* deny */, required=false)
    private ZmBoolean deny;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountACEInfo() {
        this((GranteeType) null, (String) null);
    }

    public AccountACEInfo(GranteeType granteeType, String right) {
        this.granteeType = granteeType;
        this.right = right;
    }

    public void setZimbraId(String zimbraId) { this.zimbraId = zimbraId; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public void setPassword(String password) { this.password = password; }
    public void setDeny(Boolean deny) { this.deny = ZmBoolean.fromBool(deny); }
    public String getZimbraId() { return zimbraId; }
    public GranteeType getGranteeType() { return granteeType; }
    public String getRight() { return right; }
    public String getDisplayName() { return displayName; }
    public String getAccessKey() { return accessKey; }
    public String getPassword() { return password; }
    public Boolean getDeny() { return ZmBoolean.toBool(deny); }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("zimbraId", zimbraId)
            .add("granteeType", granteeType)
            .add("right", right)
            .add("displayName", displayName)
            .add("accessKey", accessKey)
            .add("password", password)
            .add("deny", deny);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
