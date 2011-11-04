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

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class VersionCheckUpdateInfo {

    @XmlAttribute(name=AdminConstants.A_UPDATE_TYPE, required=false)
    private String type;

    @XmlAttribute(name=AdminConstants.A_CRITICAL, required=false)
    private ZmBoolean critical;

    @XmlAttribute(name=AdminConstants.A_UPDATE_URL, required=false)
    private String updateURL;

    @XmlAttribute(name=AdminConstants.A_DESCRIPTION /* description */,
                        required=false)
    private String description;

    @XmlAttribute(name=AdminConstants.A_SHORT_VERSION, required=false)
    private String shortVersion;

    @XmlAttribute(name=AdminConstants.A_RELEASE, required=false)
    private String release;

    @XmlAttribute(name=AdminConstants.A_VERSION, required=false)
    private String version;

    @XmlAttribute(name=AdminConstants.A_BUILDTYPE, required=false)
    private String buildType;

    @XmlAttribute(name=AdminConstants.A_PLATFORM, required=false)
    private String platform;

    public VersionCheckUpdateInfo() {
    }

    public void setType(String type) { this.type = type; }
    public void setCritical(Boolean critical) { this.critical = ZmBoolean.fromBool(critical); }
    public void setUpdateURL(String updateURL) { this.updateURL = updateURL; }
    public void setDescription(String description) { this.description = description; }
    public void setShortVersion(String shortVersion) { this.shortVersion = shortVersion; }
    public void setRelease(String release) { this.release = release; }
    public void setVersion(String version) { this.version = version; }
    public void setBuildType(String buildType) { this.buildType = buildType; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getType() { return type; }
    public Boolean getCritical() { return ZmBoolean.toBool(critical); }
    public String getUpdateURL() { return updateURL; }
    public String getDescription() { return description; }
    public String getShortVersion() { return shortVersion; }
    public String getRelease() { return release; }
    public String getVersion() { return version; }
    public String getBuildType() { return buildType; }
    public String getPlatform() { return platform; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("type", type)
            .add("critical", critical)
            .add("updateURL", updateURL)
            .add("description", description)
            .add("shortVersion", shortVersion)
            .add("release", release)
            .add("version", version)
            .add("buildType", buildType)
            .add("platform", platform);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
