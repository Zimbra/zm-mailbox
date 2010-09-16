/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.io.IOException;
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
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * {@link QueryOperation} which queries Lucene.
 */
public final class LuceneQueryOperation extends QueryOperation {
    private static final float sDbFirstTermFreqPerc;
    static {
        float f = 0.8f;
        try {
            f = Float.parseFloat(LC.search_dbfirst_term_percentage_cutoff.value());
        } catch (Exception e) {
        }
        if (f < 0.0 || f > 1.0) {
            f = 0.8f;
        }
        sDbFirstTermFreqPerc = f;
    }

    private int mCurHitNo = 0; // our offset into the hits
    private boolean mHaveRunSearch = false;
    private String mQueryString = "";
    private BooleanQuery mQuery;

    /**
     * Used for doing DB-joins: the list of terms for the filter one of the
     * terms in the list MUST occur in the document for it to match.
     */
    private List<Term> mFilterTerms;

    /**
     * Because we don't store the real mail-item-id of documents, we ALWAYS need
     * a DBOp in order to properly get our results.
     */
    private DBQueryOperation mDBOp = null;
    private List<QueryInfo> mQueryInfo = new ArrayList<QueryInfo>();
    private boolean mHasSpamTrashSetting = false;

    private TopDocs mTopDocs = null;
    private int mTopDocsLen = 0; // number of hits fetched
    private int mTopDocsChunkSize = 2000; // how many hits to fetch per step in Lucene
    private IndexSearcherRef mSearcher = null;

    public LuceneQueryOperation() {
        mQuery = new BooleanQuery();
    }

    /**
     * Adds the specified text clause at the toplevel.
     * <p>
     * e.g. going in "a b c" if we addClause("d") we get "a b c d".
     *
     * @param queryStr Appended to the end of the text-representation of this query
     * @param query Lucene Query term
     * @param truth allows for negated query terms
     */
    public void addClause(String queryStr, Query query, boolean truth) {
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
     * Adds the specified text clause ANDED with the existing query.
     * <p>
     * e.g. going in w/ "a b c" if we addAndedClause("d") we get "(a b c) AND d".
     * <p>
     * This API may only be called AFTER query optimizing and AFTER remote
     * queries have been split.
     * <p>
     * Note that this API does *not* update the text-representation of this query.
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
     * Adds the specified text clause as a filter over the existing query.
     * <p>
     * e.g. going in w/ "a b c" if we addAndedClause("d") we get "(a b c) AND d".
     * <p>
     * This API is used by the query executor so that it can temporarily add a
     * bunch of indexIds to the existing query -- this is necessary when we are
     * doing a DB-first query plan execution.
     * <p>
     * Note that this API does *not* update the text-representation of this query.
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
     * Clears the filter clause
     */
    void clearFilterClause() {
        mFilterTerms = null;
    }

    /**
     * Sets the text query *representation* manually -- the thing that is output
     * if we have to proxy this search somewhere else -- used when dealing with
     * wildcard searches.
     */
    public void setQueryString(String queryStr) {
        assert(mQueryString.length() == 0);
        mQueryString = queryStr;
    }

    /**
     * Used by a wrapping {@link DBQueryOperation}, when it is running a
     * DB-FIRST plan.
     *
     * @return the current query
     */
    BooleanQuery getCurrentQuery() {
        assert(!mHaveRunSearch);
        return mQuery;
    }

    @Override
    String toQueryString() {
        StringBuilder ret = new StringBuilder("(");
        ret.append(mQueryString);
        return ret.append(")").toString();
    }

    /**
     * Returns {@code true} if we think this query is best evaluated DB-FIRST.
     */
    boolean shouldExecuteDbFirst() {
        if (mSearcher == null) {
            return true;
        }

        BooleanClause[] clauses = mQuery.getClauses();
        if (clauses.length <= 1) {
            Query q = clauses[0].getQuery();

            if (q instanceof TermQuery) {
                TermQuery tq = (TermQuery)q;
                Term term = tq.getTerm();
                try {
                    int freq = mSearcher.getSearcher().docFreq(term);
                    int docsCutoff = (int) (mSearcher.getSearcher().maxDoc() * sDbFirstTermFreqPerc);
                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("Term matches " + freq +
                                " docs.  DB-First cutoff (" +
                                (100 * sDbFirstTermFreqPerc) + "%) is " +
                                docsCutoff + " docs");
                    }
                    if (freq > docsCutoff) {
                        return true;
                    }
                } catch (IOException e) {
                    return false;
                }
            }
        }

        try {
            fetchFirstResults(1000); // some arbitrarily large initial size to fetch
            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("Lucene part has " + countHits() + " hits");
            }
            if (this.countHits() > 1000) { // also arbitrary, just to make very small searches run w/o extra DB check
                int dbHitCount = this.mDBOp.getDbHitCount();
                if (ZimbraLog.index_search.isDebugEnabled()) {
                    ZimbraLog.index_search.debug("Lucene part has " + countHits() + " hits, db part has " + dbHitCount);
                }

                if (dbHitCount < this.countHits()) {
                    return true; // run DB-FIRST
                }
            }
        } catch (ServiceException e) {
            return false;
        }

