/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.ZimbraIndexReader.TermFieldEnumeration;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.util.IOUtil;

/**
 * {@link QueryOperation} which queries Lucene.
 */
public final class LuceneQueryOperation extends QueryOperation {
    private static final float DB_FIRST_TERM_FREQ_PERC;
    static {
        float f = 0.8f;
        try {
            f = Float.parseFloat(LC.search_dbfirst_term_percentage_cutoff.value());
        } catch (Exception e) {
        }
        if (f < 0.0 || f > 1.0) {
            f = 0.8f;
        }
        DB_FIRST_TERM_FREQ_PERC = f;
    }

    private int curHitNo = 0; // our offset into the hits
    private boolean haveRunSearch = false;
    private String queryString = "";
    private Query luceneQuery;

    /**
     * Used for doing DB-joins: the list of terms for the filter one of the
     * terms in the list MUST occur in the document for it to match.
     */
    private List<Term> filterTerms;

    /**
     * Because we don't store the real mail-item-id of documents, we ALWAYS need
     * a DBOp in order to properly get our results.
     */
    private DBQueryOperation dbOp;
    private final List<QueryInfo> queryInfo = Lists.newArrayList();
    private boolean hasSpamTrashSetting = false;

    private ZimbraTopDocs hits;
    private int topDocsLen = 0; // number of hits fetched
    private int topDocsChunkSize = 2000; // how many hits to fetch per step in Lucene
    private ZimbraIndexSearcher searcher;
    private Sort sort;

    /**
     * Adds the specified text clause at the top level.
     * <p>
     * e.g. going in "a b c" if we addClause("d") we get "a b c d".
     *
     * @param queryStr Appended to the end of the text-representation of this query
     * @param query Lucene query
     * @param bool allows for negated query terms
     */
    public void addClause(String queryStr, Query query, boolean bool) {
        assert(!haveRunSearch);

        // ignore empty BooleanQuery
        if (query instanceof BooleanQuery && ((BooleanQuery) query).clauses().isEmpty()) {
            return;
        }

        if (queryString.isEmpty()) {
            queryString = (bool ? "" : "-") + queryStr;
        } else {
            queryString = queryString + " " + (bool ? "" : "-") + queryStr;
        }

        if (bool) {
            if (luceneQuery == null) {
                luceneQuery = query; //no boolean query necessary
            } else {
                BooleanClause newClause = new BooleanClause(query, Occur.MUST);
                updateBoolQuery(newClause);
            }
        } else {
            BooleanClause newClause = new BooleanClause(query, Occur.MUST_NOT);
            updateBoolQuery(newClause);
        }
    }

