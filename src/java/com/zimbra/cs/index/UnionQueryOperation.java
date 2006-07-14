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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
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
import java.util.Set;

import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.SetUtil;
import com.zimbra.soap.SoapProtocol;

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

    QueryTargetSet getQueryTargets() {
        QueryTargetSet toRet = new QueryTargetSet();

        for (QueryOperation op : mQueryOperations) {
            toRet = (QueryTargetSet)SetUtil.union(toRet, op.getQueryTargets());
        }
        return toRet;
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
        ArrayList<QueryOperation> newList = new ArrayList<QueryOperation>();

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
                QueryTargetSet opTargets = op.getQueryTargets();
                assert(opTargets.size() == 1);
                if (!opTargets.isSubset(targets)) {
                    mQueryOperations.remove(i);
                }
            }
        }
    }
    
    QueryOperation runRemoteSearches(SoapProtocol proto, Mailbox mbox, OperationContext octxt, MailboxIndex mbidx, byte[] types, SortBy searchOrder, int offset, int limit) 
    throws ServiceException, IOException {

        boolean hasRemoteOps = false;
        
        Set<Folder> visibleFolders = mbox.getVisibleFolders(octxt);
        

        // Since optimize() has already been run, we know that each of our ops
        // only has one target (or none).  Find those operations which have
        // an external target and wrap them in RemoteQueryOperations
        for (int i = mQueryOperations.size()-1; i >= 0; i--) { // iterate backwards so we can remove/add w/o screwing iteration
            QueryOperation op = mQueryOperations.get(i);

            QueryTargetSet targets = op.getQueryTargets();
            
            // this assertion OK because we have already distributed multi-target query ops
            // during the optimize() step
            assert(targets.countExplicitTargets() <= 1);
            
            // the assertion above is critical: the code below all assumes
            // that we only have ONE target (ie we've already distributed if necessary)

            if (targets.hasExternalTargets()) {
                mQueryOperations.remove(i);   

                hasRemoteOps = true;
                boolean foundOne = false;

                // find a remoteOp to add this one to
                for (QueryOperation tryIt : mQueryOperations) {
                    if (tryIt instanceof RemoteQueryOperation) {
                        if (((RemoteQueryOperation)tryIt).tryAddOredOperation(op)) {
                            foundOne = true;
                            break;
                        }
                    }
                }

                if (!foundOne) {
                    RemoteQueryOperation remoteOp = new RemoteQueryOperation();
                    remoteOp.tryAddOredOperation(op);
                    mQueryOperations.add(i, remoteOp);
                }
            } else {
                // local target
                if (visibleFolders != null) {
                    if (visibleFolders.size() == 0) {
                        mQueryOperations.remove(i);
                        mQueryOperations.add(i, new NullQueryOperation());
                    } else {
                        mQueryOperations.remove(i);
                        
                        // build a "and (in:visible1 or in:visible2 or in:visible3...)" query tree here!
                        IntersectionQueryOperation intersect = new IntersectionQueryOperation();
                        intersect.addQueryOp(op);
                        
                        UnionQueryOperation union = new UnionQueryOperation();
                        intersect.addQueryOp(union);
                        
                        for (Folder f : visibleFolders) {
                            DBQueryOperation newOp = DBQueryOperation.Create();
                            union.add(newOp);
                            newOp.addInClause(f, true);
                        }
                        
                        mQueryOperations.add(i, intersect);
                    }
                }
                
                
                
            }
        }
        
        if (hasRemoteOps) {
            // okay, if we actually have remote operations, then we need to call setup() once per --
            // this actually runs the remote operation
            for (QueryOperation toSetup : mQueryOperations) {
                if (toSetup instanceof RemoteQueryOperation) {
                    ((RemoteQueryOperation)toSetup).setup(proto, octxt.getAuthenticatedUser(), types, searchOrder, offset, limit);
                }
            }
        }
        
        assert(mQueryOperations.size() > 0);
        
        if (mQueryOperations.size() == 1) {
            // this can happen if we replaced ALL of our operations with a single remote op...
            return mQueryOperations.get(0);
        }
        
        return this;
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

    public String toString() {
        StringBuilder retval = new StringBuilder("(");

        for (int i = 0; i < mQueryOperations.size(); i++) {
            retval.append(" OR ");
            retval.append(mQueryOperations.get(i).toString());
        }
        retval.append(")");
        return retval.toString();
    }

    public Object clone() {
        UnionQueryOperation toRet = null;
        toRet = (UnionQueryOperation)super.clone();

        assert(mCachedNextHit == null);

        toRet.mQueryOperations = new ArrayList<QueryOperation>(mQueryOperations.size());
        for (QueryOperation q : mQueryOperations)
            toRet.mQueryOperations.add(q);

        return toRet;
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

}