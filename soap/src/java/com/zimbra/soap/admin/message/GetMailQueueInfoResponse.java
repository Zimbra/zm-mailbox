/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
