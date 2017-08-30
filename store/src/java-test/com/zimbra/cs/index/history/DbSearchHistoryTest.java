package com.zimbra.cs.index.history;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbSearchHistory;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.db.HSQLDB;
import com.zimbra.cs.index.history.SavedSearchPromptLog.SavedSearchStatus;
import com.zimbra.cs.index.history.ZimbraSearchHistory.SearchHistoryMetadataParams;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

public class DbSearchHistoryTest {

    private DbSearchHistory db;
    private Provisioning prov;
    private Account acct;
    private Mailbox mbox;
    private DbConnection conn;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();

        prov = Provisioning.getInstance();
        acct = prov.createAccount("dbSearchHistoryIndexTest@zimbra.com", "test123", new HashMap<String, Object>());
        mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        db = new DbSearchHistory(mbox);
        conn = DbPool.getConnection(mbox);

    }

    @After
    public void tearDown() throws Exception {
        prov.deleteAccount(acct.getId());
        HSQLDB.clearDatabase();
        conn.closeQuietly();
    }


    private void createSearches(int howMany) throws ServiceException {
        for (int i=1; i<=howMany; i++) {
            db.createNewSearch(conn, i, String.format("search%d", i));
            conn.commit();
        }
    }

    private void logSearch(String searchString, long timestampMillis) throws ServiceException {
        db.logSearch(conn, searchString, timestampMillis);
        conn.commit();
    }

    public void testCreateNewSearch() throws Exception {
        createSearches(2);
        DbResults rs = DbUtil.executeQuery(conn, "SELECT id, search, status FROM mboxgroup1.searches WHERE mailbox_id = ?", mbox.getId());
        assertEquals("should see 2 rows in searches table", 2, rs.size());
        rs.next();
        assertEquals("first row should have id=1", 1, rs.getInt(1));
        assertEquals("first row should have value 'search1'", "search1", rs.getString(2));
        rs.next();
        assertEquals("second row should have id=2", 2, rs.getInt(1));
        assertEquals("second row should have value 'search2'", "search2", rs.getString(2));
    }

    @Test
    public void testLogSearch() throws Exception {
        createSearches(2);
        logSearch("search1", 1000);
        logSearch("search2", 2000);
        logSearch("search1", 3000);
        try {
            logSearch("search3", 4000);
            fail("should not be able to log search entry before initializing it");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("ID of search string"));
        }

        DbResults rs = DbUtil.executeQuery(conn, "SELECT search_id, date FROM mboxgroup1.search_history WHERE mailbox_id = ?"
                + "ORDER BY date", mbox.getId());
        assertEquals("should see 3 rows in search_history table", 3, rs.size());
        rs.next();
        assertEquals("first row should have id=1", 1, rs.getInt(1));
        rs.next();
        assertEquals("second row should have id=2", 2, rs.getInt(1));
        rs.next();
        assertEquals("third row should have id=1", 1, rs.getInt(1));

        //check last search timestamps
        rs = DbUtil.executeQuery(conn, "SELECT last_search_date FROM mboxgroup1.searches WHERE mailbox_id = ?"
                + "ORDER BY id", mbox.getId());
        rs.next();
        assertEquals("last timestamp for id=1 should be 300000", 3000, ((Timestamp) rs.getObject(1)).getTime());
        rs.next();
        assertEquals("last timestamp for id=2 should be 200000", 2000, ((Timestamp) rs.getObject(1)).getTime());
    }

    @Test
    public void testGetCount() throws Exception {
        createSearches(2);

        long now = System.currentTimeMillis();

        logSearch("search1", now - 30000);
        logSearch("search2", now - 20000);
        logSearch("search1", now - 10000);

        assertEquals("should find 2 occurrences of 'search1'", 2, db.getCount(conn, "search1", 0));
        assertEquals("should find 1 occurrence of 'search1' with 15sec cutoff", 1, db.getCount(conn, "search1", 15000));
        assertEquals("should find 1 occurrence of 'search1' with 5sec cutoff", 0, db.getCount(conn, "search1", 5000));

        assertEquals("should find 1 occurrences of 'search2'", 1, db.getCount(conn, "search2", 0));
        assertEquals("should find 0 occurrence of 'search2' with 10sec cutoff", 0, db.getCount(conn, "search2", 10000));
    }

    @Test
    public void testIsRegistered() throws Exception {
        assertFalse("search1 should not be registered", db.isRegistered(conn, "search1"));
        createSearches(2);
        assertTrue("search1 should be registered", db.isRegistered(conn, "search1"));
        assertTrue("search2 should be registered", db.isRegistered(conn, "search2"));
    }

    @Test
    public void testSearch() throws Exception {
        createSearches(5);

        long now = System.currentTimeMillis();
        logSearch("search5", now - 6000);
        logSearch("search1", now - 5000);
        logSearch("search2", now - 4000);
        logSearch("search3", now - 3000);
        logSearch("search4", now - 2000);
        logSearch("search5", now - 1000);

        SearchHistoryMetadataParams params = new SearchHistoryMetadataParams();
        List<String> results = db.search(conn, params);
        assertEquals("should see 5 results", 5, results.size());
        assertEquals("result 1 should be search5", "search5", results.get(0));
        assertEquals("result 2 should be search4", "search4", results.get(1));
        assertEquals("result 3 should be search3", "search3", results.get(2));
        assertEquals("result 4 should be search2", "search2", results.get(3));
        assertEquals("result 5 should be search1", "search1", results.get(4));

        //test age cutoff
        params.setMaxAge(3500);
        results = db.search(conn, params);
        assertEquals("should see 3 results", 3, results.size());
        assertEquals("result 1 should be search5", "search5", results.get(0));
        assertEquals("result 2 should be search4", "search4", results.get(1));
        assertEquals("result 3 should be search3", "search3", results.get(2));

        //test limit cutoff
        params = new SearchHistoryMetadataParams(2);
        results = db.search(conn, params);
        assertEquals("should see 2 results", 2, results.size());
        assertEquals("result 1 should be search5", "search5", results.get(0));
        assertEquals("result 2 should be search4", "search4", results.get(1));

        //test age and limit cutoff
        params.setMaxAge(1500);
        results = db.search(conn, params);
        assertEquals("should see 1 result", 1, results.size());
        assertEquals("result 1 should be search5", "search5", results.get(0));

        //test id constraints
        params = new SearchHistoryMetadataParams();
        params.setIds(Arrays.asList(1, 3, 5));
        results = db.search(conn, params);
        assertEquals("should see 3 results", 3, results.size());
        assertEquals("result 1 should be search5", "search5", results.get(0));
        assertEquals("result 3 should be search3", "search3", results.get(1));
        assertEquals("result 5 should be search1", "search1", results.get(2));

        //test id constraints with age cutoff
        params.setMaxAge(3500);
        results = db.search(conn, params);
        assertEquals("should see 2 results", 2, results.size());
        assertEquals("result 1 should be search5", "search5", results.get(0));
        assertEquals("result 3 should be search3", "search3", results.get(1));

        //test id constraints with limit cutoff
        params.setMaxAge(-1);
        params.setNumResults(2);
        results = db.search(conn, params);
        assertEquals("should see 2 results", 2, results.size());
        assertEquals("result 1 should be search5", "search5", results.get(0));
        assertEquals("result 3 should be search3", "search3", results.get(1));

        //test id constraints with age and limit cuttoffs
        params.setMaxAge(3500);
        params.setNumResults(1);
        results = db.search(conn, params);
        assertEquals("should see 1 results", 1, results.size());
        assertEquals("result 1 should be search5", "search5", results.get(0));
    }

    @Test
    public void testDeleteByAge() throws Exception {
        createSearches(3);

        long now = System.currentTimeMillis();

        logSearch("search1", now - 6000);
        logSearch("search2", now - 5000);
        logSearch("search3", now - 4000);
        logSearch("search1", now - 3000);
        logSearch("search2", now - 2000);
        logSearch("search3", now - 1000);

        //sanity check: make sure the counts are correct
        assertEquals("search1 should have 2 occurrences", 2, db.getCount(conn, "search1", 0));
        assertEquals("search2 should have 2 occurrences", 2, db.getCount(conn, "search2", 0));
        assertEquals("search3 should have 2 occurrences", 2, db.getCount(conn, "search3", 0));

        Collection<Integer> deleted = db.delete(conn, 3500);
        conn.commit();

        assertTrue("no IDs should be deleted", deleted.isEmpty());
        DbResults count = DbUtil.executeQuery(conn, "select count(*) from mboxgroup1.search_history where mailbox_id = ?", mbox.getId());
        assertEquals("search_history table should have 3 results left", 3, count.getInt(1));
        count = DbUtil.executeQuery(conn, "select count(*) from mboxgroup1.searches where mailbox_id = ?", mbox.getId());
        assertEquals("searches table should still have 3 entries", 3, count.getInt(1));

        assertEquals("search1 should have 1 occurrence", 1, db.getCount(conn, "search1", 0));
        assertEquals("search2 should have 1 occurrence", 1, db.getCount(conn, "search2", 0));
        assertEquals("search3 should have 1 occurrence", 1, db.getCount(conn, "search3", 0));

        deleted = db.delete(conn, 1500);
        conn.commit();

        assertEquals("two IDs should be deleted", 2, deleted.size());
        assertTrue("IDs 1 and 2 should be deleted", deleted.contains(1) && deleted.contains(2));
        count = DbUtil.executeQuery(conn, "select count(*) from mboxgroup1.search_history where mailbox_id = ?", mbox.getId());
        assertEquals("search_history table should have 1 entry left", 1, count.getInt(1));
        count = DbUtil.executeQuery(conn, "select count(*) from mboxgroup1.searches where mailbox_id = ?", mbox.getId());
        assertEquals("searches table should have 1 entry left", 1, count.getInt(1));
    }

    @Test
    public void testDeleteHistory() throws Exception {
        createSearches(3);

        long now = System.currentTimeMillis();

        logSearch("search1", now - 6000);
        logSearch("search2", now - 5000);
        logSearch("search3", now - 4000);
        logSearch("search1", now - 3000);
        logSearch("search2", now - 2000);
        logSearch("search3", now - 1000);

        db.deleteAll(conn);
        conn.commit();

        DbResults count = DbUtil.executeQuery(conn, "select count(*) from mboxgroup1.search_history where mailbox_id = ?", mbox.getId());
        assertEquals("search_history table shouldn't have any entries for this mailbox", 0, count.getInt(1));
        count = DbUtil.executeQuery(conn, "select count(*) from mboxgroup1.searches where mailbox_id = ?", mbox.getId());
        assertEquals("searches table shouldn't have any entries for this mailbox", 0, count.getInt(1));

        SearchHistoryMetadataParams params = new SearchHistoryMetadataParams();
        List<String> results = db.search(conn, params);
        assertTrue("shouldn't see any results", results.isEmpty());
    }

    @Test
    public void testManyEntries() throws Exception {
        createSearches(1000);
        long now = System.currentTimeMillis();
        int searchesPerEntry = 10;
        for (int i = 1; i <= 1000; i++ ) {
            String search = String.format("search%d", i);
            for (int j = 1; j <= searchesPerEntry; j++) {
                logSearch(search, now - i * 1000);
            }
        }
        assertEquals("should see search count=10", 10, db.getCount(conn, "search1", 0));
        SearchHistoryMetadataParams params = new SearchHistoryMetadataParams();
        List<String> results = db.search(conn, params);
        assertEquals("should see 1000 results", 1000, results.size());

        List<Integer> ids = new ArrayList<Integer>();
        IntStream.rangeClosed(1, 500).forEach(ids::add);
        params.setIds(ids);
        results = db.search(conn, params);
        assertEquals("should see 500 results", 500, results.size());
    }

    @Test
    public void testSavedSearchStatus() throws Exception {
        SavedSearchStatus status = db.getSavedSearchStatus(conn, "search1");
        assertEquals("status for unregistered search should be NOT_PROMPTED", SavedSearchStatus.NOT_PROMPTED, status);
        createSearches(1);
        logSearch("search1", System.currentTimeMillis());
        status = db.getSavedSearchStatus(conn, "search1");
        assertEquals("default status for registered search should be NOT_PROMPTED", SavedSearchStatus.NOT_PROMPTED, status);

        for (SavedSearchStatus newStatus: SavedSearchStatus.values()) {
            db.setSavedSearchStatus(conn, "search1", newStatus);
            conn.commit();
            assertEquals(String.format("status should now be %s", newStatus), newStatus, db.getSavedSearchStatus(conn, "search1"));

        }
    }
}
