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

@XmlAccessorType(XmlAccessType.FIELD)
public class NewFolderSpec {

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
    private String parentFolderId;

    @XmlAttribute(name=MailConstants.A_FETCH_IF_EXISTS, required=false)
    private ZmBoolean fetchIfExists;

    @XmlAttribute(name=MailConstants.A_SYNC, required=false)
    private ZmBoolean syncToUrl;

    @XmlElementWrapper(name=MailConstants.E_ACL, required=false)
    @XmlElement(name=MailConstants.E_GRANT, required=false)
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

    public void setDefaultView(String defaultView) {
        this.defaultView = defaultView;
    }
    public void setFlags(String flags) { this.flags = flags; }
    public void setColor(Byte color) { this.color = color; }
    public void setRgb(String rgb) { this.rgb = rgb; }
    public void setUrl(String url) { this.url = url; }
    public void setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }
    public void setFetchIfExists(Boolean fetchIfExists) { this.fetchIfExists = ZmBoolean.fromBool(fetchIfExists); }
    public void setSyncToUrl(Boolean syncToUrl) { this.syncToUrl = ZmBoolean.fromBool(syncToUrl); }
    public void setGrants(Iterable <ActionGrantSelector> grants) {
        this.grants.clear();
        if (grants != null) {
            Iterables.addAll(this.grants,grants);
        }
    }

    public NewFolderSpec addGrant(ActionGrantSelector grant) {
        this.grants.add(grant);
        return this;
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
        return Collections.unmodifiableList(grants);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("defaultView", defaultView)
            .add("flags", flags)
            .add("color", color)
            .add("rgb", rgb)
            .add("url", url)
            .add("parentFolderId", parentFolderId)
            .add("fetchIfExists", fetchIfExists)
            .add("syncToUrl", syncToUrl)
            .add("grants", grants)
            .toString();
    }
}