    private void updateBoolQuery(BooleanClause newClause) {
        BooleanQuery.Builder newQuery = new BooleanQuery.Builder();
        newQuery.add(newClause);
        if (luceneQuery == null) {
            luceneQuery = newQuery.build();
        }
        if (luceneQuery instanceof BooleanQuery) {
            for (BooleanClause clause: ((BooleanQuery) luceneQuery).clauses()) {
                newQuery.add(clause);
            }
            luceneQuery = newQuery.build();
        } else {
            newQuery.add(luceneQuery, Occur.MUST);
            luceneQuery = newQuery.build();
        }
    }
    /**
     * Adds the specified text clause ANDED with the existing query.
     * <p>
     * e.g. going in w/ "a b c" if we addAndedClause("d") we get "(a b c) AND d".
     * <p>
     * This API may only be called AFTER query optimizing and AFTER remote queries have been split.
     * <p>
     * Note that this API does *not* update the text-representation of this query.
     */
    void addAndedClause(Query query, boolean bool) {
        assert(luceneQuery != null);
        haveRunSearch = false; // will need to re-run the search with this new clause
        curHitNo = 0;

        if (luceneQuery instanceof BooleanQuery) {
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            BooleanQuery bquery = ((BooleanQuery) luceneQuery);
            boolean orOnly = true;
            for (BooleanClause clause : bquery) {
                if (clause.getOccur() != BooleanClause.Occur.SHOULD) {
                    orOnly = false;
                    break;
                }
            }
            if (!orOnly) {
                for (BooleanClause clause: bquery) {
                    builder.add(clause); //copy over existing clauses
                }
                builder.add(new BooleanClause(query, bool ? BooleanClause.Occur.MUST : BooleanClause.Occur.MUST_NOT));
                luceneQuery = builder.build();
                return;
            }
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(luceneQuery, BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(query, bool ? BooleanClause.Occur.MUST : BooleanClause.Occur.MUST_NOT));
        luceneQuery = builder.build();
    }

    /**
     * Adds the specified text clause as a filter over the existing query.
     * <p>
     * e.g. going in w/ "a b c" if we addAndedClause("d") we get "(a b c) AND d".
     * <p>
     * This API is used by the query executor so that it can temporarily add a bunch of indexIds to the existing query
     * -- this is necessary when we are doing a DB-first query plan execution.
     * <p>
     * Note that this API does *not* update the text-representation of this query.
     */
    void addFilterClause(Term t) {
        haveRunSearch = false; // will need to re-run the search with this new clause
        curHitNo = 0;

        if (filterTerms == null) {
            filterTerms = new ArrayList<Term>();
        }
        filterTerms.add(t);
    }

    /**
     * Clears the filter clause
     */
    void clearFilterClause() {
        filterTerms = null;
    }

    /**
     * Sets the text query *representation* manually -- the thing that is output if we have to proxy this search
     * somewhere else -- used when dealing with wildcard searches.
     */
    public void setQueryString(String value) {
        assert(queryString.isEmpty());
        queryString = value;
    }

    @Override
    public String toQueryString() {
        return '(' + queryString + ')';
    }

    /**
     * Returns {@code true} if we think this query is best evaluated DB-FIRST.
     * @throws ServiceException
     */
    boolean shouldExecuteDbFirst() throws ServiceException {
        if (searcher == null || luceneQuery == null) {
            return true;
        }

        if (luceneQuery instanceof TermQuery) {
            TermQuery query = (TermQuery) luceneQuery;
            Term term = query.getTerm();
            long start = System.currentTimeMillis();
            try {
                int freq = searcher.docFreq(term);
                int docsCutoff = (int) (searcher.getIndexReader().numDocs() * DB_FIRST_TERM_FREQ_PERC);
                ZimbraLog.search.debug("LuceneDocFreq freq=%d,cutoff=%d(%d%%),elapsed=%d",
                        freq, docsCutoff, (int) (100 * DB_FIRST_TERM_FREQ_PERC), System.currentTimeMillis() - start);
                if (freq > docsCutoff) {
                    return true;
                }
            } catch (IOException e) {
                return false;
            }
        }

        try {
            //TODO count results using TotalHitCountCollector
            fetchFirstResults(1000); // some arbitrarily large initial size to fetch
            if (getTotalHitCount() > 1000) { // also arbitrary, just to make very small searches run w/o extra DB check
                //Bug: 68630
                //Let's try to avoid the additional D/B lookup;
                //We can calculate the number of items contained by the folders to search by getting the total
                //item counts from the cache. If total items < total lucene hits - its cheaper to do the D/B query first.
                Set<Folder> targetFolders = dbOp.getTargetFolders();
                if (targetFolders != null && targetFolders.size() > 0) {
                    long itemCount = getTotalItemCount(targetFolders);
                    ZimbraLog.search.debug("lucene hits=%d, folders item count=%d", getTotalHitCount(), itemCount);
                    if (itemCount < getTotalHitCount())
                        return true; // run DB-FIRST
                }

                int dbHitCount = dbOp.getDbHitCount();
                ZimbraLog.search.debug("EstimatedHits lucene=%d,db=%d", getTotalHitCount(), dbHitCount);
                if (dbHitCount < getTotalHitCount()) {
                    return true; // run DB-FIRST
                }
            }
            return false;
        } catch (ServiceException e) {
            return false;
        }
    }

    private long getTotalItemCount(Set<Folder> folders) {
        long total = 0;
        for (Folder f : folders)
            total += f.getItemCount();

        return total;
    }

    @Override
    public void close() {
        IOUtil.closeQuietly(searcher);
        searcher = null;
    }

    private void fetchFirstResults(int initialChunkSize) {
        if (!haveRunSearch) {
            assert(curHitNo == 0);
            topDocsLen = 3 * initialChunkSize;
            runSearch();
        }
    }

    /**
     * Fetch the next chunk of results.
     * <p>
     * Called by a {@link DBQueryOperation} that is wrapping us in a DB-First query plan: gets a chunk of results that
     * it feeds into a SQL query.
     */
    LuceneResultsChunk getNextResultsChunk(int max) {
        if (!haveRunSearch) {
            fetchFirstResults(max);
        }

        long start = System.currentTimeMillis();
        LuceneResultsChunk result = new LuceneResultsChunk();
        int luceneLen = hits != null ? hits.getTotalHits() : 0;
        while ((result.size() < max) && (curHitNo < luceneLen)) {
            if (topDocsLen <= curHitNo) {
                topDocsLen += topDocsChunkSize;
                topDocsChunkSize *= 4;
                if (topDocsChunkSize > 1000000) {
                    topDocsChunkSize = 1000000;
                }
                if (topDocsLen > luceneLen) {
                    topDocsLen = luceneLen;
                }
                runSearch();
            }

            Document doc;
            try {
                doc = searcher.doc(hits.getScoreDoc(curHitNo).getDocumentID());
            } catch (Exception e) {
                ZimbraLog.search.error("Failed to retrieve Lucene document: %s",
                        hits.getScoreDoc(curHitNo).getDocumentID().toString(), e);
                return result;
            }
            curHitNo++;
            String mbid = doc.get(LuceneFields.L_MAILBOX_BLOB_ID);
            if (mbid != null) {
                try {
                    result.addHit(Integer.parseInt(mbid), doc);
                } catch (NumberFormatException e) {
                    ZimbraLog.search.error("Invalid MAILBOX_BLOB_ID: " + mbid, e);
                }
            }
        }
        ZimbraLog.search.debug("LuceneFetchDocs n=%d,elapsed=%d", luceneLen, System.currentTimeMillis() - start);
        return result;
    }

    /**
     * It is not possible to search for queries that only consist of a MUST_NOT clause. Combining with MatchAllDocsQuery
     * works in general, but we generate more than one documents per item for multipart messages. If we match including
     * non top level parts, negative queries will end up matching everything. Therefore we only match the top level part
     * for negative queries.
     */
    private BooleanQuery fixMustNotOnly(BooleanQuery query) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        boolean hasMustNotClause = false;
        for (BooleanClause clause : query.clauses()) {
            if (clause.getQuery() instanceof BooleanQuery) {
                Query fixed = fixMustNotOnly((BooleanQuery) clause.getQuery());
                builder.add(fixed, clause.getOccur());
            } else {
                builder.add(clause);
            }
            if (clause.getOccur() == BooleanClause.Occur.MUST_NOT) {
                hasMustNotClause = true;
            }
        }
        if (hasMustNotClause) {
            builder.add(new TermQuery(new Term(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_TOP)),
                    BooleanClause.Occur.SHOULD);
            Set<MailItem.Type> types = context.getParams().getTypes();
            if (types.contains(MailItem.Type.CONTACT)) {
                builder.add(new TermQuery(new Term(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_CONTACT)),
                        BooleanClause.Occur.SHOULD);
            }
            if (types.contains(MailItem.Type.NOTE)) {
                builder.add(new TermQuery(new Term(LuceneFields.L_PARTNAME, LuceneFields.L_PARTNAME_NOTE)),
                        BooleanClause.Occur.SHOULD);
            }
        }
        return builder.build();
    }

