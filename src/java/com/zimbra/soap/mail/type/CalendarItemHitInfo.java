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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.SearchHit;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"organizer", "categories", "geo", "fragment",
                    "instances", "alarmData", "invites", "replies"})
public class CalendarItemHitInfo
    extends CommonCalendaringData
    implements SearchHit {

    @XmlAttribute(name=MailConstants.A_SORT_FIELD /* sf */, required=false)
    private String sortField;

    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private long date;

    @XmlAttribute(name=MailConstants.A_CONTENTMATCHED /* cm */, required=false)
    private ZmBoolean contentMatched;

    @XmlAttribute(name=MailConstants.A_CAL_NEXT_ALARM /* nextAlarm */, required=false)
    private Long nextAlarm;

    @XmlElement(name=MailConstants.E_CAL_ORGANIZER /* or */, required=false)
    private CalOrganizer organizer;

    @XmlElement(name=MailConstants.E_CAL_CATEGORY /* category */, required=false)
    private List<String> categories = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_GEO /* geo */, required=false)
    private GeoInfo geo;

    @XmlElement(name=MailConstants.E_FRAG /* fr */, required=false)
    private String fragment;

    @XmlElement(name=MailConstants.E_INSTANCE /* inst */, required=false)
    private List<InstanceDataInfo> instances = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_ALARM_DATA /* alarmData */, required=false)
    private AlarmDataInfo alarmData;

    @XmlElement(name=MailConstants.E_INVITE /* inv */, required=false)
    private List<Invitation> invites = Lists.newArrayList();

    @XmlElementWrapper(name=MailConstants.E_CAL_REPLIES /* replies */, required=false)
    @XmlElement(name=MailConstants.E_CAL_REPLY /* reply */, required=false)
    private List<CalReply> replies = Lists.newArrayList();

    public CalendarItemHitInfo() {
    }

    public void setSortField(String sortField) { this.sortField = sortField; }
    public void setDate(long date) { this.date = date; }
    public void setContentMatched(Boolean contentMatched) {
        this.contentMatched = ZmBoolean.fromBool(contentMatched);
    }
    public void setNextAlarm(Long nextAlarm) { this.nextAlarm = nextAlarm; }
    public void setOrganizer(CalOrganizer organizer) {
        this.organizer = organizer;
    }
    public void setCategories(Iterable <String> categories) {
        this.categories.clear();
        if (categories != null) {
            Iterables.addAll(this.categories,categories);
        }
    }

    public CalendarItemHitInfo addCategory(String category) {
        this.categories.add(category);
        return this;
    }

    public void setGeo(GeoInfo geo) { this.geo = geo; }
    public void setFragment(String fragment) { this.fragment = fragment; }
    public void setInstances(Iterable <InstanceDataInfo> instances) {
        this.instances.clear();
        if (instances != null) {
            Iterables.addAll(this.instances,instances);
        }
    }

    public CalendarItemHitInfo addInstance(InstanceDataInfo instance) {
        this.instances.add(instance);
        return this;
    }

    public void setAlarmData(AlarmDataInfo alarmData) {
        this.alarmData = alarmData;
    }
    public void setInvites(Iterable <Invitation> invites) {
        this.invites.clear();
        if (invites != null) {
            Iterables.addAll(this.invites,invites);
        }
    }

    public CalendarItemHitInfo addInvite(Invitation invite) {
        this.invites.add(invite);
        return this;
    }

    public void setReplies(Iterable <CalReply> replies) {
        this.replies.clear();
        if (replies != null) {
            Iterables.addAll(this.replies,replies);
        }
    }

    public CalendarItemHitInfo addReply(CalReply reply) {
        this.replies.add(reply);
        return this;
    }

    public String getSortField() { return sortField; }
    public long getDate() { return date; }
    public Boolean getContentMatched() { return ZmBoolean.toBool(contentMatched); }
    public Long getNextAlarm() { return nextAlarm; }
    public CalOrganizer getOrganizer() { return organizer; }
    public List<String> getCategories() {
        return Collections.unmodifiableList(categories);
    }
    public GeoInfo getGeo() { return geo; }
    public String getFragment() { return fragment; }
    public List<InstanceDataInfo> getInstances() {
        return Collections.unmodifiableList(instances);
    }
    public AlarmDataInfo getAlarmData() { return alarmData; }
    public List<Invitation> getInvites() {
        return Collections.unmodifiableList(invites);
    }
    public List<CalReply> getReplies() {
        return Collections.unmodifiableList(replies);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("sortField", sortField)
            .add("date", date)
            .add("contentMatched", contentMatched)
            .add("nextAlarm", nextAlarm)
            .add("organizer", organizer)
            .add("categories", categories)
            .add("geo", geo)
            .add("fragment", fragment)
            .add("instances", instances)
            .add("alarmData", alarmData)
            .add("invites", invites)
            .add("replies", replies);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
