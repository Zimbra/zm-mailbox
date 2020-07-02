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

package com.zimbra.soap.account.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.account.type.VersionInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_GET_VERSION_INFO_RESPONSE)
public class GetVersionInfoResponse {

    /**
     * @zm-api-field-description Version information
     */
    @XmlElement(name=AccountConstants.E_VERSION_INFO_INFO, required=true)
    private final VersionInfo versionInfo;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetVersionInfoResponse() {
        this((VersionInfo) null);
    }

    public GetVersionInfoResponse(VersionInfo versionInfo) {
        this.versionInfo = versionInfo;
    }

    public VersionInfo getVersionInfo() { return versionInfo; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("versionInfo", versionInfo)
            .toString();
    }
}
