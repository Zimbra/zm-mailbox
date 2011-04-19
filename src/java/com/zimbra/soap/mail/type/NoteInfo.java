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
@XmlType(propOrder = { "content", "metadatas" })
public class NoteInfo {

    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_REVISION, required=false)
    private Integer revision;

    @XmlAttribute(name=MailConstants.A_FOLDER, required=false)
    private String folder;

    @XmlAttribute(name=MailConstants.A_DATE, required=false)
    private Long date;

    @XmlAttribute(name=MailConstants.A_FLAGS, required=false)
    private String flags;

    @XmlAttribute(name=MailConstants.A_TAGS, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_BOUNDS, required=false)
    private String bounds;

    @XmlAttribute(name=MailConstants.A_COLOR, required=false)
    private Byte color;

    @XmlAttribute(name=MailConstants.A_RGB, required=false)
    private String rgb;

    @XmlAttribute(name=MailConstants.A_CHANGE_DATE, required=false)
    private Long changeDate;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE, required=false)
    private Integer modifiedSequence;

    @XmlElement(name=MailConstants.E_CONTENT, required=false)
    private String content;

    @XmlElement(name=MailConstants.E_METADATA, required=false)
    private List<CustomMetadata> metadatas = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private NoteInfo() {
        this((String) null);
    }

    public NoteInfo(String id) {
        this.id = id;
    }

    public void setRevision(Integer revision) { this.revision = revision; }
    public void setFolder(String folder) { this.folder = folder; }
    public void setDate(Long date) { this.date = date; }
    public void setFlags(String flags) { this.flags = flags; }
    public void setTags(String tags) { this.tags = tags; }
    public void setBounds(String bounds) { this.bounds = bounds; }
    public void setColor(Byte color) { this.color = color; }
    public void setRgb(String rgb) { this.rgb = rgb; }
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setContent(String content) { this.content = content; }
    public void setMetadatas(Iterable <CustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public NoteInfo addMetadata(CustomMetadata metadata) {
        this.metadatas.add(metadata);
        return this;
    }

    public String getId() { return id; }
    public Integer getRevision() { return revision; }
    public String getFolder() { return folder; }
    public Long getDate() { return date; }
    public String getFlags() { return flags; }
    public String getTags() { return tags; }
    public String getBounds() { return bounds; }
    public Byte getColor() { return color; }
    public String getRgb() { return rgb; }
    public Long getChangeDate() { return changeDate; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public String getContent() { return content; }
    public List<CustomMetadata> getMetadatas() {
        return Collections.unmodifiableList(metadatas);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("id", id)
            .add("revision", revision)
            .add("folder", folder)
            .add("date", date)
            .add("flags", flags)
            .add("tags", tags)
            .add("bounds", bounds)
            .add("color", color)
            .add("rgb", rgb)
            .add("changeDate", changeDate)
            .add("modifiedSequence", modifiedSequence)
            .add("content", content)
            .add("metadatas", metadatas)
            .toString();
    }
}
