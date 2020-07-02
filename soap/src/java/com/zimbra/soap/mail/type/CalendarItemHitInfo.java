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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.SearchHit;
import com.zimbra.soap.type.ZmBoolean;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"organizer", "categories", "geo", "fragment",
                    "instances", "alarmData", "invites", "replies"})
public abstract class CalendarItemHitInfo
    extends CommonCalendaringData
    implements SearchHit {

    /**
     * @zm-api-field-tag sort-field-value
     * @zm-api-field-description Sort field value
     */
    @XmlAttribute(name=MailConstants.A_SORT_FIELD /* sf */, required=false)
    private String sortField;

    /**
     * @zm-api-field-tag date
     * @zm-api-field-description Date
     */
    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private long date;

    /**
     * @zm-api-field-tag content-matched
     * @zm-api-field-description Set if the message matched the specified query string
     */
    @XmlAttribute(name=MailConstants.A_CONTENTMATCHED /* cm */, required=false)
    private ZmBoolean contentMatched;

    /**
     * @zm-api-field-tag next-alarm-millis
     * @zm-api-field-description Time in millis to show the alarm
     */
    @XmlAttribute(name=MailConstants.A_CAL_NEXT_ALARM /* nextAlarm */, required=false)
    private Long nextAlarm;

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
     * @zm-api-field-description Data for instances
     */
    @XmlElement(name=MailConstants.E_INSTANCE /* inst */, required=false)
    private List<InstanceDataInfo> instances = Lists.newArrayList();

    /**
     * @zm-api-field-description Alarm information
     */
    @XmlElement(name=MailConstants.E_CAL_ALARM_DATA /* alarmData */, required=false)
    private AlarmDataInfo alarmData;

    /**
     * @zm-api-field-description Invites
     */
    @XmlElement(name=MailConstants.E_INVITE /* inv */, required=false)
    private List<Invitation> invites = Lists.newArrayList();

    /**
     * @zm-api-field-description Replies
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=MailConstants.E_CAL_REPLIES /* replies */, required=false)
    @XmlElement(name=MailConstants.E_CAL_REPLY /* reply */, required=false)
    private List<CalReply> replies = Lists.newArrayList();

    public CalendarItemHitInfo() {
    }

    @Override
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

    @Override
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

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
