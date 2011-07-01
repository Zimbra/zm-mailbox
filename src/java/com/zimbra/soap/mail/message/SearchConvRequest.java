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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalTZInfo;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.CursorInfo;
import com.zimbra.soap.type.SearchParamsInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_SEARCH_CONV_REQUEST)
public class SearchConvRequest extends SearchParamsInfo {

    @XmlAttribute(name=MailConstants.A_NEST_MESSAGES /* nest */, required=false)
    private Boolean nestMessages;

    @XmlAttribute(name=MailConstants.A_CONV_ID /* cid */, required=true)
    private final String conversationId;

    @XmlAttribute(name=MailConstants.A_NEED_EXP /* needExp */, required=false)
    private Boolean needCanExpand;

    // SearchParams.parse processes SearchParamsInfo (for attributes) and
    // headers/calTz/locale/cursor

    @XmlElement(name=MailConstants.A_HEADER /* header */, required=false)
    private List<AttributeName> headers = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private CalTZInfo calTz;

    @XmlElement(name=MailConstants.E_LOCALE /* locale */, required=false)
    private String locale;

    @XmlElement(name=MailConstants.E_CURSOR /* cursor */, required=false)
    private CursorInfo cursor;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SearchConvRequest() {
        this((String) null);
    }

    public SearchConvRequest(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setNestMessages(Boolean nestMessages) {
        this.nestMessages = nestMessages;
    }
    public void setNeedCanExpand(Boolean needCanExpand) {
        this.needCanExpand = needCanExpand;
    }
    public void setHeaders(Iterable <AttributeName> headers) {
        this.headers.clear();
        if (headers != null) {
            Iterables.addAll(this.headers,headers);
        }
    }

    public void addHeader(AttributeName header) {
        this.headers.add(header);
    }

    public void setCalTz(CalTZInfo calTz) { this.calTz = calTz; }
    public void setLocale(String locale) { this.locale = locale; }
    public void setCursor(CursorInfo cursor) { this.cursor = cursor; }
    public Boolean getNestMessages() { return nestMessages; }
    public String getConversationId() { return conversationId; }
    public Boolean getNeedCanExpand() { return needCanExpand; }
    public List<AttributeName> getHeaders() {
        return Collections.unmodifiableList(headers);
    }
    public CalTZInfo getCalTz() { return calTz; }
    public String getLocale() { return locale; }
    public CursorInfo getCursor() { return cursor; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("nestMessages", nestMessages)
            .add("conversationId", conversationId)
            .add("needCanExpand", needCanExpand)
            .add("headers", headers)
            .add("calTz", calTz)
            .add("locale", locale)
            .add("cursor", cursor);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
