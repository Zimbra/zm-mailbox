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

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class CommonCalendaringData extends InstanceDataAttrs {

    @XmlAttribute(name="x_uid", required=true)
    private final String xUid;

    @XmlAttribute(name=MailConstants.A_UID, required=true)
    private final String uid;

    @XmlAttribute(name=MailConstants.A_FLAGS, required=false)
    private String flags;

    @XmlAttribute(name=MailConstants.A_TAGS, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_FOLDER, required=false)
    private String folderId;

    @XmlAttribute(name=MailConstants.A_SIZE, required=false)
    private long size;

    @XmlAttribute(name=MailConstants.A_CHANGE_DATE, required=false)
    private long changeDate;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE, required=false)
    private int modifiedSequence;

    @XmlAttribute(name=MailConstants.A_REVISION, required=false)
    private int revision;

    @XmlAttribute(name=MailConstants.A_ID, required=false)
    private String id;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    protected CommonCalendaringData() {
        this((String) null, (String) null);
    }

    public CommonCalendaringData(String xUid, String uid) {
        this.xUid = xUid;
        this.uid = uid;
    }

    public void setFlags(String flags) { this.flags = flags; }
    public void setTags(String tags) { this.tags = tags; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setSize(long size) { this.size = size; }
    public void setChangeDate(long changeDate) { this.changeDate = changeDate; }
    public void setModifiedSequence(int modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setRevision(int revision) { this.revision = revision; }
    public void setId(String id) { this.id = id; }
    public String getXUid() { return xUid; }
    public String getUid() { return uid; }
    public String getFlags() { return flags; }
    public String getTags() { return tags; }
    public String getFolderId() { return folderId; }
    public long getSize() { return size; }
    public long getChangeDate() { return changeDate; }
    public int getModifiedSequence() { return modifiedSequence; }
    public int getRevision() { return revision; }
    public String getId() { return id; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("xUid", xUid)
            .add("uid", uid)
            .add("flags", flags)
            .add("tags", tags)
            .add("folderId", folderId)
            .add("size", size)
            .add("changeDate", changeDate)
            .add("modifiedSequence", modifiedSequence)
            .add("revision", revision)
            .add("id", id);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
