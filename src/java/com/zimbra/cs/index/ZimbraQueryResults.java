/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import java.util.List;

import com.zimbra.common.service.ServiceException;

/**
 * Interface for iterating through {@link ZimbraHit}s. This class is the thing
 * that is returned when you do a Search.
 *
 * @since Mar 15, 2005
 * @author tim
 */
public interface ZimbraQueryResults {

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
     * @deprecated call {@link #resetIterator()} then {@link #getNext()}.
     */
    @Deprecated
    ZimbraHit getFirstHit() throws ServiceException;

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
     * MUST be called when you are done with this iterator!
     * <p>
     * If this is not called, file descriptors can be leaked.
     *
     * @throws ServiceException
     */
    void doneWithSearchResults() throws ServiceException;

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
     * Two important requirements for this API:
     * <ul>
     *  <li>{@link SearchParams#setEstimateSize(boolean)} must have been set
     *  with true on the query's {@link SearchParams}, otherwise the estimate is
     *  not calculated (it is not free to calculate so we only do it when requested).
     *  <li>The size is not estimated until the first result is fetched. That
     *  means you must call {@link #getNext()} or {@link #peekNext()} or similar
     *  hit-returning API before calling this function.
     * </ul>
     *
     * @return An ESTIMATE (may be wrong, very wrong in some cases) of the size
     * of the result set.
     */
    int estimateResultSize() throws ServiceException;

}
