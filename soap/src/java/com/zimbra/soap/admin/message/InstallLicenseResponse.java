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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("validFrom", validFrom)
            .add("validUntil", validUntil)
            .add("serverTime", serverTime);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
