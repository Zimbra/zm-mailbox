package com.zimbra.qa.unittest;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZSearchParams;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.soap.type.SearchSortBy;

public class TestBug89152 extends TestCase {
    private static final String RECIPIENT1 = "search_test1_89152";
    private static final String RECIPIENT2 = "search_test2_89152";
    private static final String PASSWORD = "test123";
    private static final String SEARCH_STRING = "堀江";
    private static final String TEST_MESSAGE_FILE = "/Users/gsolovyev/p4/main/ZimbraQA/data/TestMailRaw/bug89152/testmessage.eml";

    @Override
    public void setUp() throws Exception {
        cleanUp();
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("zimbraMailHost", LC.zimbra_server_hostname.value());
        attrs.put("zimbraAttachmentsIndexingEnabled", true);
        attrs.put("cn", "search_test1");
        attrs.put("displayName", "TestAccount unit test user 1");
        Provisioning.getInstance().createAccount(TestUtil.getAddress(RECIPIENT1), PASSWORD, attrs);


        attrs = new HashMap<String, Object>();
        attrs.put("zimbraMailHost", LC.zimbra_server_hostname.value());
        attrs.put("zimbraAttachmentsIndexingEnabled", false);
        attrs.put("cn", "search_test2");
        attrs.put("displayName", "TestAccount unit test user 2");
        Provisioning.getInstance().createAccount(TestUtil.getAddress(RECIPIENT2), PASSWORD, attrs);
    }

