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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ZimletAclStatusPri {

    /**
     * @zm-api-field-tag zimlet-name
     * @zm-api-field-description Zimlet name
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-description Zimlet ACL
     */
    @XmlElement(name=AdminConstants.E_ACL /* acl */, required=false)
    private ZimletAcl acl;

    /**
     * @zm-api-field-description Status - valid values for <b>value</b>attribute - <b>enabled|disabled</b>
     */
    @XmlElement(name=AdminConstants.E_STATUS /* status */, required=false)
    private ValueAttrib status;

    /**
     * @zm-api-field-description Priority
     */
    @XmlElement(name=AdminConstants.E_PRIORITY /* priority */, required=false)
    private IntegerValueAttrib priority;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ZimletAclStatusPri() {
        this((String) null);
    }

    public ZimletAclStatusPri(String name) {
        this.name = name;
    }

    public void setAcl(ZimletAcl acl) { this.acl = acl; }
    public void setStatus(ValueAttrib status) { this.status = status; }
    public void setPriority(IntegerValueAttrib priority) {
        this.priority = priority;
    }

    public String getName() { return name; }
    public ZimletAcl getAcl() { return acl; }
    public ValueAttrib getStatus() { return status; }
    public IntegerValueAttrib getPriority() { return priority; }
}
