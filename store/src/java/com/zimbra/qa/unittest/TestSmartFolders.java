/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZSmartFolder;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.SmartFolder;

public class TestSmartFolders {
    private static final String USER_NAME = TestSmartFolders.class.getSimpleName();
    private Mailbox mbox;
    private ZMailbox zmbox;

    @Before
    public void setUp() throws Exception {
        cleanUp();
        mbox = TestUtil.getMailbox(USER_NAME);
        zmbox = TestUtil.getZMailbox(USER_NAME);
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    private void cleanUp() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
    }

    private Map<String, ZSmartFolder> getSmartFolders() throws Exception {
        Map<String, ZSmartFolder> tagMap = new HashMap<>();
        for (ZSmartFolder sf: zmbox.getSmartFolders()) {
            tagMap.put(sf.getName(), sf);
        }
        return tagMap;
    }

    @Test
    public void testSmartFolders() throws Exception {
        String smartFolderName1 = "finance";
        String smartFolderName2 = "important";
        String tagName = "testTag";
        SmartFolder smartFolder1 = mbox.createSmartFolder(null, smartFolderName1);
        SmartFolder smartFolder2 = mbox.createSmartFolder(null, smartFolderName2);
        mbox.createTag(null, tagName, (byte)0); //shouldn't be included in response
        Map<String, ZSmartFolder> sfMap = getSmartFolders();
        assertEquals("should see 2 SmartFolders", 2, sfMap.size());
        assertTrue("should see SmartFolder finance", sfMap.containsKey(smartFolderName1));
        assertTrue("should see SmartFolder important", sfMap.containsKey(smartFolderName2));
        assertEquals("incorrect finance SmartFolder ID", String.valueOf(smartFolder1.getId()), sfMap.get(smartFolderName1).getId());
        assertEquals("incorrect important SmartFolder ID", String.valueOf(smartFolder2.getId()), sfMap.get(smartFolderName2).getId());
   }

    private ZSmartFolder getZSmartFolder(SmartFolder sf) throws Exception {
        return zmbox.getSmartFolderById(String.valueOf(sf.getId()));
    }

    @Test
    public void testNotifications() throws Exception {
        SmartFolder sf1 = mbox.createSmartFolder(null, "testfolder");

        //test modifications
        Message msg = TestUtil.addMessage(mbox, "smartfolder test");
        assertEquals("unread count should be 0", 0, getZSmartFolder(sf1).getUnreadCount());
        mbox.alterTag(null, msg.getId(), Type.MESSAGE, sf1.getName(), true, null);
        zmbox.getSmartFolderById(String.valueOf(sf1.getId())).getUnreadCount();
        assertEquals("unread count should be 0", 0, getZSmartFolder(sf1).getUnreadCount());
        zmbox.noOp();
        assertEquals("unread count should be 1", 1, getZSmartFolder(sf1).getUnreadCount());

        //test creation of a second folder, since the first folder triggers the initial populateSmartFolderCache() call
        SmartFolder sf2 = mbox.createSmartFolder(null, "testfolder2");
        assertEquals("zmailbox should not be aware of testfolder2", null, getZSmartFolder(sf2));
        zmbox.noOp();
        assertEquals("zmailbox should not be aware of testfolder2", String.valueOf(sf2.getId()), getZSmartFolder(sf2).getId());

        //test deletion
        mbox.delete(null, sf1.getId(), Type.SMARTFOLDER);
        assertFalse("smartfolder should be not deleted", zmbox.getSmartFolderById(String.valueOf(sf1.getId())) == null);
        zmbox.noOp();
        assertTrue("smartfolder should be deleted", zmbox.getSmartFolderById(String.valueOf(sf1.getId())) == null);
    }
}
