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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class NewMountpointSpec {

    /**
     * @zm-api-field-tag mountpoint-name
     * @zm-api-field-description Mountpoint name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag default-type
     * @zm-api-field-description (optional) Default type for the folder; used by web client to decide which view to use;
     * <br />
     * possible values are the same as <b>&lt;SearchRequest></b>'s {types}: <b>conversation|message|contact|etc</b>
     */
    @XmlAttribute(name=MailConstants.A_DEFAULT_VIEW /* view */, required=false)
    private String defaultView;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-tag color
     * @zm-api-field-description color numeric; range 0-127; defaults to 0 if not present; client can display only 0-7
     */
    @XmlAttribute(name=MailConstants.A_COLOR /* color */, required=false)
    private Byte color;

    /**
     * @zm-api-field-tag rgb-color
     * @zm-api-field-description RGB color in format #rrggbb where r,g and b are hex digits
     */
    @XmlAttribute(name=MailConstants.A_RGB /* rgb */, required=false)
    private String rgb;

    /**
     * @zm-api-field-tag remote-url
     * @zm-api-field-description URL (RSS, iCal, etc.) this folder syncs its contents to
     */
    @XmlAttribute(name=MailConstants.A_URL /* url */, required=false)
    private String url;

    /**
     * @zm-api-field-tag parent-folder-id
     * @zm-api-field-description Parent folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag fetch-if-exists
     * @zm-api-field-description If set, the server will fetch the folder if it already exists rather than throwing
     * mail.ALREADY_EXISTS
     */
    @XmlAttribute(name=MailConstants.A_FETCH_IF_EXISTS /* fie */, required=false)
    private ZmBoolean fetchIfExists;

    /**
     * @zm-api-field-tag display-reminders
     * @zm-api-field-description If set, client should display reminders for shared appointments/tasks
     */
    @XmlAttribute(name=MailConstants.A_REMINDER /* reminder */, required=false)
    private ZmBoolean reminderEnabled;

    /**
     * @zm-api-field-tag owner-zimbra-id
     * @zm-api-field-description Zimbra ID (guid) of the owner of the linked-to resource
     */
    @XmlAttribute(name=MailConstants.A_ZIMBRA_ID /* zid */, required=false)
    private String ownerId;

    /**
     * @zm-api-field-tag owner-email-addr
     * @zm-api-field-description Primary email address of the owner of the linked-to resource
     */
    @XmlAttribute(name=MailConstants.A_OWNER_NAME /* owner */, required=false)
    private String ownerName;

    /**
     * @zm-api-field-tag id-of-shared-item
     * @zm-api-field-description Item ID of the linked-to resource in the remote mailbox
     */
    @XmlAttribute(name=MailConstants.A_REMOTE_ID /* rid */, required=false)
    private Integer remoteId;

    /**
     * @zm-api-field-tag path-to-shared-item
     * @zm-api-field-description Path to shared item
     */
    @XmlAttribute(name=MailConstants.A_PATH /* path */, required=false)
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
    public void setFetchIfExists(Boolean fetchIfExists) { this.fetchIfExists = ZmBoolean.fromBool(fetchIfExists); }
    public void setReminderEnabled(Boolean reminderEnabled) { this.reminderEnabled = ZmBoolean.fromBool(reminderEnabled); }
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
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
            .add("path", path);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
