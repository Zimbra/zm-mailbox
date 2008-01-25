/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.search.BooleanClause.Occur;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Mailbox;


/************************************************************************
 * 
 * LuceneQueryOperation
 * 
 ***********************************************************************/
class LuceneQueryOperation extends TextQueryOperation
{
    protected static Log mLog = LogFactory.getLog(LuceneQueryOperation.class);
    
    public static boolean USE_TOPDOCS = true;
    
    private Hits mLuceneHits = null;
    private TopDocs mTopDocs = null;
    private int mTopDocsLen = 0;
    private int mTopDocsChunkSize = 2000;
    private int mCurHitNo = 0;
    private RefCountedIndexSearcher mSearcher = null;
    private Sort mSort = null;
    private boolean mHaveRunSearch = false;
    private String mQueryString = "";
    protected static final float sDbFirstTermFreqPerc;
    
    static {
        float f = 0.8f;
        try {
            f = Float.parseFloat(LC.search_dbfirst_term_percentage_cutoff.value());
        } catch (Exception e) {}
        if (f < 0.0 || f > 1.0)
            f = 0.8f;
        sDbFirstTermFreqPerc = f;
    }

    protected boolean shouldExecuteDbFirst() {
        if (mSearcher == null)
            return true;

        BooleanClause[] clauses = mQuery.getClauses();
        if (clauses.length > 1)
            return false;
        
        Query q = clauses[0].getQuery();

        if (q instanceof TermQuery) {
            TermQuery tq = (TermQuery)q;
            Term term = tq.getTerm();
            try {
                int freq = mSearcher.getSearcher().docFreq(term);
                int docsCutoff = (int)(mSearcher.getSearcher().maxDoc() * sDbFirstTermFreqPerc);
                if (ZimbraLog.index.isDebugEnabled())
                    ZimbraLog.index.debug("Term matches "+freq+" docs.  DB-First cutoff ("+(100*sDbFirstTermFreqPerc)+"%) is "+docsCutoff+" docs");
                if (freq > docsCutoff)  
                    return true;
            } catch (IOException e) {
                return false;
            }
        }
    
        try {
            fetchFirstResults(1000); // some arbitrarily large initial size to fetch
            if (this.countHits() > (3 * DBQueryOperation.MAX_HITS_PER_CHUNK)) { // also arbitrary, just to make very small searches run w/o 
                int dbHitCount = this.mDBOp.getDbHitCount();
                
                if (ZimbraLog.index.isDebugEnabled()) {
                    ZimbraLog.index.debug("Lucene part has "+this.countHits()+" hits, db part has "+dbHitCount);
                }
                if (dbHitCount < this.countHits())
                    return true; // run DB-FIRST 
            }
        } catch (ServiceException e) {
            return false;
        }
        
        return false;
    }

    public void doneWithSearchResults() throws ServiceException {
        mSort = null;
        if (mSearcher != null) {
            mSearcher.release();
        }
    };

    /**
     * Reset our hit iterator back to the beginning of the result set.  
     */
    protected void resetDocNum() {
        mCurHitNo = 0;
    }

