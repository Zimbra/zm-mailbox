package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

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
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;

import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
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
import com.zimbra.cs.index.ZimbraTopFieldDocs;
import com.zimbra.cs.index.solr.SolrUtils.WildcardEscape;

/**
 * Base class for standalone solr and solrcloud IndexStores
 * @author Greg Solovyev
 * @author raykini
 */

public abstract class SolrIndexBase extends IndexStore {
    protected static final String CMD_INDEX_VERSION = "indexversion";
    protected static final String COMMAND = "command";
    protected static final String GENERATION = "generation";
    protected static final String SOLR_ID_FIELD = "solrId";
    protected static final String SOLR_SCORE_FIELD = "score";
    protected static final String[] MESSAGE_FETCH_FIELDS = new String[] {
        LuceneFields.L_PARTNAME, LuceneFields.L_FILENAME, LuceneFields.L_SORT_SIZE,
        LuceneFields.L_SORT_ATTACH, LuceneFields.L_SORT_FLAG, LuceneFields.L_SORT_PRIORITY,
        LuceneFields.L_MAILBOX_BLOB_ID, LuceneFields.L_SORT_DATE, LuceneFields.L_VERSION
    };


    protected static final String CMD_GET_FILE_LIST = "filelist";
    protected String accountId;


    /**
     * This method configures a request to be routed to the correct SOLR server or core.
     * @throws ServiceException
     *
     */
    public abstract void setupRequest(Object obj, SolrClient solrServer) throws ServiceException;

    protected static final Cache<Integer, SolrIndexSearcher> SEARCHER_CACHE =
            CacheBuilder.newBuilder()
            .maximumSize(LC.zimbra_index_reader_cache_size.intValue())
            .expireAfterAccess(LC.zimbra_index_reader_cache_ttl.longValue(), TimeUnit.MILLISECONDS)
            .removalListener(new RemovalListener<Integer, SolrIndexSearcher>() {
                @Override
                public void onRemoval(RemovalNotification<Integer, SolrIndexSearcher> notification) {
                    Closeables.closeQuietly(notification.getValue());
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
                Closeables.closeQuietly(searcher);
            }
        })
        .build();

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

