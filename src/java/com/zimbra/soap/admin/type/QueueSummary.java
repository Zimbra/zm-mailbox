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
public class QueueSummary {

    /**
     * @zm-api-field-tag reason|to|from|todomain|fromdomain|addr|host
     * @zm-api-field-description Queue summary type - <b>reason|to|from|todomain|fromdomain|addr|host</b>
     */
    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private final String type;

    /**
     * @zm-api-field-description Queue summary items
     */
    @XmlElement(name=AdminConstants.A_QUEUE_SUMMARY_ITEM /* qsi */, required=true)
    private List<QueueSummaryItem> items = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private QueueSummary() {
        this((String) null);
    }

    public QueueSummary(String type) {
        this.type = type;
    }

    public void setItems(Iterable <QueueSummaryItem> items) {
        this.items.clear();
        if (items != null) {
            Iterables.addAll(this.items,items);
        }
    }

    public QueueSummary addItem(QueueSummaryItem item) {
        this.items.add(item);
        return this;
    }

    public String getType() { return type; }
    public List<QueueSummaryItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
