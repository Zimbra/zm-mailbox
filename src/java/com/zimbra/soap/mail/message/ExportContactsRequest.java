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

package com.zimbra.soap.mail.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import com.zimbra.common.soap.MailConstants;
/*
<ExportContactsRequest ct="{content-type}"
    [l="{folder-id}"] [csvfmt="{csv-format}"]
    [csvlocale="{csv-locale}"] [csvsep="{csv-delimiter}"]>
</ExportContactsRequest>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="ExportContactsRequest")
@XmlType(propOrder = {})
public class ExportContactsRequest {

    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE, required=true) private String contentType;
    @XmlAttribute(name=MailConstants.A_FOLDER, required=false) private String folderId;
    @XmlAttribute(name=MailConstants.A_CSVFORMAT, required=false) private String csvFormat;
    @XmlAttribute(name=MailConstants.A_CSVLOCALE, required=false) private String csvLocale;
    @XmlAttribute(name=MailConstants.A_CSVSEPARATOR, required=false) private String csvDelimiter;
    public ExportContactsRequest() {
    }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public String getContentType() {
        return contentType;
    }
    public void setFolderId(String folderId) {
        this.folderId = folderId;
    }
    public String getFolderId() {
        return folderId;
    }
    public void setCsvFormat(String csvFormat) {
        this.csvFormat = csvFormat;
    }
    public String getCsvFormat() {
        return csvFormat;
    }
    public void setCsvLocale(String csvLocale) {
        this.csvLocale = csvLocale;
    }
    public String getCsvLocale() {
        return csvLocale;
    }
    public void setCsvDelimiter(String csvDelimiter) {
        this.csvDelimiter = csvDelimiter;
    }
    public String getCsvDelimiter() {
        return csvDelimiter;
    }
}
