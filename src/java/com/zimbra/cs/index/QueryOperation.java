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
 * Created on Oct 15, 2004
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.util.Comparator;

import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;


/************************************************************************
 * 
 * QueryOperation
 *
 * A QueryOperation is a part of a Search request -- there are potentially mutliple query operations
 * in a search.  QueryOperations return ZimbraQueryResultsImpl sets -- which can be iterated
 * over to get ZimbraHit objects.  
 * 
 * The difference between a QueryOperation and a simple ZimbraQueryResultsImpl set is that 
 * a QueryOperation knows how to Optimize and Execute itself -- whereas a QueryResults set is 
 * just a set of results and can only be iterated.  
 * 
 ***********************************************************************/
abstract class QueryOperation implements ZimbraQueryResults
{
    /**
     * Bitfield of QueryOperation types.  ORDER DOES MATTER, queries
     * are executed in order from lowest ID to highest: this is because
     * there is a preferred-order between queryOps (DB can be more efficient
     * if it has the Lucene results, etc)
     */
    public static final int OP_TYPE_NULL        = 1; // no results at all
    public static final int OP_TYPE_LUCENE      = 2;
    public static final int OP_TYPE_DB          = 3;
    public static final int OP_TYPE_SUBTRACT    = 4;
    public static final int OP_TYPE_INTERSECT   = 5; // AND
    public static final int OP_TYPE_UNION       = 6; // OR
    public static final int OP_TYPE_NO_TERM     = 7; // pseudo-op, always optimized away

    abstract int getOpType();
    
    protected static class QueryOpSortComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            QueryOperation lhs = (QueryOperation)o1;
            QueryOperation rhs = (QueryOperation)o2;
            
