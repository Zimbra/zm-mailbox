/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010 Zimbra, Inc.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;

abstract class TextQueryOperation extends QueryOperation {
    protected int mCurHitNo = 0; // our offset into the hits
    protected boolean mHaveRunSearch = false;
    protected String mQueryString = "";
    protected BooleanQuery mQuery;

    protected List<Term> mFilterTerms;  // used for doing DB-joins: the list of terms for the filter
                                        // one of the terms in the list MUST occur in the document
                                        // for it to match

    /**
     * because we don't store the real mail-item-id of documents, we ALWAYS need a DBOp
     * in order to properly get our results.
     */
    protected DBQueryOperation mDBOp = null;
    protected List<QueryInfo> mQueryInfo = new ArrayList<QueryInfo>();
    private boolean mHasSpamTrashSetting = false;

    //////////////////////////////////////////////////////////////
    //
    // Called while running the search
    //
    /**
     * @return TRUE if we think this query is best evaluated DB-FIRST
     */
    abstract protected boolean shouldExecuteDbFirst();

    /**
     * Fetch the next chunk of results from the Text Index
     *
     * @param maxChunkSize
     * @return
     * @throws ServiceException
     */
    abstract protected TextResultsChunk getNextResultsChunk(int maxChunkSize) throws ServiceException;

    /**
     * Must be called AFTER the first results chunk is fetched
     *
     * @return number of hits in this search
     */
    abstract protected int countHits();


    /**
     * Reset our hit iterator back to the beginning of the result set.
     */
    protected void resetDocNum() {
        mCurHitNo = 0;
    }


    //////////////////////////////////////////////////////////////
    //
    // Query manipulation:
    //
    /**
     * Add the specified text clause at the toplevel
     *   (e.g. going in "a b c" if we addClause("d") we get "a b c d"
     *
     * @param queryStr - Appended to the end of the text-representation of this query
     * @param query - Lucene Query term
     * @param truth - allows for negated query terms
     */
    void addClause(String queryStr, Query query, boolean truth) {
        mQueryString = mQueryString + " " + (truth ? "" : "-") + queryStr;
        assert(!mHaveRunSearch);

        if (truth) {
            mQuery.add(new BooleanClause(query, BooleanClause.Occur.MUST));
        } else {
            // Why do we add this here?  Because lucene won't allow naked "NOT" queries.
            // Why do we check against Partname=TOP instead of against "All"?  Well, it is a simple case
            // of "do mostly what the user wants" --->
            //
            // Imagine a message with two parts.  The message is from "Ross" and the toplevel part contains
            // the word "roland" and the attachment doesn't.
            //           Now a query "from:ross and not roland" WOULD match this message: since the second part
            //           of the message does indeed match these criteria!
            //
            // Basically the problem stems because we play games: sometimes treating the message like multiple
            // separate parts, but also sometimes treating it like a single part.
            //
            // Anyway....that's the problem, and for right now we just fix it by constraining the NOT to the
            // TOPLEVEL of the message....98% of the time that's going to be good enough.
            //

            // Parname:TOP now expanded to be (TOP or CONTACT or NOTE) to deal with extended partname assignemtns during indexing
            BooleanQuery top = new BooleanQuery();
            top.add(new BooleanClause(new TermQuery(new Term(
                    LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_TOP)),
                    BooleanClause.Occur.SHOULD));
            top.add(new BooleanClause(new TermQuery(new Term(
                    LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_CONTACT)),
                    BooleanClause.Occur.SHOULD));
            top.add(new BooleanClause(new TermQuery(new Term(
                    LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_NOTE)),
                    BooleanClause.Occur.SHOULD));
            mQuery.add(new BooleanClause(top, BooleanClause.Occur.MUST));
            mQuery.add(new BooleanClause(query, BooleanClause.Occur.MUST_NOT));
        }
    }

    /**
     * Add the specified text clause ANDED with the existing query
     *   (e.g. going in w/ "a b c" if we addAndedClause("d") we get "(a b c) AND d"
     *
     * This API may only be called AFTER query optimizing and AFTER remote queries have been
     * split.
     *
     * Note that this API does *not* update the text-representation of this query
     *
     * @param q
     * @param truth
     */
    void addAndedClause(Query q, boolean truth) {
        mHaveRunSearch = false; // will need to re-run the search with this new clause
        mCurHitNo = 0;

        BooleanQuery top = new BooleanQuery();
        BooleanClause lhs = new BooleanClause(mQuery, BooleanClause.Occur.MUST);
        BooleanClause rhs = new BooleanClause(q, truth ?
                BooleanClause.Occur.MUST : BooleanClause.Occur.MUST_NOT);
        top.add(lhs);
        top.add(rhs);
        mQuery = top;
    }

    /**
     * Add the specified text clause as a filter over the existing query
     *   (e.g. going in w/ "a b c" if we addAndedClause("d") we get "(a b c) AND d"
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
    void addFilterClause(Term t) {
        mHaveRunSearch = false; // will need to re-run the search with this new clause
        mCurHitNo = 0;

        if (mFilterTerms == null) {
            mFilterTerms = new ArrayList<Term>();
        }
        mFilterTerms.add(t);
    }

    /**
     * Clear the filter clause
     */
    void clearFilterClause() {
        mFilterTerms = null;
    }

