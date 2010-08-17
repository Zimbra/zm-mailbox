package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.zclient.ZDataSource;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZImapDataSource;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;

import junit.framework.TestCase;

public class TestImapOneWayImport extends TestCase {
	 private static final String REMOTE_USER_NAME = "testimapimportremote";
	 private static final String LOCAL_USER_NAME = "testimapimportlocal";
	 private static final String NAME_PREFIX = "TestImapOneWayImport";
	 private static final String DS_FOLDER_ROOT = "/" + NAME_PREFIX;
	    
	 // Folder hierarchy: /TestImapImport-f1/TestImapImport-f2, /TestImapImport-f3/TestImapImport-f4
	 private static final String REMOTE_PATH_F1 = "/" + NAME_PREFIX + "-f1";
	 private static final String REMOTE_PATH_F2 = REMOTE_PATH_F1 + "/" + NAME_PREFIX + "-f2";
	 private static final String REMOTE_PATH_F3 = "/" + NAME_PREFIX + "-f3";
	 private static final String REMOTE_PATH_F4 = REMOTE_PATH_F3 + "/" + NAME_PREFIX + "-f4";
	    
	 private static final String LOCAL_PATH_F1 = DS_FOLDER_ROOT + REMOTE_PATH_F1;
	 private static final String LOCAL_PATH_F2 = DS_FOLDER_ROOT + REMOTE_PATH_F2;
	 private static final String LOCAL_PATH_F3 = DS_FOLDER_ROOT + REMOTE_PATH_F3;
	 private static final String LOCAL_PATH_F4 = DS_FOLDER_ROOT + REMOTE_PATH_F4;
	    
	 private static final String LOCAL_PATH_INBOX = DS_FOLDER_ROOT + "/INBOX";
	 private static final String LOCAL_PATH_TRASH = DS_FOLDER_ROOT + "/Trash";
	    
	 private ZMailbox mRemoteMbox;
	 private ZMailbox mLocalMbox;
	 private String mOriginalCleartextValue;
	 private ZDataSource mDataSource;
	 private boolean mOriginalEnableStarttls;
	    
    public void setUp()
    throws Exception {
        cleanUp();
        
        // Get mailbox references
        if (!TestUtil.accountExists(LOCAL_USER_NAME)) {
            TestUtil.createAccount(LOCAL_USER_NAME);
        }
        if (!TestUtil.accountExists(REMOTE_USER_NAME)) {
            TestUtil.createAccount(REMOTE_USER_NAME);
        }
        mRemoteMbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        mLocalMbox = TestUtil.getZMailbox(LOCAL_USER_NAME);
        
        // Get or create folder
        ZFolder folder = mLocalMbox.getFolderByPath(DS_FOLDER_ROOT);
        if (folder == null) {
            folder = TestUtil.createFolder(mLocalMbox, NAME_PREFIX);
        }
        
        // Create data source
        int port = Integer.parseInt(TestUtil.getServerAttr(Provisioning.A_zimbraImapBindPort));
        mDataSource = new ZImapDataSource(NAME_PREFIX, true, "localhost",
            port, REMOTE_USER_NAME, TestUtil.DEFAULT_PASSWORD, folder.getId(), DataSource.ConnectionType.cleartext, true);
  
        String id = mLocalMbox.createDataSource(mDataSource);
        mDataSource = null;
        for (ZDataSource ds : mLocalMbox.getAllDataSources()) {
            if (ds.getId().equals(id)) {
                mDataSource = ds;
            }
        }
        assertNotNull(mDataSource);
        // Turn on cleartext login
        mOriginalCleartextValue = TestUtil.getServerAttr(Provisioning.A_zimbraImapCleartextLoginEnabled);
        TestUtil.setServerAttr(
            Provisioning.A_zimbraImapCleartextLoginEnabled, Provisioning.TRUE);
        
        // Turn off STARTTLS support so that unit tests don't bomb on Linux
        // (see bug 33683).
        mOriginalEnableStarttls = LC.javamail_imap_enable_starttls.booleanValue();
        LC.javamail_imap_enable_starttls.setDefault(Boolean.toString(false));
    }
	    