    protected Query optimizeQueryOps(Query query) {
        if (query instanceof TermQuery) {
            String term = ((TermQuery) query).getTerm().text();
            String field = ((TermQuery) query).getTerm().field();
            if (!SolrUtils.isWildcardQuery(term)) {
                if (isDismaxField(field)) {
                    if (SolrUtils.containsWhitespace(term)) {
                        return new TermQuery(new Term(field, SolrUtils.quoteText(term)));
                    } else {
                        return new TermQuery(new Term(field,"+"+term));
                    }
                } else {
                    if (SolrUtils.containsWhitespace(term)) {
                        //still need to quote phrase queries
                        return new TermQuery(new Term(field, SolrUtils.quoteText(term)));
                    } else {
                        return query;
                    }
                }
            } else {
                //complex phrase queries don't need a boolean operator in the term
                return new TermQuery(new Term(field, SolrUtils.quoteText(term)));
            }
        }
        if (!(query instanceof BooleanQuery)) {
            return query;
        }
        //must be BooleanQuery
        HashMap<String, ArrayList<String>> dismaxTermsByField = new HashMap<String,ArrayList<String>>();
        HashMap<String, Occur> DismaxOccurByField = new HashMap<String, Occur>();
        Occur occur = null;
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (BooleanClause clause : (BooleanQuery) query) {
            Query clauseQuery = clause.getQuery();
            String op = "";
            occur = clause.getOccur();
            if (occur == Occur.MUST) {
                op = "+";
            } else if (occur == Occur.MUST_NOT) {
                op = "-";
            }
            if (clauseQuery instanceof TermQuery) {
                String field = ((TermQuery) clauseQuery).getTerm().field();
                String term = ((TermQuery) clauseQuery).getTerm().text();
                if (!SolrUtils.isWildcardQuery(term)) {
                    if (isDismaxField(field)) {
                        if (SolrUtils.containsWhitespace(term)) {
                            term = String.format("%s\"%s\"", op, term);
                        } else {
                            term = op+term;
                        }
                        if (dismaxTermsByField.containsKey(field)) {
                            dismaxTermsByField.get(field).add(term);
                        } else {
                            ArrayList<String> terms = new ArrayList<String>();
                            terms.add(term);
                            dismaxTermsByField.put(field, terms);
                        }
                        //if we are pushing a + or - to the inside of the clause, then we need to add + to the outside
                        Occur occurAfterDistributing = occur == Occur.SHOULD? Occur.SHOULD: Occur.MUST;
                        DismaxOccurByField.put(field, occurAfterDistributing);
                    } else {
                        //non-dismax term queries get quoted if the are multi-term
                        if (SolrUtils.containsWhitespace(term)) {
                            TermQuery quoted = new TermQuery(new Term(field, SolrUtils.quoteText(term)));
                            builder.add(quoted, occur);
                        } else {
                            builder.add(clause);
                        }
                    }
                } else {
                    //complex phrase query doesn't get combined with other terms
                    TermQuery quoted = new TermQuery(new Term(field, SolrUtils.quoteText(term)));
                    builder.add(quoted, occur);
                }
            } else {
                //non-term queries get optimized recursively
                Query optimized = optimizeQueryOps(clauseQuery);
                builder.add(optimized, occur);
            }
        }
        //after term queries on this level have been gathered group them by field
        for (String field: dismaxTermsByField.keySet()) {
            List<String> terms = dismaxTermsByField.get(field);
            String allTerms = Joiner.on(" ").join(terms);
            TermQuery combined = new TermQuery(new Term(field, allTerms));
            //a tricky issue here is deciding which Occur value to use.
            //if we moved MUST or MUST_NOT values inside, we should use a MUST,
            //otherwise we can use a SHOULD, lest we risk making an optional term required.
            builder.add(combined, DismaxOccurByField.get(field));
        }
        //if boolean query has only one clause, extract it
        BooleanQuery booleanQuery = builder.build();
        if (booleanQuery.clauses().size() == 1) {
            return booleanQuery.clauses().get(0).getQuery();
        } else {
            return booleanQuery;
        }
    }

    private boolean isDismaxField(String field) {
        if (field == LuceneFields.L_CONTENT ||
            field == LuceneFields.L_CONTACT_DATA) {
            return true;
        } else {
            return false;
        }
    }

    private String[] getSearchedFields(String field) {
        if (field.equals(LuceneFields.L_CONTENT)) {
            return new String[] {"subject", "l.content", "from_sw", "to_sw", "cc_sw", "filename_sw"};
        } else if (field.equals(LuceneFields.L_CONTACT_DATA)) {
            return new String[] {"l.contactData", "to"};
        } else {
            return null;
        }
    }

