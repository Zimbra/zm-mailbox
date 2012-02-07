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
import com.zimbra.soap.admin.type.ServerQueues;

/**
 * @zm-api-command-description Example:
 * <pre>
 *     &lt;GetMailQueueInfoResponse/>
 *       &lt;server name="{mta-server}">
 *         &lt;queue name="deferred" n="{N}"/>
 *         &lt;queue name="incoming" n="{N}"/>
 *         &lt;queue name="active" n="{N}"/>
 *         &lt;queue name="hold" n="{N}"/>
 *         &lt;queue name="corrupt" n="{N}"/>
 *       &lt;/server>
 *     &lt;/GetMailQueueInfoResponse>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_MAIL_QUEUE_INFO_RESPONSE)
public class GetMailQueueInfoResponse {

    /**
     * @zm-api-field-description Information on queues organised by server
     */
    @XmlElement(name=AdminConstants.E_SERVER, required=true)
    private final ServerQueues server;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetMailQueueInfoResponse() {
        this((ServerQueues) null);
    }

    public GetMailQueueInfoResponse(ServerQueues server) {
        this.server = server;
    }

    public ServerQueues getServer() { return server; }
}
