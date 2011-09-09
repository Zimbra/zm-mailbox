/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;

/**
 * QueryResults wrapper that implements Re-Sorting. It does this by caching **ALL** hits and then sorting them. It is
 * used for the Task sorts as well as specially localized language sorts
 */
public final class ReSortingQueryResults implements ZimbraQueryResults {
    private static final int MAX_BUFFERED_HITS = 10000;

    private final ZimbraQueryResults results;
    private final SortBy sort;
    private List<ZimbraHit> mHitBuffer = null;
    private int iterOffset = 0;
    private final SearchParams params;

    public ReSortingQueryResults(ZimbraQueryResults results, SortBy sort, SearchParams params) {
        this.results = results;
        this.sort = sort;
        this.params = params;
    }

    @Override
    public long getCursorOffset() {
        return -1;
    }

    @Override
    public void close() throws IOException {
        results.close();
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
        return sort;
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
        switch (sort) {
            case TASK_DUE_ASC:
            case TASK_DUE_DESC:
            case TASK_STATUS_ASC:
            case TASK_STATUS_DESC:
            case TASK_PERCENT_COMPLETE_ASC:
            case TASK_PERCENT_COMPLETE_DESC:
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
        switch (sort) {
            default:
            case TASK_DUE_ASC:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByDueDate(true, lhs, rhs);
                    }
                };
                break;
            case TASK_DUE_DESC:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByDueDate(false, lhs, rhs);
                    }
                };
                break;
            case TASK_STATUS_ASC:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByStatus(true, lhs, rhs);
                    }
                };
                break;
            case TASK_STATUS_DESC:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByStatus(false, lhs, rhs);
                    }
                };
                break;
            case TASK_PERCENT_COMPLETE_ASC:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByCompletionPercent(true, lhs, rhs);
                    }
                };
                break;
            case TASK_PERCENT_COMPLETE_DESC:
                comp = new Comparator<ZimbraHit>() {
                    @Override
                    public int compare(ZimbraHit lhs, ZimbraHit rhs) {
                        return TaskHit.compareByCompletionPercent(false, lhs, rhs);
                    }
                };
                break;
            case NAME_LOCALIZED_ASC:
            case NAME_LOCALIZED_DESC:
                comp = sort.getHitComparator(params.getLocale());
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
                switch (sort) {
                    case NAME_LOCALIZED_ASC:
                    case NAME_LOCALIZED_DESC:
                        handleCursorFilteringForFirstHit = false;
                        break;
                }
            }

            // handle cursor filtering
            if (params != null && params.getCursor() != null) {
                ZimbraHit firstHit = null;
                if (params.getCursor().getSortValue() != null) {
                    firstHit = new ResultsPager.CursorHit(results, params.getCursor().getSortValue(),
                            params.getCursor().getItemId().getId());
                }
                ZimbraHit endHit = null;
                if (params.getCursor().getEndSortValue() != null) {
                    endHit = new ResultsPager.CursorHit(results, params.getCursor().getEndSortValue(), 0);
                }
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
