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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"organizer", "categories", "geo",
                        "fragment", "instances", "alarmData"})
public class LegacyCalendaringData extends CommonCalendaringData {

    @XmlElement(name=MailConstants.E_CAL_ORGANIZER, required=false)
    private CalOrganizer organizer;

    @XmlElement(name=MailConstants.E_CAL_CATEGORY, required=false)
    private List<String> categories = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_GEO, required=false)
    private GeoInfo geo;

    @XmlElement(name=MailConstants.E_FRAG, required=false)
    private String fragment;

    @XmlElement(name=MailConstants.E_INSTANCE, required=false)
    private List<LegacyInstanceDataInfo> instances = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_ALARM_DATA, required=false)
    private AlarmDataInfo alarmData;

    public void setOrganizer(CalOrganizer organizer) {
        this.organizer = organizer;
    }
    public void setCategories(Iterable <String> categories) {
        this.categories.clear();
        if (categories != null) {
            Iterables.addAll(this.categories,categories);
        }
    }

    public LegacyCalendaringData addCategory(String category) {
        this.categories.add(category);
        return this;
    }

    public void setGeo(GeoInfo geo) { this.geo = geo; }
    public void setFragment(String fragment) { this.fragment = fragment; }
    public void setInstances(Iterable <LegacyInstanceDataInfo> instances) {
        this.instances.clear();
        if (instances != null) {
            Iterables.addAll(this.instances,instances);
        }
    }

    public LegacyCalendaringData addInstance(LegacyInstanceDataInfo instance) {
        this.instances.add(instance);
        return this;
    }

    public void setAlarmData(AlarmDataInfo alarmData) {
        this.alarmData = alarmData;
    }
    public CalOrganizer getOrganizer() { return organizer; }
    public List<String> getCategories() {
        return Collections.unmodifiableList(categories);
    }
    public GeoInfo getGeo() { return geo; }
    public String getFragment() { return fragment; }
    public List<LegacyInstanceDataInfo> getInstances() {
        return Collections.unmodifiableList(instances);
    }
    public AlarmDataInfo getAlarmData() { return alarmData; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("organizer", organizer)
            .add("categories", categories)
            .add("geo", geo)
            .add("fragment", fragment)
            .add("instances", instances)
            .add("alarmData", alarmData);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