    /**
     * Set the text query *representation* manually -- the thing that is output if we have
     * to proxy this search somewhere else -- used when dealing with wildcard searches.
     * @param queryStr
     */
    void setQueryString(String queryStr) {
        assert(mQueryString.length() == 0);
        mQueryString = queryStr;
    }


    /**
     * Used by a wrapping DBQueryOperation, when it is running a DB-First plan
     * @return The current query
     */
    protected BooleanQuery getCurrentQuery() {
        assert(!mHaveRunSearch);
        return mQuery;
    }


    /**
     * Re-set our query back to a previous state.  This may only be called AFTER query optimization
     * and remote query splitting has happened.  This is used when we are in a DB-first query
     * plan so that we can restore the original query after we've temporarily hacked it up with
     * a list of IndexIDs from the DB-first part.
     *
     * @param q
     */
    protected void resetQuery(BooleanQuery q) {
        mHaveRunSearch = false;
        mQuery = q;
        mCurHitNo = 0;
    }

    @Override
    String toQueryString() {
        StringBuilder ret = new StringBuilder("(");
        ret.append(mQueryString);
        return ret.append(")").toString();
    }

    /**
     * Called from DBQueryOperation.clone()
     *
     * @param caller - our DBQueryOperation which has ALREADY BEEN CLONED
     * @return
     * @throws CloneNotSupportedException
     */
    abstract protected Object clone(DBQueryOperation caller) throws CloneNotSupportedException;

    @Override
    void forceHasSpamTrashSetting() {
        mHasSpamTrashSetting = true;
    }

    List<QueryInfo> getQueryInfo() {
        return mQueryInfo;
    }

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

    @Override
    public int estimateResultSize() throws ServiceException {
        if (mDBOp == null)
            return 0; // you need to run the query before this number is known
        else
            return mDBOp.estimateResultSize();
    }

    /**
     * Can be called more than once recursively from {@link DBQueryOperation}.
     */
    @Override
    protected final void begin(QueryContext ctx) throws ServiceException {
        assert(!mHaveRunSearch);
        context = ctx;
        if (mDBOp == null) { // 1st time called
            // wrap ourselves in a DBQueryOperation, since we're eventually
            // going to need to go to the DB
            mDBOp = new DBQueryOperation();
            mDBOp.setTextQueryOperation(this);
            mDBOp.begin(ctx); // will call back into this method again!
        } else { // 2nd time called
            setupTextQueryOperation(ctx);
        }
    }

    abstract protected void setupTextQueryOperation(QueryContext ctx);

    @Override
    boolean hasSpamTrashSetting() {
        return mHasSpamTrashSetting;
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
        return this;
    }

    @Override
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException {
        // wrap ourselves in a DBQueryOperation, since we're eventually going to need to go to the DB
        DBQueryOperation dbOp = new DBQueryOperation();
        dbOp.setTextQueryOperation(this);
        return dbOp.ensureSpamTrashSetting(mbox, includeTrash, includeSpam);
    }

    @Override
    QueryTargetSet getQueryTargets() {
        QueryTargetSet toRet = new QueryTargetSet(1);
        toRet.add(QueryTarget.UNSPECIFIED);
        return toRet;
    }

    protected void setDBOperation(DBQueryOperation op) {
        mDBOp = op;
    }

    @Override
    public void resetIterator() throws ServiceException {
        if (mDBOp != null) {
            mDBOp.resetIterator();
        }
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        if (mDBOp != null) {
            return mDBOp.getNext();
        }
        return null;
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        if (mDBOp != null) {
            return mDBOp.peekNext();
        }
        return null;
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        List<QueryInfo> toRet = new ArrayList<QueryInfo>();
        toRet.addAll(mQueryInfo);

        if (mDBOp != null)
            toRet.addAll(mDBOp.mQueryInfo);

        return toRet;
    }

    @Override
    QueryOperation optimize(Mailbox mbox) {
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

    @Override
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
            ScoredLuceneHit(float score) {
                mScore= score;
            }
            public List<Document> mDocs = new ArrayList<Document>();
            public float mScore; // highest score in list
        }


        private Map<String/*indexId*/, ScoredLuceneHit> mHits =
            new LinkedHashMap<String, ScoredLuceneHit>();

        Set<String> getIndexIds() {
            Set<String> toRet = new LinkedHashSet<String>(mHits.keySet().size());
            for (Iterator<String> iter = mHits.keySet().iterator(); iter.hasNext();) {
                String curId= iter.next();
                toRet.add(curId);
            }
            return toRet;
        }

        protected int size() {
            return mHits.size();
        }

        void addHit(String indexId, Document doc, float score) {
            ScoredLuceneHit sh = mHits.get(indexId);
            if (sh == null) {
                sh = new ScoredLuceneHit(score);
                mHits.put(indexId, sh);
            }

            sh.mDocs.add(doc);
        }

        ScoredLuceneHit getScoredHit(String indexId) {
            return mHits.get(indexId);
        }
    }
}
