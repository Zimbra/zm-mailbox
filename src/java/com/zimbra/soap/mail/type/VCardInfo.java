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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VCardInfo {

    /**
     * @zm-api-field-tag message-id
     * @zm-api-field-description Message ID.  Use in conjunction with <b>{part-identifier}</b>
     */
    @XmlAttribute(name=MailConstants.A_MESSAGE_ID /* mid */, required=false)
    private String messageId;

    /**
     * @zm-api-field-tag part-identifier
     * @zm-api-field-description Part identifier.  Use in conjunction with <b>{message-id}</b>
     */
    @XmlAttribute(name=MailConstants.A_PART /* part */, required=false)
    private String part;

    /**
     * @zm-api-field-tag uploaded-attachment-id
     * @zm-api-field-description Uploaded attachment ID
     */
    @XmlAttribute(name=MailConstants.A_ATTACHMENT_ID /* aid */, required=false)
    private String attachId;

    /**
     * @zm-api-field-tag vcard-data
     * @zm-api-field-description inlined VCARD data
     */
    @XmlValue
    private String value;

    public VCardInfo() {
    }

    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setAttachId(String attachId) { this.attachId = attachId; }
    public void setPart(String part) { this.part = part; }
    public void setValue(String value) { this.value = value; }
    public String getMessageId() { return messageId; }
    public String getAttachId() { return attachId; }
    public String getPart() { return part; }
    public String getValue() { return value; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("messageId", messageId)
            .add("attachId", attachId)
            .add("part", part)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
