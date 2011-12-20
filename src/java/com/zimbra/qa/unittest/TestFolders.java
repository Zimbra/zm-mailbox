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

import com.google.common.base.Strings;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
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
    
    public void testCreateManyFolders() throws Exception {
        //normally skip this test since it takes a long time to complete. just keep it around for quick perf checks
        boolean skip = true;
        if (skip) {
            return;
        }

        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String parentPath = "/" + NAME_PREFIX + "-parent";

        ZFolder parent = TestUtil.createFolder(mbox, parentPath);
        int max = 10000;
        int pad = ((int) Math.log10(max))+1;
        long totalTime = 0;
        double avgTime;
        long maxTime = 0;
        for (int i = 0; i < max; i++) {
            long start = System.currentTimeMillis();
            TestUtil.createFolder(mbox, parent.getId(), "child"+Strings.padStart(i+"", pad, '0'));
            long end = System.currentTimeMillis();
            long elapsed = (end-start);
            totalTime+=elapsed;
            if (elapsed > maxTime) {
                maxTime = elapsed;
                ZimbraLog.mailbox.info("FOLDER TIME new max time %dms at index %d",maxTime, i);
            }
            if (i > 0 && (i % 100 == 0 || i == max-1)) {
                avgTime = (totalTime*1.0)/(i*1.0);
                ZimbraLog.mailbox.info("FOLDER TIME average after %d = %dms", i, Math.round(avgTime));
            }
        }
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
