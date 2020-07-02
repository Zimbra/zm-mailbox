/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.SyncAdminConstants;

@XmlRootElement(name=SyncAdminConstants.E_REMOVE_STALE_DEVICE_METADATA_REQUEST)
public class RemoveStaleDeviceMetadataRequest {

    @XmlAttribute(name=SyncAdminConstants.A_LAST_USED_DATE_OLDER_THAN, required=false)
    private int lastUsedDateOlderThan;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RemoveStaleDeviceMetadataRequest() {
    }

    private RemoveStaleDeviceMetadataRequest(int days) {
        this.lastUsedDateOlderThan = days;
    }

    public int getLastUsedDateOlderThan() {
        return lastUsedDateOlderThan;
    }

    public void setLastUsedDateOlderThan(int lastUsedDateOlderThan) {
        this.lastUsedDateOlderThan = lastUsedDateOlderThan;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("lastUsedDateOlderThan", this.lastUsedDateOlderThan).toString();
    }
}
