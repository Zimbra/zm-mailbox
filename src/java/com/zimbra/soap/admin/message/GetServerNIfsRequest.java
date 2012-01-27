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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.ServerSelector;

/**
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
     * @zm-api-field-description Server
     */
    @XmlElement(name=AdminConstants.E_SERVER, required=true)
    private final ServerSelector server;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetServerNIfsRequest() {
        this((ServerSelector) null);
    }

    public GetServerNIfsRequest(ServerSelector server) {
        this.server = server;
    }

    public ServerSelector getServer() { return server; }
}
