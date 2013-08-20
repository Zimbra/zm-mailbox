/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.type.AttributeSelectorImpl;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get Server
 */
@XmlRootElement(name=AdminConstants.E_SET_SERVER_OFFLINE_REQUEST)
public class SetServerOfflineRequest extends AttributeSelectorImpl {

    /**
     * @zm-api-field-description Server
     */
    @XmlElement(name=AdminConstants.E_SERVER)
    private ServerSelector server;

    public SetServerOfflineRequest() {
        this(null);
    }

    public SetServerOfflineRequest(ServerSelector server) {
        setServer(server);
    }

    public void setServer(ServerSelector server) {
        this.server = server;
    }

    public ServerSelector getServer() { return server; }
}
