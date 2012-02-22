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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"content", "inviteComponent", "timezones", "mimeParts", "attachments"})
public class InvitationInfo extends InviteComponent {

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag content-type
     * @zm-api-field-description MIME Content-Type
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=false)
    private String contentType;

    /**
     * @zm-api-field-tag content-id
     * @zm-api-field-description MIME Content-Id
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_ID /* ci */, required=false)
    private String contentId;

    /**
     * @zm-api-field-tag raw-rfc822-msg
     * @zm-api-field-description RAW RFC822 MESSAGE (XML-encoded) <b>MUST CONTAIN A text/calendar PART</b>
     */
    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private RawInvite content;

    /**
     * @zm-api-field-description Invite component
     */
    @XmlElement(name=MailConstants.E_INVITE_COMPONENT /* comp */, required=false)
    private InviteComponent inviteComponent;

    /**
     * @zm-api-field-description Timezones
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private List<CalTZInfo> timezones = Lists.newArrayList();

    /**
     * @zm-api-field-description Meeting notes parts
     */
    @XmlElement(name=MailConstants.E_MIMEPART /* mp */, required=false)
    private List<MimePartInfo> mimeParts = Lists.newArrayList();

    /**
     * @zm-api-field-description Attachments
     */
    @XmlElement(name=MailConstants.E_ATTACH /* attach */, required=false)
    private AttachmentsInfo attachments;

    public InvitationInfo() {
        this((String) null, -1, false);
    }

    public InvitationInfo(String method, int componentNum, boolean rsvp) {
        super(method, componentNum, rsvp);
    }

    public void setId(String id) { this.id = id; }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }
    public void setContent(RawInvite content) { this.content = content; }
    public void setInviteComponent(InviteComponent inviteComponent) {
        this.inviteComponent = inviteComponent;
    }
    public void setTimezones(Iterable <CalTZInfo> timezones) {
        this.timezones.clear();
        if (timezones != null) {
            Iterables.addAll(this.timezones,timezones);
        }
    }

    public void addTimezone(CalTZInfo timezone) {
        this.timezones.add(timezone);
    }

    public void setMimeParts(Iterable <MimePartInfo> mimeParts) {
        this.mimeParts.clear();
        if (mimeParts != null) {
            Iterables.addAll(this.mimeParts,mimeParts);
        }
    }

    public void addMimePart(MimePartInfo mimePart) {
        this.mimeParts.add(mimePart);
    }

    public void setAttachments(AttachmentsInfo attachments) {
        this.attachments = attachments;
    }
    public String getId() { return id; }
    public String getContentType() { return contentType; }
    public String getContentId() { return contentId; }
    public RawInvite getContent() { return content; }
    public InviteComponent getInviteComponent() { return inviteComponent; }
    public List<CalTZInfo> getTimezones() {
        return Collections.unmodifiableList(timezones);
    }
    public List<MimePartInfo> getMimeParts() {
        return Collections.unmodifiableList(mimeParts);
    }
    public AttachmentsInfo getAttachments() { return attachments; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("contentType", contentType)
            .add("contentId", contentId)
            .add("content", content)
            .add("inviteComponent", inviteComponent)
            .add("timezones", timezones)
            .add("mimeParts", mimeParts)
            .add("attachments", attachments);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
