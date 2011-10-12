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

@XmlAccessorType(XmlAccessType.NONE)
public class DocumentActionGrant {

    @XmlAttribute(name=MailConstants.A_RIGHTS /* perm */, required=true)
    private String rights;

    @XmlAttribute(name=MailConstants.A_GRANT_TYPE /* gt */, required=true)
    private String grantType;

    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=false)
    private String zimbraId;

    public DocumentActionGrant() {
        this((String)null, (String)null, (String)null);
    }

    private DocumentActionGrant(String rights, String grantType) {
        this(rights, grantType, (String)null);
    }

    private DocumentActionGrant(String rights, String grantType, String zimbraId) {
        setRights(rights);
        setGrantType(grantType);
        setZimbraId(zimbraId);
    }

    public static DocumentActionGrant createForRightsAndGrantType(String rights, String grantType) {
        return new DocumentActionGrant(rights, grantType);
    }

    public static DocumentActionGrant createForRightsGrantTypeAndZimbraId(
            String rights, String grantType, String zimbraId) {
        return new DocumentActionGrant(rights, grantType, zimbraId);
    }

    public void setRights(String rights) { this.rights = rights; }
    public void setGrantType(String grantType) { this.grantType = grantType; }
    public void setZimbraId(String zimbraId) { this.zimbraId = zimbraId; }
    public String getRights() { return rights; }
    public String getGrantType() { return grantType; }
    public String getZimbraId() { return zimbraId; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("rights", rights)
            .add("grantType", grantType)
            .add("zimbraId", zimbraId);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
