/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

}
