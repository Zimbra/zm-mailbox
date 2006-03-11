/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Oct 29, 2004
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbSearchConstraints;
import com.zimbra.cs.db.DbMailItem.SearchResult;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.db.DbSearchConstraints.Range;
import com.zimbra.cs.index.LuceneQueryOperation.LuceneResultsChunk;
import com.zimbra.cs.index.LuceneQueryOperation.LuceneResultsChunk.ScoredLuceneHit;
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.ServiceException;


/************************************************************************
 * 
 * DBQueryOperation
 * 
 ***********************************************************************/
class DBQueryOperation extends QueryOperation
{
    private Folder mFolder = null;
    
    private static Log mLog = LogFactory.getLog(DBQueryOperation.class);

    // this gets set to FALSE if we have any real work to do
    // this lets us optimize away queries that might match "everything"
    private boolean mAllResultsQuery = true;
    private boolean mNoResultsQuery = false;
    
    private HashSet<Folder> mExcludeFolder = new HashSet<Folder>();
    
    private HashSet<Tag> mIncludeTags = new HashSet<Tag>();
    private HashSet<Tag> mExcludeTags = new HashSet<Tag>();
    
    private int mConvId = 0;
    private HashSet<Integer> mProhibitedConvIds = new HashSet<Integer>();
    
    private HashSet<DbSearchConstraints.Range>mDates = new HashSet<DbSearchConstraints.Range>();
    private HashSet<DbSearchConstraints.Range> mSizes = new HashSet<DbSearchConstraints.Range>();
    
    private HashSet<Integer> mIncludedItemIds = null; // CAREFUL!  In this case NULL means "no constraint" -- which is very different from an empty set (which means NOTHING MATCHES)
    private HashSet<Integer> mExcludedItemIds = null; 
    
    // true if we have a SETTING pertaining to Spam/Trash.  This doesn't necessarily
    // mean we actually have "in trash" or something, it just means that we've got something
    // set which means we shouldn't add the default "not in:trash and not in:junk" thing.
    private boolean mHasSpamTrashSetting = false;
    
    /**
     * An attached Lucene constraint
     */
    private LuceneQueryOperation mLuceneOp = null;

    /**
     * The current "chunk" of lucene results we are working through -- we need to keep it around
     * so that we can look up the scores of hits that match the DB
     */
    private LuceneQueryOperation.LuceneResultsChunk mLuceneChunk = null;
    
    
    /**
     * In an INTERSECTION, we can gain some efficiencies by using the output of the Lucene op
     * as parameters to our SearchConstraints....we do that by taking over the lucene op
     *(it is removed from the enclosing Intersection) and handling it internally.
     *
     * @param op
     */
    void addLuceneOp(LuceneQueryOperation op) {
        mAllResultsQuery = false;
        mLuceneOp = op;
    }
    
    int getOpType() {
        return OP_TYPE_DB;
    }

    boolean hasSpamTrashSetting() {
        return mHasSpamTrashSetting;
    }
    boolean hasNoResults() {
        return mNoResultsQuery;
    }
    boolean hasAllResults() {
        return mAllResultsQuery;
    }
    void forceHasSpamTrashSetting() {
        mHasSpamTrashSetting = true;
    }
    
    QueryTarget getQueryTarget() {
    	if (mFolder != null)  
    		return new QueryTarget(mFolder);
    	else 
    		return null;
    }
    
