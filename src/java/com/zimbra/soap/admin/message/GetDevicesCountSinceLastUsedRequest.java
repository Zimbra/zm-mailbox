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

package com.zimbra.soap.admin.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.SyncAdminConstants;
import com.zimbra.soap.admin.type.DateString;

/**
 * @zm-api-command-network-edition
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("lastUsedDate", lastUsedDate);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
