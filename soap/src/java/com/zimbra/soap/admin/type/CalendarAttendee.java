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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CalendarAttendeeInterface;
import com.zimbra.soap.base.XParamInterface;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class CalendarAttendee implements CalendarAttendeeInterface {

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
     * the property.
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
     * @zm-api-field-tag calendar-user-type
     * @zm-api-field-description iCalendar CUTYPE (Calendar user type)
     */
    @XmlAttribute(name=MailConstants.A_CAL_CUTYPE /* cutype */, required=false)
    private String cuType;

    /**
     * @zm-api-field-tag role
     * @zm-api-field-description iCalendar ROLE
     */
    @XmlAttribute(name=MailConstants.A_CAL_ROLE /* role */, required=false)
    private String role;

    /**
     * @zm-api-field-tag participation-status
     * @zm-api-field-description iCalendar PTST (Participation status)
     */
    @XmlAttribute(name=MailConstants.A_CAL_PARTSTAT /* ptst */, required=false)
    private String partStat;

    /**
     * @zm-api-field-tag rsvp
     * @zm-api-field-description iCalendar RSVP
     */
    @XmlAttribute(name=MailConstants.A_CAL_RSVP /* rsvp */, required=false)
    private ZmBoolean rsvp;

    /**
     * @zm-api-field-tag member
     * @zm-api-field-description iCalendar MEMBER - The group or list membership of the calendar user
     */
    @XmlAttribute(name=MailConstants.A_CAL_MEMBER /* member */, required=false)
    private String member;

    /**
     * @zm-api-field-tag delegated-to
     * @zm-api-field-description iCalendar DELEGATED-TO
     */
    @XmlAttribute(name=MailConstants.A_CAL_DELEGATED_TO /* delTo */, required=false)
    private String delegatedTo;

    /**
     * @zm-api-field-tag delegated-from
     * @zm-api-field-description iCalendar DELEGATED-FROM
     */
    @XmlAttribute(name=MailConstants.A_CAL_DELEGATED_FROM /* delFrom */, required=false)
    private String delegatedFrom;

    /**
     * @zm-api-field-description Non-standard parameters (XPARAMs)
     */
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

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this))
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
