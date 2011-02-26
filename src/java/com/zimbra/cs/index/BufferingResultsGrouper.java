/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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

import java.util.LinkedList;
import java.util.List;

import com.zimbra.common.service.ServiceException;

/**
 * Groups hit results for various reasons.
 */
public abstract class BufferingResultsGrouper implements ZimbraQueryResults {

    protected final ZimbraQueryResults hits;
    protected List<ZimbraHit> bufferedHit = new LinkedList<ZimbraHit>();
    protected boolean atStart = true;

    /**
     * Fills the hit buffer if necessary.  May be called even if the buffer has entries in it,
     * implementation may ignore it (but must return true) in those cases.
     *
     * @return TRUE if there some hits in the buffer, FALSE if not.
     */
    protected abstract boolean bufferHits() throws ServiceException;

    @Override
    public SortBy getSortBy() {
        return hits.getSortBy();
    }

    public BufferingResultsGrouper(ZimbraQueryResults hits) {
        this.hits = hits;
    }

    @Override
    public long getTotalHitCount() throws ServiceException {
        return hits.getTotalHitCount();
    }

    @Override
    public void resetIterator() throws ServiceException {
        if (!atStart) {
            bufferedHit.clear();
            hits.resetIterator();
            atStart = true;
        }
    }

    @Override
    public boolean hasNext() throws ServiceException {
        return bufferHits();
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        if (bufferHits()) {
            return bufferedHit.get(0);
        } else {
            return null;
        }
    }

    @Override
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

    @Override
    public ZimbraHit getNext() throws ServiceException {
        atStart = false;
        if (bufferHits()) {
            return bufferedHit.remove(0);
        } else {
            return null;
        }
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        hits.doneWithSearchResults();
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return hits.getResultInfo();
    }

}