    private String booleanQueryToString(BooleanQuery query) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (BooleanClause clause : query) {
            Query clauseQuery = clause.getQuery();
            if(clauseQuery != null) {
                Occur occur = clause.getOccur();
                if(occur != null) {
                    switch (occur) {
                        case MUST:
                            sb.append("+");
                            break;
                        case MUST_NOT:
                            sb.append("-");
                            break;
                        default:
                            break;
                    }
                }
                sb.append(queryToString(clauseQuery));
            }
            sb.append(" ");
         }
        sb.append(")");
        return sb.toString();
    }

    protected String queryToString(Query query) {
        if (query instanceof TermQuery) {
            return termQueryToString((TermQuery)query);
        } else if (query instanceof TermRangeQuery) {
            return String.format("{!lucene q.op=OR}%s",query.toString());
        } else if (query instanceof BooleanQuery) {
            return booleanQueryToString((BooleanQuery)query);
        } else  {
            return String.format("{!lucene q.op=OR}%s",query.toString());
        }
    }

    private String termQueryToString(TermQuery query) {
        String field = query.getTerm().field();
        String text = query.getTerm().text();
        boolean dismaxField = isDismaxField(field);
        if (SolrUtils.isWildcardQuery(query.getTerm().text())) {
            String[] searchedFields;
            if (dismaxField) {
                searchedFields = getSearchedFields(field);
            } else {
                searchedFields = new String[] {field};
            }
            LocalParams localParams = buildWildcardLocalParams(searchedFields);
            return localParams.encode() + text;
        }
        if (dismaxField) {
            String[] searchedFields = getSearchedFields(field);
            assert(searchedFields != null);
            String weightedFields = getDismaxWeightedFieldString(field, searchedFields);
            LocalParams localParams = buildDismaxLocalParams(weightedFields);
            return localParams.encode() + text;
        } else {
          return String.format("%s:%s",field,text);
        }
    }

    private LocalParams buildDismaxLocalParams(String weightedFields) {
        LocalParams lp = new LocalParams("edismax");
        lp.addParam(DisMaxParams.QF, weightedFields);
        lp.addParam(DisMaxParams.PF, weightedFields);
        lp.addParam(DisMaxParams.MM, "100%");
        lp.addParam(DisMaxParams.TIE, "0.1");
        return lp;
    }

    private LocalParams buildWildcardLocalParams(String[] searchedFields) {
        LocalParams lp = new LocalParams("zimbrawildcard");
        lp.addParam("fields", Joiner.on(" ").join(searchedFields));
        lp.addParam("maxExpansions", String.valueOf(LC.zimbra_index_wildcard_max_terms_expanded.intValue()));
        return lp;
    }

    private String getDismaxWeightedFieldString(String originalField, String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++ ) {
            String field = fields[i];
            int fieldWeight = getDismaxFieldWeight(originalField, field);
            sb.append(fieldWeight != 1? String.format("%s^%d", field, fieldWeight): field).append(" ");
        }
        return sb.toString();
    }

    private int getDismaxFieldWeight(String originalField, String field) {
        if (originalField.equals("l.content")) {
            switch (field) {
            case "subject":
                return 2;
            default:
                return 1;
            }
        }
        return 1;
    }

    protected String TermsToQuery(Collection<Term> terms) {
        if(terms == null || terms.size() < 1) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{!lucene q.op=OR}");
        for(Term term : terms) {
            sb.append(term.field());
            sb.append(":");
            sb.append(term.text());
            sb.append(" ");
        }
        return sb.toString();
    }
    protected String TermToQuery(Term term) {
        if (term == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("{!lucene q.op=OR}");
        sb.append(term.field());
        sb.append(":");
        if (SolrUtils.containsWhitespace(term.text())) {
            sb.append(SolrUtils.quoteText(term.text()));
        } else {
            sb.append(term.text());
        }
        return sb.toString();
    }

    protected class SolrIndexSearcher implements ZimbraIndexSearcher {
        final SolrIndexReader reader;

        public SolrIndexSearcher(SolrIndexReader reader) {
            this.reader = reader;
        }
        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub

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
            SolrClient solrServer = getSolrServer();
            try {
                if (docID instanceof ZimbraSolrDocumentID) {
                    ZimbraSolrDocumentID solrID = (ZimbraSolrDocumentID) docID;
                    SolrQuery q = new SolrQuery().setQuery(String.format("%s:%s", solrID.getIDFIeld(), solrID.getDocID())).setRows(1);
                    q.setFields(MESSAGE_FETCH_FIELDS);
                    setupRequest(q, solrServer);
                    QueryRequest req = new QueryRequest(q);
                    ZimbraLog.index.debug(String.format("retrieving document by query %s ",q.toString()));
                    try {
                        QueryResponse resp = (QueryResponse) processRequest(solrServer, req);
                        SolrDocument solrDoc = resp.getResults().get(0);
                        return buildFullDocument(solrDoc);
                    } catch (SolrException | SolrServerException e) {
                        ZimbraLog.index.error("Solr problem geting document %s, from mailbox %s",docID.toString(), accountId,e);
                    }
                    return null;
                }
            } finally {
                shutdown(solrServer);
            }
            throw new IllegalArgumentException("Expected a ZimbraSolrDocumentID");
        }

        @Override
        public int docFreq(Term term) throws IOException, ServiceException {
            SolrClient solrServer = getSolrServer();
            try {
                SolrQuery q = new SolrQuery().setQuery(TermToQuery(term)).setRows(0);
                setupRequest(q, solrServer);
                QueryRequest req = new QueryRequest(q);
                QueryResponse resp = (QueryResponse) processRequest(solrServer, req);
                return (int) resp.getResults().getNumFound();
            } catch (SolrException | SolrServerException e) {
                ZimbraLog.index.error("Solr search problem getting docFreq for mailbox %s", accountId,e);
            }  finally {
                shutdown(solrServer);
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
            //TODO: this doesn't need to be an interface method; could push these args up to the caller
            return search(query, filter, n, sort, LuceneFields.L_MAILBOX_BLOB_ID, MESSAGE_FETCH_FIELDS);
        }

        //TODO: return all fields that we need in topfielddocs so that we don't have to go back to index again
        @Override
        public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter,
                int n, Sort sort, String idField, String[] fetchFields) throws IOException, ServiceException {
            List<ZimbraScoreDoc>scoreDocs = Lists.newArrayList();
            List<SortField> sortFields = Lists.newArrayList();
            List<IndexDocument> indexDocs = Lists.newArrayList();
            float maxScore = 0;
            int totalHits = 0;

            SolrClient solrServer = getSolrServer();
            if (sort != null) {
                Collections.addAll(sortFields, sort.getSort());
            }
            query = escapeSpecialChars(query);
            if (!(query instanceof DisjunctionMaxQuery)) {
                query = optimizeQueryOps(query);
            }
            String szq = queryToString(query);
            SolrQuery q = new SolrQuery().setQuery(szq).setRows(n);

            setupRequest(q, solrServer);
            if(filter != null) {
                q.addFilterQuery(TermsToQuery(filter.getTerms()));
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

            QueryRequest req = new QueryRequest(q, METHOD.POST);

            ZimbraLog.index.debug(String.format("Searching Solr for %s with %d filter terms. First term %s ",szq, filter == null || filter.getTerms() == null ? 0 : filter.getTerms().size(),(filter == null || filter.getTerms() == null || filter.getTerms().size() == 0 ? "" : filter.getTerms().iterator().next().toString())));
            try {
                QueryResponse resp = (QueryResponse) processRequest(solrServer, req);
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
                    scoreDocs.add(ZimbraScoreDoc.create(new ZimbraSolrDocumentID(idField, solrDoc.getFieldValue(idField).toString()), score));
                }
            } catch (SolrException | SolrServerException e) {
                ZimbraLog.index.error("Solr search problem mailbox %s, query %s", accountId,query.toString(),e);
                return ZimbraTopDocs.create(totalHits, scoreDocs, maxScore, indexDocs);
            } finally {
                shutdown(solrServer);
            }
            return ZimbraTopDocs.create(totalHits, scoreDocs, maxScore, indexDocs);
        }

        private Query escapeSpecialChars(Query query) {
            if (query instanceof TermQuery) {
                Term term = ((TermQuery) query).getTerm();
                String escaped = SolrUtils.escapeSpecialChars(term.text(), WildcardEscape.ZIMBRA);
                return new TermQuery(new Term(term.field(), escaped));
            } else if (query instanceof BoostQuery) {
                BoostQuery boostQuery = (BoostQuery) query;
                return new BoostQuery(escapeSpecialChars(boostQuery.getQuery()), boostQuery.getBoost());
            } else if (query instanceof PrefixQuery) {
                Term term = ((PrefixQuery) query).getPrefix();
                String escaped = SolrUtils.escapeSpecialChars(term.text(), WildcardEscape.ZIMBRA);
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

    public abstract class SolrIndexReader implements ZimbraIndexReader {

        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub

        }

        @Override
        public int numDocs() throws ServiceException {
            SolrClient solrServer = getSolrServer();
            try {
                SolrQuery q = new SolrQuery().setQuery("*:*").setRows(0);
                setupRequest(q, solrServer);
                QueryRequest req = new QueryRequest(q);
                QueryResponse resp = (QueryResponse) processRequest(solrServer, req);
                SolrDocumentList solrDocList = resp.getResults();
                return (int)solrDocList.getNumFound();
            } catch (SolrException | SolrServerException | IOException e) {
                ZimbraLog.index.error("Caught Exception retrieving number of documents in mailbox %s", accountId,e);
            } finally {
                shutdown(solrServer);
            }
            return 0;
        }

        @Override
        public TermFieldEnumeration getTermsForField(String field,
                String firstTermValue) throws IOException, ServiceException {
            try {
                return new SolrTermValueEnumeration(field, firstTermValue);
            } catch (SolrException | SolrServerException e) {
                throw ServiceException.FAILURE(e.getMessage(),e);
            }
        }

        private final class SolrTermValueEnumeration implements TermFieldEnumeration {
            private LinkedList<org.apache.solr.client.solrj.response.TermsResponse.Term> termEnumeration = Lists.newLinkedList();
            private final String fieldName;
            private String last = null;

            private void primeTermsComponent(String firstTermValue, boolean includeLower) throws IOException, SolrServerException, ServiceException {
                SolrClient solrServer = getSolrServer();
                SolrQuery q = new SolrQuery().setRequestHandler("/terms");

                setupRequest(q, solrServer);
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
                QueryRequest req = new QueryRequest(q);
                try {
                    QueryResponse resp = (QueryResponse) processRequest(solrServer, req);
                    List<org.apache.solr.client.solrj.response.TermsResponse.Term> enumeration = resp.getTermsResponse().getTerms(fieldName);
                    termEnumeration = Lists.newLinkedList(enumeration);
                    org.apache.solr.client.solrj.response.TermsResponse.Term lastTerm = termEnumeration.peekLast();
                    if(lastTerm != null) {
                        last = lastTerm.getTerm();
                    }
                } finally {
                    shutdown(solrServer);
                }
            }

            private SolrTermValueEnumeration(String field, String firstTermValue) throws IOException, SolrServerException, ServiceException {
                fieldName = field;
                primeTermsComponent(firstTermValue, true);
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

    protected abstract class SolrIndexer implements Indexer {
        @Override
        public void close() throws IOException {
            // TODO Auto-generated method stub

        }

        protected void incrementUpdateCounter(SolrClient solrServer) throws ServiceException {
            try {
                SolrQuery params = new SolrQuery().setParam("action", " increment");
                setupRequest(params, solrServer);
                UpdateRequest req = new UpdateRequest();
                req.setParams(params);
                req.setPath("/commitcount");
                req.process(solrServer);
            } catch (SolrException | SolrServerException | IOException e) {
                ZimbraLog.index.error("Problem increasing commit counter for Core %s", accountId,e);
            }
        }

        @Override
        public void compact() {
            // TODO Auto-generated method stub
        }
    }

    public abstract SolrClient getSolrServer() throws ServiceException;

    public abstract void shutdown(SolrClient server);

    protected abstract SolrResponse processRequest(SolrClient server, SolrRequest request)
            throws SolrServerException, IOException, ServiceException;

    @Override
    /**
    * Gets the latest commit version and generation from Solr
    */
    public long getLatestIndexGeneration(String accountId) throws ServiceException {
        long version = 0L;
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("command", "indexversion");
        params.set(CommonParams.WT, "javabin");
        params.set(CommonParams.QT, "/replication");
        SolrClient solrServer = getSolrServer();
        setupRequest(params, solrServer);
        LeaderQueryRequest req = new LeaderQueryRequest(params);
        @SuppressWarnings("rawtypes")
        NamedList rsp;
        try {
            rsp = solrServer.request(req);
            version = (Long) rsp.get(GENERATION);
        } catch (SolrException | SolrServerException | IOException e) {
          throw ServiceException.FAILURE(e.getMessage(),e);
        } finally {
            shutdown(solrServer);
        }
        return version;
    }

    /**
     * solrj doesn't provide a programmatic way to encode local params, so we do that here
     */
    public static class LocalParams {

        private String queryType = null;
        private Map<String, String> params;

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

        public String encode() {
            StringBuilder sb = new StringBuilder("{!");
            if (queryType != null) {
               sb.append(queryType);
            }
            for (Map.Entry<String, String> entry: params.entrySet()) {
                encodeParam(sb, entry.getKey(), entry.getValue());
            }
            sb.append("}");
            return sb.toString();
        }

        private void encodeParam(StringBuilder sb, String key, String value) {
            String kv = String.format("%s='%s'", key, value);
            sb.append(" ").append(kv);
        }
    }
}
