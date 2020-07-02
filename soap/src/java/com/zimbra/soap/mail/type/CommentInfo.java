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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlMixed;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CommentInfo {

    /**
     * @zm-api-field-tag item-id-of-parent
     * @zm-api-field-description Item ID of parent
     */
    @XmlAttribute(name=MailConstants.A_PARENT_ID /* parentId */, required=false)
    private String parentId;

    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Item ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag uuid
     * @zm-api-field-description Item's UUID - a globally unique identifier
     */
    @XmlAttribute(name=MailConstants.A_UUID /* uuid */, required=false)
    private String uuid;

    /**
     * @zm-api-field-tag creator-email-address
     * @zm-api-field-description Creator email address
     */
    @XmlAttribute(name=MailConstants.A_EMAIL /* email */, required=false)
    private String creatorEmail;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-tag tags
     * @zm-api-field-description Tags - Comma separated list of integers.  DEPRECATED - use "tn" instead
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Comma-separated list of tag names
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    /**
     * @zm-api-field-tag color
     * @zm-api-field-description color numeric; range 0-127; defaults to 0 if not present; client can display only 0-7
     */
    @XmlAttribute(name=MailConstants.A_COLOR /* color */, required=false)
    private Byte color;

    /**
     * @zm-api-field-tag rgb-color
     * @zm-api-field-description RGB color in format #rrggbb where r,g and b are hex digits
     */
    @XmlAttribute(name=MailConstants.A_RGB /* rgb */, required=false)
    private String rgb;

    /**
     * @zm-api-field-tag timestamp
     * @zm-api-field-description Timestamp
     */
    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    // Need for @XmlMixed is forced because allow text content (used for
    // the Subject text) as well as sub-elements
    /**
     * @zm-api-field-description metadata and the subject as text
     */
    @XmlElementRefs({
        @XmlElementRef(/* meta */ type=MailCustomMetadata.class)
    })
    @XmlMixed
    private List<Object> elements = Lists.newArrayList();

    public CommentInfo() {
    }

    public void setParentId(String parentId) { this.parentId = parentId; }
    public void setId(String id) { this.id = id; }
    public void setUuid(String uuid) { this.uuid = uuid; }
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
    public String getUuid() { return uuid; }
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("parentId", parentId)
            .add("id", id)
            .add("uuid", uuid)
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
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
