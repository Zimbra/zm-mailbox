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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.util.ZimbraLog;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.Db;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.db.DbPool.Connection;
import com.zimbra.cs.db.DbSearch.SearchResult;
import com.zimbra.cs.index.TextQueryOperation.TextResultsChunk;
import com.zimbra.cs.index.TextQueryOperation.TextResultsChunk.ScoredLuceneHit;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.util.ItemId;

/**
 * {@link QueryOperation} which goes to the SQL DB. It might have a "child"
 * Lucene operation attached to it.
 *
 * @since Oct 29, 2004
 */
class DBQueryOperation extends QueryOperation {

    protected int mSizeEstimate = -1; // will only be set if the search parameters call for it

    protected int mCountDbResults = -1; // count of DB hits

    protected IConstraints mConstraints = new DbLeafNode();
    protected int mCurHitsOffset = 0; // this is the logical offset of the end of the mDBHits buffer
    protected int mOffset = 0; // this is the offset IN THE DATABASE when we're doing a DB-FIRST iteration

    /**
     * this gets set to FALSE if we have any real work to do this lets
     * us optimize away queries that might match "everything"
     */
    protected boolean mAllResultsQuery = true;

    protected boolean mIncludeIsLocalFolders = false;
    protected boolean mIncludeIsRemoteFolders = false;

    protected List<SearchResult> mDBHits;
    protected List<ZimbraHit>mNextHits = new ArrayList<ZimbraHit>();
    protected Iterator<SearchResult> mDBHitsIter;
    protected boolean atStart = true; // don't re-fill buffer twice if they call hasNext() then reset() w/o actually getting next
    protected int mHitsPerChunk = 100;
    protected static final int MAX_HITS_PER_CHUNK = 2000;

    /**
     * TRUE if we know there are no more hits to get for mDBHitsIter
     *   -- ie there is no need to call getChunk() anymore
     */
    protected boolean mEndOfHits = false;

    protected HashSet<Byte>mTypes = new HashSet<Byte>();
    protected HashSet<Byte>mExcludeTypes = new HashSet<Byte>();

    /**
     * An attached Lucene constraint
     */
    protected TextQueryOperation mLuceneOp = null;

    /**
     * The current "chunk" of lucene results we are working through -- we need to keep it around
     * so that we can look up the scores of hits that match the DB
     */
    protected TextResultsChunk mLuceneChunk = null;

    /**
     * If set, then this is the AccountId of the owner of a folder
     * we are searching.  We track it at the toplevel here b/c we need
     * to make sure that we handle unions (don't combine) and intersections
     * (always empty set) correctly
     */
    protected QueryTarget mQueryTarget = QueryTarget.UNSPECIFIED;

    private SearchResult.ExtraData mExtra = null;
    private QueryExecuteMode mExecuteMode = null;

    private static enum QueryExecuteMode {
        NO_RESULTS,
        NO_LUCENE,
        DB_FIRST,
        LUCENE_FIRST;
    }

    DBQueryOperation() {
    }

    /**
     * Since Trash can be an entire folder hierarchy, when we want to exclude trash from a query,
     * we actually have to walk that hierarchy and figure out all the folders within it.
     *
     * @param mbox
     * @return List of Folders which are in Trash, including Trash itself
     * @throws ServiceException
     */
    static List<Folder> getTrashFolders(Mailbox mbox) throws ServiceException {
        return mbox.getFolderById(null, Mailbox.ID_FOLDER_TRASH).getSubfolderHierarchy();
    }

    @Override
    QueryOperation expandLocalRemotePart(Mailbox mbox) throws ServiceException {
        if (mConstraints instanceof DbLeafNode) {
            boolean added = false;

            if (mIncludeIsLocalFolders) {
                mIncludeIsLocalFolders = false; // expanded!

                DbLeafNode leaf = (DbLeafNode)mConstraints;

                for (Folder f : mbox.getFolderById(null, Mailbox.ID_FOLDER_ROOT).getSubfolderHierarchy()) {
                    if (!(f instanceof Mountpoint) && !(f instanceof SearchFolder)) {
                        // add local folder ref
                        leaf.folders.add(f);
                        added = true;
                    }
                }
                if (!added) {
                    return new NoResultsQueryOperation();
                } else {
                    return this;
                }
            } else if (mIncludeIsRemoteFolders) {
                UnionQueryOperation toRet = new UnionQueryOperation();
                mIncludeIsRemoteFolders = false; // expanded

                for (Folder f : mbox.getFolderById(null, Mailbox.ID_FOLDER_ROOT).getSubfolderHierarchy()) {
                    if (f instanceof Mountpoint) {
                        Mountpoint mpt = (Mountpoint)f;
                        if (!mpt.isLocal()) {
                            // add remote folder ref
                            DBQueryOperation db = new DBQueryOperation();
                            db.addInRemoteFolderClause(mpt.getTarget(), "", true, true);
                            toRet.add(db);
                            added = true;
                        }
                    }
                }
                if (!added) {
                    return new NoResultsQueryOperation();
                } else {
                    return toRet;
                }
            } else {
                return this;
            }
        } else {
            throw new IllegalStateException("expandLocalRemotePart must be called before optimize() is called");
        }
    }


