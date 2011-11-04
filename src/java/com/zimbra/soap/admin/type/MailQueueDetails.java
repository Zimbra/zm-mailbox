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
import javax.xml.bind.annotation.XmlType;
import com.zimbra.soap.type.ZmBoolean;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"queueSummaries", "queueItems"})
public class MailQueueDetails {

    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    @XmlAttribute(name=AdminConstants.A_TIME, required=true)
    private final long time;

    @XmlAttribute(name=AdminConstants.A_SCAN, required=true)
    private final ZmBoolean stillScanning;

    @XmlAttribute(name=AdminConstants.A_TOTAL, required=true)
    private final int total;

    @XmlAttribute(name=AdminConstants.A_MORE, required=true)
    private final ZmBoolean more;

    @XmlElement(name=AdminConstants.A_QUEUE_SUMMARY, required=false)
    private List<QueueSummary> queueSummaries = Lists.newArrayList();

    @XmlElement(name=AdminConstants.A_QUEUE_ITEM, required=false)
    private List<QueueItem> queueItems = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MailQueueDetails() {
        this((String) null, -1L, false, -1, false);
    }

    public MailQueueDetails(String name, long time, boolean stillScanning,
                    int total, boolean more) {
        this.name = name;
        this.time = time;
        this.stillScanning = ZmBoolean.fromBool(stillScanning);
        this.total = total;
        this.more = ZmBoolean.fromBool(more);
    }

    public void setQueueSummaries(Iterable <QueueSummary> queueSummaries) {
        this.queueSummaries.clear();
        if (queueSummaries != null) {
            Iterables.addAll(this.queueSummaries,queueSummaries);
        }
    }

    public MailQueueDetails addQueueSummary(QueueSummary queueSummary) {
        this.queueSummaries.add(queueSummary);
        return this;
    }

    public void setQueueItems(Iterable <QueueItem> queueItems) {
        this.queueItems.clear();
        if (queueItems != null) {
            Iterables.addAll(this.queueItems,queueItems);
        }
    }

    public MailQueueDetails addQueueItem(QueueItem queueItem) {
        this.queueItems.add(queueItem);
        return this;
    }


    public String getName() { return name; }
    public long getTime() { return time; }
    public boolean getStillScanning() { return ZmBoolean.toBool(stillScanning); }
    public int getTotal() { return total; }
    public boolean getMore() { return ZmBoolean.toBool(more); }
    public List<QueueSummary> getQueueSummaries() {
        return Collections.unmodifiableList(queueSummaries);
    }
    public List<QueueItem> getQueueItems() {
        return Collections.unmodifiableList(queueItems);
    }
}
