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

import java.util.Collections;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

/**
 * Take {@link ZimbraHit}s which are already sorted by sort-order and additionally sort them by mail-item-id.
 * <p>
 * This Grouper has no effect if the current sort mode is "none"
 *
 * @since Mar 15, 2005
 * @author tim
 */
public final class HitIdGrouper extends BufferingResultsGrouper {
    private final SortBy sortOrder;

    public static ZimbraQueryResults create(ZimbraQueryResults hits, SortBy sortOrder) {
        if (sortOrder == SortBy.NONE) {
            return hits;
        } else {
            return new HitIdGrouper(hits, sortOrder);
        }
    }

    private HitIdGrouper(ZimbraQueryResults hits, SortBy sort) {
        super(hits);
        sortOrder = sort;
    }

    @Override
    public boolean hasNext() throws ServiceException {
        return (bufferedHit.size() > 0 || hits.hasNext());
    }

    @Override
    protected boolean bufferHits() throws ServiceException {
        if (bufferedHit.size() > 0){
            return true;
        }

        if (!hits.hasNext()) {
            return false;
        }

        ZimbraHit curGroupHit = hits.getNext();
        bufferedHit.add(curGroupHit);

        // buffer all the hits with the same sort field
        while (hits.hasNext() && curGroupHit.compareBySortField(sortOrder, hits.peekNext()) == 0) {
            ZimbraLog.search.debug("HitIdGrouper buffering %s", hits.peekNext());
            bufferedHit.add(hits.getNext());
        }

        // sort them by mail-item-id
        Collections.sort(bufferedHit, ZimbraHit.getSortAndIdComparator(sortOrder));

        // we're done
        return true;
    }
}
