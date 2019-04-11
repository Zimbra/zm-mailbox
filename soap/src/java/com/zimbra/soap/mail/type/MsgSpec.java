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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.MsgContent;
import com.zimbra.soap.type.ZmBoolean;

import com.zimbra.common.gql.GqlConstants;
import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_MESSAGE_SPECIFICATION, description="Message Specifications.")
public class MsgSpec {

    /**
     * @zm-api-field-tag message-id
     * @zm-api-field-description Message ID.  Can contain a subpart identifier (e.g. "775-778") to return a message
     * stored as a subpart of some other mail-item, specifically for Messages stored as part of Appointments
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag msg-part
     * @zm-api-field-description Supply a "part" and the retrieved data will be on the specified message/rfc822
     * subpart.  If the part does not exist or is not a message/rfc822 part, mail.NO_SUCH_PART MailServiceException
     * will be thrown
     */
    @XmlAttribute(name=MailConstants.A_PART /* part */, required=false)
    private String part;

    /**
     * @zm-api-field-tag want-raw-msg-content
     * @zm-api-field-description Set to return the raw message content rather than a parsed mime structure;
     * (default is unset.  if message is too big or not ASCII, a content servlet URL is returned)
     */
    @XmlAttribute(name=MailConstants.A_RAW /* raw */, required=false)
    private ZmBoolean raw;

    /**
     * @zm-api-field-tag mark-read
     * @zm-api-field-description Set to mark the message as read, unset to leave the read status unchanged.
     * By default, the read status is left unchanged.
     */
    @XmlAttribute(name=MailConstants.A_MARK_READ /* read */, required=false)
    private ZmBoolean markRead;

