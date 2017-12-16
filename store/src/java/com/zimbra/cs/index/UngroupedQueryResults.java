/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;

/**
 * UngroupedQueryResults which do NOT group (ie return parts or messages in whatever mix)
 *
 * @since Nov 3, 2004
 */
final class UngroupedQueryResults extends ZimbraQueryResultsImpl {
    private final ZimbraQueryResults results;

    UngroupedQueryResults(ZimbraQueryResults results, Set<MailItem.Type> types, SortBy sort, SearchParams.Fetch fetch) {
        super(types, sort, fetch);
        this.results = results;
    }

    @Override
    public long getCursorOffset() {
        return results.getCursorOffset();
    }

    @Override
    public void resetIterator() throws ServiceException {
        results.resetIterator();
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        return results.getNext();
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        return results.peekNext();
    }

    @Override
    public void close() throws IOException {
        results.close();
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        return results.skipToHit(hitNo);
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return results.getResultInfo();
    }

    @Override
    public boolean isPreSorted() {
        return results.isPreSorted();
    }

    @Override
    public boolean isRelevanceSortSupported() {
        return results.isRelevanceSortSupported();
    }
}
