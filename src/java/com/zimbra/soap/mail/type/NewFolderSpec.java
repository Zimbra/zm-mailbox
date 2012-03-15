/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class NewFolderSpec {

    /**
     * @zm-api-field-tag folder-name
     * @zm-api-field-description If "l" is unset, name is the full path of the new folder; otherwise, name may not
     * contain the folder separator '/'
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
    private String parentFolderId;

    /**
     * @zm-api-field-tag fetch-if-exists
     * @zm-api-field-description If set, the server will fetch the folder if it already exists rather than throwing
     * mail.ALREADY_EXISTS
     */
    @XmlAttribute(name=MailConstants.A_FETCH_IF_EXISTS /* fie */, required=false)
    private ZmBoolean fetchIfExists;

    /**
     * @zm-api-field-tag sync-to-url
     * @zm-api-field-description If set (default) then if "url" is set, synchronize folder content on folder creation
     */
    @XmlAttribute(name=MailConstants.A_SYNC /* sync */, required=false)
    private ZmBoolean syncToUrl;

    /**
     * @zm-api-field-description Grant specification
     */
    @XmlElementWrapper(name=MailConstants.E_ACL /* acl */, required=false)
    @XmlElement(name=MailConstants.E_GRANT /* grant */, required=false)
    private List<ActionGrantSelector> grants = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private NewFolderSpec() {
        this((String) null);
    }

    public NewFolderSpec(String name) {
        this.name = name;
    }

    public void setDefaultView(String defaultView) { this.defaultView = defaultView; }
    public void setFlags(String flags) { this.flags = flags; }
    public void setColor(Byte color) { this.color = color; }
    public void setRgb(String rgb) { this.rgb = rgb; }
    public void setUrl(String url) { this.url = url; }
    public void setParentFolderId(String parentFolderId) { this.parentFolderId = parentFolderId; }
    public void setFetchIfExists(Boolean fetchIfExists) { this.fetchIfExists = ZmBoolean.fromBool(fetchIfExists); }
    public void setSyncToUrl(Boolean syncToUrl) { this.syncToUrl = ZmBoolean.fromBool(syncToUrl); }
    public void setGrants(Iterable <ActionGrantSelector> grants) {
        this.grants.clear();
        if (grants != null) {
            Iterables.addAll(this.grants,grants);
        }
    }

    public void addGrant(ActionGrantSelector grant) {
        this.grants.add(grant);
    }

    public String getName() { return name; }
    public String getDefaultView() { return defaultView; }
    public String getFlags() { return flags; }
    public Byte getColor() { return color; }
    public String getRgb() { return rgb; }
    public String getUrl() { return url; }
    public String getParentFolderId() { return parentFolderId; }
    public Boolean getFetchIfExists() { return ZmBoolean.toBool(fetchIfExists); }
    public Boolean getSyncToUrl() { return ZmBoolean.toBool(syncToUrl); }
    public List<ActionGrantSelector> getGrants() {
        return grants;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("defaultView", defaultView)
            .add("flags", flags)
            .add("color", color)
            .add("rgb", rgb)
            .add("url", url)
            .add("parentFolderId", parentFolderId)
            .add("fetchIfExists", fetchIfExists)
            .add("syncToUrl", syncToUrl)
            .add("grants", grants);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
