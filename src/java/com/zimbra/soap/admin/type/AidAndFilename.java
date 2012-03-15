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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.CertMgrConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class AidAndFilename {

    /**
     * @zm-api-field-tag attachment-id
     * @zm-api-field-description Attachment ID
     */
    @XmlAttribute(name=AdminConstants.A_ATTACHMENT_ID /* aid */, required=false)
    private String attachmentId;

    /**
     * @zm-api-field-tag filename
     * @zm-api-field-description Filename
     */
    @XmlAttribute(name=CertMgrConstants.A_FILENAME /* filename */, required=false)
    private String filename;

    public AidAndFilename() {
    }

    public AidAndFilename(String attachmentId, String filename) {
        this.setAttachmentId(attachmentId);
        this.setFilename(filename);
    }

    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }

    public void setFilename(String filename) { this.filename = filename; }
    public String getAttachmentId() { return attachmentId; }
    public String getFilename() { return filename; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("attachmentId", attachmentId)
            .add("filename", filename);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
