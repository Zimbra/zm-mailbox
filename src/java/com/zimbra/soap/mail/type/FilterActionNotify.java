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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class FilterActionNotify extends FilterAction {

    @XmlAttribute(name=MailConstants.A_ADDRESS, required=false)
    private String address;

    @XmlAttribute(name=MailConstants.A_SUBJECT, required=false)
    private String subject;

    @XmlAttribute(name=MailConstants.A_MAX_BODY_SIZE, required=false)
    private Integer maxBodySize;

    @XmlElement(name=MailConstants.E_CONTENT, required=false)
    private String content;

    @XmlAttribute(name=MailConstants.A_ORIG_HEADERS, required=false)
    private String origHeaders;

    public FilterActionNotify() {
    }

    public void setAddress(String address) { this.address = address; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setMaxBodySize(Integer maxBodySize) {
        this.maxBodySize = maxBodySize;
    }
    public void setContent(String content) { this.content = content; }
    public void setOrigHeaders(String origHeaders) { this.origHeaders = origHeaders; }
    public String getAddress() { return address; }
    public String getSubject() { return subject; }
    public Integer getMaxBodySize() { return maxBodySize; }
    public String getContent() { return content; }
    public String getOrigHeaders() { return origHeaders; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("address", address)
            .add("subject", subject)
            .add("maxBodySize", maxBodySize)
            .add("content", content)
            .add("origHeaders", origHeaders)
            .toString();
    }
}
