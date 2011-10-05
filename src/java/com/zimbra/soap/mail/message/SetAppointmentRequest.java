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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalReply;
import com.zimbra.soap.mail.type.SetCalendarItemInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_SET_APPOINTMENT_REQUEST)
public class SetAppointmentRequest {

    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    @XmlAttribute(name=MailConstants.A_CAL_NO_NEXT_ALARM /* noNextAlarm */, required=false)
    private Boolean noNextAlarm;

    @XmlAttribute(name=MailConstants.A_CAL_NEXT_ALARM /* nextAlarm */, required=false)
    private Long nextAlarm;

    @XmlElement(name=MailConstants.A_DEFAULT /* default */, required=false)
    private SetCalendarItemInfo defaultId;

    @XmlElement(name=MailConstants.E_CAL_EXCEPT /* except */, required=false)
    private List<SetCalendarItemInfo> exceptions = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_CANCEL /* cancel */, required=false)
    private List<SetCalendarItemInfo> cancellations = Lists.newArrayList();

    @XmlElementWrapper(name=MailConstants.E_CAL_REPLIES /* replies */, required=false)
    @XmlElement(name=MailConstants.E_CAL_REPLY /* reply */, required=false)
    private List<CalReply> replies = Lists.newArrayList();

    public SetAppointmentRequest() {
    }

    public void setFlags(String flags) { this.flags = flags; }
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setNoNextAlarm(Boolean noNextAlarm) {
        this.noNextAlarm = noNextAlarm;
    }
    public void setNextAlarm(Long nextAlarm) { this.nextAlarm = nextAlarm; }
    public void setDefaultId(SetCalendarItemInfo defaultId) {
        this.defaultId = defaultId;
    }
    public void setExceptions(Iterable <SetCalendarItemInfo> exceptions) {
        this.exceptions.clear();
        if (exceptions != null) {
            Iterables.addAll(this.exceptions,exceptions);
        }
    }

    public void addException(SetCalendarItemInfo exception) {
        this.exceptions.add(exception);
    }

    public void setCancellations(Iterable <SetCalendarItemInfo> cancellations) {
        this.cancellations.clear();
        if (cancellations != null) {
            Iterables.addAll(this.cancellations,cancellations);
        }
    }

    public void addCancellation(SetCalendarItemInfo cancellation) {
        this.cancellations.add(cancellation);
    }

    public void setReplies(Iterable <CalReply> replies) {
        this.replies.clear();
        if (replies != null) {
            Iterables.addAll(this.replies,replies);
        }
    }

    public void addReply(CalReply reply) {
        this.replies.add(reply);
    }

    public String getFlags() { return flags; }
    @Deprecated
    public String getTags() { return tags; }
    public String getTagNames() { return tagNames; }
    public String getFolderId() { return folderId; }
    public Boolean getNoNextAlarm() { return noNextAlarm; }
    public Long getNextAlarm() { return nextAlarm; }
    public SetCalendarItemInfo getDefaultId() { return defaultId; }
    public List<SetCalendarItemInfo> getExceptions() {
        return Collections.unmodifiableList(exceptions);
    }
    public List<SetCalendarItemInfo> getCancellations() {
        return Collections.unmodifiableList(cancellations);
    }
    public List<CalReply> getReplies() {
        return Collections.unmodifiableList(replies);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("folderId", folderId)
            .add("noNextAlarm", noNextAlarm)
            .add("nextAlarm", nextAlarm)
            .add("defaultId", defaultId)
            .add("exceptions", exceptions)
            .add("cancellations", cancellations)
            .add("replies", replies);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
