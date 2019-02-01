/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.MoreObjects;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;

/**
 * Helper class -- deals with finding the "first result" to be returned, which
 * is dealt with in one of several ways depending on if this is an offset/limit
 * request, a forward cursor request, or a backward cursor request.
 * (see bug 2937)
 */
public final class ResultsPager {
    private ZimbraQueryResults results;
    private boolean fixedOffset;
    // in cases where ReSortingQueryResults is simulating the cursor for us, we need to skip
    // the passed-in cursor AND offset....otherwise pages will be skipped b/c we will end up
    // skipping OFFSET entries into the cursor-narrowed return set.  Note that we can't just
    // change the requested offset in SearchParams because that would cause the offset returned
    // in <SearchResponse> to be incorrect.
    private boolean ignoreOffsetHack = false;
    private List<ZimbraHit> bufferedHits;
    private SearchParams params;
    private boolean forward = true;
    private Comparator<ZimbraHit> comparator;
    private long offset = -1;

    public static ResultsPager create(ZimbraQueryResults results, SearchParams params) throws ServiceException {
        // must use results.getSortBy() because the results might have ignored our sortBy
        // request and used something else...
        params.setSortBy(results.getSortBy());

        // bug: 23427 -- TASK sorts are incompatible with cursors here so don't use the cursor at all
        boolean dontUseCursor = false;
        boolean skipOffsetHack = false;
        switch (params.getSortBy()) {
            case TASK_DUE_ASC:
            case TASK_DUE_DESC:
            case TASK_PERCENT_COMPLETE_ASC:
            case TASK_PERCENT_COMPLETE_DESC:
            case TASK_STATUS_ASC:
            case TASK_STATUS_DESC:
                dontUseCursor = true;
                break;
            case NAME_LOCALIZED_ASC:
            case NAME_LOCALIZED_DESC:
                dontUseCursor = !DebugConfig.enableContactLocalizedSort;
                // for localized sorts, the cursor is actually simulated by the ReSortingQueryResults....
                // so we need to zero out the offset here
                skipOffsetHack = !DebugConfig.enableContactLocalizedSort;
                break;
        }

        if (dontUseCursor || params.getCursor() == null) {
            return new ResultsPager(results, params, false, skipOffsetHack);
        } else {
            return new ResultsPager(results, params, true, false);
        }
    }

    /**
     * @param params if OFFSET-MODE, requires SortBy, offset, limit to be set, otherwise requires cursor to be set
     */
    private ResultsPager(ZimbraQueryResults results, SearchParams params, boolean useCursor, boolean skipOffset)
            throws ServiceException {
        this.results = results;
        this.params = params;
        this.fixedOffset = !useCursor;
        this.ignoreOffsetHack = skipOffset;

        if (DebugConfig.enableContactLocalizedSort) {
            switch (params.getSortBy()) {
                case NAME_LOCALIZED_ASC:
                case NAME_LOCALIZED_DESC:
                    comparator = params.getSortBy().getHitComparator(params.getLocale());
                    break;
            }
        }
        reset();
    }

    public SortBy getSortOrder() {
        return params.getSortBy();
    }

    public void reset() throws ServiceException {
        if (fixedOffset) {
            int offsetToUse = params.getOffset();
            if (ignoreOffsetHack) {
                offsetToUse = 0;
            }
            if (offsetToUse > 0) {
                results.skipToHit(offsetToUse-1);
            } else {
                results.resetIterator();
            }
        } else {
            if (forward) {
                bufferedHits = new ArrayList<ZimbraHit>(1);
                ZimbraHit current = forwardFindFirst();
                if (current != null)
                    bufferedHits.add(current);
            } else {
                bufferedHits = backward();
            }
        }
    }

    public long getCursorOffset() {
        return offset >= 0 ? offset + results.getCursorOffset() : -1;
    }

    public boolean hasNext() throws ServiceException {
        if (bufferedHits != null && !bufferedHits.isEmpty()) {
            return true;
        } else {
            return results.hasNext();
        }
    }

    public ZimbraHit getNextHit() throws ServiceException {
        if (bufferedHits != null && !bufferedHits.isEmpty()) {
            return bufferedHits.remove(0);
        } else {
            return results.getNext();
        }
    }

