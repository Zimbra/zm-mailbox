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

package com.zimbra.soap.mail.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
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
     * @zm-api-field-description Count
     */
    @XmlAttribute(name=MailConstants.A_COUNT, required=false)
    private String count;

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

    public GetActivityStreamResponse() {
    }

    public void setSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setCount(String count) {
        this.count = count;
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
    public String getCount() {
        return count;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("session", sessionId)
            .add("count", count)
            .add("operations", operations)
            .add("users", users)
            .add("activities", activities);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
