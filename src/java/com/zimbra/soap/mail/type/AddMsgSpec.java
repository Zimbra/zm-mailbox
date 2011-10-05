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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class AddMsgSpec {

    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    // Id or path
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folder;

    // If true then don't process ICAL attachments
    @XmlAttribute(name=MailConstants.A_NO_ICAL /* noICal */, required=false)
    private Boolean noICal;

    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private String dateReceived;

    @XmlAttribute(name=MailConstants.A_ATTACHMENT_ID /* aid */, required=false)
    private String attachmentId;

    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private String content;

    public AddMsgSpec() {
    }

    public void setFlags(String flags) { this.flags = flags; }
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public void setFolder(String folder) { this.folder = folder; }
    public void setNoICal(Boolean noICal) { this.noICal = noICal; }
    public void setDateReceived(String dateReceived) {
        this.dateReceived = dateReceived;
    }
    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }
    public void setContent(String content) { this.content = content; }
    public String getFlags() { return flags; }
    @Deprecated
    public String getTags() { return tags; }
    public String getTagNames() { return tagNames; }
    public String getFolder() { return folder; }
    public Boolean getNoICal() { return noICal; }
    public String getDateReceived() { return dateReceived; }
    public String getAttachmentId() { return attachmentId; }
    public String getContent() { return content; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("folder", folder)
            .add("noICal", noICal)
            .add("dateReceived", dateReceived)
            .add("attachmentId", attachmentId)
            .add("content", content);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
