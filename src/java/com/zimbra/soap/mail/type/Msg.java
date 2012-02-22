/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"content", "headers", "mimePart", "attachments", "invite",
           "emailAddresses", "timezones", "fragment", "extraElements"})
public class Msg {

    /**
     * @zm-api-field-tag uploaded-MIME-body-ID
     * @zm-api-field-description Uploaded MIME body ID
     */
    @XmlAttribute(name=MailConstants.A_ATTACHMENT_ID /* aid */, required=false)
    private String attachmentId;

    /**
     * @zm-api-field-tag orig-id
     * @zm-api-field-description Original ID
     */
    @XmlAttribute(name=MailConstants.A_ORIG_ID /* origid */, required=false)
    private String origId;

    /**
     * @zm-api-field-tag reply-type-r|w
     * @zm-api-field-description Reply type - <b>r|w</b>.  (r)eplied or for(w)arded.
     */
    @XmlAttribute(name=MailConstants.A_REPLY_TYPE /* rt */, required=false)
    private String replyType;

    /**
     * @zm-api-field-tag identity-id
     * @zm-api-field-description Identity ID.  The identity referenced by <b>{identity-id}</b> specifies the folder
     * where the sent message is saved.
     */
    @XmlAttribute(name=MailConstants.A_IDENTITY_ID /* idnt */, required=false)
    private String identityId;

    /**
     * @zm-api-field-tag subject
     * @zm-api-field-description Subject
     */
    @XmlAttribute(name=MailConstants.E_SUBJECT /* su */, required=false)
    private String subject;

    /**
     * @zm-api-field-description Headers
     */
    @XmlElement(name=MailConstants.E_HEADER /* header */, required=false)
    private List<Header> headers;

    /**
     * @zm-api-field-tag in-reply-to-message-id-hdr
     * @zm-api-field-description Message-ID header for message being replied to
     */
    @XmlAttribute(name=MailConstants.E_IN_REPLY_TO /* irt */, required=false)
    private String inReplyTo;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-tag content
     * @zm-api-field-description Content
     */
    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private String content;

    /**
     * @zm-api-field-description Mime part information
     */
    @XmlElement(name=MailConstants.E_MIMEPART /* mp */, required=false)
    private MimePartInfo mimePart;

    /**
     * @zm-api-field-description Attachments information
     */
    @XmlElement(name=MailConstants.E_ATTACH /* attach */, required=false)
    private AttachmentsInfo attachments;

    /**
     * @zm-api-field-description Invite information
     */
    @XmlElement(name=MailConstants.E_INVITE /* inv */, required=false)
    private InvitationInfo invite;

    /**
     * @zm-api-field-description Email address information
     */
    @XmlElement(name=MailConstants.E_EMAIL /* e */, required=false)
    private List<EmailAddrInfo> emailAddresses = Lists.newArrayList();

    // ParseMimeMessage.parseMimeMsgSoap looks for E_CAL_TZ but does no further processing.
    // CalendarUtils.parseInviteElementCommon looks for timezones in parent element
    /**
     * @zm-api-field-description Timezones
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private List<CalTZInfo> timezones = Lists.newArrayList();

    // ParseMimeMessage.parseMimeMsgSoap looks for E_FRAG but does nothing with it.
    /**
     * @zm-api-field-tag fragment
     * @zm-api-field-description First few bytes of the message (probably between 40 and 100 bytes)
     */
    @XmlElement(name=MailConstants.E_FRAG /* fr */, required=false)
    private String fragment;

    // ParseMimeMessage.parseMimeMsgSoap loops over all children.
    /**
     * @zm-api-field-description Other elements
     */
    @XmlAnyElement
    private List<org.w3c.dom.Element> extraElements = Lists.newArrayList();

    public Msg() {
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }
    public void setOrigId(String origId) { this.origId = origId; }
    public void setReplyType(String replyType) { this.replyType = replyType; }
    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }
    public void setSubject(String subject) { this.subject = subject; }
    public void setHeaders(List<Header> headers) { this.headers = headers; }
    public void setInReplyTo(String inReplyTo) { this.inReplyTo = inReplyTo; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setFlags(String flags) { this.flags = flags; }
    public void setContent(String content) { this.content = content; }
    public void setMimePart(MimePartInfo mimePart) { this.mimePart = mimePart; }
    public void setAttachments(AttachmentsInfo attachments) {
        this.attachments = attachments;
    }
    public void setInvite(InvitationInfo invite) { this.invite = invite; }
    public void setEmailAddresses(Iterable <EmailAddrInfo> emailAddresses) {
        this.emailAddresses.clear();
        if (emailAddresses != null) {
            Iterables.addAll(this.emailAddresses,emailAddresses);
        }
    }

    public void addEmailAddresse(EmailAddrInfo emailAddresse) {
        this.emailAddresses.add(emailAddresse);
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

    public void setFragment(String fragment) { this.fragment = fragment; }
    public void setExtraElements(Iterable <org.w3c.dom.Element> extraElements) {
        this.extraElements.clear();
        if (extraElements != null) {
            Iterables.addAll(this.extraElements,extraElements);
        }
    }

    public void addExtraElement(org.w3c.dom.Element extraElement) {
        this.extraElements.add(extraElement);
    }

    public String getAttachmentId() { return attachmentId; }
    public String getOrigId() { return origId; }
    public String getReplyType() { return replyType; }
    public String getIdentityId() { return identityId; }
    public String getSubject() { return subject; }
    public List<Header> getHeaders() { return headers; }
    public String getInReplyTo() { return inReplyTo; }
    public String getFolderId() { return folderId; }
    public String getFlags() { return flags; }
    public String getContent() { return content; }
    public MimePartInfo getMimePart() { return mimePart; }
    public AttachmentsInfo getAttachments() { return attachments; }
    public InvitationInfo getInvite() { return invite; }
    public List<EmailAddrInfo> getEmailAddresses() {
        return Collections.unmodifiableList(emailAddresses);
    }
    public List<CalTZInfo> getTimezones() {
        return Collections.unmodifiableList(timezones);
    }
    public String getFragment() { return fragment; }
    public List<org.w3c.dom.Element> getExtraElements() {
        return Collections.unmodifiableList(extraElements);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("attachmentId", attachmentId)
            .add("origId", origId)
            .add("replyType", replyType)
            .add("identityId", identityId)
            .add("subject", subject)
            .add("inReplyTo", inReplyTo)
            .add("folderId", folderId)
            .add("flags", flags)
            .add("content", content)
            .add("mimePart", mimePart)
            .add("attachments", attachments)
            .add("invite", invite)
            .add("emailAddresses", emailAddresses)
            .add("timezones", timezones)
            .add("fragment", fragment)
            .add("extraElements", extraElements);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }

    public static final class Header {
        /**
         * @zm-api-field-tag header-name
         * @zm-api-field-description Header name
         */
        @XmlAttribute(name=MailConstants.A_NAME)
        private String name;

        /**
         * @zm-api-field-tag header-value
         * @zm-api-field-description Header value
         */
        @XmlValue
        private String value;

        public Header() {
        }

        public Header(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }
}
