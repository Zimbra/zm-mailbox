/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
import java.util.Iterator;
import java.util.List;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;

/**
 * A list of query operations which are unioned together.
 *
 * @since Oct 29, 2004
 */
public final class UnionQueryOperation extends CombiningQueryOperation {
    private static Log mLog = LogFactory.getLog(UnionQueryOperation.class);

    private boolean atStart = true; // don't re-fill buffer twice if they call hasNext() then reset() w/o actually getting next
    private ZimbraHit mCachedNextHit = null;

    @Override
    QueryTargetSet getQueryTargets() {
        QueryTargetSet toRet = new QueryTargetSet();

        for (QueryOperation op : mQueryOperations) {
            toRet = (QueryTargetSet)SetUtil.union(toRet, op.getQueryTargets());
        }
        return toRet;
    }

    @Override
    public void resetIterator() throws ServiceException {
        if (!atStart) {
            for (Iterator<QueryOperation> iter = mQueryOperations.iterator(); iter.hasNext(); ) {
                QueryOperation q = iter.next();
                q.resetIterator();
            }
            mCachedNextHit = null;
            internalGetNext();
        }
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        atStart = false;
        ZimbraHit toRet = mCachedNextHit;
        if (mCachedNextHit != null) { // this "if" is here so we don't keep calling internalGetNext when we've reached the end of the results...
            mCachedNextHit = null;
            internalGetNext();
        }

        return toRet;
    }

    @Override
    public ZimbraHit peekNext() {
        return mCachedNextHit;
    }

    private void internalGetNext() throws ServiceException {
        if (mCachedNextHit == null) {
            if (context.getResults().getSortBy() == SortBy.NONE) {
                for (QueryOperation op : mQueryOperations) {
                    mCachedNextHit = op.getNext();
                    if (mCachedNextHit != null)
                        return;
                }
                // no more results!

            } else {
                // mergesort: loop through QueryOperations and find the "best" hit
                int currentBestHitOffset = -1;
                ZimbraHit currentBestHit = null;
                for (int i = 0; i < mQueryOperations.size(); i++) {
                    QueryOperation op = mQueryOperations.get(i);
                    if (op.hasNext()) {
                        if (currentBestHitOffset == -1) {
                            currentBestHitOffset = i;
                            currentBestHit = op.peekNext();
                        } else {
                            ZimbraHit opNext = op.peekNext();
                            int result = opNext.compareBySortField(context.getResults().getSortBy(), currentBestHit);
                            if (result < 0) {
                                // "before"
                                currentBestHitOffset = i;
                                currentBestHit = opNext;
                            }
                        }
                    }
                }
                if (currentBestHitOffset > -1) {
                    mCachedNextHit = mQueryOperations.get(currentBestHitOffset).getNext();
                    assert(mCachedNextHit == currentBestHit);
                }
            }
        }
    }


    @Override
    public void doneWithSearchResults() throws ServiceException {
        for (Iterator<QueryOperation> iter = mQueryOperations.iterator(); iter.hasNext(); ) {
            QueryOperation q = iter.next();
            q.doneWithSearchResults();
        }
    }

    @Override
    public boolean hasSpamTrashSetting() {
        boolean hasAll = true;
        for (Iterator<QueryOperation> iter = mQueryOperations.iterator(); hasAll && iter.hasNext(); ) {
            QueryOperation op = iter.next();
            hasAll = op.hasSpamTrashSetting();
        }
        return hasAll;
    }

    @Override
    void forceHasSpamTrashSetting() {
        for (Iterator<QueryOperation> iter = mQueryOperations.iterator(); iter.hasNext(); ) {
            QueryOperation op = iter.next();
            op.forceHasSpamTrashSetting();
        }
    }

