/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.qa.unittest;

import java.util.List;

import junit.framework.TestCase;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.common.util.StringUtil;


public class TestFolderFilterRules
extends TestCase {

    private static String USER_NAME = "user1";

    private static String NAME_PREFIX = "TestFolderFilterRules";
    private static String FOLDER1 = NAME_PREFIX + "1";
    private static String FOLDER2 = NAME_PREFIX + "2";
    private static String FOLDER3 = NAME_PREFIX + "3";
    private static String FOLDER4 = NAME_PREFIX + "4";

    private int mFolder1Id;
    private int mFolder2Id;
    private int mFolder3Id;
    private int mFolder4Id;
    
    private String mOriginalRules;
    
    /**
     * Creates the following folder hierarchies:
     * <ul>
     *    <li>/1/2/3</li>
     *    <li>/4</li>
     * </ul>
     */
    public void setUp()
    throws Exception {
        super.setUp();
        cleanUp();

        Folder f = createFolder(FOLDER1, Mailbox.ID_FOLDER_USER_ROOT);
        mFolder1Id = f.getId();
        f = createFolder(FOLDER2, mFolder1Id);
        mFolder2Id = f.getId();
        f = createFolder(FOLDER3, mFolder2Id);
        mFolder3Id = f.getId();
        f = createFolder(FOLDER4, Mailbox.ID_FOLDER_USER_ROOT);
        mFolder4Id = f.getId();

        // Remember original rules and set rules for this test
        RuleManager rm = RuleManager.getInstance();
        Account account = TestUtil.getAccount(USER_NAME);
        mOriginalRules = rm.getRules(account);
        rm.setRules(account, FILTER_RULES);
    }
    
    private Folder createFolder(String folderName, int parentId)
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder f =
            mbox.createFolder(null, folderName, parentId, MailItem.TYPE_UNKNOWN, 0, MailItem.DEFAULT_COLOR, null);
        return f;
    }

    /**
     * Tests filtering to folders without changes.
     */
    public void testDefault()
    throws Exception {
        sendMessages();
    }

    /**
     * Tests renaming a leaf folder.
     */
    public void testRenameLeaf()
    throws Exception {
        renameFolder(mFolder3Id, NAME_PREFIX + "New3");
        sendMessages();
    }

    /**
     * Tests renaming a parent folder.
     */
    public void testRenameParent()
    throws Exception {
        renameFolder(mFolder2Id, NAME_PREFIX + "New2");
        sendMessages();
    }
    
    /**
     * Tests moving a leaf folder.
     */
    public void testMoveLeaf()
    throws Exception {
        moveFolder(mFolder3Id, mFolder4Id);
        sendMessages();
    }

    /**
     * Tests moving a parent folder.
     */
    public void testMoveParent()
    throws Exception {
        moveFolder(mFolder2Id, mFolder4Id);
        sendMessages();
    }
    
    /**
     * Sends messages and verifies that they got filtered into the correct folders.
     */
    private void sendMessages()
    throws Exception {
        verifyFolderSize(mFolder1Id, 0);
        TestUtil.addMessageLmtp(1, FOLDER1, USER_NAME, USER_NAME);
        verifyFolderSize(mFolder1Id, 1);
        
        verifyFolderSize(mFolder2Id, 0);
        TestUtil.addMessageLmtp(2, FOLDER2, USER_NAME, USER_NAME);
        verifyFolderSize(mFolder2Id, 1);
        
        verifyFolderSize(mFolder3Id, 0);
        TestUtil.addMessageLmtp(3, FOLDER3, USER_NAME, USER_NAME);
        verifyFolderSize(mFolder3Id, 1);
        
        verifyFolderSize(mFolder4Id, 0);
        TestUtil.addMessageLmtp(4, FOLDER4, USER_NAME, USER_NAME);
        verifyFolderSize(mFolder4Id, 1);
    }

    /**
     * Verifies message count for the given folder.
     */
    private void verifyFolderSize(int folderId, int size)
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder f = mbox.getFolderById(null, folderId);
        List<Integer> ids = TestUtil.search(mbox, "in:" + f.getPath(), MailItem.TYPE_MESSAGE);
        assertEquals("Incorrect message count for folder " + f.getPath(), size, ids.size());
    }
    
    private void renameFolder(int folderId, String newName)
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder folder = mbox.getFolderById(null, folderId);
        String oldPath = folder.getPath();
        
        mbox.rename(null, folderId, MailItem.TYPE_FOLDER, newName);
        folder = mbox.getFolderById(null, folder.getId());
        String newPath = folder.getPath();
        assertEquals(newName, folder.getName());
        assertTrue("Folder path '" + newPath + "' does not end with " + newName,
            folder.getPath().endsWith(newName));

        // Confirm that filter rules are updated
        RuleManager rm = RuleManager.getInstance();
        Account account = TestUtil.getAccount(USER_NAME);
        String rules = rm.getRules(account);
        assertFalse("Found old path '" + oldPath + " in rules: " + rules, rules.indexOf(oldPath) >= 0);
        assertTrue("Could not find new path '" + newPath + " in rules: " + rules, rules.indexOf(newPath) >= 0);
    }
    
    private void moveFolder(int folderId, int newParentFolderId)
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        Folder folder = mbox.getFolderById(null, folderId);
        Folder newParent = mbox.getFolderById(null, newParentFolderId); 
        String newParentPath = newParent.getPath();
        String oldPath = folder.getPath();
        
        mbox.move(null, folderId, MailItem.TYPE_FOLDER, newParentFolderId);
        folder = mbox.getFolderById(null, folder.getId());
        String newPath = folder.getPath();
        assertFalse("Path '" + oldPath + "' did not change", newPath.equals(oldPath));
        assertTrue("Folder path '" + newPath + "' does not start with " + newParentPath,
            folder.getPath().startsWith(newParentPath));

        // Confirm that filter rules are updated
        RuleManager rm = RuleManager.getInstance();
        Account account = TestUtil.getAccount(USER_NAME);
        String rules = rm.getRules(account);
        assertFalse("Found old path '" + oldPath + " in rules: " + rules, rules.indexOf(oldPath) >= 0);
        assertTrue("Could not find new path '" + newPath + " in rules: " + rules, rules.indexOf(newPath) >= 0);
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
    }
    
    protected void tearDown() throws Exception {
        // Restore original rules
        RuleManager rm = RuleManager.getInstance();
        Account account = TestUtil.getAccount(USER_NAME);
        rm.setRules(account, mOriginalRules);
        
        cleanUp();
        super.tearDown();
    }

    private static final String FILTER_RULES = StringUtil.join("\n", new String[] {
        "require [\"fileinto\", \"reject\", \"tag\", \"flag\"];",
        "",
        "# Folder 1",
        "if anyof (header :is \"subject\" \"" + FOLDER1 + "\" )",
        "{",
        "    fileinto \"/" + FOLDER1 + "\";",
        "    stop;",
        "}",
        "",
        "# Folder 2",
        "if anyof (header :is \"subject\" \"" + FOLDER2 + "\" )",
        "{",
        "    fileinto \"/" + FOLDER1 + "/" + FOLDER2 + "\";",
        "    stop;",
        "}",
        "",
        "# Folder 3",
        "if anyof (header :is \"subject\" \"" + FOLDER3 + "\" )",
        "{",
        "    fileinto \"/" + FOLDER1 + "/" + FOLDER2 + "/" + FOLDER3 + "\";",
        "    stop;",
        "}",
        "",
        "# Folder 4",
        "if anyof (header :is \"subject\" \"" + FOLDER4 + "\" )",
        "{",
        "    fileinto \"/" + FOLDER4 + "\";",
        "    stop;",
        "}"
    });
}
