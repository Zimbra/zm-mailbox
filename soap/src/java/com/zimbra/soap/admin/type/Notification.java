/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.NotificationInterface;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class Notification
implements NotificationInterface {

    /**
     * @zm-api-field-tag truncated-flag
     * @zm-api-field-description Truncated flag
     */
    @XmlAttribute(name=MailConstants.A_TRUNCATED_CONTENT /* truncated */, required=false)
    private ZmBoolean truncatedContent;

    /**
     * @zm-api-field-tag content
     * @zm-api-field-description Content
     */
    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private String content;

    public Notification() {
    }

    @Override
    public void setTruncatedContent(Boolean truncatedContent) {
        this.truncatedContent = ZmBoolean.fromBool(truncatedContent);
    }
    @Override
    public void setContent(String content) { this.content = content; }

    @Override
    public Boolean getTruncatedContent() { return ZmBoolean.toBool(truncatedContent); }
    @Override
    public String getContent() { return content; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("truncatedContent", truncatedContent)
            .add("content", content)
            .toString();
    }
}
