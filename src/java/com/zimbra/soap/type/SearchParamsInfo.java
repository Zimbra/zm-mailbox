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

package com.zimbra.soap.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class SearchParamsInfo {

    // Handle attributes processed by SearchParams.parse.
    // Elements processed by SearchParams.parse are NOT included here to
    // avoid namespace issues.

    @XmlAttribute(name=MailConstants.A_INCLUDE_TAG_DELETED /* includeTagDeleted */, required=false)
    private Boolean includeTagDeleted;

    @XmlAttribute(name=MailConstants.A_ALLOWABLE_TASK_STATUS /* allowableTaskStatus */, required=false)
    private String allowableTaskStatus;

    @XmlAttribute(name=MailConstants.A_CAL_EXPAND_INST_START /* calExpandInstStart */, required=false)
    private Long calItemExpandStart;

    @XmlAttribute(name=MailConstants.A_CAL_EXPAND_INST_END /* calExpandInstEnd */, required=false)
    private Long calItemExpandEnd;

    @XmlAttribute(name=MailConstants.E_QUERY /* query */, required=false)
    private String query;

    @XmlAttribute(name=MailConstants.A_IN_DUMPSTER /* inDumpster */, required=false)
    private Boolean inDumpster;

    @XmlAttribute(name=MailConstants.A_SEARCH_TYPES /* types */, required=false)
    private String searchTypes;

    @XmlAttribute(name=MailConstants.A_GROUPBY /* groupBy */, required=false)
    private String groupBy;

    // Based on SortBy which is NOT an enum and appears to support runtime construction
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    // Based on ExpandResults but allows "0" and "false" as synonyms for "none" + "1" for "first"
    @XmlAttribute(name=MailConstants.A_FETCH /* fetch */, required=false)
    private String fetch;

    @XmlAttribute(name=MailConstants.A_MARK_READ /* read */, required=false)
    private Boolean markRead;

    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH /* max */, required=false)
    private Integer maxInlinedLength;

    @XmlAttribute(name=MailConstants.A_WANT_HTML /* html */, required=false)
    private Boolean wantHtml;

    @XmlAttribute(name=MailConstants.A_NEUTER /* neuter */, required=false)
    private Boolean neuterImages;

    @XmlAttribute(name=MailConstants.A_RECIPIENTS /* recip */, required=false)
    private Boolean wantRecipients;

    @XmlAttribute(name=MailConstants.A_PREFETCH /* prefetch */, required=false)
    private Boolean prefetch;

    // Valid if is a case insensitive match to a value in enum SearchResultMode
    @XmlAttribute(name=MailConstants.A_RESULT_MODE /* resultMode */, required=false)
    private String resultMode;

    @XmlAttribute(name=MailConstants.A_FIELD /* field */, required=false)
    private String field;

    @XmlAttribute(name=MailConstants.A_QUERY_LIMIT /* limit */, required=false)
    private Integer limit;

    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer offset;

    public SearchParamsInfo() {
    }

    public void setIncludeTagDeleted(Boolean includeTagDeleted) {
        this.includeTagDeleted = includeTagDeleted;
    }
    public void setAllowableTaskStatus(String allowableTaskStatus) {
        this.allowableTaskStatus = allowableTaskStatus;
    }
    public void setCalItemExpandStart(Long calItemExpandStart) {
        this.calItemExpandStart = calItemExpandStart;
    }
    public void setCalItemExpandEnd(Long calItemExpandEnd) {
        this.calItemExpandEnd = calItemExpandEnd;
    }
    public void setQuery(String query) { this.query = query; }
    public void setInDumpster(Boolean inDumpster) {
        this.inDumpster = inDumpster;
    }
    public void setSearchTypes(String searchTypes) {
        this.searchTypes = searchTypes;
    }
    public void setGroupBy(String groupBy) { this.groupBy = groupBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setFetch(String fetch) { this.fetch = fetch; }
    public void setMarkRead(Boolean markRead) { this.markRead = markRead; }
    public void setMaxInlinedLength(Integer maxInlinedLength) {
        this.maxInlinedLength = maxInlinedLength;
    }
    public void setWantHtml(Boolean wantHtml) { this.wantHtml = wantHtml; }
    public void setNeuterImages(Boolean neuterImages) {
        this.neuterImages = neuterImages;
    }
    public void setWantRecipients(Boolean wantRecipients) {
        this.wantRecipients = wantRecipients;
    }
    public void setPrefetch(Boolean prefetch) { this.prefetch = prefetch; }
    public void setResultMode(String resultMode) {
        this.resultMode = resultMode;
    }
    public void setField(String field) { this.field = field; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public Boolean getIncludeTagDeleted() { return includeTagDeleted; }
    public String getAllowableTaskStatus() { return allowableTaskStatus; }
    public Long getCalItemExpandStart() { return calItemExpandStart; }
    public Long getCalItemExpandEnd() { return calItemExpandEnd; }
    public String getQuery() { return query; }
    public Boolean getInDumpster() { return inDumpster; }
    public String getSearchTypes() { return searchTypes; }
    public String getGroupBy() { return groupBy; }
    public String getSortBy() { return sortBy; }
    public String getFetch() { return fetch; }
    public Boolean getMarkRead() { return markRead; }
    public Integer getMaxInlinedLength() { return maxInlinedLength; }
    public Boolean getWantHtml() { return wantHtml; }
    public Boolean getNeuterImages() { return neuterImages; }
    public Boolean getWantRecipients() { return wantRecipients; }
    public Boolean getPrefetch() { return prefetch; }
    public String getResultMode() { return resultMode; }
    public String getField() { return field; }
    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("includeTagDeleted", includeTagDeleted)
            .add("allowableTaskStatus", allowableTaskStatus)
            .add("calItemExpandStart", calItemExpandStart)
            .add("calItemExpandEnd", calItemExpandEnd)
            .add("query", query)
            .add("inDumpster", inDumpster)
            .add("searchTypes", searchTypes)
            .add("groupBy", groupBy)
            .add("sortBy", sortBy)
            .add("fetch", fetch)
            .add("markRead", markRead)
            .add("maxInlinedLength", maxInlinedLength)
            .add("wantHtml", wantHtml)
            .add("neuterImages", neuterImages)
            .add("wantRecipients", wantRecipients)
            .add("prefetch", prefetch)
            .add("resultMode", resultMode)
            .add("field", field)
            .add("limit", limit)
            .add("offset", offset);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
