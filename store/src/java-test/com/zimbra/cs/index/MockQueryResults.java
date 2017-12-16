/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailItem;

/**
 * Mock implementation of {@link ZimbraQueryResults} for testing.
 *
 * @author ysasaki
 */
public final class MockQueryResults extends ZimbraQueryResultsImpl {

    private List<ZimbraHit> hits = new ArrayList<ZimbraHit>();
    private int next = 0;
    private final List<QueryInfo> queryInfo = new ArrayList<QueryInfo>();

    public MockQueryResults(Set<MailItem.Type> types, SortBy sort) {
        super(types, sort, SearchParams.Fetch.NORMAL);
    }

    public void add(ZimbraHit hit) {
        hits.add(hit);
    }

    @Override
    public long getCursorOffset() {
        return -1;
    }

    @Override
    public void resetIterator() {
        next = 0;
    }

    @Override
    public ZimbraHit getNext() {
        return hits.get(next++);
    }

    @Override
    public ZimbraHit peekNext() {
        return hits.get(next);
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        next = hitNo;
        return getNext();
    }

    @Override
    public boolean hasNext() throws ServiceException {
        return next < hits.size();
    }

    @Override
    public void close() {
        hits = null;
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return queryInfo;
    }

    @Override
    public boolean isRelevanceSortSupported() {
        return false;
    }
}
