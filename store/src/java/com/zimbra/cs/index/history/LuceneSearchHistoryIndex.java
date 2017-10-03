package com.zimbra.cs.index.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.Indexer;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.ZimbraIndexDocumentID;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.index.ZimbraScoreDoc;
import com.zimbra.cs.index.ZimbraTopDocs;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryIndex;
import com.zimbra.cs.index.solr.ZimbraSolrDocumentID;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

public class LuceneSearchHistoryIndex implements SearchHistoryIndex {

    private static final int MAX_INDEX_DOCS = 1000;
    private static final int EXACT_MATCH_BOOST = 10;
    private static final int TERM_MATCH_BOOST = 5;
    private static final int EDGE_MATCH_BOOST = 3;
    private static final int PREFIX_MATCH_BOOST = 1;
    private IndexStore index;

    public LuceneSearchHistoryIndex(Account acct) throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        this.index = IndexStore.getFactory().getIndexStore(mbox.getAccountId());
    }

    @Override
    public void add(int id, String searchString) throws ServiceException {
        IndexDocument doc = IndexDocument.fromSearchString(id, searchString);
        try (Indexer indexer = index.openIndexer()){
            indexer.addSearchHistoryDocument(doc);
        } catch (IOException e) {
            ZimbraLog.search.error("unable to index search history entry %s (id=%d)", searchString, id, e);
        }
    }

    private Query buildQuery(String searchString) {
        /*
         * We want the match precedence to be:
         * 1. Exact match
         * 2. Term match ("apple" matches "apple juice" or "green apple")
         * 3. Edge match ("apple" matches "applesauce")
         * 4. Prefix match ("apple" matches "green apples")
         *
         * suffix/infix matches are NOT returned ("apple" should not match "cranapple")
         *
         * if we're on a word boundary like "apple ", don't suggest prefix matches
         */
        List<Query> disjuncts = new ArrayList<Query>();
        boolean wordBoundary = searchString.endsWith(" ");
        Query exactMatchQuery = new TermQuery(new Term(LuceneFields.L_SEARCH_EXACT, searchString));
        disjuncts.add(new BoostQuery(exactMatchQuery, EXACT_MATCH_BOOST));
        Query termMatchQuery = new TermQuery(new Term(LuceneFields.L_SEARCH_TERMS, searchString));
        disjuncts.add(new BoostQuery(termMatchQuery, TERM_MATCH_BOOST));
        Query edgeMatchQuery = new PrefixQuery(new Term(LuceneFields.L_SEARCH_EXACT, searchString));
        disjuncts.add(new BoostQuery(edgeMatchQuery, EDGE_MATCH_BOOST));
        if (!wordBoundary) {
            Query prefixMatchQuery = new PrefixQuery(new Term(LuceneFields.L_SEARCH_TERMS, searchString));
            disjuncts.add(new BoostQuery(prefixMatchQuery, PREFIX_MATCH_BOOST));
        }
        Query dismax = new DisjunctionMaxQuery(disjuncts, 0);
        return dismax;
    }

    private List<Integer> searchInternal(Query query) throws ServiceException, IOException {
        ZimbraIndexSearcher searcher = index.openSearcher();
        ZimbraTopDocs docs = searcher.search(query, null, MAX_INDEX_DOCS, null, LuceneFields.L_SEARCH_ID, null);
        List<Integer> entryIds = new ArrayList<Integer>(docs.getTotalHits());
        for (ZimbraScoreDoc scoreDoc: docs.getScoreDocs()) {
            ZimbraIndexDocumentID docId = scoreDoc.getDocumentID();
            if (docId instanceof ZimbraSolrDocumentID) {
                //this cast can be avoided once ZimbraScoreDoc can expose arbitrary doc fields
                ZimbraSolrDocumentID solrId = (ZimbraSolrDocumentID) docId;
                entryIds.add(Integer.parseInt(solrId.getDocID()));
            }
        }
        return entryIds;

    }
    @Override
    public List<Integer> search(String searchString) throws ServiceException {
        try {
            return searchInternal(buildQuery(searchString));
        } catch (IOException e) {
            ZimbraLog.search.error("unable to search search history for prefix '%s'", searchString, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void delete(Collection<Integer> ids) throws ServiceException {
        List<Integer> idList = new ArrayList<Integer>(ids);
        try (Indexer indexer = index.openIndexer()) {
            indexer.deleteDocument(idList, LuceneFields.L_SEARCH_ID);
        } catch (IOException e) {
            ZimbraLog.search.error("unable to delete %s search history docs from the index", ids.size(), e);
        }
    }

    @Override
    public void deleteAll() throws ServiceException {
        //TODO: delete all docs in one query
        Term term = new Term(LuceneFields.L_ITEM_TYPE, IndexDocument.SEARCH_HISTORY_TYPE);
        Query query = new TermQuery(term);
        try {
            List<Integer> entryIds = searchInternal(query);
            delete(entryIds);
        } catch (IOException e) {
            ZimbraLog.search.error("unable to delete search history", e);
        }
    }
}
