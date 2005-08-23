/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbMailItem.SearchConstraints;
import com.zimbra.cs.db.DbMailItem.SearchResult;
import com.zimbra.cs.db.DbPool.Connection;
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
    
    private HashSet mExcludeFolder = new HashSet();
    
    private boolean mNoResultsQuery = false;
    
    private HashSet mIncludeTags = new HashSet();
    private HashSet mExcludeTags = new HashSet();
    
    private int mConvId = 0;
    private HashSet mProhibitedConvIds = new HashSet();
    
    private HashSet /* DbMailItem.SearchConstraints.Range */mDates = new HashSet();
    
    private HashSet /* SearchConstraints.Range */ mSizes = new HashSet();
    
    
    private HashSet mIncludedItemIds = null; // CAREFUL!  NULL means "no constraint" -- which is very different from an empty set
    private HashSet mExcludedItemIds = null; // CAREFUL!  NULL means "no constraint" -- which is very different from an empty set

    // true if we have a SETTING pertaining to Spam/Trash.  This doesn't necessarily
    // mean we actually have "in trash" or something, it just means that we've got something
    // set which means we shouldn't add the default "not in:trash and not in:junk" thing.
    private boolean mHasSpamTrashSetting = false;
    
    private LuceneQueryOperation mLuceneOp = null;
    
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

    boolean hasSpamTrashSetting()
    {
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
    
    
    /**
     * Since Trash can be an entire folder hierarchy, when we want to exclude trash from a query,
     * we actually have to walk that hierarchy and figure out all the folders within it.
     * 
     * @param mbox
     * @return List of Folders which are in Trash, including Trash itself
     * @throws ServiceException
     */
    private AbstractList /* Folder */ getTrashFolders(Mailbox mbox) throws ServiceException {
        LinkedList retVal = new LinkedList();
        Folder trash = mbox.getFolderById(Mailbox.ID_FOLDER_TRASH); 
        retVal.add(trash);

        // 
        // breadth-first traversal
        //
        LinkedList /* List */ toCheck = new LinkedList();
        
        // start in "trash"
        List subFolders = trash.getSubfolders();
        if (subFolders != null) {
            toCheck.add(subFolders);
        }
        
        while(toCheck.size() > 0) 
        {
            // for every list of subfolders we see
            List cur = (List)(toCheck.removeFirst());
            for (Iterator iter = cur.iterator(); iter.hasNext();) 
            {
                // add the folder to our list of "in trash" folders
                Folder curFolder = (Folder)(iter.next());
                retVal.add(curFolder);
                
                // add the list of subfolders to our "toCheck" list
                subFolders = curFolder.getSubfolders();
                if (subFolders!=null && subFolders.size() > 0) {
                    toCheck.add(subFolders);
                }
            }
        }
        return retVal;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#ensureSpamTrashSetting(com.zimbra.cs.mailbox.Mailbox)
     */
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException
    {
        if (!mHasSpamTrashSetting) {
            if (!includeSpam) {
                Folder spam = mbox.getFolderById(Mailbox.ID_FOLDER_SPAM);            
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
                mIncludedItemIds = new HashSet();
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
                mExcludedItemIds = new HashSet();
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
        SearchConstraints.Range intv = new SearchConstraints.Range();
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
        SearchConstraints.Range intv = new SearchConstraints.Range();
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
    
    private Collection /* Integer blobid */ mDBHits;
    private Iterator mDBHitsIter;
    private int mCurHitsOffset = 0; // -1 means "no more results"
    private final int HITS_PER_CHUNK = 100;
    private boolean atStart = true; // don't re-fill buffer twice if they call hasNext() then reset() w/o actually getting next
    
    /******************
     * 
     * Hits iteration
     *
     *******************/    
    public void resetIterator() throws ServiceException {
        if (mLuceneOp != null) {
            mLuceneOp.resetDocNum();
        }
        mNextHit = null;
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
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#peekNext()
     */
    public ZimbraHit peekNext() throws ServiceException
    {
        if (mNextHit != null) {
            return mNextHit;
        }
        
        if (mDBHitsIter == null || !mDBHitsIter.hasNext()) {
            getNextChunk();
        }
        if (mDBHitsIter != null && mDBHitsIter.hasNext()) {
            SearchResult sr = (SearchResult) mDBHitsIter.next();
            ZimbraHit toRet = null;
            switch(sr.type) {
            case MailItem.TYPE_MESSAGE:
//            case MailItem.TYPE_INVITE:
                toRet = this.getResultsSet().getMessageHit(this.getMailbox(), new Integer(sr.id), null, 1.0f);
                break;
            case MailItem.TYPE_CONTACT:
                toRet = this.getResultsSet().getContactHit(this.getMailbox(), new Integer(sr.id), null, 1.0f);
                break;
            case MailItem.TYPE_NOTE:
                toRet = this.getResultsSet().getNoteHit(this.getMailbox(), new Integer(sr.id), null, 1.0f);
                break;
            // Unsupported right now:
            //            case MailItem.TYPE_DOCUMENT:
            default:
                assert(false);
                mNextHit = getNext();
                return mNextHit;
            }
            toRet.cacheSortField(this.getResultsSet().getSearchOrder(), sr.sortkey);
            mNextHit = toRet;
            return mNextHit;
        } else {
            return null;
        }
    }
    
    private ZimbraHit mNextHit = null;
    
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#getNext()
     */
    public ZimbraHit getNext() throws ServiceException {
        atStart = false;
        if (mNextHit == null) {
            peekNext();
        }
        ZimbraHit toRet = mNextHit;
        mNextHit = null;
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
//                tmp[numUsed] = MailItem.TYPE_INVITE;
//                numUsed++;
                break;
            case MailItem.TYPE_MESSAGE:
                tmp[numUsed] = MailItem.TYPE_MESSAGE;
                numUsed++;
//                tmp[numUsed] = MailItem.TYPE_INVITE;
//                numUsed++;
                break;
            case MailItem.TYPE_CONTACT:
                tmp[numUsed] = MailItem.TYPE_CONTACT;
                numUsed++;
                break;
//            case MailItem.TYPE_INVITE:
//                tmp[numUsed] = MailItem.TYPE_INVITE;
//                numUsed++;
//                break;
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
            }
        }
        
        byte[] toRet = new byte[numUsed];
        System.arraycopy(tmp,0,toRet,0,numUsed);
        
        return toRet;
    }
    
    private boolean mEndOfHits = false;
    
    private byte[] mTypes = new byte[0];
    private byte[] mExcludeTypes = new byte[0];
    
    private boolean byteArrayContains(byte[] array, byte val) {
        for (int i = array.length-1; i >= 0; i--) {
            if (array[i] == val) {
                return true;
            }
        }
        return false;
    }
    
    private byte[] combineByteArraysNoDups(byte[] a1, byte[] a2) {
        byte[] tmp = new byte[a2.length];
        int tmpOff = 0;
        
        for (int i = a2.length-1; i>=0; i--) {
            if (!byteArrayContains(a1, a2[i])) {
                tmp[tmpOff++] = a2[i];
            }
        }
        
        byte[] toRet = new byte[tmpOff + a1.length];
        
        System.arraycopy(tmp, 0, toRet, 0, tmpOff);
        System.arraycopy(a1, 0, toRet, tmpOff, a1.length);

        return toRet;
    }
    
    private byte[] addToByteArrayNoDup(byte[] array, byte val) {
        if (!byteArrayContains(array, val)) {
            byte[] toRet = new byte[array.length+1];
            System.arraycopy(array, 0, toRet, 0, array.length);
            toRet[toRet.length-1] = val;
            return toRet;
        }
        return array;
    }
    
    private byte[] intersectByteArrays(byte[] a1, byte[] a2) {
        byte[] tmp = new byte[a1.length + a2.length];
        int tmpOff = 0;
        
        for (int i = a1.length-1; i >=0; i--) {
            if (byteArrayContains(a2, a1[i])) {
                tmp[tmpOff++] = a1[i];
            }
        }

        byte[] toRet = new byte[tmpOff];
        for (int i = 0; i < tmpOff; i++) {
            toRet[i] = tmp[i];
        }
        
        // FIXME testing code only!
        for (int i = toRet.length-1; i>=0; i--) {
            assert(byteArrayContains(a1, toRet[i]) &&
                    byteArrayContains(a2, toRet[i]));
        }
        
        return toRet;
    }
    
    private byte[] getDbQueryTypes() 
    {
        byte[] defTypes = convertTypesToDbQueryTypes(this.getResultsSet().getTypes());
        
        if (mTypes.length > 0) {
            return intersectByteArrays(defTypes, mTypes);
        } else {
            return defTypes;
        }
    }
    
    void addTypeClause(byte type, boolean truth) {
        mAllResultsQuery = false;
        if (truth) {
            mTypes = addToByteArrayNoDup(mTypes, type);
        } else {
            mExcludeTypes = addToByteArrayNoDup(mExcludeTypes, type);
        }
    }
    
    private void getNextChunk() throws ServiceException
    {
        if (mCurHitsOffset == -1 || mEndOfHits) {
            return;
        }
        int mailboxId = this.getMailbox().getId();
        int searchOrder = this.getResultsSet().getSearchOrder();
        
        if (mIncludedItemIds != null && mIncludedItemIds.size() == 0) {
            mNoResultsQuery = true;
        }
        
        if (!mNoResultsQuery) {
            Connection conn = null;
            try {
                conn = DbPool.getConnection();

                byte sort = MailboxIndex.getDbMailItemSortByte(searchOrder);

                Folder[] folders = null;
                Folder[] excludeFolders = null;
                if (mFolder != null) {
                    folders = new Folder[1];
                    folders[0] = mFolder;
                } 
                
                if (mExcludeFolder.size() > 0) {
                    excludeFolders = (Folder[])(mExcludeFolder.toArray(new Folder[mExcludeFolder.size()]));
                }
                
                byte[] types = getDbQueryTypes();

                Tag[] includeTags = null;
                Tag[] excludeTags = null;
                if (mIncludeTags.size() > 0) {
                    includeTags = (Tag[]) mIncludeTags.toArray(new Tag[mIncludeTags.size()]);
                }
                if (mExcludeTags.size() > 0) {
                    excludeTags = (Tag[]) mExcludeTags.toArray(new Tag[mExcludeTags.size()]);
                }
                
                int[] prohibitedConvIds = null;
                if (mProhibitedConvIds.size() > 0) {
                    prohibitedConvIds = new int[mProhibitedConvIds.size()];
                    int i = 0;
                    for (Iterator probIter = mProhibitedConvIds.iterator(); probIter.hasNext();) {
                        Integer cid = (Integer)probIter.next();
                        prohibitedConvIds[i++] = cid.intValue();
                    }
                }
                
                DbMailItem.SearchConstraints c = new DbMailItem.SearchConstraints();
                c.mailboxId = mailboxId;
                c.tags = includeTags;
                c.excludeTags = excludeTags;
                c.folders = folders;
                c.excludeFolders = excludeFolders;
                c.convId = mConvId;
                c.prohibitedConvIds = prohibitedConvIds;
                c.types = types;
                c.sort = sort;
                
                if (mExcludeTypes.length > 0) {
                    c.excludeTypes = mExcludeTypes;
                }

                if (mIncludedItemIds != null) {
                    c.itemIds = new int[mIncludedItemIds.size()];
                    int offset = 0;
                    for (Iterator iter = mIncludedItemIds.iterator(); iter.hasNext(); offset++) {
                        c.itemIds[offset] = ((Integer)iter.next()).intValue();
                    }
                }
                if (mExcludedItemIds != null && mExcludedItemIds.size() > 0) {
                    c.prohibitedItemIds = new int[mExcludedItemIds.size()];
                    int offset = 0;
                    for (Iterator iter = mExcludedItemIds.iterator(); iter.hasNext(); offset++) {
                        c.prohibitedItemIds[offset] = ((Integer)iter.next()).intValue();
                    }
                }
                
                if (mDates.size() > 0) {
                    c.dates = (SearchConstraints.Range[])mDates.toArray(new SearchConstraints.Range[mDates.size()]);
                }
                
                if (mSizes.size() > 0) {
                    c.sizes = (SearchConstraints.Range[])mSizes.toArray(new SearchConstraints.Range[mSizes.size()]);
                }
                
                
                if (mLuceneOp == null) {
                    c.offset = mCurHitsOffset;
                    c.limit = HITS_PER_CHUNK;
                    mDBHits = DbMailItem.search(conn, c);
                    
                } else {
                    // DON'T set an sql LIMIT if we're asking for lucene hits!!!  If we did, then we wouldn't be
                    // sure that we'd "consumed" all the Lucene-ID's, and therefore we could miss hits!
                    c.indexIds = mLuceneOp.getNextIndexedIdChunk(HITS_PER_CHUNK);
                    
                    if (c.indexIds.length < HITS_PER_CHUNK) {
                        // we know we got all the index-id's from lucene.  since we don't have a
                        // LIMIT clause, we can be assured that this query will get all the remaining results.
                        mEndOfHits = true;
                    }
                    
                    if (c.indexIds.length == 0) {
                        mDBHits = new ArrayList(); 
                    } else {
                        mDBHits = DbMailItem.search(conn, c);
                    }
                    
                }
                
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
            if (mLuceneOp == null && mDBHits.size() < HITS_PER_CHUNK) {
                mEndOfHits = true;
            }
            mCurHitsOffset += mDBHits.size();
            mDBHitsIter = mDBHits.iterator();
        }
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.index.QueryOperation#prepare(com.zimbra.cs.mailbox.Mailbox, com.zimbra.cs.index.ZimbraQueryResultsImpl, com.zimbra.cs.index.MailboxIndex)
     */
    protected void prepare(Mailbox mbx, ZimbraQueryResultsImpl res, MailboxIndex mbidx) throws ServiceException, IOException
    {
        setupResults(mbx, res);
        
        if (mLuceneOp != null) {
            mLuceneOp.setDBOperation(this);
            mLuceneOp.prepare(mbx, res, mbidx);
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
            SearchConstraints.Range intv = (SearchConstraints.Range)(iter.next());
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

//        // date queries are a bit wonky right now, so don't join if both sides have dates...
//        if (mDates.size() > 0 && dbOther.mDates.size() > 0) {
//            return null;
//        } else {
//            if (mDates.size() > 0) {
//                toRet.mDates = mDates; 
//             } else {
//                 toRet.mDates = dbOther.mDates; 
//             }
//        }
        { // dates
            toRet.mDates = (HashSet)mDates.clone();
            for (Iterator iter = dbOther.mDates.iterator(); iter.hasNext();) {
                SearchConstraints.Range r = (SearchConstraints.Range)iter.next();
                toRet.addDateClause(r.lowest, r.highest, !r.negated);
            }
        }
        
        { // sizes
            toRet.mSizes = (HashSet)mSizes.clone();
            for (Iterator iter = dbOther.mSizes.iterator(); iter.hasNext();) {
                SearchConstraints.Range r = (SearchConstraints.Range)iter.next();
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
                toRet.mIncludedItemIds = new HashSet(); // start out with EMPTY SET (no results!)
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
            toRet.mTypes = combineByteArraysNoDups(mTypes, dbOther.mTypes);
            toRet.mExcludeTypes = combineByteArraysNoDups(mExcludeTypes, dbOther.mExcludeTypes);
        }
        
        
        { // ...combine tags...
            toRet.mIncludeTags = (HashSet)mIncludeTags.clone();
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
