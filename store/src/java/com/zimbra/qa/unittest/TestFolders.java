/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Strings;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;

public class TestFolders {
    private static String USER_NAME = TestFolders.class.getSimpleName();

    private static String mOriginalEmptyFolderBatchSize;


    @BeforeClass
    public static void init() throws Exception {
        mOriginalEmptyFolderBatchSize = TestUtil.getServerAttr(Provisioning.A_zimbraMailEmptyFolderBatchSize);
    }

    @Before
    public void setUp() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.createAccount(USER_NAME);
    }

    @Test
    public void testEmptyLargeFolder() throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraMailEmptyFolderBatchSize, Integer.toString(3));

        // Create folders.
        String parentPath = "/parent";
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder parent = TestUtil.createFolder(mbox, parentPath);
        ZFolder child = TestUtil.createFolder(mbox, parent.getId(), "child");

        // Add messages.
        for (int i = 1; i <= 5; i++) {
            TestUtil.addMessage(mbox, "parent " + i, parent.getId());
            TestUtil.addMessage(mbox, "child " + i, child.getId());
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
            TestUtil.addMessage(mbox, "parent " + i, parent.getId());
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

    @Ignore
    @Test
    public void testCreateManyFolders() throws Exception {
        //normally skip this test since it takes a long time to complete. just keep it around for quick perf checks
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String parentPath = "/parent";

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

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
    }

    @AfterClass
    public static void shutdown() throws Exception {
        TestUtil.setServerAttr(Provisioning.A_zimbraMailEmptyFolderBatchSize, mOriginalEmptyFolderBatchSize);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestFolders.class);
    }
}
