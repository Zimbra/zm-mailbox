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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.zimbra.common.service.ServiceException;

/**
 * {@link ZimbraQueryResults} that collects hits from multiple mailboxes.
 * <p>
 * Take a bunch of {@link ZimbraQueryResults} (each sorted with the same
 * sort-order) and iterate them in order....kind of like a MergeSort.
 * <p>
 * The primary use of this class is for cross-mailbox-search, when you need to
 * aggregate search results together from many mailboxes.
 *
 * @since Mar 17, 2005
 * @author anandp
 * @author tim
 * @author ysasaki
 */
public class MultiQueryResults implements ZimbraQueryResults {
    private final int limit;
    private final SortBy sortOrder;
    private final Comparator<ZimbraHit> comparator;
    private List<ZimbraHit> hits = new ArrayList<ZimbraHit>();
    private int next = 0; // for interation
    private int estimatedResultSize = 0;
    private List<QueryInfo> queryInfo = new ArrayList<QueryInfo>();

    public MultiQueryResults(int limit, SortBy sort) {
        this.limit = limit;
        sortOrder = sort;
        comparator = ZimbraHit.getSortAndIdComparator(sort);
    }

    @Override
    public SortBy getSortBy() {
        return sortOrder;
    }

    @Override
    public ZimbraHit peekNext() {
        try {
            return hits.get(next);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) {
        next = hitNo;
        return getNext();
    }

    @Override
    public boolean hasNext() {
        return next < hits.size();
    }

    @Override
    public void resetIterator() {
        next = 0;
    }

    @Override
    public ZimbraHit getFirstHit() {
        resetIterator();
        return getNext();
    }

    @Override
    public ZimbraHit getNext() {
        try {
            return hits.get(next++);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    @Override
    public void doneWithSearchResults() {
        hits = null;
        queryInfo = null;
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return queryInfo;
    }

    @Override
    public int estimateResultSize() {
        return estimatedResultSize;
    }

    /**
     * Add the specified {@link ZimbraQueryResults} to this result. The adding
     * result must be sorted by the same order as this result.
     * <p>
     * Holding many {@link ZimbraQueryResults} objects causes too many Lucene
     * IndexReader objects opened concurrently, which will likely result in
     * {@link OutOfMemoryError}. The caller is expected to close the {@link ZimbraQueryResults}
     * with {@link ZimbraQueryResults#doneWithSearchResults()} immediately after
     * this method, which implies that all {@link ZimbraHit} contained in the
     * {@link ZimbraQueryResults} must be still accessible even after closing
     * the {@link ZimbraQueryResults}.
     *
     * @param result result to add
     * @throws ServiceException error
     */
    public void add(ZimbraQueryResults result) throws ServiceException {
        assert(sortOrder == result.getSortBy());

        if (hits.isEmpty()) {
            for (int i = 0; i < limit && result.hasNext(); i++) {
                hits.add(result.getNext());
            }
        } else {
            ZimbraHit last = hits.get(hits.size() - 1);
            while (result.hasNext()) {
                ZimbraHit hit = result.getNext();
                if (comparator.compare(last, hit) > 0) {
                    hits.add(hit);
                } else {
                    break;
                }
            }
            // if the list grew, sort it, then shrink it down to limit.
            if (last != hits.get(hits.size() - 1)) {
                Collections.sort(hits, comparator);
                while (hits.size() > limit) {
                    hits.remove(hits.size() - 1);
                }
            }
        }

        queryInfo.addAll(result.getResultInfo());
        estimatedResultSize += result.estimateResultSize();
    }

    /**
     * Shrink the list by removing entries from the head to the offset.
     *
     * @param offset entries at and after this offset will survive
     */
    public void shrink(int offset) {
        for (int i = 0; i < offset && !hits.isEmpty(); i++) {
            hits.remove(0);
        }
    }

}
