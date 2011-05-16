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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.KeyValuePair;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"id", "part", "sentDate", "emails", "subject",
                "msgIdHeader", "invite", "headers", "contentElems"})
public class InviteAsMP extends MessageCommon {

    @XmlAttribute(name=MailConstants.A_ID, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_PART, required=false)
    private String part;

    @XmlAttribute(name=MailConstants.A_SENT_DATE, required=false)
    private Long sentDate;

    @XmlElement(name=MailConstants.E_EMAIL, required=false)
    private List<EmailInfo> emails = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_SUBJECT, required=false)
    private String subject;

    @XmlElement(name=MailConstants.E_MSG_ID_HDR, required=false)
    private String msgIdHeader;

    @XmlElement(name=MailConstants.E_INVITE, required=false)
    private MPInviteInfo invite;

    @XmlElement(name=MailConstants.A_HEADER, required=false)
    private List<KeyValuePair> headers = Lists.newArrayList();

    @XmlElements({
        @XmlElement(name=MailConstants.E_MIMEPART,
            type=PartInfo.class),
        @XmlElement(name=MailConstants.E_SHARE_NOTIFICATION,
            type=ShareNotification.class)
    })
    private List<Object> contentElems = Lists.newArrayList();

    public InviteAsMP() {
    }

    public void setId(String id) { this.id = id; }
    public void setPart(String part) { this.part = part; }
    public void setSentDate(Long sentDate) { this.sentDate = sentDate; }
    public void setEmails(Iterable <EmailInfo> emails) {
        this.emails.clear();
        if (emails != null) {
            Iterables.addAll(this.emails,emails);
        }
    }

    public InviteAsMP addEmail(EmailInfo email) {
        this.emails.add(email);
        return this;
    }

    public void setSubject(String subject) { this.subject = subject; }
    public void setMsgIdHeader(String msgIdHeader) {
        this.msgIdHeader = msgIdHeader;
    }
    public void setInvite(MPInviteInfo invite) { this.invite = invite; }
    public void setHeaders(Iterable <KeyValuePair> headers) {
        this.headers.clear();
        if (headers != null) {
            Iterables.addAll(this.headers,headers);
        }
    }

    public InviteAsMP addHeader(KeyValuePair header) {
        this.headers.add(header);
        return this;
    }

    public void setContentElems(Iterable <Object> contentElems) {
        this.contentElems.clear();
        if (contentElems != null) {
            Iterables.addAll(this.contentElems,contentElems);
        }
    }

    public InviteAsMP addContentElem(Object contentElem) {
        this.contentElems.add(contentElem);
        return this;
    }

    public String getId() { return id; }
    public String getPart() { return part; }
    public Long getSentDate() { return sentDate; }
    public List<EmailInfo> getEmails() {
        return Collections.unmodifiableList(emails);
    }
    public String getSubject() { return subject; }
    public String getMsgIdHeader() { return msgIdHeader; }
    public MPInviteInfo getInvite() { return invite; }
    public List<KeyValuePair> getHeaders() {
        return Collections.unmodifiableList(headers);
    }
    public List<Object> getContentElems() {
        return Collections.unmodifiableList(contentElems);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("part", part)
            .add("sentDate", sentDate)
            .add("emails", emails)
            .add("subject", subject)
            .add("msgIdHeader", msgIdHeader)
            .add("invite", invite)
            .add("headers", headers)
            .add("contentElems", contentElems);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
