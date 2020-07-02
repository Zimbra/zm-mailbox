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

package com.zimbra.soap.account.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VersionInfo {

    /**
     * @zm-api-field-tag version-string
     * @zm-api-field-description Full version string
     */
    @XmlAttribute(name=AccountConstants.A_VERSION_INFO_VERSION /* version */, required=true)
    private final String fullVersion;

    /**
     * @zm-api-field-tag release-string
     * @zm-api-field-description Release string
     */
    @XmlAttribute(name=AccountConstants.A_VERSION_INFO_RELEASE /* release */, required=true)
    private final String release;

    /**
     * @zm-api-field-tag build-date-YYYYMMDD-hhmm
     * @zm-api-field-description Build date in format: YYYYMMDD-hhmm
     */
    @XmlAttribute(name=AccountConstants.A_VERSION_INFO_DATE /* buildDate */, required=true)
    private final String date;

    /**
     * @zm-api-field-tag build-host-name
     * @zm-api-field-description Build host name
     */
    @XmlAttribute(name=AccountConstants.A_VERSION_INFO_HOST /* host */, required=true)
    private final String host;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private VersionInfo() {
        this((String) null, (String) null, (String) null, (String) null);
    }

    public VersionInfo(String fullVersion, String release,
                            String date, String host) {
        this.fullVersion = fullVersion;
        this.release = release;
        this.date = date;
        this.host = host;
    }

    public String getFullVersion() { return fullVersion; }
    public String getRelease() { return release; }
    public String getDate() { return date; }
    public String getHost() { return host; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("fullVersion", fullVersion)
            .add("release", release)
            .add("date", date)
            .add("host", host)
            .toString();
    }
}
