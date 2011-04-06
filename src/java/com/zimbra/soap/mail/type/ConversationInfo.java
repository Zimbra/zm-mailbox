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
import com.zimbra.soap.type.CustomMetadata;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "metadatas", "subject", "messages"})
public class ConversationInfo {

    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_NUM, required=false)
    private Integer num;

    @XmlAttribute(name=MailConstants.A_TOTAL_SIZE, required=false)
    private Integer totalSize;

    @XmlAttribute(name=MailConstants.A_FLAGS, required=false)
    private String flags;

    @XmlAttribute(name=MailConstants.A_TAGS, required=false)
    private String tags;

    @XmlElement(name=MailConstants.E_METADATA, required=true)
    private final CustomMetadata metadatas;

    @XmlElement(name=MailConstants.E_SUBJECT, required=false)
    private String subject;

    @XmlElements({

        @XmlElement(name=MailConstants.E_CHAT,
            type=ChatMessageInfo.class),
        @XmlElement(name=MailConstants.E_MSG,
            type=MessageInfo.class)
    })
    private List<MessageInfo> messages = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ConversationInfo() {
        this((String) null, (CustomMetadata) null);
    }

    public ConversationInfo(String id, CustomMetadata metadatas) {
        this.id = id;
        this.metadatas = metadatas;
    }

    public void setNum(Integer num) { this.num = num; }
    public void setTotalSize(Integer totalSize) { this.totalSize = totalSize; }
    public void setFlags(String flags) { this.flags = flags; }
    public void setTags(String tags) { this.tags = tags; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setMessages(Iterable <MessageInfo> messages) {
        this.messages.clear();
        if (messages != null) {
            Iterables.addAll(this.messages,messages);
        }
    }

    public ConversationInfo addMessag(MessageInfo messag) {
        this.messages.add(messag);
        return this;
    }

    public String getId() { return id; }
    public Integer getNum() { return num; }
    public Integer getTotalSize() { return totalSize; }
    public String getFlags() { return flags; }
    public String getTags() { return tags; }
    public CustomMetadata getMetadatas() { return metadatas; }
    public String getSubject() { return subject; }
    public List<MessageInfo> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("id", id)
            .add("num", num)
            .add("totalSize", totalSize)
            .add("flags", flags)
            .add("tags", tags)
            .add("metadatas", metadatas)
            .add("subject", subject)
            .add("messages", messages)
            .toString();
    }
}
