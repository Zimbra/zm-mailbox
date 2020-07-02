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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.FreeBusyUserSpec;

/**
 * @zm-api-command-auth-required false - Freebusy information considered public if available
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Get Free/Busy information.
 * <br />
 * For accounts listed using uid,id or name attributes, f/b search will be done for all calendar folders.
 * <br />
 * To view free/busy for a single folder in a particular account, use <b>&lt;usr></b>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_FREE_BUSY_REQUEST)
public class GetFreeBusyRequest {

    /**
     * @zm-api-field-tag range-start-millis-gmt
     * @zm-api-field-description Range start in milliseconds
     */
    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=true)
    private final long startTime;

    /**
     * @zm-api-field-tag range-end-millis-gmt
     * @zm-api-field-description Range end in milliseconds
     */
    @XmlAttribute(name=MailConstants.A_CAL_END_TIME /* e */, required=true)
    private final long endTime;

    /**
     * @zm-api-field-tag comma-sep-zimbraId-or-email
     * @zm-api-field-description <b>DEPRECATED</b>.  Comma-separated list of Zimbra IDs or emails.  Each value can be
     * a Ziimbra ID or an email.
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_UID /* uid */, required=false)
    private String uid;

    /**
     * @zm-api-field-tag comma-sep-id
     * @zm-api-field-description Comma separated list of Zimbra IDs
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag comma-sep-emails
     * @zm-api-field-description Comma separated list of Emails
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag exclude-uid
     * @zm-api-field-description UID of appointment to exclude from free/busy search
     */
    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY_EXCLUDE_UID /* excludeUid */, required=false)
    private String excludeUid;

    /**
     * @zm-api-field-description To view free/busy for a single folders in particular accounts, use these.
     */
    @XmlElement(name=MailConstants.E_FREEBUSY_USER /* usr */, required=false)
    private List<FreeBusyUserSpec> freebusyUsers = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetFreeBusyRequest() {
        this(-1L, -1L);
    }

    public GetFreeBusyRequest(long startTime, long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Deprecated
    public void setUid(String uid) { this.uid = uid; }
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setExcludeUid(String excludeUid) {
        this.excludeUid = excludeUid;
    }
    public void setFreebusyUsers(Iterable <FreeBusyUserSpec> freebusyUsers) {
        this.freebusyUsers.clear();
        if (freebusyUsers != null) {
            Iterables.addAll(this.freebusyUsers,freebusyUsers);
        }
    }

    public GetFreeBusyRequest addFreebusyUser(FreeBusyUserSpec freebusyUser) {
        this.freebusyUsers.add(freebusyUser);
        return this;
    }

    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    @Deprecated
    public String getUid() { return uid; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getExcludeUid() { return excludeUid; }
    public List<FreeBusyUserSpec> getFreebusyUsers() {
        return Collections.unmodifiableList(freebusyUsers);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("startTime", startTime)
            .add("endTime", endTime)
            .add("uid", uid)
            .add("id", id)
            .add("name", name)
            .add("excludeUid", excludeUid)
            .add("freebusyUsers", freebusyUsers);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
