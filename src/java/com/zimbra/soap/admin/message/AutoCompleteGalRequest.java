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

package com.zimbra.soap.admin.message;

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
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;

import com.zimbra.soap.admin.type.AttributeName;
import com.zimbra.soap.admin.type.CalTZInfo;
import com.zimbra.soap.admin.type.CursorInfo;
import com.zimbra.soap.admin.type.GalSearchType;
import com.zimbra.soap.admin.type.StringValue;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_AUTO_COMPLETE_GAL_REQUEST)
public class AutoCompleteGalRequest {

    @XmlAttribute(name=AdminConstants.A_DOMAIN, required=true)
    private final String domain;

    @XmlAttribute(name=AccountConstants.E_NAME, required=true)
    private final String name;

    @XmlAttribute(name=AdminConstants.A_LIMIT, required=false)
    private Integer limit;

    @XmlAttribute(name=AccountConstants.A_TYPE, required=false)
    private GalSearchType type;

    @XmlAttribute(name=AccountConstants.A_GAL_ACCOUNT_ID, required=false)
    private String galAccountId;

    @XmlAttribute(name=MailConstants.A_INCLUDE_TAG_DELETED, required=false)
    private Boolean includeTagDeleted;

    @XmlAttribute(name=MailConstants.A_ALLOWABLE_TASK_STATUS, required=false)
    private String allowableTaskStatus;

    @XmlAttribute(name=MailConstants.A_CAL_EXPAND_INST_START, required=false)
    private Long calItemExpandStart;

    @XmlAttribute(name=MailConstants.A_CAL_EXPAND_INST_END, required=false)
    private Long calItemExpandEnd;

    @XmlAttribute(name=MailConstants.E_QUERY, required=false)
    private String query;

    @XmlAttribute(name=MailConstants.A_IN_DUMPSTER, required=false)
    private Boolean inDumpster;

    @XmlAttribute(name=MailConstants.A_SEARCH_TYPES, required=false)
    private String searchTypes;

    @XmlAttribute(name=MailConstants.A_GROUPBY, required=false)
    private String groupBy;

    // Based on SortBy which is NOT an enum and appears to support
    // runtime construction
    @XmlAttribute(name=MailConstants.A_SORTBY, required=false)
    private String sortBy;

    // Based on ExpandResults but allows "0" and "false" as synonyms for
    // "none" + "1" for "first"
    @XmlAttribute(name=MailConstants.A_FETCH, required=false)
    private String fetch;

    @XmlAttribute(name=MailConstants.A_MARK_READ, required=false)
    private Boolean markRead;

    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH, required=false)
    private Integer maxInlinedLength;

    @XmlAttribute(name=MailConstants.A_WANT_HTML, required=false)
    private Boolean wantHtml;

    @XmlAttribute(name=MailConstants.A_NEUTER, required=false)
    private Boolean neuterImages;

    @XmlAttribute(name=MailConstants.A_RECIPIENTS, required=false)
    private Boolean wantRecipients;

    @XmlAttribute(name=MailConstants.A_PREFETCH, required=false)
    private Boolean prefetch;

    // Valid if is a case insensitive match to a value in enum SearchResultMode
    @XmlAttribute(name=MailConstants.A_RESULT_MODE, required=false)
    private String resultMode;

    @XmlAttribute(name=MailConstants.A_FIELD, required=false)
    private String field;

    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET, required=false)
    private Integer offset;

    @XmlElement(name=MailConstants.A_HEADER, required=false)
    private List<AttributeName> headers = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CAL_TZ, required=false)
    private CalTZInfo calTz;

    @XmlElement(name=MailConstants.E_LOCALE, required=false)
    private StringValue locale;

    @XmlElement(name=MailConstants.E_CURSOR, required=false)
    private CursorInfo cursor;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AutoCompleteGalRequest() {
        this((String) null, (String) null);
    }

    public AutoCompleteGalRequest(String domain, String name) {
        this.domain = domain;
        this.name = name;
    }

    public void setLimit(Integer limit) { this.limit = limit; }
    public void setType(GalSearchType type) { this.type = type; }

    public void setGalAccountId(String galAccountId) {
        this.galAccountId = galAccountId;
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
    public void setOffset(Integer offset) { this.offset = offset; }
    public void setHeaders(Iterable <AttributeName> headers) {
        this.headers.clear();
        if (headers != null) {
            Iterables.addAll(this.headers,headers);
        }
    }

    public AutoCompleteGalRequest addHeader(AttributeName header) {
        this.headers.add(header);
        return this;
    }

    public void setCalTz(CalTZInfo calTz) { this.calTz = calTz; }
    public void setLocale(StringValue locale) { this.locale = locale; }
    public void setCursor(CursorInfo cursor) { this.cursor = cursor; }
    public String getDomain() { return domain; }
    public String getName() { return name; }
    public Integer getLimit() { return limit; }
    public GalSearchType getType() { return type; }
    public String getGalAccountId() { return galAccountId; }
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
    public Integer getOffset() { return offset; }
    public List<AttributeName> getHeaders() {
        return Collections.unmodifiableList(headers);
    }
    public CalTZInfo getCalTz() { return calTz; }
    public StringValue getLocale() { return locale; }
    public CursorInfo getCursor() { return cursor; }
}
