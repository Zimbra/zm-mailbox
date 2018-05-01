package com.zimbra.qa.unittest;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.redisson.api.RLiveObjectService;
import org.redisson.api.RedissonClient;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Folder.FolderOptions;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.RedissonClientHolder;
import com.zimbra.cs.mailbox.cache.SharedItemData;
import com.zimbra.cs.mailbox.cache.SharedFolderData;

public class TestLiveObjects {

    private static RLiveObjectService service;
    private static Account acct;
    private static Mailbox mbox;
    private static final String USER = TestLiveObjects.class.getName();

    @BeforeClass
    public static void setUp() throws ServiceException {
        acct = TestUtil.createAccount(USER);
        mbox = TestUtil.getMailbox(USER);
        RedissonClient client = RedissonClientHolder.getInstance().getRedissonClient();
        service = client.getLiveObjectService();
        service.registerClass(SharedFolderData.class);
    }

    @AfterClass
    public static void tearDown() throws ServiceException {
        TestUtil.deleteAccountIfExists(USER);
        service.delete(SharedFolderData.class, "123");
        service.unregisterClass(SharedFolderData.class);
    }

    @Test
    public void testLiveObjects() {
        SharedItemData obj = new SharedItemData("123");
        obj.setSize(1000L);
        obj.setTags("foo,bar");
        obj.setUnreadCount(3);
        obj.setFlags(1);
        SharedItemData proxy1 = service.persist(obj);
        SharedItemData proxy2 = service.get(SharedItemData.class, "123");
        long rSize = proxy2.getSize();
        String rTags = proxy2.getTags();
        int rUnread = proxy2.getUnreadCount();
        int rFlags = proxy2.getFlags();
        proxy1.setSize(2000L);
        proxy1.setUnreadCount(5);
        proxy1.setTags("foo,bar,newtag");
        proxy1.setFlags(2);
        rSize = proxy2.getSize();
        rTags = proxy2.getTags();
        rUnread = proxy2.getUnreadCount();
        rFlags = proxy2.getFlags();
        assertTrue(true);
    }

    @Test
    public void testLiveFolders() throws ServiceException {
        Folder folder1 = mbox.createFolder(null, "/testfolder", new FolderOptions());
        SharedFolderData data1 = new SharedFolderData(acct.getId(), folder1.getUnderlyingData());
        data1 = service.persist(data1);
        folder1.attach(data1);
        Folder folder2 = (Folder) MailItem.constructItem(acct, folder1.getUnderlyingData(), mbox.getId());
        SharedFolderData data2 = service.get(SharedFolderData.class, data1.getId());
        folder2.attach(data2);
        data1.setSize(5000);
        data1.setUnreadCount(10);
        assertEquals("folder size is not synchronized", 5000L, folder2.getSize());
        assertEquals("unread count is not synchronized", 10, folder2.getUnreadCount());
    }
}
