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

package com.zimbra.soap.admin.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.soap.admin.type.DateString;

/**
 * @zm-api-command-network-edition
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required true
 * @zm-api-command-description Get the mobile devices count on the server since last used date
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=SyncAdminConstants.E_GET_DEVICES_COUNT_SINCE_LAST_USED_REQUEST)
public class GetDevicesCountSinceLastUsedRequest {

    /**
     * @zm-api-field-description Last used date
     */
    @XmlElement(name=SyncAdminConstants.E_LAST_USED_DATE /* lastUsedDate */, required=true)
    private final DateString lastUsedDate;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetDevicesCountSinceLastUsedRequest() {
        this((DateString) null);
    }

    public GetDevicesCountSinceLastUsedRequest(DateString lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public static GetDevicesCountSinceLastUsedRequest fromLastUsedDate(
                    String lastUsedDate) {
        DateString lud = new DateString(lastUsedDate);
        return new GetDevicesCountSinceLastUsedRequest(lud);
    }

    public DateString getLastUsedDate() { return lastUsedDate; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("lastUsedDate", lastUsedDate);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
