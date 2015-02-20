/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
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
import org.junit.Test;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.DevNullOutputStream;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.RedoPlayer;
import com.zimbra.cs.redolog.util.RedoLogVerify;
import com.zimbra.cs.store.MailboxBlob;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.util.ProvisioningUtil;

/**
 * Tests redolog operations
 */
public class TestRedoLog {
    private static final String NAME_PREFIX = TestRedoLog.class.getSimpleName();
    private static final String USER_NAME = NAME_PREFIX + "_user1";
    private static final String RESTORED_NAME = "testredolog";
    private boolean originalLCSetting = false;

    @Before
    public void setUp()
    throws Exception {
        cleanUp();
        originalLCSetting = ProvisioningUtil.getServerAttribute(Provisioning.A_zimbraIndexManualCommit, true);
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
        TestUtil.createAccount(USER_NAME);
    }

    @Test
    public void testRedoLogVerify()
    throws Exception {
        RedoLogVerify verify = new RedoLogVerify(null, new PrintStream(new DevNullOutputStream()));
        assertTrue(verify.verifyFile(getRedoLogFile()));
    }

    /**
     * Verifies that redolog replay successfully copies a message from one mailbox
     * to another and leaves the original blob intact.  See bug 22873.
     */

    @Test
    public void testTestRestoreMessageToNewAccount()
    throws Exception {
        // Add message to source account.
        Mailbox sourceMbox = TestUtil.getMailbox(USER_NAME);
        long startTime = System.currentTimeMillis();
        Message sourceMsg = TestUtil.addMessage(sourceMbox, NAME_PREFIX + " testRestoreMessageToNewAccount");
        String sourceContent = new String(sourceMsg.getContent());
        assertTrue(sourceContent.length() != 0);

        // Replay log to destination account.
        Account destAccount = TestUtil.createAccount(RESTORED_NAME);
        RedoPlayer player = new RedoPlayer(false, true, false, false, false);
        Map<Integer, Integer> idMap = new HashMap<Integer, Integer>();
        Mailbox destMbox = MailboxManager.getInstance().getMailboxByAccount(destAccount);
        idMap.put(sourceMbox.getId(), destMbox.getId());
        player.scanLog(getRedoLogFile(), true, idMap, startTime, Long.MAX_VALUE, null);

        // Get destination message and compare content.
        List<Integer> destIds = TestUtil.search(destMbox, "in:inbox " + NAME_PREFIX, MailItem.Type.MESSAGE);
        assertEquals(1, destIds.size());
        Message destMsg = destMbox.getMessageById(null, destIds.get(0));
        String destContent = new String(destMsg.getContent());
        assertEquals(sourceContent, destContent);

        // Make sure source content is still on disk.
        MailboxBlob blob = sourceMsg.getBlob();
        assertNotNull(blob);
        sourceContent = new String(ByteUtil.getContent(StoreManager.getInstance().getContent(blob), sourceContent.length()));
        assertEquals(destContent, sourceContent);
    }

    private File getRedoLogFile() throws IOException {
        return RedoLogProvider.getInstance().getRedoLogManager().getLogFile();//new File("/opt/zimbra/redolog/redo.log");
    }

    @After
    public void tearDown()
    throws Exception {
        cleanUp();
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(originalLCSetting);
    }

    private void cleanUp()
    throws Exception {
        if(TestUtil.accountExists(USER_NAME)) {
            TestUtil.deleteAccount(USER_NAME);
        }
        if(TestUtil.accountExists(RESTORED_NAME)) {
            TestUtil.deleteAccount(RESTORED_NAME);
        }
    }
}
