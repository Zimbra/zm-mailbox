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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * 
 */
abstract class TextQueryOperation extends QueryOperation {
    
    //////////////////////////////////////////////////////////////
    //
    // Query manipulation:
    //
    /**
     * Add the specified text clause at the toplevel 
     *   (e.g. going in "a b c" if we addClause("d") we get "a b c d"
     *   
     *   @param queryStr - Appended to the end of the text-representation of this query
     *   @param Query - Lucene Query term
     *   @param truth - allows for negated query terms 
     */
    abstract void addClause(String queryStr, Query q, boolean truth);
    
    /**
     * Add the specified text clause ANDED with the existing query
     *   (e.g. going in w/ "a b c" if we addAndedClause("d") we get "(a b c) AND d"
     * 
     * This API may only be called AFTER query optimizing and AFTER remote queries have been
     * split.  
     * 
     * This API is used by the query executor so that it can temporarily add a bunch of 
     * indexIds to the existing query -- this is necessary when we are doing a DB-first query
     * plan execution.
     * 
     * Note that this API does *not* update the text-representation of this query
     * 
     * @param q
     * @param truth
     */
    abstract void addAndedClause(Query q, boolean truth);
    
    /**
     * Set the text query *representation* manually -- the thing that is output if we have
     * to proxy this search somewhere else -- used when dealing with wildcard searches.  
     * @param queryStr
     */
    abstract void setQueryString(String queryStr);
    
    /**
     * Used by a wrapping DBQueryOperation, when it is running a DB-First plan
     * @return The current query
     */
    abstract protected BooleanQuery getCurrentQuery();
    
    /**
     * Re-set our query back to a previous state.  This may only be called AFTER query optimization
     * and remote query splitting has happened.  This is used when we are in a DB-first query 
     * plan so that we can restore the original query after we've temporarily hacked it up with
     * a list of IndexIDs from the DB-first part.
     * 
     * @param q
     */
    abstract protected void resetQuery(BooleanQuery q);


    
    //////////////////////////////////////////////////////////////
    //
    //
    //
    /**
     * @return TRUE if we think this query is best evaluated DB-FIRST
     */
    abstract protected boolean shouldExecuteDbFirst();

    

    //////////////////////////////////////////////////////////////
    //
    // Hits fetching/counting
    //
    /**
     * Reset our hit iterator back to the beginning of the result set.  
     */
    abstract protected void resetDocNum();
    
    /**
     * Fetch the next chunk of results from the Text Index
     * 
     * @param maxChunkSize
     * @return
     * @throws ServiceException
     */
    abstract protected TextResultsChunk getNextResultsChunk(int maxChunkSize) throws ServiceException;

    /**
     * @return number of hits in this search
     */
    abstract int countHits();


    
    //////////////////////////////////////////////////////////////
    //
    // Cloning
    //
    /**
     * Just clone *this* object, don't clone the embedded DBOp
     * @return
     * @throws CloneNotSupportedException
     */
    abstract protected LuceneQueryOperation cloneInternal() throws CloneNotSupportedException;
    
    /**
     * Called from DBQueryOperation.clone()
     * 
     * @param caller - our DBQueryOperation which has ALREADY BEEN CLONED
     * @return
     * @throws CloneNotSupportedException
     */
    abstract protected Object clone(DBQueryOperation caller) throws CloneNotSupportedException;
    
    

    /**
     * because we don't store the real mail-item-id of documents, we ALWAYS need a DBOp 
     * in order to properly get our results.
     */
    protected DBQueryOperation mDBOp = null;
    
    protected List<QueryInfo> mQueryInfo = new ArrayList<QueryInfo>();
    
    private boolean mHasSpamTrashSetting = false;
    
    void forceHasSpamTrashSetting() { mHasSpamTrashSetting = true; }
    
    List<QueryInfo> getQueryInfo() { return mQueryInfo; }
    
