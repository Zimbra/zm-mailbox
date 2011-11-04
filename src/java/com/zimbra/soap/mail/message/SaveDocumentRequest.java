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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.IdVersion;
import com.zimbra.soap.mail.type.MessagePartSpec;
import com.zimbra.soap.type.Id;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_SAVE_DOCUMENT_REQUEST)
public class SaveDocumentRequest {

    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    private String name;

    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=false)
    private String contentType;

    @XmlAttribute(name=MailConstants.A_DESC /* desc */, required=false)
    private String description;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_VERSION /* ver */, required=false)
    private Integer version;

    @XmlAttribute(name=MailConstants.E_CONTENT /* content */, required=false)
    private String content;

    @XmlAttribute(name=MailConstants.A_DESC_ENABLED, required=false)
    private ZmBoolean descEnabled;

    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    @XmlElement(name=MailConstants.E_UPLOAD /* upload */, required=false)
    private Id upload;

    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private MessagePartSpec messagePart;

    @XmlElement(name=MailConstants.E_DOC /* doc */, required=false)
    private IdVersion docRevision;

    public SaveDocumentRequest() {
    }

    public void setName(String name) { this.name = name; }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setId(String id) { this.id = id; }
    public void setVersion(Integer version) { this.version = version; }
    public void setContent(String content) { this.content = content; }
    public void setDescEnabled(Boolean descEnabled) {
        this.descEnabled = ZmBoolean.fromBool(descEnabled);
    }
    public void setFlags(String flags) { this.flags = flags; }
    public void setUpload(Id upload) { this.upload = upload; }
    public void setMessagePart(MessagePartSpec messagePart) {
        this.messagePart = messagePart;
    }
    public void setDocRevision(IdVersion docRevision) {
        this.docRevision = docRevision;
    }
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
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
            .add("docRevision", docRevision);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
