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

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class ZimletDeploymentStatus {

    @XmlAttribute(name=AdminConstants.A_SERVER, required=true)
    private final String server;

    @XmlAttribute(name=AdminConstants.A_STATUS, required=true)
    private final String status;

    @XmlAttribute(name=AdminConstants.A_ERROR, required=false)
    private final String error;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ZimletDeploymentStatus() {
        this((String) null, (String) null, (String) null);
    }

    public ZimletDeploymentStatus(String server, String status, String error) {
        this.server = server;
        this.status = status;
        this.error = error;
    }

    public String getServer() { return server; }
    public String getStatus() { return status; }
    public String getError() { return error; }
}
