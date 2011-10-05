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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "metadatas", "subject", "messages"})
public class ConversationInfo {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_NUM /* n */, required=false)
    private Integer num;

    @XmlAttribute(name=MailConstants.A_TOTAL_SIZE /* total */, required=false)
    private Integer totalSize;

    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private List<MailCustomMetadata> metadatas = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_SUBJECT /* su */, required=false)
    private String subject;

    @XmlElements({
        @XmlElement(name=MailConstants.E_CHAT /* chat */,
            type=ChatMessageInfo.class),
        @XmlElement(name=MailConstants.E_MSG /* m */,
            type=MessageInfo.class)
    })
    private List<MessageInfo> messages = Lists.newArrayList();

    public ConversationInfo() {
        this((String) null);
    }

    public ConversationInfo(String id) {
        this.setId(id);
    }

    public void setId(String id) { this.id = id; }
    public void setNum(Integer num) { this.num = num; }
    public void setTotalSize(Integer totalSize) { this.totalSize = totalSize; }
    public void setFlags(String flags) { this.flags = flags; }
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public void setMetadatas(Iterable <MailCustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public void addMetadata(MailCustomMetadata metadata) {
        this.metadatas.add(metadata);
    }

    public void setSubject(String subject) { this.subject = subject; }
    public void setMessages(Iterable <MessageInfo> messages) {
        this.messages.clear();
        if (messages != null) {
            Iterables.addAll(this.messages,messages);
        }
    }

    public void addMessage(MessageInfo message) {
        this.messages.add(message);
    }

    public String getId() { return id; }
    public Integer getNum() { return num; }
    public Integer getTotalSize() { return totalSize; }
    public String getFlags() { return flags; }
    @Deprecated
    public String getTags() { return tags; }
    public String getTagNames() { return tagNames; }
    public List<MailCustomMetadata> getMetadatas() {
        return Collections.unmodifiableList(metadatas);
    }
    public String getSubject() { return subject; }
    public List<MessageInfo> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("num", num)
            .add("totalSize", totalSize)
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("metadatas", metadatas)
            .add("subject", subject)
            .add("messages", messages);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
