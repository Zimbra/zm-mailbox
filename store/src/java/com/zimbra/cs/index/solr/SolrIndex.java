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
import java.util.stream.Collectors;

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
import org.apache.solr.common.params.DisMaxParams;
import org.apache.solr.common.params.FacetParams;

import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.Closeables;
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
import com.zimbra.cs.index.solr.JointCollectionLocator.IndexNameFunc;
import com.zimbra.cs.index.solr.SolrUtils.WildcardEscape;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;

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
        LuceneFields.L_PARTNAME, LuceneFields.L_FILENAME, LuceneFields.L_SORT_SIZE,
        LuceneFields.L_SORT_ATTACH, LuceneFields.L_SORT_FLAG, LuceneFields.L_SORT_PRIORITY,
        LuceneFields.L_MAILBOX_BLOB_ID, LuceneFields.L_SORT_DATE, LuceneFields.L_VERSION
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

    protected Query optimizeQueryOps(Query query) {
        if (query instanceof TermQuery) {
            String term = ((TermQuery) query).getTerm().text();
            String field = ((TermQuery) query).getTerm().field();
            if (!SolrUtils.isWildcardQuery(term)) {
                if (isDismaxField(field)) {
                    if (SolrUtils.containsWhitespace(term)) {
                        return new TermQuery(new Term(field, SolrUtils.quoteText(term)));
                    } else {
                        return new TermQuery(new Term(field, term));
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
            if (occur == Occur.MUST_NOT) {
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
        String[] fieldMappings;
        if (field.equals(LuceneFields.L_CONTENT)) {
            fieldMappings = new String[]{
                    LuceneFields.L_H_SUBJECT,
                    LuceneFields.L_CONTENT,
                    LuceneFields.L_H_FROM,
                    LuceneFields.L_H_TO,
                    LuceneFields.L_H_CC,
                    LuceneFields.L_FILENAME};
        } else if (field.equals(LuceneFields.L_CONTACT_DATA)) {
            fieldMappings = new String[]{
                    LuceneFields.L_CONTACT_DATA,
                    LuceneFields.L_H_TO};
        } else {
            return null;
        }
        List<String> searchedFields = Arrays.asList(fieldMappings).stream().map(fld -> toSearchField(fld)).collect(Collectors.toList());
        return searchedFields.toArray(new String[searchedFields.size()]);
    }

    private static String toSearchField(String origField) {
        /*From/to/cc fields are copied to a "*_search" field that includes a ReversedWildcardFilterFactory.
          Regular searches should use these fields; facet queries should use the original field as to not
          include the reversed tokens in the facet results */
        if (origField.equals(LuceneFields.L_H_FROM) ||
                origField.equals(LuceneFields.L_H_TO) ||
                origField.equals(LuceneFields.L_H_CC) ||
                origField.equals(LuceneFields.L_FILENAME)) {
            return origField + "_search";
        }
        else {
            return origField;
        }
    }

    private String booleanQueryToString(BooleanQuery query, ReferencedQueryParams referencedParams) {
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
                sb.append(queryToString(clauseQuery, referencedParams));
            }
            sb.append(" ");
         }
        sb.append(")");
        return sb.toString();
    }

    protected String queryToString(Query query, ReferencedQueryParams referencedParams) {
        if (query instanceof TermQuery) {
            return termQueryToString((TermQuery)query, referencedParams);
        } else if (query instanceof BooleanQuery) {
            return booleanQueryToString((BooleanQuery)query, referencedParams);
        } else {
            LocalParams lp = new LocalParams("lucene");
            lp.addParam("q.op", "OR");
            referencedParams.add(lp, query.toString());
            return lp.encode();
        }
    }

    private String termQueryToString(TermQuery query, ReferencedQueryParams referencedParams) {
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
            referencedParams.add(localParams, text);
            return localParams.encode();
        }
        if (dismaxField) {
            String[] searchedFields = getSearchedFields(field);
            assert(searchedFields != null);
            String weightedFields = getDismaxWeightedFieldString(field, searchedFields);
            LocalParams localParams = buildDismaxLocalParams(weightedFields);
            referencedParams.add(localParams, text);
            return localParams.encode();
        } else {
          return String.format("%s:%s",toSearchField(field), text);
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

    protected void addTermsFilter(SolrQuery query, Collection<Term> terms) {
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
            query.addFilterQuery(SolrUtils.getTermsFilter(field, values));
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
                ZimbraLog.index.debug(String.format("retrieving document by query %s ",q.toString()));
                try {
                    QueryResponse resp = (QueryResponse) solrHelper.executeQueryRequest(accountId, q);
                    SolrDocument solrDoc = resp.getResults().get(0);
                    return buildFullDocument(solrDoc);
                } catch (SolrException e) {
                    ZimbraLog.index.error("Solr problem geting document %s, from mailbox %s",docID.toString(), accountId,e);
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
            if (!(query instanceof DisjunctionMaxQuery)) {
                query = optimizeQueryOps(query);
            }

            ReferencedQueryParams referencedQueryParams = new ReferencedQueryParams();
            String szq = queryToString(query, referencedQueryParams);
            SolrQuery q = solrHelper.newQuery(accountId);
            referencedQueryParams.setAsParams(q);
            q.setQuery(szq).setRows(n);

            if(filter != null) {
                addTermsFilter(q, filter.getTerms());
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

            ZimbraLog.index.debug(String.format("Searching Solr for %s with %d filter terms. First term %s ",szq, filter == null || filter.getTerms() == null ? 0 : filter.getTerms().size(),(filter == null || filter.getTerms() == null || filter.getTerms().size() == 0 ? "" : filter.getTerms().iterator().next().toString())));
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
                    scoreDocs.add(ZimbraScoreDoc.create(new ZimbraSolrDocumentID(idField, solrDoc.getFieldValue(idField).toString()), score));
                }
            } catch (SolrException e) {
                ZimbraLog.index.error("Solr search problem mailbox %s, query %s", accountId,query.toString(),e);
                return ZimbraTopDocs.create(totalHits, scoreDocs, maxScore, indexDocs);
            }
            return ZimbraTopDocs.create(totalHits, scoreDocs, maxScore, indexDocs);
        }

        private Term escapeSpecialChars(Term term) {
            String escaped = SolrUtils.escapeSpecialChars(term.text(), WildcardEscape.ZIMBRA);
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
                }
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
        public void deleteDocument(List<Integer> ids) throws IOException, ServiceException {
            deleteDocument(ids, LuceneFields.L_MAILBOX_BLOB_ID);
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
                        ZimbraLog.index.trace("Adding solr document %s", solrDoc.toString());
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
            String indexName = SolrUtils.getMailboxIndexName(accountId);
            SolrCollectionLocator locator = new JointCollectionLocator(indexName);
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

            //since this helper is reused for different solr collections, we pass in a name lookup function
            IndexNameFunc indexNameFunc = new IndexNameFunc() {

                @Override
                public String getIndexName(String accountId) throws ServiceException {
                    return SolrUtils.getMailboxIndexName(accountId);
                }
            };

            SolrCollectionLocator locator = new JointCollectionLocator(indexNameFunc);
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
