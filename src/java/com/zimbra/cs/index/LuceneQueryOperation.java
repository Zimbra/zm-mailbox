/*
 * Created on Oct 29, 2004
 *
 */
package com.zimbra.cs.index;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;


/************************************************************************
 * 
 * LuceneQueryOperation
 * 
 ***********************************************************************/
class LuceneQueryOperation extends QueryOperation
{
    private Hits mLuceneHits = null;
    private int mCurHitNo = 0;
    private MailboxIndex.CountedIndexSearcher mSearcher = null;
    
    /**
     * because we don't store the real mail-item-id of documents, we ALWAYS need a DBOp 
     * in order to properly get our results.
     */
    private DBQueryOperation mDBOp = null;
    
    void setDBOperation(DBQueryOperation op) {
        mDBOp = op;
    }
    
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException
    {
        // wrap ourselves in a DBQueryOperation, since we're eventually going to need to go to the DB
        DBQueryOperation dbOp = DBQueryOperation.Create();
        dbOp.addLuceneOp(this);
        return dbOp.ensureSpamTrashSetting(mbox, includeTrash, includeSpam);
    }

    private boolean mHasSpamTrashSetting = false;
    
    int getOpType() {
        return OP_TYPE_LUCENE;
    }
    
    boolean hasSpamTrashSetting() {
        return mHasSpamTrashSetting;
    }
    boolean hasNoResults() {
        return false;
    }
    boolean hasAllResults() {
        return false;
    }
    void forceHasSpamTrashSetting() {
        mHasSpamTrashSetting = true;
    }
    
    public void doneWithSearchResults() throws ServiceException {
        if (mSearcher != null) {
            mSearcher.release();
        }
    };
    
    /**
     * Called by our DBOp to reset our iterator...
     */
    protected void resetDocNum() {
        mCurHitNo = 0;
    }
    
    protected int[] getNextIndexedIdChunk(int maxChunkSize) throws ServiceException {
        try {
            int numLeft = mLuceneHits.length() - mCurHitNo;
            int numToReturn = Math.min(numLeft, maxChunkSize);
            int[] toRet = new int[numToReturn];
            for (int i = 0; i < numToReturn; i++) {
                Document d = mLuceneHits.doc(mCurHitNo++);
                
                String mbid = d.get(LuceneFields.L_MAILBOX_BLOB_ID);
                try {
                    if (mbid != null) {
                        toRet[i] = Integer.parseInt(mbid);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            
            return toRet;
            
        } catch (IOException e) {
            throw ServiceException.FAILURE("Caught IOException getting lucene results", e);
        }
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
    
    protected BooleanQuery mQuery;
    
    static LuceneQueryOperation Create() {
        LuceneQueryOperation toRet = new LuceneQueryOperation();
        toRet.mQuery = new BooleanQuery();
        return toRet;
    }
    
    // used only by the AllQueryOperation subclass....
    protected LuceneQueryOperation()
    {
    }
    
    protected void prepare(Mailbox mbx, ZimbraQueryResultsImpl res, MailboxIndex mbidx) throws ServiceException, IOException
    {
        if (mDBOp == null) {
            // wrap ourselves in a DBQueryOperation, since we're eventually going to need to go to the DB
            mDBOp = DBQueryOperation.Create();
            mDBOp.addLuceneOp(this);
            mDBOp.prepare(mbx, res, mbidx); // will call back into this function again!
        } else {
            this.setupResults(mbx, res);
            
            try {
                mSearcher = mbidx.getCountedIndexSearcher();
                
                if (mQuery != null) {
                    
                    BooleanQuery outerQuery = new BooleanQuery();
                    outerQuery.add(new BooleanClause(new TermQuery(new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE)), true, false));
                    outerQuery.add(new BooleanClause(mQuery, true, false));
                    mLuceneHits = mSearcher.getSearcher().search(outerQuery, mbidx.getSort(res.getSearchOrder()));
                } else {
                    assert(false);
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (mSearcher != null) {
                    mSearcher.release();
                    mSearcher = null;
                }
                mLuceneHits = null;
            }
        }
    }
    
    QueryOperation optimize(Mailbox mbox) throws ServiceException {
        return this;
    }
    
    public String toString()
    {
        return "LUCENE(" + mQuery.toString() + ")";
    }
    
    
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        
        if (union) {
            if (other.hasNoResults()) {
                // a query for (other OR nothing) == other
                return this;
            }
        } else {
            if (other.hasAllResults()) {
                if (other.hasSpamTrashSetting()) {
                    forceHasSpamTrashSetting();
                }
                // we match all results.  (other AND anything) == other
                return this;
            }
        }
        
        if (other instanceof LuceneQueryOperation) {
            BooleanQuery top = new BooleanQuery();
            BooleanClause lhs = new BooleanClause(mQuery, !union, false);
            BooleanClause rhs = new BooleanClause(((LuceneQueryOperation)other).mQuery, !union, false);
            top.add(lhs);
            top.add(rhs);
            mQuery = top;
            return this;
        }
        return null;
    }
    
    protected int inheritedGetExecutionCost()
    {
        return 20;
    }
    
    void addClause(Query q, boolean truth) {
        if (truth) {
            mQuery.add(new BooleanClause(q, true, false));
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
            mQuery.add(new BooleanClause(new TermQuery(new Term(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_TOP)),true,false));
//            mQuery.add(new BooleanClause(new TermQuery(new Term(LuceneFields.L_ALL, LuceneFields.L_ALL_VALUE)), true, false));
            mQuery.add(new BooleanClause(q, false, true));
        }
    }
}