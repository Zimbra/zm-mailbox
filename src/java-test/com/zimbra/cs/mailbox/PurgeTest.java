/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.mail.type.Policy;
import com.zimbra.soap.mail.type.RetentionPolicy;

public class PurgeTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        Provisioning prov = Provisioning.getInstance();
        prov.deleteAccount(MockProvisioning.DEFAULT_ACCOUNT_ID);
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }
    
    @Test
    public void folderPurgePolicy() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        
        // Create folder and test messages.
        Folder folder = mbox.createFolder(null, "/folderPurgePolicy", (byte) 0, MailItem.Type.MESSAGE);
        Message older = TestUtil.addMessage(mbox, folder.getId(), "test1", System.currentTimeMillis() - (60 * Constants.MILLIS_PER_MINUTE));
        Message newer = TestUtil.addMessage(mbox, folder.getId(), "test2", System.currentTimeMillis() - (30 * Constants.MILLIS_PER_MINUTE));
        folder = mbox.getFolderById(null, folder.getId());
        
        // Run purge with default settings and make sure nothing was deleted.
        mbox.purgeMessages(null);
        folder = mbox.getFolderById(null, folder.getId());
        assertEquals(2, folder.getSize());
        
        // Add retention policy.
        Policy p = Policy.newUserPolicy("45m");
        RetentionPolicy purgePolicy = new RetentionPolicy(null, Arrays.asList(p));
        mbox.setRetentionPolicy(null, folder.getId(), MailItem.Type.FOLDER, purgePolicy);
        
        // Purge the folder cache and make sure that purge policy is reloaded from metadata.
        mbox.purge(MailItem.Type.FOLDER);
        folder = mbox.getFolderById(null, folder.getId());
        List<Policy> purgeList = folder.getRetentionPolicy().getPurgePolicy();
        assertEquals(1, purgeList.size());
        assertEquals("45m", purgeList.get(0).getLifetime());
        
        // Run purge and make sure one of the messages was deleted.
        mbox.purgeMessages(null);
        assertEquals(1, folder.getSize());
        mbox.getMessageById(null, newer.getId());
        try {
            mbox.getMessageById(null, older.getId());
            fail("Older message was not purged.");
        } catch (NoSuchItemException e) {
            // Older message was purged.
        }
        
        // Remove purge policy and verify that the folder state was properly updated.
        mbox.setRetentionPolicy(null, folder.getId(), MailItem.Type.FOLDER, null);
        mbox.purge(MailItem.Type.FOLDER);
        folder = mbox.getFolderById(null, folder.getId());
        assertEquals(0, folder.getRetentionPolicy().getKeepPolicy().size());
        assertEquals(0, folder.getRetentionPolicy().getPurgePolicy().size());
    }
    
    @Test
    public void tagPurgePolicy() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        
        // Create folder and test messages.
        Tag tag = mbox.createTag(null, "tag", (byte) 0);
        Folder inbox = mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
        Message older = TestUtil.addMessage(mbox, inbox.getId(), "test1", System.currentTimeMillis() - (60 * Constants.MILLIS_PER_MINUTE));
        Message newer = TestUtil.addMessage(mbox, inbox.getId(), "test2", System.currentTimeMillis() - (30 * Constants.MILLIS_PER_MINUTE));
        Message notTagged = TestUtil.addMessage(mbox, inbox.getId(), "test3", System.currentTimeMillis() - (90 * Constants.MILLIS_PER_MINUTE));
        mbox.setTags(null, older.getId(), older.getType(), 0, new String[] { tag.getName() });
        mbox.setTags(null, newer.getId(), newer.getType(), 0, new String[] { tag.getName() });
        
        // Run purge with default settings and make sure nothing was deleted.
        mbox.purgeMessages(null);
        assertEquals(3, inbox.getSize());
        
        // Add retention policy.
        Policy p = Policy.newUserPolicy("45m");
        RetentionPolicy purgePolicy = new RetentionPolicy(null, Arrays.asList(p));
        mbox.setRetentionPolicy(null, tag.getId(), MailItem.Type.TAG, purgePolicy);
        
        // Purge the tag cache and make sure that purge policy is reloaded from metadata.
        mbox.purge(MailItem.Type.TAG);
        tag = mbox.getTagById(null, tag.getId());
        List<Policy> purgeList = tag.getRetentionPolicy().getPurgePolicy();
        assertEquals(1, purgeList.size());
        assertEquals("45m", purgeList.get(0).getLifetime());
        
        // Run purge and make sure one of the messages was deleted.
        mbox.purgeMessages(null);
        inbox = mbox.getFolderById(null, inbox.getId());
        assertEquals(2, inbox.getSize());
        mbox.getMessageById(null, newer.getId());
        mbox.getMessageById(null, notTagged.getId());
        try {
            mbox.getMessageById(null, older.getId());
            fail("Older message was not purged.");
        } catch (NoSuchItemException e) {
        }
        
        // Remove purge policy and verify that the folder state was properly updated.
        mbox.setRetentionPolicy(null, tag.getId(), MailItem.Type.TAG, null);
        mbox.purge(MailItem.Type.TAG);
        tag = mbox.getTagById(null, tag.getId());
        assertEquals(0, tag.getRetentionPolicy().getKeepPolicy().size());
        assertEquals(0, tag.getRetentionPolicy().getPurgePolicy().size());
    }

    /**
     * Tests the user retention policy for the <tt>Inbox</tt> folder.
     */
    @Test
    public void purgeInbox()
    throws Exception {
        // Set retention policy
        Account account = getAccount();
        account.setPrefInboxUnreadLifetime("24h");
        account.setPrefInboxReadLifetime("16h");

        // Insert messages
        String prefix = "purgeInbox ";
        Mailbox mbox = getMailbox();
        Message purgedUnread = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, prefix + "purgedUnread",
            System.currentTimeMillis() - (25 * Constants.MILLIS_PER_HOUR));
        Message keptUnread = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, prefix + "keptUnread",
            System.currentTimeMillis() - (18 * Constants.MILLIS_PER_HOUR));
        Message purgedRead = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, prefix + "purgedRead",
            System.currentTimeMillis() - (18 * Constants.MILLIS_PER_HOUR));
        Message keptRead = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, prefix + "keptRead",
            System.currentTimeMillis() - (15 * Constants.MILLIS_PER_HOUR));

        // Mark read/unread and refresh
        purgedUnread = alterUnread(purgedUnread, true);
        keptUnread = alterUnread(keptUnread, true);
        purgedRead = alterUnread(purgedRead, false);
        keptRead = alterUnread(keptRead, false);

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertFalse("purgedUnread was kept", messageExists(purgedUnread.getId()));
        assertTrue("keptUnread was purged", messageExists(keptUnread.getId()));
        assertFalse("purgedRead was kept", messageExists(purgedRead.getId()));
        assertTrue("keptRead was purged", messageExists(keptRead.getId()));
    }

    /**
     * Tests the user retention policy for the <tt>Sent</tt> folder.
     */
    @Test
    public void purgeSent()
    throws Exception {
        // Set retention policy
        Account account = getAccount();
        account.setPrefSentLifetime("24h");

        // Insert messages
        String prefix = "purgeSent ";
        Mailbox mbox = getMailbox();
        Message purged = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_SENT, prefix + "purged",
            System.currentTimeMillis() - (25 * Constants.MILLIS_PER_HOUR));
        Message kept = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_SENT, prefix + "kept",
            System.currentTimeMillis() - (18 * Constants.MILLIS_PER_HOUR));

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertFalse("purged was kept", messageExists(purged.getId()));
        assertTrue("kept was purged", messageExists(kept.getId()));
    }

    /**
     * Confirms that a shorter user trash lifetime setting overrides the
     * system setting.
     */
    @Test
    public void testTrashUser()
    throws Exception {
        Account account = getAccount();
        account.setMailPurgeUseChangeDateForTrash(false);
        account.setPrefTrashLifetime("24h");
        account.setMailTrashLifetime("48h");

        // Insert messages
        String prefix = "testTrashUser ";
        Mailbox mbox = getMailbox();
        Message purged = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_TRASH, prefix + "purged",
            System.currentTimeMillis() - (36 * Constants.MILLIS_PER_HOUR));
        Message kept = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_TRASH, prefix + "kept",
            System.currentTimeMillis() - (16 * Constants.MILLIS_PER_HOUR));

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertFalse("purged was kept", messageExists(purged.getId()));
        assertTrue("kept was purged", messageExists(kept.getId()));
    }

    /**
     * Confirms that a shorter system trash lifetime setting overrides the
     * user setting.
     */
    @Test
    public void testTrashSystem()
    throws Exception {
        Account account = getAccount();
        account.setMailPurgeUseChangeDateForTrash(false);
        account.setPrefTrashLifetime("48h");
        account.setMailTrashLifetime("24h");

        // Insert messages
        String prefix = "testTrashSystem ";
        Mailbox mbox = getMailbox();
        Message purged = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_TRASH, prefix + "purged",
            System.currentTimeMillis() - (36 * Constants.MILLIS_PER_HOUR));
        Message kept = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_TRASH, prefix + "kept",
            System.currentTimeMillis() - (16 * Constants.MILLIS_PER_HOUR));

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertFalse("purged was kept", messageExists(purged.getId()));
        assertTrue("kept was purged", messageExists(kept.getId()));
    }
    
    @Test
    public void purgeBatchSize()
    throws Exception {
        Account account = getAccount();
        account.setMailPurgeUseChangeDateForTrash(false);
        account.setPrefTrashLifetime("24h");

        // Insert messages
        String prefix = "purgeBatchSize ";
        Mailbox mbox = getMailbox();
        Message purged = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_TRASH, prefix + "purged",
            System.currentTimeMillis() - (36 * Constants.MILLIS_PER_HOUR));
        Message kept = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_TRASH, prefix + "kept",
            System.currentTimeMillis() - (35 * Constants.MILLIS_PER_HOUR));

        // Run purge and verify results
        TestUtil.setServerAttr(Provisioning.A_zimbraMailPurgeBatchSize, Integer.toString(1));
        assertFalse(mbox.purgeMessages(null));
        assertFalse("purged was kept", messageExists(purged.getId()));
        assertTrue("kept was purged", messageExists(kept.getId()));

        // Run purge again and make sure that the second message was purged.
        TestUtil.setServerAttr(Provisioning.A_zimbraMailPurgeBatchSize, Integer.toString(2));
        assertTrue(mbox.purgeMessages(null));
        assertFalse("second message was not purged", messageExists(kept.getId()));
    }

    /**
     * Confirms that a shorter user Junk lifetime setting overrides the
     * system setting.
     */
    @Test
    public void testJunkUser()
    throws Exception {
        Account account = getAccount();
        account.setMailPurgeUseChangeDateForSpam(false);
        account.setPrefJunkLifetime("24h");
        account.setMailSpamLifetime("48h");

        // Insert messages
        String prefix = "testJunkUser ";
        Mailbox mbox = getMailbox();
        Message purged = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_SPAM, prefix + "purged",
            System.currentTimeMillis() - (36 * Constants.MILLIS_PER_HOUR));
        Message kept = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_SPAM, prefix + "kept",
            System.currentTimeMillis() - (16 * Constants.MILLIS_PER_HOUR));

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertFalse("purged was kept", messageExists(purged.getId()));
        assertTrue("kept was purged", messageExists(kept.getId()));
    }

    /**
     * Confirms that a shorter system Junk lifetime setting overrides the
     * user setting.
     */
    @Test
    public void testJunkSystem()
    throws Exception {
        Account account = getAccount();
        account.setMailPurgeUseChangeDateForSpam(false);
        account.setPrefJunkLifetime("48h");
        account.setMailSpamLifetime("24h");

        // Insert messages
        String prefix = "testJunkUser ";
        Mailbox mbox = getMailbox();
        Message purged = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_SPAM, prefix + "purged",
            System.currentTimeMillis() - (36 * Constants.MILLIS_PER_HOUR));
        Message kept = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_SPAM, prefix + "kept",
            System.currentTimeMillis() - (16 * Constants.MILLIS_PER_HOUR));

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertFalse("purged was kept", messageExists(purged.getId()));
        assertTrue("kept was purged", messageExists(kept.getId()));
    }

    /**
     * Tests the user retention policy for all messages.
     */
    @Test
    public void testAll()
    throws Exception {
        // Set retention policy
        Account account = getAccount();
        account.setMailMessageLifetime("40d");

        // Insert messages
        String prefix = "testAll ";
        Mailbox mbox = getMailbox();
        Folder folder = mbox.createFolder(null, "/testAll" , (byte) 0, MailItem.Type.UNKNOWN);
        Message purged = TestUtil.addMessage(mbox, folder.getId(), prefix + "purged",
            System.currentTimeMillis() - (41 * Constants.MILLIS_PER_DAY));
        Message kept = TestUtil.addMessage(mbox, folder.getId(), prefix + "kept",
            System.currentTimeMillis() - (39 * Constants.MILLIS_PER_DAY));

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertFalse("purged was kept", messageExists(purged.getId()));
        assertTrue("kept was purged", messageExists(kept.getId()));
    }

    /**
     * Tests the safeguard for the mailbox-wide message retention policy.
     */
    @Test
    public void testAllSafeguard()
    throws Exception {
        // Set retention policy
        Account account = getAccount();
        account.setMailMessageLifetime("1h");

        // Insert messages
        String prefix = "testAllSafeguard ";
        Mailbox mbox = getMailbox();
        Folder folder = mbox.createFolder(null, "/testAllSafeguard", (byte) 0, MailItem.Type.UNKNOWN);
        Message purged = TestUtil.addMessage(mbox, folder.getId(), prefix + "purged",
            System.currentTimeMillis() - (32 * Constants.MILLIS_PER_DAY));
        Message kept = TestUtil.addMessage(mbox, folder.getId(), prefix + "kept",
            System.currentTimeMillis() - (30 * Constants.MILLIS_PER_DAY));

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertFalse("purged was kept", messageExists(purged.getId()));
        assertTrue("kept was purged", messageExists(kept.getId()));
    }

    /**
     * Confirms that messages are purged from trash based on the value of
     * <tt>zimbraMailPurgeUseChangeDateForSpam<tt>.  See bug 19702 for more details.
     */
    @Test
    public void testSpamChangeDate()
    throws Exception {
        Account account = getAccount();
        account.setPrefJunkLifetime("24h");
        account.setMailPurgeUseChangeDateForSpam(true);

        // Insert message
        String subject = "testSpamChangeDate";
        Mailbox mbox = getMailbox();
        Message kept = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, subject,
            System.currentTimeMillis() - (36 * Constants.MILLIS_PER_HOUR));
        mbox.move(null, kept.getId(), MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_SPAM);

        // Validate dates
        long cutoff = System.currentTimeMillis() - Constants.MILLIS_PER_DAY;
        assertTrue("Unexpected message date: " + kept.getDate(),
            kept.getDate() < cutoff);
        assertTrue("Unexpected change date: " + kept.getChangeDate(),
            kept.getChangeDate() > cutoff);

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertTrue("kept was purged", messageExists(kept.getId()));
    }

    /**
     * Confirms that messages are purged from trash based on the value of
     * <tt>zimbraMailPurgeUseChangeDateForTrash<tt>.  See bug 19702 for more details.
     */
    @Test
    public void testTrashChangeDate()
    throws Exception {
        Account account = getAccount();
        account.setPrefTrashLifetime("24h");
        account.setMailPurgeUseChangeDateForTrash(true);

        // Insert message
        String subject = "testTrashChangeDate";
        Mailbox mbox = getMailbox();
        Message kept = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, subject,
            System.currentTimeMillis() - (36 * Constants.MILLIS_PER_HOUR));
        mbox.move(null, kept.getId(), MailItem.Type.MESSAGE, Mailbox.ID_FOLDER_TRASH);

        // Validate dates
        long cutoff = System.currentTimeMillis() - Constants.MILLIS_PER_DAY;
        assertTrue("Unexpected message date: " + kept.getDate(),
            kept.getDate() < cutoff);
        assertTrue("Unexpected change date: " + kept.getChangeDate(),
            kept.getChangeDate() > cutoff);

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertTrue("kept was purged", messageExists(kept.getId()));
    }

    /**
     * Confirms that empty folders in trash are purged (bug 16885).
     */
    @Test
    public void testFolderInTrash()
    throws Exception {
        // Create a subfolder of trash with a message in it.
        Mailbox mbox = getMailbox();
        String folderPath = "/Trash/testFolderInTrash";
        Folder f = mbox.createFolder(null, folderPath, (byte) 0, Folder.Type.MESSAGE);
        String subject = "testFolderInTrash";
        Message msg = TestUtil.addMessage(mbox, f.getId(), subject, System.currentTimeMillis() - Constants.MILLIS_PER_DAY);

        // Set retention policy.
        Account account = getAccount();
        account.setPrefTrashLifetime("1ms");
        account.setMailPurgeUseChangeDateForTrash(false);

        mbox.purgeMessages(null);

        // Make sure both the message and folder were deleted.
        try {
            mbox.getMessageById(null, msg.getId());
            fail("Message " + msg.getId() + " was not deleted.");
        } catch (NoSuchItemException e) {
        }

        try {
            mbox.getFolderById(null, f.getId());
            fail("Folder " + f.getId() + " was not deleted.");
        } catch (NoSuchItemException e) {
        }
    }

    /**
     * Confirms that recently moved trash folders do not get purged when ChangeDate is true.
     */
    @Test
    public void testRecentFolderInTrashChangeDate()
    throws Exception {
        
        Account account = getAccount();
        account.setPrefTrashLifetime("12h");
        account.setMailPurgeUseChangeDateForTrash(true);
        
        // Create a subfolder of inbox with a message in it.
        Mailbox mbox = getMailbox();
        String folderPath = "/Inbox/testRecentFolderInTrashChangeDate";
        Folder f = mbox.createFolder(null, folderPath, (byte) 0, Folder.Type.MESSAGE);
        String subject = "testRecentFolderInTrashChangeDate";
        Message msg = TestUtil.addMessage(mbox, f.getId(), subject, System.currentTimeMillis() - Constants.MILLIS_PER_DAY);

        // move the folder to trash
        mbox.move(null, f.getId(), MailItem.Type.FOLDER, Mailbox.ID_FOLDER_TRASH);

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertTrue("msg was purged", messageExists(msg.getId()));
    }
    
    /**
     * Confirms that recently moved trash folders does get purged when ChangeDate is false.
     */
    @Test
    public void testRecentFolderInTrashNoChangeDate()
    throws Exception {
        
        Account account = getAccount();
        account.setPrefTrashLifetime("12h");
        account.setMailPurgeUseChangeDateForTrash(false);
        
        // Create a subfolder of inbox with a message in it.
        Mailbox mbox = getMailbox();
        String folderPath = "/Inbox/testRecentFolderInTrashNoChangeDate";
        Folder f = mbox.createFolder(null, folderPath, (byte) 0, Folder.Type.MESSAGE);
        String subject = "testRecentFolderInTrashNoChangeDate";
        Message msg = TestUtil.addMessage(mbox, f.getId(), subject, System.currentTimeMillis() - Constants.MILLIS_PER_DAY);

        // move the folder to trash
        mbox.move(null, f.getId(), MailItem.Type.FOLDER, Mailbox.ID_FOLDER_TRASH);

        // Run purge and verify results
        mbox.purgeMessages(null);
        assertFalse("msg was not purged", messageExists(msg.getId()));
    }
    
    @Test
    public void invalidFolderMessageLifetime() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.createFolder(null, "/invalidFolderMessageLifetime", (byte) 0, MailItem.Type.MESSAGE);
        Policy p = Policy.newUserPolicy("45x");
        RetentionPolicy purgePolicy = new RetentionPolicy(null, Arrays.asList(p));
        try {
            mbox.setRetentionPolicy(null, folder.getId(), MailItem.Type.FOLDER, purgePolicy);
            fail("Invalid time interval should not have been accepted.");
        } catch (ServiceException e) {
        }
    }

    @Test
    public void multipleUserPolicy() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.createFolder(null, "/multipleUserPolicy", (byte) 0, MailItem.Type.MESSAGE);
        
        List<Policy> list = Arrays.asList(
            Policy.newUserPolicy("45d"),
            Policy.newUserPolicy("60d"));
        RetentionPolicy purgePolicy = new RetentionPolicy(null, list);
        try {
            mbox.setRetentionPolicy(null, folder.getId(), MailItem.Type.FOLDER, purgePolicy);
            fail("Multiple purge policies.");
        } catch (ServiceException e) {
        }
        
        purgePolicy = new RetentionPolicy(list, null);
        try {
            mbox.setRetentionPolicy(null, folder.getId(), MailItem.Type.FOLDER, purgePolicy);
            fail("Multiple keep policies.");
        } catch (ServiceException e) {
        }
    }
    
    @Test
    public void modifySystemPolicy() throws Exception {
        RetentionPolicyManager mgr = RetentionPolicyManager.getInstance();
        Policy keep1 = mgr.createSystemKeepPolicy("keep1", "300d");
        Policy keep2 = mgr.createSystemKeepPolicy("keep2", "400d");
        Policy purge1 = mgr.createSystemPurgePolicy("purge1", "500d");
        Policy purge2 = mgr.createSystemPurgePolicy("purge2", "500d");
        
        assertEquals(keep1, mgr.getPolicyById(keep1.getId()));
        assertEquals(keep2, mgr.getPolicyById(keep2.getId()));
        assertEquals(purge1, mgr.getPolicyById(purge1.getId()));
        assertEquals(purge2, mgr.getPolicyById(purge2.getId()));
        
        // Test modify.
        mgr.modifySystemPolicy(keep1.getId(), "new keep1", "301d");
        Policy newKeep1 = mgr.getPolicyById(keep1.getId());
        assertFalse(keep1.equals(newKeep1));
        assertEquals(keep1.getId(), newKeep1.getId());
        assertEquals("new keep1", newKeep1.getName());
        assertEquals("301d", newKeep1.getLifetime());
        
        // Test delete.
        assertTrue(mgr.deleteSystemPolicy(purge2.getId()));
        assertNull(mgr.getPolicyById(purge2.getId()));
        RetentionPolicy rp = mgr.getSystemRetentionPolicy();
        assertEquals(2, rp.getKeepPolicy().size());
        assertEquals(1, rp.getPurgePolicy().size());
    }

    /**
     * Tests {@link RetentionPolicyManager#getCompleteRetentionPolicy(RetentionPolicy).  Confirms
     * that system policy elements are updated with the latest values in LDAP.
     */
    @Test
    public void completeRetentionPolicy() throws Exception {
        RetentionPolicyManager mgr = RetentionPolicyManager.getInstance();
        Policy keep1 = mgr.createSystemKeepPolicy("keep1", "300d");
        
        // Create mailbox policy that references the system policy, and confirm that
        // lookup returns the latest values.
        RetentionPolicy mboxRP = new RetentionPolicy(Arrays.asList(Policy.newSystemPolicy(keep1.getId())), null);
        RetentionPolicy completeRP = mgr.getCompleteRetentionPolicy(mboxRP);
        Policy latest = completeRP.getKeepPolicy().get(0);
        assertEquals(keep1, latest);
        
        // Modify system policy and confirm that the accessor returns the latest values.
        mgr.modifySystemPolicy(keep1.getId(), "new keep1", "301d");
        completeRP = mgr.getCompleteRetentionPolicy(mboxRP);
        latest = completeRP.getKeepPolicy().get(0);
        assertFalse(keep1.equals(latest));
        assertEquals(keep1.getId(), latest.getId());
        assertEquals("new keep1", latest.getName());
        assertEquals("301d", latest.getLifetime());
    }
    
    @Test
    public void purgeWithSystemPolicy() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        
        // Create folder and test messages.
        Folder folder = mbox.createFolder(null, "/purgeWithSystemPolicy", (byte) 0, MailItem.Type.MESSAGE);
        Message older = TestUtil.addMessage(mbox, folder.getId(), "older", System.currentTimeMillis() - (60 * Constants.MILLIS_PER_MINUTE));
        Message newer = TestUtil.addMessage(mbox, folder.getId(), "newer", System.currentTimeMillis() - (30 * Constants.MILLIS_PER_MINUTE));
        folder = mbox.getFolderById(null, folder.getId());
        
        // Add user and system retention policy.
        Policy system = RetentionPolicyManager.getInstance().createSystemPurgePolicy("system", "45m");
        
        Policy p1 = Policy.newUserPolicy("90m");
        Policy p2 = Policy.newSystemPolicy(system.getId());
        RetentionPolicy purgePolicy = new RetentionPolicy(null, Arrays.asList(p1, p2));
        mbox.setRetentionPolicy(null, folder.getId(), MailItem.Type.FOLDER, purgePolicy);
        
        // Run purge and make sure one of the messages was deleted.
        mbox.purgeMessages(null);
        folder = mbox.getFolderById(folder.getId());
        assertEquals(1, folder.getSize());
        mbox.getMessageById(null, newer.getId());
        try {
            mbox.getMessageById(null, older.getId());
            fail("Older message was not purged.");
        } catch (NoSuchItemException e) {
        }

        // Update system policy, rerun purge, and make sure the older message was deleted.
        RetentionPolicyManager.getInstance().modifySystemPolicy(system.getId(), system.getName(), "20m");
        mbox.purgeMessages(null);
        folder = mbox.getFolderById(folder.getId());
        assertEquals(0, folder.getSize());
        try {
            mbox.getMessageById(null, newer.getId());
            fail("Newer message was not purged.");
        } catch (NoSuchItemException e) {
        }
    }
    
    private Message alterUnread(Message msg, boolean unread) throws Exception {
        Mailbox mbox = getMailbox();
        mbox.alterTag(null, msg.getId(), msg.getType(), Flag.FlagInfo.UNREAD, unread, null);
        return mbox.getMessageById(null, msg.getId());
    }
    
    private boolean messageExists(int id)
    throws Exception {
        Mailbox mbox = getMailbox();
        try {
            mbox.getMessageById(null, id);
        } catch (ServiceException e) {
            assertTrue("Unexpected exception type: " + e, e instanceof NoSuchItemException);
            return false;
        }
        return true;
    }

    private Account getAccount()
    throws ServiceException {
        return Provisioning.getInstance().get(AccountBy.id, MockProvisioning.DEFAULT_ACCOUNT_ID);
    }
    
    private Mailbox getMailbox() throws ServiceException {
        return MailboxManager.getInstance().getMailboxByAccount(getAccount());
    }
}
