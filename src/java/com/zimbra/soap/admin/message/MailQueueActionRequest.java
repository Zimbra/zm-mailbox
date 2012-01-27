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
import com.zimbra.soap.admin.type.ServerWithQueueAction;

/**
 * @zm-api-command-description Command to act on invidual queue files.  This proxies through to postsuper.
 * <br />
 * list-of-ids can be ALL.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_MAIL_QUEUE_ACTION_REQUEST)
public class MailQueueActionRequest {

    /**
     * @zm-api-field-description Server with queue action
     */
    @XmlElement(name=AdminConstants.E_SERVER, required=true)
    private final ServerWithQueueAction server;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MailQueueActionRequest() {
        this((ServerWithQueueAction) null);
    }

    public MailQueueActionRequest(ServerWithQueueAction server) {
        this.server = server;
    }

    public ServerWithQueueAction getServer() { return server; }
}