	    public void testImapOneWayImport()
	    throws Exception {
	        List<ZMessage> msgs;
	        ZMessage msg;
	        // Remote: add 1 message
	        ZimbraLog.test.info("Testing adding message to remote inbox.");
	        String remoteQuery = "in:inbox msg1";
	        TestUtil.addMessage(mRemoteMbox, NAME_PREFIX + " msg1", Integer.toString(Mailbox.ID_FOLDER_INBOX), "u");
	        checkMsgCount(mRemoteMbox, remoteQuery, 1);
	        assertNull(mLocalMbox.getFolderByPath(LOCAL_PATH_INBOX));
	        msgs = TestUtil.search(mRemoteMbox, remoteQuery);
	        assertEquals("Message count in remote inbox", 1, msgs.size());
	        msg = msgs.get(0);
	        assertTrue("Remote message is read", msg.isUnread());

	        importImap();

	        String localInboxQuery = "in:" + LOCAL_PATH_INBOX;
	        checkMsgCount(mLocalMbox, localInboxQuery, 1);
	        msgs = TestUtil.search(mRemoteMbox, remoteQuery);
	        msg = msgs.get(0);
	        assertTrue("Remote message is read", msg.isUnread());
	        compare();

	        
	        // Remote: flag message
	        ZimbraLog.test.info("Testing flag.");
	        msgs = TestUtil.search(mRemoteMbox, remoteQuery);
	        assertEquals("Message count in remote inbox", 1, msgs.size());
	        msg = msgs.get(0);
	        assertTrue("Remote message is read", msg.isUnread());
	        String remoteId = msg.getId();
	        mRemoteMbox.flagMessage(remoteId, true);
	        
	        // Make sure local copy is not flagged or read
	        msgs = TestUtil.search(mLocalMbox, localInboxQuery);
	        assertEquals("Message count in local inbox", 1, msgs.size());
	        msg = msgs.get(0);
	        assertFalse("Local message is flagged", msg.isFlagged());
	        assertTrue("Local message is read", msg.isUnread());

	        importImap();
	        
	        // Make sure that local copy is now flagged but still unread
	        msgs = TestUtil.search(mLocalMbox, localInboxQuery);
	        assertEquals("Message count in local inbox", 1, msgs.size());
	        msg = msgs.get(0);
	        assertTrue("Local message is flagged", msg.isFlagged());
	        assertTrue("Local message is read", msg.isUnread());

	        compare();
	        
	        
	        // Remote: move to trash
	        ZimbraLog.test.info("Testing remote move to trash.");
	        mRemoteMbox.trashMessage(remoteId);
	        checkMsgCount(mRemoteMbox, "in:trash", 1);
	        checkMsgCount(mLocalMbox, "in:trash", 0);
	        importImap();
	        checkMsgCount(mLocalMbox, "in:" + DS_FOLDER_ROOT + "/Trash", 1);
	        compare();

	        
	        // Create folders on both sides
	        ZimbraLog.test.info("Testing folder creation.");
	        TestUtil.createFolder(mRemoteMbox, REMOTE_PATH_F1);
	        TestUtil.createFolder(mRemoteMbox, REMOTE_PATH_F2);
	        TestUtil.createFolder(mLocalMbox, LOCAL_PATH_F3);
	        TestUtil.createFolder(mLocalMbox, LOCAL_PATH_F4);
	        importImap();
	        
	        // Make sure that new folders got created locally
	        assertNotNull("Local folder " + LOCAL_PATH_F1, mLocalMbox.getFolderByPath(LOCAL_PATH_F1));
	        assertNotNull("Local folder " + LOCAL_PATH_F2, mLocalMbox.getFolderByPath(LOCAL_PATH_F2));
	        
	        // Make sure that new folder is NOT created remotely
	        assertNull("Remote folder " + REMOTE_PATH_F3, mRemoteMbox.getFolderByPath(REMOTE_PATH_F3));
	        assertNull("Remote folder " + REMOTE_PATH_F4, mRemoteMbox.getFolderByPath(REMOTE_PATH_F4));
	        
	        // Test UIDVALIDITY change
	        ZimbraLog.test.info("Testing UIDVALIDITY change.");
	        ZFolder localFolder1 = mLocalMbox.getFolderByPath(LOCAL_PATH_F1);
	        ZFolder remoteFolder1 = mRemoteMbox.getFolderByPath(REMOTE_PATH_F1);
	        
	        // Insert message into folder1 and remember original id
	        String subject = NAME_PREFIX + " msg2";
	        String originalId = TestUtil.addMessage(mLocalMbox, subject, localFolder1.getId());
	        msgs = TestUtil.search(mLocalMbox, subject);
	        assertEquals(1, msgs.size());
	        assertEquals(originalId, msgs.get(0).getId());
	        
	        // Rename remote folder twice to force UIDVALIDITY change and sync.
	        mRemoteMbox.renameFolder(remoteFolder1.getId(), NAME_PREFIX + "-renamed");
	        mRemoteMbox.renameFolder(remoteFolder1.getId(), NAME_PREFIX + "-f1");
	        importImap();
	        
	        // Make sure the original message is still there, and was synced to
	        // the remote mailbox and back.
	        msgs = TestUtil.search(mLocalMbox, subject);
	        assertEquals(1, msgs.size());
	        assertFalse("Message id did not change: " + originalId, originalId.equals(msgs.get(0).getId()));        
	        
	        // Add remote folder, add message to remote folder and delete local folder at the same time
	        ZimbraLog.test.info("Testing simultaneous message add and folder delete 1.");
	        ZFolder remoteFolder2 = mRemoteMbox.getFolderByPath(REMOTE_PATH_F2); 
	        TestUtil.addMessage(mRemoteMbox, NAME_PREFIX + " msg3", remoteFolder2.getId());
	        ZFolder localFolder3 = mLocalMbox.getFolderByPath(LOCAL_PATH_F3);
	        TestUtil.createFolder(mRemoteMbox, REMOTE_PATH_F3);
	        mLocalMbox.deleteFolder(localFolder3.getId());
	        checkMsgCount(mLocalMbox, "in:" + LOCAL_PATH_F2, 0);
	        importImap();
	        
	        // Make sure that remote folders did not get deleted and that the message was added locally
	        assertNotNull("Remote folder 3", mRemoteMbox.getFolderByPath(REMOTE_PATH_F3));
	        checkMsgCount(mLocalMbox, "in:" + LOCAL_PATH_F2, 1);
	        
	        
	        // Add message to a local folder and delete the same folder in remote mailbox
	        ZimbraLog.test.info("Testing simultaneous message add and folder delete 2.");
	        ZFolder localFolder = mLocalMbox.getFolderByPath(LOCAL_PATH_F2);
	        TestUtil.addMessage(mLocalMbox, NAME_PREFIX + " msg4", localFolder.getId());
	        remoteFolder1 = mRemoteMbox.getFolderByPath(REMOTE_PATH_F1);
	        mRemoteMbox.deleteFolder(remoteFolder1.getId());
	        //make sure local folder exists before sync
	        assertNotNull("Local folder 1",mLocalMbox.getFolderByPath(LOCAL_PATH_F1));
	        // Make sure remote folder was deleted
	        assertNull("Remote folder 1", mRemoteMbox.getFolderByPath(REMOTE_PATH_F1));
	        importImap();
	        
	        // Make sure remote folder is still deleted
	        assertNull("Remote folder 1", mRemoteMbox.getFolderByPath(REMOTE_PATH_F1));
	        // Make sure local folder was deleted as well
	        assertNull("Local folder 1", mLocalMbox.getFolderByPath(LOCAL_PATH_F1));
	        //make sure local message did not get added to remote folder 
	        checkMsgCount(mRemoteMbox, "msg4", 0);
	        
	        // Add message to local inbox
	        ZimbraLog.test.info("Testing sync from local to remote.");
	        checkMsgCount(mLocalMbox, "in:" + LOCAL_PATH_INBOX, 0);
	        ZFolder localInbox = mLocalMbox.getFolderByPath(LOCAL_PATH_INBOX);
	        TestUtil.addMessage(mLocalMbox, NAME_PREFIX + " msg5", localInbox.getId());
	        checkMsgCount(mLocalMbox, "in:" + LOCAL_PATH_INBOX, 1);
	        checkMsgCount(mRemoteMbox, "in:inbox", 0);

	        // Empty local trash
	        checkMsgCount(mLocalMbox, "in:" + LOCAL_PATH_TRASH, 1);
	        ZFolder localTrash = mLocalMbox.getFolderByPath(LOCAL_PATH_TRASH);
	        mLocalMbox.emptyFolder(localTrash.getId());
	        checkMsgCount(mLocalMbox, "in:" + LOCAL_PATH_TRASH, 0);
	        checkMsgCount(mRemoteMbox, "in:trash", 1);
	        importImap();
	        
	        // Make sure that local changes did not propagate to remote server
	        checkMsgCount(mRemoteMbox, "in:inbox msg5", 0);
	        checkMsgCount(mRemoteMbox, "in:trash", 1);
	    }
	    
