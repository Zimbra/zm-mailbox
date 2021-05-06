/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;

import com.google.common.collect.ImmutableSet;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.Db;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.util.IOUtil;

/**
 * {@link QueryOperation} which goes to the SQL DB. It might have a "child" Lucene operation attached to it.
 *
 * @since Oct 29, 2004
 * @author tim
 * @author ysasaki
 */
public class DBQueryOperation extends QueryOperation {
    private static final int MAX_HITS_PER_CHUNK = 2000;

    private DbSearchConstraints constraints = new DbSearchConstraints.Leaf();
    private int hitsOffset = 0; // this is the logical offset of the end of the mDBHits buffer
    private int dbOffset = 0; // this is the offset IN THE DATABASE when we're doing a DB-FIRST iteration
    private int cursorOffset = -1; // calculated cursor offset

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
    private final List<QueryInfo> queryInfo = new ArrayList<QueryInfo>();

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

    Set<Folder> getTargetFolders() {
        if (constraints instanceof DbSearchConstraints.Leaf) {
            DbSearchConstraints.Leaf leaf = (DbSearchConstraints.Leaf) constraints;
            return leaf.folders;
        }
        else if (constraints instanceof DbSearchConstraints.Union) {
            DbSearchConstraints.Union node = (DbSearchConstraints.Union) constraints;
            Set<Folder> folders = new HashSet<Folder>();
            for (DbSearchConstraints subConstraints : node.getChildren()) {
                if (subConstraints instanceof DbSearchConstraints.Leaf) {
                    folders.addAll(((DbSearchConstraints.Leaf) subConstraints).folders);
                }
            }
            return folders;
        }
        else {
            //DbAndNode doesn't make sense (in:folder1 AND in:folder2 always returns empty)
            //that gets handled elsewhere, just return null
            return null;
        }
    }

