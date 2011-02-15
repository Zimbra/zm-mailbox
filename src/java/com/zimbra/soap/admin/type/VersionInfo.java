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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class VersionInfo {

    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_TYPE, required=false)
    private String type;
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_VERSION, required=true)
    private String version;
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_RELEASE, required=true)
    private String release;
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_DATE, required=true)
    private String buildDate;
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_HOST, required=true)
    private String host;
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_MAJOR, required=true)
    private String majorVersion;
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_MINOR, required=true)
    private String minorVersion;
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_MICRO, required=true)
    private String microVersion;
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_PLATFORM, required=true)
    private String platform;

    public VersionInfo() {
    }

    public void setType(String type) { this.type = type; }
    public void setVersion(String version) { this.version = version; }
    public void setRelease(String release) { this.release = release; }
    public void setBuildDate(String buildDate) { this.buildDate = buildDate; }
    public void setHost(String host) { this.host = host; }
    public void setMajorVersion(String majorVersion) { this.majorVersion = majorVersion; }
    public void setMinorVersion(String minorVersion) { this.minorVersion = minorVersion; }
    public void setMicroVersion(String microVersion) { this.microVersion = microVersion; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getType() { return type; }
    public String getVersion() { return version; }
    public String getRelease() { return release; }
    public String getBuildDate() { return buildDate; }
    public String getHost() { return host; }
    public String getMajorVersion() { return majorVersion; }
    public String getMinorVersion() { return minorVersion; }
    public String getMicroVersion() { return microVersion; }
    public String getPlatform() { return platform; }
}