    /**
     * Since Trash can be an entire folder hierarchy, when we want to exclude trash from a query,
     * we actually have to walk that hierarchy and figure out all the folders within it.
     * 
     * @param mbox
     * @return List of Folders which are in Trash, including Trash itself
     * @throws ServiceException
     */
    private List /* Folder */ getTrashFolders(Mailbox mbox) throws ServiceException {
        return mbox.getFolderById(null, Mailbox.ID_FOLDER_TRASH).getSubfolderHierarchy(); 
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#ensureSpamTrashSetting(com.zimbra.cs.mailbox.Mailbox)
     */
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException
    {
        if (!mHasSpamTrashSetting) {
            if (!includeSpam) {
                Folder spam = mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM);            
                addInClause(spam, false);
            }
            
            if (!includeTrash) {
                List trashFolders = getTrashFolders(mbox);
                for (Iterator iter  = trashFolders.iterator(); iter.hasNext();) {
                    Folder cur = (Folder)(iter.next());
                    addInClause(cur,false);
                }
            }
            mHasSpamTrashSetting = true;
        }
        return this;
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#doneWithSearchResults()
     */
    public void doneWithSearchResults() throws ServiceException {
        if (mLuceneOp != null) {
            mLuceneOp.doneWithSearchResults();
        }
    };
    
    static DBQueryOperation Create() { return new DBQueryOperation(); }
    
    protected DBQueryOperation() { }
    
    void addItemIdClause(Integer itemId, boolean truth) {
        mAllResultsQuery = false;
        if (truth) {
            if (mIncludedItemIds == null) {
                mIncludedItemIds = new HashSet<Integer>();
            }
            if (!mIncludedItemIds.contains(itemId)) {
                //
                // {1} AND {-1} AND {1} == no-results 
                //  ... NOT {1} (which could happen if you removed from both arrays on combining!)
                if (mExcludedItemIds == null || !mExcludedItemIds.contains(itemId)) {
                    mIncludedItemIds.add(itemId);
                }
            }
        } else {
            if (mExcludedItemIds == null) {
                mExcludedItemIds = new HashSet<Integer>();
            }
            if (!mExcludedItemIds.contains(itemId)) {
                //
                // {1} AND {-1} AND {1} == no-results 
                //  ... NOT {1} (which could happen if you removed from both arrays on combining!)
                if (mIncludedItemIds != null && mIncludedItemIds.contains(itemId)) {
                    mIncludedItemIds.remove(itemId);
                }
                mExcludedItemIds.add(itemId);
            }
        }
    }
    
    /**
     * @param lowest
     * @param highest
     * @param truth
     * @throws ServiceException
     */
    void addDateClause(long lowestDate, long highestDate, boolean truth)  {
        mAllResultsQuery = false;
        DbSearchConstraints.Range intv = new DbSearchConstraints.Range();
        intv.lowest = lowestDate;
        intv.highest = highestDate;
        intv.negated = !truth;
        
        this.mDates.add(intv);
    }
    
    /**
     * @param lowest
     * @param highest
     * @param truth
     * @throws ServiceException
     */
    void addSizeClause(long lowestSize, long highestSize, boolean truth)  {
        mAllResultsQuery = false;
        DbSearchConstraints.Range intv = new DbSearchConstraints.Range();
        intv.lowest = lowestSize;
        intv.highest = highestSize;
        intv.negated = !truth;
        
        this.mSizes.add(intv);
    }
    
    
    /**
     * @param convId
     * @param prohibited
     */
    void addConvId(int convId, boolean truth) {
        mAllResultsQuery = false;
        Integer cid = new Integer(convId);
        if (truth) {
            if (mProhibitedConvIds.contains(cid)) {
                mNoResultsQuery = true;
            }
            
            if (mConvId == 0) {
                mConvId = convId;
            } else {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("Query requested two conflicting convIDs, this is now a no-results-query");
                }
                mConvId = Integer.MAX_VALUE;
                mNoResultsQuery = true;
            }
        } else {
            if (convId == mConvId) {
                mNoResultsQuery = true;
            }
            mProhibitedConvIds.add(cid);
        }
    }
    
