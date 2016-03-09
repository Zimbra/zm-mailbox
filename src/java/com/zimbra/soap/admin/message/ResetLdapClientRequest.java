/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;
import javax.xml.bind.annotation.XmlAttribute;

@XmlRootElement(name = AdminConstants.E_RESET_LDAP_CLIENT_REQUEST)
public class ResetLdapClientRequest {
    
    /**
     * @zm-api-field-tag all-servers
     * @zm-api-field-description reset ldap client only on the local server or all servers
     */
	@XmlAttribute(name = AdminConstants.A_ALLSERVERS /* allServers */, required = false)
	private ZmBoolean allServers;
	public ResetLdapClientRequest(){
		allServers  = ZmBoolean.FALSE;
	}
	public ResetLdapClientRequest(boolean allServers){
		this.allServers = ZmBoolean.fromBool(allServers);
	}

	public boolean isAllServers() {
		return ZmBoolean.toBool(allServers, false);
	}
}