    /**
     * Execute the actual search via Lucene
     */
    private void runSearch() {
        haveRunSearch = true;

        if (searcher == null) { // this can happen if the Searcher couldn't be opened, e.g. index does not exist
            hits = null;
            return;
        }

        try {
            if (luceneQuery instanceof BooleanQuery) {
                luceneQuery = fixMustNotOnly((BooleanQuery) luceneQuery);
            }
            if (luceneQuery == null) { // optimized away
                hits = null;
                return;
            }
            ZimbraTermsFilter filter = (filterTerms != null) ? new ZimbraTermsFilter(filterTerms) : null;
            long start = System.currentTimeMillis();
            if (sort == null) {
                hits = searcher.search(luceneQuery, filter, topDocsLen);
            } else {
                hits = searcher.search(luceneQuery, filter, topDocsLen, sort);
            }
            ZimbraLog.search.debug("LuceneSearch query=%s,n=%d,total=%d,elapsed=%d",
                    luceneQuery, topDocsLen, hits.getTotalHits(), System.currentTimeMillis() - start);
        } catch (IOException | ServiceException e) {
            ZimbraLog.search.error("Failed to search query=%s", luceneQuery, e);
            IOUtil.closeQuietly(searcher);
            searcher = null;
            hits = null;
        }
    }

    @Override
    public String toString() {
        return "LUCENE(" + luceneQuery + (hasSpamTrashSetting() ? " <ANYWHERE>" : "") + ")";
    }

    /**
     * Just clone *this* object, don't clone the embedded DBOp
     */
    private LuceneQueryOperation cloneInternal() {
        assert(!haveRunSearch);
        LuceneQueryOperation clone = (LuceneQueryOperation) super.clone();
        clone.luceneQuery = luceneQuery; //does this work? Queries are immutable in lucene 6
        return clone;
    }

