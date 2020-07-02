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
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

import com.zimbra.common.gql.GqlConstants;
import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"content", "headers", "mimePart", "attachments", "invite",
           "emailAddresses", "timezones", "fragment", "extraElements"})
@GraphQLType(name=GqlConstants.CLASS_MESSAGE, description="A message")
public class Msg {

    /**
     * @zm-api-field-tag uploaded-MIME-body-ID
     * @zm-api-field-description Uploaded MIME body ID
     */
    @XmlAttribute(name=MailConstants.A_ATTACHMENT_ID /* aid */, required=false)
    private String attachmentId;

    /**
     * @zm-api-field-tag orig-id
     * @zm-api-field-description Original ID
     */
    @XmlAttribute(name=MailConstants.A_ORIG_ID /* origid */, required=false)
    private String origId;

    /**
     * @zm-api-field-tag reply-type-r|w
     * @zm-api-field-description Reply type - <b>r|w</b>.  (r)eplied or for(w)arded.
     */
    @XmlAttribute(name=MailConstants.A_REPLY_TYPE /* rt */, required=false)
    private String replyType;

    /**
     * @zm-api-field-tag identity-id
     * @zm-api-field-description Identity ID.  The identity referenced by <b>{identity-id}</b> specifies the folder
     * where the sent message is saved.
     */
    @XmlAttribute(name=MailConstants.A_IDENTITY_ID /* idnt */, required=false)
    private String identityId;

    /**
     * @zm-api-field-tag subject
     * @zm-api-field-description Subject
     */
    @XmlAttribute(name=MailConstants.E_SUBJECT /* su */, required=false)
    private String subject;

    /**
     * @zm-api-field-description Headers
     */
    @XmlElement(name=MailConstants.E_HEADER /* header */, required=false)
    private List<Header> headers;

    /**
     * @zm-api-field-tag in-reply-to-message-id-hdr
     * @zm-api-field-description Message-ID header for message being replied to
     */
    @XmlAttribute(name=MailConstants.E_IN_REPLY_TO /* irt */, required=false)
    private String inReplyTo;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-tag content
     * @zm-api-field-description Content
     */
    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private String content;

