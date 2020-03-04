package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.FacetParams;

import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.zimbra.common.httpclient.ZimbraHttpClientManager;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.ZimbraIndexDocumentID;
import com.zimbra.cs.index.ZimbraIndexReader;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.index.ZimbraScoreDoc;
import com.zimbra.cs.index.ZimbraTermsFilter;
import com.zimbra.cs.index.ZimbraTopDocs;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;
import com.zimbra.cs.mailbox.MailboxIndex.ItemIndexDeletionInfo;
import com.zimbra.cs.util.IOUtil;

/**
 * IndexStore implementation for standalone Solr or SolrCloud backends
 * @author Greg Solovyev
 * @author raykini
 */

public class SolrIndex extends IndexStore {
    protected static final String CMD_INDEX_VERSION = "indexversion";
    protected static final String COMMAND = "command";
    protected static final String GENERATION = "generation";
    protected static final String SOLR_SCORE_FIELD = "score";
    protected static final String[] MESSAGE_FETCH_FIELDS = new String[] {
        LuceneFields.L_PARTNAME,
        LuceneFields.L_FILENAME,
        LuceneFields.L_MAILBOX_BLOB_ID,
        LuceneFields.L_SORT_DATE,
        LuceneFields.L_VERSION,
        LuceneFields.L_MIMETYPE
    };

    protected static final String CMD_GET_FILE_LIST = "filelist";
    protected String accountId;
    protected SolrRequestHelper solrHelper;

    protected static final Cache<Integer, SolrIndexSearcher> SEARCHER_CACHE =
            CacheBuilder.newBuilder()
            .maximumSize(LC.zimbra_index_reader_cache_size.intValue())
            .expireAfterAccess(LC.zimbra_index_reader_cache_ttl.longValue(), TimeUnit.MILLISECONDS)
            .removalListener(new RemovalListener<Integer, SolrIndexSearcher>() {
                @Override
                public void onRemoval(RemovalNotification<Integer, SolrIndexSearcher> notification) {
                    IOUtil.closeQuietly(notification.getValue());
                }
            })
            .build();

    // Bug: 60631
    // cache lucene index of GAL sync account separately with no automatic eviction
    protected static final ConcurrentMap<Integer, SolrIndexSearcher> GAL_SEARCHER_CACHE =
        new ConcurrentLinkedHashMap.Builder<Integer, SolrIndexSearcher>()
        .maximumWeightedCapacity(LC.zimbra_galsync_index_reader_cache_size.intValue())
        .listener(new EvictionListener<Integer, SolrIndexSearcher>() {
            @Override
            public void onEviction(Integer mboxId, SolrIndexSearcher searcher) {
                IOUtil.closeQuietly(searcher);
            }
        })
        .build();

    public SolrIndex(String accountId, SolrRequestHelper solrHelper) {
        this.accountId = accountId;
        this.solrHelper = solrHelper;
    }

    @Override
    public void warmup() {
        if (SEARCHER_CACHE.asMap().containsKey(accountId) ||
                GAL_SEARCHER_CACHE.containsKey(accountId)) {
            return; // already warmed up
        }
        long start = System.currentTimeMillis();
        IndexSearcher searcher = null;
        try {
            searcher = (IndexSearcher) openSearcher();
            searcher.search(new TermQuery(new Term(LuceneFields.L_CONTENT, "zimbra")), 1,
                    new Sort(new SortField(LuceneFields.L_SORT_DATE, SortField.Type.STRING, true)));
        } catch (IOException | ServiceException e) {
            ZimbraLog.search.warn("Failed to warm up", e);
        }
        ZimbraLog.search.debug("WarmupSolrSearched elapsed=%d", System.currentTimeMillis() - start);

    }

    @Override
    public boolean isPendingDelete() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setPendingDelete(boolean pendingDelete) {
        // TODO Auto-generated method stub

    }

    @Override
    public void optimize() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean verify(PrintStream out) throws IOException {
        // TODO Auto-generated method stub
        return false;
    }

    protected void addTermsFilter(SolrQuery query, Collection<Term> terms) {
        addTermsFilter(query, terms, false);
    }

