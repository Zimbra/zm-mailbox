/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchParams.Cursor;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.util.ProvisioningUtil;
import com.zimbra.soap.type.SearchSortBy;
/**
 * @author Greg Solovyev
 * These tests are related to bug #89152
 */
@Ignore ("these tests take long time to run, so run them separately")
public class TestSearchSortByDate {
    private static final String NAME_PREFIX = TestSearchSortByDate.class.getSimpleName();
    private static final String ADDRESS1 = NAME_PREFIX + "user1";
    private static final String ADDRESS2 = NAME_PREFIX +  "user2";
    private static final String SENDER = "user1";
    private static final String SEARCH_STRING = "jkfheowerklsaklasdqweds";
    private boolean originalLCSetting = false;
    private static final String JP_SEARCH_STRING = "\u5800\uu6c5f"; //"\u5800\uu6c5f"; //"&#22528;&#27743";
    private static final String JP_TEST_MESSAGE_FILE = "/opt/zimbra/unittest/email/bug_89152.eml";

    @Before
    public void setUp() throws Exception {
        cleanUp();
        originalLCSetting = ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexManualCommit, true);
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
        TestUtil.createAccount(ADDRESS1);
        TestUtil.createAccount(ADDRESS2);    }

    @Test
    public void testJPMessagesWith1SecInterval()  {
        try {
            int numMessages = 2207;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            Mailbox recieverMbox = TestUtil.getMailbox(ADDRESS1);
            long timestamp = System.currentTimeMillis() - 3600*60*1000;
            byte[] msgBytes = Files.readAllBytes(FileSystems.getDefault().getPath(JP_TEST_MESSAGE_FILE));
            for(int i=0;i<numMessages;i++) {
                ParsedMessage pm = new ParsedMessage(msgBytes,timestamp, true);
                TestUtil.addMessage(recieverMbox, pm);
                timestamp +=1000;
                Thread.sleep(100);
            }
            ZMailbox zmbx = TestUtil.getZMailbox(ADDRESS1);
            ZSearchParams searchParams = new ZSearchParams("in:inbox " + JP_SEARCH_STRING);
            searchParams.setSortBy(SearchSortBy.dateDesc);
            searchParams.setLimit(firstLimit);
            searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
            List<ZMessage> resultSet = TestUtil.search(zmbx, searchParams);
            assertEquals(firstLimit,resultSet.size());
            int gotMessages = resultSet.size();
            List<String> seenIds = new ArrayList<String>();
            ZMessage lastHit = null;
            for(ZMessage m : resultSet) {
                seenIds.add(m.getId());
                lastHit = m;
            }
            int recCount = 1;
            offset+=firstLimit;
            while(gotMessages > 0) {
                searchParams = new ZSearchParams("in:inbox " + JP_SEARCH_STRING);
                searchParams.setCursor((new Cursor(lastHit.getId(), Long.toString(lastHit.getReceivedDate())) ));
                searchParams.setSortBy(SearchSortBy.dateDesc);
                searchParams.setLimit(incLimit);
                searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
                searchParams.setOffset(offset);
                resultSet = TestUtil.search(zmbx, searchParams);
                recCount++;
                gotMessages = resultSet.size();
                int resCount = 0;
                for(ZMessage m : resultSet) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, m.getId(), seenIds.indexOf(m.getId())), seenIds.contains(m.getId()));
                    seenIds.add(m.getId());
                    resCount++;
                    lastHit = m;
                }
                offset+=incLimit;
                Thread.sleep(100);
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
    public void testJPMessagesWith1msInterval()  {
        try {
            int numMessages = 5207;
            int firstLimit = 150;
            int incLimit = 50;
            int offset = 0;
            Mailbox recieverMbox = TestUtil.getMailbox(ADDRESS1);
            long timestamp = System.currentTimeMillis();
            byte[] msgBytes = Files.readAllBytes(FileSystems.getDefault().getPath(JP_TEST_MESSAGE_FILE));
            for(int i=0;i<numMessages;i++) {
                ParsedMessage pm = new ParsedMessage(msgBytes,timestamp, true);
                pm.getMimeMessage().setSentDate(new Date());
                TestUtil.addMessage(recieverMbox, pm);
                timestamp+=1;
                Thread.sleep(100);
            }
            ZMailbox zmbx = TestUtil.getZMailbox(ADDRESS1);
            ZSearchParams searchParams = new ZSearchParams("in:inbox " + JP_SEARCH_STRING);
            searchParams.setSortBy(SearchSortBy.dateDesc);
            searchParams.setLimit(firstLimit);
            searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
            List<ZMessage> resultSet = TestUtil.search(zmbx, searchParams);
            assertEquals(firstLimit,resultSet.size());
            int gotMessages = resultSet.size();
            List<String> seenIds = new ArrayList<String>();
            ZMessage lastHit = null;
            for(ZMessage m : resultSet) {
                seenIds.add(m.getId());
                lastHit = m;
            }
            int recCount = 1;
            offset+=firstLimit;
            while(gotMessages > 0) {
                searchParams = new ZSearchParams("in:inbox " + JP_SEARCH_STRING);
                searchParams.setCursor((new Cursor(lastHit.getId(), Long.toString(lastHit.getReceivedDate())) ));
                searchParams.setSortBy(SearchSortBy.dateDesc);
                searchParams.setLimit(incLimit);
                searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
                searchParams.setOffset(offset);
                resultSet = TestUtil.search(zmbx, searchParams);
                recCount++;
                gotMessages = resultSet.size();
                int resCount = 0;
                for(ZMessage m : resultSet) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, m.getId(), seenIds.indexOf(m.getId())), seenIds.contains(m.getId()));
                    seenIds.add(m.getId());
                    resCount++;
                    lastHit = m;
                }
                offset+=incLimit;
                Thread.sleep(100);
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
    public void testENMessagesWith1SecIntervalSortDateAsc()  {
        try {
            Mailbox recieverMbox = TestUtil.getMailbox(ADDRESS1);
            long timestamp = System.currentTimeMillis() - 3600*60*1000;
            int numMessages = 3309;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            ArrayList<String> expectedIds = new ArrayList<String>();
            for(int i=0;i<numMessages;i++) {
                Message msg = TestUtil.addMessage(recieverMbox, TestUtil.getAddress(ADDRESS1), TestUtil.getAddress(SENDER), NAME_PREFIX + " testing bug " + i, String.format("this message contains a search string %s which we are searching for and a number %d and timestamp %d", SEARCH_STRING, i,timestamp), timestamp);
                expectedIds.add(Integer.toString(msg.getId()));
                timestamp +=1000;
                Thread.sleep(100);
            }
            ZMailbox zmbx = TestUtil.getZMailbox(ADDRESS1);
            ZSearchParams searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
            searchParams.setSortBy(SearchSortBy.dateAsc);
            searchParams.setLimit(firstLimit);
            searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
            List<ZMessage> resultSet = TestUtil.search(zmbx, searchParams);
            assertEquals(firstLimit,resultSet.size());
            int gotMessages = resultSet.size();
            List<String> seenIds = new ArrayList<String>();
            ZMessage lastHit = null;
            for(ZMessage m : resultSet) {
                seenIds.add(m.getId());
                lastHit = m;
            }
            int recCount = 1;
            offset+=firstLimit;
            while(gotMessages > 0) {
                searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
                searchParams.setCursor((new Cursor(lastHit.getId(), Long.toString(lastHit.getReceivedDate())) ));
                searchParams.setSortBy(SearchSortBy.dateAsc);
                searchParams.setLimit(incLimit);
                searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
                searchParams.setOffset(offset);
                resultSet = TestUtil.search(zmbx, searchParams);
                recCount++;
                gotMessages = resultSet.size();
                int resCount = 0;
                for(ZMessage msg : resultSet) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, msg.getId(), seenIds.indexOf(msg.getId())), seenIds.contains(msg.getId()));
                    seenIds.add(msg.getId());
                    resCount++;
                    lastHit = msg;
                }
                offset+=incLimit;
                Thread.sleep(100);
            }
            for(int i=0;i<expectedIds.size();i++) {
               assertEquals("IDs at index " + i + " do not match", expectedIds.get(i),seenIds.get(i) );
            }
            assertEquals("Returned incorrect number of messages", numMessages, seenIds.size());
        } catch (ServiceException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testENMessagesWith1msIntervalSortDateDesc()  {
        try {
            Mailbox recieverMbox = TestUtil.getMailbox(ADDRESS2);
            long timestamp = System.currentTimeMillis();// - 3600*60*1000;
            int numMessages = 4307;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            ArrayList<String> expectedIds = new ArrayList<String>();
            for(int i=0;i<numMessages;i++) {
                Message msg = TestUtil.addMessage(recieverMbox, TestUtil.getAddress(ADDRESS2), TestUtil.getAddress(SENDER), NAME_PREFIX + " testing bug " + i, String.format("this message contains a search string %s which we are searching for and a number %d and timestamp %d", SEARCH_STRING, i,timestamp), timestamp);
                expectedIds.add(Integer.toString(msg.getId()));
                timestamp +=1;
                Thread.sleep(100);
            }

            for(int i=0;i<numMessages;i++) {
                TestUtil.addMessage(recieverMbox, TestUtil.getAddress(ADDRESS1), TestUtil.getAddress(SENDER), NAME_PREFIX + " testing bug " + i, "this message does not contains a search string which we are searching for", timestamp);
            }
            Collections.reverse(expectedIds);
            ZMailbox zmbx = TestUtil.getZMailbox(ADDRESS2);
            ZSearchParams searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
            searchParams.setSortBy(SearchSortBy.dateDesc);
            searchParams.setLimit(firstLimit);
            searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
            List<ZMessage> resultSet = TestUtil.search(zmbx, searchParams);
            assertEquals(firstLimit,resultSet.size());
            int gotMessages = resultSet.size();
            List<String> seenIds = new ArrayList<String>();
            ZMessage lastHit = null;
            for(ZMessage m : resultSet) {
                seenIds.add(m.getId());
                lastHit = m;
            }
            int recCount = 1;
            offset+=firstLimit;
            while(gotMessages > 0) {
                searchParams = new ZSearchParams("in:inbox " + SEARCH_STRING);
                searchParams.setCursor((new Cursor(lastHit.getId(), Long.toString(lastHit.getReceivedDate())) ));
                searchParams.setSortBy(SearchSortBy.dateDesc);
                searchParams.setLimit(incLimit);
                searchParams.setTypes(ZSearchParams.TYPE_MESSAGE);
                searchParams.setOffset(offset);
                resultSet = TestUtil.search(zmbx, searchParams);
                recCount++;
                gotMessages = resultSet.size();
                int resCount = 0;
                for(ZMessage msg : resultSet) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, msg.getId(), seenIds.indexOf(msg.getId())), seenIds.contains(msg.getId()));
                    seenIds.add(msg.getId());
                    resCount++;
                    lastHit = msg;
                }
                offset+=incLimit;
                Thread.sleep(100);
            }
            assertEquals("Returned incorrect number of messages", numMessages, seenIds.size());
        } catch (ServiceException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testConversationsWith1SecInterval()  {
        try {
            Mailbox recieverMbox = TestUtil.getMailbox(ADDRESS1);
            long timestamp = System.currentTimeMillis() - 3600*60*1000;
            int numMessages = 3207;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            ArrayList<String> expectedIds = new ArrayList<String>();
            for(int i=0;i<numMessages;i++) {
                Message msg = TestUtil.addMessage(recieverMbox, TestUtil.getAddress(ADDRESS1), TestUtil.getAddress(SENDER), NAME_PREFIX + " testing bug " + i, String.format("this message contains a search string %s which we are searching for and a number %d and timestamp %d", SEARCH_STRING, i,timestamp), timestamp);
                expectedIds.add(Integer.toString(msg.getId()));
                timestamp +=1000;
                Thread.sleep(100);
            }
            Collections.reverse(expectedIds);
            ZMailbox zmbx = TestUtil.getZMailbox(ADDRESS1);
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
                offset+=incLimit;
                Thread.sleep(100);
            }
            assertEquals("Returned incorrect number of conversations", numMessages, seenIds.size());
        } catch (ServiceException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testConversationsWith1msInterval()  {
        try {
            Mailbox recieverMbox = TestUtil.getMailbox(ADDRESS2);
            long timestamp = System.currentTimeMillis() - 3600*60*1000;
            int numMessages = 3207;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            ArrayList<String> expectedIds = new ArrayList<String>();
            for(int i=0;i<numMessages;i++) {
                Message msg = TestUtil.addMessage(recieverMbox, TestUtil.getAddress(ADDRESS2), TestUtil.getAddress(SENDER), NAME_PREFIX + " testing bug " + i, String.format("this message contains a search string %s which we are searching for and a number %d and timestamp %d", SEARCH_STRING, i,timestamp), timestamp);
                expectedIds.add(Integer.toString(msg.getId()));
                timestamp +=1;
                Thread.sleep(100);
            }
            Collections.reverse(expectedIds);
            ZMailbox zmbx = TestUtil.getZMailbox(ADDRESS2);
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
                offset+=incLimit;
                Thread.sleep(100);
            }
            assertEquals("Returned incorrect number of conversations", numMessages, seenIds.size());
        } catch (ServiceException e) {
            e.printStackTrace();
            fail(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @After
    public void tearDown()
    throws Exception {
        cleanUp();
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(originalLCSetting);
    }

    private void cleanUp()
    throws Exception {
        if(TestUtil.accountExists(ADDRESS1)) {
            TestUtil.deleteAccount(ADDRESS1);
        }
        if(TestUtil.accountExists(ADDRESS2)) {
            TestUtil.deleteAccount(ADDRESS2);
        }
    }

}
