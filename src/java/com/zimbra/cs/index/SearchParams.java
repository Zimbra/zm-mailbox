/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.localconfig.DebugConfig;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.calendar.ICalTimeZone;
import com.zimbra.cs.mailbox.calendar.WellKnownTimeZones;
import com.zimbra.cs.service.mail.CalendarUtils;
import com.zimbra.cs.service.mail.Search;
import com.zimbra.cs.service.mail.ToXML.OutputParticipants;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;


/**
 * Simple class that encapsulates all of the parameters involved in a Search request.
 * Not used everywhere, need to convert all code to use this....
 * <p>
 * To initialize, set:
 * <ul>
 *  <li>query str
 *  <li>offset
 *  <li>limit
 *  <li>typesStr (sets type value)
 *  <li>sortByStr (sets sortBy value)
 * </ul>
 * <p>
 * IMPORTANT NOTE: if you add new {@link SearchParams}, you MUST add
 * parsing/serialization code to the {@link SearchParams#encodeParams(Element)}
 * and {@link SearchParams#parse(Element, ZimbraSoapContext, String)}) APIs.
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

    public String getTypesStr() {
        return mGroupByStr;
    }

    public byte[] getTypes() {
        return types;
    }

    public String getSortByStr() {
        return mSortByStr;
    }

    public SortBy getSortBy() {
        return mSortBy;
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
        return mLocale;
    }

    public boolean getPrefetch() {
        return mPrefetch;
    }

    public Mailbox.SearchResultMode getMode() {
        return mMode;
    }

    public boolean getEstimateSize() {
        return mEstimateSize;
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
        return mLimit;
    }

    public int getOffset() {
        return mOffset;
    }

    // cursor parameters:
    public ItemId getPrevMailItemId() {
        return mPrevMailItemId;
    }

    public String getPrevSortValueStr() {
        return mPrevSortValueStr;
    }

    public long getPrevSortValueLong() {
        return mPrevSortValueLong;
    }

    public int getPrevOffset() {
        return mPrevOffset;
    }

    public boolean hasEndSortValue() {
        return mEndSortValueStr != null;
    }

    public String getEndSortValueStr() {
        return mEndSortValueStr;
    }

    public long getEndSortValueLong() {
        return mEndSortValueLong;
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

    public void setOffset(int offset) {
        mOffset = offset; if (mOffset > MAX_OFFSET) mOffset = MAX_OFFSET;
    }

    public void setLimit(int limit) {
        mLimit = limit; if (mLimit > MAX_LIMIT) mLimit = MAX_LIMIT;
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
     * Since the results are iterator-based, the "limit" is really the same as
     * the chunk size + offset ie, the limit is used to tell the system
     * approximately how many results you want and it tries to get them
     * in a single chunk --- but it isn't until you do the results iteration
     * that the limit is enforced.
     *
     * @param chunkSize
     */
    public void setChunkSize(int chunkSize) {
        setLimit(chunkSize + mOffset);
    }

    public void setTypesStr(String groupByStr) throws ServiceException {
        mGroupByStr = groupByStr;
        byte[] typesToSet = MailboxIndex.parseTypesString(getTypesStr());
        setTypesInternal(typesToSet);
    }

    public void setTypes(byte[] _types) {
        boolean atFirst = true;
        StringBuilder s = new StringBuilder();
        for (byte b : _types) {
            if (!atFirst) {
                s.append(',');
            }
            s.append(MailItem.getNameForType(b));
            atFirst = false;
        }
        mGroupByStr = s.toString();
        setTypesInternal(_types);
    }

    private void setTypesInternal(byte[] _types) {
        types = _types;
        checkForLocalizedContactSearch();
    }

    private boolean isSystemDefaultLocale() {
        if (mLocale == null) {
            return true;
        }

        // Gets the current value of the default locale for this instance of the Java Virtual Machine.
        Locale systemDefaultLocale = Locale.getDefault();

        return mLocale.equals(systemDefaultLocale);
    }

    private void checkForLocalizedContactSearch() {
        if (DebugConfig.enableContactLocalizedSort) {

            // FIXME: for bug 41920, disable localized contact sorting
            // bug 22665 - if searching ONLY for contacts, and locale is not EN, used localized re-sort
            if (types != null && types.length == 1 &&
                    types[0] == MailItem.TYPE_CONTACT && !isSystemDefaultLocale()) {
                if (mLocale != null) {
                    if (mSortBy != null) {
                        if (mSortBy.getType() == SortBy.Type.NAME_ASCENDING) {
                            mSortBy = new LocalizedSortBy(SortBy.Type.NAME_LOCALIZED_ASCENDING,
                                    null, SortBy.SortCriterion.NAME,
                                    SortBy.SortDirection.ASCENDING, mLocale);
                        } else if (mSortBy.getType() == SortBy.Type.NAME_DESCENDING) {
                            mSortBy = new LocalizedSortBy(SortBy.Type.NAME_LOCALIZED_DESCENDING,
                                    null, SortBy.SortCriterion.NAME,
                                    SortBy.SortDirection.DESCENDING, mLocale);
                        }
                    }
                }
            }
        }
    }

    public void setSortBy(SortBy sortBy) {
        mSortBy = sortBy;
        mSortByStr = mSortBy.toString();
        checkForLocalizedContactSearch();
    }

    public void setSortByStr(String sortByStr) {
        mSortByStr = sortByStr;
        SortBy sb = SortBy.lookup(sortByStr);
        if (sb == null) {
            sb = SortBy.DATE_DESCENDING;
        }
        setSortBy(sb);
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

    public void setLocale(Locale loc) {
        mLocale = loc;
        checkForLocalizedContactSearch();
    }

    public boolean hasCursor() {
        return mHasCursor;
    }

    public void setCursor(ItemId prevMailItemId, String prevSort,
            int prevOffset, String endSort) {
        mHasCursor = true;
        mPrevMailItemId = prevMailItemId;
        mPrevSortValueStr = prevSort;
        try {
            mPrevSortValueLong = Long.parseLong(prevSort);
        } catch (NumberFormatException e) {
            mPrevSortValueLong = 0;
        }
        mPrevOffset = prevOffset;
        mEndSortValueStr = endSort;
        mEndSortValueLong = -1;
        if (mEndSortValueStr != null) {
            try {
                mEndSortValueLong = Long.parseLong(mEndSortValueStr);
            } catch (NumberFormatException e) {
                mEndSortValueLong = Long.MAX_VALUE;
            }
        }
    }

    public void clearCursor() {
        mHasCursor = false;
        mPrevOffset = 0;
        mPrevMailItemId = null;
        mPrevSortValueStr = null;
        mPrevSortValueLong = 0;
        mEndSortValueStr = null;
        mEndSortValueLong = -1;
    }

    public void setPrefetch(boolean truthiness) {
        mPrefetch = truthiness;
    }

    public void setMode(Mailbox.SearchResultMode mode) {
        mMode = mode;
    }

    /**
     * @param estimateSize if true, the server will attempt to calculate a size
     *  estimate for the entire result set. Caller must fetch the first hit (via
     *  getNext() or getFirstHit() before the estimate is made. The estimate
     *  will be correct for a DB-only query and it may be wildly off for
     *  a remote or join query.
     */
    public void setEstimateSize(boolean estimateSize) {
        mEstimateSize = estimateSize;
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
        searchElt.addAttribute(MailConstants.A_SEARCH_TYPES, getTypesStr());
        searchElt.addAttribute(MailConstants.A_SORTBY, getSortByStr());
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
        searchElt.addAttribute(MailConstants.A_ESTIMATE_SIZE, getEstimateSize());
        searchElt.addAttribute(MailConstants.A_FIELD, getDefaultField());

        searchElt.addAttribute(MailConstants.A_QUERY_LIMIT, mLimit);
        searchElt.addAttribute(MailConstants.A_QUERY_OFFSET, mOffset);

        searchElt.addAttribute(MailConstants.A_IN_DUMPSTER, mInDumpster);

        // skip limit
        // skip offset
        // skip cursor data
    }

    /**
     * Parse the search parameters from a <SearchRequest> or similar element.
     *
     * @param requesthttp
     *            The <SearchRequest> itself, or similar element (<SearchConvRequest>, etc)
     * @param requestedAccount
     *            The account who's mailbox we should search in
     * @param zsc
     *            The SoapContext of the request.
     * @return
     * @throws ServiceException
     */
    public static SearchParams parse(Element request, ZimbraSoapContext zsc,
            String defaultQueryStr) throws ServiceException {
        SearchParams params = new SearchParams();

        params.mRequestContext = zsc;
        params.setHopCount(zsc.getHopCount());
        params.setIncludeTagDeleted(request.getAttributeBool(
                MailConstants.A_INCLUDE_TAG_DELETED, false));
        String allowableTasks = request.getAttribute(
                MailConstants.A_ALLOWABLE_TASK_STATUS, null);
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
        params.setCalItemExpandStart(request.getAttributeLong(
                MailConstants.A_CAL_EXPAND_INST_START, -1));
        params.setCalItemExpandEnd(request.getAttributeLong(
                MailConstants.A_CAL_EXPAND_INST_END, -1));
        String query = request.getAttribute(MailConstants.E_QUERY, defaultQueryStr);
        if (query == null) {
            throw ServiceException.INVALID_REQUEST(
                    "no query submitted and no default query found", null);
        }
        params.setInDumpster(request.getAttributeBool(MailConstants.A_IN_DUMPSTER, false));
        params.setQueryStr(query);
        params.setTypesStr(request.getAttribute(MailConstants.A_SEARCH_TYPES,
                request.getAttribute(MailConstants.A_GROUPBY,
                        Search.DEFAULT_SEARCH_TYPES)));
        params.setSortByStr(request.getAttribute(MailConstants.A_SORTBY,
                SortBy.DATE_DESCENDING.toString()));

        params.setInlineRule(ExpandResults.valueOf(
                request.getAttribute(MailConstants.A_FETCH, null), zsc));
        if (params.getInlineRule() != ExpandResults.NONE) {
            params.setMarkRead(request.getAttributeBool(
                    MailConstants.A_MARK_READ, false));
            params.setMaxInlinedLength((int) request.getAttributeLong(
                    MailConstants.A_MAX_INLINED_LENGTH, -1));
            params.setWantHtml(request.getAttributeBool(
                    MailConstants.A_WANT_HTML, false));
            params.setNeuterImages(request.getAttributeBool(
                    MailConstants.A_NEUTER, true));
            for (Element elt : request.listElements(MailConstants.A_HEADER))
                params.addInlinedHeader(elt.getAttribute(
                        MailConstants.A_ATTRIBUTE_NAME));
        }
        params.setWantRecipients(request.getAttributeBool(
                MailConstants.A_RECIPIENTS, false));

        // <tz>
        Element tzElt = request.getOptionalElement(MailConstants.E_CAL_TZ);
        if (tzElt != null) {
            params.setTimeZone(parseTimeZonePart(tzElt));
        }

        // <loc>
        Element locElt = request.getOptionalElement(MailConstants.E_LOCALE);
        if (locElt != null) {
            params.setLocale(parseLocale(locElt));
        }

        params.setPrefetch(request.getAttributeBool(
                MailConstants.A_PREFETCH, true));
        params.setMode(Mailbox.SearchResultMode.get(request.getAttribute(
                MailConstants.A_RESULT_MODE, null)));

        // field
        String field = request.getAttribute(MailConstants.A_FIELD, null);
        if (field != null)
            params.setDefaultField(field);

        params.setLimit(parseLimit(request));
        params.setOffset(parseOffset(request));

        Element cursor = request.getOptionalElement(MailConstants.E_CURSOR);
        if (cursor != null) {
            parseCursor(cursor, zsc.getRequestedAccountId(), params);
        }

        return params;
    }

    /**
     * Parse cursor element and set cursor info in SearchParams object
     *
     * @param cursor
     *            cursor element taken from a <SearchRequest>
     * @param acctId
     *            requested account id
     * @param params
     *            SearchParams object to set cursor info to
     * @return
     * @throws ServiceException
     */
    public static void parseCursor(Element cursor, String  acctId,
            SearchParams params) throws ServiceException {
        boolean useCursorToNarrowDbQuery = true;

        // in some cases we cannot use cursors, even if they are requested.
        //
        //  -- Task-sorts cannot be used with cursors (bug 23427) at all
        //
        //  -- Conversation mode can use cursors to find the right location in the
        //     hits, but we *can't* use a constrained-offset query to find the right place
        //     in the search results....in Conv mode we need to walk through all the results
        //     so that we can guarantee that we only return each Conversation once in
        //     a given results set
        //
        {
            // bug: 23427 -- TASK sorts are incompatible with CURSORS, since cursors require
            //               real (db-visible) sort fields
            switch (params.getSortBy().getType()) {
                case TASK_DUE_ASCENDING:
                case TASK_DUE_DESCENDING:
                case TASK_PERCENT_COMPLETE_ASCENDING:
                case TASK_PERCENT_COMPLETE_DESCENDING:
                case TASK_STATUS_ASCENDING:
                case TASK_STATUS_DESCENDING:
                case NAME_LOCALIZED_ASCENDING:
                case NAME_LOCALIZED_DESCENDING:
                    useCursorToNarrowDbQuery = false;
            }

            // bug 35039 - using cursors with conversation-coalescing leads to convs
            //             appearing on multiple pages
            for (byte b : params.getTypes()) {
                if (b == MailItem.TYPE_CONVERSATION) {
                    useCursorToNarrowDbQuery = false;
                }
            }
        }

        String cursorStr = cursor.getAttribute(MailConstants.A_ID);
        ItemId prevMailItemId = null;
        if (cursorStr != null) {
            prevMailItemId = new ItemId(cursorStr, acctId);
        }

        int prevOffset = 0;
        String sortVal = cursor.getAttribute(MailConstants.A_SORTVAL);

        String endSortVal = cursor.getAttribute(MailConstants.A_ENDSORTVAL, null);
        params.setCursor(prevMailItemId, sortVal, prevOffset, endSortVal);

        String addedPart = null;

        if (useCursorToNarrowDbQuery) {
            switch (params.getSortBy().getType()) {
                case NONE:
                    throw new IllegalArgumentException(
                            "Invalid request: cannot use cursor with SortBy=NONE");
                case DATE_ASCENDING:
                    addedPart = "date:" + quote(">=", sortVal) +
                        (endSortVal != null ? " date:" + quote("<", endSortVal) : "");
                    break;
                case DATE_DESCENDING:
                    addedPart = "date:" + quote("<=", sortVal) +
                        (endSortVal != null ? " date:" + quote(">", endSortVal) : "");
                    break;
                case SUBJ_ASCENDING:
                    addedPart = "subject:" + quote(">=", sortVal) +
                        (endSortVal != null ? " subject:" + quote("<", endSortVal) : "");
                    break;
                case SUBJ_DESCENDING:
                    addedPart = "subject:" + quote("<=", sortVal) +
                        (endSortVal != null ? " subject:" + quote(">", endSortVal) : "");
                    break;
                case SIZE_ASCENDING:
                    // hackaround because "size:>=" doesn't parse but "size:>" does
                    sortVal = "" + (Long.parseLong(sortVal) - 1);
                    addedPart = "size:" + quote(">", sortVal) +
                        (endSortVal != null ? " size:" + quote("<", endSortVal) : "");
                    break;
                case SIZE_DESCENDING:
                    // hackaround because "size:<=" doesn't parse but "size:<" does
                    sortVal = "" + (Long.parseLong(sortVal) + 1);
                    addedPart = "size:" + quote("<", sortVal) +
                        (endSortVal != null ? " size:" + quote(">", endSortVal) : "");
                    break;
                case NAME_ASCENDING:
                    addedPart = "from:" + quote(">=", sortVal) +
                        (endSortVal != null ? " from:" + quote("<", endSortVal) : "");
                    break;
                case NAME_DESCENDING:
                    addedPart = "from:" + quote("<=", sortVal) +
                        (endSortVal != null ? " from:" + quote(">", endSortVal) : "");
                    break;
            }
        }

        if (addedPart != null) {
            params.setQueryStr("(" + params.getQueryStr() + ")" + addedPart);
        }
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

    private static final String LOCALE_PATTERN = "([a-zA-Z]{2})(?:[-_]([a-zA-Z]{2})([-_](.+))?)?";
    private final static Pattern sLocalePattern = Pattern.compile(LOCALE_PATTERN);

    private static Locale parseLocale(Element localeElt) {
        String locStr = localeElt.getText();
        return lookupLocaleFromString(locStr);
    }

    private static Locale lookupLocaleFromString(String locStr) {
        if (locStr != null && locStr.length() > 0) {
            Matcher m = sLocalePattern.matcher(locStr);
            if (m.lookingAt()) {
                String lang=null, country=null, variant=null;

                if (m.start(1) != -1) {
                    lang = locStr.substring(m.start(1), m.end(1));
                }

                if (lang == null || lang.length() <= 0) {
                    return null;
                }

                if (m.start(2) != -1) {
                    country = locStr.substring(m.start(2), m.end(2));
                }

                if (m.start(4) != -1) {
                    variant = locStr.substring(m.start(4), m.end(4));
                }

                if (variant != null && country != null &&
                        variant.length() > 0 && country.length() > 0) {
                    return new Locale(lang, country, variant);
                }

                if (country != null && country.length() > 0) {
                    return new Locale(lang, country);
                }

                return new Locale(lang);
            }
        }
        return null;
    }

    public static void main(String args[]) {
        {
            Locale l = lookupLocaleFromString("da");
            System.out.println(" got locale: " + l);
        }
        {
            Locale l = lookupLocaleFromString("da_DK");
            System.out.println(" got locale: " + l);
        }
        {
            Locale l = lookupLocaleFromString("en");
            System.out.println(" got locale: " + l);
        }
        {
            Locale l = lookupLocaleFromString("en_US-MAC");
            System.out.println(" got locale: " + l);
        }
    }

    private static final String quote(String s1, String s2) {
        // escape quotation marks and quote the whole string
        String in = s1 + s2;
        in = in.replace("\"", "\\\"");
        return "\"" + in + "\"";
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

    private static int parseOffset(Element request) throws ServiceException {
        // Lookup the offset= and limit= parameters in the soap request
        return (int) request.getAttributeLong(MailConstants.A_QUERY_OFFSET, 0);
    }

    @Override
    public Object clone() {
        SearchParams o = new SearchParams();

        o.mRequestContext = mRequestContext;
        o.mHopCount = mHopCount;
        o.mDefaultField = mDefaultField;
        o.mQueryStr = mQueryStr;
        o.mOffset = mOffset;
        o.mLimit = mLimit;
        o.mInlineRule = mInlineRule;
        o.mMarkRead = mMarkRead;
        o.mMaxInlinedLength = mMaxInlinedLength;
        o.mWantHtml = mWantHtml;
        o.mNeuterImages = mNeuterImages;
        o.mInlinedHeaders = mInlinedHeaders;
        o.mRecipients = mRecipients;
        o.mCalItemExpandStart = mCalItemExpandStart;
        o.mCalItemExpandEnd = mCalItemExpandEnd;
        o.mIncludeTagDeleted = mIncludeTagDeleted;
        o.mTimeZone = mTimeZone;
        o.mLocale = mLocale;
        o.mHasCursor = mHasCursor;
        o.mPrevMailItemId = mPrevMailItemId;
        o.mPrevSortValueStr = mPrevSortValueStr;
        o.mPrevSortValueLong = mPrevSortValueLong;
        o.mPrevOffset = mPrevOffset;
        o.mEndSortValueStr = mEndSortValueStr;
        o.mEndSortValueLong = mEndSortValueLong;
        o.mGroupByStr = mGroupByStr;
        o.mSortByStr = mSortByStr;
        o.mSortBy = mSortBy;
        o.types = types;
        o.mPrefetch = mPrefetch;
        o.mMode = mMode;
        o.mEstimateSize = mEstimateSize;
        if (mAllowableTaskStatuses != null) {
            o.mAllowableTaskStatuses = new HashSet<TaskHit.Status>();
            o.mAllowableTaskStatuses.addAll(mAllowableTaskStatuses);
        }
        o.mInDumpster = mInDumpster;

        return o;
    }

    private ZimbraSoapContext mRequestContext;

    /**
     * this parameter is intentionally NOT encoded into XML, it is encoded
     * manually by the ProxiedQueryResults proxying code
     */
    private int mHopCount = 0;

    private String mDefaultField = "content:";
    private String mQueryStr;
    private int mOffset;
    private int mLimit;
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
    private Locale mLocale  = null;

    private boolean mHasCursor = false;

    /////////////////////
    // "Cursor" Data -- the three pieces of info below are enough for us to find out place in
    // the previous result set, even if entries have been added or removed from the result
    // set:
    /**
     * the mail item ID of the last item in the previous result set.
     */
    private ItemId mPrevMailItemId;

    /**
     * the sort value of the last item in the previous result set.
     */
    private String mPrevSortValueStr;

    /**
     * the sort value of the last item in the previous result set.
     */
    private long mPrevSortValueLong;

    /**
     * the offset of the last item in the previous result set.
     */
    private int mPrevOffset;

    /**
     * where to end the search. Hits >= this value are NOT included in
     * the result set.
     */
    private String mEndSortValueStr;

    /**
     * where to end the search. Hits >= this value are NOT included in
     * the result set.
     */
    private long mEndSortValueLong;


    // unparsed -- these need to go away!
    private String mGroupByStr;
    private String mSortByStr;

    // parsed:
    private SortBy mSortBy;
    private byte[] types; // types to seach for

    private boolean mPrefetch = true;
    private Mailbox.SearchResultMode mMode = Mailbox.SearchResultMode.NORMAL;

    /**
     * ask or a size estimate. Note that this might have a nontrivial
     * performance impact.
     */
    private boolean mEstimateSize = false;

}
