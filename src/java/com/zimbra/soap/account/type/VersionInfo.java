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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class VersionInfo {

    @XmlAttribute(name=AccountConstants.A_VERSION_INFO_VERSION, required=true)
    private final String fullVersion;

    @XmlAttribute(name=AccountConstants.A_VERSION_INFO_RELEASE, required=true)
    private final String release;

    @XmlAttribute(name=AccountConstants.A_VERSION_INFO_DATE, required=true)
    private final String date;

    @XmlAttribute(name=AccountConstants.A_VERSION_INFO_HOST, required=true)
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
        return Objects.toStringHelper(this)
            .add("fullVersion", fullVersion)
            .add("release", release)
            .add("date", date)
            .add("host", host)
            .toString();
    }
}
