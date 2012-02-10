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

// soap-admin.txt implies there is a "stale" attribute (See GetMailQueueResponse/server/queue) but SOAP
// handler does not add this

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"queueSummaries", "queueItems"})
public class MailQueueDetails {

    /**
     * @zm-api-field-tag queue-name
     * @zm-api-field-description Queue name
     */
    @XmlAttribute(name=AdminConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag scan-time
     * @zm-api-field-description Scan time
     */
    @XmlAttribute(name=AdminConstants.A_TIME /* time */, required=true)
    private final long time;

    /**
     * @zm-api-field-tag scan-flag
     * @zm-api-field-description Indicates that the server has not completed scanning the MTA queue, and that this
     * scan is in progress, and the client should ask again in a little while.
     */
    @XmlAttribute(name=AdminConstants.A_SCAN /* scan */, required=true)
    private final ZmBoolean stillScanning;

    /**
     * @zm-api-field-tag mail-queue-detail-total
     * @zm-api-field-description
     */
    @XmlAttribute(name=AdminConstants.A_TOTAL /* total */, required=true)
    private final int total;

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description Indicates that more qi's are available past the limit specified in the request.
     */
    @XmlAttribute(name=AdminConstants.A_MORE /* more */, required=true)
    private final ZmBoolean more;

    /**
     * @zm-api-field-description Queue summary.  The <b>&lt;qs></b> elements summarize the queue by various types of
     * data (sender addresses, recipient domain, etc).  Only the deferred queue has error summary type.
     */
    @XmlElement(name=AdminConstants.A_QUEUE_SUMMARY /* qs */, required=false)
    private List<QueueSummary> queueSummaries = Lists.newArrayList();

    /**
     * @zm-api-field-description The various queue items that match the requested query.
     */
    @XmlElement(name=AdminConstants.A_QUEUE_ITEM /* qi */, required=false)
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