    protected void addTermsFilter(SolrQuery query, Collection<Term> terms, boolean usePostFilter) {
        if(terms == null || terms.size() < 1) {
            return;
        }
        Multimap<String, String> byField = ArrayListMultimap.create();
        for (Term term: terms) {
            byField.put(term.field(), term.text());
        }
        for (Map.Entry<String, Collection<String>> entry: byField.asMap().entrySet()) {
            String field = entry.getKey();
            Collection<String> values = entry.getValue();
            String filterQuery;
            if (usePostFilter) {
                filterQuery = SolrUtils.getZimbraIdPostFilter(accountId, field, values);
            } else {
                filterQuery = SolrUtils.getTermsFilter(field, values);
            }
            query.addFilterQuery(filterQuery);
        }
    }

    protected String termToQuery(Term term) {
        if (term == null) {
            return "";
        }
        TermQuery query;
        if (SolrUtils.containsWhitespace(term.text())) {
            Term quoted = new Term(term.field(), SolrUtils.quoteText(term.text()));
            query = new TermQuery(quoted);
        } else {
            query = new TermQuery(term);
        }
        return query.toString();
    }

    protected class SolrIndexSearcher implements ZimbraIndexSearcher {
        final SolrIndexReader reader;

        public SolrIndexSearcher(SolrIndexReader reader) {
            this.reader = reader;
        }
        @Override
        public void close() throws IOException {
            // nothing to do here
        }

        private Document buildFullDocument(SolrDocument solrDoc) {
            Document document = new Document();
            for(String fieldName : solrDoc.getFieldNames()) {
                document.add(new Field(fieldName, solrDoc.getFieldValue(fieldName).toString(), StoredField.TYPE));
            }
            return document;
        }

        @Override
        public Document doc(ZimbraIndexDocumentID docID) throws IOException, ServiceException {
            if (docID == null) {
                return null;
            }
            if (docID instanceof ZimbraSolrDocumentID) {
                ZimbraSolrDocumentID solrID = (ZimbraSolrDocumentID) docID;
                SolrQuery q = new SolrQuery().setQuery(String.format("%s:%s", solrID.getIDFIeld(), solrID.getDocID())).setRows(1);
                q.setFields(MESSAGE_FETCH_FIELDS);
                ZimbraLog.search.debug(String.format("retrieving document by query %s ",q.toString()));
                try {
                    QueryResponse resp = (QueryResponse) solrHelper.executeQueryRequest(accountId, q);
                    SolrDocument solrDoc = resp.getResults().get(0);
                    return buildFullDocument(solrDoc);
                } catch (SolrException e) {
                    ZimbraLog.search.error("Solr problem geting document %s, from mailbox %s",docID.toString(), accountId,e);
                }
                return null;
            }
            throw new IllegalArgumentException("Expected a ZimbraSolrDocumentID");
        }

        @Override
        public int docFreq(Term term) throws IOException, ServiceException {
            try {
                SolrQuery q = solrHelper.newQuery(accountId).setQuery("*:*").setRows(0);
                if (SolrUtils.isWildcardQuery(term.text()) || SolrUtils.containsWhitespace(term.text())) {
                    //can't use a terms filter
                q.addFilterQuery(termToQuery(escapeSpecialChars(term)));
                } else {
                    addTermsFilter(q, Arrays.asList(term));
                }
                QueryResponse resp = (QueryResponse) solrHelper.executeQueryRequest(accountId, q);
                return (int) resp.getResults().getNumFound();
            } catch (SolrException e) {
                ZimbraLog.index.error("Solr search problem getting docFreq for mailbox %s", accountId,e);
            }
            return 0;
        }

        @Override
        public ZimbraIndexReader getIndexReader() {
            return reader;
        }

        @Override
        public ZimbraTopDocs search(Query query, int n) throws IOException, ServiceException {
            return search(query,null, n);
        }

        @Override
        public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter, int n)
                throws IOException, ServiceException {
            return search(query, filter, n, null);
        }

