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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.calendar.Attach;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class CalendarAttach {

    @XmlAttribute(name=MailConstants.A_CAL_ATTACH_URI /* uri */, required=false)
    private String uri;

    @XmlAttribute(name=MailConstants.A_CAL_ATTACH_CONTENT_TYPE /* ct */, required=false)
    private String contentType;

    @XmlValue
    private String binaryB64Data;

    public CalendarAttach() {
    }

    public CalendarAttach(Attach att) {
        this.uri = att.getUri();
        if (this.uri != null) {
            this.contentType = att.getContentType();
        } else {
            this.binaryB64Data = att.getBinaryB64Data();
        }
    }

    public void setUri(String uri) { this.uri = uri; }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setBinaryB64Data(String binaryB64Data) {
        this.binaryB64Data = binaryB64Data;
    }

    public String getUri() { return uri; }
    public String getContentType() { return contentType; }
    public String getBinaryB64Data() { return binaryB64Data; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("uri", uri)
            .add("contentType", contentType)
            .add("binaryB64Data", binaryB64Data);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
