/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2011 Zimbra, Inc.
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

}
