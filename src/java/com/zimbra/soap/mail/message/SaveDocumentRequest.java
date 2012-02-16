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

/**
 * @zm-api-command-description Save Document
 * <br />
 * <br />
 * One mechanism for Creating and updating a Document is:
 * <ol>
 * <li> Use FileUploadServlet to upload the document
 * <li> Call SaveDocumentRequest using the upload-id returned from FileUploadServlet.
 * </ol>
 * A Document represents a file.  A file can be created by uploading to FileUploadServlet.  Or it can refer to an
 * attachment of an existing message.
 * <br />
 * <br />
 * Documents are versioned.  The server maintains the metadata of each version, such as by who and when the version
 * was edited, and the fragment.
 * <br />
 * <br />
 * When updating an existing Document, the client must supply the id of Document, and the last known version of the
 * document in the 'ver' attribute.  This is used to prevent blindly overwriting someone else's change made after
 * the version this update was based upon.  The update will succeed only when the last known version supplied by the
 * client matches the current version of the item identified by item-id.
 * <br />
 * <br />
 * Saving a new document, as opposed to adding a revision of existing document, should leave the id and ver fields
 * empty in the request.  Then the server checks and see if the named document already exists, if so returns an error.
 * <br />
 * <br />
 * The request should contain either an <b>&lt;upload></b> element or a <b>&lt;msg></b> element, but not both.
 * When <b>&lt;upload></b> is used, the document should first be uploaded to FileUploadServlet, and then use the
 * upload-id from the FileUploadResponse.
 * <br />
 * When <b>&lt;m></b> is used, the document is retrieved from an existing message in the mailbox, identified by the
 * msg-id and part-id.  The content of the document can be inlined in the <b>&ly;content></b> element.
 * The content can come from another document / revision specified in the <b>&lt;doc></b> sub element.
 * <br />
 * Examples:
 * <br />
 * <br />
 * Saving a new document:
 * <pre>
 *     &lt;SaveDocumentRequest xmlns:ns0="urn:zimbraMail">
 *       &lt;doc>
 *         &lt;upload id="18baa043-394f-42ae-be8a-110b279cb696:cc2f2fdf-7957-4412-aa83-6433662ce5d0"/>
 *       &lt;/doc>
 *     &lt;/SaveDocumentRequest>
 *
 *     &lt;SaveDocumentResponse xmlns:ns0="urn:zimbraMail">
 *       &lt;doc ver="1" id="574" rest="http://localhost:7070/home/user1/Notebook/PICT0370.JPG"/>
 *     &lt;/SaveDocumentResponse>
 * </pre>
 * Updating an existing document
 * <pre>
 *     &lt;SaveDocumentRequest xmlns:ns0="urn:zimbraMail">
 *       &lt;doc ver="1" id="574" desc="rev 2.0">
 *         &lt;upload id="18baa043-394f-42ae-be8a-110b279cb696:fcb572ce-2a81-4ad3-b55b-cb998c47b416"/>
 *       &lt;/doc>
 *     &lt;/SaveDocumentRequest>
 *
 *     &lt;SaveDocumentResponse xmlns:ns0="urn:zimbraMail">
 *       &lt;doc ver="2" id="574" rest="http://localhost:7070/home/user1/Notebook/PICT0370.JPG"/>
 *     &lt;/SaveDocumentResponse>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SAVE_DOCUMENT_REQUEST)
public class SaveDocumentRequest {

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
    @XmlAttribute(name=MailConstants.A_DESC_ENABLED, required=false)
    private ZmBoolean descEnabled;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags - Any of the flags specified in soap.txt, with the addition of "t", which
     * specifies that the document is a note.
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-tag upload-id
     * @zm-api-field-description Upload ID
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
