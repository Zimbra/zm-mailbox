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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ServerQueues {

    /**
     * @zm-api-field-tag mta-server
     * @zm-api-field-description MTA server
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private final String serverName;

    /**
     * @zm-api-field-description Queue information
     */
    @XmlElement(name=AdminConstants.E_QUEUE /* queue */, required=false)
    private List<MailQueueCount> queues = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ServerQueues() {
        this((String) null);
    }

    public ServerQueues(String serverName) {
        this.serverName = serverName;
    }

    public void setQueues(Iterable <MailQueueCount> queues) {
        this.queues.clear();
        if (queues != null) {
            Iterables.addAll(this.queues,queues);
        }
    }

    public ServerQueues addQueue(MailQueueCount queue) {
        this.queues.add(queue);
        return this;
    }

    public String getServerName() { return serverName; }
    public List<MailQueueCount> getQueues() {
        return Collections.unmodifiableList(queues);
    }
}