    @Test
    public void testConversations()  {
        try {
            int numMessages = 2217;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT1);
            long timestamp = System.currentTimeMillis() - 3600*60*1000;
            byte[] msgBytes = Files.readAllBytes(FileSystems.getDefault().getPath(TEST_MESSAGE_FILE));
            for(int i=0;i<numMessages;i++) {
                ParsedMessage pm = new ParsedMessage(msgBytes,timestamp, true);
                Message msg = TestUtil.addMessage(recieverMbox, pm);
                timestamp +=1000;
            }
            //Thread.sleep(1000);
            ZMailbox zmbx = TestUtil.getZMailbox(RECIPIENT1);
            ZSearchParams searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
            searchParams.setSortBy(SearchSortBy.dateDesc);
            searchParams.setLimit(firstLimit);
            searchParams.setTypes(ZSearchParams.TYPE_CONVERSATION);
            List<String> msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
            assertEquals(firstLimit,msgIds.size());
            int gotMessages = msgIds.size();
            List<String> seenIds = new ArrayList<String>();
            seenIds.addAll(msgIds);
            int recCount = 1;
            offset+=firstLimit;
       //     int numDups = 0;
            while(gotMessages > 0) {
                searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
                searchParams.setSortBy(SearchSortBy.dateDesc);
                searchParams.setLimit(incLimit);
                searchParams.setTypes(ZSearchParams.TYPE_CONVERSATION);
                searchParams.setOffset(offset);
                msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
                recCount++;
                gotMessages = msgIds.size();
                int resCount = 0;
                for(String szId : msgIds) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, szId, seenIds.indexOf(szId)), seenIds.contains(szId));
                    seenIds.add(szId);
                    resCount++;
                }
                //Thread.sleep(1000);
                offset+=incLimit;
            }
            assertEquals("expecting " + numMessages + " messages. Got " + seenIds.size(), numMessages, seenIds.size());
     //       assertEquals("expecting 0 dups. Got " + numDups, 0,numDups);
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail(e.getMessage());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testConversations2()  {
        try {
            int numMessages = 2207;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT2);
            long timestamp = System.currentTimeMillis() - 3600*60*1000;
            byte[] msgBytes = Files.readAllBytes(FileSystems.getDefault().getPath(TEST_MESSAGE_FILE));
            for(int i=0;i<numMessages;i++) {
                ParsedMessage pm = new ParsedMessage(msgBytes,timestamp, false);
                Message msg = TestUtil.addMessage(recieverMbox, pm);
                timestamp +=1000;
            }
            //Thread.sleep(1000);
            ZMailbox zmbx = TestUtil.getZMailbox(RECIPIENT2);
            ZSearchParams searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
            searchParams.setSortBy(SearchSortBy.dateDesc);
            searchParams.setLimit(firstLimit);
            searchParams.setTypes(ZSearchParams.TYPE_CONVERSATION);
            List<String> msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
            assertEquals(firstLimit,msgIds.size());
            int gotMessages = msgIds.size();
            List<String> seenIds = new ArrayList<String>();
            seenIds.addAll(msgIds);
            int recCount = 1;
            offset+=firstLimit;
       //     int numDups = 0;
            while(gotMessages > 0) {
                searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
                searchParams.setSortBy(SearchSortBy.dateDesc);
                searchParams.setLimit(incLimit);
                searchParams.setTypes(ZSearchParams.TYPE_CONVERSATION);
                searchParams.setOffset(offset);
                msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
                recCount++;
                gotMessages = msgIds.size();
                int resCount = 0;
                for(String szId : msgIds) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, szId, seenIds.indexOf(szId)), seenIds.contains(szId));
                    seenIds.add(szId);
                    resCount++;
                }
                //Thread.sleep(1000);
                offset+=incLimit;
            }
            assertEquals("expecting " + numMessages + " messages. Got " + seenIds.size(), numMessages, seenIds.size());
        } catch (ServiceException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testMessages()  {
        try {
            int numMessages = 3206;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT1);
            long timestamp = System.currentTimeMillis() - 3600*60*1000;
            ArrayList<String> expectedIds = new ArrayList<String>();
            byte[] msgBytes = Files.readAllBytes(FileSystems.getDefault().getPath(TEST_MESSAGE_FILE));
            for(int i=0;i<numMessages;i++) {
                ParsedMessage pm = new ParsedMessage(msgBytes,timestamp, true);
                Message msg = TestUtil.addMessage(recieverMbox, pm);
                expectedIds.add(Integer.toString(msg.getId()));
                timestamp +=1001;
            }
            Collections.reverse(expectedIds);
            //Thread.sleep(1000);
            ZMailbox zmbx = TestUtil.getZMailbox(RECIPIENT1);
            ZSearchParams searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
            searchParams.setSortBy(SearchSortBy.dateDesc);
            searchParams.setLimit(firstLimit);
            searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
            List<String> msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
            assertEquals(firstLimit,msgIds.size());
            int gotMessages = msgIds.size();
            List<String> seenIds = new ArrayList<String>();
            seenIds.addAll(msgIds);
            int recCount = 1;
            offset+=firstLimit;
            while(gotMessages > 0) {
                searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
                searchParams.setSortBy(SearchSortBy.dateDesc);
                searchParams.setLimit(incLimit);
                searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
                searchParams.setOffset(offset);
                msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
                recCount++;
                gotMessages = msgIds.size();
                int resCount = 0;
                for(String szId : msgIds) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, szId, seenIds.indexOf(szId)), seenIds.contains(szId));
                    seenIds.add(szId);
                    resCount++;
                }
                //Thread.sleep(1000);
                offset+=incLimit;
            }
            for(int i=0;i<expectedIds.size();i++) {
               assertEquals("IDs at index " + i + " do not match", expectedIds.get(i),seenIds.get(i) );
            }
            assertEquals("expecting " + numMessages + " messages. Got " + seenIds.size(), numMessages, seenIds.size());
        } catch (ServiceException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testMessages2()  {
        try {
            int numMessages = 3000;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT2);
            long timestamp = System.currentTimeMillis() - 3600*60*1000;
            ArrayList<String> expectedIds = new ArrayList<String>();
            byte[] msgBytes = Files.readAllBytes(FileSystems.getDefault().getPath(TEST_MESSAGE_FILE));
            for(int i=0;i<numMessages;i++) {
                ParsedMessage pm = new ParsedMessage(msgBytes,timestamp, false);
                Message msg = TestUtil.addMessage(recieverMbox, pm);
                expectedIds.add(Integer.toString(msg.getId()));
                timestamp +=1001;
            }
            Collections.reverse(expectedIds);
            //Thread.sleep(1000);
            ZMailbox zmbx = TestUtil.getZMailbox(RECIPIENT2);
            ZSearchParams searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
            searchParams.setSortBy(SearchSortBy.dateDesc);
            searchParams.setLimit(firstLimit);
            searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
            List<String> msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
            assertEquals(firstLimit,msgIds.size());
            int gotMessages = msgIds.size();
            List<String> seenIds = new ArrayList<String>();
            seenIds.addAll(msgIds);
            int recCount = 1;
            offset+=firstLimit;
       //     int numDups = 0;
            while(gotMessages > 0) {
                searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
                searchParams.setSortBy(SearchSortBy.dateDesc);
                searchParams.setLimit(incLimit);
                searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
                searchParams.setOffset(offset);
                msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
                recCount++;
                gotMessages = msgIds.size();
                int resCount = 0;
                for(String szId : msgIds) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, szId, seenIds.indexOf(szId)), seenIds.contains(szId));
                    seenIds.add(szId);
                    resCount++;
                }
                //Thread.sleep(1000);
                offset+=incLimit;
            }
            for(int i=0;i<expectedIds.size();i++) {
               assertEquals("IDs at index " + i + " do not match", expectedIds.get(i),seenIds.get(i) );
            }
            assertEquals("expecting " + numMessages + " messages. Got " + seenIds.size(), numMessages, seenIds.size());
        } catch (ServiceException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testMessagesCloseTimestamps()  {
        try {
            int numMessages = 5500;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT1);
            long timestamp = System.currentTimeMillis();
            ArrayList<String> expectedIds = new ArrayList<String>();
            byte[] msgBytes = Files.readAllBytes(FileSystems.getDefault().getPath(TEST_MESSAGE_FILE));
            for(int i=0;i<numMessages;i++) {
                ParsedMessage pm = new ParsedMessage(msgBytes,timestamp, true);
                Message msg = TestUtil.addMessage(recieverMbox, pm);
                expectedIds.add(Integer.toString(msg.getId()));
                timestamp+=1;
            }
            Collections.reverse(expectedIds);
            //Thread.sleep(1000);
            ZMailbox zmbx = TestUtil.getZMailbox(RECIPIENT1);
            ZSearchParams searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
            searchParams.setSortBy(SearchSortBy.dateDesc);
            searchParams.setLimit(firstLimit);
            searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
            List<String> msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
            assertEquals(firstLimit,msgIds.size());
            int gotMessages = msgIds.size();
            List<String> seenIds = new ArrayList<String>();
            seenIds.addAll(msgIds);
            int recCount = 1;
            offset+=firstLimit;
       //     int numDups = 0;
            while(gotMessages > 0) {
                searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
                searchParams.setSortBy(SearchSortBy.dateDesc);
                searchParams.setLimit(incLimit);
                searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
                searchParams.setOffset(offset);
                msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
                recCount++;
                gotMessages = msgIds.size();
                int resCount = 0;
                for(String szId : msgIds) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, szId, seenIds.indexOf(szId)), seenIds.contains(szId));
                    seenIds.add(szId);
                    resCount++;
                }
                //Thread.sleep(1000);
                offset+=incLimit;
            }
          /*  for(int i=0;i<expectedIds.size();i++) {
               assertEquals("IDs at index " + i + " do not match", expectedIds.get(i),seenIds.get(i) );
            }*/
            assertEquals("expecting " + numMessages + " messages. Got " + seenIds.size(), numMessages, seenIds.size());
     //       assertEquals("expecting 0 dups. Got " + numDups, 0,numDups);
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail(e.getMessage());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


    @Test
    public void testMessagesCloseTimestamps2()  {
        try {
            int numMessages = 3607;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT2);
            long timestamp = System.currentTimeMillis();
            ArrayList<String> expectedIds = new ArrayList<String>();
            byte[] msgBytes = Files.readAllBytes(FileSystems.getDefault().getPath(TEST_MESSAGE_FILE));
            for(int i=0;i<numMessages;i++) {
                ParsedMessage pm = new ParsedMessage(msgBytes,timestamp, false);
                Message msg = TestUtil.addMessage(recieverMbox, pm);
                expectedIds.add(Integer.toString(msg.getId()));
                timestamp+=1;
            }
            Collections.reverse(expectedIds);
            //Thread.sleep(1000);
            ZMailbox zmbx = TestUtil.getZMailbox(RECIPIENT2);
            ZSearchParams searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
            searchParams.setSortBy(SearchSortBy.dateDesc);
            searchParams.setLimit(firstLimit);
            searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
            List<String> msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
            assertEquals(firstLimit,msgIds.size());
            int gotMessages = msgIds.size();
            List<String> seenIds = new ArrayList<String>();
            seenIds.addAll(msgIds);
            int recCount = 1;
            offset+=firstLimit;
       //     int numDups = 0;
            while(gotMessages > 0) {
                searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
                searchParams.setSortBy(SearchSortBy.dateDesc);
                searchParams.setLimit(incLimit);
                searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
                searchParams.setOffset(offset);
                msgIds = TestUtil.searchMessageIds(zmbx, searchParams);
                recCount++;
                gotMessages = msgIds.size();
                int resCount = 0;
                for(String szId : msgIds) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, szId, seenIds.indexOf(szId)), seenIds.contains(szId));
                    seenIds.add(szId);
                    resCount++;
                }
                //Thread.sleep(1000);
                offset+=incLimit;
            }
            /*for(int i=0;i<expectedIds.size();i++) {
               assertEquals("IDs at index " + i + " do not match", expectedIds.get(i),seenIds.get(i) );
            }*/
            assertEquals("expecting " + numMessages + " messages. Got " + seenIds.size(), numMessages, seenIds.size());
     //       assertEquals("expecting 0 dups. Got " + numDups, 0,numDups);
        } catch (ServiceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail(e.getMessage());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail(e.getMessage());
        }
    }


    @Override
    public void tearDown()
    throws Exception {
        cleanUp();
    }
    private void cleanUp()
    throws Exception {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, TestUtil.getAddress(RECIPIENT1));
        if (account != null) {
            prov.deleteAccount(account.getId());
        }

        account = prov.get(AccountBy.name, TestUtil.getAddress(RECIPIENT2));
        if (account != null) {
            prov.deleteAccount(account.getId());
        }
    }

}
