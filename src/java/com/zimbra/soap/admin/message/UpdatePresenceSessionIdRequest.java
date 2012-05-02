/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.VoiceAdminConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;

/**
 * @zm-api-command-description Generate a new Cisco Presence server session ID and persist 
 * the newly generated session id in zimbraUCCiscoPresenceSessionId attribute for the 
 * specified UC service.
 * <br />
 * Notes:
 * <ul>
 * <li>an empty attribute value removes the specified attr
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=VoiceAdminConstants.E_UPDATE_PRESENCE_SESSION_ID_REQUEST)
@XmlType(propOrder = {})
public class UpdatePresenceSessionIdRequest extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag value-of-zimbra-id
     * @zm-api-field-description Zimbra ID
     */
    @XmlAttribute(name=AdminConstants.E_ID, required=true)
    private String id;
    
    /**
     * @zm-api-field-tag app username 
     * @zm-api-field-description app username
     */
    @XmlAttribute(name=AdminConstants.E_USERNAME, required=true)
    private String username;
    
    /**
     * @zm-api-field-tag app password
     * @zm-api-field-description app password
     */
    @XmlAttribute(name=AdminConstants.E_PASSWORD, required=true)
    private String password;

    public UpdatePresenceSessionIdRequest() {
    }
    
    public UpdatePresenceSessionIdRequest(String id, String username, String password) {
        setId(id);
        setUsername(username);
        setPassword(password);
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }

    public String getId() {
        return id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
}