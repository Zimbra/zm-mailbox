/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"content", "inviteComponent", "timezones", "mimeParts", "attachments"})
@GraphQLType(name=GqlConstants.CLASS_INVITATION_INFO, description="Invitation Information")
public class InvitationInfo extends InviteComponent {

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag content-type
     * @zm-api-field-description MIME Content-Type
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_TYPE /* ct */, required=false)
    private String contentType;

    /**
     * @zm-api-field-tag content-id
     * @zm-api-field-description MIME Content-Id
     */
    @XmlAttribute(name=MailConstants.A_CONTENT_ID /* ci */, required=false)
    private String contentId;

    /**
     * @zm-api-field-tag raw-rfc822-msg
     * @zm-api-field-description RAW RFC822 MESSAGE (XML-encoded) <b>MUST CONTAIN A text/calendar PART</b>
     */
    @XmlElement(name=MailConstants.E_CONTENT /* content */, required=false)
    private RawInvite content;

    /**
     * @zm-api-field-description Invite component
     */
    @XmlElement(name=MailConstants.E_INVITE_COMPONENT /* comp */, required=false)
    private InviteComponent inviteComponent;

    /**
     * @zm-api-field-description Timezones
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    @GraphQLQuery(name=GqlConstants.TIMEZONES, description="Timezones")
    private final List<CalTZInfo> timezones = Lists.newArrayList();

    /**
     * @zm-api-field-description Meeting notes parts
     */
    @XmlElement(name=MailConstants.E_MIMEPART /* mp */, required=false)
    @GraphQLQuery(name=GqlConstants.MIME_PARTS, description="Meeting notes parts")
    private final List<MimePartInfo> mimeParts = Lists.newArrayList();

    /**
     * @zm-api-field-description Attachments
     */
    @XmlElement(name=MailConstants.E_ATTACH /* attach */, required=false)
    private AttachmentsInfo attachments;

    public InvitationInfo() {
        this((String) null, -1, false);
    }

    public InvitationInfo(String method, int componentNum, boolean rsvp) {
        super(method, componentNum, rsvp);
    }

    public static InvitationInfo create(InviteComponent comp) {
        final InvitationInfo ii = new InvitationInfo();
        ii.setInviteComponent(comp);
        return ii;
    }

    @GraphQLInputField(name=GqlConstants.INVITE_ID, description="ID")
    public void setId(String id) { this.id = id; }
    @GraphQLInputField(name=GqlConstants.CONTENT_TYPE, description="MIME Content-Type")
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    @GraphQLInputField(name=GqlConstants.CONTENT_ID, description="MIME Content-Id")
    public void setContentId(String contentId) {
        this.contentId = contentId;
    }
    @GraphQLInputField(name=GqlConstants.CONTENT, description="RAW RFC822 MESSAGE (XML-encoded) MUST CONTAIN A text/calendar PART")
    public void setContent(RawInvite content) { this.content = content; }
    @GraphQLInputField(name=GqlConstants.CLASS_INVITE_COMPONENT, description="Invite component")
    public void setInviteComponent(InviteComponent inviteComponent) {
        this.inviteComponent = inviteComponent;
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
    @GraphQLInputField(name=GqlConstants.MIME_PARTS, description="Meeting notes parts")
    public void setMimeParts(Iterable <MimePartInfo> mimeParts) {
        this.mimeParts.clear();
        if (mimeParts != null) {
            Iterables.addAll(this.mimeParts,mimeParts);
        }
    }
    @GraphQLIgnore
    public void addMimePart(MimePartInfo mimePart) {
        this.mimeParts.add(mimePart);
    }
    @GraphQLInputField(name=GqlConstants.ATTACHMENTS, description="Attachments")
    public void setAttachments(AttachmentsInfo attachments) {
        this.attachments = attachments;
    }
    @GraphQLQuery(name=GqlConstants.INVITE_ID, description="ID")
    public String getId() { return id; }
    @GraphQLQuery(name=GqlConstants.CONTENT_TYPE, description="MIME Content-Type")
    public String getContentType() { return contentType; }
    @GraphQLQuery(name=GqlConstants.CONTENT_ID, description="MIME Content-Id")
    public String getContentId() { return contentId; }
    @GraphQLQuery(name=GqlConstants.CONTENT, description="RAW RFC822 MESSAGE (XML-encoded) MUST CONTAIN A text/calendar PART")
    public RawInvite getContent() { return content; }
    @GraphQLQuery(name=GqlConstants.CLASS_INVITE_COMPONENT, description="Invite component")
    public InviteComponent getInviteComponent() { return inviteComponent; }
    @GraphQLQuery(name=GqlConstants.TIMEZONES, description="Timezones")
    public List<CalTZInfo> getTimezones() {
        return Collections.unmodifiableList(timezones);
    }
    @GraphQLQuery(name=GqlConstants.MIME_PARTS, description="Meeting notes parts")
    public List<MimePartInfo> getMimeParts() {
        return Collections.unmodifiableList(mimeParts);
    }
    @GraphQLQuery(name=GqlConstants.ATTACHMENTS, description="Attachments")
    public AttachmentsInfo getAttachments() { return attachments; }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("contentType", contentType)
            .add("contentId", contentId)
            .add("content", content)
            .add("inviteComponent", inviteComponent)
            .add("timezones", timezones)
            .add("mimeParts", mimeParts)
            .add("attachments", attachments);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
