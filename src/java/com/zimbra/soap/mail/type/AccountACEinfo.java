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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class AccountACEinfo {

    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=false)
    private String zimbraId;

    @XmlAttribute(name=MailConstants.A_GRANT_TYPE /* gt */, required=true)
    private final Grant.GranteeType grantType;

    @XmlAttribute(name=MailConstants.A_RIGHT /* right */, required=true)
    private final String right;

    @XmlAttribute(name=MailConstants.A_DISPLAY /* d */, required=false)
    private String displayName;

    @XmlAttribute(name=MailConstants.A_ACCESSKEY /* key */, required=false)
    private String accessKey;

    @XmlAttribute(name=MailConstants.A_PASSWORD /* pw */, required=false)
    private String password;

    @XmlAttribute(name=MailConstants.A_DENY /* deny */, required=false)
    private ZmBoolean deny;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountACEinfo() {
        this((Grant.GranteeType) null, (String) null);
    }

    public AccountACEinfo(Grant.GranteeType grantType, String right) {
        this.grantType = grantType;
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
    public Grant.GranteeType getGrantType() { return grantType; }
    public String getRight() { return right; }
    public String getDisplayName() { return displayName; }
    public String getAccessKey() { return accessKey; }
    public String getPassword() { return password; }
    public Boolean getDeny() { return ZmBoolean.toBool(deny); }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("zimbraId", zimbraId)
            .add("grantType", grantType)
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
