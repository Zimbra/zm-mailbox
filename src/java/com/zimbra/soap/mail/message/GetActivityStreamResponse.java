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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.OctopusXmlConstants;
import com.zimbra.soap.mail.type.ActivityInfo;
import com.zimbra.soap.mail.type.IdEmailName;
import com.zimbra.soap.type.NamedElement;

/**
 * @zm-api-response-description The response has three sections: user, op and activity.
 * <br />
 * User and op sections lists all the users and activities logged in the current stream to allow the client to show
 * filter UI if the user wants to see activities from specific users or specific operation.
 * <br />
 * Activity section contains the who, what and when.  Arg element contains extra data for some of the operations.  e.g.
 * <pre>
 * AddDocumentRevision
 *   ver - old version
 *   filename - name of the Document
 * MoveItem
 *   oldName
 *   filename - name of the item
 * RenameItem
 *   oldName
 *   filename - name of the item
 * GrantAccess
 *   target - name of the grantee, or
 *            "Authuser" for all authenticated users or
 *            "Public" for everyone
 *   role - "Read", "RW", or "Admin"
 *   filename - name of the item
 * CreateComment
 *   parentId
 *   text
 *   filename - name of the parent item
 * CreateMessage - for share notification
 *   from - the email address of the user who created the share
 *   subject - subject of the share notification
 *   sharedItemId - itemId of the shared item
 *   sharedItemName - name of the shared item
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=OctopusXmlConstants.E_GET_ACTIVITY_STREAM_RESPONSE)
@XmlType(propOrder = {"operations", "users", "activities"})
public class GetActivityStreamResponse {

    /**
     * @zm-api-field-tag session-id
     * @zm-api-field-description Session ID
     */
    @XmlAttribute(name=MailConstants.A_SESSION /* session */, required=false)
    private String sessionId;

    /**
     * @zm-api-field-description Operations
     */
    @XmlElement(name=OctopusXmlConstants.E_OPERATION /* op */, required=false)
    private List<NamedElement> operations = Lists.newArrayList();

    /**
     * @zm-api-field-description Users
     */
    @XmlElement(name=MailConstants.A_USER /* user */, required=false)
    private List<IdEmailName> users = Lists.newArrayList();

    /**
     * @zm-api-field-description Activities
     */
    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private List<ActivityInfo> activities = Lists.newArrayList();

    public GetActivityStreamResponse() {
    }

    public void setSession(String sessionId) {
        this.sessionId = sessionId;
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

    public String getSession() {
        return sessionId;
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
            .add("session", sessionId)
            .add("operations", operations)
            .add("users", users)
            .add("activities", activities);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
