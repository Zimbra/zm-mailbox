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
