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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ContentSpec;

/**
 * @zm-api-command-description Import appointments
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_IMPORT_APPOINTMENTS_REQUEST)
public class ImportAppointmentsRequest {

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Optional folder ID to import appointments into
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag content-type
     * @zm-api-field-description Content type
     * <br />
     * Only currently supported content type is "text/calendar" (and its nickname "ics")
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=true)
    private final String contentType;

    /**
     * @zm-api-field-description Content specification
     */
    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=true)
    private final ContentSpec content;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ImportAppointmentsRequest() {
        this((String) null, (ContentSpec) null);
    }

    public ImportAppointmentsRequest(String contentType, ContentSpec content) {
        this.contentType = contentType;
        this.content = content;
    }

    public void setFolderId(String folderId) { this.folderId = folderId; }
    public String getFolderId() { return folderId; }
    public String getContentType() { return contentType; }
    public ContentSpec getContent() { return content; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("folderId", folderId)
            .add("contentType", contentType)
            .add("content", content);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
