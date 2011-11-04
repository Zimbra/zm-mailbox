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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CalendarAttendeeInterface;
import com.zimbra.soap.base.XParamInterface;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class CalendarAttendee implements CalendarAttendeeInterface {

    @XmlAttribute(name=MailConstants.A_ADDRESS /* a */, required=false)
    private String address;

    // For backwards compatibility
    @XmlAttribute(name=MailConstants.A_URL /* url */, required=false)
    private String url;

    @XmlAttribute(name=MailConstants.A_DISPLAY /* d */, required=false)
    private String displayName;

    @XmlAttribute(name=MailConstants.A_CAL_SENTBY /* sentBy */, required=false)
    private String sentBy;

    @XmlAttribute(name=MailConstants.A_CAL_DIR /* dir */, required=false)
    private String dir;

    @XmlAttribute(name=MailConstants.A_CAL_LANGUAGE /* lang */, required=false)
    private String language;

    @XmlAttribute(name=MailConstants.A_CAL_CUTYPE /* cutype */, required=false)
    private String cuType;

    @XmlAttribute(name=MailConstants.A_CAL_ROLE /* role */, required=false)
    private String role;

    @XmlAttribute(name=MailConstants.A_CAL_PARTSTAT /* ptst */, required=false)
    private String partStat;

    @XmlAttribute(name=MailConstants.A_CAL_RSVP /* rsvp */, required=false)
    private ZmBoolean rsvp;

    @XmlAttribute(name=MailConstants.A_CAL_MEMBER /* member */, required=false)
    private String member;

    @XmlAttribute(name=MailConstants.A_CAL_DELEGATED_TO /* delTo */, required=false)
    private String delegatedTo;

    @XmlAttribute(name=MailConstants.A_CAL_DELEGATED_FROM /* delFrom */, required=false)
    private String delegatedFrom;

    @XmlElement(name=MailConstants.E_CAL_XPARAM /* xparam */, required=false)
    private List<XParam> xParams = Lists.newArrayList();

    public CalendarAttendee() {
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
    @Override
    public void setCuType(String cuType) { this.cuType = cuType; }
    @Override
    public void setRole(String role) { this.role = role; }
    @Override
    public void setPartStat(String partStat) { this.partStat = partStat; }
    @Override
    public void setRsvp(Boolean rsvp) { this.rsvp = ZmBoolean.fromBool(rsvp); }
    @Override
    public void setMember(String member) { this.member = member; }
    @Override
    public void setDelegatedTo(String delegatedTo) {
        this.delegatedTo = delegatedTo;
    }

    @Override
    public void setDelegatedFrom(String delegatedFrom) {
        this.delegatedFrom = delegatedFrom;
    }

    public void setXParams(Iterable <XParam> xParams) {
        this.xParams.clear();
        if (xParams != null) {
            Iterables.addAll(this.xParams,xParams);
        }
    }

    public void addXParam(XParam xParam) {
        this.xParams.add(xParam);
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
    @Override
    public String getCuType() { return cuType; }
    @Override
    public String getRole() { return role; }
    @Override
    public String getPartStat() { return partStat; }
    @Override
    public Boolean getRsvp() { return ZmBoolean.toBool(rsvp); }
    @Override
    public String getMember() { return member; }
    @Override
    public String getDelegatedTo() { return delegatedTo; }
    @Override
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

    public static Iterable <CalendarAttendee> fromInterfaces(
                Iterable <CalendarAttendeeInterface> params) {
        if (params == null)
            return null;
        List <CalendarAttendee> newList = Lists.newArrayList();
        for (CalendarAttendeeInterface param : params) {
            newList.add((CalendarAttendee) param);
        }
        return newList;
    }

    public static List <CalendarAttendeeInterface> toInterfaces(
                Iterable <CalendarAttendee> params) {
        if (params == null)
            return null;
        List <CalendarAttendeeInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }
}
