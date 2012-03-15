/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.soap.mail.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.soap.mail.type.ActivityInfo;
import com.zimbra.soap.mail.type.IdEmailName;
import com.zimbra.soap.type.NamedElement;

/**
 * @zm-api-response-description the response contains activities
 * as described in GetActivityStreamResponse
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=OctopusXmlConstants.E_GET_NOTIFICATIONS_RESPONSE)
@XmlType(propOrder = {"operations", "users", "activities"})
public class GetNotificationsResponse {

    /**
     * @zm-api-field-tag last-seen
     * @zm-api-field-description Timestamp of when the notifications were last seen
     */
    @XmlAttribute(name=OctopusXmlConstants.A_LASTSEEN /* lastSeen */, required=true)
    private long lastSeen;

    /**
     * @zm-api-field-description Operations
     */
    @XmlElement(name=OctopusXmlConstants.E_OPERATION /* op */, required=false)
    private final List<NamedElement> operations = Lists.newArrayList();

    /**
     * @zm-api-field-description Users
     */
    @XmlElement(name=MailConstants.A_USER /* user */, required=false)
    private final List<IdEmailName> users = Lists.newArrayList();

    /**
     * @zm-api-field-description Activities
     */
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private final List<ActivityInfo> activities = Lists.newArrayList();

    public GetNotificationsResponse() {
    }

    public void setlastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public void setOperations(Iterable <NamedElement> operations) {
        this.operations.clear();
        if (operations != null) {
            Iterables.addAll(this.operations,operations);
        }
    }

    public void addOperation(NamedElement operation) {
        this.operations.add(operation);
    }

    public void setUsers(Iterable <IdEmailName> users) {
        this.users.clear();
        if (users != null) {
            Iterables.addAll(this.users,users);
        }
    }

    public void addUser(IdEmailName user) {
        this.users.add(user);
    }

    public void setActivities(Iterable <ActivityInfo> activities) {
        this.activities.clear();
        if (activities != null) {
            Iterables.addAll(this.activities,activities);
        }
    }

    public void addActivity(ActivityInfo activity) {
        this.activities.add(activity);
    }

    public long getLastSeen() {
        return lastSeen;
    }
    public List<NamedElement> getOperations() {
        return Collections.unmodifiableList(operations);
    }
    public List<IdEmailName> getUsers() {
        return Collections.unmodifiableList(users);
    }
    public List<ActivityInfo> getActivities() {
        return Collections.unmodifiableList(activities);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("lastSeen", lastSeen)
            .add("operations", operations)
            .add("users", users)
            .add("activities", activities);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
