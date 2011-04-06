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
@XmlType(propOrder = { "metadatas", "emails", "subject", "fragment", "invite" })
public class MessageSummaryInfo {

    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_DATE, required=false)
    private Long date;

    @XmlAttribute(name=MailConstants.A_SIZE, required=false)
    private Long size;

    @XmlAttribute(name=MailConstants.A_FOLDER, required=false)
    private String folder;

    @XmlAttribute(name=MailConstants.A_CONV_ID, required=false)
    private String conversationId;

    @XmlAttribute(name=MailConstants.A_FLAGS, required=false)
    private String flags;

    @XmlAttribute(name=MailConstants.A_TAGS, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_REVISION, required=false)
    private Integer revision;

    @XmlAttribute(name=MailConstants.A_CHANGE_DATE, required=false)
    private Long changeDate;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE, required=false)
    private Integer modifiedSequence;

    @XmlAttribute(name=MailConstants.A_AUTO_SEND_TIME, required=false)
    private Long draftAutoSendTime;

    @XmlElement(name=MailConstants.E_METADATA, required=false)
    private CustomMetadata metadatas;

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

    public void setDate(Long date) { this.date = date; }
    public void setSize(Long size) { this.size = size; }
    public void setFolder(String folder) { this.folder = folder; }
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    public void setFlags(String flags) { this.flags = flags; }
    public void setTags(String tags) { this.tags = tags; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setDraftAutoSendTime(Long draftAutoSendTime) {
        this.draftAutoSendTime = draftAutoSendTime;
    }
    public void setMetadatas(CustomMetadata metadatas) {
        this.metadatas = metadatas;
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
    public Long getDate() { return date; }
    public Long getSize() { return size; }
    public String getFolder() { return folder; }
    public String getConversationId() { return conversationId; }
    public String getFlags() { return flags; }
    public String getTags() { return tags; }
    public Integer getRevision() { return revision; }
    public Long getChangeDate() { return changeDate; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public Long getDraftAutoSendTime() { return draftAutoSendTime; }
    public CustomMetadata getMetadatas() { return metadatas; }
    public List<EmailInfo> getEmails() {
        return Collections.unmodifiableList(emails);
    }
    public String getSubject() { return subject; }
    public String getFragment() { return fragment; }
    public InviteInfo getInvite() { return invite; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("id", id)
            .add("date", date)
            .add("size", size)
            .add("folder", folder)
            .add("conversationId", conversationId)
            .add("flags", flags)
            .add("tags", tags)
            .add("revision", revision)
            .add("changeDate", changeDate)
            .add("modifiedSequence", modifiedSequence)
            .add("draftAutoSendTime", draftAutoSendTime)
            .add("metadatas", metadatas)
            .add("emails", emails)
            .add("subject", subject)
            .add("fragment", fragment)
            .add("invite", invite)
            .toString();
    }
}
