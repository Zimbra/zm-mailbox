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

@XmlAccessorType(XmlAccessType.FIELD)
public class QueueItem {

    // See RemoteMailQueue.QueueAttr
    @XmlAttribute(name="id", required=true)
    private final String id;

    // See RemoteMailQueue.QueueAttr
    @XmlAttribute(name="time", required=true)
    private final String time;

    // See RemoteMailQueue.QueueAttr
    @XmlAttribute(name="fromdomain", required=true)
    private final String fromdomain;

    // See RemoteMailQueue.QueueAttr
    @XmlAttribute(name="size", required=true)
    private final String size;

    // See RemoteMailQueue.QueueAttr
    @XmlAttribute(name="from", required=true)
    private final String from;

    // See RemoteMailQueue.QueueAttr
    @XmlAttribute(name="to", required=true)
    private final String to;

    // See RemoteMailQueue.QueueAttr
    @XmlAttribute(name="host", required=true)
    private final String host;

    // See RemoteMailQueue.QueueAttr
    @XmlAttribute(name="addr", required=true)
    private final String addr;

    // See RemoteMailQueue.QueueAttr
    @XmlAttribute(name="reason", required=true)
    private final String reason;

    // See RemoteMailQueue.QueueAttr
    @XmlAttribute(name="filter", required=true)
    private final String filter;

    // See RemoteMailQueue.QueueAttr
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