    @Override
    public Object clone() {
        assert(searcher == null);
        LuceneQueryOperation toRet = cloneInternal();
        if (dbOp != null) {
            toRet.dbOp = (DBQueryOperation) dbOp.clone(this);
        }
        return toRet;
    }

    /**
     * Called from {@link DBQueryOperation#clone()}
     *
     * @param caller - our DBQueryOperation which has ALREADY BEEN CLONED
     */
    Object clone(DBQueryOperation caller) {
        assert(searcher == null);
        LuceneQueryOperation toRet = cloneInternal();
        toRet.setDBOperation(caller);
        return toRet;
    }

    /**
     * Must be called AFTER the first results chunk is fetched.
     *
     * @return number of hits in this search
     */
    private long getTotalHitCount() {
        return hits != null ? hits.getTotalHits() : 0;
    }

    @Override
    public long getCursorOffset() {
        return -1;
    }

    /**
     * Reset our hit iterator back to the beginning of the result set.
     */
    void resetDocNum() {
        curHitNo = 0;
    }

    @Override
    protected QueryOperation combineOps(QueryOperation other, boolean union) {
        assert(!haveRunSearch);

        if (union) {
            if (other.hasNoResults()) {
                queryInfo.addAll(other.getResultInfo());
                // a query for (other OR nothing) == other
                return this;
            }
        } else {
            if (other.hasAllResults()) {
                if (other.hasSpamTrashSetting()) {
                    forceHasSpamTrashSetting();
                }
                queryInfo.addAll(other.getResultInfo());
                // we match all results.  (other AND anything) == other
                return this;
            }
        }

        if (other instanceof LuceneQueryOperation) {
            LuceneQueryOperation otherLucene = (LuceneQueryOperation) other;
            if (union) {
                queryString = '(' + queryString + ") OR (" + otherLucene.queryString + ')';
            } else {
                queryString = '(' + queryString + ") AND (" + otherLucene.queryString + ')';
            }

            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            if (union) {
                if (luceneQuery instanceof BooleanQuery) {
                    orCopy((BooleanQuery) luceneQuery, builder);
                } else {
                    builder.add(new BooleanClause(luceneQuery, Occur.SHOULD));
                }
                if (otherLucene.luceneQuery instanceof BooleanQuery) {
                    orCopy((BooleanQuery) otherLucene.luceneQuery, builder);
                } else {
                    builder.add(new BooleanClause(otherLucene.luceneQuery, Occur.SHOULD));
                }
            } else {
                if (luceneQuery instanceof BooleanQuery) {
                    andCopy((BooleanQuery) luceneQuery, builder);
                } else {
                    builder.add(new BooleanClause(luceneQuery, Occur.MUST));
                }
                if (otherLucene.luceneQuery instanceof BooleanQuery) {
                    andCopy((BooleanQuery) otherLucene.luceneQuery, builder);
                } else {
                    builder.add(new BooleanClause(otherLucene.luceneQuery, Occur.MUST));
                }
            }
            luceneQuery = builder.build();
            queryInfo.addAll(other.getResultInfo());
            if (other.hasSpamTrashSetting()) {
                forceHasSpamTrashSetting();
            }
            return this;
        }
        return null;
    }

    private void andCopy(BooleanQuery from, BooleanQuery.Builder to) {
        boolean allAnd = true;
        for (BooleanClause clause : from) {
            if (clause.getOccur() == BooleanClause.Occur.SHOULD) {
                allAnd = false;
                break;
            }
        }
        if (allAnd) {
            for (BooleanClause clause : from) {
                to.add(clause);
            }
        } else {
            to.add(new BooleanClause(from, Occur.MUST));
        }
    }

    private void orCopy(BooleanQuery from, BooleanQuery.Builder to) {
        boolean allOr = true;
        for (BooleanClause clause : from) {
            if (clause.getOccur() != BooleanClause.Occur.SHOULD) {
                allOr = false;
                break;
            }
        }
        if (allOr) {
            for (BooleanClause clause : from) {
                to.add(clause);
            }
        } else {
            to.add(new BooleanClause(from, Occur.SHOULD));
        }
    }

    @Override
    void forceHasSpamTrashSetting() {
        hasSpamTrashSetting = true;
    }

    List<QueryInfo> getQueryInfo() {
        return queryInfo;
    }

    public String getQueryString() {
        return queryString;
    }