	    private void checkMsgCount(ZMailbox mbox, String query, int expectedCount)
	    throws Exception {
	        List<ZMessage> msgs = TestUtil.search(mbox, query);
	        assertEquals("Result size for query '" + query + "'", expectedCount, msgs.size());
	    }
	    
	    private void importImap()
	    throws Exception {
	        TestUtil.importDataSource(mDataSource, mLocalMbox, mRemoteMbox);
	    }

	    private void compare()
	    throws Exception {
	        // Recursively compare the folder trees
	        ZFolder folder1 = mRemoteMbox.getUserRoot();
	        ZFolder folder2 = mLocalMbox.getFolderByPath(DS_FOLDER_ROOT);
	        compare(mRemoteMbox, folder1, mLocalMbox, folder2);
	    }
	    
	    private void compare(ZMailbox mbox1, ZFolder folder1, ZMailbox mbox2, ZFolder folder2)
	    throws Exception {
	        assertNotNull(mbox1);
	        assertNotNull(folder1);
	        assertNotNull(mbox2);
	        assertNotNull(folder2);
	        
	        // Recursively compare children
	        for (ZFolder child1 : folder1.getSubFolders()) {
	            if (isMailFolder(child1)) {
	                ZFolder child2 = folder2.getSubFolderByPath(child1.getName());
	                String msg = String.format("Could not find folder %s/%s for %s",
	                    folder2.getPath(), child1.getName(), mbox2.getName());
	                assertNotNull(msg, child2);
	                compare(mbox1, child1, mbox2, child2);
	            }
	        }
	        assertEquals("Message count doesn't match (folder1 = " + folder1 +
	                     ", folder2 = " + folder2 + ")",
	                     folder1.getMessageCount(), folder2.getMessageCount());
	        
	        // Compare folders as long as neither one is the user root
	        if (!(folder1.getPath().equals("/") || folder2.getPath().equals("/"))) {
	            List<ZMessage> msgs1 = TestUtil.search(mbox1, "in:" + folder1.getPath());
	            List<ZMessage> msgs2 = TestUtil.search(mbox2, "in:" + folder2.getPath());
	            compareMessages(msgs1, msgs2);
	        }
	    }