    /**
     * @zm-api-field-description Mime part information
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_MIMEPART /* mp */, required=false)
    private MimePartInfo mimePart;

    /**
     * @zm-api-field-description Attachments information
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_ATTACH /* attach */, required=false)
    private AttachmentsInfo attachments;

    /**
     * @zm-api-field-description Invite information
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_INVITE /* inv */, required=false)
    private InvitationInfo invite;

    /**
     * @zm-api-field-description Email address information
     */
    @XmlElement(name=MailConstants.E_EMAIL /* e */, required=false)
    @GraphQLQuery(name=GqlConstants.EMAILADDRESSES, description="Email address information")
    private final List<EmailAddrInfo> emailAddresses = Lists.newArrayList();

    // ParseMimeMessage.parseMimeMsgSoap looks for E_CAL_TZ but does no further processing.
    // CalendarUtils.parseInviteElementCommon looks for timezones in parent element
    /**
     * @zm-api-field-description Timezones
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    @GraphQLInputField(name=GqlConstants.TIMEZONES, description="Timezones")
    private final List<CalTZInfo> timezones = Lists.newArrayList();

    // ParseMimeMessage.parseMimeMsgSoap looks for E_FRAG but does nothing with it.
    /**
     * @zm-api-field-tag fragment
     * @zm-api-field-description First few bytes of the message (probably between 40 and 100 bytes)
     */
    @ZimbraJsonAttribute
    @XmlElement(name=MailConstants.E_FRAG /* fr */, required=false)
    private String fragment;

    // ParseMimeMessage.parseMimeMsgSoap loops over all children.
    /**
     * @zm-api-field-description Other elements
     */
    @XmlAnyElement
    @GraphQLIgnore
    private final List<org.w3c.dom.Element> extraElements = Lists.newArrayList();

    public Msg() {
    }

    @GraphQLInputField(name=GqlConstants.ATTACHMENT_ID, description="Uploaded MIME body ID")
    public void setAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
    }
    @GraphQLInputField(name=GqlConstants.ORIGINAL_ID, description="Original ID")
    public void setOrigId(String origId) { this.origId = origId; }
    @GraphQLInputField(name=GqlConstants.REPLY_TYPE, description="Reply type - r|w. (r)eplied or for(w)arded.")
    public void setReplyType(String replyType) { this.replyType = replyType; }
    @GraphQLInputField(name=GqlConstants.IDENTITY_ID, description="The identity referenced by {identity-id} specifies the folder where the sent message is saved.")
    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }
    @GraphQLInputField(name=GqlConstants.SUBJECT, description="Subject")
    public void setSubject(String subject) { this.subject = subject; }
    @GraphQLInputField(name=GqlConstants.HEADERS, description="Headers")
    public void setHeaders(List<Header> headers) { this.headers = headers; }
    @GraphQLInputField(name=GqlConstants.IN_REPLY_TO, description="Message-ID header for message being replied to")
    public void setInReplyTo(String inReplyTo) { this.inReplyTo = inReplyTo; }
    @GraphQLInputField(name=GqlConstants.FOLDER_ID, description="Folder ID")
    public void setFolderId(String folderId) { this.folderId = folderId; }
    @GraphQLInputField(name=GqlConstants.FLAGS, description="Flags")
    public void setFlags(String flags) { this.flags = flags; }
    @GraphQLInputField(name=GqlConstants.CONTENT, description="Content")
    public void setContent(String content) { this.content = content; }
    @GraphQLInputField(name=GqlConstants.MIME_PART, description="Mime part information")
    public void setMimePart(MimePartInfo mimePart) { this.mimePart = mimePart; }
    @GraphQLInputField(name=GqlConstants.ATTACHMENTS, description="Attachments information")
    public void setAttachments(AttachmentsInfo attachments) {
        this.attachments = attachments;
    }
    @GraphQLInputField(name=GqlConstants.INVITE, description="Invite information")
    public void setInvite(InvitationInfo invite) { this.invite = invite; }
    @GraphQLInputField(name=GqlConstants.EMAILADDRESSES, description="Email address information")
    public void setEmailAddresses(Iterable <EmailAddrInfo> emailAddresses) {
        this.emailAddresses.clear();
        if (emailAddresses != null) {
            Iterables.addAll(this.emailAddresses,emailAddresses);
        }
    }
    @GraphQLIgnore
    public void addEmailAddress(EmailAddrInfo emailAddress) {
        this.emailAddresses.add(emailAddress);
    }
    @GraphQLInputField(name=GqlConstants.TIMEZONES, description="Timezones")
    public void setTimezones(Iterable <CalTZInfo> timezones) {
        this.timezones.clear();
        if (timezones != null) {
            Iterables.addAll(this.timezones,timezones);
        }
    }
    @GraphQLIgnore
    public void addTimezone(CalTZInfo timezone) {
        this.timezones.add(timezone);
    }
    @GraphQLInputField(name=GqlConstants.FRAGMENT, description="First few bytes of the message (probably between 40 and 100 bytes)")
    public void setFragment(String fragment) { this.fragment = fragment; }
    @GraphQLIgnore
    public void setExtraElements(Iterable <org.w3c.dom.Element> extraElements) {
        this.extraElements.clear();
        if (extraElements != null) {
            Iterables.addAll(this.extraElements,extraElements);
        }
    }
    @GraphQLIgnore
    public void addExtraElement(org.w3c.dom.Element extraElement) {
        this.extraElements.add(extraElement);
    }

    @GraphQLQuery(name=GqlConstants.ATTACHMENT_ID, description="Uploaded MIME body ID")
    public String getAttachmentId() { return attachmentId; }
    @GraphQLQuery(name=GqlConstants.ORIGINAL_ID, description="Original ID")
    public String getOrigId() { return origId; }
    @GraphQLQuery(name=GqlConstants.REPLY_TYPE, description="Reply type - r|w. (r)eplied or for(w)arded.")
    public String getReplyType() { return replyType; }
    @GraphQLQuery(name=GqlConstants.IDENTITY_ID, description="The identity referenced by {identity-id} specifies the folder where the sent message is saved.")
    public String getIdentityId() { return identityId; }
    @GraphQLQuery(name=GqlConstants.SUBJECT, description="Subject")
    public String getSubject() { return subject; }
    @GraphQLQuery(name=GqlConstants.HEADERS, description="Headers")
    public List<Header> getHeaders() { return headers; }
    @GraphQLQuery(name=GqlConstants.IN_REPLY_TO, description="Message-ID header for message being replied to")
    public String getInReplyTo() { return inReplyTo; }
    @GraphQLQuery(name=GqlConstants.FOLDER_ID, description="Folder ID")
    public String getFolderId() { return folderId; }
    @GraphQLQuery(name=GqlConstants.FLAGS, description="Flags")
    public String getFlags() { return flags; }
    @GraphQLQuery(name=GqlConstants.CONTENT, description="Content")
    public String getContent() { return content; }
    @GraphQLQuery(name=GqlConstants.MIME_PART, description="Mime part information")
    public MimePartInfo getMimePart() { return mimePart; }
    @GraphQLQuery(name=GqlConstants.ATTACHMENTS, description="Attachments information")
    public AttachmentsInfo getAttachments() { return attachments; }
    @GraphQLQuery(name=GqlConstants.INVITE, description="Invite information")
    public InvitationInfo getInvite() { return invite; }
    @GraphQLQuery(name=GqlConstants.EMAILADDRESSES, description="Email address information")
    public List<EmailAddrInfo> getEmailAddresses() {
        return Collections.unmodifiableList(emailAddresses);
    }
    @GraphQLQuery(name=GqlConstants.TIMEZONES, description="Timezones")
    public List<CalTZInfo> getTimezones() {
        return Collections.unmodifiableList(timezones);
    }
    @GraphQLQuery(name=GqlConstants.FRAGMENT, description="First few bytes of the message (probably between 40 and 100 bytes)")
    public String getFragment() { return fragment; }
    @GraphQLIgnore
    public List<org.w3c.dom.Element> getExtraElements() {
        return Collections.unmodifiableList(extraElements);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("attachmentId", attachmentId)
            .add("origId", origId)
            .add("replyType", replyType)
            .add("identityId", identityId)
            .add("subject", subject)
            .add("inReplyTo", inReplyTo)
            .add("folderId", folderId)
            .add("flags", flags)
            .add("content", content)
            .add("mimePart", mimePart)
            .add("attachments", attachments)
            .add("invite", invite)
            .add("emailAddresses", emailAddresses)
            .add("timezones", timezones)
            .add("fragment", fragment)
            .add("extraElements", extraElements);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

    @GraphQLType(name=GqlConstants.CLASS_HEADER, description="Header information")
    public static final class Header {
        /**
         * @zm-api-field-tag header-name
         * @zm-api-field-description Header name
         */
        @XmlAttribute(name=MailConstants.A_NAME)
        @GraphQLQuery(name=GqlConstants.NAME, description="Header name") 
        private String name;

        /**
         * @zm-api-field-tag header-value
         * @zm-api-field-description Header value
         */
        @XmlValue
        @GraphQLQuery(name=GqlConstants.VALUE, description="Header value")
        private String value;

        public Header() {
        }

        public Header(
            @GraphQLInputField(name=GqlConstants.NAME) String name,
            @GraphQLInputField(name=GqlConstants.VALUE) String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }
}
