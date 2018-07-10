/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("contentServletRelativeUrl", contentServletRelativeUrl)
            .add("contentType", contentType);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
