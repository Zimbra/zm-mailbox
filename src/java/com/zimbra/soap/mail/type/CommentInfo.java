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
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {})
public class CommentInfo {

    @XmlAttribute(name=MailConstants.A_PARENT_ID /* parentId */, required=false)
    private String parentId;

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_EMAIL /* email */, required=false)
    private String creatorEmail;

    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    @XmlAttribute(name=MailConstants.A_COLOR /* color */, required=false)
    private Byte color;

    @XmlAttribute(name=MailConstants.A_RGB /* rgb */, required=false)
    private String rgb;

    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    // Need for @XmlMixed is forced because allow text content (used for
    // the Subject text) as well as sub-elements
    @XmlElementRefs({
        @XmlElementRef(name=MailConstants.E_METADATA /* meta */,
            type=MailCustomMetadata.class)
    })
    @XmlMixed
    private List<Object> elements = Lists.newArrayList();

    public CommentInfo() {
    }

    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setId(String id) { this.id = id; }
    public void setCreatorEmail(String creatorEmail) { this.creatorEmail = creatorEmail; }
    public void setFlags(String flags) { this.flags = flags; }
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public void setColor(Byte color) { this.color = color; }
    public void setRgb(String rgb) { this.rgb = rgb; }
    public void setDate(Long date) { this.date = date; }
    public void setElements(Iterable <Object> elements) {
        this.elements.clear();
        if (elements != null) {
            Iterables.addAll(this.elements,elements);
        }
    }

    public void addElement(Object element) {
        this.elements.add(element);
    }

    public String getParentId() { return parentId; }
    public String getId() { return id; }
    public String getCreatorEmail() { return creatorEmail; }
    public String getFlags() { return flags; }
    @Deprecated
    public String getTags() { return tags; }
    public String getTagNames() { return tagNames; }
    public Byte getColor() { return color; }
    public String getRgb() { return rgb; }
    public Long getDate() { return date; }
    public List<Object> getElements() {
        return Collections.unmodifiableList(elements);
    }

    // non-JAXB method
    public void setText(String text) { 
        if (elements == null)
            elements = Lists.newArrayList();
        // note that remove in foreach would be unsafe.
        for (int ndx = elements.size()-1; ndx >= 0; ndx--) {
            if (elements.get(ndx) instanceof String) {
                elements.remove(ndx);
            }
        }
        elements.add(text);
    }

    // non-JAXB method
    public String getText() { 
        if (elements == null)
            return null;
        StringBuilder sb = new StringBuilder();
        for (Object obj : elements) {
            if (obj instanceof String)
                sb.append((String) obj);
        }
        return sb.toString();
    }

    // non-JAXB method
    public void setMetadatas(Iterable <MailCustomMetadata> metadatas) {
        if (elements == null)
            elements = Lists.newArrayList();
        // note that remove in foreach would be unsafe.
        for (int ndx = elements.size()-1; ndx >= 0; ndx--) {
            if (elements.get(ndx) instanceof MailCustomMetadata) {
                elements.remove(ndx);
            }
        }
        if (metadatas != null) {
            Iterables.addAll(this.elements, metadatas);
        }
    }

    // non-JAXB method
    public void addMetadata(MailCustomMetadata metadata) {
        this.elements.add(metadata);
    }

    // non-JAXB method
    public List<MailCustomMetadata> getMetadatas() {
        List<MailCustomMetadata> metadatas = Lists.newArrayList();
        for (Object obj : elements) {
            if (obj instanceof MailCustomMetadata)
                metadatas.add((MailCustomMetadata) obj);
        }
        return Collections.unmodifiableList(metadatas);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("parentId", parentId)
            .add("id", id)
            .add("creatorEmail", creatorEmail)
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("color", color)
            .add("rgb", rgb)
            .add("date", date)
            .add("elements", elements);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
