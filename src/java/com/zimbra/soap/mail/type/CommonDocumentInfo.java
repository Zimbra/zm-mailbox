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
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "metadatas", "fragment", "grants" })
public class CommonDocumentInfo {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    @XmlAttribute(name=MailConstants.A_SIZE /* s */, required=false)
    private Long size;

    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    @XmlAttribute(name=MailConstants.A_CHANGE_DATE /* md */, required=false)
    private Long changeDate;

    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    @XmlAttribute(name=MailConstants.A_DESC /* desc */, required=false)
    private String description;

    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=false)
    private String contentType;

    @XmlAttribute(name=MailConstants.A_DESC_ENABLED /* descEnabled */, required=false)
    private ZmBoolean descEnabled;

    @XmlAttribute(name=MailConstants.A_VERSION /* ver */, required=false)
    private Integer version;

    @XmlAttribute(name=MailConstants.A_LAST_EDITED_BY /* leb */, required=false)
    private String lastEditedBy;

    @XmlAttribute(name=MailConstants.A_CREATOR /* cr */, required=false)
    private String creator;

    @XmlAttribute(name=MailConstants.A_CREATED_DATE /* cd */, required=false)
    private Long createdDate;

    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private List<MailCustomMetadata> metadatas = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_FRAG /* fr */, required=false)
    private String fragment;

    @XmlElementWrapper(name=MailConstants.E_ACL /* acl */, required=false)
    @XmlElement(name=MailConstants.E_GRANT /* grant */, required=false)
    private List<Grant> grants = Lists.newArrayList();

    public CommonDocumentInfo() {
        this((String) null);
    }

    public CommonDocumentInfo(String id) {
        this.setId(id);
    }

    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSize(Long size) { this.size = size; }
    public void setDate(Long date) { this.date = date; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public void setFlags(String flags) { this.flags = flags; }
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public void setDescEnabled(Boolean descEnabled) {
        this.descEnabled = ZmBoolean.fromBool(descEnabled);
    }
    public void setVersion(Integer version) { this.version = version; }
    public void setLastEditedBy(String lastEditedBy) {
        this.lastEditedBy = lastEditedBy;
    }
    public void setCreator(String creator) { this.creator = creator; }
    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
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

    public void setFragment(String fragment) { this.fragment = fragment; }
    public void setGrants(Iterable <Grant> grants) {
        this.grants.clear();
        if (grants != null) {
            Iterables.addAll(this.grants,grants);
        }
    }

    public void addGrant(Grant grant) {
        this.grants.add(grant);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Long getSize() { return size; }
    public Long getDate() { return date; }
    public String getFolderId() { return folderId; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public Long getChangeDate() { return changeDate; }
    public Integer getRevision() { return revision; }
    public String getFlags() { return flags; }
    @Deprecated
    public String getTags() { return tags; }
    public String getTagNames() { return tagNames; }
    public String getDescription() { return description; }
    public String getContentType() { return contentType; }
    public Boolean getDescEnabled() { return ZmBoolean.toBool(descEnabled); }
    public Integer getVersion() { return version; }
    public String getLastEditedBy() { return lastEditedBy; }
    public String getCreator() { return creator; }
    public Long getCreatedDate() { return createdDate; }
    public List<MailCustomMetadata> getMetadatas() {
        return Collections.unmodifiableList(metadatas);
    }
    public String getFragment() { return fragment; }
    public List<Grant> getGrants() {
        return Collections.unmodifiableList(grants);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("name", name)
            .add("size", size)
            .add("date", date)
            .add("folderId", folderId)
            .add("modifiedSequence", modifiedSequence)
            .add("changeDate", changeDate)
            .add("revision", revision)
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("description", description)
            .add("contentType", contentType)
            .add("descEnabled", descEnabled)
            .add("version", version)
            .add("lastEditedBy", lastEditedBy)
            .add("creator", creator)
            .add("createdDate", createdDate)
            .add("metadatas", metadatas)
            .add("fragment", fragment)
            .add("grants", grants);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
