/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mailbox.MailItemType;
import com.zimbra.common.mailbox.ZimbraFetchMode;
import com.zimbra.common.mailbox.ZimbraSearchParams;
import com.zimbra.common.mailbox.ZimbraSortBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.mail.CalendarUtils;
import com.zimbra.cs.service.mail.ToXML.OutputParticipants;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.base.CalTZInfoInterface;
import com.zimbra.soap.base.SearchParameters;
import com.zimbra.soap.mail.type.MailSearchParams;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.CursorInfo;
import com.zimbra.soap.type.MsgContent;
import com.zimbra.soap.type.WantRecipsSetting;
import com.zimbra.soap.type.ZmBoolean;

/**
 * Encapsulates all parameters in a search request.
 * <p>
 * IMPORTANT NOTE: if you add new parameters, you MUST add parsing/serialization code to the
 * {@link #encodeParams(Element)} and {@link #parse(Element, ZimbraSoapContext, String)}) APIs.
 * This IS NOT optional and will break cross-server search if you do not comply.
 */
public final class SearchParams implements Cloneable, ZimbraSearchParams {

    private static final int DEFAULT_LIMIT = 10; // Default limit per query
    private static final int MAX_OFFSET = 10000000; // 10M
    private static final int MAX_PARSABLE_LIMIT = 1000; // 1K
    private static final int MAX_LIMIT = 10000000; // 10M

    private final static Pattern LOCALE_PATTERN = Pattern.compile("([a-zA-Z]{2})(?:[-_]([a-zA-Z]{2})([-_](.+))?)?");

    private ZimbraSoapContext requestContext;
    private Account account;

    /**
     * this parameter is intentionally NOT encoded into XML, it is encoded manually by the ProxiedQueryResults proxying
     * code.
     */
    private int hopCount = 0;
    private String defaultField = "content:";
    private String queryString;
    private int offset;
    private int limit;
    private ExpandResults inlineRule;
    private boolean markRead = false;
    private int maxInlinedLength;
    private boolean wantHtml = false;
    private boolean wantExpandGroupInfo = false;
    private boolean neuterImages = false;
    private Set<String> inlinedHeaders;
    private OutputParticipants recipients;
    private long calItemExpandStart = -1;
    private long calItemExpandEnd = -1;
    private boolean inDumpster = false;  // search live data or dumpster data
    private boolean fullConversation = false;  // All messages in a matching conversation should be returned
    private boolean includeMemberOf = false;  // use to include info on which contact groups a contact is in
    private MsgContent wantContent;

    /** If FALSE, then items with the \Deleted tag set are not returned. */
    private boolean includeTagDeleted = false;
    /** If FALSE, then items with the \Muted tag set are not returned. */
    private boolean includeTagMuted = true;
    private Set<TaskHit.Status> allowableTaskStatuses; // if NULL, allow all

    /**
     * timezone that the query should be parsed in (for date/time queries).
     */
    private TimeZone timezone;
    private Locale locale;
    private SortBy sortBy;
    private Set<MailItem.Type> types = EnumSet.noneOf(MailItem.Type.class); // types to seach for
    private Cursor cursor;
    private boolean prefetch = true;
    private Fetch fetch = Fetch.NORMAL;
    private boolean quick = false; // whether or not to skip the catch-up index prior to search
    private boolean logSearch = true; // whether or not to log the search in search history

    public boolean isQuick() {
        return quick;
    }

    public void setQuick(boolean value) {
        quick = value;
    }

    public ZimbraSoapContext getRequestContext() {
        return requestContext;
    }

    public int getHopCount() {
        return hopCount;
    }

    public long getCalItemExpandStart() {
        return calItemExpandStart;
    }

