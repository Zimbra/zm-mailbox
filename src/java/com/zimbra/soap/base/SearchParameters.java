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

package com.zimbra.soap.base;

import java.util.List;

import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.CursorInfo;

public interface SearchParameters {

    // Used for requests which use attributes and elements processed by SearchParams.parse
    public void setIncludeTagDeleted(Boolean includeTagDeleted);
    public void setAllowableTaskStatus(String allowableTaskStatus);
    public void setCalItemExpandStart(Long calItemExpandStart);
    public void setCalItemExpandEnd(Long calItemExpandEnd);
    public void setQuery(String query);
    public void setInDumpster(Boolean inDumpster);
    public void setSearchTypes(String searchTypes);
    public void setGroupBy(String groupBy);
    public void setQuick(Boolean quick);
    public void setSortBy(String sortBy);
    public void setFetch(String fetch);
    public void setMarkRead(Boolean markRead);
    public void setMaxInlinedLength(Integer maxInlinedLength);
    public void setWantHtml(Boolean wantHtml);
    public void setNeedCanExpand(Boolean needCanExpand);
    public void setNeuterImages(Boolean neuterImages);
    public void setWantRecipients(Boolean wantRecipients);
    public void setPrefetch(Boolean prefetch);
    public void setResultMode(String resultMode);
    public void setField(String field);
    public void setLimit(Integer limit);
    public void setOffset(Integer offset);
    public void setHeaders(Iterable <AttributeName> headers);
    public void addHeader(AttributeName header);
    public void setCalTz(CalTZInfoInterface calTz);
    public void setLocale(String locale);
    public void setCursor(CursorInfo cursor);

    public Boolean getIncludeTagDeleted();
    public String getAllowableTaskStatus();
    public Long getCalItemExpandStart();
    public Long getCalItemExpandEnd();
    public String getQuery();
    public Boolean getInDumpster();
    public String getSearchTypes();
    public String getGroupBy();
    public Boolean getQuick();
    public String getSortBy();
    public String getFetch();
    public Boolean getMarkRead();
    public Integer getMaxInlinedLength();
    public Boolean getWantHtml();
    public Boolean getNeedCanExpand();
    public Boolean getNeuterImages();
    public Boolean getWantRecipients();
    public Boolean getPrefetch();
    public String getResultMode();
    public String getField();
    public Integer getLimit();
    public Integer getOffset();
    public List<AttributeName> getHeaders();
    public CalTZInfoInterface getCalTz();
    public String getLocale();
    public CursorInfo getCursor();
}
