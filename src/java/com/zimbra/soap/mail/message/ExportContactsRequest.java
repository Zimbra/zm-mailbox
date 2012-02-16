/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

/**
 * @zm-api-command-description Export contacts
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_EXPORT_CONTACTS_REQUEST)
public class ExportContactsRequest {

    /**
     * @zm-api-field-tag content-type
     * @zm-api-field-description Content type.  Currently, the only supported content type is "csv"
     * (comma-separated values)
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=true)
    private final String contentType;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Optional folder id to export contacts from
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag csv-format
     * @zm-api-field-description Optional csv format for exported contacts.  the supported formats are defined in
     * <b>$ZIMBRA_HOME/conf/zimbra-contact-fields.xml</b>
     */
    @XmlAttribute(name=MailConstants.A_CSVFORMAT /* csvfmt */, required=false)
    private String csvFormat;

    /**
     * @zm-api-field-tag csv-locale
     * @zm-api-field-description The locale to use when there are multiple <b>{csv-format}</b> locales defined.
     * When it is not specified, the <b>{csv-format}</b> with no locale specification is used.
     */
    @XmlAttribute(name=MailConstants.A_CSVLOCALE /* csvlocale */, required=false)
    private String csvLocale;

    /**
     * @zm-api-field-tag csv-delimiter
     * @zm-api-field-description Optional delimiter character to use in the resulting csv file - usually "," or ";"
     */
    @XmlAttribute(name=MailConstants.A_CSVSEPARATOR /* csvsep */, required=false)
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
    public void setCsvDelimiter(String csvDelimiter) { this.csvDelimiter = csvDelimiter; }
    public String getContentType() { return contentType; }
    public String getFolderId() { return folderId; }
    public String getCsvFormat() { return csvFormat; }
    public String getCsvLocale() { return csvLocale; }
    public String getCsvDelimiter() { return csvDelimiter; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("contentType", contentType)
            .add("folderId", folderId)
            .add("csvFormat", csvFormat)
            .add("csvLocale", csvLocale)
            .add("csvDelimiter", csvDelimiter);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
