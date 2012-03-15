/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
public class ConversationSpec {

    /**
     * @zm-api-field-tag conv-id
     * @zm-api-field-description Conversation ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    // Values related to SearchParams.ExpandResults but case insensitive
    /**
     * @zm-api-field-tag fetch-1|all|{item-id}
     * @zm-api-field-description if value is "1" or "all" the full expanded message structure is inlined for the
     * first (or for all) messages in the conversation.
     * <br />
     * If fetch="{item-id}", only the message with the given {item-id} is expanded inline
     */
    @XmlAttribute(name=MailConstants.A_FETCH /* fetch */, required=false)
    private String inlineRule;

    /**
     * @zm-api-field-tag want-html
     * @zm-api-field-description Set to return defanged HTML content by default.  (default is unset)
     */
    @XmlAttribute(name=MailConstants.A_WANT_HTML /* html */, required=false)
    private ZmBoolean wantHtml;

    /**
     * @zm-api-field-tag max-inlined-length
     * @zm-api-field-description Maximum inlined length
     */
    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH /* max */, required=false)
    private Integer maxInlinedLength;

    /**
     * @zm-api-field-description Requested headers.  if <b>&lt;header></b>s are requested, any matching headers are
     * inlined into the response (not available when <b>raw</b> is set)
     */
    @XmlElement(name=MailConstants.A_HEADER /* header */, required=false)
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

    public void setInlineRule(String inlineRule) { this.inlineRule = inlineRule; }
    public void setWantHtml(Boolean wantHtml) { this.wantHtml = ZmBoolean.fromBool(wantHtml); }
    public void setMaxInlinedLength(Integer maxInlinedLength) { this.maxInlinedLength = maxInlinedLength; }
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
    public String getInlineRule() { return inlineRule; }
    public Boolean getWantHtml() { return ZmBoolean.toBool(wantHtml); }
    public Integer getMaxInlinedLength() { return maxInlinedLength; }
    public List<AttributeName> getHeaders() {
        return headers;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("inlineRule", inlineRule)
            .add("wantHtml", wantHtml)
            .add("maxInlinedLength", maxInlinedLength)
            .add("headers", headers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
