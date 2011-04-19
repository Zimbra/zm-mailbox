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
public class CalendarAttendee {

    @XmlAttribute(name=MailConstants.A_ADDRESS, required=false)
    private String address;

    // For backwards compatibility
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

    @XmlAttribute(name=MailConstants.A_CAL_CUTYPE, required=false)
    private String cuType;

    @XmlAttribute(name=MailConstants.A_CAL_ROLE, required=false)
    private String role;

    @XmlAttribute(name=MailConstants.A_CAL_PARTSTAT, required=false)
    private String partStat;

    @XmlAttribute(name=MailConstants.A_CAL_RSVP, required=false)
    private Boolean rsvp;

    @XmlAttribute(name=MailConstants.A_CAL_MEMBER, required=false)
    private String member;

    @XmlAttribute(name=MailConstants.A_CAL_DELEGATED_TO, required=false)
    private String delegatedTo;

    @XmlAttribute(name=MailConstants.A_CAL_DELEGATED_FROM, required=false)
    private String delegatedFrom;

    @XmlElement(name=MailConstants.E_CAL_XPARAM, required=false)
    private List<XParam> xParams = Lists.newArrayList();

    public CalendarAttendee() {
    }

    public void setAddress(String address) { this.address = address; }
    public void setUrl(String url) { this.url = url; }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    public void setSentBy(String sentBy) { this.sentBy = sentBy; }
    public void setDir(String dir) { this.dir = dir; }
    public void setLanguage(String language) { this.language = language; }
    public void setCuType(String cuType) { this.cuType = cuType; }
    public void setRole(String role) { this.role = role; }
    public void setPartStat(String partStat) { this.partStat = partStat; }
    public void setRsvp(Boolean rsvp) { this.rsvp = rsvp; }
    public void setMember(String member) { this.member = member; }
    public void setDelegatedTo(String delegatedTo) {
        this.delegatedTo = delegatedTo;
    }

    public void setDelegatedFrom(String delegatedFrom) {
        this.delegatedFrom = delegatedFrom;
    }

    public void setXParams(Iterable <XParam> xParams) {
        this.xParams.clear();
        if (xParams != null) {
            Iterables.addAll(this.xParams,xParams);
        }
    }

    public CalendarAttendee addXParam(XParam xParam) {
        this.xParams.add(xParam);
        return this;
    }

    public String getAddress() { return address; }
    public String getUrl() { return url; }
    public String getDisplayName() { return displayName; }
    public String getSentBy() { return sentBy; }
    public String getDir() { return dir; }
    public String getLanguage() { return language; }
    public String getCuType() { return cuType; }
    public String getRole() { return role; }
    public String getPartStat() { return partStat; }
    public Boolean getRsvp() { return rsvp; }
    public String getMember() { return member; }
    public String getDelegatedTo() { return delegatedTo; }
    public String getDelegatedFrom() { return delegatedFrom; }
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
            .add("cuType", cuType)
            .add("role", role)
            .add("partStat", partStat)
            .add("rsvp", rsvp)
            .add("member", member)
            .add("delegatedTo", delegatedTo)
            .add("delegatedFrom", delegatedFrom)
            .add("xParams", xParams);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
