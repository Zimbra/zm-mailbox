/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonArrayForWrapper;
import com.zimbra.soap.mail.type.AppointmentHitInfo;
import com.zimbra.soap.mail.type.ChatHitInfo;
import com.zimbra.soap.mail.type.ContactInfo;
import com.zimbra.soap.mail.type.ConversationHitInfo;
import com.zimbra.soap.mail.type.DocumentHitInfo;
import com.zimbra.soap.mail.type.MessageHitInfo;
import com.zimbra.soap.mail.type.MessagePartHitInfo;
import com.zimbra.soap.mail.type.NoteHitInfo;
import com.zimbra.soap.mail.type.SuggestedQueryString;
import com.zimbra.soap.mail.type.TaskHitInfo;
import com.zimbra.soap.mail.type.WikiHitInfo;
import com.zimbra.soap.type.BaseQueryInfo;
import com.zimbra.soap.type.SearchHit;
import com.zimbra.soap.type.SimpleSearchHit;
import com.zimbra.soap.type.WildcardExpansionQueryInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SEARCH_RESPONSE)
@XmlType(propOrder = {"searchHits", "queryInfos"})
public class SearchResponse {

    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description What to sort by.  Default is "dateDesc"
     * <br />
     * Possible values:
     * <br />
     * none|dateAsc|dateDesc|subjAsc|subjDesc|nameAsc|nameDesc|rcptAsc|rcptDesc|attachAsc|attachDesc|
     * flagAsc|flagDesc|priorityAsc|priorityDesc|idAsc|idDesc|readAsc|readDesc
     * <br />
     * If sort-by is "none" then cursors MUST NOT be used, and some searches are impossible (searches that require
     * intersection of complex sub-ops). Server will throw an IllegalArgumentException if the search is invalid.
     * ADDITIONAL SORT MODES FOR TASKS: valid only if types="task" (and task alone):
     * <br />
     * taskDueAsc|taskDueDesc|taskStatusAsc|taskStatusDesc|taskPercCompletedAsc|taskPercCompletedDesc
     */
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    /**
     * @zm-api-field-tag offset
     * @zm-api-field-description Offset - an integer specifying the 0-based offset into the results list returned as
     * the first result for this search operation.
     */
    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer queryOffset;

    /**
     * @zm-api-field-tag more-flag
     * @zm-api-field-description Set if there are more search results remaining.
     */
    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private ZmBoolean queryMore;

    /**
     * @zm-api-field-tag all-msgs
     * @zm-api-field-description All messages
     */
    @XmlAttribute(name=MailConstants.A_TOTAL_SIZE /* total */, required=false)
    private Long totalSize;

    /**
     * @zm-api-field-description Search hits
     */
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

    /**
     * @zm-api-field-description Info block.  Used to return general status information about your search.
     * The <b>&lt;wildcard></b> element tells you about the status of wildcard expansions within your search.
     * If expanded is set, then the wildcard was expanded and the matches are included in the search.  If expanded is
     * unset then the wildcard was not specific enough and therefore no wildcard matches are included
     * (exact-match <b>is</b> included in results).
     */
    @ZimbraJsonArrayForWrapper
    @XmlElementWrapper(name=MailConstants.E_INFO /* info */, required=false)
    @XmlElements({
        @XmlElement(name=MailConstants.E_SUGEST, type=SuggestedQueryString.class),
        @XmlElement(name="wildcard", type=WildcardExpansionQueryInfo.class)
    })
    private List<BaseQueryInfo> queryInfos = Lists.newArrayList();

    /**
     * @zm-api-field-description Set to TRUE if this search has been performed often enough to
     * prompt the user to create a search folder for this query
     */
    @XmlAttribute(name=MailConstants.A_SAVE_SEARCH_PROMPT /* saveSearchPrompt */, required=false)
    private ZmBoolean saveSearchPrompt;

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

    public void setSaveSearchPrompt(boolean bool) {
        saveSearchPrompt = ZmBoolean.fromBool(bool);
    }

    public boolean getSaveSearchPrompt() {
        return ZmBoolean.toBool(saveSearchPrompt, false);
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
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
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
