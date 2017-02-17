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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ServerSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get Network Interface information for a server
 * <br />
 * Get server's network interfaces. Returns IP  addresses and net masks
 * <br />
 * This call will use zmrcd to call /opt/zimbra/libexec/zmserverips
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_SERVER_NIFS_REQUEST)
public class GetServerNIfsRequest {

	/**
	 * @zm-api-field-description specifics the ipAddress type (ipV4/ipV6/both). default is ipv4
	 */
	@XmlAttribute(name=AdminConstants.A_TYPE, required=false)
	private final String type;

    /**
     * @zm-api-field-description Server
     */

    @XmlElement(name=AdminConstants.E_SERVER, required=true)
    private final ServerSelector server;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetServerNIfsRequest() {
        this((String) null, (ServerSelector) null);
    }

    public GetServerNIfsRequest(String type, ServerSelector server) {
        this.type = type;
        this.server = server;
    }

    public String getType() { return type; }
    public ServerSelector getServer() { return server; }
}
