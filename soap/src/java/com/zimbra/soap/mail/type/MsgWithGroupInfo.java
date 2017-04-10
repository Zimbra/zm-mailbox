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

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;
import com.zimbra.soap.type.KeyValuePair;
import com.zimbra.soap.type.UrlAndValue;

@XmlAccessorType(XmlAccessType.NONE)
public class MsgWithGroupInfo extends MessageCommon {

    /**
     * @zm-api-field-tag msg-id
     * @zm-api-field-description Message ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag imap-uid
     * @zm-api-field-description IMAP UID
     */
    @XmlAttribute(name=MailConstants.A_IMAP_UID /* i4uid */, required=false)
    private Integer imapUid;

    /**
     * @zm-api-field-tag X-Zimbra-Calendar-Intended-For
     * @zm-api-field-description X-Zimbra-Calendar-Intended-For header
     */
    @XmlAttribute(name=MailConstants.A_CAL_INTENDED_FOR /* cif */, required=false)
    private String calendarIntendedFor;

    /**
     * @zm-api-field-tag orig-id
     * @zm-api-field-description Message id of the message being replied to/forwarded (outbound messages only)
     */
    @XmlAttribute(name=MailConstants.A_ORIG_ID /* origid */, required=false)
    private String origId;

    /**
     * @zm-api-field-tag reply-type
     * @zm-api-field-description Reply type - <b>r|w</b>
     */
    @XmlAttribute(name=MailConstants.A_REPLY_TYPE /* rt */, required=false)
    private String draftReplyType;

    /**
     * @zm-api-field-tag identity-id
     * @zm-api-field-description If set, this specifies the identity being used to compose the message
     */
    @XmlAttribute(name=MailConstants.A_IDENTITY_ID /* idnt */, required=false)
    private String identityId;

    /**
     * @zm-api-field-tag draft-acct-id
     * @zm-api-field-description Draft account ID
     */
    @XmlAttribute(name=MailConstants.A_FOR_ACCOUNT /* forAcct */, required=false)
    private String draftAccountId;

    /**
     * @zm-api-field-tag auto-send-time
     * @zm-api-field-description Can optionally set this to specify the time at which the draft should be
     * automatically sent by the server
     */
    @XmlAttribute(name=MailConstants.A_AUTO_SEND_TIME /* autoSendTime */, required=false)
    private Long draftAutoSendTime;

    /**
     * @zm-api-field-tag date-header
     * @zm-api-field-description Date header
     */
    @XmlAttribute(name=MailConstants.A_SENT_DATE /* sd */, required=false)
    private Long sentDate;

    /**
     * @zm-api-field-tag resent-date
     * @zm-api-field-description Resent date
     */
    @XmlAttribute(name=MailConstants.A_RESENT_DATE /* rd */, required=false)
    private Long resentDate;

    /**
     * @zm-api-field-tag part
     * @zm-api-field-description Part
     */
    @XmlAttribute(name=MailConstants.A_PART /* part */, required=false)
    private String part;

    /**
     * @zm-api-field-tag fragment
     * @zm-api-field-description First few bytes of the message (probably between 40 and 100 bytes)
     */
    @ZimbraJsonAttribute
    @XmlElement(name=MailConstants.E_FRAG /* fr */, required=false)
    private String fragment;

    /**
     * @zm-api-field-description Email addresses
     */
    @XmlElement(name=MailConstants.E_EMAIL /* e */, required=false)
    private final List<EmailInfo> emails = Lists.newArrayList();

    /**
     * @zm-api-field-tag msg-subject
     * @zm-api-field-description Subject
     */
    @XmlElement(name=MailConstants.E_SUBJECT /* su */, required=false)
    private String subject;

    /**
     * @zm-api-field-tag message-id
     * @zm-api-field-description Message ID
     */
    @XmlElement(name=MailConstants.E_MSG_ID_HDR /* mid */, required=false)
    private String messageIdHeader;

    /**
     * @zm-api-field-tag in-reply-to-msg-id
     * @zm-api-field-description Message-ID header for message being replied to
     */
    @XmlElement(name=MailConstants.E_IN_REPLY_TO /* irt */, required=false)
    private String inReplyTo;

