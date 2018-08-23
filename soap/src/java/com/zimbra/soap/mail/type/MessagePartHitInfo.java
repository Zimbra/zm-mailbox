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
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;
import com.zimbra.soap.type.SearchHit;

@XmlAccessorType(XmlAccessType.NONE)
public class MessagePartHitInfo implements SearchHit {

    /**
     * @zm-api-field-tag message-id
     * @zm-api-field-description Message ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag sort-field-value
     * @zm-api-field-description Sort field value
     */
    @XmlAttribute(name=MailConstants.A_SORT_FIELD /* sf */, required=false)
    private String sortField;

    /**
     * @zm-api-field-tag size
     * @zm-api-field-description Size in bytes
     */
    @XmlAttribute(name=MailConstants.A_SIZE /* s */, required=false)
    private Long size;

    /**
     * @zm-api-field-tag date
     * @zm-api-field-description Secs since epoch, from date header in message
     */
    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    /**
     * @zm-api-field-tag conv-id
     * @zm-api-field-description Converstation id. only present if <b>&lt;m></b> is not enclosed within a
     * <b>&lt;c></b> element
     */
    @XmlAttribute(name=MailConstants.A_CONV_ID /* cid */, required=false)
    private Integer conversationId;

    /**
     * @zm-api-field-tag message-item-id
     * @zm-api-field-description Message item ID
     */
    @XmlAttribute(name=MailConstants.A_MESSAGE_ID /* mid */, required=false)
    private Integer messageId;

    /**
     * @zm-api-field-tag content-type
     * @zm-api-field-description Content type
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=false)
    private String contentType;

    /**
     * @zm-api-field-tag filename
     * @zm-api-field-description Filename
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_NAME /* name */, required=false)
    private String contentName;

    /**
     * @zm-api-field-tag mime-part-name
     * @zm-api-field-description MIME part name
     */
    @XmlAttribute(name=MailConstants.A_PART /* part */, required=false)
    private String part;

    /**
     * @zm-api-field-description Email address information
     */
    @XmlElement(name=MailConstants.E_EMAIL /* e */, required=false)
    private EmailInfo email;

    /**
     * @zm-api-field-tag subject
     * @zm-api-field-description Subject
     */
    @ZimbraJsonAttribute
    @XmlElement(name=MailConstants.E_SUBJECT /* su */, required=false)
    private String subject;

    public MessagePartHitInfo() {
    }

    @Override
    public void setId(String id) { this.id = id; }
    @Override
    public void setSortField(String sortField) { this.sortField = sortField; }
    public void setSize(Long size) { this.size = size; }
    public void setDate(Long date) { this.date = date; }
    public void setConversationId(Integer conversationId) {
        this.conversationId = conversationId;
    }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    public void setContentName(String contentName) {
        this.contentName = contentName;
    }
    public void setPart(String part) { this.part = part; }
    public void setEmail(EmailInfo email) { this.email = email; }
    public void setSubject(String subject) { this.subject = subject; }
    @Override
    public String getId() { return id; }
    @Override
    public String getSortField() { return sortField; }
    public Long getSize() { return size; }
    public Long getDate() { return date; }
    public Integer getConversationId() { return conversationId; }
    public Integer getMessageId() { return messageId; }
    public String getContentType() { return contentType; }
    public String getContentName() { return contentName; }
    public String getPart() { return part; }
    public EmailInfo getEmail() { return email; }
    public String getSubject() { return subject; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("sortField", sortField)
            .add("size", size)
            .add("date", date)
            .add("conversationId", conversationId)
            .add("messageId", messageId)
            .add("contentType", contentType)
            .add("contentName", contentName)
            .add("part", part)
            .add("email", email)
            .add("subject", subject);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
