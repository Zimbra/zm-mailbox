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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class XMPPComponentSpec extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=AccountConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-description Domain selector
     */
    @XmlElement(name=AdminConstants.E_DOMAIN, required=true)
    private final DomainSelector domain;

    /**
     * @zm-api-field-description Server selector
     */
    @XmlElement(name=AdminConstants.E_SERVER, required=true)
    private final ServerSelector server;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private XMPPComponentSpec() {
        this((String) null, (DomainSelector) null, (ServerSelector) null);
    }

    public XMPPComponentSpec(String name, DomainSelector domain,
                            ServerSelector server) {
        this.name = name;
        this.domain = domain;
        this.server = server;
    }

    public String getName() { return name; }
    public DomainSelector getDomain() { return domain; }
    public ServerSelector getServer() { return server; }
}
