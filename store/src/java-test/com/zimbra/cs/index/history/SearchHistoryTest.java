package com.zimbra.cs.index.history;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.history.SavedSearchPromptLog.SavedSearchStatus;
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
}
