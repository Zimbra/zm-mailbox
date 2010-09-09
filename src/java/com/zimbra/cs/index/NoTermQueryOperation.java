/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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
 * A QueryOperation which is generated when a query term evaluates to "nothing".
 *
 * This is not the same as a NullQueryOperation because:
 *     RESULTS(Op AND NoTermQuery) = RESULTS(Op)
 *
 * It is also not the same as an AllQueryOperation because:
 *     RESULTS(NoTemQuery) = NONE
 *
 * Basically, this pseudo-Operation is here to handle the situation when a Lucene term
 * evaluates to the empty string (as might happen if a stopword were searched for,
 * eg searching for "the") -- by generating a special-purpose Pseudo-Operation for
 * this case we can hand-tune the Optimizer behavior and make it do the right thing in all
 * cases.
 *
 * @author tim
 */
public final class NoTermQueryOperation extends QueryOperation {

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
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash,
            boolean includeSpam) {
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
    QueryTargetSet getQueryTargets() {
        QueryTargetSet toRet = new QueryTargetSet(1);
        toRet.add(QueryTarget.UNSPECIFIED);
        return toRet;
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
        return null;
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
    public void doneWithSearchResults() {
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
    }

}
