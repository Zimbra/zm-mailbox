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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ServerMailQueueDetails {

    /**
     * @zm-api-field-tag mta-server
     * @zm-api-field-description MTA Server
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private final String serverName;

    /**
     * @zm-api-field-description Mail queue details
     */
    @XmlElement(name=AdminConstants.E_QUEUE /* queue */, required=true)
    private final MailQueueDetails queue;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ServerMailQueueDetails() {
        this((String) null, (MailQueueDetails) null);
    }

    public ServerMailQueueDetails(String serverName, MailQueueDetails queue) {
        this.serverName = serverName;
        this.queue = queue;
    }

    public String getServerName() { return serverName; }
    public MailQueueDetails getQueue() { return queue; }
}
