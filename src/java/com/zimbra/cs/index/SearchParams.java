/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

package com.zimbra.cs.index;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.mail.CalendarUtils;
import com.zimbra.cs.service.mail.ToXML.OutputParticipants;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * Encapsulates all parameters in a search request.
 * <p>
 * IMPORTANT NOTE: if you add new parameters, you MUST add parsing/serialization code to the
 * {@link #encodeParams(Element)} and {@link #parse(Element, ZimbraSoapContext, String)}) APIs.
 * This IS NOT optional and will break cross-server search if you do not comply.
 */
public final class SearchParams implements Cloneable {

    private static final int MAX_OFFSET = 10000000; // 10M
    private static final int MAX_LIMIT = 10000000; // 10M
    private final static Pattern LOCALE_PATTERN = Pattern.compile("([a-zA-Z]{2})(?:[-_]([a-zA-Z]{2})([-_](.+))?)?");

    private ZimbraSoapContext requestContext;

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
    private boolean recipients = false;
    private long calItemExpandStart = -1;
    private long calItemExpandEnd = -1;
    private boolean inDumpster = false;  // search live data or dumpster data

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
        return recipients ? OutputParticipants.PUT_RECIPIENTS : OutputParticipants.PUT_SENDERS;
    }

    public TimeZone getTimeZone() {
        return timezone;
    }

    public Locale getLocale() {
        return locale;
    }

    public boolean getPrefetch() {
        return prefetch;
    }

    public Fetch getFetchMode() {
        return fetch;
    }

    public String getDefaultField() {
        return defaultField;
    }

    public final boolean getIncludeTagDeleted() {
        return includeTagDeleted;
    }

    public final boolean getIncludeTagMuted() {
        return includeTagMuted;
    }

    public Set<TaskHit.Status> getAllowableTaskStatuses() {
        return allowableTaskStatuses;
    }

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

    public void setHopCount(int value) {
        hopCount = value;
    }

    public void setQueryString(String value) {
        queryString = value;
    }

    public void setOffset(int value) {
        offset = Math.min(value, MAX_OFFSET);
    }

    public void setLimit(int value) {
        limit = Math.min(value, MAX_LIMIT);
    }

    public void setDefaultField(String value) {
        if (!value.endsWith(":")) {
            value = value + ':'; // MUST end with the ':'
        }
        defaultField = value;
    }

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
            sort = SortBy.DATE_DESC;
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

    public void setWantRecipients(boolean value) {
        recipients = value;
    }

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

    public void setPrefetch(boolean value) {
        prefetch = value;
    }

    public void setFetchMode(Fetch value) {
        fetch = value;
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
        el.addAttribute(MailConstants.A_CAL_EXPAND_INST_START, getCalItemExpandStart());
        el.addAttribute(MailConstants.A_CAL_EXPAND_INST_END, getCalItemExpandEnd());
        el.addAttribute(MailConstants.E_QUERY, getQueryString(), Element.Disposition.CONTENT);
        el.addAttribute(MailConstants.A_SEARCH_TYPES, MailItem.Type.toString(types));
        if (sortBy != null) {
            el.addAttribute(MailConstants.A_SORTBY, sortBy.toString());
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
        el.addAttribute(MailConstants.A_RECIPIENTS, recipients);

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

        // skip cursor data
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
        params.setWantRecipients(request.getAttributeBool(MailConstants.A_RECIPIENTS, false));

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
            params.parseCursor(cursor, zsc.getRequestedAccountId());
        }

        return params;
    }

    /**
     * Parse a cursor element.
     *
     * @param cursor cursor element taken from a {@code <SearchRequest>}
     * @param acctId requested account id
     */
    public void parseCursor(Element el, String acctId) throws ServiceException {
        cursor = new Cursor();
        cursor.itemId = new ItemId(el.getAttribute(MailConstants.A_ID), acctId);
        cursor.sortValue = el.getAttribute(MailConstants.A_SORTVAL, null); // optional
        cursor.endSortValue = el.getAttribute(MailConstants.A_ENDSORTVAL, null); // optional
        cursor.includeOffset = el.getAttributeBool(MailConstants.A_INCLUDE_OFFSET, false); // optional
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

    private static int parseLimit(Element request) throws ServiceException {
        int limit = request.getAttributeInt(MailConstants.A_QUERY_LIMIT, -1);
        if (limit <= 0) {
            limit = 10;
        } else if (limit > 1000) {
            limit = 1000;
        }
        return limit;
    }

    @Override
    public Object clone() {
        SearchParams result = new SearchParams();
        result.requestContext = requestContext;
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

        private static final Map<String, ExpandResults> MAP = ImmutableMap.<String, ExpandResults>builder()
            .put(NONE.name, NONE).put("0", NONE).put("false", NONE)
            .put(FIRST.name, FIRST).put("1", FIRST)
            .put(UNREAD.name, UNREAD).put("u", UNREAD)
            .put(UNREAD_FIRST.name, UNREAD_FIRST).put("u1", UNREAD_FIRST)
            .put(HITS.name, HITS)
            .put(ALL.name, ALL)
            .build();

        private final String name;
        private ItemId itemId;

        private ExpandResults(String name) {
            this.name = name;
        }

        private ExpandResults(String name, ItemId id) {
            this.name = name;
            this.itemId = id;
        }

        public boolean matches(MailItem item) {
            return itemId != null && item != null && matches(new ItemId(item));
        }

        public boolean matches(ItemId id) {
            return id != null && id.equals(itemId);
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
                return new ExpandResults(value, new ItemId(value, zsc));
            } catch (Exception e) {
                throw ServiceException.INVALID_REQUEST("invalid 'fetch' value: " + value, null);
            }
        }

        @Override
        public String toString() {
            return name;
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
        NORMAL,
        /* Only IMAP data. */
        IMAP,
        /* Only the metadata modification sequence number. */
        MODSEQ,
        /* Only the ID of the item's parent (-1 if no parent). */
        PARENT,
        /* Only ID. */
        IDS;
    }

}