	    private boolean isMailFolder(ZFolder folder) {
	        ZFolder.View view = folder.getDefaultView();
	        return view == null || view == ZFolder.View.message || view == ZFolder.View.conversation;
	    }
	    
	    private void compareMessages(List<ZMessage> msgs1, List<ZMessage> msgs2)
	    throws Exception {
	        // Keep track of message ID's in first set
	        Map<String, ZMessage> msgMap = new HashMap<String, ZMessage>();
	        for (ZMessage msg : msgs1) {
	            msgMap.put(msg.getMessageIdHeader(), msg);
	        }
	        
	        // Compare messages in second set
	        for (ZMessage msg2 : msgs2) {
	            String id = msg2.getMessageIdHeader();
	            ZMessage msg1 = msgMap.remove(id);
	            assertNotNull("Found message '" + msg2.getSubject() + "' in mbox2 but not in mbox1", msg1);
	            assertEquals("Message content", msg1.getContent(), msg2.getContent());
	            String f1 = msg1.getFlags() != null ? msg1.getFlags() : "";
	            String f2 = msg2.getFlags() != null ? msg2.getFlags() : "";
	            assertEquals("Flags for message '" + msg1.getSubject() + "' don't match", f1, f2);
	        }
	        
	        // Fail if there are any remaining messages
	        if (msgMap.size() != 0) {
	            List<String> subjects = new ArrayList<String>();
	            for (ZMessage msg : msgMap.values()) {
	                subjects.add(msg.getSubject());
	            }
	            fail("Found messages in mbox1 but not in mbox2: " + StringUtil.join(",", subjects));
	        }
	    }
	    
	    public void tearDown()
	    throws Exception {
	        cleanUp();
	        TestUtil.setServerAttr(
	            Provisioning.A_zimbraImapCleartextLoginEnabled, mOriginalCleartextValue);
	        LC.javamail_imap_enable_starttls.setDefault(Boolean.toString(mOriginalEnableStarttls));
	    }
	    
	    public void cleanUp()
	    throws Exception {
	        if (TestUtil.accountExists(REMOTE_USER_NAME)) {
	            TestUtil.deleteAccount(REMOTE_USER_NAME);
	        }
	        if (TestUtil.accountExists(LOCAL_USER_NAME)) {
	            TestUtil.deleteAccount(LOCAL_USER_NAME);
	        }
	    }
	    
	    public static void main(String[] args)
	    throws Exception {
	        TestUtil.cliSetup();
	        TestUtil.runTest(TestImapOneWayImport.class);
	    }
}
