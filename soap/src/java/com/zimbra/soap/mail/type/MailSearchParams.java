/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.type;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CalTZInfoInterface;
import com.zimbra.soap.base.SearchParameters;
import com.zimbra.soap.json.jackson.annotate.ZimbraJsonAttribute;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.CursorInfo;
import com.zimbra.soap.type.MsgContent;
import com.zimbra.soap.type.WantRecipsSetting;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class MailSearchParams implements SearchParameters {

    // Handles attributes and elements processed by SearchParams.parse

    /**
     * @zm-api-field-tag include-items-tagged-for-delete
     * @zm-api-field-description Set to <b>1 (true)</b> to include items with the \Deleted tag set in results
     */
    @XmlAttribute(name=MailConstants.A_INCLUDE_TAG_DELETED /* includeTagDeleted */, required=false)
    private ZmBoolean includeTagDeleted;

    /**
     * @zm-api-field-tag include-items-tagged-for-delete
     * @zm-api-field-description Set to <b>1 (true)</b> to include items with the \Muted tag set in results
     */
    @XmlAttribute(name=MailConstants.A_INCLUDE_TAG_MUTED /* includeTagMuted */, required=false)
    private ZmBoolean includeTagMuted;

    // Values are from TaskHit.Status enum but case insensitive
    // soap.txt documentation on SearchRequest implies values : need,inprogress,completed,canceled
    /**
     * @zm-api-field-description Comma separated list of allowable Task statuses.
     * <br />
     * Valid values : NEED, INPR, WAITING, DEFERRED, COMP
     */
    @XmlAttribute(name=MailConstants.A_ALLOWABLE_TASK_STATUS /* allowableTaskStatus */, required=false)
    private String allowableTaskStatus;

    // TODO: Make a good linkage between this and the response description of expanded instance data.
    /**
     * @zm-api-field-tag cal-item-expand-start-time-in-msec
     * @zm-api-field-description Start time in milliseconds for the range to include instances for calendar items from.
     * <br />
     * If <b>calExpandInstStart</b> and <b>calExpandInstEnd</b> are specified, and the search types include calendar
     * item types (e.g. appointment), then the search results include the instances for calendar items within that
     * range in the form described in the description of the response.
     * <br />
     * <br />
     * <b>***IMPORTANT NOTE</b>: Calendar Items that have no instances within that range are COMPLETELY EXCLUDED from
     * the results (e.g. not even an <b>&lt;appt></b> element.  Calendar Items with no data (such as Tasks with no
     * date specified) are included, but with no instance information***
     */
    @XmlAttribute(name=MailConstants.A_CAL_EXPAND_INST_START /* calExpandInstStart */, required=false)
    private Long calItemExpandStart;

    /**
     * @zm-api-field-tag cal-item-expand-end-time-in-msec
     * @zm-api-field-description End time in milliseconds for the range to include instances for calendar items from.
     */
    @XmlAttribute(name=MailConstants.A_CAL_EXPAND_INST_END /* calExpandInstEnd */, required=false)
    private Long calItemExpandEnd;

    /**
     * @zm-api-field-tag query-string
     * @zm-api-field-description Query string
     */
    @ZimbraJsonAttribute
    @XmlElement(name=MailConstants.E_QUERY /* query */, required=false)
    private String query;

    /**
     * @zm-api-field-description Set this flat to <b>1 (true)</b> to search dumpster data instead of live data.
     */
    @XmlAttribute(name=MailConstants.A_IN_DUMPSTER /* inDumpster */, required=false)
    private ZmBoolean inDumpster;

    /**
     * @zm-api-field-tag comma-sep-search-types
     * @zm-api-field-description Comma separated list of search types
     * <br />
     * Legal values are: <b>conversation|message|contact|appointment|task|wiki|document</b>
     * <br />
     * Default is <b>"conversation"</b>.
     * <br />
     * <b>NOTE</b>: only ONE of message, conversation may be set. If both are set, the first is used.
     */
    @XmlAttribute(name=MailConstants.A_SEARCH_TYPES /* types */, required=false)
    private String searchTypes;

    /**
     * @zm-api-field-tag deprecated-group-by
     * @zm-api-field-description Deprecated.  Use <b>{comma-sep-search-types}</b> instead
     */
    @XmlAttribute(name=MailConstants.A_GROUPBY /* groupBy */, required=false)
    private String groupBy;

    /**
     * @zm-api-field-tag
     * @zm-api-field-description "Quick" flag.
     * <br />
     * For performance reasons, the index system accumulates messages with not-indexed-yet state until a certain
     * threshold and indexes them as a batch. To return up-to-date search results, the index system also indexes those
     * pending messages right before a search. To lower latencies, this option gives a hint to the index system not to
     * trigger this catch-up index prior to the search by giving up the freshness of the search results, i.e. recent
     * messages may not be included in the search results.
     */
    @XmlAttribute(name=MailConstants.A_QUICK /* quick */, required=false)
    private ZmBoolean quick;

    // Based on SortBy which is NOT an enum and appears to support runtime construction
    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description SortBy setting.
     * <br />
     * Default value is <b>"dateDesc"</b>
     * <br />
     * Possible values:
     * <b>none|dateAsc|dateDesc|subjAsc|subjDesc|nameAsc|nameDesc|rcptAsc|rcptDesc|attachAsc|attachDesc|flagAsc|flagDesc|
      priorityAsc|priorityDesc|idAsc|idDesc|readAsc|readDesc</b>
     * If <b>{sort-by}</b> is "none" then cursors MUST NOT be used, and some searches are impossible (searches that
     * require intersection of complex sub-ops). Server will throw an IllegalArgumentException if the search is
     * invalid.
     * <br />
     * ADDITIONAL SORT MODES FOR TASKS: valid only if <b>types="task"</b> (and task alone):
     * <br />
     * <b>taskDueAsc|taskDueDesc|taskStatusAsc|taskStatusDesc|taskPercCompletedAsc|taskPercCompletedDesc</b>
     */
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    // Based on SearchParams.ExpandResults but allows "0" and "false" as synonyms for "none" + "1" for "first"
    /**
     * @zm-api-field-description Select setting for hit expansion.
     * <br />
     * if fetch="1" (or fetch="first") is specified, the first hit will be expanded inline (messages only at present)
     * <br />
     * if fetch="{item-id}", only the message with the given {item-id} is expanded inline
     * <br />
     * if fetch="{item-id-1,item-id-2,...,item-id-n}", messages with ids in the comma-separated list will be expanded
     * <br />
     * if fetch="all", all messages are expanded inline
     * <br />
     * if fetch="!", only the first message in the conversation will be expanded, whether it's a hit or not
     * <br />
     * if fetch="u" (or fetch="unread"), all unread hits are expanded
     * <br />
     * if fetch="u1" (or fetch="unread-first"), if there are any unread hits, they are expanded, otherwise the first
     * hit is expanded.
     * <br />
     * if fetch="u1!", if there are any unread hits, they are expanded, otherwise the first hit and the first message
     * are expanded (those may be the same)
     * <br />
     * if fetch="hits", all hits are expanded
     * <br />
     * if fetch="hits!", all hits are expanded if there are any, otherwise the first message is expanded
     * <br />
     * + if html="1" is also specified, inlined hits will return HTML parts if available
     * <br />
     * + if read="1" is also specified, inlined hits will be marked as read
     * <br />
     * + if neuter="0" is also specified, images in inlined HTML parts will not be "neutered"
     * <br />
     * + if <b>&lt;header></b>s are requested, any matching headers are included in inlined message hits
     * <br />
     * + if max="{max-inlined-length}" is specified, inlined body content in limited to the given length;
     * <br />
     * if the part is truncated, truncated="1" is specified on the &lt;mp> in question
     */
    @XmlAttribute(name=MailConstants.A_FETCH /* fetch */, required=false)
    private String fetch;

    /**
     * @zm-api-field-tag mark-as-read
     * @zm-api-field-description Inlined hits will be marked as read
     */
    @XmlAttribute(name=MailConstants.A_MARK_READ /* read */, required=false)
    private ZmBoolean markRead;

    /**
     * @zm-api-field-tag max-inlined-length
     * @zm-api-field-description If specified, inlined body content in limited to the given length;
     * <br />
     * if the part is truncated, truncated="1" is specified on the &lt;mp> in question
     */
    @XmlAttribute(name=MailConstants.A_MAX_INLINED_LENGTH /* max */, required=false)
    private Integer maxInlinedLength;

    /**
     * @zm-api-field-tag want-html
     * @zm-api-field-description Set to <b>1 (true)</b> to cause inlined hits to return HTML parts if available
     */
    @XmlAttribute(name=MailConstants.A_WANT_HTML /* html */, required=false)
    private ZmBoolean wantHtml;


    /**
     * @zm-api-field-tag need-can-expand
     * @zm-api-field-description If 'needExp' is set in the request, two additional flags
     *   may be included in <b>&lt;e></b> elements for messages returned inline.
     * <ul>
     * <li> isGroup - set if the email address is a group
     * <li> exp - present only when isGroup="1".
     *      <br />
     *      Set if the authed user can (has permission to) expand members in this group
     *      <br />
     *      Unset if the authed user does not have permission to expand group members
     * </ul>
     */
    @XmlAttribute(name=MailConstants.A_NEED_EXP /* needExp */, required=false)
    private ZmBoolean needCanExpand;

    /**
     * @zm-api-field-tag neuter-images
     * @zm-api-field-description Set to <b>0 (false)</b> to stop images in inlined HTML parts from being "neutered"
     */
    @XmlAttribute(name=MailConstants.A_NEUTER /* neuter */, required=false)
    private ZmBoolean neuterImages;

    /**
     * @zm-api-field-tag want-recipients
     * @zm-api-field-description Setting specifying which recipients should be returned.
     * <table border="1">
     * <tr> <td> <b>0 [default]</b> </td> <td>
     *     <ul>
     *         <li>returned sent messages will contain "From:" Senders only
     *         <li>returned conversations will contain an aggregated list of "From:" Senders
     *             from messages in the conversation (maximum of 8)
     *     </ul>
     * </td> </tr>
     * <tr> <td> <b>1</b> </td> <td>
     *     <ul>
     *         <li>returned sent messages will contain the set of "To:" Recipients instead of the Sender
     *         <li>returned conversations whose first hit was sent by the user will contain that hit's "To:" recipients
     *             instead of the conversation's sender list (maximum of 8)
     *     </ul>
     * </td> </tr>
     * <tr> <td> <b>2</b> </td> <td>
     *     <ul>
     *         <li>returned sent messages will contain the sets of both "From:" Senders and "To:" Recipients
     *         <li>returned conversations will contain an aggregated list of "From:" Senders and "To:" Recipients
     *             from messages in the conversation (maximum of 8 of each)
     *     </ul>
     * </td> </tr>
     * </table>
     */
    @XmlAttribute(name=MailConstants.A_RECIPIENTS /* recip */, required=false)
    private WantRecipsSetting wantRecipients;

    /**
     * @zm-api-field-description Prefetch
     */
    @XmlAttribute(name=MailConstants.A_PREFETCH /* prefetch */, required=false)
    private ZmBoolean prefetch;

    // Valid if is a case insensitive match to a value in enum SearchResultMode
    /**
     * @zm-api-field-tag result-mode
     * @zm-api-field-description Specifies the type of result.
     * <table>
     * <tr> <td> <b>NORMAL [default]</b> </td> <td> Everything </td> </tr>
     * <tr> <td> <b>IDS</b> </td> <td> Only IDs </td> </tr>
     * </table>
     */
    @XmlAttribute(name=MailConstants.A_RESULT_MODE /* resultMode */, required=false)
    private String resultMode;

    /**
     * @zm-api-field-tag full-conversation
     * @zm-api-field-description By default, only matching messages are included in conversation results.<br />
     * Set to <b>1 (true)</b> to include all messages in the conversation, even if they don't match the search,
     * including items in Trash and Junk folders.
     */
    @ZimbraJsonAttribute
    @XmlAttribute(name=MailConstants.A_FULL_CONVERSATION /* fullConversation */, required=false)
    private ZmBoolean fullConversation;

    /**
     * @zm-api-field-tag default-field
     * @zm-api-field-description By default, text without an operator searches the CONTENT field.  By setting the
     * {default-field} value, you can control the default operator. Specify any of the text operators that are
     * available in query.txt, e.g. 'content:' [the default] or 'subject:', etc.  The date operators
     * (date, after, before) and the "item:" operator should not be specified as default fields because of quirks in
     * the search grammar.
     */
    @XmlAttribute(name=MailConstants.A_FIELD /* field */, required=false)
    private String field;

    /**
     * @zm-api-field-description The maximum number of results to return. It defaults to 10 if not specified, and is
     * capped by 1000
     */
    @XmlAttribute(name=MailConstants.A_QUERY_LIMIT /* limit */, required=false)
    private Integer limit;

    /**
     * @zm-api-field-description Specifies the 0-based offset into the results list to return as the first result for
     * this search operation.
     * <br />
     * For example, limit=10 offset=30 will return the 31st through 40th results inclusive.
     */
    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer offset;

    /**
     * @zm-api-field-description if <b>&lt;header></b>s are requested, any matching headers are included in inlined
     * message hits
     */
    @XmlElement(name=MailConstants.A_HEADER /* header */, required=false)
    private final List<AttributeName> headers = Lists.newArrayList();

    /**
     * @zm-api-field-description Timezone specification
     */
    @XmlElement(name=MailConstants.E_CAL_TZ /* tz */, required=false)
    private CalTZInfo calTz;

    /**
     * @zm-api-field-tag locale-name
     * @zm-api-field-description Client locale identification.
     * <br />
     * Value is of the form LL-CC[-V+] where:
     * <br />
     * LL is two character language code
     * <br />
     * CC is two character country code
     * <br />
     * V+ is optional variant identifier string
     * <br />
     * <br />
     * See:
     * <br />
     * ISO Language Codes: http://www.ics.uci.edu/pub/ietf/http/related/iso639.txt
     * <br />
     * ISO Country Codes: http://www.chemie.fu-berlin.de/diverse/doc/ISO_3166.html
     */
    @XmlElement(name=MailConstants.E_LOCALE /* locale */, required=false)
    private String locale;

    /**
     * @zm-api-field-description Cursor specification
     */
    @XmlElement(name=MailConstants.E_CURSOR /* cursor */, required=false)
    private CursorInfo cursor;

    /**
     * @zm-api-field-tag want-content
     * @zm-api-field-description used by clients if they want mail content with/without quoted text
     */
    @XmlAttribute(name=MailConstants.A_WANT_CONTENT  /* wantContent */ , required=false)
    private MsgContent wantContent;

    /**
     * @zm-api-field-tag include-member-of
     * @zm-api-field-description If set, Include the list of contact groups this contact is a member of.
     * <br />
     * <b>Note</b>: use sparingly, there is a performance penalty associated with computing this information
     */
    @XmlAttribute(name=MailConstants.E_CONTACT_MEMBER_OF /* memberOf */, required=false)
    private ZmBoolean includeMemberOf;

    public MailSearchParams() {
    }

    public void setWantContent(MsgContent msgContent) {
        this.wantContent = msgContent;
    }

    @Override
    public void setIncludeTagDeleted(Boolean includeTagDeleted) {
        this.includeTagDeleted = ZmBoolean.fromBool(includeTagDeleted);
    }
    @Override
    public void setIncludeTagMuted(Boolean includeTagMuted) {
        this.includeTagMuted = ZmBoolean.fromBool(includeTagMuted);
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
    public void setNeedCanExpand(Boolean needCanExpand) { this.needCanExpand = ZmBoolean.fromBool(needCanExpand); }
    @Override
    public void setNeuterImages(Boolean neuterImages) { this.neuterImages = ZmBoolean.fromBool(neuterImages); }
    @Override
    public void setWantRecipients(WantRecipsSetting wantRecipients) {
        this.wantRecipients = WantRecipsSetting.usefulValue(wantRecipients);
    }
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
    public void setLocale(String locale) { this.locale = locale; }
    @Override
    public void setCursor(CursorInfo cursor) { this.cursor = cursor; }
    @Override
    public Boolean getIncludeTagDeleted() { return ZmBoolean.toBool(includeTagDeleted); }
    @Override
    public Boolean getIncludeTagMuted() { return ZmBoolean.toBool(includeTagMuted); }
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
    public Boolean getNeedCanExpand() { return ZmBoolean.toBool(needCanExpand); }
    @Override
    public Boolean getNeuterImages() { return ZmBoolean.toBool(neuterImages); }
    @Override
    public WantRecipsSetting getWantRecipients() { return WantRecipsSetting.usefulValue(wantRecipients); }
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

    public ZmBoolean getFullConversation() { return fullConversation; }
    public void setFullConversation(ZmBoolean fullConversation) { this.fullConversation = fullConversation; }

    public MsgContent getWantContent() {
        return wantContent;
    }

    public void setIncludeMemberOf(Boolean include) {
        includeMemberOf = ZmBoolean.fromBool(include);
    }
    public boolean getIncludeMemberOf() { return ZmBoolean.toBool(includeMemberOf, false); }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("includeTagDeleted", includeTagDeleted)
            .add("includeTagMuted", includeTagMuted)
            .add("includeMemberOf", includeMemberOf)
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
            .add("needCanExpand", needCanExpand)
            .add("neuterImages", neuterImages)
            .add("wantRecipients", wantRecipients)
            .add("prefetch", prefetch)
            .add("resultMode", resultMode)
            .add("fullConversation", fullConversation)
            .add("field", field)
            .add("limit", limit)
            .add("offset", offset)
            .add("headers", headers)
            .add("calTz", calTz)
            .add("locale", locale)
            .add("cursor", cursor)
            .add("wantContent", wantContent);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }

    @Override
    public void setCalTz(CalTZInfoInterface calTz) {
        setCalTz((CalTZInfo) calTz);
    }
}