    QueryTarget getQueryTarget(QueryTarget targetOfParent) {
        return targetOfParent;
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
    QueryOperation expandLocalRemotePart(Mailbox mbox) throws ServiceException {
        List<QueryOperation> newList = new ArrayList<QueryOperation>();
        for (QueryOperation op : mQueryOperations) {
            newList.add(op.expandLocalRemotePart(mbox));
        }
        mQueryOperations = newList;
        return this;
    }

    @Override
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException {
        ArrayList<QueryOperation> newList = new ArrayList<QueryOperation>();

        for (Iterator<QueryOperation> iter = mQueryOperations.iterator(); iter.hasNext(); ) {
            QueryOperation op = iter.next();
            if (!op.hasSpamTrashSetting()) {
                newList.add(op.ensureSpamTrashSetting(mbox, includeTrash, includeSpam));
            } else {
                newList.add(op);
            }
        }
        assert(newList.size() == mQueryOperations.size());
        mQueryOperations = newList;
        return this;
    }


    public void add(QueryOperation op) {
        mQueryOperations.add(op);
    }

    void pruneIncompatibleTargets(QueryTargetSet targets) {
        // go from end--front so we don't get confused when entries are removed
        for (int i = mQueryOperations.size()-1; i >= 0; i--) {
            QueryOperation op = mQueryOperations.get(i);
            if (op instanceof UnionQueryOperation) {
                assert(false); // shouldn't be here, should have optimized already
                ((UnionQueryOperation)op).pruneIncompatibleTargets(targets);
            } else if (op instanceof IntersectionQueryOperation) {
                ((IntersectionQueryOperation)op).pruneIncompatibleTargets(targets);
            } else {
                QueryTargetSet qts = op.getQueryTargets();
                assert(qts.size() <= 1);
                if ((qts.size() == 0) || (!qts.isSubset(targets) && !qts.contains(QueryTarget.UNSPECIFIED))) {
                    mQueryOperations.remove(i);
                }
            }
        }
    }

    @Override
    public QueryOperation optimize(Mailbox mbox) throws ServiceException {
        restartSubOpt:
            do {
                for (Iterator<QueryOperation> iter = mQueryOperations.iterator(); iter.hasNext(); ) {
                    QueryOperation q = iter.next();
                    QueryOperation newQ = q.optimize(mbox);
                    if (newQ != q) {
                        iter.remove();
                        if (newQ != null) {
                            mQueryOperations.add(newQ);
                        }
                        continue restartSubOpt;
                    }
                }
                break;
            } while(true);

        if (mQueryOperations.size() == 0) {
            return new NoTermQueryOperation();
        }

        outer: do {
            for (int i = 0; i < mQueryOperations.size(); i++) {
                QueryOperation lhs = mQueryOperations.get(i);

                // if one of our direct children is an OR, then promote all of its
                // elements to our level -- this can happen if a subquery has
                // ORed terms at the top level
                if (lhs instanceof UnionQueryOperation) {
                    combineOps(lhs, true);
                    mQueryOperations.remove(i);
                    continue outer;
                }

                for (int j = i+1; j < mQueryOperations.size(); j++) {
                    QueryOperation rhs = mQueryOperations.get(j);
                    QueryOperation joined = lhs.combineOps(rhs,true);
                    if (joined != null) {
                        mQueryOperations.remove(j);
                        mQueryOperations.remove(i);
                        mQueryOperations.add(joined);
                        continue outer;
                    }
                }
            }
            break;
        } while(true);

        // now - check to see if we have only one child -- if so, then WE can be
        // eliminated, so push the child up
        if (mQueryOperations.size() == 1) {
            return mQueryOperations.get(0);
        }

        return this;
    }

    @Override
    String toQueryString() {
        StringBuilder ret = new StringBuilder("(");

        boolean atFirst = true;

        for (QueryOperation op : mQueryOperations) {
            if (!atFirst)
                ret.append(" OR ");

            ret.append(op.toQueryString());
            atFirst = false;
        }

        ret.append(')');
        return ret.toString();
    }

    @Override
    public String toString() {
        StringBuilder retval = new StringBuilder("UNION{");

        boolean atFirst = true;

        for (int i = 0; i < mQueryOperations.size(); i++) {
            if (atFirst)
                atFirst = false;
            else
                retval.append(" OR ");

            retval.append(mQueryOperations.get(i).toString());
        }
        retval.append("}");
        return retval.toString();
    }

    @Override
    public Object clone() {
        UnionQueryOperation toRet = null;
        toRet = (UnionQueryOperation)super.clone();

        assert(mCachedNextHit == null);

        toRet.mQueryOperations = new ArrayList<QueryOperation>(mQueryOperations.size());
        for (QueryOperation q : mQueryOperations)
            toRet.mQueryOperations.add((QueryOperation)(q.clone()));

        return toRet;
    }

    @Override
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        if (union && other instanceof UnionQueryOperation) {
            mQueryOperations.addAll(((UnionQueryOperation)other).mQueryOperations);
            return this;
        }
        return null;
    }

    @Override
    protected void begin(QueryContext ctx) throws ServiceException {
        assert(context == null);
        context = ctx;

        for (int i = 0; i < mQueryOperations.size(); i++) {
            QueryOperation qop = mQueryOperations.get(i);
            if (mLog.isDebugEnabled()) {
                mLog.debug("Executing: " + qop.toString());
            }
            // add 1 to chunk size b/c we buffer
            qop.begin(new QueryContext(ctx.getMailbox(), ctx.getResults(),
                    ctx.getParams(), ctx.getChunkSize() + 1));
        }

        internalGetNext();
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        List<QueryInfo> toRet = new ArrayList<QueryInfo>();
        for (QueryOperation op : mQueryOperations) {
            toRet.addAll(op.getResultInfo());
        }
        return toRet;
    }

    @Override
    public int estimateResultSize() throws ServiceException {
        int total = 0;
        for (QueryOperation qop : mQueryOperations) {
            // assume ORed terms are independent for now
            total += qop.estimateResultSize();
        }
        return total;
    }

    @Override
    protected void depthFirstRecurse(RecurseCallback cb) {
        for (int i = 0; i < mQueryOperations.size(); i++) {
            QueryOperation op = mQueryOperations.get(i);
            op.depthFirstRecurse(cb);
        }
        cb.recurseCallback(this);
    }
}
