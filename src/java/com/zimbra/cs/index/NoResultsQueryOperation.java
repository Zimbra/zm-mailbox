/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Nov 12, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * A query operation which returns no elements at all.
 * 
 * This is generated, for example, when we determine that part of the query can 
 * never return any results, eg: (A and not A)
 */
class NoResultsQueryOperation extends QueryOperation {

    public NoResultsQueryOperation() { }
    
    @Override
    QueryOperation expandLocalRemotePart(Mailbox mbox) throws ServiceException {
        return this;
    }
    
    @Override
    public QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException
    {
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
    QueryTargetSet getQueryTargets() {
    	return new QueryTargetSet();
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
    
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#resetIterator()
     */
    public void resetIterator() throws ServiceException {
        //empty 
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#getNext()
     */
    public ZimbraHit getNext() throws ServiceException {
        return null;
    }

    public ZimbraHit peekNext() throws ServiceException
    {
        return null;
    }

    @Override
    protected void prepare(Mailbox mbx, ZimbraQueryResultsImpl res, MailboxIndex mbidx, SearchParams params, int chunkSize)
            throws IOException, ServiceException {
        mParams = params;
        // empty
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#doneWithSearchResults()
     */
    public void doneWithSearchResults() throws ServiceException {
        //empty 
    }

    @Override
    public QueryOperation optimize(Mailbox mbox) throws ServiceException {
        return this;
    }

    @Override
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        return null;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#inheritedGetExecutionCost()
     */
    protected int inheritedGetExecutionCost() {
        return 0;
    }
    
    public List<QueryInfo> getResultInfo() { 
        return new ArrayList<QueryInfo>(); 
    }
    
    public int estimateResultSize() throws ServiceException { 
        return 0; 
    }
    
    @Override
    protected void depthFirstRecurse(RecurseCallback cb) {
        //empty 
    }
}
