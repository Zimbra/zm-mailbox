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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class AccountQuotaInfo {

    /**
     * @zm-api-field-tag account-name
     * @zm-api-field-description Account name
     */
    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account ID
     */
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final String id;

    /**
     * @zm-api-field-tag quota-used-bytes
     * @zm-api-field-description Used quota in bytes, or 0 if no quota used
     */
    @XmlAttribute(name=AdminConstants.A_QUOTA_USED /* used */, required=true)
    private final long quotaUsed;

    /**
     * @zm-api-field-tag quota-limit-bytes
     * @zm-api-field-description Quota limit in bytes, or 0 if unlimited
     */
    @XmlAttribute(name=AdminConstants.A_QUOTA_LIMIT /* limit */, required=true)
    private final long quotaLimit;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AccountQuotaInfo() {
        this((String) null, (String) null, -1L, -1L);
    }

    public AccountQuotaInfo(String name, String id, long quotaUsed, long quotaLimit) {
        this.name = name;
        this.id = id;
        this.quotaUsed = quotaUsed;
        this.quotaLimit = quotaLimit;
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public long getQuotaUsed() { return quotaUsed; }
    public long getQuotaLimit() { return quotaLimit; }
}
