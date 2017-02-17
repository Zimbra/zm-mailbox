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
import com.zimbra.soap.admin.type.ServerMailQueueQuery;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
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
