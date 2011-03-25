/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
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
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.util.ItemId;

/**
 * {@link QueryOperation} which goes to the SQL DB. It might have a "child" Lucene operation attached to it.
 *
 * @since Oct 29, 2004
 * @author tim
 * @author ysasaki
 */
public class DBQueryOperation extends QueryOperation {

    private IConstraints constraints = new DbLeafNode();
    private int hitsOffset = 0; // this is the logical offset of the end of the mDBHits buffer
    private int dbOffset = 0; // this is the offset IN THE DATABASE when we're doing a DB-FIRST iteration

    /**
     * this gets set to FALSE if we have any real work to do this lets us optimize away queries that might match
     * "everything".
     */
    private boolean allResultsQuery = true;
    private boolean includeIsLocalFolders = false;
    private boolean includeIsRemoteFolders = false;

    private int dbHitCount = -1; // count of DB hits
    private List<DbSearch.Result> dbHits;
    private List<ZimbraHit> nextHits = new ArrayList<ZimbraHit>();
    private Iterator<DbSearch.Result> dbHitsIter;
    private boolean atStart = true; // don't re-fill buffer twice if they call hasNext() then reset() w/o actually getting next
    private int hitsPerChunk = 100;
    private static final int MAX_HITS_PER_CHUNK = 2000;

    /**
     * TRUE if we know there are no more hits to get for mDBHitsIter, i.e. there is no need to call getChunk() anymore.
     */
    private boolean endOfHits = false;
    private final Set<MailItem.Type> excludeTypes = EnumSet.noneOf(MailItem.Type.class);

    /**
     * An attached Lucene constraint.
     */
    private LuceneQueryOperation luceneOp = null;

    /**
     * The current "chunk" of lucene results we are working through.
     */
    private LuceneQueryOperation.LuceneResultsChunk luceneChunk = null;

    /**
     * If set, then this is the AccountId of the owner of a folder we are searching. We track it at the toplevel here
     * b/c we need to make sure that we handle unions (don't combine) and intersections (always empty set) correctly.
     */
    private QueryTarget queryTarget = QueryTarget.UNSPECIFIED;
    private List<QueryInfo> queryInfo = new ArrayList<QueryInfo>();

    private DbSearch.FetchMode fetch = null;
    private QueryExecuteMode executeMode = null;

    private static enum QueryExecuteMode {
        NO_RESULTS,
        NO_LUCENE,
        DB_FIRST,
        LUCENE_FIRST;
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
        if (constraints instanceof DbLeafNode) {
            boolean added = false;

            if (includeIsLocalFolders) {
                includeIsLocalFolders = false; // expanded!

                DbLeafNode leaf = (DbLeafNode) constraints;

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
            } else if (includeIsRemoteFolders) {
                UnionQueryOperation toRet = new UnionQueryOperation();
                includeIsRemoteFolders = false; // expanded

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

            constraints.ensureSpamTrashSetting(mbox, exclude);
        }
        return this;
    }

    @Override
    boolean hasSpamTrashSetting() {
        if (luceneOp != null && luceneOp.hasSpamTrashSetting()) {
            return true;
        } else {
            if (includeIsRemoteFolders) {
                return true;
            } else {
                return constraints.hasSpamTrashSetting();
            }
        }
    }

    @Override
    void forceHasSpamTrashSetting() {
        constraints.forceHasSpamTrashSetting();
    }

    @Override
    boolean hasNoResults() {
        return constraints.hasNoResults();
    }

    @Override
    boolean hasAllResults() {
        return allResultsQuery;
    }

    @Override
    QueryTargetSet getQueryTargets() {
        QueryTargetSet toRet = new QueryTargetSet(1);
        toRet.add(queryTarget);
        return toRet;
    }

    List<QueryInfo> getQueryInfo() {
        return queryInfo;
    }

