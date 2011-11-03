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

import com.zimbra.common.soap.AdminConstants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.FIELD)
public class DomainAggregateQuotaInfo {

    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final String id;
    @XmlAttribute(name=AdminConstants.A_QUOTA_USED, required=true)
    private final long quotaUsed;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DomainAggregateQuotaInfo() {
        this(null, null, -1L);
    }

    public DomainAggregateQuotaInfo(String name, String id, long quotaUsed) {
        this.name = name;
        this.id = id;
        this.quotaUsed = quotaUsed;
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public long getQuotaUsed() { return quotaUsed; }
}
