package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZFolder.Color;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchResult;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.type.SearchSortBy;

public class TestSavedSearchPrompt {

    private static String USER = "TestSearchHistory-test";
    private Account acct;
    private ZMailbox mbox;
    private int searchesForPrompt;

    @Before
    public void setUp() throws Exception {
        cleanUp();
        acct = TestUtil.createAccount(USER);
        mbox = TestUtil.getZMailbox(USER);
        searchesForPrompt = acct.getNumSearchesForSavedSearchPrompt();
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteAccountIfExists(USER);
    }
    private void searchAndTestPrompt(String query, boolean shouldHavePrompt) throws Exception {
        ZSearchResult result = mbox.search(new ZSearchParams(query));
        if (shouldHavePrompt) {
            assertTrue("search response should have saveSearchPrompt", result.hasSavedSearchPrompt());
        } else {
            assertFalse("search response should not have saveSearchPrompt", result.hasSavedSearchPrompt());
        }
    }

    private void doSearchesUpToPrompt() throws Exception {
        for (int i = 0; i < searchesForPrompt - 1; i++) {
            searchAndTestPrompt("testSearch", false);
        }
        searchAndTestPrompt("another search", false); //sanity check - throw in another search
        searchAndTestPrompt("testSearch", true);
    }

    @Test
    public void testSaveSearchPromptNoResponse() throws Exception {
        doSearchesUpToPrompt();
        //another search shouldn't return a prompt if there is one still outstanding
        searchAndTestPrompt("testSearch", false);
    }

    @Test
    public void testSaveSearchPromptRejected() throws Exception {
        doSearchesUpToPrompt();
        mbox.rejectSaveSearchFolderPrompt("testSearch");
        //if a prompt was rejected, further searches shouldn't prompt again
        searchAndTestPrompt("testSearch", false);
    }

    @Test
    public void testSaveSearchPromptAccepted() throws Exception {
        doSearchesUpToPrompt();
        //if a prompt was accepted, further searches shouldn't prompt again
        mbox.createSearchFolder(String.valueOf(Mailbox.ID_FOLDER_INBOX), "search", "testSearch", "message", SearchSortBy.dateAsc, Color.BLUE);
        searchAndTestPrompt("testSearch", false);

    }
}