    /**
     * @zm-api-field-description Parsed out iCalendar invite
     */
    @XmlElement(name=MailConstants.E_INVITE /* inv */, required=false)
    private InviteWithGroupInfo invite;

    /**
     * @zm-api-field-description Headers
     */
    @XmlElement(name=MailConstants.A_HEADER /* header */, required=false)
    private final List<KeyValuePair> headers = Lists.newArrayList();

    /**
     * @zm-api-field-description Content elements
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_MIMEPART /* mp */, type=PartInfo.class),
        @XmlElement(name=MailConstants.E_SHARE_NOTIFICATION /* shr */, type=ShareNotification.class),
        @XmlElement(name=MailConstants.E_DL_SUBSCRIPTION_NOTIFICATION /* dlSubs */, type=DLSubscriptionNotification.class)
    })
    private final List<Object> contentElems = Lists.newArrayList();

    /**
     * @zm-api-field-description Content
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private UrlAndValue content;

    public MsgWithGroupInfo() {
    }

    public void setId(String id) { this.id = id; }
    public void setImapUid(Integer imapUid) { this.imapUid = imapUid; }
    public void setCalendarIntendedFor(String calendarIntendedFor) {
        this.calendarIntendedFor = calendarIntendedFor;
    }
    public void setOrigId(String origId) { this.origId = origId; }
    public void setDraftReplyType(String draftReplyType) {
        this.draftReplyType = draftReplyType;
    }
    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }
    public void setDraftAccountId(String draftAccountId) {
        this.draftAccountId = draftAccountId;
    }
    public void setDraftAutoSendTime(Long draftAutoSendTime) {
        this.draftAutoSendTime = draftAutoSendTime;
    }
    public void setSentDate(Long sentDate) { this.sentDate = sentDate; }
    public void setResentDate(Long resentDate) { this.resentDate = resentDate; }
    public void setPart(String part) { this.part = part; }
    public void setFragment(String fragment) { this.fragment = fragment; }
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
    public void setInReplyTo(String inReplyTo) { this.inReplyTo = inReplyTo; }
    public void setInvite(InviteWithGroupInfo invite) { this.invite = invite; }
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

    public void setContent(UrlAndValue content) { this.content = content; }
    public String getId() { return id; }
    public Integer getImapUid() { return imapUid; }
    public String getCalendarIntendedFor() { return calendarIntendedFor; }
    public String getOrigId() { return origId; }
    public String getDraftReplyType() { return draftReplyType; }
    public String getIdentityId() { return identityId; }
    public String getDraftAccountId() { return draftAccountId; }
    public Long getDraftAutoSendTime() { return draftAutoSendTime; }
    public Long getSentDate() { return sentDate; }
    public Long getResentDate() { return resentDate; }
    public String getPart() { return part; }
    public String getFragment() { return fragment; }
    public List<EmailInfo> getEmails() {
        return Collections.unmodifiableList(emails);
    }
    public String getSubject() { return subject; }
    public String getMessageIdHeader() { return messageIdHeader; }
    public String getInReplyTo() { return inReplyTo; }
    public InviteWithGroupInfo getInvite() { return invite; }
    public List<KeyValuePair> getHeaders() {
        return Collections.unmodifiableList(headers);
    }
    public List<Object> getContentElems() {
        return Collections.unmodifiableList(contentElems);
    }
    public UrlAndValue getContent() { return content; }

    @Override
    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("imapUid", imapUid)
            .add("calendarIntendedFor", calendarIntendedFor)
            .add("origId", origId)
            .add("draftReplyType", draftReplyType)
            .add("identityId", identityId)
            .add("draftAccountId", draftAccountId)
            .add("draftAutoSendTime", draftAutoSendTime)
            .add("sentDate", sentDate)
            .add("resentDate", resentDate)
            .add("part", part)
            .add("fragment", fragment)
            .add("emails", emails)
            .add("subject", subject)
            .add("messageIdHeader", messageIdHeader)
            .add("inReplyTo", inReplyTo)
            .add("invite", invite)
            .add("headers", headers)
            .add("contentElems", contentElems)
            .add("content", content);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
