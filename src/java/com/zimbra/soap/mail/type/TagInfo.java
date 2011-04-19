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

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.CustomMetadata;

@XmlAccessorType(XmlAccessType.FIELD)
public class TagInfo {

    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_NAME, required=false)
    private String name;

    @XmlAttribute(name=MailConstants.A_COLOR, required=false)
    private Byte color;

    @XmlAttribute(name=MailConstants.A_RGB, required=false)
    private String rgb;

    @XmlAttribute(name=MailConstants.A_UNREAD, required=false)
    private Integer unread;

    @XmlAttribute(name=MailConstants.A_IMAP_UNREAD, required=false)
    private Integer imapUnread;

    @XmlAttribute(name=MailConstants.A_DATE, required=false)
    private Long date;

    @XmlAttribute(name=MailConstants.A_REVISION, required=false)
    private Integer revision;

    @XmlAttribute(name=MailConstants.A_CHANGE_DATE, required=false)
    private Long changeDate;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE, required=false)
    private Integer modifiedSequence;

    @XmlElement(name=MailConstants.E_METADATA, required=false)
    private List<CustomMetadata> metadatas = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private TagInfo() {
        this((String) null);
    }

    public TagInfo(String id) {
        this.id = id;
    }

    public void setName(String name) { this.name = name; }
    public void setColor(Byte color) { this.color = color; }
    public void setRgb(String rgb) { this.rgb = rgb; }
    public void setUnread(Integer unread) { this.unread = unread; }
    public void setImapUnread(Integer imapUnread) {
        this.imapUnread = imapUnread;
    }
    public void setDate(Long date) { this.date = date; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }
    public void setModifiedSequence(Integer modifiedSequence) { this.modifiedSequence = modifiedSequence; }
    public void setMetadatas(Iterable <CustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public TagInfo addMetadata(CustomMetadata metadata) {
        this.metadatas.add(metadata);
        return this;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Byte getColor() { return color; }
    public String getRgb() { return rgb; }
    public Integer getUnread() { return unread; }
    public Integer getImapUnread() { return imapUnread; }
    public Long getDate() { return date; }
    public Integer getRevision() { return revision; }
    public Long getChangeDate() { return changeDate; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public List<CustomMetadata> getMetadatas() {
        return Collections.unmodifiableList(metadatas);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("id", id)
            .add("name", name)
            .add("color", color)
            .add("rgb", rgb)
            .add("unread", unread)
            .add("imapUnread", imapUnread)
            .add("date", date)
            .add("revision", revision)
            .add("changeDate", changeDate)
            .add("modifiedSequence", modifiedSequence)
            .add("metadatas", metadatas)
            .toString();
    }
}