    @Override
    QueryOperation ensureSpamTrashSetting(Mailbox mbox, boolean includeTrash, boolean includeSpam) throws ServiceException {
        if (!hasSpamTrashSetting()) {
            ArrayList<Folder> exclude = new ArrayList<Folder>();
            if (!includeSpam) {
                Folder spam = mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM);
                exclude.add(spam);
            }

            if (!includeTrash) {
                List<Folder> trashFolders = getTrashFolders(mbox);
                for (Iterator<Folder> iter  = trashFolders.iterator(); iter.hasNext();) {
                    Folder cur = iter.next();
                    exclude.add(cur);
                }
            }

            mConstraints.ensureSpamTrashSetting(mbox, exclude);
        }
        return this;
    }

    @Override
    boolean hasSpamTrashSetting() {
        if (mLuceneOp != null && mLuceneOp.hasSpamTrashSetting())
            return true;
        else {
            if (mIncludeIsRemoteFolders)
                return true;
            else
                return mConstraints.hasSpamTrashSetting();
        }
    }

    @Override
    void forceHasSpamTrashSetting() {
        mConstraints.forceHasSpamTrashSetting();
    }

    @Override
    boolean hasNoResults() {
        return mConstraints.hasNoResults();
    }

    @Override
    boolean hasAllResults() {
        return mAllResultsQuery;
    }

    @Override
    QueryTargetSet getQueryTargets() {
        QueryTargetSet toRet = new QueryTargetSet(1);
        toRet.add(mQueryTarget);
        return toRet;
    }

    /**
     * A bit weird -- basically we want to AND a new constraint: but since
     * the mConstraints object could potentially be a tree, we need a function
     * to find the right place in the tree to add the new constraint
     *
     * @return
     */
    DbLeafNode topLevelAndedConstraint() {
        switch (mConstraints.getNodeType()) {
            case LEAF:
                return (DbLeafNode)mConstraints;
            case AND:
                DbAndNode and = (DbAndNode)mConstraints;
                return and.getLeafChild();
            case OR:
                IConstraints top = new DbAndNode();
                mConstraints = top.andIConstraints(mConstraints);
                return ((DbAndNode)mConstraints).getLeafChild();
        }
        assert(false);
        return  null;
    }

    /**
     * In an INTERSECTION, we can gain some efficiencies by using the output of the Lucene op
     * as parameters to our SearchConstraints....we do that by taking over the lucene op
     *(it is removed from the enclosing Intersection) and handling it internally.
     *
     * @param op
     */
    void addTextOp(TextQueryOperation op) {
        assert(mLuceneOp == null);
        mAllResultsQuery = false;
        mLuceneOp = op;
    }

    void addItemIdClause(Mailbox mbox, ItemId itemId, boolean truth) {
        mAllResultsQuery = false;
        if (itemId.belongsTo(mbox)) {
            // LOCAL
            topLevelAndedConstraint().addItemIdClause(itemId.getId(), truth);
        } else {
            // REMOTE
            topLevelAndedConstraint().addRemoteItemIdClause(itemId, truth);

        }

    }

    /**
     * @param lowest
     * @param highest
     * @param truth
     * @throws ServiceException
     */
    void addDateClause(long lowestDate, boolean lowestEq, long highestDate, boolean highestEq, boolean truth)  {
        mAllResultsQuery = false;
        topLevelAndedConstraint().addDateClause(lowestDate, lowestEq, highestDate, highestEq, truth);
    }

    /**
     * @param lowest
     * @param highest
     * @param truth
     * @throws ServiceException
     */
    void addCalStartDateClause(long lowestDate, boolean lowestEq, long highestDate, boolean highestEq, boolean truth)  {
        mAllResultsQuery = false;
        topLevelAndedConstraint().addCalStartDateClause(lowestDate, lowestEq, highestDate, highestEq, truth);
    }

    /**
     * @param lowest
     * @param highest
     * @param truth
     * @throws ServiceException
     */
    void addCalEndDateClause(long lowestDate, boolean lowestEq, long highestDate, boolean highestEq, boolean truth)  {
        mAllResultsQuery = false;
        topLevelAndedConstraint().addCalEndDateClause(lowestDate, lowestEq, highestDate, highestEq, truth);
    }

    /**
     * @param lowest
     * @param lowestEq
     * @param highest
     * @param highestEq
     * @param truth
     */
    void addConvCountClause(long lowest, boolean lowestEq, long highest, boolean highestEq, boolean truth)  {
        mAllResultsQuery = false;
        topLevelAndedConstraint().addConvCountClause(lowest, lowestEq, highest, highestEq, truth);
    }

    void addModSeqClause(long lowest, boolean lowestEq, long highest, boolean highestEq, boolean truth)  {
        mAllResultsQuery = false;
        topLevelAndedConstraint().addModSeqClause(lowest, lowestEq, highest, highestEq, truth);
    }

    /**
     * @param lowest
     * @param highest
     * @param truth
     * @throws ServiceException
     */
    void addSizeClause(long lowestSize, long highestSize, boolean truth)  {
        mAllResultsQuery = false;
        topLevelAndedConstraint().addSizeClause(lowestSize, highestSize, truth);
    }

    /**
     * @param lowest
     * @param highest
     * @param truth
     * @throws ServiceException
     */
    void addRelativeSubject(String lowestSubj, boolean lowerEqual, String highestSubj, boolean higherEqual, boolean truth)  {
        mAllResultsQuery = false;
        topLevelAndedConstraint().addSubjectRelClause(lowestSubj, lowerEqual, highestSubj, higherEqual, truth);
    }

    /**
     * @param lowest
     * @param highest
     * @param truth
     * @throws ServiceException
     */
    void addRelativeSender(String lowestSubj, boolean lowerEqual, String highestSubj, boolean higherEqual, boolean truth)  {
        mAllResultsQuery = false;
        topLevelAndedConstraint().addSenderRelClause(lowestSubj, lowerEqual, highestSubj, higherEqual, truth);
    }

    /**
     * @param convId
     * @param prohibited
     */
    void addConvId(Mailbox mbox, ItemId convId, boolean truth) {
        mAllResultsQuery = false;
        if (convId.belongsTo(mbox)) {
            // LOCAL!
            if (!mQueryTarget.isCompatibleLocal())
                throw new IllegalArgumentException("Cannot addConvId w/ local target b/c DBQueryOperation already has a remote target");
            mQueryTarget = QueryTarget.LOCAL;
            topLevelAndedConstraint().addConvId(convId.getId(), truth);
        } else {
            // REMOTE!
            if (mQueryTarget != QueryTarget.UNSPECIFIED && !mQueryTarget.toString().equals(convId.getAccountId()))
                throw new IllegalArgumentException("Cannot addConvId w/ remote target b/c DBQueryOperation already has an incompatible remote target");

            mQueryTarget = new QueryTarget(convId.getAccountId());
            topLevelAndedConstraint().addRemoteConvId(convId, truth);
        }
    }

    /**
     * Handles 'is:local' clause meaning all local folders
     */
    void addIsLocalClause() {
        if (!mQueryTarget.isCompatibleLocal()) {
            throw new IllegalArgumentException("Cannot addIsLocalFolderClause b/c DBQueryOperation already has a remote target");
        }
        mQueryTarget = QueryTarget.LOCAL;
        mAllResultsQuery = false;

        mIncludeIsLocalFolders = true;
    }

    /**
     * Handles 'is:local' clause meaning all local folders
     */
    void addIsRemoteClause() {
        if (mQueryTarget == QueryTarget.LOCAL) {
            throw new IllegalArgumentException("Cannot addIsRemoteFolderClause b/c DBQueryOperation already has a local target");
        }
        if (!(mQueryTarget == QueryTarget.IS_REMOTE || mQueryTarget == QueryTarget.UNSPECIFIED)) {
            throw new IllegalArgumentException("Cannot addIsRemoteFolderClause b/c DBQueryOperation already has a remote target: "+mQueryTarget);
        }
        mQueryTarget = QueryTarget.IS_REMOTE;
        mAllResultsQuery = false;

        mIncludeIsRemoteFolders = true;
    }


    /**
     * Handles query clause that resolves to a remote folder.
     */
    void addInRemoteFolderClause(ItemId remoteFolderId, String subfolderPath, boolean includeSubfolders, boolean truth)
    {
        mAllResultsQuery = false;

        if (mQueryTarget != QueryTarget.UNSPECIFIED && !mQueryTarget.toString().equals(remoteFolderId.getAccountId()))
            throw new IllegalArgumentException("Cannot addInClause b/c DBQueryOperation already has an incompatible remote target");

        mQueryTarget = new QueryTarget(remoteFolderId.getAccountId());
        topLevelAndedConstraint().addInRemoteFolderClause(remoteFolderId, subfolderPath, includeSubfolders, truth);
    }

    /**
     * @param folder
     * @param truth
     */
    void addInClause(Folder folder, boolean truth)
    {
        mAllResultsQuery = false;

        assert(!(folder instanceof Mountpoint) || ((Mountpoint)folder).isLocal());

        if (truth) {
            // EG: -in:trash is not necessarily a "local" target -- we only imply
            // a target when we're positive
            if (!mQueryTarget.isCompatibleLocal())
                throw new IllegalArgumentException("Cannot addInClause w/ local target b/c DBQueryOperation already has a remote target");
            mQueryTarget = QueryTarget.LOCAL;
        }

        topLevelAndedConstraint().addInClause(folder, truth);
    }

    void addAnyFolderClause(boolean truth) {
        topLevelAndedConstraint().addAnyFolderClause(truth);

        if (!truth) {
            // if they are weird enough to say "NOT is:anywhere" then we
            // just make it a no-results-query.
            mAllResultsQuery = false;
        }
    }

    /**
     * @param tag
     * @param truth
     */
    void addTagClause(Tag tag, boolean truth) {
        mAllResultsQuery = false;
        topLevelAndedConstraint().addTagClause(tag, truth);
    }

    /**
     * @param type
     * @param truth
     */
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

    private static final class ScoredDBHit implements Comparable<ScoredDBHit> {
        public SearchResult mSr;
        public float mScore;

        ScoredDBHit(SearchResult sr, float score) {
            mSr = sr;
            mScore = score;
        }

        long scoreAsLong() {
            return (long)(mScore * 10000);
        }

        @Override
        public int compareTo(ScoredDBHit other) {
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

        @Override
        public boolean equals(Object o) {
            return (o == this) || (compareTo((ScoredDBHit) o) == 0);
        }
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        if (mLuceneOp != null) {
            mLuceneOp.doneWithSearchResults();
        }
    }

    @Override
    public void resetIterator() {
        if (mLuceneOp != null) {
            mLuceneOp.resetDocNum();
        }
        mNextHits.clear();
        mSeenHits.clear();
        if (!atStart) {
            mOffset = 0;
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
     *   Step 1: Get the list of DbSearch.searchResults chunk-by-chunk
     *           (50 or 100 or whatever at a time)
     *
     *   Step 2: As we need them, grab the next SearchResult and build a
     *           real ZimbraHit out of them
     */
    @Override
    public ZimbraHit peekNext() throws ServiceException {
        ZimbraHit toRet = null;
        if (mNextHits.size() > 0) {
            // already have some hits, so our job is easy!
            toRet = mNextHits.get(0);
        } else {
            // we don't have any buffered SearchResults, try to get more
            while (toRet == null) {

                //
                // Check to see if we need to refil mDBHits
                //
                if ((mDBHitsIter == null || !mDBHitsIter.hasNext()) && !mEndOfHits) {
                    if (mExtra == null) {
                        mExtra = SearchResult.ExtraData.NONE;
                        switch (context.getResults().getSearchMode()) {
                            case NORMAL:
                                if (isTopLevelQueryOp()) {
                                    mExtra = SearchResult.ExtraData.MAIL_ITEM;
                                } else {
                                    mExtra = SearchResult.ExtraData.NONE;
                                }
                                break;
                            case IMAP:
                                mExtra = SearchResult.ExtraData.IMAP_MSG;
                                break;
                            case IDS:
                                mExtra = SearchResult.ExtraData.NONE;
                                break;
                            case MODSEQ:
                                mExtra = SearchResult.ExtraData.MODSEQ;
                                break;
                            case PARENT:
                                mExtra = SearchResult.ExtraData.PARENT;
                        }
                    }

                    if (mExecuteMode == null) {
                        if (hasNoResults() || !prepareSearchConstraints()) {
                            mExecuteMode = QueryExecuteMode.NO_RESULTS;
                        } else if (mLuceneOp == null) {
                            mExecuteMode = QueryExecuteMode.NO_LUCENE;
                        } else if (shouldExecuteDbFirst()) {
                            mLuceneOp.clearFilterClause();
                            mExecuteMode = QueryExecuteMode.DB_FIRST;
                        } else {
                            mExecuteMode = QueryExecuteMode.LUCENE_FIRST;
                        }
                    }

                    getNextChunk();
                }

                //
                // at this point, we've filled mDBHits if possible (and initialized its iterator)
                //
                if (mDBHitsIter != null && mDBHitsIter.hasNext()) {
                    SearchResult sr = mDBHitsIter.next();

                    // Sometimes, a single search result might yield more than one Lucene
                    // document -- e.g. an RFC822 message with separately-indexed mimeparts.
                    // Each of these parts will turn into a separate ZimbraHit at this point,
                    // although they might be combined together at a higher level (via a HitGrouper)
                    List <Document> docs = null;
                    float score = 1.0f;
                    if (mLuceneChunk != null) {
                        TextResultsChunk.ScoredLuceneHit sh = mLuceneChunk.getScoredHit(sr.indexId);
                        if (sh != null) {
                            docs = sh.mDocs;
                            score = sh.mScore;
                        } else {
                            // This could conceivably happen if we're doing a db-first query and we got multiple LuceneChunks
                            // from a single MAX_DBFIRST_RESULTS set of db-IDs....In practice, this should never happen
                            // since we only pull IDs out of the DB 200 at a time, and the max hits per LuceneChunk is 2000....
                            // ...but I am leaving this log message here temporarily so that I can know if this edge cases
                            // is really happening.  If it *does* somehow happen it isn't the end of the world: we don't lose hits,
                            // we only lose separate part hits -- the net result would be that a document which had a match in multiple
                            // parts would only be returned as a single hit for the document.
                            ZimbraLog.index_search.info("Missing ScoredLuceneHit for sr.indexId="+sr.indexId+" sr.id="+sr.id+" type="+sr.type+" part hits may be list");
                            docs = null;
                            score = 1.0f;
                        }
                    }

                    if (docs == null || !ZimbraQueryResultsImpl.shouldAddDuplicateHits(sr.type)) {
                        ZimbraHit toAdd = context.getResults().getZimbraHit(
                                context.getMailbox(), score, sr, null, mExtra);
                        if (toAdd != null) {
                            // make sure we only return each hit once
                            if (!mSeenHits.containsKey(toAdd)) {
                                mSeenHits.put(toAdd, toAdd);
                                mNextHits.add(toAdd);
                            }
                        }
                    } else {
                        for (Document doc : docs) {
                            ZimbraHit toAdd = context.getResults().getZimbraHit(
                                    context.getMailbox(), score, sr, doc, mExtra);
                            if (toAdd != null) {
                                // make sure we only return each hit once
                                if (!mSeenHits.containsKey(toAdd)) {
                                    mSeenHits.put(toAdd, toAdd);
                                    mNextHits.add(toAdd);
                                }
                            }
                        }
                    }

                    if (mNextHits.size() > 0)
                        toRet = mNextHits.get(0);
                } else {
                    return null;
                }
            }
        }

        return toRet;
    }

    /**
     * There are some situations where the lower-level code might return a given hit multiple times
     * for example an Appointment might have hits from multiple Exceptions (each of which has
     * its own Lucene document) and they will return the same AppointmentHit to us.  This is
     * the place where we collapse those hits down to single hits.
     *
     * Note that in the case of matching multiple MessageParts, the ZimbraHit that is returned is
     * different (since MP is an actual ZimbraHit subclass)....therefore MessageParts are NOT
     * coalesced at this level.  That is done at the top level grouper.
     */
    private LRUHashMap<ZimbraHit> mSeenHits = new LRUHashMap<ZimbraHit>(2048, 100);

    static final class LRUHashMap<T> extends LinkedHashMap<T, T> {
        private static final long serialVersionUID = -8616556084756995676L;
        private final int mMaxSize;

        LRUHashMap(int maxSize) {
            super(maxSize, 0.75f, true);
            mMaxSize = maxSize;
        }

        LRUHashMap(int maxSize, int tableSize) {
            super(tableSize, 0.75f, true);
            mMaxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<T, T> eldest) {
            return size() > mMaxSize;
        }
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        atStart = false;
        if (mNextHits.size() == 0) {
            peekNext();
        }
        if (mNextHits.size() == 0) {
            return null;
        }
        ZimbraHit toRet = mNextHits.remove(0);
        return toRet;
    }

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
                    tmp[numUsed++] = MailItem.TYPE_UNKNOWN;
                    break;
                case MailItem.TYPE_CONVERSATION:
                    tmp[numUsed++] = MailItem.TYPE_MESSAGE;
                    tmp[numUsed++] = MailItem.TYPE_CHAT;
                    break;
                case MailItem.TYPE_MESSAGE:
                    tmp[numUsed++] = MailItem.TYPE_MESSAGE;
                    tmp[numUsed++] = MailItem.TYPE_CHAT;
                    break;
                case MailItem.TYPE_CONTACT:
                    tmp[numUsed++] = MailItem.TYPE_CONTACT;
                    break;
                case MailItem.TYPE_APPOINTMENT:
                    tmp[numUsed++] = MailItem.TYPE_APPOINTMENT;
                    break;
                case MailItem.TYPE_TASK:
                    tmp[numUsed++] = MailItem.TYPE_TASK;
                    break;
                case MailItem.TYPE_DOCUMENT:
                    tmp[numUsed++] = MailItem.TYPE_DOCUMENT;
                    break;
                case MailItem.TYPE_NOTE:
                    tmp[numUsed++] = MailItem.TYPE_NOTE;
                    break;
                case MailItem.TYPE_FLAG:
                    tmp[numUsed++] = MailItem.TYPE_FLAG;
                    break;
                case MailItem.TYPE_WIKI:
                    tmp[numUsed++] = MailItem.TYPE_WIKI;
                    break;
            }
        }

        byte[] toRet = new byte[numUsed];
        System.arraycopy(tmp,0,toRet,0,numUsed);

        return toRet;
    }

    private Set<Byte> getDbQueryTypes() {
        byte[] defTypes = convertTypesToDbQueryTypes(context.getResults().getTypes());
        HashSet<Byte> toRet = new HashSet<Byte>();
        for (Byte b : defTypes)
            toRet.add(b);

        if (mTypes.size() > 0) {
            for (Byte b : mTypes)
                if (!toRet.contains(b))
                    toRet.add(b);
        }
        return toRet;
    }

    /**
     * Build a DbMailIte.SearchConstraints given all of the constraint parameters we have.
     *
     * @return FALSE if the search cannot be run (no results)
     */
    private boolean prepareSearchConstraints() {

        Set<Byte> types = getDbQueryTypes();
        if (types.size() == 0)  {
            ZimbraLog.index_search.debug("NO RESULTS -- no known types requested");
            return false;
        } else {
            mConstraints.setTypes(types);
            return true;
        }
    }

    private SortBy getSortOrder() {
        return context.getResults().getSortBy();
    }

    private boolean shouldExecuteDbFirst() throws ServiceException {
        if (context.getResults().getSortBy() == SortBy.SCORE_DESCENDING) {
            // we can't sort DB-results by score-order, so we must execute SCORE queries
            // in LUCENE-FIRST order
            return false;
        }

        // look for item-id or conv-id query parts, if those are set, then we'll execute DB-FIRST
        DbLeafNode toplevel = topLevelAndedConstraint();
        if (toplevel.convId > 0 || toplevel.itemIds.size() > 0) {
            return true;
        }

        if (mLuceneOp != null && mLuceneOp.shouldExecuteDbFirst()) {
            return true;
        }

        return mConstraints.tryDbFirst(context.getMailbox());
    }

    private void noLuceneGetNextChunk(Connection conn, Mailbox mbox, SortBy sort) throws ServiceException {
        if (context.getParams().getEstimateSize() && mSizeEstimate == -1)
            mSizeEstimate = DbSearch.countResults(conn, mConstraints, mbox);

        DbSearch.search(mDBHits, conn, mConstraints, mbox, sort, mCurHitsOffset, mHitsPerChunk, mExtra);

        if (mDBHits.size() < mHitsPerChunk) {
            mEndOfHits = true;
        }
        // exponentially expand the chunk size in case we have to go back to the DB
        mHitsPerChunk*=2;
        if (mHitsPerChunk > MAX_HITS_PER_CHUNK) {
            mHitsPerChunk = MAX_HITS_PER_CHUNK;
        }
    }

    private void dbFirstGetNextChunk(Connection conn, Mailbox mbox, SortBy sort) throws ServiceException {
        long overallStart = 0;
        if (ZimbraLog.index_search.isDebugEnabled()) {
            ZimbraLog.index_search.debug("Fetching a DB-FIRST chunk");
            overallStart = System.currentTimeMillis();
        }

        // we want only indexed items from db
        DbLeafNode sc = topLevelAndedConstraint();
        sc.hasIndexId = Boolean.TRUE;

        do {
            //
            // (1) Get the next chunk of results from the DB
            //
            List<SearchResult> dbRes = new ArrayList<SearchResult>();

            if (context.getParams().getEstimateSize() && mSizeEstimate == -1) {
                mSizeEstimate = DbSearch.countResults(conn, mConstraints, mbox);
            }

            DbSearch.search(dbRes, conn, mConstraints, mbox, sort, mOffset, MAX_HITS_PER_CHUNK, mExtra);

            if (dbRes.size() < MAX_HITS_PER_CHUNK) {
                mEndOfHits = true;
            }

            if (dbRes.size() > 0) {
                mOffset += dbRes.size();

                //
                // (2) for each of the results returned in (1), do a lucene search
                //    for "ORIGINAL-LUCENE-PART AND id:(RESULTS-FROM-1-ABOVE)"
                //

//                // save the original Lucene query, we'll restore it later
//                BooleanQuery originalQuery = mLuceneOp.getCurrentQuery();

                try {
                    //
                    // For each search result, do two things:
                    //    -- remember the indexId in a hash, so we can find the SearchResult later
                    //    -- add that indexId to our new booleanquery
                    //
                    HashMap<String, List<SearchResult>> mailItemToResultsMap = new HashMap<String, List<SearchResult>>();

                    for (SearchResult res : dbRes) {
                        List<SearchResult> l = mailItemToResultsMap.get(res.indexId);
                        if (l == null) {
                            l = new LinkedList<SearchResult>();
                            mailItemToResultsMap.put(res.indexId, l);
                        }
                        l.add(res);

                        // add the new query to the mLuceneOp's query
                        mLuceneOp.addFilterClause(new Term(LuceneFields.L_MAILBOX_BLOB_ID, res.indexId));
                    }

                    boolean hasMore = true;

                    boolean printedQuery = false;

                    // we have to get ALL of the lucene hits for these ids.  There can very likely be more
                    // hits from Lucene then there are DB id's, so we just ask for a large number.
                    while(hasMore) {
                        mLuceneChunk = mLuceneOp.getNextResultsChunk(MAX_HITS_PER_CHUNK);

                        Collection<String> indexIds = mLuceneChunk.getIndexIds();
                        if (indexIds.size() < MAX_HITS_PER_CHUNK) {
                            hasMore = false;
                        }
                        for (String indexId : indexIds) {
                            List<SearchResult> l = mailItemToResultsMap.get(indexId);

                            if (l == null) {
                                if (ZimbraLog.index_search.isDebugEnabled()) {
                                    if (!printedQuery) {
                                        ZimbraLog.index_search.debug("DBQueryOperation.dbFirstGetNextChunk: LuceneQuery is \""+mLuceneOp.getCurrentQuery().toString()+"\"");
                                        printedQuery = true;
                                    }
                                }
                                ZimbraLog.index_search.warn("DBQueryOperation.dbFirstGetNextChunk: Lucene returned item ID "+indexId+" but wasn't in resultMap");
                                throw ServiceException.FAILURE("Inconsistent DB/Index query results: Text Index returned item ID "+indexId+" but wasn't in resultMap", null);
                            } else for (SearchResult sr : l) {
                                mDBHits.add(sr);
                            }
                        }
                    }
                } finally {
//                    // restore the query
//                    mLuceneOp.resetQuery(originalQuery);
                    mLuceneOp.clearFilterClause();
                }
            }

        } while(mDBHits.size() ==0 && !mEndOfHits);

        if (ZimbraLog.index_search.isDebugEnabled()) {
            long overallTime = System.currentTimeMillis() - overallStart;
            ZimbraLog.index_search.debug("Done fetching DB-FIRST chunk (took "+overallTime+"ms)");
        }
    }

    private void luceneFirstGetNextChunk(Connection conn, Mailbox mbox, SortBy sort) throws ServiceException {
        long overallStart = 0;
        if (ZimbraLog.index_search.isDebugEnabled()) {
            ZimbraLog.index_search.debug("Fetching a LUCENE-FIRST chunk");
            overallStart = System.currentTimeMillis();
        }

        // do the Lucene op first, pass results to DB op
        do {
            // DON'T set an sql LIMIT if we're asking for lucene hits!!!  If we did, then we wouldn't be
            // sure that we'd "consumed" all the Lucene-ID's, and therefore we could miss hits!

            long luceneStart = 0;
            if (ZimbraLog.index_search.isDebugEnabled())
                luceneStart = System.currentTimeMillis();

            // limit in clause based on Db capabilities - bug 15511
            mLuceneChunk = mLuceneOp.getNextResultsChunk(Math.min(
                Db.getINClauseBatchSize(), mHitsPerChunk));

            // we need to set our index-id's here!
            DbLeafNode sc = topLevelAndedConstraint();

            if (context.getParams().getEstimateSize() && mSizeEstimate==-1) {
                // FIXME TODO should probably be a %age, this is worst-case
                sc.indexIds = new HashSet<String>();
                int dbResultCount;

                dbResultCount = DbSearch.countResults(conn, mConstraints, mbox);

                int numTextHits = mLuceneOp.countHits();

                if (ZimbraLog.index.isDebugEnabled())
                    ZimbraLog.index.debug("LUCENE="+numTextHits+"  DB="+dbResultCount);
                mSizeEstimate = Math.min(dbResultCount, numTextHits);
            }

            sc.indexIds = mLuceneChunk.getIndexIds();

            if (ZimbraLog.index_search.isDebugEnabled()) {
                long luceneTime = System.currentTimeMillis() - luceneStart;
                ZimbraLog.index_search.debug("Fetched Lucene Chunk of "+sc.indexIds.size()+" hits in "+luceneTime+"ms");
            }

            // exponentially expand the chunk size in case we have to go back to the DB
            mHitsPerChunk*=2;
            if (mHitsPerChunk > MAX_HITS_PER_CHUNK) {
                mHitsPerChunk = MAX_HITS_PER_CHUNK;
            }

            if (sc.indexIds.size() == 0) {
                // we know we got all the index-id's from lucene.  since we don't have a
                // LIMIT clause, we can be assured that this query will get all the remaining results.
                mEndOfHits = true;
            } else {
                long dbStart = System.currentTimeMillis();

                // must not ask for offset,limit here b/c of indexId constraints!,
                DbSearch.search(mDBHits, conn, mConstraints, mbox, sort, -1, -1, mExtra);

                if (ZimbraLog.index_search.isDebugEnabled()) {
                    long dbTime = System.currentTimeMillis() - dbStart;
                    ZimbraLog.index_search.debug("Fetched DB-second chunk in "+dbTime+"ms");
                }

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

        if (ZimbraLog.index_search.isDebugEnabled()) {
            long overallTime = System.currentTimeMillis() - overallStart;
            ZimbraLog.index_search.debug("Done fetching LUCENE-FIRST chunk (took "+overallTime+"ms)");
        }
    }


    /**
     * Use all the search parameters (including the embedded TextQueryOperation) to
     * get a chunk of search results and put them into mDBHits
     *
     * On Exit:
     *    If there are more results to be had
     *         mDBHits has entries
     *         mDBHitsIter is initialized
     *         mCurHitsOffset is the absolute offset (into the result set) of the last entry in mDBHits +1
     *                               that is, it is the offset of the next hit, when we go to get it.
     *
     *    If there are NOT any more results
     *        mDBHits is empty
     *        mDBHitsIter is null
     *        mEndOfHits is set
     *
     * @throws ServiceException
     */
    private void getNextChunk() throws ServiceException {
        assert(!mEndOfHits);
        assert(mDBHitsIter == null || !mDBHitsIter.hasNext());

        if (mExecuteMode == QueryExecuteMode.NO_RESULTS) {
            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug(" Returned **NO DB RESULTS (no-results-query-optimization)**");
            }
            mDBHitsIter = null;
            mEndOfHits = true;
        } else {
            SortBy sort = getSortOrder();
            mDBHits = new ArrayList<SearchResult>();

            Mailbox mbox = context.getMailbox();
            synchronized (DbMailItem.getSynchronizer(mbox)) {
                Connection conn = null;
                try {
                    conn = DbPool.getConnection(mbox);
                    switch (mExecuteMode) {
                        case NO_RESULTS:
                            assert(false); // notreached
                            break;
                        case NO_LUCENE:
                            noLuceneGetNextChunk(conn, mbox, sort);
                            break;
                        case DB_FIRST:
                            dbFirstGetNextChunk(conn, mbox, sort);
                            break;
                        case LUCENE_FIRST:
                            luceneFirstGetNextChunk(conn, mbox, sort);
                            break;
                    }
                } finally {
                    DbPool.quietClose(conn);
                }
            }

            if (mDBHits.size() == 0) {
                mDBHitsIter = null;
                mDBHits = null;
                mEndOfHits = true;
            } else {
                mCurHitsOffset += mDBHits.size();
                mDBHitsIter = mDBHits.iterator();
            }

        }
    }

    @Override
    protected void begin(QueryContext ctx) throws ServiceException {
        assert(context == null);
        context = ctx;

        mHitsPerChunk = ctx.getChunkSize();
        if (mHitsPerChunk > MAX_HITS_PER_CHUNK) {
            mHitsPerChunk = MAX_HITS_PER_CHUNK;
        }

        if (mLuceneOp != null) {
            mHitsPerChunk *= 2; // enlarge chunk size b/c of join
            mLuceneOp.setDBOperation(this);
            mLuceneOp.begin(new QueryContext(ctx.getMailbox(),
                    ctx.getResults(), ctx.getParams(), mHitsPerChunk));
        }
    }

    @Override
    QueryOperation optimize(Mailbox mbox) {
        return this;
    }

    @Override
    String toQueryString() {
        StringBuilder ret = new StringBuilder("(");
        if (mLuceneOp != null)
            ret.append(mLuceneOp.toQueryString()).append(" AND ");
        ret.append(mConstraints.toQueryString());
        ret.append(')');
        return ret.toString();
    }

    @Override
    public String toString() {
        boolean atFirst = true;
        StringBuilder retVal = new StringBuilder("<");
        if (mLuceneOp != null) {
            retVal.append(mLuceneOp.toString());
            atFirst = false;
        }
        if (!atFirst)
            retVal.append(" AND ");

        retVal.append("DB(");
        if (mAllResultsQuery) {
            retVal.append("ANYWHERE");
        } else if (hasNoResults()) {
            retVal.append("--- NO RESULT ---");
        } else {
            if (this.mIncludeIsLocalFolders) {
                retVal.append("IS:LOCAL ");
            } else if (this.mIncludeIsRemoteFolders) {
                retVal.append("IS:REMOTE ");
            }
            retVal.append(mConstraints.toString());
        }
        retVal.append(")");

        retVal.append('>');

        return retVal.toString();
    }

    private DBQueryOperation cloneInternal() {
        try {
            DBQueryOperation toRet = (DBQueryOperation)super.clone();

            assert(mDBHits == null);
            assert(mDBHitsIter == null);
            assert(mLuceneChunk == null);


            toRet.mConstraints = (IConstraints)mConstraints.clone();

            toRet.mTypes = new HashSet<Byte>();  toRet.mTypes.addAll(mTypes);
            toRet.mExcludeTypes = new HashSet<Byte>();  toRet.mExcludeTypes.addAll(mExcludeTypes);

            toRet.mNextHits = new ArrayList<ZimbraHit>();

            return toRet;
        } catch (CloneNotSupportedException e) {
            assert(false);
            return null;
        }
    }

    @Override
    public Object clone() {
        try {
            DBQueryOperation toRet = cloneInternal();
            if (mLuceneOp != null)
                toRet.mLuceneOp = (TextQueryOperation)mLuceneOp.clone(this);
            return toRet;
        } catch (CloneNotSupportedException e) {
            assert(false);
            return null;
        }
    }

    /**
     * Called from TextQueryOperation.clone()
     *
     * @param caller - our TextQueryOperation which has ALREADY BEEN CLONED
     * @return
     * @throws CloneNotSupportedException
     */
    protected Object clone(TextQueryOperation caller) {
        DBQueryOperation toRet = cloneInternal();
        toRet.mLuceneOp = caller;
        return toRet;
    }



    @Override
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        if (union) {
            if (hasNoResults()) {
                // a query for (other OR nothing) == other
                return other;
            }
            if (other.hasNoResults()) {
                return this;
            }

            if (other instanceof DBQueryOperation) {
                DBQueryOperation dbOther = (DBQueryOperation)other;

                if (mQueryTarget != null && dbOther.mQueryTarget != null) {
                    if (!mQueryTarget.equals(dbOther.mQueryTarget))
                        return null;  // can't OR entries with different targets
                }

                if (mAllResultsQuery)
                    return this;

                dbOther = (DBQueryOperation)other;

                if (dbOther.mAllResultsQuery) // (something OR ALL ) == ALL
                    return dbOther;

                if (mLuceneOp != null || dbOther.mLuceneOp != null){
                    // can't combine
                    return null;
                }

                if (mQueryTarget == null)
                    mQueryTarget = dbOther.mQueryTarget;

                mConstraints = mConstraints.orIConstraints(dbOther.mConstraints);
                return this;
            } else {
                return null;
            }
        } else {
            if (mAllResultsQuery) {
                // we match all results.  (other AND anything) == other

                assert(mLuceneOp == null);
                if (hasSpamTrashSetting()) {
                    other.forceHasSpamTrashSetting();
                }
                return other;
            }

            DBQueryOperation dbOther = null;

            if (other instanceof DBQueryOperation) {
                dbOther = (DBQueryOperation)other;
            } else {
                return null;
            }

            if (dbOther.mAllResultsQuery) {
                if (dbOther.hasSpamTrashSetting())
                    this.forceHasSpamTrashSetting();
                return this;
            }

            if (mQueryTarget != QueryTarget.UNSPECIFIED && dbOther.mQueryTarget != QueryTarget.UNSPECIFIED) {
                if (!mQueryTarget.equals(dbOther.mQueryTarget)) {
                    ZimbraLog.index_search.debug("ANDing two DBOps with different targets -- this is a no results query!");
                    return new NoResultsQueryOperation();
                }
            }

            if (mQueryTarget == QueryTarget.UNSPECIFIED)
                mQueryTarget = dbOther.mQueryTarget;

            if (mLuceneOp != null) {
                if (dbOther.mLuceneOp != null) {
                    mLuceneOp.combineOps(dbOther.mLuceneOp, false);
                }
            } else {
                mLuceneOp = dbOther.mLuceneOp;
            }

            if (mAllResultsQuery && dbOther.mAllResultsQuery) {
                mAllResultsQuery = true;
            } else {
                mAllResultsQuery = false;
            }

            mConstraints = mConstraints.andIConstraints(dbOther.mConstraints);

            return this;
        }

    }

    List<QueryInfo> mQueryInfo = new ArrayList<QueryInfo>();

    @Override
    public List<QueryInfo> getResultInfo() {
        List<QueryInfo> toRet = new ArrayList<QueryInfo>();
        toRet.addAll(mQueryInfo);

        if (mLuceneOp != null)
            toRet.addAll(mLuceneOp.getQueryInfo());

        return toRet;
    }

    @Override
    public int estimateResultSize() {
        return mSizeEstimate;
    }

    @Override
    protected void depthFirstRecurse(RecurseCallback cb) {
        if (mLuceneOp != null)
            mLuceneOp.depthFirstRecurseInternal(cb);
        cb.recurseCallback(this);
    }

    protected int getDbHitCount(Connection conn, Mailbox mbox) throws ServiceException {
        if (mCountDbResults == -1)
            mCountDbResults = DbSearch.countResults(conn, mConstraints, mbox);
        return mCountDbResults;
    }

    protected int getDbHitCount() throws ServiceException {
        if (mCountDbResults == -1) {
            Mailbox mbox = context.getMailbox();
            synchronized (DbMailItem.getSynchronizer(mbox)) {
                Connection conn = null;
                try {
                    conn = DbPool.getConnection(mbox);
                    mCountDbResults = getDbHitCount(conn, mbox);
                } finally {
                    DbPool.quietClose(conn);
                }
            }
        }
        return mCountDbResults;
    }
}
