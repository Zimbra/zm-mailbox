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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
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
    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
