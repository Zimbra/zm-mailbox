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

// see mail.ToXML.encodeMessageSummary

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"emails", "subject", "fragment", "invite"})
public class MessageSummary extends MessageCommon {

    /**
     * @zm-api-field-tag message-id
     * @zm-api-field-description Message ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag auto-send-time
     * @zm-api-field-description Auto send time
     */
    @XmlAttribute(name=MailConstants.A_AUTO_SEND_TIME /* autoSendTime */, required=false)
    private Long autoSendTime;

    /**
     * @zm-api-field-description Email address information
     */
    @XmlElement(name=MailConstants.E_EMAIL /* e */, required=false)
    private List<EmailInfo> emails = Lists.newArrayList();

    /**
     * @zm-api-field-tag subject
     * @zm-api-field-description Subject
     */
    @XmlElement(name=MailConstants.E_SUBJECT /* su */, required=false)
    private String subject;

    /**
     * @zm-api-field-tag fragment
     * @zm-api-field-description First few bytes of the message (probably between 40 and 100 bytes)
     */
    @XmlElement(name=MailConstants.E_FRAG /* fr */, required=false)
    private String fragment;

    /**
     * @zm-api-field-description Invite information
     */
    @XmlElement(name=MailConstants.E_INVITE /* inv */, required=false)
    private InviteInfo invite;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MessageSummary() {
        this((String) null);
    }

    public MessageSummary(String id) {
        this.id = id;
    }

    public void setAutoSendTime(Long autoSendTime) {
        this.autoSendTime = autoSendTime;
    }

    public void setEmails(Iterable <EmailInfo> emails) {
        this.emails.clear();
        if (emails != null) {
            Iterables.addAll(this.emails,emails);
        }
    }

    public MessageSummary addEmail(EmailInfo email) {
        this.emails.add(email);
        return this;
    }

    public void setSubject(String subject) { this.subject = subject; }
    public void setFragment(String fragment) { this.fragment = fragment; }
    public void setInvite(InviteInfo invite) { this.invite = invite; }
    public String getId() { return id; }
    public Long getAutoSendTime() { return autoSendTime; }
    public List<EmailInfo> getEmails() {
        return Collections.unmodifiableList(emails);
    }
    public String getSubject() { return subject; }
    public String getFragment() { return fragment; }
    public InviteInfo getInvite() { return invite; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("autoSendTime", autoSendTime)
            .add("emails", emails)
            .add("subject", subject)
            .add("fragment", fragment)
            .add("invite", invite);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
