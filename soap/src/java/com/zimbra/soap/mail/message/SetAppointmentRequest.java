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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalReply;
import com.zimbra.soap.mail.type.SetCalendarItemInfo;
import com.zimbra.soap.type.ZmBoolean;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

//   TODO: need way to link message to appointment after the fact
/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Directly set status of an entire appointment.  This API is intended for mailbox
 * Migration (ie migrating a mailbox onto this server) and is not used by normal mail clients.
 * <br />
 * <br />
 * Need to specify folder for appointment
 * <br />
 * <br />
 * Need way to add message WITHOUT processing it for calendar parts.
 * Need to generate and patch-in the iCalendar for the <b>&lt;inv></b> but w/o actually processing the
 * <b>&lt;inv></b> as a new request
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SET_APPOINTMENT_REQUEST)
public class SetAppointmentRequest {

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-tag tags
     * @zm-api-field-description Tags (Deprecated - use <b>{tag-names}</b> instead)
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Comma separated list of tag names
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description ID of folder to create appointment in
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag no-next-alarm
     * @zm-api-field-description Set if all alarms have been dismissed; if this is set, nextAlarm should not be set
     */
    @XmlAttribute(name=MailConstants.A_CAL_NO_NEXT_ALARM /* noNextAlarm */, required=false)
    private ZmBoolean noNextAlarm;

    /**
     * @zm-api-field-tag next-alarm-will-go-off-at
     * @zm-api-field-description If specified, time when next alarm should go off.
     * <br />
     * If missing, two possibilities:
     * <ol>
     * <li> if noNextAlarm isn't set, keep current next alarm time (this is a backward compatibility case)
     * <li> if noNextAlarm is set, indicates all alarms have been dismissed
     * </ol>
     */
    @XmlAttribute(name=MailConstants.A_CAL_NEXT_ALARM /* nextAlarm */, required=false)
    private Long nextAlarm;

    /**
     * @zm-api-field-description Default calendar item information
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.A_DEFAULT /* default */, required=false)
    private SetCalendarItemInfo defaultId;

    /**
     * @zm-api-field-description Calendar item information for exceptions
     */
    @XmlElement(name=MailConstants.E_CAL_EXCEPT /* except */, required=false)
    private List<SetCalendarItemInfo> exceptions = Lists.newArrayList();

    /**
     * @zm-api-field-description Calendar item information for cancellations
     */
    @XmlElement(name=MailConstants.E_CAL_CANCEL /* cancel */, required=false)
    private List<SetCalendarItemInfo> cancellations = Lists.newArrayList();

    /**
     * @zm-api-field-description List of replies received from attendees.  If SetAppointmenRequest doesn't contain
     * a <b>&lt;replies></b> block, existing replies will be kept.  If <b>&lt;replies/></b> element is provided with
     * no <b>&lt;reply></b> elements inside, existing replies will be removed, replaced with an empty set.
     * If <b>&lt;replies></b> contains one or more <b>&lt;reply></b> elements, existing replies are replaced with the
     * ones provided.
     */
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
        this.noNextAlarm = ZmBoolean.fromBool(noNextAlarm);
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
    public Boolean getNoNextAlarm() { return ZmBoolean.toBool(noNextAlarm); }
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
