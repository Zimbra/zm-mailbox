package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZSearchParams;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;

public class TestSearchHistory {

    private static String USER = "TestSearchHistory-test";
    private Account acct;
    private ZMailbox mbox;

    @Before
    public void setUp() throws Exception {
        cleanUp();
        acct = TestUtil.createAccount(USER);
        mbox = TestUtil.getZMailbox(USER);
        search("apple");
        search("applesauce");
        search("green apple");
        search("banana");
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteAccountIfExists(USER);
    }

    private void search(String query) throws Exception {
        mbox.search(new ZSearchParams(query));
        Thread.sleep(1000); //search history currently doesn't have sub-second resolution
    }

    private void checkResults(String[] expected, List<String> actual) throws Exception {
        assertEquals("incorrect search history", Arrays.asList(expected), actual);
    }

    @Test
    public void testGetSearchHistory() throws Exception {
        List<String> results = mbox.getSearchHistory();
        String[] expected = {"banana", "green apple", "applesauce", "apple"};
        checkResults(expected, results);

        results = mbox.getSearchHistory(2);
        String[] limited = {"banana", "green apple"};
        checkResults(limited, results);
    }

    @Test
    public void testSearchSuggest() throws Exception {
        List<String> results = mbox.getSearchSuggestions("a");
        //It is hard to predict the exact relevance order of search results given the nature of the query issued to the index.
        //However, we know that "apple" should be first.
        assertEquals("should see 3 results", 3, results.size());
        assertEquals("'apple' should be the first result", "apple", results.get(0));
        assertTrue("'applesauce' should be in the result set", results.contains("applesauce"));
        assertTrue("'green apple' should be in the result set", results.contains("green apple"));

        results = mbox.getSearchSuggestions("a", 2);
        assertEquals("should see 2 results", 2, results.size());
        assertEquals("'apple' should be the first result", "apple", results.get(0));
    }

    @Test
    public void testSearchSuggestEmptyPrefix() throws Exception {
        //issuing SearchSuggestRequest with no query is identical to GetSearchHistoryRequest
        List<String> results = mbox.getSearchSuggestions("");
        String[] expected = {"banana", "green apple", "applesauce", "apple"};
        checkResults(expected, results);
    }
}
