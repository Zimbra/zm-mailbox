/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.redolog;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Folder.FolderOptions;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.MailboxTransactionProxy;
import com.zimbra.cs.redolog.op.MockRedoableOp;
import com.zimbra.cs.redolog.op.RedoableOp;
import com.zimbra.cs.redolog.util.RedoLogVerify;

public class RedoPlayerTest {
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        clearRedoLogs();
        RedoLogProvider provider = RedoLogProvider.getInstance();
        if (provider instanceof MockRedoLogProvider) {
            //mock provider does not start/stop normally since most other tests don't need it
            ((MockRedoLogProvider) provider).forceStart();
        } else {
            provider.startup();
        }
        MailboxTestUtil.clearData();
    }

    private void clearRedoLogs() throws IOException, ServiceException {
        RedoLogManager mgr = RedoLogProvider.getInstance().getRedoLogManager();
        Set<File> redoLogFiles = new HashSet<File>();
        File[] archived = mgr.getArchivedLogs();
        if (archived != null && archived.length > 0) {
            redoLogFiles.addAll(Arrays.asList(archived));
        }
        redoLogFiles.add(mgr.getLogFile());
        for (File file : redoLogFiles) {
            ZimbraLog.test.debug("deleting redolog file %s", file.getAbsolutePath());
            file.delete();
        }
    }

    @After
    public void tearDown() throws Exception {
        RedoLogProvider provider = RedoLogProvider.getInstance();
        if (provider instanceof MockRedoLogProvider) {
            ((MockRedoLogProvider) provider).forceStop();
        } else {
            provider.shutdown();
        }
        clearRedoLogs();
        MailboxTestUtil.clearData();
    }


    @Test
    public void playbackCrashedOp() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String folderName = "testfolder";
        String serverId = "someserverid";
        MockRedoableOp op  = new MockRedoableOp(mbox.getId(), folderName, Mailbox.ID_FOLDER_INBOX, new FolderOptions());

        op.setFolderIdAndUuid(1234, "fakeuuid");
        op.setServerId(serverId);
        op.start(System.currentTimeMillis());
        op.log(true);

        RedoPlayer player = new RedoPlayer(false);

        player.runCrashRecovery(RedoLogProvider.getInstance().getRedoLogManager(), new ArrayList<RedoableOp>(), null);

        Folder testFolder = mbox.getFolderByPath(null, "/Inbox/" + folderName);
        Assert.assertNotNull(testFolder);
        Assert.assertEquals(folderName, testFolder.getName());
    }

    @Test(expected=NoSuchItemException.class)
    public void playbackCrashedOpOtherServer() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String folderName = "testfolder";
        String serverId = "someserverid";
        MockRedoableOp op  = new MockRedoableOp(mbox.getId(), folderName, Mailbox.ID_FOLDER_INBOX, new FolderOptions());

        op.setFolderIdAndUuid(1234, "fakeuuid");
        op.setServerId(serverId);
        op.start(System.currentTimeMillis());
        op.log(true);

        RedoPlayer player = new RedoPlayer(false);

        Set<String> serverIds = new HashSet<String>();
        serverIds.add("otherserverid");
        player.runCrashRecovery(RedoLogProvider.getInstance().getRedoLogManager(), new ArrayList<RedoableOp>(), serverIds);

        mbox.getFolderByPath(null, "/Inbox/" + folderName);
        Assert.fail("getting non-existant folder should throw exception");
    }

    @Test
    public void playbackCrashedOpByServer() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String folderName = "testfolder";
        String serverId = "someserverid";
        MockRedoableOp op  = new MockRedoableOp(mbox.getId(), folderName, Mailbox.ID_FOLDER_INBOX, new FolderOptions());

        op.setFolderIdAndUuid(1234, "fakeuuid");
        op.setServerId(serverId);
        op.start(System.currentTimeMillis());
        op.log(true);

        RedoPlayer player = new RedoPlayer(false);

        Set<String> serverIds = new HashSet<String>();
        serverIds.add(serverId);
        player.runCrashRecovery(RedoLogProvider.getInstance().getRedoLogManager(), new ArrayList<RedoableOp>(), serverIds);

        Folder testFolder = mbox.getFolderByPath(null, "/Inbox/" + folderName);
        Assert.assertNotNull(testFolder);
        Assert.assertEquals(folderName, testFolder.getName());
    }

    @Test(expected=NoSuchItemException.class)
    public void doNotPlaybackCommitted() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String folderName = "testfolder";
        MockRedoableOp op  = new MockRedoableOp(mbox.getId(), folderName, Mailbox.ID_FOLDER_INBOX, new FolderOptions());

        op.setFolderIdAndUuid(1234, "fakeuuid");
        MailboxTransactionProxy.beginTransaction(mbox, "mockOp", op);
        MailboxTransactionProxy.endTransaction(mbox, true);

        File logFile = RedoLogProvider.getInstance().getRedoLogManager().getLogFile();

        RedoPlayer player = new RedoPlayer(false);
        //make sure redocommitted = false doesn't process committed txn
        player.scanLog(logFile, false, null, 0, System.currentTimeMillis(), null);

        mbox.getFolderByPath(null, "/Inbox/" + folderName);
        Assert.fail("getting non-existant folder should throw exception");
    }

    @Test
    public void playbackCommitted() throws Exception {
        ZimbraLog.test.info("playbackCommitted(): begin");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        String folderName = "testfolder";
        MockRedoableOp op  = new MockRedoableOp(mbox.getId(), folderName, Mailbox.ID_FOLDER_INBOX, new FolderOptions());

        op.setFolderIdAndUuid(1234, "fakeuuid");
        MailboxTransactionProxy.beginTransaction(mbox, "mockOp", op);
        MailboxTransactionProxy.endTransaction(mbox, true);

        RedoLogProvider.getInstance().getRedoLogManager().flush();
        File logFile = RedoLogProvider.getInstance().getRedoLogManager().getLogFile();

        RedoLogVerify verify = new RedoLogVerify(null, System.out);
        Assert.assertTrue(verify.verifyFile(logFile));

        RedoPlayer player = new RedoPlayer(false);

        //playback with redocommitted = true; this should result in op completion
        ZimbraLog.test.info("playbackCommitted(): starting to playback");
        player.scanLog(logFile, true, null, 0, System.currentTimeMillis(), null);
        ZimbraLog.test.info("playbackCommitted(): done playback");
        Folder testFolder = mbox.getFolderByPath(null, "/Inbox/" + folderName);
        Assert.assertNotNull(testFolder);
        Assert.assertEquals(folderName, testFolder.getName());
        ZimbraLog.test.info("playbackCommitted(): end");
    }

    @Test
    public void playbackCommittedByServer() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        String folderName = "testfolder";
        String serverId = "someserverid";

        MockRedoableOp op  = new MockRedoableOp(mbox.getId(), folderName, Mailbox.ID_FOLDER_INBOX, new FolderOptions());
        op.setFolderIdAndUuid(1234, "fakeuuid");
        op.setServerId(serverId);
        MailboxTransactionProxy.beginTransaction(mbox, "mockOp", op);
        MailboxTransactionProxy.endTransaction(mbox, true);
        RedoLogProvider.getInstance().getRedoLogManager().flush();
        File logFile = RedoLogProvider.getInstance().getRedoLogManager().getLogFile();
        RedoPlayer player = new RedoPlayer(false);

        //playback with some other serverids
        HashSet<String> serverIds = new HashSet<String>();
        serverIds.add("someotherserverid");
        serverIds.add(MockRedoableOp.getLocalServerId());
        player.scanLog(logFile, true, null, 0, System.currentTimeMillis(), serverIds);

        try {
            Folder testFolder = mbox.getFolderByPath(null, "/Inbox/" + folderName);
            Assert.fail("getting non-existant folder should throw exception");
        } catch (NoSuchItemException e) {
            //expected; continue
        }

        serverIds = new HashSet<String>();
        serverIds.add(serverId);
        //since we're playing back committed and the txn commit is from this server we have to include local server ID
        //basically deferring the need for more test-related plumbing for now
        serverIds.add(MockRedoableOp.getLocalServerId());
        ZimbraLog.test.info("playbackCommittedByServer(): starting to playback");
        player.scanLog(logFile, true, null, 0, System.currentTimeMillis(), serverIds);
        ZimbraLog.test.info("playbackCommittedByServer(): finished playback");
        Folder testFolder = mbox.getFolderByPath(null, "/Inbox/" + folderName);
        Assert.assertNotNull(testFolder);
        Assert.assertEquals(folderName, testFolder.getName());
    }


}
