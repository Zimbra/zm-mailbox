/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import junit.framework.TestCase;

import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;
import com.zimbra.cs.account.Provisioning;

public class TestFolders extends TestCase
{
    private static String USER_NAME = "user1";
    private static String NAME_PREFIX = "TestFolders";

    private String mOriginalEmptyFolderBatchSize;

    @Override
    protected void setUp()
    throws Exception {
        cleanUp();
        mOriginalEmptyFolderBatchSize = TestUtil.getServerAttr(Provisioning.A_zimbraMailEmptyFolderBatchSize);
    }


    public void testEmptyLargeFolder()
    throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraMailEmptyFolderBatchSize, Integer.toString(3));
        
        // Create folders.
        String parentPath = "/" + NAME_PREFIX + "-parent";
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder parent = TestUtil.createFolder(mbox, parentPath);
        ZFolder child = TestUtil.createFolder(mbox, parent.getId(), "child");
        
        // Add messages.
        for (int i = 1; i <= 5; i++) {
            TestUtil.addMessage(mbox, NAME_PREFIX + " parent " + i, parent.getId());
            TestUtil.addMessage(mbox, NAME_PREFIX + " child " + i, child.getId());
        }
        mbox.noOp();
        assertEquals(5, parent.getMessageCount());
        assertEquals(5, child.getMessageCount());

        // Empty parent folder without deleting subfolders.
        mbox.emptyFolder(parent.getId(), false);
        mbox.noOp();
        assertEquals(0, parent.getMessageCount());
        assertEquals(5, child.getMessageCount());
        
        // Add more messages to the parent folder.
        for (int i = 6; i <= 10; i++) {
            TestUtil.addMessage(mbox, NAME_PREFIX + " parent " + i, parent.getId());
        }
        mbox.noOp();
        assertEquals(5, parent.getMessageCount());
        assertEquals(5, child.getMessageCount());
        
        // Empty parent folder and delete subfolders.
        String childPath = child.getPath();
        assertNotNull(mbox.getFolderByPath(childPath));
        mbox.emptyFolder(parent.getId(), true);
        mbox.noOp();
        assertEquals(0, parent.getMessageCount());
        assertNull(mbox.getFolderByPath(childPath));
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder f = mbox.getFolderByPath("/f1");
        if (f != null) {
            mbox.deleteFolder(f.getId());
        }
    }

    @Override
    protected void tearDown() throws Exception {
        cleanUp();
        TestUtil.setServerAttr(Provisioning.A_zimbraMailEmptyFolderBatchSize, mOriginalEmptyFolderBatchSize);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestFolders.class);
    }
}
