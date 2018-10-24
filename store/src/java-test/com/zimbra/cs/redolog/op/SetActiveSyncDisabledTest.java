/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.redolog.op;

import java.util.HashMap;
import org.junit.Ignore;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class SetActiveSyncDisabledTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    /**
     * Verifies serializing, de-serializing, and replaying for folder.
     */
    @Test
    public void setDisableActiveSyncUserFolder() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);

        // Create folder.
        Folder folder = mbox.createFolder(null, "/test", new Folder.FolderOptions().setDefaultView(MailItem.Type.MESSAGE));
        Assert.assertFalse(folder.isActiveSyncDisabled());

        // Create RedoableOp.
        SetActiveSyncDisabled redoPlayer = new SetActiveSyncDisabled(mbox.getId(), folder.getId(), true);

        // Serialize, deserialize, and redo.
        byte[] data = redoPlayer.testSerialize();
        redoPlayer = new SetActiveSyncDisabled();
        redoPlayer.setMailboxId(mbox.getId());
        redoPlayer.testDeserialize(data);
        redoPlayer.redo();
        folder = mbox.getFolderById(null, folder.getId());
        Assert.assertTrue(folder.isActiveSyncDisabled());
    }

    @Test
    public void setDisableActiveSyncSystemFolder() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(null, Mailbox.ID_FOLDER_INBOX);
        Assert.assertFalse(folder.isActiveSyncDisabled());

        //try setting disableActiveSync to true!!
        folder.setActiveSyncDisabled(true);

        //cannot disable activesync for system folders!!
        Assert.assertFalse(folder.isActiveSyncDisabled());
    }

}
