/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ServerSelector;
import com.zimbra.soap.type.AttributeSelectorImpl;
import com.zimbra.soap.type.ZmBoolean;

@XmlRootElement(name=AdminConstants.E_GET_SERVER_REQUEST)
public class GetServerRequest extends AttributeSelectorImpl {

    @XmlAttribute(name=AdminConstants.A_APPLY_CONFIG, required=false)
    private ZmBoolean applyConfig;
    @XmlElement(name=AdminConstants.E_SERVER)
    private ServerSelector server;

    public GetServerRequest() {
        this(null, null);
    }

    public GetServerRequest(ServerSelector server, Boolean applyConfig) {
        setServer(server);
        setApplyConfig(applyConfig);
    }

    public void setServer(ServerSelector server) {
        this.server = server;
    }

    public void setApplyConfig(Boolean applyConfig) {
        this.applyConfig = ZmBoolean.fromBool(applyConfig);
    }

    public ServerSelector getServer() { return server; }
    public Boolean isApplyConfig() { return ZmBoolean.toBool(applyConfig); }
}
