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
    
    private static final String USER1_NAME = "testimapimport1";
    private static final String USER2_NAME = "testimapimport2";
    private static final String NAME_PREFIX = "TestImapImport";
    private static final String FOLDER_PATH = "/" + NAME_PREFIX;
    
    private ZMailbox mMbox1;
    private ZMailbox mMbox2;
    private String mOriginalCleartextValue;
    private ZDataSource mDataSource;
    
    
    public void setUp()
    throws Exception {
        cleanUp();
        
        // Get mailbox references
        if (!TestUtil.accountExists(USER1_NAME)) {
            TestUtil.createAccount(USER1_NAME);
        }
        if (!TestUtil.accountExists(USER2_NAME)) {
            TestUtil.createAccount(USER2_NAME);
        }
        mMbox1 = TestUtil.getZMailbox(USER1_NAME);
        mMbox2 = TestUtil.getZMailbox(USER2_NAME);
        
        // Get or create folder
        ZFolder folder = mMbox2.getFolderByPath(FOLDER_PATH);
        if (folder == null) {
            folder = mMbox2.createFolder(
                Integer.toString(Mailbox.ID_FOLDER_USER_ROOT), NAME_PREFIX, null, null, null, null);
        }
        
        // Create data source
        int port = Integer.parseInt(TestUtil.getServerAttr(Provisioning.A_zimbraImapBindPort));
        mDataSource = new ZImapDataSource(NAME_PREFIX, true, "localhost",
            port, USER1_NAME, TestUtil.DEFAULT_PASSWORD, folder.getId(), DataSource.ConnectionType.cleartext); 
        String id = mMbox2.createDataSource(mDataSource);
        mDataSource = null;
        for (ZDataSource ds : mMbox2.getAllDataSources()) {
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
        // Compare empty mailboxes
        List<ZMessage> msgs1 = TestUtil.search(mMbox1, "in:inbox");
        List<ZMessage> msgs2 = TestUtil.search(mMbox2, "in:" + FOLDER_PATH);
        assertEquals("msgs1.size()", 0, msgs1.size());
        assertEquals("msgs2.size()", 0, msgs2.size());
        
        // Add 1 message
        TestUtil.insertMessage(mMbox1, 1, NAME_PREFIX + " " + 1);
        msgs1 = TestUtil.search(mMbox1, "in:inbox");
        msgs2 = TestUtil.search(mMbox2, "in:" + FOLDER_PATH);
        assertEquals("msgs1.size()", 1, msgs1.size());
        importImap();
        msgs2 = TestUtil.search(mMbox2, "in:" + FOLDER_PATH);
        assertEquals("msgs2.size()", 1, msgs2.size());
        compareMessages(msgs1, msgs2);
        
        // Flag
        assertFalse("msg2 is flagged", msgs2.get(0).isFlagged());
        mMbox1.flagMessage(msgs1.get(0).getId(), true);
        importImap();
        msgs2 = TestUtil.search(mMbox2, "in:" + FOLDER_PATH);
        assertTrue("msg2 is not flagged", msgs2.get(0).isFlagged());
        
        // Move to trash
        mMbox1.moveMessage(msgs1.get(0).getId(), Integer.toString(Mailbox.ID_FOLDER_TRASH));
        msgs1 = TestUtil.search(mMbox1, "in:inbox");
        assertEquals("msgs1.size()", 0, msgs1.size());
        importImap();
        msgs2 = TestUtil.search(mMbox2, "in:" + FOLDER_PATH);
        assertEquals("msgs2.size()", 0, msgs2.size());
    }
    
    private void importImap()
    throws Exception {
        List<ZDataSource> list = new ArrayList<ZDataSource>();
        list.add(mDataSource);
        mMbox2.importData(list);
        
        // Wait for import to complete
        ZImportStatus status = null;
        while (true) {
            List<ZImportStatus> statusList = mMbox2.getImportStatus();
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
        
        // Handle any remaining messages
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
    }
    
    public void cleanUp()
    throws Exception {
        if (TestUtil.accountExists(USER1_NAME)) {
            TestUtil.deleteTestData(USER1_NAME, NAME_PREFIX);
        }
        if (TestUtil.accountExists(USER2_NAME)) {
            ZMailbox mbox = TestUtil.getZMailbox(USER2_NAME);
            for (ZDataSource ds : mbox.getAllDataSources()) {
                if (ds.getName().contains(NAME_PREFIX)) {
                    mbox.deleteDataSource(ds);
                }
            }

            TestUtil.deleteTestData(USER2_NAME, NAME_PREFIX);
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
            if (TestUtil.accountExists(USER1_NAME)) {
                TestUtil.deleteAccount(USER1_NAME);
            }
            if (TestUtil.accountExists(USER2_NAME)) {
                TestUtil.deleteAccount(USER2_NAME);
            }
        }
    }
    
    public static void main(String[] args)
    throws Exception {
        CliUtil.toolSetup();
        TestUtil.runTest(new TestSuite(TestImapImport.class), null);
        TestUtil.runTest(new TestSuite(TestImapImport.TearDown.class), null);
    }
}
