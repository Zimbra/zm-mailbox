/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import com.zimbra.cs.mailbox.Mailbox;

/**
 * A query operation which returns no elements at all.
 *
 * This is generated, for example, when we determine that part of the query can
 * never return any results, eg: (A and not A)
 *
 * @since Nov 12, 2004
 */
final class NoResultsQueryOperation extends QueryOperation {

    @Override
    public SortBy getSortBy() {
        return context.getParams().getSortBy();
    }


    @Override
    QueryOperation expandLocalRemotePart(Mailbox mbox) {
        return this;
    }

    @Override
    public QueryOperation ensureSpamTrashSetting(Mailbox mbox,
            boolean includeTrash, boolean includeSpam) {
        return this;
    }

    @Override
    public boolean hasSpamTrashSetting() {
        // if someone ANDS with us, then there's no need to set the spam-trash b/c
        // we match nothing.  On the other hand, if someone OR's with us, this func's
        // return won't matter
        return true;
    }
    @Override
    void forceHasSpamTrashSetting() {
        //empty
    }

    @Override
    QueryTargetSet getQueryTargets() {
        return new QueryTargetSet();
    }

    @Override
    boolean hasNoResults() {
        return true;
    }
    @Override
    boolean hasAllResults() {
        return false;
    }

    @Override
    String toQueryString() {
        return "";
    }

    @Override
    public String toString() {
        return "NO_RESULTS_QUERY_OP";
    }


    @Override
    public void resetIterator() {
        //empty
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
    protected void begin(QueryContext ctx) {
        assert(context == null);
        context = ctx;
    }

    @Override
    public void doneWithSearchResults() {
        //empty
    }

    @Override
    public QueryOperation optimize(Mailbox mbox) {
        return this;
    }

    @Override
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        return null;
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        return new ArrayList<QueryInfo>();
    }

    @Override
    public int estimateResultSize() {
        return 0;
    }

    @Override
    protected void depthFirstRecurse(RecurseCallback cb) {
        //empty
    }

}
