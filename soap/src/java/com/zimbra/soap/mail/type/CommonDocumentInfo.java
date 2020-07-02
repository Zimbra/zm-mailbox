/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.type;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "metadatas", "fragment", "acl" })
public class CommonDocumentInfo {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag uuid
     * @zm-api-field-description Item's UUID - a globally unique identifier
     */
    @XmlAttribute(name=MailConstants.A_UUID /* uuid */, required=false)
    private String uuid;

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag size
     * @zm-api-field-description Size
     */
    @XmlAttribute(name=MailConstants.A_SIZE /* s */, required=false)
    private Long size;

    /**
     * @zm-api-field-tag date-millis
     * @zm-api-field-description Date the item's content was last modified in milliseconds since 1970-01-01 00:00:00 UTC.
     * For immutable objects (e.g. received messages), this will be the same as the date the item was created.
     */
    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag folder-uuid
     * @zm-api-field-description Folder UUID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER_UUID /* luuid */, required=false)
    private String folderUuid;

    /**
     * @zm-api-field-tag modified-sequence
     * @zm-api-field-description Modified sequence
     */
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    /**
     * @zm-api-field-tag metadata-version
     * @zm-api-field-description Metadata version
     */
    @XmlAttribute(name=MailConstants.A_METADATA_VERSION /* mdver */, required=false)
    private Integer metadataVersion;

    /**
     * @zm-api-field-tag change-date-seconds
     * @zm-api-field-description The date the item's metadata and/or content was last modified in seconds since
     * 1970-01-01 00:00:00 UTC.
     */
    @XmlAttribute(name=MailConstants.A_CHANGE_DATE /* md */, required=false)
    private Long changeDate;

    /**
     * @zm-api-field-tag revision
     * @zm-api-field-description Revision
     */
    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-tag tags
     * @zm-api-field-description Tags - Comma separated list of integers.  DEPRECATED - use "tn" instead
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Comma-separated list of tag names
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    /**
     * @zm-api-field-tag optional-description
     * @zm-api-field-description Optional description
     */
    @XmlAttribute(name=MailConstants.A_DESC /* desc */, required=false)
    private String description;

    /**
     * @zm-api-field-tag content-type
     * @zm-api-field-description Content type
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=false)
    private String contentType;

    /**
     * @zm-api-field-tag is-description-enabled
     * @zm-api-field-description Flags whether description is enabled or not
     */
    @XmlAttribute(name=MailConstants.A_DESC_ENABLED /* descEnabled */, required=false)
    private ZmBoolean descEnabled;

    /**
     * @zm-api-field-tag version
     * @zm-api-field-description Version
     */
    @XmlAttribute(name=MailConstants.A_VERSION /* ver */, required=false)
    private Integer version;

    /**
     * @zm-api-field-tag last-edited-by
     * @zm-api-field-description Last edited by
     */
    @XmlAttribute(name=MailConstants.A_LAST_EDITED_BY /* leb */, required=false)
    private String lastEditedBy;

    /**
     * @zm-api-field-tag revision-creator
     * @zm-api-field-description Revision creator
     */
    @XmlAttribute(name=MailConstants.A_CREATOR /* cr */, required=false)
    private String creator;

    /**
     * @zm-api-field-tag revision-create-date-millis
     * @zm-api-field-description Revision creation date in milliseconds since 1970-01-01 00:00:00 UTC.
     */
    @XmlAttribute(name=MailConstants.A_CREATED_DATE /* cd */, required=false)
    private Long createdDate;

    /**
     * @zm-api-field-description Metadata
     */
    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private final List<MailCustomMetadata> metadatas = Lists.newArrayList();

    /**
     * @zm-api-field-tag fragment
     * @zm-api-field-description First few bytes of the message (probably between 40 and 100 bytes)
     */
    @ZimbraJsonAttribute
    @XmlElement(name=MailConstants.E_FRAG /* fr */, required=false)
    private String fragment;

    /**
     * @zm-api-field-description ACL for sharing
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_ACL /* acl */, required=false)
    private Acl acl;

    public CommonDocumentInfo() {
        this((String) null);
    }

    public CommonDocumentInfo(String id) {
        this.setId(id);
    }

    public void setId(String id) { this.id = id; }
    public void setUuid(String uuid) { this.uuid = uuid; }
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
    public void setAcl(Acl acl) { this.acl = acl; }

    public String getId() { return id; }
    public String getUuid() { return uuid; }
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
    public Acl getAcl() { return acl; }
    public String getFolderUuid() { return folderUuid; }
    public void setFolderUuid(String folderUuid) { this.folderUuid = folderUuid; }
    public Integer getMetadataVersion() { return metadataVersion; }
    public void setMetadataVersion(Integer metadataVersion) { this.metadataVersion = metadataVersion; }
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {

        return helper
            .add("id", id)
            .add("uuid", uuid)
            .add("name", name)
            .add("size", size)
            .add("date", date)
            .add("folderId", folderId)
            .add("folderUuid", folderUuid)
            .add("modifiedSequence", modifiedSequence)
            .add("metadataVersion", metadataVersion)
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
            .add("acl", acl);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
