/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Zimbra, Inc.
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

package com.zimbra.cs.mailbox;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailItem.UnderlyingData;

@RunWith(PowerMockRunner.class)
@PrepareForTest({OperationContext.class, Mailbox.class, CalendarItem.class})
public class CalendarItemTest {
    private OperationContext octxt;
    private CalendarItem calItem;
    private final Integer SEQ = 123;

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        octxt = PowerMockito.mock(OperationContext.class);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        calItem = PowerMockito.mock(CalendarItem.class);
        calItem.mMailbox = mbox;
    }

    /**
     * @throws java.lang.Exception
     */
    @Test
    public void testPerformSetPrevFoldersOperation() throws Exception {
        PowerMockito.when(calItem.getModifiedSequence()).thenReturn(SEQ);
        PowerMockito.when(calItem, "performSetPrevFoldersOperation", octxt).thenCallRealMethod();
        PowerMockito.when(calItem, "getPrevFolders").thenCallRealMethod();

        calItem.performSetPrevFoldersOperation(octxt);
        // above method adds 2 in mod sequence, so verify SEQ+2 and Mailbox.ID_FOLDER_TRASH

        Assert.assertEquals(calItem.state.getPrevFolders(), (SEQ+2)+":" + Mailbox.ID_FOLDER_TRASH);
        Assert.assertEquals(calItem.getPrevFolders(), (SEQ+2)+":" + Mailbox.ID_FOLDER_TRASH);
        Assert.assertEquals(calItem.state.getPrevFolders(), calItem.getPrevFolders());
    }
}