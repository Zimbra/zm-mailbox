/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZFolder;
import com.zimbra.client.ZMailbox;
import com.zimbra.common.mailbox.Color;
import com.zimbra.common.service.ServiceException;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.DevNullOutputStream;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.redolog.RedoPlayer;
import com.zimbra.cs.redolog.op.CreateFolder;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.redolog.util.RedoLogVerify;
import com.zimbra.cs.simioj.SimiojConnector;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;

import junit.framework.TestCase;

/**
 * Tests redolog operations
 */
public class TestRedoLog {

    @Rule
    public TestName testInfo = new TestName();

    private static String USER_NAME;
    private static String RESTORED_NAME;
    private static String NAME_PREFIX;

    @Before
    public void setUp() throws Exception {
        NAME_PREFIX = this.getClass().getSimpleName();
        String prefix = NAME_PREFIX + "-" + testInfo.getMethodName() + "-";
        USER_NAME = prefix + "user";
        RESTORED_NAME = prefix + "restored-user";
        tearDown();
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteAccountIfExists(USER_NAME);
        TestUtil.deleteAccountIfExists(RESTORED_NAME);
    }

    @Test
    public void testRedoLogVerify()
            throws Exception {
        try (PrintStream ps = new PrintStream(new DevNullOutputStream())) {
            RedoLogVerify verify = new RedoLogVerify(null, ps);
            assertTrue("RedoLogVerify.verifyFile should have been true", verify.verifyFile(getRedoLogFile()));
        }
    }

    /**
     * Verifies that redolog replay successfully copies a message from one mailbox
     * to another and leaves the original blob intact.  See bug 22873.
     */

    @Test
    public void testTestRestoreMessageToNewAccount()
    throws Exception {
        TestUtil.createAccount(USER_NAME);
        Mailbox sourceMbox = TestUtil.getMailbox(USER_NAME);  // make sure mailbox is pre-created as well as account
        // Add message to source account.
        long startTime = System.currentTimeMillis();
        Message sourceMsg = TestUtil.addMessage(sourceMbox, NAME_PREFIX + " testRestoreMessageToNewAccount");
        String sourceContent = new String(sourceMsg.getContent());
        assertTrue("Message.getContent() length should not be 0", sourceContent.length() != 0);

        // Replay log to destination account.
        Account destAccount = TestUtil.createAccount(RESTORED_NAME);
        RedoPlayer player = new RedoPlayer(false, true, false, false, false);
        Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
        Mailbox destMbox = MailboxManager.getInstance().getMailboxByAccount(destAccount);
        idMap.put(sourceMbox.getId(), destMbox.getId());
        ZimbraLog.test.info("Source Mailbox ID=%s Dest ID=%s", sourceMbox.getId(), destMbox.getId());
        player.scanLog(getRedoLogFile(), true, idMap, startTime, Long.MAX_VALUE);

        // Get destination message and compare content.
        List<Integer> destIds = TestUtil.search(destMbox, "in:inbox " + NAME_PREFIX, MailItem.Type.MESSAGE);
        assertEquals("Search should should only find 1 message", 1, destIds.size());
        Message destMsg = destMbox.getMessageById(null, destIds.get(0));
        String destContent = new String(destMsg.getContent());
        assertEquals("Expected=sourceContent Actual=destContent", sourceContent, destContent);

        // Make sure source content is still on disk.
        MailboxBlob blob = sourceMsg.getBlob();
        assertNotNull("Blob for sourceMsg should not be null", blob);
        sourceContent = new String(ByteUtil.getContent(StoreManager.getInstance().getContent(blob),
                sourceContent.length()));
        assertEquals("From disk Expected=destContent Actual=sourceContent", destContent, sourceContent);
    }

    public void testRestoreFoldersToNewAccount()
    throws Exception {
        // Add message to source account.
        long startTime = System.currentTimeMillis();
        Mailbox sourceMbox = TestUtil.getMailbox(USER_NAME);
        ZMailbox sourceZMbox = TestUtil.getZMailbox(USER_NAME);
        String folderPath = NAME_PREFIX + " testRestoreFolderToNewAccount";
        String subFolder = "sub folder";
        String subFolderPath = folderPath + "/" + subFolder;
        ZFolder srcFolder = TestUtil.createFolder(sourceZMbox, folderPath);
        assertTrue("Name of source folder should not be empty", srcFolder.getName().length() > 0);
        ZFolder srcSubFolder = TestUtil.createFolder(sourceZMbox, srcFolder.getId(), subFolder);
        assertTrue("Name of source folder should not be empty", srcSubFolder.getName().length() > 0);

        // Replay log to destination account.
        Account destAccount = TestUtil.createAccount(RESTORED_NAME);
        RedoPlayer player = new RedoPlayer(false, true, false, false, false);
        Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
        Mailbox destMbox = MailboxManager.getInstance().getMailboxByAccount(destAccount);
        idMap.put(sourceMbox.getId(), destMbox.getId());
        player.scanLog(getRedoLogFile(), true, idMap, startTime, Long.MAX_VALUE);
        Folder destFolder = TestUtil.getFolderByPath(destMbox, folderPath);
        assertNotNull(String.format("Should have found dest folder by path '%s'", folderPath), destFolder);
        assertEquals("source and dest folder names", srcFolder.getName(), destFolder.getName());
        Folder destSubFolder = TestUtil.getFolderByPath(destMbox, subFolderPath);
        assertNotNull(String.format("Should have found dest folder by path '%s'", subFolderPath), destSubFolder);
        assertEquals("source and dest subfolder names", srcSubFolder.getName(), destSubFolder.getName());
        assertEquals("source and dest subfolder paths", srcSubFolder.getPath(), destSubFolder.getPath());
    }

    private File getRedoLogFile() {
        return new File("/opt/zimbra/redolog/redo.log");
    }

    public void testSimiojCreateFolder() throws ServiceException, IOException {
        Mailbox sourceMbox = TestUtil.getMailbox(USER_NAME);
        int parentFolderId = Mailbox.ID_FOLDER_USER_ROOT;
        Folder.FolderOptions fopt = new Folder.FolderOptions();
        fopt.setColor(new Color(MailItem.DEFAULT_COLOR)).setUrl(null);
        fopt.setDefaultView(MailItem.Type.of(null)).setFlags(Flag.toBitmask(null));
        CreateFolder cfOpt = new CreateFolder(sourceMbox.getId(), "Name", parentFolderId, fopt);
        String opFolderName = SimiojConnector.INSTANCE.submit(cfOpt);
        RedoableOp obj = SimiojConnector.INSTANCE.waitForCommit(opFolderName);
        assertTrue(String.format("Class %s should be CreateFolder", obj.getClass().getName()),
                obj instanceof CreateFolder);
    }

    @Override public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        TestUtil.deleteAccount(RESTORED_NAME);
    }
}
