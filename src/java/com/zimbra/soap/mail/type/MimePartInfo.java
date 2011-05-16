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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"mimeParts", "attachments"})
public class MimePartInfo {

    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE, required=false)
    private String contentType;

    @XmlAttribute(name=MailConstants.E_CONTENT, required=false)
    private String content;

    @XmlAttribute(name=MailConstants.A_CONTENT_ID, required=false)
    private String contentId;

    @XmlElement(name=MailConstants.E_MIMEPART, required=false)
    private List<MimePartInfo> mimeParts = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_ATTACH, required=false)
    private AttachmentsInfo attachments;

    public MimePartInfo() {
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public void setContent(String content) { this.content = content; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public void setMimeParts(Iterable <MimePartInfo> mimeParts) {
        this.mimeParts.clear();
        if (mimeParts != null) {
            Iterables.addAll(this.mimeParts,mimeParts);
        }
    }

    public MimePartInfo addMimePart(MimePartInfo mimePart) {
        this.mimeParts.add(mimePart);
        return this;
    }

    public void setAttachments(AttachmentsInfo attachments) {
        this.attachments = attachments;
    }
    public String getContentType() { return contentType; }
    public String getContent() { return content; }
    public String getContentId() { return contentId; }
    public List<MimePartInfo> getMimeParts() {
        return Collections.unmodifiableList(mimeParts);
    }
    public AttachmentsInfo getAttachments() { return attachments; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("contentType", contentType)
            .add("content", content)
            .add("contentId", contentId)
            .add("mimeParts", mimeParts)
            .add("attachments", attachments);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
