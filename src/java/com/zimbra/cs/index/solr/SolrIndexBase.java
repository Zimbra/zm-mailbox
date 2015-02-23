package com.zimbra.cs.index.solr;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrResponse;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest.ACTION;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.EvictionListener;
import com.zimbra.common.account.ZAttrProvisioning;
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
import com.zimbra.cs.index.solr.SolrUtils.WildcardEscape;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.IndexItemEntry;
import com.zimbra.cs.util.ProvisioningUtil;

/**
 * Base class for standalone solr and solrcloud IndexStores
 * @author gsolovyev
 * @author raykini
 */

public abstract class SolrIndexBase extends IndexStore {
    protected static final String CMD_INDEX_VERSION = "indexversion";
    protected static final String COMMAND = "command";
    protected static final String GENERATION = "generation";
    protected static final String SOLR_ID_FIELD = "solrId";


    protected static final String CMD_GET_FILE_LIST = "filelist";
    protected String accountId;


    /**
     * This method configures a request to be routed to the correct SOLR server or core.
     * @throws ServiceException
     *
     */
    public abstract void setupRequest(Object obj, SolrServer solrServer) throws ServiceException;

    protected static final Cache<Integer, SolrIndexSearcher> SEARCHER_CACHE =
            CacheBuilder.newBuilder()
            .maximumSize(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexReaderCacheSize, 20))
            .expireAfterAccess(ProvisioningUtil.getTimeIntervalServerAttribute(ZAttrProvisioning.A_zimbraIndexReaderCacheTtl, 300000L), TimeUnit.MILLISECONDS)
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
        .maximumWeightedCapacity(ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexReaderGalSyncCacheSize, 5))
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
        BooleanQuery newQuery = new BooleanQuery();
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
                            newQuery.add(quoted, occur);
                        } else {
                            newQuery.add(clause);
                        }
                    }
                } else {
                    //complex phrase query doesn't get combined with other terms
                    TermQuery quoted = new TermQuery(new Term(field, SolrUtils.quoteText(term)));
                    newQuery.add(quoted, occur);
                }
            } else {
                //non-term queries get optimized recursively
                Query optimized = optimizeQueryOps(clauseQuery);
                newQuery.add(optimized, occur);
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
            newQuery.add(combined, DismaxOccurByField.get(field));
        }
        //if boolean query has only one clause, extract it
        if (newQuery.getClauses().length == 1) {
            return newQuery.getClauses()[0].getQuery();
        } else {
            return newQuery;
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
            return buildComplexPhraseQuery(text, searchedFields);
        }
        if (dismaxField) {
            String[] searchedFields = getSearchedFields(field);
            assert(searchedFields != null);
            String weightedFields = getDismaxWeightedFieldString(field, searchedFields);
            return "_query_:\"{!edismax qf=\\\""+weightedFields+"\\\" mm=\\\"100%\\\"}"+SolrUtils.escapeQuotes(text)+"\"";
        } else {
          return String.format("%s:%s",field,text);
        }
    }

    private String buildComplexPhraseQuery(String text, String[] searchedFields) {
        StringBuilder sb = new StringBuilder("_query_:\"{!zimbrawildcard ");
        sb.append("fields=\\\"").append(Joiner.on(" ").join(searchedFields)).append("\\\"")
        .append(" maxExpansions=\\\"").append(ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexWildcardMaxTermsExpanded, 20000)).append("\\\"}")
        .append(SolrUtils.escapeQuotes(text))
        .append("\"");
        return sb.toString();
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

        @Override
        public Document doc(ZimbraIndexDocumentID docID) throws IOException, ServiceException {
            if (docID == null || !indexExists()) {
                return null;
            }
            SolrServer solrServer = getSolrServer();
            try {
                if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                    waitForIndexCommit(LC.zimbra_index_commit_wait.intValue());
                }
                if (docID instanceof ZimbraSolrDocumentID) {
                    SolrQuery q = new SolrQuery().setQuery(String.format("%s:%s",LuceneFields.L_MAILBOX_BLOB_ID,((ZimbraSolrDocumentID) docID).getDocID())).setRows(1);
                    q.setFields(LuceneFields.L_PARTNAME, LuceneFields.L_FILENAME, LuceneFields.L_SORT_SIZE,
                            LuceneFields.L_SORT_ATTACH, LuceneFields.L_SORT_FLAG, LuceneFields.L_SORT_PRIORITY,
                            LuceneFields.L_MAILBOX_BLOB_ID, LuceneFields.L_SORT_DATE, LuceneFields.L_VERSION);
                    setupRequest(q, solrServer);
                    QueryRequest req = new QueryRequest(q);
                    ZimbraLog.index.debug(String.format("retrieving document by query %s ",q.toString()));
                    try {
                        QueryResponse resp = (QueryResponse) processRequest(solrServer, req);
                        SolrDocument solrDoc = resp.getResults().get(0);
                        Document document = new Document();
                        for(String fieldName : solrDoc.getFieldNames()) {
                            document.add(new Field(fieldName, solrDoc.getFieldValue(fieldName).toString(), Field.Store.YES, Field.Index.NO));
                        }
                        return document;
                    } catch (SolrServerException e) {
                        ZimbraLog.index.error("Solr problem geting document %s, from mailbox %s",docID.toString(), accountId,e);
                    }  catch (RemoteSolrException e) {
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
            if(!indexExists()) {
                return 0;
            }
            SolrServer solrServer = getSolrServer();
            try {
                if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                    waitForIndexCommit(LC.zimbra_index_commit_wait.intValue());
                }
                SolrQuery q = new SolrQuery().setQuery(TermToQuery(term)).setRows(0);
                setupRequest(q, solrServer);
                QueryRequest req = new QueryRequest(q);
                QueryResponse resp = (QueryResponse) processRequest(solrServer, req);
                return (int) resp.getResults().getNumFound();
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Solr search problem getting docFreq for mailbox %s", accountId,e);
            } catch (RemoteSolrException e) {
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

        //TODO: return all fields that we need in topfielddocs so that we don't have to go back to index again
        @Override
        public ZimbraTopDocs search(Query query, ZimbraTermsFilter filter,
                int n, Sort sort) throws IOException, ServiceException {
            List<ZimbraScoreDoc>scoreDocs = Lists.newArrayList();
            List<SortField> sortFields = Lists.newArrayList();
            List<IndexDocument> indexDocs = Lists.newArrayList();
            float maxScore = 0;
            int totalHits = 0;

            if(!indexExists()) {
                return ZimbraTopDocs.create(totalHits, scoreDocs, maxScore, indexDocs);
            }

            SolrServer solrServer = getSolrServer();
            if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                waitForIndexCommit(LC.zimbra_index_commit_wait.intValue());
            }

            if (sort != null) {
                Collections.addAll(sortFields, sort.getSort());
            }
            Query optimized = optimizeQueryOps(escapeSpecialChars(query));
            String szq = queryToString(optimized);
            SolrQuery q = new SolrQuery().setQuery(szq).setRows(n);

            setupRequest(q, solrServer);
            if(filter != null) {
                q.addFilterQuery(TermsToQuery(filter.getTerms()));
            }
            q.setFields(LuceneFields.L_MAILBOX_BLOB_ID,"score",LuceneFields.L_PARTNAME, LuceneFields.L_FILENAME, LuceneFields.L_SORT_SIZE,
                    LuceneFields.L_SORT_ATTACH, LuceneFields.L_SORT_FLAG, LuceneFields.L_SORT_PRIORITY,
                    LuceneFields.L_SORT_DATE, LuceneFields.L_VERSION);
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
                        score = (Float)solrDoc.getFieldValue("score");
                    } catch (RuntimeException e) {
                        score = new Float(0);
                    }
                    maxScore = Math.max(maxScore, score);
                    indexDocs.add(toIndexDocument(solrDoc));
                    scoreDocs.add(ZimbraScoreDoc.create(new ZimbraSolrDocumentID(solrDoc.getFieldValue(LuceneFields.L_MAILBOX_BLOB_ID).toString()),((Float)solrDoc.getFieldValue("score")).longValue()));
                }
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Solr search problem mailbox %s, query %s", accountId,query.toString(),e);
                return ZimbraTopDocs.create(totalHits, scoreDocs, maxScore, indexDocs);
            } catch (RemoteSolrException e) {
                ZimbraLog.index.error("Solr search problem mailbox %s, query %s", accountId,query.toString(),e);
                return ZimbraTopDocs.create(totalHits, scoreDocs, maxScore, indexDocs);
            } finally {
                shutdown(solrServer);
            }
            return ZimbraTopDocs.create(totalHits, scoreDocs, maxScore, indexDocs);
        }

        private Query escapeSpecialChars(Query query) {
            Query escapedQuery;
            if (query instanceof TermQuery) {
                Term term = ((TermQuery) query).getTerm();
                String escaped = SolrUtils.escapeSpecialChars(term.text(), WildcardEscape.ZIMBRA);
                escapedQuery = new TermQuery(new Term(term.field(), escaped));
            } else {
                // if not a term query, it's a boolean query
                BooleanClause[] clauses = ((BooleanQuery) query).getClauses();
                escapedQuery = new BooleanQuery();
                for (int i = 0; i < clauses.length; i++) {
                    BooleanClause clause = clauses[i];
                    ((BooleanQuery) escapedQuery).add(escapeSpecialChars(clause.getQuery()), clause.getOccur());
                }
            }
            return escapedQuery;
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
            if(!indexExists()) {
                return 0;
            }
            SolrServer solrServer = getSolrServer();
            try {
                SolrQuery q = new SolrQuery().setQuery("*:*").setRows(0);
                setupRequest(q, solrServer);
                QueryRequest req = new QueryRequest(q);
                QueryResponse resp = (QueryResponse) processRequest(solrServer, req);
                SolrDocumentList solrDocList = resp.getResults();
                return (int)solrDocList.getNumFound();
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Caught SolrServerException retrieving number of documents in mailbox %s", accountId,e);
            } catch (RemoteSolrException e) {
                ZimbraLog.index.error("Caught SolrServerException retrieving number of documents in mailbox %s", accountId,e);
            } catch (IOException e) {
                ZimbraLog.index.error("Caught SolrServerException retrieving number of documents in mailbox %s", accountId,e);
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
            } catch (SolrServerException e) {
                throw ServiceException.FAILURE(e.getMessage(),e);
            }
        }

        private final class SolrTermValueEnumeration implements TermFieldEnumeration {
            private LinkedList<org.apache.solr.client.solrj.response.TermsResponse.Term> termEnumeration = Lists.newLinkedList();
            private final String fieldName;
            private String last = null;

            private void primeTermsComponent(String firstTermValue, boolean includeLower) throws IOException, ServiceException, SolrServerException {
                if(!indexExists()) {
                    return;
                }
                SolrServer solrServer = getSolrServer();
                if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                    waitForIndexCommit(LC.zimbra_index_commit_wait.intValue());
                }
                SolrQuery q = new SolrQuery().setRequestHandler("/terms");

                setupRequest(q, solrServer);
                q.setTerms(true);
                q.addTermsField(fieldName);
                q.setTermsLimit(Provisioning.getInstance().getLocalServer().getIndexTermsCacheSize());
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

            private SolrTermValueEnumeration(String field, String firstTermValue) throws IOException, ServiceException, SolrServerException {
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
                    } catch (IOException | ServiceException | SolrServerException e) {
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
                } catch (IOException | ServiceException | SolrServerException e) {
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

        protected void setAction(UpdateRequest req) {
            req.setAction(ACTION.COMMIT, true, true, false);
        }

        @Override
        public void add(List<Mailbox.IndexItemEntry> entries) throws IOException, ServiceException {
            if(!indexExists()) {
                initIndex();
            }
            SolrServer solrServer = getSolrServer();
            UpdateRequest req = new UpdateRequest();
            setupRequest(req, solrServer);
            if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                setAction(req);
            }
            for (IndexItemEntry entry : entries) {
                if (entry.documents == null) {
                    ZimbraLog.index.warn("NULL index data item=%s", entry);
                    continue;
                }
                int partNum = 1;
                for (IndexDocument doc : entry.documents) {
                    SolrInputDocument solrDoc;
                    // doc can be shared by multiple threads if multiple mailboxes are referenced in a single email
                    synchronized (doc) {
                        setFields(entry.item, doc);
                        solrDoc = doc.toInputDocument();
                        solrDoc.addField(SOLR_ID_FIELD, String.format("%d_%d",entry.item.getId(),partNum));
                        partNum++;
                        if (ZimbraLog.index.isTraceEnabled()) {
                            ZimbraLog.index.trace("Adding solr document %s", solrDoc.toString());
                        }
                    }
                    req.add(solrDoc);
                }
            }
            try {
                if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                    incrementUpdateCounter(solrServer);
                }
                processRequest(solrServer, req);
            } catch (RemoteSolrException | SolrServerException e) {
                ZimbraLog.index.error("Problem indexing documents", e);
            }  finally {
                shutdown(solrServer);
            }
        }

        @Override
        public void addDocument(MailItem item, List<IndexDocument> docs) throws IOException, ServiceException {
            if(!indexExists()) {
                initIndex();
            }
            if (docs == null || docs.isEmpty()) {
                return;
            }

            int partNum = 1;
            for (IndexDocument doc : docs) {
                SolrInputDocument solrDoc;
                // doc can be shared by multiple threads if multiple mailboxes are referenced in a single email
                synchronized (doc) {
                    setFields(item, doc);
                    solrDoc = doc.toInputDocument();
                    solrDoc.addField(SOLR_ID_FIELD, String.format("%d_%d",item.getId(),partNum));
                    partNum++;
                    if (ZimbraLog.index.isTraceEnabled()) {
                        ZimbraLog.index.trace("Adding solr document %s", solrDoc.toString());
                    }
                }
                SolrServer solrServer = getSolrServer();
                UpdateRequest req = new UpdateRequest();
                setupRequest(req, solrServer);
                req.add(solrDoc);
                if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                    setAction(req);
                }
                try {
                    if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                        incrementUpdateCounter(solrServer);
                    }
                    processRequest(solrServer, req);
                } catch (SolrServerException e) {
                    ZimbraLog.index.error("Problem indexing document with id=%d", item.getId(),e);
                } catch (RemoteSolrException e) {
                    ZimbraLog.index.error("Problem indexing document with id=%d", item.getId(),e);
                }  finally {
                    shutdown(solrServer);
                }
            }
        }

        protected void incrementUpdateCounter(SolrServer solrServer) throws ServiceException {
            try {
                if(!indexExists()) {
                    return;
                }
                SolrQuery q = new SolrQuery().setParam("action", " increment");
                QueryRequest req = new QueryRequest(q);
                req.setPath("/commitcount");
                setupRequest(req, solrServer);
                req.process(solrServer);
            } catch (SolrServerException e) {
                ZimbraLog.index.error("Problem increasing commit counter for Core %s", accountId,e);
            }
        }
        @Override
        public void deleteDocument(List<Integer> ids) throws IOException,ServiceException {
            if(!indexExists()) {
                return;
            }
            SolrServer solrServer = getSolrServer();
            try {
                for (Integer id : ids) {
                    UpdateRequest req = new UpdateRequest().deleteByQuery(String.format("%s:%d",LuceneFields.L_MAILBOX_BLOB_ID,id));
                    setupRequest(req, solrServer);
                    if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                        setAction(req);
                    }
                    try {
                        if(ProvisioningUtil.getServerAttribute(ZAttrProvisioning.A_zimbraIndexManualCommit, false)) {
                            incrementUpdateCounter(solrServer);
                        }
                        processRequest(solrServer, req);
                        ZimbraLog.index.debug("Deleted document id=%d", id);
                    } catch (SolrServerException e) {
                        ZimbraLog.index.error("Problem deleting document with id=%d", id,e);
                    } catch (RemoteSolrException e) {
                        ZimbraLog.index.error("Problem deleting document with id=%d", id,e);
                    }
                }
            } finally {
                shutdown(solrServer);
            }
        }

        @Override
        public void compact() {
            // TODO Auto-generated method stub

        }
    }

    public abstract SolrServer getSolrServer() throws ServiceException;

    public abstract void shutdown(SolrServer server);

    protected SolrResponse processRequest(SolrServer server, SolrRequest request)
            throws SolrServerException, IOException {
        return request.process(server);
    }

    @Override
    public void waitForIndexCommit(int maxWaitTimeMillis) throws ServiceException  {
        SolrServer solrServer = getSolrServer();
        int waitIncrement = Math.max(maxWaitTimeMillis/3, 500);
        long startWait = System.currentTimeMillis();
        while (maxWaitTimeMillis > 0) {
            if(indexExists()) {
                SolrQuery q = new SolrQuery().setParam("action", "get");
                QueryRequest req = new QueryRequest(q);
                req.setPath("/commitcount");
                setupRequest(req, solrServer);
                QueryResponse resp;
                try {
                    resp = req.process(solrServer);
                    Integer outstandingCommits = (Integer)resp.getResponse().get("count");
                    if(outstandingCommits == null || outstandingCommits == 0) {
                        break;
                    } else if (outstandingCommits < 0) {
                        ZimbraLog.index.warn("outstanding commits is less than zero, this may be a problem in solr?");
                        break;
                    } else {
                        ZimbraLog.index.debug("waiting for %d outstanding commits", outstandingCommits);
                        try {
                            Thread.sleep(waitIncrement);
                        } catch (InterruptedException e) {
                            break;
                        }
                        maxWaitTimeMillis = maxWaitTimeMillis - waitIncrement;
                    }
                } catch (SolrServerException e) {
                    ZimbraLog.index.error("Problem waiting for index commit count to go to zero for Core: %s", accountId,e);
                    break;
                }
            } else {
                try {
                    Thread.sleep(waitIncrement);
                } catch (InterruptedException e) {
                    break;
                }
                maxWaitTimeMillis = maxWaitTimeMillis - waitIncrement;
            }
        }
        ZimbraLog.index.debug("waited %dms for commit", System.currentTimeMillis() - startWait);
    }
}
