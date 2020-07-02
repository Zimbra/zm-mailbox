/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.IOUtil;
import com.zimbra.common.service.ServiceException;

import com.zimbra.common.util.ZimbraLog;

/**
 * Set of query results ANDed together.
 */
public final class IntersectionQueryOperation extends CombiningQueryOperation {
    private boolean noHits = false;
    private List<ZimbraHit> bufferedNext = new ArrayList<ZimbraHit>(1);
    private HitGrouper messageGrouper[] = null;

    @Override
    public long getCursorOffset() {
        return -1;
    }

    @Override
    public void resetIterator() throws ServiceException {
        bufferedNext.clear();
        for (int i = 0; i < messageGrouper.length; i++) {
            messageGrouper[i].resetIterator();
        }
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        if (noHits || !hasNext()) {
            return null;
        }
        return bufferedNext.remove(0);
    }

    /**
     * There can be multiple Hits with the same exact sort-field. This function does a complete N^2 intersection of all
     * of the hits for a particular sort field.
     *
     * FIXME: this function is hideous, do _something_ with it.
     */
    void bufferNextHits() throws ServiceException {
        if (bufferedNext.size() > 0) {
            return;
        }
        TryAgain: while (true) {
            if (!messageGrouper[0].bufferNextHits()) {
                return;
            }
            ZimbraLog.search.debug("MsgGrp0: %s", messageGrouper[0]);
            ZimbraHit curHit = messageGrouper[0].getGroupHit();
            int msgId = messageGrouper[0].getCurMsgId();

            // for every other op, buffer all the hits for this
            // step....
            for (int i = 1; i < messageGrouper.length; i++) {
                // TODO check if this group is FINISHED and if so, quick return out
                if (!(messageGrouper[i].bufferNextHits(curHit))) {
                    // no matches this grouper for that timestamp...go to top and try again
                    continue TryAgain;
                }
                ZimbraLog.search.debug("MsgGrp[%d]: %s", i, messageGrouper[i]);
            }

            List<Integer> seenMsgs = new ArrayList<Integer>();

            do {
                if (curHit != null && msgId > 0) {

                    // okay, do the big intersection
                    for (int i = 0; i < messageGrouper.length; i++) {
                        messageGrouper[i].setMsgId(msgId);
                        ZimbraHit hit = messageGrouper[i].getNextHit();
                        while (hit != null) {
                            if (!bufferedNext.contains(hit)) {
                                boolean ok = true;
                                for (int j = 0; ok && j < messageGrouper.length; j++) {
                                    if (j != i) {
                                        if (hit instanceof MessageHit) {
                                            if (!messageGrouper[j].intersectWithBuffer((MessageHit) hit)) {
                                                ok = false;
                                            }
                                        } else if (hit instanceof MessagePartHit) {
                                            if (!messageGrouper[j].intersectWithBuffer((MessagePartHit) hit)) {
                                                ok = false;
                                            }
                                        }
                                    }
                                } // intersect against every other group
                                if (ok) {
                                    bufferedNext.add(hit);
                                }
                            } // contained in mBufferedNext?
                            hit = messageGrouper[i].getNextHit();
                        } // for each hit within group
                    } // for each group
                } // assuming the first one isn't empty

                seenMsgs.add(Integer.valueOf(msgId));
                msgId = messageGrouper[0].getNextMessageId(seenMsgs);
            } while (msgId > 0);

            if (bufferedNext.size() > 0) {
                // we've got some hits -- so we can leave now...
                break TryAgain;
            }
            // no hits -- go back to the top and try again.
        } // while true (for easy retry)

        for (int i = 0; i < bufferedNext.size(); i++) {
            ZimbraHit hit = bufferedNext.get(i);
            ZimbraLog.search.debug("BUFFERED: %s", hit);
        }
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        if (noHits) {
            return null;
        } else {
            bufferNextHits();
            if (bufferedNext.size() > 0) {
                return bufferedNext.get(0);
            } else {
                return null;
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (QueryOperation op : operations) {
            op.close();
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
    private static final class HitGrouper {
        private final QueryOperation subOp;
        private final SortBy sortOrder;
        private final List<ZimbraHit> bufferedHit = new ArrayList<ZimbraHit>();
        private int curMsgId = -1;
        private ZimbraHit groupHit = null;
        private int curBufPos = 0; // for iterating the current buffer

        @Override
        public String toString() {
            StringBuffer toRet = new StringBuffer(subOp.toString()).append("\n\t");
            for (int i = 0; i < bufferedHit.size(); i++) {
                ZimbraHit hit = bufferedHit.get(i);
                toRet.append(hit.toString()).append("\n\t");
            }
            return toRet.toString();
        }

        HitGrouper(QueryOperation subOperation, SortBy sort) {
            subOp = subOperation;
            sortOrder = sort;
        }

        void resetIterator() throws ServiceException {
            bufferedHit.clear();
            subOp.resetIterator();
            curMsgId = -1;
            groupHit = null;
            curBufPos = 0;

        }

        int getNextMessageId(List<Integer> seenMsgs) throws ServiceException {
            for (int i = 1; i < bufferedHit.size(); i++) {
                Integer checkId = Integer.valueOf(bufferedHit.get(i).getItemId());
                if (!seenMsgs.contains(checkId)) {
                    return checkId.intValue();
                }
            }
            return -1;
        }

        /**
         * Advance to the next timestamp and buffer one or more hits for that timestamp.
         */
        boolean bufferNextHits() throws ServiceException {
            bufferedHit.clear();

            // step 1: establish the current stamp
            if (!subOp.hasNext()) {
                return false;
            }
            groupHit = subOp.getNext();
            setMsgId(groupHit.getItemId());
            bufferedHit.add(groupHit);

            // step 2: buffer all hits with the current stamp
            while (subOp.hasNext()) {
                ZimbraHit hit = subOp.peekNext();

                if (hit.compareTo(sortOrder, groupHit) == 0) {
                    bufferedHit.add(hit);
                    // go to next one:
                    ZimbraHit check = subOp.getNext();
                    assert (check == hit);
                } else {
                    return !bufferedHit.isEmpty();
                }
            }
            return !bufferedHit.isEmpty();
        }

        int getCurMsgId() {
            return curMsgId;
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
            return groupHit;
        }

        void setMsgId(int msgId) {
            curMsgId = msgId;
            curBufPos = 0;
        }

        ZimbraHit getNextHit() throws ServiceException {
            while (curBufPos < bufferedHit.size()) {
                if (bufferedHit.get(curBufPos).getItemId() == curMsgId) {
                    curBufPos++;
                    return bufferedHit.get(curBufPos - 1);
                }
                curBufPos++;
            }
            return null;
        }

        boolean intersectWithBuffer(MessageHit hit) throws ServiceException {
            int hitMsgId = hit.getItemId();
            for (int i = 0; i < bufferedHit.size(); i++) {
                if (bufferedHit.get(i).getItemId() == hitMsgId) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Return TRUE if passed-in Hit intersects with something within my group.
         */
        boolean intersectWithBuffer(MessagePartHit hit) throws ServiceException {
            int hitMsgId = hit.getItemId();
            for (int i = 0; i < bufferedHit.size(); i++) {
                ZimbraHit bufHit = bufferedHit.get(i);
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
         * Buffer a bunch of hits from SubOp, all hits must have a SortField EQUAL TO curHit's SortField
         */
        boolean bufferNextHits(ZimbraHit curHit) throws ServiceException {
            groupHit = curHit;
            bufferedHit.clear();

            if (!subOp.hasNext()) {
                return false;
            }

            ZimbraHit newStamp = null;
            while ((newStamp = subOp.peekNext()) != null) {
                int result = newStamp.compareTo(sortOrder, groupHit);
                if (result == 0) {
                    bufferedHit.add(newStamp);
                    // go to nex thit
                    ZimbraHit check = subOp.getNext();
                    assert (check == newStamp);
                } else if (result < 0) {
                    // newstamp is logically "Before" current...skip it
                    // mNextHit = null;
                    subOp.getNext(); // skip next hit
                } else {
                    // newstamp is after us. Current doesn't match, but don't
                    // skip newStamp,
                    // we might still get to it.
                    //mNextHit = newStamp;
                    return bufferedHit.size() > 0;
                }
            }
            return bufferedHit.size() > 0;
        }
    }

    @Override
    boolean hasSpamTrashSetting() {
        for (QueryOperation op : operations) {
            if (op.hasSpamTrashSetting()) {
                return true;
            }
        }
        return false;
    }

    @Override
    void forceHasSpamTrashSetting() {
        assert(false); // not called, but if it were, it would go:
        for (QueryOperation op : operations) {
            op.forceHasSpamTrashSetting();
        }
    }

    @Override
    Set<QueryTarget> getQueryTargets() {
        Set<QueryTarget> result = Sets.newHashSet();

        Iterator<QueryOperation> itr = operations.iterator();
        if (itr.hasNext()) {
            result.addAll(itr.next().getQueryTargets());

            // loop through rest of ops add to toRet if it is in every other set
            while (itr.hasNext()) {
                Set<QueryTarget> set = itr.next().getQueryTargets();

                // so this gets wacky:
                //  -- If both sides have an UNSPECIFIED, then the result is
                //     (RHS union LHS) including UNSPECIFIED
                //  -- If only LHS has an UNSPECIFIED, then the result is (RHS)
                //     If RHS then the result is LHS

                if (result.contains(QueryTarget.UNSPECIFIED)) {
                    if (set.contains(QueryTarget.UNSPECIFIED)) {
                        result.addAll(set);
                    } else {
                        result.clear();
                        result.addAll(set);
                    }
                } else if (!set.contains(QueryTarget.UNSPECIFIED)) {
                    result.retainAll(set);
                }
            }
        }
        return result;
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
        for (QueryOperation op : operations) {
            newList.add(op.expandLocalRemotePart(mbox));
        }
        operations = newList;
        return this;
    }

    @Override
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException {
        // just tack it on -- presumably this will be combined in the optimize() step...
        if (!hasSpamTrashSetting()) {
            // ensureSpamTrashSetting might very well return a new root node...so we need
            // to build a new mQueryOperations list using the result of ensureSpamTrashSetting
            List<QueryOperation> newList = new ArrayList<QueryOperation>();
            for (QueryOperation op : operations) {
                newList.add(op.ensureSpamTrashSetting(mbox, includeTrash, includeSpam));
            }
            operations = newList;
        }
        return this;
    }

    public void addQueryOp(QueryOperation op) {
        assert(op != null);
        operations.add(op);
    }

    private void addQueryOps(List<QueryOperation>ops) {
        operations.addAll(ops);
    }

    void pruneIncompatibleTargets(Set<QueryTarget> targets) {
        for (QueryOperation op : operations) {
            if (op instanceof UnionQueryOperation) {
                ((UnionQueryOperation) op).pruneIncompatibleTargets(targets);
            } else if (op instanceof IntersectionQueryOperation) {
                assert(false); // shouldn't be here, should have optimized already
                ((IntersectionQueryOperation) op).pruneIncompatibleTargets(targets);
            } else {
                // do nothing, must be part of the right set
                Set<QueryTarget> set = op.getQueryTargets();
                assert(set.size() == 1);
                assert(set.contains(QueryTarget.UNSPECIFIED) || set.containsAll(targets));
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
        OPTIMIZE_LOOP: while (true) {
            for (int i = 0; i < operations.size(); i++) {
                QueryOperation op = operations.get(i);
                QueryOperation optimized = op.optimize(mbox);
                if (optimized == null) {
                    operations.remove(i);
                } else if (op != optimized) {
                    operations.remove(i);
                    operations.add(optimized);
                    continue OPTIMIZE_LOOP;
                }
            }
            break;
        }

        // if all of our sub-ops optimized-away, then we're golden!
        if (operations.isEmpty()) {
            return new NoTermQueryOperation();
        }

        // Step 2: do an N^2 combine() of all of our subops
        JOIN_LOOP: while (true) {
            for (int i = 0; i < operations.size(); i++) {
                QueryOperation lhs = operations.get(i);

                // if one of our direct children is an AND, then promote all of its children to our level -- this can
                // happen if a sub-query has ANDed terms at the top level
                if (lhs instanceof IntersectionQueryOperation) {
                    combineOps(lhs, false);
                    operations.remove(i);
                    continue JOIN_LOOP;
                } else if (lhs instanceof NoTermQueryOperation) {
                    for (QueryOperation op : operations) { // other Lucene operation absorbs it
                        if (op instanceof LuceneQueryOperation) {
                            operations.remove(i);
                            continue JOIN_LOOP;
                        }
                    }
                    return new NoResultsQueryOperation(); // no other Lucene operations results in no results
                }

                for (int j = i + 1; j < operations.size(); j++) {
                    QueryOperation rhs = operations.get(j);
                    QueryOperation joined = lhs.combineOps(rhs, false);
                    if (joined != null) {
                        operations.remove(j);
                        operations.remove(i);
                        operations.add(joined);
                        continue JOIN_LOOP;
                    }
                }
            }
            break JOIN_LOOP;
        }

        // Step 2.5: now we want to eliminate any subtrees that have query targets which aren't compatible,
        // i.e. (A or B or C) and (B or C) means we eliminate A
        Set<QueryTarget> targets = getQueryTargets();
        if (targets.isEmpty()) {
            ZimbraLog.search.debug("ELIMINATING %s b/c of incompatible QueryTargets", this);
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
        for (int i = 0; i < operations.size(); i++) {
            QueryOperation lhs = operations.get(i);
            if (lhs instanceof UnionQueryOperation ||
                    lhs.getQueryTargets().size() > 1) {
                // need to distribute!
                distributeLhs = i;
                break;
            }
        }

        if (distributeLhs >= 0) {
            // if lhs has >1 explicit target at this point, it MUST be a union...
            UnionQueryOperation lhs = (UnionQueryOperation) operations.remove(distributeLhs);
            UnionQueryOperation topOp = new UnionQueryOperation();

            for (QueryOperation lhsCur : lhs.operations) {
                IntersectionQueryOperation newAnd = new IntersectionQueryOperation();
                topOp.add(newAnd);

                newAnd.addQueryOp(lhsCur);

                for (QueryOperation rhsCur : operations) {
                    newAnd.addQueryOp((QueryOperation)(rhsCur.clone()));
                }
            }

            // recurse!
            return topOp.optimize(mbox);
        }

        // at this point, we know that the entire query has one and only one QueryTarget.
        assert(QueryTarget.getExplicitTargetCount(getQueryTargets()) <= 1);

        //
        // Step 3: hacky special case for Lucene Ops and DB Ops: Lucene and DB don't
        // combine() like other operations -- if they did, then we'd run the risk of
        // failing to combine OR'ed Lucene terms (OR'ed DB terms don't combine) -- instead
        // we wait until here to combine those terms.  Weird, but functional.
        //
        // WARNING: Lucene ops ALWAYS combine, so we assume there is only one!
        LuceneQueryOperation lop = null;
        for (Iterator<QueryOperation> iter = operations.iterator(); iter.hasNext();) {
            QueryOperation op = iter.next();
            if (op instanceof LuceneQueryOperation) {
                lop = (LuceneQueryOperation) op;
                iter.remove();
                break;
            }
        }
        if (lop != null) {
            boolean foundIt = false;
            for (QueryOperation op : operations) {
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
        if (operations.size() == 1) {
            return operations.get(0);
        }

        return this;
    }

    @Override
    String toQueryString() {
        StringBuilder ret = new StringBuilder("(");

        boolean atFirst = true;

        for (QueryOperation op : operations) {
            if (!atFirst) {
                ret.append(" AND ");
            }
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

        for (QueryOperation op : operations) {
            if (atFirst) {
                atFirst = false;
            } else {
                retval.append(" AND ");
            }
            retval.append(op.toString());
        }
        retval.append(']');
        return retval.toString();
    }

    @Override
    public Object clone() {
        assert(messageGrouper == null);
        IntersectionQueryOperation result = (IntersectionQueryOperation) super.clone();
        result.bufferedNext = new ArrayList<ZimbraHit>(1);
        result.operations = new ArrayList<QueryOperation>(operations.size());
        for (QueryOperation op : operations) {
            result.operations.add((QueryOperation) op.clone());
        }
        return result;
    }

    @Override
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        if (!union && other instanceof IntersectionQueryOperation) {
            addQueryOps(((IntersectionQueryOperation) other).operations);
            return this;
        }
        return null;
    }

    @Override
    protected void begin(QueryContext ctx) throws ServiceException {
        assert(context == null);
        // scale up the chunk size since we are doing an intersection...
        context = new QueryContext(ctx.getMailbox(), ctx.getResults(), ctx.getParams(), (ctx.getChunkSize() + 1) * 3);
        messageGrouper = new HitGrouper[operations.size()];

        for (int i = 0; i < operations.size(); i++) {
            QueryOperation op = operations.get(i);
            op.begin(ctx);
            messageGrouper[i] = new HitGrouper(op, context.getResults().getSortBy());

            if (!op.hasNext()) {
                // This operation has no terms at all. Since we're an Intersection query, that means that this entire
                // query has no results. Sooo, lets release all of the operations we've already prepare()d and create
                // a single operation, a NullQueryOperation below us.
                ZimbraLog.search.debug(
                        "Dropping out of intersect query since we got to 0 results on execution %d out of %d",
                        i + 1, operations.size());

                // first, we need to be DONE with all unused query operations..
                for (int j = 0; j <= i; j++) {
                    IOUtil.closeQuietly(operations.get(j));
                }

                operations.clear();
                messageGrouper = new HitGrouper[1];

                QueryOperation nullOp = new NoResultsQueryOperation();
                addQueryOp(nullOp);
                messageGrouper[0] = new HitGrouper(nullOp, context.getResults().getSortBy());
                return;
            }
        }
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        List<QueryInfo> result = new ArrayList<QueryInfo>();
        for (QueryOperation op : operations) {
            result.addAll(op.getResultInfo());
        }
        return result;
    }

    @Override
    protected void depthFirstRecurse(RecurseCallback cb) {
        for (QueryOperation op : operations) {
            op.depthFirstRecurse(cb);
        }
        cb.recurseCallback(this);
    }

}
