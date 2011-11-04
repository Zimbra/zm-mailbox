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
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class MsgSpec {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_PART /* part */, required=false)
    private String part;

    @XmlAttribute(name=MailConstants.A_RAW /* raw */, required=false)
    private ZmBoolean raw;

    @XmlAttribute(name=MailConstants.A_MARK_READ /* read */, required=false)
    private ZmBoolean markRead;

    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH /* max */, required=false)
    private Integer maxInlinedLength;

    @XmlAttribute(name=MailConstants.A_WANT_HTML /* html */, required=false)
    private ZmBoolean wantHtml;

    @XmlAttribute(name=MailConstants.A_NEUTER /* neuter */, required=false)
    private ZmBoolean neuter;

    @XmlAttribute(name=MailConstants.A_CAL_RECURRENCE_ID_Z /* ridZ */, required=false)
    private String recurIdZ;

    @XmlAttribute(name=MailConstants.A_NEED_EXP /* needExp */, required=false)
    private ZmBoolean needCanExpand;

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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
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
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
