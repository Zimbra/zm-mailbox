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
@XmlType(propOrder = { "content", "mimeParts"})
public class PartInfo {

    @XmlAttribute(name=MailConstants.A_PART, required=true)
    private final String part;

    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE, required=true)
    private final String contentType;

    @XmlAttribute(name=MailConstants.A_SIZE, required=false)
    private Integer size;

    @XmlAttribute(name=MailConstants.A_CONTENT_DISPOSTION, required=false)
    private String contentDisposition;

    @XmlAttribute(name=MailConstants.A_CONTENT_FILENAME, required=false)
    private String contentFilename;

    @XmlAttribute(name=MailConstants.A_CONTENT_ID, required=false)
    private String contentId;

    @XmlAttribute(name=MailConstants.A_CONTENT_LOCATION, required=false)
    private String location;

    @XmlAttribute(name=MailConstants.A_BODY, required=false)
    private Boolean body;

    @XmlAttribute(name=MailConstants.A_TRUNCATED_CONTENT, required=false)
    private Boolean truncatedContent;

    @XmlElement(name=MailConstants.E_CONTENT, required=false)
    private String content;

    @XmlElement(name=MailConstants.E_MIMEPART, required=false)
    private List<PartInfo> mimeParts = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private PartInfo() {
        this((String) null, (String) null);
    }

    public PartInfo(String part, String contentType) {
        this.part = part;
        this.contentType = contentType;
    }

    public void setSize(Integer size) { this.size = size; }
    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }
    public void setContentFilename(String contentFilename) {
        this.contentFilename = contentFilename;
    }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public void setLocation(String location) { this.location = location; }
    public void setBody(Boolean body) { this.body = body; }
    public void setTruncatedContent(Boolean truncatedContent) {
        this.truncatedContent = truncatedContent;
    }
    public void setContent(String content) { this.content = content; }
    public void setMimeParts(Iterable <PartInfo> mimeParts) {
        this.mimeParts.clear();
        if (mimeParts != null) {
            Iterables.addAll(this.mimeParts,mimeParts);
        }
    }

    public PartInfo addMimePart(PartInfo mimePart) {
        this.mimeParts.add(mimePart);
        return this;
    }

    public String getPart() { return part; }
    public String getContentType() { return contentType; }
    public Integer getSize() { return size; }
    public String getContentDisposition() { return contentDisposition; }
    public String getContentFilename() { return contentFilename; }
    public String getContentId() { return contentId; }
    public String getLocation() { return location; }
    public Boolean getBody() { return body; }
    public Boolean getTruncatedContent() { return truncatedContent; }
    public String getContent() { return content; }
    public List<PartInfo> getMimeParts() {
        return Collections.unmodifiableList(mimeParts);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("part", part)
            .add("contentType", contentType)
            .add("size", size)
            .add("contentDisposition", contentDisposition)
            .add("contentFilename", contentFilename)
            .add("contentId", contentId)
            .add("location", location)
            .add("body", body)
            .add("truncatedContent", truncatedContent)
            .add("content", content)
            .add("mimeParts", mimeParts)
            .toString();
    }
}
