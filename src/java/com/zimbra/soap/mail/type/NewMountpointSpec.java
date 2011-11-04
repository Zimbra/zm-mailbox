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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class NewMountpointSpec {

    @XmlAttribute(name=MailConstants.A_NAME, required=true)
    private final String name;

    @XmlAttribute(name=MailConstants.A_DEFAULT_VIEW, required=false)
    private String defaultView;

    @XmlAttribute(name=MailConstants.A_FLAGS, required=false)
    private String flags;

    @XmlAttribute(name=MailConstants.A_COLOR, required=false)
    private Byte color;

    @XmlAttribute(name=MailConstants.A_RGB, required=false)
    private String rgb;

    @XmlAttribute(name=MailConstants.A_URL, required=false)
    private String url;

    @XmlAttribute(name=MailConstants.A_FOLDER, required=false)
    private String folderId;

    @XmlAttribute(name=MailConstants.A_FETCH_IF_EXISTS, required=false)
    private ZmBoolean fetchIfExists;

    @XmlAttribute(name=MailConstants.A_REMINDER, required=false)
    private ZmBoolean reminderEnabled;

    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID, required=false)
    private String ownerId;

    @XmlAttribute(name=MailConstants.A_OWNER_NAME, required=false)
    private String ownerName;

    @XmlAttribute(name=MailConstants.A_REMOTE_ID, required=false)
    private Integer remoteId;

    @XmlAttribute(name=MailConstants.A_PATH, required=false)
    private String path;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private NewMountpointSpec() {
        this((String) null);
    }

    public NewMountpointSpec(String name) {
        this.name = name;
    }

    public void setDefaultView(String defaultView) { this.defaultView = defaultView; }
    public void setFlags(String flags) { this.flags = flags; }
    public void setColor(Byte color) { this.color = color; }
    public void setRgb(String rgb) { this.rgb = rgb; }
    public void setUrl(String url) { this.url = url; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setFetchIfExists(Boolean fetchIfExists) {
        this.fetchIfExists = ZmBoolean.fromBool(fetchIfExists);
    }
    public void setReminderEnabled(Boolean reminderEnabled) {
        this.reminderEnabled = ZmBoolean.fromBool(reminderEnabled);
    }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setRemoteId(Integer remoteId) { this.remoteId = remoteId; }
    public void setPath(String path) { this.path = path; }
    public String getName() { return name; }
    public String getDefaultView() { return defaultView; }
    public String getFlags() { return flags; }
    public Byte getColor() { return color; }
    public String getRgb() { return rgb; }
    public String getUrl() { return url; }
    public String getFolderId() { return folderId; }
    public Boolean getFetchIfExists() { return ZmBoolean.toBool(fetchIfExists); }
    public Boolean getReminderEnabled() { return ZmBoolean.toBool(reminderEnabled); }
    public String getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public Integer getRemoteId() { return remoteId; }
    public String getPath() { return path; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("defaultView", defaultView)
            .add("flags", flags)
            .add("color", color)
            .add("rgb", rgb)
            .add("url", url)
            .add("folderId", folderId)
            .add("fetchIfExists", fetchIfExists)
            .add("reminderEnabled", reminderEnabled)
            .add("ownerId", ownerId)
            .add("ownerName", ownerName)
            .add("remoteId", remoteId)
            .add("path", path)
            .toString();
    }
}
