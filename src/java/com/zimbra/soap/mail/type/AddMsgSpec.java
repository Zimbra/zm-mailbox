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

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class AddMsgSpec {

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags - (u)nread, (f)lagged, has (a)ttachment, (r)eplied, (s)ent by me, for(w)arded,
     * (d)raft, deleted (x), (n)otification sent
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
     * @zm-api-field-tag folder-id-or-path
     * @zm-api-field-description Folder pathname (starts with '/') or folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folder;

    // TODO: even if noICal is true, calendar should still link message to existing appointment
    //       (by UID) if the appointment already exists.
    /**
     * @zm-api-field-tag no-ical
     * @zm-api-field-description If set, then don't process iCal attachments.  Default is unset.
     */
    @XmlAttribute(name=MailConstants.A_NO_ICAL /* noICal */, required=false)
    private ZmBoolean noICal;

    /**
     * @zm-api-field-tag received-date
     * @zm-api-field-description (optional) Time the message was originally received, in MILLISECONDS since the epoch
     */
    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private String dateReceived;

    /**
     * @zm-api-field-tag uploaded-MIME-body-ID
     * @zm-api-field-description Uploaded MIME body ID - ID of message uploaded via FileUploadServlet
     */
    @XmlAttribute(name=MailConstants.A_ATTACHMENT_ID /* aid */, required=false)
    private String attachmentId;

    /**
     * @zm-api-field-tag content
     * @zm-api-field-description The entire message's content.  (Omit if you specify an "aid" attribute.)
     * <br />
     * No <b>&lt;mp></b> elements should be provided within <b>&lt;m></b>.
     */
    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private String content;

    public AddMsgSpec() {
    }

    public void setFlags(String flags) { this.flags = flags; }
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public void setFolder(String folder) { this.folder = folder; }
    public void setNoICal(Boolean noICal) { this.noICal = ZmBoolean.fromBool(noICal); }
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
    public Boolean getNoICal() { return ZmBoolean.toBool(noICal); }
    public String getDateReceived() { return dateReceived; }
    public String getAttachmentId() { return attachmentId; }
    public String getContent() { return content; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
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
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
