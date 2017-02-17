/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