    /**
     * @zm-api-field-tag max-inlined-length
     * @zm-api-field-description Use <b>{max-inlined-length}</b> to limit the length of the text inlined into body
     * <b>&lt;content></b>.
     * <br/>Only applicable when <b>raw</b> is unset.  <b>Ignored</b> when <b>raw</b> is set.
     * <br/>(Default is unset, meaning no limit)
     */
    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH /* max */, required=false)
    private Integer maxInlinedLength;

    /**
     * @zm-api-field-tag use-content-url
     * @zm-api-field-description If set, never inline raw <b>&lt;content></b> for messages, specify by <b>"url"</b> instead.
     * <br/>Only applicable when <b>raw</b> is set.  <b>Ignored</b> when <b>raw</b> is unset.
     * <br/>(Default is unset - meaning inline content unless it is too big, in which case the <b>"url"</b> method
     * will be used)
     */
    @XmlAttribute(name=MailConstants.A_USE_CONTENT_URL/* useContentUrl */, required=false)
    private ZmBoolean useContentUrl;

    /**
     * @zm-api-field-tag want-defanged-html
     * @zm-api-field-description Set to return defanged HTML content by default.  (default is unset.)
     */
    @XmlAttribute(name=MailConstants.A_WANT_HTML /* html */, required=false)
    private ZmBoolean wantHtml;

    /**
     * @zm-api-field-tag want-imap-uid
     * @zm-api-field-description Set to return IMAP UID.  (default is unset.)
     */
    @XmlAttribute(name=MailConstants.A_WANT_IMAP_UID /* wantImapUid */, required=false)
    private ZmBoolean wantImapUid;

    /**
     * @zm-api-field-tag want-modified-sequence
     * @zm-api-field-description Set to return Modified Sequence.  (default is unset.)
     */
    @XmlAttribute(name=MailConstants.A_WANT_MODIFIED_SEQUENCE /* wantModSeq */, required=false)
    private ZmBoolean wantModifiedSequence;

    /**
     * @zm-api-field-tag neuter-img-tags
     * @zm-api-field-description Set to "neuter" <b>&lt;IMG></b> tags returned in HTML content; this involves
     * switching the <b>"src"</b> attribute to <b>"dfsrc"</b> so that images don't display by default (default is set.)
     */
    @XmlAttribute(name=MailConstants.A_NEUTER /* neuter */, required=false)
    private ZmBoolean neuter;

    /**
     * @zm-api-field-tag recurrence-id-YYYYMMDD[ThhmmssZ]
     * @zm-api-field-description Recurrence ID in format <b>YYYYMMDD[ThhmmssZ]</b>.  Used only when making GetMsg call
     * to open an instance of a recurring appointment.  The value specified is the date/time data of the
     * RECURRENCE-ID of the instance being requested.
     */
    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID_Z /* ridZ */, required=false)
    private String recurIdZ;

    /**
     * @zm-api-field-tag need-can-expand
     * @zm-api-field-description Set to return group info (isGroup and exp flags) on <b>&lt;e></b> elements in the
     * response (default is unset.)
     */
    @XmlAttribute(name=MailConstants.A_NEED_EXP /* needExp */, required=false)
    private ZmBoolean needCanExpand;

    /**
     * @zm-api-field-tag want-content
     * @zm-api-field-description wantContent = <b>"full"</b> to get the complete message along with the quoted content
     * <br/> wantContent = <b>"original"</b> to get the message without quoted content
     * <br/> wantContent = <b>"both"</b> to get complete message as well as the message without quoted content
     * <br/> By default wantContent = <b>"full"</b>
     * <br/>Only applicable when <b>raw</b> is unset.
     * <p>Note: Quoted text identification is a best effort. It is not supported by any RFCs</p>
     */
    @XmlAttribute(name=MailConstants.A_WANT_CONTENT  /* wantContent */ , required=false)
    private MsgContent wantContent;

    /**
     * @zm-api-field-description if <b>&lt;header></b>s are requested, any matching headers are inlined into the
     * response (not available when raw is set)
     */
    @XmlElement(name=MailConstants.A_HEADER /* header */, required=false)
    private final List<AttributeName> headers = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MsgSpec() {
        this((String) null);
    }

    public MsgSpec(@GraphQLNonNull @GraphQLInputField String id) {
        this.id = id;
    }

    @GraphQLInputField(name=GqlConstants.MESSAGE_PART, description="Set the part or message or rfc822 part.")
    public void setPart(String part) { this.part = part; }

    @GraphQLInputField(name=GqlConstants.INCLUDE_RAW_MESSAGE, description="Set to return the raw message content rather than a parsed mime structure.")
    public void setRaw(Boolean raw) { this.raw = ZmBoolean.fromBool(raw); }

    @GraphQLInputField(name=GqlConstants.MARK_MESSAGE_AS_READ, description="Set to true to mark the message as read.")
    public void setMarkRead(Boolean markRead) { this.markRead = ZmBoolean.fromBool(markRead); }

    @GraphQLInputField(name=GqlConstants.INLINED_TEXT_LENGTH, description="Set the length of the text inlined into body.")
    public void setMaxInlinedLength(Integer maxInlinedLength) {
        this.maxInlinedLength = maxInlinedLength;
    }

    @GraphQLInputField(name=GqlConstants.INCLUDE_URL_CONTENT, description="Set the content to raw or url method.")
    public void setUseContentUrl(Boolean useUrl) { this.useContentUrl = ZmBoolean.fromBool(useUrl); }

    @GraphQLInputField(name=GqlConstants.INCLUDE_HTML, description="Set to return defanged html content.")
    public void setWantHtml(Boolean wantHtml) { this.wantHtml = ZmBoolean.fromBool(wantHtml); }

    @GraphQLInputField(name=GqlConstants.INCLUDE_IMAP_UID, description="Set to return the IMAP UID.")
    public void setWantImapUid(Boolean wantImapUid) { this.wantImapUid = ZmBoolean.fromBool(wantImapUid); }

    @GraphQLInputField(name=GqlConstants.INCLUDE_MODIFIED_SEQUENCE, description="Set to return modified sequence.")
    public void setWantModifiedSequence(Boolean wantModSeq) { this.wantModifiedSequence = ZmBoolean.fromBool(wantModSeq); }

    @GraphQLInputField(name=GqlConstants.HIDE_IMAGES, description="Set to not display images by default.")
    public void setNeuter(Boolean neuter) { this.neuter = ZmBoolean.fromBool(neuter); }

    @GraphQLInputField(name=GqlConstants.RECURRENCE_DATE_ID, description="Set the date-time data to open an instance of a recurring appointment.")
    public void setRecurIdZ(String recurIdZ) { this.recurIdZ = recurIdZ; }

    @GraphQLInputField(name=GqlConstants.CONTENT_TYPE, description="Set the message content type.")
    public void setWantContent(MsgContent msgContent) { this.wantContent = msgContent; }

    @GraphQLInputField(name=GqlConstants.INCLUDE_GROUP_INFO, description="Set to return group info in the response ")
    public void setNeedCanExpand(Boolean needCanExpand) { this.needCanExpand = ZmBoolean.fromBool(needCanExpand); }

    @GraphQLInputField(name=GqlConstants.HEADERS, description="Set the headers to add.")
    public void setHeaders(Iterable <AttributeName> headers) {
        this.headers.clear();
        if (headers != null) {
            Iterables.addAll(this.headers,headers);
        }
    }

    @GraphQLIgnore
    public void addHeader(AttributeName header) {
        this.headers.add(header);
    }

    public String getId() { return id; }
    public String getPart() { return part; }
    public Boolean getRaw() { return ZmBoolean.toBool(raw); }
    public Boolean getMarkRead() { return ZmBoolean.toBool(markRead); }
    public Integer getMaxInlinedLength() { return maxInlinedLength; }
    public Boolean getUseContentUrl() { return ZmBoolean.toBool(useContentUrl); }
    public Boolean getWantHtml() { return ZmBoolean.toBool(wantHtml); }
    public Boolean getWantImapUid() { return ZmBoolean.toBool(wantImapUid, false); }
    public Boolean getWantModifiedSequence() { return ZmBoolean.toBool(wantModifiedSequence, false); }
    public Boolean getNeuter() { return ZmBoolean.toBool(neuter); }
    public String getRecurIdZ() { return recurIdZ; }
    public MsgContent getWantContent() { return wantContent; }
    public Boolean getNeedCanExpand() { return ZmBoolean.toBool(needCanExpand); }
    public List<AttributeName> getHeaders() {
        return Collections.unmodifiableList(headers);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("part", part)
            .add("raw", raw)
            .add("markRead", markRead)
            .add("maxInlinedLength", maxInlinedLength)
            .add("useContentUrl", useContentUrl)
            .add("wantHtml", wantHtml)
            .add("wantImapUid", wantImapUid)
            .add("wantModSeq", wantModifiedSequence)
            .add("neuter", neuter)
            .add("recurIdZ", recurIdZ)
            .add("needCanExpand", needCanExpand)
            .add("wantContent", wantContent)
            .add("headers", headers);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
