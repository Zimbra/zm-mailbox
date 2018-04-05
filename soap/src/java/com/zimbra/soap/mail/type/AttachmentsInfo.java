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

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("attachmentId", attachmentId)
            .add("attachments", attachments)
            .add("extraElements", extraElements);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
