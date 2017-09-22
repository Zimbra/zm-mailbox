package com.zimbra.cs.index.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.primitives.Ints;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.IndexDocument;
import com.zimbra.cs.index.IndexStore;
import com.zimbra.cs.index.LuceneFields;
import com.zimbra.cs.index.ZimbraIndexSearcher;
import com.zimbra.cs.index.ZimbraTopDocs;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class LuceneSearchHistoryIndexTest {

    private IndexStore idxStore;
    private LuceneSearchHistoryIndex index;
    private Provisioning prov;
    private Account acct;
    private Mailbox mbox;
    private BiMap<String, Integer> idMap = HashBiMap.create();

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        prov = Provisioning.getInstance();
        acct = prov.createAccount("luceneSearchHistoryIndexTest@zimbra.com", "test123", new HashMap<String, Object>());
        index = new LuceneSearchHistoryIndex(acct);
        mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        idxStore = IndexStore.getFactory().getIndexStore(mbox.getAccountId());
        addToIndex(1, "apple");
        addToIndex(2, "an apple");
        addToIndex(3, "apple juice");
        addToIndex(4, "green apple");
        addToIndex(5, "applesauce");
        addToIndex(6, "appleseeds");
        addToIndex(7, "red apples");
        addToIndex(8, "green applesauce");
        addToIndex(9, "cranapple");
        addToIndex(10, "a banana");
    }

    private void addToIndex(int id, String searchString) throws ServiceException {
        index.add(id, searchString);
        idMap.put(searchString, id);
    }

    private int getId(String searchString) {
        return idMap.get(searchString);
    }

    private String getString(int id) {
        return idMap.inverse().get(id);
    }

    @After
    public void tearDown() throws Exception {
        index.deleteAll();
        idxStore.deleteIndex();
        prov.deleteAccount(acct.getId());
    }

    @Test
    public void testAdd() throws Exception {
        //verify that the index has the expected terms
        ZimbraIndexSearcher searcher = idxStore.openSearcher();
        Query query = new TermQuery(new Term(LuceneFields.L_ITEM_TYPE, IndexDocument.SEARCH_HISTORY_TYPE));
        ZimbraTopDocs results = searcher.search(query, 10);
        assertEquals("should see 10 terms in the index", 10, results.getTotalHits());
    }

    private boolean ranksHigherThan(List<Integer> resultIds, String[] arr1, String[] arr2) {
        //return true if all entries in arr1 outrank all entries in arr2 in resultIds
        int[] indexArr1 = new int[arr1.length];
        int[] indexArr2 = new int[arr2.length];
        for (int i=0; i<arr1.length; i++) {
            indexArr1[i] = resultIds.indexOf(getId(arr1[i]));
        }
        for (int i=0; i<arr2.length; i++) {
            indexArr2[i] = resultIds.indexOf(getId(arr2[i]));
        }
        return Ints.max(indexArr1) < Ints.min(indexArr2);
    }

    private String resultsAsString(List<Integer> resultIds) {
        List<String> results = new ArrayList<String>(resultIds.size());
        for (int id: resultIds) {
            results.add(getString(id));
        }
        return Joiner.on(",").join(results);
    }

    private static String err(String missingResult, String results, boolean shouldContain) {
        if (shouldContain) {
            return String.format("'%s' is missing: %s", missingResult, results);
        } else {
            return String.format("'%s' is present: %s", missingResult, results);
        }
    }

    private void testResultSetPresense(List<Integer> resultIds, String[] expected, String[] notExpected, String searchResultsStr) {

        for (String term: expected) {
            assertTrue(err(term, searchResultsStr, true), resultIds.contains(getId(term)));
        }
        for (String term: notExpected) {
            assertFalse(err(term, searchResultsStr, false), resultIds.contains(getId(term)));
        }
    }

    @Test
    public void testSearchWord() throws Exception {
        List<Integer> resultIds = index.search("apple");
        String searchResultsStr = resultsAsString(resultIds);

        String[] expected = { "apple", "an apple", "apple juice", "green apple",
                "applesauce", "appleseeds", "red apples", "green applesauce" };

        String[] notExpected = {"cranapple", "a banana"};

        testResultSetPresense(resultIds, expected, notExpected, searchResultsStr);

        String[] termMatches  = { "an apple", "apple juice", "green apple" };
        String[] edgeMatches  = { "applesauce", "appleseeds" };
        String[] ngramMatches = { "green applesauce", "red apples" };

        assertEquals("exact match should be first", 0, resultIds.indexOf(getId("apple")));
        assertTrue(String.format("term matches don't outrank edge matches: %s", searchResultsStr), ranksHigherThan(resultIds, termMatches, edgeMatches));
        assertTrue(String.format("edge matches don't outrank ngram matches: %s", searchResultsStr), ranksHigherThan(resultIds, edgeMatches, ngramMatches));
    }

    @Test
    public void testSearchPrefix() throws Exception {
        List<Integer> resultIds = index.search("a");
        String searchResultsStr = resultsAsString(resultIds);

        String[] expected = { "apple", "an apple", "apple juice", "green apple",
                "applesauce", "appleseeds", "red apples", "green applesauce", "a banana"};

        String[] notExpected = {"cranapple"};

        testResultSetPresense(resultIds, expected, notExpected, searchResultsStr);

        String[] edgeMatches  = { "apple", "an apple", "apple juice", "applesauce", "appleseeds", "a banana" };
        String[] ngramMatches = { "green apple", "green applesauce", "red apples" };

        assertTrue(String.format("edge matches don't outrank ngram matches: %s", searchResultsStr), ranksHigherThan(resultIds, edgeMatches, ngramMatches));
    }

    @Test
    public void testDelete() throws Exception {
        List<Integer> toDelete = new ArrayList<Integer>(1);
        toDelete.add(getId("apple"));
        toDelete.add(getId("an apple"));
        index.delete(toDelete);

        List<Integer> resultIds = index.search("a");
        String searchResultsStr = resultsAsString(resultIds);
        String[] expected = { "apple juice", "green apple", "applesauce", "appleseeds", "red apples", "green applesauce", "a banana"};
        String[] notExpected = {"apple", "an apple", "cranapple"};

        testResultSetPresense(resultIds, expected, notExpected, searchResultsStr);
    }

    @Test
    public void deleteAll() throws Exception {
        index.deleteAll();
        ZimbraIndexSearcher searcher = idxStore.openSearcher();
        Query query = new TermQuery(new Term(LuceneFields.L_ITEM_TYPE, IndexDocument.SEARCH_HISTORY_TYPE));
        ZimbraTopDocs results = searcher.search(query, 10);
        assertEquals("should not see any matching terms in the index", 0, results.getTotalHits());
        Collection<Integer> ids = index.search("t");
        assertTrue("should not get any results from the index", ids.isEmpty());
    }
}
