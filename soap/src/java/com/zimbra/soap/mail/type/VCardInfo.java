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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.base.MoreObjects;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_VCARD_INFO, description="Input for creating a new contact VCard")
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

    @GraphQLInputField(name=GqlConstants.MESSAGE_ID, description="Message ID. Use in conjuction with part")
    public void setMessageId(String messageId) { this.messageId = messageId; }
    @GraphQLInputField(name=GqlConstants.ATTACHMENT_ID, description="Uploaded attachment ID")
    public void setAttachId(String attachId) { this.attachId = attachId; }
    @GraphQLInputField(name=GqlConstants.PART, description="Part identifier. Use in conjunction with message id")
    public void setPart(String part) { this.part = part; }
    @GraphQLInputField(name=GqlConstants.VALUE, description="VCARD data")
    public void setValue(String value) { this.value = value; }
    public String getMessageId() { return messageId; }
    public String getAttachId() { return attachId; }
    public String getPart() { return part; }
    public String getValue() { return value; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("messageId", messageId)
            .add("attachId", attachId)
            .add("part", part)
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
