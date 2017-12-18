/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import com.google.common.collect.ImmutableSet;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * A {@link QueryOperation} which is generated when a query term evaluates to "nothing".
 *
 * This is not the same as a {@link NoResultsQueryOperation} because:
 * <ul>
 *  <li>{@code LuceneQueryOperation AND NoTermQueryOperation = LuceneQueryOperation}
 *  <li>{@code DBQueryOperation AND NoTermQueryOperation = NoTermQueryOperation}
 * </ul>
 *
 * It is also not the same as an {@link AllQueryOperation} because:
 * <ul>
 *  <li>{@code RESULTS(NoTermQueryOperation) = NONE}
 * </ul>
 *
 * Basically, this pseudo-Operation is here to handle the situation when a Lucene term evaluates to the empty string
 * (as might happen if a stop-word were searched for, e.g. searching for "the") -- by generating a special-purpose
 * Pseudo-Operation for this case we can hand-tune the Optimizer behavior and make it do the right thing in all cases.
 *
 * @author tim
 */
public final class NoTermQueryOperation extends QueryOperation {

    @Override
    public long getCursorOffset() {
        return -1;
    }

    @Override
    protected void begin(QueryContext ctx) {
        assert(context == null);
        context = ctx;
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
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) {
        return this;
    }

    @Override
    boolean hasSpamTrashSetting() {
        return false;
    }

    @Override
    void forceHasSpamTrashSetting() {
        assert(false);
    }

    @Override
    String toQueryString() {
        return "";
    }

    @Override
    public String toString() {
        return "NO_TERM_QUERY_OP";
    }

    @Override
    Set<QueryTarget> getQueryTargets() {
        return ImmutableSet.of(QueryTarget.UNSPECIFIED);
    }

    @Override
    boolean hasNoResults() {
        return false;
    }

    @Override
    boolean hasAllResults() {
        return false;
    }

    @Override
    QueryOperation optimize(Mailbox mbox) {
        return this;
    }

    @Override
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        return other;
    }

    @Override
    public void resetIterator() {
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
    public List<QueryInfo> getResultInfo() {
        return new ArrayList<QueryInfo>();
    }

    @Override
    protected void depthFirstRecurse(RecurseCallback cb) {
    }

    @Override
    public boolean isRelevanceSortSupported() {
        return false;
    }

}
