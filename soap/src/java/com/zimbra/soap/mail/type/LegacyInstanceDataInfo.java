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
import com.zimbra.soap.type.ZmBoolean;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"organizer", "categories", "geo", "fragment"})
public class LegacyInstanceDataInfo
extends LegacyInstanceDataAttrs
implements InstanceDataInterface {

    /**
     * @zm-api-field-tag start-time
     * @zm-api-field-description Start time
     */
    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=false)
    private Long startTime;

    /**
     * @zm-api-field-tag is-exception
     * @zm-api-field-description Set if is an exception
     */
    @XmlAttribute(name=MailConstants.A_CAL_IS_EXCEPTION /* ex */, required=false)
    private ZmBoolean isException;

    /**
     * @zm-api-field-description Organizer
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_CAL_ORGANIZER /* or */, required=false)
    private CalOrganizer organizer;

    /**
     * @zm-api-field-tag categories
     * @zm-api-field-description Categories
     */
    @XmlElement(name=MailConstants.E_CAL_CATEGORY /* category */, required=false)
    private List<String> categories = Lists.newArrayList();

    /**
     * @zm-api-field-description Information for iCalendar GEO property
     */
    @XmlElement(name=MailConstants.E_CAL_GEO /* geo */, required=false)
    private GeoInfo geo;

    /**
     * @zm-api-field-tag fragment
     * @zm-api-field-description First few bytes of the message (probably between 40 and 100 bytes)
     */
    @ZimbraJsonAttribute
    @XmlElement(name=MailConstants.E_FRAG /* fr */, required=false)
    private String fragment;

    public LegacyInstanceDataInfo() {
    }

    public void setStartTime(Long startTime) { this.startTime = startTime; }
    public void setIsException(Boolean isException) {
        this.isException = ZmBoolean.fromBool(isException);
    }
    public void setOrganizer(CalOrganizer organizer) {
        this.organizer = organizer;
    }
    public void setCategories(Iterable <String> categories) {
        this.categories.clear();
        if (categories != null) {
            Iterables.addAll(this.categories,categories);
        }
    }

    public void addCategory(String category) {
        this.categories.add(category);
    }

    public void setGeo(GeoInfo geo) { this.geo = geo; }
    public void setFragment(String fragment) { this.fragment = fragment; }
    public Long getStartTime() { return startTime; }
    public Boolean getIsException() { return ZmBoolean.toBool(isException); }
    public CalOrganizer getOrganizer() { return organizer; }
    public List<String> getCategories() {
        return Collections.unmodifiableList(categories);
    }
    public GeoInfo getGeo() { return geo; }
    public String getFragment() { return fragment; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("startTime", startTime)
            .add("isException", isException)
            .add("organizer", organizer)
            .add("categories", categories)
            .add("geo", geo)
            .add("fragment", fragment);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
