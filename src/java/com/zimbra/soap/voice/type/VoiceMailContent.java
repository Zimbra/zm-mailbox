/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceMailContent {

    /**
     * @zm-api-field-tag url
     * @zm-api-field-description Content servlet relative url for retrieving message content.  This url will retrieve
     * the binary voice content.
     */
    @XmlAttribute(name=MailConstants.A_URL /* url */, required=true)
    private String contentServletRelativeUrl;

    /**
     * @zm-api-field-tag content-type
     * @zm-api-field-description Content type
     */
    @XmlAttribute(name=VoiceConstants.A_CONTENT_TYPE /* ct */, required=true)
    private String contentType;

    public VoiceMailContent() {
    }

    public void setContentServletRelativeUrl(String contentServletRelativeUrl) {
        this.contentServletRelativeUrl = contentServletRelativeUrl;
    }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContentServletRelativeUrl() { return contentServletRelativeUrl; }
    public String getContentType() { return contentType; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("contentServletRelativeUrl", contentServletRelativeUrl)
            .add("contentType", contentType);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
