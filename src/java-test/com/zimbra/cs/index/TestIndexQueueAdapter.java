package com.zimbra.cs.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.MockMailItem;

public class TestIndexQueueAdapter {

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());

    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testPutTake() throws Exception {
        //instantiate an adapter
        IndexingQueueAdapter adapter = DefaultIndexingQueueAdapter.getInstance();

        //publish a message
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        UnderlyingData underlyingData = new UnderlyingData();
        underlyingData.setSubject("test subject");
        underlyingData.folderId = Mailbox.ID_FOLDER_INBOX;
        underlyingData.name = "name";
        underlyingData.type = MailItem.Type.MESSAGE.toByte();
        underlyingData.uuid = account.getUid();
        underlyingData.setBlobDigest("test digest");
        MailItem item = new MockMailItem(mbox,underlyingData);
        adapter.put(new IndexingQueueItemLocator(mbox.getId(),mbox.getSchemaGroupId(),item.getId(),item.getType(), account.getId(), false, false));

        //verify that message is in the queue
        assertTrue("item queue should not be empty", adapter.hasMoreItems());
        IndexingQueueItemLocator queuedItem = adapter.peek();
        assertNotNull("empty queued item", queuedItem);
        assertEquals("queued item's mailbox ID is different from test mailbox ID", mbox.getId(),queuedItem.getMailboxID());
        assertEquals("queued item's ID is different from test item's ID", item.getId(),queuedItem.getMailItemID());
        assertEquals("queued item's type is different from test item's type", item.getType(),queuedItem.getMailItemType());

        //pop the message
        IndexingQueueItemLocator nextItem = adapter.take();
        assertNotNull("empty dequeued item", nextItem);
        assertEquals("dequeued item's mailbox ID is different from test mailbox ID", mbox.getId(),nextItem.getMailboxID());
        assertEquals("dequeued item's ID is different from test item's ID", item.getId(),nextItem.getMailItemID());
        assertEquals("dequeued item's type is different from test item's type", item.getType(),nextItem.getMailItemType());

        //verify that there are no more messages
        assertFalse("item queue should be empty", adapter.hasMoreItems());
    }
}
