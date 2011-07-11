/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.redolog.op;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.RetentionPolicy;

public class SetRetentionPolicyTest {

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
     * Verifies serializing, deserializing, and replaying.
     */
    @Test
    public void redo() throws Exception {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        
        // Create folder.
        Folder folder = mbox.createFolder(null, "/redo", (byte) 0, MailItem.Type.MESSAGE);
        assertEquals(0, folder.getKeepPolicy().size());
        assertEquals(0, folder.getPurgePolicy().size());
        
        // Create RedoableOp.
        List<RetentionPolicy> keepPolicy = Lists.newArrayList();
        keepPolicy.add(RetentionPolicy.newSystemPolicy("123"));
        List<RetentionPolicy> purgePolicy = Lists.newArrayList();
        purgePolicy.add(RetentionPolicy.newUserPolicy("45m"));
        SetRetentionPolicy redoPlayer = new SetRetentionPolicy(mbox.getId(), folder.getId(), keepPolicy, purgePolicy);
        
        // Serialize, deserialize, and redo.
        byte[] data = redoPlayer.testSerialize();
        redoPlayer = new SetRetentionPolicy();
        redoPlayer.setMailboxId(mbox.getId());
        redoPlayer.testDeserialize(data);
        redoPlayer.redo();
        folder = mbox.getFolderById(null, folder.getId());
        assertEquals(1, folder.getKeepPolicy().size());
        assertEquals(1, folder.getPurgePolicy().size());
        assertEquals("45m", folder.getPurgePolicy().get(0).getLifetimeString());
        assertEquals("123", folder.getKeepPolicy().get(0).getId());
    }
}
