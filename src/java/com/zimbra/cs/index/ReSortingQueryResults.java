/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.localconfig.DebugConfig;

/**
 * QueryResults wrapper that implements Re-Sorting. It does this by caching **ALL** hits and then sorting them. It is
 * used for the Task sorts as well as specially localized language sorts
 */
public class ReSortingQueryResults implements ZimbraQueryResults {
    private static final int MAX_BUFFERED_HITS = 10000;

    private final ZimbraQueryResults results;
    private final SortBy desiredSort;
    private List<ZimbraHit> mHitBuffer = null;
    private int iterOffset = 0;
    private final SearchParams params;

    ReSortingQueryResults(ZimbraQueryResults results, SortBy desiredSort, SearchParams params) {
        this.results = results;
        this.desiredSort = desiredSort;
        this.params = params;
    }

    @Override
    public long getTotalHitCount() throws ServiceException {
        return results.getTotalHitCount();
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        results.doneWithSearchResults();
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        if (hasNext()) {
            ZimbraHit toRet = peekNext();
            iterOffset++;
            return toRet;
        } else {
            return null;
        }
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return results.getResultInfo();
    }

    @Override
    public SortBy getSortBy() {
        return desiredSort;
    }

    @Override
    public boolean hasNext() throws ServiceException {
        return (iterOffset < getHitBuffer().size());
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        List<ZimbraHit> buffer = getHitBuffer();
        if (hasNext()) {
            return buffer.get(iterOffset);
        } else {
            return null;
        }
    }

    @Override
    public void resetIterator() throws ServiceException {
        iterOffset = 0;
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        List<ZimbraHit> buffer = getHitBuffer();
        if (hitNo >= buffer.size()) {
            iterOffset = buffer.size();
        } else {
            iterOffset = hitNo;
        }
        return getNext();
    }

    private List<ZimbraHit> getHitBuffer() throws ServiceException {
        if (mHitBuffer == null) {
            bufferAllHits();
        }
        return mHitBuffer;
    }

    private boolean isTaskSort() {
        switch (desiredSort.getType()) {
            case TASK_DUE_ASCENDING:
            case TASK_DUE_DESCENDING:
            case TASK_STATUS_ASCENDING:
            case TASK_STATUS_DESCENDING:
            case TASK_PERCENT_COMPLETE_ASCENDING:
            case TASK_PERCENT_COMPLETE_DESCENDING:
                return true;
            default:
                return false;
        }
    }

    private void bufferAllHits() throws ServiceException {
        assert(mHitBuffer == null);
        mHitBuffer = new ArrayList<ZimbraHit>();

        // get the proper comparator
        Comparator<ZimbraHit> comp;
        switch (desiredSort.getType()) {
            default:
            case TASK_DUE_ASCENDING:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByDueDate(true, lhs, rhs);
                    }
                };
                break;
            case TASK_DUE_DESCENDING:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByDueDate(false, lhs, rhs);
                    }
                };
                break;
            case TASK_STATUS_ASCENDING:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByStatus(true, lhs, rhs);
                    }
                };
                break;
            case TASK_STATUS_DESCENDING:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByStatus(false, lhs, rhs);
                    }
                };
                break;
            case TASK_PERCENT_COMPLETE_ASCENDING:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByCompletionPercent(true, lhs, rhs);
                    }
                };
                break;
            case TASK_PERCENT_COMPLETE_DESCENDING:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByCompletionPercent(false, lhs, rhs);
                    }
                };
                break;
            case NAME_LOCALIZED_ASCENDING:
            case NAME_LOCALIZED_DESCENDING:
                comp = ((LocalizedSortBy) desiredSort).getZimbraHitComparator();
                break;
        }

        ZimbraHit cur;
        while ((cur = results.getNext()) != null) {

            if (isTaskSort()) {
                if (!(cur instanceof TaskHit) && !(cur instanceof ProxiedHit)) {
                    throw ServiceException.FAILURE("Invalid hit type, can only task-sort Tasks", null);
                }
            }

            boolean skipHit = false;

            boolean handleCursorFilteringForFirstHit = true;
            if (DebugConfig.enableContactLocalizedSort) {
                if (desiredSort.getType() == SortBy.Type.NAME_LOCALIZED_ASCENDING ||
                        desiredSort.getType() == SortBy.Type.NAME_LOCALIZED_DESCENDING) {
                    handleCursorFilteringForFirstHit = false;
                }
            }

            // handle cursor filtering
            if (params != null && params.hasCursor()) {
                ZimbraHit firstHit = null;
                if (params.getPrevSortValueStr() != null)
                    firstHit = new ResultsPager.DummyHit(params.getPrevSortValueStr(), params.getPrevSortValueStr(),
                            params.getPrevSortValueLong(), params.getPrevMailItemId().getId());

                ZimbraHit endHit = null;
                if (params.getEndSortValueStr() != null)
                    endHit = new ResultsPager.DummyHit(params.getEndSortValueStr(), params.getEndSortValueStr(),
                            params.getEndSortValueLong(), 0);

                // fail if cur < first OR cur >= end
                if (handleCursorFilteringForFirstHit) {
                    if (firstHit != null && comp.compare(cur, firstHit) < 0) {
                        skipHit = true;
                    }
                }
                if (endHit != null && comp.compare(cur, endHit) >= 0) {
                    skipHit = true;
                }
            }

            if (!skipHit) {
                mHitBuffer.add(cur);
            }
            if (mHitBuffer.size() >= MAX_BUFFERED_HITS) {
                break;
            }

        }
        Collections.sort(mHitBuffer, comp);
    }

}
