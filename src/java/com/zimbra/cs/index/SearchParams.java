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
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.WellKnownTimeZones;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.mail.CalendarUtils;
import com.zimbra.cs.service.mail.Search;
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

    public static final class ExpandResults {
        /**
         * Don't expand any hits.
         */
        public static ExpandResults NONE = new ExpandResults("none");

        /**
         * Expand the first hit.
         */
        public static ExpandResults FIRST = new ExpandResults("first");

        /**
         * For searchConv, expand the members of the conversation that match
         * the search.
         */
        public static ExpandResults HITS = new ExpandResults("hits");

        /**
         * Expand ALL hits.
         */
        public static ExpandResults ALL = new ExpandResults("all");

        private final String mRep;
        private ItemId mItemId;

        private ExpandResults(String rep) {
            mRep = rep;
        }

        private ExpandResults setId(ItemId iid) {
            mItemId = iid;
            return this;
        }

        public boolean matches(MailItem item) {
            return mItemId != null && item != null && matches(new ItemId(item));
        }

        public boolean matches(ItemId iid) {
            return iid != null && iid.equals(mItemId);
        }

        public static ExpandResults valueOf(String value, ZimbraSoapContext zsc)
            throws ServiceException {

            if (value == null) {
                return NONE;
            }

            value = value.trim().toLowerCase();
            if (value.equals("none") || value.equals("0") || value.equals("false")) {
                return NONE;
            } else if (value.equals("first") || value.equals("1")) {
                return FIRST;
            } else if (value.equals("hits")) {
                return HITS;
            } else if (value.equals("all")) {
                return ALL;
            }

            ItemId iid = null;
            try {
                iid = new ItemId(value, zsc);
            } catch (Exception e) {
            }
            if (iid != null) {
                return new ExpandResults(value).setId(iid);
            } else {
                throw ServiceException.INVALID_REQUEST(
                        "invalid 'fetch' value: " + value, null);
            }
        }

        @Override
        public String toString() {
            return mRep;
        }
    }

    public ZimbraSoapContext getRequestContext() {
        return mRequestContext;
    }

    public int getHopCount() {
        return mHopCount;
    }

    public long getCalItemExpandStart() {
        return mCalItemExpandStart;
    }

    public long getCalItemExpandEnd() {
        return mCalItemExpandEnd;
    }

    public String getQueryStr() {
        return mQueryStr;
    }

    public Set<MailItem.Type> getTypes() {
        return types;
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public ExpandResults getInlineRule() {
        return mInlineRule;
    }

    public boolean getMarkRead() {
        return mMarkRead;
    }

    public int getMaxInlinedLength() {
        return mMaxInlinedLength;
    }

    public boolean getWantHtml() {
        return mWantHtml;
    }

    public boolean getNeuterImages() {
        return mNeuterImages;
    }

    public Set<String> getInlinedHeaders() {
        return mInlinedHeaders;
    }

    public OutputParticipants getWantRecipients() {
        return mRecipients ? OutputParticipants.PUT_RECIPIENTS : OutputParticipants.PUT_SENDERS;
    }

    public TimeZone getTimeZone() {
        return mTimeZone;
    }

    public Locale getLocale() {
        return locale;
    }

    public boolean getPrefetch() {
        return mPrefetch;
    }

    public Mailbox.SearchResultMode getMode() {
        return mMode;
    }

    public String getDefaultField() {
        return mDefaultField;
    }

    public final boolean getIncludeTagDeleted() {
        return mIncludeTagDeleted;
    }

    public Set<TaskHit.Status> getAllowableTaskStatuses() {
        return mAllowableTaskStatuses;
    }

    public int getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    public boolean inDumpster() {
        return mInDumpster;
    }

    public void setInDumpster(boolean inDumpster) {
        mInDumpster = inDumpster;
    }

    public void setHopCount(int hopCount) {
        mHopCount = hopCount;
    }

    public void setQueryStr(String queryStr) {
        mQueryStr = queryStr;
    }

    public void setOffset(int value) {
        offset = Math.min(value, MAX_OFFSET);
    }

    public void setLimit(int value) {
        limit = Math.min(value, MAX_LIMIT);
    }

    public void setDefaultField(String field) {
        // yes, it MUST end with the ':'
        if (field.charAt(field.length()-1) != ':') {
            field = field + ':';
        }
        mDefaultField = field;
    }

    public final void setIncludeTagDeleted(boolean includeTagDeleted) {
        mIncludeTagDeleted = includeTagDeleted;
    }

    public void setAllowableTaskStatuses(Set<TaskHit.Status> statuses) {
        mAllowableTaskStatuses = statuses;
    }

    /**
     * Set the range of dates over which we want to expand out the instances of
     * any returned CalendarItem objects.
     *
     * @param calItemExpandStart
     */
    public void setCalItemExpandStart(long calItemExpandStart) {
        mCalItemExpandStart = calItemExpandStart;
    }

    /**
     * Set the range of dates over which we want to expand out the instances of
     * any returned CalendarItem objects.
     *
     * @param calItemExpandStart
     */
    public void setCalItemExpandEnd(long calItemExpandEnd) {
        mCalItemExpandEnd = calItemExpandEnd;
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

    public void setInlineRule(ExpandResults fetch) {
        mInlineRule = fetch;
    }

    public void setMarkRead(boolean read) {
        mMarkRead = read;
    }

    public void setMaxInlinedLength(int maxSize) {
        mMaxInlinedLength = maxSize;
    }

    public void setWantHtml(boolean html) {
        mWantHtml = html;
    }

    public void setNeuterImages(boolean neuter) {
        mNeuterImages = neuter;
    }

    public void addInlinedHeader(String name) {
        if (mInlinedHeaders == null) {
            mInlinedHeaders = new HashSet<String>();
        }
        mInlinedHeaders.add(name);
    }

    public void setWantRecipients(boolean recips) {
        mRecipients = recips;
    }

    public void setTimeZone(TimeZone tz) {
        mTimeZone = tz;
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

    public void setPrefetch(boolean truthiness) {
        mPrefetch = truthiness;
    }

    public void setMode(Mailbox.SearchResultMode mode) {
        mMode = mode;
    }

    /**
     * Encode the necessary parameters into a <SearchRequest> (or similar
     * element) in cases where we have to proxy a search request over to
     * a remote server.
     * <p>
     * Note that not all parameters are encoded here -- some params (like
     * offset, limit, etc) are changed by the entity doing the search proxying,
     * and so they are set at that level.
     *
     * @param searchElt This object's parameters are added as attributes (or
     *  sub-elements) of this parameter
     */
    public void encodeParams(Element searchElt) {
        if (mAllowableTaskStatuses != null) {
            StringBuilder taskStatusStr = new StringBuilder();
            for (TaskHit.Status s : mAllowableTaskStatuses) {
                if (taskStatusStr.length() > 0) {
                    taskStatusStr.append(",");
                }
                taskStatusStr.append(s.name());
            }
            searchElt.addAttribute(MailConstants.A_ALLOWABLE_TASK_STATUS,
                    taskStatusStr.toString());
        }
        searchElt.addAttribute(MailConstants.A_INCLUDE_TAG_DELETED,
                getIncludeTagDeleted());
        searchElt.addAttribute(MailConstants.A_CAL_EXPAND_INST_START,
                getCalItemExpandStart());
        searchElt.addAttribute(MailConstants.A_CAL_EXPAND_INST_END,
                getCalItemExpandEnd());
        searchElt.addAttribute(MailConstants.E_QUERY, getQueryStr(),
                Element.Disposition.CONTENT);
        searchElt.addAttribute(MailConstants.A_SEARCH_TYPES, MailItem.Type.toString(types));
        if (sortBy != null) {
            searchElt.addAttribute(MailConstants.A_SORTBY, sortBy.toString());
        }
        if (getInlineRule() != null)
            searchElt.addAttribute(MailConstants.A_FETCH,
                    getInlineRule().toString());
        searchElt.addAttribute(MailConstants.A_MARK_READ, getMarkRead());
        searchElt.addAttribute(MailConstants.A_MAX_INLINED_LENGTH,
                getMaxInlinedLength());
        searchElt.addAttribute(MailConstants.A_WANT_HTML, getWantHtml());
        searchElt.addAttribute(MailConstants.A_NEUTER, getNeuterImages());
        if (getInlinedHeaders() != null) {
            for (String name : getInlinedHeaders())
                searchElt.addElement(MailConstants.A_HEADER).addAttribute(
                        MailConstants.A_ATTRIBUTE_NAME, name);
        }
        searchElt.addAttribute(MailConstants.A_RECIPIENTS, mRecipients);

        if (getLocale() != null) {
            searchElt.addElement(MailConstants.E_LOCALE).setText(getLocale().toString());
        }
        searchElt.addAttribute(MailConstants.A_PREFETCH, getPrefetch());
        searchElt.addAttribute(MailConstants.A_RESULT_MODE, getMode().name());
        searchElt.addAttribute(MailConstants.A_FIELD, getDefaultField());

        searchElt.addAttribute(MailConstants.A_QUERY_LIMIT, limit);
        searchElt.addAttribute(MailConstants.A_QUERY_OFFSET, offset);

        searchElt.addAttribute(MailConstants.A_IN_DUMPSTER, mInDumpster);

        // skip limit
        // skip offset
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

        params.mRequestContext = zsc;
        params.setHopCount(zsc.getHopCount());
        params.setIncludeTagDeleted(request.getAttributeBool(MailConstants.A_INCLUDE_TAG_DELETED, false));
        String allowableTasks = request.getAttribute(MailConstants.A_ALLOWABLE_TASK_STATUS, null);
        if (allowableTasks != null) {
            params.mAllowableTaskStatuses = new HashSet<TaskHit.Status>();
            String[] split = allowableTasks.split(",");
            if (split != null) {
                for (String s : split) {
                    try {
                        TaskHit.Status status = TaskHit.Status.valueOf(s.toUpperCase());
                        params.mAllowableTaskStatuses.add(status);
                    } catch (IllegalArgumentException e) {
                        ZimbraLog.index.debug("Skipping unknown task completion status: " + s);
                    }
                }
            }
        }
        params.setCalItemExpandStart(request.getAttributeLong(MailConstants.A_CAL_EXPAND_INST_START, -1));
        params.setCalItemExpandEnd(request.getAttributeLong(MailConstants.A_CAL_EXPAND_INST_END, -1));
        String query = request.getAttribute(MailConstants.E_QUERY, defaultQueryStr);
        if (query == null) {
            throw ServiceException.INVALID_REQUEST("no query submitted and no default query found", null);
        }
        params.setInDumpster(request.getAttributeBool(MailConstants.A_IN_DUMPSTER, false));
        params.setQueryStr(query);
        String types = request.getAttribute(MailConstants.A_SEARCH_TYPES,
                request.getAttribute(MailConstants.A_GROUPBY, null));
        if (Strings.isNullOrEmpty(types)) {
            params.setTypes(Search.DEFAULT_SEARCH_TYPES);
        } else {
            params.setTypes(types);
        }
        params.setSortBy(request.getAttribute(MailConstants.A_SORTBY, null));

        params.setInlineRule(ExpandResults.valueOf(request.getAttribute(MailConstants.A_FETCH, null), zsc));
        if (params.getInlineRule() != ExpandResults.NONE) {
            params.setMarkRead(request.getAttributeBool(MailConstants.A_MARK_READ, false));
            params.setMaxInlinedLength((int) request.getAttributeLong(MailConstants.A_MAX_INLINED_LENGTH, -1));
            params.setWantHtml(request.getAttributeBool(MailConstants.A_WANT_HTML, false));
            params.setNeuterImages(request.getAttributeBool(MailConstants.A_NEUTER, true));
            for (Element elt : request.listElements(MailConstants.A_HEADER)) {
                params.addInlinedHeader(elt.getAttribute(MailConstants.A_ATTRIBUTE_NAME));
            }
        }
        params.setWantRecipients(request.getAttributeBool(MailConstants.A_RECIPIENTS, false));

        // <tz>
        Element tzElt = request.getOptionalElement(MailConstants.E_CAL_TZ);
        if (tzElt != null) {
            params.setTimeZone(parseTimeZonePart(tzElt));
        }

        // <loc>
        Element locElt = request.getOptionalElement(MailConstants.E_LOCALE);
        if (locElt != null) {
            params.setLocale(parseLocale(locElt.getText()));
        }

        params.setPrefetch(request.getAttributeBool(MailConstants.A_PREFETCH, true));
        params.setMode(Mailbox.SearchResultMode.get(request.getAttribute(MailConstants.A_RESULT_MODE, null)));

        // field
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

    private static java.util.TimeZone parseTimeZonePart(Element tzElt) throws ServiceException {
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

    private final static Pattern LOCALE_PATTERN = Pattern.compile("([a-zA-Z]{2})(?:[-_]([a-zA-Z]{2})([-_](.+))?)?");

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
        int limit = (int) request.getAttributeLong(MailConstants.A_QUERY_LIMIT, -1);
        if (limit <= 0) {
            limit = 30;
        }
        if (limit > 1000) {
            limit = 1000;
        }
        return limit;
    }

    @Override
    public Object clone() {
        SearchParams result = new SearchParams();
        result.mRequestContext = mRequestContext;
        result.mHopCount = mHopCount;
        result.mDefaultField = mDefaultField;
        result.mQueryStr = mQueryStr;
        result.offset = offset;
        result.limit = limit;
        result.mInlineRule = mInlineRule;
        result.mMaxInlinedLength = mMaxInlinedLength;
        result.mWantHtml = mWantHtml;
        result.mNeuterImages = mNeuterImages;
        result.mInlinedHeaders = mInlinedHeaders;
        result.mRecipients = mRecipients;
        result.mCalItemExpandStart = mCalItemExpandStart;
        result.mCalItemExpandEnd = mCalItemExpandEnd;
        result.mIncludeTagDeleted = mIncludeTagDeleted;
        result.mTimeZone = mTimeZone;
        result.locale = locale;
        result.sortBy = sortBy;
        result.types = types;
        result.mPrefetch = mPrefetch;
        result.mMode = mMode;
        if (mAllowableTaskStatuses != null) {
            result.mAllowableTaskStatuses = new HashSet<TaskHit.Status>();
            result.mAllowableTaskStatuses.addAll(mAllowableTaskStatuses);
        }
        if (cursor != null) {
            result.cursor = new Cursor(cursor);
        }
        result.mInDumpster = mInDumpster;
        return result;
    }

    private ZimbraSoapContext mRequestContext;

    /**
     * this parameter is intentionally NOT encoded into XML, it is encoded
     * manually by the ProxiedQueryResults proxying code
     */
    private int mHopCount = 0;

    private String mDefaultField = "content:";
    private String mQueryStr;
    private int offset;
    private int limit;
    private ExpandResults mInlineRule = null;
    private boolean mMarkRead = false;
    private int mMaxInlinedLength;
    private boolean mWantHtml = false;
    private boolean mNeuterImages = false;
    private Set<String> mInlinedHeaders = null;
    private boolean mRecipients = false;
    private long mCalItemExpandStart = -1;
    private long mCalItemExpandEnd = -1;
    private boolean mInDumpster = false;  // search live data or dumpster data

    /**
     * if FALSE, then items with the /Deleted tag set are not returned.
     */
    private boolean mIncludeTagDeleted = false;
    private Set<TaskHit.Status> mAllowableTaskStatuses = null; // if NULL, allow all

    /**
     * timezone that the query should be parsed in (for date/time queries).
     */
    private TimeZone mTimeZone = null;
    private Locale locale;

    private SortBy sortBy;
    private Set<MailItem.Type> types = EnumSet.noneOf(MailItem.Type.class); // types to seach for
    private Cursor cursor;

    private boolean mPrefetch = true;
    private Mailbox.SearchResultMode mMode = Mailbox.SearchResultMode.NORMAL;

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
}
