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
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"organizer", "categories", "geo", "fragment", "instances", "alarmData"})
public abstract class CalendaringData
extends CommonCalendaringData
implements CalendaringDataInterface {

    /**
     * @zm-api-field-tag date
     * @zm-api-field-description Date
     */
    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    /**
     * @zm-api-field-description Organizer
     */
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

    /**
     * @zm-api-field-description Instances
     */
    @XmlElement(name=MailConstants.E_INSTANCE /* inst */, required=false)
    private List<InstanceDataInfo> instances = Lists.newArrayList();

    /**
     * @zm-api-field-description Alarm information
     */
    @XmlElement(name=MailConstants.E_CAL_ALARM_DATA /* alarmData */, required=false)
    private AlarmDataInfo alarmData;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    protected CalendaringData() {
        this((String) null, (String) null);
    }

    public CalendaringData(String xUid, String uid) {
        super(xUid, uid);
    }

    @Override
    public void setDate(Long date) { this.date = date; }

    @Override
    public void setOrganizer(CalOrganizer organizer) {
        this.organizer = organizer;
    }
    @Override
    public void setCategories(Iterable <String> categories) {
        this.categories.clear();
        if (categories != null) {
            Iterables.addAll(this.categories,categories);
        }
    }

    @Override
    public void addCategory(String category) {
        this.categories.add(category);
    }

    @Override
    public void setGeo(GeoInfo geo) { this.geo = geo; }
    @Override
    public void setFragment(String fragment) { this.fragment = fragment; }
    public void setInstances(Iterable <InstanceDataInfo> instances) {
        this.instances.clear();
        if (instances != null) {
            Iterables.addAll(this.instances,instances);
        }
    }

    public void addInstance(InstanceDataInfo instance) {
        this.instances.add(instance);
    }

    @Override
    public void setAlarmData(AlarmDataInfo alarmData) {
        this.alarmData = alarmData;
    }

    @Override
    public Long getDate() { return date; }
    @Override
    public CalOrganizer getOrganizer() { return organizer; }
    @Override
    public List<String> getCategories() {
        return Collections.unmodifiableList(categories);
    }
    @Override
    public GeoInfo getGeo() { return geo; }
    @Override
    public String getFragment() { return fragment; }
    public List<InstanceDataInfo> getInstances() {
        return Collections.unmodifiableList(instances);
    }
    @Override
    public AlarmDataInfo getAlarmData() { return alarmData; }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("date", date)
            .add("organizer", organizer)
            .add("categories", categories)
            .add("geo", geo)
            .add("fragment", fragment)
            .add("instances", instances)
            .add("alarmData", alarmData);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

    // Non-JAXB method needed by CalendaringDataInterface
    @Override
    public void setCalendaringInstances(
            Iterable <InstanceDataInterface> instances) {
        this.instances.clear();
        if (instances != null) {
            for (InstanceDataInterface inst : instances) {
                addCalendaringInstance(inst);
            }
        }
    }

    // Non-JAXB method needed by CalendaringDataInterface
    @Override
    public void addCalendaringInstance(InstanceDataInterface instance) {
        if (instance instanceof InstanceDataInfo) {
            addInstance((InstanceDataInfo) instance);
        }
    }

    // Non-JAXB method needed by CalendaringDataInterface
    @Override
    public List<InstanceDataInterface> getCalendaringInstances() {
        List<InstanceDataInterface> insts = Lists.newArrayList();
        Iterables.addAll(insts,instances);
        return Collections.unmodifiableList(insts);
    }
}
