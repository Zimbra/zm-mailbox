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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.SuggestedQueryString;
import com.zimbra.soap.type.BaseQueryInfo;
import com.zimbra.soap.type.WildcardExpansionQueryInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"messages", "queryInfos"})
public class NestedSearchConversation {

    /**
     * @zm-api-field-tag conv-id
     * @zm-api-field-description Conversation ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag num-msgs
     * @zm-api-field-description Number of messages in conversation without IMAP <b>\Deleted</b> flag set
     */
    @XmlAttribute(name=MailConstants.A_NUM /* n */, required=false)
    private Integer num;

    /**
     * @zm-api-field-tag all-msgs
     * @zm-api-field-description Total number of messages in conversation
     */
    @XmlAttribute(name=MailConstants.A_TOTAL_SIZE /* total */, required=false)
    private Integer totalSize;

    /**
     * @zm-api-field-tag conversation-flags
     * @zm-api-field-description Same flags as on <b>&lt;m></b> ("sarwfdxnu!?"), aggregated from all the
     * conversation's messages
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-tag tags
     * @zm-api-field-description Tags - Comma separated list of integers.  DEPRECATED - use "tn" instead
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Comma-separated list of tag names
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    // Assumed that the only type of hit is a Message hit
    /**
     * @zm-api-field-description Message hits
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private List<MessageHitInfo> messages = Lists.newArrayList();

    /**
     * @zm-api-field-description Info block.  Used to return general status information about your search.
     * The <b>&lt;wildcard></b> element tells you about the status of wildcard expansions within your search.
     * If expanded is set, then the wildcard was expanded and the matches are included in the search.  If expanded is
     * unset then the wildcard was not specific enough and therefore no wildcard matches are included
     * (exact-match <b>is</b> included in results).
     */
    @XmlElementWrapper(name=MailConstants.E_INFO /* info */, required=false)
    @XmlElements({
        @XmlElement(name=MailConstants.E_SUGEST, type=SuggestedQueryString.class),
        @XmlElement(name="wildcard", type=WildcardExpansionQueryInfo.class)
    })
    private List<BaseQueryInfo> queryInfos = Lists.newArrayList();

    public NestedSearchConversation() {
    }

    public void setId(String id) { this.id = id; }
    public void setNum(Integer num) { this.num = num; }
    public void setTotalSize(Integer totalSize) { this.totalSize = totalSize; }
    public void setFlags(String flags) { this.flags = flags; }
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
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

    public String getId() { return id; }
    public Integer getNum() { return num; }
    public Integer getTotalSize() { return totalSize; }
    public String getFlags() { return flags; }
    @Deprecated
    public String getTags() { return tags; }
    public String getTagNames() { return tagNames; }
    public List<MessageHitInfo> getMessages() {
        return Collections.unmodifiableList(messages);
    }
    public List<BaseQueryInfo> getQueryInfos() {
        return Collections.unmodifiableList(queryInfos);
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("num", num)
            .add("totalSize", totalSize)
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("messages", messages)
            .add("queryInfos", queryInfos);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
