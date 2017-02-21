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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ServerMailQueueQuery {

    /**
     * @zm-api-field-tag mta-server
     * @zm-api-field-description MTA Server
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private final String serverName;

    /**
     * @zm-api-field-description Mail queue query details
     */
    @XmlElement(name=AdminConstants.E_QUEUE /* queue */, required=true)
    private final MailQueueQuery queue;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ServerMailQueueQuery() {
        this((String) null, (MailQueueQuery) null);
    }

    public ServerMailQueueQuery(String serverName, MailQueueQuery queue) {
        this.serverName = serverName;
        this.queue = queue;
    }

    public String getServerName() { return serverName; }
    public MailQueueQuery getQueue() { return queue; }
}
