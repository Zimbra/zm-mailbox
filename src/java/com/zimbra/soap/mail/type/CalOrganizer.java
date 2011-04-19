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

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class CalOrganizer {

    @XmlAttribute(name=MailConstants.A_ADDRESS, required=false)
    private String address;

    @XmlAttribute(name=MailConstants.A_URL, required=false)
    private String url;

    @XmlAttribute(name=MailConstants.A_DISPLAY, required=false)
    private String displayName;

    @XmlAttribute(name=MailConstants.A_CAL_SENTBY, required=false)
    private String sentBy;

    @XmlAttribute(name=MailConstants.A_CAL_DIR, required=false)
    private String dir;

    @XmlAttribute(name=MailConstants.A_CAL_LANGUAGE, required=false)
    private String language;

    @XmlElement(name=MailConstants.E_CAL_XPARAM, required=false)
    private List<XParam> xParams = Lists.newArrayList();

    public CalOrganizer() {
    }

    public void setAddress(String address) { this.address = address; }
    public void setUrl(String url) { this.url = url; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setSentBy(String sentBy) { this.sentBy = sentBy; }
    public void setDir(String dir) { this.dir = dir; }
    public void setLanguage(String language) { this.language = language; }
    public void setXParams(Iterable <XParam> xParams) {
        this.xParams.clear();
        if (xParams != null) {
            Iterables.addAll(this.xParams,xParams);
        }
    }

    public CalOrganizer addXParam(XParam xParam) {
        this.xParams.add(xParam);
        return this;
    }

    public String getAddress() { return address; }
    public String getUrl() { return url; }
    public String getDisplayName() { return displayName; }
    public String getSentBy() { return sentBy; }
    public String getDir() { return dir; }
    public String getLanguage() { return language; }
    public List<XParam> getXParams() {
        return Collections.unmodifiableList(xParams);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("address", address)
            .add("url", url)
            .add("displayName", displayName)
            .add("sentBy", sentBy)
            .add("dir", dir)
            .add("language", language)
            .add("xParams", xParams);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
