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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ZimletAcl {

    /**
     * @zm-api-field-tag cos-name
     * @zm-api-field-description Name of Class Of Service (COS)
     */
    @XmlAttribute(name=AdminConstants.A_COS /* cos */, required=false)
    private final String cos;

    /**
     * @zm-api-field-tag acl-grant-or-deny
     * @zm-api-field-description <b>grant</b> or <b>deny</b>
     */
    @XmlAttribute(name=AdminConstants.A_ACL /* acl */, required=false)
    private final String acl;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ZimletAcl() {
        this((String) null, (String) null);
    }

    public ZimletAcl(String cos, String acl) {
        this.cos = cos;
        this.acl = acl;
    }

    public String getCos() { return cos; }
    public String getAcl() { return acl; }
}
