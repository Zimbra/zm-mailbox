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
import com.zimbra.soap.type.CustomMetadata;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "emails", "subject", "fragment", "invite" })
public class MessageSummaryInfo extends MessageCommon {

    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_AUTO_SEND_TIME, required=false)
    private Long draftAutoSendTime;

    @XmlElement(name=MailConstants.E_EMAIL, required=false)
    private List<EmailInfo> emails = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_SUBJECT, required=false)
    private String subject;

    @XmlElement(name=MailConstants.E_FRAG, required=false)
    private String fragment;

    @XmlElement(name=MailConstants.E_INVITE, required=false)
    private InviteInfo invite;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MessageSummaryInfo() {
        this((String) null);
    }

    public MessageSummaryInfo(String id) {
        this.id = id;
    }

    public void setDraftAutoSendTime(Long draftAutoSendTime) {
        this.draftAutoSendTime = draftAutoSendTime;
    }

    public void setEmails(Iterable <EmailInfo> emails) {
        this.emails.clear();
        if (emails != null) {
            Iterables.addAll(this.emails,emails);
        }
    }

    public MessageSummaryInfo addEmail(EmailInfo email) {
        this.emails.add(email);
        return this;
    }

    public void setSubject(String subject) { this.subject = subject; }
    public void setFragment(String fragment) { this.fragment = fragment; }
    public void setInvite(InviteInfo invite) { this.invite = invite; }
    public String getId() { return id; }
    public Long getDraftAutoSendTime() { return draftAutoSendTime; }
    public List<EmailInfo> getEmails() {
        return Collections.unmodifiableList(emails);
    }
    public String getSubject() { return subject; }
    public String getFragment() { return fragment; }
    public InviteInfo getInvite() { return invite; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("draftAutoSendTime", draftAutoSendTime)
            .add("emails", emails)
            .add("subject", subject)
            .add("fragment", fragment)
            .add("invite", invite);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
