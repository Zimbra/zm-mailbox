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
import com.zimbra.soap.mail.type.AppointmentHitInfo;
import com.zimbra.soap.mail.type.ChatHitInfo;
import com.zimbra.soap.mail.type.ContactInfo;
import com.zimbra.soap.mail.type.ConversationHitInfo;
import com.zimbra.soap.mail.type.DocumentHitInfo;
import com.zimbra.soap.mail.type.MessageHitInfo;
import com.zimbra.soap.mail.type.MessagePartHitInfo;
import com.zimbra.soap.mail.type.NoteHitInfo;
import com.zimbra.soap.mail.type.SpellingSuggestionsQueryInfo;
import com.zimbra.soap.mail.type.TaskHitInfo;
import com.zimbra.soap.mail.type.WikiHitInfo;
import com.zimbra.soap.type.BaseQueryInfo;
import com.zimbra.soap.type.SearchHit;
import com.zimbra.soap.type.SimpleSearchHit;
import com.zimbra.soap.type.WildcardExpansionQueryInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_SEARCH_RESPONSE)
@XmlType(propOrder = {"searchHits", "queryInfos"})
public class SearchResponse {

    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer queryOffset;

    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private ZmBoolean queryMore;

    @XmlAttribute(name=MailConstants.A_TOTAL_SIZE /* total */, required=false)
    private Long totalSize;

    @XmlElements({
        @XmlElement(name="hit", type=SimpleSearchHit.class),
        @XmlElement(name=MailConstants.E_CONV /* c */, type=ConversationHitInfo.class),
        @XmlElement(name=MailConstants.E_MSG /* m */, type=MessageHitInfo.class),
        @XmlElement(name=MailConstants.E_CHAT /* chat */, type=ChatHitInfo.class),
        @XmlElement(name=MailConstants.E_MIMEPART /* mp */, type=MessagePartHitInfo.class),
        @XmlElement(name=MailConstants.E_CONTACT /* cn */, type=ContactInfo.class),
        @XmlElement(name=MailConstants.E_NOTE /* note */, type=NoteHitInfo.class),
        @XmlElement(name=MailConstants.E_DOC /* doc */, type=DocumentHitInfo.class),
        @XmlElement(name=MailConstants.E_WIKIWORD /* w */, type=WikiHitInfo.class),
        @XmlElement(name=MailConstants.E_APPOINTMENT /* appt */, type=AppointmentHitInfo.class),
        @XmlElement(name=MailConstants.E_TASK /* task */, type=TaskHitInfo.class)
    })
    private List<SearchHit> searchHits = Lists.newArrayList();

    @XmlElementWrapper(name=MailConstants.E_INFO /* info */, required=false)
    @XmlElements({
        @XmlElement(name=MailConstants.E_SUGEST, type=SpellingSuggestionsQueryInfo.class),
        @XmlElement(name="wildcard", type=WildcardExpansionQueryInfo.class)
    })
    private List<BaseQueryInfo> queryInfos = Lists.newArrayList();

    public SearchResponse() {
    }

    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setQueryOffset(Integer queryOffset) {
        this.queryOffset = queryOffset;
    }
    public void setQueryMore(Boolean queryMore) { this.queryMore = ZmBoolean.fromBool(queryMore); }
    public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
    public void setSearchHits(Iterable <SearchHit> searchHits) {
        this.searchHits.clear();
        if (searchHits != null) {
            Iterables.addAll(this.searchHits,searchHits);
        }
    }

    public SearchResponse addSearchHit(SearchHit searchHit) {
        this.searchHits.add(searchHit);
        return this;
    }

    public void setQueryInfos(Iterable <BaseQueryInfo> queryInfos) {
        this.queryInfos.clear();
        if (queryInfos != null) {
            Iterables.addAll(this.queryInfos,queryInfos);
        }
    }

    public SearchResponse addQueryInfo(BaseQueryInfo queryInfo) {
        this.queryInfos.add(queryInfo);
        return this;
    }

    public String getSortBy() { return sortBy; }
    public Integer getQueryOffset() { return queryOffset; }
    public Boolean getQueryMore() { return ZmBoolean.toBool(queryMore); }
    public Long getTotalSize() { return totalSize; }
    public List<SearchHit> getSearchHits() {
        return Collections.unmodifiableList(searchHits);
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
            .add("totalSize", totalSize)
            .add("searchHits", searchHits)
            .add("queryInfos", queryInfos);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
