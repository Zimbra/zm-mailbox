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

package com.zimbra.cs.redolog.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder.FolderOptions;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.redolog.FilesystemRedoLogFile;
import com.zimbra.cs.redolog.MockRedoLogProvider;
import com.zimbra.cs.redolog.RedoLogFile;
import com.zimbra.cs.redolog.RedoLogManager;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.op.MockRedoableOp;

public class RedoLogVerifyTest {
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
            provider.startup(true);
        }
        MailboxTestUtil.clearData();
    }

    private void clearRedoLogs() throws IOException, ServiceException {
        RedoLogManager mgr = RedoLogProvider.getInstance().getRedoLogManager();
        Set<RedoLogFile> redoLogFiles = new HashSet<RedoLogFile>();
        RedoLogFile[] archived = mgr.getArchivedLogs();
        if (archived != null && archived.length > 0) {
            redoLogFiles.addAll(Arrays.asList(archived));
        }
        redoLogFiles.add(new FilesystemRedoLogFile(mgr.getLogFile()));
        for (RedoLogFile file : redoLogFiles) {
            file.getFile().delete();
        }
    }

    @After
    public void tearDown() throws ServiceException, IOException {
        RedoLogProvider provider = RedoLogProvider.getInstance();
        if (provider instanceof MockRedoLogProvider) {
            ((MockRedoLogProvider) provider).forceStop();
        } else {
            provider.shutdown();
        }
        clearRedoLogs();
    }


    private final int STATE_BEFORE_HEADER = 0;
    private final int STATE_START_HEADER = 1;
    private final int STATE_END_HEADER = 2;

    private List<String> getOpsFromOutput(ByteArrayOutputStream baos) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));

        String line = null;
        List<String> oplines = new ArrayList<String>();

        int state = STATE_BEFORE_HEADER;
        while ((line = reader.readLine()) != null) {
            ZimbraLog.test.debug("##RedoLogVerify Output## " + line);
            switch (state) {
            case STATE_BEFORE_HEADER:
                if (line.equals(RedoLogVerify.HEADER_MARKER)) state = STATE_START_HEADER;
                break;
            case STATE_START_HEADER:
                if (line.equals(RedoLogVerify.HEADER_MARKER)) state = STATE_END_HEADER;
                break;
            case STATE_END_HEADER:
                oplines.add(line);
                break;
            }
        }
        return oplines;
    }

    @Test
    public void verifyOp() throws Exception {
        String folderName = "testfolder";
        String serverId = "someserverid";
        int mboxId = 999;
        MockRedoableOp op  = new MockRedoableOp(mboxId, folderName, Mailbox.ID_FOLDER_INBOX, new FolderOptions());

        op.setFolderIdAndUuid(1234, "fakeuuid");
        op.setServerId(serverId);
        op.start(System.currentTimeMillis());
        op.log(true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RedoLogVerify verify = new RedoLogVerify(null, new PrintStream(baos));

        File logfile = RedoLogProvider.getInstance().getRedoLogManager().getLogFile();

        verify.scanLog(logfile);

        List<String> ops = getOpsFromOutput(baos);
        Assert.assertEquals(1, ops.size());
        String line = ops.get(0);
        Assert.assertTrue(line.contains("serverId=" + serverId));
        Assert.assertTrue(line.contains("mailbox=" + mboxId));
        Assert.assertTrue(line.contains("name=" + folderName));
    }

    @Test
    public void verifyOpByServer() throws Exception {
        String folderName = "testfolder";
        String serverId = "someserverid";
        int mboxId = 999;
        MockRedoableOp op  = new MockRedoableOp(mboxId, folderName, Mailbox.ID_FOLDER_INBOX, new FolderOptions());

        op.setFolderIdAndUuid(1234, "fakeuuid");
        op.setServerId(serverId);
        op.start(System.currentTimeMillis());
        op.log(true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        RedoLogVerify.Params params = new RedoLogVerify.Params();
        params.serverIds = new HashSet<String>();
        params.serverIds.add(serverId);
        RedoLogVerify verify = new RedoLogVerify(params, new PrintStream(baos));

        File logfile = RedoLogProvider.getInstance().getRedoLogManager().getLogFile();

        verify.scanLog(logfile);

        List<String> ops = getOpsFromOutput(baos);
        Assert.assertEquals(1, ops.size());
        String line = ops.get(0);
        Assert.assertTrue(line.contains("serverId=" + serverId));
        Assert.assertTrue(line.contains("mailbox=" + mboxId));
        Assert.assertTrue(line.contains("name=" + folderName));
    }

    @Test
    public void verifyOpWrongServer() throws Exception {
        String folderName = "testfolder";
        String serverId = "someserverid";
        int mboxId = 999;
        MockRedoableOp op  = new MockRedoableOp(mboxId, folderName, Mailbox.ID_FOLDER_INBOX, new FolderOptions());

        op.setFolderIdAndUuid(1234, "fakeuuid");
        op.setServerId(serverId);
        op.start(System.currentTimeMillis());
        op.log(true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        RedoLogVerify.Params params = new RedoLogVerify.Params();
        params.serverIds = new HashSet<String>();
        params.serverIds.add("wrongserver");
        RedoLogVerify verify = new RedoLogVerify(params, new PrintStream(baos));

        File logfile = RedoLogProvider.getInstance().getRedoLogManager().getLogFile();

        verify.scanLog(logfile);

        List<String> ops = getOpsFromOutput(baos);
        Assert.assertEquals(0, ops.size());
    }

}
