/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * A QueryOperation that filters results out of the result set
 * 
 * Currently used only as a base class for other QueryOps that have to do passthrough/filtering
 */
public abstract class FilterQueryOperation extends QueryOperation {
    
    protected QueryOperation mOp = null;
    
    @Override
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        return null;
    }

    @Override
    protected void depthFirstRecurse(RecurseCallback cb) {
        mOp.depthFirstRecurse(cb);
        cb.recurseCallback(this);
    }

    @Override
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam)
        throws ServiceException {
        return mOp.ensureSpamTrashSetting(mbox, includeTrash, includeSpam);
    }

    @Override
    void forceHasSpamTrashSetting() {
        mOp.forceHasSpamTrashSetting();
    }

    @Override
    QueryTargetSet getQueryTargets() {
        return mOp.getQueryTargets();
    }

    @Override
    boolean hasAllResults() {
        return mOp.hasAllResults();
    }

    @Override
    boolean hasNoResults() {
        return mOp.hasNoResults();
    }

    @Override
    boolean hasSpamTrashSetting() {
        return mOp.hasSpamTrashSetting();
    }

    @Override
    QueryOperation optimize(Mailbox mbox) throws ServiceException {
        // optimize our sub-op, but *don't* optimize us out
        mOp = mOp.optimize(mbox);
        return this;
    }

    @Override
    protected void prepare(Mailbox mbx, ZimbraQueryResultsImpl res, MailboxIndex mbidx, SearchParams params,
        int chunkSize) throws IOException, ServiceException {
        mParams = params;
        mOp.prepare(mbx, res, mbidx, params, chunkSize);
    }

    @Override
    String toQueryString() {
        return mOp.toQueryString();
    }

    public void doneWithSearchResults() throws ServiceException {
        mOp.doneWithSearchResults();
    }

    public int estimateResultSize() throws ServiceException {
        return mOp.estimateResultSize();
    }

    public ZimbraHit getNext() throws ServiceException {
        ZimbraHit toRet = peekNext();
        if (toRet != null)
            mOp.getNext(); // skip the current hit
        return toRet;
    }

    public List<QueryInfo> getResultInfo() {
        return mOp.getResultInfo(); 
    }
    
    public ZimbraHit peekNext() throws ServiceException {
        return mOp.peekNext();
    }

    public void resetIterator() throws ServiceException {
        mOp.resetIterator();
    }
}
