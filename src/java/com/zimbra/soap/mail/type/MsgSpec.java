/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
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
     * <b>&lt;content></b> when raw is set.  (default is unset, meaning no limit.)
     */
    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH /* max */, required=false)
    private Integer maxInlinedLength;

    /**
     * @zm-api-field-tag want-defanged-html
     * @zm-api-field-description Set to return defanged HTML content by default.  (default is unset.)
     */
    @XmlAttribute(name=MailConstants.A_WANT_HTML /* html */, required=false)
    private ZmBoolean wantHtml;

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
     * @zm-api-field-description if <b>&lt;header></b>s are requested, any matching headers are inlined into the
     * response (not available when raw is set)
     */
    @XmlElement(name=MailConstants.A_HEADER /* header */, required=false)
    private List<AttributeName> headers = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MsgSpec() {
        this((String) null);
    }

    public MsgSpec(String id) {
        this.id = id;
    }

    public void setPart(String part) { this.part = part; }
    public void setRaw(Boolean raw) { this.raw = ZmBoolean.fromBool(raw); }
    public void setMarkRead(Boolean markRead) { this.markRead = ZmBoolean.fromBool(markRead); }
    public void setMaxInlinedLength(Integer maxInlinedLength) {
        this.maxInlinedLength = maxInlinedLength;
    }
    public void setWantHtml(Boolean wantHtml) { this.wantHtml = ZmBoolean.fromBool(wantHtml); }
    public void setNeuter(Boolean neuter) { this.neuter = ZmBoolean.fromBool(neuter); }
    public void setRecurIdZ(String recurIdZ) { this.recurIdZ = recurIdZ; }
    public void setNeedCanExpand(Boolean needCanExpand) { this.needCanExpand = ZmBoolean.fromBool(needCanExpand); }
    public void setHeaders(Iterable <AttributeName> headers) {
        this.headers.clear();
        if (headers != null) {
            Iterables.addAll(this.headers,headers);
        }
    }

    public void addHeader(AttributeName header) {
        this.headers.add(header);
    }

    public String getId() { return id; }
    public String getPart() { return part; }
    public Boolean getRaw() { return ZmBoolean.toBool(raw); }
    public Boolean getMarkRead() { return ZmBoolean.toBool(markRead); }
    public Integer getMaxInlinedLength() { return maxInlinedLength; }
    public Boolean getWantHtml() { return ZmBoolean.toBool(wantHtml); }
    public Boolean getNeuter() { return ZmBoolean.toBool(neuter); }
    public String getRecurIdZ() { return recurIdZ; }
    public Boolean getNeedCanExpand() { return ZmBoolean.toBool(needCanExpand); }
    public List<AttributeName> getHeaders() {
        return Collections.unmodifiableList(headers);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("part", part)
            .add("raw", raw)
            .add("markRead", markRead)
            .add("maxInlinedLength", maxInlinedLength)
            .add("wantHtml", wantHtml)
            .add("neuter", neuter)
            .add("recurIdZ", recurIdZ)
            .add("needCanExpand", needCanExpand)
            .add("headers", headers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
