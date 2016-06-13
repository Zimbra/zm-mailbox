/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZSearchParams;
import com.zimbra.client.ZSearchParams.Cursor;
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
    private static final String RECIPIENT1 = "search_test";
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
    }


    @Test
    public void testMessagesWithDifferentTimestamps()  {
        try {
            int numMessages = 2207;
            int firstLimit = 100;
            int incLimit = 50;
            int offset = 0;
            Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT1);
            long timestamp = System.currentTimeMillis() - 3600*60*1000;
            byte[] msgBytes = Files.readAllBytes(FileSystems.getDefault().getPath(TEST_MESSAGE_FILE));
            for(int i=0;i<numMessages;i++) {
                ParsedMessage pm = new ParsedMessage(msgBytes,timestamp, true);
                Message msg = TestUtil.addMessage(recieverMbox, pm);
                timestamp +=1001;
                //Thread.sleep(300);
            }
            Thread.sleep(150);
            ZMailbox zmbx = TestUtil.getZMailbox(RECIPIENT1);
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
                for(ZMessage m : resultSet) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, m.getId(), seenIds.indexOf(m.getId())), seenIds.contains(m.getId()));
                    seenIds.add(m.getId());
                    resCount++;
                    lastHit = m;
                }
                Thread.sleep(300);
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
    public void testMessagesWithSimilarTimestamps()  {
        try {
            int numMessages = 5207;
            int firstLimit = 150;
            int incLimit = 50;
            int offset = 0;
            Mailbox recieverMbox = TestUtil.getMailbox(RECIPIENT1);
            long timestamp = System.currentTimeMillis();
            byte[] msgBytes = Files.readAllBytes(FileSystems.getDefault().getPath(TEST_MESSAGE_FILE));
            for(int i=0;i<numMessages;i++) {
                ParsedMessage pm = new ParsedMessage(msgBytes,timestamp, true);
                pm.getMimeMessage().setSentDate(new Date());
                Message msg = TestUtil.addMessage(recieverMbox, pm);
                timestamp+=1;
                //Thread.sleep(300);
            }
            Thread.sleep(150);
            ZMailbox zmbx = TestUtil.getZMailbox(RECIPIENT1);
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
                for(ZMessage m : resultSet) {
                    assertFalse(String.format("Request %d, result %d, encountered duplicate ID %s. Previously seen at %d", recCount, resCount, m.getId(), seenIds.indexOf(m.getId())), seenIds.contains(m.getId()));
                    seenIds.add(m.getId());
                    resCount++;
                    lastHit = m;
                }
                Thread.sleep(300);
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
    }

}
