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

    /**
     * @zm-api-field-tag mime-part-name
     * @zm-api-field-description MIME part name. "" means top-level part, 1 first part, 1.1 first part of a multipart
     * inside of 1.
     */
    @XmlAttribute(name=MailConstants.A_PART /* part */, required=true)
    private final String part;

    /**
     * @zm-api-field-tag content-type
     * @zm-api-field-description MIME Content-Type. The mime type is the content of the element.
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=true)
    private final String contentType;

    /**
     * @zm-api-field-tag size-in-bytes
     * @zm-api-field-description Size in bytes
     */
    @XmlAttribute(name=MailConstants.A_SIZE /* s */, required=false)
    private Integer size;

    /**
     * @zm-api-field-tag content-disp
     * @zm-api-field-description MIME Content-Disposition
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_DISPOSITION /* cd */, required=false)
    private String contentDisposition;

    /**
     * @zm-api-field-tag filename
     * @zm-api-field-description Filename attribute from the Content-Disposition param list
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_FILENAME /* filename */, required=false)
    private String contentFilename;

    /**
     * @zm-api-field-tag content-id
     * @zm-api-field-description MIME Content-ID (for display of embedded images)
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_ID /* ci */, required=false)
    private String contentId;

    /**
     * @zm-api-field-tag content-location
     * @zm-api-field-description MIME/Microsoft Content-Location (for display of embedded images)
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_LOCATION /* cl */, required=false)
    private String location;

    /**
     * @zm-api-field-tag is-body
     * @zm-api-field-description Set if this part is considered to be the "body" of the message for display purposes.
     */
    @XmlAttribute(name=MailConstants.A_BODY /* body */, required=false)
    private ZmBoolean body;

    /**
     * @zm-api-field-tag truncated-content
     * @zm-api-field-description Set if the content for the part is truncated
     */
    @XmlAttribute(name=MailConstants.A_TRUNCATED_CONTENT /* truncated */, required=false)
    private ZmBoolean truncatedContent;

    /**
     * @zm-api-field-tag content
     * @zm-api-field-description The content of the part, if requested
     */
    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private String content;

    /**
     * @zm-api-field-description Mime parts
     */
    @XmlElement(name=MailConstants.E_MIMEPART /* mp */, required=false)
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
        return MoreObjects.toStringHelper(this)
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