    public Query getQuery() {
        return luceneQuery;
    }

    @Override
    boolean hasSpamTrashSetting() {
        return hasSpamTrashSetting;
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
    Set<QueryTarget> getQueryTargets() {
        return ImmutableSet.of(QueryTarget.UNSPECIFIED);
    }

    void setDBOperation(DBQueryOperation op) {
        dbOp = op;
    }

    @Override
    public void resetIterator() throws ServiceException {
        if (dbOp != null) {
            dbOp.resetIterator();
        }
    }

    @Override
    public ZimbraHit getNext() throws ServiceException {
        if (dbOp != null) {
            return dbOp.getNext();
        }
        return null;
    }

    @Override
    public ZimbraHit peekNext() throws ServiceException {
        if (dbOp != null) {
            return dbOp.peekNext();
        }
        return null;
    }

    @Override
    public List<QueryInfo> getResultInfo() {
        List<QueryInfo> toRet = new ArrayList<QueryInfo>();
        toRet.addAll(queryInfo);
        if (dbOp != null) {
            toRet.addAll(dbOp.getQueryInfo());
        }
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
        if (dbOp != null) {
            dbOp.depthFirstRecurse(cb);
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
        queryInfo.add(inf);
    }

    /**
     * Can be called more than once recursively from {@link DBQueryOperation}.
     */
    @Override
    protected final void begin(QueryContext ctx) throws ServiceException {
        assert(!haveRunSearch);
        context = ctx;
        if (dbOp == null) { // 1st time called
            // wrap ourselves in a DBQueryOperation, since we're eventually
            // going to need to go to the DB
            dbOp = new DBQueryOperation();
            dbOp.setLuceneQueryOperation(this);
            dbOp.begin(ctx); // will call back into this method again!
        } else { // 2nd time called
            try {
                searcher = ctx.getMailbox().index.getIndexStore().openSearcher();
            } catch (IOException e) {
                throw ServiceException.FAILURE("Failed to open searcher", e);
            }
            sort = toLuceneSort(ctx.getResults().getSortBy());
        }
    }

    private Sort toLuceneSort(SortBy sortBy) {
        if (sortBy == null) {
            return null;
        }

        switch (sortBy.getKey()) {
            case NONE:
                return null;
            case NAME:
            case NAME_NATURAL_ORDER:
            case SENDER:
                return new Sort(new SortField(LuceneFields.L_SORT_NAME, SortField.Type.STRING,
                        sortBy.getDirection() == SortBy.Direction.DESC));
            case SUBJECT:
                return new Sort(new SortField(LuceneFields.L_SORT_SUBJECT, SortField.Type.STRING,
                        sortBy.getDirection() == SortBy.Direction.DESC));
            case SIZE:
                return new Sort(new SortField(LuceneFields.L_SORT_SIZE, SortField.Type.LONG,
                        sortBy.getDirection() == SortBy.Direction.DESC));
            case ATTACHMENT:
                return new Sort(new SortField(LuceneFields.L_SORT_ATTACH, SortField.Type.STRING,
                        sortBy.getDirection() == SortBy.Direction.DESC));
            case FLAG:
                return new Sort(new SortField(LuceneFields.L_SORT_FLAG, SortField.Type.STRING,
                        sortBy.getDirection() == SortBy.Direction.DESC));
            case PRIORITY:
                return new Sort(new SortField(LuceneFields.L_SORT_PRIORITY, SortField.Type.STRING,
                        sortBy.getDirection() == SortBy.Direction.DESC));
            case RCPT:
                assert false : sortBy; // should already be checked in the compile phase
            case RELEVANCE:
                return new Sort(new SortField(LuceneFields.L_SORT_RELEVANCE, SortField.Type.FLOAT,
                        sortBy.getDirection() == SortBy.Direction.DESC));
            case DATE:
            default: // default to DATE_DESCENDING
                return new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.Type.STRING,
                        sortBy.getDirection() == SortBy.Direction.DESC));
        }
    }

    /**
     * We use this data structure to track a "chunk" of Lucene hits which the {@link DBQueryOperation} will use to check
     * against the DB.
     */
    static final class LuceneResultsChunk {
        private final Multimap<Integer, Document> hits = LinkedHashMultimap.create();

        Set<Integer> getIndexIds() {
            return hits.keySet();
        }

        int size() {
            return hits.size();
        }

        void addHit(int indexId, Document doc) {
            hits.put(indexId, doc);
        }

        Collection<Document> getHit(int indexId) {
            return hits.get(indexId);
        }
    }
}
