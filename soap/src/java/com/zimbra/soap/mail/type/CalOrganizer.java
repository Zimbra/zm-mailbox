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
import com.zimbra.soap.base.CalOrganizerInterface;
import com.zimbra.soap.base.XParamInterface;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_CALENDAR_ORGANIZER, description="Calendar organizer")
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
    @GraphQLQuery(name=GqlConstants.XPARAMS, description="Non-standard parameters")
    private final List<XParam> xParams = Lists.newArrayList();

    public CalOrganizer() {
    }

    public static CalOrganizer createForAddress(String addr) {
        CalOrganizer co = new CalOrganizer();
        co.setAddress(addr);
        return co;
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
    @GraphQLInputField(name=GqlConstants.XPARAMS, description="Non-standard parameters")
    public void setXParams(Iterable <XParam> xParams) {
        this.xParams.clear();
        if (xParams != null) {
            Iterables.addAll(this.xParams,xParams);
        }
    }

    @GraphQLIgnore
    public CalOrganizer addXParam(XParam xParam) {
        this.xParams.add(xParam);
        return this;
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
    @GraphQLQuery(name=GqlConstants.XPARAMS, description="Non-standard parameters")
    public List<XParam> getXParams() {
        return Collections.unmodifiableList(xParams);
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
