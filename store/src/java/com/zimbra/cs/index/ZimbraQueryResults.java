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

import java.io.Closeable;
import java.util.List;

import com.zimbra.common.service.ServiceException;

/**
 * Interface for iterating through {@link ZimbraHit}s. This class is the thing that is returned when you do a Search.
 *
 * @since Mar 15, 2005
 * @author tim
 */
public interface ZimbraQueryResults extends Closeable {

    /**
     * Resets the iterator to the beginning
     *
     * @throws ServiceException
     */
    void resetIterator() throws ServiceException;

    /**
     * @return The next hit, advancing the iterator.
     * @throws ServiceException
     */
    ZimbraHit getNext() throws ServiceException;

    /**
     * @return The next hit without advancing the iterator.
     * @throws ServiceException
     */
    ZimbraHit peekNext() throws ServiceException;

    /**
     * Slightly more efficient in a few cases (DB-only queries), skip to
     * a specific hit offset.
     *
     * @param hitNo
     * @return
     * @throws ServiceException
     */
    ZimbraHit skipToHit(int hitNo) throws ServiceException;

    /**
     * @return TRUE if there is another Hit
     *
     * @throws ServiceException
     */
    boolean hasNext() throws ServiceException;

    /**
     * Note that in some cases, this might be a different Sort from the one
     * passed into Mailbox.Search() -- if the sort is overridden by a "Sort:"
     * operator in the search string.
     *
     * @return The Sort used by these results.
     */
    SortBy getSortBy();

    /**
     * {@link QueryInfo} is returned from the Search subsystem with meta
     * information about the search, such as information about wildcard
     * expansion, etc.
     *
     * @return
     */
    List<QueryInfo> getResultInfo();

    /**
     * Returns the cursor offset from the top, or -1 if undetermined.
     *
     * @return offset of the cursor position from the top
     */
    long getCursorOffset();

    /**
     * @return true if results are already sorted in the desired order - for instance they are based on
     * a proxied search.
     */
    public boolean isPreSorted();

    /**
     * @return true if these results support relevance sort
     */
    public boolean isRelevanceSortSupported();
}