        return false;
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        if (mSearcher != null) {
            mSearcher.dec();
        }
    }

    private void fetchFirstResults(int initialChunkSize) {
        if (!mHaveRunSearch) {
            assert(mCurHitNo == 0);
            mTopDocsLen = 3*initialChunkSize;
            long start = 0;
            if (ZimbraLog.index_search.isDebugEnabled()) {
                start = System.currentTimeMillis();
            }
            runSearch();
            if (ZimbraLog.index_search.isDebugEnabled()) {
                long time = System.currentTimeMillis() - start;
                int totalResults = mTopDocs != null ? mTopDocs.totalHits : 0;
                ZimbraLog.index_search.debug("Fetched Initial " + mTopDocsLen +
                        " (out of " + totalResults +
                        " total) search results from Lucene in " + time + "ms");
            }
        }
    }

    /**
     * Fetch the next chunk of results.
     * <p>
     * Called by a {@link DBQueryOperation} that is wrapping us in a DB-First
     * query plan: gets a chunk of results that it feeds into a SQL query.
     */
    LuceneResultsChunk getNextResultsChunk(int maxChunkSize) throws ServiceException {
        try {
            if (!mHaveRunSearch) {
                fetchFirstResults(maxChunkSize);
            }

            LuceneResultsChunk toRet = new LuceneResultsChunk();
            int luceneLen;

            luceneLen = mTopDocs != null ? mTopDocs.totalHits : 0;

            long timeUsed = 0;
            long start = 0;
            long fetchFromLucene1 = 0;
            long fetchFromLucene2 = 0;

            while ((toRet.size() < maxChunkSize) && (mCurHitNo < luceneLen)) {
                if (mTopDocsLen <= mCurHitNo) {
                    mTopDocsLen += mTopDocsChunkSize;
                    mTopDocsChunkSize *= 4;
                    if (mTopDocsChunkSize > 1000000) {
                        mTopDocsChunkSize = 1000000;
                    }
                    if (mTopDocsLen > luceneLen) {
                        mTopDocsLen = luceneLen;
                    }
                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        start = System.currentTimeMillis();
                    }
                    runSearch();
                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        long time = System.currentTimeMillis() - start;
                        ZimbraLog.index_search.debug("Fetched " + mTopDocsLen +
                                " search results from Lucene in " + time + "ms");
                    }
                }

                if (ZimbraLog.index_search.isDebugEnabled()) {
                    start = System.currentTimeMillis();
                }


                int docId = mTopDocs.scoreDocs[mCurHitNo].doc;
                Document d = mSearcher.getSearcher().doc(docId);

                if (ZimbraLog.index_search.isDebugEnabled()) {
                    long now = System.currentTimeMillis();
                    fetchFromLucene1 += (now - start);
                    start = now;
                }

                float score;
                score = mTopDocs.scoreDocs[mCurHitNo].score;

                if (ZimbraLog.index_search.isDebugEnabled()) {
                    fetchFromLucene2 += (System.currentTimeMillis() - start);
                }

                mCurHitNo++;

                String mbid = d.get(LuceneFields.L_MAILBOX_BLOB_ID);
                try {
                    if (mbid != null) {
                        start = System.currentTimeMillis();
                        toRet.addHit(mbid, d, score);
                        long end = System.currentTimeMillis();
                        timeUsed += (end-start);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }

            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("FetchFromLucene1 " + fetchFromLucene1 +
                        "ms FetchFromLucene2 " + fetchFromLucene2 + "ms ");
            }

            return toRet;

        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException getting lucene results", e);
        }
    }

    /**
     * Execute the actual search via Lucene
     */
    private void runSearch() {
        mHaveRunSearch = true;

        try {
            if (mQuery != null) {
                if (mSearcher != null) { // this can happen if the Searcher couldn't be opened, e.g. index does not exist
                    BooleanQuery outerQuery = new BooleanQuery();
                    outerQuery.add(new BooleanClause(new TermQuery(
                            new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE)), Occur.MUST));
                    outerQuery.add(new BooleanClause(mQuery, Occur.MUST));
                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("Executing Lucene Query: " + outerQuery.toString());
                    }

                    TermsFilter filter = null;
                    if (mFilterTerms != null) {
                        filter = new TermsFilter();
                        for (Term t : mFilterTerms) {
                            filter.addTerm(t);
                        }
                    }
                    mTopDocs = mSearcher.search(outerQuery, filter, mTopDocsLen);
                } else {
                    mTopDocs = null;
                }
            } else {
                assert(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (mSearcher != null) {
                mSearcher.dec();
                mSearcher = null;
            }
            mTopDocs = null;
        }
    }

    private void setupTextQueryOperation(QueryContext ctx) {
        MailboxIndex midx = ctx.getMailbox().getMailboxIndex();
        if (midx != null) {
            try {
                mSearcher = midx.getIndexSearcherRef(ctx.getResults().getSortBy());
            } catch (IOException e) {
                ZimbraLog.index_search.error("failed to obtain searcher", e);
            }
        }
    }

    @Override
    public String toString() {
        return "LUCENE(" + mQuery.toString() + (hasSpamTrashSetting() ? " <ANYWHERE>" : "") + ")";
    }

    /**
     * Just clone *this* object, don't clone the embedded DBOp
     * @return
     * @throws CloneNotSupportedException
     */
    private LuceneQueryOperation cloneInternal() throws CloneNotSupportedException {
        assert(!mHaveRunSearch);
        LuceneQueryOperation toRet = (LuceneQueryOperation) super.clone();
        mQuery = (BooleanQuery)mQuery.clone();
        return toRet;
    }

    @Override
    public Object clone() {
        assert(mSearcher == null);
        try {
            LuceneQueryOperation toRet = cloneInternal();
            if (mDBOp != null) {
                toRet.mDBOp = (DBQueryOperation) mDBOp.clone(this);
            }
            return toRet;
        } catch (CloneNotSupportedException e) {
            assert(false);
            return null;
        }
    }

    /**
     * Called from {@link DBQueryOperation#clone()}
     *
     * @param caller - our DBQueryOperation which has ALREADY BEEN CLONED
     * @return
     * @throws CloneNotSupportedException
     */
    Object clone(DBQueryOperation caller) throws CloneNotSupportedException {
        assert(mSearcher == null);
        LuceneQueryOperation toRet = cloneInternal();
        toRet.setDBOperation(caller);

        return toRet;
    }

    /**
     * Must be called AFTER the first results chunk is fetched.
     *
     * @return number of hits in this search
     */
    int countHits() {
        return mTopDocs != null ? mTopDocs.totalHits : 0;
    }

    /**
     * Reset our hit iterator back to the beginning of the result set.
     */
    void resetDocNum() {
        mCurHitNo = 0;
    }

    @Override
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        assert(!mHaveRunSearch);

        if (union) {
            if (other.hasNoResults()) {
                mQueryInfo.addAll(other.getResultInfo());
                // a query for (other OR nothing) == other
                return this;
            }
        } else {
            if (other.hasAllResults()) {
                if (other.hasSpamTrashSetting()) {
                    forceHasSpamTrashSetting();
                }
                mQueryInfo.addAll(other.getResultInfo());
                // we match all results.  (other AND anything) == other
                return this;
            }
        }

        if (other instanceof LuceneQueryOperation) {
            LuceneQueryOperation otherLuc = (LuceneQueryOperation) other;
            if (union) {
                mQueryString = '(' + mQueryString + ") OR (" + otherLuc.mQueryString + ')';
            } else {
                mQueryString = '(' + mQueryString + ") AND (" + otherLuc.mQueryString + ')';
            }

            BooleanQuery top = new BooleanQuery();
            BooleanClause lhs = new BooleanClause(mQuery, union ? Occur.SHOULD : Occur.MUST);
            BooleanClause rhs = new BooleanClause(otherLuc.mQuery, union ? Occur.SHOULD : Occur.MUST);
            top.add(lhs);
            top.add(rhs);
            mQuery = top;
            mQueryInfo.addAll(other.getResultInfo());
            return this;
        }
        return null;
    }

    @Override
    void forceHasSpamTrashSetting() {
        mHasSpamTrashSetting = true;
    }

    List<QueryInfo> getQueryInfo() {
        return mQueryInfo;
    }

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
        dbOp.setLuceneQueryOperation(this);
        return dbOp.ensureSpamTrashSetting(mbox, includeTrash, includeSpam);
    }

    @Override
    QueryTargetSet getQueryTargets() {
        QueryTargetSet toRet = new QueryTargetSet(1);
        toRet.add(QueryTarget.UNSPECIFIED);
        return toRet;
    }

    void setDBOperation(DBQueryOperation op) {
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
     */
    void depthFirstRecurseInternal(RecurseCallback cb) {
        cb.recurseCallback(this);
    }

    @Override
    protected void depthFirstRecurse(RecurseCallback cb) {
        if (mDBOp != null) {
            mDBOp.depthFirstRecurse(cb);
        } else {
            depthFirstRecurseInternal(cb);
        }
    }


    /**
     * Allows the parser (specifically the Query subclasses) to store some query
     * result information so that it can be returned to the caller after the
     * query has run. This is used for things like spelling suggestion
     * correction, or wildcard expansion info: things that are not results
     * per-se but still need to have some way to be sent back to the caller.
     */
    public void addQueryInfo(QueryInfo inf) {
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
            mDBOp.setLuceneQueryOperation(this);
            mDBOp.begin(ctx); // will call back into this method again!
        } else { // 2nd time called
            setupTextQueryOperation(ctx);
        }
    }

    static final class ScoredLuceneHit {
        private final List<Document> mDocs = new ArrayList<Document>();
        private final float mScore; // highest score in list

        ScoredLuceneHit(float score) {
            mScore = score;
        }

        float getScore() {
            return mScore;
        }

        List<Document> getDocuments() {
            return mDocs;
        }
    }

    /**
     * We use this data structure to track a "chunk" of Lucene hits which
     * the {@link DBQueryOperation} will use to check against the DB.
     */
    static final class LuceneResultsChunk {
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

        int size() {
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
