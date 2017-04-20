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
import org.junit.Test;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMountpoint;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.mailbox.Mailbox;

public class TestMountpoint {

    private static final String NAME_PREFIX = TestMountpoint.class.getName();
    private static final String USER_NAME = "user1";
    private static final String REMOTE_USER_NAME = "user2";

    @Before
    public void setUp() throws Exception {
        cleanUp();
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        TestUtil.deleteTestData(REMOTE_USER_NAME, NAME_PREFIX);
    }

    /**
     * Tests {@link ZMailbox#getValidFolderIds(String)}.
     */
    @Test
    public void testInvalidMountpoint()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZMailbox remoteMbox = TestUtil.getZMailbox(REMOTE_USER_NAME);
        String remoteFolderPath = "/" + NAME_PREFIX + "-testInvalidMountpoint-remote";
        ZFolder remoteFolder = TestUtil.createFolder(remoteMbox, remoteFolderPath);
        ZMountpoint mountpoint = TestUtil.createMountpoint(remoteMbox, remoteFolderPath, mbox, NAME_PREFIX + "-mountpoint");

        // Test valid mountpoint.
        Set<String> folderIds = new HashSet<String>();
        folderIds.add(mountpoint.getId());
        String inboxId = Integer.toString(Mailbox.ID_FOLDER_INBOX);
        folderIds.add(inboxId);
        String idString = mbox.getValidFolderIds(StringUtil.join(",", folderIds));
        List<String> returnedIds = Arrays.asList(idString.split(","));
        Assert.assertEquals(2, returnedIds.size());
        Assert.assertTrue(returnedIds.contains(inboxId));
        Assert.assertTrue(returnedIds.contains(mountpoint.getId()));
        Assert.assertEquals(1, getNumCommas(idString));

        // Delete remote folder and confirm that the id is no longer returned.
        remoteMbox.deleteFolder(remoteFolder.getId());
        idString = mbox.getValidFolderIds(StringUtil.join(",", folderIds));
        returnedIds = Arrays.asList(idString.split(","));
        Assert.assertEquals(1, returnedIds.size());
        Assert.assertTrue(returnedIds.contains(inboxId));
        Assert.assertEquals(0, getNumCommas(idString));
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
