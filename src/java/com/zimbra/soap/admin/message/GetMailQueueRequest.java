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
import com.zimbra.soap.admin.type.ServerMailQueueQuery;

/**
 * @zm-api-command-description Summarize and/or search a particular mail queue on a particular server.
 * <br />
 * The admin SOAP server initiates a MTA queue scan (via ssh) and then caches the result of the queue scan.
 * To force a queue scan, specify scan=1 in the request.
 * <br />
 * The response has two parts.
 * <ul>
 * <li> <b>&lt;qs></b> elements summarize queue by various types of data (sender addresses, recipient domain, etc).
 *     Only the deferred queue has error summary type.
 * <li> <b>&lt;qi></b> elements list the various queue items that match the requested query.
 * </ul>
 * The stale-flag in the response means that since the scan, some queue action was done and the data being
 * presented is now stale.  This allows us to let the user dictate when to do a queue scan.
 * <br />
 * <br />
 * The scan-flag in the response indicates that the server has not completed scanning the MTA queue, and that this
 * scan is in progress, and the client should ask again in a little while.
 * <br />
 * <br />
 * The more-flag in the response indicates that more qi's are available past the limit specified in the request.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_GET_MAIL_QUEUE_REQUEST)
public class GetMailQueueRequest {

    /**
     * @zm-api-field-description Server Mail Queue Query
     */
    @XmlElement(name=AdminConstants.E_SERVER, required=true)
    private final ServerMailQueueQuery server;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetMailQueueRequest() {
        this((ServerMailQueueQuery) null);
    }

    public GetMailQueueRequest(ServerMailQueueQuery server) {
        this.server = server;
    }

    public ServerMailQueueQuery getServer() { return server; }
}