    /**
     * A bit weird -- basically we want to AND a new constraint: but since the constraints object could potentially be
     * a tree, we need a function to find the right place in the tree to add the new constraint
     */
    DbLeafNode getTopANDedConstraint() {
        switch (constraints.getNodeType()) {
            case LEAF:
                return (DbLeafNode) constraints;
            case AND:
                DbAndNode and = (DbAndNode) constraints;
                return and.getLeafChild();
            case OR:
                IConstraints top = new DbAndNode();
                constraints = top.andIConstraints(constraints);
                return ((DbAndNode) constraints).getLeafChild();
            default:
                assert false : constraints.getNodeType();
                return null;
        }
    }

    /**
     * In an INTERSECTION, we can gain some efficiencies by using the output of
     * the Lucene op as parameters to our SearchConstraints....we do that by
     * taking over the lucene op (it is removed from the enclosing Intersection)
     * and handling it internally.
     *
     * @param op Lucene query operation
     */
    void setLuceneQueryOperation(LuceneQueryOperation op) {
        assert(luceneOp == null);
        allResultsQuery = false;
        luceneOp = op;
    }

    public void addItemIdClause(Mailbox mbox, ItemId itemId, boolean truth) {
        allResultsQuery = false;
        if (itemId.belongsTo(mbox)) { // LOCAL
            assert queryTarget.isCompatibleLocal() : getTopANDedConstraint() + "," + itemId;
            queryTarget = QueryTarget.LOCAL;
            getTopANDedConstraint().addItemIdClause(itemId.getId(), truth);
        } else { // REMOTE
            assert queryTarget != QueryTarget.LOCAL : getTopANDedConstraint() + "," + itemId;
            queryTarget = new QueryTarget(itemId.getAccountId());
            getTopANDedConstraint().addRemoteItemIdClause(itemId, truth);
        }
    }

    public void addDateClause(long lowestDate, boolean lowestEq, long highestDate, boolean highestEq, boolean truth)  {
        allResultsQuery = false;
        getTopANDedConstraint().addDateClause(lowestDate, lowestEq, highestDate, highestEq, truth);
    }

    public void addCalStartDateClause(long lowestDate, boolean lowestEq, long highestDate, boolean highestEq, boolean truth)  {
        allResultsQuery = false;
        getTopANDedConstraint().addCalStartDateClause(lowestDate, lowestEq, highestDate, highestEq, truth);
    }

    public void addCalEndDateClause(long lowestDate, boolean lowestEq, long highestDate, boolean highestEq, boolean truth)  {
        allResultsQuery = false;
        getTopANDedConstraint().addCalEndDateClause(lowestDate, lowestEq, highestDate, highestEq, truth);
    }

    public void addConvCountClause(long lowest, boolean lowestEq, long highest, boolean highestEq, boolean truth)  {
        allResultsQuery = false;
        getTopANDedConstraint().addConvCountClause(lowest, lowestEq, highest, highestEq, truth);
    }

    public void addModSeqClause(long lowest, boolean lowestEq, long highest, boolean highestEq, boolean truth)  {
        allResultsQuery = false;
        getTopANDedConstraint().addModSeqClause(lowest, lowestEq, highest, highestEq, truth);
    }

    public void addSizeClause(long lowestSize, long highestSize, boolean truth)  {
        allResultsQuery = false;
        getTopANDedConstraint().addSizeClause(lowestSize, highestSize, truth);
    }

    public void addRelativeSubject(String lowestSubj, boolean lowerEqual, String highestSubj, boolean higherEqual, boolean truth)  {
        allResultsQuery = false;
        getTopANDedConstraint().addSubjectRelClause(lowestSubj, lowerEqual, highestSubj, higherEqual, truth);
    }

    public void addRelativeSender(String lowestSubj, boolean lowerEqual, String highestSubj, boolean higherEqual, boolean truth)  {
        allResultsQuery = false;
        getTopANDedConstraint().addSenderRelClause(lowestSubj, lowerEqual, highestSubj, higherEqual, truth);
    }

    public void setFromContact(boolean bool) {
        allResultsQuery = false;
        queryTarget = QueryTarget.LOCAL;
        getTopANDedConstraint().setFromContact(bool);
    }

