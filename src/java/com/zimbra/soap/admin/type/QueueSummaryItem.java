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
public class QueueSummaryItem {

    /**
     * @zm-api-field-tag q-summ-count
     * @zm-api-field-description Count
     */
    @XmlAttribute(name=AdminConstants.A_N, required=true)
    private final int count;

    /**
     * @zm-api-field-tag text-for-item
     * @zm-api-field-description Text for item.  e.g. "connect to 10.10.20.40 failed"
     */
    @XmlAttribute(name=AdminConstants.A_T, required=true)
    private final String term;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private QueueSummaryItem() {
        this(-1, (String) null);
    }

    public QueueSummaryItem(int count, String term) {
        this.count = count;
        this.term = term;
    }

    public int getCount() { return count; }
    public String getTerm() { return term; }
}
