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

package com.zimbra.soap.admin.type;

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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.EmailInfoInterface;
import com.zimbra.soap.base.InviteInfoInterface;
import com.zimbra.soap.base.MessageInfoInterface;
import com.zimbra.soap.type.KeyValuePair;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "fragment", "emails", "subject",
    "messageIdHeader", "inReplyTo", "invite", "headers", "contentElems" })
public class MessageInfo
extends MessageCommon
implements MessageInfoInterface {

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
    private InviteInfo invite;

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

    public MessageInfo() {
    }

    public MessageInfo(String id) {
        this.id = id;
    }

    @Override
    public MessageInfoInterface createFromId(String id) {
        return new MessageInfo(id);
    }

    @Override
    public void setId(String id) { this.id = id; }

    @Override
    public void setCalendarIntendedFor(String calendarIntendedFor) {
        this.calendarIntendedFor = calendarIntendedFor;
    }

    @Override
    public void setOrigId(String origId) { this.origId = origId; }
    @Override
    public void setDraftReplyType(String draftReplyType) {
        this.draftReplyType = draftReplyType;
    }

    @Override
    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    @Override
    public void setDraftAccountId(String draftAccountId) {
        this.draftAccountId = draftAccountId;
    }

    @Override
    public void setDraftAutoSendTime(Long draftAutoSendTime) {
        this.draftAutoSendTime = draftAutoSendTime;
    }

    @Override
    public void setSentDate(Long sentDate) { this.sentDate = sentDate; }
    @Override
    public void setResentDate(Long resentDate) {
        this.resentDate = resentDate;
    }

    @Override
    public void setPart(String part) { this.part = part; }
    @Override
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

    @Override
    public void setSubject(String subject) { this.subject = subject; }
    @Override
    public void setMessageIdHeader(String messageIdHeader) {
        this.messageIdHeader = messageIdHeader;
    }
    @Override
    public void setInReplyTo(String inReplyTo) { this.inReplyTo = inReplyTo; }
    public void setInvite(InviteInfo invite) { this.invite = invite; }
    @Override
    public void setHeaders(Iterable <KeyValuePair> headers) {
        this.headers.clear();
        if (headers != null) {
            Iterables.addAll(this.headers,headers);
        }
    }

    @Override
    public void addHeader(KeyValuePair header) {
        this.headers.add(header);
    }

    @Override
    public void setContentElems(Iterable <Object> contentElems) {
        this.contentElems.clear();
        if (contentElems != null) {
            Iterables.addAll(this.contentElems,contentElems);
        }
    }

    @Override
    public void addContentElem(Object contentElem) {
        this.contentElems.add(contentElem);
    }

    @Override
    public String getId() { return id; }
    @Override
    public String getCalendarIntendedFor() { return calendarIntendedFor; }
    @Override
    public String getOrigId() { return origId; }
    @Override
    public String getDraftReplyType() { return draftReplyType; }
    @Override
    public String getIdentityId() { return identityId; }
    @Override
    public String getDraftAccountId() { return draftAccountId; }
    @Override
    public Long getDraftAutoSendTime() { return draftAutoSendTime; }
    @Override
    public Long getSentDate() { return sentDate; }
    @Override
    public Long getResentDate() { return resentDate; }
    @Override
    public String getPart() { return part; }
    @Override
    public String getFragment() { return fragment; }
    public List<EmailInfo> getEmails() {
        return Collections.unmodifiableList(emails);
    }
    @Override
    public String getSubject() { return subject; }
    @Override
    public String getMessageIdHeader() { return messageIdHeader; }
    @Override
    public String getInReplyTo() { return inReplyTo; }
    public InviteInfo getInvite() { return invite; }
    @Override
    public List<KeyValuePair> getHeaders() {
        return Collections.unmodifiableList(headers);
    }
    @Override
    public List<Object> getContentElems() {
        return Collections.unmodifiableList(contentElems);
    }

    @Override
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
            .add("contentElems", contentElems);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
    @Override
    public void setEmailInterfaces(Iterable<EmailInfoInterface> emails) {
        setEmails(EmailInfo.fromInterfaces(emails));
    }

    @Override
    public void addEmailInterface(EmailInfoInterface email) {
        addEmail((EmailInfo) email);
    }

    @Override
    public void setInviteInterface(InviteInfoInterface invite) {
        setInvite((InviteInfo) invite);
    }

    @Override
    public List<EmailInfoInterface> getEmailInterfaces() {
        return EmailInfo.toInterfaces(emails);
    }

    @Override
    public InviteInfoInterface getInvitInterfacee() {
        return invite;
    }
}