    public void addConvId(Mailbox mbox, ItemId convId, boolean truth) {
        allResultsQuery = false;
        if (convId.belongsTo(mbox)) { // LOCAL
            if (!queryTarget.isCompatibleLocal()) {
                throw new IllegalArgumentException(
                        "Cannot addConvId w/ local target b/c DBQueryOperation already has a remote target");
            }
            queryTarget = QueryTarget.LOCAL;
            getTopANDedConstraint().addConvId(convId.getId(), truth);
        } else { // REMOTE
            if (queryTarget != QueryTarget.UNSPECIFIED && !queryTarget.toString().equals(convId.getAccountId())) {
                throw new IllegalArgumentException(
                        "Cannot addConvId w/ remote target b/c DBQueryOperation already has an incompatible remote target");
            }
            queryTarget = new QueryTarget(convId.getAccountId());
            getTopANDedConstraint().addRemoteConvId(convId, truth);
        }
    }

    /**
     * Handles 'is:local' clause meaning all local folders
     */
    public void addIsLocalClause() {
        if (!queryTarget.isCompatibleLocal()) {
            throw new IllegalArgumentException("Cannot addIsLocalFolderClause b/c DBQueryOperation already has a remote target");
        }
        queryTarget = QueryTarget.LOCAL;
        allResultsQuery = false;
        includeIsLocalFolders = true;
    }

    /**
     * Handles 'is:local' clause meaning all local folders
     */
    public void addIsRemoteClause() {
        if (queryTarget == QueryTarget.LOCAL) {
            throw new IllegalArgumentException(
                    "Cannot addIsRemoteFolderClause b/c DBQueryOperation already has a local target");
        }
        if (!(queryTarget == QueryTarget.IS_REMOTE || queryTarget == QueryTarget.UNSPECIFIED)) {
            throw new IllegalArgumentException(
                    "Cannot addIsRemoteFolderClause b/c DBQueryOperation already has a remote target: " + queryTarget);
        }
        queryTarget = QueryTarget.IS_REMOTE;
        allResultsQuery = false;
        includeIsRemoteFolders = true;
    }


    /**
     * Handles query clause that resolves to a remote folder.
     */
    public void addInRemoteFolderClause(ItemId remoteFolderId, String subfolderPath,
            boolean includeSubfolders, boolean truth) {
        allResultsQuery = false;

        if (queryTarget != QueryTarget.UNSPECIFIED && !queryTarget.toString().equals(remoteFolderId.getAccountId())) {
            throw new IllegalArgumentException(
                    "Cannot addInClause b/c DBQueryOperation already has an incompatible remote target");
        }

        queryTarget = new QueryTarget(remoteFolderId.getAccountId());
        getTopANDedConstraint().addInRemoteFolderClause(remoteFolderId, subfolderPath, includeSubfolders, truth);
    }

    public void addInClause(Folder folder, boolean truth) {
        assert !(folder instanceof Mountpoint) || ((Mountpoint)folder).isLocal() : folder;

        allResultsQuery = false;
        if (truth) {
            // EG: -in:trash is not necessarily a "local" target -- we only imply
            // a target when we're positive
            if (!queryTarget.isCompatibleLocal()) {
                throw new IllegalArgumentException(
                        "Cannot addInClause w/ local target b/c DBQueryOperation already has a remote target");
            }
            queryTarget = QueryTarget.LOCAL;
        }

        getTopANDedConstraint().addInClause(folder, truth);
    }

    public void addAnyFolderClause(boolean truth) {
        getTopANDedConstraint().addAnyFolderClause(truth);
        if (!truth) { // if they are weird enough to say "NOT is:anywhere" then we just make it a no-results-query.
            allResultsQuery = false;
        }
    }

    public void addTagClause(Tag tag, boolean truth) {
        allResultsQuery = false;
        getTopANDedConstraint().addTagClause(tag, truth);
    }

    @Override
    public void doneWithSearchResults() throws ServiceException {
        if (luceneOp != null) {
            luceneOp.doneWithSearchResults();
        }
    }

