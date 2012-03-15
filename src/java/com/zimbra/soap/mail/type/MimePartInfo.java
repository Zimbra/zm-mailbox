/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"mimeParts", "attachments"})
public class MimePartInfo {

    /**
     * @zm-api-field-tag content-type
     * @zm-api-field-description Content type
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=false)
    private String contentType;

    /**
     * @zm-api-field-tag content
     * @zm-api-field-description Content
     */
    @XmlAttribute(name=MailConstants.E_CONTENT /* content */, required=false)
    private String content;

    /**
     * @zm-api-field-tag content-id
     * @zm-api-field-description Content ID
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_ID /* ci */, required=false)
    private String contentId;

    /**
     * @zm-api-field-description MIME Parts
     */
    @XmlElement(name=MailConstants.E_MIMEPART /* mp */, required=false)
    private List<MimePartInfo> mimeParts = Lists.newArrayList();

    /**
     * @zm-api-field-description Attachments
     */
    @XmlElement(name=MailConstants.E_ATTACH /* attach */, required=false)
    private AttachmentsInfo attachments;

    public MimePartInfo() {
    }

    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setContent(String content) { this.content = content; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public void setMimeParts(Iterable <MimePartInfo> mimeParts) {
        this.mimeParts.clear();
        if (mimeParts != null) {
            Iterables.addAll(this.mimeParts,mimeParts);
        }
    }

    public void addMimePart(MimePartInfo mimePart) {
        this.mimeParts.add(mimePart);
    }

    public void setAttachments(AttachmentsInfo attachments) { this.attachments = attachments; }
    public String getContentType() { return contentType; }
    public String getContent() { return content; }
    public String getContentId() { return contentId; }
    public List<MimePartInfo> getMimeParts() {
        return mimeParts;
    }
    public AttachmentsInfo getAttachments() { return attachments; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("contentType", contentType)
            .add("content", content)
            .add("contentId", contentId)
            .add("mimeParts", mimeParts)
            .add("attachments", attachments);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
