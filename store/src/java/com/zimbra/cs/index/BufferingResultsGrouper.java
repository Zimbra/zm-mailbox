/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
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
    public long getCursorOffset() {
        return hits.getCursorOffset();
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
    public void close() throws IOException {
        hits.close();
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return hits.getResultInfo();
    }

    @Override
    public boolean isPreSorted() {
        return hits.isPreSorted();
    }

    @Override
    public boolean isRelevanceSortSupported() {
        return hits.isRelevanceSortSupported();
    }
}
