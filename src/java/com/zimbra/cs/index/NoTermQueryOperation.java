/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author tim
 * 
 * A QueryOperation which is generated when a query term evaluates to "nothing".  
 * 
 * This is not the same as a NullQueryOperation because:
 *     RESULTS(Op AND NoTermQuery) = RESULTS(Op)
 *     
 * It is also not the same as an AllQueryOperation because:
 *     RESULTS(NoTemQuery) = NONE
 *
 * Basically, this pseudo-Operation is here to handle the situation when a Lucene term 
 * evaluates to the empty string -- by generating a special-purpose Pseudo-Operation for 
 * this case we can hand-tune the Optimizer behavior and make it do the right thing in all 
 * cases.
 *
 */
public class NoTermQueryOperation extends QueryOperation {

    public NoTermQueryOperation() {
        super();
    }
    
    protected void prepare(Mailbox mbx, ZimbraQueryResultsImpl res,
            MailboxIndex mbidx, SearchParams params, int chunkSize) throws IOException, ServiceException {
        mParams = params;
    }

    @Override
    QueryOperation expandLocalRemotePart(Mailbox mbox) throws ServiceException {
        return this;
    }
    
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash,
            boolean includeSpam) throws ServiceException {
        return this;
    }

    boolean hasSpamTrashSetting() {
        return false;
    }

    void forceHasSpamTrashSetting() {
        assert(false);
    }
    
    String toQueryString() {
    	return "";
    }
    
    public String toString() {
    	return "NO_TERM_QUERY_OP";
    }

    QueryTargetSet getQueryTargets() {
    	QueryTargetSet toRet = new QueryTargetSet(1);
    	toRet.add(QueryTarget.UNSPECIFIED);
    	return toRet;
    }
    
    boolean hasNoResults() {
        // TODO Auto-generated method stub
        return false;
    }

    boolean hasAllResults() {
        // TODO Auto-generated method stub
        return false;
    }

    QueryOperation optimize(Mailbox mbox) throws ServiceException {
        return null;
    }

    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        return other;
    }

    protected int inheritedGetExecutionCost() {
        return 10000;
    }

    public void resetIterator() throws ServiceException {
        }

    public ZimbraHit getNext() throws ServiceException {
        return null;
    }

    public ZimbraHit peekNext() throws ServiceException {
        return null;
    }

    public void doneWithSearchResults() throws ServiceException {
    }

    public List<QueryInfo> getResultInfo() { return new ArrayList<QueryInfo>(); }
    
    public int estimateResultSize() throws ServiceException { return 0; }
    
    protected void depthFirstRecurse(RecurseCallback cb) {}
    

}
