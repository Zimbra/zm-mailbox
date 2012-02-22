/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

    /**
     * @zm-api-field-tag rights-rwd
     * @zm-api-field-description Permissions - (r)ead, (w)rite, (d)elete
     */
    @XmlAttribute(name=MailConstants.A_RIGHTS /* perm */, required=true)
    private String rights;

    /**
     * @zm-api-field-tag grant-type-all|pub
     * @zm-api-field-description Grant type - <b>all|pub</b>
     */
    @XmlAttribute(name=MailConstants.A_GRANT_TYPE /* gt */, required=true)
    private String grantType;

    /**
     * @zm-api-field-tag expiry-millis
     * @zm-api-field-description (Optional) Time when this grant expires in milliseconds since the Epoch
     */
    @XmlAttribute(name=MailConstants.A_EXPIRY /* expiry */, required=false)
    private Long expiry;

    public DocumentActionGrant() {
        this((String)null, (String)null, (Long)null);
    }

    private DocumentActionGrant(String rights, String grantType) {
        this(rights, grantType, (Long)null);
    }

    private DocumentActionGrant(String rights, String grantType, Long expiry) {
        setRights(rights);
        setGrantType(grantType);
        setExpiry(expiry);
    }

    public static DocumentActionGrant createForRightsAndGrantType(String rights, String grantType) {
        return new DocumentActionGrant(rights, grantType);
    }

    public static DocumentActionGrant createForRightsGrantTypeAndExpiry(String rights, String grantType, Long expiry) {
        return new DocumentActionGrant(rights, grantType, expiry);
    }

    public void setRights(String rights) { this.rights = rights; }
    public void setGrantType(String grantType) { this.grantType = grantType; }
    public void setExpiry(Long expiry) { this.expiry = expiry; }
    public String getRights() { return rights; }
    public String getGrantType() { return grantType; }
    public Long getExpiry() { return expiry; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("rights", rights)
            .add("grantType", grantType)
            .add("expiry", expiry);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
