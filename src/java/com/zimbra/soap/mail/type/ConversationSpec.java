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

@XmlAccessorType(XmlAccessType.FIELD)
public class ConversationSpec {

    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private final String id;

    // Values related to SearchParams.ExpandResults but case insensitive
    @XmlAttribute(name=MailConstants.A_FETCH, required=false)
    private String inlineRule;

    @XmlAttribute(name=MailConstants.A_WANT_HTML, required=false)
    private ZmBoolean wantHtml;

    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH, required=false)
    private Integer maxInlinedLength;

    @XmlElement(name=MailConstants.A_HEADER, required=false)
    private List<AttributeName> headers = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ConversationSpec() {
        this((String) null);
    }

    public ConversationSpec(String id) {
        this.id = id;
    }

    public void setInlineRule(String inlineRule) {
        this.inlineRule = inlineRule;
    }

    public void setWantHtml(Boolean wantHtml) { this.wantHtml = ZmBoolean.fromBool(wantHtml); }
    public void setMaxInlinedLength(Integer maxInlinedLength) {
        this.maxInlinedLength = maxInlinedLength;
    }

    public void setHeaders(Iterable <AttributeName> headers) {
        this.headers.clear();
        if (headers != null) {
            Iterables.addAll(this.headers,headers);
        }
    }

    public ConversationSpec addHeader(AttributeName header) {
        this.headers.add(header);
        return this;
    }

    public String getId() { return id; }
    public String getInlineRule() { return inlineRule; }
    public Boolean getWantHtml() { return ZmBoolean.toBool(wantHtml); }
    public Integer getMaxInlinedLength() { return maxInlinedLength; }
    public List<AttributeName> getHeaders() {
        return Collections.unmodifiableList(headers);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("id", id)
            .add("inlineRule", inlineRule)
            .add("wantHtml", wantHtml)
            .add("maxInlinedLength", maxInlinedLength)
            .add("headers", headers)
            .toString();
    }
}
