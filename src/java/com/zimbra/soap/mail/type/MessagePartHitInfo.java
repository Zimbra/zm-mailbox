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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.SearchHit;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class MessagePartHitInfo implements SearchHit {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_SORT_FIELD /* sf */, required=false)
    private String sortField;

    @XmlAttribute(name=MailConstants.A_SIZE /* s */, required=false)
    private Long size;

    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    @XmlAttribute(name=MailConstants.A_CONV_ID /* cid */, required=false)
    private Integer conversationId;

    @XmlAttribute(name=MailConstants.A_MESSAGE_ID /* mid */, required=false)
    private Integer messageId;

    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=false)
    private String contentType;

    @XmlAttribute(name=MailConstants.A_CONTENT_NAME /* name */, required=false)
    private String contentName;

    @XmlAttribute(name=MailConstants.A_PART /* part */, required=false)
    private String part;

    @XmlElement(name=MailConstants.E_EMAIL /* e */, required=false)
    private EmailInfo email;

    @XmlElement(name=MailConstants.E_SUBJECT /* su */, required=false)
    private String subject;

    public MessagePartHitInfo() {
    }

    public void setId(String id) { this.id = id; }
    public void setSortField(String sortField) { this.sortField = sortField; }
    public void setSize(Long size) { this.size = size; }
    public void setDate(Long date) { this.date = date; }
    public void setConversationId(Integer conversationId) {
        this.conversationId = conversationId;
    }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public void setContentName(String contentName) {
        this.contentName = contentName;
    }
    public void setPart(String part) { this.part = part; }
    public void setEmail(EmailInfo email) { this.email = email; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getId() { return id; }
    public String getSortField() { return sortField; }
    public Long getSize() { return size; }
    public Long getDate() { return date; }
    public Integer getConversationId() { return conversationId; }
    public Integer getMessageId() { return messageId; }
    public String getContentType() { return contentType; }
    public String getContentName() { return contentName; }
    public String getPart() { return part; }
    public EmailInfo getEmail() { return email; }
    public String getSubject() { return subject; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("sortField", sortField)
            .add("size", size)
            .add("date", date)
            .add("conversationId", conversationId)
            .add("messageId", messageId)
            .add("contentType", contentType)
            .add("contentName", contentName)
            .add("part", part)
            .add("email", email)
            .add("subject", subject);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