    public long getCalItemExpandEnd() {
        return calItemExpandEnd;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    public Set<MailItem.Type> getTypes() {
        return types;
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public ExpandResults getInlineRule() {
        return inlineRule;
    }

    public boolean getMarkRead() {
        return markRead;
    }

    public int getMaxInlinedLength() {
        return maxInlinedLength;
    }

    public boolean getWantHtml() {
        return wantHtml;
    }

    public boolean getWantExpandGroupInfo() {
        return wantExpandGroupInfo;
    }

    public boolean getNeuterImages() {
        return neuterImages;
    }

    public Set<String> getInlinedHeaders() {
        return inlinedHeaders;
    }

    public OutputParticipants getWantRecipients() {
        return recipients;
    }

    @Override
    public TimeZone getTimeZone() {
        return timezone;
    }

    public Locale getLocale() {
        return locale;
    }

    @Override
    public boolean getPrefetch() {
        return prefetch;
    }

    public Fetch getFetchMode() {
        return fetch;
    }

    public String getDefaultField() {
        return defaultField;
    }

    @Override
    public final boolean getIncludeTagDeleted() {
        return includeTagDeleted;
    }

    public final boolean getIncludeTagMuted() {
        return includeTagMuted;
    }

    public Set<TaskHit.Status> getAllowableTaskStatuses() {
        return allowableTaskStatuses;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public boolean inDumpster() {
        return inDumpster;
    }

    public void setInDumpster(boolean value) {
        inDumpster = value;
    }

    public boolean fullConversation() {
        return fullConversation;
    }

    public void setFullConversation(boolean value) {
        fullConversation = value;
    }

    public boolean includeMemberOf() {
        return includeMemberOf;
    }

    public void setIncludeMemberOf(boolean value) {
        includeMemberOf = value;
    }

    public MsgContent getWantContent() {
        return wantContent;
    }

    public void setHopCount(int value) {
        hopCount = value;
    }

    @Override
    public void setQueryString(String value) {
        queryString = value;
    }

    public void setOffset(int value) {
        offset = Math.min(value, MAX_OFFSET);
    }

    @Override
    public void setLimit(int value) {
        limit = Math.min(value, MAX_LIMIT);
    }

    public void setDefaultField(String value) {
        if (!value.endsWith(":")) {
            value = value + ':'; // MUST end with the ':'
        }
        defaultField = value;
    }

    @Override
    public final void setIncludeTagDeleted(boolean value) {
        includeTagDeleted = value;
    }

    public final void setIncludeTagMuted(boolean value) {
        includeTagMuted = value;
    }

    public void setAllowableTaskStatuses(Set<TaskHit.Status> value) {
        allowableTaskStatuses = value;
    }

    /**
     * Set the range of dates over which we want to expand out the instances of any returned CalendarItem objects.
     */
    public void setCalItemExpandStart(long value) {
        calItemExpandStart = value;
    }

    /**
     * Set the range of dates over which we want to expand out the instances of any returned CalendarItem objects.
     */
    public void setCalItemExpandEnd(long value) {
        calItemExpandEnd = value;
    }

    /**
     * Since the results are iterator-based, the {@code limit} is really the same as the {@code chunk size + offset}
     * i.e. the limit is used to tell the system approximately how many results you want and it tries to get them in a
     * single chunk, but it isn't until you do the results iteration that the limit is enforced.
     */
    public void setChunkSize(int value) {
        setLimit(value + offset);
    }

    public void setTypes(String value) throws ServiceException {
        try {
            setTypes(MailItem.Type.setOf(value));
        } catch (IllegalArgumentException e) {
            throw MailServiceException.INVALID_TYPE(e.getMessage());
        }
    }

    public void setTypes(Set<MailItem.Type> value) {
        types = value;
        checkForLocalizedContactSearch();
    }

    private boolean isSystemDefaultLocale() {
        if (locale == null) {
            return true;
        }
        // Gets the current value of the default locale for this instance of the Java Virtual Machine.
        return locale.equals(Locale.getDefault());
    }

    private void checkForLocalizedContactSearch() {
        if (DebugConfig.enableContactLocalizedSort) {
            // FIXME: for bug 41920, disable localized contact sorting
            // bug 22665 - if searching ONLY for contacts, and locale is not EN, used localized re-sort
            if (types.size() == 1 && types.contains(MailItem.Type.CONTACT) && !isSystemDefaultLocale()) {
                if (locale != null) {
                    if (sortBy != null) {
                        switch (sortBy) {
                            case NAME_ASC:
                                sortBy = SortBy.NAME_LOCALIZED_ASC;
                                break;
                            case NAME_DESC:
                                sortBy = SortBy.NAME_LOCALIZED_DESC;
                                break;
                        }
                    }
                }
            }
        }
    }

    public void setSortBy(SortBy value) {
        sortBy = value;
        checkForLocalizedContactSearch();
    }

    public void setSortBy(String value) {
        SortBy sort = SortBy.of(value);
        if (sort == null) {
            sort = account.isDefaultSortByRelevance() ? SortBy.RELEVANCE_DESC : SortBy.DATE_DESC;
        }
        setSortBy(sort);
    }

    public void setInlineRule(ExpandResults value) {
        inlineRule = value;
    }

    public void setMarkRead(boolean value) {
        markRead = value;
    }

    public void setMaxInlinedLength(int value) {
        maxInlinedLength = value;
    }

    public void setWantHtml(boolean value) {
        wantHtml = value;
    }

    public void setWantExpandGroupInfo(boolean value) {
        wantExpandGroupInfo = value;
    }

    public void setNeuterImages(boolean value) {
        neuterImages = value;
    }

    public void addInlinedHeader(String value) {
        if (inlinedHeaders == null) {
            inlinedHeaders = new HashSet<String>();
        }
        inlinedHeaders.add(value);
    }

    public void setWantRecipients(WantRecipsSetting jaxbValue) {
        recipients = OutputParticipants.fromJaxb(jaxbValue);
    }

    public void setWantRecipients(Integer value) {
        if (value == null) {
            recipients = OutputParticipants.PUT_SENDERS;
        } else if (2 == value) {
            recipients = OutputParticipants.PUT_BOTH;
        } else if (1 == value) {
            recipients = OutputParticipants.PUT_RECIPIENTS;
        } else {
            recipients = OutputParticipants.PUT_SENDERS;
        }
    }

    @Override
    public void setTimeZone(TimeZone value) {
        timezone = value;
    }

    public void setLocale(Locale value) {
        locale = value;
        checkForLocalizedContactSearch();
    }

    public Cursor getCursor() {
        return cursor;
    }

    /**
     * Sets the cursor, or null to clear.
     */
    public void setCursor(Cursor value) {
        cursor = value;
    }

    @Override
    public void setPrefetch(boolean value) {
        prefetch = value;
    }

    public void setFetchMode(Fetch value) {
        fetch = value;
    }

    public void setWantContent(MsgContent wantContent) {
        this.wantContent = wantContent;
    }

    public void setLogSearch(boolean logSearch) {
        this.logSearch = logSearch;
    }

    /**
     * Encode the necessary parameters into a {@code <SearchRequest>} (or similar element) in cases where we have to
     * proxy a search request over to a remote server.
     * <p>
     * Note that not all parameters are encoded here -- some params (like cursor, etc) are changed by the entity
     * doing the search proxying, and so they are set at that level.
     *
     * @param el This object's parameters are added as attributes (or sub-elements) of this parameter
     */
    public void encodeParams(Element el) {
        if (allowableTaskStatuses != null) {
            el.addAttribute(MailConstants.A_ALLOWABLE_TASK_STATUS, Joiner.on(',').join(allowableTaskStatuses));
        }
        el.addAttribute(MailConstants.A_INCLUDE_TAG_DELETED, getIncludeTagDeleted());
        el.addAttribute(MailConstants.A_INCLUDE_TAG_MUTED, getIncludeTagMuted());
        if (this.includeMemberOf()) {
            el.addAttribute(MailConstants.E_CONTACT_MEMBER_OF /* memberOf */, true);
        }
        el.addAttribute(MailConstants.A_CAL_EXPAND_INST_START, getCalItemExpandStart());
        el.addAttribute(MailConstants.A_CAL_EXPAND_INST_END, getCalItemExpandEnd());
        el.addAttribute(MailConstants.E_QUERY, getQueryString(), Element.Disposition.CONTENT);
        el.addAttribute(MailConstants.A_SEARCH_TYPES, MailItem.Type.toString(types));
        if (sortBy != null) {
            if (SortBy.NAME_LOCALIZED_ASC.equals(sortBy)) {
                el.addAttribute(MailConstants.A_SORTBY, SortBy.NAME_ASC.toString());
            } else if (SortBy.NAME_LOCALIZED_DESC.equals(sortBy)) {
                el.addAttribute(MailConstants.A_SORTBY, SortBy.NAME_DESC.toString());
            } else {
                el.addAttribute(MailConstants.A_SORTBY, sortBy.toString());
            }
        }
        if (getInlineRule() != null) {
            el.addAttribute(MailConstants.A_FETCH, getInlineRule().toString());
        }
        el.addAttribute(MailConstants.A_MARK_READ, getMarkRead());
        el.addAttribute(MailConstants.A_MAX_INLINED_LENGTH, getMaxInlinedLength());
        el.addAttribute(MailConstants.A_WANT_HTML, getWantHtml());
        el.addAttribute(MailConstants.A_NEED_EXP, getWantExpandGroupInfo());
        el.addAttribute(MailConstants.A_NEUTER, getNeuterImages());
        if (getInlinedHeaders() != null) {
            for (String name : getInlinedHeaders()) {
                el.addElement(MailConstants.A_HEADER).addAttribute(MailConstants.A_ATTRIBUTE_NAME, name);
            }
        }
        if (recipients != null) {
            el.addAttribute(MailConstants.A_RECIPIENTS, recipients.getIntValue());
        }

        if (getLocale() != null) {
            el.addElement(MailConstants.E_LOCALE).setText(getLocale().toString());
        }
        el.addAttribute(MailConstants.A_PREFETCH, getPrefetch());
        el.addAttribute(MailConstants.A_RESULT_MODE, getFetchMode().name());
        el.addAttribute(MailConstants.A_FIELD, getDefaultField());

        el.addAttribute(MailConstants.A_QUERY_LIMIT, limit);
        el.addAttribute(MailConstants.A_QUERY_OFFSET, offset);

        el.addAttribute(MailConstants.A_IN_DUMPSTER, inDumpster);
        if (quick) {
            el.addAttribute(MailConstants.A_QUICK, quick);
        }

        if (getWantContent() != null) {
            el.addAttribute(MailConstants.A_WANT_CONTENT, getWantContent().toString());
        }
        el.addAttribute(MailConstants.A_LOG_SEARCH, logSearch);

        // skip cursor data
    }

    /**
     * Parse the search parameters from a {@code <SearchRequest>} or similar element.
     *
     * @param request {@code <SearchRequest>} itself, or similar element ({@code <SearchConvRequest>}, etc)
     * @param requestedAccount account who's mailbox we should search in
     * @param zsc SoapContext of the request
     */
    public static SearchParams parse(SearchParameters soapParams, ZimbraSoapContext zsc, String defaultQueryStr)
            throws ServiceException {
        SearchParams params = new SearchParams();

        params.requestContext = zsc;
        params.account = Provisioning.getInstance().getAccountById(zsc.getAuthtokenAccountId());
        params.setHopCount(zsc.getHopCount());
        params.setCalItemExpandStart(MoreObjects.firstNonNull(soapParams.getCalItemExpandStart(), -1L));
        params.setCalItemExpandEnd(MoreObjects.firstNonNull(soapParams.getCalItemExpandEnd(), -1L));
        String query = soapParams.getQuery() == null ? defaultQueryStr : soapParams.getQuery();
        if (query == null) {
            throw ServiceException.INVALID_REQUEST("no query submitted and no default query found", null);
        }
        params.setQueryString(query);
        params.setInDumpster(MoreObjects.firstNonNull(soapParams.getInDumpster(), false));
        params.setQuick(MoreObjects.firstNonNull(soapParams.getQuick(), false));
        String types = soapParams.getSearchTypes() == null ? soapParams.getGroupBy() : soapParams.getSearchTypes();
        if (Strings.isNullOrEmpty(types)) {
            params.setTypes(EnumSet.of(params.isQuick() ? MailItem.Type.MESSAGE : MailItem.Type.CONVERSATION));
        } else {
            params.setTypes(types);
        }
        params.setSortBy(soapParams.getSortBy());
        if (soapParams.getSortBy() != null && query.toLowerCase().contains("is:unread") && isSortByReadFlag(SortBy.of(soapParams.getSortBy()))) {
            params.setSortBy(SortBy.DATE_DESC);
        } else {
            params.setSortBy(soapParams.getSortBy());
        }
        params.setIncludeTagDeleted(MoreObjects.firstNonNull(soapParams.getIncludeTagDeleted(), false));
        params.setIncludeTagMuted(MoreObjects.firstNonNull(soapParams.getIncludeTagMuted(), true));
        String allowableTasks = soapParams.getAllowableTaskStatus();
        if (allowableTasks != null) {
            params.allowableTaskStatuses = new HashSet<TaskHit.Status>();
            for (String task : Splitter.on(',').split(allowableTasks)) {
                try {
                    TaskHit.Status status = TaskHit.Status.valueOf(task.toUpperCase());
                    params.allowableTaskStatuses.add(status);
                } catch (IllegalArgumentException e) {
                    ZimbraLog.index.debug("Skipping unknown task completion status: %s", task);
                }
            }
        }

        params.setInlineRule(ExpandResults.valueOf(soapParams.getFetch(), zsc));
        if (params.getInlineRule() != ExpandResults.NONE) {
            params.setMarkRead(MoreObjects.firstNonNull(soapParams.getMarkRead(), false));
            params.setMaxInlinedLength(MoreObjects.firstNonNull(soapParams.getMaxInlinedLength(), -1));
            params.setWantHtml(MoreObjects.firstNonNull(soapParams.getWantHtml(), false));
            params.setWantExpandGroupInfo(MoreObjects.firstNonNull(soapParams.getNeedCanExpand(), false));
            params.setNeuterImages(MoreObjects.firstNonNull(soapParams.getNeuterImages(), true));
            for (AttributeName hdr : soapParams.getHeaders()) {
                params.addInlinedHeader(hdr.getName());
            }
        }
        params.setWantRecipients(soapParams.getWantRecipients());

        CalTZInfoInterface calTZ = soapParams.getCalTz();
        if (calTZ != null) {
            params.setTimeZone(parseTimeZonePart(calTZ));
        }

        String locale = soapParams.getLocale();
        if (locale != null) {
            params.setLocale(parseLocale(locale));
        }

        params.setPrefetch(MoreObjects.firstNonNull(soapParams.getPrefetch(), true));
        String resultMode = soapParams.getResultMode();
        if (!Strings.isNullOrEmpty(resultMode)) {
            try {
                params.setFetchMode(Fetch.valueOf(resultMode.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST(
                        String.format("Invalid %s \"%s\"", MailConstants.A_RESULT_MODE, resultMode), e);
            }
        }

        String field = params.getDefaultField();
        if (field != null) {
            params.setDefaultField(field);
        }

        params.setLimit(parseLimit(soapParams.getLimit()));
        params.setOffset(MoreObjects.firstNonNull(soapParams.getOffset(), 0));

        CursorInfo cursor = soapParams.getCursor();
       
        if (cursor != null ) {
            params.parseCursor(cursor, zsc.getRequestedAccountId(), params);
        }

        if (soapParams instanceof MailSearchParams) {
            MailSearchParams mailParams = (MailSearchParams) soapParams;
            params.setFullConversation(ZmBoolean.toBool(mailParams.getFullConversation(), false));
            params.setWantContent(MoreObjects.firstNonNull(mailParams.getWantContent(), MsgContent.full));
            params.setIncludeMemberOf(mailParams.getIncludeMemberOf());
        }

        return params;
    }


    /**
     * @param params
     * @return
     */
    public static boolean isSortByReadFlag(SortBy sortBy) {
        if (sortBy == null) {
            return false;
        }
        return (sortBy.getKey() == SortBy.READ_ASC.getKey() 
            || sortBy.getKey() == SortBy.READ_DESC.getKey());
    }

    /**
     * Parse the search parameters from a {@code <SearchRequest>} or similar element.
     *
     * @param request {@code <SearchRequest>} itself, or similar element ({@code <SearchConvRequest>}, etc)
     * @param requestedAccount account who's mailbox we should search in
     * @param zsc SoapContext of the request
     */
    public static SearchParams parse(Element request, ZimbraSoapContext zsc, String defaultQueryStr)
            throws ServiceException {
        SearchParams params = new SearchParams();

        params.requestContext = zsc;
        params.account = Provisioning.getInstance().getAccountById(zsc.getAuthtokenAccountId());
        params.setHopCount(zsc.getHopCount());
        params.setCalItemExpandStart(request.getAttributeLong(MailConstants.A_CAL_EXPAND_INST_START, -1));
        params.setCalItemExpandEnd(request.getAttributeLong(MailConstants.A_CAL_EXPAND_INST_END, -1));
        String query = request.getAttribute(MailConstants.E_QUERY, defaultQueryStr);
        if (query == null) {
            throw ServiceException.INVALID_REQUEST("no query submitted and no default query found", null);
        }
        params.setQueryString(query);
        params.setInDumpster(request.getAttributeBool(MailConstants.A_IN_DUMPSTER, false));
        params.setQuick(request.getAttributeBool(MailConstants.A_QUICK, false));
        String types = request.getAttribute(MailConstants.A_SEARCH_TYPES, request.getAttribute(MailConstants.A_GROUPBY, null));
        if (Strings.isNullOrEmpty(types)) {
            params.setTypes(EnumSet.of(params.isQuick() ? MailItem.Type.MESSAGE : MailItem.Type.CONVERSATION));
        } else {
            params.setTypes(types);
        }
        params.setSortBy(request.getAttribute(MailConstants.A_SORTBY, null));

        params.setIncludeTagDeleted(request.getAttributeBool(MailConstants.A_INCLUDE_TAG_DELETED, false));
        params.setIncludeTagMuted(request.getAttributeBool(MailConstants.A_INCLUDE_TAG_MUTED, true));
        String allowableTasks = request.getAttribute(MailConstants.A_ALLOWABLE_TASK_STATUS, null);
        if (allowableTasks != null) {
            params.allowableTaskStatuses = new HashSet<TaskHit.Status>();
            for (String task : Splitter.on(',').split(allowableTasks)) {
                try {
                    TaskHit.Status status = TaskHit.Status.valueOf(task.toUpperCase());
                    params.allowableTaskStatuses.add(status);
                } catch (IllegalArgumentException e) {
                    ZimbraLog.index.debug("Skipping unknown task completion status: %s", task);
                }
            }
        }

        params.setInlineRule(ExpandResults.valueOf(request.getAttribute(MailConstants.A_FETCH, null), zsc));
        if (params.getInlineRule() != ExpandResults.NONE) {
            params.setMarkRead(request.getAttributeBool(MailConstants.A_MARK_READ, false));
            params.setMaxInlinedLength((int) request.getAttributeLong(MailConstants.A_MAX_INLINED_LENGTH, -1));
            params.setWantHtml(request.getAttributeBool(MailConstants.A_WANT_HTML, false));
            params.setWantExpandGroupInfo(request.getAttributeBool(MailConstants.A_NEED_EXP, false));
            params.setNeuterImages(request.getAttributeBool(MailConstants.A_NEUTER, true));
            for (Element elt : request.listElements(MailConstants.A_HEADER)) {
                params.addInlinedHeader(elt.getAttribute(MailConstants.A_ATTRIBUTE_NAME));
            }
        }
        params.setWantRecipients((int) request.getAttributeLong(MailConstants.A_RECIPIENTS, 0));

        Element tz = request.getOptionalElement(MailConstants.E_CAL_TZ);
        if (tz != null) {
            params.setTimeZone(parseTimeZonePart(tz));
        }

        Element locale = request.getOptionalElement(MailConstants.E_LOCALE);
        if (locale != null) {
            params.setLocale(parseLocale(locale.getText()));
        }

        params.setPrefetch(request.getAttributeBool(MailConstants.A_PREFETCH, true));
        String fetch = request.getAttribute(MailConstants.A_RESULT_MODE, null);
        if (!Strings.isNullOrEmpty(fetch)) {
            try {
                params.setFetchMode(Fetch.valueOf(fetch.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("Invalid " + MailConstants.A_RESULT_MODE, e);
            }
        }

        String field = request.getAttribute(MailConstants.A_FIELD, null);
        if (field != null) {
            params.setDefaultField(field);
        }

        params.setLimit(parseLimit(request));
        params.setOffset(request.getAttributeInt(MailConstants.A_QUERY_OFFSET, 0));

        Element cursor = request.getOptionalElement(MailConstants.E_CURSOR);
        if (cursor != null) {
            params.parseCursor(cursor, zsc.getRequestedAccountId(), params);
        }

        params.setWantContent(MoreObjects.firstNonNull(
            MsgContent.fromString(request.getAttribute(MailConstants.A_WANT_CONTENT, null)),
            MsgContent.full));

        return params;
    }

    /**
     * Parse a cursor element.
     *
     * @param cursor cursor element taken from a {@code <SearchRequest>}
     * @param acctId requested account id
     */
    public void parseCursor(Element el, String acctId, SearchParams params) throws ServiceException {
        cursor = new Cursor();
        cursor.itemId = new ItemId(el.getAttribute(MailConstants.A_ID), acctId);
        if (!isSortByReadFlag(params.getSortBy())) {
        cursor.sortValue = el.getAttribute(MailConstants.A_SORTVAL, null); // optional
        cursor.endSortValue = el.getAttribute(MailConstants.A_ENDSORTVAL, null); // optional
        }
        cursor.includeOffset = el.getAttributeBool(MailConstants.A_INCLUDE_OFFSET, false); // optional
    }

    /**
     * Parse a cursor element.
     *
     * @param cursorInfo cursor element taken from a {@code <SearchRequest>}
     * @param acctId requested account id
     */
    public void parseCursor(CursorInfo cursorInfo, String acctId,  SearchParams params) throws ServiceException {
        cursor = new Cursor();
        if (null == cursorInfo.getId()) {
                throw ServiceException.INVALID_REQUEST("Invalid ID for " + MailConstants.E_CURSOR, null);
        }
        cursor.itemId = new ItemId(cursorInfo.getId(), acctId);
        if (!isSortByReadFlag(params.getSortBy())) {
            cursor.sortValue = cursorInfo.getSortVal(); // optional
            cursor.endSortValue = cursorInfo.getEndSortVal(); // optional
        }
       
        cursor.includeOffset = MoreObjects.firstNonNull(cursorInfo.getIncludeOffset(), false); // optional
    }

    private static TimeZone parseTimeZonePart(CalTZInfoInterface calTZ) throws ServiceException {
        String id = calTZ.getId();

        // is it a well-known timezone?  if so then we're done here
        ICalTimeZone knownTZ = WellKnownTimeZones.getTimeZoneById(id);
        if (knownTZ != null) {
            return knownTZ;
        }

        // custom timezone!
        return CalendarUtils.parseTimeZone(calTZ);
    }

    private static TimeZone parseTimeZonePart(Element tzElt) throws ServiceException {
        String id = tzElt.getAttribute(MailConstants.A_ID);

        // is it a well-known timezone?  if so then we're done here
        ICalTimeZone knownTZ = WellKnownTimeZones.getTimeZoneById(id);
        if (knownTZ != null) {
            return knownTZ;
        }

        // custom timezone!

        String test = tzElt.getAttribute(MailConstants.A_CAL_TZ_STDOFFSET, null);
        if (test == null) {
            throw ServiceException.INVALID_REQUEST("Unknown TZ: \"" + id +
                    "\" and no " + MailConstants.A_CAL_TZ_STDOFFSET + " specified", null);
        }

        return CalendarUtils.parseTzElement(tzElt);
    }

    static Locale parseLocale(String src) {
        if (Strings.isNullOrEmpty(src)) {
            return null;
        }
        Matcher matcher = LOCALE_PATTERN.matcher(src);
        if (matcher.lookingAt()) {
            String lang = null;
            String country = null;
            String variant = null;
            if (matcher.start(1) >= 0) {
                lang = matcher.group(1);
            }

            if (Strings.isNullOrEmpty(lang)) {
                return null;
            }

            if (matcher.start(2) >= 0) {
                country = matcher.group(2);
            }

            if (matcher.start(4) >= 0) {
                variant = matcher.group(4);
            }

            if (Strings.isNullOrEmpty(country)) {
                return new Locale(lang);
            } else if (Strings.isNullOrEmpty(variant)) {
                return new Locale(lang, country);
            } else {
                return new Locale(lang, country, variant);
            }
        }
        return null;
    }

    private static int parseLimit(Integer limit) throws ServiceException {
        if ((null == limit) || (limit <= 0)) {
            return DEFAULT_LIMIT;
        } else if (limit > MAX_PARSABLE_LIMIT) {
            return MAX_PARSABLE_LIMIT;
        }
        return limit;
    }

    private static int parseLimit(Element request) throws ServiceException {
        return parseLimit(request.getAttributeInt(MailConstants.A_QUERY_LIMIT, -1));
    }

    @Override
    public Object clone() {
        SearchParams result = new SearchParams();
        result.requestContext = requestContext;
        result.account = account;
        result.hopCount = hopCount;
        result.defaultField = defaultField;
        result.queryString = queryString;
        result.offset = offset;
        result.limit = limit;
        result.inlineRule = inlineRule;
        result.maxInlinedLength = maxInlinedLength;
        result.wantHtml = wantHtml;
        result.wantExpandGroupInfo = wantExpandGroupInfo;
        result.neuterImages = neuterImages;
        result.inlinedHeaders = inlinedHeaders;
        result.recipients = recipients;
        result.calItemExpandStart = calItemExpandStart;
        result.calItemExpandEnd = calItemExpandEnd;
        result.includeTagDeleted = includeTagDeleted;
        result.includeTagMuted = includeTagMuted;
        result.includeMemberOf = includeMemberOf;
        result.timezone = timezone;
        result.locale = locale;
        result.sortBy = sortBy;
        result.types = types;
        result.prefetch = prefetch;
        result.fetch = fetch;
        if (allowableTaskStatuses != null) {
            result.allowableTaskStatuses = new HashSet<TaskHit.Status>(allowableTaskStatuses);
        }
        if (cursor != null) {
            result.cursor = new Cursor(cursor);
        }
        result.inDumpster = inDumpster;
        return result;
    }

    public static final class ExpandResults {
        /**
         * Don't expand any hits.
         */
        public static final ExpandResults NONE = new ExpandResults("none");

        /**
         * Expand the first hit.
         */
        public static final ExpandResults FIRST = new ExpandResults("first");

        /**
         * Expand all unread messages.
         */
        public static final ExpandResults UNREAD = new ExpandResults("unread");

        /**
         * Expand all unread messages, or the first hit if there are no unread messages.
         */
        public static final ExpandResults UNREAD_FIRST = new ExpandResults("unread-first");

        /**
         * For searchConv, expand the members of the conversation that match the search.
         */
        public static final ExpandResults HITS = new ExpandResults("hits");

        /**
         * Expand ALL hits.
         */
        public static final ExpandResults ALL = new ExpandResults("all");

        /**
         * For searchConv: expand only the first message in the conversation
         */
        public static final ExpandResults FIRST_MSG = new ExpandResults("first-msg");

        /**
         * For searchConv: if there are matching hits, expand them, otherwise expand the first message in the conversation
         */
        public static final ExpandResults HITS_OR_FIRST_MSG = new ExpandResults("hits!");

        /**
         * For searchConv: if there are unread matching hits, expand them, otherwise expand first message in the conversation
         * This is a little ambiguous - should 1st msg be expanded if there are no matching hits at all, or if there are no UNREAD matching hits?
         * For now, expand 1st msg if there are no UNREAD matching hits (or no matching hits at all, obviously)
         */
        public static final ExpandResults U_OR_FIRST_MSG = new ExpandResults("u!");

        /**
         * For searchConv: if there are unread matching hits, expand them, otherwise expand first hit AND first message in conversation
         * Somewhat ambiguous: what to do if no matching hits? For now, expand the first message.
         */
        public static final ExpandResults U1_OR_FIRST_MSG = new ExpandResults("u1!");

        private static final Map<String, ExpandResults> MAP = ImmutableMap.<String, ExpandResults>builder()
            .put(NONE.name, NONE).put("0", NONE).put("false", NONE)
            .put(FIRST.name, FIRST).put("1", FIRST)
            .put(UNREAD.name, UNREAD).put("u", UNREAD)
            .put(UNREAD_FIRST.name, UNREAD_FIRST).put("u1", UNREAD_FIRST)
            .put(HITS.name, HITS)
            .put(ALL.name, ALL)
            .put(FIRST_MSG.name, FIRST_MSG).put("!",FIRST_MSG)
            .put(HITS_OR_FIRST_MSG.name,HITS_OR_FIRST_MSG)
            .put(U_OR_FIRST_MSG.name,U_OR_FIRST_MSG)
            .put(U1_OR_FIRST_MSG.name,U1_OR_FIRST_MSG).put("u!1",U1_OR_FIRST_MSG)
            .build();

        private final String name;
        private List<ItemId> itemIds;

        private ExpandResults(String name) {
            this.name = name;
        }

        private ExpandResults(String name,List<ItemId> ids)
        {
            this.name = name;
            this.itemIds = ids;
        }
        public boolean matches(MailItem item) {
            return itemIds != null && item != null && matches(new ItemId(item));
        }

        public boolean matches(ItemId id) {
            return itemIds != null && itemIds.contains(id);
        }

        public static ExpandResults valueOf(String value, ZimbraSoapContext zsc) throws ServiceException {
            if (value == null) {
                return NONE;
            }
            value = value.toLowerCase();
            ExpandResults result = MAP.get(value);
            if (result != null) {
                return result;
            }
            try {
                String[] split = value.split(",");
                ArrayList<ItemId> itemIds = new ArrayList<ItemId>();
                for (int i = 0; i < split.length; i++) {
                    itemIds.add(new ItemId(split[i],zsc));
                }
                return new ExpandResults(value,itemIds);
            } catch (Exception e) {
                throw ServiceException.INVALID_REQUEST("invalid 'fetch' value: " + value, null);
            }
        }

        @Override
        public String toString() {
            return name;
        }

        private static final Map<String, ExpandResults> LEGACY_MAP = ImmutableMap.<String, ExpandResults>builder()
                .put(FIRST.name, FIRST).put("1", FIRST)
                .put(HITS.name, HITS)
                .put(ALL.name, ALL)
                .build();

        public ExpandResults toLegacyExpandResults(Server server) {
            if (server != null && server.getServerVersion() == null) {
                if (LEGACY_MAP.containsKey(this.name)) {
                    return this;
                } else {
                    //pre 8.5; no way to know which version
                    //assume HELIX as lowest common - fetch="1|hits|all|{item-id}"
                    ExpandResults mapped = ALL;
                    if (this == FIRST_MSG || this == U_OR_FIRST_MSG || this == U1_OR_FIRST_MSG) {
                        mapped = FIRST;
                    } else if (this == HITS_OR_FIRST_MSG) {
                        mapped = HITS;
                    }
                    ZimbraLog.search.debug("mapped current ExpandResults %s to %s for legacy server %s", this, mapped, server.getName());
                    return mapped;
                }
            } else {
                //for now 8.5+ supports the same set of expands; would add code here if 8.6 or 9.0 changes it
                return this;
            }
        }
    }

    /**
     * A cursor can be specified by itemId and sortValue. These should be enough for us to find out place in the
     * previous result set, even if entries have been added or removed from the result set. If the client doesn't know
     * sortValue, e.g. changing the sort field from date to subject, it may leave it null, then the server fetches the
     * item by the specified itemId, and sets the sortValue accordingly. If the item no longer exist when fetching it,
     * the cursor gets cleared.
     */
    public static final class Cursor {
        private ItemId itemId; // item ID of the last item in the previous result set
        private String sortValue; // sort value of the last item in the previous result set
        private String endSortValue; // sort value (exclusive) to stop the cursor
        private boolean includeOffset = false; // whether or not to include the cursor offset in the response

        private Cursor() {
        }

        private Cursor(Cursor src) {
            itemId = src.itemId;
            sortValue = src.sortValue;
            endSortValue = src.endSortValue;
            includeOffset = src.includeOffset;
        }

        public ItemId getItemId() {
            return itemId;
        }

        public String getSortValue() {
            return sortValue;
        }

        public void setSortValue(String value) {
            sortValue = value;
        }

        public String getEndSortValue() {
            return endSortValue;
        }

        public boolean isIncludeOffset() {
            return includeOffset;
        }
    }

    public enum Fetch {
        /* Everything. */
        NORMAL(ZimbraFetchMode.NORMAL),
        /* Only IMAP data. */
        IMAP(ZimbraFetchMode.IMAP),
        /* Only the metadata modification sequence number. */
        MODSEQ(ZimbraFetchMode.MODSEQ),
        /* Only the ID of the item's parent (-1 if no parent). */
        PARENT(ZimbraFetchMode.PARENT),
        /* Only ID. */
        IDS(ZimbraFetchMode.IDS);

        private final ZimbraFetchMode zfm;

        private Fetch(ZimbraFetchMode zimbraFetchMode) {
            zfm = zimbraFetchMode;
        }

        public static Fetch fromZimbraFetchMode(ZimbraFetchMode zimbraFetchMode) {
            for (Fetch typ :Fetch.values()) {
                if (typ.zfm == zimbraFetchMode) {
                    return typ;
                }
            }
            throw new IllegalArgumentException("Unrecognised ZimbraFetchMode:" + zimbraFetchMode);
        }

        public ZimbraFetchMode toZimbraFetchMode() {
            return zfm;
        }
    }

    @Override
    public Set<MailItemType> getMailItemTypes() {
        return MailItem.Type.toCommon(types);
    }

    @Override
    public ZimbraSearchParams setMailItemTypes(Set<MailItemType> values) {
        types = MailItem.Type.fromCommon(values);
        return this;
    }

    @Override
    public ZimbraSortBy getZimbraSortBy() {
        if (null == sortBy) {
            return ZimbraSortBy.none;
        }
        return sortBy.toZimbraSortBy();
    }

    @Override
    public ZimbraSearchParams setZimbraSortBy(ZimbraSortBy value) {
        if (null == value) {
            setSortBy(SortBy.NONE);
        }
        setSortBy(SortBy.fromZimbraSortBy(value));
        return this;
    }

    @Override
    public ZimbraFetchMode getZimbraFetchMode() {
        Fetch fet =  this.getFetchMode();
        if (null == fet) {
            return ZimbraFetchMode.NORMAL;
        }
        return fet.toZimbraFetchMode();
    }

    @Override
    public ZimbraSearchParams setZimbraFetchMode(ZimbraFetchMode value) {
        setFetchMode(Fetch.fromZimbraFetchMode(value));
        return this;
    }

}
