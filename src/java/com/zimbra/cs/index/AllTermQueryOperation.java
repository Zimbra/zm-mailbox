/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 VMware, Inc.
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

import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * This is simply a wildcard operation includes all lucene terms
 * @author smukhopadhyay
 *
 */
public class AllTermQueryOperation extends QueryOperation {

    @Override
    protected void begin(QueryContext ctx) throws ServiceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void depthFirstRecurse(RecurseCallback cb) {
        // TODO Auto-generated method stub
        
    }

    @Override
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash,
            boolean includeSpam) throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    QueryOperation expandLocalRemotePart(Mailbox mbox) throws ServiceException {
        return this;
    }

    @Override
    void forceHasSpamTrashSetting() {
        // TODO Auto-generated method stub
        
    }

    @Override
    QueryTargetSet getQueryTargets() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    boolean hasAllResults() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    boolean hasNoResults() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    boolean hasSpamTrashSetting() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    QueryOperation optimize(Mailbox mbox) throws ServiceException {
        return this;
    }

    @Override
    String toQueryString() {
        return "";
    }
    
    @Override
    public String toString() {
        return "ALL_TERM_QUERY_OP";
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public int estimateResultSize() throws ServiceException {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void resetIterator() throws ServiceException {
        // TODO Auto-generated method stub
        
    }

}
