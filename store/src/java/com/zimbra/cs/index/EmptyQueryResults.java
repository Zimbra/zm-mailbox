/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.zimbra.cs.mailbox.MailItem;

/**
 * @since Oct 22, 2004
 * @author tim
 */
final class EmptyQueryResults extends ZimbraQueryResultsImpl {

    EmptyQueryResults(Set<MailItem.Type> types, SortBy searchOrder, SearchParams.Fetch fetch) {
        super(types, searchOrder, fetch);
    }

    @Override
    public long getCursorOffset() {
        return 0;
    }

    @Override
    public void resetIterator()  {
    }

    @Override
    public ZimbraHit getNext() {
        return null;
    }

    @Override
    public ZimbraHit peekNext() {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) {
        return null;
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return new ArrayList<QueryInfo>();
    }

    @Override
    public boolean isRelevanceSortSupported() {
        return false;
    }

}
