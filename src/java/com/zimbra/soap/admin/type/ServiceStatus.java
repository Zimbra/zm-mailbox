/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;

/**
 * Used by {@CountAccountResponse}
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class ServiceStatus {

    @XmlAttribute(name=AdminConstants.A_SERVER, required=true)
    private final String server;
    @XmlAttribute(name=AdminConstants.A_SERVICE, required=true)
    private final String service;
    @XmlAttribute(name=AdminConstants.A_T, required=true)
    private final String time;
    @XmlValue
    private final String status;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ServiceStatus() {
        this(null, null, null, null);
    }

    public ServiceStatus(String server, String service, String time, String status) {
        this.server = server;
        this.service = service;
        this.time = time;
        this.status = status;
    }

    public String getServer() { return server; }
    public String getService() { return service; }
    public String getTime() { return time; }
    public String getStatus() { return status; }
}
