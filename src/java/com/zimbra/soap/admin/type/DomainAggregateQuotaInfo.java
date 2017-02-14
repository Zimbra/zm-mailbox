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

import com.zimbra.common.soap.AdminConstants;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public class DomainAggregateQuotaInfo {

    /**
     * @zm-api-field-tag domain-name
     * @zm-api-field-description Domain name
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;
    /**
     * @zm-api-field-tag domain-id
     * @zm-api-field-description Domain name
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final String id;

    /**
     * @zm-api-field-tag quota-used-on-server
     * @zm-api-field-description Quota used on server
     */
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
