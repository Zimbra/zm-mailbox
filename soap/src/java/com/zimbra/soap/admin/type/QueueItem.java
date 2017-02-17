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

@XmlAccessorType(XmlAccessType.NONE)
public class QueueItem {

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name="id", required=true)
    private final String id;

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag arrival-time
     * @zm-api-field-description Arrival time
     */
    @XmlAttribute(name="time", required=true)
    private final String time;

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag fromdomain
     * @zm-api-field-description From domain
     */
    @XmlAttribute(name="fromdomain", required=true)
    private final String fromdomain;

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag size
     * @zm-api-field-description Size
     */
    @XmlAttribute(name="size", required=true)
    private final String size;

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag sender
     * @zm-api-field-description Sender
     */
    @XmlAttribute(name="from", required=true)
    private final String from;

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag recipients-comma-separated
     * @zm-api-field-description Comma separated list of recipients
     */
    @XmlAttribute(name="to", required=true)
    private final String to;

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag origin-host-name
     * @zm-api-field-description Hostname of origin
     */
    @XmlAttribute(name="host", required=true)
    private final String host;

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag origin-ip-address
     * @zm-api-field-description IP address of origin
     */
    @XmlAttribute(name="addr", required=true)
    private final String addr;

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag reason
     * @zm-api-field-description Reason
     */
    @XmlAttribute(name="reason", required=true)
    private final String reason;

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag content-filter
     * @zm-api-field-description Content filter
     */
    @XmlAttribute(name="filter", required=true)
    private final String filter;

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag todomain
     * @zm-api-field-description To domain
     */
    @XmlAttribute(name="todomain", required=true)
    private final String todomain;

    // See RemoteMailQueue.QueueAttr
    /**
     * @zm-api-field-tag received
     * @zm-api-field-description IP address message received from
     */
    @XmlAttribute(name="received", required=true)
    private final String received;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private QueueItem() {
        this((String) null, (String) null, (String) null, (String) null, (String) null, (String) null,
                (String) null, (String) null, (String) null, (String) null, (String) null, (String) null);
    }

    public QueueItem(String id, String time, String fromdomain, String size, String from, String to,
                    String host, String addr, String reason, String filter, String todomain, String received) {
        this.id = id;
        this.time = time;
        this.fromdomain = fromdomain;
        this.size = size;
        this.from = from;
        this.to = to;
        this.host = host;
        this.addr = addr;
        this.reason = reason;
        this.filter = filter;
        this.todomain = todomain;
        this.received = received;
    }

    public String getId() { return id; }
    public String getTime() { return time; }
    public String getFromdomain() { return fromdomain; }
    public String getSize() { return size; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public String getHost() { return host; }
    public String getAddr() { return addr; }
    public String getReason() { return reason; }
    public String getFilter() { return filter; }
    public String getTodomain() { return todomain; }
    public String getReceived() { return received; }
}
