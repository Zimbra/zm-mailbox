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
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * UngroupedQueryResults which do NOT group (ie return parts or messages in whatever mix)
 *
 * @since Nov 3, 2004
 */
class UngroupedQueryResults extends ZimbraQueryResultsImpl {
    ZimbraQueryResults mResults;

    UngroupedQueryResults(ZimbraQueryResults results, Set<Byte> types,
            SortBy searchOrder, Mailbox.SearchResultMode mode) {
        super(types, searchOrder, mode);
        mResults = results;
    }

    @Override
    public void resetIterator() throws ServiceException {
        mResults.resetIterator();
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        return mResults.getNext();
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        return mResults.peekNext();
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        mResults.doneWithSearchResults();
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        return mResults.skipToHit(hitNo);
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return mResults.getResultInfo();
    }

    @Override
    public int estimateResultSize() throws ServiceException {
        return mResults.estimateResultSize();
    }

}