        @Override
        public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter,
                int n, Sort sort) throws IOException, ServiceException {
            return search(query, filter, n, sort, LuceneFields.L_MAILBOX_BLOB_ID, MESSAGE_FETCH_FIELDS);
        }

        @Override
        public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter,
                int n, Sort sort, String idField, String[] fetchFields) throws IOException, ServiceException {
            List<ZimbraScoreDoc>scoreDocs = Lists.newArrayList();
            List<SortField> sortFields = Lists.newArrayList();
            List<IndexDocument> indexDocs = Lists.newArrayList();
            float maxScore = 0;
            int totalHits = 0;

            if (sort != null) {
                Collections.addAll(sortFields, sort.getSort());
            }
            query = escapeSpecialChars(query);
            ReferencedQueryParams referencedQueryParams = new ReferencedQueryParams();
            SolrQuery q = solrHelper.newQuery(accountId);
            referencedQueryParams.setAsParams(q);
            q.setQuery(query.toString()).setRows(n);

            if(filter != null) {
                addTermsFilter(q, filter.getTerms(), true);
            }

            String[] fields = fetchFields != null ? new String[fetchFields.length + 2] : new String[2];
            fields[0] = idField;
            fields[1] = SOLR_SCORE_FIELD;
            if (fetchFields != null) {
                for (int i=0; i<fetchFields.length; i++) {
                    fields[i+2] = fetchFields[i];
                }
            }
            q.setFields(fields);
            for (SortField sortField : sortFields) {
                q.addSort(sortField.getField(), sortField.getReverse() ? SolrQuery.ORDER.desc : SolrQuery.ORDER.asc);
            }

            ZimbraLog.search.debug("Searching Solr:Query='%s'->SolrQuery='%s' filter='%s'",
                    query, q, filter);
            try {
                QueryResponse resp = (QueryResponse) solrHelper.executeQueryRequest(accountId, q);
                SolrDocumentList solrDocList = resp.getResults();
                totalHits = (int) solrDocList.getNumFound();
                for(SolrDocument solrDoc : solrDocList) {
                    Float score = new Float(0);
                    try {
                        score = (Float)solrDoc.getFieldValue(SOLR_SCORE_FIELD);
                    } catch (RuntimeException e) {
                        score = new Float(0);
                    }
                    maxScore = Math.max(maxScore, score);
                    indexDocs.add(toIndexDocument(solrDoc));
                    scoreDocs.add(ZimbraScoreDoc.create(new ZimbraSolrDocumentID(
                            idField, solrDoc.getFieldValue(idField).toString()), score));
                }
            } catch (SolrException e) {
                ZimbraLog.index.error("Solr search problem mailbox %s, query %s", accountId, query, e);
                return ZimbraTopDocs.create(totalHits, scoreDocs, maxScore, indexDocs);
            }
            return ZimbraTopDocs.create(totalHits, scoreDocs, maxScore, indexDocs);
        }

        private Term escapeSpecialChars(Term term) {
            String escaped = SolrUtils.escapeSpecialChars(term.text());
            return new Term(term.field(), escaped);
        }

        private Query escapeSpecialChars(Query query) {
            if (query instanceof TermQuery) {
                Term term = ((TermQuery) query).getTerm();
                return new TermQuery(escapeSpecialChars(term));
            } else if (query instanceof BoostQuery) {
                BoostQuery boostQuery = (BoostQuery) query;
                return new BoostQuery(escapeSpecialChars(boostQuery.getQuery()), boostQuery.getBoost());
            } else if (query instanceof PrefixQuery) {
                Term term = ((PrefixQuery) query).getPrefix();
                String escaped = SolrUtils.escapeSpecialChars(term.text());
                return new PrefixQuery(new Term(term.field(), escaped));
            } else if (query instanceof BooleanQuery) {
                List<BooleanClause> clauses = ((BooleanQuery) query).clauses();
                BooleanQuery.Builder builder = new BooleanQuery.Builder();
                for (BooleanClause clause: clauses) {
                    builder.add(escapeSpecialChars(clause.getQuery()), clause.getOccur());
                }
                return builder.build();
            } else if (query instanceof DisjunctionMaxQuery) {
                DisjunctionMaxQuery dismax = (DisjunctionMaxQuery) query;
                List<Query> disjuncts = (dismax).getDisjuncts();
                List<Query> escapedDisjuncts = new ArrayList<Query>(disjuncts.size());
                for (Query disjunct: disjuncts) {
                    escapedDisjuncts.add(escapeSpecialChars(disjunct));
                }
                return new DisjunctionMaxQuery(escapedDisjuncts, dismax.getTieBreakerMultiplier());
            } else {
                return query;
            }
        }

        private IndexDocument toIndexDocument(SolrDocument solrDoc) {
            //Seems weird to have to covert SolrDocument back to a SolrInputDocument.
            //The issue seems to be that IndexDocument is used for both indexing and querying, while SOLR uses two different objects for this
            SolrInputDocument document = new SolrInputDocument();
            for(String fieldName : solrDoc.getFieldNames()) {
                document.addField(fieldName, solrDoc.getFieldValue(fieldName).toString());
            }
            IndexDocument indexDoc = new IndexDocument(document);
            return indexDoc;
        }
    }

    public class SolrIndexReader implements ZimbraIndexReader {

        @Override
        public void close() throws IOException {
            // nothing to do here
        }

        @Override
        public int numDocs() throws ServiceException {
            try {
                SolrQuery q = solrHelper.newQuery(accountId).setQuery(new MatchAllDocsQuery().toString()).setRows(0);
                QueryResponse resp = (QueryResponse) solrHelper.executeQueryRequest(accountId, q);
                SolrDocumentList solrDocList = resp.getResults();
                return (int)solrDocList.getNumFound();
            } catch (SolrException e) {
                ZimbraLog.index.error("Caught Exception retrieving number of documents in mailbox %s", accountId,e);
            }
            return 0;
        }

        @Override
        public TermFieldEnumeration getTermsForField(String field) throws IOException, ServiceException {

            try {
                if (solrHelper.needsAccountFilter()) {
                    return new SolrFacetEnumeration(field);
                } else {
                    return new SolrTermValueEnumeration(field);
                }
            } catch (SolrException | SolrServerException e) {
                throw ServiceException.FAILURE(e.getMessage(),e);
            }
        }

        private final class SolrFacetEnumeration implements TermFieldEnumeration {

            private static final int FACET_RESULTS_CHUNK_SIZE = 100;
            private String field;
            private boolean hasMore;
            private int offset = 0;
            private LinkedList<Count> facetChunk;

            public SolrFacetEnumeration(String field) throws ServiceException {
                this.field = field;
                getNextFacetChunk();
            }

            private void getNextFacetChunk() throws ServiceException {
                SolrQuery q = solrHelper.newQuery(accountId);
                q.setQuery(new MatchAllDocsQuery().toString()).setRows(0);
                q.setFacet(true);
                q.addFacetField(field);
                q.setFacetSort("index");
                q.setFacetMinCount(1);
                q.setFacetLimit(FACET_RESULTS_CHUNK_SIZE + 1); // +1 for hasMore check
                q.set(FacetParams.FACET_OFFSET, offset);
                QueryResponse resp = (QueryResponse) solrHelper.executeQueryRequest(accountId, q);
                FacetField facetField = resp.getFacetField(field);
                if (facetField.getValueCount() > FACET_RESULTS_CHUNK_SIZE) {
                    facetChunk = Lists.newLinkedList(facetField.getValues().subList(0, FACET_RESULTS_CHUNK_SIZE));
                    hasMore = true;
                } else {
                    facetChunk = Lists.newLinkedList(facetField.getValues());
                    hasMore = false;
                }
                ZimbraLog.search.debug("got %d facets for field %s (offset=%d, hasMore=%s)", facetChunk.size(), field, offset, hasMore);
            }

            @Override
            public boolean hasMoreElements() {
                return facetChunk != null && (facetChunk.peek() != null || hasMore);
            }

            @Override
            public BrowseTerm nextElement() {
                if (facetChunk == null) {
                    throw new NoSuchElementException("No more values");
                }
                Count facetValue = facetChunk.poll();
                if (facetValue != null) {
                    return new BrowseTerm(facetValue.getName(), (int)facetValue.getCount());
                }
                try {
                    offset += FACET_RESULTS_CHUNK_SIZE;
                    getNextFacetChunk();
                    facetValue = facetChunk.poll();
                    if(facetValue != null) {
                        return new BrowseTerm(facetValue.getName(), (int)facetValue.getCount());
                    }
                } catch (SolrException | ServiceException e) {
                    ZimbraLog.index.error("Solr problem enumerating faceted terms for field %s", field, e);
                    throw new NoSuchElementException("No more values");
                }
                throw new NoSuchElementException("No more values");
            }

            @Override
            public void close() throws IOException {
                facetChunk = null;
            }

        }

        private final class SolrTermValueEnumeration implements TermFieldEnumeration {
            private LinkedList<org.apache.solr.client.solrj.response.TermsResponse.Term> termEnumeration = Lists.newLinkedList();
            private final String fieldName;
            private String last = null;

            private void primeTermsComponent(String firstTermValue, boolean includeLower) throws IOException, SolrServerException, ServiceException {
                SolrQuery q = new SolrQuery().setRequestHandler("/terms");

                q.setTerms(true);
                q.addTermsField(fieldName);
                //zimbraIndexTermsLimit, default 1024
                q.setTermsLimit(1024);
                if(firstTermValue != null && !firstTermValue.isEmpty()) {
                    q.setTermsLower(firstTermValue);
                    q.setTermsLowerInclusive(includeLower);
                }
                q.setTermsMinCount(1);
                q.setTermsSortString("index");
                QueryResponse resp = (QueryResponse) solrHelper.executeQueryRequest(accountId, q);
                List<org.apache.solr.client.solrj.response.TermsResponse.Term> enumeration = resp.getTermsResponse().getTerms(fieldName);
                termEnumeration = Lists.newLinkedList(enumeration);
                org.apache.solr.client.solrj.response.TermsResponse.Term lastTerm = termEnumeration.peekLast();
                if(lastTerm != null) {
                    last = lastTerm.getTerm();
                }
            }

            private SolrTermValueEnumeration(String field) throws IOException, SolrServerException, ServiceException {
                fieldName = field;
                primeTermsComponent("", true);
            }

            @Override
            public boolean hasMoreElements()  {
                if (termEnumeration == null) {
                    return false;
                } else if(termEnumeration.peek() == null) {
                    try {
                        primeTermsComponent(last, false);
                        return (termEnumeration.peek() != null);
                    } catch (IOException | SolrException | SolrServerException | ServiceException e) {
                        ZimbraLog.index.error("Solr problem enumerating terms for field %s", fieldName,e);
                        return false;
                    }
                } else {
                    return (termEnumeration.peek() != null);
                }
            }

            @Override
            public BrowseTerm nextElement() {
                if (termEnumeration == null) {
                    throw new NoSuchElementException("No more values");
                }
                org.apache.solr.client.solrj.response.TermsResponse.Term term = termEnumeration.poll();
                if (term != null) {
                    return new BrowseTerm(term.getTerm(), (int)term.getFrequency());
                }
                try {
                    primeTermsComponent(last, false);
                    term = termEnumeration.poll();
                    if(term != null) {
                        return new BrowseTerm(term.getTerm(), (int)term.getFrequency());
                    }
                } catch (IOException | SolrServerException | SolrException | ServiceException e) {
                    ZimbraLog.index.error("Solr problem enumerating terms for field %s", fieldName,e);
                    throw new NoSuchElementException("No more values");
                }
                throw new NoSuchElementException("No more values");
            }

            @Override
            public void close() throws IOException {
                termEnumeration = null;
            }
        }
    }

    protected class SolrIndexer implements Indexer {

        @Override
        public void close() throws IOException {}

        @Override
        public void deleteDocumentById(List<Integer> ids) throws IOException, ServiceException {
            deleteDocument(ids, LuceneFields.L_MAILBOX_BLOB_ID);
        }


        @Override
        public void deleteDocument(List<ItemIndexDeletionInfo> deleteInfo) throws IOException, ServiceException {
            String route = String.format("%s!", accountId);
            int[] itemIds = deleteInfo.stream().mapToInt(i -> i.getItemId()).toArray();
            UpdateRequest req = new UpdateRequest();
            int numDeleted = 0;
            for (ItemIndexDeletionInfo info : deleteInfo) {
                int itemId = info.getItemId();
                int numIndexDocs = info.getNumIndexDocs();
                if (numIndexDocs > 0) {
                    for (int part = 1; part <= numIndexDocs; part++) {
                        String docId = solrHelper.getSolrId(accountId, itemId, part);
                        req.deleteById(docId, route);
                        numDeleted++;
                    }
                } else {
                    ZimbraLog.index.warn("numIndexDocs for item %s is %s, skipping", itemId, numIndexDocs);
                }
            }
            if (numDeleted > 0) {
                try {
                    solrHelper.executeUpdate(accountId, req);
                    ZimbraLog.index.debug("Deleted index documents for items %s (%s docs)", Arrays.toString(itemIds), numDeleted);
                } catch (ServiceException e) {
                    ZimbraLog.index.error("Problem deleting index documents for items %s (%s docs)", Arrays.toString(itemIds), numDeleted);
                }
            }
        }

        @Override
        public void addSearchHistoryDocument(IndexDocument doc) throws IOException, ServiceException {
            UpdateRequest req = new UpdateRequest();
            SolrInputDocument solrDoc = doc.toInputDocument();
            String searchId = (String) solrDoc.getFieldValue(LuceneFields.L_SEARCH_ID);
            solrDoc.addField(LuceneFields.SOLR_ID, solrHelper.getSolrId(accountId,  "sh", searchId));
            req.add(solrDoc);
            try {
                solrHelper.executeUpdate(accountId, req);
            } catch (ServiceException e) {
                throw ServiceException.FAILURE(String.format(Locale.US, "Failed to index document for account %s", accountId), e);
            }
        }

        @Override
        public void addDocument(MailItem item, List<IndexDocument> docs) throws IOException, ServiceException {
            if (docs == null || docs.isEmpty()) {
                return;
            }
            int partNum = 1;
            UpdateRequest req = new UpdateRequest();
            for (IndexDocument doc : docs) {
                SolrInputDocument solrDoc;
                // doc can be shared by multiple threads if multiple mailboxes are referenced in a single email
                synchronized (doc) {
                    setFields(item, doc);
                    solrDoc = doc.toInputDocument();
                    solrDoc.addField(LuceneFields.SOLR_ID, solrHelper.getSolrId(accountId, item.getId(), partNum));
                    partNum++;
                    if (ZimbraLog.index.isTraceEnabled()) {
                        ZimbraLog.index.trace("Adding solr document %s", solrDoc);
                    }
                }
                req.add(solrDoc);
            }
            try {
                solrHelper.executeUpdate(accountId, req);
            } catch (ServiceException e) {
                throw ServiceException.FAILURE(String.format(Locale.US, "Failed to index part %d of Mail Item with ID %d for Account %s ", partNum, item.getId(), accountId), e);
            }
        }

        @Override
        public void deleteDocument(List<Integer> ids, String fieldName) throws IOException, ServiceException {
            if (ids.isEmpty()) {
                return;
            }
            UpdateRequest req = new UpdateRequest();
            BooleanQuery.Builder disjunctionBuilder = new BooleanQuery.Builder();
            for (Integer id : ids) {
                disjunctionBuilder.add(new TermQuery(new Term(fieldName, id.toString())), Occur.SHOULD);
            }
            BooleanQuery.Builder queryBuilder;
            if (solrHelper.needsAccountFilter()) {
                queryBuilder = new BooleanQuery.Builder();
                queryBuilder.add(new TermQuery(new Term(LuceneFields.L_ACCOUNT_ID, accountId)), Occur.MUST);
                queryBuilder.add(disjunctionBuilder.build(), Occur.MUST);
            } else {
                queryBuilder = disjunctionBuilder;
            }
            req.deleteByQuery(queryBuilder.build().toString());
            String idsStr = Joiner.on(",").join(ids);
            try {
                solrHelper.executeUpdate(accountId, req);
                ZimbraLog.index.debug("Deleted documents with field %s=[%s]", fieldName, idsStr);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Problem deleting documents with field %s=[%s]",fieldName, idsStr, e);
            }
        }

        @Override
        public void add(List<IndexItemEntry> entries) throws IOException,
                ServiceException {
            UpdateRequest req = new UpdateRequest();
            for (IndexItemEntry entry : entries) {
                if (entry.getDocuments() == null) {
                    ZimbraLog.index.warn("NULL index data item=%s", entry);
                    continue;
                }
                int partNum = 1;
                for (IndexDocument doc : entry.getDocuments()) {
                    SolrInputDocument solrDoc;
                    // doc can be shared by multiple threads if multiple mailboxes are referenced in a single email
                    synchronized (doc) {
                        setFields(entry.getItem(), doc);
                        solrDoc = doc.toInputDocument();
                        solrDoc.addField(LuceneFields.SOLR_ID, solrHelper.getSolrId(accountId, entry.getItem().getId(), partNum));
                        partNum++;
                        if (ZimbraLog.index.isTraceEnabled()) {
                            ZimbraLog.index.trace("Adding solr document %s", solrDoc.toString());
                        }
                    }
                    req.add(solrDoc);
                }
            }
            try {
                solrHelper.executeUpdate(accountId,req);
            } catch (ServiceException e) {
                ZimbraLog.index.error("Problem indexing documents", e);
            }
        }
    }


    /**
     * solrj doesn't provide a programmatic way to encode local params, so we do that here
     */
    public static class LocalParams {

        private String queryType = null;
        private Map<String, String> params;
        private String valueParamName = null;

        public LocalParams() {
            params = new HashMap<>();
        }

        public LocalParams(String queryType) {
            this();
            this.queryType = queryType;
        }

        public void addParam(String key, String value) {
            params.put(key, value);
        }

        public void setValueParamName(String valueParamName) {
            this.valueParamName = valueParamName;
        }

        public String encode() {
            StringBuilder sb = new StringBuilder("{!");
            if (queryType != null) {
               sb.append(queryType);
            }
            for (Map.Entry<String, String> entry: params.entrySet()) {
                encodeParam(sb, entry.getKey(), entry.getValue());
            }
            if (valueParamName != null) {
                sb.append(" v=$").append(valueParamName);
            }
            sb.append("}");
            return sb.toString();
        }

        private void encodeParam(StringBuilder sb, String key, String value) {
            String kv = String.format("%s='%s'", key, value);
            sb.append(" ").append(kv);
        }
    }

    @Override
    public Indexer openIndexer() throws IOException, ServiceException {
        return new SolrIndexer();
    }

    @Override
    public ZimbraIndexSearcher openSearcher() throws IOException, ServiceException {
        final SolrIndexReader reader = new SolrIndexReader();
        return new SolrIndexSearcher(reader);
    }

    @Override
    public void evict() {
        // SOLR backend doesn't need this
    }

    @Override
    public void deleteIndex() throws IOException, ServiceException {
        solrHelper.deleteAccountData(accountId);
    }

    public static final class StandaloneSolrFactory implements IndexStore.Factory {

        public StandaloneSolrFactory() {
            ZimbraLog.index.info("Created StandaloneSolrFactory");
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public void destroy() {
            ZimbraLog.index.info("Destroyed SolrCloudFactory");
        }

        @Override
        public IndexStore getIndexStore(String accountId) throws ServiceException {
            CloseableHttpClient httpClient = ZimbraHttpClientManager.getInstance().getInternalHttpClient();
            SolrCollectionLocator locator = new MultiCollectionLocator();
            String baseUrl = Provisioning.getInstance().getLocalServer().getIndexURL().substring("solr:".length());
            StandaloneSolrHelper requestHelper = new StandaloneSolrHelper(locator, httpClient, IndexType.MAILBOX, baseUrl);
            return new SolrIndex(accountId, requestHelper);
        }
    }

    public static final class SolrCloudFactory implements IndexStore.Factory {
        SolrCloudHelper solrHelper;

        public SolrCloudFactory() throws ServiceException {
            ZimbraLog.index.info("Created SolrCloudFactory");
            String zkHost = Provisioning.getInstance().getLocalServer().getIndexURL().substring("solrcloud:".length());
            CloudSolrClient client = SolrUtils.getCloudSolrClient(zkHost);
            SolrCollectionLocator locator = new MultiCollectionLocator();
            solrHelper = new SolrCloudHelper(locator, client, IndexType.MAILBOX);
        }

        @Override
        public SolrIndex getIndexStore(String accountId) throws ServiceException {
            return new SolrIndex(accountId,  solrHelper);
        }

        /**
         * Cleanup any caches etc associated with the IndexStore
         */
        @Override
        public void destroy() {
            try {
                solrHelper.close();
            } catch (IOException e) {
                ZimbraLog.index.error("Cought an exception trying to close ClourSolrClient instance", e);
            }
            ZimbraLog.index.info("Destroyed SolrCloudIndex.Factory\n");
        }
    }

    public static enum IndexType {
        MAILBOX, EVENTS;
    }

    private static class ReferencedQueryParams {
        private int counter = 0;
        private Map<String, String> paramsMap = new HashMap<>();

        public void add(LocalParams localParams, String queryStr) {
            String param = String.format("q_%d", ++counter);
            paramsMap.put(param, queryStr);
            localParams.setValueParamName(param);
        }

        public void setAsParams(SolrQuery request) {
            for (Map.Entry<String, String> entry: paramsMap.entrySet()) {
                request.set(entry.getKey(), entry.getValue());
            }
        }
    }
}
