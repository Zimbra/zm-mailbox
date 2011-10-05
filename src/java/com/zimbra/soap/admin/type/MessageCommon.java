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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CustomMetadataInterface;
import com.zimbra.soap.base.MessageCommonInterface;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"metadatas"})
public class MessageCommon
implements MessageCommonInterface {

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

    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    @XmlAttribute(name=MailConstants.A_CHANGE_DATE /* md */, required=false)
    private Long changeDate;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private List<AdminCustomMetadata> metadatas = Lists.newArrayList();

    public MessageCommon() {
    }

    @Override
    public void setSize(Long size) { this.size = size; }
    @Override
    public void setDate(Long date) { this.date = date; }
    @Override
    public void setFolder(String folder) { this.folder = folder; }
    @Override
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    @Override
    public void setFlags(String flags) { this.flags = flags; }
    @Override
    public void setTags(String tags) { this.tags = tags; }
    @Override
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    @Override
    public void setRevision(Integer revision) { this.revision = revision; }
    @Override
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }
    @Override
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setMetadatas(Iterable <AdminCustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public void addMetadata(AdminCustomMetadata metadata) {
        this.metadatas.add(metadata);
    }

    @Override
    public Long getSize() { return size; }
    @Override
    public Long getDate() { return date; }
    @Override
    public String getFolder() { return folder; }
    @Override
    public String getConversationId() { return conversationId; }
    @Override
    public String getFlags() { return flags; }
    @Override
    public String getTags() { return tags; }
    @Override
    public String getTagNames() { return tagNames; }
    @Override
    public Integer getRevision() { return revision; }
    @Override
    public Long getChangeDate() { return changeDate; }
    @Override
    public Integer getModifiedSequence() { return modifiedSequence; }
    public List<AdminCustomMetadata> getMetadatas() {
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
            .add("tagNames", tagNames)
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

    @Override
    public void setMetadataInterfaces(
            Iterable<CustomMetadataInterface> metadatas) {
        for (CustomMetadataInterface meta : metadatas) {
            addMetadata((AdminCustomMetadata)meta);
        }
    }

    @Override
    public void addMetadataInterfaces(CustomMetadataInterface metadata) {
        addMetadata((AdminCustomMetadata)metadata);
    }

    @Override
    public List<CustomMetadataInterface> getMetadataInterfaces() {
        List<CustomMetadataInterface> metas = Lists.newArrayList();
        metas.addAll(metadatas);
        return Collections.unmodifiableList(metas);
    }
}
