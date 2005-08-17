/*
 * Created on Oct 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/*******************************************************************************
 * 
 * IntersectionQueryOperation
 * 
 *    Set of query results ANDed together
 *  
 ******************************************************************************/
class IntersectionQueryOperation extends QueryOperation {
    boolean noHits = false;
    private static Log mLog = LogFactory.getLog(IntersectionQueryOperation.class);
    
    int getOpType() {
        return OP_TYPE_INTERSECT;
    }

    /***************************************************************************
     * 
     * Hits iteration
     *  
     **************************************************************************/
    public void resetIterator() throws ServiceException {
        if (mLog.isDebugEnabled()) {
            mLog.debug("Intersection.resetIterator()");
        }
        mBufferedNext.clear(); 
        for (int i = 0; i < mMessageGrouper.length; i++) {
            mMessageGrouper[i].resetIterator();
        }
    }

    public ZimbraHit getNext() throws ServiceException {
        if (noHits || !hasNext()) {
            return null;
        }
        return (ZimbraHit) (mBufferedNext.remove(0));

    }

    ArrayList /* ZimbraHit */mBufferedNext = new ArrayList(1);

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
                
                ArrayList seenMsgs = new ArrayList();
                
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
            ZimbraHit hit = (ZimbraHit)(mBufferedNext.get(i));
            if (mLog.isDebugEnabled()) {
                mLog.debug("BUFFERED: "+hit.toString());
            }
        }
        
        }
    }

    public ZimbraHit peekNext() throws ServiceException {
        if (noHits) {
            return null;
        } else {
            bufferNextHits();
            if (mBufferedNext.size() > 0) {
                return (ZimbraHit) (mBufferedNext.get(0));
            } else {
                return null;
            }
        }
    }

    public void doneWithSearchResults() throws ServiceException {
        for (int i = 0; i < mQueryOperations.size(); i++) {
            QueryOperation op = (QueryOperation) mQueryOperations.get(i);
            op.doneWithSearchResults();
        }
    }

    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        if (noHits) {
            return null;
        }
        return super.skipToHit(hitNo);
    }

    /**
     * @author tim
     * 
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

        private int mSortOrder;

        public String toString() {
            StringBuffer toRet = new StringBuffer(mSubOp.toString()+"\n\t");
            for (int i = 0; i < mBufferedHit.size(); i++) {
                    ZimbraHit hit = (ZimbraHit) (mBufferedHit.get(i));
                    toRet.append(hit.toString()).append("\n\t");
            }
            return toRet.toString();
        }

        HitGrouper(QueryOperation subOperation, int sortOrder) {
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

        private ArrayList /* ZimbraHit */mBufferedHit = new ArrayList();

        int getNextMessageId(ArrayList seenMsgs) throws ServiceException {
            for (int i = 1; i < mBufferedHit.size(); i++) {
                Integer checkId = new Integer(
                        ((ZimbraHit) (mBufferedHit.get(i))).getItemId());
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

        //        ZimbraHit peekNext() {
        //            return mGroupHit;
        //        }

        void setMsgId(int msgId) {
            mCurMsgId = msgId;
            mCurBufPos = 0;
        }

        ZimbraHit getNextHit() throws ServiceException {
            while (mCurBufPos < mBufferedHit.size()) {
                if (((ZimbraHit) mBufferedHit.get(mCurBufPos)).getItemId() == mCurMsgId) {
                    mCurBufPos++;
                    return (ZimbraHit) mBufferedHit.get(mCurBufPos - 1);
                }
                mCurBufPos++;
            }
            return null;
        }

        boolean intersectWithBuffer(MessageHit hit) throws ServiceException {
            int hitMsgId = hit.getItemId();
            for (int i = 0; i < mBufferedHit.size(); i++) {
                if (((ZimbraHit) mBufferedHit.get(i)).getItemId() == hitMsgId) {
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
                ZimbraHit bufHit = (ZimbraHit) mBufferedHit.get(i);
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
                if (mLog.isDebugEnabled()) {
//                    System.out.println("...newstamp=\""+newStamp.toString()+" result="+result); //+"\n\t\t\tcurhit=\""+mGroupHit.toString()+"\" result="+result);
                }
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
                //                if (mSubOp.hasNext()) {
                //                    newStamp = mSubOp.getNext();
                //                } else {
                //                    newStamp = null;
                //                }
            }
            return mBufferedHit.size() > 0;
        }
    }

    /***************************************************************************
     * 
     * Internals
     *  
     **************************************************************************/

    /**
     * a SORTED (sorted at add time) list of query operations which are ANDed together
     */
    List mQueryOperations = null;

    boolean hasSpamTrashSetting() {
        boolean hasOne = false;
        for (Iterator iter = mQueryOperations.iterator(); !hasOne 
                && iter.hasNext();) {
            QueryOperation op = (QueryOperation) iter.next();
            hasOne = op.hasSpamTrashSetting();
        }
        return hasOne;
    }
    void forceHasSpamTrashSetting() {
        assert(false); // not called, but if it were, it would go:
        for (Iterator iter = mQueryOperations.iterator(); iter.hasNext();) { 
            QueryOperation op = (QueryOperation) iter.next();
            op.forceHasSpamTrashSetting();
        }
    }
    
    
    boolean hasNoResults() {
        return false;
    }
    boolean hasAllResults() {
        return false;
    }
    

    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException {
        // just tack it on -- presumably this will be combined in the optimize()
        // step...
        if (!hasSpamTrashSetting()) {
            
            boolean addedOne = false;
            
            // first, try to add it to the DB operations...Note that this is a bit of a
            // heuristic hack here: We could conceivably push the trash/spam setting
            // down onto ALL our subOps, and you could argue that would be faster...EXCEPT
            // that we know that when we do the "ensure trash/spam" pushdown, it always 
            // becomes a DB op (only the DB knows the answer).....and soooooo, since most
            // of the time when we've got a non-DBQueryOp child, there is NOT a DB operation
            // already, pushing the trash/spam setting down would actually create most work.
            for (Iterator iter = mQueryOperations.iterator(); iter.hasNext();) {
                QueryOperation op = (QueryOperation) iter.next();
                if (op instanceof DBQueryOperation) {
                    op.ensureSpamTrashSetting(mbox, includeTrash, includeSpam);
                    addedOne = true;
                }
            }
            
            // okay, we had no DB operations below us...so just push it down to everyone
            //
            // hmmm....perhaps we should only be pushing it down to one operation here?  That could
            // very well be a bit faster....
            if (!addedOne) {
                List newList = new ArrayList();
                for (Iterator iter = mQueryOperations.iterator(); iter.hasNext();) {
                    QueryOperation op = (QueryOperation) iter.next();
                    newList.add(op.ensureSpamTrashSetting(mbox, includeTrash, includeSpam));
                }
                mQueryOperations = newList;
            }
        }
        return this;
    }

    void addQueryOp(QueryOperation op) {
        assert(op!=null);
        if (mQueryOperations == null) {
            mQueryOperations = new ArrayList();
        }
        mQueryOperations.add(op);
        Collections.sort(mQueryOperations, sQueryOpSortComparator);
    }
    
    void addQueryOps(List /* QueryOperation */ ops) {
        mQueryOperations.addAll(ops);
        Collections.sort(mQueryOperations, sQueryOpSortComparator);
    }

    QueryOperation optimize(Mailbox mbox) throws ServiceException 
    {
    
        //
        // Step 1: optimize each individual sub-operation we have
        //
        restartSubOpt: do {
            for (Iterator iter = mQueryOperations.iterator(); iter.hasNext();) {
                QueryOperation q = (QueryOperation) iter.next();
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
                QueryOperation lhs = (QueryOperation) mQueryOperations.get(i);

                // if one of our direct children is an and, then promote all of
                // its elements to our level -- this can happen if a subquery has
                // ANDed terms at the top level
                if (lhs instanceof IntersectionQueryOperation) {
                    combineOps(lhs, false);
                    mQueryOperations.remove(i);
                    continue outer;
                }

                for (int j = i + 1; j < mQueryOperations.size(); j++) {
                    QueryOperation rhs = (QueryOperation) mQueryOperations
                            .get(j);
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
        // Step 3: hacky special case for Lucene Ops and DB Ops: Lucene and DB don't 
        // combine() like other operations -- if they did, then we'd run the risk of
        // failing to combine OR'ed Lucene terms (OR'ed DB terms don't combine) -- instead
        // we wait until here to combine those terms.  Weird, but functional.  
        //
        // WARNING: Lucene ops ALWAYS combine, so we assume there is only one!
        {
            LuceneQueryOperation lop = null;
            for (Iterator iter = mQueryOperations.iterator(); iter.hasNext();) {
                QueryOperation op = (QueryOperation) iter.next();
                if (op.getOpType() == OP_TYPE_LUCENE) {
                    lop = (LuceneQueryOperation)op;
                    iter.remove();
                    break;
                }
            }
            if (lop != null) {
                boolean foundIt = false;
                for (Iterator iter = mQueryOperations.iterator(); iter.hasNext();) {
                    QueryOperation op = (QueryOperation) iter.next();
                    if (op.getOpType() == OP_TYPE_DB) {
                        ((DBQueryOperation)op).addLuceneOp(lop);
                        foundIt = true;
                    }
                }
                if (!foundIt) {
                    // add the lucene op back in! 
                    addQueryOp(lop);
                }
                
            }
        }

        // now - check to see if we have only one child -- if so, then WE can be
        // eliminated, so push the child up
        if (mQueryOperations.size() == 1) {
            return (QueryOperation) mQueryOperations.get(0);
        }

        return this;
    }

    public String toString() {
        StringBuffer retval = new StringBuffer("(");

        for (Iterator iter = mQueryOperations.iterator(); iter.hasNext();) {
            QueryOperation op = (QueryOperation) iter.next();
            retval.append(" AND ");
            retval.append(op.toString());
        }
        retval.append(")");
        return retval.toString();
    }

    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        if (!union && other instanceof IntersectionQueryOperation) {
            addQueryOps(((IntersectionQueryOperation) other).mQueryOperations);
            return this;
        }
        return null;
    }

    private HitGrouper mMessageGrouper[] = null;

    protected void prepare(Mailbox mbx, ZimbraQueryResultsImpl res,
            MailboxIndex mbidx) throws ServiceException, IOException 
    {
        
        mMessageGrouper = new HitGrouper[mQueryOperations.size()];
        this.setupResults(mbx, res);
        
        for (int i = 0; i < mQueryOperations.size(); i++) {
            QueryOperation op = (QueryOperation) mQueryOperations.get(i);
            op.prepare(mbx, res, mbidx);
            mMessageGrouper[i] = new HitGrouper(op, res.getSearchOrder());

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
                    ((QueryOperation) mQueryOperations.get(j))
                            .doneWithSearchResults();
                }

                mQueryOperations.clear();
                mMessageGrouper = new HitGrouper[1];

                QueryOperation nullOp = new NullQueryOperation();
                addQueryOp(nullOp);
                mMessageGrouper[0] = new HitGrouper(nullOp, res
                        .getSearchOrder());
                return;
            }
        }
    }

    protected int inheritedGetExecutionCost() {
        int retVal = 15;
        for (Iterator iter = mQueryOperations.iterator(); iter.hasNext();) {
            QueryOperation op = (QueryOperation) iter.next();
            retVal += op.getExecutionCost();
        }
        return retVal;
    }

}