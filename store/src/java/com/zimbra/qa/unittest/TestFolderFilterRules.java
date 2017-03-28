/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.client.ZFilterRule;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMessage;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

public class TestFolderFilterRules {

    private static String NAME_PREFIX = "TestFolderFilterRules";
    private static String USER_NAME = NAME_PREFIX + "-user1";
    
    private static String FOLDER1_NAME = "folder1";
    private static String FOLDER2_NAME = "folder2";
    private static String FOLDER3_NAME = "folder3";
    private static String FOLDER4_NAME = "folder4";
    
    private static String SUBJECT1 = NAME_PREFIX + " 1";
    private static String SUBJECT2 = NAME_PREFIX + " 2";
    private static String SUBJECT3 = NAME_PREFIX + " 3";
    private static String SUBJECT4 = NAME_PREFIX + " 4";

    private ZFolder mFolder1;
    private ZFolder mFolder2;
    private ZFolder mFolder3;
    private ZFolder mFolder4;

    private Account account;

    private static String localServer = null;

    @BeforeClass
    public static void beforeClass() throws Exception {
        localServer = Provisioning.getInstance().getLocalServer().getServiceHostname();
    }
    /**
     * Creates the following folder hierarchies:
     * <ul>
     *    <li>/1/2/3</li>
     *    <li>/4</li>
     * </ul>
     */
    @Before
    public void setUp() throws Exception {
        cleanUp();
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraMailHost, localServer);
        attrs.put(Provisioning.A_zimbraMailSieveScript, FILTER_RULES);
        account = TestUtil.createAccount(USER_NAME, attrs);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        mFolder1 = TestUtil.createFolder(mbox, FOLDER1_NAME);
        mFolder2 = TestUtil.createFolder(mbox, mFolder1.getId(), FOLDER2_NAME);
        mFolder3 = TestUtil.createFolder(mbox, mFolder2.getId(), FOLDER3_NAME);
        mFolder4 = TestUtil.createFolder(mbox, FOLDER4_NAME);
    }
    
    /**
     * Tests filtering to folders without changes.
     */
    @Test
    public void testDefault() throws Exception {
        sendMessages();
    }

    /**
     * Tests renaming a leaf folder.
     */
    @Test
    public void testRenameLeaf() throws Exception {
        renameFolder(mFolder3.getId(), NAME_PREFIX + "New3", null);
        sendMessages();
    }

    /**
     * Tests renaming the root folder, which isn't referenced with a leading
     * 
     * @throws Exception
     */
    @Test
    public void testRenameRoot() throws Exception {
        renameFolder(mFolder1.getId(), NAME_PREFIX + "New1", null);
        sendMessages();
    }
    
    /**
     * Tests moving a leaf folder.
     */
    @Test
    public void testMoveLeaf() throws Exception {
        moveFolder(mFolder3.getId(), mFolder4.getId());
        sendMessages();
    }

    /**
     * Tests moving a parent folder.
     */
    @Test
    public void testMoveParent() throws Exception {
        moveFolder(mFolder2.getId(), mFolder4.getId());
        sendMessages();
    }
    
    /**
     * Tests moving to a new parent folder and renaming at the same time. 
     */
    @Test
    public void testMoveAndRename() throws Exception {
        renameFolder(mFolder2.getId(), NAME_PREFIX + "New2", mFolder4.getId());
        sendMessages();
    }
    
    /**
     * Tests moving to a new parent folder and changing the folder name
     * to upper-case.
     */
    @Test
    public void testMoveAndChangeCase() throws Exception {
        String newName = mFolder2.getName().toUpperCase();
        renameFolder(mFolder2.getId(), newName, mFolder4.getId());
        sendMessages();
    }
    
    /**
     * Confirms that when a folder is deleted, any rules that filed into that
     * folder or its subfolders are disabled (bug 17797).
     */
    @Test
    public void testDeleteFolder() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        mbox.deleteFolder(mFolder2.getId());
        
        // Deliver messages that used to match rules 2 and 3, and make sure
        // that they get delivered to inbox.
        TestUtil.addMessageLmtp(SUBJECT2, USER_NAME, USER_NAME);
        TestUtil.getMessage(mbox, "in:inbox subject:\"" + SUBJECT2 + "\"");
        TestUtil.addMessageLmtp(SUBJECT3, USER_NAME, USER_NAME);
        TestUtil.getMessage(mbox, "in:inbox subject:\"" + SUBJECT3 + "\"");
        
        // Confirm that rules for folders 2 and 3 are disabled.
        List<ZFilterRule> rules = mbox.getIncomingFilterRules(true).getRules();
        assertEquals(4, rules.size());
        for (ZFilterRule rule : rules) {
            if (rule.getName().equals("Folder 1")) {
                assertTrue(rule.isActive());
            } else if (rule.getName().equals("Folder 2")) {
                assertFalse(rule.isActive());
            } else if (rule.getName().equals("Folder 3")) {
                assertFalse(rule.isActive());
            } else if (rule.getName().equals("Folder 4")) {
                assertTrue(rule.isActive());
            } else {
                fail("Unexpected rule name: " + rule.getName());
            }
        }
    }
    
    /**
     * Confirms that when a folder moved to the trash, any rules that filed into that
     * folder or its subfolders are disabled (bug 17797).
     */
    @Test
    public void testMoveFolderToTrash() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        mbox.trashFolder(mFolder2.getId());
        
        // Deliver messages that used to match rules 2 and 3, and make sure
        // that they get delivered to inbox.
        TestUtil.addMessageLmtp(SUBJECT2, USER_NAME, USER_NAME);
        TestUtil.getMessage(mbox, "in:inbox subject:\"" + SUBJECT2 + "\"");
        TestUtil.addMessageLmtp(SUBJECT3, USER_NAME, USER_NAME);
        TestUtil.getMessage(mbox, "in:inbox subject:\"" + SUBJECT3 + "\"");
        
        // Confirm that rules for folders 2 and 3 are disabled.
        List<ZFilterRule> rules = mbox.getIncomingFilterRules(true).getRules();
        assertEquals(4, rules.size());
        for (ZFilterRule rule : rules) {
            if (rule.getName().equals("Folder 1")) {
                assertTrue(rule.isActive());
            } else if (rule.getName().equals("Folder 2")) {
                assertFalse(rule.isActive());
            } else if (rule.getName().equals("Folder 3")) {
                assertFalse(rule.isActive());
            } else if (rule.getName().equals("Folder 4")) {
                assertTrue(rule.isActive());
            } else {
                fail("Unexpected rule name: " + rule.getName());
            }
        }
    }
    
    /**
     * Sends messages and verifies that they got filtered into the correct folders.
     */
    private void sendMessages()
    throws Exception {
        verifyFolderSize(mFolder1.getId(), 0);
        TestUtil.addMessageLmtp(SUBJECT1, USER_NAME, USER_NAME);
        verifyFolderSize(mFolder1.getId(), 1);
        
        verifyFolderSize(mFolder2.getId(), 0);
        TestUtil.addMessageLmtp(SUBJECT2, USER_NAME, USER_NAME);
        verifyFolderSize(mFolder2.getId(), 1);
        
        verifyFolderSize(mFolder3.getId(), 0);
        TestUtil.addMessageLmtp(SUBJECT3, USER_NAME, USER_NAME);
        verifyFolderSize(mFolder3.getId(), 1);
        
        verifyFolderSize(mFolder4.getId(), 0);
        TestUtil.addMessageLmtp(SUBJECT4, USER_NAME, USER_NAME);
        verifyFolderSize(mFolder4.getId(), 1);
    }

    /**
     * Verifies message count for the given folder.
     */
    private void verifyFolderSize(String folderId, int size)
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder f = mbox.getFolderById(folderId);
        List<ZMessage> messages = TestUtil.search(mbox, "in:" + f.getPath());
        assertEquals("Incorrect message count for folder " + f.getPath(), size, messages.size());
    }
    
    /**
     * Renames the given folder and confirms that filter rules were updated
     * with the new path.
     */
    private void renameFolder(String folderId, String newName, String newParentId)
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder = mbox.getFolderById(folderId);
        
        // Confirm that the old path is in the script.
        String oldPath = folder.getPath();
        if (oldPath.charAt(0) == '/') {
            // Path in scripts may not have a leading slash.  
            oldPath = oldPath.substring(1, oldPath.length());
        }
        String script = account.getAttr(Provisioning.A_zimbraMailSieveScript);
        assertTrue("Could not find path " + oldPath + " in script: " + script, script.contains(oldPath));
        
        // Rename the folder and check the new path.
        mbox.renameFolder(folderId, newName, newParentId);
        folder = mbox.getFolderById(folder.getId());
        String newPath = folder.getPath();
        if (newPath.charAt(0) == '/') {
            newPath = newPath.substring(1, newPath.length());
        }
        assertEquals(newName, folder.getName());
        assertTrue("Folder path '" + newPath + "' does not end with " + newName,
            folder.getPath().endsWith(newName));

        // Confirm that filter rules are updated.
        account = TestUtil.getAccount(USER_NAME); // refresh
        script = account.getAttr(Provisioning.A_zimbraMailSieveScript);
        assertFalse("Found old path '" + oldPath + " in script: " + script, script.indexOf(oldPath) >= 0);
        assertTrue("Could not find new path '" + newPath + " in script: " + script, script.indexOf(newPath) >= 0);
    }
    
    private void moveFolder(String folderId, String newParentFolderId)
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder = mbox.getFolderById(folderId);
        ZFolder newParent = mbox.getFolderById(newParentFolderId); 
        String newParentPath = newParent.getPath();
        String oldPath = folder.getPath();
        if (oldPath.charAt(0) == '/') {
            // Path in scripts may not have a leading slash  
            oldPath = oldPath.substring(1, oldPath.length());
        }

        // Confirm that the old path is in the script.
        String script = account.getAttr(Provisioning.A_zimbraMailSieveScript);
        assertTrue("Could not find path " + oldPath + " in script: " + script, script.contains(oldPath));
        
        mbox.moveFolder(folderId, newParentFolderId);
        folder = mbox.getFolderById(folder.getId());
        String newPath = folder.getPath();
        assertFalse("Path '" + oldPath + "' did not change", newPath.equals(oldPath));
        assertTrue("Folder path '" + newPath + "' does not start with " + newParentPath,
            folder.getPath().startsWith(newParentPath));

        // Confirm that filter rules are updated
        account = TestUtil.getAccount(USER_NAME); // refresh
        script = account.getAttr(Provisioning.A_zimbraMailSieveScript);
        assertFalse("Found old path '" + oldPath + " in script: " + script, script.indexOf(oldPath) >= 0);
        assertTrue("Could not find new path '" + newPath + " in script: " + script, script.indexOf(newPath) >= 0);
    }

    private void cleanUp() throws Exception {
        if(TestUtil.accountExists(USER_NAME)) {
            TestUtil.deleteAccount(USER_NAME);
        }
    }

    @After
    public void tearDown() throws Exception {
        cleanUp();
    }

    private static final String FILTER_RULES = StringUtil.join("\n", new String[] {
        "require [\"fileinto\", \"reject\", \"tag\", \"flag\"];",
        "",
        "# Folder 1",
        "if anyof (header :is \"subject\" \"" + SUBJECT1 + "\" )",
        "{",
        "    fileinto \"" + FOLDER1_NAME + "\";",
        "    stop;",
        "}",
        "",
        "# Folder 2",
        "if anyof (header :is \"subject\" \"" + SUBJECT2 + "\" )",
        "{",
        "    fileinto \"/" + FOLDER1_NAME + "/" + FOLDER2_NAME + "\";",
        "    stop;",
        "}",
        "",
        "# Folder 3",
        "if anyof (header :is \"subject\" \"" + SUBJECT3 + "\" )",
        "{",
        "    fileinto \"" + FOLDER1_NAME + "/" + FOLDER2_NAME + "/" + FOLDER3_NAME + "\";",
        "    stop;",
        "}",
        "",
        "# Folder 4",
        "if anyof (header :is \"subject\" \"" + SUBJECT4 + "\" )",
        "{",
        "    fileinto \"/" + FOLDER4_NAME + "\";",
        "    stop;",
        "}"
    });

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestFolderFilterRules.class);
    }
}
