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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CalendarAttendeeInterface;
import com.zimbra.soap.base.XParamInterface;
import com.zimbra.soap.type.ZmBoolean;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_CALENDAR_ATTENDEE, description="Calendar attendee information")
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

    // Think that full iCalendar equivalents can also be used?
    /**
     * @zm-api-field-tag participation-status
     * @zm-api-field-description iCalendar PTST (Participation status)
     * <br />
     * Valid values: <b>NE|AC|TE|DE|DG|CO|IN|WE|DF</b>
     * <br />
     * Meanings:
     * <br />
     * "NE"eds-action, "TE"ntative, "AC"cept, "DE"clined, "DG" (delegated), "CO"mpleted (todo), "IN"-process (todo),
     * "WA"iting (custom value only for todo), "DF" (deferred; custom value only for todo)
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
    @GraphQLQuery(name=GqlConstants.XPARAMS, description="Non-standard parameters")
    private final List<XParam> xParams = Lists.newArrayList();

    public CalendarAttendee() {
    }

    public static CalendarAttendee createForAddressDisplaynameRolePartstatRsvp(String attendeeEmail,
            String attendeeName, String role, String partstat, Boolean rsvp) {
        CalendarAttendee calAttendee = new CalendarAttendee();
        calAttendee.setAddress(attendeeEmail);
        calAttendee.setDisplayName(attendeeName);
        calAttendee.setRole(role);
        calAttendee.setPartStat(partstat);
        calAttendee.setRsvp(rsvp);
        return calAttendee;
    }

    @Override
    @GraphQLInputField(name=GqlConstants.ADDRESS, description="Email address (without MAILTO:)")
    public void setAddress(String address) { this.address = address; }
    @Override
    @GraphQLInputField(name=GqlConstants.URL, description="URL - has same value as emailAddress")
    public void setUrl(String url) { this.url = url; }
    @Override
    @GraphQLInputField(name=GqlConstants.DISPLAY_NAME, description="Friendly name - CN in iCalendar")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    @Override
    @GraphQLInputField(name=GqlConstants.SENT_BY, description="iCalendar SENT_BY")
    public void setSentBy(String sentBy) { this.sentBy = sentBy; }
    @Override
    @GraphQLInputField(name=GqlConstants.DIRECTORY, description="iCalendar DIR - Reference to a directory entry associated with the calendar user")
    public void setDir(String dir) { this.dir = dir; }
    @Override
    @GraphQLInputField(name=GqlConstants.LANGUAGE, description="iCalendar LANGUAGE - As defined in RFC5646 * (e.g. \"en-US\")")
    public void setLanguage(String language) { this.language = language; }
    @Override
    @GraphQLInputField(name=GqlConstants.CALENDAR_USER_TYPE, description="iCalendar CUTYPE (Calendar user type)")
    public void setCuType(String cuType) { this.cuType = cuType; }
    @Override
    @GraphQLInputField(name=GqlConstants.ROLE, description="iCalendar ROLE")
    public void setRole(String role) { this.role = role; }
    @Override
    @GraphQLInputField(name=GqlConstants.PARTICIPATION_STATUS, description="iCalendar PTST (Participation status)\n"
        + "* \"NE\"eds-action\n "
        + "* \"TE\"ntative\n "
        + "* \"AC\"cept\n "
        + "* \"DE\"clined\n "
        + "* \"DG\" (delegated)\n "
        + "* \"CO\"mpleted (todo)\n "
        + "* \"IN\"-process (todo)\n " 
        + "* \"WA\"iting (custom value only for todo)\n "
        + "* \"DF\" (deferred; custom value only for todo)")
    public void setPartStat(String partStat) { this.partStat = partStat; }
    @Override
    @GraphQLInputField(name=GqlConstants.RSVP, description="iCalendar RSVP")
    public void setRsvp(Boolean rsvp) { this.rsvp = ZmBoolean.fromBool(rsvp); }
    @Override
    @GraphQLInputField(name=GqlConstants.MEMBER, description="iCalendar MEMBER - The group or list membership of the calendar user")
    public void setMember(String member) { this.member = member; }
    @Override
    @GraphQLInputField(name=GqlConstants.DELEGATED_TO, description="iCalendar DELEGATED-TO")
    public void setDelegatedTo(String delegatedTo) {
        this.delegatedTo = delegatedTo;
    }

    @Override
    @GraphQLInputField(name=GqlConstants.DELEGATED_FROM, description="iCalendar DELEGATED-FROM")
    public void setDelegatedFrom(String delegatedFrom) {
        this.delegatedFrom = delegatedFrom;
    }

    @GraphQLInputField(name=GqlConstants.XPARAMS, description="Non-standard parameters")
    public void setXParams(Iterable <XParam> xParams) {
        this.xParams.clear();
        if (xParams != null) {
            Iterables.addAll(this.xParams,xParams);
        }
    }

    @GraphQLIgnore
    public void addXParam(XParam xParam) {
        this.xParams.add(xParam);
    }

    @Override
    @GraphQLQuery(name=GqlConstants.ADDRESS, description="Email address (without MAILTO:)")
    public String getAddress() { return address; }
    @Override
    @GraphQLQuery(name=GqlConstants.URL, description="URL - has same value as emailAddress")
    public String getUrl() { return url; }
    @Override
    @GraphQLQuery(name=GqlConstants.DISPLAY_NAME, description="Friendly name - CN in iCalendar")
    public String getDisplayName() { return displayName; }
    @Override
    @GraphQLQuery(name=GqlConstants.SENT_BY, description="iCalendar SENT_BY")
    public String getSentBy() { return sentBy; }
    @Override
    @GraphQLQuery(name=GqlConstants.DIRECTORY, description="iCalendar DIR - Reference to a directory entry associated with the calendar user")
    public String getDir() { return dir; }
    @Override
    @GraphQLQuery(name=GqlConstants.LANGUAGE, description="iCalendar LANGUAGE - As defined in RFC5646 * (e.g. \"en-US\")")
    public String getLanguage() { return language; }
    @Override
    @GraphQLQuery(name=GqlConstants.CALENDAR_USER_TYPE, description="iCalendar CUTYPE (Calendar user type)")
    public String getCuType() { return cuType; }
    @Override
    @GraphQLQuery(name=GqlConstants.ROLE, description="iCalendar ROLE")
    public String getRole() { return role; }
    @Override
    @GraphQLQuery(name=GqlConstants.PARTICIPATION_STATUS, description="iCalendar PTST (Participation status)\n"
        + "* \"NE\"eds-action\n "
        + "* \"TE\"ntative\n "
        + "* \"AC\"cept\n "
        + "* \"DE\"clined\n "
        + "* \"DG\" (delegated)\n "
        + "* \"CO\"mpleted (todo)\n "
        + "* \"IN\"-process (todo)\n " 
        + "* \"WA\"iting (custom value only for todo)\n "
        + "* \"DF\" (deferred; custom value only for todo)")
    public String getPartStat() { return partStat; }
    @Override
    @GraphQLQuery(name=GqlConstants.RSVP, description="iCalendar RSVP")
    public Boolean getRsvp() { return ZmBoolean.toBool(rsvp); }
    @Override
    @GraphQLQuery(name=GqlConstants.MEMBER, description="iCalendar MEMBER - The group or list membership of the calendar user")
    public String getMember() { return member; }
    @Override
    @GraphQLQuery(name=GqlConstants.DELEGATED_TO, description="iCalendar DELEGATED-TO")
    public String getDelegatedTo() { return delegatedTo; }
    @Override
    @GraphQLQuery(name=GqlConstants.DELEGATED_FROM, description="iCalendar DELEGATED-FROM")
    public String getDelegatedFrom() { return delegatedFrom; }
    @GraphQLQuery(name=GqlConstants.XPARAMS, description="Non-standard parameters")
    public List<XParam> getXParams() {
        return Collections.unmodifiableList(xParams);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

    @Override
    @GraphQLIgnore
    public void setXParamInterfaces(Iterable<XParamInterface> xParams) {
        setXParams(XParam.fromInterfaces(xParams));
    }

    @Override
    @GraphQLIgnore
    public void addXParamInterface(XParamInterface xParam) {
        addXParam((XParam) xParam);
    }

    @Override
    @GraphQLIgnore
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
