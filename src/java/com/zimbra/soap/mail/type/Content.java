/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2012, 2013, 2014, 2016 Synacor, Inc.
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
