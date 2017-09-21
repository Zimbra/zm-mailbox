package com.zimbra.cs.index.history;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.history.SavedSearchPromptLog.SavedSearchStatus;
import com.zimbra.cs.index.history.SearchHistory.SearchHistoryParams;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class SearchHistoryTest {

    private Provisioning prov;
    private Account acct;
    private Mailbox mbox;
    private int searchesForPrompt = 3;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();

        prov = Provisioning.getInstance();
        acct = prov.createAccount("dbSearchHistoryIndexTest@zimbra.com", "test123", new HashMap<String, Object>());
        acct.setNumSearchesForSavedSearchPrompt(searchesForPrompt);
        mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
    }

    @After
    public void tearDown() throws Exception {
        prov.deleteAccount(acct.getId());
        MailboxTestUtil.clearData();
    }

    private void logSearch(String search, long timestamp, boolean expectPrompt) throws Exception {
        boolean prompt = mbox.addToSearchHistory(null, search, timestamp);
        if (expectPrompt) {
            assertTrue("should see saved search prompt", prompt);
        } else {
            assertFalse("should not see saved search prompt", prompt);
        }
    }

    @Test
    public void testSearchHistory() throws Exception {
        long now = System.currentTimeMillis();
        logSearch("search-1", now-8000, false);
        logSearch("search-2", now-7000, false);
        logSearch("search-3", now-6000, false);
        logSearch("search-1", now-4000, false);
        logSearch("search-2", now-3000, false);
        logSearch("search-1", now-2000, true);

        assertEquals("search-1 should be counted 3 times", 3, mbox.getSearchHistoryCount(null, "search-1"));
        assertEquals("search-2 should be counted 2 times", 2, mbox.getSearchHistoryCount(null, "search-2"));
        assertEquals("search-3 should be counted 1 time",  1, mbox.getSearchHistoryCount(null, "search-3"));

        assertEquals("status for search-1 should be PROMPTED", SavedSearchStatus.PROMPTED, mbox.getSavedSearchPromptStatus(null, "search-1"));
        assertEquals("status for search-2 should be NOT_PROMPTED", SavedSearchStatus.NOT_PROMPTED, mbox.getSavedSearchPromptStatus(null, "search-2"));
        assertEquals("status for search-3 should be NOT_PROMPTED", SavedSearchStatus.NOT_PROMPTED, mbox.getSavedSearchPromptStatus(null, "search-3"));

        mbox.setSavedSearchPromptStatus(null, "search-1", SavedSearchStatus.CREATED);
        assertEquals("status for search-1 should be CREATED", SavedSearchStatus.CREATED, mbox.getSavedSearchPromptStatus(null, "search-1"));

        logSearch("search-2", now-1000, true);
        assertEquals("status for search-2 should be PROMPTED", SavedSearchStatus.PROMPTED, mbox.getSavedSearchPromptStatus(null, "search-2"));
        mbox.setSavedSearchPromptStatus(null, "search-2", SavedSearchStatus.REJECTED);
        assertEquals("status for search-2 should be REJECTED", SavedSearchStatus.REJECTED, mbox.getSavedSearchPromptStatus(null, "search-2"));

        acct.setSearchHistoryAge("5s"); //doesn't actually purge yet, but should ignore first 3 searches
        assertEquals("search-1 should be counted 2 times", 2, mbox.getSearchHistoryCount(null, "search-1"));
        assertEquals("search-2 should be counted 1 time",  2, mbox.getSearchHistoryCount(null, "search-2"));
        assertEquals("search-3 should be counted 0 times", 0, mbox.getSearchHistoryCount(null, "search-3"));
        mbox.purgeSearchHistory(null); // this should purge the first three items; results should be the same
        assertEquals("search-1 should be counted 2 times", 2, mbox.getSearchHistoryCount(null, "search-1"));
        assertEquals("search-2 should be counted 2 times", 2, mbox.getSearchHistoryCount(null, "search-2"));
        assertEquals("search-3 should be counted 0 times", 0, mbox.getSearchHistoryCount(null, "search-3"));
        mbox.deleteSearchHistory(null);
        assertEquals("search-1 should be counted 0 times", 0, mbox.getSearchHistoryCount(null, "search-1"));
        assertEquals("search-2 should be counted 0 times", 0, mbox.getSearchHistoryCount(null, "search-2"));
        assertEquals("search-3 should be counted 0 times", 0, mbox.getSearchHistoryCount(null, "search-3"));
    }

    @Test
    public void testQuerySearchHistory() throws Exception {
        long now = System.currentTimeMillis();
        mbox.addToSearchHistory(null, "apple", now-11000);
        mbox.addToSearchHistory(null, "an apple", now-9000);
        mbox.addToSearchHistory(null, "apple juice", now-8000);
        mbox.addToSearchHistory(null, "green apple", now-7000);
        mbox.addToSearchHistory(null, "applesauce", now-6000);
        mbox.addToSearchHistory(null, "appleseeds", now-5000);
        mbox.addToSearchHistory(null, "red apples", now-4000);
        mbox.addToSearchHistory(null, "green applesauce", now-3000);
        mbox.addToSearchHistory(null, "cranapple", now-2000);
        mbox.addToSearchHistory(null, "a banana", now-1000);

        SearchHistoryParams params = new SearchHistoryParams();
        params.setPrefix("apple");
        List<String> results = mbox.getSearchHistory(null, params);
        assertEquals("should see 8 results", 8, results.size());
        //no need to test the entire result order here; this is tested in LuceneSearchHistoryIndexTest.
        //just verify that "apple" is the first result even though it's the oldest
        assertEquals("'apple' should be the first result", "apple", results.get(0));

        //setting a result limit shouldn't change the order of results
        params.setNumResults(5);
        List<String> limitedByNum = mbox.getSearchHistory(null, params);
        assertEquals("should see 5 results", 5, limitedByNum.size());
        for (int i=0; i<5; i++) {
            assertEquals("results don't match", results.get(i), limitedByNum.get(i));
        }

        params.setNumResults(0);
        acct.setSearchHistoryAge("10s"); //this should push 'apple' out of the result set
        List<String> limitedByAge = mbox.getSearchHistory(null, params);
        assertFalse("'apple' shouldn't be in the results", limitedByAge.contains("apple"));
        for (int i=0; i<6; i++) {
            assertEquals("results don't match", results.get(i+1), limitedByAge.get(i));
        }
    }

    private void testResults(List<String> results, int expectedSize) throws Exception {
        assertEquals(String.format("should see %d results", expectedSize), expectedSize, results.size());
        int i = 5;
        for (String result: results) {
            assertEquals("incorrect result", String.format("search-%d", i), result);
            i--;
        }
    }

    @Test
    public void testGetSearchHistory() throws Exception {
        long now = System.currentTimeMillis();
        mbox.addToSearchHistory(null, "search-1", now-6000);
        mbox.addToSearchHistory(null, "search-2", now-4000);
        mbox.addToSearchHistory(null, "search-3", now-3000);
        mbox.addToSearchHistory(null, "search-4", now-2000);
        mbox.addToSearchHistory(null, "search-5", now-1000);

        SearchHistoryParams params = new SearchHistoryParams();
        List<String> results = mbox.getSearchHistory(null, params);
        testResults(results, 5);

        //test limiting by number
        params.setNumResults(3);
        results = mbox.getSearchHistory(null, params);
        testResults(results, 3);

        //test limiting by age
        params.setNumResults(0);
        acct.setSearchHistoryAge("5s");
        results = mbox.getSearchHistory(null, params);
        testResults(results, 4);

        //test limiting by both
        params.setNumResults(2);
        results = mbox.getSearchHistory(null, params);
        testResults(results, 2);

        //puts search-1 at the front of results, pushes out search-4 since it's outside the limit
        mbox.addToSearchHistory(null, "search-1", now);
        results = mbox.getSearchHistory(null, params);
        assertEquals("should see 2 results", 2, results.size());
        assertEquals("first result should be search-1", "search-1", results.get(0));
        assertEquals("second result should be search-5", "search-5", results.get(1));
    }
}
