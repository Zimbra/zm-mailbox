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

import com.zimbra.common.soap.AdminConstants;

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

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private QueueItem() {
        this((String) null, (String) null, (String) null, (String) null, (String) null, (String) null,
                (String) null, (String) null, (String) null, (String) null, (String) null);
    }

    public QueueItem(String id, String time, String fromdomain, String size, String from, String to,
                    String host, String addr, String reason, String filter, String todomain) {
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
}
