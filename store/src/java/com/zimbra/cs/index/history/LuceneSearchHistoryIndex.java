package com.zimbra.cs.index.history;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
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
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.index.ZimbraScoreDoc;
import com.zimbra.cs.index.ZimbraTopDocs;
import com.zimbra.cs.index.analysis.HalfwidthKanaVoicedMappingFilter;
import com.zimbra.cs.index.analysis.SearchHistoryQueryAnalyzer;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryIndex;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

public class LuceneSearchHistoryIndex implements SearchHistoryIndex {

    private static final int MAX_INDEX_DOCS = 1000;
    private IndexStore index;

    public LuceneSearchHistoryIndex(Account acct) throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        this.index = IndexStore.getFactory().getIndexStore(mbox);
    }

    @Override
    public void add(int id, String searchString) throws ServiceException {
        IndexDocument doc = IndexDocument.fromSearchString(id, searchString);
        try {
            Indexer indexer = index.openIndexer();
            indexer.addDocument(doc);
            indexer.close();
        } catch (IOException e) {
            ZimbraLog.search.error("unable to index search history entry %s (id=%d)", searchString, id, e);
        }
    }

    private Query buildQuery(String searchString) {
        DisjunctionMaxQuery dismax = new DisjunctionMaxQuery(0);
        boolean wordBoundary = searchString.endsWith(" ");
        Query exactMatch = new TermQuery(new Term(LuceneFields.L_SEARCH_EXACT, searchString));
        dismax.add(exactMatch);
        Query edgeMatch = new PrefixQuery(new Term(LuceneFields.L_SEARCH_EXACT, searchString));
        dismax.add(edgeMatch);
        BooleanQuery termMatch = new BooleanQuery();
        dismax.add(termMatch);

        BooleanQuery prefixMatch = null;
        if (!wordBoundary) {
            prefixMatch = new BooleanQuery();
            dismax.add(prefixMatch);
        }


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
        exactMatch.setBoost(10);
        termMatch.setBoost(5);
        edgeMatch.setBoost(3);
        if (!wordBoundary) {
            prefixMatch.setBoost(1);
        }

        Analyzer analyzer = new SearchHistoryQueryAnalyzer();
        try {
            Reader reader = new HalfwidthKanaVoicedMappingFilter(new StringReader(searchString));
            TokenStream tokenStream = analyzer.tokenStream(LuceneFields.L_SEARCH_TERMS, reader);
            CharTermAttribute termAttr = tokenStream.addAttribute(CharTermAttribute.class);
            try {
                tokenStream.reset();
                while (tokenStream.incrementToken()) {
                    String token = termAttr.toString();
                    termMatch.add(new TermQuery(new Term(LuceneFields.L_SEARCH_TERMS, token)), Occur.MUST);
                    if (!wordBoundary) {
                        prefixMatch.add(new PrefixQuery(new Term(LuceneFields.L_SEARCH_TERMS, token)), Occur.MUST);
                    }
                }
                tokenStream.end();
                tokenStream.close();
            } catch (IOException e) {
            }
            return dismax;
        } finally {
            analyzer.close();
        }
    }

    @Override
    public List<Integer> search(String searchString)
            throws ServiceException {
        try {
            ZimbraIndexSearcher searcher = index.openSearcher();
            Query query = buildQuery(searchString);
            ZimbraTopDocs docs = searcher.search(query, null, MAX_INDEX_DOCS);
            List<Integer> entryIds = new ArrayList<Integer>(docs.getTotalHits());
            for (ZimbraScoreDoc scoreDoc: docs.getScoreDocs()) {
                Document doc =  searcher.doc(scoreDoc.getDocumentID());
                entryIds.add(Integer.parseInt(doc.get(LuceneFields.L_SEARCH_ID)));
            }
            return entryIds;
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
            indexer.close();
        } catch (IOException e) {
            ZimbraLog.search.error("unable to delete %s search history docs from the index", ids.size(), e);
        }
    }

    @Override
    public void deleteAll() throws ServiceException {
        Term term = new Term(LuceneFields.L_ITEM_TYPE, IndexDocument.SEARCH_HISTORY_TYPE);
        Query query = new TermQuery(term);
        ZimbraIndexSearcher searcher;
        try {
            searcher = index.openSearcher();
            int historySize = searcher.docFreq(term);
            if (historySize == 0) {
                return; //nothing to do
            }
            ZimbraTopDocs docs = searcher.search(query, historySize);
            List<Integer> entryIds = new ArrayList<Integer>(docs.getTotalHits());
            for (ZimbraScoreDoc scoreDoc: docs.getScoreDocs()) {
                Document doc =  searcher.doc(scoreDoc.getDocumentID());
                entryIds.add(Integer.parseInt(doc.get(LuceneFields.L_SEARCH_ID)));
            }
            delete(entryIds);
        } catch (IOException e) {
            ZimbraLog.search.error("unable to delete search history", e);
        }
    }
}
