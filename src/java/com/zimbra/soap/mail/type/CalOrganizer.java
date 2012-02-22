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
import com.zimbra.soap.base.CalOrganizerInterface;
import com.zimbra.soap.base.XParamInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class CalOrganizer implements CalOrganizerInterface {

    /**
     * @zm-api-field-tag email-address
     * @zm-api-field-description Email address (without "MAILTO:")
     */
    @XmlAttribute(name=MailConstants.A_ADDRESS /* a */, required=false)
    private String address;

    /**
     * @zm-api-field-tag url
     * @zm-api-field-description URL - has same value as <b>{email-address}</b>.
     * <br />
     * Maintained for backwards compatibility with ZCS 4.5
     */
    @XmlAttribute(name=MailConstants.A_URL /* url */, required=false)
    private String url;

    /**
     * @zm-api-field-tag friendly-name
     * @zm-api-field-description Friendly name - "CN" in iCalendar
     */
    @XmlAttribute(name=MailConstants.A_DISPLAY /* d */, required=false)
    private String displayName;

    /**
     * @zm-api-field-tag sent-by
     * @zm-api-field-description iCalendar SENT-BY
     */
    @XmlAttribute(name=MailConstants.A_CAL_SENTBY /* sentBy */, required=false)
    private String sentBy;

    /**
     * @zm-api-field-tag dir
     * @zm-api-field-description iCalendar DIR - Reference to a directory entry associated with the calendar user.
     */
    @XmlAttribute(name=MailConstants.A_CAL_DIR /* dir */, required=false)
    private String dir;

    /**
     * @zm-api-field-tag language
     * @zm-api-field-description iCalendar LANGUAGE - As defined in RFC5646 * (e.g. "en-US")
     */
    @XmlAttribute(name=MailConstants.A_CAL_LANGUAGE /* lang */, required=false)
    private String language;

    /**
     * @zm-api-field-description Non-standard parameters (XPARAMs)
     */
    @XmlElement(name=MailConstants.E_CAL_XPARAM /* xparam */, required=false)
    private List<XParam> xParams = Lists.newArrayList();

    public CalOrganizer() {
    }

    @Override
    public void setAddress(String address) { this.address = address; }
    @Override
    public void setUrl(String url) { this.url = url; }
    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    @Override
    public void setSentBy(String sentBy) { this.sentBy = sentBy; }
    @Override
    public void setDir(String dir) { this.dir = dir; }
    @Override
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

    @Override
    public String getAddress() { return address; }
    @Override
    public String getUrl() { return url; }
    @Override
    public String getDisplayName() { return displayName; }
    @Override
    public String getSentBy() { return sentBy; }
    @Override
    public String getDir() { return dir; }
    @Override
    public String getLanguage() { return language; }
    public List<XParam> getXParams() {
        return Collections.unmodifiableList(xParams);
    }

    @Override
    public void setXParamInterfaces(Iterable<XParamInterface> xParams) {
        setXParams(XParam.fromInterfaces(xParams));
    }

    @Override
    public void addXParamInterface(XParamInterface xParam) {
        addXParam((XParam) xParam);
    }

    @Override
    public List<XParamInterface> getXParamInterfaces() {
        return XParam.toInterfaces(xParams);
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
