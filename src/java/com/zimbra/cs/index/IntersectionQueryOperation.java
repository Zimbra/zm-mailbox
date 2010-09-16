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
 * Set of query results ANDed together.
 */
public final class IntersectionQueryOperation extends CombiningQueryOperation {
    private static Log mLog = LogFactory.getLog(IntersectionQueryOperation.class);
    private boolean noHits = false;

    @Override
    public void resetIterator() throws ServiceException {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Intersection.resetIterator()");
        }
        mBufferedNext.clear();
        for (int i = 0; i < mMessageGrouper.length; i++) {
            mMessageGrouper[i].resetIterator();
        }
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        if (noHits || !hasNext()) {
            return null;
        }
        return mBufferedNext.remove(0);

    }

    List<ZimbraHit>mBufferedNext = new ArrayList<ZimbraHit>(1);

    /**
     * There can be multiple Hits with the same exact sort-field.  This function does
     * a complete N^2 intersection of all of the hits for a particular sort field.
     *
     * FIXME: this function is hideous, do _something_ with it.
     *
     * @throws ServiceException
     */
    void bufferNextHits() throws ServiceException {
        if (mBufferedNext.size() == 0) {
            TryAgain: while (true)
            {
                if (!this.mMessageGrouper[0].bufferNextHits()) {
                    return;
                }
                if (mLog.isDebugEnabled()) {
                    mLog.debug("\nMsgGrp0: "+mMessageGrouper[0].toString());
                }
                ZimbraHit curHit = mMessageGrouper[0].getGroupHit();
                int msgId = mMessageGrouper[0].getCurMsgId();

                // for every other op, buffer all the hits for this
                // step....
                for (int i = 1; i < mMessageGrouper.length; i++)
                {
                    //
                    // TODO check if this group is FINISHED and if so,
                    // quick return out
                    //
                    if (!(mMessageGrouper[i].bufferNextHits(curHit))) {
                        // no matches this grouper for that
                        // timestamp...go to top and try again
                        continue TryAgain;
                    }
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("MsgGrp"+i+": "+ mMessageGrouper[i].toString());
                    }

                }

                ArrayList<Integer>seenMsgs = new ArrayList<Integer>();

                do {
                    if (curHit != null && msgId > 0) {

                        // okay, do the big intersection
                        for (int i = 0; i < mMessageGrouper.length; i++) {
                            mMessageGrouper[i].setMsgId(msgId);
                            ZimbraHit hit = mMessageGrouper[i].getNextHit();
                            while (hit != null) {
                                if (!mBufferedNext.contains(hit)) {
                                    boolean ok = true;
                                    for (int j = 0; ok
                                    && j < mMessageGrouper.length; j++) {
                                        if (j != i) {
                                            if (hit instanceof MessageHit) {
                                                if (!mMessageGrouper[j]
                                                                     .intersectWithBuffer((MessageHit) hit)) {
                                                    ok = false;
                                                }
                                            } else if (hit instanceof MessagePartHit) {
                                                if (!mMessageGrouper[j]
                                                                     .intersectWithBuffer((MessagePartHit) hit)) {
                                                    ok = false;
                                                }
                                            }
                                        }
                                    } // intersect against every other group
                                    if (ok) {
                                        mBufferedNext.add(hit);
                                    }
                                } // contained in mBufferedNext?
                                hit = mMessageGrouper[i].getNextHit();
                            } // for each hit within group
                        } // for each group
                    } // assuming the first one isn't empty

                    seenMsgs.add(new Integer(msgId));
                    msgId = mMessageGrouper[0].getNextMessageId(seenMsgs);
                } while (msgId > 0);


                if (mBufferedNext.size() > 0) {
                    // we've got some hits -- so we can leave now...
                    break TryAgain;
                }
                // no hits -- go back to the top and try again.
            } // while true (for easy retry)

        for (int i = 0; i < mBufferedNext.size(); i++) {
            ZimbraHit hit = mBufferedNext.get(i);
            if (mLog.isDebugEnabled()) {
                mLog.debug("BUFFERED: "+hit.toString());
            }
        }

        }
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        if (noHits) {
            return null;
        } else {
            bufferNextHits();
            if (mBufferedNext.size() > 0) {
                return mBufferedNext.get(0);
            } else {
                return null;
            }
        }
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        for (int i = 0; i < mQueryOperations.size(); i++) {
            QueryOperation op = mQueryOperations.get(i);
            op.doneWithSearchResults();
        }
    }

    @Override
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        if (noHits) {
            return null;
        }
        return super.skipToHit(hitNo);
    }

    /**
     * Responsible for grouping sub-results with the same sort value into a chunk
     * so that they can then be combined
     *
     * 1) Call buffer() to buffer the next timestamp, or buffer(timestamp) to
     * buffer a particular timestamp
     *
     * 2) Iterate through all the messageId's in the current timestamp
     *
     * 3) call getNextHit() to iterate the hits within the current message OR
     * call intersectWithBuffer() to tell you if the a particular hit intersects
     * with something within our buffer
     *
     * Note: this class is somewhat confusing because there are really two completely
     * different paths through it: the first Grouper which gathers a bunch of hits and
     * then is iterated using getNextHit() and then the other groupers which gather hits
     * and then use intersectWithBuffer.....this is because we're using an N^2
     * intersection instead of an insertion intersection.....this should be fixed.
     */
    private static class HitGrouper {
        private QueryOperation mSubOp = null;

        private SortBy mSortOrder;

        @Override
        public String toString() {
            StringBuffer toRet = new StringBuffer(mSubOp.toString()+"\n\t");
            for (int i = 0; i < mBufferedHit.size(); i++) {
                ZimbraHit hit = mBufferedHit.get(i);
                toRet.append(hit.toString()).append("\n\t");
            }
            return toRet.toString();
        }

        HitGrouper(QueryOperation subOperation, SortBy sortOrder) {
            mSubOp = subOperation;
            mSortOrder = sortOrder;
        }

        void resetIterator() throws ServiceException {
            mBufferedHit.clear();
            mSubOp.resetIterator();
            mCurMsgId = -1;
            mGroupHit = null;
            mCurBufPos = 0;

        }

        private ArrayList <ZimbraHit>mBufferedHit = new ArrayList<ZimbraHit>();

        int getNextMessageId(List<Integer> seenMsgs) throws ServiceException {
            for (int i = 1; i < mBufferedHit.size(); i++) {
                Integer checkId = new Integer(mBufferedHit.get(i).getItemId());
                if (!seenMsgs.contains(checkId)) {
                    return checkId.intValue();
                }
            }
            return -1;
        }

        /**
         * Advance to the next timestamp and buffer one or more hits for that timestamp.
         *
         * @return
         * @throws ServiceException
         */
        boolean bufferNextHits() throws ServiceException {
            mBufferedHit.clear();

            //
            // step 1: establish the current stamp
            //
            if (!mSubOp.hasNext()) {
                return false;
            }
            mGroupHit = mSubOp.getNext();
            setMsgId(mGroupHit.getItemId());
            mBufferedHit.add(mGroupHit);

            //
            // step 2: buffer all hits with the current stamp
            //
            while (mSubOp.hasNext()) {
                ZimbraHit hit = mSubOp.peekNext();

                if (hit.compareBySortField(mSortOrder, mGroupHit) == 0) {
                    mBufferedHit.add(hit);
                    // go to next one:
                    ZimbraHit check = mSubOp.getNext();
                    assert (check == hit);
                } else {
                    return !mBufferedHit.isEmpty();
                }
            }
            return !mBufferedHit.isEmpty();
        }

        private int mCurMsgId = -1;

        private ZimbraHit mGroupHit = null; /*
         * ALL hits in this group will have the same
         * sort-order as this one
         */

        private int mCurBufPos = 0; // for iterating the current buffer

        int getCurMsgId() {
            return mCurMsgId;
        }

        /**
         * Returns a hit from the current message group. This is useful because
         * the hit's SORT FIELD is guaranteed to be the same as the sort field
         * in every other hit in this group (that's what the Message Grouper
         * does, after all)
         *
         * @return current hit
         */
        ZimbraHit getGroupHit() {
            return mGroupHit;
        }

        void setMsgId(int msgId) {
            mCurMsgId = msgId;
            mCurBufPos = 0;
        }

        ZimbraHit getNextHit() throws ServiceException {
            while (mCurBufPos < mBufferedHit.size()) {
                if (mBufferedHit.get(mCurBufPos).getItemId() == mCurMsgId) {
                    mCurBufPos++;
                    return mBufferedHit.get(mCurBufPos - 1);
                }
                mCurBufPos++;
            }
            return null;
        }

        boolean intersectWithBuffer(MessageHit hit) throws ServiceException {
            int hitMsgId = hit.getItemId();
            for (int i = 0; i < mBufferedHit.size(); i++) {
                if (mBufferedHit.get(i).getItemId() == hitMsgId) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Return TRUE if passed-in Hit intersects with something within my group.
         *
         * @param hit
         * @return
         * @throws ServiceException
         */
        boolean intersectWithBuffer(MessagePartHit hit) throws ServiceException {
            int hitMsgId = hit.getItemId();
            for (int i = 0; i < mBufferedHit.size(); i++) {
                ZimbraHit bufHit = mBufferedHit.get(i);
                if (bufHit.getItemId() == hitMsgId) {
                    if (bufHit instanceof MessagePartHit) {
                        MessagePartHit mph = (MessagePartHit) bufHit;
                        if (mph == hit) {
                            return true;
                        }
                    } else {
                        // msgID's must be equal
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Buffer a bunch of hits from SubOp, all hits must have a SortField
         * EQUAL TO curHit's SortField
         *
         * @param curHit
         * @return
         * @throws ServiceException
         */
        boolean bufferNextHits(ZimbraHit curHit) throws ServiceException {
            mGroupHit = curHit;
            mBufferedHit.clear();

            if (!mSubOp.hasNext()) {
                return false;
            }

            ZimbraHit newStamp = null;
            while ((newStamp = mSubOp.peekNext()) != null) {
                int result = newStamp.compareBySortField(mSortOrder, mGroupHit);
                if (result == 0) {
                    mBufferedHit.add(newStamp);
                    // go to nex thit
                    ZimbraHit check = mSubOp.getNext();
                    assert (check == newStamp);
                } else if (result < 0) {
                    // newstamp is logically "Before" current...skip it
                    // mNextHit = null;
                    mSubOp.getNext(); // skip next hit
                } else {
                    // newstamp is after us. Current doesn't match, but don't
                    // skip newStamp,
                    // we might still get to it.
                    //mNextHit = newStamp;
                    return mBufferedHit.size() > 0;
                }
            }
            return mBufferedHit.size() > 0;
        }
    }

    @Override
    boolean hasSpamTrashSetting() {
        boolean hasOne = false;
        for (Iterator<QueryOperation> iter = mQueryOperations.iterator(); !hasOne
        && iter.hasNext();) {
            QueryOperation op = iter.next();
            hasOne = op.hasSpamTrashSetting();
        }
        return hasOne;
    }

    @Override
    void forceHasSpamTrashSetting() {
        assert(false); // not called, but if it were, it would go:
        for (Iterator<QueryOperation> iter = mQueryOperations.iterator(); iter.hasNext();) {
            QueryOperation op = iter.next();
            op.forceHasSpamTrashSetting();
        }
    }

    @Override
    QueryTargetSet getQueryTargets() {
        QueryTargetSet  toRet = new QueryTargetSet();

        for (Iterator<QueryOperation> qopIter = mQueryOperations.iterator(); qopIter.hasNext();) {
            toRet = qopIter.next().getQueryTargets();

            // loop through rest of ops add to toRet if it is in every other set
            while (qopIter.hasNext()) {
                QueryTargetSet curSet = qopIter.next().getQueryTargets();

                // so this gets wacky:
                //  -- If both sides have an UNSPECIFIED, then the result is
                //     (RHS union LHS) including UNSPECIFIED
                //  -- If only LHS has an UNSPECIFIED, then the result is (RHS)
                //     If RHS then the result is LHS

                if (toRet.contains(QueryTarget.UNSPECIFIED)) {
                    if (curSet.contains(QueryTarget.UNSPECIFIED)) {
                        toRet = (QueryTargetSet)SetUtil.union(toRet, curSet);
                    } else {
                        toRet = curSet;
                    }
                } else if (curSet.contains(QueryTarget.UNSPECIFIED)) {
                    // toRet stays as it is...
                } else {
                    toRet = (QueryTargetSet)SetUtil.intersect(new QueryTargetSet(), toRet, curSet);
                }
            }
        }
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
        // just tack it on -- presumably this will be combined in the optimize()
        // step...
        if (!hasSpamTrashSetting()) {
            // ensureSpamTrashSetting might very well return a new root node...so we need
            // to build a new mQueryOperations list using the result of ensureSpamTrashSetting
            List<QueryOperation> newList = new ArrayList<QueryOperation>();
            for (QueryOperation op : mQueryOperations) {
                newList.add(op.ensureSpamTrashSetting(mbox, includeTrash, includeSpam));
            }
            mQueryOperations = newList;
        }
        return this;
    }

    public void addQueryOp(QueryOperation op) {
        assert(op!=null);
        mQueryOperations.add(op);
    }

    private void addQueryOps(List<QueryOperation>ops) {
        mQueryOperations.addAll(ops);
    }

    void pruneIncompatibleTargets(QueryTargetSet targets) {
        for (QueryOperation op : mQueryOperations) {
            if (op instanceof UnionQueryOperation) {
                ((UnionQueryOperation)op).pruneIncompatibleTargets(targets);
            } else if (op instanceof IntersectionQueryOperation) {
                assert(false); // shouldn't be here, should have optimized already
                ((IntersectionQueryOperation)op).pruneIncompatibleTargets(targets);
            } else {
                // do nothing, must be part of the right set
                QueryTargetSet qts = op.getQueryTargets();
                assert(qts.size() == 1);
                assert(qts.contains(QueryTarget.UNSPECIFIED) || qts.isSubset(targets));
            }
        }
    }


    /**
     * We always transform the query into DNF:
     *       a AND (b OR c)
     * into
     *       (a AND b) OR (a AND c)
     *
     * If b or c have different targets (servers they execute on) then we *must*
     * distribute but otherwise we have a choice.
     * <p>
     * Tim: setting this to ALWAYS for now.  I think in most cases it will be a
     * win, even though it appears to create 4 executable terms instead of 3. It
     * will be a win because (from the example above) in the 2nd case, it is
     * almost certain that one both terms will combine thereby reducing the
     * number of operations to 3 with no ANDs, which is always faster.
     *
     * The ideal solution would be to try both ways and compare the final # of
     * executable ops.
     * <p>
     * tim 1/2008: SortBy="none" requires this setting, and so if you want to
     * disable it you will need to pass down and check the requested Sort.
     * <p>
     * tim:1/2009 convinced this is always the best choice. The # ops is less
     * important than the number of rows evaluated. Pushing the AND down lowers
     * the total # rows.
     */
    @Override
    QueryOperation optimize(Mailbox mbox) throws ServiceException {
        // Step 1: optimize each individual sub-operation we have
        restartSubOpt: do {
            for (Iterator<QueryOperation> iter = mQueryOperations.iterator(); iter.hasNext();) {
                QueryOperation q = iter.next();
                QueryOperation newQ = q.optimize(mbox);
                if (newQ != q) {
                    iter.remove();
                    if (newQ != null) {
                        addQueryOp(newQ);
                    }
                    continue restartSubOpt;
                }

            }
            break;
        } while (true);

        // if all of our sub-ops optimized-away, then we're golden!
        if (mQueryOperations.size() == 0) {
            return new NoTermQueryOperation();
        }

        //
        // Step 2: do an N^2 combine() of all of our subops
        //
        outer: do {
            for (int i = 0; i < mQueryOperations.size(); i++) {
                QueryOperation lhs = mQueryOperations.get(i);

                // if one of our direct children is an and, then promote all of
                // its elements to our level -- this can happen if a subquery has
                // ANDed terms at the top level
                if (lhs instanceof IntersectionQueryOperation) {
                    combineOps(lhs, false);
                    mQueryOperations.remove(i);
                    continue outer;
                }

                for (int j = i + 1; j < mQueryOperations.size(); j++) {
                    QueryOperation rhs = mQueryOperations.get(j);
                    QueryOperation joined = lhs.combineOps(rhs, false);
                    if (joined != null) {
                        mQueryOperations.remove(j);
                        mQueryOperations.remove(i);
                        addQueryOp(joined);
                        continue outer;
                    }
                }
            }
            break outer;
        } while (true);

        //
        // Step 2.5: now we want to eliminate any subtrees that have query targets
        // which aren't compatible (ie (AorBorC) and (BorC) means we elim A
        //
        QueryTargetSet targets = getQueryTargets();
        if (targets.size() == 0) {
            mLog.debug("ELIMINATING "+toString()+" b/c of incompatible QueryTargets");
            return new NoResultsQueryOperation();
        }

        pruneIncompatibleTargets(targets);

        // Step 2.6
        //
        // incompat targets are pruned, now distribute as necessary
        //
        // at this point we can assume that all the invalid targets have been pruned
        //
        // We only have to distribute if there is more than one explicit target,
        // otherwise we know we can be executed on one server so we're golden.
        int distributeLhs = -1;
        for (int i = 0; i < mQueryOperations.size(); i++) {
            QueryOperation lhs = mQueryOperations.get(i);
            if (lhs instanceof UnionQueryOperation ||
                    lhs.getQueryTargets().size() > 1) {
                // need to distribute!
                distributeLhs = i;
                break;
            }
        }

        if (distributeLhs >= 0) {
            // if lhs has >1 explicit target at this point, it MUST be a union...
            UnionQueryOperation lhs = (UnionQueryOperation)(mQueryOperations.remove(distributeLhs));
            UnionQueryOperation topOp = new UnionQueryOperation();

            for (QueryOperation lhsCur : lhs.mQueryOperations)
            {
                IntersectionQueryOperation newAnd = new IntersectionQueryOperation();
                topOp.add(newAnd);

                newAnd.addQueryOp(lhsCur);

                for (QueryOperation rhsCur : mQueryOperations) {
                    newAnd.addQueryOp((QueryOperation)(rhsCur.clone()));
                }
            }

            // recurse!
            return topOp.optimize(mbox);
        }

        // at this point, we know that the entire query has one and only one QueryTarget.
        assert(getQueryTargets().countExplicitTargets() <= 1);

        //
        // Step 3: hacky special case for Lucene Ops and DB Ops: Lucene and DB don't
        // combine() like other operations -- if they did, then we'd run the risk of
        // failing to combine OR'ed Lucene terms (OR'ed DB terms don't combine) -- instead
        // we wait until here to combine those terms.  Weird, but functional.
        //
        // WARNING: Lucene ops ALWAYS combine, so we assume there is only one!
        LuceneQueryOperation lop = null;
        for (Iterator<QueryOperation> iter = mQueryOperations.iterator(); iter.hasNext();) {
            QueryOperation op = iter.next();
            if (op instanceof LuceneQueryOperation) {
                lop = (LuceneQueryOperation) op;
                iter.remove();
                break;
            }
        }
        if (lop != null) {
            boolean foundIt = false;
            for (QueryOperation op : mQueryOperations) {
                if (op instanceof DBQueryOperation) {
                    ((DBQueryOperation) op).setLuceneQueryOperation(lop);
                    foundIt = true;
                }
            }
            if (!foundIt) {
                // add the lucene op back in!
                addQueryOp(lop);
            }
        }

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
                ret.append(" AND ");

            ret.append(op.toQueryString());
            atFirst = false;
        }

        ret.append(')');
        return ret.toString();
    }

    @Override
    public String toString() {
        StringBuilder retval = new StringBuilder("INTERSECTION[");

        boolean atFirst = true;

        for (Iterator<QueryOperation> iter = mQueryOperations.iterator(); iter.hasNext();) {
            QueryOperation op = iter.next();
            if (atFirst)
                atFirst = false;
            else
                retval.append(" AND ");

            retval.append(op.toString());
        }
        retval.append("]");
        return retval.toString();
    }

    @Override
    public Object clone() {
        IntersectionQueryOperation toRet = (IntersectionQueryOperation)super.clone();

        assert(mMessageGrouper == null);

        toRet.mBufferedNext = new ArrayList<ZimbraHit>(1);

        toRet.mQueryOperations = new ArrayList<QueryOperation>(mQueryOperations.size());
        for (QueryOperation q : mQueryOperations)
            toRet.mQueryOperations.add((QueryOperation)(q.clone()));

        return toRet;
    }

    @Override
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        if (!union && other instanceof IntersectionQueryOperation) {
            addQueryOps(((IntersectionQueryOperation) other).mQueryOperations);
            return this;
        }
        return null;
    }

    private HitGrouper mMessageGrouper[] = null;

    @Override
    protected void begin(QueryContext ctx) throws ServiceException {
        assert(context == null);
        // scale up the chunk size since we are doing an intersection...
        context = new QueryContext(ctx.getMailbox(), ctx.getResults(),
                ctx.getParams(), (ctx.getChunkSize() + 1) * 3);

        mMessageGrouper = new HitGrouper[mQueryOperations.size()];

        for (int i = 0; i < mQueryOperations.size(); i++) {
            QueryOperation op = mQueryOperations.get(i);
            op.begin(ctx);
            mMessageGrouper[i] = new HitGrouper(op,
                    context.getResults().getSortBy());

            if (!op.hasNext()) {
                //
                // This operation has no terms at all. Since we're an
                // Intersection query, that means that
                // this entire query has no results. Sooo, lets release all of
                // the operations we've already
                // prepare()d and create a single operation, a
                // NullQueryOperation below us.
                //
                if (mLog.isDebugEnabled()) {
                    mLog.debug("*Dropping out of intersect query since we got to 0 results on execution "
                                + Integer.toString(i + 1)
                                + " out of "
                                + mQueryOperations.size());
                }

                // first, we need to be DONE with all unused query operations..
                for (int j = 0; j <= i; j++) {
                    mQueryOperations.get(j).doneWithSearchResults();
                }

                mQueryOperations.clear();
                mMessageGrouper = new HitGrouper[1];

                QueryOperation nullOp = new NoResultsQueryOperation();
                addQueryOp(nullOp);
                mMessageGrouper[0] = new HitGrouper(nullOp,
                        context.getResults().getSortBy());
                return;
            }
        }
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
        if (mQueryOperations.size() == 0)
            return 0;

        int maxValue = Integer.MAX_VALUE;
        for (QueryOperation qop : mQueryOperations) {
            // assume half of the entries match?
            maxValue = Math.min(maxValue, qop.estimateResultSize())/2;
        }

        return maxValue;
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
