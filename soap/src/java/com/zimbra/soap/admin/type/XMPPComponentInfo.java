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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;

@XmlAccessorType(XmlAccessType.NONE)
public class XMPPComponentInfo extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=AccountConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-description x-domainName
     */
    @XmlAttribute(name="x-domainName", required=false)
    private String domainName;

    /**
     * @zm-api-field-description x-serverName
     */
    @XmlAttribute(name="x-serverName", required=false)
    private String serverName;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private XMPPComponentInfo() {
        this((String) null, (String) null);
    }

    public XMPPComponentInfo(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public String getDomainName() { return domainName; }
    public String getServerName() { return serverName; }
}
