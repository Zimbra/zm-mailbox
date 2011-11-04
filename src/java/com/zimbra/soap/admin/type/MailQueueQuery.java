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
import javax.xml.bind.annotation.XmlElement;
import com.zimbra.soap.type.ZmBoolean;

import com.zimbra.common.soap.AdminConstants;
@XmlAccessorType(XmlAccessType.FIELD)
public class MailQueueQuery {

    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String queueName;

    @XmlAttribute(name=AdminConstants.A_SCAN, required=false)
    private final ZmBoolean scan;

    @XmlAttribute(name=AdminConstants.A_WAIT, required=false)
    private final Long waitSeconds;

    @XmlElement(name=AdminConstants.E_QUERY, required=true)
    private final QueueQuery query;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MailQueueQuery() {
        this((String) null, (Boolean) null, (Long) null, (QueueQuery) null);
    }

    public MailQueueQuery(String queueName, Boolean scan, Long waitSeconds,
                        QueueQuery query) {
        this.queueName = queueName;
        this.scan = ZmBoolean.fromBool(scan);
        this.waitSeconds = waitSeconds;
        this.query = query;
    }

    public String getQueueName() { return queueName; }
    public Boolean getScan() { return ZmBoolean.toBool(scan); }
    public Long getWaitSeconds() { return waitSeconds; }
    public QueueQuery getQuery() { return query; }
}
