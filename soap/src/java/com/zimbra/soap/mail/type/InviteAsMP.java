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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;

// See mail.ToXML.encodeInviteAsMP

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"emails", "subject", "messageIdHeader", "invite", "headers", "contentElems"})
public class InviteAsMP extends MessageCommon {

    /**
     * @zm-api-field-tag sub-part-id
     * @zm-api-field-description Sub-part ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag
     * @zm-api-field-description If non-null, this message/rfc822 subpart of the specified Message is serialized
     * instead of the Message itself.
     * 
     */
    @XmlAttribute(name=MailConstants.A_PART /* part */, required=false)
    private String part;

    /**
     * @zm-api-field-tag sent-date
     * @zm-api-field-description Sent date
     */
    @XmlAttribute(name=MailConstants.A_SENT_DATE /* sd */, required=false)
    private Long sentDate;

    /**
     * @zm-api-field-description Email addresses
     */
    @XmlElement(name=MailConstants.E_EMAIL /* e */, required=false)
    private List<EmailInfo> emails = Lists.newArrayList();

    /**
     * @zm-api-field-tag subject
     * @zm-api-field-description Subject
     */
    @ZimbraJsonAttribute
    @XmlElement(name=MailConstants.E_SUBJECT /* su */, required=false)
    private String subject;

    /**
     * @zm-api-field-tag msg-id-header
     * @zm-api-field-description Message ID header
     */
    @ZimbraJsonAttribute
    @XmlElement(name=MailConstants.E_MSG_ID_HDR /* mid */, required=false)
    private String messageIdHeader;

    /**
     * @zm-api-field-description Invite
     */
    @XmlElement(name=MailConstants.E_INVITE /* inv */, required=false)
    private MPInviteInfo invite;

    /**
     * @zm-api-field-description Headers
     */
    @XmlElement(name=MailConstants.A_HEADER /* header */, required=false)
    private List<KeyValuePair> headers = Lists.newArrayList();

    /**
     * @zm-api-field-description Mime parts, share notifications and distribution list subscription notifications
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_MIMEPART /* mp */, type=PartInfo.class),
        @XmlElement(name=MailConstants.E_SHARE_NOTIFICATION /* shr */, type=ShareNotification.class),
        @XmlElement(name=MailConstants.E_DL_SUBSCRIPTION_NOTIFICATION /* dlSubs */, type=DLSubscriptionNotification.class)
    })
    private List<Object> contentElems = Lists.newArrayList();

    public InviteAsMP() {
    }

    public void setId(String id) { this.id = id; }
    public void setPart(String part) { this.part = part; }
    public void setSentDate(Long sentDate) { this.sentDate = sentDate; }
    public void setEmails(Iterable <EmailInfo> emails) {
        this.emails.clear();
        if (emails != null) {
            Iterables.addAll(this.emails,emails);
        }
    }

    public void addEmail(EmailInfo email) {
        this.emails.add(email);
    }

    public void setSubject(String subject) { this.subject = subject; }
    public void setMessageIdHeader(String messageIdHeader) {
        this.messageIdHeader = messageIdHeader;
    }
    public void setInvite(MPInviteInfo invite) { this.invite = invite; }
    public void setHeaders(Iterable <KeyValuePair> headers) {
        this.headers.clear();
        if (headers != null) {
            Iterables.addAll(this.headers,headers);
        }
    }

    public void addHeader(KeyValuePair header) {
        this.headers.add(header);
    }

    public void setContentElems(Iterable <Object> contentElems) {
        this.contentElems.clear();
        if (contentElems != null) {
            Iterables.addAll(this.contentElems,contentElems);
        }
    }

    public void addContentElem(Object contentElem) {
        this.contentElems.add(contentElem);
    }

    public String getId() { return id; }
    public String getPart() { return part; }
    public Long getSentDate() { return sentDate; }
    public List<EmailInfo> getEmails() {
        return Collections.unmodifiableList(emails);
    }
    public String getSubject() { return subject; }
    public String getMessageIdHeader() { return messageIdHeader; }
    public MPInviteInfo getInvite() { return invite; }
    public List<KeyValuePair> getHeaders() {
        return Collections.unmodifiableList(headers);
    }
    public List<Object> getContentElems() {
        return Collections.unmodifiableList(contentElems);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("part", part)
            .add("sentDate", sentDate)
            .add("emails", emails)
            .add("subject", subject)
            .add("messageIdHeader", messageIdHeader)
            .add("invite", invite)
            .add("headers", headers)
            .add("contentElems", contentElems);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
