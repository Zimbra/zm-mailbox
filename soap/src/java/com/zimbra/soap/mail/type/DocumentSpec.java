/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.Id;
import com.zimbra.soap.type.NewFileCreationTypes;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class DocumentSpec {

    /**
     * @zm-api-field-tag file-name
     * @zm-api-field-description File name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag content-type
     * @zm-api-field-description Content Type
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=false)
    private String contentType;

    /**
     * @zm-api-field-tag description
     * @zm-api-field-description Description
     */
    @XmlAttribute(name=MailConstants.A_DESC /* desc */, required=false)
    private String description;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Item ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag last-known-version
     * @zm-api-field-description Last known version
     */
    @XmlAttribute(name=MailConstants.A_VERSION /* ver */, required=false)
    private Integer version;

    /**
     * @zm-api-field-tag inlined-document-content-string
     * @zm-api-field-description Inlined document content string
     */
    @XmlAttribute(name=MailConstants.E_CONTENT /* content */, required=false)
    private String content;

    /**
     * @zm-api-field-tag desc-enabled
     * @zm-api-field-description Desc enabled flag
     */
    @XmlAttribute(name=MailConstants.A_DESC_ENABLED /* descEnabled */, required=false)
    private ZmBoolean descEnabled;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags - Any of the flags specified in soap.txt, with the addition of <b>"t"</b>, which
     * specifies that the document is a note.
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-description Upload specification
     */
    @XmlElement(name=MailConstants.E_UPLOAD /* upload */, required=false)
    private Id upload;

    /**
     * @zm-api-field-description Message part specification
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private MessagePartSpec messagePart;

    /**
     * @zm-api-field-description Information on document version to restore to
     */
    @XmlElement(name=MailConstants.E_DOC /* doc */, required=false)
    private IdVersion docRevision;

    /**
     * @zm-api-field-description Action on the Document
     */
    @XmlAttribute(name = MailConstants.A_ACTION /* action */, required = false)
    private String action;

    /**
     * @zm-api-field-description Type of Document that can be created
     */
    @XmlAttribute(name = MailConstants.A_NEW_DOC_TYPE /* type */, required = false)
    private NewFileCreationTypes type;

    public DocumentSpec() {
    }

    public void setName(String name) { this.name = name; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setDescription(String description) { this.description = description; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setId(String id) { this.id = id; }
    public void setVersion(Integer version) { this.version = version; }
    public void setContent(String content) { this.content = content; }
    public void setDescEnabled(Boolean descEnabled) { this.descEnabled = ZmBoolean.fromBool(descEnabled); }
    public void setFlags(String flags) { this.flags = flags; }
    public void setUpload(Id upload) { this.upload = upload; }
    public void setMessagePart(MessagePartSpec messagePart) { this.messagePart = messagePart; }
    public void setDocRevision(IdVersion docRevision) { this.docRevision = docRevision; }
    public void setAction(String action) { this.action = action; }
    public void setType(NewFileCreationTypes type) { this.type = type; }
    public String getName() { return name; }
    public String getContentType() { return contentType; }
    public String getDescription() { return description; }
    public String getFolderId() { return folderId; }
    public String getId() { return id; }
    public Integer getVersion() { return version; }
    public String getContent() { return content; }
    public Boolean getDescEnabled() { return ZmBoolean.toBool(descEnabled); }
    public String getFlags() { return flags; }
    public Id getUpload() { return upload; }
    public MessagePartSpec getMessagePart() { return messagePart; }
    public IdVersion getDocRevision() { return docRevision; }
    public String getAction() { return action; }
    public NewFileCreationTypes getType() { return type; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("contentType", contentType)
            .add("description", description)
            .add("folderId", folderId)
            .add("id", id)
            .add("version", version)
            .add("content", content)
            .add("descEnabled", descEnabled)
            .add("flags", flags)
            .add("upload", upload)
            .add("messagePart", messagePart)
            .add("docRevision", docRevision)
            .add("action", action)
            .add("type", type);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
