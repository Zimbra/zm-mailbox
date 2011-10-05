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
@XmlType(propOrder = { "content", "metadatas" })
public class NoteInfo {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folder;

    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    @XmlAttribute(name=MailConstants.A_BOUNDS /* pos */, required=false)
    private String bounds;

    @XmlAttribute(name=MailConstants.A_COLOR /* color */, required=false)
    private Byte color;

    @XmlAttribute(name=MailConstants.A_RGB /* rgb */, required=false)
    private String rgb;

    @XmlAttribute(name=MailConstants.A_CHANGE_DATE /* md */, required=false)
    private Long changeDate;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */,
                            required=false)
    private Integer modifiedSequence;

    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private String content;

    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private List<MailCustomMetadata> metadatas = Lists.newArrayList();

    public NoteInfo() {
        this((String) null);
    }

    public NoteInfo(String id) {
        this.setId(id);
    }

    public void setId(String id) { this.id = id; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public void setFolder(String folder) { this.folder = folder; }
    public void setDate(Long date) { this.date = date; }
    public void setFlags(String flags) { this.flags = flags; }
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public void setBounds(String bounds) { this.bounds = bounds; }
    public void setColor(Byte color) { this.color = color; }
    public void setRgb(String rgb) { this.rgb = rgb; }
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setContent(String content) { this.content = content; }
    public void setMetadatas(Iterable <MailCustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public void addMetadata(MailCustomMetadata metadata) {
        this.metadatas.add(metadata);
    }

    public String getId() { return id; }
    public Integer getRevision() { return revision; }
    public String getFolder() { return folder; }
    public Long getDate() { return date; }
    public String getFlags() { return flags; }
    @Deprecated
    public String getTags() { return tags; }
    public String getTagNames() { return tagNames; }
    public String getBounds() { return bounds; }
    public Byte getColor() { return color; }
    public String getRgb() { return rgb; }
    public Long getChangeDate() { return changeDate; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public String getContent() { return content; }
    public List<MailCustomMetadata> getMetadatas() {
        return Collections.unmodifiableList(metadatas);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("revision", revision)
            .add("folder", folder)
            .add("date", date)
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("bounds", bounds)
            .add("color", color)
            .add("rgb", rgb)
            .add("changeDate", changeDate)
            .add("modifiedSequence", modifiedSequence)
            .add("content", content)
            .add("metadatas", metadatas);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
