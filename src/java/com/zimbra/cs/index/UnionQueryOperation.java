/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



/************************************************************************
 * 
 * UnionQueryOperation
 *
 *
 * -- a list of query operations which are unioned together.
 * 
 ***********************************************************************/
class UnionQueryOperation extends QueryOperation
{
    
    private static Log mLog = LogFactory.getLog(UnionQueryOperation.class);
    
    private boolean atStart = true; // don't re-fill buffer twice if they call hasNext() then reset() w/o actually getting next
    
    int getOpType() {
        return OP_TYPE_UNION;
    }
    
    QueryTarget getQueryTarget() {
    	return null;
    }
    
    
    /******************
     * 
     * Hits iteration
     *
     *******************/
    public void resetIterator() throws ServiceException {
        if (!atStart) {
            for (Iterator iter = mQueryOperations.iterator(); iter.hasNext(); )
            {
                QueryOperation q = (QueryOperation)iter.next();
                q.resetIterator();
            }
            mCachedNextHit = null;
            internalGetNext();
        }
    }
    
    private ZimbraHit mCachedNextHit = null;
    
    public ZimbraHit getNext() throws ServiceException {
        atStart = false;
        ZimbraHit toRet = mCachedNextHit;
        if (mCachedNextHit != null) { // this "if" is here so we don't keep calling internalGetNext when we've reached the end of the results...
            mCachedNextHit = null;
            internalGetNext();
        }
        
        return toRet;
    }
    
    public ZimbraHit peekNext() throws ServiceException
    {
        return mCachedNextHit;
    }
    
    private void internalGetNext() throws ServiceException
    {
        if (mCachedNextHit == null) {
            int i = 0;
            
            // loop through QueryOperations and find the "best" hit
            int currentBestHitOffset = -1;
            ZimbraHit currentBestHit = null;
            for (i = 0; i < mQueryOperations.size(); i++) {
                QueryOperation op = (QueryOperation)(mQueryOperations.get(i));
                if (op.hasNext()) {
                    if (currentBestHitOffset == -1) {
                        currentBestHitOffset = i;
                        currentBestHit = op.peekNext();
                    } else {
                        ZimbraHit opNext = op.peekNext();
                        int result = opNext.compareBySortField(getResultsSet().getSortBy(), currentBestHit);
                        if (result < 0) {
                            // "before"
                            currentBestHitOffset = i;
                            currentBestHit = opNext;
                        }
                    }
                }
            }
            if (currentBestHitOffset > -1) {
                mCachedNextHit = ((QueryOperation)(mQueryOperations.get(currentBestHitOffset))).getNext();
                assert(mCachedNextHit == currentBestHit);
            }
        }
    }
    
    
    public void doneWithSearchResults() throws ServiceException {
        for (Iterator iter = mQueryOperations.iterator(); iter.hasNext(); )
        {
            QueryOperation q = (QueryOperation)iter.next();
            q.doneWithSearchResults();
        }
    }
    
    
    ArrayList<QueryOperation>mQueryOperations = new ArrayList<QueryOperation>();
    
    public boolean hasSpamTrashSetting() {
        boolean hasAll = true;
        for (Iterator iter = mQueryOperations.iterator(); hasAll && iter.hasNext(); )
        {
            QueryOperation op = (QueryOperation)iter.next();
            hasAll = op.hasSpamTrashSetting();
        }
        return hasAll;
    }
    
    void forceHasSpamTrashSetting() {
        assert(false); // not called, but if it were it would go:
        for (Iterator iter = mQueryOperations.iterator(); iter.hasNext(); )
        {
            QueryOperation op = (QueryOperation)iter.next();
            op.forceHasSpamTrashSetting();
        }
    }
    
    QueryTarget getQueryTarget(QueryTarget targetOfParent) {
    	return targetOfParent;
    }
    
    
    boolean hasNoResults() {
        return false;
    }
    boolean hasAllResults() {
        return false;
    }
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException
    {
        ArrayList /* QueryOperation */ newList = new ArrayList();
        
        for (Iterator iter = mQueryOperations.iterator(); iter.hasNext(); )
        {
            QueryOperation op = (QueryOperation)iter.next();
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
    
    public QueryOperation optimize(Mailbox mbox) throws ServiceException {
        restartSubOpt:
            do {
                for (Iterator iter = mQueryOperations.iterator(); iter.hasNext(); )
                {
                    QueryOperation q = (QueryOperation)iter.next();
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
        
        outer:
        do {
            for (int i = 0; i < mQueryOperations.size(); i++) {
                QueryOperation lhs = (QueryOperation)mQueryOperations.get(i);
                
                // if one of our direct children is an OR, then promote all of its
                // elements to our level -- this can happen if a subquery has
                // ORed terms at the top level
                if (lhs instanceof UnionQueryOperation) {
                    combineOps(lhs, true);
                    mQueryOperations.remove(i);
                    continue outer;
                }
                
                for (int j = i+1; j < mQueryOperations.size(); j++) {
                    QueryOperation rhs = (QueryOperation)mQueryOperations.get(j);
                    QueryOperation joined = lhs.combineOps(rhs,true);
                    if (joined != null) {
                        mQueryOperations.remove(j);
                        mQueryOperations.remove(i);
                        mQueryOperations.add(joined);
                        continue outer;
                    }
                    
                    // order the operations by execution cost -- cheapest first.  no
                    // real point here - TODO remove this probably
                    if (rhs.getExecutionCost() < lhs.getExecutionCost()) {
                        // move i to the end...
                        // note that this effectively makes the big loop a cubic (N^3) operation.
                        // It probably doesn't matter, but TODO maybe fixme
                        mQueryOperations.remove(i);
                        mQueryOperations.add(lhs);
                        continue outer;
                    }
                }
            }
            break;
        } while(true);
    
    
    // now - check to see if we have only one child -- if so, then WE can be
    // eliminated, so push the child up
    if (mQueryOperations.size() == 1) {
        return (QueryOperation)mQueryOperations.get(0);
    }
    
    return this;
    }
    
    public String toString() {
        StringBuffer retval = new StringBuffer("(");
        
        for (int i = 0; i < mQueryOperations.size(); i++) {
            retval.append(" OR ");
            retval.append(mQueryOperations.get(i).toString());
        }
        retval.append(")");
        return retval.toString();
    }
    
    
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        if (mLog.isDebugEnabled()) {
            mLog.debug("combineOps("+toString()+","+other.toString()+")");
        }
        if (union && other instanceof UnionQueryOperation) {
            mQueryOperations.addAll(((UnionQueryOperation)other).mQueryOperations);
            return this;
        }
        return null;
    }


    protected void prepare(Mailbox mbx, ZimbraQueryResultsImpl res, MailboxIndex mbidx, int chunkSize) throws ServiceException, IOException
    {
        this.setupResults(mbx, res);
        
        for (int i = 0; i < mQueryOperations.size(); i++) {
            QueryOperation qop = (QueryOperation)mQueryOperations.get(i);
            if (mLog.isDebugEnabled()) {
                mLog.debug("Executing: "+qop.toString());
            }
            qop.prepare(mbx, res, mbidx, chunkSize+1); // add 1 to chunksize b/c we buffer
        }
        
        internalGetNext();
    }
    
    protected int inheritedGetExecutionCost()
    {
        int retVal = 10;
        for (int i = 0; i < mQueryOperations.size(); i++) {
            retVal += ((QueryOperation)mQueryOperations.get(i)).getExecutionCost();
        }
        return retVal;
    }
}