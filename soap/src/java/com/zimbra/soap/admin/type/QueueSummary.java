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
