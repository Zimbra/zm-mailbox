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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * @since Oct 22, 2004
 * @author tim
 */
class EmptyQueryResults extends ZimbraQueryResultsImpl {


    EmptyQueryResults(Set<Byte> types, SortBy searchOrder, Mailbox.SearchResultMode mode) {
        super(types, searchOrder, mode);
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
    public void doneWithSearchResults() {
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
    public int estimateResultSize() throws ServiceException {
        return 0;
    }

}
