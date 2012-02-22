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
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

// See ParseMimeMessage.handleAttachments

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_DOC)
public class DocAttachSpec
extends AttachSpec {

    /**
     * @zm-api-field-tag document-path
     * @zm-api-field-description Document path.  If specified "id" and "ver" attributes are ignored
     */
    @XmlAttribute(name=MailConstants.A_PATH /* path */, required=false)
    private String path;

    /**
     * @zm-api-field-tag item-id
     * @zm-api-field-description Item ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag version
     * @zm-api-field-description Optional Version.
     */
    @XmlAttribute(name=MailConstants.A_VERSION /* ver */, required=false)
    private Integer version;

    public DocAttachSpec() {
    }

    public void setPath(String path) { this.path = path; }
    public void setId(String id) { this.id = id; }
    public void setVersion(Integer version) { this.version = version; }
    public String getPath() { return path; }
    public String getId() { return id; }
    public Integer getVersion() { return version; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("path", path)
            .add("id", id)
            .add("version", version);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
