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

import java.util.Collections;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * Take {@link ZimbraHit}s which are already sorted by sort-order and
 * additionally sort them by mail-item-id
 * <p>
 * This Grouper has no effect if the current sort mode is "none"
 *
 * @since Mar 15, 2005
 * @author tim
 */
public class HitIdGrouper extends BufferingResultsGrouper {
    private SortBy mSortOrder;

    public static ZimbraQueryResults Create(ZimbraQueryResults hits, SortBy sortOrder) {
        if (sortOrder == SortBy.NONE) {
            return hits;
        } else {
            return new HitIdGrouper(hits, sortOrder);
        }
    }

    private HitIdGrouper(ZimbraQueryResults hits, SortBy sortOrder) {
        super(hits);
        mSortOrder = sortOrder;
    }

    @Override
    public boolean hasNext() throws ServiceException {
        return (mBufferedHit.size() > 0 || mHits.hasNext());
    }

    @Override
    protected boolean bufferHits() throws ServiceException {
        if (mBufferedHit.size() > 0){
            return true;
        }

        if (!mHits.hasNext()) {
            return false;
        }

        ZimbraHit curGroupHit = mHits.getNext();
        mBufferedHit.add(curGroupHit);

        // buffer all the hits with the same sort field
        while (mHits.hasNext() &&
                curGroupHit.compareBySortField(mSortOrder, mHits.peekNext()) == 0) {
            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("HitIdGrouper buffering " + mHits.peekNext());
            }
            mBufferedHit.add(mHits.getNext());
        }

        // sort them by mail-item-id
        Collections.sort(mBufferedHit,
                ZimbraHit.getSortAndIdComparator(mSortOrder));

        // we're done
        return true;
    }
}
