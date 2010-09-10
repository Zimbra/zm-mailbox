/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.qa.unittest;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.db.DbResults;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;

public class TestPurge extends TestCase {

    private static final String USER_NAME = "user4";
    private static final String NAME_PREFIX = TestPurge.class.getSimpleName();
    
    private String mOriginalSystemTrashLifetime;
    private String mOriginalSystemJunkLifetime;
    private String mOriginalSystemMessageLifetime;
    
    private String mOriginalUserInboxReadLifetime;
    private String mOriginalUserInboxUnreadLifetime;
    private String mOriginalUserSentLifetime;
    private String mOriginalUserTrashLifetime;
    private String mOriginalUserJunkLifetime;
    
    private String mOriginalUseChangeDateForTrash;
    private String mOriginalUseChangeDateForSpam;
    private String mOriginalPurgeBatchSize;
    
    private long mOriginalTombstoneAge;
    
    long mPurgedTimestamp = System.currentTimeMillis() - (2 * Constants.MILLIS_PER_MONTH);
    long mLaterCutoff = mPurgedTimestamp + Constants.MILLIS_PER_HOUR;
    long mMiddleTimestamp = mLaterCutoff + Constants.MILLIS_PER_HOUR;
    long mEarlierCutoff = mMiddleTimestamp + Constants.MILLIS_PER_HOUR; 
    long mKeptTimestamp = mEarlierCutoff + Constants.MILLIS_PER_HOUR;
    
    public void setUp()
    throws Exception {
        cleanUp();
        
        Account account = TestUtil.getAccount(USER_NAME);
        mOriginalSystemTrashLifetime = account.getAttr(Provisioning.A_zimbraMailTrashLifetime);
        mOriginalSystemJunkLifetime = account.getAttr(Provisioning.A_zimbraMailSpamLifetime);
        mOriginalSystemMessageLifetime = account.getAttr(Provisioning.A_zimbraMailMessageLifetime);
        
        mOriginalUserInboxReadLifetime = account.getAttr(Provisioning.A_zimbraPrefInboxReadLifetime);
        mOriginalUserInboxUnreadLifetime = account.getAttr(Provisioning.A_zimbraPrefInboxUnreadLifetime);
        mOriginalUserSentLifetime = account.getAttr(Provisioning.A_zimbraPrefSentLifetime);
        mOriginalUserTrashLifetime = account.getAttr(Provisioning.A_zimbraPrefTrashLifetime);
        mOriginalUserJunkLifetime = account.getAttr(Provisioning.A_zimbraPrefJunkLifetime);
        
        mOriginalUseChangeDateForTrash =
            account.getAttr(Provisioning.A_zimbraMailPurgeUseChangeDateForTrash);
        mOriginalUseChangeDateForSpam =
            account.getAttr(Provisioning.A_zimbraMailPurgeUseChangeDateForSpam);
        
        mOriginalTombstoneAge = LC.tombstone_max_age_ms.longValue();
        mOriginalPurgeBatchSize = TestUtil.getServerAttr(Provisioning.A_zimbraMailPurgeBatchSize);
    }
    
    /**
     * Tests the user retention policy for the <tt>Inbox</tt> folder.
     */
    public void testInbox()
    throws Exception {
        // Set retention policy
        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefInboxUnreadLifetime, "24h");
        attrs.put(Provisioning.A_zimbraPrefInboxReadLifetime, "16h");
        Provisioning.getInstance().modifyAttrs(account, attrs);
        
        // Insert messages
        String prefix = NAME_PREFIX + " testInbox ";
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
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
    public void testSent()
    throws Exception {
        // Set retention policy
        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefSentLifetime, "24h");
        Provisioning.getInstance().modifyAttrs(account, attrs);
        
        // Insert messages
        String prefix = NAME_PREFIX + " testSent ";
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
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
    public void testTrashUser()
    throws Exception {
        // Use the item date for purge.
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraMailPurgeUseChangeDateForTrash, LdapUtil.LDAP_FALSE);
        
