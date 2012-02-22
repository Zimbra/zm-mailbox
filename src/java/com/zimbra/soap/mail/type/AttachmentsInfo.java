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
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"attachments", "extraElements"})
public class AttachmentsInfo {

    /**
     * @zm-api-field-tag attach-upload-id
     * @zm-api-field-description Attachment upload ID
     */
    @XmlAttribute(name=MailConstants.A_ATTACHMENT_ID /* aid */, required=false)
    private String attachmentId;

    /**
     * @zm-api-field-description Attachment details
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_MIMEPART /* mp */, type=MimePartAttachSpec.class),
        @XmlElement(name=MailConstants.E_MSG /* m */, type=MsgAttachSpec.class),
        @XmlElement(name=MailConstants.E_CONTACT /* cn */, type=ContactAttachSpec.class),
        @XmlElement(name=MailConstants.E_DOC /* doc */, type=DocAttachSpec.class)
    })
    private List<AttachSpec> attachments = Lists.newArrayList();

    /**
     * @zm-api-field-description Other elements
     */
    @XmlAnyElement
    private List<org.w3c.dom.Element> extraElements = Lists.newArrayList();

    public AttachmentsInfo() {
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }
    public void setAttachments(Iterable <AttachSpec> attachments) {
        this.attachments.clear();
        if (attachments != null) {
            Iterables.addAll(this.attachments,attachments);
        }
    }

    public AttachmentsInfo addAttachment(AttachSpec attachment) {
        this.attachments.add(attachment);
        return this;
    }

    public void setExtraElements(Iterable <org.w3c.dom.Element> extraElements) {
        this.extraElements.clear();
        if (extraElements != null) {
            Iterables.addAll(this.extraElements,extraElements);
        }
    }

    public AttachmentsInfo addExtraElement(org.w3c.dom.Element extraElement) {
        this.extraElements.add(extraElement);
        return this;
    }

    public String getAttachmentId() { return attachmentId; }
    public List<AttachSpec> getAttachments() {
        return Collections.unmodifiableList(attachments);
    }
    public List<org.w3c.dom.Element> getExtraElements() {
        return Collections.unmodifiableList(extraElements);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("attachmentId", attachmentId)
            .add("attachments", attachments)
            .add("extraElements", extraElements);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
