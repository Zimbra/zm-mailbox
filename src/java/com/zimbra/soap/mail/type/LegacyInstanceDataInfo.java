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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"organizer", "categories", "geo", "fragment"})
public class LegacyInstanceDataInfo
extends LegacyInstanceDataAttrs
implements InstanceDataInterface {

    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=false)
    private Long startTime;

    @XmlAttribute(name=MailConstants.A_CAL_IS_EXCEPTION /* ex */, required=false)
    private ZmBoolean isException;

    @XmlElement(name=MailConstants.E_CAL_ORGANIZER /* or */, required=false)
    private CalOrganizer organizer;

    @XmlElement(name=MailConstants.E_CAL_CATEGORY /* category */, required=false)
    private List<String> categories = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_GEO /* geo */, required=false)
    private GeoInfo geo;

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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
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
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
