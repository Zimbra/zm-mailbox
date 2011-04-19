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
public class FilterTestAddressBook extends FilterTestInfo {

    @XmlAttribute(name=MailConstants.A_HEADER, required=false)
    private String header;

    @XmlAttribute(name=MailConstants.A_FOLDER_PATH, required=false)
    private String folderPath;

    public FilterTestAddressBook() {
    }

    public void setHeader(String header) { this.header = header; }
    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }
    public String getHeader() { return header; }
    public String getFolderPath() { return folderPath; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("header", header)
            .add("folderPath", folderPath)
            .toString();
    }
}
