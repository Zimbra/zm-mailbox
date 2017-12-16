/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * A query operation which returns no elements at all.
 *
 * This is generated, for example, when we determine that part of the query can
 * never return any results, eg: (A and not A)
 *
 * @since Nov 12, 2004
 */
public final class NoResultsQueryOperation extends QueryOperation {

    @Override
    public long getCursorOffset() {
        return 0;
    }

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
    Set<QueryTarget> getQueryTargets() {
        return Collections.emptySet();
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
    public void close() {
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
    protected void depthFirstRecurse(RecurseCallback cb) {
        //empty
    }

    @Override
    public boolean isRelevanceSortSupported() {
        return false;
    }

}
