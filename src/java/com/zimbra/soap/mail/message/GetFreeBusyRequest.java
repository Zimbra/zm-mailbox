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

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.FreeBusyUserSpec;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_GET_FREE_BUSY_REQUEST)
public class GetFreeBusyRequest {

    @XmlAttribute(name=MailConstants.A_CAL_START_TIME /* s */, required=true)
    private final long startTime;

    @XmlAttribute(name=MailConstants.A_CAL_END_TIME /* e */, required=true)
    private final long endTime;

    @XmlAttribute(name=MailConstants.A_UID /* uid */, required=false)
    private String uid;

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    @XmlAttribute(name=MailConstants.A_APPT_FREEBUSY_EXCLUDE_UID /* excludeUid */,
            required=false)
    private String excludeUid;

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
    public String getUid() { return uid; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getExcludeUid() { return excludeUid; }
    public List<FreeBusyUserSpec> getFreebusyUsers() {
        return Collections.unmodifiableList(freebusyUsers);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
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
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
