/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.VoiceAdminConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;
import com.zimbra.soap.admin.type.UCServiceSelector;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Generate a new Cisco Presence server session ID and persist 
 * the newly generated session id in zimbraUCCiscoPresenceSessionId attribute for the 
 * specified UC service.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=VoiceAdminConstants.E_UPDATE_PRESENCE_SESSION_ID_REQUEST)
public class UpdatePresenceSessionIdRequest extends AdminAttrsImpl {

    /**
     * @zm-api-field-description the UC service
     */
    @XmlElement(name=AdminConstants.E_UC_SERVICE, required=true)
    private UCServiceSelector ucService;
    
    /**
     * @zm-api-field-description app username
     */
    @XmlElement(name=AdminConstants.E_USERNAME, required=true)
    private String username;
    
    /**
     * @zm-api-field-description app password
     */
    @XmlElement(name=AdminConstants.E_PASSWORD, required=true)
    private String password;

    public UpdatePresenceSessionIdRequest() {
    }
    
    public UpdatePresenceSessionIdRequest(UCServiceSelector ucService, String username, String password) {
        setUCService(ucService);
        setUsername(username);
        setPassword(password);
    }

    public void setUCService(UCServiceSelector ucService) {
        this.ucService = ucService;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    public UCServiceSelector getId() {
        return ucService;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
}
