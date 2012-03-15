/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="content")
public class Content {

    /**
     * @zm-api-field-tag attachment-upload-id
     * @zm-api-field-description Attachment upload ID of uploaded object to use
     */
    @XmlAttribute(required=false, name="aid") private String attachUploadId;

    /**
     * @zm-api-field-tag inlined-content
     * @zm-api-field-description Inlined content data.  Ignored if "aid" is specified
     */
    @XmlValue private String value;

    public Content() {
    }

    public String getAttachUploadId() {
        return attachUploadId;
    }

    public void setAttachUploadId(String attachUploadId) {
        this.attachUploadId = attachUploadId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