    /**
     * Allows the parser (specifically the BaseQuery subclasses) to store some query result 
     * information so that it can be returned to the caller after the query has run.  This is
     * used for things like spelling suggestion correction, or wildcard expansion info: 
     * things that are not results per-se but still need to have some way to be sent back to 
     * the caller
     * 
     * @param inf
     */
    void addQueryInfo(QueryInfo inf) {
        mQueryInfo.add(inf);
    }
    
    boolean hasSpamTrashSetting() { return mHasSpamTrashSetting; }
    boolean hasNoResults() { return false; }
    boolean hasAllResults() { return false; }
    
    @Override
    QueryOperation expandLocalRemotePart(Mailbox mbox) throws ServiceException {
        return this;
    }
    
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException
    {
        // wrap ourselves in a DBQueryOperation, since we're eventually going to need to go to the DB
        DBQueryOperation dbOp = DBQueryOperation.Create();
        dbOp.addTextOp(this);
        return dbOp.ensureSpamTrashSetting(mbox, includeTrash, includeSpam);
    }

    QueryTargetSet getQueryTargets() {
        QueryTargetSet toRet = new QueryTargetSet(1);
        toRet.add(QueryTarget.UNSPECIFIED);
        return toRet;
    }

    protected void setDBOperation(DBQueryOperation op) {
        mDBOp = op;
    }
    
    public void resetIterator() throws ServiceException {
        if (mDBOp != null) {
            mDBOp.resetIterator();
        }
    }

    public ZimbraHit getNext() throws ServiceException {
        if (mDBOp != null) {
            return mDBOp.getNext();
        }
        return null;
    }

    public ZimbraHit peekNext() throws ServiceException
    {
        if (mDBOp != null) {
            return mDBOp.peekNext();
        }
        return null;
    }
    
    public List<QueryInfo> getResultInfo() {
        List<QueryInfo> toRet = new ArrayList<QueryInfo>();
        toRet.addAll(mQueryInfo);
        
        if (mDBOp != null)
            toRet.addAll(mDBOp.mQueryInfo);
        
        return toRet;
    }
    
    QueryOperation optimize(Mailbox mbox) throws ServiceException {
        return this;
    }

    /**
     * Helper for implementing QueryOperation.depthFirstRecurse(RecurseCallback)
     * 
     * @param cb
     */
    protected void depthFirstRecurseInternal(RecurseCallback cb) {
        cb.recurseCallback(this);
    }

    protected void depthFirstRecurse(RecurseCallback cb) {
        if (mDBOp != null) 
            mDBOp.depthFirstRecurse(cb);
        else 
            depthFirstRecurseInternal(cb);
    }
    
    /**
     * We use this data structure to track a "chunk" of Lucene hits which
     * the DBQueryOperation will use to check against the DB.
     */
    protected static class TextResultsChunk {

        static class ScoredLuceneHit {
            ScoredLuceneHit(float score) { mScore= score; }
            public List<Document> mDocs = new ArrayList<Document>();
            public float mScore; // highest score in list
        }

        Set<Integer> getIndexIds() { 
            Set<Integer>toRet = new LinkedHashSet<Integer>(mHits.keySet().size());
            for (Iterator<Integer> iter = mHits.keySet().iterator(); iter.hasNext();) {
                Integer curInt = (Integer)iter.next();
                toRet.add(curInt);
            }
            return toRet;
        }

        protected int size() { return mHits.size(); }

        protected void addHit(int indexId, Document doc, float score) {
            addHit(new Integer(indexId), doc, score);
        }

        private void addHit(Integer indexId, Document doc, float score) {
            ScoredLuceneHit sh = mHits.get(indexId);
            if (sh == null) {
                sh = new ScoredLuceneHit(score);
                mHits.put(indexId, sh);
            }

            sh.mDocs.add(doc);
        }

        ScoredLuceneHit getScoredHit(Integer indexId) { 
            return mHits.get(indexId);
        }

        private HashMap <Integer /*indexId*/, ScoredLuceneHit> mHits = new LinkedHashMap<Integer, ScoredLuceneHit>();
    }
}
