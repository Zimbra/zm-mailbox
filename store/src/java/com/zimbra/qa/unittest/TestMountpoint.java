/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMountpoint;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailbox.Mailbox;

public class TestMountpoint {

    @Rule
    public TestName testInfo = new TestName();

    private static final String NAME_PREFIX = TestMountpoint.class.getName();
    private static String USER_NAME = "user1";
    private static String REMOTE_USER_NAME = "user2";

    @Before
    public void setUp() throws Exception {
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        USER_NAME = prefix + "user1";
        REMOTE_USER_NAME = prefix + "remoteuser";
        tearDown();
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(REMOTE_USER_NAME);
    }

    /**
     * Tests {@link ZMailbox#getValidFolderIds(String)}.
     */
    @Test
    public void testInvalidMountpoint()
    throws Exception {
        TestUtil.createAccount(USER_NAME);
        TestUtil.createAccount(REMOTE_USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZMailbox remoteMbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        String remoteFolderPath = "/" + NAME_PREFIX + "-testInvalidMountpoint-remote";
        ZFolder remoteFolder = TestUtil.createFolder(remoteMbox, remoteFolderPath);
        ZMountpoint mountpoint = TestUtil.createMountpoint(
                remoteMbox, remoteFolderPath, mbox, NAME_PREFIX + "-mountpoint");

        // Test valid mountpoint.
        Set<String> folderIds = new HashSet<String>();
        folderIds.add(mountpoint.getId());
        String inboxId = Integer.toString(Mailbox.ID_FOLDER_INBOX);
        folderIds.add(inboxId);
        String idString = mbox.getValidFolderIds(StringUtil.join(",", folderIds));
        List<String> returnedIds = Arrays.asList(idString.split(","));
        Assert.assertEquals("Number of return IDs from mbox.getValidFolderIds", 2, returnedIds.size());
        Assert.assertTrue("Returned IDs should contain ID of inbox", returnedIds.contains(inboxId));
        Assert.assertTrue("Returned IDs should contain ID of mountpoint", returnedIds.contains(mountpoint.getId()));
        Assert.assertEquals("Should be 1 comma in string returned by mbox.getValidFolderIds", 1, getNumCommas(idString));

        // Delete remote folder and confirm that the id is no longer returned.
        remoteMbox.deleteFolder(remoteFolder.getId());
        idString = mbox.getValidFolderIds(StringUtil.join(",", folderIds));
        returnedIds = Arrays.asList(idString.split(","));
        Assert.assertEquals("Number of return IDs from mbox.getValidFolderIds after mountpoint delete",
                1, returnedIds.size());
        Assert.assertTrue("Returned IDs should contain ID of inbox after mp delete", returnedIds.contains(inboxId));
        Assert.assertEquals("Should no commas in string returned by mbox.getValidFolderIds after mp delete",
                0, getNumCommas(idString));
    }

    private int getNumCommas(String s) {
        int numCommas = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == ',') {
                numCommas++;
            }
        }
        return numCommas;
    }
    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestMountpoint.class);
    }
}
