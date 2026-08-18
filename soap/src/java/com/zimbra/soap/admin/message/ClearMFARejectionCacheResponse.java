/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.soap.admin.message;

import com.zimbra.common.soap.AdminConstants;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * @zm-api-command-description Response for ClearMFARejectionCacheRequest.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = AdminConstants.E_CLEAR_MFA_REJECTION_CACHE_RESPONSE)
@XmlType(propOrder = {})
public class ClearMFARejectionCacheResponse {

    /**
     * @zm-api-field-description Status of the cache clear operation
     */
    @XmlAttribute(name = AdminConstants.A_STATUS, required = true)
    private String status;

    /**
     * @zm-api-field-description Number of cache entries cleared
     */
    @XmlAttribute(name = AdminConstants.A_ENTRIES_CLEARED, required = true)
    private int entriesCleared;

    /**
     * no-argument constructor wanted by JAXB.
     */
    public ClearMFARejectionCacheResponse() {
        this(null, 0);
    }

    public ClearMFARejectionCacheResponse(String status, int entriesCleared) {
        this.status = status;
        this.entriesCleared = entriesCleared;
    }

    public String getStatus() {
        return status;
    }

    public int getEntriesCleared() {
        return entriesCleared;
    }
}
