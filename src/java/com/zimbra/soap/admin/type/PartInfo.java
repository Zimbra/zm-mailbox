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

package com.zimbra.soap.admin.type;

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
import com.zimbra.soap.base.PartInfoInterface;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = { "content", "mimeParts"})
public class PartInfo
implements PartInfoInterface {

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
    private ZmBoolean body;

    @XmlAttribute(name=MailConstants.A_TRUNCATED_CONTENT, required=false)
    private ZmBoolean truncatedContent;

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

    @Override
    public PartInfoInterface createFromPartAndContentType(String part,
            String contentType) {
        return new PartInfo(part, contentType);
    }

    @Override
    public void setSize(Integer size) { this.size = size; }
    @Override
    public void setContentDisposition(String contentDisposition) {
        this.contentDisposition = contentDisposition;
    }
    @Override
    public void setContentFilename(String contentFilename) {
        this.contentFilename = contentFilename;
    }
    @Override
    public void setContentId(String contentId) { this.contentId = contentId; }
    @Override
    public void setLocation(String location) { this.location = location; }
    @Override
    public void setBody(Boolean body) { this.body = ZmBoolean.fromBool(body); }
    @Override
    public void setTruncatedContent(Boolean truncatedContent) {
        this.truncatedContent = ZmBoolean.fromBool(truncatedContent);
    }
    @Override
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

    @Override
    public String getPart() { return part; }
    @Override
    public String getContentType() { return contentType; }
    @Override
    public Integer getSize() { return size; }
    @Override
    public String getContentDisposition() { return contentDisposition; }
    @Override
    public String getContentFilename() { return contentFilename; }
    @Override
    public String getContentId() { return contentId; }
    @Override
    public String getLocation() { return location; }
    @Override
    public Boolean getBody() { return ZmBoolean.toBool(body); }
    @Override
    public Boolean getTruncatedContent() { return ZmBoolean.toBool(truncatedContent); }
    @Override
    public String getContent() { return content; }
    public List<PartInfo> getMimeParts() {
        return Collections.unmodifiableList(mimeParts);
    }

    @Override
    public void setMimePartInterfaces(Iterable<PartInfoInterface> mimeParts) {
        setMimeParts(PartInfo.fromInterfaces(mimeParts));

    }

    @Override
    public void addMimePartInterface(PartInfoInterface mimePart) {
        addMimePart((PartInfo) mimePart);
    }

    @Override
    public List<PartInfoInterface> getMimePartInterfaces() {
        return PartInfo.toInterfaces(mimeParts);
    }

    public static Iterable <PartInfo> fromInterfaces(
                    Iterable <PartInfoInterface> ifs) {
        if (ifs == null)
            return null;
        List <PartInfo> newList = Lists.newArrayList();
        for (PartInfoInterface listEnt : ifs) {
            newList.add((PartInfo) listEnt);
        }
        return newList;
    }

    public static List <PartInfoInterface> toInterfaces(
                    Iterable <PartInfo> params) {
        if (params == null)
            return null;
        List <PartInfoInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
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
