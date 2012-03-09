package com.zimbra.cs.redolog.op;

import java.util.HashMap;

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

public class SetActiveSyncDisabledTest {
    
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
        Folder folder = mbox.createFolder(null, "/test", (byte) 0, MailItem.Type.MESSAGE);
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
