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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class ContactAttr extends Attr {

    // part/contentType/size/contentFilename are required when
    // encoding attachments

    @XmlAttribute(name=MailConstants.A_PART, required=false)
    private String part;

    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE, required=false)
    private String contentType;

    @XmlAttribute(name=MailConstants.A_SIZE, required=false)
    private int size;

    @XmlAttribute(name=MailConstants.A_CONTENT_FILENAME, required=false)
    private String contentFilename;

    public ContactAttr() {
    }

    public void setPart(String part) { this.part = part; }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setSize(int size) { this.size = size; }
    public void setContentFilename(String contentFilename) {
        this.contentFilename = contentFilename;
    }

    public String getPart() { return part; }
    public String getContentType() { return contentType; }
    public int getSize() { return size; }
    public String getContentFilename() { return contentFilename; }
}
