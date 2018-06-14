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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_MIMEPART)
public class MimePartAttachSpec extends AttachSpec {

    /**
     * @zm-api-field-tag message-id
     * @zm-api-field-description Message ID
     */
    @XmlAttribute(name=MailConstants.A_MESSAGE_ID, required=true)
    private final String messageId;

    /**
     * @zm-api-field-tag part
     * @zm-api-field-description Part
     */
    @XmlAttribute(name=MailConstants.A_PART, required=true)
    private final String part;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MimePartAttachSpec() {
        this((String) null, (String) null);
    }

    public MimePartAttachSpec(String messageId, String part) {
        this.messageId = messageId;
        this.part = part;
    }

    public String getMessageId() { return messageId; }
    public String getPart() { return part; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("messageId", messageId)
            .add("part", part);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
