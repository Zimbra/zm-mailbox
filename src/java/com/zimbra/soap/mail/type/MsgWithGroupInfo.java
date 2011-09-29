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
import javax.xml.bind.annotation.XmlElements;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.soap.type.UrlAndValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class MsgWithGroupInfo extends MessageCommon {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_CAL_INTENDED_FOR /* cif */,
                    required=false)
    private String calendarIntendedFor;

    @XmlAttribute(name=MailConstants.A_ORIG_ID /* origid */, required=false)
    private String origId;

    @XmlAttribute(name=MailConstants.A_REPLY_TYPE /* rt */, required=false)
    private String draftReplyType;

    @XmlAttribute(name=MailConstants.A_IDENTITY_ID /* idnt */, required=false)
    private String identityId;

    @XmlAttribute(name=MailConstants.A_FOR_ACCOUNT /* forAcct */,
                    required=false)
    private String draftAccountId;

    @XmlAttribute(name=MailConstants.A_AUTO_SEND_TIME /* autoSendTime */,
                    required=false)
    private Long draftAutoSendTime;

    @XmlAttribute(name=MailConstants.A_SENT_DATE /* sd */, required=false)
    private Long sentDate;

    @XmlAttribute(name=MailConstants.A_RESENT_DATE /* rd */, required=false)
    private Long resentDate;

    @XmlAttribute(name=MailConstants.A_PART /* part */, required=false)
    private String part;

    @XmlElement(name=MailConstants.E_FRAG /* fr */, required=false)
    private String fragment;

    @XmlElement(name=MailConstants.E_EMAIL /* e */, required=false)
    private List<EmailInfo> emails = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_SUBJECT /* su */, required=false)
    private String subject;

    @XmlElement(name=MailConstants.E_MSG_ID_HDR /* mid */, required=false)
    private String messageIdHeader;

    @XmlElement(name=MailConstants.E_IN_REPLY_TO /* irt */, required=false)
    private String inReplyTo;

    @XmlElement(name=MailConstants.E_INVITE /* inv */, required=false)
    private InviteWithGroupInfo invite;

    @XmlElement(name=MailConstants.A_HEADER /* header */, required=false)
    private List<KeyValuePair> headers = Lists.newArrayList();

    @XmlElements({
        @XmlElement(name=MailConstants.E_MIMEPART /* mp */,
            type=PartInfo.class),
        @XmlElement(name=MailConstants.E_SHARE_NOTIFICATION /* shr */,
            type=ShareNotification.class),
        @XmlElement(name=MailConstants.E_DL_SUBSCRIPTION_NOTIFICATION /* dlSubs */,
            type=DLSubscriptionNotification.class)              
    })
    private List<Object> contentElems = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private UrlAndValue content;

    public MsgWithGroupInfo() {
    }

    public void setId(String id) { this.id = id; }
    public void setCalendarIntendedFor(String calendarIntendedFor) {
        this.calendarIntendedFor = calendarIntendedFor;
    }
    public void setOrigId(String origId) { this.origId = origId; }
    public void setDraftReplyType(String draftReplyType) {
        this.draftReplyType = draftReplyType;
    }
    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }
    public void setDraftAccountId(String draftAccountId) {
        this.draftAccountId = draftAccountId;
    }
    public void setDraftAutoSendTime(Long draftAutoSendTime) {
        this.draftAutoSendTime = draftAutoSendTime;
    }
    public void setSentDate(Long sentDate) { this.sentDate = sentDate; }
    public void setResentDate(Long resentDate) { this.resentDate = resentDate; }
    public void setPart(String part) { this.part = part; }
    public void setFragment(String fragment) { this.fragment = fragment; }
    public void setEmails(Iterable <EmailInfo> emails) {
        this.emails.clear();
        if (emails != null) {
            Iterables.addAll(this.emails,emails);
        }
    }

    public void addEmail(EmailInfo email) {
        this.emails.add(email);
    }

    public void setSubject(String subject) { this.subject = subject; }
    public void setMessageIdHeader(String messageIdHeader) {
        this.messageIdHeader = messageIdHeader;
    }
    public void setInReplyTo(String inReplyTo) { this.inReplyTo = inReplyTo; }
    public void setInvite(InviteWithGroupInfo invite) { this.invite = invite; }
    public void setHeaders(Iterable <KeyValuePair> headers) {
        this.headers.clear();
        if (headers != null) {
            Iterables.addAll(this.headers,headers);
        }
    }

    public void addHeader(KeyValuePair header) {
        this.headers.add(header);
    }

    public void setContentElems(Iterable <Object> contentElems) {
        this.contentElems.clear();
        if (contentElems != null) {
            Iterables.addAll(this.contentElems,contentElems);
        }
    }

    public void addContentElem(Object contentElem) {
        this.contentElems.add(contentElem);
    }

    public void setContent(UrlAndValue content) { this.content = content; }
    public String getId() { return id; }
    public String getCalendarIntendedFor() { return calendarIntendedFor; }
    public String getOrigId() { return origId; }
    public String getDraftReplyType() { return draftReplyType; }
    public String getIdentityId() { return identityId; }
    public String getDraftAccountId() { return draftAccountId; }
    public Long getDraftAutoSendTime() { return draftAutoSendTime; }
    public Long getSentDate() { return sentDate; }
    public Long getResentDate() { return resentDate; }
    public String getPart() { return part; }
    public String getFragment() { return fragment; }
    public List<EmailInfo> getEmails() {
        return Collections.unmodifiableList(emails);
    }
    public String getSubject() { return subject; }
    public String getMessageIdHeader() { return messageIdHeader; }
    public String getInReplyTo() { return inReplyTo; }
    public InviteWithGroupInfo getInvite() { return invite; }
    public List<KeyValuePair> getHeaders() {
        return Collections.unmodifiableList(headers);
    }
    public List<Object> getContentElems() {
        return Collections.unmodifiableList(contentElems);
    }
    public UrlAndValue getContent() { return content; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("calendarIntendedFor", calendarIntendedFor)
            .add("origId", origId)
            .add("draftReplyType", draftReplyType)
            .add("identityId", identityId)
            .add("draftAccountId", draftAccountId)
            .add("draftAutoSendTime", draftAutoSendTime)
            .add("sentDate", sentDate)
            .add("resentDate", resentDate)
            .add("part", part)
            .add("fragment", fragment)
            .add("emails", emails)
            .add("subject", subject)
            .add("messageIdHeader", messageIdHeader)
            .add("inReplyTo", inReplyTo)
            .add("invite", invite)
            .add("headers", headers)
            .add("contentElems", contentElems)
            .add("content", content);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
