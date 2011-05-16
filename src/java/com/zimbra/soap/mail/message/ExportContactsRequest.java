/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_EXPORT_CONTACTS_REQUEST)
public class ExportContactsRequest {

    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE, required=true)
    private final String contentType;

    @XmlAttribute(name=MailConstants.A_FOLDER, required=false)
    private String folderId;

    @XmlAttribute(name=MailConstants.A_CSVFORMAT, required=false)
    private String csvFormat;

    @XmlAttribute(name=MailConstants.A_CSVLOCALE, required=false)
    private String csvLocale;

    @XmlAttribute(name=MailConstants.A_CSVSEPARATOR, required=false)
    private String csvDelimiter;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ExportContactsRequest() {
        this((String) null);
    }

    public ExportContactsRequest(String contentType) {
        this.contentType = contentType;
    }

    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setCsvFormat(String csvFormat) { this.csvFormat = csvFormat; }
    public void setCsvLocale(String csvLocale) { this.csvLocale = csvLocale; }
    public void setCsvDelimiter(String csvDelimiter) {
        this.csvDelimiter = csvDelimiter;
    }
    public String getContentType() { return contentType; }
    public String getFolderId() { return folderId; }
    public String getCsvFormat() { return csvFormat; }
    public String getCsvLocale() { return csvLocale; }
    public String getCsvDelimiter() { return csvDelimiter; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("contentType", contentType)
            .add("folderId", folderId)
            .add("csvFormat", csvFormat)
            .add("csvLocale", csvLocale)
            .add("csvDelimiter", csvDelimiter);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