        // Set retention policy
        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefTrashLifetime, "24h");
        attrs.put(Provisioning.A_zimbraMailTrashLifetime, "48h");
        Provisioning.getInstance().modifyAttrs(account, attrs);
        
        // Insert messages
        String prefix = NAME_PREFIX + " testTrashUser ";
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
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
    public void testTrashSystem()
    throws Exception {
        // Use the item date for purge.
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraMailPurgeUseChangeDateForTrash, LdapUtil.LDAP_FALSE);

        // Set retention policy
        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefTrashLifetime, "48h");
        attrs.put(Provisioning.A_zimbraMailTrashLifetime, "24h");
        Provisioning.getInstance().modifyAttrs(account, attrs);
        
        // Insert messages
        String prefix = NAME_PREFIX + " testTrashUser ";
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
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
     * Confirms that a shorter user Junk lifetime setting overrides the
     * system setting.
     */
    public void testJunkUser()
    throws Exception {
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraMailPurgeUseChangeDateForSpam, LdapUtil.LDAP_FALSE);
        
        // Set retention policy
        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefJunkLifetime, "24h");
        attrs.put(Provisioning.A_zimbraMailSpamLifetime, "48h");
        Provisioning.getInstance().modifyAttrs(account, attrs);
        
        // Insert messages
        String prefix = NAME_PREFIX + " testJunkUser ";
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
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
    public void testJunkSystem()
    throws Exception {
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraMailPurgeUseChangeDateForSpam, LdapUtil.LDAP_FALSE);

        // Set retention policy
        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefJunkLifetime, "48h");
        attrs.put(Provisioning.A_zimbraMailSpamLifetime, "24h");
        Provisioning.getInstance().modifyAttrs(account, attrs);
        
        // Insert messages
        String prefix = NAME_PREFIX + " testJunkUser ";
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
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
    public void testAll()
    throws Exception {
        // Set retention policy
        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailMessageLifetime, "40d");
        Provisioning.getInstance().modifyAttrs(account, attrs);
        
        // Insert messages
        String prefix = NAME_PREFIX + " testAll ";
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder folder = mbox.createFolder(null, "/" + NAME_PREFIX, (byte) 0, MailItem.TYPE_UNKNOWN);
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
    public void testAllSafeguard()
    throws Exception {
        // Set retention policy
        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailMessageLifetime, "1h");
        Provisioning.getInstance().modifyAttrs(account, attrs);
        
        // Insert messages
        String prefix = NAME_PREFIX + " testAllSafeguard ";
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder folder = mbox.createFolder(null, "/" + NAME_PREFIX, (byte) 0, MailItem.TYPE_UNKNOWN);
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
    public void testSpamChangeDate()
    throws Exception {
        // Set retention policy
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraPrefJunkLifetime, "24h");
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraMailPurgeUseChangeDateForSpam, LdapUtil.LDAP_TRUE);
        
        // Insert message
        String subject = NAME_PREFIX + " testSpamChangeDate";
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Message kept = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, subject,
            System.currentTimeMillis() - (36 * Constants.MILLIS_PER_HOUR));
        mbox.move(null, kept.getId(), MailItem.TYPE_MESSAGE, Mailbox.ID_FOLDER_SPAM);
        
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
    public void testTrashChangeDate()
    throws Exception {
        // Set retention policy
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraPrefTrashLifetime, "24h");
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraMailPurgeUseChangeDateForTrash, LdapUtil.LDAP_TRUE);
        
        // Insert message
        String subject = NAME_PREFIX + " testTrashChangeDate";
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Message kept = TestUtil.addMessage(mbox, Mailbox.ID_FOLDER_INBOX, subject,
            System.currentTimeMillis() - (36 * Constants.MILLIS_PER_HOUR));
        mbox.move(null, kept.getId(), MailItem.TYPE_MESSAGE, Mailbox.ID_FOLDER_TRASH);
        
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
    public void testFolderInTrash()
    throws Exception {
        // Create a subfolder of trash with a message in it.
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        String folderPath = "/Trash/" + NAME_PREFIX + "-testFolderInTrash";
        Folder f = mbox.createFolder(null, folderPath, (byte) 0, Folder.TYPE_MESSAGE);
        String subject = NAME_PREFIX + " testFolderInTrash";
        Message msg = TestUtil.addMessage(mbox, f.getId(), subject, System.currentTimeMillis());
        ZimbraLog.test.info("Date: %d, change date: %d.", msg.getDate(), msg.getChangeDate());
        
        // Set retention policy.
        Account account = TestUtil.getAccount(USER_NAME);
        account.setPrefTrashLifetime("1s");
        account.setMailPurgeUseChangeDateForTrash(false);
        
        // Sleep longer than the trash lifetime and purge.
        Thread.sleep(2000);
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
     * Confirms that tombstones get purged correctly (bug 12965).
     */
    // XXX bburtin: Disabling this method until bug 12965 is fixed. 
    public void disabledTestTombstones()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Message msg = TestUtil.addMessage(mbox, NAME_PREFIX + " testTombstones");

        // Clear out the tombstone table.
        LC.tombstone_max_age_ms.setDefault("0");
        mbox.purgeMessages(null);
        assertEquals(0, getNumTombstones(mbox));
        
        // Delete message to write the tombstone.
        mbox.beginTrackingSync();
        mbox.delete(null, msg.getId(), msg.getType());
        assertEquals(1, getNumTombstones(mbox));
        
        // Set tombstone age to 1 month, run purge, make sure tombstone wasn't deleted.
        LC.tombstone_max_age_ms.setDefault(Long.toString(Constants.MILLIS_PER_MONTH));
        mbox.purgeMessages(null);
        assertEquals(1, getNumTombstones(mbox));
        
        // Set tombstone age to 0, run purge, make sure tombstone was deleted.
        LC.tombstone_max_age_ms.setDefault("0");
        Thread.sleep(Constants.MILLIS_PER_SECOND);
        mbox.purgeMessages(null);
        assertEquals(0, getNumTombstones(mbox));
    }
    
    /**
     * Confirms that old conversations get purged correctly.
     */
    public void testConversations()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        
        // Create original message and reply.
        String subject = NAME_PREFIX + " testConversations";
        Message original = TestUtil.addMessage(mbox, subject);
        Message reply = TestUtil.addMessage(mbox, "RE: " + subject);
        int convId = original.getConversationId();
        assertEquals(convId, reply.getConversationId());
        assertEquals(1, getNumConversations(mbox, convId));
        
        // Set conversation age to 1 month, run purge, make sure the conversation is still open.
        LC.conversation_max_age_ms.setDefault(Long.toString(Constants.MILLIS_PER_MONTH));
        mbox.purgeMessages(null);
        assertEquals(1, getNumConversations(mbox, convId));
        
        // Set conversation age to 0, run purge, make sure the conversation was closed.
        LC.conversation_max_age_ms.setDefault("0");
        Thread.sleep(Constants.MILLIS_PER_SECOND);
        mbox.purgeMessages(null);
        assertEquals(0, getNumConversations(mbox, convId));
    }
    
    public void testBatchSize()
    throws Exception {
        // Use the item date for purge.
        TestUtil.setAccountAttr(USER_NAME, Provisioning.A_zimbraMailPurgeUseChangeDateForTrash, LdapUtil.LDAP_FALSE);
        
        // Set retention policy
        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraPrefTrashLifetime, "24h");
        Provisioning.getInstance().modifyAttrs(account, attrs);
        
        // Insert messages
        String prefix = NAME_PREFIX + " testPurgeMaxItems ";
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
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
    
    private int getNumConversations(Mailbox mbox, int convId)
    throws ServiceException {
        DbResults results = DbUtil.executeQuery(
            "SELECT COUNT(*) FROM " + DbMailItem.getConversationTableName(mbox) +
            " WHERE mailbox_id = " + mbox.getId() + " AND conv_id = " + convId);
        return results.getInt(1);
    }
    
    private int getNumTombstones(Mailbox mbox)
    throws ServiceException {
        DbResults results = DbUtil.executeQuery(
            "SELECT COUNT(*) FROM " + DbMailItem.getTombstoneTableName(mbox) + " WHERE mailbox_id = " + mbox.getId());
        return results.getInt(1);
    }
    
    private Message alterUnread(Message msg, boolean unread)
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        mbox.alterTag(null, msg.getId(), msg.getType(), Flag.ID_FLAG_UNREAD, unread);
        return mbox.getMessageById(null, msg.getId());
    }
    
    private boolean messageExists(int id)
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        try {
            mbox.getMessageById(null, id);
        } catch (ServiceException e) {
            assertTrue("Unexpected exception type: " + e, e instanceof NoSuchItemException);
            return false;
        }
        return true;
    }
    
    public void tearDown()
    throws Exception {
        LC.tombstone_max_age_ms.setDefault(Long.toString(mOriginalTombstoneAge));
        
        Account account = TestUtil.getAccount(USER_NAME);
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailTrashLifetime, mOriginalSystemTrashLifetime);
        attrs.put(Provisioning.A_zimbraMailSpamLifetime, mOriginalSystemJunkLifetime);
        attrs.put(Provisioning.A_zimbraMailMessageLifetime, mOriginalSystemMessageLifetime);
        attrs.put(Provisioning.A_zimbraPrefInboxReadLifetime, mOriginalUserInboxReadLifetime);
        attrs.put(Provisioning.A_zimbraPrefInboxUnreadLifetime, mOriginalUserInboxUnreadLifetime);
        attrs.put(Provisioning.A_zimbraPrefSentLifetime, mOriginalUserSentLifetime);
        attrs.put(Provisioning.A_zimbraPrefTrashLifetime, mOriginalUserTrashLifetime);
        attrs.put(Provisioning.A_zimbraPrefJunkLifetime, mOriginalUserJunkLifetime);
        attrs.put(Provisioning.A_zimbraMailPurgeUseChangeDateForTrash, mOriginalUseChangeDateForTrash);
        attrs.put(Provisioning.A_zimbraMailPurgeUseChangeDateForSpam, mOriginalUseChangeDateForSpam);
        Provisioning.getInstance().modifyAttrs(account, attrs);

        TestUtil.setServerAttr(Provisioning.A_zimbraMailPurgeBatchSize, mOriginalPurgeBatchSize);
        
        cleanUp();
}
    
    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }
}