    private void fetchFirstResults(int initialChunkSize) throws ServiceException {
        if (USE_TOPDOCS) {
            if (!mHaveRunSearch) {
                assert(mCurHitNo == 0);
                mTopDocsLen = 3*initialChunkSize;
                long start = 0;
                if (mLog.isDebugEnabled())
                    start = System.currentTimeMillis(); 
                runSearch();
                if (mLog.isDebugEnabled()) {
                    long time = System.currentTimeMillis() - start;
                    int totalResults = mTopDocs != null ? mTopDocs.totalHits : 0;
                    mLog.debug("Fetched Initial "+mTopDocsLen+" (out of "+totalResults+" total) search results from Lucene in "+time+"ms");
                }
            }
        } else {
            if (!mHaveRunSearch) {
                long start = 0;
                if (mLog.isDebugEnabled())
                    start = System.currentTimeMillis(); 
                runSearch();
                if (mLog.isDebugEnabled()) {
                    long time = System.currentTimeMillis() - start;
                    mLog.debug("Fetched initial results from Lucene in "+time+"ms");
                }
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
    protected TextResultsChunk getNextResultsChunk(int maxChunkSize) throws ServiceException {
        try {
            if (!mHaveRunSearch) 
                fetchFirstResults(maxChunkSize);
            
            TextResultsChunk toRet = new TextResultsChunk();
            int luceneLen;
            
            if (USE_TOPDOCS) {
                luceneLen = mTopDocs != null ? mTopDocs.totalHits : 0;
            } else {
                luceneLen = mLuceneHits != null ? mLuceneHits.length() : 0;
            }
            
            long timeUsed = 0;
            long start = 0;
            long fetchFromLucene1 = 0;
            long fetchFromLucene2 = 0;
            FieldSelector f = new MapFieldSelector(new String[] { LuceneFields.L_MAILBOX_BLOB_ID } );

            while ((toRet.size() < maxChunkSize) && (mCurHitNo < luceneLen)) {
                if (USE_TOPDOCS && mTopDocsLen <= mCurHitNo) {
                    mTopDocsLen += mTopDocsChunkSize;
                    mTopDocsChunkSize *=4;
                    if (mTopDocsChunkSize > 1000000)
                        mTopDocsChunkSize = 1000000;
                    if (mTopDocsLen > luceneLen)
                        mTopDocsLen = luceneLen;
                    if (mLog.isDebugEnabled()) {
                        start = System.currentTimeMillis();
                    }
                    runSearch();
                    if (mLog.isDebugEnabled()) {
                        long time = System.currentTimeMillis() - start;
                        mLog.debug("Fetched "+mTopDocsLen+" search results from Lucene in "+time+"ms");
                    }
                }
                
                if (mLog.isDebugEnabled())
                    start = System.currentTimeMillis();
                
                Document d;
                if (USE_TOPDOCS) {
                    int docId = mTopDocs.scoreDocs[mCurHitNo].doc;
                    d = mSearcher.getSearcher().doc(docId);
                } else {
                    if (false) {
//                      d = mLuceneHits.doc(mCurHitNo);
                        d = mLuceneHits.doc(mCurHitNo);
                    } else {
                        int docId = mLuceneHits.id(mCurHitNo);
                        d = mSearcher.getReader().document(docId, f);
                    }
                }
                
                if (mLog.isDebugEnabled()) {
                    long now = System.currentTimeMillis();
                    fetchFromLucene1 += (now - start);
                    start = now;
                }
                
                float score;
                if (USE_TOPDOCS) {
                    score = mTopDocs.scoreDocs[mCurHitNo].score;
                } else {
                    score = mLuceneHits.score(mCurHitNo);
                }
                
                if (mLog.isDebugEnabled())
                    fetchFromLucene2 += (System.currentTimeMillis() - start);
                
                mCurHitNo++;
                
                String mbid = d.get(LuceneFields.L_MAILBOX_BLOB_ID);
                try {
                    if (mbid != null) {
                        start = System.currentTimeMillis();
                        toRet.addHit(Integer.parseInt(mbid), null, score);
                        long end = System.currentTimeMillis();
                        timeUsed += (end-start);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            
            if (mLog.isDebugEnabled()) {
                mLog.debug("FetchFromLucene1 "+fetchFromLucene1+"ms FetchFromLucene2 "+fetchFromLucene2+"ms ");
            }

            return toRet;

        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException getting lucene results", e);
        }
    }

    private BooleanQuery mQuery;

    protected static LuceneQueryOperation doCreate() {
        LuceneQueryOperation toRet = new LuceneQueryOperation();
        toRet.mQuery = new BooleanQuery();
        return toRet;
    }

    private LuceneQueryOperation() { }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#prepare(com.zimbra.cs.mailbox.Mailbox, com.zimbra.cs.index.ZimbraQueryResultsImpl, com.zimbra.cs.index.MailboxIndex, com.zimbra.cs.index.SearchParams, int)
     */
    protected void prepare(Mailbox mbx, ZimbraQueryResultsImpl res, MailboxIndex mbidx, SearchParams params, int chunkSize) throws ServiceException, IOException
    {
        mParams = params;
        assert(!mHaveRunSearch);
        if (mDBOp == null) {
            // wrap ourselves in a DBQueryOperation, since we're eventually going to need to go to the DB
            mDBOp = DBQueryOperation.Create();
            mDBOp.addTextOp(this);
            mDBOp.prepare(mbx, res, mbidx, params, chunkSize); // will call back into this function again!
        } else {
            this.setupResults(mbx, res);

            try {
                if (mbidx != null) {
                    mSearcher = mbidx.getLuceneIndex().getCountedIndexSearcher();
                    mSort = mbidx.getLuceneIndex().getSort(res.getSortBy());
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (mSearcher != null) {
                    mSearcher.release();
                    mSearcher = null;
                }
                mSort = null;
                mLuceneHits = null;
            }
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
                    outerQuery.add(new BooleanClause(new TermQuery(new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE)), Occur.MUST));
                    outerQuery.add(new BooleanClause(mQuery, Occur.MUST));
                    if (USE_TOPDOCS) {
                        if (mSort == null) 
                            mTopDocs = mSearcher.getSearcher().search(outerQuery, null, mTopDocsLen);
                        else
                            mTopDocs = mSearcher.getSearcher().search(outerQuery, null, mTopDocsLen, mSort);
                    } else {
                        mLuceneHits = mSearcher.getSearcher().search(outerQuery, mSort);
                    }
                } else {
                    mTopDocs = null;
                    mLuceneHits = null;
                }
            } else {
                assert(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (mSearcher != null) {
                mSearcher.release();
                mSearcher = null;
            }
            mTopDocs = null;
            mLuceneHits = null;
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#toQueryString()
     */
    String toQueryString() {
        StringBuilder ret = new StringBuilder("(");

        ret.append(this.mQueryString);

        return ret.append(")").toString();
    }

    public String toString()
    {
        return "LUCENE(" + mQuery.toString() + (hasSpamTrashSetting() ? " <ANYWHERE>" : "") + ")";
    }

    /**
     * Just clone *this* object, don't clone the embedded DBOp
     * @return
     * @throws CloneNotSupportedException
     */
    protected LuceneQueryOperation cloneInternal() throws CloneNotSupportedException {
        LuceneQueryOperation toRet = (LuceneQueryOperation)super.clone();

        assert(mSearcher == null);
        assert(mSort == null);
        assert(!mHaveRunSearch);

        mQuery = (BooleanQuery)mQuery.clone();

        return toRet;
    }
    
    public Object clone() {
        try {
            LuceneQueryOperation toRet = cloneInternal();
            if (mDBOp != null)
                toRet.mDBOp = (DBQueryOperation)mDBOp.clone(this);
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
    protected Object clone(DBQueryOperation caller) throws CloneNotSupportedException {
        LuceneQueryOperation toRet = cloneInternal();
        toRet.setDBOperation(caller);

        return toRet;
    }

    
    

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#combineOps(com.zimbra.cs.index.QueryOperation, boolean)
     */
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
            LuceneQueryOperation otherLuc = (LuceneQueryOperation)other;
            if (union) {
                mQueryString = '('+mQueryString+") OR ("+otherLuc.mQueryString+')';
            } else {
                mQueryString = '('+mQueryString+") AND ("+otherLuc.mQueryString+')';
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
    
    /**
     * This API may only be called AFTER query optimizing and AFTER remote queries have been
     * split.  This API is used by the query executor so that it can temporarily add a bunch of 
     * indexIds to the existing query -- this is necessary when we are doing a DB-first query
     * plan execution.
     * 
     * @param q
     * @param truth
     */
    void addAndedClause(Query q, boolean truth) {
        mQueryString = "UNKNOWN"; // not supported for this case

        mHaveRunSearch = false; // will need to re-run the search with this new clause
        mCurHitNo = 0;

        BooleanQuery top = new BooleanQuery();
        BooleanClause lhs = new BooleanClause(mQuery, Occur.MUST);
        BooleanClause rhs = new BooleanClause(q, truth ? Occur.MUST : Occur.MUST_NOT);
        top.add(lhs);
        top.add(rhs);
        mQuery = top;
    }    

    /**
     * Set the text query representation manually -- used when we're creating a wildcard query 
     * (add a bunch of wildcard terms) but we have no regular terms 
     * @param queryStr
     */
    void setQueryString(String queryStr) {
        assert(mQueryString.length() == 0);
        mQueryString = queryStr;
    }
    
    /**
     * 
     * @param q
     * @param truth
     */
    void addClause(String queryStr, Query q, boolean truth) {
        mQueryString = mQueryString+" "+queryStr;
        assert(!mHaveRunSearch);

        if (truth) {
            mQuery.add(new BooleanClause(q, Occur.MUST));
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
            mQuery.add(new BooleanClause(new TermQuery(new Term(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_TOP)),Occur.MUST));
//          mQuery.add(new BooleanClause(new TermQuery(new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE)), true, false));
            mQuery.add(new BooleanClause(q, Occur.MUST_NOT));
        }
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.ZimbraQueryResults#estimateResultSize()
     */
    public int estimateResultSize() throws ServiceException {
        if (mDBOp == null)
            return 0; // you need to run the query before this number is known
        else
            return mDBOp.estimateResultSize();
    }
    
    int countHits() {
        if (USE_TOPDOCS) {
            int totalResults = mTopDocs != null ? mTopDocs.totalHits : 0;
            return totalResults;
        } else {
            if (mLuceneHits != null) {
                return mLuceneHits.length();
            } else {
                return 0;
            }
        }
    }
}
