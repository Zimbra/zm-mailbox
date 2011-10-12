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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlSeeAlso({
    NoteActionSelector.class,
    ContactActionSelector.class,
    FolderActionSelector.class
})
public class ActionSelector {

    // Comma separated list
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    protected final String ids;

    @XmlAttribute(name=MailConstants.A_OPERATION /* op */, required=true)
    protected final String operation;

    @XmlAttribute(name=MailConstants.A_TARGET_CONSTRAINT /* tcon */, required=false)
    protected String constraint;

    // Deprecated, use tagNames instead
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAG /* tag */, required=false)
    protected Integer tag;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    protected String folder;

    @XmlAttribute(name=MailConstants.A_RGB /* rgb */, required=false)
    protected String rgb;

    @XmlAttribute(name=MailConstants.A_COLOR /* color */, required=false)
    protected Byte color;

    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=false)
    protected String name;

    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    protected String flags;

    // comma separated list of integers.  Deprecated, use tagNames instead
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    protected String tags;

    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    protected String tagNames;

    /**
     * no-argument constructor wanted by JAXB
     */
    protected ActionSelector() {
        this((String) null, (String) null);
    }

    public ActionSelector(String ids, String operation) {
        this.ids = ids;
        this.operation = operation;
    }

    public static ActionSelector createForIdsAndOperation(String ids, String operation) {
        return new ActionSelector(ids, operation);
    }

    public void setConstraint(String constraint) { this.constraint = constraint; }

    /**
     * Use {@link ActionSelector#setTagNames(String)} instead.
     */
    @Deprecated
    public void setTag(Integer tag) { this.tag = tag; }
    public void setFolder(String folder) { this.folder = folder; }
    public void setRgb(String rgb) { this.rgb = rgb; }
    public void setColor(Byte color) { this.color = color; }
    public void setName(String name) { this.name = name; }
    public void setFlags(String flags) { this.flags = flags; }
    /**
     * Use {@link ActionSelector#setTagNames(String)} instead.
     */
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }

    public String getIds() { return ids; }
    public String getOperation() { return operation; }
    public String getConstraint() { return constraint; }

    /**
     * Use {@link ActionSelector#getTagNames()} instead.
     */
    @Deprecated
    public Integer getTag() { return tag; }
    public String getFolder() { return folder; }
    public String getRgb() { return rgb; }
    public Byte getColor() { return color; }
    public String getName() { return name; }
    public String getFlags() { return flags; }
    /**
     * Use {@link ActionSelector#getTagNames()} instead.
     */
    @Deprecated
    public String getTags() { return tags; }
    public String getTagNames() { return tagNames; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("ids", ids)
            .add("operation", operation)
            .add("constraint", constraint)
            .add("tag", tag)
            .add("folder", folder)
            .add("rgb", rgb)
            .add("color", color)
            .add("name", name)
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