    @Override
    public void resetIterator() {
        if (luceneOp != null) {
            luceneOp.resetDocNum();
        }
        nextHits.clear();
        mSeenHits.clear();
        if (!atStart) {
            dbOffset = 0;
            dbHitsIter = null;
            hitsOffset = 0;
            endOfHits = false;
            atStart = true;
        } else {
            if (dbHits != null) {
                dbHitsIter = dbHits.iterator();
            }
        }
    }

    /**
     * Return the next hit in our search. If there are no hits buffered then calculate the next hit and put it into the
     * mNextHits list.
     * <ol>
     *  <li>Get the list of DBSearchResults chunk-by-chunk (50 or 100 or whatever at a time)
     *  <li>As we need them, grab the next SearchResult and build a real ZimbraHit out of them
     * </ol>
     */
    @Override
    public ZimbraHit peekNext() throws ServiceException {
        ZimbraHit toRet = null;
        if (nextHits.size() > 0) {
            // already have some hits, so our job is easy!
            toRet = nextHits.get(0);
        } else {
            // we don't have any buffered SearchResults, try to get more
            while (toRet == null) {
                // Check to see if we need to refil mDBHits
                if ((dbHitsIter == null || !dbHitsIter.hasNext()) && !endOfHits) {
                    if (fetch == null) {
                        switch (context.getResults().getSearchMode()) {
                            case NORMAL:
                                fetch = isTopLevelQueryOp() ? DbSearch.FetchMode.MAIL_ITEM : DbSearch.FetchMode.ID;
                                break;
                            case IMAP:
                                fetch = DbSearch.FetchMode.IMAP_MSG;
                                break;
                            case IDS:
                                fetch = DbSearch.FetchMode.ID;
                                break;
                            case MODSEQ:
                                fetch = DbSearch.FetchMode.MODSEQ;
                                break;
                            case PARENT:
                                fetch = DbSearch.FetchMode.PARENT;
                                break;
                            default:
                                assert false : context.getResults().getSearchMode();
                        }
                    }

                    if (executeMode == null) {
                        if (hasNoResults()) {
                            executeMode = QueryExecuteMode.NO_RESULTS;
                        } else if (luceneOp == null) {
                            executeMode = QueryExecuteMode.NO_LUCENE;
                        } else if (shouldExecuteDbFirst()) {
                            luceneOp.clearFilterClause();
                            executeMode = QueryExecuteMode.DB_FIRST;
                        } else {
                            executeMode = QueryExecuteMode.LUCENE_FIRST;
                        }
                    }

                    getNextChunk();
                }

                // at this point, we've filled mDBHits if possible (and initialized its iterator)
                if (dbHitsIter != null && dbHitsIter.hasNext()) {
                    DbSearch.Result sr = dbHitsIter.next();
                    // Sometimes, a single search result might yield more than one Lucene document -- e.g. an RFC822
                    // message with separately-indexed MIME parts. Each of these parts will turn into a separate
                    // ZimbraHit at this point, although they might be combined together at a higher level (via a
                    // HitGrouper).
                    Collection<Document> docs = luceneChunk != null ? luceneChunk.getHit(sr.getIndexId()) : null;

                    if (docs == null || !ZimbraQueryResultsImpl.shouldAddDuplicateHits(sr.getType())) {
                        ZimbraHit toAdd = context.getResults().getZimbraHit(context.getMailbox(), sr, null, fetch);
                        if (toAdd != null) {
                            // make sure we only return each hit once
                            if (!mSeenHits.containsKey(toAdd)) {
                                mSeenHits.put(toAdd, toAdd);
                                nextHits.add(toAdd);
                            }
                        }
                    } else {
                        for (Document doc : docs) {
                            ZimbraHit toAdd = context.getResults().getZimbraHit(context.getMailbox(), sr, doc, fetch);
                            if (toAdd != null) {
                                // make sure we only return each hit once
                                if (!mSeenHits.containsKey(toAdd)) {
                                    mSeenHits.put(toAdd, toAdd);
                                    nextHits.add(toAdd);
                                }
                            }
                        }
                    }

                    if (nextHits.size() > 0) {
                        toRet = nextHits.get(0);
                    }
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
        if (nextHits.size() == 0) {
            peekNext();
        }
        if (nextHits.size() == 0) {
            return null;
        }
        ZimbraHit toRet = nextHits.remove(0);
        return toRet;
    }

    private Set<MailItem.Type> toDbQueryTypes(Set<MailItem.Type> types) {
        Set<MailItem.Type> result = EnumSet.noneOf(MailItem.Type.class);

        for (MailItem.Type type : types) {
            switch (type) {
                case FOLDER:
                case SEARCHFOLDER:
                case TAG:
                    result.add(MailItem.Type.UNKNOWN);
                    break;
                case CONVERSATION:
                    result.add(MailItem.Type.MESSAGE);
                    result.add(MailItem.Type.CHAT);
                    break;
                case MESSAGE:
                    result.add(MailItem.Type.MESSAGE);
                    result.add(MailItem.Type.CHAT);
                    break;
                case CONTACT:
                    result.add(MailItem.Type.CONTACT);
                    break;
                case APPOINTMENT:
                    result.add(MailItem.Type.APPOINTMENT);
                    break;
                case TASK:
                    result.add(MailItem.Type.TASK);
                    break;
                case DOCUMENT:
                    result.add(MailItem.Type.DOCUMENT);
                    break;
                case NOTE:
                    result.add(MailItem.Type.NOTE);
                    break;
                case FLAG:
                    result.add(MailItem.Type.FLAG);
                    break;
                case WIKI:
                    result.add(MailItem.Type.WIKI);
                    break;
            }
        }

        return result;
    }

    private SortBy getSortOrder() {
        return context.getResults().getSortBy();
    }

    private void dbSearch(List<DbSearch.Result> results, SortBy sort, int offset, int size) throws ServiceException {
        results.addAll(context.getMailbox().index.search(constraints, fetch, sort, offset, size, searchInDumpster()));
    }

    private boolean shouldExecuteDbFirst() throws ServiceException {
        // look for item-id or conv-id query parts, if those are set, then we'll execute DB-FIRST
        DbLeafNode toplevel = getTopANDedConstraint();
        if (toplevel.convId > 0 || toplevel.itemIds.size() > 0) {
            return true;
        }

        if (luceneOp != null && luceneOp.shouldExecuteDbFirst()) {
            return true;
        }

        return constraints.tryDbFirst(context.getMailbox());
    }

    private void noLuceneGetNextChunk(SortBy sort) throws ServiceException {
        dbSearch(dbHits, sort, hitsOffset, hitsPerChunk);

        if (dbHits.size() < hitsPerChunk) {
            endOfHits = true;
        }
        // exponentially expand the chunk size in case we have to go back to the DB
        hitsPerChunk *= 2;
        if (hitsPerChunk > MAX_HITS_PER_CHUNK) {
            hitsPerChunk = MAX_HITS_PER_CHUNK;
        }
    }

    private boolean searchInDumpster() {
        return context.getParams().inDumpster();
    }

    private void dbFirstGetNextChunk(SortBy sort) throws ServiceException {
        long begin = System.currentTimeMillis();
        ZimbraLog.search.debug("Fetching a DB-FIRST chunk");

        // we want only indexed items from db
        DbLeafNode sc = getTopANDedConstraint();
        sc.hasIndexId = Boolean.TRUE;

        do {
            // (1) Get the next chunk of results from the DB
            List<DbSearch.Result> dbResults = new ArrayList<DbSearch.Result>();
            dbSearch(dbResults, sort, dbOffset, MAX_HITS_PER_CHUNK);

            if (dbResults.size() < MAX_HITS_PER_CHUNK) {
                endOfHits = true;
            }

            if (dbResults.size() > 0) {
                dbOffset += dbResults.size();

                // (2) for each of the results returned in (1), do a lucene search
                //    for "ORIGINAL-LUCENE-PART AND id:(RESULTS-FROM-1-ABOVE)"
                try {
                    // For each search result, do two things:
                    //    -- remember the indexId in a hash, so we can find the SearchResult later
                    //    -- add that indexId to our new booleanquery
                    Map<Integer, List<DbSearch.Result>> mailItemToResultsMap = new HashMap<Integer, List<DbSearch.Result>>();

                    for (DbSearch.Result sr : dbResults) {
                        List<DbSearch.Result> results = mailItemToResultsMap.get(sr.getIndexId());
                        if (results == null) {
                            results = new LinkedList<DbSearch.Result>();
                            mailItemToResultsMap.put(sr.getIndexId(), results);
                        }
                        results.add(sr);
                        // add the new query to the mLuceneOp's query
                        luceneOp.addFilterClause(new Term(LuceneFields.L_MAILBOX_BLOB_ID,
                                String.valueOf(sr.getIndexId())));
                    }

                    boolean hasMore = true;

                    // we have to get ALL of the lucene hits for these ids.  There can very likely be more
                    // hits from Lucene then there are DB id's, so we just ask for a large number.
                    while (hasMore) {
                        luceneChunk = luceneOp.getNextResultsChunk(MAX_HITS_PER_CHUNK);
                        Set<Integer> indexIds = luceneChunk.getIndexIds();
                        if (indexIds.size() < MAX_HITS_PER_CHUNK) {
                            hasMore = false;
                        }
                        for (int indexId : indexIds) {
                            List<DbSearch.Result> results = mailItemToResultsMap.get(indexId);
                            if (results != null) {
                                for (DbSearch.Result sr : results) {
                                    dbHits.add(sr);
                                }
                            } else {
                                ZimbraLog.search.warn("Lucene returned item ID %d but wasn't in resultMap", indexId);
                                throw ServiceException.FAILURE(
                                        "Inconsistent DB/Index query results: Text Index returned item ID " +
                                        indexId + " but wasn't in resultMap", null);
                            }
                        }
                    }
                } finally {
                    luceneOp.clearFilterClause();
                }
            }

        } while (dbHits.size() ==0 && !endOfHits);

        ZimbraLog.search.debug("Done fetching DB-FIRST chunk (took %d ms)", System.currentTimeMillis() - begin);
    }

    private void luceneFirstGetNextChunk(SortBy sort) throws ServiceException {
        long begin = System.currentTimeMillis();
        ZimbraLog.search.debug("Fetching a LUCENE-FIRST chunk");

        // do the Lucene op first, pass results to DB op
        do {
            // DON'T set an sql LIMIT if we're asking for lucene hits!!!  If we did, then we wouldn't be
            // sure that we'd "consumed" all the Lucene-ID's, and therefore we could miss hits!

            long luceneStart = System.currentTimeMillis();

            // limit in clause based on Db capabilities - bug 15511
            luceneChunk = luceneOp.getNextResultsChunk(Math.min(Db.getINClauseBatchSize(), hitsPerChunk));

            DbLeafNode sc = getTopANDedConstraint();
            sc.indexIds = luceneChunk.getIndexIds();

            ZimbraLog.search.debug("Fetched Lucene Chunk of %d hits in %d ms",
                    sc.indexIds.size(), System.currentTimeMillis() - luceneStart);

            // exponentially expand the chunk size in case we have to go back to the DB
            hitsPerChunk *= 2;
            if (hitsPerChunk > MAX_HITS_PER_CHUNK) {
                hitsPerChunk = MAX_HITS_PER_CHUNK;
            }

            if (sc.indexIds.size() == 0) {
                // we know we got all the index-id's from lucene.  since we don't have a
                // LIMIT clause, we can be assured that this query will get all the remaining results.
                endOfHits = true;
            } else {
                long dbStart = System.currentTimeMillis();

                // must not ask for offset,limit here b/c of indexId constraints!,
                dbSearch(dbHits, sort, -1, -1);

                ZimbraLog.search.debug("Fetched DB-second chunk in %d ms", System.currentTimeMillis() - dbStart);
            }
        } while (dbHits.size() == 0 && !endOfHits);

        ZimbraLog.search.debug("Done fetching LUCENE-FIRST chunk (took %d ms)", System.currentTimeMillis() - begin);
    }

    /**
     * Use all the search parameters (including the embedded {@link LuceneQueryOperation}) to get a chunk of search
     * results and put them into dbHits
     * <p>
     * On Exit:
     * If there are more results to be had
     * <ul>
     *  <li>dbHits has entries
     *  <li>dbHitsIter is initialized
     *  <li>hitsOffset is the absolute offset (into the result set) of the last entry in dbHits +1 that is, it is the
     *  offset of the next hit, when we go to get it.
     * </ul>
     * If there are NOT any more results
     * <ul>
     *  <li>dbHits is empty
     *  <li>dbHitsIter is null
     *  <li>endOfHits is set
     * </ul>
     */
    private void getNextChunk() throws ServiceException {
        assert(!endOfHits);
        assert(dbHitsIter == null || !dbHitsIter.hasNext());

        if (executeMode == QueryExecuteMode.NO_RESULTS) {
            ZimbraLog.search.debug("Returned **NO DB RESULTS (no-results-query-optimization)**");
            dbHitsIter = null;
            endOfHits = true;
        } else {
            SortBy sort = getSortOrder();
            dbHits = new ArrayList<DbSearch.Result>();

            Mailbox mbox = context.getMailbox();
            synchronized (DbMailItem.getSynchronizer(mbox)) {
                switch (executeMode) {
                    case NO_RESULTS:
                        assert(false); // notreached
                        break;
                    case NO_LUCENE:
                        noLuceneGetNextChunk(sort);
                        break;
                    case DB_FIRST:
                        dbFirstGetNextChunk(sort);
                        break;
                    case LUCENE_FIRST:
                        luceneFirstGetNextChunk(sort);
                        break;
                }
            }

            if (dbHits.size() == 0) {
                dbHitsIter = null;
                dbHits = null;
                endOfHits = true;
            } else {
                hitsOffset += dbHits.size();
                dbHitsIter = dbHits.iterator();
            }

        }
    }

    @Override
    protected void begin(QueryContext ctx) throws ServiceException {
        assert(context == null);
        context = ctx;

        hitsPerChunk = ctx.getChunkSize();
        if (hitsPerChunk > MAX_HITS_PER_CHUNK) {
            hitsPerChunk = MAX_HITS_PER_CHUNK;
        }

        constraints.setTypes(toDbQueryTypes(context.getResults().getTypes()));

        if (luceneOp != null) {
            hitsPerChunk *= 2; // enlarge chunk size b/c of join
            luceneOp.setDBOperation(this);
            // this is 2nd time to call begin() of this Lucene op.
            luceneOp.begin(new QueryContext(ctx.getMailbox(), ctx.getResults(), ctx.getParams(), hitsPerChunk));
        }
    }

    @Override
    QueryOperation optimize(Mailbox mbox) {
        return this;
    }

    @Override
    String toQueryString() {
        StringBuilder ret = new StringBuilder("(");
        if (luceneOp != null) {
            ret.append(luceneOp.toQueryString()).append(" AND ");
        }
        ret.append(constraints.toQueryString());
        ret.append(')');
        return ret.toString();
    }

    @Override
    public String toString() {
        boolean atFirst = true;
        StringBuilder result = new StringBuilder("<");
        if (luceneOp != null) {
            result.append(luceneOp.toString());
            atFirst = false;
        }
        if (!atFirst) {
            result.append(" AND ");
        }
        result.append("DB[");
        if (allResultsQuery) {
            result.append("ANYWHERE");
        } else if (hasNoResults()) {
            result.append("--- NO RESULT ---");
        } else {
            if (includeIsLocalFolders) {
                result.append("(IS:LOCAL)");
            } else if (includeIsRemoteFolders) {
                result.append("(IS:REMOTE)");
            }
            result.append(constraints.toString());
        }
        result.append("]>");
        return result.toString();
    }

    private DBQueryOperation cloneInternal() {
        try {
            DBQueryOperation result = (DBQueryOperation) super.clone();

            assert(dbHits == null);
            assert(dbHitsIter == null);
            assert(luceneChunk == null);

            result.constraints = (IConstraints) constraints.clone();
            result.excludeTypes.addAll(excludeTypes);
            result.nextHits = new ArrayList<ZimbraHit>();
            return result;
        } catch (CloneNotSupportedException e) {
            assert(false);
            return null;
        }
    }

    @Override
    public Object clone() {
        DBQueryOperation toRet = cloneInternal();
        if (luceneOp != null) {
            toRet.luceneOp = (LuceneQueryOperation) luceneOp.clone(this);
        }
        return toRet;
    }

    /**
     * Called from {@link LuceneQueryOperation#clone()}.
     *
     * @param caller our LuceneQueryOperation which has ALREADY BEEN CLONED
     */
    protected Object clone(LuceneQueryOperation caller) {
        DBQueryOperation toRet = cloneInternal();
        toRet.luceneOp = caller;
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

                if (queryTarget != null && dbOther.queryTarget != null) {
                    if (!queryTarget.equals(dbOther.queryTarget))
                        return null;  // can't OR entries with different targets
                }

                if (allResultsQuery) {
                    return this;
                }
                dbOther = (DBQueryOperation)other;

                if (dbOther.allResultsQuery) { // (something OR ALL ) == ALL
                    return dbOther;
                }
                if (luceneOp != null || dbOther.luceneOp != null){
                    // can't combine
                    return null;
                }

                if (queryTarget == null) {
                    queryTarget = dbOther.queryTarget;
                }
                constraints = constraints.orIConstraints(dbOther.constraints);
                return this;
            } else {
                return null;
            }
        } else {
            if (allResultsQuery) { // we match all results.  (other AND anything) == other
                assert(luceneOp == null);
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

            if (dbOther.allResultsQuery) {
                if (dbOther.hasSpamTrashSetting()) {
                    this.forceHasSpamTrashSetting();
                }
                return this;
            }

            if (queryTarget != QueryTarget.UNSPECIFIED && dbOther.queryTarget != QueryTarget.UNSPECIFIED) {
                if (!queryTarget.equals(dbOther.queryTarget)) {
                    ZimbraLog.search.debug("ANDing two DBOps with different targets -- this is a no results query!");
                    return new NoResultsQueryOperation();
                }
            }

            if (queryTarget == QueryTarget.UNSPECIFIED) {
                queryTarget = dbOther.queryTarget;
            }
            if (luceneOp != null) {
                if (dbOther.luceneOp != null) {
                    luceneOp.combineOps(dbOther.luceneOp, false);
                }
            } else {
                luceneOp = dbOther.luceneOp;
            }

            if (allResultsQuery && dbOther.allResultsQuery) {
                allResultsQuery = true;
            } else {
                allResultsQuery = false;
            }

            constraints = constraints.andIConstraints(dbOther.constraints);

            return this;
        }

    }

    @Override
    public List<QueryInfo> getResultInfo() {
        List<QueryInfo> toRet = new ArrayList<QueryInfo>();
        toRet.addAll(queryInfo);
        if (luceneOp != null) {
            toRet.addAll(luceneOp.getQueryInfo());
        }
        return toRet;
    }

    @Override
    protected void depthFirstRecurse(RecurseCallback cb) {
        if (luceneOp != null) {
            luceneOp.depthFirstRecurseInternal(cb);
        }
        cb.recurseCallback(this);
    }

    int getDbHitCount() throws ServiceException {
        if (dbHitCount < 0) {
            Mailbox mbox = context.getMailbox();
            synchronized (DbMailItem.getSynchronizer(mbox)) {
                DbConnection conn = DbPool.getConnection(mbox);
                try {
                    dbHitCount = DbSearch.countResults(conn, constraints, mbox, searchInDumpster());
                } finally {
                    DbPool.quietClose(conn);
                }
            }
        }
        return dbHitCount;
    }

    @Override
    public long getTotalHitCount() throws ServiceException {
        Folder folder = getTopANDedConstraint().getOnlyFolder();
        if (folder != null) {
            if (context.getResults().getTypes().contains(MailItem.Type.CONVERSATION)) {
                return folder.getConversationCount();
            } else {
                return folder.getItemCount();
            }
        } else {
            return -1;
        }
    }

}