            return lhs.getOpType() - rhs.getOpType();
        }
        public boolean equals(Object obj) {
            if (obj instanceof QueryOpSortComparator) return true;
            return false;
        }
    }
    
    final static QueryOpSortComparator sQueryOpSortComparator = new QueryOpSortComparator();

    private final static boolean USE_PRELOADING_GROUPER = true; // mostly useful for debugging
    
    private SortBy mSortOrder = null;
    public SortBy getSortBy() { return mSortOrder; }
    
    ////////////////////
    // Top-Level Execution  
    final ZimbraQueryResults run(Mailbox mbox, MailboxIndex mbidx, byte[] types, SortBy searchOrder, int chunkSize) throws IOException, ServiceException
    {
        mIsToplevelQueryOp = true;
        mSortOrder = searchOrder;
        
        chunkSize++; // one extra for checking the "more" flag at the end of the results
        
        if (chunkSize < 26) {
            chunkSize = 26;
        } else if (chunkSize > 5000) {
            chunkSize = 5000;
        }
        
        int retType = MailboxIndex.SEARCH_RETURN_DOCUMENTS;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == MailItem.TYPE_CONVERSATION) {
                retType = MailboxIndex.SEARCH_RETURN_CONVERSATIONS;
                break;
            }
            if (types[i] == MailItem.TYPE_MESSAGE) {
                retType = MailboxIndex.SEARCH_RETURN_MESSAGES;
                break;
            }
        }
        
        // set me to TRUE if you're returning Conversations or something which could benefit from preloading        
        boolean preloadOuterResults = false;
        
        int outerChunkSize = chunkSize;
        
        switch (retType) {
        case MailboxIndex.SEARCH_RETURN_CONVERSATIONS:
            if (USE_PRELOADING_GROUPER) {
                chunkSize+= 2; // one for the ConvQueryResults, one for the Grouper 
                setupResults(mbox, new ConvQueryResults(new ItemPreloadingGrouper(this, chunkSize, mbox), types, searchOrder));
                chunkSize*=2; // guess 2 msgs per conv
            } else {
                chunkSize++; // one for the ConvQueryResults
                setupResults(mbox, new ConvQueryResults(this, types, searchOrder));
                chunkSize*=2;
            }
            preloadOuterResults = true;
            break;
        case MailboxIndex.SEARCH_RETURN_MESSAGES:
            if (USE_PRELOADING_GROUPER) {
                chunkSize+= 2; // one for the MsgQueryResults, one for the Grouper 
                setupResults(mbox, new MsgQueryResults(new ItemPreloadingGrouper(this, chunkSize, mbox), types, searchOrder));
            } else {
                chunkSize++; // one for the MsgQueryResults
                setupResults(mbox, new MsgQueryResults(this, types, searchOrder));
            }
            break;
        case MailboxIndex.SEARCH_RETURN_DOCUMENTS:
            if (USE_PRELOADING_GROUPER) {
                chunkSize++; // one for the grouper
                setupResults(mbox, new UngroupedQueryResults(new ItemPreloadingGrouper(this, chunkSize, mbox), types, searchOrder));
            } else {
                setupResults(mbox, new UngroupedQueryResults(this, types, searchOrder));
            }
            break;
        }
        
        prepare(mMailbox, mResults, mbidx, chunkSize);
        
        if (USE_PRELOADING_GROUPER && preloadOuterResults) {
            return new ItemPreloadingGrouper(mResults, outerChunkSize, mbox);
        } else {
            return mResults;
        }
    }
    
    
    /******************
     * 
     * Hits iteration
     *
     *******************/
    public boolean hasNext() throws ServiceException
    {
        return peekNext() != null;
    }
    
    /**
     * 
     * prepare() is the API which begins query execution.  It is allowed to grab and hold resources, which are then
     * released via doneWithSearchResults().
     * 
     * 
     * IMPORTANT IMPORTANT: prepare() and doneWithSearchResults must always be called in a pair.  That is, 
     * if you call prepare, you MUST call doneWithSearchResults.
     * 
     * @param mbx
     * @param res
     * @param mbidx
     * @param chunkSize A hint to the query operation telling it what size to chunk data in.  Higher numbers
     *                   can be more efficient if you are using a lot of results, but have more overhead 
     * @throws IOException
     * @throws ServiceException
     */
    protected abstract void prepare(Mailbox mbx, ZimbraQueryResultsImpl res, MailboxIndex mbidx, int chunkSize) throws IOException, ServiceException;

	public ZimbraHit getFirstHit() throws ServiceException {
		resetIterator();
		return getNext();
	}
	
    public ZimbraHit skipToHit(int hitNo) throws ServiceException {
        resetIterator();
        for (int i = 0; i < hitNo; i++) {
            if (!hasNext()) {
                return null;
            }
            getNext();
        }
        return getNext();
    }
    
    /******************
     * 
     * Internals
     *
     *******************/
    
    private boolean mIsToplevelQueryOp = false;
    protected boolean isTopLevelQueryOp() { return mIsToplevelQueryOp; }
    
    private ZimbraQueryResultsImpl mResults;
    private Mailbox mMailbox;
    final protected Mailbox getMailbox() { return mMailbox; }
    final protected ZimbraQueryResultsImpl getResultsSet() { return mResults; }
    final protected void setupResults(Mailbox mbx, ZimbraQueryResultsImpl res) {
        mMailbox = mbx;
        mResults = res;
    }
    
    private int mExecutionCost = -1;
    
    
    ////////////////////
    // Execution Cost
    final protected int getExecutionCost() {
        if (mExecutionCost == -1) {
            mExecutionCost = inheritedGetExecutionCost();
        }
        return mExecutionCost;
    }
    
    /**
     * We use this code to recursively descend the operation tree and set the "-in:junk -in:trash"
     * setting as necessary.  Descend down the tree, when you hit an AND, then only one of the 
     * subtrees must have it, when you hit an OR, then every subtree must have a setting, or else one must 
     * be added. 
     * @param includeTrash TODO
     * @param includeSpam TODO
     *
     */ 
    abstract QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException;
    
    /** 
     * @return TRUE if this subtree has a trash/spam paramater, FALSE if one is needed.
     */
    abstract boolean hasSpamTrashSetting();
    
    /**
     * A bit of a hack -- when we combine a "all items including trash/spam" term with another query, this
     * API lets us force the "include trash/spam" part in the other query and thereby drop the 1st one. 
     */
    abstract void forceHasSpamTrashSetting();
    
    
    
    abstract boolean hasNoResults();
    abstract boolean hasAllResults();
    
    /**
     * @param mbox
     * @return An Optimzed version of this query, or NULL if this query "should have no effect".  
     * If NULL is returned, it is up to the caller to decide what to do (usually replace with a 
     * NullQueryOperation -- however sometimes like within an Intersection you might just want 
     * to remove the term)
     * 
     * @throws ServiceException
     */
    abstract QueryOperation optimize(Mailbox mbox) throws ServiceException;

    /**
     * Called when optimize()ing a UNION or INTERSECTION -- see if two Ops can be expressed as one (e.g.
     * is:unread and in:inbox can both be expressed via one DBQueryOperation)
     * 
     * @param other
     * @param union
     * @return the new operation that handles both us and the other constraint, or NULL if the ops could
     * not be combined.
     */
    protected abstract QueryOperation combineOps(QueryOperation other, boolean union);
    
    protected abstract int inheritedGetExecutionCost();

    
}