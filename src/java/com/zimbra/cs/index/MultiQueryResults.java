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

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;

/**
 * A QueryResults set which cross multiple mailboxes.
 * <p>
 * Take a bunch of QueryResults (each sorted with the same sort-order) and
 * iterate them in order....kind of like a MergeSort.
 * <p>
 * TODO - this class duplicates functionality in UnionQueryOperation,
 * they should be combined somehow
 * <p>
 * The primary use of this class is for cross-mailbox-search, when you need to
 * aggregate search results together from many mailboxes
 *
 * @since Mar 17, 2005
 */
public class MultiQueryResults implements ZimbraQueryResults {
    private SortBy mSortOrder;
    private ZimbraQueryResults[] mResults;
    private ZimbraHit mCachedNextHit = null;

    public MultiQueryResults(ZimbraQueryResults[] results, SortBy sortOrder) {
        mSortOrder = sortOrder;
        mResults = new ZimbraQueryResults[results.length];
        for (int i = 0; i < results.length; i++) {
            mResults[i] = HitIdGrouper.Create(results[i], sortOrder);
        }
    }

    public SortBy getSortBy() {
        return mSortOrder;
    }

    public ZimbraHit peekNext() throws ServiceException {
        bufferNextHit();
        return mCachedNextHit;
    }

    private void internalGetNextHit() throws ServiceException {
        if (mCachedNextHit == null) {
            if (mSortOrder == SortBy.NONE) {
                for (ZimbraQueryResults res : mResults) {
                    mCachedNextHit = res.getNext();
                    if (mCachedNextHit != null) {
                        return;
                    }
                }
                // no more results!

            } else {
                // mergesort: loop through QueryOperations and find the "best" hit

                int currentBestHitOffset = -1;
                ZimbraHit currentBestHit = null;
                for (int i = 0; i < mResults.length; i++) {
                    ZimbraQueryResults op = mResults[i];
                    if (op.hasNext()) {
                        if (currentBestHitOffset == -1) {
                            currentBestHitOffset = i;
                            currentBestHit = op.peekNext();
                        } else {
                            ZimbraHit opNext = op.peekNext();
                            int result = opNext.compareBySortField(mSortOrder, currentBestHit);
                            if (result < 0) {
                                // "before"
                                currentBestHitOffset = i;
                                currentBestHit = opNext;
                            }
                        }
                    }
                }
                if (currentBestHitOffset > -1) {
                    mCachedNextHit = mResults[currentBestHitOffset].getNext();
                    assert(mCachedNextHit == currentBestHit);
                }
            }
        }
    }

    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        resetIterator();
        for (int i = 0; i < hitNo; i++) {
            if (!hasNext()) {
                return null;
            }
            getNext();
        }
        return getNext();
    }

    public boolean hasNext() throws ServiceException {
        return bufferNextHit();
    }

    public boolean bufferNextHit() throws ServiceException {
        if (mCachedNextHit == null) {
            internalGetNextHit();
        }
        return (mCachedNextHit != null);
    }

    public void resetIterator() throws ServiceException {
        for (int i = 0; i < mResults.length; i++) {
            mResults[i].resetIterator();
        }
    }

    public ZimbraHit getFirstHit() throws ServiceException {
        resetIterator();
        return getNext();
    }

    public ZimbraHit getNext() throws ServiceException {
        bufferNextHit();
        ZimbraHit toRet = mCachedNextHit;
        mCachedNextHit = null;
        return toRet;
    }

    public void doneWithSearchResults() throws ServiceException {
        for (int i = 0; i < mResults.length; i++) {
            mResults[i].doneWithSearchResults();
        }
    }

    public List<QueryInfo> getResultInfo() {
        List<QueryInfo> toRet = new ArrayList<QueryInfo>();
        for (ZimbraQueryResults results : mResults) {
            toRet.addAll(results.getResultInfo());
        }
        return toRet;
    }

    public int estimateResultSize() throws ServiceException {
        long total = 0;
        for (ZimbraQueryResults results : mResults) {
            total += results.estimateResultSize();
        }
        if (total > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) total;
        }
    }

}
