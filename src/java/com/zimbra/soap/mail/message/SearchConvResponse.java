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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.MessageHitInfo;
import com.zimbra.soap.mail.type.NestedSearchConversation;
import com.zimbra.soap.mail.type.SpellingSuggestionsQueryInfo;
import com.zimbra.soap.type.BaseQueryInfo;
import com.zimbra.soap.type.WildcardExpansionQueryInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SEARCH_CONV_RESPONSE)
@XmlType(propOrder = {"conversation", "messages", "queryInfos"})
public class SearchConvResponse {

    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer queryOffset;

    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private Boolean queryMore;

    @XmlElement(name=MailConstants.E_CONV /* c */, required=false)
    private NestedSearchConversation conversation;

    // Assumed that the only type of hit is a Message hit
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private List<MessageHitInfo> messages = Lists.newArrayList();

    @XmlElementWrapper(name=MailConstants.E_INFO /* info */, required=false)
    @XmlElements({
        @XmlElement(name=MailConstants.E_SUGEST, type=SpellingSuggestionsQueryInfo.class),
        @XmlElement(name="wildcard", type=WildcardExpansionQueryInfo.class)
    })
    private List<BaseQueryInfo> queryInfos = Lists.newArrayList();

    public SearchConvResponse() {
    }

    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setQueryOffset(Integer queryOffset) {
        this.queryOffset = queryOffset;
    }
    public void setQueryMore(Boolean queryMore) { this.queryMore = queryMore; }
    public void setConversation(NestedSearchConversation conversation) {
        this.conversation = conversation;
    }

    public void setMessages(Iterable <MessageHitInfo> messages) {
        this.messages.clear();
        if (messages != null) {
            Iterables.addAll(this.messages,messages);
        }
    }

    public void addMessage(MessageHitInfo message) {
        this.messages.add(message);
    }

    public void setQueryInfos(Iterable <BaseQueryInfo> queryInfos) {
        this.queryInfos.clear();
        if (queryInfos != null) {
            Iterables.addAll(this.queryInfos,queryInfos);
        }
    }

    public void addQueryInfo(BaseQueryInfo queryInfo) {
        this.queryInfos.add(queryInfo);
    }

    public String getSortBy() { return sortBy; }
    public Integer getQueryOffset() { return queryOffset; }
    public Boolean getQueryMore() { return queryMore; }
    public NestedSearchConversation getConversation() { return conversation; }
    public List<MessageHitInfo> getMessages() {
        return Collections.unmodifiableList(messages);
    }
    public List<BaseQueryInfo> getQueryInfos() {
        return Collections.unmodifiableList(queryInfos);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("sortBy", sortBy)
            .add("queryOffset", queryOffset)
            .add("queryMore", queryMore)
            .add("conversation", conversation)
            .add("messages", messages)
            .add("queryInfos", queryInfos);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
