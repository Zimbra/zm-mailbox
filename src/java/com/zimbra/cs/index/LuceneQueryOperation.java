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


/**
 * LuceneQueryOperation.
 */
final class LuceneQueryOperation extends TextQueryOperation {
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

    private TopDocs mTopDocs = null;
    private int mTopDocsLen = 0; // number of hits fetched
    private int mTopDocsChunkSize = 2000; // how many hits to fetch per step in lucene
    private IndexSearcherRef mSearcher = null;

    LuceneQueryOperation() {
        mQuery = new BooleanQuery();
    }

    @Override
    protected boolean shouldExecuteDbFirst() {
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
     * Called be a DBQueryOperation that is wrapping us in a DB-First query plan:
     * gets a chunk of results that it feeds into a SQL query
     *
     * @param maxChunkSize
     * @return
     * @throws ServiceException
     */
    @Override
    protected TextResultsChunk getNextResultsChunk(int maxChunkSize) throws ServiceException {
        try {
            if (!mHaveRunSearch) {
                fetchFirstResults(maxChunkSize);
            }

            TextResultsChunk toRet = new TextResultsChunk();
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

    @Override
    protected void setupTextQueryOperation(QueryContext ctx) {
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

    //////////////////////////////////////////////////////////////
    //
    // Cloning
    //
    /**
     * Just clone *this* object, don't clone the embedded DBOp
     * @return
     * @throws CloneNotSupportedException
     */
    protected LuceneQueryOperation cloneInternal() throws CloneNotSupportedException {
        LuceneQueryOperation toRet = (LuceneQueryOperation) super.clone();

        assert(!mHaveRunSearch);

        mQuery = (BooleanQuery)mQuery.clone();

        return toRet;
    }

    @Override
    public Object clone() {
        try {
            assert(mSearcher == null);
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
     * Called from DBQueryOperation.clone()
     *
     * @param caller - our DBQueryOperation which has ALREADY BEEN CLONED
     * @return
     * @throws CloneNotSupportedException
     */
    @Override
    protected Object clone(DBQueryOperation caller) throws CloneNotSupportedException {
        assert(mSearcher == null);
        LuceneQueryOperation toRet = cloneInternal();
        toRet.setDBOperation(caller);

        return toRet;
    }

    @Override
    protected int countHits() {
        return mTopDocs != null ? mTopDocs.totalHits : 0;
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

}