    private ZimbraHit forwardFindFirst() throws ServiceException {
        offset = -1;
        ZimbraHit prevHit = getPrevCursorHit();
        results.resetIterator();
        ZimbraHit hit = results.getNext();
        while (hit != null) {
            offset++;

            if (hit.getItemId() == params.getCursor().getItemId().getId()) { // found it!
                return results.getNext();
            }

            int comp;
            if (DebugConfig.enableContactLocalizedSort && comparator != null) {
                comp = comparator.compare(hit, prevHit);
            } else {
                comp = hit.compareTo(params.getSortBy(), prevHit);
            }
            // if (hit at the SAME TIME as prevSortValue) AND the ID is > prevHitId
            //   --> this depends on a secondary sort-order of HitID.  This doesn't
            //  currently hold up with ProxiedHits: we need to convert Hit sorting to
            //  use ItemIds (instead of int's) TODO FIXME
            if (comp == 0) {
                if (params.getCursor().getItemId().getId() == 0) { // special case prevId of 0
                    return hit;
                }
                /* See also DbSearch.sortBy(SortBy, boolean) */
                if (params.getSortBy().getDirection() == SortBy.Direction.DESC || params.getSortBy().getKey() == SortBy.Key.UNREAD) {
                    if (hit.getItemId() < params.getCursor().getItemId().getId()) {
                        return hit;
                    }
                } else {
                    if (hit.getItemId() > params.getCursor().getItemId().getId()) {
                        return hit;
                    }
                }
                // keep looking...
                hit = results.getNext();
            } else if (comp < 0) {
                // oops, we haven't gotten to the cursor-specified sort field yet...this happens
                // when we use a cursor without doing adding a range constraint to specify
                // the sort ranges....e.g. when using a Cursor with Conversation search
                // we skip the range b/c we need to force the search code to iterate over all
                // results (to build the conversations and hit them into the right spot
                // in the results)
                hit = results.getNext();
            } else {
                return hit;
            }
        }

        // end of line
        offset = -1;
        return null;
    }

    /**
     * Returns a list (in reverse order) of all hits between start and current-cursor position.
     */
    private List<ZimbraHit> backward() throws ServiceException {
        List<ZimbraHit> result = new LinkedList<ZimbraHit>();
        offset = -1;
        results.resetIterator();
        ZimbraHit hit = results.getNext();
        ZimbraHit prevHit = getPrevCursorHit();

        ZimbraHit dummyEndHit = null;
        if (params.getCursor().getEndSortValue() != null) {
            dummyEndHit = getEndCursorHit();
        }

        while (hit != null) {
            offset++;
            result.add(hit);
            if (hit.getItemId() == params.getCursor().getItemId().getId()) { // found old one
                break;
            }
            // hit COMES AFTER sortValue
            if (hit.compareTo(params.getSortBy(), prevHit) > 0) {
                break;
            }
            // hit COMES BEFORE endSortValue
            if (params.getCursor().getEndSortValue() != null && hit.compareTo(params.getSortBy(), dummyEndHit) <= 0) {
                break;
            }
            hit = results.getNext();
        }
        return result;
    }

    /**
     * Returns a dummy hit which is immediately before the first hit we want to return.
     */
    private ZimbraHit getPrevCursorHit() {
        return new CursorHit(results, params.getCursor().getSortValue(), params.getCursor().getItemId().getId());
    }

    /**
     * Returns a dummy hit which is immediately after the last hit we want to return.
     */
    private ZimbraHit getEndCursorHit() {
        return new CursorHit(results, params.getCursor().getEndSortValue(), 0);
    }

    static final class CursorHit extends ZimbraHit {
        private final int idCursor;

        CursorHit(ZimbraQueryResults results, String sortValue, int id) {
            super(results, null, sortValue);
            idCursor = id;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                .add("id", idCursor)
                .add("sortValue", sortValue)
                .toString();
        }

        @Override
        public int getConversationId() {
            return 0;
        }

        @Override
        public int getItemId() {
            return idCursor;
        }

        @Override
        void setItem(MailItem item) {
        }

        @Override
        boolean itemIsLoaded() {
            return false;
        }

        @Override
        public String getName() {
            return sortValue.toString();
        }

        @Override
        public MailItem getMailItem() {
            return null;
        }
    }

}