    @Override
    QueryOperation expandLocalRemotePart(Mailbox mbox) throws ServiceException {
        if (constraints instanceof DbSearchConstraints.Leaf) {
            boolean added = false;

            if (includeIsLocalFolders) {
                includeIsLocalFolders = false; // expanded!

                DbSearchConstraints.Leaf leaf = (DbSearchConstraints.Leaf) constraints;

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
                            db.addInRemoteFolder(mpt.getTarget(), "", true, true);
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
    Set<QueryTarget> getQueryTargets() {
        return ImmutableSet.of(queryTarget);
    }

    List<QueryInfo> getQueryInfo() {
        return queryInfo;
    }

    /**
     * A bit weird -- basically we want to AND a new constraint: but since the constraints object could potentially be
     * a tree, we need a function to find the right place in the tree to add the new constraint
     */
    DbSearchConstraints.Leaf getTopLeafConstraint() {
        if (constraints instanceof DbSearchConstraints.Intersection) {
            DbSearchConstraints.Intersection and = (DbSearchConstraints.Intersection) constraints;
            return and.getLeafChild();
        } else if (constraints instanceof DbSearchConstraints.Union) {
            DbSearchConstraints top = new DbSearchConstraints.Intersection();
            constraints = top.and(constraints);
            return ((DbSearchConstraints.Intersection) constraints).getLeafChild();
        } else {
            return (DbSearchConstraints.Leaf) constraints;
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
        allResultsQuery = false;
        if (luceneOp == null) {
            luceneOp = op;
        } else {
            luceneOp.addClause(op.getQueryString(), op.getQuery(), true);
        }
    }

    public void addItemIdClause(Mailbox mbox, ItemId itemId, boolean truth) {
        allResultsQuery = false;
        if (itemId.belongsTo(mbox)) { // LOCAL
            assert queryTarget.isCompatibleLocal() : getTopLeafConstraint() + "," + itemId;
            queryTarget = QueryTarget.LOCAL;
            getTopLeafConstraint().addItemIdClause(itemId.getId(), truth);
        } else { // REMOTE
            assert queryTarget != QueryTarget.LOCAL : getTopLeafConstraint() + "," + itemId;
            queryTarget = new QueryTarget(itemId.getAccountId());
            getTopLeafConstraint().addRemoteItemIdClause(itemId, truth);
        }
    }

    public void addDateRange(long min, boolean minInclusive, long max, boolean maxInclusive, boolean bool) {
        allResultsQuery = false;
        getTopLeafConstraint().addDateRange(min, minInclusive, max, maxInclusive, bool);
    }

    public void addMDateRange(long min, boolean minInclusive, long max, boolean maxInclusive, boolean bool) {
        allResultsQuery = false;
        getTopLeafConstraint().addMDateRange(min, minInclusive, max, maxInclusive, bool);
    }

    public void addCalStartDateRange(long min, boolean minInclusive, long max, boolean maxInclusive, boolean bool) {
        allResultsQuery = false;
        getTopLeafConstraint().addCalStartDateRange(min, minInclusive, max, maxInclusive, bool);
    }

    public void addCalEndDateRange(long min, boolean minInclusive, long max, boolean maxInclusive, boolean bool) {
        allResultsQuery = false;
        getTopLeafConstraint().addCalEndDateRange(min, minInclusive, max, maxInclusive, bool);
    }

    public void addConvCountRange(long min, boolean minInclusive, long max, boolean maxInclusive, boolean bool) {
        allResultsQuery = false;
        getTopLeafConstraint().addConvCountRange(min, minInclusive, max, maxInclusive, bool);
    }

    public void addModSeqRange(long min, boolean minInclusive, long max, boolean maxInclusive, boolean bool) {
        allResultsQuery = false;
        getTopLeafConstraint().addModSeqRange(min, minInclusive, max, maxInclusive, bool);
    }

    public void addSizeRange(long min, boolean minInclusive, long max, boolean maxInclusive, boolean bool) {
        allResultsQuery = false;
        getTopLeafConstraint().addSizeRange(min, minInclusive, max, maxInclusive, bool);
    }

    public void addSubjectRange(String min, boolean minInclusive, String max, boolean maxInclusive, boolean bool) {
        allResultsQuery = false;
        getTopLeafConstraint().addSubjectRange(min, minInclusive, max, maxInclusive, bool);
    }

    public void addSenderRange(String min, boolean minInclusive, String max, boolean maxIncluisve, boolean bool) {
        allResultsQuery = false;
        getTopLeafConstraint().addSenderRange(min, minInclusive, max, maxIncluisve, bool);
    }

    public void addItemIdRange(int min, boolean minInclusive, int max, boolean maxInclusive, boolean bool) {
        allResultsQuery = false;
        getTopLeafConstraint().addItemIdRange(min, minInclusive, max, maxInclusive, bool);
    }
    public void addConvId(Mailbox mbox, ItemId convId, boolean truth) {
        allResultsQuery = false;
        if (convId.belongsTo(mbox)) { // LOCAL
            if (!queryTarget.isCompatibleLocal()) {
                throw new IllegalArgumentException(
                        "Cannot addConvId w/ local target b/c DBQueryOperation already has a remote target");
            }
            queryTarget = QueryTarget.LOCAL;
            getTopLeafConstraint().addConvId(convId.getId(), truth);
        } else { // REMOTE
            if (queryTarget != QueryTarget.UNSPECIFIED && !queryTarget.toString().equals(convId.getAccountId())) {
                throw new IllegalArgumentException(
                        "Cannot addConvId w/ remote target b/c DBQueryOperation already has an incompatible remote target");
            }
            queryTarget = new QueryTarget(convId.getAccountId());
            getTopLeafConstraint().addRemoteConvId(convId, truth);
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
        if (!(queryTarget == QueryTarget.REMOTE || queryTarget == QueryTarget.UNSPECIFIED)) {
            throw new IllegalArgumentException(
                    "Cannot addIsRemoteFolderClause b/c DBQueryOperation already has a remote target: " + queryTarget);
        }
        queryTarget = QueryTarget.REMOTE;
        allResultsQuery = false;
        includeIsRemoteFolders = true;
    }


    /**
     * Handles query clause that resolves to a remote folder.
     */
    public void addInRemoteFolder(ItemId remoteFolderId, String subfolderPath,
            boolean includeSubfolders, boolean bool) {
        allResultsQuery = false;
        if (queryTarget != QueryTarget.UNSPECIFIED && !queryTarget.toString().equals(remoteFolderId.getAccountId())) {
            throw new IllegalArgumentException(
                    "Cannot addInClause b/c DBQueryOperation already has an incompatible remote target");
        }
        queryTarget = new QueryTarget(remoteFolderId.getAccountId());
        getTopLeafConstraint().addInRemoteFolder(remoteFolderId, subfolderPath, includeSubfolders, bool);
    }

    public void addInFolder(Folder folder, boolean bool) {
        assert !(folder instanceof Mountpoint) || ((Mountpoint)folder).isLocal() : folder;

        allResultsQuery = false;
        if (bool) {
            // EG: -in:trash is not necessarily a "local" target -- we only imply
            // a target when we're positive
            if (!queryTarget.isCompatibleLocal()) {
                throw new IllegalArgumentException(
                        "Cannot addInClause w/ local target b/c DBQueryOperation already has a remote target");
            }
            queryTarget = QueryTarget.LOCAL;
        }
        getTopLeafConstraint().addInFolder(folder, bool);
    }

    public void addAnyFolder(boolean bool) {
        getTopLeafConstraint().addAnyFolder(bool);
        if (!bool) { // if they are weird enough to say "NOT is:anywhere" then we just make it a no-results-query.
            allResultsQuery = false;
        }
    }

    public void addTag(Tag tag, boolean bool) {
        allResultsQuery = false;
        getTopLeafConstraint().addTag(tag, bool);
    }

    @Override
    public void close() {
        IOUtil.closeQuietly(luceneOp);
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
                        switch (context.getResults().getFetchMode()) {
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
                                assert false : context.getResults().getFetchMode();
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
    private final LRUHashMap<ZimbraHit> mSeenHits = new LRUHashMap<ZimbraHit>(2048, 100);

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
        long start = System.currentTimeMillis();
        results.addAll(context.getMailbox().index.search(constraints, fetch, sort, offset, size,
                context.getParams().inDumpster()));
        ZimbraLog.search.debug("DBSearch elapsed=%d", System.currentTimeMillis() - start);
    }

    private boolean shouldExecuteDbFirst() throws ServiceException {
        // look for item-id or conv-id query parts, if those are set, then we'll execute DB-FIRST
        DbSearchConstraints.Leaf top = getTopLeafConstraint();
        if (top.convId > 0 || !top.itemIds.isEmpty()) {
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

    private void dbFirstGetNextChunk(SortBy sort) throws ServiceException {
        // we want only indexed items from db
        DbSearchConstraints.Leaf sc = getTopLeafConstraint();
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
                        luceneChunk = luceneOp.getNextResultsChunk(MAX_HITS_PER_CHUNK*3);
                        Set<Integer> indexIds = luceneChunk.getIndexIds();
                        if (indexIds.size() < MAX_HITS_PER_CHUNK*3) {
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
    }

    private void luceneFirstGetNextChunk(SortBy sort) throws ServiceException {
        // do the Lucene op first, pass results to DB op
        do {
            // DON'T set an sql LIMIT if we're asking for lucene hits!!!  If we did, then we wouldn't be
            // sure that we'd "consumed" all the Lucene-ID's, and therefore we could miss hits!

            // limit in clause based on Db capabilities - bug 15511
            luceneChunk = luceneOp.getNextResultsChunk(Math.min(Db.getINClauseBatchSize(), hitsPerChunk));

            DbSearchConstraints.Leaf sc = getTopLeafConstraint();
            sc.indexIds.clear();
            sc.indexIds.addAll(luceneChunk.getIndexIds());

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
                // must not ask for offset,limit here b/c of indexId constraints!,
                dbSearch(dbHits, sort, -1, -1);
            }
        } while (dbHits.size() == 0 && !endOfHits);
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
        addCursorConstraint();
        addCalItemExpandRange();

        if (luceneOp != null) {
            hitsPerChunk *= 2; // enlarge chunk size b/c of join
            luceneOp.setDBOperation(this);
            // this is 2nd time to call begin() of this Lucene op.
            luceneOp.begin(new QueryContext(ctx.getMailbox(), ctx.getResults(), ctx.getParams(), hitsPerChunk));
        }
    }

    private void addCalItemExpandRange() {
        SearchParams params = context.getParams();
        if (params.getCalItemExpandStart() > 0) {
            addCalEndDateRange(params.getCalItemExpandStart(), true, -1, false, true);
        }
        if (params.getCalItemExpandEnd() > 0) {
            addCalStartDateRange(-1, false, params.getCalItemExpandEnd(), true, true);
        }
    }

    private void addCursorConstraint() throws ServiceException {
        SearchParams.Cursor cursor = context.getParams().getCursor();
        if (cursor == null) {
            return;
        }
        // bug 35039 - using cursors with conversation-coalescing leads to convs appearing on multiple pages
        if (context.getParams().getTypes().contains(MailItem.Type.CONVERSATION)) {
            return;
        }
        boolean calcOffset = cursor.isIncludeOffset();
        DbSearchConstraints.Leaf offsetConstraints = null; // to calculate the cursor offset
        SortBy sort = context.getParams().getSortBy();
        // in some cases we cannot use cursors, even if they are requested.
        // - Task-sorts cannot be used with cursors (bug 23427) at all.
        // - Conversation mode can use cursors to find the right location in the hits, but we *can't* use a
        //   constrained-offset query to find the right place in the search results....in Conv mode we need to walk
        //   through all the results so that we can guarantee that we only return each Conversation once in a given
        //   results set.
        // - bug:23427 TASK sorts are incompatible with CURSORS, since cursors require real (db-visible) sort fields.
        switch (sort) {
            case NONE:
                throw new IllegalArgumentException("Invalid request: cannot use cursor with SortBy=NONE");
            case TASK_DUE_ASC:
            case TASK_DUE_DESC:
            case TASK_PERCENT_COMPLETE_ASC:
            case TASK_PERCENT_COMPLETE_DESC:
            case TASK_STATUS_ASC:
            case TASK_STATUS_DESC:
            case NAME_LOCALIZED_ASC:
            case NAME_LOCALIZED_DESC:
                return;
            case ID_ASC: {
                int low = Integer.parseInt(cursor.getSortValue());
                int high = cursor.getEndSortValue() != null ?
                        Integer.parseInt(cursor.getEndSortValue()) : -1;
                DbSearchConstraints.Leaf top = getTopLeafConstraint();
                if (calcOffset) {
                    offsetConstraints = top.clone();
                    offsetConstraints.addItemIdRange(-1, false, low, false, true);
                }
                top.addItemIdRange(low, true, high, false, true);
                break;
            }
            case ID_DESC: {
                int high = Integer.parseInt(cursor.getSortValue());
                int low = cursor.getEndSortValue() != null ? Integer.parseInt(cursor.getEndSortValue()) : -1;
                DbSearchConstraints.Leaf top = getTopLeafConstraint();
                if (calcOffset) {
                    offsetConstraints = top.clone();
                    offsetConstraints.addItemIdRange(high, false, -1, false, true);
                }
                top.addItemIdRange(low, false, high, true, true);
                break;
            }
            case DATE_ASC: {
                long low = Long.parseLong(cursor.getSortValue());
                long high = cursor.getEndSortValue() != null ? Long.parseLong(cursor.getEndSortValue()) : -1;
                DbSearchConstraints.Leaf top = getTopLeafConstraint();
                if (calcOffset) {
                    offsetConstraints = top.clone();
                    offsetConstraints.addDateRange(-1, false, low, false, true);
                }
                top.addDateRange(low, true, high, false, true);
                break;
            }
            case DATE_DESC: {
                long high = Long.parseLong(cursor.getSortValue());
                long low = cursor.getEndSortValue() != null ? Long.parseLong(cursor.getEndSortValue()) : -1;
                DbSearchConstraints.Leaf top = getTopLeafConstraint();
                if (calcOffset) {
                    offsetConstraints = top.clone();
                    offsetConstraints.addDateRange(high, false, -1, false, true);
                }
                top.addDateRange(low, false, high, true, true);
                break;
            }
            case SIZE_ASC: {
                long low = Long.parseLong(cursor.getSortValue());
                long high = cursor.getEndSortValue() != null ? Long.parseLong(cursor.getEndSortValue()) : -1;
                DbSearchConstraints.Leaf top = getTopLeafConstraint();
                if (calcOffset) {
                    offsetConstraints = top.clone();
                    offsetConstraints.addSizeRange(-1, false, low, false, true);
                }
                top.addSizeRange(low, true, high, false, true);
                break;
            }
            case SIZE_DESC: {
                long high = Long.parseLong(cursor.getSortValue());
                long low = cursor.getEndSortValue() != null ? Long.parseLong(cursor.getEndSortValue()) : -1;
                DbSearchConstraints.Leaf top = getTopLeafConstraint();
                if (calcOffset) {
                    offsetConstraints = top.clone();
                    offsetConstraints.addSizeRange(high, false, -1, false, true);
                }
                top.addSizeRange(low, false, high, true, true);
                break;
            }
            case READ_ASC:
            case SUBJ_ASC:
            case NAME_ASC:
            case ATTACHMENT_ASC:
            case FLAG_ASC:
            case PRIORITY_ASC: {
                String low = cursor.getSortValue();
                String high = cursor.getEndSortValue();
                DbSearchConstraints.Leaf top = getTopLeafConstraint();
                if (calcOffset) {
                    offsetConstraints = top.clone();
                    offsetConstraints.setCursorRange(null, false, low, false, sort);
                }
                top.setCursorRange(low, true, high, false, sort);
                break;
            }
            case READ_DESC:
            case SUBJ_DESC:
            case NAME_DESC:
            case ATTACHMENT_DESC:
            case FLAG_DESC:
            case PRIORITY_DESC: {
                String high = cursor.getSortValue();
                String low = cursor.getEndSortValue();
                DbSearchConstraints.Leaf top = getTopLeafConstraint();
                if (calcOffset) {
                    offsetConstraints = top.clone();
                    offsetConstraints.setCursorRange(high, false, null, false, sort);
                }
                top.setCursorRange(low, false, high, true, sort);
                break;
            }
            default:
                break;
        }

        if (offsetConstraints != null) {
            assert cursorOffset < 0 : cursorOffset;
            Mailbox mbox = context.getMailbox();
            DbConnection conn = DbPool.getConnection(mbox);
            try {
                cursorOffset = new DbSearch(mbox, context.getParams().inDumpster()).countResults(conn, offsetConstraints);
            } finally {
                conn.closeQuietly();
            }
        }
    }

    @Override
    QueryOperation optimize(Mailbox mbox) {
        return this;
    }

    @Override
    String toQueryString() {
        StringBuilder out = new StringBuilder("(");
        if (luceneOp != null) {
            out.append(luceneOp.toQueryString()).append(" AND ");
        }
        constraints.toQueryString(out);
        return out.append(')').toString();
    }

    /**
     * Part of fix for Bug 79576 - invalid hits in shared folders being returned.
     * Check whether this is an AND search including a search for a non-existent tag.
     * WARNING - should only be used for local searches - that tag might exist for a shared folder.
     */
    public boolean isSearchForNonexistentLocalTag(Mailbox mbox) {
        if (allResultsQuery || includeIsRemoteFolders || luceneOp != null) {
            return false;
        }
        if (constraints instanceof DbSearchConstraints.Intersection) {
            DbSearchConstraints.Intersection intersection = (DbSearchConstraints.Intersection) constraints;
            for (DbSearchConstraints child : intersection.getChildren()) {
                if (child instanceof DbSearchConstraints.Leaf) {
                    DbSearchConstraints.Leaf leaf = (DbSearchConstraints.Leaf) child;
                    for (Tag tag : leaf.tags) {
                        try {
                            mbox.getTagByName(null, tag.getName());
                        } catch (MailServiceException mse) {
                            if (MailServiceException.NO_SUCH_TAG.equals(mse.getCode())) {
                                return true;
                            }
                        } catch (ServiceException e) {
                        }
                    }
                }
            }
        }
        return false;

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
        assert(dbHits == null);
        assert(dbHitsIter == null);
        assert(luceneChunk == null);

        DBQueryOperation result = (DBQueryOperation) super.clone();
        result.constraints = (DbSearchConstraints) constraints.clone();
        result.excludeTypes.addAll(excludeTypes);
        result.nextHits = new ArrayList<ZimbraHit>();
        return result;
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
                constraints = constraints.or(dbOther.constraints);
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

            constraints = constraints.and(dbOther.constraints);

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
            DbConnection conn = DbPool.getConnection(mbox);
            try {
                dbHitCount = new DbSearch(mbox, context.getParams().inDumpster()).countResults(conn, constraints);
            } finally {
                conn.closeQuietly();
            }
        }
        return dbHitCount;
    }

    @Override
    public long getCursorOffset() {
        return cursorOffset;
    }

}
