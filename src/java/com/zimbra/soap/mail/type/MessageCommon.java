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

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"metadatas"})
public class MessageCommon {

    @XmlAttribute(name=MailConstants.A_SIZE /* s */, required=false)
    private Long size;

    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folder;

    @XmlAttribute(name=MailConstants.A_CONV_ID /* cid */, required=false)
    private String conversationId;

    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    @XmlAttribute(name=MailConstants.A_CHANGE_DATE /* md */, required=false)
    private Long changeDate;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private List<MailCustomMetadata> metadatas = Lists.newArrayList();

    public MessageCommon() {
    }

    public void setSize(Long size) { this.size = size; }
    public void setDate(Long date) { this.date = date; }
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
    public void setMetadatas(Iterable <MailCustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public void addMetadata(MailCustomMetadata metadata) {
        this.metadatas.add(metadata);
    }

    public Long getSize() { return size; }
    public Long getDate() { return date; }
    public String getFolder() { return folder; }
    public String getConversationId() { return conversationId; }
    public String getFlags() { return flags; }
    public String getTags() { return tags; }
    public Integer getRevision() { return revision; }
    public Long getChangeDate() { return changeDate; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public List<MailCustomMetadata> getMetadatas() {
        return Collections.unmodifiableList(metadatas);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("size", size)
            .add("date", date)
            .add("folder", folder)
            .add("conversationId", conversationId)
            .add("flags", flags)
            .add("tags", tags)
            .add("revision", revision)
            .add("changeDate", changeDate)
            .add("modifiedSequence", modifiedSequence)
            .add("metadatas", metadatas);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
