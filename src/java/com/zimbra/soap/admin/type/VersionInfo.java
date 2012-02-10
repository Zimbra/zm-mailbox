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

@XmlAccessorType(XmlAccessType.NONE)
public class VersionInfo {

    /**
     * @zm-api-field-tag type
     * @zm-api-field-description Type
     */
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_TYPE /* type */, required=false)
    private String type;

    /**
     * @zm-api-field-tag version-string
     * @zm-api-field-description Version string
     */
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_VERSION /* version */, required=true)
    private String version;

    /**
     * @zm-api-field-tag release-string
     * @zm-api-field-description Release string
     */
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_RELEASE /* release */, required=true)
    private String release;

    /**
     * @zm-api-field-tag buildDate-YYYYMMDD-hhmm
     * @zm-api-field-description Build Date - format : <b>YYYYMMDD-hhmm</b>
     */
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_DATE /* buildDate */, required=true)
    private String buildDate;

    /**
     * @zm-api-field-tag host-name
     * @zm-api-field-description Host name
     */
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_HOST /* host */, required=true)
    private String host;

    /**
     * @zm-api-field-description Major version
     */
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_MAJOR /* majorversion */, required=true)
    private String majorVersion;

    /**
     * @zm-api-field-description Minor version
     */
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_MINOR /* minorversion */, required=true)
    private String minorVersion;

    /**
     * @zm-api-field-description Micro version
     */
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_MICRO /* microversion */, required=true)
    private String microVersion;

    /**
     * @zm-api-field-description Platform
     */
    @XmlAttribute(name=AdminConstants.A_VERSION_INFO_PLATFORM /* platform */, required=true)
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
