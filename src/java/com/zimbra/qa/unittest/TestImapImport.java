/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.zimbra.common.util.CliUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.zclient.ZDataSource;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZImapDataSource;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.zclient.ZMessage;
import com.zimbra.cs.zclient.ZMailbox.ZImportStatus;

public class TestImapImport
extends TestCase {
    
    private static final String REMOTE_USER_NAME = "testimapimportremote";
    private static final String LOCAL_USER_NAME = "testimapimportlocal";
    private static final String NAME_PREFIX = "TestImapImport";
    private static final String DS_FOLDER_ROOT = "/" + NAME_PREFIX;
    
    private ZMailbox mRemoteMbox;
    private ZMailbox mLocalMbox;
    private String mOriginalCleartextValue;
    private ZDataSource mDataSource;
    
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
            port, REMOTE_USER_NAME, TestUtil.DEFAULT_PASSWORD, folder.getId(), DataSource.ConnectionType.cleartext); 
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
    }
    
    public void testImapImport()
    throws Exception {
        // Compare mailboxes in their initial state
        importImap();
        compare();
        
        // Add 1 message
        TestUtil.insertMessage(mRemoteMbox, 1, NAME_PREFIX + " 1");
        importImap();
        compare();
        
        // Flag
        List<ZMessage> msgs1 = TestUtil.search(mRemoteMbox, "in:inbox");
        assertEquals("msgs1.size()", 1, msgs1.size());
        mRemoteMbox.flagMessage(msgs1.get(0).getId(), true);
        importImap();
        compare();
        
        // Move to trash
        mRemoteMbox.moveMessage(msgs1.get(0).getId(), Integer.toString(Mailbox.ID_FOLDER_TRASH));
        importImap();
        compare();
        
        // Create folders on both sides
        String remote1 = "/" + NAME_PREFIX + "-remote1";
        String remote2 = remote1 + "/" + NAME_PREFIX + "-remote2";
        String local1 = "/" + NAME_PREFIX + "/" + NAME_PREFIX + "-local1";
        String local2 = local1 + "/" + NAME_PREFIX + "-local2";
        
        TestUtil.createFolder(mRemoteMbox, remote1);
        TestUtil.createFolder(mRemoteMbox, remote2);
        TestUtil.createFolder(mLocalMbox, local1);
        TestUtil.createFolder(mLocalMbox, local2);
        importImap();
        compare();
        
        // Add message to remote folder and delete local folder at the same time
        ZFolder remoteFolder2 = mRemoteMbox.getFolderByPath(remote2); 
        TestUtil.insertMessage(mRemoteMbox, 2, NAME_PREFIX + " 2", remoteFolder2.getId());
        ZFolder localFolder1 = mLocalMbox.getFolderByPath(local1);
        mLocalMbox.deleteFolder(localFolder1.getId());
        importImap();
        compare();
        
        // Add message to a local folder and delete the same folder in remote mailbox
        String path = "/" + NAME_PREFIX + remote2;
        ZFolder localFolder = mLocalMbox.getFolderByPath(path);
        assertNotNull("Could not find local folder " + path, localFolder);
        TestUtil.insertMessage(mLocalMbox, 3, NAME_PREFIX + " 3", localFolder.getId());
        ZFolder remoteFolder1 = mRemoteMbox.getFolderByPath(remote1);
        mRemoteMbox.deleteFolder(remoteFolder1.getId());
        importImap();
        compare();
    }
    
    private void importImap()
    throws Exception {
        List<ZDataSource> list = new ArrayList<ZDataSource>();
        list.add(mDataSource);
        mLocalMbox.importData(list);
        
        // Wait for import to complete
        ZImportStatus status = null;
        while (true) {
            List<ZImportStatus> statusList = mLocalMbox.getImportStatus();
            assertEquals("Unexpected number of imports running", 1, statusList.size());
            status = statusList.get(0);
            assertEquals("Unexpected data source type", status.getType(), DataSource.Type.imap.name());
            if (!status.isRunning()) {
                break;
            }
            Thread.sleep(500);
        }
        assertTrue("Import failed: " + status.getError(), status.getSuccess());
    }

    private void compare()
    throws Exception {
        // Do a no-op, to get the latest state chanages after an IMAP import
        mRemoteMbox.noOp();
        mLocalMbox.noOp();
        
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
        assertEquals("Message count doesn't match", folder1.getMessageCount(), folder2.getMessageCount());
        
        List<ZMessage> msgs1 = TestUtil.search(mbox1, "in:" + folder1.getPath());
        List<ZMessage> msgs2 = TestUtil.search(mbox2, "in:" + folder2.getPath());
        compareMessages(msgs1, msgs2);
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
            assertEquals("Flags don't match", msg1.getFlags(), msg2.getFlags());
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
        // cleanUp();
        TestUtil.setServerAttr(
            Provisioning.A_zimbraImapCleartextLoginEnabled, mOriginalCleartextValue);
    }
    
    public void cleanUp()
    throws Exception {
        if (TestUtil.accountExists(REMOTE_USER_NAME)) {
            TestUtil.deleteTestData(REMOTE_USER_NAME, NAME_PREFIX);
        }
        if (TestUtil.accountExists(LOCAL_USER_NAME)) {
            ZMailbox mbox = TestUtil.getZMailbox(LOCAL_USER_NAME);
            for (ZDataSource ds : mbox.getAllDataSources()) {
                if (ds.getName().contains(NAME_PREFIX)) {
                    mbox.deleteDataSource(ds);
                }
            }

            TestUtil.deleteTestData(LOCAL_USER_NAME, NAME_PREFIX);
        }
    }
    
    /**
     * Separate class for teardown, since adding/deleting the mailboxes
     * takes a long time.
     */
    public static class TearDown
    extends TestCase {
        
        public void testTeardown()
        throws Exception {
            if (TestUtil.accountExists(LOCAL_USER_NAME)) {
                TestUtil.deleteAccount(LOCAL_USER_NAME);
            }
            if (TestUtil.accountExists(REMOTE_USER_NAME)) {
                TestUtil.deleteAccount(REMOTE_USER_NAME);
            }
        }
    }
    
    public static void main(String[] args)
    throws Exception {
        CliUtil.toolSetup();
        TestUtil.runTest(new TestSuite(TestImapImport.class), null);
        // TestUtil.runTest(new TestSuite(TestImapImport.TearDown.class), null);
    }
}
