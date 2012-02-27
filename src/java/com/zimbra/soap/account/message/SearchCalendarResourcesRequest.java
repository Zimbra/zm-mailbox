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

package com.zimbra.soap.account.message;

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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.type.CalTZInfo;
import com.zimbra.soap.account.type.EntrySearchFilterInfo;
import com.zimbra.soap.base.CalTZInfoInterface;
import com.zimbra.soap.base.SearchParameters;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.AttributeSelectorImpl;
import com.zimbra.soap.type.CursorInfo;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Search calendar resources
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_SEARCH_CALENDAR_RESOURCES_REQUEST)
public class SearchCalendarResourcesRequest
extends AttributeSelectorImpl
implements SearchParameters {

    /**
     * @zm-api-field-description
     */
    @XmlAttribute(name=MailConstants.A_INCLUDE_TAG_DELETED /* includeTagDeleted */, required=false)
    private ZmBoolean includeTagDeleted;

    @XmlAttribute(name=MailConstants.A_ALLOWABLE_TASK_STATUS /* allowableTaskStatus */, required=false)
    private String allowableTaskStatus;

    @XmlAttribute(name=MailConstants.A_CAL_EXPAND_INST_START /* calExpandInstStart */, required=false)
    private Long calItemExpandStart;

    @XmlAttribute(name=MailConstants.A_CAL_EXPAND_INST_END /* calExpandInstEnd */, required=false)
    private Long calItemExpandEnd;

    @XmlAttribute(name=MailConstants.E_QUERY /* query */, required=false)
    private String query;

    @XmlAttribute(name=MailConstants.A_IN_DUMPSTER /* inDumpster */, required=false)
    private ZmBoolean inDumpster;

    @XmlAttribute(name=MailConstants.A_SEARCH_TYPES /* types */, required=false)
    private String searchTypes;

    @XmlAttribute(name=MailConstants.A_GROUPBY /* groupBy */, required=false)
    private String groupBy;

    @XmlAttribute(name=MailConstants.A_QUICK /* quick */, required=false)
    private ZmBoolean quick;

    // Based on SortBy which is NOT an enum and appears to support runtime construction
    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description Name of attribute to sort on. default is the calendar resource name.
     */
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    // Based on SearchParams.ExpandResults but allows "0" and "false" as synonyms for "none" + "1" for "first"
    @XmlAttribute(name=MailConstants.A_FETCH /* fetch */, required=false)
    private String fetch;

    @XmlAttribute(name=MailConstants.A_MARK_READ /* read */, required=false)
    private ZmBoolean markRead;

    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH /* max */, required=false)
    private Integer maxInlinedLength;

    @XmlAttribute(name=MailConstants.A_WANT_HTML /* html */, required=false)
    private ZmBoolean wantHtml;

    @XmlAttribute(name=MailConstants.A_NEUTER /* neuter */, required=false)
    private ZmBoolean neuterImages;

    @XmlAttribute(name=MailConstants.A_RECIPIENTS /* recip */, required=false)
    private ZmBoolean wantRecipients;

    @XmlAttribute(name=MailConstants.A_PREFETCH /* prefetch */, required=false)
    private ZmBoolean prefetch;

    // Valid if is a case insensitive match to a value in enum SearchResultMode
    @XmlAttribute(name=MailConstants.A_RESULT_MODE /* resultMode */, required=false)
    private String resultMode;

    @XmlAttribute(name=MailConstants.A_FIELD /* field */, required=false)
    private String field;

    /**
     * @zm-api-field-description An integer specifying the 0-based offset into the results list to return as the
     * first result for this search operation
     */
    @XmlAttribute(name=MailConstants.A_QUERY_LIMIT /* limit */, required=false)
    private Integer limit;

    /**
     * @zm-api-field-description The 0-based offset into the results list to return as the first result for this
     * search operation.
     */
    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer offset;

    @XmlElement(name=MailConstants.A_HEADER /* header */, required=false)
    private List<AttributeName> headers = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private CalTZInfo calTz;

    @XmlElement(name=MailConstants.E_LOCALE /* locale */, required=false)
    private String locale;

    @XmlElement(name=MailConstants.E_CURSOR /* cursor */, required=false)
    private CursorInfo cursor;

    // May be added by GalSearchControl.proxyGalAccountSearch
    @XmlAttribute(name=AccountConstants.A_GAL_ACCOUNT_ID /* galAcctId */, required=false)
    private String galAccountId;

    // The text of this is supplied to GalSearchParams.setQuery(String)
    @XmlElement(name=AccountConstants.E_NAME /* name */, required=false)
    private final String name;

    @XmlElement(name=AccountConstants.E_ENTRY_SEARCH_FILTER /* searchFilter */, required=false)
    private EntrySearchFilterInfo searchFilter;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SearchCalendarResourcesRequest() {
        this((String) null);
    }

    public SearchCalendarResourcesRequest(String name) {
        this.name = name;
    }

    @Override
    public void setIncludeTagDeleted(Boolean includeTagDeleted) {
        this.includeTagDeleted = ZmBoolean.fromBool(includeTagDeleted);
    }
    @Override
    public void setAllowableTaskStatus(String allowableTaskStatus) { this.allowableTaskStatus = allowableTaskStatus; }
    @Override
    public void setCalItemExpandStart(Long calItemExpandStart) { this.calItemExpandStart = calItemExpandStart; }
    @Override
    public void setCalItemExpandEnd(Long calItemExpandEnd) { this.calItemExpandEnd = calItemExpandEnd; }
    @Override
    public void setQuery(String query) { this.query = query; }
    @Override
    public void setInDumpster(Boolean inDumpster) { this.inDumpster = ZmBoolean.fromBool(inDumpster); }
    @Override
    public void setSearchTypes(String searchTypes) { this.searchTypes = searchTypes; }
    @Override
    public void setGroupBy(String groupBy) { this.groupBy = groupBy; }
    @Override
    public void setQuick(Boolean quick) { this.quick = ZmBoolean.fromBool(quick); }
    @Override
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    @Override
    public void setFetch(String fetch) { this.fetch = fetch; }
    @Override
    public void setMarkRead(Boolean markRead) { this.markRead = ZmBoolean.fromBool(markRead); }
    @Override
    public void setMaxInlinedLength(Integer maxInlinedLength) { this.maxInlinedLength = maxInlinedLength; }
    @Override
    public void setWantHtml(Boolean wantHtml) { this.wantHtml = ZmBoolean.fromBool(wantHtml); }
    @Override
    public void setNeedCanExpand(Boolean needCanExpand) { throw new UnsupportedOperationException(); }
    @Override
    public void setNeuterImages(Boolean neuterImages) { this.neuterImages = ZmBoolean.fromBool(neuterImages); }
    @Override
    public void setWantRecipients(Boolean wantRecipients) { this.wantRecipients = ZmBoolean.fromBool(wantRecipients); }
    @Override
    public void setPrefetch(Boolean prefetch) { this.prefetch = ZmBoolean.fromBool(prefetch); }
    @Override
    public void setResultMode(String resultMode) { this.resultMode = resultMode; }
    @Override
    public void setField(String field) { this.field = field; }
    @Override
    public void setLimit(Integer limit) { this.limit = limit; }
    @Override
    public void setOffset(Integer offset) { this.offset = offset; }
    @Override
    public void setHeaders(Iterable <AttributeName> headers) {
        this.headers.clear();
        if (headers != null) {
            Iterables.addAll(this.headers,headers);
        }
    }

    @Override
    public void addHeader(AttributeName header) {
        this.headers.add(header);
    }

    public void setCalTz(CalTZInfo calTz) { this.calTz = calTz; }
    @Override
    public void setCalTz(CalTZInfoInterface calTz) { setCalTz((CalTZInfo) calTz); }
    @Override
    public void setLocale(String locale) { this.locale = locale; }
    @Override
    public void setCursor(CursorInfo cursor) { this.cursor = cursor; }
    public void setGalAccountId(String galAccountId) { this.galAccountId = galAccountId; }
    public void setSearchFilter(EntrySearchFilterInfo searchFilter) { this.searchFilter = searchFilter; }
    @Override
    public Boolean getIncludeTagDeleted() { return ZmBoolean.toBool(includeTagDeleted); }
    @Override
    public String getAllowableTaskStatus() { return allowableTaskStatus; }
    @Override
    public Long getCalItemExpandStart() { return calItemExpandStart; }
    @Override
    public Long getCalItemExpandEnd() { return calItemExpandEnd; }
    @Override
    public String getQuery() { return query; }
    @Override
    public Boolean getInDumpster() { return ZmBoolean.toBool(inDumpster); }
    @Override
    public String getSearchTypes() { return searchTypes; }
    @Override
    public String getGroupBy() { return groupBy; }
    @Override
    public Boolean getQuick() { return ZmBoolean.toBool(quick); }
    @Override
    public String getSortBy() { return sortBy; }
    @Override
    public String getFetch() { return fetch; }
    @Override
    public Boolean getMarkRead() { return ZmBoolean.toBool(markRead); }
    @Override
    public Integer getMaxInlinedLength() { return maxInlinedLength; }
    @Override
    public Boolean getWantHtml() { return ZmBoolean.toBool(wantHtml); }
    @Override
    public Boolean getNeedCanExpand() { throw new UnsupportedOperationException(); }
    @Override
    public Boolean getNeuterImages() { return ZmBoolean.toBool(neuterImages); }
    @Override
    public Boolean getWantRecipients() { return ZmBoolean.toBool(wantRecipients); }
    @Override
    public Boolean getPrefetch() { return ZmBoolean.toBool(prefetch); }
    @Override
    public String getResultMode() { return resultMode; }
    @Override
    public String getField() { return field; }
    @Override
    public Integer getLimit() { return limit; }
    @Override
    public Integer getOffset() { return offset; }
    @Override
    public List<AttributeName> getHeaders() {
        return Collections.unmodifiableList(headers);
    }
    @Override
    public CalTZInfo getCalTz() { return calTz; }
    @Override
    public String getLocale() { return locale; }
    @Override
    public CursorInfo getCursor() { return cursor; }
    public String getGalAccountId() { return galAccountId; }
    public String getName() { return name; }
    public EntrySearchFilterInfo getSearchFilter() { return searchFilter; }

    @Override
    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("includeTagDeleted", includeTagDeleted)
            .add("allowableTaskStatus", allowableTaskStatus)
            .add("calItemExpandStart", calItemExpandStart)
            .add("calItemExpandEnd", calItemExpandEnd)
            .add("query", query)
            .add("inDumpster", inDumpster)
            .add("searchTypes", searchTypes)
            .add("groupBy", groupBy)
            .add("quick", quick)
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
            .add("offset", offset)
            .add("headers", headers)
            .add("calTz", calTz)
            .add("locale", locale)
            .add("cursor", cursor)
            .add("galAccountId", galAccountId)
            .add("name", name)
            .add("searchFilter", searchFilter);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }

}
