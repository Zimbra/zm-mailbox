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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_INSTALL_LICENSE_RESPONSE)
public class InstallLicenseResponse {

    /**
     * @zm-api-field-tag valid-from-date-in-ms
     * @zm-api-field-description Valid form date in milliseconds
     */
    @XmlAttribute(name=AdminConstants.A_VALID_FROM /* validFrom */, required=true)
    private final long validFrom;

    /**
     * @zm-api-field-tag valid-until-date-in-ms
     * @zm-api-field-description Valid until date in milliseconds
     */
    @XmlAttribute(name=AdminConstants.A_VALID_UNTIL /* validUntil */, required=true)
    private final long validUntil;

    /**
     * @zm-api-field-tag server-time-in-ms
     * @zm-api-field-description Time on server in milliseconds
     */
    @XmlAttribute(name=AdminConstants.A_SERVER_TIME /* serverTime */, required=true)
    private final long serverTime;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private InstallLicenseResponse() {
        this(-1L, -1L, -1L);
    }

    public InstallLicenseResponse(
                        long validFrom, long validUntil, long serverTime) {
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.serverTime = serverTime;
    }

    public long getValidFrom() { return validFrom; }
    public long getValidUntil() { return validUntil; }
    public long getServerTime() { return serverTime; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("validFrom", validFrom)
            .add("validUntil", validUntil)
            .add("serverTime", serverTime);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