    /**
     * @param folder
     * @param truth
     */
    void addInClause(Folder folder, boolean truth) 
    {
        mAllResultsQuery = false;
        if (truth) {
            if ((mFolder != null && mFolder != folder) || (mExcludeFolder.contains(folder))) {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("AND of conflicting folders, no-results-query");
                }
                mNoResultsQuery = true;
            }
            mFolder = folder;
            mHasSpamTrashSetting = true;
        } else {
            if (mFolder == folder) {
                if (mLog.isDebugEnabled()) {
                    mLog.debug("AND of conflicting folders, no-results-query");
                }
                mNoResultsQuery = true;
            }
            mExcludeFolder.add(folder);

            int fid = folder.getId();
            if (fid == Mailbox.ID_FOLDER_TRASH || fid == Mailbox.ID_FOLDER_SPAM) {
                mHasSpamTrashSetting = true;
            }
        }
    }
    
    void addAnyFolderClause(boolean truth) {
        // support for "is:anywhere" basically as a way to get around
        // the trash/spam autosetting
        mHasSpamTrashSetting = true;
        if (!truth) {
            // if they are weird enough to say "NOT is:anywhere" then we
            // just make it a no-results-query.
            
            if (mLog.isDebugEnabled()) {
                mLog.debug("addAnyFolderClause(FALSE) called -- changing to no-results-query.");
            }
            mNoResultsQuery = true;
            mAllResultsQuery = false;
        }
    }
    
    /**
     * @param tag
     * @param truth
     */
    void addTagClause(Tag tag, boolean truth) {
        mAllResultsQuery = false;
        if (mLog.isDebugEnabled()) {
            mLog.debug("AddTagClause("+tag+","+truth+")");
        }
        if (truth) {
            if (mExcludeTags.contains(tag)) {
                mNoResultsQuery = true;
            }
            mIncludeTags.add(tag);
        } else {
            if (mIncludeTags.contains(tag)) {
                mNoResultsQuery = true;
            }
            mExcludeTags.add(tag);
        }
    }
    
    private Collection <SearchResult> mDBHits;
    private Iterator mDBHitsIter;
    private int mCurHitsOffset = 0; // -1 means "no more results"
    private boolean atStart = true; // don't re-fill buffer twice if they call hasNext() then reset() w/o actually getting next
    
    private int mHitsPerChunk = 100;
    private static final int MAX_HITS_PER_CHUNK = 2000;
    
    private static class ScoredDBHit implements Comparable {
    	public SearchResult mSr;
    	public float mScore;
    	
    	ScoredDBHit(SearchResult sr, float score) {
    		mSr = sr;
    		mScore = score;
    	}
    	
    	long scoreAsLong() { 
    		return (long)(mScore * 10000);
    	}
    	
    	public int compareTo(Object o) {
    		ScoredDBHit other = (ScoredDBHit)o;

    		long mys = scoreAsLong();
    		long os = other.scoreAsLong();
    		
    		if (mys == os) 
    			return mSr.id - other.mSr.id;
    		else {
    			long l = os - mys;
    			if (l > 0) 
    				return 1;
    			else if (l < 0)
    				return -1;
    			else return 0;
    		}
    	}
    	
    	public boolean equals(Object o) {
    		return (o==this) || (compareTo(o) == 0);
    	}
    }
    
    /******************
     * 
     * Hits iteration
     *
     *******************/    
    public void resetIterator() throws ServiceException {
        if (mLuceneOp != null) {
            mLuceneOp.resetDocNum();
        }
        mNextHits.clear();
        if (!atStart) {
            mDBHitsIter = null;
            mCurHitsOffset = 0;
            mEndOfHits = false;
            atStart = true;
        } else {
            if (mDBHits != null) {
                mDBHitsIter = mDBHits.iterator();
            }
        }
    }
    
    /*
     * Return the next hit in our search.  If there are no hits buffered
     * then calculate the next hit and put it into the mNextHits list.
     * 
     *   Step 1: Get the list of DbMailItem.SearchResults chunk-by-chunk 
     *           (50 or 100 or whatever at a time)
     *            
     *   Step 2: As we need them, grab the next SearchResult and build a
     *           real ZimbraHit out of them
     */
    public ZimbraHit peekNext() throws ServiceException
    {
    	ZimbraHit toRet = null;
        if (mNextHits.size() > 0) {
        	// already have some hits, so our job is easy!
        	toRet = (ZimbraHit)mNextHits.get(0);
        } else {
        	// we don't have any SearchResults
        if (mDBHitsIter == null || !mDBHitsIter.hasNext()) {
        		// try to get another chunk of them
            getNextChunk();
        }
        	    
        	// okay, check again to see if we've got a chunk of SearchResults
        if (mDBHitsIter != null && mDBHitsIter.hasNext()) {
            SearchResult sr = (SearchResult) mDBHitsIter.next();
            
        		// Sometimes, a single search result might yield more than one Lucene
        		// document -- e.g. an RFC822 message with separately-indexed mimeparts.
        		// Each of these parts will turn into a separate ZimbraHit at this point,
        		// although they might be combined together at a higher level (via a HitGrouper)
        		List <Document> docs = null;
            float score = 1.0f;
            if (mLuceneChunk != null) {
        			LuceneResultsChunk.ScoredLuceneHit sh = mLuceneChunk.getScoredHit(sr.indexId);
            	if (sh != null) { 
            		docs = sh.mDocs;
            		score = sh.mScore;
            	} else {
        				// how can this happen??
            		mLog.info("Could not find ScoredLuceneHit for sr.indexId="+sr.indexId+" sr.id="+sr.id+" type="+sr.type);
            		docs = null;
            		score = 1.0f;
            	}
            }
            
        		if (docs == null) {
        			ZimbraHit toAdd = getResultsSet().getZimbraHit(getMailbox(), score, sr, null);
                        mNextHits.add(toAdd);
                } else {
        			for (Document doc : docs) {
        				ZimbraHit toAdd = getResultsSet().getZimbraHit(getMailbox(), score, sr, doc);
                    mNextHits.add(toAdd);
                }
        		}
        			toRet = (ZimbraHit)mNextHits.get(0);
            }
        }
        
        return toRet;
    }
    
    private List<ZimbraHit>mNextHits = new ArrayList<ZimbraHit>();
    
    
    public ZimbraHit getNext() throws ServiceException {
        atStart = false;
        if (mNextHits.size() == 0) {
            peekNext();
        }
        if (mNextHits.size() == 0) {
            return null;
        }
        ZimbraHit toRet = (ZimbraHit)mNextHits.remove(0);
        return toRet;
    }
    
    /**
     * @param types
     */
    private byte[] convertTypesToDbQueryTypes(byte[] types) 
    {
        // hackery
        int numUsed = 0;
        byte[] tmp = new byte[2*types.length]; // boy I love java - no resizable array holds native types
        
        for (int i = 0; i < types.length; i++) {
            if (types[i] == 0) {
                types = null;
                break;
            }
            switch(types[i]) {
            case 0:
                return null;
            case MailItem.TYPE_FOLDER:
            case MailItem.TYPE_SEARCHFOLDER:
            case MailItem.TYPE_TAG:
                tmp[numUsed] = MailItem.TYPE_UNKNOWN;
                numUsed++;
                break;
            case MailItem.TYPE_CONVERSATION:
                tmp[numUsed] = MailItem.TYPE_MESSAGE;
                numUsed++;
                break;
            case MailItem.TYPE_MESSAGE:
                tmp[numUsed] = MailItem.TYPE_MESSAGE;
                numUsed++;
                break;
            case MailItem.TYPE_CONTACT:
                tmp[numUsed] = MailItem.TYPE_CONTACT;
                numUsed++;
                break;
            case MailItem.TYPE_APPOINTMENT:
                tmp[numUsed] = MailItem.TYPE_APPOINTMENT;
                numUsed++;
                break;
            case MailItem.TYPE_DOCUMENT:
                tmp[numUsed] = MailItem.TYPE_DOCUMENT;
                numUsed++;
                break;
            case MailItem.TYPE_NOTE:
                tmp[numUsed] = MailItem.TYPE_NOTE;
                numUsed++;
                break;
            case MailItem.TYPE_FLAG:
                tmp[numUsed] = MailItem.TYPE_FLAG;
                numUsed++;
                break;
            case MailItem.TYPE_WIKI:
                tmp[numUsed] = MailItem.TYPE_WIKI;
                numUsed++;
                break;
            }
        }
        
        byte[] toRet = new byte[numUsed];
        System.arraycopy(tmp,0,toRet,0,numUsed);
        
        return toRet;
    }
    
    /**
     * TRUE if we know there are no more hits to get for mDBHitsIter 
     *   -- ie there is no need to call getChunk() anymore
     */
    private boolean mEndOfHits = false;
    
    private HashSet<Byte> mTypes = new HashSet<Byte>();
    private HashSet<Byte>mExcludeTypes = new HashSet<Byte>();
    
    private Collection<Byte> getDbQueryTypes() 
    {
    	byte[] defTypes = convertTypesToDbQueryTypes(this.getResultsSet().getTypes());
    	ArrayList<Byte> toRet = new ArrayList<Byte>();
    	for (Byte b : defTypes)
    		toRet.add(b);
    	
        if (mTypes.size() > 0) {
        	for (Byte b : mTypes)
        		if (!toRet.contains(b))
        			toRet.add(b);
        }
    	return toRet;
    }
    
    void addTypeClause(byte type, boolean truth) {
        mAllResultsQuery = false;
        if (truth) {
        	if (!mTypes.contains(type))
        		mTypes.add(type);
        } else {
        	if (!mExcludeTypes.contains(type))
        		mExcludeTypes.add(type);
        }
    }
    
    /**
     * Build a DbMailIte.SearchConstraints given all of the constraint parameters we have.
     *
     * @return
     */
    private DbSearchConstraints getSearchConstraints() {
        SortBy searchOrder = this.getResultsSet().getSortBy();
        Collection<Byte> types = getDbQueryTypes();
        if (types.size() == 0)  {
            mLog.debug("NO RESULTS -- no known types requested");
            return null;
        } else { 
            DbSearchConstraints c = new DbSearchConstraints();
            c.mailboxId = getMailbox().getId();
        
            // tags
            if (mIncludeTags.size() > 0) {
            	c.tags = mIncludeTags;
//            	c.tags = (Tag[]) mIncludeTags.toArray(new Tag[mIncludeTags.size()]);
            	
            	
        }
        
            // exclude-tags
            if (mExcludeTags.size() > 0) {
            	c.excludeTags = mExcludeTags;
            }

            // folders
                if (mFolder != null) {
                	c.folders = new ArrayList<Folder>(1);
                	c.folders.add(mFolder);
                } 
                
            // exclude-folders
                if (mExcludeFolder.size() > 0) {
                	c.excludeFolders = mExcludeFolder;
                }
                
            c.convId = mConvId;
                    
            // prohibited-conv-ids
                    if (mProhibitedConvIds.size() > 0) {
                    	c.prohibitedConvIds = mProhibitedConvIds;
                    }
                    
                    c.types = types;
                    c.sort = searchOrder.getDbMailItemSortByte(); 
                    
                    if (mExcludeTypes.size() > 0) {
                        c.excludeTypes = mExcludeTypes;
                    }
                    
                    if (mIncludedItemIds != null) {
                    	c.itemIds = mIncludedItemIds;
                    }
                    if (mExcludedItemIds != null && mExcludedItemIds.size() > 0) {
                        c.prohibitedItemIds = mExcludedItemIds;
                    }
                    
                    if (mDates.size() > 0) {
                    	c.dates = mDates;
                    }
                    
                    if (mSizes.size() > 0) {
                    	c.sizes = mSizes;
                    }
            return c;
        }
    }
    
    private boolean mFailedDbFirst = false;
    
    /**
     * The maximum DB results we will allow when running a DB-FIRST query: if there 
     * are more than this many results we switch over to LUCENE-FIRST.
     * 
     * This limitation is necessary so that SCORE sorting is possible -- for score sorting
     * to work, we have to get *all* of the results from the DB and pass them to lucene
     * at the same time (since they don't come back in score-order from the DB) -- and
     * we don't want fetch huge numbers of results from the DB.
     * 
     * This number *must* be smaller than the value of LuceneQuery.maxClauseCount 
     * (LuceneQuery.getMaxClauseCount())
     * 
     */
    private static final int MAX_DBFIRST_RESULTS = 1000;
    
    private boolean tryDbFirst() {
    	if (!mFailedDbFirst) {
    		Mailbox mbox = getMailbox();
    		
    		if (mConvId != 0 || (mIncludeTags!=null && mIncludeTags.contains(mbox.mUnreadFlag))) 
    		{
    			return true;
    		}
    	}
    	return false;
    }
                    
    /**
     * Use all the search parameters (including the embedded LuceneQueryOperation) to
     * get a chunk of SearchResults.
     * 
     * @throws ServiceException
     */
    private void getNextChunk() throws ServiceException
    {
        if (mCurHitsOffset == -1 || mEndOfHits) {
            return;
        }
        if (mIncludedItemIds != null && mIncludedItemIds.size() == 0) {
            mNoResultsQuery = true;
        }
        
        if (!mNoResultsQuery) {
            Connection conn = null;
            try {
                conn = DbPool.getConnection();
                
                DbSearchConstraints c = getSearchConstraints();
                if (c == null) {
                    mDBHitsIter = null;
                    mCurHitsOffset = -1;
                    return;
                } else {
                    if (mLuceneOp == null) {
                    	// no lucene op, this is easy
                        c.offset = mCurHitsOffset;
                        c.limit = mHitsPerChunk;
                        
                        boolean getFullRows = this.isTopLevelQueryOp();
                        mDBHits = DbMailItem.search(conn, c, getFullRows);
                    } else {
                    	boolean success = false;
                        
                    	////////////////////
                    	//
                    	// Possibly try doing a DB-FIRST query here
                    	//
                    	if (tryDbFirst()) {
                    		assert(c.offset == -1);
                    		assert(c.limit == -1);
                    		assert(MAX_DBFIRST_RESULTS < BooleanQuery.getMaxClauseCount());
                    		
                    		mLog.debug("Attempting a DB-FIRST execution");
                    		// do DB op first, pass results to Lucene op
                            c.offset = 0;
                            c.limit = MAX_DBFIRST_RESULTS;
                            
                            boolean getFullRows = this.isTopLevelQueryOp();
                            Collection<SearchResult> dbRes = DbMailItem.search(conn, c, getFullRows);
                            
                            if (dbRes.size() == MAX_DBFIRST_RESULTS) {
                            	mLog.info("FAILED DB-FIRST: Too many results");
                            	mFailedDbFirst = true;
                            	
                            	// reset the offset/limit to what they should be (we're going to 
                            	// fall through below)
                            	c.offset = -1;
                            	c.offset = -1;
                            }
                            
                            if (!mFailedDbFirst) {
                            	BooleanQuery idsQuery = new BooleanQuery();
                            	
                            	// maps an IndexID to a list of SearchResults
                            	HashMap<Integer, List<SearchResult>> mailItemToResultsMap = new HashMap<Integer, List<SearchResult>>();
                            	
                            	for (SearchResult res : dbRes) {
                            		List<SearchResult> l = mailItemToResultsMap.get(res.indexId);
                            		if (l == null) {
                            			l = new LinkedList<SearchResult>();
                            			mailItemToResultsMap.put(res.indexId, l);
                            		}
                            		l.add(res);
                            		
                            		idsQuery.add(new TermQuery(new Term(LuceneFields.L_MAILBOX_BLOB_ID, Integer.toString(res.indexId))), false, false);
                            	}
                            	mLuceneOp.addAndedClause(idsQuery, true);
                            	
                            	mLuceneChunk = mLuceneOp.getNextResultsChunk(dbRes.size());
                            	
                            	mDBHits = new ArrayList<SearchResult>(dbRes.size());
                            	
                            	Collection<Integer> indexIds = mLuceneChunk.getIndexIds();
                            	for (int indexId : indexIds) {
                            		List<SearchResult> l = mailItemToResultsMap.get(indexId);
                            		
                            		for (SearchResult sr : l) {
                            			mDBHits.add(sr);
                            		}
                            	}
                            	
                            	// DB-first queries always calculate all the DB hits in one operation....
                            	// we have to do it that way or else we cannot support Score searches (since
                            	// we can't sort DB-results by score-order)
                            	mEndOfHits = true;                    		
                            	
                            	success = true; 
                            }
                    	}
                    		
                    	////////////////////
                    	//
                    	// If we haven't run anything yet (b/c DB-FIRST wasn't called-for,
                    	// or b/c DB-FIRST ran and failed) then do a LUCENE-FIRST query
                    	//
                    	if (!success) {
                    		mLog.debug("Running a LUCENE-FIRST execution");
                    		// do the Lucene op first, pass results to DB op
                        do {
                            // DON'T set an sql LIMIT if we're asking for lucene hits!!!  If we did, then we wouldn't be
                            // sure that we'd "consumed" all the Lucene-ID's, and therefore we could miss hits!
                    			mLuceneChunk = mLuceneOp.getNextResultsChunk(mHitsPerChunk);
                            c.indexIds = mLuceneChunk.getIndexIds();
                            
                            // exponentially expand the chunk size in case we have to go back to the DB
                            mHitsPerChunk*=2;
                            if (mHitsPerChunk > MAX_HITS_PER_CHUNK) {
                                mHitsPerChunk = MAX_HITS_PER_CHUNK;
                            }
                            
                            if (c.indexIds.size() == 0) {
                                // we know we got all the index-id's from lucene.  since we don't have a
                                // LIMIT clause, we can be assured that this query will get all the remaining results.
                                mEndOfHits = true;
                                
                                mDBHits = new ArrayList<SearchResult>(); 
                            } else {
                            	mDBHits = DbMailItem.search(conn, c);
                            	
                    				if (getSortBy() == SortBy.SCORE_DESCENDING) {
                    					// We have to re-sort the chunk by score here b/c the DB doesn't
                    					// know about scores
                            		ScoredDBHit[] scHits = new ScoredDBHit[mDBHits.size()];
                            		int offset = 0;
                            		for (SearchResult sr : mDBHits) {
                            			ScoredLuceneHit lucScore = mLuceneChunk.getScoredHit(sr.indexId);
                            			
                            			scHits[offset++] = new ScoredDBHit(sr, lucScore.mScore);
                            		}
                            		
                            		Arrays.sort(scHits);
                            		
                            		mDBHits = new ArrayList<SearchResult>(scHits.length);
                            		for (ScoredDBHit sdbHit : scHits)
                            			mDBHits.add(sdbHit.mSr);
                            	}
                            	
                            }
                        } while (mDBHits.size() == 0 && !mEndOfHits);
                    }
                    }
                } // if types.length check...
                
            } catch (ServiceException e) {
                e.printStackTrace();
                throw e;
            } finally {
                DbPool.quietClose(conn);
            }
            
            if (mLog.isDebugEnabled()) {
                mLog.debug(this.toString()+" Returned "+mDBHits.size()+" results ("+mCurHitsOffset+")");
            }
        } else {
            if (mLog.isDebugEnabled()) {
                mLog.debug(" Returned **NO DB RESULTS (no-results-query-optimization)**");
            }
            mDBHitsIter = null;
            mCurHitsOffset = -1;
            return;
        }

        if (mDBHits.size() == 0) {
            mCurHitsOffset = -1;
            mDBHitsIter = null;
            mDBHits = null;
        } else {
            if (mLuceneOp == null && mDBHits.size() < mHitsPerChunk) {
                mEndOfHits = true;
            }
            mCurHitsOffset += mDBHits.size();
            mDBHitsIter = mDBHits.iterator();
            
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#prepare(com.zimbra.cs.mailbox.Mailbox, com.zimbra.cs.index.ZimbraQueryResultsImpl, com.zimbra.cs.index.MailboxIndex)
     */
    protected void prepare(Mailbox mbx, ZimbraQueryResultsImpl res, MailboxIndex mbidx, int chunkSize) throws ServiceException, IOException
    {
        if (chunkSize > MAX_HITS_PER_CHUNK) {
            chunkSize = MAX_HITS_PER_CHUNK;
        }
        
        mHitsPerChunk = chunkSize;
        
        setupResults(mbx, res);
        
        if (mLuceneOp != null) {
            mHitsPerChunk *= 2; // enlarge chunk size b/c of join
            mLuceneOp.setDBOperation(this);
            mLuceneOp.prepare(mbx, res, mbidx, mHitsPerChunk);
        }
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#optimize(com.zimbra.cs.mailbox.Mailbox)
     */
    QueryOperation optimize(Mailbox mbox) throws ServiceException {
        return this;
    }
    
    public String toString()
    {
        StringBuffer retVal = new StringBuffer("");
        if (mLuceneOp != null) {
            retVal.append(" (").append(mLuceneOp.toString()).append(" AND ");
        }
        retVal.append("DB(");
        if (mAllResultsQuery) {
            retVal.append("ANYWHERE ");
        }
        if (mFolder != null) {
            retVal.append("IN:"+mFolder.getName());
            retVal.append(' ');
        }
        if (mExcludeFolder.size() > 0) {
            for (Iterator iter = mExcludeFolder.iterator(); iter.hasNext();) 
            {
                retVal.append("-IN:");
                retVal.append(((Folder)iter.next()).getName());
                retVal.append(' ');
            }
        }
        
        for (Iterator iter = mIncludeTags.iterator(); iter.hasNext();) {
            Tag cur = (Tag)iter.next(); 
            retVal.append(" TAG:"+cur.getName());
            retVal.append(' ');
        }
        
        for (Iterator iter = mExcludeTags.iterator(); iter.hasNext();) {
            Tag cur = (Tag)iter.next(); 
            retVal.append("-TAG:"+cur.getName());
            retVal.append(' ');
        }
        
        if (mIncludedItemIds != null) {
            for (Iterator iter = mIncludedItemIds.iterator(); iter.hasNext();) {
                Integer cur = (Integer)(iter.next());
                retVal.append("ITEM:"+cur+" ");
            }
        }
        if (mExcludedItemIds != null) {
            for (Iterator iter = mExcludedItemIds.iterator(); iter.hasNext();) {
                Integer cur = (Integer)(iter.next());
                retVal.append("-ITEM:"+cur+" ");
            }
        }
        
        if (mConvId != 0) {
            retVal.append("CONV:"+mConvId+" ");
        }
        
        for (Iterator iter = mProhibitedConvIds.iterator(); iter.hasNext();) {
            Integer cur = (Integer)(iter.next());
            retVal.append("-CONV:"+cur+" ");
        }

        
        for (Iterator iter = mDates.iterator(); iter.hasNext();) {
            DbSearchConstraints.Range intv = (DbSearchConstraints.Range)(iter.next());
            if (intv.negated) {
                retVal.append("NOT (");
            }
            
            if (intv.lowest > -1) {
                retVal.append("AFTER:");
                retVal.append((new Date(intv.lowest)).toString());
                retVal.append(' ');
            }
            
            if (intv.highest > -1) {
                retVal.append("BEFORE:");
                retVal.append((new Date(intv.highest)).toString());
                retVal.append(' ');
            }
            if (intv.negated) {
                retVal.append(")");
            }
            
        }
        
        if (mNoResultsQuery) {
            retVal.append("--- NO RESULT ---");
        }
        retVal.append(")");
        if (mLuceneOp != null) {
            retVal.append(") ");
        }
        
        return retVal.toString();
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#combineOps(com.zimbra.cs.index.QueryOperation, boolean)  
     */
    protected QueryOperation combineOps(QueryOperation other, boolean union) 
    {
        if (union) {
            // only join on intersection right now...
            if (mNoResultsQuery) {
                // a query for (other OR nothing) == other
                return other;
            }
            return null;
        } else {
            if (mAllResultsQuery) {
                // we match all results.  (other AND anything) == other
                
                assert(mLuceneOp == null);
                if (hasSpamTrashSetting()) {
                    other.forceHasSpamTrashSetting();
                }
                return other;
            }
        }
        
        DBQueryOperation dbOther = null;
        
        if (other instanceof DBQueryOperation) {
            dbOther = (DBQueryOperation)other;
        } else {
            return null;
        }

        DBQueryOperation toRet = new DBQueryOperation();
        
        if (mLuceneOp != null) {
            toRet.mLuceneOp = mLuceneOp;
            
            if (dbOther.mLuceneOp != null) {
                toRet.mLuceneOp.combineOps(dbOther.mLuceneOp, false);
            }
        } else {
            toRet.mLuceneOp = dbOther.mLuceneOp;
        }
        
        if (mHasSpamTrashSetting || dbOther.mHasSpamTrashSetting) {
            toRet.mHasSpamTrashSetting = true;
        } else {
            toRet.mHasSpamTrashSetting = false;
        }
        
        if (mAllResultsQuery && dbOther.mAllResultsQuery) {
            toRet.mAllResultsQuery = true;
        } else {
            toRet.mAllResultsQuery = false;
        }
        
        if (mNoResultsQuery || dbOther.mNoResultsQuery) {
            toRet.mNoResultsQuery = true;
            return toRet;
        }

        { // dates
            toRet.mDates = (HashSet<Range>)mDates.clone();
            for (Iterator iter = dbOther.mDates.iterator(); iter.hasNext();) {
                DbSearchConstraints.Range r = (DbSearchConstraints.Range)iter.next();
                toRet.addDateClause(r.lowest, r.highest, !r.negated);
            }
        }
        
        { // sizes
            toRet.mSizes = (HashSet<Range>)mSizes.clone();
            for (Iterator iter = dbOther.mSizes.iterator(); iter.hasNext();) {
                DbSearchConstraints.Range r = (DbSearchConstraints.Range)iter.next();
                toRet.addSizeClause(r.lowest, r.highest, !r.negated);
            }
        }
        
        {   // item-id's are INTERSECTED because they are SETS (ie "one of...") instead of
            // additive required parameters (tags="UNSEEN and TAG1...")
            
            
            // these we have to intersect:
            //
            //   Item{A or B or C} AND Item{B or C or D} --> Item{IN-BOTH}
            //
            if (mIncludedItemIds == null) {
                toRet.mIncludedItemIds = dbOther.mIncludedItemIds;
            } else if (dbOther.mIncludedItemIds == null) {
                toRet.mIncludedItemIds = mIncludedItemIds;
            } else {
                toRet.mIncludedItemIds = new HashSet<Integer>(); // start out with EMPTY SET (no results!)
                for (Iterator iter = mIncludedItemIds.iterator(); iter.hasNext();)
                {
                    Integer i = (Integer)(iter.next());
                    if (dbOther.mIncludedItemIds.contains(i)) {
                        toRet.addItemIdClause(i, true);
                    }
                }
            }

            //
            // these we can just combine, since:
            //
            // -Item{A or B} AND -Item{C or D} --> 
            //   (-Item(A) AND -Item(B)) AND (-C AND -D) -->
            //     (A AND B AND C AND D)
            //
            if (mExcludedItemIds == null) {
                toRet.mExcludedItemIds = dbOther.mExcludedItemIds;
            } else if (dbOther.mExcludedItemIds == null) {
                toRet.mExcludedItemIds = mExcludedItemIds;
            } else {
                toRet.mExcludedItemIds = (HashSet)mExcludedItemIds.clone();
                
                for (Iterator iter = dbOther.mExcludedItemIds.iterator(); iter.hasNext();)
                {
                    Integer i = (Integer)(iter.next());
                    toRet.addItemIdClause(i, false);
                }
            }
        }
     

        { // conversation id's
            toRet.mProhibitedConvIds = (HashSet)mProhibitedConvIds.clone();
            for (Iterator iter = dbOther.mProhibitedConvIds.iterator(); iter.hasNext();)
            {
                Integer i = (Integer)(iter.next());
                toRet.addConvId(i.intValue(), false);
            }
            
            if (mConvId != 0) {
                toRet.addConvId(mConvId, true);
            }
            if (dbOther.mConvId != 0) {
                toRet.addConvId(dbOther.mConvId, true);
            }
            
        }
        
        { // FOLDERS
            if (mFolder != null) {
                toRet.addInClause(mFolder, true);
            }
            if (dbOther.mFolder != null) {
                toRet.addInClause(dbOther.mFolder, true);
            }
            
            for (Iterator iter = mExcludeFolder.iterator(); iter.hasNext();)
            {
                Folder f = (Folder)(iter.next());
                toRet.addInClause(f, false);
            }
            
            for (Iterator iter = dbOther.mExcludeFolder.iterator(); iter.hasNext();)
            {
                Folder f = (Folder)(iter.next());
                toRet.addInClause(f, false);
            }
        }
        
        { // ...combine types...
//            toRet.mTypes = combineByteArraysNoDups(mTypes, dbOther.mTypes);
        	toRet.mTypes = (HashSet<Byte>)mTypes.clone();
        	toRet.mTypes.addAll(dbOther.mTypes);
        	
//          toRet.mExcludeTypes = combineByteArraysNoDups(mExcludeTypes, dbOther.mExcludeTypes);
        	toRet.mExcludeTypes = (HashSet<Byte>)mExcludeTypes.clone();
        	toRet.mExcludeTypes.addAll(dbOther.mExcludeTypes);
        }
        
        
        { // ...combine tags...
            toRet.mIncludeTags = (HashSet<Tag>)mIncludeTags.clone();
            for (Iterator iter = dbOther.mIncludeTags.iterator(); iter.hasNext();)
            {
                Tag t = (Tag)(iter.next());
                toRet.addTagClause(t, true);
            }
            
            for (Iterator iter = mExcludeTags.iterator(); iter.hasNext();)
            {
                Tag t = (Tag)(iter.next());
                toRet.addTagClause(t, false);
            }
            
            for (Iterator iter = dbOther.mExcludeTags.iterator(); iter.hasNext();)
            {
                Tag t = (Tag)(iter.next());
                toRet.addTagClause(t, false);
            }
        }
        
        
        return toRet;
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#inheritedGetExecutionCost()
     */
    protected int inheritedGetExecutionCost()
    {
//        return mExecCost;
        return (mLuceneOp != null ? 20 : 10);
    }        
    
}
